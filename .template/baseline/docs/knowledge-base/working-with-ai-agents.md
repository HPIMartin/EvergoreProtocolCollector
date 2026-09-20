# 09: Working with AI agents (the AI-assisted workflow)

This project develops **with an AI agent as a first-class collaborator**. The showcase tool is
Claude Code, but this workflow is tool-neutral: any capable AI coding agent (or a human) follows the
same loop. This doc captures it so it's repeatable. The engineering standards themselves are in
[engineering-handbook.md](engineering-handbook.md); this is about *how we collaborate with the agent*.

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

1. **Orient:** read your tool's entry file (e.g. `CLAUDE.md`), then `docs/knowledge-base/README.md`
   (the map), the backlog's "▶ Current status / next action" section and the decisions touching your
   task. Don't re-scan the whole codebase or read the big docs whole; trust + verify the KB
   (see "Context & token hygiene" below).
2. **Pick one backlog item** (smallest valuable slice). Confirm scope.
3. **Clarify by asking:** when a decision is the author's, ask with **multiple-choice options**
   (recommended first). Record the answer in `open-questions.md` under Decisions.
4. **TDD:** red → green → refactor (see handbook §4). For user-facing work, add a BDD scenario.
5. **Update the KB:** change the relevant doc in the same change as the code.
6. **Commit (gated):** propose **one** one-line, present-tense-verb message, get the author's
   confirmation, then commit. **Never push.** LF endings; whitespace separate from logic.
7. **Log decisions/assumptions** so the next session inherits them; record process slips in
   `docs/process-learnings.md`.

## Using sub-agents (parallel fan-out)

For broad reading/analysis, spin up parallel agents (e.g. one mapping architecture, one assessing
tests/build) and synthesize their reports, much faster than reading everything serially. Keep
*decisions* with the human; use agents for *gathering and drafting*. (See "How a knowledge base gets
bootstrapped" below for a worked example.)

For **implementation**, we use a dedicated agent team (Planner (you + the main session) → Implementer
→ Falsifier panel → Reviewer/Gate; the per-role models live in the `.claude/agents/` frontmatter and
the playbook's roles table) defined in `.claude/agents/` and described in
**[multi-agent-playbook.md](multi-agent-playbook.md)**. The author approves the commit plan and is
the only one who pushes.

## Context & token hygiene

Long sessions dominate cost: every token that enters the context is re-read on **every** subsequent
turn, and after any pause the whole context is re-written to the prompt cache. Context size × session
length is the product to minimize; keep the working context lean:

- **Section-scoped reads for the big docs.** The backlog, the decisions log and the handbook are
  thousands of tokens each. At session start read the KB README (the map), the backlog's
  **"▶ Current status / next action"** section and only the decisions/sections touching the task;
  locate them with search or line-scoped reads instead of pulling whole files. Read a file whole only
  when it is the object of the work.
- **Never re-read a file already in context** unless it changed on disk; the earlier read is still
  there, and each re-read injects the full file again.
- **Batch independent tool calls** into one turn (and prefer combined one-liners where the permission
  rules match, see below): every round trip re-reads the entire context, so avoidable calls and
  error-retries are pure waste.
- **Delegate exploration *and* implementation to sub-agents, not the main context.** Use explore-style
  sub-agents for broad reading and the implementer agent for code changes; they return conclusions
  and diffs, keeping large file dumps out of the main (planner) session.
- **`/clear` at task boundaries; prefer a fresh session per work package** over one long-lived
  mega-session. Start a fresh context between unrelated backlog items; a long-lived large context
  also pays a full cache re-write after every pause longer than the cache TTL (5 min on API billing;
  1 h for Claude Code on a subscription). The agent flags good `/clear` points; the author triggers
  it (the agent can't clear its own context).
- **Load heavyweight skills / one-off imports in a throwaway session,** not mid-feature: anything
  injected stays in the context for the rest of the session.
- **Reserve the Workflow / fan-out tooling for occasional large parallel audits** (explicit opt-in),
  never the interactive, human-gated commit loop (see [multi-agent-playbook.md](multi-agent-playbook.md)).
  Routine TDD runs lean: plan → implementer → falsifier panel → reviewer.
- **Terse by default:** bullets and the outcome first; expand on request.
- **Keep the shared docs themselves lean** (KB-current): this hygiene only works if the per-session
  entry docs stay small; condense, don't accrete.

## Choosing the session model

The main session is the planner's brain, and its whole context is re-read on every turn, so the
session model dominates token spend more than any subagent choice. Pick it by the session's job:
planning, architecture and review/audit sessions run the strongest model tier the current plan
offers; routine TDD/implementation sessions run a smaller tier, and the pipeline's gates still check
the work. When the strongest tier is unavailable, fall back one tier rather than postponing the
session. Subagent tiers are pinned per role in the `.claude/agents/` frontmatter; one-off gate
escalation follows the playbook's escalation rule.

## Permissions & autonomy (committed vs local)

The agent runs against a permission allow/deny list so it can work **autonomously and
token-efficiently** (no prompt on every command) inside guardrails that stop a wrong turn from doing
harm. Two files, two distinct purposes:

- **Committed [`.claude/settings.json`](../../.claude/settings.json), the shared, portable policy**
  (the template ships one as a starting point). What *every* contributor of this repo should inherit. A rule belongs here only if it is **portable** (no machine/host paths, no
  personal scratch dirs), **project-relevant** (the build tool, git, this project's doc sources,
  standard read-only shell utilities), and something you'd hand a teammate. Keep it **small and stable**.
- **Local `.claude/settings.local.json` (gitignored), per-machine / per-dev taste.** Allows *you*
  personally accept but won't impose on others: machine-specific absolute paths, or a broader tool
  you're comfortable with (one dev allows blanket `curl`, another doesn't). Each dev curates their own;
  nothing here is shared.

**Decision rule (don't let it drift):** portable + project-relevant + shareable → *committed*;
machine-specific or personal-taste → *local*. **Never promote local → committed** just to "tidy up":
it forces one dev's taste on everyone and can leak host detail. Promotion is a deliberate,
author-confirmed act. And don't bury a genuinely shared, portable rule down in local where teammates
never get it.

**One reviewed diff per config change.** Settings files and other policy-bearing configuration
(permission lists, hooks, CI config) are never edited incrementally in a trickle of small changes.
Collect all agreed changes first, apply them as one batch, and present the author a **single complete
diff** for confirmation (or the prepared target content *before* applying, if the change was not
pre-approved in detail). Any later correction is again one complete reviewed diff. Many small edits
leave the author no reviewable unit, and re-verifying a permission or policy file after the fact
costs more than reviewing one diff up front.

**Keep it from sprawling.** The "always allow" button writes the *exact command string* to the local
file. With a `cd`, an inline `VAR=…`, an absolute scratch path or a one-off message baked in, that
rule never matches again, so the file silently fills with dead one-shot entries. Two habits prevent it:
when a *recurring* command isn't covered, deliberately add a **portable** rule to committed
`settings.json` instead of clicking "always allow"; and periodically prune the local file back to the
few rules you actually chose. (A *running* agent session keeps its approval list in memory and rewrites
the local file on every new approval, so an in-session prune can be clobbered: prune when the session
is idle, or edit the file yourself.)

**Token-efficient commands that still match.** Combined one-liners (`echo … && git status && grep …`)
are **fewer tool round-trips** and are auto-allowed **as long as every segment matches an allow rule**,
so prefer them over splitting into atomic calls. An inline **`VAR=…`** assignment still makes a chain
un-matchable, so use literal values. A leading **`cd`** auto-allows only in its project-anchored
absolute form (the project root's absolute path or a path below it), and deny rules block `cd`
arguments containing `..`, `$`, `` ` `` or `~`, so the working directory cannot leave the project
silently; bare `cd`, relative and quoted forms still prompt. Prefer `git -C <path>` / absolute paths
anyway; `cd` is the fallback when a tool must run from a subdirectory (e.g. running {{BUILD_CMD}}
inside a worktree). (Worktree-path specifics: handbook §7.)

**Autonomy within guardrails.** The aim is maximum useful autonomy at minimum ceremony plus a deny
floor under it. The template ships this policy as a starting point in the committed
[`.claude/settings.json`](../../.claude/settings.json). Its `deny` list blocks the named destructive
commands outright (`git push`, `git reset` in all forms, `git clean`, `git branch -D`, `rm -rf`,
their `-C` variants, the `cd` escape guards, and reads of secret/credential files, shipped as generic
patterns such as `.env`). Between allow and deny sits a small **`ask` tier** for legitimate but risky commands
(remote-API calls that can mutate the remote, `git restore` that can discard working-tree state):
they always prompt and must never drift into a blanket allow. It is a guardrail against accidents,
not a sandbox: broad interpreter allows (`python3`, `node`, `find`, `sed -i`, shell redirects) could
technically reach the same effects and are trusted by design (trusted contributors, accident threat
model). Widen the allow list freely for convenience; **never weaken the deny floor**, and route
anything genuinely destructive or outward-facing through the human.

> **ADAPT:** State your threat model. The guardrails above assume trusted contributors and defend
> against accidents, not malice; a team with untrusted contributors or CI-run agents needs a real
> sandbox, not just a deny list. Then adapt the shipped
> [`.claude/settings.json`](../../.claude/settings.json): replace the generic secret-file deny
> patterns with your project's actual secret files, substitute the {{BUILD_CMD}}, {{TEST_CMD}} and
> {{PROJECT_NAME}} placeholders, adjust the project-anchored `cd` allow rules if your project root is
> not `/workspaces/{{PROJECT_NAME}}`, and add your stack's tools and doc domains to the allow list.

## How to ask questions (the author's preference)

- Always offer **multiple-choice** options; the author will free-type only if none fit.
- Put the recommended option first and say so. Be picky: surface trade-offs and decisions the
  author may not have considered, rather than quietly defaulting.

## How a knowledge base gets bootstrapped (example)

From the project this template was extracted from (example): the initial pass read git history plus
the uncommitted diff (discovering it was 95% line-ending churn over ~15 real files), pulled the
external data source the project was to replace and reverse-engineered its structure (verifying a
key formula against real rows), and ran two parallel sub-agents to map the architecture and the
test/build state. Findings were written to `docs/knowledge-base/`, a prioritized backlog to
`docs/backlog.md`, and four strategic questions were put to the author as multiple-choice, whose
answers were recorded in `open-questions.md`. That loop, **gather → persist → ask → decide →
build**, is the pattern to repeat when adopting this template on an existing codebase.
