# 04 — Architecture

Java · Micronaut (Netty) · ORMLite + SQLite · Selenium · OpenAPI/Swagger — exact versions live in
[`build.gradle.kts`](../../build.gradle.kts), the single source of truth (this prose must not duplicate them).
Root package `dev.schoenberg.evergore.protocolParser` (`…` below).

## Data-flow (the one use case that matters)

```
@Scheduled (24h)  EvergoreDataCollectorJob
        │
        ▼
   EvergoreDataExtractor.loadData()
        │  1. PageSource (port) → SeleniumPageSource (Selenium adapter)  ──Selenium──▶  evergore.de (login, paginate bank + Lager protocols)
        │  2. EntityParser.parse → EntryFactory   (raw text ▶ domain Entry list, regex, dedup)
        │  3. map Entry ▶ BankEntry / StorageEntry
        │  4. keep only entries newer than repo.getNewest()
        │  5. persist via BankRepository / StorageRepository
        ▼
   EvergoreDataEvaluator.evaluateData()
        │  per avatar, since last_updated: sum bank + value storage  (TransferType visitor + EvergoreItem)
        │  write results to MetaInformationRepository; advance last_updated
        ▼
   LastRunStatus.recordSuccessfulRun(clock.instant())   (monitoring seam)
        ▼
   PostCollectionHook   (no-op in prod; test seam)

Independent read path:  HTTP ▶ filters (rate-limit, token) ▶ OverviewController / AvatarController
                        ▶ read MetaInformation / repositories ▶ OutputFormatter ▶ HTML

Monitoring read path:   GET /health  (token-exempt, anonymous) ▶ Micronaut management
                        ▶ LastRunHealthIndicator ▶ reports UNKNOWN (no run yet) or UP + lastSuccessfulRun timestamp
```

## Layers & responsibilities (condensed)

- **Entry/lifecycle:** `Application` (boots Micronaut) · `ApplicationFactory` (`@Factory`,
  the **composition root** — builds the un-annotated repositories + `FileLoader` + no-op hooks) ·
  `EvergoreDataCollectorJob` (`@Scheduled`) · `DatabaseStartupInitialization` (creates tables on startup).
- **Extraction pipeline:** `helper/selenium/{Browser,Driver,FileLoader}` ·
  `dataExtraction/website/SeleniumPageSource` (the Selenium adapter: login, cookie banner,
  pagination; implements `PageSource`) · `PageContents` (DTO) · `EvergoreDataExtractor` (coordinator + delta filter) ·
  `parser/{EntityParser,EntryFactory}` (text ▶ `Entry`, regex from `Constants`).
- **Persistence:** `database/Repository` (ORMLite base) · `database/{bank,storage,metaInformation}/*`
  (the adapters that implement the businessLogic ports) · `database/TransferTypeDatabaseVisitor`
  (enum ⇄ German DB strings "Einlagerung"/"Entnahme").
- **Business logic (framework-free):** ports `BankRepository`, `StorageRepository`,
  `MetaInformationRepository` · value objects `BankEntry`, `StorageEntry` · `TransferType` + visitor ·
  `MetaInformation` / `MetaInformationKey` (typed keys: `DateTimeKey`/`LongKey`/`DoubleKey`) · `Constants`.
- **Domain (framework-free):** `Entry`, `Item`, `EvergoreItem` (catalog + value math).
- **REST:** `OverviewController` (`/overview`) · `AvatarController` (`/avatars/{a}/bank|storage`) ·
  `FaviconController` · `OutputFormatter` (HTML table builder, escapes cells) ·
  filters `BrowserLoggingFilter` (per-IP rate
  limit) + `TokenValidationFilter` (`?token=`) · `ApplicationExceptionHandler` (dispatches via the
  `TransferType`/exception visitors, no `instanceof`). `TokenValidationFilter`
  exempts `/favicon.ico` and `/health` (exact) + `/health/*` (sub-paths) — using exact match, NOT a
  broad prefix, so `/healthz` and similar paths remain protected.
- **Monitoring:** `monitoring/LastRunStatus` (`@Singleton`, records the `Instant` of the last
  successful collection) · `monitoring/LastRunHealthIndicator` (implements `HealthIndicator`, exposed
  at `GET /health` via `micronaut-management`; returns UNKNOWN before first run, UP + `lastSuccessfulRun`
  detail after).
- **Cross-cutting:** `Logger` (own interface) + `helper/logger/Slf4jLogger` ·
  `helper/exceptionWrapper/*` (`silentThrow`) · `helper/fileLoader/*` (disc→resource→fallback) ·
  `helper/config/Configuration`.

## What's GOOD (keep this)

- **Domain & businessLogic are framework-free — now mechanically enforced.** The `domain` and
  `businessLogic` packages import nothing from `micronaut`, `selenium`, `ormlite`, `jakarta`, or
  `netty`; only ~16 files touch the framework, all at the edges. `HexagonalArchitectureTest`
  (ArchUnit) fails the build on any violation, so this invariant is a build gate, not just a convention.
- **Persistence is correctly inverted.** `businessLogic` defines the repository *interfaces*;
  `database/*` implements them; application/REST code depends only on the interfaces. The
  `ApplicationFactory` binds interface→impl. This is a genuine Dependency-Inversion seam.
- **`FileLoader`, `Logger` are ports with adapters.** `Pre/PostCollectionHook` are deliberate test seams.
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

1. **`Configuration` is config in name only** — values are hard-coded Java fields (browser, server,
   paths, in-memory toggle); ignores `application.yml`/env.
2. **Secrets in source/image** — the API token is now env-injected (`evergore.security.api-token` via
   `SecurityConfiguration`, required at startup), but Evergore credentials still live in `zugang.txt`
   baked into the Docker image.
3. **Application use-cases carry framework annotations** — `EvergoreDataExtractor`,
   `EvergoreDataEvaluator`, and `LastRunStatus` are `@Singleton` and sit next to the adapters, so the
   "application layer is framework-free" goal isn't met yet; the ArchUnit guard covers only `domain`/
   `businessLogic`, not the use-case package or framework-annotation creep.

## Target structure (proposed, hexagonal)

```
domain/            EvergoreItem, Entry, Item, TransferType, value objects   (no framework)
application/       use cases: CollectGuildData, EvaluateContributions, query services + PORT interfaces
adapters/in/       rest controllers/filters, the scheduled job
adapters/out/      selenium (PageSource impl), persistence (ORMLite repos), file, logging
config/            Micronaut @Factory wiring + @ConfigurationProperties
```
This keeps the already-clean core, names the missing PageSource port, and pushes Micronaut to
`adapters` + `config` only.
