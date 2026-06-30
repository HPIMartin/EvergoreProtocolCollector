# 10 — Working with AI agents (the AI-assisted workflow showcase)

A goal of this project is to be a **worked example of developing well *with* an AI agent**. The
showcase tool is Claude Code, but this workflow is tool-neutral — any capable AI coding agent (or a
human) follows the same loop. This doc captures it so it's repeatable. The engineering standards
themselves are in [engineering-handbook.md](engineering-handbook.md); this is about *how we
collaborate with the agent*.

## Why this approach

LLM agents are most useful when they (a) start from durable project knowledge instead of re-deriving
it every session, (b) make their reasoning and decisions inspectable, and (c) ask the human the right
questions instead of guessing. This repo is structured to make all three the default.

## The three memory layers (what goes where)

| Layer | Lives in | Holds | Lifetime |
|-------|----------|-------|----------|
| **Project truth** | `docs/knowledge-base/` + `docs/backlog.md` + `docs/open-questions.md` | How the system works, the plan, decisions | Versioned with the code |
| **Session bootstrap** | the tool's entry file (e.g. `/CLAUDE.md`), a thin wrapper built from [agent-entry-template.md](agent-entry-template.md) | Rules + pointers, auto-loaded each session | Versioned with the code |
| **Cross-session memory** | the tool's own user-level memory (outside the repo) | Author profile, working preferences, handy references | Per-user, across projects |

Rule of thumb: **anything another contributor needs goes in the repo** (`docs/`), not only in
cross-session memory.

## Session playbook

1. **Orient** — read your tool's entry file (e.g. `CLAUDE.md`), then `docs/knowledge-base/README.md`,
   `backlog.md`, `open-questions.md`. Don't re-scan the whole codebase; trust + verify the KB.
2. **Pick one backlog item** (smallest valuable slice). Confirm scope.
3. **Clarify by asking** — when a decision is the author's, ask with **multiple-choice options**
   (recommended first). Record the answer in `open-questions.md` under Decisions.
4. **TDD** — red → green → refactor (see handbook §4). For user-facing work, add a BDD scenario.
5. **Update the KB** — change the relevant doc in the same change as the code.
6. **Commit (gated)** — propose **one** one-line, present-tense-verb message, get the author's
   confirmation, then commit. **Never push.** LF endings; whitespace separate from logic.
7. **Log decisions/assumptions** so the next session inherits them; record process slips in
   `docs/process-learnings.md`.

## Using sub-agents (parallel fan-out)

For broad reading/analysis, spin up parallel agents (e.g. one mapping architecture, one assessing
tests/build) and synthesize their reports — much faster than reading everything serially. Keep
*decisions* with the human; use agents for *gathering and drafting*. (This very knowledge base was
bootstrapped that way — see "How this was built" below.)

For **implementation**, we use a dedicated agent team — Planner (you+me, Opus) → Implementer
(Sonnet) → Falsifier (Sonnet) → Reviewer/Gate (Opus) — defined in `.claude/agents/` and described in
**[multi-agent-playbook.md](multi-agent-playbook.md)**. The author approves the commit plan and is
the only one who pushes.

## Context & token hygiene

Long sessions dominate cost — every turn re-sends the whole accumulated context, so a sprawling
session is paid for repeatedly. Keep the working context lean:

- **Search via sub-agents, not the main context.** Use `Explore` / sub-agents for broad reading;
  they return conclusions and keep large file dumps out of the main session. Read specific line
  ranges rather than whole large files.
- **`/clear` at task boundaries.** Start a fresh context between unrelated backlog items. The agent
  flags good `/clear` points; the author triggers it (the agent can't clear its own context).
- **Reserve the Workflow / fan-out tooling for occasional large parallel audits** (explicit opt-in) —
  never the interactive, human-gated commit loop (see [multi-agent-playbook.md](multi-agent-playbook.md)).
  Routine TDD runs lean: plan → implementer → falsifier → reviewer.
- **Terse by default** — bullets and the outcome first; expand on request.

## Permissions & autonomy (committed vs local)

The agent runs against a permission allow/deny list so it can work **autonomously and
token-efficiently** — no prompt on every command — inside guardrails that stop a wrong turn from doing
harm. Two files, two distinct purposes:

- **Committed `.claude/settings.json` — the shared, portable policy.** What *every* contributor of this
  repo should inherit. A rule belongs here only if it is **portable** (no machine/host paths, no
  personal scratch dirs), **project-relevant** (the build tool, git, this project's doc sources,
  standard read-only shell utilities), and something you'd hand a teammate. Keep it **small and stable**.
- **Local `.claude/settings.local.json` (gitignored) — per-machine / per-dev taste.** Allows *you*
  personally accept but won't impose on others — machine-specific absolute paths, or a broader tool
  you're comfortable with (one dev allows blanket `curl`, another doesn't). Each dev curates their own;
  nothing here is shared.

**Decision rule (don't let it drift):** portable + project-relevant + shareable → *committed*;
machine-specific or personal-taste → *local*. **Don't promote local → committed** to "tidy up" — it
forces one dev's taste on everyone and can leak host detail; and don't bury a genuinely shared,
portable rule down in local where teammates never get it.

**Keep it from sprawling.** The "always allow" button writes the *exact command string* to the local
file — with a `cd`, an inline `VAR=…`, an absolute scratch path or a one-off message baked in, that
rule never matches again, so the file silently fills with dead one-shot entries. Two habits prevent it:
when a *recurring* command isn't covered, deliberately add a **portable** rule to committed
`settings.json` instead of clicking "always allow"; and periodically prune the local file back to the
few rules you actually chose. (A *running* agent session keeps its approval list in memory and rewrites
the local file on every new approval, so an in-session prune can be clobbered — prune when the session
is idle, or edit the file yourself.)

**Token-efficient commands that still match.** Combined one-liners (`echo … && git status && grep …`)
are **fewer tool round-trips** and are auto-allowed **as long as every segment matches an allow rule** —
so prefer them over splitting into atomic calls. Only two things make a chain un-matchable and force a
prompt: a leading **`cd`** and an inline **`VAR=…`** assignment. Avoid those (use absolute paths /
`git -C <path>` / literal values) and chains run prompt-free. (Worktree-path specifics: handbook §7.)

**Autonomy within guardrails.** The aim is maximum useful autonomy at minimum ceremony *and* a hard
floor a mistake can't cross even with no bad intent. The `deny` list is that floor (here: `git push`,
`git reset --hard`, `git clean`, `rm -rf`, and their `-C` variants). Widen the allow list freely for
convenience; **never weaken the deny floor**, and route anything genuinely destructive or
outward-facing through the human.

## How to ask questions (the author's preference)

- Always offer **multiple-choice** options; the author will free-type only if none fit.
- Put the recommended option first and say so. Be picky — surface trade-offs and decisions the
  author may not have considered, rather than quietly defaulting.

## How this knowledge base was built (worked example)

The initial pass: read git history + the uncommitted diff (discovering it was 95% line-ending
churn over ~15 real files), pulled the Google Sheet via gviz and reverse-engineered its columns
(verifying the Gildenmehrwert formula against real rows), and ran two parallel sub-agents to map the
architecture and the test/build state. Findings were written to `docs/knowledge-base/`, a prioritized
backlog to `docs/backlog.md`, and four strategic questions were put to the author as multiple-choice
— whose answers are recorded in `open-questions.md`. That loop — **gather → persist → ask → decide →
build** — is the pattern to repeat.
