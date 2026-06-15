# Backlog — Evergore Protocol Collector

> **Status: v2 (2026-06-13) — strategic questions answered.** Scope = feature-parity dashboard
> (Epic E) **+** clean-code craftsmanship **+** a *showcase of working with Claude* (Epic G).
> Status is "live but learning/fun, no production pressure", so Epic C/F are good-practice, not
> urgent. Architecture: targeted fixes now, full repackaging (D3) deferred until tested. Decisions
> in [open-questions.md](open-questions.md); knowledge base in [knowledge-base/README.md](knowledge-base/README.md).

## ▶ Current status / next action (2026-06-15)

**Done & committed on `Rebuild`** (not pushed; `git log` is the source of truth — we don't mirror it
here): the knowledge base, standards, agent team, devcontainer, README, git permissions, **and the
formerly-entangled storage WIP**, now landed as focused commits via the falsifier→reviewer gate:
- **A2 + B1 + B2** — storage value calculation (`value×qty×quality/100`) in `EvergoreDataEvaluator`,
  a `getAllFor(avatar, after)` storage port, `MetaInformationKey` double keys, and
  `EvergoreDataEvaluatorTest` (6 unit tests) with hand-written fakes; the duplicate-avatars call (**B2**) is fixed.
- **A1** — `.gitattributes` enforces LF.
- **B6** — `clickCookieShit` renamed to dismiss-cookie-banner.
- partial **C4** — fixed the invalid `logback` root level (`verbose`→`info`) and added a test-only
  `logback-test.xml` (root `warn`) so test runs aren't flooded with Micronaut DEBUG.
- tooling: Dependabot config + the `continue` slash command.

**Next (smallest valuable):** **A3** (delete dead `CsvParser`/`FileWriter`/`DiskFileWriter`), then
**A4** (CI: GitHub Actions `mvn -B verify`, Java 17), then Epic **C** (config/secrets, incl. the
hard-coded `C:\…\firefox.exe` in `Browser.java` — **C1**) and **D1/D2** (PageSource port + BDD
acceptance) before Epic **E** (dashboard parity). **C1** is still open — it was *not* part of the WIP.

**High-leverage new picks** (from the enterprise-setup review below): **D6** (ArchUnit framework-import
gate — ~1h, lands green today, makes the hexagonal rule a build failure) and **G7** (deterministic
enforcement hooks) are the strongest showcase additions; **D6** is an excellent next pick alongside A3/A4.

**Open housekeeping:** `.github/modernize/` deleted (done). `docs/claude-code-enterprise-setup.md` is
absorbed into the *"Derived from the enterprise-setup review"* section below → the untracked file is
safe to delete (not to be committed).

**Orient (any new session):** `CLAUDE.md` → `docs/knowledge-base/README.md` → this backlog → `docs/open-questions.md`.

## Priority legend & effort

`P0` unblock everything · `P1` correctness & safety · `P2` architecture & parity · `P3` product/ops growth.
Effort: `S` ≤½ day · `M` ~1–2 days · `L` ≥3 days. IDs are stable references.

## Recommended sequence (post-decisions, 2026-06-13)

Standards docs (**G1**) and `CLAUDE.md` are **done** — they govern everything below. Then:

1. **A1** — enforce **LF** line endings (`.gitattributes`) and commit the whitespace normalization alone.
2. **B1** — write the `EvergoreDataEvaluator` tests (TDD) to lock the core math (also gives A2 its test).
3. **A2** — commit the in-progress storage feature cleanly (now test-backed).
4. Then **A3** (dead code), **A4** (CI), and into Epic **C** (config/secrets) + **D1/D2** (PageSource port + BDD acceptance) as the showcase pieces, before Epic **E** (dashboard parity).

---

## Working agreements (the "how", per the author's goals)

- **Clean code:** small classes, intention-revealing names, no dead code, no hard-coded env/secrets.
- **TDD:** new logic starts with a failing unit test; the domain core stays framework-free and fast to test.
- **BDD (PO view):** each user-facing capability has a plain given/when/then scenario in
  `knowledge-base` / tests, written in product language (members, contribution, overview).
- **SOLID + Hexagonal:** depend on ports, not adapters; Micronaut/Selenium/ORMLite live only at the edges.
- **Definition of Done:** compiles · unit-tested · KB doc updated · committed in a focused commit ·
  no new hard-coded secrets · CI green.

---

## Epic A — Repo hygiene & foundation `P0`

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **A1** | Add `.gitattributes` (`* text=auto eol=lf`; `*.exe`/drivers/`*.sqlite` binary), set `core.autocrlf`, renormalize, commit whitespace **alone** | 95% of the scary diff is CRLF churn ([07-git-state](knowledge-base/git-state.md)) | After commit, `git diff HEAD --ignore-all-space` ≈ the real 15-file change; future diffs aren't whitespace-polluted | S |
| **A2** | Commit the in-progress **storage value calc** as one focused feature commit (+ its test from B1); finalize `Main.java` deletion & `micronaut-cli.yml` move | The real change is small & coherent; get it landed | One reviewable commit; build green | S |
| **A3** | Delete dead code: `CsvParser` (imports Guava — not a dependency), `FileWriter`, `helper/fileWriter/DiskFileWriter` | They don't compile / aren't wired; they blur the surface | Classes gone; build still green | S |
| **A4** | Add **CI** (GitHub Actions: `mvn -B verify` on push/PR, Java 17) | No automated guard today | PRs run build+tests; badge in README | S |
| **A5** | Decide & apply gitignore for `logs.txt`, scratch `.agentscan*.txt`/`diag.txt`, local `database/`; move **personal** scratch patterns (`agentscan`/`gs_temp`/`diag`) out of the *committed* `.gitignore` into `.git/info/exclude`; broaden per **A8** | Keep the committed tree clean & showcase-appropriate | Committed `.gitignore` holds only project-relevant ignores; no stray artifacts | S |
| **A6** | Write a top-level `README.md` (what/why/run) linking the KB | Onboarding | A newcomer can build & run from the README | S |

## Epic B — Lock the core with tests `P0→P1`

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **B1** | Flesh out `EvergoreDataEvaluatorTest` with fake repositories: bank sums, storage value (`value×qty×quality/100`), `UNDEFINED` fallback, `last_updated` watermark | The core aggregation has **zero** real tests ([06-testing](knowledge-base/testing.md)) | Tests fail-first then pass; cover place & withdraw for bank + storage | M |
| **B2** | Fix evaluator bug: duplicate `bankRepo.getAllDifferentAvatars()` call | Dead/confusing call | Single call; test asserts storage-only avatars are included | S |
| **B3** | Expand `EntryFactory`/`EntityParser` tests: date/avatar/type/quality parsing, merged quantity (3+5+7→1 item, qty 15), `Entnahme`, `Impressum` terminator | Parser is barely tested; it's the ingest contract | Table-driven tests over representative raw protocol snippets | M |
| **B4** | Repository tests against in-memory SQLite: `getNewest`, paging, `getAllFor(avatar, after)` | Only incidental smoke coverage | Fast tests using `:memory:` | M |
| **B5** | Resolve the `Category.storage` vs recipe-based `getStorageValue()` question; pin behavior with tests | `Category.storage` is defined but unused — possible bug ([03-domain-model](knowledge-base/domain-model.md)) | Documented decision + test locking the chosen rule | S |
| **B6** | Rename `clickCookieShit` in `PageContentExtractor` (cookie-consent click) | Joke name violates clean-naming; found in the in-flight WIP | Intention-revealing name (e.g. `dismissCookieBanner`) | S |

## Epic C — Configuration & security `P1`  *(good-practice showcase; not urgent while learning/fun)*

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **C1** | Make `Configuration` real via `@ConfigurationProperties` bound from `application.yml`/env (browser, server, db path, credentials path, in-memory toggle, **+ the hard-coded Firefox binary path in `Browser.java`**) | Hard-coded fields defeat config & deployability | No domain settings hard-coded in `.java`; overridable by env | M |
| **C2** | Move the API token out of `TokenValidationFilter` into config/secret; rotate off `"secret_token"` | Hard-coded secret in source + test | Token read from config; absent token fails closed | S |
| **C3** | Stop baking `zugang.txt` into the image; inject credentials via env/secret/mount; read via `FileLoader` port | Credentials in the image is a leak | Image has no credentials; documented secret-injection path | M |
| **C4** | Lower `logback` root from `verbose`; ensure credentials/tokens never logged | Chatty logs may leak secrets | Sensible levels; a log-scrub check | S |

## Epic D — Hexagonal completion `P1→P2`

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **D1** | Introduce a **`PageSource` port** (e.g. `PageContents fetch()`); make Selenium an adapter implementing it; inject `Driver`/`WebDriver` | Biggest structural gap — core is welded to Selenium ([04-architecture](knowledge-base/architecture.md)) | `EvergoreDataExtractor` depends on the port; a fake page-source drives a test | M |
| **D2** | Acceptance (BDD) test of collect→evaluate→overview using in-memory fakes (page-source + `:memory:` DB) | Proves the whole use case without a browser | Green scenario asserting overview numbers from canned protocol text | M |
| **D3** | Restructure packages to `domain / application / adapters{in,out} / config`; keep core framework-free | Make the boundaries explicit & enforceable | Micronaut/Selenium/ORMLite imports only under `adapters`+`config` | L |
| **D4** | Unify the two `TransferType→String` visitors; replace `ApplicationExceptionHandler` `instanceof` chain with a visitor (its own TODO) | Remove duplication & the pattern the project is eliminating | One mapping source; handler has no `instanceof` | S |
| **D5** | Return `Optional` from `getNewest()` instead of `MIN_VALUE` sentinel | Stop leaking fake domain objects | Callers handle empty explicitly; test covers empty repo | S |

## Epic E — Product: replace the Google Sheet `P2→P3`  *(in scope — full parity; no time pressure)*

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **E1** | Compute & store **erzeugter Gildenmehrwert** per avatar (bank+storage net) | Sheet col 5 — the headline metric | Overview shows net value; matches the verified formula | S |
| **E2** | Surface **last bank/storage activity** per avatar (sheet col 10/11) | Easy parity win from stored timestamps | Overview/avatar view shows last-activity | S |
| **E3** | Implement **geschätzte Jagdeinlagerungen** + percentage(s) (sheet col 6/7/8) | Needs the hunt-loot valuation rule resolved first (Q3) | Values reproduce sheet within tolerance on a sample | M |
| **E4** | **Date-range reporting** (Datum von/bis) instead of only a running watermark | Sheet reports over a chosen window | Query metrics for an arbitrary `[from,to]` | M |
| **E5** | Real **overview dashboard** (sortable table, all columns, maybe charts); consider JSON API + small frontend vs. current HTML-string templates | The sheet's value is the at-a-glance view | A guild officer can replace the sheet with this page | L |
| **E6** | **History / time-series**: snapshot metrics over time for trends per avatar | The sheet is a point-in-time; trends are more useful | Stored snapshots; a trend view | L |

## Epic F — Ops, robustness & creative growth `P3`

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **F1** | Replace bundled gecko binaries with Selenium Manager / WebDriverManager; make scraping resilient (retries, selector config, change detection) | Brittle, Windows-pinned drivers | Driver auto-resolves; scrape survives minor markup changes or fails loudly | M |
| **F2** | Observability: health/readiness endpoint, structured logs, last-successful-run metric | Know when scraping silently breaks | `/health` + a visible "last run" signal | S |
| **F3** | **Creative:** weekly guild report (Discord webhook / email), CSV/Sheets export, inactivity alerts | Push value to the guild, not just a page | One delivery channel shipped behind config | M |
| **F4** | **Creative:** multi-guild / multi-world support | Generalize beyond `[Boten]` on `zyrthania` | Config-driven guild/world; data partitioned | L |
| **F5** | **Creative:** public read-only OpenAPI JSON API for guild tooling | Already have OpenAPI; expose clean JSON | Documented `/api` returning per-avatar metrics | M |

## Epic G — Documentation, standards & Claude-workflow showcase `P0→P1`

The project doubles as an example of clean, AI-assisted development — so the "how" is a deliverable.

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **G1** | ✅ **Done 2026-06-13** — `knowledge-base/engineering-handbook.md`, `knowledge-base/working-with-claude.md`, root `CLAUDE.md` | Define how code should look & how we develop; bootstrap every Claude session | Files exist & are linked from the KB index | — |
| **G2** | Top-level `README.md` (what/why/run + links to KB & `CLAUDE.md`) — *same as A6* | Onboarding front door | A newcomer builds, runs, and finds the KB from the README | S |
| **G3** | Keep KB in lockstep with code (baked into Definition of Done) | Docs rot otherwise | Behavior-changing commits touch the matching KB doc | S (ongoing) |
| **G4** | Decide BDD tooling: plain JUnit given/when/then vs Cucumber `.feature` (open question D-9) | Shapes how scenarios are written & shared | Decision logged; first scenario follows it | S |
| **G5** | Optional: short "case study" of the rebuild for the showcase | The meta-goal is demonstrating AI-assisted dev | A narrative others can learn from | M |
| **G6** | *(future)* Static code-analysis / metrics gate — complexity, duplication, coverage thresholds (SonarQube/Sonar). **Re-add the `sonarlint` devcontainer feature** (removed because its `node`→yarn-repo dependency broke apt with a GPG error — troubleshoot the key) | Objective quality guardrail beyond agent review | CI fails on threshold breach | M |

## Epic H — Dev environment & virtualization `P0→P2`

Goal: **fully virtualized dev — nothing (JDK/Maven/Firefox) installed or run on the host.** All work
(incl. agents) runs in the devcontainer or via Docker; JDK upgrades = change an image tag, not the host.
See [knowledge-base/dev-environment.md](knowledge-base/dev-environment.md).

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **H1** | ✅ **Done 2026-06-13** — fix devcontainer to match the project: **Maven** (not Gradle), **JDK 17** pinned, `~/.m2` cache, dependency warm-up | The template devcontainer installed Gradle for a Maven project & JDK 25 vs 17 — wouldn't build | `Reopen in Container` → working `mvn verify` | S |
| **H2** | docker-compose: add a `selenium/standalone-firefox` service; tests use `RemoteWebDriver`; retire bundled `gecko-*-win.exe`. **Re-add the `docker-outside-of-docker` devcontainer feature** (removed to get a building container) | Browser-in-container ⇒ scraping/integration tests run anywhere, no host Firefox | An integration test scrapes via the service | M |
| **H3** | De-hack the `Dockerfile`: drop `dos2unix` (after A1/LF), parameterize the jar name, stop baking `zugang.txt`, add `.dockerignore`, fix `apt-get … -y` | Current image is fragile & bakes secrets | Clean reproducible build; no secret in image (ties to C3) | M |
| **H4** | Single-source the JDK version (devcontainer = Dockerfile bases = pom `jdk.version`) + documented upgrade procedure | One-touch upgrades without host installs | Bumping one set of pins upgrades everything | S |
| **H5** | Re-add a cross-rebuild Maven cache (named volume at `~/.m2`) with correct ownership for the `vscode` user | Faster rebuilds; the first attempt's root-owned volume broke `~/.m2` | Deps cached across rebuilds; container builds clean | S |

---

## Derived from the enterprise-setup review (2026-06-15)

A workflow gap-analysis compared an external *"enterprise Java shop"* Claude Code guide against this
repo and **adversarially verified** every derived item (genuinely missing? worth it for a *solo*
Micronaut showcase?). The guide is Spring/Gradle/team-scale; only the items below survived. The source
doc is intentionally **not** committed — its value lives here.

### New items (verified worth doing)

| ID | Item | Why | Priority | Sequencing / caveat |
|----|------|-----|----------|---------------------|
| **D6** | **ArchUnit** test forbidding `io.micronaut`/`jakarta`/`org.openqa.selenium`/`com.j256.ormlite` imports in `domain`+`businessLogic` | Turns the defining hexagonal rule (CLAUDE.md, Epic D) from convention into a build failure; core is already clean so it lands green | P2 | Independent of D3 (keys off imports, not layout); only truly *gates* once A4/CI exists. ~1h, one test-scope dep. **Highest-leverage new item.** |
| **G7** | **Deterministic enforcement hooks** in `.claude/settings.json`: (a) PreToolUse Edit/Write **secret-scan**; (b) Pre/PostToolUse reject of `System.out`/`printStackTrace`/leftover `// TODO` | Demonstrates the guide's core thesis (CLAUDE.md ~80% vs hooks 100%) — the showcase's headline technique | P2 | secret-scan: tune pattern (must catch `?token=…`), sequence with/after **C2** so it doesn't block the secret cleanup. System.out check: sequence after **A3** (deletes `CsvParser` = ~half the hits); whitelist `@Ignore` Gherkin once D2/G4 land; fold any CI-grep into A4. |
| **G7-fix** | Clean existing violations: `System.out` in `PageContentExtractor`/`AlternativeFileLoaderWrapper`, `printStackTrace` in `SmokeTest`, `// TODO: Visitor-Pattern` in `ApplicationExceptionHandler` | The "no comments / logger-only / self-explanatory" rules are currently violated | P2 | `CsvParser` System.out sites are covered by **A3** (dead-code deletion). |
| **A7** | **Spotless** (`spotless-maven-plugin`) bound to `verify`; `check` in CI, `apply` locally | Ends the CRLF/import-order diff churn (A1/this session); one style, no review nits | P2 | **MUST** use a tab-preserving engine (Eclipse JDT profile), **NOT** google-java-format/palantir (2-space → massive reformat churn). Decide engine first (open-question **D-10**). `check` non-blocking until A4/CI. |
| **G8** | **`/commit`** slash command encoding the strict one-line/no-footer/never-push protocol | Repo's strictest, most-violated-by-default rule (footers slip in); reproducible showcase artifact | P3 | `/review`,`/tdd` rejected (duplicate reviewer/implementer agents). `/spec` deferred → gate on **G4**; keep MCP-free + JUnit-`@Ignore`-first (not Cucumber/Jira). |
| **A8** | Broaden `.gitignore`: add `CLAUDE.local.md`, `.claude/cache/`, `.claude/.tmp/` | Pre-empts committing personal overrides/cache | P3 | Scope to those paths; **avoid** a blanket `**/*.local.*` (would swallow legit `*.local.properties` fixtures). Folds into **A5**. |
| **B7** | Migrate remaining JUnit `Assertions` → **AssertJ** (`SmokeTest`, `MetaInformationTest`) | Single assertion idiom (documented preference) | P3 | Opportunistic. `MetaInformationTest` = clean win; defer `SmokeTest` to its planned Levenshtein-rework. |
| **H6** | `maven-failsafe-plugin` + rename boot/integration tests to `*IT` (separate integration phase) | Keeps the fast TDD loop fast; isolates server-booting tests | P3 | Gate on **H2** (real Selenium IT), **not** D2 (D2 is an in-memory *fast* acceptance test). Update testing.md same change. |
| **G6+** | **JaCoCo** report-only (no enforced threshold) wired into `mvn verify` | Visible coverage to guide B3/B4 test work; low-ceremony first step toward G6 | P3 | Report-only — don't gate a young suite. Promote to a threshold under **G6** later; "into CI" half needs A4. |

### Considered and rejected (do not re-propose without a new reason)

- **Checkstyle** — duplicates **G6** + the reviewer agent; fold into G6 if anything.
- **maven-enforcer** — fabricated origin, BOM-managed deps, in-container fixed JDK → low value.
- **commit-message *skill*** — rule already in CLAUDE.md + reviewer gate + the `git push` deny; a skill adds context cost, not enforcement (use **G8** `/commit`, or a git `commit-msg` hook).
- **security-auditor *subagent*** — redundant with the reviewer's `security` category; OWASP ceremony for a no-prod-pressure scraper. Keep only "extend reviewer + secret-scan hook" under C2/C3.
- **ADRs (`docs/adr/`)** — duplicate the `open-questions.md` Decisions table ("git is history; docs are knowledge").
- **format-on-save hook** — premature; folded into **A7** (needs a formatter + CI first).
- **block-dangerous-bash hook** — now largely covered by the hardened permission **deny** (`git push`, `rm -rf`, `git reset --hard`, added 2026-06-15); revisit only for force-push/rebase nuance.
- **N/A / enterprise-only:** Spring Modulith (not Spring), CLAUDE.md <200-line guard (it's 76), `.mcp.json`/Jira/GitHub MCP (solo, no tracker), `output-styles/`, path-scoped `rules/`, directory-level `CLAUDE.md`, a `docs-writer`/`tdd-runner` agent (tdd = existing `implementer`), quarterly surface audit, English-in-repo (already decided).

---

## Dependency notes

- A1 → A2 (normalize before the clean commit). B1 supports A2.
- D1 → D2 (port enables the acceptance test) → most of E (date-range, dashboard build on a testable core).
- C-epic is independent and can run in parallel; do C2/C3 before any real deployment.
- B5 & Q3 gate E3 (hunt-loot valuation).
