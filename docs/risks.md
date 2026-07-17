# Risk Register: Evergore Protocol Collector

> What can hurt the project or its data, and the countermeasure. Backlog IDs point to still-live
> items; per-item detail stays in [backlog.md](backlog.md). A risk that is mitigated to acceptance
> or has been handled is removed (git is history). Re-check when the
> [roadmap](roadmap.md) changes. L/I = likelihood/impact: H/M/L.

## Data (the SQLite DB is the only history)

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| The game serves only ~30 days of logs; DB loss or a >30-day scrape outage is irreversible | M | H | Open: no automated backup exists; proposed: scheduled off-host copy of the mounted `database/` dir (author decision pending). Migrations must prove 1:1 (backlog D10) |
| The watermark advances before the avatar update and derives from `now()`: an aborted run under-counts permanently | M | H | First milestone (roadmap M1, backlog B15) |
| Silent misclassification corrupts stored data: `Einzahlung` defaults to a deposit, `+1` items merge with their base, the catalog typo zero-values a real item | M | M | Author confirmations plus contract tests or a logged rejection (backlog B9, B11); unknown items become loud (backlog B14); all in roadmap M1 |
| The production DB file is named `temp.sqlite`, inviting careless deletion | L | H | Rename once config is really bindable (backlog C1); until then a known trap |

## External dependency: the game

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| An Evergore HTML/layout change breaks parser or scraper; a silent failure burns the 30-day window | M | H | Fail-loud scraping stays in scope (backlog F1); the last ingest-aborting parser gap closes in roadmap M4 (backlog B17); a push/alert channel (backlog F3) would surface silence |
| The login flow changes (bot protection) or the game shuts down | L | H | Accepted (hobby project): the DB remains as archive |
| ToS/PII stance for scraping and storing other members' activity is undecided (open question D-11) | L | M | Decide and log before any exposure beyond the guild; data stays token-protected |

## Security

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| A secret or host detail lands in the public showcase repo | L | H | Hard no-secrets rule (handbook §7), deny-listed secret reads, gitignored `*.local.*`/`zugang.txt`; staged-content scan hooks (backlog G13); leak protocol: purge and rotate |
| The `/api/**` filter inversion widens the public surface (`/swagger/**`, static assets: no token, rate limit or audit log there) | M | M | Tests pin the exact public set; the accepted surface is documented in the same change (roadmap M2) |
| Credentials are baked into the Docker image | M | M | Image stays local-only until env/secret injection lands (backlog C3, before any deployment beyond the home server) |
| The 2023 `selenium/standalone-firefox:109.0` image carries browser/OS CVEs | M | M | Dependabot `docker` ecosystem plus one refresh pass (backlog H11) |
| The per-IP rate-limit map grows without bound | L | L | Bound/evict (backlog C7) |

## Process: solo, AI-assisted

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| Long hobby pauses lose context between sessions | H | M | `/pause` parks state as `[wip]` and updates the backlog status; `/continue` reorients from repo and git; the KB carries the knowledge |
| Fake-green tests or hallucinated APIs pass a solo review | M | H | Independent falsifier panel and reviewer gate (caught the round-1 fake-greens, see process-learnings 2026-07-09); doc-grounded WebFetch for thin-training-data libraries (backlog G9); escalated gates for risky domains |
| KB drift misleads KB-first agents | M | M | Rotating doc-reviewer sweep at every gate; accuracy sweep and citation guard (backlog G18, G19) |
| The planned history rewrite (after backlog F1) breaks clones and dangles commit-hash references in durable docs | H | L | Author-only and announced; replace doc hash references (e.g. `cc75a2e`) with symbol references before rewriting |
| Parallel worktree strands collide on shared files (build scripts, settings) | M | L | Strand scoping and serialized landings via the gateway; repo-wide reformats only between strands (backlog G17) |

## Latent tech debt

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| Duplicated Bank/Storage repositories with a cross-imported column constant; three unclosed connection sources on one SQLite file | M | M | Unify the base, one managed connection source with `busy_timeout` (backlog D12) |
| Micronaut 5 jump regressions | M | M | Deferred until 1:1 is re-proven; the acceptance net gates it (backlog H9, roadmap M7) |
| Pending Dependabot major PRs (TypeScript, Vite, ESLint) rot unmerged | M | L | One refresh pass through the acceptance and frontend nets (backlog H11); merging stays the author's call |
