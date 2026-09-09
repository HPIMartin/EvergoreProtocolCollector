# 06: Testing

## Inventory

| Test | Scope | Style |
|------|-------|-------|
| `SmokeTest` | Boots the real Micronaut `EmbeddedServer`; mocks Selenium (`TestEvergoreDataExtractor extends EvergoreDataExtractor` with `super(null,…)`); points DB at `build/tmp/smokeTest.sqlite` (under the build directory, so rewriting it per run cannot invalidate the `processTestResources` inputs); **resets that file, and its `-journal`/`-wal`/`-shm` sidecars, from `TestConfiguration`'s static initializer**, next to the one place that names the path; sets job delay 0; asserts app starts + the job runs, a row seeded through each repository reads back byte-exact through `/api/v1/avatars` & `/api/v1/avatars/{a}/bank\|storage` (one avatar name per test, so the class is order-independent), unknown path → 4xx, and that the `PageSource` bean resolves to the `SeleniumPageSource` adapter. `@MockBean` for `Configuration`, the hooks, task-exception-handler. | `@MicronautTest` integration / smoke |
| `DashboardBrowserSmokeTest` | The one test that joins the two halves: boots the real server against the fixture DB (real evaluator, stubbed scraper) and drives **headless Firefox** at `/overview` and at a `/avatars/{a}/bank` **deep link**, then reads the rendered rows back through the SPA's `data-testid` hooks. Pins that the bundled SPA really reaches the API and paints its data: the overview shows all three avatars with de-DE grouped totals (`1.500`), the ledger its entries newest-first with Berlin wall-clock timestamps and the German transfer names. Everything else proves one half only, since the Vitest suite fakes `HttpGet` and the Java suite stops at the JSON. `assumeTrue`-skipped where no browser is on the `PATH`, like `DockerBrowserSmokeTest`. The `WebDriverWait` is a **hang guard, not a correctness condition**: it waits for a rendered row to exist, so slow hardware waits longer and a never-rendering page fails instead of passing. | integration (real browser + real server) |
| `DockerBrowserSmokeTest` | Starts the browser for the **configured** mode (`Configuration.browser` → `Browser.fromString`), loads a `data:` URL and reads an element back, so the scrape path's driver half is covered without the network or the live game. Selenium Manager resolves the driver itself (`~/.cache/selenium`, geckodriver 0.37.1 against Firefox ESR 140); a browser on the `PATH` is the only prerequisite. `assumeTrue`-skipped where none exists — the production image's build stage runs `check` on `eclipse-temurin:25-jdk` and must not gain a browser dependency. | integration (real browser, no server) |
| `MetaInformationTest` | `MetaInformation<T>` delegates serialize/deserialize to its `MetaInformationKey<T>`. | pure unit |
| `MetaInformationKeyTest` | Pins the `sums_recomputed_at_<avatar>` key: its id, and that it serializes an `Instant` as **epoch millis** and reads that instant back unchanged. The load-bearing case is the Berlin **fall-back hour**: two instants an hour apart that share one wall-clock text serialize differently and read back distinctly, which is exactly what `last_updated`'s wall-clock format cannot do. | pure unit |
| `EntryFactoryTest` | `deduplicates()`: 3 raw lines collapse to 1 item by name+quality; a timestamped headline with no known transfer type is dropped **with a warning**. | pure unit (thin) |
| `EntityParserContractTest` | The parser contract, pinned line by line: headline → entry, `Einzahlung`→`EINLAGERUNG`, withdrawal entries, quality defaulting to 100 when absent, the value-neutral `+1` modifier (ignored, and merged with its unmodified counterpart), quantity merging for same name+quality, separation when quality differs, the `Impressum` terminator, one entry per headline, empty protocol → no entries, and the malformed/out-of-range-date paths (skipped without aborting the ingest, neighbouring entries still parsed, the malformed headline's items **not** folded into the preceding entry, a warning for **every** dropped block head, whether it fails on the transfer type or on the strict headline match), and the unparseable-item-line path (an amount or quality that is no parseable number skips **that line only**, with a warning; the entry's other items and the neighbouring entries survive), the no-parseable-items path (an entry whose item lines all fail the item regex warns), and the avatar/type-word path (one parameterized case per headline shape: an ASCII-letter suffix, an umlaut suffix, punctuation fencing the type word on both sides, and a headline with no separate type field at all, each minting no entry and warning, while a legitimate avatar name containing a type word still parses). | pure unit |
| `EvergoreItemTest` | `getStorageValue()`/`getWithdrawlValue()` for raw, craftable and gem items, plus two all-items golden-master rules: withdrawl value is market value scaled by 0.6 for **every** item, storage value is zero for all non-craftable and the sum of ingredient withdrawl costs per unit for all craftable ones. | pure unit |
| `HexagonalArchitectureTest` | ArchUnit guard, four rules: `domain`+`businessLogic` depend on **no** framework/library packages (Micronaut, jakarta, Selenium, ORMLite/SQLite, Jackson, RxJava, Apache Commons, logback/SLF4J, Netty); the `application` use cases stay framework-free too; `application` depends only **inward**; the core (`domain`+`businessLogic`) likewise, and additionally not on `application`. Turns the hexagonal golden rule into a build failure (each rule verified non-vacuous by a temporary deliberate violation: forbidding `java.time` flags 22 core usages, a class literal from `rest.filter` in `Item` fails the core's inward-only rule). | architecture guard (ArchUnit + JUnit 5) |
| `KnownAvatarsTest` | Pure unit tests for the single definition of "an avatar the service knows": both ledgers' avatars as one list sorted by **German collation** (`Anna` before `Ärger` before `Zorn`, so an umlaut name does not land behind `Z`), an avatar living in both ledgers named once, both sides sorted **together** rather than the storage side appended, and nobody while neither ledger has rows. Hand-written fakes: `BankRepositoryStub`, `StorageRepositoryStub`. | pure unit |
| `ContributionTest` | Pure unit tests over the four ledger sums: `net()` reproduces the **verified** sheet column 5 for the five member rows [google-sheet.md](google-sheet.md) documents (`@CsvSource`, one case per member), `sumOf` adds a collection of contributions into the guild's own and answers `NOTHING` for none at all, and `inWholeGold()` rounds each storage sum to whole gold **before** a net or a total is taken from it, so a served row always adds up and a total is the exact column sum of its rows. No framework. | pure unit |
| `AvatarContributionsTest` | Pure unit tests over the assembler: all four stored sums of a known avatar are read, an avatar without any stored sum counts as zero instead of dropping out of the list, the German collation order of `KnownAvatars` is kept, and each ledger's newest entry becomes that avatar's last activity while a ledger he never used stays unanswered. Hand-written fakes: `BankRepositoryStub`, `StorageRepositoryStub`, `FakeMetaInformationRepository`. Also pins that one call reads the store through exactly **one** snapshot, and the staleness read on top of it: the newest per-avatar recompute instant is the last collection, a row older than it answers the instant its own sums come from, a row at it answers none, an avatar carrying no recompute instant at all answers none, and over the guild `containsStaleSums` is true exactly when one row lags | pure unit |
| `EvergoreDataEvaluatorTest` | Unit tests covering: bank aggregation (placement + withdrawl sums), storage valuation (craftable item with quantity and partial quality), `Erde-Eibenlanze` resolving by its real in-game spelling, unknown item fallback (zero value + WARN + counted per occurrence into `EvaluationResult`, and empty when everything resolves), full recompute (sums start at zero over all stored entries and **overwrite** stale meta values; a second run is idempotent), mid-run-failure self-healing (the failing avatar's own sums are withheld and recover on the next clean run), avatar union across both repos (all keys written per avatar), and `last_updated` being written in Berlin wall-clock time from the application `Clock`. Hand-written fakes: `FakeMetaInformationRepository`, `BankRepositoryStub`, `StorageRepositoryStub`, `LoggerSpy`. Also pins that one run hands the store **one** batch carrying every known avatar's four keys plus `last_updated` (a regression guard against writing per avatar again, which is what let a request read a half-written recompute). Failure isolation: an avatar whose ledger read throws is reported in the `EvaluationResult` and logged at `error` while every healthy avatar still refreshes in the same batch, that avatar's stored sums stay untouched, and the collection timestamp `last_updated` is still stamped on such a run, because it records that a scrape happened rather than that every avatar recomputed. Recompute instants: one run stamps **one** instant for every avatar it refreshed, proven with a hand-written **ticking** `Clock` rather than a fixed one, because a fixed clock cannot tell one call from many and the first version called it per avatar, which made every avatar but the last look stale; a second run re-stamps its own single instant. A failing avatar keeps the instant of the last run that reached him; a failing avatar carrying none is seeded from the newest instant the per-avatar keys already held, and is left without one while no avatar carries any. Two of those pin the **seed's exactness**, which is the whole reason it does not read `last_updated`: the seed survives the second pass of the Berlin fall-back hour to the millisecond, and a seeded avatar comes out exactly as current as the avatar whose stamp anchors that run, read back through `AvatarContributions` | pure unit |
| `EvergoreDataExtractorTest` | Unit tests covering: parsed bank/storage entries are persisted; a still-visible entry older than the stored max but missing from the database is healed (ingested) via `getAllSince(min scraped timestamp)`; an identical already-stored row (bank and storage) is not duplicated; a scraped identical pair with one already stored ingests only the surplus; two identical scraped rows both survive when neither is stored yet; entries are ingested oldest-first for partial-batch safety. `FakePageSource` returns canned `PageContents`; capturing extensions of `BankRepositoryStub`/`StorageRepositoryStub` assert the `getAllSince` argument (a wrong argument yields an empty result, so a mutant passing the wrong timestamp is caught). No browser, no framework. | pure unit |
| `MetaInformationSnapshotIsolationTest` | Runs a read **against a recompute that is still open**, on real SQLite and real threads, deterministic via two `CountDownLatch`es (no `sleep`, no wall-clock condition; the `await` timeouts are hang guards only): a key whose `serialize` blocks holds the write transaction open from **inside**, and the reader on its own connection must see the pre-recompute generation **whole**, then the committed one whole. Verified non-vacuous: with the transaction removed from `add` the reader sees `[1, 2]`, the torn read itself. | adapter integration |
| `AvatarSummariesSnapshotTest` | The same guarantee from the response's side, single-threaded: a meta repository whose every snapshot answers a later recompute generation must still yield **one** generation across all rows and the guild total. Pins that the read takes one snapshot for the whole page; a per-avatar snapshot would mix generations again. | pure unit |
| `MetaInformationDatabaseRepositoryTest` | Adapter tests for the meta store's write atomicity: a batch stores every entry, a batch whose last entry cannot be serialized stores **none** of the earlier ones, and a key that already held a value keeps it when the batch it sits in fails. Rollback is the proof that `add` is one transaction, so no reader can observe a half-written recompute; single-threaded and clock-free, no concurrency needed to pin it. A NULL value needs no case here: the column is `NOT NULL`. | adapter integration |
| `DatabaseMigrationTest` | Adapter tests for the Flyway setup on a file database: an empty database ends up with all three tables; a database still carrying the pre-Flyway schema keeps **every row byte-for-byte** through the migration; after it, a `NULL` in **every** column of all three tables is refused with `NOT NULL constraint failed`; migrating twice changes neither schema nor rows; a database missing one of the three tables has it created and migrates anyway (the case a baseline *at* `V1` got wrong); a pre-existing `NULL` row aborts the migration, leaves the database as it was and migrates cleanly once the row is fixed; a table that exists but lacks a column aborts with `no such column` instead of dropping or altering it; and eight threads migrating the same fresh database at once still migrate it exactly once, the case that pins the `synchronized` block against `SQLITE_BUSY`. The row-for-row assertion is what proves the irreplaceable history survives. | adapter integration |
| `BankDatabaseRepositoryTest` | Repository is usable without separate init (file DB); `getAllSince(timestamp)` includes the row exactly at the boundary, excludes older rows, includes newer rows; `countFor` counts only the given avatar's rows and is zero for an unknown one; `latestTimestampPerAvatar` names the newest row per avatar and nobody at all for an empty ledger. Every case runs on a file database: Flyway migrates over its own connection, which a `:memory:` database would not share. | adapter integration |
| `StorageDatabaseRepositoryTest` | `getAllSince(timestamp)` includes the row exactly at the boundary, excludes older rows, includes newer rows; `countFor` counts only the given avatar's rows and is zero for an unknown one; `latestTimestampPerAvatar` names the newest row per avatar and nobody at all for an empty ledger. Every case runs on a file database: Flyway migrates over its own connection, which a `:memory:` database would not share. | adapter integration |
| `ProtocolEvaluationAcceptanceTest` | End-to-end: copies the committed synthetic fixture DB (`testdata.sqlite`) to a `build/` working copy, boots the real Micronaut `EmbeddedServer` against it, stubs the scraper (`loadData()` no-op) while the **real** `EvergoreDataEvaluator` runs via the scheduled job, then asserts the overview totals for every avatar of **both** ledgers (including the storage-only one, whose row exists only because the overview lists the union) and the **byte-exact** bank and storage response bodies (so field names, key order, the ISO-8601 UTC instants and the `DEPOSIT`/`WITHDRAWAL` wire names are all pinned). `GET /api/v1/admin/status`'s `lastUpdated` gets one test per property there (parses as an instant, is the UTC form and not an offset form, reads back in the application zone as the stored wall-clock time). Plus the paging and error contract: a page past the end is 200 with `items: []` and the true `totalCount`, an unknown avatar is 404 while a **known** avatar with an empty ledger is 200 with `totalCount: 0` in **both** directions (`Calix` has bank but no storage rows, `Brynja` storage but no bank rows), and a `@ParameterizedTest` walks every out-of-bounds paging window over all three routes, each a 400. Plus storage **valuation** at the `MetaInformationRepository` bean level, which is the independent pin behind the four sums the overview body asserts, and an unknown endpoint answering 4xx. | `@MicronautTest` acceptance / e2e |
| `TestDataGenerator` | Run-on-demand writer (`./gradlew generateAcceptanceDb`) of the committed synthetic fixture `testdata.sqlite`: 4 avatars, one of them **storage-only** (`Brynja`, the case the overview's union exists for; her deposit is a **craftable** item, so her storage sum is non-zero, which puts `bankDeposited: 0` beside `storageDeposited: 370.08` in her row and pins that the bank columns stay bank-only while the storage columns really carry the storage half; a zero-value item would have made both true by accident); bank in both directions; storage with quality scaling and a zero-value item. Item names reference `EvergoreItem.*.ingameName`, so values stay derived, not invented. | fixture generator (`main`) |
| `LastRunStatusTest` | Pure unit tests, all read through `snapshot()`: the four scrape/recompute outcome pairs (`recordSuccessfulScrape`/`recordScrapeFailure`/`recordSuccessfulRecompute`/`recordRecomputeFailure`) each empty before any run, record a specific `Instant` and return it, a second record overwrites the first, and the four are recorded independently of each other; a successful recompute takes its unknown-item and failed-avatar names as parameters of the same call; no unknown item names initially; a later clean recompute **replaces** the previous run's unknown-item and failed-avatar names (an empty pair clears them); a recompute failure **preserves** the last successful run's instant and both name lists while flipping `recomputeHealthy` to `false`. No framework. | pure unit |
| `LastRunStatusRecomputeIsolationTest` | Proves `LastRunStatus`'s recompute write is one atomic snapshot, not a torn read, two ways: (1) a writer thread's `recordSuccessfulRecompute` blocks while copying `failedAvatarNames` — the **last** value it copies before publishing — so a concurrent `snapshot()` lands genuinely mid-write; it asserts every field still belongs to the run before it, never a mix of the new `unknownItemNames` with the old instant/`failedAvatarNames`, which a sequential-field writer would fail (verified by hand against a throwaway torn writer, then discarded); once released, the next `snapshot()` shows every field belonging to the new run. (2) An implementation-order-independent stress test races two writer threads (`WRITES_PER_WRITER_THREAD` recomputes each, tagged `A`/`B`) against a spinning reader, asserting on every read that the instant, `unknownItemNames` and `failedAvatarNames` all tag the same run. No framework. | pure unit |
| `LastRunHealthIndicatorTest` | Pure unit tests (with framework dep on `micronaut-management`): reports `UNKNOWN` with no detail map before any recompute; reports `UP` with `lastSuccessfulRecompute` detail key after a recompute; omits the unknown-item detail when the last run had none; reports the unknown-item count **and** names when present; a scrape failure recorded beside a successful recompute shows `lastScrapeFailure` but not `lastRecomputeFailure` while the status stays `UP`, and a recompute failure recorded beside an earlier successful recompute shows `lastRecomputeFailure` but not `lastScrapeFailure` while the status drops to `DOWN` (the latest attempt is what counts, not "ever succeeded"), pinning that `/health` tells the two apart; a first-ever recompute attempt that fails is `DOWN` with only the failure detail, not `UNKNOWN`. Subscribes to the `Publisher` inline via an anonymous `Subscriber`. | pure unit |
| `EvergoreDataCollectorJobTest` | Pure unit tests: records `lastSuccessfulScrape` and `lastSuccessfulRecompute` after a successful cycle; a failed scrape still runs the recompute (`evaluateData` is called, `lastScrapeFailure` and `lastSuccessfulRecompute` are both recorded, `lastSuccessfulScrape` stays empty), unlike a failed recompute, which is rethrown and leaves `lastSuccessfulRecompute` empty beside a recorded `lastRecomputeFailure`; the `PostCollectionHook` runs only after a successful recompute: it does not run when the recompute throws, and it still runs after a failed scrape as long as the recompute that follows succeeds; passes the run's unknown items and failed avatars to `LastRunStatus` as part of the same `recordSuccessfulRecompute` call; the initial delay restores the interrupt flag on interruption instead of swallowing it, proven via a positive-delay `Configuration` (interrupting the current thread first, `TimeUnit.SECONDS.sleep` with a positive argument throws immediately without an actual wait, `sleep(0)` does not throw at all and so cannot pin this). Uses `Clock.fixed(…)`, `ZeroDelayConfiguration extends Configuration` (delay 0), and local `FailableExtractor`/`FailableEvaluator` inner classes; no static state. | pure unit |
| `HealthEndpointTest` | Boots the real Micronaut `EmbeddedServer`; mocks the extractor (no-op `loadData`), config (zero delay, test DB path), and hooks (BootSignalRecorder pattern). Asserts: `GET /health` returns **exactly 200** without a token; response body contains `lastRun` + `lastSuccessfulRecompute`; `/healthz` is **not** served as the health endpoint (401, so a prefix match cannot inherit the exemption, and the router is held to the rule `PublicPathsTest` states at unit level). The token scope itself lives in `TokenScopeTest`, not here. | `@MicronautTest` integration |
| `AdminStatusEndpointTest` | Boots the real Micronaut `EmbeddedServer` against the fixture DB with the scraper stubbed; asserts `GET /api/v1/admin/status` answers 200 without a token, and that after recording a scrape outcome and a recompute outcome (with its unknown items and failed avatars) on `LastRunStatus` the body carries a non-null `lastUpdated` and the matching outcome instants plus the deduplicated, sorted `unknownItemNames`/`failedAvatarNames`. | `@MicronautTest` integration |
| `AdminStatusEmptyStateEndpointTest` | Boots the server against an **empty** database with the collector disabled and asserts `GET /api/v1/admin/status` answers byte for byte with every key present and `null`/empty, the "no run yet" counterpart to `AdminStatusEndpointTest`; the one test that pins that the admin surface renders a missing outcome rather than dropping the key. | `@MicronautTest` integration |
| `TransferTypeTest` | Locks `TransferType.toGermanString()` for both constants (`EINLAGERUNG`→"Einlagerung", `ENTNAHME`→"Entnahme"), the single source for the enum→German mapping. | pure unit |
| `ApplicationExceptionHandlerTest` | Unit tests asserting each `ProtocolParserException` subclass maps to its HTTP status via the visitor, plus the `onUnknown` branch, plus that the logged path carries **no** token query parameter, plus the log severity per mapped status (client error → one `info` line, server error → `error` with the exception) and that a failing response mapping is still logged with its trace before it propagates. `accept` is `abstract`, so a new exception subclass is a compile error rather than a silent fallback. | pure unit |
| `ApplicationExceptionHandlerHttpTest` | Boots the server against its own fixture DB copy and drives the not-found path through the **real Netty write**, which the pure unit test cannot reach: an unknown avatar answers 404 with the default `Not Found` reason phrase (no echo of the requested name), and an unknown avatar whose percent-encoded name carries `CRLF` still answers 404 instead of the 500 that a control character in the reason phrase used to cause. | `@MicronautTest` integration |
| `ProductionSnapshotRecomputeCheck` | **Opt-in, on-demand** (`-DprodSnapshot.check=true`): boots the real context against a *copy* of a local production snapshot (`temp.sqlite`, gitignored) with the scraper stubbed, so the real `EvergoreDataEvaluator` recomputes the meta sums on real data and the delta can be inspected before a deploy. Writes the summaries response verbatim to `build/tmp/prodSnapshot/overview-after-recompute.json` (decision 2026-08-08) and asserts the artifact carries every avatar rather than a first page of them, which holds up to the API's `MAX_SIZE` of 1000 avatars and fails rather than truncates beyond it; also exports the valuation catalog and asserts item names are unique (`findItem` takes the first name match). Holds every meta sum the snapshot stored against the one recomputed from the same rows and writes the pair to `metaSums-stored-vs-recomputed.tsv`; it asserts that every stored key was compared and that none was dropped, so a failed read cannot pass as an empty diff. It does **not** assert equality: the diff is the measurement, not a gate. Details: [1:1 against the production instance](#11-against-the-production-instance). | `@MicronautTest` on-demand check |
| `ProductionSnapshotMigrationCheck` | **Opt-in, on-demand** (`-DprodSnapshot.check=true`, same flag as the recompute check): runs the real `DatabaseMigration` over a *copy* of the local production snapshot and holds every row of all three tables against a SHA-256 taken before it, so a migration that drops, reorders or rewrites a row fails instead of being noticed after the deploy. Also pins that every column comes out `NOT NULL`, that no `*_strict` table is left behind, and that a second run is a no-op. Run it before every deploy that carries a new migration. Measured 2026-09-07: 237,538 rows, digests identical, ~4 min on a slow bind mount. | on-demand check |
| `MetaSumComparisonTest` | Pure unit tests over the snapshot comparison tool: `StoredMetaSums` reads every `metaInformation` key of a database **read-only** and leaves out a key stored as `NULL`; `MetaSumComparison` calls a key unchanged when its *number* is unchanged even if its text differs, carries both sides plus their ratio for a changed one, reports no ratio where the stored value was `0`, and counts only the keys both sides hold. Hand-built maps plus one throwaway SQLite file under `build/tmp/test/` | pure unit |
| `ProductionSnapshotCheckOptInTest` | Pins the on-demand check's switch in one place: the build really forwards `prodSnapshot.check` into the test JVM (an absent property would leave the check unrunnable with nothing to distinguish that from a passing run), the check carries that same property as its `@EnabledIfSystemProperty` condition, and an ordinary run leaves it off | pure unit |
| `RateLimitCounterTest` | Pure unit tests for `RateLimitCounter`: the block lifts deterministically after `block-duration` (injected `Clock`, no `sleep`), stays active before expiry, and `isIdle()` answers the eviction question — true once the interval elapsed, false while it runs, false while a block is still active, true again once the block expired. The concurrency case is a **lost-update** test: 20 threads × 50 `increment()` calls must hand out 1000 distinct counts, and it fails without the `synchronized` counter. Concurrent `block()` calls on a frozen clock would prove nothing, since every thread writes the same instant. | pure unit |
| `RateLimitCountersTest` | Pure unit tests for the bounded per-IP map, which owns every `RateLimitCounter` and answers the whole throttle question in one call: with a budget of one request, the first is admitted and the second blocked, another client still starts fresh, an expired block lets the client back in, and the blocked client is logged once. The bound: an idle client is forgotten when a new one appears, a **blocked** client is not, the least recently used client goes once `max-tracked-clients` is reached (and counting a known client again makes another the oldest), and 100 distinct clients leave the map at its bound. Deterministic throughout: a frozen clock isolates the LRU rule, an advanced one the idle and block rules. | pure unit |
| `RateLimitFilterTest` | Boots the server in the `ratelimit` environment (`rebuildContext = true`) against its own fixture DB copy: `/favicon.ico` is blocked with 429 once the configured limit is exceeded, and so are `/`, `/index.html` and a **bundled** asset (resolved from the packaged `assets/` dir, not hard-coded) — token-free does not mean uncounted. A `//probe` is counted like any other path instead of being skipped as the SPA root, so its third request hits 429 rather than a third 401. A burst of malformed targets (`/overview%zz`, sent over `RawHttpClient`) is answered 400, 400, 429: the filters do not read the path, so an invalid escape is counted like anything else. The counterpart pins what the filters cannot see: an **oversized** target (5000 characters) answers 413 three times without ever earning a 429, the measured remainder of **C10**. | `@MicronautTest` integration |
| `RequestAuditLogFilterTest` | Pure unit tests over a `LoggerSpy` and a recording filter chain: one `info` line with client IP and user-agent per request, for the SPA shell and a bundled asset as much as for the API, never carrying the `?token=` query, and the request always proceeds. | pure unit |
| `SpaHistoryFallbackTest` | Boots the server against its own fixture DB copy and pins the fallback in **both** directions: an unknown navigation path, a client route carrying a dot (in a middle segment and in the last one) and each of the three dashboard deep links (`/overview`, `/avatars/{a}/bank\|storage`, `@ParameterizedTest`) return 200 with the byte-identical bundled `index.html` and `Cache-Control: no-cache`, while a missing asset (dotted **and** dotless), a missing swagger path, an unknown `/api` path and a traversal resolving below `/assets` keep the default 404. | `@MicronautTest` integration |
| `SpaNavigationPathsTest` | Pure unit tests for the rule behind that fallback: the SPA owns unknown navigation paths including dotted avatar names, but not the reserved mappings (`/api`, `/assets`, `/health`, the OpenAPI UIs) nor a path whose last segment carries a lowercase file extension; a prefix only matches on a segment boundary (`/apiary/tour` stays SPA-owned). | pure unit |
| `TokenScopeTest` | Boots the server and proves the default-deny scope over HTTP in both directions: `/api/v1/avatars`, `/overview`, `/avatars/{a}/bank`, an **unmapped** path (`/i_dont_exist`) and a SPA client route are all 401 without a token (and the API with a wrong one), while a tokened `/overview` asking for `text/html` is the 200 shell, and `/`, `/index.html`, a **bundled** asset resolved from the shipped `index.html`, `/swagger-ui/index.html`, `/health` and `/favicon.ico` answer 200 without one. Includes the traversal pair (`/assets/../overview` and its percent-encoded form are 401, not treated as assets) and the leading-`//` pair (`//overview`, `//api/v1/avatars` are 401, not read as an authority). Also pins the framework boundary over five malformed targets: they answer **400 instead of 401**, because the invalid escape breaks the request URI before any path-based decision exists, so the token filter cannot reject them. That is a **characterization test of Micronaut, not of our code** (our canonicalizer never receives a path), kept as the canary for a framework upgrade changing the behaviour. | `@MicronautTest` integration |
| `PublicPathsTest` | Pure unit tests for the default-deny matcher: an exactly configured path and anything below a configured `/**` are public; an unconfigured path, the API and a path that merely extends a configured one (`/healthz`, `/assetsomething`) are not; an empty **and** a missing configuration protect everything, so a misconfiguration fails closed. | pure unit |
| `PathCanonicalizerTest` | Pure unit tests for the one path form all path decisions share: resolves `..` (plain, percent-encoded, and hidden behind an encoded separator), decodes exactly once so `%252e` stays literal text, drops `.` and empty segments, clamps `..` at the root, drops a trailing slash, keeps a dot / decoded space / `+` inside a segment, does **not** let a leading `//` swallow the first segment, and keeps a segment with a malformed escape (`%zz`, a lone `%`) as literal text instead of throwing, so the function is total and every caller fails closed. | pure unit |
| `RawHttpClient` | Sends a request target byte for byte over a socket. Needed for exactly one case, measured rather than assumed: Unirest (and Apache HttpClient under it) **refuses** a malformed percent-escape client-side with `IllegalArgumentException`, so `/overview%zz` cannot be sent through it at all. A leading `//` needs no raw socket, Unirest sends that unchanged. Its read timeout is a hang guard, never a correctness condition. | helper (no `@Test`) |
| `AvatarSummariesControllerTest` | Pure unit tests over the stubbed repositories: the overview lists an avatar that only ever moved items, `totalCount` counts every known avatar while a page shows only part of them, an avatar without any stored sum carries zeros, a seeded avatar's four ledger sums plus the derived net are served as whole gold, the envelope's `totals` cover **every** known avatar rather than only the served page, and each summary carries the last activity of both ledgers. Staleness on the wire: a refreshed row serves no instant, a row the last collection missed serves the instant its sums come from, and `totals.containsStaleSums` states that the guild contains such a row even on a page that does not show it. Uses `KnownAvatars` over the two repository stubs plus `FakeMetaInformationRepository`. | pure unit |
| `AvatarSummariesEmptyStateTest` | Boots the server against an **empty** database with the collector disabled and asserts the overview body byte for byte: `{"page":0,"size":100,"totalCount":0,...,"items":[]}`. The one test that proves the `jackson.serialization-inclusion: ALWAYS` contract, i.e. that "no run yet" stays a visible explicit `null` and an empty page a visible empty array rather than missing keys. The pinned body carries `totals`' `containsStaleSums: false`, so the flag is proven present rather than omitted on a store that never ran. | `@MicronautTest` integration |
| `RenameSafetyTest` | ArchUnit guard, three rules: every non-static field under `rest/controller/api/wire` carries `@JsonProperty`, every `@DatabaseField` declares a non-empty `columnName`, every `@DatabaseTable` a non-empty `tableName`. Turns "a rename must never change a published contract or the schema" into a build failure; the column rule found the two `id` fields that relied on the field-name default. | architecture guard (ArchUnit + JUnit 5) |
| `SpaBundlePackagingTest` | Guards that `/static/ui/index.html` is on the test runtime classpath, i.e. the SPA bundle really is packaged into the jar by `processResources`. | pure unit (packaging guard) |
| `RateLimitStartupValidatorTest` | Startup fails, naming the offending property, for each value that would silently disable the throttle: a request budget of `0`, a zero or missing `interval`, a zero or negative `block-duration`, and a client budget of `0` or negative. The working configuration starts up and the `onApplicationEvent` entry point propagates the same failure. Keeps the config guard out of the constructor (handbook §3), the way `ApiTokenStartupValidator` does. | pure unit |
| `ApiTokenStartupValidatorTest` | Startup fails with the property name (`evergore.security.api-token`) in the message for an empty, blank and `null` token; a set token starts up; the `onApplicationEvent` entry point propagates the same failure. | pure unit |
| `CredentialsStartupValidatorTest` | Startup fails with the property name (`evergore.credentials.username`/`.password`) in the message for an empty, blank and `null` username or password; a set pair starts up; the `onApplicationEvent` entry point propagates the same failure; the logged error names the environment variable that sets the value and never the value itself. One test boots a **real `ApplicationContext`** with a blank login, so the listener wiring is pinned too, not just the validator method: a lost `@Singleton` would fail it. | pure unit + one real context boot |
| `TimezoneStartupValidatorTest` | Startup fails naming the zone when the injected `ZoneId`'s rules are not a fixed offset (`Europe/Berlin`); a fixed-offset zone starts up whether it is `UTC` or another fixed offset (`Etc/GMT-2`), proving the check is "fixed offset", not "must be UTC"; the `onApplicationEvent` entry point propagates the same failure; the logged error names the zone. One test boots a **real `ApplicationContext`** under the real system default zone (UTC in this environment), pinning the `ApplicationFactory` wiring of the effective zone. A second boot test replaces the injected `ZoneId` bean with a DST-observing one (`Europe/Berlin`, via a `spec.name`-guarded `@Replaces` test factory) and asserts the boot itself fails naming the zone, so the reject path is proven through the real bean graph too, not only via a hand-constructed instance. Interim safeguard for **D14** (backlog **D22**). | pure unit + two real context boots |
| `BootSignalRecorderTest` | `awaitCollection()` unblocks both on `recordCollectionFinished()` and on `recordException()` (real threads, no timeouts), and the `dataLoaded`/`exceptionOccurred` queries flip false→true. Pins the boot-signal seam itself. | pure unit (concurrency) |
| `NoApplicationTextInHttpStatusTest` | ArchUnit guard, one rule: no class calls a reason-phrase-carrying `status(…)` overload. Covers all **six** that micronaut-http's response API offers, enumerated from the jar: `HttpResponse.status(HttpStatus\|int, String)`, `MutableHttpResponse.status(HttpStatus\|int, CharSequence)` and the `HttpResponseFactory.status(HttpStatus\|int, String)` the static helpers delegate to (missing that pair leaves a reachable bypass). `HttpStatus` is an enum and has no reason setter, so those six are the complete set. Each leg verified non-vacuous by a temporary probe calling all six; the rule flags every call by file and line. | architecture guard (ArchUnit + JUnit 5) |
| `BankEntryEqualityTest` / `StorageEntryEqualityTest` | Value equality of the two entry records: equal when all fields match, different for each single field in turn (timestamp, avatar, amount/quantity, name, quality, transfer type). The window-dedup in `EvergoreDataExtractor` compares entries by value, so this is load-bearing, not record boilerplate. | pure unit |
| `KbCitationGuardTest` | Guards every `docs/knowledge-base/*.md` file for a phantom backtick class reference: extracts candidate class names per `KbClassReferenceExtractor`'s inclusion rules (a compound-PascalCase, ≥2-hump token; a bare single-word class citation like `Configuration` is a disclosed blind spot the rule does not cover), resolves each against `KnownJavaSymbols`' classpath-backed lookup (the project's own compiled classes, every dependency jar, and the JDK, read from the test runtime classpath by listing class-file/module entry names, no bytecode parsing), and fails naming every unresolved `file:line: token`. Verified non-vacuous by injecting a temporary phantom reference into a `@TempDir` fixture. Scoped to Java `src/` references only: the documented React/TypeScript frontend symbols the extractor's rules still pick up in `frontend.md` never resolve against that scan, so they are named in an explicit disclosed-gap set (itself pinned to only ever accept `frontend.md`-rooted entries) rather than silently swallowed by skipping the file. | architecture guard (ArchUnit + JUnit 5) |
| `SeleniumPageSourceTest` | Drives `SeleniumPageSource` against a `RecordingWebDriver` fake, using **injected `Clock` and `Sleeper`** so wait timeouts never touch real time: the driver is quit after a successful scrape **and** after a failing one (try/finally), the scrape failure propagates and is logged, both-fail contract is pinned (scrape failure propagates; both failures logged), and a timeout waits deterministically without real-time dependency. The login is covered too: the configured username and password reach the login form's fields, and a failing login logs exactly one `warn` that names the login and carries neither credential. | pure unit (fake driver, injected clock/sleeper) |
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
- `SmokeTest` awaits it in `@BeforeEach` too, for the tests that seed rows: the collector's **full
  recompute overwrites the meta sums**, so a seed written while the run is still going would be
  replaced, and the order of the two writes is otherwise undefined.

## Coverage map

**Has tests:** `EvergoreItem` (value math per item kind **plus** two all-items golden-master rules over the whole catalog) · `MetaInformation` (serialization) ·
`EntryFactory` / `EntityParser` (the parsing contract: headline→entry, type mapping, quality defaulting, `+1` merging, quantity merging, `Impressum` terminator, malformed/out-of-range dates) · `EvergoreDataEvaluator` (bank aggregation, storage valuation, unknown item fallback + per-occurrence counting, full-recompute overwrite + idempotence + failed-run self-heal, avatar union, Berlin wall-clock `last_updated`) ·
`EvergoreDataExtractor` (parse→persist pipeline, window dedup via `getAllSince(min scraped timestamp)`: heals a still-visible entry missing from the database, no-duplicate + surplus-only dedup, oldest-first partial-batch safety) ·
`BankDatabaseRepository` / `StorageDatabaseRepository` (`getAllSince` inclusive boundary, `countFor` per avatar, a file database) ·
the **evaluate→overview pipeline end-to-end** via `ProtocolEvaluationAcceptanceTest` (the JSON contract byte-exact, plus its paging and error cases) ·
`LastRunStatus` (record + read, incl. unknown item names) · `LastRunHealthIndicator` (UNKNOWN / UP + detail, incl. the unknown-item detail) ·
`EvergoreDataCollectorJob` (records run on success, not on failure; forwards unknown items) ·
`/health` endpoint + wrong-token rejection via `HealthEndpointTest` ·
the **token scope in both directions**: the default-deny rule itself (`PublicPathsTest`) and over HTTP incl. the traversal pair and an unmapped path (`TokenScopeTest`), on the canonicalized path (`PathCanonicalizerTest`) ·
the **rename safety** of the JSON contract and the DB schema (`RenameSafetyTest`) ·
`TransferType`→German mapping (`toGermanString`) · `ApplicationExceptionHandler` exception→HTTP visitor dispatch + token-free logging + severity by mapped status +
the reason phrase staying free of the requested value, through the real Netty write (`ApplicationExceptionHandlerHttpTest`) ·
`RateLimitCounter` (no lost counts under 20 threads + deterministic expiry + the idle rule), the bound on the counter map (`RateLimitCountersTest`), the limit applying to every path (`RateLimitFilterTest`) and the audit line for every request (`RequestAuditLogFilterTest`) ·
`ApiTokenStartupValidator` (startup aborts on an unset/blank token), `RateLimitStartupValidator` (startup aborts on any rate-limit value that would disable the throttle), `CredentialsStartupValidator` (startup aborts on an unset/blank Evergore login, pinned through a real context boot) and `TimezoneStartupValidator` (startup aborts on a DST-observing effective zone, D22) ·
the SPA seam: history fallback vs. 404 in both directions (`SpaHistoryFallbackTest`, `SpaNavigationPathsTest`), bundle packaging (`SpaBundlePackagingTest`) and the bundled SPA painting real API data in a real browser (`DashboardBrowserSmokeTest`) ·
`BankEntry`/`StorageEntry` value equality (load-bearing for the window dedup) ·
`SeleniumPageSource`'s driver lifecycle (quit on success and failure, failure logging, deterministic wait timeout via injected clock/sleeper) and its login against the fake driver (the configured credentials reach the form; a failed login warns without leaking them) ·
`BootSignalRecorder` (the boot-signal seam itself) ·
and *indirectly* via `SmokeTest`: controllers, filters, repositories, the job.

**Most important UNTESTED logic:**
1. **`SeleniumPageSource`'s scrape itself**: navigation and pagination against the live site
   (inherently hard), and whether the login the form submits is actually *accepted* there. The driver
   *lifecycle* and the login *form interaction* around it are covered by `SeleniumPageSourceTest`
   against the fake driver; only the live round trip is not.
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
but **opt-in**, because the snapshot it needs carries guild members' data (PII) and stays
**gitignored**. See the next section.

## 1:1 against the production instance

The release gate before deploying: read the **same database** through the candidate build and
through the live instance, then compare the numbers per avatar. It catches drift that the synthetic
fixture cannot, because it uses the real data volume, the real item mix and the real avatar set.

The two sides no longer speak the same format: the candidate answers JSON, the live instance still
renders HTML. So the comparison is **value-wise, not byte-wise** (decision 2026-08-08); a byte diff
was possible only while both sides rendered the same pages.

### Procedure

1. `./gradlew installDist`.
2. Run the distribution from an **isolated working directory** holding a *copy* of the production
   snapshot at `database/temp.sqlite`, with `EVERGORE_SECURITY_API_TOKEN` set. Never point a run at
   the original snapshot file: a first run recomputes the meta sums in place and is not reversible.
3. Fetch `/api/v1/avatars?size=1000` plus `/api/v1/avatars/{avatar}/bank|storage` for every avatar
   from the candidate, and the matching `/overview` and `/avatars/{avatar}/bank|storage` pages from
   the live instance.
4. Compare **per avatar**: the bank totals (`bankWithdrawn`/`bankDeposited` against the overview table's
   "Entnommen"/"Eingelagert" cells) and each ledger's entries (timestamp, amount or
   quantity/name/quality, transfer direction). Explain every difference; a recompute delta is
   expected and is validated against the independent SQL recomputation below, never waved through.
5. The status contract differs by design and is **not** compared: an empty page is 200 with
   `totalCount: 0` on the candidate and 404 on the live instance (the 404-vs-empty decision in
   [frontend.md](frontend.md)), and `/health` is anonymous only on the candidate.

- **Pace the sweep.** The live instance enforces 5 requests per 10 s and then blocks the client IP for
  1 minute; the candidate allows 30 (`evergore.rate-limit.*`), so pace by the stricter live side. The
  counter runs on blocked requests too, so a renewed **burst** inside a block extends it (a single retry
  per window does not, the interval reset zeroes the count first): leave at least 4 s between requests
  and back off well past a minute after a 429.
- The scrape branch cannot run in the devcontainer (no Firefox binary). Micronaut's task exception
  handler catches it, the app keeps serving, and the database stays untouched, so the check is
  unaffected.

### Parity evidence on record

The evidence is one full sweep over the whole avatar set, 165 responses per side, against one
identical snapshot, taken while both sides still rendered HTML:

- Every status code matches, including the 404s from the empty page-1 requests.
- `/overview` is byte-identical after LF normalisation.
- The detail pages carry **one** differing line each, the same line in all of them: the live
  instance ships the client-side paging script with the literal placeholder `?token=secret_token`,
  the candidate reads the token from the current URL. That is the intended fix that came with the
  config-driven API token. No rendered data differs.

What it does **not** cover: the JSON surface, because a byte diff was only possible while both
sides rendered the same pages.

**Value-wise sweep, JSON vs. HTML (2026-08-16, the `0.1.0` release gate).** Candidate = the release
image on a copy of the production snapshot; live = the old stand on the home server. Five avatars,
chosen for what the synthetic fixture cannot reach: the largest ledger pair (`Feonir`, 14.4k
entries), the **storage-only** avatar (`Gauß`, the 42nd in the union), an umlaut name with data on
both ledgers (`Zwölf`), a name with spaces whose storage meta is exactly `0.0` despite 1653 rows
(`Thyla Vom Moos`), and the asymmetric meta case (`Valtan Glutherz`, placement `0.0` / withdrawl
`354000.0`).

- **Every entry matches**, in both directions, for all seven fully compared ledgers (bank 850/43/461/1,
  storage 2019/1653/3): timestamp, amount or quantity/name/quality, and direction, as multisets.
  `totalCount` matches the live row count for each of them.
- The two ledgers too large to fetch whole (`Feonir` storage 136 pages, `Zwölf` storage 79) were
  **sampled** at first, middle and last page: all 537 live rows are present in the candidate.
- **The wire mapping is pinned by real data:** the candidate's UTC instant converted to
  `Europe/Berlin` equals the live wall-clock string exactly, `DEPOSIT`/`WITHDRAWAL` equal
  `Einlagerung`/`Entnahme`, and both sides sort newest-first.
- **The recompute delta reproduces:** 31 of 41 overview rows changed, **every one downward**, 10
  identical — the same shape as the measurement below, on which the ship-the-lowered-numbers
  decision rests.
- The live instance's own drift (it kept scraping) turned out to be **zero** here: both sides had
  ingested the same new entries, so the comparison holds with and without a cutoff at the snapshot's
  newest timestamp.
- Not covered: the other 37 avatars, and the interior pages of the two sampled ledgers.

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
  Measured on the 03.09.2026 snapshot: **124 of its 513 distinct storage item names**, covering
  **1 684 of 243 443 rows** (0.69 %) and 129 062 units moved. Backlog **D23** holds the question of
  which of them should carry a value at all.
- **Not covered by this check:** the re-ingest of still-visible entries missing from the database
  needs a live scrape, so it is only exercised by `EvergoreDataExtractorTest`.

### Does a production database reproduce its own stored sums?

Measured 2026-09-04 with the opt-in check, once per local snapshot, each recomputed **from its own
rows**. The recompute is idempotent, so the code is its own reference here; the
committed comparison is `MetaSumComparison` over the `metaInformation` table, stored value against
recomputed value per key.

| snapshot | `last_updated` | meta keys changed | avatars matching on all four sums |
| --- | --- | --- | --- |
| `database/temp.sqlite` | 16.06.2026 | 113 of 168 | 0 of 42 |
| `temp.sqlite.bak-20260816` | 15.08.2026 | 114 of 168 | 0 of 42 |
| `temp.sqlite` | 03.09.2026 | **0 of 168** | **42 of 42** |

- The `0.1.0` deploy is 2026-08-16. Both snapshots that diverge were taken **before** it; the one
  taken **after** it reproduces every stored sum exactly. Only `last_updated` moves there, which the
  recompute rewrites. That is what proves the recompute ran rather than the diff being vacuous.
- The pre-deploy divergence is a **frozen absolute amount, not a growing one**: `Fugger`'s
  `storage_withdrawl` sits `+27 922 355.4` below the recompute in *both* pre-deploy snapshots, two
  months and 10 000 storage rows apart. It is `0` in the post-deploy one.
- Same shape as the delta above: in the 15.08 snapshot bank deposits changed for 31 of 42 avatars
  and **every changed one moves down**, by at least 0.5 % (ratios `0.722` to `0.995`; the other 11
  are untouched at exactly `1.000`). Storage withdrawals changed for all 42.
- **The eight avatars stored at `0` beside real ledger rows share one trait, without exception:**
  their last storage row is in **2023** (`Gauß` 2023-01-24, `Ivory` and `Malak Almawet` 2023-05-08,
  `Mightypanda` 2023-06-07, `Maesch` 2023-06-19, `Koma` 2023-08-15, `Thyla Vom Moos` 2023-10-24,
  `Elazia Kapp` 2023-12-01), while all 34 avatars with a non-zero storage sum have theirs on
  **2024-04-22 or later**. Both storage sums are `0` for all eight; their bank sums are not (bar
  `Gauß`, who has no bank rows), so it is a storage-side condition. In the post-deploy snapshot all
  eight carry exactly the values the recompute yields. The ninth `storage_placement` zero
  (`Valtan Glutherz`, rows through 2026-08-14) does **not** share the trait and stays 0 on both
  sides.
- **Not measured, and not claimable from this:** whether the recompute computes the *right* number.
  Code and store carrying the same error would leave this diff empty by construction. That is
  **B19**'s purpose and stays a separate item. The catalog gap above is a known instance: both sides
  value an unknown item at zero, so it reproduces perfectly and this check stays silent on it.
- **Not reproducible:** the per-avatar ratio band of `0.12`-`1.33` first reported from the 31.07.2026 file. No quantity tried (per key, per
  family, gross, deposits, withdrawals, net) yields a `0.12` lower bound on any surviving snapshot;
  per-avatar `net` ratios are unbounded because `net` crosses zero. The file that band came from
  (`last_updated` 31.07.2026) is no longer on disk, the root snapshot having been replaced on
  2026-09-03, so that band cannot be re-derived. Its Fugger figures do reconcile:
  `66 141 289` → `94 063 645` is
  the **same** `+27 922 356` gap measured above.

### The production-snapshot harness

`ProductionSnapshotRecomputeCheck` boots the real context against a **copy** of the snapshot, stubs
the scraper, and lets the real `EvergoreDataEvaluator` run through the scheduled job, mirroring
`ProtocolEvaluationAcceptanceTest`'s mock-bean set. It writes the recomputed summaries to
`build/tmp/prodSnapshot/overview-after-recompute.json`, which is the candidate side of the value
comparison, and exports the valuation catalog, which is what makes the storage cross-check above
possible.

- The class itself holds **no production data**: the PII sits in `temp.sqlite`, which stays
  gitignored. So the harness is committed and reviewable, and only the snapshot it feeds on is
  local.
- It is **opt-in, not disabled**: `@EnabledIfSystemProperty(named = "prodSnapshot.check", matches =
  "true")`. Without the opt-in it has nothing to run against, and a check that passes on one machine
  only must not become a build gate; with it, nothing has to be edited to take a measurement.
  `tasks.withType<Test>` forwards `prodSnapshot.check` **always** (defaulting to `false`) and
  `prodSnapshot.file` only when given, so a missing forward cannot masquerade as a passing run.
  `ProductionSnapshotCheckOptInTest` pins that. It still compiles under `-Werror`, so a refactor
  cannot rot it unnoticed.
- **To run it:**

  ```bash
  EVERGORE_SECURITY_API_TOKEN=test-token ./gradlew test \
      --tests '*ProductionSnapshotRecomputeCheck*' \
      -DprodSnapshot.check=true -DprodSnapshot.file=temp.sqlite
  ```

  `prodSnapshot.file` defaults to `temp.sqlite` and takes any path, so a second snapshot is measured
  by naming it, not by editing the check. Artefacts land in `build/tmp/prodSnapshot/`. Opting in
  without a snapshot at that path fails on the missing file instead of passing empty.
- It writes only under `build/`, never to the snapshot. Always copy the snapshot; never open the
  original read-write.

## Test execution model (one JVM)

The whole suite runs in a **single test JVM**. `tasks.test` sets no `forkEvery`, and a
configuration-time `check` fails the build if it is ever set again.

- **Nothing needs the isolation.** There is no shared mutable state across test classes: every
  `@MicronautTest` overrides `Configuration.getDatabasePath()` in its own `@MockBean` subclass and
  owns a private SQLite file under `build/tmp/**`, and boot signals travel through the DI-scoped
  `BootSignalRecorder` (above), not statics. `Configuration.DATABASE_TEMP_SQLITE` is public and
  mutable but is an *instance* field of a `@Singleton` that nothing writes.
- **What forking cost.** Gradle hands every non-anonymous **class file** of the test source set to
  the test-class processor, so `forkEvery = 1` restarted the JVM once per class file — 198 of them,
  only 28 holding tests. `--tests` does not reduce that count: it filters inside the worker at JUnit
  discovery, so one focused test still paid all 198 boots (19m33s for an 8.1 s test), and the full
  suite took ~9 min for ~31 s of actual test time.
- **Why a guard.** Per-class forking has twice been added as a crutch for a startup race that was
  already fixed elsewhere, and twice removed once the suite was proven green in one JVM (decisions
  2026-06-20, 2026-08-01). The check is configuration-time on purpose: `forkEvery` is not a tracked
  task input, so a re-add leaves `test` UP-TO-DATE and an execution-time check would never run.

## Proving a run really executed

The gateway build is **`./gradlew clean build --no-build-cache`**. All three parts are load-bearing:

- **`clean` alone does not force execution.** It deletes the outputs, but `org.gradle.caching` is on
  and the local build cache is shared by every worktree (build-run-deploy.md), so Gradle restores
  `:test` and `:frontend:npmTest` `FROM-CACHE` and *rewrites the result XMLs from that entry*.
  Measured 2026-08-16: exit `0`, a full-looking `build/test-results/test/`, and not a single test
  executed. Neither the exit code nor the XML count can tell that run from a real one.
- **`--no-build-cache` is the mechanism that forces execution**; the task lines are the evidence.
  A run counts only when the lines for `:test` and `:frontend:npmTest` stand **bare** — no
  `FROM-CACHE`, no `UP-TO-DATE` marker — and the XML count under `build/test-results/test/` is read
  alongside them.
- **Why it matters beyond bookkeeping:** the load-sensitive failures (the Vitest worker starvation in
  [frontend.md](frontend.md), backlog **B20**) only appear when the suites really run, and a cache
  hit hides exactly that class reliably. Two consecutive runs stay the bar for a load-sensitive
  change, and a cached second run is not one of them.

## Testing direction for the rebuild (TDD/BDD)

- **The unit under test is named `tested`** (author decision 2026-08-05), one name across the whole
  suite, so every test reads the same way. Older classes still use their own names; **B8** carries the
  repo-wide rename.
- **A behaviour is pinned once, at one level** (author decision 2026-08-05). Duplicate coverage is
  maintenance cost, not safety: when a dedicated class takes a behaviour over, the older ad-hoc
  assertions about it are removed **in the same change**, and an assertion belongs only in the class
  whose subject it is. A token-rejection check inside the rate-limiter's test is misplaced even when it
  passes.
- **Tests rank above production code** (author decision 2026-08-04), so they get the greater care of
  the two. Every test is strictly **arrange / act / assert** with the act as its own named value:
  `int status = statusOfGet(path);` then assert on `status`, never `assertThat(get(path).getStatus())`.
  A custom assert helper (`assertCanonical`, `assertPublic`) is the other allowed shape, and it holds
  the same structure inside. Where several inputs share one behaviour, use `@ParameterizedTest`
  (`junit-jupiter-params`) rather than repeating the assertion.
- **The test name carries the intent, not an assertion message** (author decision 2026-08-15).
  `.as(...)`, `.describedAs(...)` and their equivalents are for the exception: an assertion whose
  failure output would leave the reader guessing which of several inputs failed, or one that proves
  something by absence. Everywhere else the name states the behaviour and the message is redundant
  narration. A message that says something the name does not is a name that needs rewriting.
- **BDD (PO perspective):** capture the use cases as scenarios, e.g. *"Given a member deposited
  N gold and crafted items worth M, when I view the overview, then their guild value is N+M."*
  The whole collect→evaluate→overview flow is now covered by `ProtocolEvaluationAcceptanceTest`
  (scraper stubbed, real evaluation, asserted via HTTP + the meta repo).
