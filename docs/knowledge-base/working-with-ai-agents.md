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

## How to ask questions (the author's preference)

- Always offer **multiple-choice** options; the author will free-type only if none fit.
- Put the recommended option first and say so. Be picky — surface trade-offs and decisions the
  author may not have considered, rather than quietly defaulting.

## Environment gotchas (save yourself time)

- **Reading the Google Sheet:** use the gviz CSV endpoint
  `…/gviz/tq?tqx=out:csv&gid=<GID>` (the plain `/export?format=csv` redirect expires before it can be followed).
- Tool-specific quirks (e.g. a host's Bash-stdout behaviour) belong in that tool's own entry file, not here.

## How this knowledge base was built (worked example)

The initial pass: read git history + the uncommitted diff (discovering it was 95% line-ending
churn over ~15 real files), pulled the Google Sheet via gviz and reverse-engineered its columns
(verifying the Gildenmehrwert formula against real rows), and ran two parallel sub-agents to map the
architecture and the test/build state. Findings were written to `docs/knowledge-base/`, a prioritized
backlog to `docs/backlog.md`, and four strategic questions were put to the author as multiple-choice
— whose answers are recorded in `open-questions.md`. That loop — **gather → persist → ask → decide →
build** — is the pattern to repeat.
