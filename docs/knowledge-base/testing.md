# 06: Testing

## Inventory

| Test | Scope | Style |
|------|-------|-------|
| `SmokeTest` | Boots the real Micronaut `EmbeddedServer`; mocks Selenium (`TestEvergoreDataExtractor extends EvergoreDataExtractor` with `super(null,…)`); points DB at `build/tmp/smokeTest.sqlite` (under the build directory, so rewriting it per run cannot invalidate the `processTestResources` inputs); sets job delay 0; asserts app starts + the job runs, `/overview` & `/avatars/{a}/bank\|storage` return seeded HTML (fuzzy via Levenshtein), unknown path → 4xx, and that the `PageSource` bean resolves to the `SeleniumPageSource` adapter. `@MockBean` for `Configuration`, the hooks, task-exception-handler. | `@MicronautTest` integration / smoke |
| `DockerBrowserSmokeTest` | Starts the browser for the **configured** mode (`Configuration.browser` → `Browser.fromString`), loads a `data:` URL and reads an element back, so the scrape path's driver half is covered without the network or the live game. Selenium Manager resolves the driver itself (`~/.cache/selenium`, geckodriver 0.37.1 against Firefox ESR 140); a browser on the `PATH` is the only prerequisite. `assumeTrue`-skipped where none exists — the production image's build stage runs `check` on `eclipse-temurin:25-jdk` and must not gain a browser dependency. | integration (real browser, no server) |
| `TestHelper` | Levenshtein "closest line" helper for fuzzy HTML assertions. | helper (no `@Test`) |
| `MetaInformationTest` | `MetaInformation<T>` delegates serialize/deserialize to its `MetaInformationKey<T>`. | pure unit |
| `EntryFactoryTest` | `deduplicates()`: 3 raw lines collapse to 1 item by name+quality; a timestamped headline with no known transfer type is dropped **with a warning**. | pure unit (thin) |
| `EntityParserContractTest` | The parser contract, pinned line by line: headline → entry, `Einzahlung`→`EINLAGERUNG`, withdrawal entries, quality defaulting to 100 when absent, the value-neutral `+1` modifier (ignored, and merged with its unmodified counterpart), quantity merging for same name+quality, separation when quality differs, the `Impressum` terminator, one entry per headline, empty protocol → no entries, and the malformed/out-of-range-date paths (skipped without aborting the ingest, neighbouring entries still parsed, the malformed headline's items **not** folded into the preceding entry, a warning when a typed headline is dropped). | pure unit |
| `EvergoreItemTest` | `getStorageValue()`/`getWithdrawlValue()` for raw, craftable and gem items, plus two all-items golden-master rules: withdrawl value is market value scaled by 0.6 for **every** item, storage value is zero for all non-craftable and the sum of ingredient withdrawl costs per unit for all craftable ones. | pure unit |
| `HexagonalArchitectureTest` | ArchUnit guard, four rules: `domain`+`businessLogic` depend on **no** framework/library packages (Micronaut, jakarta, Selenium, ORMLite/SQLite, Jackson, RxJava, Apache Commons, logback/SLF4J, Netty); the `application` use cases stay framework-free too; `application` depends only **inward**; the core (`domain`+`businessLogic`) likewise, and additionally not on `application`. Turns the hexagonal golden rule into a build failure (each rule verified non-vacuous by a temporary deliberate violation: forbidding `java.time` flags 22 core usages, a class literal from `rest.filter` in `Item` fails the core's inward-only rule). | architecture guard (ArchUnit + JUnit 5) |
| `EvergoreDataEvaluatorTest` | Unit tests covering: bank aggregation (placement + withdrawl sums), storage valuation (craftable item with quantity and partial quality), `Erde-Eibenlanze` resolving by its real in-game spelling, unknown item fallback (zero value + WARN + counted per occurrence into `EvaluationResult`, and empty when everything resolves), full recompute (sums start at zero over all stored entries and **overwrite** stale meta values; a second run is idempotent), mid-run-failure self-healing (a failed avatar writes nothing, and no `last_updated` is written), avatar union across both repos (all keys written per avatar), and `last_updated` being written in Berlin wall-clock time from the application `Clock`. Hand-written fakes: `FakeMetaInformationRepository`, `BankRepositoryStub`, `StorageRepositoryStub`, `LoggerSpy`. | pure unit |
| `EvergoreDataExtractorTest` | Unit tests covering: parsed bank/storage entries are persisted; a still-visible entry older than the stored max but missing from the database is healed (ingested) via `getAllSince(min scraped timestamp)`; an identical already-stored row (bank and storage) is not duplicated; a scraped identical pair with one already stored ingests only the surplus; two identical scraped rows both survive when neither is stored yet; entries are ingested oldest-first for partial-batch safety. `FakePageSource` returns canned `PageContents`; capturing extensions of `BankRepositoryStub`/`StorageRepositoryStub` assert the `getAllSince` argument (a wrong argument yields an empty result, so a mutant passing the wrong timestamp is caught). No browser, no framework. | pure unit |
| `BankDatabaseRepositoryTest` | Repository is usable without separate init (file DB); `getAllSince(timestamp)` includes the row exactly at the boundary, excludes older rows, includes newer rows; `countFor` counts only the given avatar's rows and is zero for an unknown one (in-memory SQLite). | adapter integration |
| `StorageDatabaseRepositoryTest` | `getAllSince(timestamp)` includes the row exactly at the boundary, excludes older rows, includes newer rows; `countFor` counts only the given avatar's rows and is zero for an unknown one (in-memory SQLite). | adapter integration |
| `ProtocolEvaluationAcceptanceTest` | End-to-end: copies the committed synthetic fixture DB (`testdata.sqlite`) to a `build/` working copy, boots the real Micronaut `EmbeddedServer` against it, stubs the scraper (`loadData()` no-op) while the **real** `EvergoreDataEvaluator` runs via the scheduled job, then asserts `/overview` bank totals and `/avatars/{a}/bank\|storage` rows through `RenderedTable`, plus storage **valuation** at the `MetaInformationRepository` bean level (no endpoint surfaces it yet, Epic E1) and an unknown endpoint answering 4xx. | `@MicronautTest` acceptance / e2e |
| `ProtocolEvaluationJsonAcceptanceTest` | The JSON sibling of the above, on the same fixture and the same real-evaluator boot: asserts the overview totals for all three avatars, `lastUpdated` parsing as an ISO-8601 instant, and the **byte-exact** bank and storage response bodies (so field names, key order, the ISO-8601 UTC instants and the `DEPOSIT`/`WITHDRAWAL` wire names are all pinned). `lastUpdated` gets one test per property (parses as an instant, is the UTC form and not an offset form, reads back in the application zone as the stored wall-clock time). Plus the paging and error contract: a page past the end is 200 with `items: []` and the true `totalCount`, an unknown avatar is 404 while a **known** avatar with an empty ledger (`Calix`, who has bank but no storage rows in the fixture) is 200 with `totalCount: 0`, and a `@ParameterizedTest` walks every out-of-bounds paging window over all three routes, each a 400. | `@MicronautTest` acceptance / e2e |
| `RenderedTable` | Parses rendered HTML tables into a header + rows of cell text, tolerant to attributes/styling/wrapper tags, so UI restyling never breaks assertions. The robust successor to `TestHelper`'s Levenshtein matching; **all** markup coupling lives here alone. | helper (no `@Test`) |
| `TestDataGenerator` | Run-on-demand writer (`./gradlew generateAcceptanceDb`) of the committed synthetic fixture `testdata.sqlite`: 3 avatars; bank in both directions; storage with quality scaling and a zero-value item. Item names reference `EvergoreItem.*.ingameName`, so values stay derived, not invented. | fixture generator (`main`) |
| `LastRunStatusTest` | Pure unit tests: empty before any run; records a specific `Instant` and returns it; second record overwrites the first; no unknown item names initially; records the unknown item names of the last run. No framework. | pure unit |
| `LastRunHealthIndicatorTest` | Pure unit tests (with framework dep on `micronaut-management`): reports `UNKNOWN` with no detail map before any run; reports `UP` with `lastSuccessfulRun` detail key after a run; omits the unknown-item detail when the last run had none; reports the unknown-item count **and** names when present. Subscribes to the `Publisher` inline via an anonymous `Subscriber`. | pure unit |
| `EvergoreDataCollectorJobTest` | Pure unit tests: records `lastSuccessfulRun` after a successful cycle; does **not** record when `loadData` throws; does **not** record when `evaluateData` throws; forwards the run's unknown items to `LastRunStatus`. Uses `Clock.fixed(…)`, `ZeroDelayConfiguration extends Configuration` (delay 0), and local `FailableExtractor`/`FailableEvaluator` inner classes; no static state. | pure unit |
| `HealthEndpointTest` | Boots the real Micronaut `EmbeddedServer`; mocks the extractor (no-op `loadData`), config (zero delay, test DB path), and hooks (BootSignalRecorder pattern). Asserts: `GET /health` returns **exactly 200** without a token; response body contains `lastRun` + `lastSuccessfulRun`; `/healthz` is **not** served as the health endpoint (401, so a prefix match cannot inherit the exemption, and the router is held to the rule `PublicPathsTest` states at unit level). The token scope itself lives in `TokenScopeTest`, not here. | `@MicronautTest` integration |
| `TransferTypeTest` | Locks `TransferType.toGermanString()` for both constants (`EINLAGERUNG`→"Einlagerung", `ENTNAHME`→"Entnahme"), the single source for the enum→German mapping. | pure unit |
| `ApplicationExceptionHandlerTest` | Unit tests asserting each `ProtocolParserException` subclass maps to its HTTP status via the visitor, plus the `onUnknown` branch, plus that the logged path carries **no** token query parameter, plus the log severity per mapped status (client error → one `info` line, server error → `error` with the exception) and that a failing response mapping is still logged with its trace before it propagates. `accept` is `abstract`, so a new exception subclass is a compile error rather than a silent fallback. | pure unit |
| `ProductionSnapshotRecomputeCheck` | **`@Disabled`, on-demand**: boots the real context against a *copy* of a local production snapshot (`temp.sqlite`, gitignored) with the scraper stubbed, so the real `EvergoreDataEvaluator` recomputes the meta sums on real data and the delta can be inspected before a deploy; also exports the valuation catalog and asserts item names are unique (`findItem` takes the first name match). Details: [1:1 against the production instance](#11-against-the-production-instance). | `@MicronautTest` on-demand check |
| `RateLimitCounterTest` | Pure unit tests for `RateLimitCounter`: the block lifts deterministically after `block-duration` (injected `Clock`, no `sleep`), stays active before expiry, and 20 concurrent `block()` calls leave consistent state. | pure unit |
| `RateLimitFilterTest` | Boots the server in the `ratelimit` environment (`rebuildContext = true`) against its own fixture DB copy: `/favicon.ico` is blocked with 429 once the configured limit is exceeded, while `/`, `/index.html` and a **bundled** asset (resolved from the packaged `assets/` dir, not hard-coded) stay repeatedly reachable without a token. A `//probe` is counted like any other path instead of being skipped as the SPA root, so its third request hits 429 rather than a third 401. A burst of malformed targets is answered 400 throughout and never reaches the counter, which characterizes the framework rather than our filter. | `@MicronautTest` integration |
| `SpaHistoryFallbackTest` | Boots the server against its own fixture DB copy and pins the fallback in **both** directions: an unknown navigation path and a client route carrying a dot (in a middle segment and in the last one) return 200 with the byte-identical bundled `index.html` and `Cache-Control: no-cache`, while a missing asset (dotted **and** dotless), a missing swagger path, an unknown `/api` path and a traversal resolving below `/assets` keep the default 404. | `@MicronautTest` integration |
| `SpaNavigationPathsTest` | Pure unit tests for the rule behind that fallback: the SPA owns unknown navigation paths including dotted avatar names, but not the reserved mappings (`/api`, `/assets`, `/health`, the OpenAPI UIs) nor a path whose last segment carries a lowercase file extension; a prefix only matches on a segment boundary (`/apiary/tour` stays SPA-owned). | pure unit |
| `TokenScopeTest` | Boots the server and proves the default-deny scope over HTTP in both directions: `/api/v1/avatars`, `/overview`, `/avatars/{a}/bank`, an **unmapped** path (`/i_dont_exist`) and a SPA client route are all 401 without a token (and the API with a wrong one), while `/`, `/index.html`, a **bundled** asset resolved from the shipped `index.html`, `/swagger-ui/index.html`, `/health` and `/favicon.ico` answer 200 without one. Includes the traversal pair (`/assets/../overview` and its percent-encoded form are 401, not treated as assets) and the leading-`//` pair (`//overview`, `//api/v1/avatars` are 401, not read as an authority). Also pins the framework boundary over five malformed targets: Micronaut answers them **400 before any filter runs**, proven by the absence of the request log's `Client IP` line. That is a **characterization test of Micronaut, not of our code** (our canonicalizer never sees the request), kept as the canary for a framework upgrade changing the behaviour. | `@MicronautTest` integration |
| `PublicPathsTest` | Pure unit tests for the default-deny matcher: an exactly configured path and anything below a configured `/**` are public; an unconfigured path, the API and a path that merely extends a configured one (`/healthz`, `/assetsomething`) are not; an empty **and** a missing configuration protect everything, so a misconfiguration fails closed. | pure unit |
| `PathCanonicalizerTest` | Pure unit tests for the one path form all path decisions share: resolves `..` (plain, percent-encoded, and hidden behind an encoded separator), decodes exactly once so `%252e` stays literal text, drops `.` and empty segments, clamps `..` at the root, drops a trailing slash, keeps a dot / decoded space / `+` inside a segment, does **not** let a leading `//` swallow the first segment, and keeps a segment with a malformed escape (`%zz`, a lone `%`) as literal text instead of throwing, so the function is total and every caller fails closed. | pure unit |
| `RawHttpClient` | Sends a request target byte for byte over a socket. Needed for exactly one case, measured rather than assumed: Unirest (and Apache HttpClient under it) **refuses** a malformed percent-escape client-side with `IllegalArgumentException`, so `/overview%zz` cannot be sent through it at all. A leading `//` needs no raw socket, Unirest sends that unchanged. Its read timeout is a hang guard, never a correctness condition. | helper (no `@Test`) |
| `AvatarSummariesEmptyStateTest` | Boots the server against an **empty** database with the collector disabled and asserts the overview body byte for byte: `{"lastUpdated":null,...,"items":[]}`. The one test that proves the `jackson.serialization-inclusion: ALWAYS` contract, i.e. that "no run yet" stays a visible explicit `null` and an empty page a visible empty array rather than missing keys. | `@MicronautTest` integration |
| `RenameSafetyTest` | ArchUnit guard, three rules: every non-static field under `rest/controller/api/wire` carries `@JsonProperty`, every `@DatabaseField` declares a non-empty `columnName`, every `@DatabaseTable` a non-empty `tableName`. Turns "a rename must never change a published contract or the schema" into a build failure; the column rule found the two `id` fields that relied on the field-name default. | architecture guard (ArchUnit + JUnit 5) |
| `SpaStaticResourcePathsTest` | Pure unit tests that the static-resource match runs on the canonicalized path, so neither `/assets/../overview` (plain and percent-encoded) nor a leading-`//` path is mistaken for an asset by the request log and rate limiter. | pure unit |
| `SpaBundlePackagingTest` | Guards that `/static/ui/index.html` is on the test runtime classpath, i.e. the SPA bundle really is packaged into the jar by `processResources`. | pure unit (packaging guard) |
| `ApiTokenStartupValidatorTest` | Startup fails with the property name (`evergore.security.api-token`) in the message for an empty, blank and `null` token; a set token starts up; the `onApplicationEvent` entry point propagates the same failure. | pure unit |
| `BootSignalRecorderTest` | `awaitCollection()` unblocks both on `recordCollectionFinished()` and on `recordException()` (real threads, no timeouts), and the `dataLoaded`/`exceptionOccurred` queries flip false→true. Pins the boot-signal seam itself. | pure unit (concurrency) |
| `BankEntryEqualityTest` / `StorageEntryEqualityTest` | Value equality of the two entry records: equal when all fields match, different for each single field in turn (timestamp, avatar, amount/quantity, name, quality, transfer type). The window-dedup in `EvergoreDataExtractor` compares entries by value, so this is load-bearing, not record boilerplate. | pure unit |
| `SeleniumPageSourceTest` | Drives `SeleniumPageSource` against a `RecordingWebDriver` fake: the driver is quit after a successful scrape **and** after a failing one (try/finally), the scrape failure propagates and is logged, and a failure while quitting is logged without discarding the already-scraped contents. | pure unit (fake driver) |
| Fakes & stubs | `LoggerSpy` (records info/warn/error messages), `FakeMetaInformationRepository` (in-memory map), `BankRepositoryStub` / `StorageRepositoryStub`, `RecordingWebDriver` (scriptable Selenium `WebDriver`). Hand-written, no mocking framework. | helpers (no `@Test`) |

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

**Has tests:** `EvergoreItem` (value math per item kind **plus** two all-items golden-master rules over the whole catalog) · `MetaInformation` (serialization) ·
`EntryFactory` / `EntityParser` (the parsing contract: headline→entry, type mapping, quality defaulting, `+1` merging, quantity merging, `Impressum` terminator, malformed/out-of-range dates) · `EvergoreDataEvaluator` (bank aggregation, storage valuation, unknown item fallback + per-occurrence counting, full-recompute overwrite + idempotence + failed-run self-heal, avatar union, Berlin wall-clock `last_updated`) ·
`EvergoreDataExtractor` (parse→persist pipeline, window dedup via `getAllSince(min scraped timestamp)`: heals a still-visible entry missing from the database, no-duplicate + surplus-only dedup, oldest-first partial-batch safety) ·
`BankDatabaseRepository` / `StorageDatabaseRepository` (`getAllSince` inclusive boundary, `countFor` per avatar, in-memory SQLite) ·
the **evaluate→overview pipeline end-to-end** via `ProtocolEvaluationAcceptanceTest` (HTML) and `ProtocolEvaluationJsonAcceptanceTest` (the JSON contract byte-exact, plus its paging and error cases) ·
`LastRunStatus` (record + read, incl. unknown item names) · `LastRunHealthIndicator` (UNKNOWN / UP + detail, incl. the unknown-item detail) ·
`EvergoreDataCollectorJob` (records run on success, not on failure; forwards unknown items) ·
`/health` endpoint + wrong-token rejection via `HealthEndpointTest` ·
the **token scope in both directions**: the default-deny rule itself (`PublicPathsTest`) and over HTTP incl. the traversal pair and an unmapped path (`TokenScopeTest`), on the canonicalized path (`PathCanonicalizerTest`, `SpaStaticResourcePathsTest`) ·
the **rename safety** of the JSON contract and the DB schema (`RenameSafetyTest`) ·
`TransferType`→German mapping (`toGermanString`) · `ApplicationExceptionHandler` exception→HTTP visitor dispatch + token-free logging + severity by mapped status ·
`RateLimitCounter` (block thread-safety + deterministic expiry) and the filter's public-path exemptions (`RateLimitFilterTest`) ·
`ApiTokenStartupValidator` (startup aborts on an unset/blank token) ·
the SPA seam: history fallback vs. 404 in both directions (`SpaHistoryFallbackTest`, `SpaNavigationPathsTest`) and bundle packaging (`SpaBundlePackagingTest`) ·
`BankEntry`/`StorageEntry` value equality (load-bearing for the window dedup) ·
`SeleniumPageSource`'s driver lifecycle (quit on success and failure, failure logging) ·
`BootSignalRecorder` (the boot-signal seam itself) ·
and *indirectly* via `SmokeTest`: controllers, filters, repositories, the job, `OutputFormatter`.

**Most important UNTESTED logic:**
1. **`SeleniumPageSource`'s scrape itself**: navigation, pagination and login against the live site
   (inherently hard). The driver *lifecycle* around it is covered by `SeleniumPageSourceTest`.
2. **Repositories**: paging, `getAllFor(avatar)`. Only incidental smoke coverage (`getAllSince` has adapter tests).

## Migration verification: Gradle / Java 25 / Micronaut 4.10 (2026-06-16)

The build migration (Maven→Gradle, Java 17→25, Micronaut 3.8.4→4.10.3) was held to **identical
observable behaviour**:

- **Unit/integration suite** reproduces the pre-migration baseline exactly: **22 tests, 7 classes, 0
  failures** on the new stack.
- **1:1 against production data:** the migrated distribution rendered the production snapshot
  identically to the live instance. Procedure and current result: [1:1 against the production
  instance](#11-against-the-production-instance).
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
richer variant (boot against a real prod snapshot) is `ProductionSnapshotRecomputeCheck`: committed
but `@Disabled`, because the snapshot it needs carries guild members' data (PII) and stays
**gitignored**. See the next section.

## 1:1 against the production instance

The release gate before deploying: render the **same database** through the candidate build and
through the live instance, then diff. It catches rendering drift that the synthetic fixture cannot,
because it uses the real data volume, the real item mix and the real avatar set.

### Procedure

1. `./gradlew installDist`.
2. Run the distribution from an **isolated working directory** holding a *copy* of the production
   snapshot at `database/temp.sqlite`, with `EVERGORE_SECURITY_API_TOKEN` set. Never point a run at
   the original snapshot file: a first run recomputes the meta sums in place and is not reversible.
3. Fetch `/overview` plus `/avatars/{avatar}/bank` and `/avatars/{avatar}/storage` for every avatar,
   from **both** instances. Sweep pages 0 **and** 1: page 1 is the more interesting case, because
   `getAllFor(avatar, page, size)` throws `NoElementFound` on an empty result, so any avatar with
   fewer than `PAGE_SIZE` entries answers **404** there. That 404 pattern is part of the contract
   and must match too.
4. Diff after LF normalisation only. Explain every remaining difference; do not widen the
   normalisation until the diff is empty.

- **Pace the sweep.** Both sides enforce 5 requests per 10 s and then block the client IP for 1
  minute (hard-coded before, `evergore.rate-limit.*` now, same numbers). The filter increments the
  counter on blocked requests too, so retrying inside a block **extends** it: leave at least 4 s
  between requests and back off well past a minute after a 429.
- The scrape branch cannot run in the devcontainer (no Firefox binary). Micronaut's task exception
  handler catches it, the app keeps serving, and the database stays untouched, so the check is
  unaffected.

### Current result

Full sweep over the whole avatar set, 165 responses per side, against one identical snapshot:

- **Every status code matches**, including the 404s from the empty page-1 requests.
- `/overview` is **byte-identical** after LF normalisation.
- The detail pages differ in **exactly one line**, the same line in every one of them: the live
  instance still ships the client-side paging script with the literal placeholder
  `?token=secret_token`, the candidate reads the token from the current URL instead. That is the
  intended fix that came with the config-driven API token (the old page's paging control navigated
  with a bogus token). **No rendered data differs.**
- `/health` is the one endpoint that answers differently by design: token-exempt and anonymous in
  the candidate, token-gated in the live instance.

### Recompute delta on real data

The meta sums are recomputed from all stored entries instead of being accumulated behind a
watermark, so the first run on a long-lived database corrects accumulated drift. Measured on the
production snapshot via the gitignored harness described below:

- **Bank:** 31 of 41 avatars change their deposit total, 2 their withdrawal total. Every changed
  value moves **down**, never up: the old accumulator over-counted by 5.24 % of deposits.
- **Storage:** all 42 avatars change; here most values move **up**, because the old accumulator also
  missed history. No endpoint surfaces these yet.
- **Both sides verified against an independent recomputation** and matched exactly: bank totals
  against a plain `SUM(amount) GROUP BY avatar, type`, storage totals against a catalog-driven sum
  over every stored row. The old values disagreed with that ground truth for 31 of 41 avatars, the
  new ones for none. The delta is the fix landing, not a regression.
- **Catalog gap, pre-existing:** the snapshot holds storage rows whose item name is not in
  `EvergoreItem`; they value at zero and are reported through `/health`'s `unknownItemNames`.
- **Not covered by this check:** the re-ingest of still-visible entries missing from the database
  needs a live scrape, so it is only exercised by `EvergoreDataExtractorTest`.

### The production-snapshot harness

`ProductionSnapshotRecomputeCheck` boots the real context against a **copy** of the snapshot, stubs
the scraper, and lets the real `EvergoreDataEvaluator` run through the scheduled job, mirroring
`ProtocolEvaluationAcceptanceTest`'s mock-bean set. It also exports the valuation catalog, which is
what makes the storage cross-check above possible.

- The class itself holds **no production data**: the PII sits in `temp.sqlite`, which stays
  gitignored. So the harness is committed and reviewable, and only the snapshot it feeds on is
  local.
- It is **`@Disabled`**: without a local snapshot it has nothing to run against, and a check that
  passes on one machine only must not become a build gate. It still compiles under `-Werror`, so a
  refactor cannot rot it unnoticed.
- To run it, drop the `@Disabled` for that run and restore it afterwards. Re-enabling it by flag
  would need `junit.jupiter.conditions.deactivate` forwarded to the test JVM from
  `build.gradle.kts`, which is not wired.
- It writes only under `build/`, never to the snapshot. Always copy the snapshot; never open the
  original read-write.

## Test execution model (one JVM)

The whole suite runs in a **single test JVM**. `tasks.test` sets no `forkEvery`, and a
configuration-time `check` fails the build if it is ever set again.

- **Nothing needs the isolation.** There is no shared mutable state across test classes: every
  `@MicronautTest` overrides `Configuration.getDatabasePath()` in its own `@MockBean` subclass and
  owns a private SQLite file under `build/tmp/**`, and boot signals travel through the DI-scoped
  `BootSignalRecorder` (above), not statics. `Configuration.useInMemory` / `DATABASE_TEMP_SQLITE`
  are public and mutable but are *instance* fields of a `@Singleton` that nothing writes.
- **What forking cost.** Gradle hands every non-anonymous **class file** of the test source set to
  the test-class processor, so `forkEvery = 1` restarted the JVM once per class file — 198 of them,
  only 28 holding tests. `--tests` does not reduce that count: it filters inside the worker at JUnit
  discovery, so one focused test still paid all 198 boots (19m33s for an 8.1 s test), and the full
  suite took ~9 min for ~31 s of actual test time.
- **Why a guard.** Per-class forking has twice been added as a crutch for a startup race that was
  already fixed elsewhere, and twice removed once the suite was proven green in one JVM (decisions
  2026-06-20, 2026-08-01). The check is configuration-time on purpose: `forkEvery` is not a tracked
  task input, so a re-add leaves `test` UP-TO-DATE and an execution-time check would never run.

## Testing direction for the rebuild (TDD/BDD)

- **BDD (PO perspective):** capture the use cases as scenarios, e.g. *"Given a member deposited
  N gold and crafted items worth M, when I view the overview, then their guild value is N+M."*
  The whole collect→evaluate→overview flow is now covered by `ProtocolEvaluationAcceptanceTest`
  (scraper stubbed, real evaluation, asserted via HTTP + the meta repo).
- `RenderedTable` provides the structured, restyle-proof HTML assertions that should replace
  `SmokeTest`'s brittle Levenshtein matching (`TestHelper`) when `SmokeTest` is next reworked.
