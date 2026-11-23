# 11: Multi-Agent Playbook

How we develop with a small team of AI agents: **you + I plan; cheaper agents implement;
independent agents try to break it and gate the commit.** Part of the project's showcase goal
(see [working-with-ai-agents.md](working-with-ai-agents.md)).

## Roles

| Role | Who / model | Spawned? | Input → Output |
|------|-------------|----------|----------------|
| **Planner** | You + me (main-session model), in chat | No, a *hat* | Backlog item → the feature's **author-confirmed `.feature`** (handbook §5) + approved, ordered commit/test list |
| **Scenario falsifier** | `falsifier-scenario` (`model: sonnet`) | Yes (fresh) | A draft `.feature`, before implementation → skeptical verdict on it as a specification against its ticket + concrete Gherkin rewrites |
| **Stakeholder falsifier** | `falsifier-stakeholder` (`model: opus`) | Yes (fresh) | A draft `.feature` (or the whole suite, in a catch-up's closing run) read against every `.feature` file → verdict on actor, product-owner readability, time, numbers + rewrites + merge proposals for overlaps |
| **Implementer** | `implementer` (frontmatter `model: sonnet`) | Yes | Approved plan → red→green→refactor per step, commits each locally with the plan's message (a protocol-conform one of its own where the plan names none), arms the scenarios → summary |
| **Falsifier panel** | `falsifier-domain` · `falsifier-robustness` · `falsifier-frontend` (each `model: sonnet`); spawned per touched surface (see cadence) | Yes (fresh) | Feature diff/commits → adversarial verdicts (domain math · test honesty/robustness · SPA behavior) + counter-tests |
| **Doc reviewer** | `doc-reviewer` (`model: haiku`; round 1 of a large docs-only strand: handbook §9) | Yes (fresh) | Feature diff/commits → PASS/FAIL against the KB README's DOC checklist + findings with rule IDs |
| **Reviewer / Gate** | `reviewer` (`model: opus`) | Yes (fresh, per gate) | Scenario gate: the draft `.feature` + both scenario-gate falsifiers' reports → PASS/FAIL. Feature gate: feature commits, from round 2 on also the earlier rounds' panel and doc-reviewer reports → PASS/FAIL + findings + proposed `process-learnings.md` entry |

<!-- MODULE security-lens -->
The falsifier panel also holds `falsifier-security` (frontmatter `model: sonnet`), spawned only for
a diff that touches the authentication surface: it reads the change as an attacker and returns a
way in through that surface, or none, from few targeted requests against a container of the tip
(→ Cadence).
<!-- /MODULE security-lens -->

**Non-negotiable:** falsifiers, doc-reviewer and Reviewer are **fresh, independent** agents, never
the Implementer checking itself (independence is the point). Subagents **never push**; on the agent
seat the session that owns the strand pushes its `claude/<topic>` branch at the gateway with
`agent/push`, and only the author moves `origin/main`
([engineering-handbook.md](engineering-handbook.md) §7).

## The pipeline (per backlog item)

```
0. SPECIFY (you + me, chat), mandatory for any observable behavior (handbook §5)
   - draft the feature's `.feature` in product language, checked against the completeness list
   - scenario gate: spawn `falsifier-scenario` and `falsifier-stakeholder` in parallel, then
     `reviewer` in scenario mode with both reports; fix, re-gate (all three in full each time
     up to the bound below, handbook §9)
   - every re-gate brief carries the refuted list (handbook §9)
   - the bound is round 9: one more full run after that round's fold, then only the reviewer's
     mandatory folds, then a closing run; what is still open goes to author gate 1 beside the
     scenarios (handbook §9)
   - open lows reach author gate 1 unfolded (handbook §9)
   - the reviewer's verdict rule: handbook §9 (see "The scenario-gate dossier")
   - ► AUTHOR GATE 1: YOU CONFIRM THE SCENARIOS AND THEIR COMPLETENESS (a gate, not a notification)
   - commit the confirmed scenarios tagged @wip (the default run excludes them: the build stays green)
   - the only way past this step is an explicitly claimed exemption: a pure refactoring,
     a [doc]/process commit, or build/infra work
1. PLAN (you + me, chat)
   - the runner's dry run lists the undefined steps: that is the glue work; then a small ordered
     test list → ordered one-line commit messages, step definitions and production code alike
   - the test list names any real-time test (handbook §6)
   - slice the commits BEFORE the first edit (handbook §7): each green and reviewable alone
   - name the gate line: the lenses and their models (see cadence)
   - ► YOU APPROVE THE PLAN (the slicing and the test list; the agent words the messages)
2. IMPLEMENT (spawn `implementer`) with the approved plan
   - one spawn per phase (handbook §4)
   <!-- MODULE gate -->
   - the phase check `gate/dossier-check --phase` after each phase (handbook §4)
   <!-- /MODULE gate -->
   - per step: red (failing test) → ./verify focus → green (minimal code) → refactor
     → commit locally with the message the plan names, or a protocol-conform one where it names
     none; the scenarios run via ./verify bdd
   - ARM: per handbook §5 step 5
   - REFACTOR the feature under the armed net: no behavior change, each step its own green commit
   - the feature is done when the armed scenarios pass, not when the steps are ticked off
   - never pushes; domain/businessLogic/application stay framework-free
3. FALSIFY AND REVIEW, one panel round
   - spawn the lenses the change can break, the `doc-reviewer` and the `reviewer` together, each
     fresh (see cadence)
   - each in its own detached worktree at the tip under review
   - from round 2 on, every lens whose read paths the fold touches re-runs, the robustness lens on
     every fold of code outside `*.md` (handbook §9, delta rounds)
   - first, every round: `git fetch origin`; where `origin/main` moved ahead, `main` synced
     (`sh hooks/sync-main`), the strand rebased and the gate's evidence rebuilt before any gate
     agent is spawned (handbook §7, "A moved `main` is taken in before the next panel"), then
     `./verify format` green at the tip (author decision 2026-10-09)
   - domain lens: value math, aggregation, parser fidelity
   - robustness lens: fake-green tests, edges, time/concurrency/resources, boundaries, secrets
   - frontend lens: fake-green component tests, user paths, async determinism, layer boundaries,
     API contract, the browser-driven scenarios
   <!-- MODULE security-lens -->
   - security lens: a way in through the authentication surface, by few targeted requests against
     a container of the tip
   <!-- /MODULE security-lens -->
   - each returns verdict + counter-tests, and ends with the status of its worktree, new files
     included (`treeDiff`; a left-behind probe is its own finding)
   - doc-reviewer: docs hygiene per the DOC checklist, task-scoped, plus a stateless
     rotating one-doc sweep (pick = commit count mod doc count; see the agent definition)
   - reviewer: process adherence (scenario gate and author gate 1 honored? real
     red→green→refactor with red evidence? arming per handbook §5 step 5? refactor commits
     behavior-free? whitespace separate? commit-msg rules?) + code criteria; from round 2 on it
     integrates the earlier rounds' reports, all of which are in its brief
4. REVIEW GATE, once per FEATURE commit
   <!-- MODULE gate -->
   - the round closes on a PASS of `sh gate/dossier-check <abs strand worktree> <tip>`, run from
     the primary checkout, once its reports are in:
     the reviewer runs it as its first act, beside the lenses, and the orchestrator again once
     `reviewer.md` is saved (see "The gate dossier")
   <!-- /MODULE gate -->
   - PASS → proceed (a round whose findings are all low files them first, handbook §9); FAIL →
     fix and re-gate under the loop rules below; log a process-learning if a rule slipped
5. ► AUTHOR GATE 2 (handbook §7 gateway): sync main (`sh hooks/sync-main`), rebase onto it, run
   ./verify all on the rebased tip, present the reviewable statement (branch, worktree identity,
   compare range, on the agent seat the pull request), YOU review the rebased tip (its log too,
   reworded where needed)
   <!-- MODULE agent-seat -->
   - on the agent seat (`AGENT_SEAT` reads `unattended`) the session that owns the strand pushes
     the rebased tip with `agent/push` before the hand-off; the strand's pull request is open by
     then
   <!-- /MODULE agent-seat -->
   - a re-rebase after your review: clean and green → landed without a second review; a
     conflict resolution → the gate agents handbook §7 names re-run, then your review again
6. LANDING (YOU): `/land <tip>` on the pull request, or `git push origin <tip>:refs/heads/main`
   (handbook §7); a fast-forward either way
   - once origin/main holds the tip, the agent fast-forwards main (`sh hooks/sync-main`) and
     cleans up the worktree and the branch, on the agent seat its remote copy too
     (`agent/push --delete <branch>`)
```

Step 0 is in force in this project (handbook §5, "Status in this project"): a plan without
scenarios claims one of the §5 exemptions explicitly and then starts at step 1.

The plan's go (decision 2026-10-15):

- a refactoring keeps the plan's review before the first edit;
- a `[doc]` or build/infra strand may get the go at the start, and the plan's review at the
  gateway (handbook §7).

<!-- MODULE agent-seat -->

On the agent seat (author decision 2026-10-17):

- a brief that names the strands, their proofs and their exemptions is the approved plan;
  - Why: an unattended seat has nobody to wait for.
- the questions the agent may still ask are handbook §9's brakes.
<!-- /MODULE agent-seat -->

### FAIL-loop rules (when a falsifier or reviewer rejects)

A FAIL sends work back to step 2; which gate agents the next round re-runs is handbook §9's
(delta rounds).
Loop-bounding governance is canonical in [engineering-handbook.md](engineering-handbook.md) §9.
Harness-specific history hygiene:

- **Fold fixes into the commit they belong to; never append "fix review" commits.** The branch is
  the strand's own; rewriting its history is expected (history reads as if done right the first
  time).
  <!-- MODULE agent-seat -->
  - On the seat, where the strand's branch is pushed, a rewrite is followed by
    `agent/push <worktree>`, and a reset-free fold by `agent/push <worktree> <remote tip>`
    (handbook §7, "Pushing").
  <!-- /MODULE agent-seat -->
- **Answer a gate finding by amending the commit that owns it** (scripted `GIT_SEQUENCE_EDITOR`
  rebase-edit + `--amend`, no `fixup!`; where a later pick stops on a conflict, resolve it, `git add`
  and `git rebase --continue`, since `--amend` there folds that pick into the commit before it,
  as observed), and fold added coverage into the commit whose behavior it pins. **Fold at the
  moment of fixing, before the next build**, never at the end of the round: the
  fold is the act of fixing, not a step after it. The gate reads the message list before the diff,
  so a "gate round N fixes" commit is a FAIL on sight; a proposed message that names one hunk of a
  multi-hunk commit, or an "and" joining two clauses, is the signal that the fold was skipped or the
  commit must split.
  - `pre-commit` refuses an amend at a conflict stop (engineering-handbook.md §7).
  - For a commit the hooks recorded (engineering-handbook.md §7) the amend and `agent/fold` are
    refused, so the fix goes in at the recorded commit's own edit stop.
    - Start it scripted, with `edit` on the todo's first line, the recorded commit:
      `GIT_SEQUENCE_EDITOR="sed -i 1s/^pick/edit/" git rebase -i <recorded commit>^`
      ([permission-probe-record.md](permission-probe-record.md), row W25), with `--root` in place
      of `<recorded commit>^` for a recorded root commit.
      - That form runs with the strand's worktree as working directory, as W25 ran; from the
        primary checkout each git command of this recipe takes `-C <worktree>` after `git` (W16
        ran `git -C` with a rebase, W25 the prefix; the two together are not probed).
    - At the stop, `git add` the fix, then run `GIT_EDITOR=true git rebase --continue`, as
      `hooks/breach-guard` prints it.
      - Why `GIT_EDITOR=true`: at an `edit` stop with staged changes a plain continue opens the
        commit-message editor, which an unattended session cannot answer.
    - Why: the sequencer amends without `pre-commit`, and `post-rewrite` checks the result again.
  - Fixes for several earlier commits ride in temporary commits with ordinary messages.
  - They carry no `fixup!` subject, which `commit-msg` refuses (hooks/README.md).
  - A scripted todo moves each behind its target as a `fixup` line (`agent/fold`, below).
  - `/workspaces/EvergoreProtocolCollector/agent/fold <absolute worktree path> <hash of the owning commit>`
    folds the worktree's last commit into the owning one.
    - It keeps the owning commit's message and opens no editor.
    - For a commit the hooks recorded it cannot run: `pre-commit` refuses the commit of the fix it
      needs first (above).
    - It folds only in a linked worktree on a `claude/` branch (its `branch_prefix`) or on a
      detached head.
    - It refuses a worktree with tracked changes, a worktree without a commit, an owning commit
      that is not an ancestor, a root commit, an owning commit that `main` or `origin/main`
      already holds, and a range that holds a merge.
      - A fix to a landed commit stays a commit of its own: a fold would put copies of landed
        commits on the strand.
    - A conflict stops the rebase where git stops it; resolve, `git -C <worktree> add` the files,
      then `GIT_EDITOR=true git -C <worktree> rebase --continue`, or `--abort`; either meets the
      classifier.
      - Why `GIT_EDITOR=true`: the continue of a conflicted `fixup` opens the commit-message
        editor, which an unattended session cannot answer.
      - A fold called while the rebase stands refuses and names that way on; one called while a
        `git am` stands names `git am --continue` or `--abort`.
    - The settings allow `Bash(/workspaces/EvergoreProtocolCollector/agent/fold*)`, with the
      attached `*` and a deny for a `..` step after it (not probed).
    <!-- MODULE guard -->
    - `agent/bash-shape-guard` approves it with an absolute worktree path and a commit hash, so a
      fold runs unattended.
    <!-- /MODULE guard -->
    - `agent/self-test` proves it in throwaway repositories.
  - **A scripted history rewrite** (a fold script, a scripted rebase):
    - every `git rebase --continue` runs behind a guard that greps the staged files for
      line-start conflict markers, runs `git diff --check` and `./verify format`, and stops on the
      first failure;
    - git does not stop again at an `edit` pick that conflicted;
      - that commit's planned changes go into the conflict resolution, or a second rebase is
        planned for them;
    - a fold rebase names the strand's merge-base SHA as its upstream, never `main`;
      - Why: a `main` that moved meanwhile would replant the strand without a fresh read.
    - after the rebase, `git range-diff <old base>..<old tip> <new base>..<new tip>` compares the
      two series commit by commit;
    - a mutation is reverted as handbook §4 says.
- **Apply a panel finding to the whole repository, not to the file it was raised against.** When a
  finding is a *rule*, search for its siblings before reporting it resolved.
  - The search covers the source tree and the test names, `docs/`, `.claude/agents/`,
    `.claude/skills/` and the entry file, not only the files the strand touches.
  - It searches by the claim's key words, not only by its identifiers.
  - The fix report quotes each search with its hits.
  - Why: a fix that reaches only the named instance leaves its siblings to the next round.
- **Permission-blocked in this harness:** `git reset`, `git clean` and `git branch -D`, each in every
  plain form, `git push` in every form (an agent pushes only through `agent/push`, handbook §7,
  "Pushing"), and `rm` (**all** forms: the agent's own cleanup is git-native, `git worktree
  remove`/`prune` and `git branch -d`, run autonomously; anything else is handed to the author or
  removed by a committed build task such as `./gradlew clearProbes`, handbook §7). Built-in
  guardrails override project `allow`; the committed `deny` list encodes the same floor for tools
  without the built-ins. What guards a push, and which `git -C` rules fire:
  working-with-ai-agents.md. A reset is the author's to run, never the agent's in another spelling
  (working-with-ai-agents.md, "Refusals and limits").
- **Reset-free fold** (works for deep commits): save the corrected tree as a WIP commit → branch
  afresh off the feature base → per logical group write each file back with
  `git show <wip-tip>:<path> > <path>`, `git add` it and commit → swap names via `git branch -m` →
  drop the stale WIP branch with `git update-ref -d refs/heads/<name>`, this recipe's named route
  for the agent's own scratch branch, only once `git diff --quiet <wip-tip> <rebuilt tip>` exits 0
  (the rebuilt branch holds its content).
  - `git branch -m`, `git update-ref` and `git rebase` have no allow and meet the auto-mode
    classifier (working-with-ai-agents.md, "Autonomy within guardrails").
  - Not `git checkout <wip-tip> -- <path>` written plainly: it is `ask` and meets the classifier
    behind `git -C` (the raw-blob caveat: working-with-ai-agents.md, "Command shapes that keep a
    session unattended").
  <!-- MODULE guard -->
  - With the guard module that redirect is refused: in an agent worktree write each file back with
    the approved `git -C <worktree> checkout <wip-tip hash> -- <path>`, in the primary checkout
    through `git show` into the output target and a copy (working-with-ai-agents.md, "The
    shell-shape guard").
  <!-- /MODULE guard -->
- Tip-only fix: `git commit --amend`. `git rebase -i` has no interactive TTY here.

**Cadence (cost vs rigor; decisions in open-questions.md):** micro-steps run lightweight;
falsifier panel + review gate run **at the feature commit**, not per micro-commit.

- **Spawn only the lenses the change can break:** backend-only → `falsifier-domain` +
  `falsifier-robustness`; frontend-only → `falsifier-frontend` + `falsifier-robustness`; full-stack
  → all three. Spawning a lens that cannot see the change buys a confident PASS that means nothing.
  This picks round 1; from round 2 on the read paths pick the lenses of a delta round (below,
  "Read paths of the lenses (delta rounds)"; handbook §9).
<!-- MODULE security-lens -->
- **`falsifier-security` joins wherever the diff touches the authentication surface** (author
  decision 2026-10-15): a path that starts with a line of `gate/authentication-surface`, the
  surface's one path list (the request filters, the security configuration, login, cookie and
  JWT, roles, passwords, the rate limit, deploy secrets).
  - The list names production paths, server and SPA.
  - A test file under a listed directory matches its line too (`frontend/src/login/*.test.tsx`
    under `frontend/src/login/`), so a change to it calls for the lens; a `*.md` file alone does
    not.
  - The full dossier check refuses a line of the list that starts no path the tip tracks
    (`surface`), so a rename that leaves a line behind fails the gate.
  - It works a fixed attack list, only the classes the diff touches (author decision 2026-10-20);
    its brief (`.claude/agents/falsifier-security.md`) holds the list.
  - Budget: at most 10 requests per attack idea and 120 per run (author decision 2026-10-20).
  - It starts the tip as a container of its own on a free port, never the home server.
  - It does no brute force, no fuzzing loops, no load and no dependency CVE scan (that is
    `vulnScan`).
  - Every request it sends goes through `gate/lens-request`, which fixes the host
    (`host.docker.internal` on the PC) and sends the path raw.
    - A named limit: the wrapper fixes the host alone, not the instance behind the port, the body
      file's place or the number of requests, so the brief's rules on those rest on the lens.
    - A named limit: a path holding `/..` meets the worktree deny before the wrapper runs, so the
      lens lists that attack under `notExercised:` (working-with-ai-agents.md, "Autonomy within
      guardrails").
<!-- /MODULE security-lens -->
<!-- MODULE agent-seat+security-lens -->
- **The host of `gate/lens-request` is `localhost` on the agent seat** (`AGENT_SEAT` reads
  `unattended`).
  - Why: the tip's container runs on the seat's own Docker daemon.
<!-- /MODULE agent-seat+security-lens -->
- **One detached worktree per lens**
  (`git worktree add --detach <root>/lens-<lens>-<topic> <sha>`, under the worktree root and with
  the scratch name of handbook §7) at the tip under review, named in the brief by its absolute
  path, so two lenses cannot collide with each other or with the planner. Every lens that writes a
  probe or a mutation ends its report with the status of its worktree, new files included
  (`treeDiff`), so a left-behind change shows up in its own report rather than in the next build.
- **Every prior verdict is an input to the next gate agent**, not a message to the author alone: the
  falsifiers', the doc-reviewer's and the reviewer's reports go into the gate dossier verbatim,
  where the reviewer of a later round reads them (→ The gate dossier); a lens report of an earlier
  tip stands as handbook §9 allows, otherwise the lens is re-run, never quoted.
- **The planner reproduces one quoted command of each doc-reviewer report** before it files the
  report's findings or hands the report on.
  - Why: a retyped or summarized output can carry evidence the command never printed.
- **Every lens brief and the reviewer's at the feature gate carry an "Already decided, out of
  scope" list** (author decisions 2026-10-12 and 2026-11-23; handbook §9): the findings earlier rounds rejected or
  filed, the author's decisions bearing on the strand and what the plan leaves out, so a gate agent
  does not raise them again each round.
- **Pure documentation/process changes (`[doc]` scope) skip the falsifier panel;** a docs-only
  fold of any strand runs the same way (handbook §9).
  - The `doc-reviewer` and the `reviewer` are the gate.
  <!-- MODULE gate -->
  - With the gate module the dossier holds it: the `reviewer` joins every `[doc]` gate and
    docs-only fold (handbook §9; → The gate dossier, Required reports).
  <!-- /MODULE gate -->
  - Round 1 of a large docs-only strand splits the doc-reviewer (handbook §9).
- **Escalation:** features touching valuation math, time/timezones, concurrency, data migration or
  the auth/security surface (filters, token handling, what is publicly exposed) → spawn gate agents
  with a one-off `model` override on `opus`; frontmatter defaults stay unchanged.
- **Tiers for the quota** (author decision 2026-10-12), since the PC draws on the agent seat's
  quota ([working-with-ai-agents.md](working-with-ai-agents.md), "Choosing the session model"):
  - no lens runs on `fable`;
  - a delta round spawns its lenses and its reviewer on `sonnet`, escalated or not, a delta being
    small.
- **The plan names the gate line** before the first lens is spawned: the lenses and their models.
  - Why: a first round that should have escalated and did not can leave the costliest findings to
    later rounds.

### Read paths of the lenses (delta rounds)

- The orchestrator finds the paths a fold changed with
  `git diff --name-only --no-renames <report tip> <tip>`.
  - The two trees are compared directly, so one command covers the fold and a rebase onto a moved
    `main` alike.
  - `--no-renames` lists a renamed path at both its ends, so code renamed into the docs still
    calls its lenses.
- `falsifier-robustness` reads every path outside `*.md`.
- The doc-reviewer and the reviewer have no read paths: handbook §9 says when they run again.
<!-- MODULE gate -->
- The read paths live in the ADAPT block of `gate/dossier-check`: a lens's `needs_` value calls for
  its report, its `reads_` value lets an earlier one stand (→ The gate dossier).
  - The block knows the domain and frontend lenses; a lens added per surface needs its pair of
    values added to the check as well.
<!-- /MODULE gate -->

<!-- MODULE security-lens -->
- `falsifier-security` reads every path outside `*.md`, as the robustness lens does: it attacks the
  running app, so every code path is one it reads.
  - Its report stands after a fold that changes only docs and is re-run after any other fold.
  - The dossier check holds the rule in code (→ The gate dossier).
<!-- /MODULE security-lens -->

## One track: every change on a branch (decided 2026-10-04)

Every change, a one-commit fix included, runs on a `claude/<topic>` branch in its own worktree
and lands through the gateway; no commit is made on `main`
([engineering-handbook.md](engineering-handbook.md) §7).

- **Size signal**: a planned strand of more than about 15 commits is a candidate to split into
  strands that land one after another, since each gate round re-checks every commit; a judgement
  per strand, not a rule (author, 2026-09-29).
- The implementer commits the messages the plan names and writes protocol-conform ones only where
  the plan names none (no per-commit approval; avoids N round-trips). Panel + reviewers review
  the **branch diff**; at the gateway author + planner review the branch `git log`, reword if needed
  (scripted rebase), and the author lands it by **rebase + fast-forward only** (no merge commit, no
  squash). **Every commit on `main` builds green**; broken branch commits are repaired by rebase
  before the landing.
- Parallel/benchmark runs use detached git worktrees,
  `git worktree add --detach <root>/<name> <sha>` under the worktree root (handbook §7), each named
  in its brief by absolute path, not the `Agent` tool's `isolation: "worktree"`, so the root stays
  certain:
  - isolation put its worktree under `.claude/worktrees/` of the primary checkout (measured
    2026-10-13 on the agent seat, whose root that is);
  - whether it honours `AGENT_WORKTREES` on the PC is unmeasured.

### Gateway hand-off and cleanup (the agent's side)

- **State the full worktree identity at every review-gateway hand-off:** the branch name **and** the
  absolute worktree path, plus the compare range `<tip>..<base>` on its own line, newest first and
  nothing else in it, and on the agent seat the pull request's link on another
  ([working-with-ai-agents.md](working-with-ai-agents.md)), with the branch pushed under that name
  (`agent/push`) and its pull request open first; on a local machine the branch stays unpushed
  (handbook §7). The author opens that path in the editor, fetches the branch on another machine
  or opens the link, and pastes the range into GitLens; a hand-off missing any of these stalls the
  gateway.
- **Rebase autonomously, land conditionally:** sync `main` (`sh hooks/sync-main`) and rebase a
  feature strand onto it without asking first, and run the full `./verify all` on the rebased tip
  every time (handbook §7): a clean rebase is not a green one until the build says so, and on the
  agent seat only a green tip is pushed again, with `agent/push`. If the strand was already
  author-reviewed **and** the rebase ran clean (no conflicts, no judgment-call resolutions) **and**
  that build is green, it goes to the author's landing without a further review round-trip. When
  the rebase hit a conflict: resolve it, run the full build, **re-run the gate agents handbook §7
  names for a conflict resolution** (the bullet "Every rebase is followed by the full gateway
  build"), then present exactly what was resolved plus the compare range and wait for the author's
  go. Moving `origin/main` always stays with the author.
- **A dependency bot's update lands as a strand** through the hooks, the gate and a fast-forward,
  never by the merge button of its pull request.
  - On the agent seat the session closes the superseded bot pull request with
    `agent/gh pr close <number>` (`/plan-next`, "Build the difference list", check 4).
- **Clean up landed strands immediately, without asking:** once `origin/main` holds a strand's tip
  and the primary checkout's `main` has fast-forwarded to it (`sh hooks/sync-main`), `./verify
  stop` in its worktree, then `git worktree remove <path>` and `git branch -d <branch>`, then
  `git worktree prune`, and `rmdir` a directory the removal left behind empty (handbook §7). This
  is the agent's own scaffolding and the agent owns it (handbook §7); the commits live on in
  `main`, the branch label and worktree are pure redundancy, and the reflog still holds deleted
  tips for a while. The safety is in the command, not in judgment: `git branch -d` refuses a
  branch that is not fully merged, and `git branch -D` stays denied. Never reach for `rm` here.
  <!-- MODULE agent-seat -->
  - On the agent seat the branch's copy on `origin` goes too, with `agent/push --delete <branch>`,
    once its current tip is in `origin/main` (handbook §7, "Landing models").
  <!-- /MODULE agent-seat -->
- **Sweep the stale ones periodically.** `git worktree list`, `git branch --merged main`, `git
  branch --no-merged main` (a parked branch without a worktree) and, after `git fetch --prune
  origin`, `git branch -r --list 'origin/claude/*'` (each name checked as handbook §7 says) are the
  inventory.
  - A worktree is removed the same way only when all three hold: its branch is merged into
    `main`, its `git status` is clean, and none of its files changed within the last day.
  - Every other worktree is reported to the author with its branch, absolute path and last file
    change, and removed only on the author's word: a session may still be running in it.
  - A remote branch whose tip `origin/main` contains is deleted with
    `agent/push --delete <branch>` on the seat (handbook §7); one that is *not* merged is unlanded
    work: reported to the author, never removed.
  <!-- MODULE volumes -->
  - The sweep, and the cleanup's `git worktree prune` above, run only in a session whose container
    mounts the volume, inside the container on the PC ([dev-environment.md](dev-environment.md),
    "Worktrees on a volume"; handbook §7, "Deleting project content").
  <!-- /MODULE volumes -->
- **A fast-forward moves a branch that a worktree holds, so run it inside that worktree**
  (`sh hooks/sync-main`, in the primary checkout), not by moving the ref with plumbing: a ref
  moved under a checkout leaves the index and files on the old commit, the strand reads as if it
  had reverted itself, and `git log` cannot see it. When plumbing is genuinely unavoidable, run
  `git status` in the receiving worktree afterwards and read it; never report a landing without it.

## How to invoke

Orchestrator = the main session (me), via the `Agent` tool: `subagent_type` = `implementer` /
`falsifier-scenario` / `falsifier-stakeholder` / `falsifier-domain` / `falsifier-robustness` /
`falsifier-frontend` / `doc-reviewer` / `reviewer` (defined in `.claude/agents/`). Planner phase
and commit-plan approvals happen with **you** in chat; moving `origin/main` is yours.
<!-- MODULE security-lens -->

`falsifier-security` is a `subagent_type` too, spawned only where the diff touches the
authentication surface ("Cadence").
<!-- /MODULE security-lens -->

## Handoff contracts (what each agent returns)

- **Scenario falsifier:** `soundSpec: yes/no`; findings `{severity, scenario, why}`; concrete
  Gherkin rewrites; coverage gaps as proposed scenario titles; `fingerprints:`, the output of
  `gate/scenario-check --fingerprints` verbatim.
- **Stakeholder falsifier:** `filesRead: n of n present`; `soundSpec: yes/no`; findings `{severity,
  scenario, lens, why}`; concrete Gherkin rewrites; `merges: {keep, drop|merge, why}`, kept pairs
  included with `none`; `fingerprints:`, the output of `gate/scenario-check --fingerprints`
  verbatim.
- **Closing run of a scenario gate:** handbook §9.
- **Implementer:** per-step status (red evidence: the failing test's name and failure line, or the
  recorded mutation for an adapted test; green; refactor), commit message used, focused-test
  result, the state of the `.feature` (armed, or which scenarios still fail), full-suite result
  before hand-off, any deviation from the plan (with reason); with the gate module, the
  `PHASE PASS` line of `gate/dossier-check --phase <worktree>` (→ The gate dossier).
- **Falsifier (each code lens):** first line `tip: <full SHA>` (the commit it read);
  `robust: yes/no`; weaknesses `{severity, where (file:line), why}`; concrete counter-test
  snippets; `treeDiff`, the output of `git -C <its worktree> status --porcelain
  --untracked-files=all`, taken last, so a new file it left shows too. Bias to skepticism: if
  unsure, flag it.
- **Doc reviewer:** first line `tip: <full SHA>` (the commit it read); `verdict: PASS|FAIL`;
  findings `{rule: DOC-n, where, what, fix}`; `evidence:`, each command it ran with its raw output
  verbatim.
- **Reviewer:** at the feature gate, first line `tip: <full SHA>` (the commit it read); at the
  scenario gate, `scenarioCheck:`, the output of `gate/scenario-check` verbatim; `verdict:
  PASS|FAIL`; findings `{category: process|cleancode|solid|hexagonal|tests|security|docs, where,
  fix}`, one per line; on a FAIL, the minimal set of changes to PASS (after round 9 of a scenario
  gate, the mandatory folds, handbook §9); ready-to-paste `process-learnings.md` row if a process
  rule slipped.
<!-- MODULE gate -->
- The doc-reviewer's report also carries its `findings:` and `sweep:` lines, `none` where it found
  nothing, and each report's verdict stands alone on its line, exactly one per report (→ The gate
  dossier).
<!-- /MODULE gate -->
<!-- MODULE security-lens -->
- **Security falsifier:** the code lens's fields, and beside them `requests: <n> of 120`; each
  weakness with the requests that proved it; `notTouched:`, the attack classes the diff does not
  reach; `notExercised:`, the touched classes it could only read in the code; a counter-test per
  weakness that fails today at the HTTP boundary, in the suite's own form.
  - The dossier check refuses a report without its `requests:` line, with a `requests:` line in
    another form or with anything numeric after its `of 120`, with a count above 120 anywhere in
    it, or without its `notTouched:` or `notExercised:` line (→ The gate dossier).
<!-- /MODULE security-lens -->

<!-- MODULE gate -->
## The gate dossier

- **What it is:** a feature gate's evidence, gathered before a panel round is spawned and completed
  by its reports.
- **The check:** `sh gate/dossier-check <abs strand worktree> <tip>`, run from the primary
  checkout, refuses a dossier that breaks a rule below; each rule is a leg of the check, and a
  refusal names its leg in brackets.
  - Its values (test paths, runner lines, the read paths of the lenses) live in the ADAPT block at
    its top.
  - A PASS shows evidence that is consistent, not a run that happened.
    - Why: every record of the dossier is text the gated agent writes.
  - It judges by the ADAPT values and the breach guard of the checkout it runs from.
    - So `main`'s copy runs, as `sh gate/dossier-check <abs strand worktree> <tip>` from the
      primary checkout: a strand's own copy may have changed them.
  - `gate/self-test` is its proof, run by `pre-commit` whenever a commit touches `gate/`.
    - A named limit: its `PATH` shims split at a drive colon, so a `TMPDIR` such as `C:/…` on a
      Windows host fails its stand-in cases loudly.
  - A named limit: a relative worktree argument fails the check closed where it is shaped like
    `name=value`, which `awk` reads as an assignment, and where the strand needs the security
    report, whose wrapper hash then resolves the path twice; give the worktree as an absolute path.
- **When:** every piece of evidence but the reports is in place before a round's gate agents are
  spawned; the round closes only on a PASS once every report is in, and that PASS line opens the
  hand-off to the author.
- **Who runs it:**
  - the orchestrator, before it spawns a round and once it has saved `reviewer.md`;
    - before the spawn, a `lens` refusal of a report the round is to write is expected, and every
      other refusal is cleared first;
  - the reviewer, as its first act, on the strand worktree, beside the lenses, in the same form
    from the primary checkout;
    - a refusal of class `lens`, `report`, `finding` or `tip` that names a report the round
      spawned beside it concerns that report: the reviewer quotes it, it is not the reviewer's;
    - the refusal of the missing `reviewer.md` is its own report's, which the orchestrator saves
      once the reviewer returns, then runs the check again;
    - in a split round 1 the refusal of the missing `doc-reviewer.md` concerns the split
      doc-reviewers;
    - every other refusal is a `process` finding of a FAIL, and the reviewer's findings are traced
      as `reviewer-<n>`;
    - among them: a refusal naming a report of an earlier round, and a `finding` refusal about the
      disposition of an earlier round's finding (`fixed` at no strand commit, a backlog row the tip
      lacks, a rejection without its reason);
  - the implementer, as `gate/dossier-check --phase <worktree>`, before it hands back a phase or a
    fold, which runs the base leg's history refusals (stand-ins, `[wip]` and merge commits,
    test-changing commits of one subject, the breach guard) and the red, mutation and new-test
    legs over `base..HEAD` alone.
- **The PASS line** names every leg that is off: the security leg while neither the base nor the
  tip holds `gate/authentication-surface`, or when the strand removes the list its base holds (as
  off after the strand where the base's list still calls for the report), and a walk, docs-guard
  or `@wip` leg its ADAPT value switches off.
  - It names every top-level report whose own verdict is `FAIL` or `robust: no`, as
    `reports that do not pass: <file> (<field>: <value>)`.
    - Why: such a report passes the check where each of its findings is traced, as after the
      round cap (handbook §9).
    - A named limit: a part report of a split round 1 (`round1/doc-reviewer-<part>.md`) is
      never named there.
  - It names the number of findings traced as rejected, as `rejected findings: <n>`.
  - A line that names neither has no such top-level report and no rejected finding.
- **Location:** `<strand worktree>/gate.local.d/`, ignored by git (`*.local.*`), so no build
  cleans it and the worktree's removal takes it along; a dossier elsewhere, or a symlink in its
  place, is refused, and so is a dossier git does not ignore (`ignored`).
  - A named limit: a symlink in place of `red/`, `walk/` or `mutation/` inside it is not checked.
- **Base and freeze** are read live:
  - the strand's merge-base is the tip of the block's `base_ref`, `refs/heads/main`;
  - a ref or a file in the common git dir that could stand in for either (handbook §7's list) is
    refused, and so is a `[wip]` commit in the range;
  - a `main` or `origin/main` that git resolves, at exit 0, to other than `refs/heads/main` or
    `refs/remotes/origin/main`, an empty, ambiguous answer included, is refused, which catches a
    stand-in no list shows; a name that resolves to nothing passes;
  - a merge commit in the range is refused by its sha, in the full check and in `--phase`;
  - two commits that change a test path under one subject are refused by their short hashes and
    that subject, in the full check and in `--phase`, before the red leg: a red record names its
    commit by subject, so reword one (handbook §7);
  - the check runs `sh <its checkout>/hooks/breach-guard --reaches <tip>` from the strand worktree
    (hooks/README.md, `--reaches`):
    - exit 1 is refused with the guard's first stderr line;
    - a missing guard, or any exit but 0 and 1, is refused with the exit status and that line,
      never skipped;
  - the tip's tree differs from the base's, and the worktree is clean and checked out at the tip;
  - `--base <commit>` judges a strand stacked on one not yet landed from that commit, which must
    contain the base ref's tip.
    - A named limit: the check does not hold that commit to be the tip of another strand.
    - It judges no commit below that commit.
    - So the reviewer confirms that the named commit is the tip of the strand below.
- **Top-level reports, runs and `findings.txt`** open with `tip: <full sha>`, a full sha being 40
  hex digits, or 64 in a SHA-256 repository, as does every other file at the dossier's top level;
  one naming another tip is refused, unless:
  - it is a lens report, and no path the lens reads changed since its tip, on the strand or on
    the base (a `reads_` value names them; the robustness lens and the security lens read every
    path outside `*.md`);
    - Why: a verdict on unchanged code still holds, and a re-run costs a full lens.
  - it is the doc-reviewer's or the reviewer's passing report or a run, and only
    `docs/backlog.md` changed since, in rows (`| **<ID>** |`) `findings.txt` traces as
    `backlog <ID>` or in the doc-debt bullets of a file it traces as `debt <file>` (author
    decision 2026-10-13).
    - Why: a row that only records the passing round's own lows needs no second review.
  - Evidence that predates the tip lives in subdirectories.
- **Reports** are each agent's final message verbatim (→ Handoff contracts):
  - saved as `lens-<lens>.md` per falsifier lens (`lens-security.md` for the security lens),
    `doc-reviewer.md` and `reviewer.md`;
  - a report's own verdict is its one line that opens with `robust:` or `verdict:`, after a list
    marker, bold or a backtick, and reads `yes`, `no`, `PASS` or `FAIL` alone;
  - a choice, an echo of the contract or a verdict quoted further into a line reads as none;
    - Why: a delta-round report quotes the round before it.
  - a report without its own verdict line, or a lens report without its `treeDiff:` field, is a
    summary and is refused.
- **Required reports** follow the strand's diff: the doc-reviewer's and the reviewer's always, the
  robustness lens's for any path outside `*.md`, another lens's for a path its `needs_` value
  matches.
  - A renamed path counts at both its ends, so code renamed into the docs still needs its lenses.
  - A name git quotes counts without its quotes.
  - The security lens's report is required for a path that starts with a line of
    `gate/authentication-surface`, a file of the repository.
  - The check reads that list as the base and as the tip hold it, and a path that matches a line of
    either calls for the lens.
    - So a strand that adds a path and its line together, or deletes a line, is held by the list on
      the other side.
  - A `*.md` file under a listed directory alone calls for no security lens.
  - While neither the base nor the tip holds the list, the leg is off and the PASS line says so.
  - Where the base holds the list and the strand removes it, the PASS line names the leg off from
    here; where the base's list still calls for the report, the strand needs it, and the PASS line
    names the leg off after the strand.
  - It reads each line without its trailing blanks, tabs and carriage return, and skips a blank
    line.
  - In the full check, not in `--phase`, it refuses a line of the tip's list that starts no path
    tracked at the tip (`surface`), and a list at the tip that names no path (`surface`).
  - It refuses a strand that needs the security report and changes `gate/lens-request` in any way
    against its base, and in a stacked check against the base ref's tip too (`lens-request`): its
    content, its mode, a link in its place, a rename or a delete.
    - The lens runs the strand's own wrapper, so such a change lands in a strand of its own, which
      calls for no security lens.
    - Where the strand needs the report, the worktree's `gate/lens-request` must be the tip's byte
      for byte, read with git's filters off, so an edit `git status` does not show (behind
      `skip-worktree`, `assume-unchanged` or a clean filter) is refused; where the tip tracks no
      wrapper, the worktree must hold none.
  - A security report also carries `requests: <n> of 120`, every line that opens with `requests:`
    in that form with nothing numeric after `of 120`, `notTouched:` and `notExercised:`, each of
    the last two with `none` where it has nothing to name; one without them is refused
    (→ Handoff contracts).
    - The budget is the largest count anywhere in the report, read as bytes and in any case:
      `<n> of 120`, `<n> out of 120` or `<n>/120`, with markup around `of` and a separator before
      each group of three digits; it is at most 120.
    - Named limits of that reading:
      - a `notTouched:` or `notExercised:` line passes on its label, with nothing after it;
      - a count written otherwise (in words, `1 000 of 120`, markup inside its digits, in prose)
        is not read, nor a second count one character after another (`5/120 130/120`);
      - a port before a path that starts with `120` (`localhost:8080/120/orders`) reads as a count
        of 8080 and refuses the report.
  - Its findings are traced as `security-<n>`.
- **A split round 1** (handbook §9) writes no top-level `doc-reviewer.md`:
  - each part's report, verbatim, is `round1/doc-reviewer-<part>.md`;
  - a doc's part is its path without `.md` and without a leading dot, each `/` read as `-`
    (`docs-knowledge-base-engineering-handbook`), and the range's part is `range`;
    - a named limit: a part named like a report (a root `reviewer.md` gives `reviewer`) has its
      findings counted under that report;
  - `findings.txt` traces the split findings as `<part>-<n>` (author decision 2026-10-15),
    numbered per report, for the rest of the gate;
  - the check tests their dispositions as any other;
  - the count of `doc-reviewer-<n>` leaves them out;
  - the reviewer counts each part's findings against its `<part>-<n>` lines;
  - after a fold, round 2's delta run writes the top-level `doc-reviewer.md`; with no fold, one
    `doc-reviewer` on its own default writes it at the tip under review, after the lows' row of
    "A passing round whose findings are all low" (handbook §9) if there is one, and that run
    belongs to round 1 (author decision 2026-10-16).
- **`red/`:** one record per commit that changes a test path (a path `test_path_re` matches:
  `src/test/`, a `test/` or `tests/` directory, `*.test.*` and `*.spec.*` scripts, a `self-test`,
  `release/test_*.py`), whatever its tag, opening with `commit: <subject>` of a commit in the range
  (stable across rebases):
  - then `tree: <sha>` of the state the run read and a `blob: <sha> <path>` line per test file the
    commit adds or changes, written before the run, then the failing run;
  - the `tree:` line names its tree by a full hex id alone, as a walk log's does, in a `mutation/`
    record too;
  - each blob is a test file's, the one the tree holds at that path and the commit holds;
  - a line opening with `blob: ` holds the sha, one blank and the path, and any other is refused:
    an empty one, or a path with a tab or with a blank at either end;
  - or an `exempt: <reason>` line in place of all three, for a test refactoring, whose failing
    lines count for no test;
  - a second record of a commit is refused;
    - its subject is the key, so two commits that change tests share none (→ Base and freeze);
  - a record that shows a diff of its commit's own production code, or a diff hunk without its file
    header, is refused: a mutation belongs under `mutation/`;
  - a named limit: the check reads the paths a commit changes as git prints them without `-z`,
    quoted where a name holds a double quote, a backslash or a control character;
    - so a commit whose test file has such a name is asked for no `red/` record, and a red record's
      diff of a production file with such a name is not refused;
    - and a doc with such a name no longer ends in `.md` and reads as a code path, so the walk leg
      asks a log for its commit (backlog **G96**).
- **`mutation/`:** one record per test that cannot start red: the mutation's diff, its `tree:` and
  `blob:` lines (the test file at the tip, held by that tree), the failing run and the revert; a
  line `older: <reason>` claims a new test over behavior older than the strand.
- **New tests:** each test the strand adds (one a file declares more often at the tip than at the
  base, a renamed file read under its old path) shows on a failing line, as its runner prints it,
  in a record whose `tree:` and `blob:` lines the check has proven.
  - A test moved to another file or renamed reads as new.
  - The check refuses a Java test annotation it cannot read to its end, a vitest test whose title
    is no string literal, and a file its extractor fails on (`new-test`).
  - The record is the `red/` record of a commit that changes tests, or a `mutation/` record with
    its `older:` claim.
  - Its failing lines count only for the test files its `blob:` lines name, each followed through a
    later move to its path at the tip.
  - The readers on are the block's `test_readers`: JUnit under Gradle, vitest, the `sh` self-tests,
    and unittest or pytest.
    - The JUnit reader takes a test annotation that opens a line or stands behind other
      annotations on that line (`@Tag("fast") @Test`).
    - A self-test's failing line is `FAIL <name>`, and it may end in one bracket that names the
      case (`[<label>]`, `[case <name>]`).
  - A test file whose reader is off adds no new test.
  - The check proves that such a line exists, not that the test fails for the right reason; that
    judgement stays the reviewer's (handbook §4).
  - A named limit: a test a reader does not see adds no new test either, so no red run is asked of
    it.
    - The `sh` reader sees only literal `check "<name>"` lines, not a case declared through a
      helper, and reads the `<<` of `$((1 << 2))` as a here-document, skipping the lines after it.
    - The unittest and pytest readers see a class only where its own header names `TestCase`.
    - The new-test leg reads JUnit tests in `*.java` and vitest's bare `it` and `test`; a Kotlin
      test or a fixture test made by `test.extend` is not read, so its red record is the
      reviewer's to check.
- **`walk/`:** one `./verify all` log per code tree (the commit's tree without `*.md`), at the path
  `--walk-log <worktree> <commit>` prints, opening with `tree: <sha>` of a tree that holds the
  commit's code tree, whose last line that opens with `[gate/walk] ` is exactly
  `[gate/walk] verify exited 0`, and showing the block's walk proof lines (`BUILD SUCCESSFUL`, bare
  `> Task :test` and `> Task :frontend:npmTest` lines and a non-zero
  `[verify] executed test classes`).
  - A log without the tree line, with a tree the repository does not hold, with another commit's
    code tree or without that last line is refused, each for its own reason.
    - So a log of a build that failed, or left the walk worktree moved or dirty, after that line
      is refused.
  - A commit whose code tree is unchanged since the last walk, or since `main`, reuses that proof,
    so after a fold only the changed trees run again.
  - The walk leg and `--walk-green` judge a log by one definition.
  - `<abs worktree>/gate/walk <strand worktree> <walk worktree>` writes those logs: it builds each
    commit of the strand's range, from its merge-base with the block's `base_ref`, whose code tree
    has no green log yet, in a detached walk worktree (`<root>/<topic>-walk`).
    - It cuts the walk worktree anew for every commit it builds: `git worktree remove`, then
      `git worktree add --detach` at that commit.
      - Why: nothing an earlier build or an earlier walk left there serves the build, the files
        git ignores included, so a commit that is not green alone gets no proof log.
      - Every build therefore starts cold.
      - It calls `./verify stop` in the worktree before it removes it.
      - It refuses, naming git's reason, where git cannot remove the worktree.
      - A named limit: git removes no worktree in which a build initialized a submodule without
        `--force`, which the walk does not pass, so the walk of such a strand ends at its second
        build.
    - Its own line `[gate/walk] verify exited <n>` opens a line of its own, also behind a build
      output that ends without a newline.
    - It prints one line per commit and exits non-zero when a build failed.
    - A commit whose code tree is the base's is marked `unchanged from the base` and not built.
    - A log is green when `gate/dossier-check --walk-green` says so, which holds it to the tree
      line, a tree sha that holds the commit's code tree, `gate/walk`'s last line reading
      `verify exited 0` and the proof lines of the block; an older log that is not, one from
      before that line was required included, is built again.
    - A failed build's log goes to `<log>.failed`, never to the proof path.
    - The test reports a failed build leaves, at each glob of the block's `walk_failed_reports`
      (`build/test-results/test/*.xml`), are copied to `<log>.failed-reports/` before the next
      commit replaces them.
      - The globs are separated by blanks, one per stack of a composed project, each below the
        walk worktree.
      - Reports of two globs that share a file name keep the later glob's.
      - A value that is empty keeps none.
      - Only a report written during that build is copied.
        - A named limit: the build's stamp lies in the dossier and the reports in the walk
          worktree, so where the two sit on file systems of different timestamp granularity, the
          comparison crosses clocks.
      - A report that is a link, or whose directory, resolved with its links, lies outside the
        walk worktree, is not copied.
      - A glob with a `..` segment, or a segment of a dot and glob characters only (`.*`, `.?`,
        `.[...]`), which may match `..`, is refused before any build.
      - The copies beside the log outlast the walk worktree, which the next build cuts anew.
      - It refuses when the build's stamp cannot be made.
      - It refuses when file times do not advance within its bounded wait.
      - The directory holds only the latest failed build of that code tree.
      - A green rebuild of the tree removes it together with `<log>.failed`.
    - It takes only a walk path that ends in `-walk` and holds no `.` or `..` segment.
      - A named limit: the `-walk` shape is fixed in `gate/walk`, so renaming the walk shape
        (handbook §7) changes the script too.
      - It refuses one that is a file.
      - It refuses one that is no worktree's top level.
      - It refuses one that has a branch checked out.
      - It refuses one that is not clean, an untracked file included whatever git is set to show.
      - It refuses a new one inside another worktree, unless it lies in the ignored
        `.claude/worktrees/` of the primary checkout.
      - It makes the parent directories of a new one.
    - A build that leaves the walk worktree moved or dirty counts as failed and ends the walk.
    - It takes a lock for the strand's `walk/` directory and one for the walk worktree, so a second
      walk of the strand, or of another strand on the same walk worktree, refuses.
      - A lock is a directory made by `mkdir`, with its owner's process id inside.
      - A lock whose owner no longer runs is taken over.
      - A lock that names no owner is left to be removed by hand.
      - A refusal on a live owner says how to clear a lock whose process id is no run of the gate.
      - Named limits: a run of another user reads as a dead owner (`kill -0` answers EPERM), so
        its lock is taken over, and three takers of one stale lock at once can nest a lock.
      - A named limit: a lock is released without a look at its owner, so a run whose lock was
        taken over removes the taker's lock when it ends.
      - A named limit: a process id that lives again after a restart holds the lock until the
        author removes it, since the deny floor refuses an agent's `rm`.
    - It calls `./verify stop` when it ends, so no build tool keeps the walk worktree busy.
- **`kb-guard.log`:** whenever a commit is proven by a run on other docs than its own (a docs-only
  fold, a `[doc]` commit), a `./verify focus` run of `KbCitationGuardTest`, the block's
  `docs_guard_test`, at the tip, since that test reads the KB prose.
  - A run the build reports up to date counts only where the build declares the docs the test reads
    as its inputs, which the block's `docs_guard_ran_re` then admits.
  - The check takes the log's `./verify focus` line only for a path whose last segment is exactly
    `<docs_guard_test>.<ext>`, so a run of a file that only holds the name proves nothing.
    - A named limit: the check puts `docs_guard_test` into a regular expression as it stands, so a
      name holding `+`, `*`, `(`, `|` or `.` misreads a log, and `gate/tip-evidence` reads a `[`
      in it as a pathspec glob.
  - A named limit: the check and `gate/tip-evidence` want a line of `docs_guard_ran_re` and one of
    `walk_success_re` in the log, so a runner whose focused run prints no walk success line cannot
    pass the leg.
- **`findings.txt`:** one line per finding the reports raise, numbered per report (`<lens>-<n>`,
  `doc-reviewer-<n>`, `reviewer-<n>`):
  - a finding is a list item that opens with `{severity:` in a lens report, `{rule: DOC-` in the
    doc-reviewer's (its sweep's included) or `{category:` in the reviewer's, one per line, numbered
    1..n in the report's order, each traced once; a report whose own verdict is no readable
    `robust: yes` or `verdict: PASS` lists at least one;
  - each is `fixed <strand commit>`, `backlog <ID>` (a row `docs/backlog.md` holds at the tip),
    `debt <file>` or `rejected <reason>`; a commit is named by 7 to 64 hex digits; anything else is
    refused;
  - a `fixed` commit is neither the tip its raising report's `tip:` line names nor a commit below
    it;
    - Why: a commit that report already had before it, its own tip included, cannot answer its
      finding.
    - So a finding of a report at the tip under review is never `fixed`: the report is run again
      at the fixed tip.
    - A named limit: a lens report that stands from a tip before a rebase holds no fix to its
      tip, since that tip is no ancestor of the rebased strand, so the rebased copy of a commit it
      already had passes as a fix.
  - a `rejected` finding of a report whose own verdict is `FAIL` or `robust: no` names in its
    reason the date of an author row of `docs/open-questions.md` at the tip: a table row that
    opens with that date and holds `author decision`;
    - Why: the party the gate judges writes the dispositions.
    - The other way is that report's re-run with the rejection in its brief.
    - A named limit: the check holds the date and the origin, not what the row decides, which
      the reviewer reads.
  - a finding of a split round 1's part has no top-level report, so neither rule is held to it;
  - a rejection that names a test file names its line.
- **`bdd.log` and `wip.expected`** (the `@wip` leg, mechanical for Cucumber-JVM under Gradle), for
  a strand that changes a path under `src/` or `frontend/` (the block's `bdd_paths_re`):
  - the `./verify bdd` run at the tip, and one line per expected failure,
    `<the failing line without FAILED> | <its failure line>`, the failure line never empty;
  - every scenario tagged `@wip` at the tip (on itself, its feature, its rule or one of its
    `Examples`), by its feature and name, is among them, and no other;
  - the log holds a bare `> Task :test` line, and shows `BUILD SUCCESSFUL` or a test summary that
    counts its FAILED lines, and those failures are exactly the listed ones, each with its line.
  - The `@wip` reader knows the structural keywords of English and, under `# language: de`, of
    German; it reads no step.
    - German feature: `Funktionalität`, `Funktion`.
    - German rule: `Regel`.
    - `Rule` is read under `# language: de` as well.
    - German scenario: `Szenario`, `Beispiel`.
    - German scenario outline: `Szenariogrundriss`, `Szenarien`.
    - German examples table: `Beispiele`.
    - English feature: `Feature`, `Ability`, `Business Need`.
    - `Szenarien:` opens a scenario outline.
    - The English `Scenarios:` opens an examples table.
    - The language line counts where Gherkin reads it: before the first tag or keyword line.
    - A byte order mark before the first line is dropped.
    - A feature in any other language is refused by name (`wip`), never read as a file without
      `@wip` scenarios.
    - So is a feature file git cannot show.
    - A scenario of a feature without a name is kept: its name alone is looked for in
      `wip.expected`.
    - A name is matched as written, a backslash in it included.
    - A named limit: a scenario without a name is not listed, so one tagged `@wip` is asked of
      no `wip.expected`.
    - A doc string ends at the delimiter that opened it, `"""` or three backticks.
    - A feature whose doc string never closes is refused by name (`wip`).
    - So is a feature file with no feature line the reader knows, a German one without its
      `# language: de` line included.
      - An empty file, or one of comments and tags alone, is such a file too: it gets a feature
        line or leaves the strand.
  - A commit whose changed paths git cannot list is refused (`red`), never read as a commit that
    changes no test.
  - `<abs worktree>/gate/tip-evidence <strand worktree>` writes `bdd.log` and `kb-guard.log` at the
    tip, each opening with `tip: <sha>`.
    - With no `@wip` scenario it writes a `wip.expected` that names the tip alone; while one is, it
      keeps `wip.expected` for its expected failures and says so.
    - A block with `bdd_leg='off'` gets neither `bdd.log` nor `wip.expected`.
      - It removes an older one.
      - The output names both.
    - A block with no `docs_guard_test` gets no `kb-guard.log`; the output says so.
    - The docs guard test is the one file `<docs_guard_test>.<ext>` the strand holds, in any
      directory outside `.template/` (author decision 2026-11-23).
      - Why: the template baseline holds a preset file of that name.
      - It lists the files NUL-separated, so a path holding a quote or a backslash reaches `verify`
        as it is.
      - It refuses, with exit 2 and before any run, a strand that holds more than one.
      - It exits non-zero when the strand holds none.
    - It refuses a strand worktree that is not clean, an untracked file included.
    - It exits non-zero when the docs guard run fails or shows no line matching the block's
      `docs_guard_ran_re` or `walk_success_re`, which is how the check judges that run.
    - A strand that moves or gets dirty during the runs keeps neither log.
    - It takes a lock for the strand's dossier, so a second run on the strand refuses, and calls
      `./verify stop` when it ends.
    - It reads `@wip` through `gate/dossier-check --wip <worktree> <commit>`, which prints the
      `@wip` scenarios as the check reads them and refuses a commit it cannot read.
      - That listing exits 2 and names the file where a feature cannot be shown or is written in
        a language the reader does not read.
      - `gate/tip-evidence` then refuses and leaves `wip.expected` as it is.
- **The helpers** `gate/walk`, `gate/tip-evidence` and `gate/scenario-check` read their values from
  the same block through `gate/dossier-check --value`, so one edit changes the check and the helpers
  together, and each refuses with a named message where `awk`, `git` or `mktemp` is missing.
<!-- /MODULE gate -->

<!-- MODULE gate -->
## The scenario-gate dossier

A scenario gate's reports, held so a check can tell which features each agent read.
`gate/scenario-check` judges them; its proof is `gate/self-test`.

- **Location:** `<strand worktree>/gate.local.d/scenario/`.
  - The feature gate's check reads no file under `scenario/`, so the two dossiers do not mix.
- **Reports:** `falsifier-scenario.md`, `falsifier-stakeholder.md` and `reviewer.md`, each the
  agent's final message verbatim.
  - An earlier round's reports move to `scenario/round<n>/`.
- **Fingerprints:** `<abs worktree>/gate/scenario-check --fingerprints <abs worktree>` prints one
  `fingerprint: <git hash-object> <path>` line per `.feature` under the block's `features_dir`,
  `src/test/resources/features/`.
  - It hashes the working tree, so an uncommitted draft counts.
  - It refuses, with exit 2:
    - a worktree that is no git checkout;
    - a `features_dir` that is no directory;
    - a feature path that holds a line break.
- **`<abs worktree>/gate/scenario-check <abs worktree>`** compares each report's fingerprints with
  the features as they are now. It refuses:
  - a missing falsifier report;
  - a report without its own verdict line, `soundSpec:` in a falsifier's and `verdict:` in the
    reviewer's, read as the feature gate's check reads a report's verdict;
  - a report with no fingerprint line;
  - a report naming a feature at another fingerprint than the feature has now;
  - a report naming a feature that is gone;
  - a report naming a file that is no `.feature` file under `features_dir`;
  - a report that does not name a feature present now;
  - a `reviewer.md`, where there is one, that breaks any of these.
- **What counts as a fingerprint line:** one that opens with `fingerprint:` and a full hash, after
  indentation or one list marker (`-`, `*`, `+`).
  - Blanks at its end are dropped.
  - A quoted or abbreviated one counts for nothing.
  - A full hash is 40 hex digits, or 64 in a SHA-256 repository.
- **Its output:** the fingerprints it compared, then a last line.
  - A pass ends on `scenario-check: PASS on <n> feature(s);`.
  - Without a `reviewer.md` the pass line goes on `no reviewer.md yet`.
  - With one it goes on `reviewer.md judged the same`.
  - A pass says that every report read the features as they are now, not that every report
    passed.
    - A report that says `soundSpec: no` or `verdict: FAIL` is named behind that:
      `; reports that do not pass: <file> (<field>: <value>)`.
- **Who runs what:**
  - each falsifier runs the fingerprint listing as its first act, on the worktree it reads;
  - the reviewer runs the check as its first act, both falsifier reports saved, and a refusal stops
    the gate;
  - the orchestrator runs the check once `reviewer.md` is saved;
  - only a pass line that holds `reviewer.md judged the same` takes the scenarios to author gate
    1 (the pass rule: handbook §9).
<!-- /MODULE gate -->

## Environment gotchas (tell every agent)

- **Sessions share the machine.** Every build goes through `./verify`, in a gate agent's worktree
  too: it queues on one machine-wide lock and may first wait out other sessions' gate builds, longer
  than a foreground tool call's 10-minute cap, so a `./verify all` runs in the background. Daemons
  are stopped with `./verify stop`, never `./gradlew --stop`
  ([dev-environment.md](dev-environment.md), "Several sessions on one machine").
- **Bash stdout** may not surface on the Windows host: redirect to a file
  (`./verify all > out.txt 2>&1`) and `Read` it; in-container, Bash is normal.
- Permission-blocked commands + reset-free rewrite: see the FAIL-loop section. Prefer Read/Grep/Glob.
- **A refused command is no obstacle to get around:** working-with-ai-agents.md, "Refusals and
  limits", which says when a rewrite or another route is right and when the step is skipped and
  named in the report.
  - Why: a refused effect reached another way is a breach the report does not show.
- **Edit only what the brief's task needs:** working-with-ai-agents.md, "Session playbook",
  step 2; what else looks wrong goes into the report.
  - Why: an edit nobody asked for reaches the review inside a diff about something else.
- **Remove or stop only what is the workflow's own:** the strand's scaffolding at its cleanup
  point ("Gateway hand-off and cleanup (the agent's side)"), the walk or lens worktree you were
  given or created, a container or process you started.
  - What another agent or session may still hold is left alone and reported.
  - Why: a sibling's run, branch or container looks like a leftover from the outside.
- **Never overlap a tree-mutating agent with another agent's build.** The falsifier writes probes and
  runs Gradle; a concurrent run on the same tree reports phantom failures and the two corrupt each
  other's `build/` state. Every gate agent of a round gets its own detached worktree (→ Cadence),
  so the round's agents run side by side. (The doc-reviewer only reads docs and runs no build.)
- **Start a run only once the tree is the state you intend to measure.** While edits are in flight,
  no run is evidence of anything, and a run started against a half-edited file measures a tree
  already known to be broken.
- **Probes live in the gitignored `src/probe/java`**, run via `./gradlew probe` and get cleared with
  `./gradlew clearProbes`, never with `rm` (→ [build-run-deploy.md](build-run-deploy.md),
  handbook §7): outside `check`/`build` and outside git, a leftover probe breaks nothing.
- **Copies of production data stay in the worktree's own `build/`**, never in `/tmp` or a session
  scratchpad, so they go with the worktree ([testing.md](testing.md), the production-snapshot
  harness).
- Focused tests during micro-steps (`./verify focus <path>`), the `@wip` scenarios via
  `./verify bdd`; the full `./verify all` before the gate, cache disabled, executed-proof read
  ([testing.md](testing.md)).
- **The shell's working directory drifts between worktrees.** With a strand worktree checked out
  beside `main`, a Bash call can silently run in the wrong one, and a relative path then reads or
  writes the wrong tree with no error at all.
  - Address the target explicitly in every call: `git -C <absolute path>`, absolute paths for
    reads, writes and Gradle.
  - Run `git status --short` in **both** worktrees before a gateway claim.
  - `git show <sha>` keeps working from either place (worktrees share one object database), so a
    file check that disagrees with `git show` is a directory problem until proven otherwise.
  - Before the **first** edit of a session, require `git rev-parse --is-inside-work-tree` to answer
    `true`.
  - A drifted Gradle run is **green**, so an exit code proves nothing: count the test-result XMLs in
    the worktree's own `build/` and check the strand's new test classes are among them, or the
    suite ran without the change.
  - Mechanical enforcement is still open (backlog **G7**).
- **Run a worktree's build by its absolute path:** `<abs worktree>/verify focus <abs path>`, never
  `W=<wt>; cd $W && ./verify …`, which the `cd` guard denies (working-with-ai-agents.md,
  "Permissions & autonomy").
  - Every brief in `.claude/agents/` that runs a build says so.

## Worked example (the storage-value evaluator feature)

1. **Specify:** *Given* a member deposited goods of a catalogued item, *when* a member
   opens the overview, *then* the member's storage sum shows the item's storage value times quantity and
   quality. Drafted as a `.feature`, gated by the scenario gate, **confirmed by the author as
   complete**, committed `@wip`.
2. **Plan:** test list for `EvergoreDataEvaluator` storage calc:
   - ① place value = Σ storageValue·qty·quality/100;
   - ② withdraw value uses withdrawlValue;
   - ③ unknown item → UNDEFINED+log;
   - ④ an item with market value 0 is reported as zero-valued;
   - the plan approved; the agent words the messages.
3. **Implement:** four red→green→refactor micro-commits with those messages; the one that turns
   the scenario green also removes `@wip`, and a refactor commit tidies the evaluator under that
   net.
4. **Falsify and review, one round side by side:** domain lens recomputes ① ② by hand; robustness lens: does ③ assert the log? quality=0 edge? is green real if the production line is reverted? → counter-tests; doc-reviewer: `domain-model.md`/`testing.md` updated, backlog row removed?; reviewer: boundaries clean, messages one-line verb-first? → PASS once the dossier check passes.
5. **You, author gate 2:** the rebased tip with its compare range on your review; you land it
   with `/land <tip>` on its pull request (agent seat) or `git push origin <tip>:refs/heads/main`.

## Evolution

- Stay lean (1 implementer, the scenario gate, the per-surface lens panel, doc-reviewer, 1
  reviewer).
- Widen the panel + majority vote for high-risk changes.
- **Workflow tool** only for occasional large parallel audits (explicit opt-in), never for the
  interactive, human-gated commit loop.
