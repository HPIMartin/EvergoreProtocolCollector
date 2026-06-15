# Open Questions & Decisions Log

A living record of decisions and the questions that shape the plan. When a question is answered,
move it to **Decisions** with the date and rationale. Agents: read this before changing scope.

## Decisions (answered)

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-06-13 | Created the knowledge base under `docs/knowledge-base/` and this backlog/log under `docs/` | The author asked for durable, agent-readable findings; avoids re-scanning |
| 2026-06-13 | **Q1 — Scope:** Feature-parity dashboard (Epic E) **+** clean-code craftsmanship **+** the project doubles as a *showcase of working with Claude*. The KB must also document **how the code should look and how we develop** (TDD/BDD/clean code). | Author wants both the product (replace the sheet) and an exemplary, well-documented way of working. → added `engineering-handbook.md`, `working-with-claude.md`, `/CLAUDE.md`; new Epic G. |
| 2026-06-13 | **Q2 — Status:** Game is live but this is primarily learning/fun; **no production pressure**. | Security/ops (Epic C/F) stay good-practice for the showcase but are not urgent; we *can* run against live evergore.de when useful. |
| 2026-06-13 | **Q3 — Uncommitted diff:** Normalize whitespace to **LF** (`.gitattributes`, own commit), then commit the real 15-file storage feature cleanly (A1 → A2). | Author explicitly wants Linux line endings in the repo and a clean history. |
| 2026-06-13 | **Q4 — Architecture depth:** Targeted fixes now (PageSource port D1, real config/secrets C1–C3, unify visitors D4); **defer** the full `domain/application/adapters` repackaging (D3) until the core is test-covered. | Lower risk; restructure on top of a green test suite, not before it. |
| 2026-06-13 | **Language:** conversation in German; **code, comments, identifiers and docs in English** (German kept only for domain terms / item names). | Author's stated preference. |
| 2026-06-13 | **Commit workflow (strict):** propose **one** single-line, present-tense-verb message → author confirms → commit; **no body, no `Co-Authored-By`/footer; never `git push`.** | Author's rule; pushing is his decision alone. Encoded in `CLAUDE.md`, handbook §7, memory. |
| 2026-06-13 | **TDD/BDD cycle:** red→green→refactor→commit. BDD is doc/showcase-only: write Gherkin `@Ignore`-first → commit, then activate & drive green via TDD → commit. | Author's process; see handbook §4/§5. |
| 2026-06-13 | **Multi-agent team:** Planner = hat (you+me, Opus); subagents `implementer` (Sonnet), `falsifier` (Sonnet, single, feature-end), `reviewer`/gate (Opus, **per feature-commit**). Implementer makes pre-approved micro-commits; agents never push. | Q-round 2026-06-13. Built in `.claude/agents/` + `knowledge-base/multi-agent-playbook.md`. |
| 2026-06-13 | **Dev environment:** fully virtualized via devcontainer — **nothing native on the host**; agents run in-container. Devcontainer fixed now (Maven, JDK 17 pinned, m2 cache); Selenium-compose + Dockerfile de-hack deferred (Epic H). Sequence: fix+commit on host, then `Reopen in Container` for coding. | Author wants future JDK upgrades (e.g. Java 25) without local installs. |
| 2026-06-13 | **In-flight WIP:** resolve the entangled storage-calc WIP **in the container, verified** (split via `git add -p`; fix the dup-avatars bug, the `clickCookieShit` name, and the hard-coded Firefox path), **not** blind-committed on the host. Plan in `backlog.md` → Current status. | The WIP mixes concerns in one file and the build can't be verified on the host. |
| 2026-06-13 | **Git permissions:** `git add`/`commit` (and read-only git) allowed without prompts; **`git push` hard-denied** in `.claude/settings.json`. | Author's request; pushing stays a manual, deliberate act. |
| 2026-06-15 | **Migrate the build Maven → Gradle** as the next major item (before D2 and feature work). Reverses the 2026-06-13 "devcontainer fixed to Maven" stance. Keep the tooling surface minimal to ease the move: **defer** A7 (Spotless), G6+ (JaCoCo), H6 (failsafe) and the `failOnWarning` flip and **land them in Gradle**, not as new Maven plugins. | Author is no longer happy with Maven; a low plugin count eases the migration. |
| 2026-06-15 | **Warnings-as-errors:** compiler/lint warnings are build failures (`-Xlint:all` + `failOnWarning`); default is to **fix**, not suppress; obsolete lint excluded deliberately (e.g. `-serial`); ask the author per-warning when unsure. The flip rides with the Gradle migration (build config). | Author's rule — surfaces quality issues early; keeps the showcase build clean. Codified in CLAUDE.md golden rules + handbook §3/§8. |
| 2026-06-15 | **No local setup details in committed files:** no absolute paths, usernames, or host layout in the repo; machine-specific config (e.g. an absolute-path-scoped `cd` permission) lives in gitignored `*.local.*` files. | Author's **core rule** — the repo is a public showcase; keep committed config portable and free of personal environment details. Codified in CLAUDE.md + handbook §7. |

## Strategic questions — ANSWERED 2026-06-13

The four strategic questions were answered by the author (see the Decisions table above):

- **Q1 — Scope:** parity dashboard **+** clean-code craftsmanship **+** *showcase of working with Claude*
  (the KB documents how the code should look and how we develop). → Epic E in scope; new Epic G added.
- **Q2 — Status:** live but learning/fun; no production pressure. → C/F good-practice, not urgent.
- **Q3 — Diff:** normalize to LF then clean feature commit (A1 → A2).
- **Q4 — Architecture:** targeted fixes now (D1, C1–C3, D4); defer full repackaging (D3) until tested.

## Detail questions (lower priority — to confirm later)

- **D-1 — Sheet columns 8 & 9:** exact meaning/formula of the 2nd percentage and the small integer
  count? (col 7 ≈ hunt-loot ÷ storage-deposits is confirmed; 8 & 9 are not — see [google-sheet](knowledge-base/google-sheet.md)).
- **D-2 — Other workbook tabs:** does the spreadsheet have more tabs (raw protocol, item price list,
  per-month history) beyond `gid=937183112`? The CSV export only sees one. Should we scrape/import them?
- **D-3 — Item value source of truth:** are `EvergoreItem.marketValue`s authoritative, or were the
  sheet's values maintained separately (and possibly drifted)? How are new items / price changes maintained?
- **D-4 — Hunt-loot valuation rule:** the sheet estimates hunt-deposit value (col 6) but code gives
  `JAGDBEUTEN` storage value 0. What's the intended rule? (gates E3, relates to B5 / `Category.storage`.)
- **D-5 — `Category.storage` field:** dead artifact to remove, or the intended (currently bypassed)
  storage-valuation multiplier? (B5)
- **D-6 — Output preference:** keep server-rendered HTML, or move to a JSON API + small frontend for
  the dashboard? (shapes E5)
- ~~**D-7 — Deployment target**~~ ✅ DECIDED 2026-06-15: built as a **Docker image locally** and run on the
  author's **home server** ("im Keller"); **no CI/CD for now** (optionally a *local* build gate later, low prio).
  → **A4 deprioritized**; the deploy-relevant items are **C3** (inject credentials, don't bake `zugang.txt` into
  the image) and **H3** (Dockerfile de-hack).
- ~~**D-8 — Language**~~ ✅ DECIDED 2026-06-13: conversation German; code/comments/docs English with German domain terms.
- **D-9 — BDD tooling:** plain JUnit given/when/then, or a framework (Cucumber)? Assumed plain unless asked.
- **D-10 — Formatter engine (gates A7):** if Spotless is adopted, which engine? The repo is uniformly
  **TAB**-indented, so it must be a tab-preserving Eclipse JDT profile — **not** google-java-format/palantir
  (2-space, would force a repo-wide reformat-churn commit). Recommended: Eclipse JDT (tabs). Author's call before A7.
  *Update 2026-06-15:* with the Gradle migration decided, A7/Spotless re-homes to **Gradle** (still
  tab-preserving, not google-java-format); pick the Gradle Spotless engine during the migration.
- **D-11 — ToS / PII (enterprise-audit Pitfall #7):** the tool scrapes evergore.de and stores *other*
  guild members' bank/storage activity. No production pressure (Q2), but worth a deliberate stance:
  is the scraping within the site's ToS, and is storing other members' activity acceptable for the
  guild's use? Low priority; logged so it isn't silently ignored.

## Assumptions currently baked into the plan (challenge if wrong)

1. This is a single-guild, low-traffic tool; correctness & maintainability ≫ performance.
2. The author wants to *continue* the rebuild, not abandon it.
3. The verified Gildenmehrwert formula (deposits − withdrawals across bank+storage) is the
   intended definition of member contribution.
