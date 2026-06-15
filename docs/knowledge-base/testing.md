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

## Coverage map

**Has tests:** `EvergoreItem` (value math, 3 of ~600 entries) · `MetaInformation` (serialization) ·
`EntryFactory` (dedup size only) · `EvergoreDataEvaluator` (bank aggregation, storage valuation, unknown item fallback, value accumulation, avatar union, watermark) ·
and *indirectly* via `SmokeTest`: controllers, filters, repositories, visitors, the job, `OutputFormatter`.

**Most important UNTESTED logic:**
1. **`EvergoreDataExtractor`** — the "only entries newer than newest stored" delta logic. Mocked out in smoke.
2. **`EntryFactory` / `EntityParser`** — date/avatar/type/quality regex parsing, `Entnahme` branch,
   merged-quantity value, `Impressum` terminator. Only dedup-size is asserted.
3. **`PageContentExtractor`** — Selenium scraping/pagination/login (inherently hard; needs the page-source port to fake).
4. **Repositories** — `getNewest()` SQL, paging, `getAllFor(avatar, after)`. Only incidental smoke coverage.

## Testing direction for the rebuild (TDD/BDD)

- **BDD (PO perspective):** capture the use cases as scenarios — e.g. *"Given a member deposited
  N gold and crafted items worth M, when I view the overview, then their guild value is N+M."*
  Once the **page-source port** exists, the whole collect→evaluate→overview flow can be an
  acceptance test with in-memory fakes (no Selenium, no SQLite file).
- Replace `SmokeTest`'s brittle Levenshtein HTML assertions with structured assertions once the
  output path is behind a cleaner port.
