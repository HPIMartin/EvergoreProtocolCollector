# 11: Multi-Agent Playbook

How we develop with a small team of AI agents: **you + I plan; cheaper agents implement;
independent agents try to break it and gate the commit.** Part of the project's showcase goal
(see [working-with-ai-agents.md](working-with-ai-agents.md)).

## Roles

| Role | Who / model | Spawned? | Input → Output |
|------|-------------|----------|----------------|
| **Planner** | You + me (main-session model), in chat | No, a *hat* | Backlog item → the feature's **author-confirmed `.feature`** (handbook §5) + approved, ordered commit/test list |
| **Scenario falsifier** | `falsifier-scenario` (`model: sonnet`) | Yes (fresh) | A draft `.feature`, before implementation → skeptical verdict on it as a specification + concrete Gherkin rewrites |
| **Implementer** | `implementer` (frontmatter `model: sonnet`) | Yes | Approved plan → red→green→refactor per step, commits each locally, arms the scenarios → summary |
| **Falsifier panel** | `falsifier-domain` · `falsifier-robustness` · `falsifier-frontend` (each `model: sonnet`); spawned per touched surface (see cadence) | Yes (fresh) | Feature diff/commits → adversarial verdicts (domain math · test honesty/robustness · SPA behavior) + counter-tests |
| **Doc reviewer** | `doc-reviewer` (`model: haiku`) | Yes (fresh) | Feature diff/commits → PASS/FAIL against the KB README's DOC checklist + findings with rule IDs |
| **Reviewer / Gate** | `reviewer` (`model: opus`) | Yes (fresh, per gate) | Scenario gate: the draft `.feature` + the scenario falsifier's report → PASS/FAIL. Feature gate: feature commits + panel + doc-reviewer reports → PASS/FAIL + findings + proposed `process-learnings.md` entry |

**Non-negotiable:** falsifiers, doc-reviewer and Reviewer are **fresh, independent** agents, never
the Implementer checking itself (independence is the point). Agents **never `git push`**; pushing
is the author's decision ([engineering-handbook.md](engineering-handbook.md) §7).

## The pipeline (per backlog item)

```
0. SPECIFY (you + me, chat), mandatory for any observable behavior (handbook §5)
   - draft the feature's `.feature` in product language, checked against the completeness list
   - scenario gate: spawn `falsifier-scenario`, then `reviewer` in scenario mode; fix, re-gate
   - ► AUTHOR GATE 1: YOU CONFIRM THE SCENARIOS AND THEIR COMPLETENESS (a gate, not a notification)
   - commit the confirmed scenarios tagged @wip (the default run excludes them: the build stays green)
   - the only way past this step is an explicitly claimed exemption: a pure refactoring,
     a [doc]/process commit, or build/infra work
1. PLAN (you + me, chat)
   - the runner's dry run lists the undefined steps: that is the glue work; then a small ordered
     test list → ordered one-line commit messages, step definitions and production code alike
   - slice the commits BEFORE the first edit (handbook §7): each green and reviewable alone
   - ► YOU APPROVE THE PLAN (the ordered commit messages)
2. IMPLEMENT (spawn `implementer`) with the approved plan
   - per step: red (failing test) → ./verify focus → green (minimal code) → refactor
     → commit locally with the EXACT pre-approved message; the scenarios run via ./verify bdd
   - ARM: when every scenario of the feature passes, one commit removes @wip, the last TDD step;
     from here ./verify all runs the scenarios on every commit and at every gate
   - REFACTOR the feature under the armed net: no behavior change, each step its own green commit
   - the feature is done when the armed scenarios pass, not when the steps are ticked off
   - never pushes; domain/businessLogic/application stay framework-free
3. FALSIFY (spawn the lenses the change can break, each fresh, each in its own detached
   worktree at the tip under review; see cadence)
   - domain lens: value math, watermark/aggregation, parser fidelity
   - robustness lens: fake-green tests, edges, time/concurrency/resources, boundaries, secrets
   - frontend lens: fake-green component tests, user paths, async determinism, layer boundaries,
     API contract, the one real-artifact scenario
   - each returns verdict + counter-tests, and ends with the diff of the production tree against
     HEAD in its worktree (a left-behind probe is its own finding)
4. REVIEW GATE (spawn `doc-reviewer` + `reviewer`, both fresh), once per FEATURE commit
   - doc-reviewer: docs hygiene per the DOC checklist, task-scoped, plus a stateless
     rotating one-doc sweep (pick = commit count mod doc count; see the agent definition)
   - reviewer: process adherence (scenario gate and author gate 1 honored? real
     red→green→refactor with red evidence? arming commit the last TDD step, refactor commits
     behavior-free? whitespace separate? commit-msg rules?) + code criteria; integrates the
     panel's + doc-reviewer's findings, all of which are in its brief
   - PASS → proceed; FAIL → fix and re-gate under the loop rules below; log a
     process-learning if a rule slipped
5. ► AUTHOR GATE 2 (handbook §7 gateway): rebase onto current main, run ./verify all on the
   rebased tip, present the reviewable statement (worktree identity + compare range), YOU review
   the rebased tip; on your go `git merge --ff-only`, then the worktree and branch are cleaned up
   - a re-rebase after your review: clean and green → ff without a second review; a conflict
     resolution → panel and your review again
6. PUSH DECISION (YOU)
   - only the author pushes, when satisfied
```

Step 0 is **suspended in this project** until the author picks the scenario catch-up from the
backlog (handbook §5, "Status in this project"): until then every plan claims the §5 exemption
explicitly and the pipeline starts at step 1.

### FAIL-loop rules (when a falsifier or reviewer rejects)

A FAIL sends work back to step 2, then re-runs a **fresh** panel and **fresh** reviewers.
Loop-bounding governance (cap 2 re-implement rounds → escalate + re-plan; early-escalate on a
repeated finding; process-only FAILs don't consume a round) is canonical in
[engineering-handbook.md](engineering-handbook.md) §9. Harness-specific history hygiene:

- **Fold fixes into the commit they belong to; never append "fix review" commits.** The branch is
  unpushed; rewriting local history is expected (history reads as if done right the first time).
- **Answer a gate finding by amending the commit that owns it** (scripted `GIT_SEQUENCE_EDITOR`
  rebase-edit + `--amend`, no `fixup!`), and fold added coverage into the commit whose behavior it
  pins. **Fold at the moment of fixing, before the next build**, never at the end of the round: the
  fold is the act of fixing, not a step after it. The gate reads the message list before the diff,
  so a "gate round N fixes" commit is a FAIL on sight; a proposed message that names one hunk of a
  multi-hunk commit, or an "and" joining two clauses, is the signal that the fold was skipped or the
  commit must split.
- **Apply a panel finding to the whole diff, not to the file it was raised against.** When a finding
  is a *rule*, re-grep every file the strand touches for that pattern before reporting it resolved.
- **Permission-blocked in this harness:** `git push`, `git reset` (all forms), `git clean`,
  `git branch -D`, `rm` (**all** forms: the agent's own cleanup is git-native, `git worktree
  remove`/`prune` and `git branch -d`, run autonomously; anything else is handed to the author or
  removed by a committed build task such as `./gradlew clearProbes`, handbook §7). Built-in
  guardrails override project `allow`; the committed `deny` list encodes the same floor for tools
  without the built-ins. If you ever reset outside the tool: prefer `git reset --soft`, never
  `--hard` (discards the working tree).
- **Reset-free fold** (works for deep commits): save the corrected tree as a WIP commit → branch
  afresh off the feature base → per logical group `git checkout <wip-tip> -- <files>` + commit →
  swap names via `git branch -m` → drop the stale WIP branch with
  `git update-ref -d refs/heads/<name>` (plumbing bypasses the `-D` block).
- Tip-only fix: `git commit --amend`. `git rebase -i` has no interactive TTY here.

**Cadence (cost vs rigor; decided 2026-06-13, panel + escalation 2026-07-03, per-surface lenses +
doc fast lane 2026-07-17):** micro-steps run lightweight; falsifier panel + review gate run **at
the feature commit**, not per micro-commit.

- **Spawn only the lenses the change can break:** backend-only → `falsifier-domain` +
  `falsifier-robustness`; frontend-only → `falsifier-frontend` + `falsifier-robustness`; full-stack
  → all three. Spawning a lens that cannot see the change buys a confident PASS that means nothing.
- **One detached worktree per lens** (`git worktree add <path> <sha> --detach`) at the tip under
  review, named in the brief, so two lenses cannot collide with each other or with the planner.
  Every lens that writes a probe or a mutation ends its report with the diff of the production tree
  against `HEAD` in its worktree, so a left-behind change shows up in its own report rather than in
  the next build.
- **Every prior verdict is an input to the next gate agent**, not a message to the author alone: the
  falsifiers' and the doc-reviewer's reports go into the reviewer's brief verbatim, and a lens whose
  tip is no longer the one under review is re-run, never quoted.
- **Pure documentation/process changes (`[doc]` scope) skip the falsifier panel:** the
  `doc-reviewer` is the gate; add the `reviewer` only when process adherence is genuinely in doubt.
- **Escalation:** features touching valuation math, time/timezones, concurrency, data migration or
  the auth/security surface (filters, token handling, what is publicly exposed) → spawn gate agents
  with a one-off `model` override on the strongest available tier (`fable`, else `opus`);
  frontmatter defaults stay unchanged.

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

### Gateway hand-off and cleanup (the agent's side)

- **State the full worktree identity at every review-gateway hand-off:** the branch name **and** the
  absolute worktree path, plus the compare range `<tip>..<base>` on its own line, newest first and
  nothing else in it ([working-with-ai-agents.md](working-with-ai-agents.md)). The author opens that
  path in the editor and pastes the range into GitLens; a hand-off missing any of the three stalls
  the gateway.
- **Rebase autonomously, merge conditionally:** rebase a feature strand onto the current `main`
  without asking first, and run the full `./verify all` on the rebased tip every time (handbook
  §7): a clean rebase is not a green one until the build says so. If the strand was already
  author-reviewed **and** the rebase ran clean (no conflicts, no judgment-call resolutions) **and**
  that build is green, fast-forward-merge it without a further review round-trip. When the rebase
  hit a conflict: resolve it, run the full build, **re-run the falsifier panel and the reviewer on
  the resolved tip**, then present exactly what was resolved plus the compare range and wait for
  the author's go. Conflict resolutions are the one place new, unreviewed content can appear, so
  both reviews stay exactly there. Pushing always stays with the author.
- **Clean up merged strands immediately, without asking:** once a strand is fast-forward-merged into
  `main`, `git worktree remove <path>` and `git branch -d <branch>`, then `git worktree prune`. This
  is the agent's own scaffolding and the agent owns it (handbook §7); the commits live on in `main`,
  the branch label and worktree are pure redundancy, and the reflog still holds deleted tips for a
  while. The safety is in the command, not in judgment: `git branch -d` refuses a branch that is not
  fully merged, and `git branch -D` stays denied. Never reach for `rm` here.
- **Sweep the stale ones periodically.** `git worktree list` and `git branch --merged main` are the
  inventory; a worktree whose branch is merged, or whose session is long gone, is removed the same
  way. A worktree that is *not* merged is reported to the author, never removed.
- **Landing moves a branch that a worktree holds, so land inside that worktree** (`git merge
  --ff-only`), not by moving the ref with plumbing: a ref moved under a checkout leaves the index
  and files on the old commit, the strand reads as if it had reverted itself, and `git log` cannot
  see it. When plumbing is genuinely unavoidable, run `git status` in the receiving worktree
  afterwards and read it; never report a landing without it.

## How to invoke

Orchestrator = the main session (me), via the `Agent` tool: `subagent_type` = `implementer` /
`falsifier-scenario` / `falsifier-domain` / `falsifier-robustness` / `falsifier-frontend` /
`doc-reviewer` / `reviewer` (defined in `.claude/agents/`). Planner phase, commit-plan approvals and
pushes happen with **you** in chat.

## Handoff contracts (what each agent returns)

- **Scenario falsifier:** `soundSpec: yes/no`; findings `{severity, scenario, why}`; concrete
  Gherkin rewrites; coverage gaps as proposed scenario titles.
- **Implementer:** per-step status (red evidence: the failing test's name and failure line, or the
  recorded mutation for an adapted test; green; refactor), commit message used, focused-test
  result, the state of the `.feature` (armed, or which scenarios still fail), full-suite result
  before hand-off, any deviation from the plan (with reason).
- **Falsifier (each code lens):** `robust: yes/no`; weaknesses `{severity, where (file:line),
  why}`; concrete counter-test snippets; `treeDiff`, the production-tree diff against `HEAD` in
  its worktree, taken last. Bias to skepticism: if unsure, flag it.
- **Doc reviewer:** `PASS|FAIL`; findings `{rule: DOC-n, where, what, fix}`.
- **Reviewer:** `PASS|FAIL`; findings `{category: process|cleancode|solid|hexagonal|tests|security|docs,
  where, fix}`; ready-to-paste `process-learnings.md` row if a process rule slipped.

## Environment gotchas (tell every agent)

- **Bash stdout** may not surface on the Windows host: redirect to a file
  (`./verify all > out.txt 2>&1`) and `Read` it; in-container, Bash is normal.
- Permission-blocked commands + reset-free rewrite: see the FAIL-loop section. Prefer Read/Grep/Glob.
- **Never overlap a tree-mutating agent with another agent's build.** The falsifier writes probes and
  runs Gradle; a concurrent run on the same tree reports phantom failures and the two corrupt each
  other's `build/` state. Run falsify → review sequentially, or isolate per worktree (`Agent`
  `isolation: "worktree"`); same inside the panel. (The doc-reviewer only reads docs and runs no
  build; it may run alongside the reviewer.)
- **Start a run only once the tree is the state you intend to measure.** While edits are in flight,
  no run is evidence of anything, and a run started against a half-edited file measures a tree
  already known to be broken.
- **Probes live in the gitignored `src/probe/java`**, run via `./gradlew probe` and get cleared with
  `./gradlew clearProbes`, never with `rm` (→ [build-run-deploy.md](build-run-deploy.md),
  handbook §7): outside `check`/`build` and outside git, a leftover probe breaks nothing.
- Focused tests during micro-steps (`./verify focus <path>`), the `@wip` scenarios via
  `./verify bdd`; the full `./verify all` before the gate, cache disabled, executed-proof read
  ([testing.md](testing.md)).
- **The shell's working directory drifts between worktrees.** With a strand worktree checked out
  beside `main`, a Bash call can silently run in the wrong one, and a relative path then reads or
  writes the wrong tree with no error at all (seen three times in one gate, once losing two
  process-learnings rows into the main worktree while the branch stayed without them). Address the
  target explicitly in every call: `git -C <absolute path>`, absolute paths for reads, writes and
  Gradle, and `git status --short` in **both** worktrees before a gateway claim. `git show <sha>`
  keeps working from either place (worktrees share one object database), so a file check that
  disagrees with `git show` is a directory problem until proven otherwise; before the **first**
  edit of a session, require `git rev-parse --is-inside-work-tree` to answer `true`. A drifted
  Gradle run is **green**, so an exit code proves nothing: count the test-result XMLs in the
  worktree's own `build/` and check the strand's new test classes are among them, or the suite ran
  without the change. Mechanical enforcement is backlog **G7**.

## Worked example (the storage-value evaluator feature)

1. **Specify:** *Given* a member deposited crafted goods of a known recipe, *when* the officer
   opens the overview, *then* the member's storage sum shows the recipe value times quantity and
   quality. Drafted as a `.feature`, gated by the scenario falsifier, **confirmed by the author as
   complete**, committed `@wip`.
2. **Plan:** test list for `EvergoreDataEvaluator` storage calc: ① place value = Σ storageValue·qty·quality/100 ② withdraw value uses withdrawlValue ③ unknown item → UNDEFINED+log ④ watermark advances. Messages approved.
3. **Implement:** four red→green→refactor micro-commits with those messages; when the scenario
   passes, one commit arms it (removes `@wip`), and a refactor commit tidies the evaluator under
   that net.
4. **Falsify:** domain lens recomputes ① ② by hand; robustness lens: does ③ assert the log? quality=0 edge? is green real if the production line is reverted? → counter-tests.
5. **Review gate:** doc-reviewer: `domain-model.md`/`testing.md` updated, backlog row removed? reviewer: boundaries clean, messages one-line verb-first? → PASS.
6. **You, author gate 2:** the rebased tip with its compare range on your review, `--ff-only` on
   your go; push when happy.

## Evolution

Stay lean (1 implementer, the scenario gate, the per-surface lens panel, doc-reviewer, 1 reviewer).
Widen the panel + majority vote for high-risk changes; consider a `/tdd-step` skill once the loop is
proven; **Workflow tool** only for occasional large parallel audits (explicit opt-in), never for the
interactive, human-gated commit loop.
