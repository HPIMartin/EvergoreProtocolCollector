# 06: Testing

## Inventory

| Test | Scope | Style |
|------|-------|-------|
| `SmokeTest` | Boots the real Micronaut `EmbeddedServer`; mocks Selenium (`TestEvergoreDataExtractor extends EvergoreDataExtractor` with `super(null,…)`); points DB at `build/tmp/smokeTest.sqlite` (under the build directory, so rewriting it per run cannot invalidate the `processTestResources` inputs); **resets that file, and its `-journal`/`-wal`/`-shm` sidecars, from `TestConfiguration`'s static initializer**, next to the one place that names the path; sets job delay 0; asserts app starts + the job runs, a row seeded through each repository reads back byte-exact through `/api/v1/avatars` & `/api/v1/avatars/{a}/bank\|storage` (one avatar name per test, so the class is order-independent), unknown path → 4xx, and that the `PageSource` bean resolves to the `SeleniumPageSource` adapter. `@MockBean` for `Configuration`, the hooks, task-exception-handler. | `@MicronautTest` integration / smoke |
| `DashboardBrowserSmokeTest` | The one test that joins the two halves: boots the real server against the fixture DB (real evaluator, stubbed scraper) and drives **headless Firefox** at `/overview` and at a `/avatars/{a}/bank` **deep link**, then reads the rendered rows back through the SPA's `data-testid` hooks. Pins that the bundled SPA really reaches the API and paints its data: the overview shows all three avatars with de-DE grouped totals (`1.500`), its stat header states the guild's four figures (`1.750`, `822`, `120`, `0`, each derived from the fixture rather than from the served header), and the ledger shows its entries newest-first with Berlin wall-clock timestamps and the German transfer names. Everything else proves one half only, since the Vitest suite fakes `HttpGet` and the Java suite stops at the JSON. `assumeTrue`-skipped where no browser is on the `PATH`, like `DockerBrowserSmokeTest`. The `WebDriverWait` is a **hang guard, not a correctness condition**: it waits for a rendered row to exist, so slow hardware waits longer and a never-rendering page fails instead of passing. Registers the orphaned-browser sweep, like every class here that starts a browser. | integration (real browser + real server) |
| `DockerBrowserSmokeTest` | Starts the browser for the **configured** mode (`Configuration.browser` → `Browser.fromString`), loads a `data:` URL and reads an element back, so the scrape path's driver half is covered without the network or the live game. Selenium Manager resolves the driver itself (`~/.cache/selenium`, geckodriver 0.37.1 against Firefox ESR 140); a browser on the `PATH` is the only prerequisite. `assumeTrue`-skipped where none exists — the production image's build stage runs `check` on `eclipse-temurin:25-jdk` and must not gain a browser dependency. Registers the orphaned-browser sweep, like every class here that starts a browser. | integration (real browser, no server) |
| `OrphanedBrowserSweepTest` | Pins `OrphanedBrowserSweep`, the JUnit extension `DashboardBrowserSmokeTest`, `DockerBrowserSmokeTest` and `GameCatalogScrapeCheck` register and the acceptance runner's `@BeforeAll` hook calls: before a class starts a browser of its own it kills the geckodriver and Firefox processes of runs that are gone, then fails that class naming each one with its pid and age. A terminated JVM runs no `finally`, so an interrupted run leaks its browser and nothing says so; eleven such runs held 11.9 GB of the container's 15.9 GB while every later build started a browser of its own and passed. **Ancestry decides, not the process name**: geckodriver is a child of the JVM that started it and Firefox a child of geckodriver, so a browser with no living JVM among its ancestors belongs to no running test, and a run started beside this one is left alone. A name pattern cannot manage that, and carries a trap of its own: `pkill -f firefox-esr` also kills the shell running it, because the pattern sits in that shell's own command line (`[f]irefox-esr` does not). The cases plant a copy of `sleep` named `geckodriver`, detached through `setsid --fork` for the leftover and started as a direct child for the browser in use, so the two plants differ in ancestry alone. The failure is self-clearing: it fires once, on the run that finds the leftover, and the run after it is green. | integration (real processes, no browser) |
| `MetaInformationTest` | `MetaInformation<T>` delegates serialize/deserialize to its `MetaInformationKey<T>`. | pure unit |
| `MetaInformationKeyTest` | Pins the `sums_recomputed_at_<avatar>` key: its id, and that it serializes an `Instant` as **epoch millis** and reads that instant back unchanged. The load-bearing case is the Berlin **fall-back hour**: two instants an hour apart that share one wall-clock text serialize differently and read back distinctly, which is exactly what `last_updated`'s wall-clock format cannot do. | pure unit |
| `EntryFactoryTest` | `deduplicates()`: 3 raw lines collapse to 1 item by name+quality; a timestamped headline with no known transfer type is dropped **with a warning**. | pure unit (thin) |
| `EntityParserContractTest` | The parser contract, pinned line by line: headline → entry, `Einzahlung`→`EINLAGERUNG`, withdrawal entries, quality defaulting to 100 when absent, the value-neutral `+1` modifier (ignored, and merged with its unmodified counterpart), quantity merging for same name+quality, separation when quality differs, the `Impressum` terminator, one entry per headline, empty protocol → no entries, and the malformed/out-of-range-date paths (skipped without aborting the ingest, neighbouring entries still parsed, the malformed headline's items **not** folded into the preceding entry, a headline with a single-digit day, month, hour or minute dropped with a warning in the same way, one parameterized case per field, garbled separators and a headline with nothing after its time, while an item line that reads like a date and time (`2000 2026 10:10`, `5 5.2026 1:30 …`) stays an item of its entry and a line shaped almost like a headline (an empty or 3-digit field, a 3- or 5-digit year, a missing or wrong separator before or in the time) opens no entry and is not logged, a warning for **every** dropped block head, whether it fails on the transfer type or on the strict headline match), and the unparseable-item-line path (an amount or quality that is no parseable number skips **that line only**, with a warning; the entry's other items and the neighbouring entries survive), the no-parseable-items path (an entry whose item lines all fail the item regex warns), and the avatar/type-word path (one parameterized case per headline shape: an ASCII-letter suffix, an umlaut suffix, punctuation fencing the type word on both sides, and a headline with no separate type field at all, each minting no entry and warning, while a legitimate avatar name containing a type word still parses). | pure unit |
| `EvergoreItemTest` | The valuation rule, one case per credit tier (mined, hunted, gems, trader goods, the ammunition the game's own trader sells, ammunition that can only be crafted, crafted, processed raw material), plus an all-items golden-master rule: a withdrawal costs 0.6 of market value for **every** item. Which entries credit in full is pinned by name across the catalog: the goods bought from the guild trader and the three sorts the game's own trader sells, so no other entry can inherit the full credit. Three round-trip cases pin what taking an item out and putting it straight back does, including the 40 % a trader good would earn. The guild's two announced worked examples are their own named cases, read from the recipes: 135 `PFEILE` gain 258 (the announcement's 96 credits them at 60 %), 5 `EISENBARREN` gain 120. Two cases pin the raw stones against the names the game uses and the prices the wiki table records, and `noTwoCatalogEntriesClaimTheSameName` holds the invariant the lookup needs, which until now only the opt-in production-snapshot check asserted. Catalog-wide rules guard the recipe graph on four fronts: what a recipe may be, what an entry may be worth, what a practice piece consumes, and whether a missing ingredient list means the game does not craft the item or that nobody has read it. On recipe shape, every entry either names what it consumes or says why it cannot, a recipe is unrewritable once published so no reader can poison the catalog for every other test sharing the JVM, and the number of production chains is pinned, so satisfying a rule by deleting a recipe cannot pass unseen. On value, only the deliberately worthless are craftable for nothing, nothing the value guards exclude carries a price of its own, no item whose value is meant to reflect its inputs is worth less than the ingredients its recipe consumes, every craftable is credited at least what its withdrawal charges, and only gathered goods sit in a category that credits a deposit nothing. The practice-piece front exists because the value rules deliberately exempt the deliberately worthless, so without it nothing would judge a practice piece's ingredients at all: each names the material it is trained on, is trained on material its own craft also uses, yields a single piece, and costs its apprentice exactly what withdrawing that material charges. The unread-recipe front pins both the entries that say so and how many there are, and that the two sentinels are types of their own rather than two equal instances of one class. | pure unit |
| `HexagonalArchitectureTest` | ArchUnit guard, four rules: `domain`+`businessLogic` depend on **no** framework/library packages (Micronaut, jakarta, Selenium, ORMLite/SQLite, Jackson, RxJava, Apache Commons, logback/SLF4J, Netty); the `application` use cases stay framework-free too; `application` depends only **inward**; the core (`domain`+`businessLogic`) likewise, and additionally not on `application`. Turns the hexagonal golden rule into a build failure (each rule verified non-vacuous by a temporary deliberate violation: forbidding `java.time` flags 22 core usages, a class literal from `rest.filter` in `Item` fails the core's inward-only rule). | architecture guard (ArchUnit + JUnit 5) |
| `GermanOrderTest` | Pure unit tests for the project's one German collation: an umlaut name sorts where German puts it (`Äxtchen` before `Zunder`), a name occurring twice is listed once, no names give an empty list, an own collator (`ownCollator()`) orders names with hyphen, space and apostrophe as the shared order does, and every call gives a collator of its own, so no two connections queue on one (verified by recorded mutations: Swedish rules, one shared collator). | pure unit |
| `RoundTripOrderTest` | Pure unit tests for the by-avatar-then-item order of round trips and abstentions, pinned with an umlaut avatar (`Ärger` before `Zorn`); the admin page and the health report both use it. | pure unit |
| `AdminStatusControllerTest` | Pure unit tests for the admin status wire: name lists in German order and each name once, round trips and abstentions by avatar then item in German order. | pure unit |
| `KnownAvatarsTest` | Pure unit tests for the single definition of "an avatar the service knows": both ledgers' avatars as one list sorted by **German collation** (`Anna` before `Ärger` before `Zorn`, so an umlaut name does not land behind `Z`), an avatar living in both ledgers named once, both sides sorted **together** rather than the storage side appended, and nobody while neither ledger has rows. Hand-written fakes: `BankRepositoryStub`, `StorageRepositoryStub`. | pure unit |
| `ContributionTest` | Pure unit tests over the four ledger sums and the goods value beside them: `net()` reproduces the **verified** sheet column 5 for the five member rows [google-sheet.md](google-sheet.md) documents (`@CsvSource`, one case per member), `sumOf` adds a collection of contributions into the guild's own and answers `NOTHING` for none at all, and `inWholeGold()` rounds every figure to whole gold once from its exact value, halves away from zero: the net, the storage value and the figure before the guild's share from the exact sums, never from the rounded ones. `GuildShare` carries the two flows between what a deposit credited and what it is worth to the guild, is absent while no recompute has produced them, and a guild total is absent as soon as one avatar's is (an empty guild still answers a share of nothing, which is a sum over no summands rather than an unknown one, and a case tells the two apart). No framework. | pure unit |
| `AvatarContributionsTest` | Pure unit tests over the assembler: all four stored sums of a known avatar are read, an avatar without all four stored sums keeps his entry but has no contribution and no stale instant rather than zeros (whichever of the four is missing, and even when a recompute instant is stored for him, which then dates the last collection for nobody, however new), `total()` is empty while one avatar has none and the sum of all of them otherwise, the German collation order of `KnownAvatars` is kept, and each ledger's newest entry becomes that avatar's last activity while a ledger he never used stays unanswered. Hand-written fakes: `BankRepositoryStub`, `StorageRepositoryStub`, `FakeMetaInformationRepository`. Also pins that one call reads the store through exactly **one** snapshot, and the staleness read on top of it: the newest per-avatar recompute instant is the last collection, a row older than it answers the instant its own sums come from, a row at it answers none, an avatar carrying no recompute instant at all answers none, and over the guild `containsStaleSums` is true exactly when one row lags | pure unit |
| `RoundTripDetectorTest` | Unit tests over `RoundTripDetector.detect`: a trader good withdrawn and deposited again inside the 48-hour window is reported; only the overlapping quantity is reported whichever side is smaller; a deposit exactly at the window boundary still matches, one minute later does not; a deposit matches the oldest open lot still inside the window rather than a newer or an expired one, which a second deposit pins: what the first left of the oldest lot expires, so neither a deleted expiry nor newest-first matching reaches the asserted total; items and avatars are kept apart; a same-minute withdrawal and deposit count as a round trip whatever their order in the input list; the three ammunition sorts the game's own trader sells are watched like trader goods, while a raw material and an ammunition that can only be crafted are never reported. A deposit of one of the three answers its own sort's open withdrawal first, and only the quantity beyond it counts as crafted and uses up its recipe's trader goods; an open withdrawal of one of them is abstained like any trader good when an unread-recipe deposit arrives. The crafting carve-out: a deposited product whose published recipe consumes the watched item uses up that much of an open withdrawal before a later same-item deposit can match it, so only the restocked share is reported, none at all when the recipe consumes exactly what was withdrawn, and a product the game does not craft between the two moves consumes nothing; a product deposit whose quantity times an ingredient's amount exceeds an `int` still consumes what its recipe used, and a consumption beyond an `int` is refused with an `ArithmeticException`; `staysSilentForACrafterWhoWithdrawsMaterialDepositsTheProductAndRestocksTheMaterial` pins the plain acceptance case. A deposit whose recipe is unread abstains the pair for every item with an open lot at that moment, reported as a `RoundTripAbstention` instead of a `RoundTrip`; an unread deposit with no open lot changes nothing; the abstention is per pair, so an unrelated trader good's own round trip started after the unread deposit is still reported. | pure unit |
| `RoundTripTest` | A `RoundTrip` of zero or a negative quantity is refused where it is built. | pure unit |
| `EvergoreDataEvaluatorTest` | Unit tests covering: bank aggregation (placement + withdrawl sums), storage valuation (craftable item with quantity and partial quality), `Erde-Eibenlanze` resolving by its real in-game spelling, unknown item fallback (zero value + WARN + counted per occurrence into `EvaluationResult`, and empty when everything resolves), the three raw stones valued under the names the game uses, credited nothing and booked as a donation on deposit and charged on withdrawal, with a near-miss spelling staying unknown rather than silently valuing at zero, full recompute (sums start at zero over all stored entries and **overwrite** stale meta values; a second run is idempotent), mid-run-failure self-healing (the failing avatar's own sums are withheld and recover on the next clean run), avatar union across both repos (all keys written per avatar), and `last_updated` being written in Berlin wall-clock time from the application `Clock`. Hand-written fakes: `FakeMetaInformationRepository`, `BankRepositoryStub`, `StorageRepositoryStub`, `LoggerSpy`. Also pins that one run hands the store **one** batch carrying every known avatar's four keys plus `last_updated` (a regression guard against writing per avatar again, which is what let a request read a half-written recompute). Failure isolation: an avatar whose ledger read throws is reported in the `EvaluationResult` and logged at `error` while every healthy avatar still refreshes in the same batch, that avatar's stored sums stay untouched, and the collection timestamp `last_updated` is still stamped on such a run, because it records that a scrape happened rather than that every avatar recomputed. Recompute instants: one run stamps **one** instant for every avatar it refreshed, proven with a hand-written **ticking** `Clock` rather than a fixed one, because a fixed clock cannot tell one call from many and the first version called it per avatar, which made every avatar but the last look stale; a second run re-stamps its own single instant. A failing avatar keeps the instant of the last run that reached him; a failing avatar carrying none is left without one when no sums are stored for him, even after an earlier run, and so is one with stored sums while no avatar carries an instant yet; one whose sums are stored is otherwise seeded from the newest instant of the avatars with stored sums, exact to the millisecond across the second pass of the Berlin fall-back hour, never from an instant left on an avatar without sums, never for three of four sums, and as the instant alone; such an avatar stays current until an avatar with stored sums is stamped after the run whose instant seeds him, which can be the seeding run itself; and while a new member's own recompute keeps failing, the guild total stays empty across runs until one reaches him; a stored sum that cannot be read, or an unreadable instant of a failed avatar without sums, stops no run, and the sum is overwritten. Round trips: a same-day trader-good round trip over the ledger is carried in the `EvaluationResult`, a clean ledger carries none, and an avatar whose ledger cannot be read contributes none while a healthy avatar's own round trip is still carried in the same run. | pure unit |
| `EvergoreDataExtractorTest` | Unit tests covering: parsed bank/storage entries are persisted; a still-visible entry older than the stored max but missing from the database is healed (ingested) via `getAllSince(min scraped timestamp)`; an identical already-stored row (bank and storage) is not duplicated; a scraped identical pair with one already stored ingests only the surplus; two identical scraped rows both survive when neither is stored yet; entries are ingested oldest-first, so any part of a batch that is stored is the oldest rows. `FakePageSource` returns canned `PageContents`; capturing extensions of `BankRepositoryStub`/`StorageRepositoryStub` assert the `getAllSince` argument (a wrong argument yields an empty result, so a mutant passing the wrong timestamp is caught). No browser, no framework. | pure unit |
| `MetaInformationSnapshotIsolationTest` | Runs a read **against a recompute that is still open**, on real SQLite and real threads, deterministic via two `CountDownLatch`es (no `sleep`, no wall-clock condition; the `await` timeouts are hang guards only): a key whose `serialize` blocks holds the write transaction open from **inside**, and a read through the **same** repository, the production topology (one per context, on the one `SqliteDatabase`), must see the pre-recompute generation **whole**, then the committed one whole. Verified non-vacuous twice: on one JDBC connection shared by every thread the reader sees `[1, 2]`, the torn read itself, and so it does with the transaction removed from `add`. | adapter integration |
| `ReadWaitsOutAWritersLockTest` | A read through the meta repository that lands on **another connection's `BEGIN EXCLUSIVE`** waits the lock out: it has not answered while the lock is held, and answers the seeded state once the lock is released, instead of failing with `SQLITE_BUSY`. It **waits on the wall clock**, by author decision (a real lock, chosen over an injected short bound and over a `PRAGMA`-only pin): the lock is held for 4 s, so it fails on sqlite-jdbc's implicit 3 s bound with ~1 s to spare and passes ~6 s under the 10 s bound (measured: the read answers after ~4.0 s). The `CountDownLatch` await and the join timeout are hang guards only. Verified non-vacuous: with the writer on `BEGIN IMMEDIATE`, which leaves reads free, the read answers while the lock is held, and only the not-answered assertion catches it. | adapter integration |
| `AvatarSummariesSnapshotTest` | The same guarantee from the response's side, single-threaded: a meta repository whose every snapshot answers a later recompute generation must still yield **one** generation across all rows and the guild total. Pins that the read takes one snapshot for the whole page; a per-avatar snapshot would mix generations again. | pure unit |
| `MetaInformationDatabaseRepositoryTest` | Adapter tests for the meta store's write atomicity: a batch stores every entry, a batch whose last entry cannot be serialized stores **none** of the earlier ones, and a key that already held a value keeps it when the batch it sits in fails. Rollback is the proof that `add` is one transaction, so no reader can observe a half-written recompute; single-threaded and clock-free, no concurrency needed to pin it. A NULL value needs no case here: the column is `NOT NULL`. | adapter integration |
| `DatabaseMigrationTest` | Adapter tests for the Flyway setup on a file database: an empty database ends up with all three tables; a database still carrying the pre-Flyway schema keeps **every row byte-for-byte** through the migration; after it, a `NULL` in **every** column of all three tables is refused with `NOT NULL constraint failed`; migrating twice changes neither schema nor rows; a database missing one of the three tables has it created and migrates anyway (the case a baseline *at* `V1` got wrong); a pre-existing `NULL` row aborts the migration, leaves the database as it was and migrates cleanly once the row is fixed; a table that exists but lacks a column aborts with `no such column` instead of dropping or altering it; and eight threads migrating the same fresh database at once still migrate it exactly once, the case that pins the `synchronized` block against `SQLITE_BUSY`; both ledgers get the `(avatar, timeStamp)` index on an existing database as on an empty one, and on each ledger the query plan reads an avatar's page in time order by an index search, and counts it and finds every avatar's latest movement from the index alone (the literal `EXPLAIN QUERY PLAN` lines: a lost index fails all of them, a swapped column order or an index on `avatar` alone fails the page and the latest-movement cases, while the count is served by any index that leads with `avatar`); a database already carrying an index of the migration's name aborts the migration, keeps neither ledger index, and migrates once that index is dropped. The row-for-row assertion is what proves the irreplaceable history survives. | adapter integration |
| `GermanOrderConnectionSourceTest` | Adapter tests on a file database for the pool that registers `GERMAN_ORDER`: through the default registration, two connections held at the same time both sort `Stahlbarren` before `Stahl-Rüstung` under it, and so does a connection the pool opens after it closed the last one (`setMaxConnectionsFree(0)`), and closing the source closes the connections it keeps; through a registration that calls `GermanOrderCollation.registerOn` and records what it returns, two connections held at the same time hold collations and collators of their own, and a connection sorts under the very collation registered on it (its SQL comparison is `BLOCKED` while the test holds that collation's collator). A connection whose registration fails is closed, whether the injected registration throws an `SQLException` or an unchecked exception. Verified by recorded mutations: registering only while the pool holds no connection fails the first case, registering only until the first close fails the second, not closing the refused connection fails the third, closing it only on an `SQLException` fails the fourth, one collation registered on every connection fails the own-collation case, registering a shared collation while answering a fresh one fails the sorts-under case, and a `close()` that keeps the pool's connections open fails the closing case. | adapter integration |
| `GermanOrderCollationTest` | Unit tests for the collation behind `GERMAN_ORDER`: every registration (on two in-memory connections) gets a collation and a collator of its own, it compares under that collator's lock (a thread comparing while the test holds that collator is `BLOCKED`), and a comparison on another connection finishes while one connection's collator is held, so concurrent name sorts never queue on a shared lock. Every wait for a thread's state is bounded. Verified by recorded mutations: one collation registered on every connection fails the registration case, one shared collator the first case, comparing through `GermanOrder.NAMES` the second, a lock shared by every collation the third. | unit |
| `BankDatabaseRepositoryTest` | Repository is usable without separate init (file DB); `getAllSince(timestamp)` includes the row exactly at the boundary, excludes older rows, includes newer rows; `getAllFor(avatar)` holds exactly the rows stored before it returned and none stored after, fails at the call, not later on access, on a stored row of an unknown transfer type, and leaves nothing open once it returns (another connection takes `BEGIN EXCLUSIVE` without waiting): the materialised read the recompute's unknown-item reporting relies on; a stored withdrawal reads back as a withdrawal; `countFor` counts only the given avatar's rows and is zero for an unknown one; `latestTimestampPerAvatar` names the newest row per avatar and nobody at all for an empty ledger; a page sorted by the amount starts with the largest amount of the whole ledger, one sorted by the transfer puts deposits first and each kind newest first, one sorted by the time ascending starts with the oldest movement, and one sorted by the avatar stays newest first; the three are verified by recorded mutations of their column mapping; `add` writes several rows in **one** commit, read off SQLite's file change counter (header offset 24), which the pooled source's `create(Collection)` raised once per row; `add` fails and stores nothing when an entry cannot be converted, and a row a constraint refuses fails naming the constraint, leaves no lock behind, and commits at most the rows before it in write order, read over a fresh connection (of four rows written oldest first, `[]`, `[1]` or `[1, 2]`). Every case runs on a file database: Flyway migrates over its own connection, which a `:memory:` database would not share. Verified non-vacuous: a lazily converting `getAllFor(avatar)` fails only the unknown-type case, an iterator it leaves open only the nothing-open case, a stored form that is always `Einlagerung` only the withdrawal case, converting inside the batch only the unconvertible-entry case (by its count), writing newest first only the refused-row case, a commit on the thread connection without a rollback only the refused-row case (by its lock check), and a swallowed failure only those two; the batch inside one transaction passes by design. | adapter integration |
| `StorageDatabaseRepositoryTest` | `getAllSince(timestamp)` includes the row exactly at the boundary, excludes older rows, includes newer rows; `getAllFor(avatar)` holds exactly the rows stored before it returned and none stored after, fails at the call, not later on access, on a stored row of an unknown transfer type, and leaves nothing open once it returns (another connection takes `BEGIN EXCLUSIVE` without waiting): the materialised read the recompute's unknown-item reporting relies on; a stored withdrawal reads back as a withdrawal; `countFor` counts only the given avatar's rows and is zero for an unknown one; `latestTimestampPerAvatar` names the newest row per avatar and nobody at all for an empty ledger; a sorted page orders the whole ledger, not only itself: the largest quantity descending, the smallest quality ascending, deposits before withdrawals ascending, and by the avatar newest first, since every row is that avatar's; the time sorts by the moment in both directions across `2001-09-09T01:46:39Z`/`…40Z`, where epoch seconds and epoch millis each gain a digit, so a store that writes either as text fails it; rows tied on the column stand newest first, also across a page boundary, and pages of three over seven rows tied on everything but their quality, sorted by the quantity and by the time, hold every row once and in row-`id` order, read back over a fresh connection; item names sort in `GermanOrder` (`Apfel`, `Äpfel`, `Stahlbarren`, `Stahl-Rüstung`, `Zwiebel`), and items of one name stand newest first behind it; `add` writes several rows in **one** commit, read off SQLite's file change counter (header offset 24), which the pooled source's `create(Collection)` raised once per row; `add` fails and stores nothing when an entry cannot be converted, and a row a constraint refuses fails naming the constraint, leaves no lock behind, and commits at most the rows before it in write order, read over a fresh connection (of four rows written oldest first, `[]`, `[1]` or `[1, 2]`). Every case runs on a file database: Flyway migrates over its own connection, which a `:memory:` database would not share. Verified non-vacuous: a lazily converting `getAllFor(avatar)` fails only the unknown-type case, an iterator it leaves open only the nothing-open case, a stored form that is always `Einlagerung` only the withdrawal case, converting inside the batch only the unconvertible-entry case (by its count), writing newest first only the refused-row case, a commit on the thread connection without a rollback only the refused-row case (by its lock check), and a swallowed failure only those two; the batch inside one transaction passes by design. The sort cases are verified by recorded mutations: ignoring the direction fails the time-descending case, mapping the avatar to the time fails the avatar case, dropping the `id` fails both page-boundary cases, and a time key running oldest first fails both tie cases. | adapter integration |
| `ContextShutdownClosesTheDatabaseTest` | Boots a **real `ApplicationContext`** on `ThrowawayDatabaseFactory` and closes it: the meta repository it built can no longer read, which proves the context closes the one `SqliteDatabase` it opened. Verified non-vacuous: without `preDestroy = "close"` on the factory method the read still answers. | real context boot |
| `ProtocolEvaluationAcceptanceTest` | End-to-end: copies the committed synthetic fixture DB (`testdata.sqlite`) to a `build/` working copy, boots the real Micronaut `EmbeddedServer` against it, stubs the scraper (`loadData()` no-op) while the **real** `EvergoreDataEvaluator` runs via the scheduled job, then asserts the overview totals for every avatar of **both** ledgers (including the storage-only one, whose row exists only because the overview lists the union) and the **byte-exact** bank and storage response bodies (so field names, key order, the ISO-8601 UTC instants and the `DEPOSIT`/`WITHDRAWAL` wire names are all pinned). `GET /api/v1/admin/status`'s `lastUpdated` gets one test per property there (parses as an instant, is the UTC form and not an offset form, reads back in the application zone as the stored wall-clock time). Plus the paging and error contract: a page past the end is 200 with `items: []` and the true `totalCount`, an unknown avatar is 404 while a **known** avatar with an empty ledger is 200 with `totalCount: 0` in **both** directions (`Calix` has bank but no storage rows, `Brynja` storage but no bank rows), and a `@ParameterizedTest` walks every out-of-bounds paging window over all three routes, each a 400. Plus the sort: a storage page sorted by the quantity descending and a bank page sorted by the amount descending each start with the ledger's largest figure, a storage page sorted by the name ascending lists its items by name (German order itself is pinned at the repository), a valid sort on an unknown avatar still answers 404, and a `@ParameterizedTest` answers 400 to a column of the other ledger, an unknown direction, an empty column, a column followed by SQL text, and an unknown sort on an unknown avatar (the sort is checked before the avatar). Plus storage **valuation** at the `MetaInformationRepository` bean level, which is the independent pin behind the four sums the overview body asserts, and an unknown endpoint answering 4xx. | `@MicronautTest` acceptance / e2e |
| `TestDataGenerator` | Run-on-demand writer (`./gradlew generateAcceptanceDb`) of the committed synthetic fixture `testdata.sqlite`: 4 avatars, one of them **storage-only** (`Brynja`, the case the overview's union exists for; her deposit is a **craftable** item, so her storage sum is non-zero, which puts `bankDeposited: 0` beside `storageDeposited: 370.08` in her row and pins that the bank columns stay bank-only while the storage columns really carry the storage half; a zero-value item would have made both true by accident); bank in both directions; storage with quality scaling and a zero-value item. Item names reference `EvergoreItem.*.ingameName`, so values stay derived, not invented. | fixture generator (`main`) |
| `LastRunStatusTest` | Pure unit tests, all read through `snapshot()`: the four scrape/recompute outcome pairs (`recordSuccessfulScrape`/`recordScrapeFailure`/`recordSuccessfulRecompute`/`recordRecomputeFailure`) each empty before any run, record a specific `Instant` and return it, a second record overwrites the first, and the four are recorded independently of each other; a successful recompute takes its unknown-item and failed-avatar names from the same `EvaluationResult`; no unknown item names initially; a later clean recompute **replaces** the previous run's unknown-item and failed-avatar names (an empty pair clears them); a recompute failure **preserves** the last successful run's instant and both name lists while flipping `recomputeHealthy` to `false`. A successful recompute also takes the round trips and abstentions of the same result. No framework. | pure unit |
| `LastRunStatusRecomputeIsolationTest` | Proves `LastRunStatus`'s recompute write is one atomic snapshot, not a torn read, two ways: (1) a writer thread's `recordSuccessfulRecompute` blocks while copying `failedAvatarNames` — the **last** value it copies before publishing — so a concurrent `snapshot()` lands genuinely mid-write; it asserts every field still belongs to the run before it, never a mix of the new `unknownItemNames` with the old instant/`failedAvatarNames`, which a sequential-field writer would fail (verified by hand against a throwaway torn writer, then discarded); once released, the next `snapshot()` shows every field belonging to the new run. (2) An implementation-order-independent stress test races two writer threads (`WRITES_PER_WRITER_THREAD` recomputes each, tagged `A`/`B`) against a spinning reader, asserting on every read that the instant, `unknownItemNames` and `failedAvatarNames` all tag the same run. No framework. | pure unit |
| `LastRunHealthIndicatorTest` | Pure unit tests (with framework dep on `micronaut-management`): reports `UNKNOWN` with no detail map before any recompute; reports `UP` with `lastSuccessfulRecompute` detail key after a recompute; omits the unknown-item detail when the last run had none; reports the unknown-item count **and** names when present; every name list in German order with each count equal to the number of distinct names listed, and the round-trip lines in German order; a scrape failure recorded beside a successful recompute shows `lastScrapeFailure` but not `lastRecomputeFailure` while the status stays `UP`, and a recompute failure recorded beside an earlier successful recompute shows `lastRecomputeFailure` but not `lastScrapeFailure` while the status drops to `DOWN` (the latest attempt is what counts, not "ever succeeded"), pinning that `/health` tells the two apart; a first-ever recompute attempt that fails is `DOWN` with only the failure detail, not `UNKNOWN`. Round trips: reports `roundTripCount` and the sorted `roundTrips` description lines when the last run reported one, omitted when none did; the same pair for `roundTripAbstentionCount`/`roundTripAbstentions`. Subscribes to the `Publisher` inline via an anonymous `Subscriber`. | pure unit |
| `EvergoreDataCollectorJobTest` | Pure unit tests: records `lastSuccessfulScrape` and `lastSuccessfulRecompute` after a successful cycle; a failed scrape still runs the recompute (`evaluateData` is called, `lastScrapeFailure` and `lastSuccessfulRecompute` are both recorded, `lastSuccessfulScrape` stays empty), unlike a failed recompute, which is rethrown and leaves `lastSuccessfulRecompute` empty beside a recorded `lastRecomputeFailure`; the `PostCollectionHook` runs only after a successful recompute: it does not run when the recompute throws, and it still runs after a failed scrape as long as the recompute that follows succeeds; passes the run's unknown items and failed avatars to `LastRunStatus` as part of the same `recordSuccessfulRecompute` call; the initial delay restores the interrupt flag on interruption instead of swallowing it, proven via a positive-delay `Configuration` (interrupting the current thread first, `TimeUnit.SECONDS.sleep` with a positive argument throws immediately without an actual wait, `sleep(0)` does not throw at all and so cannot pin this). Uses `Clock.fixed(…)`, `ZeroDelayConfiguration extends Configuration` (delay 0), and local `FailableExtractor`/`FailableEvaluator` inner classes; no static state. | pure unit |
| `HealthEndpointTest` | Boots the real Micronaut `EmbeddedServer`; mocks the extractor (no-op `loadData`), config (zero delay, test DB path), and hooks (BootSignalRecorder pattern). Asserts: `GET /health` returns **exactly 200** without a token; response body contains `lastRun` + `lastSuccessfulRecompute`; `/healthz` is **not** served as the health endpoint (401, so a prefix match cannot inherit the exemption, and the router is held to the rule `PublicPathsTest` states at unit level). The token scope itself lives in `TokenScopeTest`, not here. | `@MicronautTest` integration |
| `AdminStatusEndpointTest` | Boots the real Micronaut `EmbeddedServer` against the fixture DB with the scraper stubbed; asserts `GET /api/v1/admin/status` answers 200 without a token, and that after recording a scrape outcome and a recompute outcome (with its unknown items and failed avatars) on `LastRunStatus` the body carries a non-null `lastUpdated` and the matching outcome instants plus the deduplicated, sorted `unknownItemNames`/`failedAvatarNames`. A recompute carrying a round trip and an abstention serves them as `{avatar, item, quantity}`/`{avatar, item}` entries under `roundTrips`/`roundTripAbstentions`, `item` being the catalog's `ingameName`. | `@MicronautTest` integration |
| `AdminStatusEmptyStateEndpointTest` | Boots the server against an **empty** database with the collector disabled and asserts `GET /api/v1/admin/status` answers byte for byte with every key present and `null`/empty, the "no run yet" counterpart to `AdminStatusEndpointTest`; the one test that pins that the admin surface renders a missing outcome rather than dropping the key, `roundTrips`/`roundTripAbstentions` the sole exception, serving `[]` rather than `null` (recorded mutation: the controller serving `null` for the pair instead). | `@MicronautTest` integration |
| `TransferTypeTest` | Locks `TransferType.toGermanString()` for both constants (`EINLAGERUNG`→"Einlagerung", `ENTNAHME`→"Entnahme"), the single source for the enum→German mapping. | pure unit |
| `ApplicationExceptionHandlerTest` | Unit tests asserting each `ProtocolParserException` subclass maps to its HTTP status via the visitor, plus the `onUnknown` branch, plus that the logged path carries **no** token query parameter, plus the log severity per mapped status (client error → one `info` line, server error → `error` with the exception) and that a failing response mapping is still logged with its trace before it propagates. `accept` is `abstract`, so a new exception subclass is a compile error rather than a silent fallback. | pure unit |
| `ApplicationExceptionHandlerHttpTest` | Boots the server against its own fixture DB copy and drives the not-found path through the **real Netty write**, which the pure unit test cannot reach: an unknown avatar answers 404 with the default `Not Found` reason phrase (no echo of the requested name), and an unknown avatar whose percent-encoded name carries `CRLF` still answers 404 instead of the 500 that a control character in the reason phrase used to cause. | `@MicronautTest` integration |
| `ProductionSnapshotRecomputeCheck` | **Opt-in, on-demand** (`-DprodSnapshot.check=true`): boots the real context against a *copy* of a local production snapshot (`temp.sqlite`, gitignored) with the scraper stubbed, so the real `EvergoreDataEvaluator` recomputes the meta sums on real data and the delta can be inspected before a deploy. Writes the summaries response verbatim to `build/tmp/prodSnapshot/overview-after-recompute.json` (decision 2026-08-08) and asserts the artifact carries every avatar rather than a first page of them, which holds up to the API's `MAX_SIZE` of 1000 avatars and fails rather than truncates beyond it; also exports the valuation catalog and asserts item names are unique (the lookup takes an arbitrary entry carrying the name, see [domain-model.md](domain-model.md)). Holds every meta sum the snapshot stored against the one recomputed from the same rows and writes the pair to `metaSums-stored-vs-recomputed.tsv`; it asserts that every stored key was compared and that none was dropped, so a failed read cannot pass as an empty diff. It does **not** assert equality: the diff is the measurement, not a gate. It does assert that every served figure, per row and in the totals, is its recomputed exact value rounded once, halves away from zero, which the old rule of adding rounded figures fails on this snapshot, and that the header identity reconciles within two gold. Details: [1:1 against the production instance](#11-against-the-production-instance). | `@MicronautTest` on-demand check |
| `ProductionSnapshotMigrationCheck` | **Opt-in, on-demand** (`-DprodSnapshot.check=true`, same flag as the recompute check): runs the real `DatabaseMigration` over a *copy* of the local production snapshot and holds every row of all three tables against a SHA-256 taken before it, so a migration that drops, reorders or rewrites a row fails instead of being noticed after the deploy. Also pins that every column comes out `NOT NULL`, that no `*_strict` table is left behind, that a second run is a no-op, and that the migrated copy carries both ledger indexes and reads the busiest avatar's ledger page of either ledger by an index search. Run it before every deploy that carries a new migration. Each of its two tests migrates its own copy, so a snapshot older than `V2` pays the table rebuild twice. Measured 2026-09-07: 237,538 rows, digests identical, ~4 min per rebuild on a slow bind mount; 2026-09-27 over a `V2` copy of the 03.09.2026 snapshot (251,300 rows): digests identical, both indexes present, `V3` in 1.3 s. | on-demand check |
| `GameCatalogScrapeCheck` | **Opt-in, on-demand** (`-DgameCatalog.scrape=true`, driven by `./run-game-scrape.sh`): signs in to the live game and dumps each page named in `gameCatalog.pages` into `build/tmp/gameCatalog/` as rendered text, HTML and a link table, so a catalogued price or recipe can be settled against the game instead of against memory. A named `stock_out` selection with no `pos=` is fetched in full, page by page. It signs in through `EvergoreSession` and builds its browser through `Driver`, exactly as production does, so it holds no login knowledge of its own. It reads and navigates only, submitting no form beyond the login the production scraper already performs. The one instrument behind every catalog value; how to drive it and where the prices and recipes sit is below | on-demand check |
| `MetaSumComparisonTest` | Pure unit tests over the snapshot comparison tool: `StoredMetaSums` reads every `metaInformation` key of a database **read-only** and leaves out a key stored as `NULL`; `MetaSumComparison` calls a key unchanged when its *number* is unchanged even if its text differs, carries both sides plus their ratio for a changed one, reports no ratio where the stored value was `0`, and counts only the keys both sides hold. Hand-built maps plus one throwaway SQLite file under `build/tmp/test/` | pure unit |
| `OnDemandCheckOptInTest` | Pins every on-demand check's switch in one place: the build really forwards each opt-in property into the test JVM (an absent property would leave that check unrunnable with nothing to distinguish that from a passing run), each check is switched on by its own property being true, and an ordinary run leaves every one of them off. It discovers the checks by their `@EnabledIfSystemProperty` annotation and pins how many it finds, so a check the discovery misses fails the suite instead of going unpinned. A hand-kept map keyed by property could not hold `ProductionSnapshotMigrationCheck` at all, which shares `prodSnapshot.check` with the recompute check | pure unit |
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
| `SortRequestTest` | Pure unit tests for the sort parameters of a ledger read: every bank and every storage column is named as the wire names its field, both directions in words, the default is the time newest first, and a column the ledger lacks, an unknown direction or a column name in another case is a 400 (`HttpStatusException`). Verified non-vacuous: making the default column the avatar fails only the default case. | unit |
| `AvatarSummariesControllerTest` | Pure unit tests over the stubbed repositories: the overview lists an avatar that only ever moved items, `totalCount` counts every known avatar while a page shows only part of them, an avatar without stored sums is served with all five sums `null` rather than zeros, beside the computed rows, and `totals` serves every figure as `null` while one avatar is so, a seeded avatar's four ledger sums plus the derived net are served as whole gold, the envelope's `totals` cover **every** known avatar rather than only the served page and are rounded from the guild's exact sums rather than added from the rounded rows, the figure before the guild's share (per row and in `totals`) and the guild's storage value are served rounded from their exact values and absent while the flows are, and each summary carries the last activity of both ledgers. Staleness on the wire: a refreshed row serves no instant, a row the last collection missed serves the instant its sums come from, and `totals.containsStaleSums` states that the guild contains such a row even on a page that does not show it. Uses `KnownAvatars` over the two repository stubs plus `FakeMetaInformationRepository`. | pure unit |
| `AvatarSummariesEmptyStateTest` | Boots the server against an **empty** database with the collector disabled and asserts the overview body byte for byte: `{"page":0,"size":100,"totalCount":0,...,"items":[]}`. It proves that an uncollected store still answers every contract key: `totals` with all its figures and an empty page as a visible empty array rather than missing keys. It pins no `null` literal, so it cannot tell `jackson.serialization-inclusion: ALWAYS` from `NON_NULL`; the explicit `null` of a member not yet computed is guarded only end to end, by the armed not-yet-computed scenarios. The pinned body carries `totals`' `containsStaleSums: false`, so the flag is proven present rather than omitted on a store that never ran. | `@MicronautTest` integration |
| `RenameSafetyTest` | ArchUnit guard, three rules: every non-static field under `rest/controller/api/wire` carries `@JsonProperty`, every `@DatabaseField` declares a non-empty `columnName`, every `@DatabaseTable` a non-empty `tableName`. Turns "a rename must never change a published contract or the schema" into a build failure; the column rule found the two `id` fields that relied on the field-name default. | architecture guard (ArchUnit + JUnit 5) |
| `SpaBundlePackagingTest` | Guards that `/static/ui/index.html` is on the test runtime classpath, i.e. the SPA bundle really is packaged into the jar by `processResources`. | pure unit (packaging guard) |
| `RateLimitStartupValidatorTest` | Startup fails, naming the offending property, for each value that would silently disable the throttle: a request budget of `0`, a zero or missing `interval`, a zero or negative `block-duration`, and a client budget of `0` or negative. The working configuration starts up and the `onApplicationEvent` entry point propagates the same failure. Keeps the config guard out of the constructor (handbook §3), the way `ApiTokenStartupValidator` does. | pure unit |
| `ApiTokenStartupValidatorTest` | Startup fails with the property name (`evergore.security.api-token`) in the message for an empty, blank and absent token; a set token starts up; the `onApplicationEvent` entry point propagates the same failure; the logged error names the environment variable that sets the token. One test boots a **real `ApplicationContext`** without the `test` environment and without the token, so an unset variable is pinned to end in the same message as a blank one, not in a binding failure. The boot runs on `ThrowawayDatabaseFactory`. | pure unit + one real context boot |
| `CredentialsStartupValidatorTest` | Startup fails with the property name (`evergore.credentials.username`/`.password`) in the message for an empty, blank and absent username or password; a set pair starts up; the `onApplicationEvent` entry point propagates the same failure; the logged error names the environment variable that sets the value and never the value itself. Three tests boot a **real `ApplicationContext`** (without the `test` environment for the unset ones), one with a blank login and one each without the username and the password, so the listener wiring is pinned too, not just the validator method: a lost `@Singleton` would fail them, and an unset variable is pinned to end in the same message as a blank one, not in a binding failure. The boots run on `ThrowawayDatabaseFactory`. | pure unit + three real context boots |
| `TimezoneStartupValidatorTest` | Startup fails naming the zone when the injected `ZoneId`'s rules are not a fixed offset (`Europe/Berlin`); a fixed-offset zone starts up whether it is `UTC` or another fixed offset (`Etc/GMT-2`), proving the check is "fixed offset", not "must be UTC"; the `onApplicationEvent` entry point propagates the same failure; the logged error names the zone. One test boots a **real `ApplicationContext`** under the real system default zone (UTC in this environment), pinning the `ApplicationFactory` wiring of the effective zone. A second boot test replaces the injected `ZoneId` bean with a DST-observing one (`Europe/Berlin`, via a `spec.name`-guarded `@Replaces` test factory) and asserts the boot itself fails naming the zone, so the reject path is proven through the real bean graph too, not only via a hand-constructed instance. Both boots run on `ThrowawayDatabaseFactory`. The validator is the interim safeguard (**D22**) until the timestamps move to an epoch/UTC format (**D14**). | pure unit + two real context boots |
| `BootSignalRecorderTest` | `awaitCollection()` unblocks both on `recordCollectionFinished()` and on `recordException()` (real threads, no timeouts), and the `dataLoaded`/`exceptionOccurred` queries flip false→true. Pins the boot-signal seam itself. | pure unit (concurrency) |
| `NoApplicationTextInHttpStatusTest` | ArchUnit guard, one rule: no class calls a reason-phrase-carrying `status(…)` overload. Covers all **six** that micronaut-http's response API offers, enumerated from the jar: `HttpResponse.status(HttpStatus\|int, String)`, `MutableHttpResponse.status(HttpStatus\|int, CharSequence)` and the `HttpResponseFactory.status(HttpStatus\|int, String)` the static helpers delegate to (missing that pair leaves a reachable bypass). `HttpStatus` is an enum and has no reason setter, so those six are the complete set. Each leg verified non-vacuous by a temporary probe calling all six; the rule flags every call by file and line. | architecture guard (ArchUnit + JUnit 5) |
| `BankEntryEqualityTest` / `StorageEntryEqualityTest` | Value equality of the two entry records: equal when all fields match, different for each single field in turn (timestamp, avatar, amount/quantity, name, quality, transfer type). The window-dedup in `EvergoreDataExtractor` compares entries by value, so this is load-bearing, not record boilerplate. | pure unit |
| `KbCitationGuardTest` | Guards every `docs/knowledge-base/*.md` file for a phantom backtick class reference: extracts candidate class names per `KbClassReferenceExtractor`'s inclusion rules (a compound-PascalCase, ≥2-hump token; a bare single-word class citation like `Configuration` is a disclosed blind spot the rule does not cover), resolves each against `KnownJavaSymbols`' classpath-backed lookup (the project's own compiled classes, every dependency jar, and the JDK, read from the test runtime classpath by listing class-file/module entry names, no bytecode parsing), and fails naming every unresolved `file:line: token`. Verified non-vacuous by injecting a temporary phantom reference into a `@TempDir` fixture. Scoped to Java `src/` references only: the documented React/TypeScript frontend symbols the extractor's rules still pick up in `frontend.md` never resolve against that scan, so they are named in an explicit disclosed-gap set (itself pinned to only ever accept `frontend.md`-rooted entries) rather than silently swallowed by skipping the file. | architecture guard (ArchUnit + JUnit 5) |
| `SeleniumPageSourceTest` | Drives `SeleniumPageSource` against a `RecordingWebDriver` fake, using **injected `Clock` and `Sleeper`** so wait timeouts never touch real time: the driver is quit after a successful scrape **and** after a failing one (try/finally), the scrape failure propagates and is logged, both-fail contract is pinned (scrape failure propagates; both failures logged), and a timeout waits deterministically without real-time dependency. The login it delegates is covered here only at its seam: a failing sign-in logs exactly one `warn` that names the login and carries neither credential, and an absent username or password never reaches the login form. | pure unit (fake driver, injected clock/sleeper) |
| `GameFactsGuardTest` | Holds the catalog against `src/test/resources/gameCatalog/game-facts.tsv`, the committed record of what the game itself said (see "The recorded game facts" below). Every recorded price must be the catalog's price and every recorded recipe the catalog's recipe, and the failure names each divergence with the page it was read from and the date. It also pins that the guard really reaches every fact (671 price rows, 424 recipe rows, none skipped for want of a catalog entry), the headline counts the catalog rests on (the storage prices 433 entries, the market 229, together 483 of 601), that every row says where and when it was read, and that no guild member or storage-access line survived into the file. | pure unit (committed data file) |
| `CatalogLookupTest` | Pins `CatalogLookup`, the one description of how a scraped name resolves to a catalog entry: the catalog's own name, a recorded second spelling, a trailing magic affix stripped, an affix sitting before a ` [2H]` suffix, and nothing at all for a name the catalog has never seen. `EvergoreDataEvaluator` and `GameFactsGuardTest` both resolve through it, so neither can drift from the other. | pure unit |
| `EvergoreSessionTest` | Drives `EvergoreSession`, the one description of how the game is signed in, against the same fake: the configured username and password reach the fields the login form names for them, the browser is left on the game rather than on the world portal it passes through, the consent banner is clicked before the form is touched and its absence changes nothing, the world portal is optional (a login that goes straight to the game still arrives, and the portal's confirmation button is left alone when no portal was shown), and `openPage` waits for the page it asked for instead of reading whatever the browser still shows. Both the production scraper and `GameCatalogScrapeCheck` go through this class, so these cases cover the check's login too. | pure unit (fake driver, injected clock/sleeper) |
| `RunAcceptanceScenariosTest` | The cucumber-jvm suite: `@Suite` + `@IncludeEngines("cucumber")` over the classpath resource `features` (`src/test/resources/features/`, one `.feature` per capability, product language, handbook §5); glue package `dev.schoenberg.evergore.protocolParser.acceptance`; the default tag filter in `junit-platform.properties` excludes `@wip` and `@characterization`, and `./verify bdd` / `./verify focus <feature>` override it through the `test` task's forwarded system properties. Carries the catch-up's scenarios, `@wip` until their step definitions exist; what a scenario runs against: "The acceptance runner" below. | acceptance (cucumber-jvm on the JUnit platform) |
| `StepKeywordTest` | Dry-runs every scenario, `@wip` included, through cucumber's own step matching (`--dry-run`, the `message` formatter) and fails when one step definition is matched under two keywords, naming the pattern and the keywords (handbook §5, TIME-8): cucumber binds a step without its keyword, so a precondition reused as an action compiles and runs. Asserts it read step definitions at all, so an empty dry run cannot pass. | acceptance guard (cucumber dry run) |
| Fakes & stubs | `LoggerSpy` (records info/warn/error messages), `FakeMetaInformationRepository` (in-memory map), `BankRepositoryStub` / `StorageRepositoryStub` (thin subclasses of the generic `LedgerRepositoryStub`), `RecordingWebDriver` (scriptable Selenium `WebDriver`: click targets, elements a page does not carry, and a `stopNavigating` that freezes the browser where it is), `MutableClock`/`CountingSleeper` (wait timeouts without real time), `ThrowawayDatabaseFactory` (opt-in through the `test.throwaway-database-path` property: a context booted with it keeps its `Configuration` on that file under `build/tmp/test/`, never on `database/temp.sqlite`, and an initial delay no suite outlasts, because the scheduled job outlives `close()` and would otherwise open a real Firefox and try to sign in to the live game once its 30 s ran out, orphaning the browser when the JVM exits; measured 2026-09-23: a JVM kept alive 45 s past the two startup-validator classes spawned two geckodrivers ~37 s after their boots without it, none with it), `SqliteFile` (raw SQL on the throwaway file behind a repository test, over a connection of its own, an exclusive lock taken without waiting, the commits a piece of work cost, off the file change counter, and a column's committed values over a fresh connection). Hand-written, no mocking framework. | helpers (no `@Test`) |

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

## The acceptance runner

What a scenario runs against (`dev.schoenberg.evergore.protocolParser.acceptance`, handbook §5):

- **A fresh service per scenario** (author choice 2026-09-28): the real Micronaut context and
  server on a random port, against its own SQLite file under `build/tmp/acceptance/`, started in
  `ServiceHooks`' `@Before` and closed in its `@After`. Environments `test` and `acceptance`; the
  test beans of the `acceptance.service` package carry `@Requires(env = "acceptance")`, so no
  `@MicronautTest` class sees them.
- **The scenario's world** (`acceptance.world`) lives outside the context and is handed in with
  `ApplicationContextBuilder.singletons`, so it outlives any one context; cucumber-picocontainer
  shares it between the step classes of one scenario.
  - `GameProtocols`: the game's bank and storage protocol pages, newest entry first; the
    `PageSource` bean reads them in place of `SeleniumPageSource`. `the game's bank protocol
    shows:` (or `storage`) replaces that page with its text line for line, lines no entry can hold
    included; a movement recorded into a page shown as text fails the step. `the game cannot be
    reached` makes the read throw, as a failed sign-in does.
  - A ledger table under `Given` (`the guild bank ledger holds:`) writes its rows into the
    protocol and stores them through the real `EvergoreDataExtractor` at once: "the ledger holds"
    means the stored ledger, whether or not a collection follows. A later collection reads the
    same pages, and the extractor's dedup keeps them stored once.
  - `ScenarioTime`: the one clock, in Berlin wall-clock time; `the daily collection ran at` sets
    it, `has run` sets it to 05:00 the morning after the newest movement (TIME-4).
  - `LedgerFaults`: a `java.lang.reflect.Proxy` around the ledger and meta ports, installed by
    `BeanCreatedEventListener`s (author choice 2026-09-28): a member's stored movements fail every
    read naming the member, only while a collection runs; withheld figures fail every read of the
    meta store outside one, and figures that cannot be saved fail every call on it inside one, so
    the recompute fails as a whole. It knows the port types only, no method or schema.
  - `ServiceLog`: every line the service writes through its `Logger` port, recorded by
    `RecordedLog` (a `BeanCreatedEventListener` wrapping the port) before it reaches SLF4J. "The
    service's log" in a scenario is this record; the framework's own logging is not in it. A
    collection against a reachable game fails its step unless it logs that it read the game, so a
    scrape that crashes cannot pass for one that found nothing.
  - `OperatorSettings`: the environment variables, throttle settings and time zone the operator
    starts the service with.
- **The daily collection is fired by hand**: `ManualTaskScheduler` replaces the `scheduled`
  `TaskScheduler`, records the fixed-delay job `@Scheduled` registers instead of starting it, and
  runs it when a step says the collection runs, through the same runnable the scheduler would
  call. The step asserts exactly one job ran; Micronaut's health monitor, the only other
  fixed-delay job, is switched off in this context (`micronaut.health.monitor.enabled`).
- **The service as the operator deploys it**: the startup scenarios and the throttle restart the
  service without the `test` environment, so `application.yml`'s throttle defaults hold and the
  secrets come from `OperatorSettings` through a property source with Micronaut's
  environment-variable convention, where a `Given` unsets or blanks one; the process's own
  environment is left out. The zone the `TimezoneStartupValidator` checks is the settings' one
  (`AcceptanceBeans` replaces `ApplicationFactory#effectiveZone`; default the JVM's). A start the
  service refuses never reaches the server's start, so no port stays bound; Micronaut counts the
  context as running only after its startup event, so the beans made before the refusal are left
  undestroyed. `the service has been restarted since` closes the context and starts a new one on
  the same SQLite file, with the run outcomes held in memory gone.
- **Service work runs on a thread of its own** (`OwnThread`): Micronaut forks ForkJoin tasks while
  it starts, and on a worker of cucumber's parallel pool the join steals another scenario onto the
  same thread, which then shares the first one's world (measured: `Cannot stop. Current container
  state was: DISPOSED`, `No bean of type EvergoreDataExtractor`).
- **The member through the browser** (`MemberBrowser`): a pool of browsers (`Browsers`), each
  started once and reused by later scenarios, at most one per scenario running at a time, cleared
  to `about:blank` between scenarios and quit in `@AfterAll`. The pool is the one run-scoped
  static state the steps keep (author decision 2026-09-28,
  [open-questions.md](../open-questions.md)):
  cucumber-picocontainer knows no run scope, and a browser per scenario costs a cold start of
  about 20 s. The pool does not tell browsers apart: it holds one choice per run (the
  `EPC_ACCEPTANCE_*` variables, read once), so a step that needs a second browser at the same time
  has to give the pool a key first. `MemberBrowser.leave()` returns a driver to the pool only after its
  cleanup succeeded; a driver whose cleanup failed is quit and dropped (`Browsers.discard`, which
  forgets it only once `quit` succeeded; `quitAll` quits every started driver, each in its own
  `try`, clears the set whatever happens, and rethrows the first failure with the others suppressed,
  so a `quit` that throws a runtime exception does not stop the others), and the cleanup failure stays the exception
  `leave()` throws, with a failed `quit` attached as suppressed.
- **A member's browser in its own real-world time zone** (`MemberBrowser.runsInTimeZone`,
  `BrowserChoice.startInTimeZone`) skips the pool rather than keying it: geckodriver has no session
  capability for the browser process's time zone, but `GeckoDriverService.Builder.withEnvironment`
  sets `TZ` on the geckodriver process it starts, and geckodriver's child Firefox inherits it
  (measured: the page's `resolvedOptions().timeZone` reads the given zone).
  A dedicated `FirefoxDriver` is built once for the scenario that asks and quit in `leave()` instead
  of returned to `Browsers`, since a `TZ` env var can only take effect at process start. Local
  Firefox only; the grid has no equivalent wired up yet. Right after it starts, `MemberBrowser`
  asserts the browser's own `resolvedOptions().timeZone` equals the requested zone, so a silent
  fallback to the container's UTC fails loudly.
- **Reading a page:** one `executeScript` per view (`read-overview.js`, `read-ledger.js`,
  `read-navigation.js`, `read-admin.js` under `src/test/resources/acceptance/`) returns the rendered
  text as JSON (`read-navigation.js`: the page frame's links with their `href` and current mark, plus
  every `href` on the page), a
  note's text the way a reader of the `role=note` gets it (none inside `aria-hidden`). A mark or a
  note a step names is also read the way the member sees it: focused, then the text WebDriver
  reports as displayed (`find-note.js`), so a note the stylesheet never shows fails. A colour a step names is read as the browser computes
  it, anchored to the design tokens (`colour-of-token.js` reads a token through a probe element,
  `find-cell.js` and `find-stat.js` find a member's figure cell and a figure of the guild's
  position, `colour-of-figure.js` reads the colour of the element holding the figure's last text
  node, its text-fill colour where set, so a child element's colour or a text-fill colour overriding the cell
  or box cannot hide): a credit or debit figure equals `--color-positive` or `--color-negative`, an
  uncoloured one equals `--color-text` and its parent's colour, and that parent equals
  `--color-text` too, so a rule painting a whole row fails. The admin reads through the same
  `MemberBrowser`; no separate pool exists per actor (handbook §5).
- **The operator reads over HTTP, not the browser** (handbook §5): `HealthReport`
  (`acceptance.world`) `GET`s `/health` on the running service's port, with the guild's token
  unless the step says without, and parses the `lastRun` indicator's `details`, so a step comparing
  "the admin page" against "the health report" (`{surface}`, `acceptance.steps.Surface`) reads
  each the way its actor really would; `AdminReport` and `HealthFacts` (`acceptance.operator`) read
  the same dates and name lists off either.
- **A client's raw request** (`ServiceRequests`, `acceptance.operator`): an HTTP/1.0 `GET` written
  on a socket, because `java.net.URI` refuses the garbled addresses the security scenarios send.
  The client's network address travels in the `X-Acceptance-Client-Address` header, which
  `AcceptanceClientIp` (test source in `rest.filter`, replacing `ClientIp` in the acceptance
  environment) takes before the socket's own address: no local socket comes from `203.0.113.7`.
  The throttle's windows move on `ScenarioTime`, starting off the clock's ten-second grid and 30 ms
  a request, so neither a fixed window nor a count from the latest request passes; no step waits
  one out.
- **The browser's clock** is shifted with a WebDriver BiDi preload script
  (`script.addPreloadScript`) and checked with `script.evaluate` in the page's realm, because the
  classic `executeScript` runs in a sandbox that does not see the page's `Date`.
- **Parallel**: `junit-platform.properties` runs 4 scenarios at a time;
  `EPC_ACCEPTANCE_PARALLELISM` overrides both the parallelism and the pool's maximum (the pool
  grows to its maximum while a worker blocks, so the parallelism alone does not bound it).
- **Where no browser exists** (the production image's build stage) a browser-driven scenario is
  skipped (`TestAbortedException`), like the browser smoke tests.
- **Measured** (2026-09-28, devcontainer, 12 cores, local headless Firefox unless named; what ran
  is in the decision row of that day, [open-questions.md](../open-questions.md)):

  | Run | Wall time |
  |-----|-----------|
  | `./verify bdd` (every `@wip` scenario) | 191 s, of which the suite 145 s |
  | the acceptance suite inside `./verify all` | 29 s (the whole gateway 535 s) |
  | the armed overview scenarios, one at a time | 74 s |
  | the same, 2 / 4 / 6 at a time | 48 s / 41 s / 42 s |
  | the same on the grid, 4 at a time: Firefox / Chrome / Edge | 43 s / 41 s / 42 s |

  - A worker's first scenario takes about 20 s, the browser's cold start and the first context;
    every later one 3 to 4 s. Beyond 4 at a time the cold starts dominate a run this short.
  - A scenario that ends on an undefined step still boots its service in `@Before`, so an
    unbuilt cluster costs `./verify bdd` about 2 s a scenario, half a second of wall time at 4 at
    a time.

## Coverage map

**Has tests:** `EvergoreItem` (value math per item kind **plus** two all-items golden-master rules over the whole catalog) · `MetaInformation` (serialization) ·
`EntryFactory` / `EntityParser` (the parsing contract: headline→entry, type mapping, quality defaulting, `+1` merging, quantity merging, `Impressum` terminator, malformed/out-of-range dates) · `EvergoreDataEvaluator` (bank aggregation, storage valuation, unknown item fallback + per-occurrence counting, full-recompute overwrite + idempotence + failed-run self-heal, avatar union, Berlin wall-clock `last_updated`) ·
`EvergoreDataExtractor` (parse→persist pipeline, window dedup via `getAllSince(min scraped timestamp)`: heals a still-visible entry missing from the database, no-duplicate + surplus-only dedup, oldest-first ingest) ·
`BankDatabaseRepository` / `StorageDatabaseRepository` (`getAllSince` inclusive boundary, `getAllFor(avatar)` materialised on return, `countFor` per avatar, one commit per `add`, a file database) ·
the **evaluate→overview pipeline end-to-end** via `ProtocolEvaluationAcceptanceTest` (the JSON contract byte-exact, plus its paging and error cases) ·
`LastRunStatus` (record + read, incl. unknown item names) · `LastRunHealthIndicator` (UNKNOWN / UP + detail, incl. the unknown-item detail) ·
`EvergoreDataCollectorJob` (records run on success, not on failure; forwards unknown items) ·
`/health` endpoint + wrong-token rejection via `HealthEndpointTest` ·
the **token scope in both directions**: the default-deny rule itself (`PublicPathsTest`) and over HTTP incl. the traversal pair and an unmapped path (`TokenScopeTest`), on the canonicalized path (`PathCanonicalizerTest`) ·
the **rename safety** of the JSON contract and the DB schema (`RenameSafetyTest`) ·
`TransferType`→German mapping (`toGermanString`) · `ApplicationExceptionHandler` exception→HTTP visitor dispatch + token-free logging + severity by mapped status +
the reason phrase staying free of the requested value, through the real Netty write (`ApplicationExceptionHandlerHttpTest`) ·
`RateLimitCounter` (no lost counts under 20 threads + deterministic expiry + the idle rule), the bound on the counter map (`RateLimitCountersTest`), the limit applying to every path (`RateLimitFilterTest`) and the audit line for every request (`RequestAuditLogFilterTest`) ·
`ApiTokenStartupValidator` (startup aborts on an unset/blank token, the unset one pinned through a real context boot), `RateLimitStartupValidator` (startup aborts on any rate-limit value that would disable the throttle), `CredentialsStartupValidator` (startup aborts on an unset/blank Evergore login, pinned through a real context boot) and `TimezoneStartupValidator` (startup aborts on a DST-observing effective zone, D22) ·
the SPA seam: history fallback vs. 404 in both directions (`SpaHistoryFallbackTest`, `SpaNavigationPathsTest`), bundle packaging (`SpaBundlePackagingTest`) and the bundled SPA painting real API data in a real browser (`DashboardBrowserSmokeTest`) ·
`BankEntry`/`StorageEntry` value equality (load-bearing for the window dedup) ·
`SeleniumPageSource`'s driver lifecycle (quit on success and failure, failure logging, deterministic wait timeout via injected clock/sleeper) and, in `EvergoreSession`, the sign-in both it and the catalog scrape use (the configured credentials reach the form, the consent banner is optional, the browser ends on the game; a failed login warns without leaking them) ·
`BootSignalRecorder` (the boot-signal seam itself) ·
and *indirectly* via `SmokeTest`: controllers, filters, repositories, the job.

**Most important UNTESTED logic:**
1. **`SeleniumPageSource`'s scrape itself**: navigation and pagination against the live site
   (inherently hard), and whether the login the form submits is actually *accepted* there. The driver
   *lifecycle* is covered by `SeleniumPageSourceTest` and the login *form interaction* by
   `EvergoreSessionTest`, both against the fake driver; only the live round trip is not.
2. **Repositories**: paging, which rows `getAllFor(avatar)` selects. Only incidental smoke coverage (`getAllSince` and the eagerness of `getAllFor(avatar)` have adapter tests).

## Migration verification: Gradle / Java 25 / Micronaut 4.10 (2026-06-16)

The build migration (Maven→Gradle, Java 17→25, Micronaut 3.8.4→4.10.3) was held to **identical
observable behaviour**:

- **Unit/integration suite** reproduces the pre-migration baseline exactly: **22 tests, 7 classes, 0
  failures** on the new stack.
- **1:1 against production data:** the migrated distribution rendered the production snapshot
  identically to the live instance. Method and current result: [1:1 against the production
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

The release gate before deploying: run the previous release and the candidate on **copies of the
same production snapshot**, then compare what both serve. It catches drift that the synthetic
fixture cannot, because it uses the real data volume, the real item mix and the real avatar set.

The comparison is **scripted** (decision 2026-10-10) and **value-wise, not byte-wise** (decision
2026-08-08): both sides serve `/api/v1`, the script reads each side's wire output and its exact sums.

### Scripted comparison

- **Run it:** `release/compare RUNNING_DIST CANDIDATE_DIST SNAPSHOT`; a distribution is a directory
  holding `bin/protocolParser`.
  - The running release (`0.2.0`) is built in its own worktree, detached on the tag; the candidate
    is built from the strand. A distribution is the unpacked `build/distributions/*.tar` that
    `./verify all` leaves behind.
  - The tag's own `./verify` predates the build lock and the per-worktree daemon registry: run it
    as `GRADLE_OPTS=-Dorg.gradle.daemon.registry.base=<that worktree>/registry.local.d flock
    /tmp/epc-build.lock <that worktree>/verify all`, and stop its daemon through the same registry,
    never with a bare `--stop`.
  - Snapshot copies live only under `<worktree>/build/release-comparison/` (replaced at the start of
    a run, left after it for inspection); the original is copied once per side and never opened.
  - **Never build while a side runs from that `build/`**, and `./verify all` clears `build/`.
- **How the sides run:** each on its own port (`18091` and `18092`, overridable with
  `RELEASE_COMPARE_RUNNING_PORT` and `RELEASE_COMPARE_CANDIDATE_PORT`), in its own process group,
  with a throwaway token and credentials per run and `TZ=UTC`; equal ports are refused. The token
  and the credentials travel in the environment, never in a command's arguments.
  - The scheduled job fails to scrape with the throwaway credentials and recomputes anyway; the
    script waits for `lastSuccessfulRecompute` on the admin status (`RELEASE_COMPARE_TIMEOUT`,
    default 900 s), fetches both sides, stops both (TERM, then KILL after `RELEASE_COMPARE_GRACE`,
    default 10 s) and only then compares.
  - A side that reports only `lastRecomputeFailure`, or that stops running, ends the run at once;
    every request has a socket timeout, so a side that accepts and never answers cannot hold it.
  - Each side is paced to 25 requests per 10 s (the limit is 30); after a 429 the script waits
    65 s and retries, at most 3 times.
- **What it reads:** every page of `/api/v1/avatars` with its totals, every member's bank and
  storage ledger, `/api/v1/admin/status`, and each copy's exact sums from `metaInformation`
  (read-only, after both sides are stopped).
  - The exact sums couple the script to the internal meta key names (decision 2026-10-10); in
    return rounding, the ammunition credit and last-bit drift are rules over the exact values: the
    ammunition delta is recomputed from the member's deposits and may differ from the stored one
    only by float accumulation over that member's entries, and any other exact storage sum may drift
    by as much, `n + 4` ulp with n the member's storage entries (decision 2026-10-10).
- **What it compares:**
  - that both sides recomputed in this run: a side without `lastSuccessfulRecompute`, or a member
    whose `sums_recomputed_at_` in that side's copy predates the run's start, is a finding; a member
    with no stored sums on either side is exempt (decision 2026-10-10), so the not-yet-computed rule
    can apply;
  - every row and totals figure, the members each side lists and `totalCount`;
  - each ledger as a multiset of whole entries over every page, so a different order is no finding;
  - the field sets of rows, totals, admin status and ledger entries;
  - the admin status name lists as multisets; the five run instants differ by construction and are
    not compared (the report says so).
  - `0.2.0` serves no `balance` or storage value; both are derived from its rounded figures as its
    overview did.
- **Expected deviations:** `release/expected_deviations.py` lists each rule with its name and the
  decisions behind it; every difference outside the list is a finding. The report names each rule
  that applied with its decision dates and lists the rules that did not occur, which does not fail
  the run.
- **Exit status:** `0` no finding (only listed deviations), `1` at least one finding, `2` the call
  was refused (missing snapshot or distribution, a snapshot with a `-wal` or `-journal` file
  beside it, a non-numeric or equal port, a non-numeric timeout, poll or grace, a script outside a
  git worktree, a side of a previous run still alive, named by its process group), `3`
  a side showed no successful recompute in time, stopped running, could not be fetched or could
  not be stopped (its process group is named), `4` the
  comparison itself crashed (a malformed capture, a copy without `metaInformation`). Both sides are
  gone after every exit path, a hangup and a second interrupt included; a signal ends the run with
  128 plus its number once both sides are stopped.
- **The script's own proof:** `sh release/self-test` runs the unit tests and the driver tests on a
  stub distribution (under a minute, synthetic data only); `pre-commit` runs it for every commit that
  touches `release/`.
- **The report** lists every finding, then per expected-deviation rule each acceptance and their
  count (a shown figure or an exact sum with both values, a wire field only the candidate serves by
  name), then the rules that did not occur.

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
  Measured on the 03.09.2026 snapshot: **15 of its 513 distinct storage item names**, covering
  **31 of 243 443 rows** (0.013 %) and 1 263 units moved. Thirteen are gem-forged gear no complete
  read of the game priced, recorded as `unpriced` with that read's date in the game facts below;
  two are parser misses rather than items, a `Gold` row and a consent-banner sentence, both read
  into one 2024-05-29 entry. Re-measured 2026-09-22: the same 15 names over the same 31 rows, and
  the guild position below reproduces to the gold, so recording them moves no sum.
- **Closing the catalog against the game moves a third of the guild.** Measured on the 03.09.2026
  snapshot by running the recompute check on each side of the change: `Gildenspende` rises by
  `2 478 114`, storage deposits by `6 318 321` and storage withdrawals by `3 883 118`, so the net
  after deductions rises by `2 435 203` to the guild position below and **35 of 42** avatars
  change. One member crosses from a negative contribution to a positive one. Distinct unknown item
  names fall from **124 to 15** and the rows they cover from **1 684 to 31**; a further 17 names
  over 690 rows are known and deliberately worth nothing, which `/health` counts apart from the
  unknown ones.
- **The three raw stones' correction is measured, not asserted.** Two independent methods agree to the
  gold on the 03.09.2026 snapshot: a catalog-driven SQL sum over the `Marmor`, `Granit` and
  `Schiefer` rows, and the recompute check run once on each side of the correction. The guild
  `donation` rises by `2 478 114`, storage withdrawal by `371 016`, and the net after deductions
  falls by the same `371 016`, which puts `Gildenlagerwert` `2 107 098` higher; 3 of 42 avatars
  move, one of them carrying most of it. 591 storage rows gain a value, and the distinct
  unknown-item names `/health` reports fall from 124 to 121.
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
- **Re-measured 2026-09-23**, under the current catalog: of the 252 value keys the recompute
  writes (six per avatar, without `last_updated` and `sums_recomputed_at_*`), every bank key of the
  03.09.2026 snapshot reproduces, the 84 craft-subsidy and donation keys are absent from the
  snapshot, and 60 of its 84 storage placement and withdrawal keys differ. A release re-proves 1:1
  through the scripted comparison above.
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
  not closed by the scripted comparison either: it holds a candidate against the previous release,
  so an error both carry passes it. The catalog gap above is a known instance: both sides
  value an unknown item at zero, so it reproduces perfectly and this check stays silent on it.
- **Not reproducible:** the per-avatar ratio band of `0.12`-`1.33` first reported from the 31.07.2026 file. No quantity tried (per key, per
  family, gross, deposits, withdrawals, net) yields a `0.12` lower bound on any surviving snapshot;
  per-avatar `net` ratios are unbounded because `net` crosses zero. The file that band came from
  (`last_updated` 31.07.2026) is no longer on disk, the root snapshot having been replaced on
  2026-09-03, so that band cannot be re-derived. Its Fugger figures do reconcile:
  `66 141 289` → `94 063 645` is
  the **same** `+27 922 356` gap measured above.

### The guild position on real data (2026-09-10)

Measured with the same opt-in check after the valuation moved to the announced rule, against the
03.09.2026 snapshot, 42 avatars, read out of the check's own
`overview-after-recompute.json` rather than computed beside it. The table stands for the catalog as
closed against the game on 2026-09-11, for every figure rounded once from its exact value and for
the full credit of the ammunition the game's own trader sells (re-measured 2026-10-01):

| figure | value |
| --- | --- |
| Gildenbank (bank in less bank out, measured gold) | `119.334.247` |
| Gildenlagerwert (what the storage holds at the guild's own price) | `25.145.110` |
| Gildenspende (deposits the guild credits nothing for) | `107.075.238` |
| Handwerkssubventionen (what the guild credits above its own price) | `43.437.570` |
| Einlagerung, the table's total row | `283.286.443` |
| Nach Abzügen, the table's total row | `80.841.689` |
| Vor Abzügen, the table's total row | `144.479.357` |

- The identity `net = Gildenbank + Gildenlagerwert - Gildenspende + Handwerkssubventionen` holds
  exactly on the exact values and within two gold on the shown ones, which is what makes the header
  checkable against the table; the check's reconciliation asserts that tolerance. On this snapshot
  the guild total and 37 of the 42 rows reconcile exactly; the other 5, whose net rounds
  differently from their rounded storage sums, are one gold off.
- The guild's trader lands at `+1.456.343` (re-measured 2026-10-01) on about 90 Mio of goods moved,
  the break-even the 100 % credit is meant to produce, and the split says why: `38.758.510` of the
  guild's whole `43.437.570` subsidy is that one avatar's, against a donation of only `897.138`.
- The full credit of `Pfeile`, `Bolzen` and `Magieessenz` raised `Handwerkssubventionen`, the
  table's `Einlagerung` and `Nach Abzügen` by `3.995.648` each and left `Gildenlagerwert`,
  `Gildenspende` and `Vor Abzügen` where they were: `3.645.942` of it reached the trader, who moved
  from `-2.189.599`, and `349.706` the other members, whose 41 rounded rows add up to `349.704`.

### The ledger index on real data (2026-09-27)

Measured on copies of the 03.09.2026 snapshot (7,688 bank, 243,443 storage, 169 meta rows), one
migrated to `V2` as production runs it, one taken on through `V3` by
`ProductionSnapshotMigrationCheck`. The queries are the adapter's own shapes for the busiest avatar
(860 bank, 13,739 storage rows), page size 20; median of 200 warm runs, whole file in SQLite's page
cache, Python's `sqlite3` (SQLite 3.40.1; the application runs sqlite-jdbc's 3.41.2, whose plans are
the same lines) on the devcontainer's `/workspaces` bind mount.

| storage ledger | at `V2` | at `V3` | plan at `V3` |
| --- | --- | --- | --- |
| ledger page (`getAllFor(avatar, page, size)`) | 11.8 ms | 0.015 ms | `SEARCH … USING INDEX` |
| `countFor` | 8.0 ms | 0.30 ms (~27×) | `SEARCH … USING COVERING INDEX` |
| `latestTimestampPerAvatar` | 46.8 ms | 14.4 ms | `SCAN … USING COVERING INDEX`, no temp B-tree |

- The figures are query cost inside one open read transaction. A statement in autocommit, which is
  how the adapter runs, adds about 0.58 ms of file locking on this mount (a trivial statement:
  0.583 ms autocommit, 0.001 ms inside a transaction); measured in autocommit, the storage page
  reads 13.1 ms → 0.62 ms.
- The bank ledger, 32× smaller, gains the same way: page 0.42 → 0.011 ms, count 0.25 → 0.019 ms,
  latest 1.66 → 0.72 ms.
- At `V2`, every one of the six plans is a `SCAN` of the whole table, with a temp B-tree for the page
  and the grouped query.
- The table measures the ledger page before it took a sort. The page now orders by the chosen
  column, then `timeStamp` descending, then `id` ([architecture.md](architecture.md)); its plan
  adds `USE TEMP B-TREE FOR RIGHT PART OF ORDER BY` in the default order and `USE TEMP B-TREE FOR
  ORDER BY` for any other column, and its cost on the snapshot is not yet measured (**E25**).
- The index sets the order of a read that names none: `getAllFor(avatar)` returns an avatar's rows in time order, so
  the recompute adds its `double` sums in that order: on the snapshot one recomputed sum,
  `storageWithdrawl` of one avatar, moves in its last bit (`…002E7` → `…001E7`), and the other 167
  of the keys the snapshot stores come out bitwise equal (the domain gate's probe through the real
  repository and catalog). The overview's whole gold is unchanged, and so is the snapshot recompute
  check's count of changed keys (its 60 come from the catalog having moved since the snapshot);
  only that one key's recomputed figure differs.

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
  `OnDemandCheckOptInTest` pins that for every on-demand check at once, finding them by their
  `@EnabledIfSystemProperty` annotation on the classpath rather than from a hand-kept list, and
  pinning how many it finds so a check cannot slip past the discovery. It still compiles under
  `-Werror`, so a refactor cannot rot it unnoticed.
- **To run it:**

  ```bash
  EVERGORE_SECURITY_API_TOKEN=test-token ./gradlew test \
      --tests '*ProductionSnapshotRecomputeCheck*' \
      -DprodSnapshot.check=true -DprodSnapshot.file=temp.sqlite
  ```

  `prodSnapshot.file` defaults to `temp.sqlite` and takes any path, so a second snapshot is measured
  by naming it, not by editing the check. **The path is a cache-key input, the file's content is
  not**: replacing the dump at the same path and re-running serves `:test FROM-CACHE` and writes no
  artifacts at all, so a re-measurement reads as a pass without touching a byte. Pass
  `--rerun --no-build-cache` whenever the figures themselves are the point. Artefacts land in `build/tmp/prodSnapshot/`. Opting in
  without a snapshot at that path fails on the missing file instead of passing empty.
- It writes only under `build/`, never to the snapshot. Always copy the snapshot; never open the
  original read-write.
- Every other copy of production data, a probe's or a gate agent's, also lives only under the
  worktree's own `build/`, never in `/tmp` or a session scratchpad: `clean` and
  `git worktree remove` take it away with the worktree, while a copy outside it outlives the
  session and carries the guild's personal data.

### The game-catalog scrape

`GameCatalogScrapeCheck` signs in to the live game and dumps the pages the item catalog is read
from, so a catalogued price can be checked against the game rather than against memory. It is the
only instrument that can settle what an item is worth; without it every later catalog entry is an
assertion. It reads and navigates, and submits no form beyond the login the production scraper
already performs. Opt-in on the same pattern as the snapshot harness, and pinned by the same
`OnDemandCheckOptInTest`.

- **It signs in the way production does, through `EvergoreSession`**, and builds its browser through
  `Driver` rather than reaching for `Browser` itself. The check therefore holds no field id, submit
  xpath, consent selector or wait duration of its own: a changed login is corrected in one place, and
  the check does not quietly skip `prepareLocalDriver` if `Configuration.browser` ever moves off
  `docker`. `EvergoreSession` is production's own sign-in with production as its primary caller, not
  a seam opened for the check.

- **To run it:**

  ```bash
  ./run-game-scrape.sh '-DgameCatalog.pages=stock_out&selection=7,academy_craft&selection=52'
  ```

  Quote the argument: real page parameters carry `&`, and unquoted the shell backgrounds the run.
  The script reads the credentials file (`zugang.txt` beside the main checkout, line 1 the user,
  line 2 the password; `EVERGORE_CREDENTIALS_FILE` overrides the location), exports them as
  `EVERGORE_CREDENTIALS_USERNAME`/`_PASSWORD`, and opts the check in. It resolves the file from
  `git rev-parse --git-common-dir`, so it works the same from a worktree as from the main checkout.
  Each page named in the comma-separated `gameCatalog.pages` is dumped three ways into
  `build/tmp/gameCatalog/`: `.txt` (the rendered text, which is what the values are read from),
  `.html` and `.tsv` (every link with its target, which is how the `selection` numbers are found).
- **It sweeps before it opens a browser**, like the browser smoke tests: an interrupted scrape leaks geckodriver and Firefox exactly the same way, so the next run kills and names what the last one left (`OrphanedBrowserSweepTest` in the inventory).
- **The credentials file is CRLF on the author's machine, and the script strips the `\r`.** Without
  that, `sed -n 1p` leaves a carriage return on the username, WebDriver normalises it to the Enter
  key, the login form submits before the password field is filled, and the run then sits silently in
  `waitForUrl` for the full minute before failing.
- **A repeat of the same scrape would otherwise not scrape.** Gradle finds `:test` up to date for an
  unchanged task input and reports `BUILD SUCCESSFUL` without opening a browser, while the previous
  run's dumps sit in `build/tmp/gameCatalog/` looking current (the directory is no declared output).
  The script passes `--rerun` for exactly that reason; changing `gameCatalog.pages` re-runs anyway.
- **Where the prices are.** The guild storage, `page=stock_out&selection=<n>&pos=<n>`: the
  `selection` numbers are the links on the first such page, and its `(Gesamt: N)` gives how many
  rows that selection holds, twenty to a page. Stock held at quality 100 and stored blueprints both
  carry the item's own gold value, so either serves as the price.
- **A `stock_out&selection=<n>` named with no `pos=` is read in full.** The fetch loop reads the
  `(Gesamt: N)` that sits in parentheses on the selection's first page and fetches every `pos` up
  to `ceil(N / 20)` itself, twenty rows to a page, capped at 100 pages (a row count beyond that is
  rejected before it is divided, so it fails loudly rather than risking an overflowed page count);
  a first page that carries no such total fails loudly too, rather than being read as one page and
  passing green. Naming `pos=` explicitly still fetches exactly that one page (a leading space
  before it, `&selection=7& pos=2`, still counts as explicit); the bare `stock_out` overview page
  (no `&selection=`) is always fetched once, unpaged, since it is the index the `selection` numbers
  are read from, not a selection itself. `academy_craft` and every other page outside `stock_out` is
  fetched exactly as named, unpaged. This does **not** cover `guild_protocol`/`town_protocol`, the
  two pages `SeleniumPageSource` itself pages through in production: those paginate by whether a
  page still carries a recognizable entry line, not by a `(Gesamt: N)` total, so this check's
  total-based paging does not apply to them; naming either here still reads page one only. Every
  dump name is checked against every other one actually written **within the same run**, so an
  auto-paged and an explicitly named `pos=` can never silently overwrite each other in one
  invocation; a stale dump from an earlier, larger run can still linger in `build/tmp/gameCatalog/`
  since nothing clears that directory between runs. **Known limitation, unverified against a real
  page:** the total is read from the first `(Gesamt: N)` found anywhere in the page's text; no page
  dump is committed (the dumps carry host/guild data, see below), so whether a real
  `stock_out` page ever carries a second parenthesized total ahead of the selection's own is not
  established either way. The loop's stopping point is the pure function pair
  `pagePositions(String)`/`pageRequestsFor(String, String)`, the auto-page/single-fetch decision is
  `isAPagedStorageSelection(String)`, and the collision guard is `recordDumpName(String)`; all are
  unit-tested directly (`GameCatalogScrapePagePositionsTest`, `GameCatalogScrapePageRequestsTest`,
  `GameCatalogScrapePagedSelectionTest`, `GameCatalogScrapeDumpNameGuardTest`) without a browser.
- **Where the recipes are.** `page=academy_craft&selection=51..67`, seventeen crafts holding 424
  blueprints between them (read 2026-09-12). It lists **every** blueprint with its ingredients
  regardless of the account's own skill level, which is why it is used instead of `page=craft`: that
  one is locked, the scraping account standing at level 0 in every craft. Take the `selection`
  numbers from any craft page's own `.tsv` rather than counting: the range starts at **51**, and a
  craft page omits its own link, so no single page lists all seventeen. **Gem-forged gear appears in
  none of them**: those are crafted from a blueprint learned as an item, not from an academy recipe,
  so the academy can never show their ingredients and this route cannot attest a gem recipe. That is
  what settled the two `Achat-` recipes the catalog used to hold. A gem blueprint *is* visible in
  the guild storage, priced like the item it makes, but listed without its ingredients.
- **Two line formats carry a value, and both have to be read**:
  `<kg>, <category>, Stufe <n> <gold> Gold` and `<kg> <gold> Gold / Stk.`. Ammunition uses the
  second, without a category, which is how `Steinbrecher` and `Jagdpfeile` were first missed and
  left catalogued at zero.
- **The dump does not survive a clean build**: `build/tmp/gameCatalog/` goes with `build/`. Copy it
  aside before running one, or the evidence a catalog change rests on is gone.

### The recorded game facts

`src/test/resources/gameCatalog/game-facts.tsv` is the committed, diffable record of what the scrape
established, so a catalog value can be re-checked without signing into the game again. One fact per
line, tab separated, `kind item value ingredients source read`: a `price` row carries the gold value
and an empty ingredients column, a `recipe` row carries the yield in `value` and `5 Buchenholz + 1
Harz` in `ingredients`. `source` is the page parameter the fact was read from
(`stock_out&selection=4&pos=1`, `market_all_articles`, `academy_craft&selection=58`) and `read` the
date, **per line**, so a later partial re-scrape re-dates only the rows it touched.

- **Only derived facts, never the dumps.** The raw pages carry guild member names, `Lagerzugriff`
  lines and `Hergestellt von <member> für <member>` clauses, which is host data (handbook §1–§3) and
  is never committed. `GameFactsGuardTest` guards the file itself against those markers.
- **An `unpriced` row records an absence**: a name the ledger holds that neither complete paged
  read priced, with `stock_out+market_all_articles` as its source and that read's date, so "the
  game has not priced it" is a dated claim rather than a gap. `GameFactsGuardTest` names the 13
  such rows and fails once a price row or a catalog entry for one of them appears, which is when
  the row has to go. A price is never filled in from the wiki or the gem price ladder.
- **An item priced by two pages keeps both rows**, so the storage and the market corroborate each
  other instead of one silently replacing the other; 178 names are priced by both and none disagree.
- **A price is read from a holding at quality 100 only**, because the storage scales a holding's
  gold with its quality (an `Äther-Spitzhut` at quality 20 lists a fifth of its value).
- **Both value line shapes have to be read.** `<kg>, <category>, Stufe <n> <gold> Gold` is the
  common one, but raw materials, gems, crafting material and **ammunition** use
  `<kg> <gold> Gold / Stk.` with no category, and crafted gear inserts a third clause,
  `, Hergestellt von … für …`, before the gold. Missing the second shape is what once left
  `Steinbrecher` and `Jagdpfeile` catalogued below the game; missing the third hides 109 of the
  storage's 560 rows. The extraction is checked against the game's own `(Gesamt: N)` totals for
  exactly that reason: 560 of 560 storage rows and 232 of 232 market offers were read.
- **Re-deriving it means one complete paged run, not a question per value**: the storage's 28
  selections auto-paged, `market_all_articles&selection=0&pos=1..N` named explicitly (the check only
  auto-pages `stock_out`), and `academy_craft&selection=51..67`.

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

The gateway build is **`./verify all`**, which runs `./gradlew clean build --no-build-cache` and
prints the executed test-class count ([build-run-deploy.md](build-run-deploy.md)). All three parts
of the Gradle command are load-bearing:

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
- **A test for an injected seam asserts that the seam was driven**, not only the outcome it enables:
  count the fake's calls, read the fake clock. If the assertion would still pass with the production
  collaborator wired in, the test is fake-green.
- **BDD comes first and is mandatory** (handbook §5, author decision 2026-09-20): a feature with
  observable behavior begins with Gherkin scenarios in `src/test/resources/features/`, gated by the
  scenario gate, **confirmed by the author as the complete acceptance**, and committed `@wip`
  before any production code; TDD cycles drive them green, step definitions in
  `dev.schoenberg.evergore.protocolParser.acceptance` included, and the feature is armed (`@wip`
  removed) when they pass. `RunAcceptanceScenariosTest` is the one acceptance runner for the whole
  system, backend and SPA; the frontend keeps its Vitest unit tests only. Its step definitions
  drive the member and the admin through the browser and the operator over HTTP and JSON
  (handbook §5). `./verify bdd` runs the `@wip` scenarios, `./verify all` the armed ones; a
  `@characterization` scenario awaiting the
  author's confirmation (`/bdd-catch-up`) is excluded from `all` like `@wip`. Scenario language:
  English, with the game's German names quoted as the game spells them. The catch-up's
  scenarios sit in seven cluster folders (`overview/`, `ledgers/`, `valuation/`, `round_trips/`,
  `collection/`, `dashboard/`, `operations/`), committed `@wip` until their step definitions exist;
  a scenario the author has not settled carries `@characterization` besides. The
  collect→evaluate→overview flow is covered today by `ProtocolEvaluationAcceptanceTest` (scraper
  stubbed, real evaluation, asserted via HTTP + the meta repo).
