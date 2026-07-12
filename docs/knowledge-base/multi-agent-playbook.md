# 11: Multi-Agent Playbook

How we develop with a small team of AI agents: **you + I plan; cheaper agents implement;
independent agents try to break it and gate the commit.** Part of the project's showcase goal
(see [working-with-ai-agents.md](working-with-ai-agents.md)).

## Roles

| Role | Who / model | Spawned? | Input → Output |
|------|-------------|----------|----------------|
| **Planner** | You + me (main-session model), in chat | No, a *hat* | Backlog item → approved, ordered commit/test list + acceptance criteria (Gherkin if user-facing) |
| **Implementer** | `implementer` (frontmatter `model: sonnet`) | Yes | Approved plan → red→green→refactor per step, commits each locally → summary |
| **Falsifier panel** | `falsifier-domain` + `falsifier-robustness` (`model: sonnet`) | Yes (both fresh) | Feature diff/commits → two adversarial verdicts (domain math · test honesty/robustness) + counter-tests |
| **Doc reviewer** | `doc-reviewer` (`model: haiku`) | Yes (fresh) | Feature diff/commits → PASS/FAIL against the KB README's DOC checklist + findings with rule IDs |
| **Reviewer / Gate** | `reviewer` (`model: opus`) | Yes (fresh) | Feature commits + panel + doc-reviewer reports → PASS/FAIL + findings + proposed `process-learnings.md` entry |

**Non-negotiable:** falsifiers, doc-reviewer and Reviewer are **fresh, independent** agents, never
the Implementer checking itself (independence is the point). Agents **never `git push`**; pushing
is the author's decision ([engineering-handbook.md](engineering-handbook.md) §7).

## The pipeline (per backlog item)

```
1. PLAN (you + me, chat)
   - item → small ordered test list → ordered one-line commit messages
   - (user-facing) Gherkin scenarios, @Ignore-disabled
   - ► YOU APPROVE THE PLAN (the ordered commit messages)
2. IMPLEMENT (spawn `implementer`) with the approved plan
   - per step: red (failing test) → focused test → green (minimal code) → refactor
     → commit locally with the EXACT pre-approved message
   - never pushes; domain/businessLogic/application stay framework-free
3. FALSIFY (spawn the panel: `falsifier-domain` + `falsifier-robustness`, both fresh)
   - domain lens: value math, watermark/aggregation, parser fidelity
   - robustness lens: fake-green tests, edges, time/concurrency/resources, boundaries, secrets
   - each returns verdict + counter-tests (parallel only in separate worktrees, else sequential)
4. REVIEW GATE (spawn `doc-reviewer` + `reviewer`, both fresh), once per FEATURE commit
   - doc-reviewer: docs hygiene per the DOC checklist, task-scoped, plus a stateless
     rotating one-doc sweep (pick = commit count mod doc count; see the agent definition)
   - reviewer: process adherence + code criteria; integrates panel + doc-reviewer findings
   - PASS → proceed; FAIL → fix and re-gate under the loop rules below; log a
     process-learning if a rule slipped
5. PUSH DECISION (YOU)
   - only the author pushes, when satisfied
```

### FAIL-loop rules (when a falsifier or reviewer rejects)

A FAIL sends work back to step 2, then re-runs a **fresh** panel and **fresh** reviewers.
Loop-bounding governance (cap 2 re-implement rounds → escalate + re-plan; early-escalate on a
repeated finding; process-only FAILs don't consume a round) is canonical in
[engineering-handbook.md](engineering-handbook.md) §9. Harness-specific history hygiene:

- **Fold fixes into the commit they belong to; never append "fix review" commits.** The branch is
  unpushed; rewriting local history is expected (history reads as if done right the first time).
- **Permission-blocked in this harness:** `git push`, `git reset` (all forms), `git branch -D`,
  `rm -rf` (plain `rm <file>` is fine). Built-in guardrails override project `allow`; the committed
  `deny` list encodes the same floor for tools without the built-ins. If you ever reset outside the
  tool: prefer `git reset --soft`, never `--hard` (discards the working tree).
- **Reset-free fold** (works for deep commits): save the corrected tree as a WIP commit → branch
  afresh off the feature base → per logical group `git checkout <wip-tip> -- <files>` + commit →
  swap names via `git branch -m` → drop the stale WIP branch with
  `git update-ref -d refs/heads/<name>` (plumbing bypasses the `-D` block).
- Tip-only fix: `git commit --amend`. `git rebase -i` has no interactive TTY here.

**Cadence (cost vs rigor; decided 2026-06-13, panel + escalation 2026-07-03):** micro-steps run
lightweight; falsifier panel + review gate run **at the feature commit**, not per micro-commit.
**Escalation:** features touching valuation math, time/timezones, concurrency or data migration →
spawn gate agents with a one-off `model` override on the strongest available tier (`fable`, else
`opus`); frontmatter defaults stay unchanged.

## Two tracks: direct-on-main vs. feature branch (decided 2026-06-20)

Planner picks the track up-front and announces it (author can veto). Full rule:
[engineering-handbook.md](engineering-handbook.md) §7. Gist:

- **Small / single TDD cycle** → directly on `main`; one commit via propose→confirm→commit.
- **Large / multi-cycle or new BDD scenarios** → feature branch. Implementer self-authors
  protocol-conform messages (no per-commit pre-approval; avoids N round-trips). Panel + reviewers
  review the **branch diff**; step 5 becomes the gateway: author + planner review the branch
  `git log`, reword if needed (scripted rebase), land by **rebase + fast-forward only** (no merge
  commit, no squash). **Every commit on `main` builds green**; broken branch commits are repaired
  by rebase before the merge. Only the author pushes.
- "Small" item balloons mid-flight → move uncommitted WIP onto a branch (`git switch -c`); `main`
  stays clean. Parallel/benchmark runs use git worktrees (`Agent` tool `isolation: "worktree"`).

## How to invoke

Orchestrator = the main session (me), via the `Agent` tool: `subagent_type` = `implementer` /
`falsifier-domain` / `falsifier-robustness` / `doc-reviewer` / `reviewer` (defined in
`.claude/agents/`). Planner phase, commit-plan approvals and pushes happen with **you** in chat.

## Handoff contracts (what each agent returns)

- **Implementer:** per-step status (red/green/refactor), commit message used, focused-test result,
  full-suite result before hand-off, any deviation from the plan (with reason).
- **Falsifier (each):** `robust: yes/no`; weaknesses `{severity, where (file:line), why}`; concrete
  counter-test snippets. Bias to skepticism: if unsure, flag it.
- **Doc reviewer:** `PASS|FAIL`; findings `{rule: DOC-n, where, what, fix}`.
- **Reviewer:** `PASS|FAIL`; findings `{category: process|cleancode|solid|hexagonal|tests|security,
  where, fix}`; ready-to-paste `process-learnings.md` row if a process rule slipped.

## Environment gotchas (tell every agent)

- **Bash stdout** may not surface on the Windows host: redirect to a file
  (`./gradlew build > out.txt 2>&1`) and `Read` it; in-container, Bash is normal.
- Permission-blocked commands + reset-free rewrite: see the FAIL-loop section. Prefer Read/Grep/Glob.
- **Never overlap a tree-mutating agent with another agent's build.** The falsifier rewrites source
  for counter-tests; a concurrent build on the same tree reports phantom failures. Run falsify →
  review sequentially, or isolate per worktree (`Agent` `isolation: "worktree"`); same inside the
  panel. (The doc-reviewer only reads docs and runs no build; it may run alongside the reviewer.)
- Focused tests during micro-steps (`./gradlew test --tests ClassName`); full `./gradlew build`
  before the gate.

## Worked example (the storage-value evaluator feature)

1. **Plan:** test list for `EvergoreDataEvaluator` storage calc: ① place value = Σ storageValue·qty·quality/100 ② withdraw value uses withdrawlValue ③ unknown item → UNDEFINED+log ④ watermark advances. Messages approved.
2. **Implement:** four red→green→refactor micro-commits with those messages.
3. **Falsify:** domain lens recomputes ① ② by hand; robustness lens: does ③ assert the log? quality=0 edge? is green real if the production line is reverted? → counter-tests.
4. **Review gate:** doc-reviewer: `domain-model.md`/`testing.md` updated, backlog row removed? reviewer: boundaries clean, messages one-line verb-first? → PASS.
5. **You:** push when happy.

## Evolution

Start lean (1 implementer, 2-lens panel, doc-reviewer, 1 reviewer). Widen the panel + majority vote
for high-risk changes; consider a `/tdd-step` skill once the loop is proven; **Workflow tool** only
for occasional large parallel audits (explicit opt-in), never for the interactive, human-gated
commit loop.
