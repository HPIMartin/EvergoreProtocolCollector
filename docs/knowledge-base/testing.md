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
| `EvergoreDataExtractorTest` | Two unit tests: parsed bank entries are persisted via repository, parsed storage entries are persisted via repository. `FakePageSource` returns canned `PageContents`; capturing extensions of `BankRepositoryStub`/`StorageRepositoryStub` record `add()` calls and return `Instant.MIN` sentinel from `getNewest()`. No browser, no framework. | pure unit |
| `ProtocolEvaluationAcceptanceTest` | End-to-end: copies the committed synthetic fixture DB (`testdata.sqlite`) to a `build/` working copy, boots the real Micronaut `EmbeddedServer` against it, stubs the scraper (`loadData()` no-op) while the **real** `EvergoreDataEvaluator` runs via the scheduled job, then asserts `/overview` bank totals and `/avatars/{a}/bank|storage` rows through `RenderedTable`, plus storage **valuation** at the `MetaInformationRepository` bean level (no endpoint surfaces it yet — Epic E1). | `@MicronautTest` acceptance / e2e |
| `RenderedTable` | Parses rendered HTML tables into a header + rows of cell text, tolerant to attributes/styling/wrapper tags, so UI restyling never breaks assertions. The robust successor to `TestHelper`'s Levenshtein matching; **all** markup coupling lives here alone. | helper (no `@Test`) |
| `TestDataGenerator` | Run-on-demand writer (`./gradlew generateAcceptanceDb`) of the committed synthetic fixture `testdata.sqlite` — 3 avatars; bank in both directions; storage with quality scaling and a zero-value item. Item names reference `EvergoreItem.*.ingameName`, so values stay derived, not invented. | fixture generator (`main`) |

## Coverage map

**Has tests:** `EvergoreItem` (value math, 3 of ~600 entries) · `MetaInformation` (serialization) ·
`EntryFactory` (dedup size only) · `EvergoreDataEvaluator` (bank aggregation, storage valuation, unknown item fallback, value accumulation, avatar union, watermark) ·
`EvergoreDataExtractor` (parse→persist pipeline with a fake `PageSource`, no browser) ·
the **evaluate→overview pipeline end-to-end** via `ProtocolEvaluationAcceptanceTest` (real evaluator + real DB + HTTP) ·
and *indirectly* via `SmokeTest`: controllers, filters, repositories, visitors, the job, `OutputFormatter`.

**Most important UNTESTED logic:**
1. **`EvergoreDataExtractor` delta filter** — the "only entries newer than newest stored" branch (test uses `Instant.MIN` sentinel so all entries pass through).
2. **`EntryFactory` / `EntityParser`** — date/avatar/type/quality regex parsing, `Entnahme` branch,
   merged-quantity value, `Impressum` terminator. Only dedup-size is asserted.
3. **`SeleniumPageSource`** — Selenium scraping/pagination/login (inherently hard; page-source port now exists, but the Selenium path itself is not unit-tested).
4. **Repositories** — `getNewest()` SQL, paging, `getAllFor(avatar, after)`. Only incidental smoke coverage.

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
