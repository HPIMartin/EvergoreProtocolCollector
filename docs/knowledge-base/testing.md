# 06 — Testing

## Inventory

| Test | Scope | Style |
|------|-------|-------|
| `SmokeTest` | Boots the real Micronaut `EmbeddedServer`; mocks Selenium (`TestEvergoreDataExtractor extends EvergoreDataExtractor` with `super(null,…)`); points DB at `src/test/resources/smokeTest.sqlite`; sets job delay 0; asserts app starts + the job runs, `/overview` & `/avatars/{a}/bank|storage` return seeded HTML (fuzzy via Levenshtein), unknown path → 4xx. `@MockBean` for `Configuration`, the hooks, task-exception-handler. | `@MicronautTest` integration / smoke |
| `TestHelper` | Levenshtein "closest line" helper for fuzzy HTML assertions. | helper (no `@Test`) |
| `MetaInformationTest` | `MetaInformation<T>` delegates serialize/deserialize to its `MetaInformationKey<T>`. | pure unit |
| `EntryFactoryTest` | One test `deduplicates()` — 3 raw lines collapse to 1 item by name+quality. | pure unit (thin) |
| `EvergoreItemTest` | `getStorageValue()`/`getWithdrawlValue()` for 3 cases (raw, craftable, gem). | pure unit |
| `HexagonalArchitectureTest` | ArchUnit guard: `domain`+`businessLogic` depend on **no** framework/library packages (Micronaut, jakarta, Selenium, ORMLite/SQLite, Jackson, RxJava, Apache Commons, logback/SLF4J, Netty). Turns the hexagonal golden rule into a build failure (verified non-vacuous: temporarily forbidding `java.time` flags 22 core usages). | architecture guard (ArchUnit + JUnit 5) |
| `EvergoreDataEvaluatorTest` | Six unit tests covering: bank aggregation (placement + withdrawl sums), storage valuation (craftable item with quantity and partial quality), unknown item fallback (zero value + logger message), value accumulation onto a previously stored value, avatar union across both repos (all keys written per avatar), and the `last_updated` watermark (old value used as cutoff for both repos, new value written and is after old). Hand-written fakes: `FakeMetaInformationRepository`, `BankRepositoryStub`, `StorageRepositoryStub`, `LoggerSpy`. | pure unit |
| `EvergoreDataExtractorTest` | Four unit tests: parsed bank/storage entries are persisted, entries older than the stored watermark are filtered out, and all entries are kept when no newest entry exists (`Optional.empty()`). `FakePageSource` returns canned `PageContents`; capturing extensions of `BankRepositoryStub`/`StorageRepositoryStub` return `Optional` from `getNewest()`. No browser, no framework. | pure unit |
| `BankDatabaseRepositoryTest` | Three tests: repository is usable without separate init (file DB); `getNewest()` returns `Optional.empty()` on an empty table; `getNewest()` returns the entry with the latest timestamp after inserts (in-memory SQLite). | adapter integration |
| `StorageDatabaseRepositoryTest` | Two tests: `getNewest()` returns `Optional.empty()` on empty; returns the entry with the latest timestamp after inserts (in-memory SQLite). | adapter integration |
| `ProtocolEvaluationAcceptanceTest` | End-to-end: copies the committed synthetic fixture DB (`testdata.sqlite`) to a `build/` working copy, boots the real Micronaut `EmbeddedServer` against it, stubs the scraper (`loadData()` no-op) while the **real** `EvergoreDataEvaluator` runs via the scheduled job, then asserts `/overview` bank totals and `/avatars/{a}/bank|storage` rows through `RenderedTable`, plus storage **valuation** at the `MetaInformationRepository` bean level (no endpoint surfaces it yet — Epic E1). | `@MicronautTest` acceptance / e2e |
| `RenderedTable` | Parses rendered HTML tables into a header + rows of cell text, tolerant to attributes/styling/wrapper tags, so UI restyling never breaks assertions. The robust successor to `TestHelper`'s Levenshtein matching; **all** markup coupling lives here alone. | helper (no `@Test`) |
| `TestDataGenerator` | Run-on-demand writer (`./gradlew generateAcceptanceDb`) of the committed synthetic fixture `testdata.sqlite` — 3 avatars; bank in both directions; storage with quality scaling and a zero-value item. Item names reference `EvergoreItem.*.ingameName`, so values stay derived, not invented. | fixture generator (`main`) |
| `LastRunStatusTest` | Three pure unit tests: empty before any run; records a specific `Instant` and returns it; second record overwrites the first. No framework. | pure unit |
| `LastRunHealthIndicatorTest` | Two pure unit tests (with framework dep on `micronaut-management`): reports `UNKNOWN` with no detail map before any run; reports `UP` with `lastSuccessfulRun` detail key after a run. Subscribes to the `Publisher` inline via an anonymous `Subscriber`. | pure unit |
| `EvergoreDataCollectorJobTest` | Three unit tests: records `lastSuccessfulRun` after a successful cycle; does **not** record when `loadData` throws; does **not** record when `evaluateData` throws. Uses `Clock.fixed(…)`, `ZeroDelayConfiguration extends Configuration` (delay 0), and local `FailableExtractor`/`FailableEvaluator` inner classes — no static state. | pure unit |
| `HealthEndpointTest` | Boots the real Micronaut `EmbeddedServer`; mocks the extractor (no-op `loadData`), config (zero delay, test DB path), and hooks (BootSignalRecorder pattern). Asserts: `GET /health` returns **exactly 200** without a token; response body contains `lastRun` + `lastSuccessfulRun`; `/overview` without a token is rejected (4xx); `/healthz` is rejected with the same status as `/overview` (exact-match scoping test — ensures the `/health` exemption does not bleed to prefix matches). | `@MicronautTest` integration |

## Boot-signal seam

`SmokeTest`, `ProtocolEvaluationAcceptanceTest`, and `HealthEndpointTest` need state written by a context bean at startup —
the `@Scheduled` collector finished, data was loaded, an exception fired — and read back in the test
body. They observe it through an injected, DI-shared **`BootSignalRecorder`** (`@Singleton` scope via a
test `@Factory`), **not** `static` flags. This bridges the `@MockBean`/context lifecycle and the JUnit
test-instance lifecycle without global mutable state: the mock-bean helpers get the recorder by
constructor injection and write to it; the test injects the same instance and reads it. A
`@TestInstance(PER_CLASS)` + instance-fields alternative was tried and broke `SmokeTest` (the startup
signal went unobserved → timeout). The recorder also owns `awaitCollection()`, deduplicated across the
tests. It blocks on a `CountDownLatch` with **no timeout** — the test returns exactly when the collection
finishes, so it is deterministic on any hardware (slow machines just wait longer, they never flake).
`recordException()` releases the latch too, so a failed boot fails the `exceptionOccurred()` assertion
instead of hanging forever. The acceptance + health tests call it from `@BeforeEach` — it's a shared,
non-test-relevant precondition, not a per-test arrange (handbook §6). `SmokeTest.applicationIsStarting`
is the exception: there the collection completing *is* the behaviour under test, so the await stays in
the test body.

## Coverage map

**Has tests:** `EvergoreItem` (value math, 3 of ~600 entries) · `MetaInformation` (serialization) ·
`EntryFactory` (dedup size only) · `EvergoreDataEvaluator` (bank aggregation, storage valuation, unknown item fallback, value accumulation, avatar union, watermark) ·
`EvergoreDataExtractor` (parse→persist pipeline, delta filter including the empty-watermark case) ·
`BankDatabaseRepository` / `StorageDatabaseRepository` (`getNewest()` empty + newest-wins, in-memory SQLite) ·
the **evaluate→overview pipeline end-to-end** via `ProtocolEvaluationAcceptanceTest` (real evaluator + real DB + HTTP) ·
`LastRunStatus` (record + read) · `LastRunHealthIndicator` (UNKNOWN / UP + detail) ·
`EvergoreDataCollectorJob` (records run on success, not on failure) ·
`/health` endpoint + `TokenValidationFilter` exact-match scoping via `HealthEndpointTest` ·
and *indirectly* via `SmokeTest`: controllers, filters, repositories, visitors, the job, `OutputFormatter`.

**Most important UNTESTED logic:**
1. **`EntryFactory` / `EntityParser`** — date/avatar/type/quality regex parsing, `Entnahme` branch,
   merged-quantity value, `Impressum` terminator. Only dedup-size is asserted.
2. **`SeleniumPageSource`** — Selenium scraping/pagination/login (inherently hard; page-source port now exists, but the Selenium path itself is not unit-tested).
3. **Repositories** — paging, `getAllFor(avatar, after)`. Only incidental smoke coverage.

## Migration verification — Gradle / Java 25 / Micronaut 4.10 (2026-06-16)

The build migration (Maven→Gradle, Java 17→25, Micronaut 3.8.4→4.10.3) was held to **identical
observable behaviour**:

- **Unit/integration suite** reproduces the pre-migration baseline exactly: **22 tests, 7 classes, 0
  failures** on the new stack.
- **1:1 against production data:** the migrated distribution was run against a snapshot of the
  production SQLite DB and its `/overview`, `/avatars/{avatar}/bank` and `/avatars/{avatar}/storage`
  responses were **byte-identical (after LF normalisation)** to the live production instance.
- **JDK 25 behaviour change found & fixed:** `java.sql.Timestamp.from(Instant)` now uses
  `Math.multiplyExact` and **throws** on extreme instants where JDK 17 silently wrapped. The empty
  watermark `LocalDateTime.MIN` hit this in `EvergoreDataEvaluator` (first-run / empty-meta path); it
  was replaced with an earliest-representable sentinel (`BEGINNING_OF_TIME`) that preserves the
  include-everything semantics. `SmokeTest`'s throwaway `Instant.MIN` timestamp was likewise made a
  valid instant. `ArchUnit` was bumped to 1.4.1 so it parses Java 25 bytecode (1.3.0 silently
  imported zero classes, making the hexagonal guard a false green).

An automated, offline acceptance test of the same flow now exists as `ProtocolEvaluationAcceptanceTest`,
driven by a **synthetic** committed fixture DB (`TestDataGenerator` → `testdata.sqlite`) — no production
data, no PII, so the fixture is safe to commit and the test is fully reproducible. The optional richer
variant (boot against a real prod snapshot) would carry guild members' data (PII) and must stay
**gitignored**, never committed.

### Test isolation (forking)

`build.gradle.kts` runs each test class in a fresh JVM (`setForkEvery(1)`). Two `@MicronautTest` classes
now boot the embedded server with the real `@Scheduled` collector job; sharing one JVM let one boot's
startup timing perturb the other's, exposing a **latent startup race in `SmokeTest`** — its job
(`initialDelay` 0 in tests) could evaluate before `DatabaseStartupInitialization` created the tables
(`no such table: metaInformation`). Per-class JVM isolation removes the cross-class interference.
`ProtocolEvaluationAcceptanceTest` is immune by construction (its fixture already contains the tables);
the underlying ordering assumption in the production startup (job-vs-table-init) is tracked as a
follow-up, not fixed here.

## Testing direction for the rebuild (TDD/BDD)

- **BDD (PO perspective):** capture the use cases as scenarios — e.g. *"Given a member deposited
  N gold and crafted items worth M, when I view the overview, then their guild value is N+M."*
  The whole collect→evaluate→overview flow is now covered by `ProtocolEvaluationAcceptanceTest`
  (scraper stubbed, real evaluation, asserted via HTTP + the meta repo).
- `RenderedTable` provides the structured, restyle-proof HTML assertions that should replace
  `SmokeTest`'s brittle Levenshtein matching (`TestHelper`) when `SmokeTest` is next reworked.
