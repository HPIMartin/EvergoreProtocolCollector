# 04: Architecture

- Stack: Java · Micronaut (Netty) · ORMLite + SQLite · Selenium · OpenAPI/Swagger; exact versions in
  [`build.gradle.kts`](../../build.gradle.kts), the single source of truth (not duplicated here).
- Root package `dev.schoenberg.evergore.protocolParser` (`…` below).

## Data-flow (the one use case that matters)

```
@Scheduled (24h)  EvergoreDataCollectorJob
        │
        ▼
   EvergoreDataExtractor.loadData()
        │  1. PageSource (port) → SeleniumPageSource (Selenium adapter)  ──Selenium──▶  evergore.de (login, paginate bank + Lager protocols)
        │  2. EntityParser.parse → EntryFactory   (raw text ▶ domain Entry list, regex, dedup)
        │  3. map Entry ▶ BankEntry / StorageEntry
        │  4. dedup over the whole scraped window: fetch repo.getAllSince(min scraped timestamp) and
        │     ingest only the surplus over those stored rows (occurrence-counting, so two legitimate
        │     identical same-minute rows both survive); heals a still-visible row an earlier, buggy
        │     scrape failed to store, regardless of how old it is relative to the stored max
        │  5. persist via BankRepository / StorageRepository
        ▼
   (scrape success ▶ LastRunStatus.recordSuccessfulScrape(...); scrape failure ▶ recordScrapeFailure(...),
    logged, not rethrown, so the recompute below runs regardless)
        ▼
   EvergoreDataEvaluator.evaluateData()
        │  per avatar, full recompute from ALL stored entries (no cutoff): sum bank + value storage
        │  (TransferType visitor + EvergoreItem); overwrites MetaInformationRepository
        │  a failing avatar is caught, named and skipped; last_updated stamped on every completed run
        ▼
   LastRunStatus.recordSuccessfulRecompute(when, EvaluationResult)  (one atomic snapshot)
   (recompute failure ▶ recordRecomputeFailure(...), rethrown)   (monitoring seam)
        ▼
   PostCollectionHook   (no-op in prod; test seam, runs only after a successful recompute)

Independent read path:  HTTP ▶ filters (audit log, rate limit, unreadable-request answer, token) ▶ AvatarSummariesController /
                        AvatarEntriesController ▶ read MetaInformation / repositories
                        ▶ wire records ▶ JSON

Monitoring read path:   GET /health  (token-exempt, anonymous) ▶ Micronaut management
                        ▶ LastRunHealthIndicator ▶ a verdict on the latest recompute + the lastRun
                        details, both read from LastRunStatus

Admin read path:        GET /api/v1/admin/status  (token-exempt, anonymous) ▶ AdminStatusController
                        ▶ lastUpdated read from AvatarContributions/MetaInformationRepository
                        (persisted) + the last run's facts read from LastRunStatus (in-memory, the
                        source /health reads); it serves the facts, /health keeps the verdict
```

- Each endpoint's verdict and fields: [build-run-deploy.md](build-run-deploy.md), "HTTP endpoints".

## Layers & responsibilities (condensed)

- **Entry/lifecycle:** `Application` (boots Micronaut) · `ApplicationFactory` (`@Factory` composition
  root: builds the one `SqliteDatabase` and the un-annotated repositories on it, the framework-free
  `application` use-cases, `FileLoader`, no-op hooks) · `EvergoreDataCollectorJob` (`@Scheduled`).
  The schema belongs to Flyway: `SqliteDatabase.open` runs the versioned migrations in
  `src/main/resources/db/migration` before it opens the connection source, so no caller can reach
  an unmigrated database.
- **Application use-cases (framework-free):** `application/{EvergoreDataExtractor,EvergoreDataEvaluator}`
  (collect + evaluate coordinators) · `application/LastRunStatus` (monitoring seam: what the
  last run reached, see the pipeline above). Plain objects, wired in `ApplicationFactory`.
- **Extraction pipeline:** `helper/selenium/{Browser,Driver,FileLoader}` ·
  `dataExtraction/website/SeleniumPageSource` (Selenium adapter: login, cookie banner, pagination;
  implements `PageSource`) · `PageContents` (DTO) · `parser/{EntityParser,EntryFactory}` (text ▶
  `Entry`, regex from `Constants`; `ApplicationFactory` builds the `EntityParser` around the `Logger` bean and injects it into the extractor). Coordinator `EvergoreDataExtractor` (delta filter) lives in
  `application`.
- **Persistence:** three adapters implementing the businessLogic ports, all built on one database:
  - `database/SqliteDatabase`, one per context, opens the file once and hands out pooled
    connections:
    - none to a second thread while one holds it;
    - a transaction keeps its own connection until it ends, so a read on another thread cannot land
      inside it;
    - a transaction runs on one connection with its own `BEGIN`, `COMMIT` and `ROLLBACK`;
    - any failure inside it, a commit SQLite refuses or an `Error` included, rolls it back before
      the connection goes back to the pool, unless the rollback itself fails;
      - why: sqlite-jdbc's `setAutoCommit(true)` marks the connection as autocommitting before its
        `commit`, so after a refused commit the pool's rollback-on-release never fires;
    - the caller sees that failure, with a failed rollback attached to it, not one from the
      rollback;
    - a transaction inside another one fails, since SQLite starts no transaction within a
      transaction;
    - the context closes the database when it stops.
  - **Lock waits:**
    - reads and write-first statements wait up to **10 s**
      (`Configuration.getDatabaseBusyTimeout()`) on another connection's lock before they
      fail with `SQLITE_BUSY`;
      - why: a read outlasts an unusually long write instead of answering 500 (sqlite-jdbc's
        implicit bound is 3 s);
    - a commit also waits up to 10 s for an outside reader to finish;
    - a transaction that reads before it writes, the recompute's batch, fails at once under another
      writer's lock (SQLite answers `SQLITE_BUSY` there without waiting);
    - in-process that failure is unreachable, because the scheduled job is the only writer.
  - `database/LedgerDatabaseRepository` is the one ledger base:
    - the seven ledger operations live here once, over the `LedgerDatabaseEntry` row (the columns
      both ledger rows share);
    - so does the transfer-type conversion through `TransferTypeDatabaseVisitor` (enum ⇄ German DB
      strings "Einlagerung"/"Entnahme");
    - `database/{bank,storage}/*` add only their row and the mapping between that row and its entry
      record.
  - **A ledger write is one batch:**
    - `add` converts the whole list first, so an entry that cannot be converted stores nothing;
    - it then writes the list in one `SqliteDatabase` transaction with one commit;
      - why: on the pool ORMLite's `create(Collection)` commits every row (300 rows: 43 s against
        210 ms);
    - a batch that fails stores none of its rows, whether a constraint refuses one, its commit is
      refused or SQLite rolls it back itself;
    - a row a constraint refuses fails its whole batch every run until it leaves the game's 30-day
      window;
    - rows of that batch older than it that leave the window meanwhile are lost.
  - **A ledger page is read in a total order** (decision 2026-10-05):
    - the chosen column in the chosen direction, then `timeStamp` newest first, then the row `id`
      ascending;
      - why: a page boundary inside a run of ties neither repeats a row nor drops one;
    - `timeStamp` is left out when the time is the chosen column;
    - the `id` is a random UUID, so rows that tie on everything before it stand in a stable but
      arbitrary order;
    - the port names the column by a key per ledger (`BankSortKey`, `StorageSortKey`);
    - each ledger maps its keys to its own column constants (`columnOf`);
      - why: no column name from a request reaches the SQL;
    - the avatar, the same on every row of one avatar's ledger, sorts without the collation below.
  - **The item name sorts `COLLATE GERMAN_ORDER`,** so "Äpfel" sorts before "Zwiebel" rather than
    after it as SQLite's byte order would put it:
    - `GermanOrderConnectionSource` registers the collation on every connection the pool opens;
    - each registration compares with a collator of its own (`GermanOrder.ownCollator()`), the
      rules `GermanOrder` sorts by;
      - why: a `Collator` compares under a lock, so a shared one serializes concurrent sorts;
        - a synthetic probe, 16 sorts of 13,739 rows on 8 threads: 5.9 to 8.9 s shared, 0.55 s
          with one per connection;
    - the source subclasses ORMLite's pool because `makeConnection` is the only per-connection hook
      it offers;
    - it overrides `close()` to declare `SQLException`;
      - why: ORMLite's `close() throws Exception` raises `-Xlint:try` on every subclass, and the
        warning is fixed rather than suppressed (decision 2026-10-06);
    - ORMLite 6.1's `close()` throws only `SQLException`;
    - any other checked exception a later version throws is wrapped in one;
    - a connection whose registration fails is closed before the failure propagates, so the pool
      never loses an open connection to it;
    - the source keeps no record of the collations it registers;
      - why: a collation holds its connection, and any record would hold every connection the pool
        ever opened;
    - a test records them through the registration it hands the source instead.
  - `database/metaInformation/*`, the meta store: a recompute's batch is written in one
    `SqliteDatabase` transaction.
- **Business logic (framework-free):** ports `BankRepository` and `StorageRepository` (each the
  generic `base/LedgerRepository` over its entry record, declaring nothing of its own),
  `MetaInformationRepository` · records `BankEntry`, `StorageEntry`, `MetaInformation` ·
  `TransferType` + visitor · `MetaInformationKey` (typed: `DateTimeKey`/`LongKey`/`DoubleKey`) ·
  `contribution/{Contribution,AvatarContribution,AvatarContributions}` (the four ledger sums, their
  net and the guild total, assembled per known avatar) · `Constants`.
- **Last activity comes from the ledgers, not from the meta store** (decision 2026-09-02):
  - both ledger ports answer `latestTimestampPerAvatar()` with **one grouped query** per ledger
    (`MAX(timeStamp) GROUP BY avatar`), so the overview materializes one row per avatar instead of
    one per ledger entry;
  - both ledgers carry an index on `(avatar, timeStamp)` (migration `V3`), which serves this query
    as a covering index and the per-avatar page and count as an index search, so none of the three
    scans the table;
  - the domain types are real instants, so nothing here reintroduces
    `MetaInformationKey.DateTimeKey`'s ambiguity and no key family joins the pending schema
    migration;
  - the ledger's **storage** format persists wall-clock text, so these columns inherit the DST
    fall-back defect of the default-timezone timestamp strings until the epoch/UTC format lands
    (backlog D14); only the container's UTC default keeps them right.
- **`TimezoneStartupValidator` is the interim safeguard:**
  - it aborts boot if the effective `ZoneId` (`ApplicationFactory#effectiveZone`, the JVM's
    `ZoneId.systemDefault()`) is not a fixed offset, so a misconfigured deploy fails fast instead
    of silently reintroducing the DST defect;
  - it does not reach `last_updated`, which is pinned to the hard-coded `Constants.APP_ZONE` and
    not to the effective zone the validator checks.
- **Domain (framework-free):** `Entry`, `Item`, `EvergoreItem` (catalog + value math).
- **REST** (what each endpoint answers: [build-run-deploy.md](build-run-deploy.md), "HTTP
  endpoints"; the JSON contract: [frontend.md](frontend.md)):
  - `rest/controller/api/*` is the JSON API under `/api/v1`: `AvatarSummariesController`,
    `AvatarEntriesController`, `AdminStatusController`;
  - `AdminStatusController` keeps the operator's facts apart from the token-protected member-facing
    overview;
  - `rest/controller/api/wire/*` holds the published contract types plus `TransferTypeWireNames`;
  - `FaviconController` serves the favicon;
  - `SpaHistoryFallbackController` serves the SPA shell for unknown navigation paths;
  - `SpaNavigationPaths` decides which 404s it may answer;
  - the filter `RequestAuditLogFilter` writes one `info` line per request;
  - the filter `RateLimitFilter` throttles per IP on counters `RateLimitCounters` holds;
  - the filter `UnreadableRequestFilter` answers the stand-in request of a head the server cannot
    read;
  - the filter `TokenValidationFilter` checks `?token=`;
  - `ApplicationExceptionHandler` dispatches via the `TransferType`/exception visitors, no
    `instanceof`;
  - its mapped status picks the log severity, so an expected client error is one `info` line and
    only a server error logs its stack trace.
- **Controllers run off the event loop** (author decisions 2026-10-16,
  [open-questions.md](../open-questions.md)): `micronaut.server.thread-selection: IO` hands every
  controller method to Micronaut's IO pool.
  - Why: a read that waits on an outside writer's lock, up to the 10 s `busy_timeout`, costs its
    own request, not every connection its event-loop thread serves.
  - The management endpoints such as `/health` run on that pool too.
  - The setting covers a new controller the moment it exists, with no annotation to remember.
  - Filters and the handlers under `rest/netty/` still run on the event loop.
  - Blocking work, such as a database read, therefore belongs in a controller or below it, never in
    a filter.
- **The IO pool is fixed** (`micronaut.executors.io.type: fixed`; author decision 2026-10-16,
  [open-questions.md](../open-questions.md)).
  - Why: an unbounded pool would let any client start a platform thread per request while an
    outside lock lasts.
  - Its size is Micronaut's default, twice the machine's cores.
  - Requests beyond the pool wait in its queue.
  - While an outside writer's lock holds as many reads as the pool has threads, every further
    request, `/health` included, waits until the lock lifts.
- **The IO pool's threads are platform threads, not the virtual threads of
  `TaskExecutors.BLOCKING`** (author decision 2026-10-16, [open-questions.md](../open-questions.md)).
  - Why: SQLite waits for a lock in native code, which pins a virtual thread's carrier.
- **The token scope is default-deny** (author decision 2026-08-04): every path needs a token except
  the ones configured under `evergore.security.public-paths`.
  - Why: a new controller is protected the moment it exists, with no list to remember.
  - An empty or missing configuration protects everything (fail closed).
  - `PublicPaths` matches the configured paths with Micronaut's `AntPathMatcher`, so `/assets/**`
    is one entry.
- **Every path-based decision runs on the canonicalized path** (`PathCanonicalizer`).
  - Why: a `..` segment cannot make a protected path look public, and a leading `//` cannot be
    read as an authority.
  - The token filter therefore stays mapped on `/**`: a narrower `@Filter` pattern is matched
    against the raw path and would never reach the canonicalizing code.
  - `PathCanonicalizer`, `PublicPaths` and `SpaNavigationPaths` are injected `@Singleton`s, not
    static utilities (handbook §1).
- **The audit log and the rate limit know no exception** (author decision 2026-08-14): both filters
  run for every path.
  - Why: the configured `public-paths` stay the one list of paths with a special status.
- **`RateLimitCounters` owns the per-IP map;** a `RateLimitCounter` never leaves that class.
  - How it bounds the map and why its decision cannot evict a client mid-block:
    [build-run-deploy.md](build-run-deploy.md), "HTTP endpoints".
- **Monitoring:** `monitoring/LastRunHealthIndicator` (adapter implementing `HealthIndicator`,
  exposed at `GET /health` via `micronaut-management`), fed by `application/LastRunStatus` (above);
  its verdict and details: [build-run-deploy.md](build-run-deploy.md), "HTTP endpoints".
- **Cross-cutting:** `Logger` (own interface) + `helper/logger/Slf4jLogger` ·
  `helper/exceptionWrapper/*` (`silentThrow`) · `helper/fileLoader/*` (disc→resource→fallback) ·
  `helper/config/Configuration`.

## What's GOOD (keep this)

- **Framework-free core, mechanically enforced:** `domain`, `businessLogic`, `application` import
  nothing from `micronaut`, `selenium`, `ormlite`, `jakarta`, or `netty`; `application` and the
  core (`domain`, `businessLogic`) additionally depend only inward, the core never on adapters,
  config or the use-cases around it. `HexagonalArchitectureTest` (ArchUnit) fails the build on any
  violation: a build gate, not just a convention.
- **Persistence correctly inverted** (genuine Dependency-Inversion seam): `businessLogic` defines
  the repository *interfaces*, `database/*` implements them, application/REST depend only on the
  interfaces; `ApplicationFactory` binds interface→impl.
- **`FileLoader`, `Logger` are ports with adapters;** `PreDatabaseConnectionHook` and
  `PostCollectionHook` are deliberate test seams.
- **`TransferType` as a visitor** removes enum `switch`/`instanceof` for transfer direction.

## Hexagonal gap analysis

| Port (outbound) | Status |
|-----------------|--------|
| Persistence (bank / storage / meta) | ✅ Exists, done right |
| File / driver access (`FileLoader`) | ✅ Exists, done right |
| Logging (`Logger`) | ✅ Exists, done right |
| **Page source (scrape raw protocol)** | ✅ `PageSource` interface in `dataExtraction`; `EvergoreDataExtractor` depends on it; `SeleniumPageSource` implements it with injected `Driver`. |
| Output / presentation | 🟡 Partial: the `api` controllers map repository records to `wire` records in place; no outbound presentation port. The rendering itself sits outside the service, in the SPA. |

- **The open violation: `Configuration` is config in name only** (making it real: backlog C1):
  - it holds hard-coded Java fields (browser, server, paths) and ignores `application.yml` and the
    environment;
  - the secrets are the exception, bound from the environment (`SecurityConfiguration`,
    `CredentialsConfiguration`).
- The target layering is handbook §1's
  ([engineering-handbook.md](engineering-handbook.md), "1. Architecture rules (hexagonal)").
