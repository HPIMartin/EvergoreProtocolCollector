# 11 — Multi-Agent Playbook

How we develop this project with a small team of AI agents: **you + I (Opus) plan; cheaper agents
implement; independent agents try to break it and gate the commit.** This is part of the project's
"showcase how to work with Claude" goal (see [working-with-claude.md](working-with-claude.md)).

## Roles

| Role | Who / model | Spawned? | Input → Output |
|------|-------------|----------|----------------|
| **Planner** | You + me (Opus), in chat | No — it's a *hat* | Backlog item → an **approved, ordered commit/test list** + acceptance criteria (Gherkin if user-facing) |
| **Implementer** | `implementer` subagent (**Sonnet**) | Yes | Approved plan → executes red→green→refactor per step, **commits each locally** with the pre-approved message → summary |
| **Falsifier** | `falsifier` subagent (**Sonnet**) | Yes (fresh) | The feature diff/commits → adversarial verdict + concrete counter-tests / weaknesses |
| **Reviewer / Gate** | `reviewer` subagent (**Opus**) | Yes (fresh) | The feature commits + falsifier report → PASS/FAIL + findings + proposed `process-learnings.md` entry |

**Non-negotiable:** Falsifier and Reviewer are **fresh, independent** agents — never the Implementer
checking itself (independence is the whole point). Agents **never `git push`** — pushing is the
author's decision (see [engineering-handbook.md](engineering-handbook.md) §7).

## The pipeline (per backlog item)

```
1. PLAN (you + me, chat)
   - turn the item into a small ordered test list → ordered one-line commit messages
   - (user-facing) write Gherkin scenarios, @Ignore-disabled
   - ► YOU APPROVE THE PLAN (the ordered commit messages)
2. IMPLEMENT (spawn `implementer`, Sonnet) — with the approved plan
   - per step: red (write failing test) → run focused test → green (minimal code) → refactor
     → commit locally with the EXACT pre-approved message
   - never pushes; domain/businessLogic stay framework-free
3. FALSIFY (spawn `falsifier`, Sonnet, fresh)
   - tries to break it: edge cases, tautological/fake-green tests, boundary violations, secrets
   - returns verdict + proposed counter-tests
4. REVIEW GATE (spawn `reviewer`, Opus, fresh) — once per FEATURE commit
   - process adherence (real red→green→refactor? whitespace separate? KB updated? commit-msg rules?)
   - code criteria (clean code, SOLID, hexagonal); integrates falsifier findings
   - PASS → proceed;  FAIL → fix and re-gate under the loop rules below; logs a process-learning if a rule slipped
5. PUSH DECISION (YOU)
   - only the author pushes, when satisfied
```

### FAIL-loop rules (when the falsifier or reviewer rejects)

A FAIL sends the work back to step 2, then re-runs a **fresh** falsifier and a **fresh** reviewer.
Two rules keep that loop bounded and the history clean:

- **Cap at 2 re-implement rounds, then re-plan.** A feature gets at most two FAIL→fix rounds
  (three implement attempts total). On hitting the cap, **stop the agent loop and escalate to the
  author** with the falsifier+reviewer findings — repeated FAILs usually mean the *plan/spec* is
  wrong, not the code, so we go back to step 1 (PLAN) rather than re-implement again. **Early
  escalate** if two consecutive rounds raise the *same* finding (same signal — re-plan now).
  **Process-only FAILs** (whitespace not separated, KB not updated, commit-message format) are cheap
  mechanical fixes and **do not consume a round**.
- **Fold fixes into the commit they belong to — never append "fix review" commits.** The branch is
  unpushed, so rewriting local history is safe and expected (clean up before the author pushes), so the
  history reads as if the work were done right the first time. **In this harness `git reset` (all forms,
  including `--soft`/`--mixed`) and `git branch -D` are permission-blocked** by Claude Code's built-in
  destructive-git guardrails (alongside `git push`); they override project `allow` and can't be
  allowlisted via `settings.json` at all, so the reset/rebase recipes don't run. (Plain `rm <file>` is
  **fine** — only `rm -rf` is denied.) **If you ever do run a reset outside the tool — prefer the
  least-destructive form, `git reset --soft`; never `--hard`** (it discards the working tree). Reset-free method that
  folds a fix into any commit, even a deep one: save the corrected tree on a WIP commit, branch off the
  feature base afresh, then for each logical group `git checkout <wip-tip> -- <files>` to stage it and
  commit, and swap names with `git branch -m`. Drop the stale WIP branch with
  `git update-ref -d refs/heads/<name>` (the `-D` block doesn't reach plumbing). A tip-only fix still
  takes `git commit --amend`; `git rebase -i` has no interactive TTY here.

**Cadence (cost vs rigor, decided 2026-06-13):** micro-steps run lightweight; the **Opus reviewer
gates at the feature commit**, not every micro-commit. A **single** Sonnet falsifier runs at the
feature end (scale to a 2–3 panel later if we want more rigor).

## Two tracks: direct-on-main vs. feature branch (decided 2026-06-20)

The planner picks the track **up-front** and announces it (the author can veto). See
[engineering-handbook.md](engineering-handbook.md) §7 for the full rule; the operational gist:

- **Small / single TDD cycle** → **directly on `main`**, one commit via the propose→confirm→commit
  protocol. (This is the classic loop above.)
- **Large / multi-cycle or new BDD scenarios** → a **feature branch**. The implementer commits each
  red→green→refactor step **itself** with protocol-conform messages — **no per-commit pre-approval on
  the branch** (that's the point: real TDD cadence without N round-trips). Falsifier + reviewer review
  the **branch diff**; then **step 5 becomes the gateway**: the **author + planner review the branch's
  `git log` together**, reword messages if needed (scripted rebase), and the branch lands by **rebase +
  fast-forward only — no merge commit, no squash**. **Every commit on `main` builds green**, so a
  broken branch commit is repaired by rebase before the merge. Only the author pushes.

If a "small" item balloons mid-flight, the planner moves the *uncommitted* WIP onto a branch
(`git switch -c`) before it grows — `main` stays clean. **Parallel/benchmark runs** use git worktrees
(`Agent` tool `isolation: "worktree"`).

## How to invoke

The **orchestrator is the main Opus session** (me). I drive the pipeline with the `Agent` tool,
`subagent_type` = `implementer` / `falsifier` / `reviewer` (defined in `.claude/agents/`). The
Planner phase and every commit-plan approval and push happen with **you** in the main chat.

## Handoff contracts (what each agent returns)

- **Implementer:** per-step status (red/green/refactor done), the commit message used, focused-test
  result, full-suite result before hand-off, and any deviation from the approved plan (with reason).
- **Falsifier:** `robust: yes/no`; a list of weaknesses `{severity, where (file:line), why}`; concrete
  counter-test snippets. Bias to skepticism — if unsure, flag it.
- **Reviewer:** `PASS|FAIL`; findings `{category: process|cleancode|solid|hexagonal|tests|security,
  where, fix}`; a ready-to-paste `process-learnings.md` row if any process rule was missed.

## Environment gotchas (tell every agent)

- **Bash stdout** isn't surfaced on this machine: redirect to a file (`./gradlew build > out.txt 2>&1`)
  and `Read` it. `git reset` (all forms) and `git branch -D` are permission-blocked (see §FAIL-loop for the
  reset-free history rewrite + `git update-ref -d` to drop a scratch branch); plain `rm` is fine, only
  `rm -rf` is denied. Prefer Read/Grep/Glob.
- **Never overlap a tree-mutating agent with another agent's build.** The falsifier rewrites source to
  run its counter-tests; a reviewer (or any build) hitting the same working tree at the same time builds
  the mutated tree and reports phantom failures. Run falsify → review **sequentially**, or isolate each
  in its own worktree (`Agent` `isolation: "worktree"`).
- Run **focused** tests during micro-steps (`./gradlew test --tests ClassName`), full `./gradlew build` before the gate.

## Worked example (the storage feature, backlog B1 → A2)

1. **Plan:** test list for `EvergoreDataEvaluator` storage calc — e.g. ① place value = Σ storageValue·qty·quality/100 ② withdraw value uses withdrawlValue ③ unknown item → UNDEFINED+log ④ watermark advances. Commit messages approved.
2. **Implement (Sonnet):** four red→green→refactor micro-commits with those messages.
3. **Falsify (Sonnet):** "does ③ assert the log? does quality=0 edge work? is green real if I revert the production line?" → counter-tests.
4. **Review (Opus):** boundaries clean? KB `domain-model.md`/`testing.md` updated? messages one-line verb-first? → PASS.
5. **You:** push when happy.

## Evolution

Start lean (1 implementer, 1 falsifier, 1 reviewer). Add a falsifier **panel** + majority vote for
high-risk changes; consider a `/tdd-step` slash command in `.claude/commands/` once the loop is proven;
use the **Workflow tool** only for occasional large parallel audits (explicit opt-in), never for the
interactive, human-gated commit loop.
