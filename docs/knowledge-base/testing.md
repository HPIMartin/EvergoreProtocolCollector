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

## Coverage map

**Has tests:** `EvergoreItem` (value math, 3 of ~600 entries) · `MetaInformation` (serialization) ·
`EntryFactory` (dedup size only) · `EvergoreDataEvaluator` (bank aggregation, storage valuation, unknown item fallback, value accumulation, avatar union, watermark) ·
`EvergoreDataExtractor` (parse→persist pipeline with a fake `PageSource`, no browser) ·
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

An automated, offline version of the 1:1 check (boot the app against a stored prod snapshot, assert
the rendered pages) is the natural form of backlog **D2** — its fixtures are guild members' data
(PII) and must stay **gitignored**, never committed.

## Testing direction for the rebuild (TDD/BDD)

- **BDD (PO perspective):** capture the use cases as scenarios — e.g. *"Given a member deposited
  N gold and crafted items worth M, when I view the overview, then their guild value is N+M."*
  Once the **page-source port** exists, the whole collect→evaluate→overview flow can be an
  acceptance test with in-memory fakes (no Selenium, no SQLite file).
- Replace `SmokeTest`'s brittle Levenshtein HTML assertions with structured assertions once the
  output path is behind a cleaner port.
