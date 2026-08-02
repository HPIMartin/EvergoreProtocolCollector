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

Independent read path:  HTTP ▶ filters (rate-limit, token) ▶ OverviewController / AvatarController
                        ▶ read MetaInformation / repositories ▶ OutputFormatter ▶ HTML

Monitoring read path:   GET /health  (token-exempt, anonymous) ▶ Micronaut management
                        ▶ LastRunHealthIndicator ▶ reports UNKNOWN (no run yet) or UP + lastSuccessfulRun
                        timestamp + unknownItemCount/unknownItemNames when the last run hit unknown items
```

## Layers & responsibilities (condensed)

- **Entry/lifecycle:** `Application` (boots Micronaut) · `ApplicationFactory` (`@Factory` composition
  root: builds the un-annotated repositories, the framework-free `application` use-cases,
  `FileLoader`, no-op hooks) · `EvergoreDataCollectorJob` (`@Scheduled`) ·
  `DatabaseStartupInitialization` (creates tables at startup).
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
  `Constants`.
- **Domain (framework-free):** `Entry`, `Item`, `EvergoreItem` (catalog + value math).
- **REST:** `OverviewController` (`/overview`) · `AvatarController` (`/avatars/{a}/bank|storage`) ·
  `FaviconController` · `OutputFormatter` (HTML table builder, escapes cells) · filters
  `BrowserLoggingFilter` (per-IP rate limit) + `TokenValidationFilter` (`?token=`; exempts
  `/favicon.ico` and `/health` (exact) + `/health/*`, exact match NOT a broad prefix, so `/healthz`
  and similar stay protected) · `ApplicationExceptionHandler` (dispatches via the
  `TransferType`/exception visitors, no `instanceof`; the mapped status picks the log severity, so
  an expected client error is one `info` line and only a server error logs its stack trace).
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
| Output / presentation | 🟡 Partial (`OutputFormatter`), emits HTML directly. |

### Top violations to fix (detail in [../backlog.md](../backlog.md))

1. **`Configuration` is config in name only:** hard-coded Java fields (browser, server, paths,
   in-memory toggle); ignores `application.yml`/env.
2. **Secrets in source/image:** the API token is env-injected (`evergore.security.api-token` via
   `SecurityConfiguration`, required at startup), but Evergore credentials still live in `zugang.txt`
   baked into the Docker image.

## Target structure (proposed, hexagonal)

```
domain/            EvergoreItem, Entry, Item, TransferType, value objects   (no framework)
application/       use cases: CollectGuildData, EvaluateContributions, query services + PORT interfaces
adapters/in/       rest controllers/filters, the scheduled job
adapters/out/      selenium (PageSource impl), persistence (ORMLite repos), file, logging
config/            Micronaut @Factory wiring + @ConfigurationProperties
```
Keeps the clean core, names the missing PageSource port, pushes Micronaut to `adapters` + `config` only.
