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
                        ▶ LastRunHealthIndicator ▶ reports UNKNOWN (no recompute attempted yet), UP
                        (the latest recompute succeeded) or DOWN (the latest recompute failed, even
                        after an earlier success) + lastSuccessfulRecompute timestamp; scrape and
                        recompute outcomes are tracked independently, so the details also carry
                        lastSuccessfulScrape/lastScrapeFailure/lastRecomputeFailure when present,
                        telling "could not scrape" apart from
                        "could not recompute", + unknownItemCount/unknownItemNames
                        (the count is the number of distinct names, the names are in German order;
                        every name list and its count follow this rule) when the last run hit
                        unknown items, + failedAvatarCount/failedAvatarNames when an avatar could not be
                        recomputed, + roundTripCount/roundTrips when the last run's RoundTripDetector
                        reported one, + roundTripAbstentionCount/roundTripAbstentions when it abstained
                        from judging a pair

Admin read path:        GET /api/v1/admin/status  (token-exempt, anonymous) ▶ AdminStatusController
                        ▶ lastUpdated read from AvatarContributions/MetaInformationRepository (persisted,
                        stamped on the last completed evaluation) + the four scrape/recompute outcome
                        instants, unknownItemNames/failedAvatarNames and roundTrips/roundTripAbstentions
                        (names in German order, each once; round trips by avatar then item,
                        item = ingameName, in German order) read from LastRunStatus
                        (in-memory, the same source /health uses). It serves the facts; /health keeps the
                        UP/DOWN verdict derived from them
```

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
  `Entry`, regex from `Constants`). Coordinator `EvergoreDataExtractor` (delta filter) lives in
  `application`.
- **Persistence:** three adapters implementing the businessLogic ports, all built on one database:
  - `database/SqliteDatabase`, one per context: it opens the file once, runs the migrations and
    hands out pooled connections, none to a second thread while one holds it, and a transaction
    keeps its own connection until it ends, so a read on another thread cannot land inside it. The
    exception is a commit SQLite refuses, because an outside process holds a read transaction longer
    than the connection waits: that connection goes back to the pool with its transaction still open.
    The context closes the database when it stops. Reads and write-first statements wait up to
    **10 s** on another connection's lock before they fail with `SQLITE_BUSY` (sqlite-jdbc's implicit
    bound is 3 s), so a read outlasts an unusually long write instead of answering 500. A commit also
    waits up to 10 s for an outside reader to finish, and the meta `add` through `inTransaction`
    commits twice, so it can wait about 20 s (measured 20.5 s). A transaction that reads before it
    writes, the recompute's batch, fails at once under another writer's lock: SQLite answers
    `SQLITE_BUSY` there without waiting. In-process that is unreachable, because the scheduled job is
    the only writer.
  - `database/LedgerDatabaseRepository`, the one ledger base: the seven ledger operations live here
    once, over the `LedgerDatabaseEntry` row (the columns both ledger rows share), and so does the
    transfer-type conversion through `TransferTypeDatabaseVisitor` (enum ⇄ German DB strings
    "Einlagerung"/"Entnahme"). Its `add` converts the whole list first, so an entry that cannot be
    converted stores nothing, then writes it as one batch on the batch's own connection, one commit,
    because on the pool ORMLite's `create(Collection)` commits every row: 300 rows took 43 s against
    210 ms. A row a constraint refuses keeps at most the rows before it and fails every run until it
    leaves the game's 30-day window; an error on which SQLite rolls the transaction back itself
    loses the whole batch; the extractor writes oldest first, so any stored part is the oldest rows.
    A ledger page is read in a **total order** (decision 2026-10-05): the chosen column in the chosen
    direction, then `timeStamp` newest first (left out when the time is the chosen column), then the
    row `id` ascending, so a page boundary inside a run of ties neither repeats a row nor drops one;
    the `id` is a random UUID, so rows that tie on everything before it stand in a stable but
    arbitrary order. The port names the column by a key per ledger
    (`BankSortKey`, `StorageSortKey`), and each ledger maps its keys to its own column constants
    (`columnOf`), so no column name from a request reaches the SQL. The item name sorts `COLLATE
    GERMAN_ORDER`, a collation that `GermanOrderConnectionSource` registers on every connection the
    pool opens, so "Äpfel" sorts before "Zwiebel" rather than after it as SQLite's byte order would
    put it. Each registration compares with a collator of its own (`GermanOrder.ownCollator()`), the
    rules `GermanOrder` sorts by: a `Collator` compares under a lock, and one shared by every
    connection queued concurrent name sorts (a synthetic probe, 16 sorts of 13,739 rows on 8
    threads: 5.9 to 8.9 s shared, 0.55 s with one per connection). The avatar, the same on every
    row of one avatar's ledger, sorts without the collation. The source subclasses
    ORMLite's pool because `makeConnection` is the only per-connection hook it offers, and it
    overrides `close()` to declare `SQLException`: ORMLite's `close() throws Exception` raises
    `-Xlint:try` on every subclass, and the warning is fixed rather than suppressed (decision
    2026-10-06). ORMLite 6.1's `close()` throws only `SQLException`; any other checked exception a
    later version throws is wrapped in one. A connection whose registration fails is closed before the failure propagates,
    so the pool never loses an open connection to it. The source keeps no record of the collations it
    registers: a collation holds its connection, so any record would hold every connection the pool
    ever opened; a test records them through the registration it hands the source instead.
    `database/{bank,storage}/*` add only their row and the mapping between that row and its entry
    record.
  - `database/metaInformation/*`, the meta store: a recompute's batch is written in one
    `SqliteDatabase` transaction.
- **Business logic (framework-free):** ports `BankRepository` and `StorageRepository` (each the
  generic `base/LedgerRepository` over its entry record, declaring nothing of its own),
  `MetaInformationRepository` · records `BankEntry`, `StorageEntry`, `MetaInformation` ·
  `TransferType` + visitor · `MetaInformationKey` (typed: `DateTimeKey`/`LongKey`/`DoubleKey`) ·
  `contribution/{Contribution,AvatarContribution,AvatarContributions}` (the four ledger sums, their
  net and the guild total, assembled per known avatar) · `Constants`.
- **Last activity comes from the ledgers, not from the meta store** (decision 2026-09-02): both
  ledger ports answer `latestTimestampPerAvatar()` with **one grouped query** per ledger
  (`MAX(timeStamp) GROUP BY avatar`), so the overview materializes one row per avatar instead of one
  per ledger entry. Both ledgers carry an index on `(avatar, timeStamp)` (migration `V3`), which
  serves this query as a covering index and the per-avatar page and count as an index search, so
  none of the three scans the table. The domain types are real instants, so nothing here reintroduces
  `MetaInformationKey.DateTimeKey`'s ambiguity and no key family joins the pending schema migration.
  The ledger's **storage** format is a separate matter: it persists wall-clock text, so these
  columns inherit **D14**'s DST fall-back defect until the epoch/UTC format lands, and today only
  the container's UTC default keeps them right. `TimezoneStartupValidator` is the interim safeguard
  (**D22**): it aborts boot if the effective `ZoneId` (`ApplicationFactory#effectiveZone`, the JVM's
  `ZoneId.systemDefault()`) is not a fixed offset, so a misconfigured deploy fails fast instead of
  silently reintroducing the DST defect.
- **Domain (framework-free):** `Entry`, `Item`, `EvergoreItem` (catalog + value math).
- **REST:** `controller/api/*` (the JSON API under `/api/v1`: `AvatarSummariesController`,
  `AvatarEntriesController`, `AdminStatusController` (anonymous, `/api/v1/admin/status`: the
  operational facts an operator needs, separate from the token-protected member-facing overview),
  and `controller/api/wire/*` holding the published contract types plus
  `TransferTypeWireNames`; contract in
  [frontend.md](frontend.md)) · `FaviconController` ·
  `SpaHistoryFallbackController` (serves the SPA shell for unknown navigation paths;
  `SpaNavigationPaths` decides which 404s it may answer) · filters `RequestAuditLogFilter` (one
  `info` line per request) + `RateLimitFilter` (per-IP counters, held by `RateLimitCounters`) +
  `UnreadableRequestFilter` (answers the stand-in request `rest/netty/UnreadableHeadReplacer` makes of a head the server cannot read, after the two above) +
  `TokenValidationFilter` (`?token=`) · `ApplicationExceptionHandler` (dispatches via
  the `TransferType`/exception visitors, no `instanceof`; the mapped status picks the log severity,
  so an expected client error is one `info` line and only a server error logs its stack trace).
- **The token scope is default-deny** (author decision 2026-08-04): **every** path needs a token
  except the ones configured under `evergore.security.public-paths`, matched by `PublicPaths` with
  Micronaut's `AntPathMatcher` so `/assets/**` is one entry. A new controller is therefore protected
  the moment it exists; nobody has to remember to add it to a list. An empty or missing
  configuration protects everything (fail closed).
- Every path-based decision runs on the **canonicalized** path (`PathCanonicalizer`), so a `..`
  segment cannot make a protected path look public and a leading `//` cannot be read as an authority.
  The filter therefore stays mapped on `/**`: a narrower `@Filter` pattern is matched against the raw
  path and would never reach the canonicalizing code. `PathCanonicalizer`, `PublicPaths` and
  `SpaNavigationPaths` are injected `@Singleton`s, not static utilities (handbook §1).
- **The audit log and the rate limit know no exception** (author decision 2026-08-14): both filters
  run for every path, so there is exactly one public surface, the configured `public-paths`, and no
  second list of paths that skip counting or logging. `RateLimitCounters` **owns** the per-IP map and
  bounds it: it forgets a client whose interval elapsed and is not blocked, and beyond
  `evergore.rate-limit.max-tracked-clients` the least recently seen client, so the bound holds
  against any number of distinct IPs. A `RateLimitCounter` never leaves that class, and the whole
  decision (count the request, block on exceeding the budget, answer whether to reject) is **one**
  `synchronized` call, so no client can be evicted between exceeding its budget and being blocked.
- **Monitoring:** `monitoring/LastRunHealthIndicator` (adapter implementing `HealthIndicator`,
  exposed at `GET /health` via `micronaut-management`; UNKNOWN before any recompute is attempted,
  then UP or DOWN depending on whether the latest recompute succeeded, with the `lastRun` details
  listed in the pipeline above). Fed by `application/LastRunStatus` (above).
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
- **`FileLoader`, `Logger` are ports with adapters;** `Pre/PostCollectionHook` are deliberate test seams.
- **`TransferType` as a visitor** removes enum `switch`/`instanceof` for transfer direction.

## Hexagonal gap analysis

| Port (outbound) | Status |
|-----------------|--------|
| Persistence (bank / storage / meta) | ✅ Exists, done right |
| File / driver access (`FileLoader`) | ✅ Exists, done right |
| Logging (`Logger`) | ✅ Exists, done right |
| **Page source (scrape raw protocol)** | ✅ `PageSource` interface in `dataExtraction`; `EvergoreDataExtractor` depends on it; `SeleniumPageSource` implements it with injected `Driver`. |
| Output / presentation | 🟡 Partial: the `api` controllers map repository records to `wire` records in place; no outbound presentation port. The rendering itself now sits outside the service, in the SPA. |

### Top violations to fix (detail in [../backlog.md](../backlog.md))

1. **`Configuration` is config in name only:** hard-coded Java fields (browser, server, paths);
   ignores `application.yml`/env. The secrets are the exception and are already
   bound from the environment (`SecurityConfiguration`, `CredentialsConfiguration`).

## Target structure (proposed, hexagonal)

```
domain/            EvergoreItem, Entry, Item, TransferType, value objects   (no framework)
application/       use cases: CollectGuildData, EvaluateContributions, query services + PORT interfaces
adapters/in/       rest controllers/filters, the scheduled job
adapters/out/      selenium (PageSource impl), persistence (ORMLite repos), file, logging
config/            Micronaut @Factory wiring + @ConfigurationProperties
```
Keeps the clean core, names the missing PageSource port, pushes Micronaut to `adapters` + `config` only.
