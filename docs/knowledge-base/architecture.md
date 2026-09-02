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
   EvergoreDataEvaluator.evaluateData()
        │  per avatar, full recompute from ALL stored entries (no cutoff): sum bank + value storage
        │  (TransferType visitor + EvergoreItem); overwrites MetaInformationRepository
        │  last_updated (display-only) written once, after every avatar succeeds
        ▼
   LastRunStatus.recordSuccessfulRun(clock.instant()) + recordUnknownItems(...)   (monitoring seam)
        ▼
   PostCollectionHook   (no-op in prod; test seam)

Independent read path:  HTTP ▶ filters (audit log, rate limit, token) ▶ AvatarSummariesController /
                        AvatarEntriesController ▶ read MetaInformation / repositories
                        ▶ wire records ▶ JSON

Monitoring read path:   GET /health  (token-exempt, anonymous) ▶ Micronaut management
                        ▶ LastRunHealthIndicator ▶ reports UNKNOWN (no run yet) or UP + lastSuccessfulRun
                        timestamp + unknownItemCount/unknownItemNames when the last run hit unknown items
```

## Layers & responsibilities (condensed)

- **Entry/lifecycle:** `Application` (boots Micronaut) · `ApplicationFactory` (`@Factory` composition
  root: builds the un-annotated repositories, the framework-free `application` use-cases,
  `FileLoader`, no-op hooks) · `EvergoreDataCollectorJob` (`@Scheduled`); each `Repository` creates
  its own table lazily, on first use (`ensureTable()`), rather than at a dedicated startup step.
- **Application use-cases (framework-free):** `application/{EvergoreDataExtractor,EvergoreDataEvaluator}`
  (collect + evaluate coordinators) · `application/LastRunStatus` (monitoring seam: records the
  `Instant` of the last successful run). Plain objects, wired in `ApplicationFactory`.
- **Extraction pipeline:** `helper/selenium/{Browser,Driver,FileLoader}` ·
  `dataExtraction/website/SeleniumPageSource` (Selenium adapter: login, cookie banner, pagination;
  implements `PageSource`) · `PageContents` (DTO) · `parser/{EntityParser,EntryFactory}` (text ▶
  `Entry`, regex from `Constants`). Coordinator `EvergoreDataExtractor` (delta filter) lives in
  `application`.
- **Persistence:** `database/Repository` (ORMLite base) · `database/{bank,storage,metaInformation}/*`
  (adapters implementing the businessLogic ports) · `database/TransferTypeDatabaseVisitor`
  (enum ⇄ German DB strings "Einlagerung"/"Entnahme").
- **Business logic (framework-free):** ports `BankRepository`, `StorageRepository`,
  `MetaInformationRepository` · records `BankEntry`, `StorageEntry`, `MetaInformation` ·
  `TransferType` + visitor · `MetaInformationKey` (typed: `DateTimeKey`/`LongKey`/`DoubleKey`) ·
  `contribution/{Contribution,AvatarContribution,AvatarContributions}` (the four ledger sums, their
  net and the guild total, assembled per known avatar) · `Constants`.
- **Last activity comes from the ledgers, not from the meta store** (decision 2026-09-02): both
  ledger ports answer `latestTimestampPerAvatar()` with **one grouped query** per ledger
  (`MAX(timeStamp) GROUP BY avatar`), so the overview materializes one row per avatar instead of one
  per ledger entry. SQLite still scans the table for it: neither `avatar` nor `timeStamp` is indexed,
  and adding an index is DDL that waits on the migration framework (**D10**), so this is the
  remaining scaling ceiling of the read path. The domain types are real instants, so nothing here reintroduces
  `MetaInformationKey.DateTimeKey`'s ambiguity and no key family joins the pending schema migration.
  The ledger's **storage** format is a separate matter: it persists wall-clock text, so these
  columns inherit **D14**'s DST fall-back defect until the epoch/UTC format lands, and today only
  the container's UTC default keeps them right.
- **Domain (framework-free):** `Entry`, `Item`, `EvergoreItem` (catalog + value math).
- **REST:** `controller/api/*` (the JSON API under `/api/v1`: `AvatarSummariesController`,
  `AvatarEntriesController`, and `controller/api/wire/*` holding the published contract types plus
  `TransferTypeWireNames`; contract in
  [frontend.md](frontend.md)) · `FaviconController` ·
  `SpaHistoryFallbackController` (serves the SPA shell for unknown navigation paths;
  `SpaNavigationPaths` decides which 404s it may answer) · filters `RequestAuditLogFilter` (one
  `info` line per request) + `RateLimitFilter` (per-IP counters, held by `RateLimitCounters`) +
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
  exposed at `GET /health` via `micronaut-management`; UNKNOWN before the first run, then UP +
  `lastSuccessfulRun` detail). Fed by `application/LastRunStatus` (above).
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

1. **`Configuration` is config in name only:** hard-coded Java fields (browser, server, paths,
   in-memory toggle); ignores `application.yml`/env. The secrets are the exception and are already
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
