# 10: Multi-Agent Playbook

How this project is developed with a small team of AI agents: **you + I plan; cheaper agents
implement; independent agents try to break it and gate the commit.** The single-agent ground rules
live in [working-with-ai-agents.md](working-with-ai-agents.md).

## Roles

| Role | Who / model | Spawned? | Input → Output |
|------|-------------|----------|----------------|
| **Planner** | You + me (the main-session model), in chat | No, it's a *hat* | Backlog item → the feature's **author-confirmed `.feature`** (handbook §5) + an **approved, ordered commit/test list** |
| **Scenario falsifier** | `falsifier-scenario` subagent (frontmatter `model: sonnet`) | Yes (fresh) | A draft `.feature`, before implementation → skeptical verdict on it as a specification + concrete Gherkin rewrites |
| **Implementer** | `implementer` subagent (frontmatter `model: sonnet`) | Yes | Approved plan → executes red→green→refactor per step, **commits each locally** with the pre-approved message, arms the scenarios → summary |
| **Falsifier panel** | `falsifier-domain` + `falsifier-robustness` subagents, plus `falsifier-frontend` for a UI surface (frontmatter `model: sonnet`); spawned **per touched surface** (see cadence) | Yes (each fresh) | The feature diff/commits → adversarial verdicts (domain correctness · test honesty/robustness · UI honesty) + concrete counter-tests / weaknesses |
| **Doc reviewer** | `doc-reviewer` subagent (frontmatter `model: haiku`) | Yes (fresh) | The feature diff/commits → PASS/FAIL against the KB README's DOC checklist + findings citing rule IDs |
| **Reviewer / Gate** | `reviewer` subagent (frontmatter `model: opus`) | Yes (fresh, per gate) | Scenario gate: the draft `.feature` + the scenario falsifier's report → PASS/FAIL. Feature gate: the feature commits + the panel's + doc-reviewer's reports → PASS/FAIL + findings + proposed `process-learnings.md` entry |

**Non-negotiable:** the falsifiers, the doc-reviewer and the Reviewer are **fresh, independent** agents, never the Implementer
checking itself (independence is the whole point). Agents **never `git push`**; pushing is the
author's decision (see [engineering-handbook.md](engineering-handbook.md) §7).

## The pipeline (per backlog item)

```
0. SPECIFY (you + me, chat) - MANDATORY for any observable behavior (handbook §5)
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
   - per step: red (write failing test) → ./verify focus → green (minimal code) → refactor
     → commit locally with the EXACT pre-approved message; the scenarios run via ./verify bdd
   - ARM: when every scenario of the feature passes, one commit removes @wip, the last TDD step;
     from here ./verify all runs the scenarios on every commit and at every gate
   - REFACTOR the feature under the armed net: no behavior change, each step its own green commit
   - the feature is done when the armed scenarios pass, not when the steps are ticked off
   - never pushes; the architecture's boundary rules hold (see architecture.md)
3. FALSIFY (spawn the lenses the change can break, each fresh, each in its own detached
   worktree at the tip under review; see cadence)
   - domain lens: the project's core calculations and invariants; robustness lens: fake-green
     tests, edge cases, time/concurrency/resources, boundary violations, secrets
   - each returns a verdict + proposed counter-tests, and ends with the diff of the production
     tree against HEAD in its worktree (a left-behind probe is its own finding)
4. REVIEW GATE (spawn `doc-reviewer` + `reviewer`, both fresh), once per FEATURE commit
   - doc-reviewer: docs hygiene per the DOC checklist, task-scoped, plus a stateless rotating
     one-doc sweep (pick = commit count mod doc count; see the agent definition)
   - reviewer: process adherence (scenario gate and author gate 1 honored? real
     red→green→refactor? arming commit the last TDD step, refactor commits behavior-free?
     whitespace separate? commit-msg rules?) + code criteria (clean code, SOLID, architecture
     boundaries); integrates the panel's + doc-reviewer's findings, all of which are in its brief
   - PASS → proceed;  FAIL → fix and re-gate under the loop rules below; logs a process-learning if a rule slipped
5. ► AUTHOR GATE 2 (handbook §7 gateway): rebase onto current main, run ./verify all on the
   rebased tip, present the reviewable statement, YOU review the rebased tip; on your go
   `git merge --ff-only`, then the worktree and branch are cleaned up
   - a re-rebase after your review: clean and green → ff without a second review; a conflict
     resolution → panel and your review again
6. PUSH DECISION (YOU)
   - only the author pushes, when satisfied
```

> **ADAPT:** define the domain lens for your project: the core calculations, invariants, and parsing/IO fidelity that `falsifier-domain` must recompute and attack by hand (keep this in sync with `.claude/agents/falsifier-domain.md`). The robustness lens is project-agnostic.
>
The template ships `falsifier-frontend` as the lens for a **UI surface** (fake-green component
tests, user paths, async determinism, layer boundaries, API contract, the one real-artifact
scenario); it is spawned only when the project has that surface and the change touches it.

> **ADAPT:** name the UI doc that lens checks against, and add a lens per further **surface** the
> project has (a CLI, a message consumer) on the same shape. One agent file per lens, spawned only
> when that surface is touched.

### FAIL-loop rules (when the falsifier or reviewer rejects)

A FAIL sends the work back to step 2, then re-runs a **fresh** falsifier panel and a **fresh** reviewer.
The **loop-bounding governance** (cap at 2 re-implement rounds → escalate + re-plan; early-escalate on
a repeated finding; process-only FAILs don't consume a round) is canonical and tool-neutral in
[engineering-handbook.md](engineering-handbook.md) §9. Below is the harness-specific *how* of keeping
the history clean while that loop runs:

- **Fold fixes into the commit they belong to; never append "fix review" commits.** The branch is
  unpushed, so rewriting local history is safe and expected (clean up before the author pushes), so the
  history reads as if the work were done right the first time. **In this harness `git reset` (all forms,
  including `--soft`/`--mixed`) and `git branch -D` are permission-blocked** by Claude Code's built-in
  destructive-git guardrails (alongside `git push`); they override project `allow` and can't be
  allowlisted via `settings.json` at all, so the reset/rebase recipes don't run. The committed `deny`
  list encodes the same floor, so it holds even in tools without those built-ins. `rm` is denied in
  **all** forms (handbook §7): the agent's own cleanup is git-native (`git worktree remove` /
  `prune`, `git branch -d`), which it runs autonomously, and anything else is handed to the author or
  removed by a committed build task. **If you ever do run a reset outside the tool, prefer the
  least-destructive form, `git reset --soft`; never `--hard`** (it discards the working tree). Reset-free method that
  folds a fix into any commit, even a deep one: save the corrected tree on a WIP commit, branch off the
  feature base afresh, then for each logical group `git checkout <wip-tip> -- <files>` to stage it and
  commit, and swap names with `git branch -m`. Drop the stale WIP branch with
  `git update-ref -d refs/heads/<name>` (the `-D` block doesn't reach plumbing). A tip-only fix still
  takes `git commit --amend`; `git rebase -i` has no interactive TTY here.
- **Answer a gate finding by amending the commit that owns it** (scripted `GIT_SEQUENCE_EDITOR`
  rebase-edit + `--amend`, no `fixup!`), and fold added coverage into the commit whose behavior it
  pins. **Fold at the moment of fixing, before the next build**, never at the end of the round: the
  fold is the act of fixing, not a step after it. The gate reads the message list before the diff,
  so a "gate round N fixes" commit is a FAIL on sight; a proposed message that names one hunk of a
  multi-hunk commit, or an "and" joining two clauses, is the signal that the fold was skipped or the
  commit must split.
- **Apply a panel finding to the whole diff, not to the file it was raised against.** When a finding
  is a *rule*, re-grep every file the strand touches for that pattern before reporting it resolved.

**Cadence (cost vs rigor):** micro-steps run lightweight; the **reviewer gates at the feature
commit**, not every micro-commit. The **falsifier panel** runs at the feature end.

- **Spawn only the lenses the change can break.** A backend-only change gets `falsifier-domain` +
  `falsifier-robustness`; a change confined to another surface gets that surface's lens +
  `falsifier-robustness`; a full-stack change gets all of them. Spawning a lens that cannot see the
  change buys a confident PASS that means nothing.
- **One detached worktree per lens** (`git worktree add <path> <sha> --detach`) at the tip under
  review, named in the brief, so two lenses cannot collide with each other or with the planner. Every
  lens that writes a probe or a mutation ends its report with the diff of the production tree
  against `HEAD` in its worktree, so a left-behind change shows up in its own report rather than in
  the next build.
- **Every prior verdict is an input to the next gate agent**, not a message to the author alone: the
  falsifiers' and the doc-reviewer's reports go into the reviewer's brief verbatim, and a lens whose
  tip is no longer the one under review is re-run, never quoted.
- **`[doc]`-scope changes skip the falsifier panel** (the doc fast lane): the `doc-reviewer` is the
  gate, and the `reviewer` joins only when process adherence is genuinely in doubt.
- **Escalation rule:** for features touching the project's high-risk areas, the planner spawns the
  gate agents with a one-off `model` override on the strongest tier the plan offers (`fable` when
  available, otherwise `opus`); the frontmatter defaults stay unchanged.

> **ADAPT:** name the high-risk areas that trigger gate escalation (typical candidates: money or
> valuation math, time/timezones, concurrency, data migration, and the auth/security surface, i.e.
> filters, token handling, what is publicly exposed).

## Two tracks: direct-on-main vs. feature branch

The planner picks the track **up-front** and announces it (the author can veto). See
[engineering-handbook.md](engineering-handbook.md) §7 for the full rule; the operational gist:

- **Small / single TDD cycle** → **directly on `main`**, one commit via the propose→confirm→commit
  protocol. (This is the classic loop above.)
- **Large / multi-cycle or new BDD scenarios** → a **feature branch in its own worktree**. The
  implementer commits each red→green→refactor step **itself** with protocol-conform messages, **no
  per-commit pre-approval on the branch** (that's the point: real TDD cadence without N round-trips).
  Falsifier + reviewer review the **branch diff**; then **step 5 becomes the gateway**: the **author +
  planner review the branch's `git log` together**, reword messages if needed (scripted rebase), and
  the branch lands by **rebase + fast-forward only, no merge commit, no squash**. **Every commit on
  `main` builds green**, so a broken branch commit is repaired by rebase before the merge. Only the
  author pushes.

If a "small" item balloons mid-flight, the planner moves the *uncommitted* WIP onto a branch
(`git switch -c`) before it grows; `main` stays clean. **Parallel/benchmark runs** use git worktrees
(`Agent` tool `isolation: "worktree"`).

### Landing a branch (gateway hand-off, rebase, merge, cleanup)

- **State the full worktree identity at every review-gateway hand-off:** the branch name **and** the
  absolute worktree path, plus the compare range `<tip>..<base>` on its own line, newest first and
  nothing else in it ([working-with-ai-agents.md](working-with-ai-agents.md)). The author opens that
  path directly in their editor and pastes the range into the compare tool, so the hand-off must
  contain everything needed to do so; a hand-off missing any of the three stalls the gateway.
- **Rebase autonomously, merge conditionally:** rebase a feature strand onto the current `main`
  without asking first, and run the full `./verify all` on the rebased tip every time (handbook §7):
  a clean rebase is not a green one until the build says so. If the strand was already
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
- **Sweep the stale ones periodically.** `git worktree list` and
  `git branch --merged main` are the inventory; a worktree whose branch is merged, or whose session
  is long gone, is removed the same way. A worktree that is *not* merged is reported to the author,
  never removed.
- **Landing moves a branch that a worktree holds, so land inside that worktree** (`git merge
  --ff-only`), not by moving the ref with plumbing: a ref moved under a checkout leaves the index and
  files on the old commit, the strand reads as if it had reverted itself, and `git log` cannot see
  it. When plumbing is genuinely unavoidable, run `git status` in the receiving worktree afterwards
  and read it; never report a landing without it.

## How to invoke

The **orchestrator is the main session** (me). I drive the pipeline with the `Agent` tool,
`subagent_type` = `implementer` / `falsifier-scenario` / `falsifier-domain` / `falsifier-robustness`
/ `falsifier-frontend` (UI surface only) / `doc-reviewer` / `reviewer` (defined in `.claude/agents/`). The
Planner phase and every commit-plan approval and push happen with **you** in the main chat.

## Handoff contracts (what each agent returns)

- **Implementer:** per-step status (red/green/refactor done), the commit message used, focused-test
  result, full-suite result before hand-off, and any deviation from the approved plan (with reason).
- **Falsifier (each panel member):** `robust: yes/no`; a list of weaknesses `{severity, where (file:line), why}`; concrete
  counter-test snippets. Bias to skepticism: if unsure, flag it.
- **Doc reviewer:** `PASS|FAIL`; findings `{rule: DOC-n, where, what, fix}`.
- **Reviewer:** `PASS|FAIL`; findings `{category: process|cleancode|solid|architecture|tests|security|docs,
  where, fix}`; a ready-to-paste `process-learnings.md` row if any process rule was missed.

## Environment gotchas (tell every agent)

- **The working directory is not reliable.** With several worktrees checked out, a Bash call can
  silently land in the primary checkout instead of the strand it was told to work in, and a relative
  path then reads or writes the wrong tree **with no error**. `git show <sha>` keeps working from
  either place (worktrees share one object database), which makes the contradiction look like a lost
  write rather than a wrong directory. So: **never use a relative path across worktrees.** Every
  read, grep, write and build in a strand goes through `git -C <abs path>` or an absolute path,
  including the one-off greps that feel too small to bother; print `pwd` in the same command that
  reads or writes. A file check that disagrees with `git show` is a directory problem until proven
  otherwise. And before the **first** edit of a session, require
  `git rev-parse --is-inside-work-tree` to answer `true`: reading files proves nothing about where
  you are, and a bare repository's directory carries a full file tree while accepting no commit.
  A drifted build run is **green**, so its exit code proves nothing: read the executed test results
  in the strand worktree's own build output and check the strand's new tests are among them, or the
  suite ran without the change.
- `git reset` (all forms), `git branch -D` and `rm` (all forms) are permission-blocked (see
  §FAIL-loop for the reset-free history rewrite + `git update-ref -d` to drop a scratch branch).
  Prefer Read/Grep/Glob.
- **Throwaway code goes into a build-owned probe location, never the test tree.** An agent that needs
  to probe a library's real behavior, or reproduce something by hand, writes it where the build and
  git both ignore it: a **gitignored source set outside `check`/`build`**, with its own run task and
  a committed clean-up task. That is what makes a leftover probe harmless (it cannot turn the build
  red and `git add -A` cannot commit it) and removes the *need* to delete rather than the rule
  against it, so no `rm` and no author round-trip sits on the gate loop's critical path.
- **Never overlap a tree-mutating agent with another agent's build.** The falsifier writes probes and
  runs the build; a reviewer (or any build) hitting the same working tree at the same time builds the
  mutated tree, reports phantom failures, and the two corrupt each other's build state. Panel members
  that **run** anything get their own worktree (`Agent` `isolation: "worktree"`) or run serialized;
  only read-only lenses share one. If a sibling's run is already active in a tree, report that rather
  than racing it. (The doc-reviewer only reads docs and runs no build; it may run alongside the
  reviewer.)
- **Start a run only once the tree is the state you intend to measure.** While edits are in flight,
  no run is evidence of anything, and a run started against a half-edited file measures a tree
  already known to be broken.
- Run **focused** tests during micro-steps (`./verify focus <path>`), the full `./verify all` before
  the gate, with the cache disabled and the executed-not-cached check of handbook §6.

> **ADAPT:** add machine-specific quirks agents must know (example: a host where Bash stdout is not surfaced, so agents redirect output to a file, `./verify all > out.txt 2>&1`, and `Read` it).
>
> **ADAPT:** name the probe location and its two tasks for your stack (example, Gradle: a gitignored
> `src/probe/java` source set on the test classpath, `./gradlew probe` to run it and a committed
> `Delete` task `./gradlew clearProbes` to clear it), and record them in build-run-deploy.md.

## Worked example: a small calculation feature (example)

1. **Specify:** *Given* a customer ordered three items at the documented rates, *when* the manager
   opens the monthly overview, *then* the contribution column shows the summed value. Drafted as a
   `.feature`, gated by the scenario falsifier, **confirmed by the author as complete**, committed
   `@wip`.
2. **Plan:** test list for a value evaluator, e.g. ① happy-path value follows the documented formula
   ② the variant operation uses its own rate ③ unknown input → explicit UNDEFINED result + log
   ④ the incremental-progress watermark advances. Commit messages approved.
3. **Implement:** four red→green→refactor micro-commits with those messages; when the scenario
   passes, one commit arms it (removes `@wip`), and a refactor commit tidies the evaluator under
   that net.
4. **Falsify (panel):** the domain lens recomputes ① and ② by hand; the robustness lens asks "does ③
   assert the log? does the zero-quantity edge work? is green real if I revert the production line?"
   → counter-tests.
5. **Review:** boundaries clean? KB ([domain-model.md](domain-model.md) / [testing.md](testing.md))
   updated? messages one-line verb-first? → PASS.
6. **You, author gate 2:** the rebased tip with its compare range on your review, `--ff-only` on
   your go; push when happy.

## Evolution

Start lean: the implementer, the scenario gate, the domain and robustness lenses, the doc-reviewer
and the reviewer. Add a lens per surface as the project grows one (§Roles). Widen the panel +
majority vote for high-risk changes; consider a `/tdd-step` skill in `.claude/skills/` once the loop
is proven; use the **Workflow tool** only for occasional large parallel audits (explicit opt-in),
never for the interactive, human-gated commit loop.
