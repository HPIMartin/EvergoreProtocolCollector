# Risk Register: Evergore Protocol Collector

> What can hurt the project or its data, and the countermeasure. Backlog IDs point to still-live
> items; per-item detail stays in [backlog.md](backlog.md). A risk that is mitigated to acceptance
> or has been handled is removed (git is history). Re-check when the
> [roadmap](roadmap.md) changes. L/I = likelihood/impact: H/M/L.

## Data (the SQLite DB is the only history)

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| The game serves only ~30 days of logs; DB loss or a >30-day scrape outage is irreversible | M | H | Monthly manual backup by the author (decision 2026-07-17: no automation); a prompt restore re-scrapes the 30-day window, bounding the gap. Migrations must prove 1:1, which the Flyway setup pins with a row-for-row test and, for `V2`, a measured run over a copy of the production snapshot (2026-09-07: 237,538 rows, identical SHA-256 per table before and after) |
| The production DB file is named `temp.sqlite`, inviting careless deletion | L | H | Rename once config is really bindable (backlog C1); until then a known trap |

## External dependency: the game

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| An Evergore HTML/layout change breaks parser or scraper; a silent failure burns the 30-day window | M | H | Fail-loud scraping stays in scope (backlog F1); no single malformed protocol line can abort the ingest any more, and every dropped block head is logged; a push/alert channel (backlog F3) would surface silence |
| The login flow changes (bot protection) or the game shuts down | L | H | Accepted (hobby project): the DB remains as archive |
| ToS/PII stance for scraping and storing other members' activity is undecided (open question D-11) | L | M | Decide and log before any exposure beyond the guild; data stays token-protected |

## Security

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| A secret or host detail lands in the public showcase repo | L | H | Hard no-secrets rule (handbook §7), deny-listed secret reads, gitignored `*.local.*`/`zugang.txt`; staged-content scan hooks (backlog G13); leak protocol: purge and rotate |
| The 2023 `selenium/standalone-firefox:109.0` image carries browser/OS CVEs | M | M | Dependabot `docker` ecosystem plus one refresh pass (backlog H11) |
| Text the author did not write reaches an agent's context and is acted on as an instruction | L | H | Only the author's own chat turn counts as an instruction, every other source is data, and non-routine occurrences are reported rather than quietly refused ([working-with-ai-agents.md](knowledge-base/working-with-ai-agents.md), "Instruction sources"). Observed once, 2026-09-09, with a harmless payload. Nothing mechanical enforces that judgment, so the deny floor and the `ask` tier are the real backstop |

## Process: solo, AI-assisted

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| Long hobby pauses lose context between sessions | H | M | `/pause` parks state as `[wip]` and updates the backlog status; `/continue` reorients from repo and git; the KB carries the knowledge |
| Fake-green tests or hallucinated APIs pass a solo review | M | H | Independent falsifier panel and reviewer gate (caught the round-1 fake-greens, see process-learnings 2026-07-09); doc-grounded WebFetch for thin-training-data libraries (backlog G9); escalated gates for risky domains |
| KB drift misleads KB-first agents | M | M | Rotating doc-reviewer sweep at every gate; accuracy sweep (backlog G18) and the committed citation guard |
| The planned history rewrite (after backlog F1) breaks clones and dangles commit-hash references in durable docs | H | L | Author-only and announced; doc hash references (e.g. `cc75a2e`) become symbol references beforehand (folded into the backlog F1 acceptance) |
| Parallel worktree strands collide on shared files (build scripts, settings) | M | L | Strand scoping and serialized landings via the gateway; repo-wide reformats only between strands (backlog G17) |

## Latent tech debt

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| Duplicated Bank/Storage repositories with a cross-imported column constant; three unclosed connection sources on one SQLite file | M | M | Unify the base, one managed connection source with `busy_timeout` (backlog D12) |
| Micronaut 5 jump regressions | M | M | Deferred until 1:1 is re-proven; the acceptance net gates it (backlog H9, the ops and environment milestone, roadmap M12) |
| Pending Dependabot major PRs (TypeScript, Vite, ESLint) rot unmerged | M | L | One refresh pass through the acceptance and frontend nets (backlog H11); merging stays the author's call |
