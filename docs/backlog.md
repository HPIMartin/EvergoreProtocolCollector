# Backlog: Evergore Protocol Collector

> **Status: v2 (2026-06-13), strategic questions answered.** Scope = feature-parity dashboard
> (Epic E) **+** clean-code craftsmanship **+** a *showcase of working with Claude* (Epic G).
> Status is "live but learning/fun, no production pressure", so Epic C/F are good-practice, not
> urgent. Architecture: targeted fixes now, full repackaging (D3) deferred until tested. Decisions
> in [open-questions.md](open-questions.md); knowledge base in [knowledge-base/README.md](knowledge-base/README.md).

## ▶ Current status / next action

`git log` is the record of what landed; this section tracks only **where we are and what's next**.
**Backlog convention:** completed items are **removed** (the commits show them, no `DONE` tombstones);
only **rejected/deferred** items stay, with their decision + rationale (knowledge git doesn't hold).
Item IDs live **only here**; removing an item also removes **every shortcode that referenced it** (in other
rows' prose and across the docs), so no dangling code is left behind (handbook §3).

**Where we are:** the rebuild's core is in place on an up-to-date stack (Gradle / Java 25 / Micronaut
4.10), verified **1:1** against the production DB; single public `main` branch. The framework-free core
is ArchUnit-guarded, style is enforced from one place, an offline acceptance test covers
evaluate→overview against a synthetic committed fixture, startup and boot tests are deterministic
(no-timeout latch), and observability (anonymous `/health`, config-driven rate-limiting) is in place.
The *how* lives in the KB ([architecture](knowledge-base/architecture.md) ·
[testing](knowledge-base/testing.md) · [build-run-deploy](knowledge-base/build-run-deploy.md) ·
[frontend](knowledge-base/frontend.md)); decisions + rationale in [open-questions.md](open-questions.md);
the code is the source of truth for the rest. A one-off conformance audit (2026-06-27) filed its
remaining finding as **D9**.

**Next action:** **B16**/**B20** finish M4, then **E1**/**E2**, then **H2**+**H6**; **B19** comes
before **H9**, which also waits on 1:1 being re-proven. Milestone
cuts + acceptance: [roadmap.md](roadmap.md); risk register: [risks.md](risks.md). Plan via the agent
pipeline (planner → implementer → falsifier panel → reviewer). *(A4/CI stays deprioritized:
local-only Docker → home-server deploy.)*

**Deferred frontend follow-ups (non-blocking):**
- **React-hook lint:** no `eslint-plugin-react-hooks` is declared, so nothing enforces `rules-of-hooks` or
  `exhaustive-deps` on the SPA's hooks. Adding it needs a call on `useLoad`, whose effect depends on the
  request key alone **on purpose** (the key is the request's full identity), which `exhaustive-deps` flags.
- **Test-helper duplication:** `tonesOf` stands byte-identical in `frontend/src/ui/SortableTable.test.tsx`
  and `frontend/src/app/App.test.tsx`. A shared helper needs a home that is not production code without a
  test of its own (same shape of problem as the duplicated test boot setup).
- **A column that offers sorting must be able to sort:** the overview's `Lager` column is a link column
  whose text is the same word in every row, so its header renders a sort button that can never reorder
  anything. Either `Column` gains a way to opt out of sorting, or such a column sorts by another field.
- **Row identity:** an entry carries no id on the wire, so a ledger row is keyed by its position in the
  loaded page. That holds while a page is replaced as a whole (sorting reorders the same objects), but
  paging **inside** a loaded page needs a real identity per entry first, which is a contract question.
- **Hook hardening:** the pre-commit host-path scan can be prefix-squatted (`/home/<allowed>/home/<real>/…`
  slips past `grep -oE`): relates to **G7** / **G13**. The `/home/app` Dockerfile exemption (commit `cc75a2e`)
  itself was reviewed as necessary and correct.

**Gotchas worth keeping:**
- The IDE re-saves edited files as **CRLF**; `.gitattributes` normalizes to LF on commit. Ignore the
  warning, `git diff --check` if unsure. (The new build serves CRLF if the working-tree templates are
  CRLF; a clean LF checkout serves LF; the 1:1 check normalises line endings.)
- **Java 25 made `java.sql.Timestamp.from` strict** (`Math.multiplyExact` throws where JDK 17
  wrapped): converting an extreme instant (e.g. `LocalDateTime.MIN`) to `java.sql.Timestamp` throws;
  avoid extreme sentinel instants in any code that converts to `Timestamp`.
  **ArchUnit must be ≥ 1.4.1** to read Java 25 bytecode (older silently checks zero classes = false green).
- The **devcontainer image itself is built by the host's Dev Containers extension**, so a change
  under `.devcontainer/` is only proven by the author's next rebuild; an agent cannot validate it.
  (The *production* `Dockerfile` is buildable from inside the container since
  `docker-outside-of-docker` returned.)
- The Claude "always allow" flow can re-pollute the committed `settings.json` (path-bearing rules +
  tabs→spaces). Prefer bare commands matching the portable `Bash(<cmd>:*)` rules; diff against HEAD if unsure.
  The worktree `git -C <abs-path>` re-prompt loop is fixed by the committed portable wildcard
  `Bash(git -C *.claude/worktrees/*)` (2026-06-28); still watch the tabs→spaces rewrite.
- **The shell's working directory drifts between worktrees.** With a strand worktree checked out next to
  `main`, a Bash call can silently run in the wrong one, and a relative path then reads or writes the wrong
  tree with no error at all (seen three times in the `json-api` gate, once losing two `process-learnings`
  rows into the main worktree while the branch stayed without them). Address the target explicitly in every
  call: `git -C <abs path> …`, absolute paths for reads, writes and Gradle. `git status --short` in **both**
  worktrees before a gateway claim. Mechanical enforcement is folded into **G7**. A drifted Gradle run is
  **green**, so an exit code proves nothing: count the test-result XMLs in the worktree's own `build/` and
  check the strand's new test classes are among them, or the suite silently ran without the change.
- Keep `zugang.txt` (creds, gitignored); machine-specific config stays in gitignored `*.local.*` files.
- **Commit from inside the devcontainer:** a Windows host session fails the pre-commit gate
  (`checkstyleMain` → `:frontend:npmBuild` → Linux-installed `node_modules`, no `tsc`); see the
  **G13** host-commit finding.

**Orient (any new session):** `CLAUDE.md` → `docs/knowledge-base/README.md` → **this section** (not the
whole backlog) → `docs/open-questions.md` for decisions touching your task. Section-scoped reading is
the rule: [working-with-ai-agents.md](knowledge-base/working-with-ai-agents.md) "Context & token hygiene".

## Priority legend & effort

`P0` unblock everything · `P1` correctness & safety · `P2` architecture & parity · `P3` product/ops growth.
Effort: `S` ≤½ day · `M` ~1–2 days · `L` ≥3 days. IDs are stable references.

---

## Working agreements (the "how", per the author's goals)

Canonical home: the [engineering handbook](knowledge-base/engineering-handbook.md) (clean code /
SOLID / hexagonal §1–§3, TDD §4, BDD §5, Definition of Done §8); not restated here.

---

## Epic A: Repo hygiene & foundation `P0`

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **A4** | ~~CI (GitHub Actions)~~ **Deprioritized 2026-06-15** (local-only deploy, no CI). *Optional later, low prio:* a **local** gate, git `pre-commit` running `mvn -B verify` | Solo + local Docker→home-server deploy; no shared PRs to guard | revisit if the repo ever goes shared/CI | S |

## Epic B: Lock the core with tests `P0→P1`

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **B4** | Repository tests against in-memory SQLite: paging, `getAllFor(avatar)` (`getAllSince` is already covered); **plus a `MetaInformationDatabaseRepository` adapter test** (setup review 2026-07-03: the business-critical `MetaInformationKey` round-trip is untested while Bank/Storage have adapter tests) | Only incidental smoke coverage | Fast tests using `:memory:`; the meta round-trip covered | M |
| **B8** | **Audit & align the existing test suite to the test-style conventions** (emerged 2026-06-29 reworking the parser contract tests): no logic buried in tests (no case-`record` + loop + soft-assert iteration); one named `@Test` per distinct behaviour with the expected literal in the assert; "one assert per test" = one assertion *case* (assert a composite value as a whole via an intention-revealing helper like `assertEntry`); minimal, noise-free input values; data-driven (`@ParameterizedTest`/`@MethodSource`) only where it genuinely fits (ask when unsure). **Codify the rules in the KB first** (handbook §6 / `testing.md`), then bring older tests (e.g. `SmokeTest`) into line. **Setup-review additions 2026-07-03:** delete the 12 record-equality test methods (they test the compiler, not the code; author decision) and rework the acceptance test's two index-coupled `anyMatch` assertions to the `containsExactly` style. **Author decisions 2026-08-05, as one cleanup commit:** rename the unit under test to `tested` in every test class that names it otherwise (`PathCanonicalizerTest`, `SpaNavigationPathsTest`, `SpaStaticResourcePathsTest`, `PublicPathsTest` and the older suite), and sweep the suite for behaviours pinned at more than one level, the way the `json-api` strand's three duplicate token assertions were removed | The conventions are newer than most of the suite; the showcase's tests should be exemplary and consistent | Rules documented in the KB; existing tests conform | M |
| **B16** | **`SeleniumPageSource` test hardening** (verification findings 2026-07-08): the unit test drives the real `WebDriverWait(driver, ofMinutes(1))` with the system sleeper/clock: it resolves on the first evaluation today, but a future URL/redirect mismatch would hang ~1 min per `wait()` on every machine before failing, against the no-timeouts rule (handbook §6); and the both-fail path (scrape throws *and* `quit()` throws → the scrape exception must propagate, both failures logged) is correct but not locked in by a test | Test-suite determinism and regression safety in the most fragile adapter | The wait has no wall-clock dependency in tests; a both-fail test pins the "scrape exception propagates, both logged" contract | S |
| **B21** | **A headline with single-digit date fields is absorbed into the preceding entry** (residual of the parser-robustness work, deliberately left out 2026-08-09): `LAGER_EINTRAG_BOUNDARY` needs two digits per field (`\d{2}`), so `1.1.2000 0:00 …` opens no block and its item lines are attributed to the entry above it. Widening the boundary changes which lines start an entry, so it can alter the parse output of already-stored entries and needs a reconciliation run (`StoredAvatarReconciliationTest` is the pattern) before it lands | Evergore zero-pads, so this needs corruption that also changes field widths, but the invariant it breaks is mis-attribution, the one failure mode that silently moves value between members | A single-digit headline opens its own block or is dropped with a warning; either way its items never land on the preceding avatar, and a reconciliation run shows no stored entry changing | S |
| **B22** | **`Constants.LAGER_EINTRAG_START` carries two contracts** (reviewer finding 2026-08-09): besides the parse rule it is `SeleniumPageSource.checkForContent`'s pagination terminator, so tightening the parser also tightens when a scrape stops paging, and nothing in the code or the tests records that second contract. Two questions ride on the same constant: whether the transfer type is always the **last** token of a headline (if so, anchoring it to the line end closes the last mis-attribution shape, `… Der Entnahme Meister Auszahlung`, which no character rule can reach), and whether an item line that matches but has an empty name (`5 (10)` yields `Item(5, "", 10)`) should be skipped. Both change the parse output of stored entries, so both need a reconciliation run before they land | One constant silently governing both a parser rule and a scrape-termination rule makes every parser tightening an untested change to the scraper; the residual mis-attribution is the failure mode that moves value between members without leaving a trace | The second contract has its own named constant or its own test, and both questions are answered with a reconciliation run behind them | S |
| **B18** | **Extract one shared `@MicronautTest` boot fixture** (reviewer finding 2026-08-06): the fixture-DB copy, the `Configuration`/`EvergoreDataExtractor` overrides and both hook `@MockBean`s are now duplicated near-verbatim across `SmokeTest`, `HealthEndpointTest`, `RateLimitFilterTest`, `SpaHistoryFallbackTest`, `ProtocolEvaluationAcceptanceTest`, `DashboardBrowserSmokeTest` and `ApplicationExceptionHandlerHttpTest` | The per-class `@MockBean` set is exactly what forces a fresh context, so sharing it cuts the duplication **and** the number of boots, which dominate suite runtime | One fixture holds the boot setup; each test declares only what it varies; the number of context boots drops | M |
| **B19** | **Script the 1:1 value comparison** (falsifier finding 2026-08-08): the release gate lost its byte diff when the HTML pages went, and testing.md now prescribes a **human** per-avatar comparison of the candidate's JSON against the live instance's rendered pages | A gate a person performs by reading is weaker than one that goes red on its own, and it guards the storage-valuation drift the recompute is there to correct | A harness reads both sides, compares per-avatar values and each ledger's entries, and reports a non-empty delta as a failure | M |
| **B20** | **`SmokeTest` failed once with `SQLITE_IOERR_DELETE_NOENT` under full-build load and the cause is not established** (2026-08-08): a `clean build` went red in three of its six cases (table create failing on I/O, meta reads coming back empty, the collector recording an exception) while five standalone runs of the class were green. The reset-ordering explanation was **disproven** by a falsifier probe (the eager bean graph forces the test class's initializer to complete before any repository resolves a path), and a leftover journal sidecar is ruled out by the preceding `clean`. Hardening landed anyway (sidecar deletion, `awaitCollection()` before seeding), but it targets no proven cause | An unexplained red build is a live risk: the next occurrence looks like a flake to wave through, and this suite has twice paid for that with `forkEvery` crutches | Either the cause is identified and pinned by a test, or the failure is reproduced often enough to characterize it (e.g. under deliberate I/O load in the devcontainer's WSL2 mount) | S |

## Epic C: Configuration & security `P1`  *(good-practice showcase; not urgent while learning/fun)*

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **C1** | Make `Configuration` real via `@ConfigurationProperties` bound from `application.yml`/env (browser, server, db path, credentials path, in-memory toggle, **+ the hard-coded Firefox binary path in `Browser.java`**). The conformance audit also flagged the **public mutable fields** `useInMemory`/`DATABASE_TEMP_SQLITE` and the Windows `c:\evergore` default path: fold these in (immutable record/port, no host paths) | Hard-coded fields defeat config & deployability; mutable public config breaks "prefer immutable records" | No domain settings hard-coded in `.java`; overridable by env; config is immutable | M |
| **C3** | Stop baking `zugang.txt` into the image; inject credentials via env/secret/mount; read via `FileLoader` port | Credentials in the image is a leak | Image has no credentials; documented secret-injection path | M |
| **C7** | **Rate-limiter cleanup** (setup review 2026-07-03): `BrowserLoggingFilter` is the rate limiter in disguise; rename it to say what it does, and bound/evict the per-IP counter map (it grows without limit). No `X-Forwarded-For` handling needed: the service is directly reachable, no reverse proxy (author decision 2026-07-03). **Falsifier finding 2026-08-05:** the skip list is a *second* definition of the public shell surface, hardcoded in `SpaStaticResourcePaths` (`/`, `/index.html`, `/assets/**`) next to the config-driven `evergore.security.public-paths`, with nothing keeping them in sync; dropping `/assets/**` from the config would leave assets token-protected but unaudited. Derive the skip list from one source and cross-check it in a test | A security component with a misleading name and unbounded memory growth | Intention-revealing name; the map is bounded/evicted | S |
| **C10** | **A malformed request target is answered before the rate limiter and the request log see it** (falsifier finding 2026-08-05, verified in the `json-api` gate): Micronaut answers `/overview%zz` (any invalid percent-escape) with **400** ahead of the filter chain, so `BrowserLoggingFilter` neither counts nor logs it. A caller can therefore send unlimited malformed requests without appearing in the request log or the per-IP counter. Not fixable in our filters, which never run: needs a Netty/Micronaut-level seam. **Author decision 2026-08-05: every client is counted on every request, without exception** — an attacker must not be able to probe which shapes answer how without appearing in the counter and the log. The "accept the gap" alternative is therefore off the table. Pinned by `RateLimitFilterTest.doesNotCountAMalformedRequestTargetBecauseTheFrameworkAnswersItFirst` and by a five-target `@ParameterizedTest` in `TokenScopeTest`; both **characterize Micronaut, not our filters**, and go red if a framework upgrade changes the behaviour. Verified 2026-08-05 by log evidence: a malformed target produces no `Client IP` line at all, while the well-formed control `/overview%2e%2e` answers 401 and logs | An unlogged, uncounted request path defeats both the throttle and the audit trail | Every request counts against its client's rate limit and appears in the request log, whatever its target looks like; a burst of malformed targets earns a 429 exactly like a burst of well-formed ones | M |
| **C11** | **Compare the API token in constant time** (falsifier finding 2026-08-05, pre-existing): `TokenValidationFilter` uses `token.equals(securityConfiguration.apiToken())`, whose runtime depends on the shared prefix length, so it leaks the token byte by byte to a patient caller. Use `MessageDigest.isEqual` on the UTF-8 bytes | A timing side-channel on the only authentication secret the service has | The comparison runs in time independent of how much of the token matches | S |
| **C8** | **Drive `./gradlew vulnScan` findings to zero, then gate on any finding** (author request 2026-07-04): bump the direct dependencies (e.g. selenium 4.7.2, sqlite-jdbc, ormlite are dated) and pin/constrain transitives until the Trivy report is clean; **no Micronaut major jump** (that stays deferred, H9): respect Micronaut-managed versions. CVEs without a sane fix go into a reviewed **`.trivyignore`** (CVE + justification + expiry) so exceptions stay temporary. Finish by setting `vulnScan.failOnSeverity=UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL` in `gradle.properties` and updating build-run-deploy.md to the gating default | The scan exists but tolerates all findings; zero-vuln + explicit whitelist makes drift visible | `vulnScan` is green with the gate on; every ignored CVE has a justification and expiry | M |

## Epic D: Hexagonal completion `P1→P2`

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **D3** | Restructure packages to `domain / application / adapters{in,out} / config`; keep core framework-free; fold in the package-naming cleanup (lowercase the camelCase packages; root package `protocolParser` vs the Collector repo name) | Make the boundaries explicit & enforceable | Micronaut/Selenium/ORMLite imports only under `adapters`+`config` | L |
| **D9** | **Naming/consistency follow-ups** (conformance-audit findings): (a) fix the pervasive `withdrawl`→`withdrawal` misspelling in identifiers **and** DB key strings (`bank_withdrawl_`→`bank_withdrawal_`, `storage_withdrawl_`→`storage_withdrawal_`, needs a one-time key migration); (b) rename the German identifiers `PageContents.lager`/`updateLagerEntries`/`Constants.LAGER_EINTRAG_START` → `storage`/`STORAGE_ENTRY_PATTERN`; (c) move `Logger` to the last constructor param in the `Repository` base + its three subclasses; (d) rename `TransferTypeDatabaseVisitor`, it is a converter, not a visitor (setup review 2026-07-03) | English-only + intention-revealing-name rules; consistency with `StorageEntry`/`StorageRepository` | Identifiers corrected; DB migration keeps values 1:1 | S |
| **D6** | **Refactor the `EvergoreItem` catalog in-place**: the ~660-constant enum (752 lines) is the project's biggest file. **Decision 2026-06-19: keep it an enum** (the compile-checked recipe hard-links are valued) and **do NOT externalize** to a config/YAML file. Refactor for readability/maintainability instead, e.g.: pull the nested `Category`/`Recipe`/`Ingredient` types into their own files; add concise recipe/ingredient factory helpers to shorten each constant; group constants. Approach TBD with the author. Any change **must** keep a **golden-master test** asserting `getStorageValue`/`getWithdrawlValue` are unchanged for all items | Tame the biggest file without losing the type-safe cross-references; eases value maintenance (relates to **D-3**) | P3 | Golden-master test gates it (1:1). |
| **D10** | **Introduce a schema-migration framework** (author decision 2026-07-03): evaluate Flyway vs Liquibase for SQLite/ORMLite fit; versioned, repeatable migrations instead of ad-hoc `ensureTable()` DDL. Gates **D9**(a)'s key migration. Historical data **must** survive every migration: Evergore serves only the last 30 days of logs, so the DB is the only history | Epic E grows the schema and D9 renames keys; both need a verifiable path that cannot lose the irreplaceable history | Framework chosen and wired; the D9 key rename runs as its first migration, verified 1:1 via golden master | M |
| **D11** | **Exception & logging hygiene, dead code** (setup review 2026-07-03): `ExceptionWrapper` catches `Throwable` and rethrows bare `RuntimeException`; `EvergoreDataCollectorJob` swallows `InterruptedException` without restoring the interrupt flag; adapters throw bare `RuntimeException` past the curated `exceptions/` hierarchy (`Browser`, `DiscFileLoader`, `ResourceFileLoader`, `TransferTypeDatabaseVisitor`); `Slf4jLogger` does a full stack-walk with a hard-coded index on every log call; dead code (`Entry.print()`, the `toBeParsed.txt` placeholder baked into the jar, the unreachable `onUnknown` in the exception visitor) | Swallowed interrupts and bare exceptions defeat the curated error model; dead code violates the working agreements | Interrupt flag restored; adapters use `exceptions/`; dead code gone | M |
| **D12** | **Unify the ~95 % duplicated Bank/Storage repositories** (setup review 2026-07-03): the copy has already bitten, `BankDatabaseRepository` imports `StorageDatabaseEntry.TIMESTAMP_COLUMN` into bank queries (works only because both constants happen to be `"timeStamp"`); `countFor` is now a third duplicated read method (author decision 2026-08-04: leave it duplicated rather than half-unify it inside the `json-api` strand); three `JdbcConnectionSource`s on the same SQLite file are never closed. Extract the shared base, fix the cross-import, use one `@Singleton` connection source with `busy_timeout` | A latent wrong-query bug plus a resource/locking hazard | No cross-entity constant use; one managed connection source; the duplication collapsed | M |
| **D14** | **Store timestamps in an epoch/UTC format instead of default-TZ wall-clock strings** (author decision 2026-07-19): the `DATE_STRING` columns persist wall-clock text in the JVM default TZ (pinned nowhere); in a DST-zone fall-back hour round-trip equality and the lexicographic max break (proven in jshell under `Europe/Berlin`), which would corrupt the ingest window dedup that relies on value equality. Needs a data migration → gate on **D10**. Until it lands, run the service under a fixed DST-free TZ (the container default is UTC). **Covers `last_updated` too** (author decision 2026-08-04: every persisted time is an `Instant`, the UI applies the zone): it is a `LocalDateTime` all the way into `MetaInformationKey.DateTimeKey`, so it never saw a zone, and the JSON API has to reconstruct an instant from a Berlin wall-clock value, which is ambiguous inside the DST fall-back hour | The dedup's correctness must not depend on the host TZ, and a reported timestamp must not be an hour off once a year | Timestamps and `last_updated` round-trip TZ-independently as instants; dedup correct under any host TZ; history preserved 1:1 | M |
| **D15** | **Convert `EntryFactory`/`EntityParser` from static methods to injectable instances** (author review 2026-07-19): both are all-static and now thread `Logger` through every call as a parameter — a smell since the logger arrived (not mockable, fights DI). Make them `@Singleton`-constructed with a constructor-injected `Logger` and inject the parser into `EvergoreDataExtractor`; keep the pure helpers private. Tests already use a `LoggerSpy` handfake, so the seam does not get heavier | Static + a threaded logger defeats DI and individual mockability; sibling to **D11**'s logging hygiene | No static parsing entrypoints; the parser is injected; the logger is a constructor dependency, not a parameter | S |

## Epic E: Product, replace the Google Sheet `P2→P3`  *(in scope, full parity; no time pressure)*

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **E1** | Compute & store **erzeugter Gildenmehrwert** per avatar (bank+storage net) | Sheet col 5, the headline metric | Overview shows net value; matches the verified formula | S |
| **E2** | Surface **last bank/storage activity** per avatar (sheet col 10/11) | Easy parity win from stored timestamps | Overview/avatar view shows last-activity | S |
| **E3** | Implement **geschätzte Jagdeinlagerungen** + percentage(s) (sheet col 6/7/8) | Needs the hunt-loot valuation rule resolved first (D-4) | Values reproduce sheet within tolerance on a sample | M |
| **E4** | **Date-range reporting** (Datum von/bis) instead of only a running watermark | Sheet reports over a chosen window | Query metrics for an arbitrary `[from,to]` | M |
| **E6** | **History / time-series**: snapshot metrics over time for trends per avatar | The sheet is a point-in-time; trends are more useful | Stored snapshots; a trend view | L |
| **E7** | **An avatar with only storage rows is missing from the overview** (falsifier finding 2026-08-04): `/api/v1/avatars` lists `bankRepo.getAllDifferentAvatars()`, so a member who only ever deposited items and never gold does not appear at all. [google-sheet.md](knowledge-base/google-sheet.md) names such a member, so this is real data, not a hypothetical, and the SPA will inherit it. **Widened 2026-08-05** by the entry-resource contract: such an avatar's `/bank` and `/storage` now answer 200 with real paging metadata, so the API affirmatively confirms a member the overview claims does not exist. Before that change both endpoints 404'd, which was at least weakly consistent with being invisible | A missing member is a wrong report, and E1's headline metric spans bank **and** storage | The overview lists the union of both repositories' avatars; the acceptance nets cover a storage-only avatar | S |

## Epic F: Ops, robustness & creative growth `P3`

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **F1** | **Remove the bundled webdriver binaries and the local-browser machinery** (author decision 2026-07-03): 87 MB of Jan-2023 gecko/chrome/edge binaries in `src/main/resources` ship in every jar and image and are never used (default browser is `docker`; the 2023 builds cannot drive current browsers anyway). The `CHROME`/`EDGE`/`FIREFOX` local paths and the loader/driver machinery go with them; **H2**'s remote driver becomes the only path. Afterwards the author rewrites history himself (`git filter-repo`; the pack drops from 66.7 MiB; breaks clones and hashes, accepted). Scrape resilience (retries, selector config, fail loudly) stays in scope | 87 dead MB in every artifact and permanently in the pack; obsolete driver tech | No binaries under `src/main/resources`; local-browser code gone; scraping works via H2's service; durable-doc hash references replaced by symbol references, then history rewritten by the author | M |
| **F3** | **Creative:** weekly guild report (Discord webhook / email), CSV/Sheets export, inactivity alerts | Push value to the guild, not just a page | One delivery channel shipped behind config | M |
| **F4** | **Creative:** multi-guild / multi-world support | Generalize beyond `[Boten]` on `zyrthania` | Config-driven guild/world; data partitioned | L |
| **F5** | **Creative:** public read-only OpenAPI JSON API for guild tooling | Already have OpenAPI; expose clean JSON | Documented `/api` returning per-avatar metrics | M |

## Epic G: Documentation, standards & Claude-workflow showcase `P0→P1`

The project doubles as an example of clean, AI-assisted development, so the "how" is a deliverable.

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **G3** | Keep KB in lockstep with code (baked into Definition of Done) | Docs rot otherwise | Behavior-changing commits touch the matching KB doc | S (ongoing) |
| **G4** | Decide BDD tooling: plain JUnit given/when/then vs Cucumber `.feature` (open question D-9) | Shapes how scenarios are written & shared | Decision logged; first scenario follows it | S |
| **G5** | Optional: short "case study" of the rebuild for the showcase | The meta-goal is demonstrating AI-assisted dev | A narrative others can learn from | M |
| **G6** | *(future)* Static code-analysis / metrics gate: complexity, duplication, coverage thresholds (SonarQube/Sonar). *(The old "re-add the `sonarlint` feature" strand is closed as a misdiagnosis, 2026-08-01: that third-party feature's `install.sh` only `echo`s — its sole effect is two VS Code extensions plus a `dependsOn` on `node`, and the GPG breakage came from the `node` feature's yarn apt repo, which node 2.x no longer uses. The `SonarSource.sonarlint-vscode` extension is now declared directly; a real gate still needs a Sonar **analysis** step, which is what this row is about.)* | Objective quality guardrail beyond agent review | CI fails on threshold breach | M |
| **G17** | **Forbid wildcard imports project-wide** (author decision 2026-06-30): the de-facto style is `import …*;` (all prod + legacy tests), but nothing in the build enforces either way and the newer tests use explicit imports → inconsistency. Add a **single-purpose** Checkstyle rule (`AvoidStarImport`, sibling to the existing `NeedBraces` gate, *not* a general linter, so the rejected-items note still holds), then **one-time-expand** every existing wildcard import to explicit (IDE organize-imports; Spotless can't expand `.*` since it needs type info) | Clean-code showcase + consistency: explicit imports make dependencies visible and match the new tests; "enforce style from one place" is already the pattern | Build fails on any `import …*;`; no wildcard import remains under `src/`. **Own change, *after* the in-flight strands land**: a project-wide import reformat would conflict with every open worktree | M |
| **G18** | **KB accuracy sweep** (setup review 2026-07-03; KB-first is only as safe as the KB is accurate): remove the phantom `DatabaseStartupInitialization` (architecture.md); domain-model.md (`stupidMerge()` no longer exists, the evaluator's package, component `date` vs `timeStamp`, "~600 items" vs the real 446); handbook §1 (the PageSource port exists and is wired) and §3 (quotes deleted code as present examples); build-run-deploy.md (`.github/modernize/` does not exist, duplicated version literals against the single-source rule, the unconfigured "Dependabot alerting" claim); `@Ignore`→`@Disabled` (the project is JUnit 5) in handbook/decisions; add the missing 2026-06-27 branching-revision decisions row and update the stale 2026-06-20 entry; align the extension lists (dev-environment.md vs devcontainer.json vs .vscode); make the playbook's falsifier wording precise (the panel is read-only and writes scratch counter-tests via Bash, decision 2026-07-03); rename `git-state.md`→`git-conventions.md` and fix links (author decision 2026-07-03) | Several central KB claims are provably false; agents are told to trust them | Every named drift fixed; no swept KB claim contradicts the code | M |
| **G19** | **KB citation guard** (author decision 2026-07-03): a small test or hook that extracts class-name references from `docs/knowledge-base/` and checks each resolves under `src/` | Automates the biggest drift class the review found (a phantom class survived in two docs) | The guard fails on a KB reference to a class that does not exist | S |

## Epic H: Dev environment & virtualization `P0→P2`

Goal: **fully virtualized dev, nothing (JDK/Maven/Firefox) installed or run on the host.** All work
(incl. agents) runs in the devcontainer or via Docker; JDK upgrades = change an image tag, not the host.
See [knowledge-base/dev-environment.md](knowledge-base/dev-environment.md).

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **H2** | Add a `selenium/standalone-firefox` service (compose or plain `docker run`, the `docker-outside-of-docker` feature is back) and switch `Browser.DOCKER` from its **local** `FirefoxDriver` to a `RemoteWebDriver` with a configurable hub URL; retire bundled `gecko-*-win.exe`. *(Interim since 2026-08-01: `firefox-esr` in the devcontainer image makes the scrape path locally runnable with no code change — H2 replaces it and drops it from the image again.)* | Dev browser matches the production runtime; scraping/integration tests run anywhere | An integration test scrapes via the service | M |
| **H9** | **Jump Micronaut 4.10 → 5.0** (Java-25 baseline, Apr 2026) and re-verify 1:1. **Only after** the current migration's 1:1 is locked (ideally via the automated acceptance test `ProtocolEvaluationAcceptanceTest`) | Stay on the newest line; deferred deliberately, one framework risk at a time | `./gradlew build` green on MN5 + endpoints still 1:1 vs the prod snapshot | M |
| **H11** | **Extend Dependabot to `docker` + `gradle` and run one dependency-refresh pass** (author decision 2026-07-03): `docker` now covers `/.devcontainer` only, `gradle` not at all; the `selenium/standalone-firefox:109.0` pin (~3.5 years old, browser + OS-base CVEs) and the Gradle deps (Selenium 4.7.2 of 2022, sqlite-jdbc 2023, unirest EOL) age silently. Refresh through the acceptance-test net, which exists exactly for this | The OWASP scan finds CVEs, not staleness | dependabot.yml covers all three ecosystems; deps and image current; build + 1:1 green | M |

---

## Derived from the enterprise-setup review (2026-06-15)

A workflow gap-analysis compared an external *"enterprise Java shop"* Claude Code guide against this
repo and **adversarially verified** every derived item (genuinely missing? worth it for a *solo*
Micronaut showcase?). The guide is Spring/Gradle/team-scale; only the items below survived. The source
doc is intentionally **not** committed; its value lives here.

### New items (verified worth doing)

| ID | Item | Why | Priority | Sequencing / caveat |
|----|------|-----|----------|---------------------|
| **G7** | **Deterministic enforcement hooks** in `.claude/settings.json`: (a) PreToolUse Edit/Write **secret-scan**; (b) Pre/PostToolUse reject of `System.out`/`printStackTrace`/leftover `// TODO`; (c) **PreToolUse guard against working-directory drift** (author request 2026-08-05): reject a Bash call that writes through a repo-relative path, or runs `git`/`./gradlew` without an explicit `-C`/absolute target, while more than one worktree is checked out. The failure mode is silent, so a rule cannot catch it and a hook can | Demonstrates the guide's core thesis (CLAUDE.md ~80% vs hooks 100%), the showcase's headline technique | P2 | secret-scan: tune pattern (must catch `?token=…`); the hard-coded API token is already env-injected, so no cleanup-ordering constraint remains. System.out check: the dead-code deletion already removed ~half the hits (`CsvParser`); whitelist `@Ignore` Gherkin once G4 lands. |
| **G8** | **`/commit`** slash command encoding the strict one-line/no-footer/never-push protocol | Repo's strictest, most-violated-by-default rule (footers slip in); reproducible showcase artifact | P3 | `/review`,`/tdd` rejected (duplicate reviewer/implementer agents). `/spec` deferred → gate on **G4**; keep MCP-free + JUnit-`@Ignore`-first (not Cucumber/Jira). |
| **B7** | Migrate the remaining JUnit `Assertions` in `SmokeTest` → **AssertJ** | Single assertion idiom (documented preference) | P3 | The Levenshtein half is done: `SmokeTest` reads its seeded rows back through the JSON API, so `assertClosest` and `TestHelper` are gone. Only the assertion idiom is left. |
| **H6** | `maven-failsafe-plugin` + rename boot/integration tests to `*IT` (separate integration phase) | Keeps the fast TDD loop fast; isolates server-booting tests | P3 | Gate on **H2** (real Selenium IT), **not** the in-memory *fast* acceptance test (`ProtocolEvaluationAcceptanceTest`). Update testing.md same change. **2026-06-15: land in Gradle post-migration (failsafe → Gradle integration test set).** |
| **G9** | **Point the agent at official docs (WebFetch) for less-trafficked libraries**, a `working-with-ai-agents.md` convention: for **Micronaut / ORMLite / Selenium / RxJava** (thin in LLM training data), fetch the official docs before writing against an unfamiliar API; prefer doc-grounded code over confabulation | Enterprise-audit Pitfall #5, the most stack-relevant gap: a solo dev has no reviewer to catch a hallucinated API, and ArchUnit/tests catch structure, not invented method signatures. MCP-free (WebFetch is available) | P2 | Flagged independently by two audit reviewers. Doc-only; fits the Epic-G showcase. |
| **G10** | **SessionStart orient/lessons hook** in `.claude/settings.json`: deterministically inject the orient pointer (CLAUDE.md → KB README → backlog "Current status" → open-questions → `process-learnings.md`) so every fresh session reads the lessons first | The 100%-fires complement to the advisory `/continue` + CLAUDE.md "Start here"; completes the self-improvement loop and is the most on-thesis hooks-over-rules showcase artifact (sibling to **G7**) | P3 | Enterprise-audit gap. The lessons file already exists (`process-learnings.md`); only the deterministic hook is missing. |
| **G11** | **Improve the AI/agent working environment in the devcontainer** (low prio): pre-allow common read-only commands in the committed `settings.json` (portable forms only), and add conventions/hooks that cut token use (scratch-file hygiene, scoped reads over blind re-scans) | Recurring friction: repeated permission prompts, the "always allow" flow re-polluting `settings.json`, broad re-reads burning tokens | P3 | Author request 2026-06-16. Sits with **G7**/**G10** (the hooks/showcase items). The tool pre-installs are done (2026-08-01: `bc`, `git-filter-repo`, `sqlite3`, `firefox-esr` in the image; `gh` and `docker` as features); open are the `settings.json` pre-allows and the token-cutting hooks. The falsifier probe location (gitignored `src/probe/java` + `./gradlew probe`) is in place, see [build-run-deploy.md](knowledge-base/build-run-deploy.md). |
| **G13** | **Git enforcement hooks** (committed `hooks/` + `core.hooksPath` wired in the devcontainer `postCreate`, in-container): **pre-commit** = `./gradlew spotlessCheck checkstyleMain checkstyleTest` + a **secret-scan** (tokens/keys/credentials/real emails/host-paths) + reject `System.out`/`printStackTrace`/leftover `// TODO`; **commit-msg** = one-line, present-tense-verb-first, no body/`Co-Authored-By`/footer. **Excludes tests/`build`** (would break the fast TDD micro-commit loop; commits are already green). | Mechanically prevents slips rules can't: an earlier feature's re-implementation commits were Spotless-dirty because `spotlessApply` output was left uncommitted; "hooks > rules" at the git layer | P2 | Author request 2026-06-22 (all 4 checks chosen). **Git-level complement** to harness-level **G7** (PreToolUse, catches Claude edits earlier) + **G8** (`/commit`); the lighter/faster form of **A4**'s local gate (no full `verify`). Implement **after the preceding feature**. **Setup-review follow-ups 2026-07-03:** document `git config core.hooksPath hooks` as the host/fresh-clone setup step (hooks activate only via the devcontainer postCreate today, so a host session commits ungated; author decision: documented one-liner, no bootstrap script), and extend the pre-commit secret-scan to build files (`*.kts`; token patterns are checked only under `src/main`). **Host-commit finding 2026-07-17:** the gradle leg cannot run in a Windows host session (`checkstyleMain` pulls `:frontend:npmBuild`; `frontend/node_modules` is Linux-installed, no `tsc` on Windows), so commits happen in-container until the gate gets a host-viable or docs-only path. |

### Considered and rejected (do not re-propose without a new reason)

- **Checkstyle *as a general linter*** duplicates **G6** + the reviewer agent; fold into G6 if anything.
  *(Introduced 2026-06-19 in a single-purpose, disjoint scope: one rule, `NeedBraces`, for the brace gap the
  formatter cannot fill, see build-run-deploy.md. The "general linter" rejection still stands.)*
- **maven-enforcer**: fabricated origin, BOM-managed deps, in-container fixed JDK → low value.
- **commit-message *skill***: rule already in CLAUDE.md + reviewer gate + the `git push` deny; a skill adds context cost, not enforcement (use **G8** `/commit`, or a git `commit-msg` hook).
- **security-auditor *subagent***: redundant with the reviewer's `security` category; OWASP ceremony for a no-prod-pressure scraper. Keep only "extend reviewer + secret-scan hook" under C3.
- **ADRs (`docs/adr/`)** duplicate the `open-questions.md` Decisions table ("git is history; docs are knowledge").
- **format-on-save hook**: premature; folded into the shared-formatter adoption (needs a formatter + CI first).
- **block-dangerous-bash hook**: now largely covered by the hardened permission **deny** (`git push`, `rm -rf`, `git reset --hard`, added 2026-06-15; `-C` worktree variants + `git -C * clean` added 2026-06-28); revisit only for force-push/rebase nuance.
- **N/A / enterprise-only:** Spring Modulith (not Spring), CLAUDE.md <200-line guard (it's 76), `.mcp.json`/Jira/GitHub MCP (solo, no tracker), `output-styles/`, path-scoped `rules/`, directory-level `CLAUDE.md`, a `docs-writer`/`tdd-runner` agent (tdd = existing `implementer`), quarterly surface audit, English-in-repo (already decided).

---

## Dependency notes

- The acceptance test (`ProtocolEvaluationAcceptanceTest`) enables most of E (date-range, dashboard build on a testable core).
- C-epic is independent and can run in parallel; do C3 before any real deployment.
- **D-4** (hunt-loot valuation rule) gates E3; the storage-multiplier question is **resolved** (`Category.storage` removed; see the open-questions Decisions entry on that removal).
