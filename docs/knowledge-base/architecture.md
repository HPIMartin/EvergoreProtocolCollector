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
   CreateAccounts.forEveryKnownAvatar()   (an account for every avatar the stored ledgers name that has
                                           none; a failure is logged and changes nothing above)
        ▼
   PostCollectionHook   (no-op in prod; test seam, runs only after a successful recompute)

Independent read path:  HTTP ▶ filters (audit log, rate limit, unreadable-request answer, token, login body limit) ▶ AvatarSummariesController /
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
  last run reached, see the pipeline above) · `application/CreateAccounts` (the account step of the
  pipeline) · `application/{LogIn,ChooseNewPassword,AdminKey}` (a login by name and password, its
  second step after a reset, and the console's admin key, below). Plain objects, wired in `ApplicationFactory`.
- **Extraction pipeline:** `helper/selenium/{Browser,Driver,FileLoader}` ·
  `dataExtraction/website/SeleniumPageSource` (Selenium adapter: login, cookie banner, pagination;
  implements `PageSource`) · `PageContents` (DTO) · `parser/{EntityParser,EntryFactory}` (text ▶
  `Entry`, regex from `Constants`; `ApplicationFactory` builds the `EntityParser` around the `Logger` bean and injects it into the extractor). Coordinator `EvergoreDataExtractor` (delta filter) lives in
  `application`.
- **Persistence:** four adapters implementing the businessLogic and application ports, all built on
  one database:
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
    - a transaction takes the write lock when it begins (`BEGIN IMMEDIATE`), so under another
      writer's lock it waits up to the same bound before its work starts;
      - why: a deferred transaction that reads before it writes, such as the recompute's batch,
        fails at once there (SQLite answers `SQLITE_BUSY` without waiting);
    - only writes open a transaction;
    - the scheduled job and the login are the two writers in-process:
      - the login writes the last login, and after a reset the new password's hash, each in one
        short transaction of its own, every hash and check done before it begins;
      - while the job's recompute batch or a ledger batch holds the write lock, that transaction
        waits up to the 10 s bound and the login's answer waits with it;
      - a batch longer than the bound fails the login with a 500, and the member tries again;
      - the job's own transactions likewise wait behind a login's, which lasts one update.
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
    - an empty list opens no transaction, so a ledger write with nothing new needs no write lock;
    - a batch that fails stores none of its rows, whether a constraint refuses one, its commit is
      refused or SQLite rolls it back itself;
    - a row a constraint refuses fails its whole batch every run until it leaves the game's 30-day
      window;
    - rows of that batch older than it that leave the window meanwhile are lost.
  - **A ledger page is read in a total order** (decision 2026-10-05):
    - the chosen column in the chosen direction, then the instant newest first, then the row `id`
      ascending;
      - why: a page boundary inside a run of ties neither repeats a row nor drops one;
    - the instant is left out when the time is the chosen column;
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
  - `database/account/*` implements the port `application/AccountRepository` over the `account`
    table (migration `V5`), one row per avatar the ledgers name:
    - columns: `id`, `avatar` (as the ledgers write it), `name_key` (the folded login name,
      unique), `password_hash`, `password_state` (`ISSUED` or `CHOSEN`), `role` (`MEMBER` or
      `ADMIN`, default `MEMBER`) and `last_login_epoch_millis`;
    - the port holds what the login needs: find by name key, add if absent, replace the password
      with its state, record the last login;
    - every write is one `SqliteDatabase` transaction, a read stays on the pooled connection;
    - `last_login_epoch_millis` is the one nullable column (handbook §3, strict data): the entity
      holds it as a `Long` and the repository answers an `Optional<Instant>`, so a missing login
      never reads as the epoch;
    - instants are UTC epoch milliseconds, as the ledgers' instant column.
- **Business logic (framework-free):** ports `BankRepository` and `StorageRepository` (each the
  generic `base/LedgerRepository` over its entry record, declaring nothing of its own),
  `MetaInformationRepository` · records `BankEntry`, `StorageEntry`, `MetaInformation` ·
  `TransferType` + visitor · `MetaInformationKey` (typed: `DateTimeKey`/`InstantKey`/`LongKey`/
  `DoubleKey`) · `contribution/{Contribution,AvatarContribution,AvatarContributions}` (the four
  ledger sums, their net and the guild total, assembled per known avatar) ·
  `account/{PasswordRule,PasswordRefusal}` (what a chosen password must hold, judged in the NFC form
  of `account/NormalForm`, where a special character is a punctuation or symbol code point, and why
  a new one is refused: it breaks the rule, or it is the one it replaces) · `account/{Account,Role,PasswordState}`
  (an avatar's account: its password hash and state, its role, its last login) · `account/Login`
  (what a signed login says: the account name, role, generation, its `account/LoginSource`, id and
  its two instants) · `account/LoginName` (a typed name
  matches an avatar's regardless of capitals, folded with `Locale.ROOT`, so the JVM's default locale
  never changes the key; the matching rule: [build-run-deploy.md](build-run-deploy.md), "The login
  API") · `Constants`.
- **Last activity comes from the ledgers, not from the meta store** (decision 2026-09-02):
  - both ledger ports answer `latestTimestampPerAvatar()` with **one grouped query** per ledger
    (`MAX(timeStampEpochMillis) GROUP BY avatar`), so the overview materializes one row per avatar instead of
    one per ledger entry;
  - both ledgers carry an index on `(avatar, timeStampEpochMillis)` (migration `V4`), which serves this query
    as a covering index and the per-avatar page and count as an index search, so none of the three
    scans the table;
  - `contribution/ActivityLine` draws the overview's 30-day line from those two maps (the newest
    activity of any avatar, less 30 calendar days on the Berlin clock, `Constants.APP_ZONE`, so a
    change of summer time moves no one across the line) and answers per avatar whether it is active; the overview
    serves the answer as `active`;
  - the domain types are real instants, so nothing here reintroduces
    `MetaInformationKey.DateTimeKey`'s ambiguity and the ledger times stay out of the meta store;
  - a ledger row stores its time twice, whatever the JVM's zone:
    - `timeStampEpochMillis`, the instant in milliseconds since the epoch;
    - `timeStamp`, the same moment as text in UTC with its milliseconds padded to six digits
      (`UtcDateStringType`);
      - why: a release before `V4` reads and writes that text, so it can still run on the file;
    - every read, the ingest window, the sort, the latest movement and the entry itself, takes the
      instant;
    - the text's `(avatar, timeStamp)` index (migration `V3`) stays for a release before `V4`.
- **The service runs in any time zone:**
  - no code reads the JVM's default zone;
  - the meta store's minute is in `Constants.APP_ZONE`, beside its instant;
  - the game's own times are read in `Constants.APP_ZONE` too.
- **Domain (framework-free):** `Entry`, `Item`, `EvergoreItem` (catalog + value math).
- **REST** (what each endpoint answers: [build-run-deploy.md](build-run-deploy.md), "HTTP
  endpoints"; the JSON contract: [frontend.md](frontend.md)):
  - `rest/controller/api/*` is the JSON API under `/api/v1`: `AvatarSummariesController`,
    `AvatarEntriesController`, `AdminStatusController`, `LoginController`;
  - `AdminStatusController` keeps the operator's facts apart from the token-protected member-facing
    overview;
  - `rest/controller/api/wire/*` holds the published contract types plus `TransferTypeWireNames`;
  - `FaviconController` serves the favicon;
  - `LoginPageController` serves the login page's own bundle at `/login`;
  - `SpaHistoryFallbackController` serves the SPA shell for unknown navigation paths;
  - `SpaNavigationPaths` decides which 404s it may answer;
  - the filter `RequestAuditLogFilter` writes one `info` line per request;
  - the filter `RateLimitFilter` throttles per IP on counters `RateLimitCounters` holds;
  - the filter `UnreadableRequestFilter` answers the stand-in request of a head the server cannot
    read;
  - the filter `TokenValidationFilter` checks `?token=`;
  - the filter `LoginBodyLimitFilter` answers 413 to a body the login's paths do not take (what
    it refuses: [build-run-deploy.md](build-run-deploy.md), "The login API");
  - `ApplicationExceptionHandler` dispatches via the `TransferType`/exception visitors, no
    `instanceof`;
  - `BareErrorResponseProcessor` replaces Micronaut's `ErrorResponseProcessor`, so no error
    answer carries a body, whichever exception or framework check raised it (the answers:
    [build-run-deploy.md](build-run-deploy.md), "The login API"); a 5xx logs its cause at error
    on the server, a 400 logs a warning, and `UndecodableBodyHandler` answers a body its declared
    encoding does not decode with a quiet 400;
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
- **Password hashing:** the port `application/PasswordHasher` (`hash`, `verify`) with the adapter
  `helper/security/Argon2idPasswordHasher` (Bouncy Castle):
  - a random 16-byte salt per hash from `SecureRandom`, a 32-byte hash;
  - the encoded form is `$argon2id$v=19$m=…,t=…,p=…$<salt>$<hash>`, so the parameters travel with
    the hash and a later change of `evergore.password-hash.*` still verifies the old ones;
  - the hashes are compared with `MessageDigest.isEqual`;
  - a value that is not in that form verifies nothing;
  - the password is brought to NFC (`businessLogic/account/NormalForm`) before it is hashed or
    verified, so a decomposed and a composed umlaut are one password;
  - a `Semaphore` of `evergore.password-hash.max-concurrent` permits (default 2) wraps each Argon2id
    run, which allocates about 64 MiB: a further check waits and is never refused.
- **Accounts for the members:** `application/CreateAccounts` gives every avatar `KnownAvatars` names
  an account that begins with a reset, on the job's own thread:
  - the password comes from the port `application/PasswordGenerator`
    (`helper/security/RandomPasswordGenerator`: 16 characters from `SecureRandom`, always passing
    `PasswordRule`);
  - only its hash is stored, with the state `ISSUED`;
  - the clear password goes to the port `application/IssuedPasswords`, whose production bean
    discards it until the password delivery exists;
  - an avatar that already has an account, whatever the capitals, is left as it is and is issued
    nothing;
  - an avatar whose folded name is `admin` gets none, and the service logs
    `No account for the avatar <Avatar>: the name admin logs in with the admin key.`;
  - an avatar whose hash or insert throws is logged with its name at error and the other avatars
    still get their accounts.
- **Signed logins:** the port `application/SignedLogins` (`issue(name, role, generation)` for an
  account, `issueForTheAdminKey(name)` for the admin key, `read(value)` answering an
  `Optional<Login>`) with the adapter `helper/security/JwtLogins` (Auth0 java-jwt):
  - the value is a JWT signed with HMAC-SHA256;
  - the key is 32 bytes `ApplicationFactory` draws from `SecureRandom` at every start;
  - a restart ends every login, because the key is gone;
  - the key is never configured and never logged;
  - the claims are the name (`sub`), `role`, `gen` (the generation), `src` (`ACCOUNT` or
    `ADMIN_KEY`, whether an account's password or the admin key made the login), a unique `jti`,
    and `iat` and `exp` from the injected `Clock`, with `exp` exactly 7 days after `iat`;
  - the admin key's login carries the name `admin`, the role `ADMIN` and the generation 0;
  - the verifier pins the algorithm and refuses a value signed with another key, with another
    algorithm or with none, one whose claims were altered, one at or after its `exp`, and anything
    that is no JWT, each as an empty answer rather than an exception;
  - the verifier does not check `iat` against the clock, so a step back of the clock logs no one
    out, and `exp` still bounds every login;
  - a value signed with the right key whose `role`, `src` or `gen` claim is of the wrong kind, or
    names no role or no source, is refused the same way: the read checks each claim, it catches no
    `NullPointerException`.
- **Logging in:** `application/LogIn` checks a name and a password, `application/ChooseNewPassword`
  takes the new password after a reset (what the endpoints answer:
  [build-run-deploy.md](build-run-deploy.md), "The login API"):
  - both check the password through `application/PasswordCheck`, which looks the account up by the
    folded name key (`LoginName`) and runs exactly one Argon2id check;
  - an unknown name is checked against a hash of no password, drawn once at the start, so it costs
    the same check as a wrong password;
  - `application/LoginStart` records the last login with the `Clock`'s instant and issues a signed
    login with the account's name and role and the generation `application/LoginGenerations` holds
    for it, an in-memory count per account that starts at 0;
  - `LogIn` and `ChooseNewPassword` read that generation before the password check and sign with
    it, so a login that a change of the password overtakes during its check carries the old
    generation and ends with that change;
  - `LogIn` starts a login for a right chosen password; for a right issued password it starts none
    and asks for a new password;
  - `ChooseNewPassword` checks, in this order, that the account exists, holds an issued password
    and that this password is right, then `PasswordRule`; only then it hashes the new password,
    replaces the hash with the state `CHOSEN` and starts a login;
  - the replacing is one update that holds only `WHERE` the state is still `ISSUED`
    (`AccountRepository.replaceIssuedPassword`): of two requests that carry the issued password at
    once, one wins and the other gets the answer of a wrong password and no login;
  - a refused choice changes nothing, so the issued password stays valid;
  - every hash and password check runs before the transactions, never inside them;
  - a name that folds to `admin` is the admin key's (`application/AdminKey`), never an account's:
    - `LogIn` starts the key's login only for the exact key;
    - the key is compared with `MessageDigest.isEqual`;
    - the key's login records no last login;
    - `ChooseNewPassword` finds no account under that name and refuses.
- **The admin key:** `application/AdminKey` is drawn by `ApplicationFactory` from `SecureRandom`:
  - `helper/security/AdminKeyAnnouncement` writes it on the `StartupEvent` through `Logger`;
  - its format, the log line and its lifetime: [build-run-deploy.md](build-run-deploy.md), "The
    console admin key".
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
