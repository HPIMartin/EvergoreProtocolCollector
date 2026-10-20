# Risk Register: Evergore Protocol Collector

> What can hurt the project or its data, and the countermeasure. Backlog IDs point to still-live
> items; per-item detail stays in [backlog.md](backlog.md). A risk that is mitigated to acceptance
> or has been handled is removed (git is history). Re-check when the
> [roadmap](roadmap.md) changes. L/I = likelihood/impact: H/M/L.

## Data (the SQLite DB is the only history)

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| The game serves only ~30 days of logs; DB loss or a >30-day scrape outage is irreversible | M | H | The author's monthly manual backup, whose restore re-scrapes the 30-day window ([open-questions.md](open-questions.md), 2026-07-17); every migration is proven row for row before its deploy ([build-run-deploy.md](knowledge-base/build-run-deploy.md), "Schema migrations (Flyway)") |
| The production DB file is named `temp.sqlite`, inviting careless deletion | L | H | Rename once config is really bindable (backlog C1); until then a known trap |
| A database directory open to every account lets any account on the host rewrite or delete the history | L | H | The ownership and mode of release step 3 ([build-run-deploy.md](knowledge-base/build-run-deploy.md)) |

## External dependency: the game

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| An Evergore HTML/layout change breaks parser or scraper; a silent failure burns the 30-day window | M | H | Fail-loud scraping stays in scope (backlog F1); no single malformed protocol line aborts the ingest, and every dropped block head is logged; a push/alert channel (backlog F3) would surface silence |
| The login flow changes (bot protection) or the game shuts down | L | H | Accepted (hobby project): the DB remains as archive |
| ToS/PII stance for scraping and storing other members' activity is undecided (open question D-11) | L | M | Decide and log before any exposure beyond the guild; data stays token-protected |

## Security

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| A secret or host detail lands in the public showcase repo | L | H | Hard no-secrets rule (handbook §7), deny-listed secret reads, gitignored `*.local.*`/`zugang.txt`; staged-content scan hooks (backlog G13); leak protocol: purge and rotate |
| The 2023 `selenium/standalone-firefox:109.0` image carries browser/OS CVEs | M | M | Dependabot `docker` ecosystem plus one refresh pass (backlog H11) |
| An agent pushes what it should not, with the author's rights on the PC or with the agent machine's deploy key ([git-state.md](knowledge-base/git-state.md), "What GitHub enforces") | L | H | Layered push guards from the managed policy down to the rulesets, their residue accepted ([working-with-ai-agents.md](knowledge-base/working-with-ai-agents.md), "Permissions & autonomy"); open work: the refused push per ruleset (backlog G36) and the line between the machines (backlog G39) |
| The unattended agent seat runs a Docker daemon of its own inside its container, so an agent could try to escape the container through it | L | H | Sysbox runs the daemon without a host socket, and the container reaches no local network ([dev-environment.md](knowledge-base/dev-environment.md), "Docker on the agent seat") |
| On the PC's case-insensitive checkout, a fetched ref that differs from `main` only in case (a branch `Main`, a tag `MAIN`) takes the place of `origin/main` or `main` | L | H | The sync's case check after every fetch (handbook §7); a background fetch after that check stays open until the PC's refs are case-sensitive (open question D-21) |
| Text the author did not write reaches an agent's context and is acted on as an instruction | L | H | Only the author's own chat turn or pull request comment is an instruction, with the deny floor and the `ask` tier as the backstop ([working-with-ai-agents.md](knowledge-base/working-with-ai-agents.md), "Instruction sources") |

## Process: solo, AI-assisted

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| Long hobby pauses lose context between sessions | H | M | `/pause` parks state as `[wip]` and updates the backlog status; `/continue` reorients from repo and git; the KB carries the knowledge |
| Fake-green tests or hallucinated APIs pass a solo review | M | H | An independent falsifier panel and reviewer gate, escalated for risky domains ([multi-agent-playbook.md](knowledge-base/multi-agent-playbook.md)) |
| KB drift misleads KB-first agents | M | M | Rotating doc-reviewer sweep at every gate; accuracy sweep (backlog G18) and the committed citation guard |
| The planned history rewrite (after backlog F1) breaks clones and dangles commit-hash references in durable docs | H | L | The author rewrites history only after announcing it and after durable docs swap their commit hashes for symbol references (backlog F1) |
| Parallel worktree strands collide on shared files (build scripts, settings) | M | L | Scoped strands land one at a time through the gateway (handbook §7); a repo-wide reformat runs only between strands |

## Latent tech debt

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| Micronaut 5 jump regressions | M | M | Deferred until 1:1 is re-proven; the acceptance net gates it (backlog H9, the ops and environment milestone, roadmap M12) |
| The pending Dependabot major, TypeScript 6 to 7, rots unmerged | M | L | Revisited when TypeScript 7.1 turns stable or typescript-eslint's native path (typescript-eslint #12803) lands, which even then needs TypeScript 6 beside it; Oxlint is the alternative ([open-questions.md](open-questions.md), 2026-10-13) |
