# 04 — Architecture

Java 17 · Micronaut 3.8.4 (Netty) · ORMLite + SQLite · Selenium · OpenAPI/Swagger.
Root package `dev.schoenberg.evergore.protocolParser` (`…` below).

## Data-flow (the one use case that matters)

```
@Scheduled (24h)  EvergoreDataCollectorJob
        │
        ▼
   EvergoreDataExtractor.loadData()
        │  1. PageContentExtractor  ──Selenium──▶  evergore.de (login, paginate bank + Lager protocols)
        │  2. EntityParser.parse → EntryFactory   (raw text ▶ domain Entry list, regex, dedup)
        │  3. map Entry ▶ BankEntry / StorageEntry
        │  4. keep only entries newer than repo.getNewest()
        │  5. persist via BankRepository / StorageRepository
        ▼
   EvergoreDataEvaluator.evaluateData()
        │  per avatar, since last_updated: sum bank + value storage  (TransferType visitors + EvergoreItem)
        │  write results to MetaInformationRepository; advance last_updated
        ▼
   PostCollectionHook   (no-op in prod; test seam)

Independent read path:  HTTP ▶ filters (rate-limit, token) ▶ OverviewController / AvatarController
                        ▶ read MetaInformation / repositories ▶ OutputFormatter ▶ HTML
```

## Layers & responsibilities (condensed)

- **Entry/lifecycle:** `Application` (boots Micronaut) · `ApplicationFactory` (`@Factory`,
  the **composition root** — builds the un-annotated repositories + `FileLoader` + no-op hooks) ·
  `EvergoreDataCollectorJob` (`@Scheduled`) · `DatabaseStartupInitialization` (creates tables on startup).
- **Extraction pipeline:** `helper/selenium/{Browser,Driver,FileLoader}` ·
  `dataExtraction/website/PageContentExtractor` (the Selenium adapter: login, cookie banner,
  pagination) · `PageContents` (DTO) · `EvergoreDataExtractor` (coordinator + delta filter) ·
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
  `TransferTypeControllerVisitor` (enum ▶ UI string) · filters `BrowserLoggingFilter` (per-IP rate
  limit) + `TokenValidationFilter` (`?token=`) · `ApplicationExceptionHandler`.
- **Cross-cutting:** `Logger` (own interface) + `helper/logger/Slf4jLogger` ·
  `helper/exceptionWrapper/*` (`silentThrow`) · `helper/fileLoader/*` (disc→resource→fallback) ·
  `helper/config/Configuration`.

## What's GOOD (keep this)

- **Domain & businessLogic are framework-free.** The `domain` package imports nothing from
  `database`, `micronaut`, `selenium`, or `ormlite`. Only ~16 files touch the framework, all at the edges.
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
| **Page source (scrape raw protocol)** | ❌ **MISSING** — `PageContentExtractor` *is* the Selenium adapter with no interface in front of it; `EvergoreDataExtractor` depends on the concrete class, which self-`new`s `Driver`/`WebDriver`. |
| Output / presentation | 🟡 Partial (`OutputFormatter`), emits HTML directly; a `FileWriter` port exists but is dead. |

### Top violations to fix (detail in [../backlog.md](../backlog.md))

1. **No page-source port** → the whole scrape→store use case can only run against a real browser
   and the live site. Biggest structural problem.
2. **`Configuration` is config in name only** — values are hard-coded Java fields (browser, server,
   paths, in-memory toggle); ignores `application.yml`/env.
3. **Secrets in source/image** — `TokenValidationFilter` hard-codes `"secret_token"`; Evergore
   credentials live in `zugang.txt` baked into the Docker image.
4. **Duplicated mapping** — `TransferType`→German-string exists in *two* visitors (DB + controller);
   `ApplicationExceptionHandler` uses an `instanceof` chain (its own `// TODO: Visitor-Pattern`).
5. **Dead code at the root** — `CsvParser` (imports Guava, which is **not** a declared dependency, so
   it would not compile if referenced), `FileWriter`, `helper/fileWriter/DiskFileWriter`. No inbound refs.
6. **Sentinel returns** — `getNewest()` fabricates a `…DatabaseEntry(MIN_VALUE)` instead of `Optional`.

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
