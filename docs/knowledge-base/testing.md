# 06: Testing

## Inventory

| Test | Scope | Style |
|------|-------|-------|
| `SmokeTest` | Boots the real Micronaut `EmbeddedServer`; mocks Selenium (`TestEvergoreDataExtractor extends EvergoreDataExtractor` with `super(null,…)`); points DB at `src/test/resources/smokeTest.sqlite`; sets job delay 0; asserts app starts + the job runs, `/overview` & `/avatars/{a}/bank|storage` return seeded HTML (fuzzy via Levenshtein), unknown path → 4xx. `@MockBean` for `Configuration`, the hooks, task-exception-handler. | `@MicronautTest` integration / smoke |
| `TestHelper` | Levenshtein "closest line" helper for fuzzy HTML assertions. | helper (no `@Test`) |
| `MetaInformationTest` | `MetaInformation<T>` delegates serialize/deserialize to its `MetaInformationKey<T>`. | pure unit |
| `EntryFactoryTest` | One test `deduplicates()`: 3 raw lines collapse to 1 item by name+quality. | pure unit (thin) |
| `EvergoreItemTest` | `getStorageValue()`/`getWithdrawlValue()` for 3 cases (raw, craftable, gem). | pure unit |
| `HexagonalArchitectureTest` | ArchUnit guard: `domain`+`businessLogic` depend on **no** framework/library packages (Micronaut, jakarta, Selenium, ORMLite/SQLite, Jackson, RxJava, Apache Commons, logback/SLF4J, Netty). Turns the hexagonal golden rule into a build failure (verified non-vacuous: temporarily forbidding `java.time` flags 22 core usages). | architecture guard (ArchUnit + JUnit 5) |
| `EvergoreDataEvaluatorTest` | Unit tests covering: bank aggregation (placement + withdrawl sums), storage valuation (craftable item with quantity and partial quality), unknown item fallback (zero value + WARN + collected into `EvaluationResult`), full recompute (sums start at zero over all stored entries and **overwrite** stale meta values; a second run is idempotent), mid-run-failure self-healing (a failed avatar writes nothing; `last_updated` is display-only, written once after full success), and avatar union across both repos (all keys written per avatar). Hand-written fakes: `FakeMetaInformationRepository`, `BankRepositoryStub`, `StorageRepositoryStub`, `LoggerSpy`. | pure unit |
| `EvergoreDataExtractorTest` | Unit tests covering: parsed bank/storage entries are persisted; a still-visible entry older than the stored max but missing from the database is healed (ingested) via `getAllSince(min scraped timestamp)`; an identical already-stored row (bank and storage) is not duplicated; a scraped identical pair with one already stored ingests only the surplus; two identical scraped rows both survive when neither is stored yet; entries are ingested oldest-first for partial-batch safety. `FakePageSource` returns canned `PageContents`; capturing extensions of `BankRepositoryStub`/`StorageRepositoryStub` assert the `getAllSince` argument (a wrong argument yields an empty result, so a mutant passing the wrong timestamp is caught). No browser, no framework. | pure unit |
| `BankDatabaseRepositoryTest` | Repository is usable without separate init (file DB); `getAllSince(timestamp)` includes the row exactly at the boundary, excludes older rows, includes newer rows (in-memory SQLite). | adapter integration |
| `StorageDatabaseRepositoryTest` | `getAllSince(timestamp)` includes the row exactly at the boundary, excludes older rows, includes newer rows (in-memory SQLite). | adapter integration |
| `ProtocolEvaluationAcceptanceTest` | End-to-end: copies the committed synthetic fixture DB (`testdata.sqlite`) to a `build/` working copy, boots the real Micronaut `EmbeddedServer` against it, stubs the scraper (`loadData()` no-op) while the **real** `EvergoreDataEvaluator` runs via the scheduled job, then asserts `/overview` bank totals and `/avatars/{a}/bank|storage` rows through `RenderedTable`, plus storage **valuation** at the `MetaInformationRepository` bean level (no endpoint surfaces it yet, Epic E1). | `@MicronautTest` acceptance / e2e |
| `RenderedTable` | Parses rendered HTML tables into a header + rows of cell text, tolerant to attributes/styling/wrapper tags, so UI restyling never breaks assertions. The robust successor to `TestHelper`'s Levenshtein matching; **all** markup coupling lives here alone. | helper (no `@Test`) |
| `TestDataGenerator` | Run-on-demand writer (`./gradlew generateAcceptanceDb`) of the committed synthetic fixture `testdata.sqlite`: 3 avatars; bank in both directions; storage with quality scaling and a zero-value item. Item names reference `EvergoreItem.*.ingameName`, so values stay derived, not invented. | fixture generator (`main`) |
| `LastRunStatusTest` | Three pure unit tests: empty before any run; records a specific `Instant` and returns it; second record overwrites the first. No framework. | pure unit |
| `LastRunHealthIndicatorTest` | Two pure unit tests (with framework dep on `micronaut-management`): reports `UNKNOWN` with no detail map before any run; reports `UP` with `lastSuccessfulRun` detail key after a run. Subscribes to the `Publisher` inline via an anonymous `Subscriber`. | pure unit |
| `EvergoreDataCollectorJobTest` | Three unit tests: records `lastSuccessfulRun` after a successful cycle; does **not** record when `loadData` throws; does **not** record when `evaluateData` throws. Uses `Clock.fixed(…)`, `ZeroDelayConfiguration extends Configuration` (delay 0), and local `FailableExtractor`/`FailableEvaluator` inner classes; no static state. | pure unit |
| `HealthEndpointTest` | Boots the real Micronaut `EmbeddedServer`; mocks the extractor (no-op `loadData`), config (zero delay, test DB path), and hooks (BootSignalRecorder pattern). Asserts: `GET /health` returns **exactly 200** without a token; response body contains `lastRun` + `lastSuccessfulRun`; `/overview` without a token is rejected (4xx); `/healthz` is rejected with the same status as `/overview` (exact-match scoping test: ensures the `/health` exemption does not bleed to prefix matches). | `@MicronautTest` integration |
| `TransferTypeTest` | Two pure unit tests locking `TransferType.toGermanString()` for both constants (`EINLAGERUNG`→"Einlagerung", `ENTNAHME`→"Entnahme"), the single source for the enum→German mapping. | pure unit |
| `ApplicationExceptionHandlerTest` | Four unit tests asserting each `ProtocolParserException` subclass maps to its HTTP status via the visitor, plus the `onUnknown` branch. `accept` is `abstract`, so a new exception subclass is a compile error rather than a silent fallback. | pure unit |
| `RateLimitCounterTest` | Three pure unit tests for `RateLimitCounter`: the block lifts deterministically after `block-duration` (injected `Clock`, no `sleep`), stays active before expiry, and 20 concurrent `block()` calls leave consistent state. | pure unit |

## Boot-signal seam

- `SmokeTest`, `ProtocolEvaluationAcceptanceTest`, and `HealthEndpointTest` need state written by a
  context bean at startup (the `@Scheduled` collector finished, data was loaded, an exception
  fired) and read back in the test body.
- They observe it through an injected, DI-shared **`BootSignalRecorder`** (`@Singleton` scope via a
  test `@Factory`), **not** `static` flags: this bridges the `@MockBean`/context lifecycle and the
  JUnit test-instance lifecycle without global mutable state. The mock-bean helpers get the
  recorder by constructor injection and write to it; the test injects the same instance and reads
  it.
- A `@TestInstance(PER_CLASS)` + instance-fields alternative was tried and broke `SmokeTest` (the
  startup signal went unobserved, causing a timeout).
- The recorder owns `awaitCollection()`, deduplicated across the tests. It blocks on a
  `CountDownLatch` with **no timeout**: the test returns exactly when the collection finishes, so
  it is deterministic on any hardware (slow machines just wait longer, they never flake).
  `recordException()` releases the latch too, so a failed boot fails the `exceptionOccurred()`
  assertion instead of hanging forever.
- The acceptance + health tests call it from `@BeforeEach`: it's a shared, non-test-relevant
  precondition, not a per-test arrange (handbook §6). `SmokeTest.applicationIsStarting` is the
  exception: there the collection completing *is* the behaviour under test, so the await stays in
  the test body.

## Coverage map

**Has tests:** `EvergoreItem` (value math, 3 of ~600 entries) · `MetaInformation` (serialization) ·
`EntryFactory` (dedup size only) · `EvergoreDataEvaluator` (bank aggregation, storage valuation, unknown item fallback, full-recompute overwrite + idempotence + failed-run self-heal, avatar union) ·
`EvergoreDataExtractor` (parse→persist pipeline, window dedup via `getAllSince(min scraped timestamp)`: heals a still-visible entry missing from the database, no-duplicate + surplus-only dedup, oldest-first partial-batch safety) ·
`BankDatabaseRepository` / `StorageDatabaseRepository` (`getAllSince` inclusive boundary, in-memory SQLite) ·
the **evaluate→overview pipeline end-to-end** via `ProtocolEvaluationAcceptanceTest` (real evaluator + real DB + HTTP) ·
`LastRunStatus` (record + read) · `LastRunHealthIndicator` (UNKNOWN / UP + detail) ·
`EvergoreDataCollectorJob` (records run on success, not on failure) ·
`/health` endpoint + `TokenValidationFilter` exact-match scoping via `HealthEndpointTest` ·
`TransferType`→German mapping (`toGermanString`) · `ApplicationExceptionHandler` exception→HTTP visitor dispatch ·
`RateLimitCounter` (block thread-safety + deterministic expiry) ·
and *indirectly* via `SmokeTest`: controllers, filters, repositories, the job, `OutputFormatter`.

**Most important UNTESTED logic:**
1. **`EntryFactory` / `EntityParser`**: date/avatar/type/quality regex parsing, `Entnahme` branch,
   merged-quantity value, `Impressum` terminator. Only dedup-size is asserted.
2. **`SeleniumPageSource`**: Selenium scraping/pagination/login (inherently hard; page-source port now exists, but the Selenium path itself is not unit-tested).
3. **Repositories**: paging, `getAllFor(avatar)`. Only incidental smoke coverage (`getAllSince` has adapter tests).

## Migration verification: Gradle / Java 25 / Micronaut 4.10 (2026-06-16)

The build migration (Maven→Gradle, Java 17→25, Micronaut 3.8.4→4.10.3) was held to **identical
observable behaviour**:

- **Unit/integration suite** reproduces the pre-migration baseline exactly: **22 tests, 7 classes, 0
  failures** on the new stack.
- **1:1 against production data:** the migrated distribution was run against a snapshot of the
  production SQLite DB; its `/overview`, `/avatars/{avatar}/bank` and `/avatars/{avatar}/storage`
  responses were **byte-identical (after LF normalisation)** to the live production instance.
- **JDK 25 behaviour change found & fixed:** `java.sql.Timestamp.from(Instant)` now uses
  `Math.multiplyExact` and **throws** on extreme instants where JDK 17 silently wrapped. The
  evaluator's then-existing empty-watermark sentinel (`LocalDateTime.MIN`) hit this on the first
  run and was replaced with an earliest-representable valid instant (the watermark itself is gone
  since the full-recompute rework — avoid extreme sentinel instants in anything converted to
  `Timestamp`). `SmokeTest`'s throwaway `Instant.MIN` timestamp was likewise made a valid instant. `ArchUnit` was bumped to 1.4.1 so it parses Java 25 bytecode (1.3.0 silently
  imported zero classes, making the hexagonal guard a false green).

An automated, offline acceptance test of the same flow now exists as `ProtocolEvaluationAcceptanceTest`,
driven by a **synthetic** committed fixture DB (`TestDataGenerator` → `testdata.sqlite`): no
production data, no PII, so the fixture is safe to commit and the test is fully reproducible. The
optional richer variant (boot against a real prod snapshot) would carry guild members' data (PII)
and must stay **gitignored**, never committed.

### Test isolation (forking)

`build.gradle.kts` runs each test class in a fresh JVM (`forkEvery = 1`). This is a **temporary
workaround, not the intended strategy**: it was added alongside the meta-sums recompute so the
`@MicronautTest` classes that boot the real `@Scheduled` collector job stop interfering across a
shared JVM (a `no such table: metaInformation` failure when a job evaluates before table
initialization). The blanket per-class fork is a blunt fix and costs build time; the suspected root
cause (shared mutable static state — `Configuration.useInMemory`/`DATABASE_TEMP_SQLITE`, a shared
SQLite file, `BootSignalRecorder`) is unconfirmed, and the proper resolution (remove the shared
state and run one JVM, or parallelize, or split a boot-test suite) is tracked in the
build-performance plan (S4) together with the doc/build-agreement item **G16**.

## Testing direction for the rebuild (TDD/BDD)

- **BDD (PO perspective):** capture the use cases as scenarios, e.g. *"Given a member deposited
  N gold and crafted items worth M, when I view the overview, then their guild value is N+M."*
  The whole collect→evaluate→overview flow is now covered by `ProtocolEvaluationAcceptanceTest`
  (scraper stubbed, real evaluation, asserted via HTTP + the meta repo).
- `RenderedTable` provides the structured, restyle-proof HTML assertions that should replace
  `SmokeTest`'s brittle Levenshtein matching (`TestHelper`) when `SmokeTest` is next reworked.
