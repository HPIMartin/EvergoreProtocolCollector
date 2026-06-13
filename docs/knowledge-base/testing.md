# 06 — Testing

## Inventory

| Test | Scope | Style |
|------|-------|-------|
| `SmokeTest` | Boots the real Micronaut `EmbeddedServer`; mocks Selenium (`TestEvergoreDataExtractor extends EvergoreDataExtractor` with `super(null,…)`); points DB at `src/test/resources/smokeTest.sqlite`; sets job delay 0; asserts app starts + the job runs, `/overview` & `/avatars/{a}/bank|storage` return seeded HTML (fuzzy via Levenshtein), unknown path → 4xx. `@MockBean` for `Configuration`, the hooks, task-exception-handler. | `@MicronautTest` integration / smoke |
| `TestHelper` | Levenshtein "closest line" helper for fuzzy HTML assertions. | helper (no `@Test`) |
| `MetaInformationTest` | `MetaInformation<T>` delegates serialize/deserialize to its `MetaInformationKey<T>`. | pure unit |
| `EntryFactoryTest` | One test `deduplicates()` — 3 raw lines collapse to 1 item by name+quality. | pure unit (thin) |
| `EvergoreItemTest` | `getStorageValue()`/`getWithdrawlValue()` for 3 cases (raw, craftable, gem). | pure unit |
| `EvergoreDataEvaluatorTest` | **Brand-new stub** — one empty `@Test calculatesBankDifferences()`, `@BeforeEach` body commented out. Tests nothing yet. | empty stub |

## Coverage map

**Has tests:** `EvergoreItem` (value math, 3 of ~600 entries) · `MetaInformation` (serialization) ·
`EntryFactory` (dedup size only) · and *indirectly* via `SmokeTest`: controllers, filters,
repositories, visitors, the job, `OutputFormatter`.

**Most important UNTESTED logic:**
1. **`EvergoreDataEvaluator`** — the core aggregation (bank sums, storage value = `value × qty × quality/100`,
   item lookup → `UNDEFINED` fallback, `last_updated` watermark). Only an empty stub exists. **Highest-value gap.**
2. **`EvergoreDataExtractor`** — the "only entries newer than newest stored" delta logic. Mocked out in smoke.
3. **`EntryFactory` / `EntityParser`** — date/avatar/type/quality regex parsing, `Entnahme` branch,
   merged-quantity value, `Impressum` terminator. Only dedup-size is asserted.
4. **`PageContentExtractor`** — Selenium scraping/pagination/login (inherently hard; needs the page-source port to fake).
5. **Repositories** — `getNewest()` SQL, paging, `getAllFor(avatar, after)`. Only incidental smoke coverage.

## Compile-health of the current working tree

Likely **compiles and passes** — but only because the one broken line is commented out:
- Production ctor is `EvergoreDataEvaluator(MetaInformationRepository, StorageRepository, BankRepository, Logger)`.
  The stub's commented `new EvergoreDataEvaluator(meta, storage, bank, Logger.getAnonymousLogger())`
  matches arity/order **but** `Logger.getAnonymousLogger()` is a `java.util.logging` call that does
  **not** exist on this project's custom `Logger` interface — so it must stay commented until rewritten.
- Other test↔production signatures (repo `get(...)` factories, `EvergoreDataExtractor(null,…)`,
  `BankEntry`/`StorageEntry` ctors, `MetaInformationKey` factories) line up.
- Main uncertainty is `SmokeTest`'s integration behavior under the in-flight refactor (it depends on
  controllers/templates/visitors that the refactor touched), not a compile error.

## Testing direction for the rebuild (TDD/BDD)

- **TDD next step:** flesh out `EvergoreDataEvaluatorTest` — fake the three repositories, feed known
  bank + storage entries, assert the computed `MetaInformation` values (this is also the natural place
  to lock in the Gildenmehrwert formula from [02-google-sheet.md](google-sheet.md)).
- **BDD (PO perspective):** capture the use cases as scenarios — e.g. *"Given a member deposited
  N gold and crafted items worth M, when I view the overview, then their guild value is N+M."*
  Once the **page-source port** exists, the whole collect→evaluate→overview flow can be an
  acceptance test with in-memory fakes (no Selenium, no SQLite file).
- Replace `SmokeTest`'s brittle Levenshtein HTML assertions with structured assertions once the
  output path is behind a cleaner port.
