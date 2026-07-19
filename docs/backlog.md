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
remaining findings as **D9** / **G16**.

**Next action:** **E5**'s remaining strands **`json-api`** → **`spa-views`** (decision 2026-07-04),
then **B16**/**B17**, **E1**/**E2**, **H2**+**H6**; **H9** only *after* 1:1 is re-proven. Milestone
cuts + acceptance: [roadmap.md](roadmap.md); risk register: [risks.md](risks.md). Plan via the agent
pipeline (planner → implementer → falsifier panel → reviewer). *(A4/CI stays deprioritized:
local-only Docker → home-server deploy.)*

**Deferred frontend follow-ups (non-blocking, mostly for `json-api`):**
- **For `json-api`** (which reworks the filter/fallback anyway): (a) `SpaHistoryFallbackController.isSpaNavigationRequest`
  uses `!path.contains(".")`, wrong both ways (a dotless missing asset → 200 SPA shell instead of 404; a dotted
  client route → 404 instead of the shell): tighten to a trailing-extension match with tests both ways; (b)
  `SpaStaticResourcePaths.matches` tests a **non-canonicalized** path, so `/assets/../overview` skips the token
  filter (no proven leak, Micronaut's static resolver 404s it): fold the fix into the `/api/**` inversion; (c)
  document/accept that `/`, `/index.html`, `/assets/**` are served with **no token, rate-limit or audit log**.
- **Test hygiene:** harden the frontend `vitest` worker pool/timeout (a spurious "failed to start forks worker"
  flake under CPU load, seen twice incl. once on the landing build); and move `smokeTest.sqlite` out of
  `src/test/resources`: it is rewritten every run, perpetually busting `processTestResources → test → jacoco`
  caching for the whole backend chain and risking cross-run flakiness (relates to **B8** / **B4**).
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
- The **devcontainer/`Dockerfile` are not built inside the devcontainer** (no docker-in-docker, H2);
  validate them on the Docker host / next container rebuild.
- The Claude "always allow" flow can re-pollute the committed `settings.json` (path-bearing rules +
  tabs→spaces). Prefer bare commands matching the portable `Bash(<cmd>:*)` rules; diff against HEAD if unsure.
  The worktree `git -C <abs-path>` re-prompt loop is fixed by the committed portable wildcard
  `Bash(git -C *.claude/worktrees/*)` (2026-06-28); still watch the tabs→spaces rewrite.
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
| **B8** | **Audit & align the existing test suite to the test-style conventions** (emerged 2026-06-29 reworking the parser contract tests): no logic buried in tests (no case-`record` + loop + soft-assert iteration); one named `@Test` per distinct behaviour with the expected literal in the assert; "one assert per test" = one assertion *case* (assert a composite value as a whole via an intention-revealing helper like `assertEntry`); minimal, noise-free input values; data-driven (`@ParameterizedTest`/`@MethodSource`) only where it genuinely fits (ask when unsure). **Codify the rules in the KB first** (handbook §6 / `testing.md`), then bring older tests (e.g. `SmokeTest`) into line. **Setup-review additions 2026-07-03:** delete the 12 record-equality test methods (they test the compiler, not the code; author decision) and rework the acceptance test's two index-coupled `anyMatch` assertions to the `containsExactly` style | The conventions are newer than most of the suite; the showcase's tests should be exemplary and consistent | Rules documented in the KB; existing tests conform | M |
| **B16** | **`SeleniumPageSource` test hardening** (verification findings 2026-07-08): the unit test drives the real `WebDriverWait(driver, ofMinutes(1))` with the system sleeper/clock: it resolves on the first evaluation today, but a future URL/redirect mismatch would hang ~1 min per `wait()` on every machine before failing, against the no-timeouts rule (handbook §6); and the both-fail path (scrape throws *and* `quit()` throws → the scrape exception must propagate, both failures logged) is correct but not locked in by a test | Test-suite determinism and regression safety in the most fragile adapter | The wait has no wall-clock dependency in tests; a both-fail test pins the "scrape exception propagates, both logged" contract | S |
| **B17** | **The parser still aborts the ingest on a non-date malformed line** (verification finding 2026-07-08, sibling to the malformed-date fix that just landed): that fix hardened the *date* path, but `EntryFactory.parseItems` stays unguarded. An item line whose amount group is empty (a leading-whitespace line that is not recognized as a headline boundary and falls through to `parseItems`) makes `Integer.parseInt("")` throw an uncaught `NumberFormatException`, aborting the whole ingest. A related low-realism residual: a malformed headline with single-digit fields (`1.1.2000 0:00 …`) is not caught by `LAGER_EINTRAG_BOUNDARY` (`\d{2}` needs two digits), so it is absorbed into the preceding entry and its items leak (Evergore zero-pads, so real corruption keeps 2-digit fields). Fix: guard `parseItems` amount parsing (skip/log a non-numeric amount) so no single line can abort the ingest. **Panel findings 2026-07-19, same guard:** (a) an entry whose item lines all fail the item regex silently becomes a zero-item no-op (e.g. a thousands-separator amount `1.000 Gold` matches nothing) — warn when a recognized entry yields no parseable items; (b) a headline matching the loose `LAGER_EINTRAG_BOUNDARY` (wildcard dots) but failing the strict `TIMESTAMPED_HEADLINE` (e.g. `11.12X2001 13:37 …`) is dropped with no warning — warn unconditionally when a block head fails the strict match; (c) an unknown-type headline whose avatar contains a type word as substring (`… Entnahmefreund Auszahlung`) bypasses the warn and mints a wrong entry (avatar `""`, type from the name) — the greedy avatar regex must not backtrack into type words. **CAUTION for the implementer:** any parser change that alters the parse output of already-stored, still-visible entries makes them unequal to their stored twins, so the ingest window dedup re-ingests them as duplicates — such a change needs a one-off data reconciliation | The "one bad line must not kill the ingest" invariant is only half-met after the malformed-date fix, and the loud-warn guarantee has proven regex bypasses | No single malformed protocol line aborts the ingest, mis-attributes items or vanishes silently; covered by tests | S |

## Epic C: Configuration & security `P1`  *(good-practice showcase; not urgent while learning/fun)*

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **C1** | Make `Configuration` real via `@ConfigurationProperties` bound from `application.yml`/env (browser, server, db path, credentials path, in-memory toggle, **+ the hard-coded Firefox binary path in `Browser.java`**). The conformance audit also flagged the **public mutable fields** `useInMemory`/`DATABASE_TEMP_SQLITE` and the Windows `c:\evergore` default path: fold these in (immutable record/port, no host paths) | Hard-coded fields defeat config & deployability; mutable public config breaks "prefer immutable records" | No domain settings hard-coded in `.java`; overridable by env; config is immutable | M |
| **C3** | Stop baking `zugang.txt` into the image; inject credentials via env/secret/mount; read via `FileLoader` port | Credentials in the image is a leak | Image has no credentials; documented secret-injection path | M |
| **C4** | Lower `logback` root from `verbose`; ensure credentials/tokens never logged | Chatty logs may leak secrets | Sensible levels; a log-scrub check | S |
| **C7** | **Rate-limiter cleanup** (setup review 2026-07-03): `BrowserLoggingFilter` is the rate limiter in disguise; rename it to say what it does, and bound/evict the per-IP counter map (it grows without limit). No `X-Forwarded-For` handling needed: the service is directly reachable, no reverse proxy (author decision 2026-07-03) | A security component with a misleading name and unbounded memory growth | Intention-revealing name; the map is bounded/evicted | S |
| **C8** | **Drive `./gradlew vulnScan` findings to zero, then gate on any finding** (author request 2026-07-04): bump the direct dependencies (e.g. selenium 4.7.2, sqlite-jdbc, ormlite, commons-text are dated) and pin/constrain transitives until the Trivy report is clean; **no Micronaut major jump** (that stays deferred, H9): respect Micronaut-managed versions. CVEs without a sane fix go into a reviewed **`.trivyignore`** (CVE + justification + expiry) so exceptions stay temporary. Finish by setting `vulnScan.failOnSeverity=UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL` in `gradle.properties` and updating build-run-deploy.md to the gating default | The scan exists but tolerates all findings; zero-vuln + explicit whitelist makes drift visible | `vulnScan` is green with the gate on; every ignored CVE has a justification and expiry | M |

## Epic D: Hexagonal completion `P1→P2`

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **D3** | Restructure packages to `domain / application / adapters{in,out} / config`; keep core framework-free; fold in the package-naming cleanup (lowercase the camelCase packages; root package `protocolParser` vs the Collector repo name) | Make the boundaries explicit & enforceable | Micronaut/Selenium/ORMLite imports only under `adapters`+`config` | L |
| **D9** | **Naming/consistency follow-ups** (conformance-audit findings): (a) fix the pervasive `withdrawl`→`withdrawal` misspelling in identifiers **and** DB key strings (`bank_withdrawl_`→`bank_withdrawal_`, `storage_withdrawl_`→`storage_withdrawal_`, needs a one-time key migration); (b) rename the German identifiers `PageContents.lager`/`updateLagerEntries`/`Constants.LAGER_EINTRAG_START` → `storage`/`STORAGE_ENTRY_PATTERN`; (c) move `Logger` to the last constructor param in the `Repository` base + its three subclasses; (d) rename `TransferTypeDatabaseVisitor`, it is a converter, not a visitor (setup review 2026-07-03) | English-only + intention-revealing-name rules; consistency with `StorageEntry`/`StorageRepository` | Identifiers corrected; DB migration keeps values 1:1 | S |
| **D6** | **Refactor the `EvergoreItem` catalog in-place**: the ~660-constant enum (752 lines) is the project's biggest file. **Decision 2026-06-19: keep it an enum** (the compile-checked recipe hard-links are valued) and **do NOT externalize** to a config/YAML file. Refactor for readability/maintainability instead, e.g.: pull the nested `Category`/`Recipe`/`Ingredient` types into their own files; add concise recipe/ingredient factory helpers to shorten each constant; group constants. Approach TBD with the author. Any change **must** keep a **golden-master test** asserting `getStorageValue`/`getWithdrawlValue` are unchanged for all items | Tame the biggest file without losing the type-safe cross-references; eases value maintenance (relates to **D-3**) | P3 | Golden-master test gates it (1:1). |
| **D10** | **Introduce a schema-migration framework** (author decision 2026-07-03): evaluate Flyway vs Liquibase for SQLite/ORMLite fit; versioned, repeatable migrations instead of ad-hoc `ensureTable()` DDL. Gates **D9**(a)'s key migration. Historical data **must** survive every migration: Evergore serves only the last 30 days of logs, so the DB is the only history | Epic E grows the schema and D9 renames keys; both need a verifiable path that cannot lose the irreplaceable history | Framework chosen and wired; the D9 key rename runs as its first migration, verified 1:1 via golden master | M |
| **D11** | **Exception & logging hygiene, dead code** (setup review 2026-07-03): `ExceptionWrapper` catches `Throwable` and rethrows bare `RuntimeException`; `EvergoreDataCollectorJob` swallows `InterruptedException` without restoring the interrupt flag; adapters throw bare `RuntimeException` past the curated `exceptions/` hierarchy (`Browser`, `DiscFileLoader`, `ResourceFileLoader`, `TransferTypeDatabaseVisitor`); `Slf4jLogger` does a full stack-walk with a hard-coded index on every log call; dead code (`Entry.print()`, the `toBeParsed.txt` placeholder baked into the jar, the unreachable `onUnknown` in the exception visitor) | Swallowed interrupts and bare exceptions defeat the curated error model; dead code violates the working agreements | Interrupt flag restored; adapters use `exceptions/`; dead code gone | M |
| **D12** | **Unify the ~95 % duplicated Bank/Storage repositories** (setup review 2026-07-03): the copy has already bitten, `BankDatabaseRepository` imports `StorageDatabaseEntry.TIMESTAMP_COLUMN` into bank queries (works only because both constants happen to be `"timeStamp"`); three `JdbcConnectionSource`s on the same SQLite file are never closed. Extract the shared base, fix the cross-import, use one `@Singleton` connection source with `busy_timeout` | A latent wrong-query bug plus a resource/locking hazard | No cross-entity constant use; one managed connection source; the duplication collapsed | M |
| **D13** | **Extend `HexagonalArchitectureTest` to inward-only for the core** (author decision 2026-07-03): today only `application` has an inward-only rule; `domain`/`businessLogic` are guarded by a framework denylist and may import the project's own adapter packages unpunished. Add inward-only for the core (the denylist stays as a second line); optionally add a cycle check | The core's purity is doctrine but only half-enforced by the gate | An adapter-package import in `domain`/`businessLogic` fails the build | S |
| **D14** | **Store timestamps in an epoch/UTC format instead of default-TZ wall-clock strings** (author decision 2026-07-19): the `DATE_STRING` columns persist wall-clock text in the JVM default TZ (pinned nowhere); in a DST-zone fall-back hour round-trip equality and the lexicographic max break (proven in jshell under `Europe/Berlin`), which would corrupt the ingest window dedup that relies on value equality. Needs a data migration → gate on **D10**. Until it lands, run the service under a fixed DST-free TZ (the container default is UTC) | The dedup's correctness must not depend on the host TZ | Timestamps round-trip TZ-independently; dedup correct under any host TZ; history preserved 1:1 | M |

## Epic E: Product, replace the Google Sheet `P2→P3`  *(in scope, full parity; no time pressure)*

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **E1** | Compute & store **erzeugter Gildenmehrwert** per avatar (bank+storage net) | Sheet col 5, the headline metric | Overview shows net value; matches the verified formula | S |
| **E2** | Surface **last bank/storage activity** per avatar (sheet col 10/11) | Easy parity win from stored timestamps | Overview/avatar view shows last-activity | S |
| **E3** | Implement **geschätzte Jagdeinlagerungen** + percentage(s) (sheet col 6/7/8) | Needs the hunt-loot valuation rule resolved first (D-4) | Values reproduce sheet within tolerance on a sample | M |
| **E4** | **Date-range reporting** (Datum von/bis) instead of only a running watermark | Sheet reports over a chosen window | Query metrics for an arbitrary `[from,to]` | M |
| **E5** | Real **overview dashboard**: JSON API + React SPA (decision 2026-07-04, resolves D-6), replacing the current HTML-string templates; sortable table, all columns, maybe charts | The sheet's value is the at-a-glance view | A guild officer can replace the sheet with this page | L |
| **E6** | **History / time-series**: snapshot metrics over time for trends per avatar | The sheet is a point-in-time; trends are more useful | Stored snapshots; a trend view | L |

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
| **G16** | **Refresh `testing.md`** (conformance-audit findings): add the missing test classes to the inventory (`BootSignalRecorderTest`, `ApiTokenStartupValidatorTest`, `RateLimitFilterTest`) and correct the stale `@Test` counts. *(The fork-isolation half is resolved: `build.gradle.kts` now sets `forkEvery = 1`, matching testing.md's documented per-class-isolation rationale, the `SmokeTest` startup race; its build-time cost belongs in the build-performance plan.)* | Docs-are-knowledge: the inventory drifted | Inventory matches the suite | S |
| **G6** | *(future)* Static code-analysis / metrics gate: complexity, duplication, coverage thresholds (SonarQube/Sonar). **Re-add the `sonarlint` devcontainer feature** (removed because its `node`→yarn-repo dependency broke apt with a GPG error; troubleshoot the key) | Objective quality guardrail beyond agent review | CI fails on threshold breach | M |
| **G17** | **Forbid wildcard imports project-wide** (author decision 2026-06-30): the de-facto style is `import …*;` (all prod + legacy tests), but nothing in the build enforces either way and the newer tests use explicit imports → inconsistency. Add a **single-purpose** Checkstyle rule (`AvoidStarImport`, sibling to the existing `NeedBraces` gate, *not* a general linter, so the rejected-items note still holds), then **one-time-expand** every existing wildcard import to explicit (IDE organize-imports; Spotless can't expand `.*` since it needs type info) | Clean-code showcase + consistency: explicit imports make dependencies visible and match the new tests; "enforce style from one place" is already the pattern | Build fails on any `import …*;`; no wildcard import remains under `src/`. **Own change, *after* the in-flight strands land**: a project-wide import reformat would conflict with every open worktree | M |
| **G18** | **KB accuracy sweep** (setup review 2026-07-03; KB-first is only as safe as the KB is accurate): remove the phantom `DatabaseStartupInitialization` (architecture.md, testing.md); domain-model.md (`stupidMerge()` no longer exists, the evaluator's package, component `date` vs `timeStamp`, "~600 items" vs the real 446); handbook §1 (the PageSource port exists and is wired) and §3 (quotes deleted code as present examples); build-run-deploy.md (`.github/modernize/` does not exist, duplicated version literals against the single-source rule, the unconfigured "Dependabot alerting" claim); `@Ignore`→`@Disabled` (the project is JUnit 5) in handbook/decisions; add the missing 2026-06-27 branching-revision decisions row and update the stale 2026-06-20 entry; align the extension lists (dev-environment.md vs devcontainer.json vs .vscode); make the playbook's falsifier wording precise (the panel is read-only and writes scratch counter-tests via Bash, decision 2026-07-03); rename `git-state.md`→`git-conventions.md` and fix links (author decision 2026-07-03). testing.md itself is **G16** | Several central KB claims are provably false; agents are told to trust them | Every named drift fixed; no swept KB claim contradicts the code | M |
| **G19** | **KB citation guard** (author decision 2026-07-03): a small test or hook that extracts class-name references from `docs/knowledge-base/` and checks each resolves under `src/` | Automates the biggest drift class the review found (a phantom class survived in two docs) | The guard fails on a KB reference to a class that does not exist | S |

## Epic H: Dev environment & virtualization `P0→P2`

Goal: **fully virtualized dev, nothing (JDK/Maven/Firefox) installed or run on the host.** All work
(incl. agents) runs in the devcontainer or via Docker; JDK upgrades = change an image tag, not the host.
See [knowledge-base/dev-environment.md](knowledge-base/dev-environment.md).

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **H2** | docker-compose: add a `selenium/standalone-firefox` service; tests use `RemoteWebDriver`; retire bundled `gecko-*-win.exe`. **Re-add the `docker-outside-of-docker` devcontainer feature** (removed to get a building container) | Browser-in-container ⇒ scraping/integration tests run anywhere, no host Firefox | An integration test scrapes via the service | M |
| **H5** | *(was: cross-rebuild Maven cache, obsolete, Maven gone)* Optional: persist the Gradle caches (`~/.gradle`, build cache) across rebuilds with correct `vscode` ownership | Faster rebuilds | Gradle deps/build cached across container rebuilds | S |
| **H9** | **Jump Micronaut 4.10 → 5.0** (Java-25 baseline, Apr 2026) and re-verify 1:1. **Only after** the current migration's 1:1 is locked (ideally via the automated acceptance test `ProtocolEvaluationAcceptanceTest`) | Stay on the newest line; deferred deliberately, one framework risk at a time | `./gradlew build` green on MN5 + endpoints still 1:1 vs the prod snapshot | M |
| **H10** | **Fix the broken nested `features` object in devcontainer.json** (python:1 is silently never installed; author decision 2026-07-03: python stays in): un-nest the feature, regenerate the lock file | The container silently lacks a wanted feature and the next rebuild will surprise | A rebuild installs python; the lock file matches the config | S |
| **H11** | **Extend Dependabot to `docker` + `gradle` and run one dependency-refresh pass** (author decision 2026-07-03): only `devcontainers` is covered today; the `selenium/standalone-firefox:109.0` pin (~3.5 years old, browser + OS-base CVEs) and the Gradle deps (Selenium 4.7.2 of 2022, sqlite-jdbc 2023, unirest EOL) age silently. Refresh through the acceptance-test net, which exists exactly for this | The OWASP scan finds CVEs, not staleness | dependabot.yml covers all three ecosystems; deps and image current; build + 1:1 green | M |
| **H12** | **Small build/supply-chain hardening** (setup review 2026-07-03): add `distributionSha256Sum` to the Gradle wrapper config; make Spotless cover `*.kts` (build scripts currently pass every gate unformatted); chain the devcontainer postCreate with `&&` instead of `;`; pin the floating base-image/node tags (the lock pins only features) | Cheap integrity and consistency wins | Wrapper checksum pinned; `spotlessCheck` fails on unformatted `.kts`; postCreate aborts on first failure | S |

---

## Derived from the enterprise-setup review (2026-06-15)

A workflow gap-analysis compared an external *"enterprise Java shop"* Claude Code guide against this
repo and **adversarially verified** every derived item (genuinely missing? worth it for a *solo*
Micronaut showcase?). The guide is Spring/Gradle/team-scale; only the items below survived. The source
doc is intentionally **not** committed; its value lives here.

### New items (verified worth doing)

| ID | Item | Why | Priority | Sequencing / caveat |
|----|------|-----|----------|---------------------|
| **G7** | **Deterministic enforcement hooks** in `.claude/settings.json`: (a) PreToolUse Edit/Write **secret-scan**; (b) Pre/PostToolUse reject of `System.out`/`printStackTrace`/leftover `// TODO` | Demonstrates the guide's core thesis (CLAUDE.md ~80% vs hooks 100%), the showcase's headline technique | P2 | secret-scan: tune pattern (must catch `?token=…`); the hard-coded API token is already env-injected, so no cleanup-ordering constraint remains. System.out check: the dead-code deletion already removed ~half the hits (`CsvParser`); whitelist `@Ignore` Gherkin once G4 lands. |
| **G8** | **`/commit`** slash command encoding the strict one-line/no-footer/never-push protocol | Repo's strictest, most-violated-by-default rule (footers slip in); reproducible showcase artifact | P3 | `/review`,`/tdd` rejected (duplicate reviewer/implementer agents). `/spec` deferred → gate on **G4**; keep MCP-free + JUnit-`@Ignore`-first (not Cucumber/Jira). |
| **B7** | Migrate the remaining JUnit `Assertions` in `SmokeTest` → **AssertJ** | Single assertion idiom (documented preference) | P3 | `SmokeTest` stays deferred to its planned `RenderedTable`/Levenshtein-rework (its `assertClosest` helper should go too). |
| **H6** | `maven-failsafe-plugin` + rename boot/integration tests to `*IT` (separate integration phase) | Keeps the fast TDD loop fast; isolates server-booting tests | P3 | Gate on **H2** (real Selenium IT), **not** the in-memory *fast* acceptance test (`ProtocolEvaluationAcceptanceTest`). Update testing.md same change. **2026-06-15: land in Gradle post-migration (failsafe → Gradle integration test set).** |
| **G9** | **Point the agent at official docs (WebFetch) for less-trafficked libraries**, a `working-with-ai-agents.md` convention: for **Micronaut / ORMLite / Selenium / RxJava** (thin in LLM training data), fetch the official docs before writing against an unfamiliar API; prefer doc-grounded code over confabulation | Enterprise-audit Pitfall #5, the most stack-relevant gap: a solo dev has no reviewer to catch a hallucinated API, and ArchUnit/tests catch structure, not invented method signatures. MCP-free (WebFetch is available) | P2 | Flagged independently by two audit reviewers. Doc-only; fits the Epic-G showcase. |
| **G10** | **SessionStart orient/lessons hook** in `.claude/settings.json`: deterministically inject the orient pointer (CLAUDE.md → KB README → backlog "Current status" → open-questions → `process-learnings.md`) so every fresh session reads the lessons first | The 100%-fires complement to the advisory `/continue` + CLAUDE.md "Start here"; completes the self-improvement loop and is the most on-thesis hooks-over-rules showcase artifact (sibling to **G7**) | P3 | Enterprise-audit gap. The lessons file already exists (`process-learnings.md`); only the deterministic hook is missing. |
| **G11** | **Improve the AI/agent working environment in the devcontainer** (low prio): pre-install tools used every session (`git-filter-repo`, `sqlite3`, `jq`), pre-allow common read-only commands in the committed `settings.json` (portable forms only), and add conventions/hooks that cut token use (scratch-file hygiene, scoped reads over blind re-scans) | Recurring friction: missing `git-filter-repo`/`docker`, repeated permission prompts, the "always allow" flow re-polluting `settings.json`, broad re-reads burning tokens | P3 | Author request 2026-06-16. Sits with **G7**/**G10** (the hooks/showcase items). Open: the tool pre-installs (`git-filter-repo`, `sqlite3`, `jq`) and the token-cutting hooks. |
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
