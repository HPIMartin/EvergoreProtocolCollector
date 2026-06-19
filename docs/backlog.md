# Backlog — Evergore Protocol Collector

> **Status: v2 (2026-06-13) — strategic questions answered.** Scope = feature-parity dashboard
> (Epic E) **+** clean-code craftsmanship **+** a *showcase of working with Claude* (Epic G).
> Status is "live but learning/fun, no production pressure", so Epic C/F are good-practice, not
> urgent. Architecture: targeted fixes now, full repackaging (D3) deferred until tested. Decisions
> in [open-questions.md](open-questions.md); knowledge base in [knowledge-base/README.md](knowledge-base/README.md).

## ▶ Current status / next action

`git log` is the record of what landed; this section tracks only **where we are and what's next**.

**Where we are:** the rebuild's core is in place — the storage-value feature, a hexagonal `PageSource`
port for scraping, and an ArchUnit guard keeping `domain`/`businessLogic` framework-free. **The build
is now Gradle on an up-to-date stack** (2026-06-16): Maven→Gradle (Kotlin `build.gradle.kts`, wrapper
9.5.1), **Java 17→25** (Gradle toolchain, foojay-provisioned), **Micronaut 3.8.4→4.10.3**; the
devcontainer (JDK 25) and `Dockerfile` (temurin-25 build → selenium-firefox runtime, `installDist`)
were rebuilt to match. `./gradlew build` is green (22 tests) and the migrated stack was verified **1:1**
against the production DB (`/overview`, `/avatars/{a}/bank|storage` byte-identical after LF
normalisation). The repo is a clean public showcase on a single `main` branch.

**H8 + A7 landed (2026-06-19).** **H8:** warnings-as-errors enforced — `-Xlint:all` + `-Werror` on every
`JavaCompile`, `-serial`/`-processing` excluded, gate proven real; Java-25 Netty native-access silenced +
future-proofed via `--enable-native-access=ALL-UNNAMED` (test JVM + dist start script). **A7:** adopted a
**shared Eclipse formatter** — one central `config/eclipse/formatter.xml` (tabs, `lineSplit=180`) used by
VS Code, Eclipse, IntelliJ *and* the Gradle build (Spotless `eclipse()` + `importOrder` + whitespace); VS
Code formats + organizes imports on save; **star imports forbidden** (`starThreshold 999999`). Codebase
reflowed to the standard (38 files, max line 180) **and all wildcard imports expanded to explicit** (IDE
organize-imports; Spotless `removeUnusedImports` added) — wildcard-free, build green. Author chose *uniform
+ enforced* over hand-tuned wrapping; `.editorconfig` dropped (VS-Code-only).

**Formatter fine-tuning landed (2026-06-19).** Empirically reduced to the two settings that are *real*
deviations: `alignment_for_selector_in_method_invocation=48` (method chains wrap **one per `.`** only when
they exceed `lineSplit=180`; 3 files reflowed) and `number_of_blank_lines_at_end_of_method_body=0` (no blank
before a method's closing `}`). The other handoff candidates proved to be **JDT defaults already** — `if`
one-liners are broken (braceless) and multiple blanks collapse to one without any setting — so they were
**not** added (the profile stays deviations-only). Removing the blank after `class X {` / at method-body
start is **not possible** in JDT without also dropping the wanted inter-method blanks (`number_of_empty_lines_to_preserve`
governs all three), so it was dropped. **Braces** are a separate concern: the formatter wraps `if`-bodies but
cannot *insert* `{ }`, so always-braces is enforced outside the formatter.

**Checkstyle landed (2026-06-19, single-purpose).** A minimal `config/checkstyle/checkstyle.xml` (the
Gradle-default path) with **exactly one rule, `NeedBraces`** — disjoint from the formatter (layout stays in
`formatter.xml`; no rule lives in both, so no parallel upkeep). Gradle `checkstyle` plugin → `check`, gate
proven by a deliberate braceless `if`; same config drives the IDE (vscode-checkstyle, recommended +
devcontainer-installed). The earlier rejection still holds for Checkstyle *as a general linter* (overlap with
reviewer/Sonar G6); this is the narrow brace gap only. **Note:** Checkstyle only *reports* (no auto-fix — the
intended VS Code `addBraces` cleanup turned out **not to exist** in redhat.java); braces are added via the
"Add braces" quick-fix. New item **D6**: **refactor** `EvergoreItem` in-place — keep the enum hard-links,
**not** externalize.

**Next dev action — pick from the remaining items H7 unblocked** (all land *in Gradle*): **G6+**
(JaCoCo), **H6** (failsafe → Gradle integration-test set), **C2** (move `secret_token` out of source),
**C5** (vuln scan), **D2** (automated 1:1 acceptance test — boot against a **gitignored** prod snapshot;
PII never committed). **H9** (jump to Micronaut 5) only *after* 1:1 is re-proven. Plan via the agent
pipeline (planner → implementer → falsifier → reviewer).
*(A4/CI stays deprioritized — local-only Docker → home-server deploy.)*

**Gotchas worth keeping:**
- The IDE re-saves edited files as **CRLF**; `.gitattributes` normalizes to LF on commit — ignore the
  warning, `git diff --check` if unsure. (The new build serves CRLF if the working-tree templates are
  CRLF; a clean LF checkout serves LF — the 1:1 check normalises line endings.)
- **Java 25 made `java.sql.Timestamp.from` strict** (`Math.multiplyExact` throws where JDK 17 wrapped);
  the empty watermark `LocalDateTime.MIN` was replaced by `BEGINNING_OF_TIME` in `EvergoreDataEvaluator`.
  **ArchUnit must be ≥ 1.4.1** to read Java 25 bytecode (older silently checks zero classes = false green).
- The **devcontainer/`Dockerfile` are not built inside the devcontainer** (no docker-in-docker — H2);
  validate them on the Docker host / next container rebuild.
- The Claude "always allow" flow can re-pollute the committed `settings.json` (path-bearing rules +
  tabs→spaces). Prefer bare commands matching the portable `Bash(<cmd>:*)` rules; diff against HEAD if unsure.
- Keep `zugang.txt` (creds, gitignored); machine-specific config stays in gitignored `*.local.*` files.

**Orient (any new session):** `CLAUDE.md` → `docs/knowledge-base/README.md` → this backlog → `docs/open-questions.md`.

## Priority legend & effort

`P0` unblock everything · `P1` correctness & safety · `P2` architecture & parity · `P3` product/ops growth.
Effort: `S` ≤½ day · `M` ~1–2 days · `L` ≥3 days. IDs are stable references.

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
| **A4** | ~~CI (GitHub Actions)~~ **Deprioritized 2026-06-15** (local-only deploy, no CI). *Optional later, low prio:* a **local** gate — git `pre-commit` running `mvn -B verify` | Solo + local Docker→home-server deploy; no shared PRs to guard | revisit if the repo ever goes shared/CI | S |
| **A5** | Decide & apply gitignore for `logs.txt`, scratch `.agentscan*.txt`/`diag.txt`, local `database/`; move **personal** scratch patterns (`agentscan`/`gs_temp`/`diag`) out of the *committed* `.gitignore` into `.git/info/exclude`; broaden per **A8** | Keep the committed tree clean & showcase-appropriate | Committed `.gitignore` holds only project-relevant ignores; no stray artifacts | S |

## Epic B — Lock the core with tests `P0→P1`

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **B3** | Expand `EntryFactory`/`EntityParser` tests: date/avatar/type/quality parsing, merged quantity (3+5+7→1 item, qty 15), `Entnahme`, `Impressum` terminator | Parser is barely tested; it's the ingest contract | Table-driven tests over representative raw protocol snippets | M |
| **B4** | Repository tests against in-memory SQLite: `getNewest`, paging, `getAllFor(avatar, after)` | Only incidental smoke coverage | Fast tests using `:memory:` | M |
| **B5** | Resolve the `Category.storage` vs recipe-based `getStorageValue()` question; pin behavior with tests | `Category.storage` is defined but unused — possible bug ([03-domain-model](knowledge-base/domain-model.md)) | Documented decision + test locking the chosen rule | S |

## Epic C — Configuration & security `P1`  *(good-practice showcase; not urgent while learning/fun)*

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **C1** | Make `Configuration` real via `@ConfigurationProperties` bound from `application.yml`/env (browser, server, db path, credentials path, in-memory toggle, **+ the hard-coded Firefox binary path in `Browser.java`**) | Hard-coded fields defeat config & deployability | No domain settings hard-coded in `.java`; overridable by env | M |
| **C2** | Move the API token out of `TokenValidationFilter` into config/secret; rotate off `"secret_token"` — **H7 done (2026-06-16); now actionable** | Hard-coded secret in source + test | Token read from config; absent token fails closed | S |
| **C3** | Stop baking `zugang.txt` into the image; inject credentials via env/secret/mount; read via `FileLoader` port | Credentials in the image is a leak | Image has no credentials; documented secret-injection path | M |
| **C4** | Lower `logback` root from `verbose`; ensure credentials/tokens never logged | Chatty logs may leak secrets | Sensible levels; a log-scrub check | S |
| **C5** | **Simple dependency vulnerability scan** wired into the build (OWASP dependency-check or the Gradle-native equivalent) | Catch known-vulnerable deps; good-practice for the showcase | Build surfaces known CVEs in deps. Gated on **H7** (land in Gradle); *Dependabot already covers basic dependency alerts* | S |

## Epic D — Hexagonal completion `P1→P2`

| ID | Item | Why | Acceptance | Effort |
|----|------|-----|------------|--------|
| **D2** | Acceptance test of collect→evaluate→overview — **H7 done (2026-06-16); now actionable.** Two flavours: (a) in-memory fakes (page-source + `:memory:` DB) for a fast canned-text scenario; (b) the **offline 1:1 benchmark** — boot against a **gitignored** prod-DB snapshot and assert the rendered pages match stored (PII) expecteds, never committed | Proves the whole use case without a browser | Green scenario asserting overview numbers; benchmark reproduces the manual 1:1 check | M |
| **D3** | Restructure packages to `domain / application / adapters{in,out} / config`; keep core framework-free | Make the boundaries explicit & enforceable | Micronaut/Selenium/ORMLite imports only under `adapters`+`config` | L |
| **D4** | Unify the two `TransferType→String` visitors; replace `ApplicationExceptionHandler` `instanceof` chain with a visitor (its own TODO) | Remove duplication & the pattern the project is eliminating | One mapping source; handler has no `instanceof` | S |
| **D5** | Return `Optional` from `getNewest()` instead of `MIN_VALUE` sentinel | Stop leaking fake domain objects | Callers handle empty explicitly; test covers empty repo | S |
| **D6** | **Refactor the `EvergoreItem` catalog in-place** — the ~660-constant enum (752 lines) is the project's biggest file. **Decision 2026-06-19: keep it an enum** (the compile-checked recipe hard-links are valued) and **do NOT externalize** to a config/YAML file. Refactor for readability/maintainability instead, e.g.: pull the nested `Category`/`Recipe`/`Ingredient` types into their own files; add concise recipe/ingredient factory helpers to shorten each constant; group constants. Approach TBD with the author. Any change **must** keep a **golden-master test** asserting `getStorageValue`/`getWithdrawlValue` are unchanged for all items | Tame the biggest file without losing the type-safe cross-references; eases value maintenance (relates to **D-3**) | P3 | Golden-master test gates it (1:1). Independent of A7. |

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
| **H2** | docker-compose: add a `selenium/standalone-firefox` service; tests use `RemoteWebDriver`; retire bundled `gecko-*-win.exe`. **Re-add the `docker-outside-of-docker` devcontainer feature** (removed to get a building container) | Browser-in-container ⇒ scraping/integration tests run anywhere, no host Firefox | An integration test scrapes via the service | M |
| **H3** | *Mostly done 2026-06-16 with the Gradle migration:* `dos2unix` dropped (LF enforced), jar-name hack gone (version-independent `installDist` dir), `.dockerignore` added, `apt` cleaned. **Remaining: stop baking `zugang.txt`** → folds into **C3** | Image was fragile & bakes secrets | Clean reproducible build; **no-secret-in-image still open (C3)** | S |
| **H5** | *(was: cross-rebuild Maven cache — obsolete, Maven gone)* Optional: persist the Gradle caches (`~/.gradle`, build cache) across rebuilds with correct `vscode` ownership | Faster rebuilds | Gradle deps/build cached across container rebuilds | S |
| **H8** | ~~**Warnings-as-errors:** `-Werror` + `-Xlint:all` in the Gradle build~~ **DONE 2026-06-19.** `-serial`/`-processing` excluded deliberately; code was already warning-clean; gate proven real (a deliberate `[cast]` warning fails the build). Java-25 Netty native-access **enabled** (`--enable-native-access=ALL-UNNAMED`, test JVM + start script), not suppressed | Mechanically enforces the warnings-as-errors standard (CLAUDE.md/handbook) | Build fails on any warning; existing warnings fixed or deliberately excluded | M |
| **H9** | **Jump Micronaut 4.10 → 5.0** (Java-25 baseline, Apr 2026) and re-verify 1:1. **Only after** the current migration's 1:1 is locked (ideally via the **D2** automated benchmark) | Stay on the newest line; deferred deliberately — one framework risk at a time | `./gradlew build` green on MN5 + endpoints still 1:1 vs the prod snapshot | M |

---

## Derived from the enterprise-setup review (2026-06-15)

A workflow gap-analysis compared an external *"enterprise Java shop"* Claude Code guide against this
repo and **adversarially verified** every derived item (genuinely missing? worth it for a *solo*
Micronaut showcase?). The guide is Spring/Gradle/team-scale; only the items below survived. The source
doc is intentionally **not** committed — its value lives here.

### New items (verified worth doing)

| ID | Item | Why | Priority | Sequencing / caveat |
|----|------|-----|----------|---------------------|
| **G7** | **Deterministic enforcement hooks** in `.claude/settings.json`: (a) PreToolUse Edit/Write **secret-scan**; (b) Pre/PostToolUse reject of `System.out`/`printStackTrace`/leftover `// TODO` | Demonstrates the guide's core thesis (CLAUDE.md ~80% vs hooks 100%) — the showcase's headline technique | P2 | secret-scan: tune pattern (must catch `?token=…`), sequence with/after **C2** so it doesn't block the secret cleanup. System.out check: the dead-code deletion already removed ~half the hits (`CsvParser`); whitelist `@Ignore` Gherkin once D2/G4 land. |
| **G7-fix** | Clean existing violations: `System.out` in `SeleniumPageSource`/`AlternativeFileLoaderWrapper`, `printStackTrace` in `SmokeTest`, `// TODO: Visitor-Pattern` in `ApplicationExceptionHandler`, the empty `catch (Exception e) {}` swallow + `// NOOP` comment in `SeleniumPageSource`, and the commented-out line in `SmokeTest` | The "no comments / logger-only / self-explanatory" rules are currently violated | P2 | `CsvParser` System.out sites are covered by the earlier dead-code deletion. |
| **A7** | ~~**Spotless** bound to the build~~ **DONE 2026-06-19 (shared Eclipse formatter).** One central `config/eclipse/formatter.xml` (tabs, `lineSplit=180`, empty bodies compact, enum one-per-line) consumed by VS Code + Eclipse + IntelliJ + Gradle/Spotless (`eclipse().configFile` + `importOrder` java→extern→`dev.schoenberg`→static-last + `trimTrailingWhitespace` + `endWithNewline`). VS Code formats + organizes imports on save; **star imports forbidden** (`starThreshold 999999`). Codebase reflowed (38 files, max 180). **Finishing-step open:** one-time wildcard→explicit expansion (IDE) + no-wildcard build gate | One enforced style across IDEs + build from a single file; ends diff churn & review nits | P2 | D-10 resolved. Reflow accepted for uniformity. `.editorconfig` dropped (VS-Code-only); universal basics → native VS Code `files.*`. |
| **G8** | **`/commit`** slash command encoding the strict one-line/no-footer/never-push protocol | Repo's strictest, most-violated-by-default rule (footers slip in); reproducible showcase artifact | P3 | `/review`,`/tdd` rejected (duplicate reviewer/implementer agents). `/spec` deferred → gate on **G4**; keep MCP-free + JUnit-`@Ignore`-first (not Cucumber/Jira). |
| **A8** | Broaden `.gitignore`: add `CLAUDE.local.md`, `.claude/cache/`, `.claude/.tmp/` | Pre-empts committing personal overrides/cache | P3 | Scope to those paths; **avoid** a blanket `**/*.local.*` (would swallow legit `*.local.properties` fixtures). Folds into **A5**. |
| **B7** | Migrate remaining JUnit `Assertions` → **AssertJ** (`SmokeTest`, `MetaInformationTest`) | Single assertion idiom (documented preference) | P3 | Opportunistic. `MetaInformationTest` = clean win; defer `SmokeTest` to its planned Levenshtein-rework. |
| **H6** | `maven-failsafe-plugin` + rename boot/integration tests to `*IT` (separate integration phase) | Keeps the fast TDD loop fast; isolates server-booting tests | P3 | Gate on **H2** (real Selenium IT), **not** D2 (D2 is an in-memory *fast* acceptance test). Update testing.md same change. **2026-06-15: land in Gradle post-H7 (failsafe → Gradle integration test set).** |
| **G6+** | **JaCoCo** report-only (no enforced threshold) wired into `mvn verify` | Visible coverage to guide B3/B4 test work; low-ceremony first step toward G6 | P3 | Report-only — don't gate a young suite. Promote to a threshold under **G6** later; "into CI" half needs A4. **2026-06-15: land in Gradle post-H7.** |
| **G9** | **Point Claude at official docs (WebFetch) for less-trafficked libraries** — a `working-with-claude.md` convention: for **Micronaut / ORMLite / Selenium / RxJava** (thin in LLM training data), fetch the official docs before writing against an unfamiliar API; prefer doc-grounded code over confabulation | Enterprise-audit Pitfall #5, the most stack-relevant gap: a solo dev has no reviewer to catch a hallucinated API, and ArchUnit/tests catch structure, not invented method signatures. MCP-free (WebFetch is available) | P2 | Flagged independently by two audit reviewers. Doc-only; fits the Epic-G showcase. |
| **G10** | **SessionStart orient/lessons hook** in `.claude/settings.json`: deterministically inject the orient pointer (CLAUDE.md → KB README → backlog "Current status" → open-questions → `process-learnings.md`) so every fresh session reads the lessons first | The 100%-fires complement to the advisory `/continue` + CLAUDE.md "Start here"; completes the self-improvement loop and is the most on-thesis hooks-over-rules showcase artifact (sibling to **G7**) | P3 | Enterprise-audit gap. The lessons file already exists (`process-learnings.md`); only the deterministic hook is missing. |
| **G11** | **Improve the AI/agent working environment in the devcontainer** (low prio): pre-install tools used every session (`git-filter-repo`, `sqlite3`, `jq`), pre-allow common read-only commands in the committed `settings.json` (portable forms only), and add conventions/hooks that cut token use (scratch-file hygiene, scoped reads over blind re-scans) | Recurring friction: missing `git-filter-repo`/`docker`, repeated permission prompts, the "always allow" flow re-polluting `settings.json`, broad re-reads burning tokens | P3 | Author request 2026-06-16. Sits with **G7**/**G10** (the hooks/showcase items). |

### Considered and rejected (do not re-propose without a new reason)

- **Checkstyle *as a general linter*** — duplicates **G6** + the reviewer agent; fold into G6 if anything.
  *(Introduced 2026-06-19 in a single-purpose, disjoint scope: one rule, `NeedBraces`, for the brace gap the
  formatter cannot fill — see build-run-deploy.md. The "general linter" rejection still stands.)*
- **maven-enforcer** — fabricated origin, BOM-managed deps, in-container fixed JDK → low value.
- **commit-message *skill*** — rule already in CLAUDE.md + reviewer gate + the `git push` deny; a skill adds context cost, not enforcement (use **G8** `/commit`, or a git `commit-msg` hook).
- **security-auditor *subagent*** — redundant with the reviewer's `security` category; OWASP ceremony for a no-prod-pressure scraper. Keep only "extend reviewer + secret-scan hook" under C2/C3.
- **ADRs (`docs/adr/`)** — duplicate the `open-questions.md` Decisions table ("git is history; docs are knowledge").
- **format-on-save hook** — premature; folded into **A7** (needs a formatter + CI first).
- **block-dangerous-bash hook** — now largely covered by the hardened permission **deny** (`git push`, `rm -rf`, `git reset --hard`, added 2026-06-15); revisit only for force-push/rebase nuance.
- **N/A / enterprise-only:** Spring Modulith (not Spring), CLAUDE.md <200-line guard (it's 76), `.mcp.json`/Jira/GitHub MCP (solo, no tracker), `output-styles/`, path-scoped `rules/`, directory-level `CLAUDE.md`, a `docs-writer`/`tdd-runner` agent (tdd = existing `implementer`), quarterly surface audit, English-in-repo (already decided).

---

## Dependency notes

- D2 (the acceptance test) enables most of E (date-range, dashboard build on a testable core).
- C-epic is independent and can run in parallel; do C2/C3 before any real deployment.
- B5 & Q3 gate E3 (hunt-loot valuation).
