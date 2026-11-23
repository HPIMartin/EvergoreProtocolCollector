# 10: Multi-Agent Playbook

How this project is developed with a small team of AI agents: **you + I plan; cheaper agents
implement; independent agents try to break it and gate the commit.** The single-agent ground rules
live in [working-with-ai-agents.md](working-with-ai-agents.md).

## Roles

| Role | Who / model | Spawned? | Input → Output |
|------|-------------|----------|----------------|
| **Planner** | You + me (the main-session model), in chat | No, it's a *hat* | Backlog item → the feature's **author-confirmed `.feature`** (handbook §5) + an **approved, ordered commit/test list** |
| **Scenario falsifier** | `falsifier-scenario` subagent (frontmatter `model: sonnet`) | Yes (fresh) | A draft `.feature`, before implementation → skeptical verdict on it as a specification + concrete Gherkin rewrites |
| **Stakeholder lens** | `falsifier-stakeholder` subagent (frontmatter `model: opus`; one tier above the lens default, never escalated, see cadence) | Yes (fresh) | A draft `.feature` (or the whole suite, in a catch-up's closing run), read as a product owner would and against every `.feature` file → skeptical verdict on handbook §5's "Reading as the stakeholder" (actor, product language, overlap, time, numbers) + Gherkin rewrites + merge proposals |
| **Implementer** | `implementer` subagent (frontmatter `model: sonnet`) | Yes | Approved plan → executes red→green→refactor per step, **commits each locally** with the plan's message (or its own where the plan names none), arms the scenarios → summary |
| **Falsifier panel** | `falsifier-domain` + `falsifier-robustness` subagents, plus `falsifier-frontend` for a UI surface (frontmatter `model: sonnet`); spawned **per touched surface** (see cadence) | Yes (each fresh) | The feature diff/commits → adversarial verdicts (domain correctness · test honesty/robustness · UI honesty) + concrete counter-tests / weaknesses |
| **Doc reviewer** | `doc-reviewer` subagent (frontmatter `model: haiku`; a split round 1: see cadence) | Yes (fresh) | The feature diff/commits → PASS/FAIL against the KB README's DOC checklist + findings citing rule IDs |
| **Reviewer / Gate** | `reviewer` subagent (frontmatter `model: opus`) | Yes (fresh, per gate) | Scenario gate: the draft `.feature` + the scenario falsifier's and the stakeholder lens's reports → PASS/FAIL. Feature gate: the feature commits + the panel's + doc-reviewer's reports → PASS/FAIL + findings + proposed `process-learnings.md` entry |

<!-- MODULE security-lens -->
With the security-lens module the falsifier panel also holds `falsifier-security` (frontmatter
`model: sonnet`), spawned only for a diff that touches the authentication surface: it reads the
change as an attacker and returns a way in, or none, from few requests against a running instance
of the tip (→ Cadence).
<!-- /MODULE security-lens -->

**Non-negotiable:** the falsifiers, the doc-reviewer and the Reviewer are **fresh, independent** agents, never the Implementer
checking itself (independence is the whole point). Who lands and who may push:
[engineering-handbook.md](engineering-handbook.md) §7.

## The pipeline (per backlog item)

```
0. SPECIFY (you + me, chat): MANDATORY for any observable behavior (handbook §5)
   - draft the feature's `.feature` in product language, checked against the completeness list
   - scenario gate: spawn `falsifier-scenario` and `falsifier-stakeholder` in parallel, then
     `reviewer` in scenario mode with both reports; fix, re-gate (handbook §9)
   - every re-gate brief carries the refuted list (handbook §9)
   - ► AUTHOR GATE 1: YOU CONFIRM THE SCENARIOS AND THEIR COMPLETENESS (a gate, not a notification)
   - commit the confirmed scenarios tagged @wip (the default run excludes them: the build stays green)
   - the only way past this step is an explicitly claimed exemption: a pure refactoring,
     a [doc]/process commit, or build/infra work
1. PLAN (you + me, chat)
   - the runner's dry run lists the undefined steps: that is the glue work; then a small ordered
     test list → ordered one-line commit messages, step definitions and production code alike
   - the test list names any real-time test (handbook §6)
   - slice the commits BEFORE the first edit (handbook §7): each green and reviewable alone
   - name the gate line: the lenses and their tier (see cadence)
   - ► YOU APPROVE THE PLAN (the ordered commit messages); for a [doc] or build/infra strand the
     go may come first and the plan's review at the gateway (handbook §7)
2. IMPLEMENT (spawn `implementer`) with the approved plan
   - one spawn per phase (handbook §4)
   <!-- MODULE gate -->
   - with the gate module, the phase check `gate/dossier-check --phase` after each phase
     (handbook §4)
   <!-- /MODULE gate -->
   - per step: red (write failing test) → ./verify focus → green (minimal code) → refactor
     → commit locally with the plan's message, EXACT, or a protocol-conform one where the plan
     names none; the scenarios run via ./verify bdd
   - ARM: per handbook §5 step 5
   - REFACTOR the feature under the armed net: no behavior change, each step its own green commit
   - the feature is done when the armed scenarios pass, not when the steps are ticked off
   - never pushes; the architecture's boundary rules hold (see architecture.md)
3. FALSIFY (spawn the lenses the change can break, each fresh, each in its own detached
   worktree at the tip under review; from round 2 on only those the read-path table picks;
   see cadence)
   - domain lens: the project's core calculations and invariants; robustness lens: fake-green
     tests, edge cases, time/concurrency/resources, boundary violations, secrets
   <!-- MODULE security-lens -->
   - security lens (the authentication surface only): a way in through that surface, by few
     targeted requests against a running instance of the tip
   <!-- /MODULE security-lens -->
   - each returns a verdict + proposed counter-tests, and ends with the status of its worktree,
     new files included (a left-behind probe is its own finding)
4. REVIEW GATE (spawn `doc-reviewer`, beside the lenses or after them, then `reviewer` once
   every other report of the round is in; each fresh), once per FEATURE commit
   - doc-reviewer: docs hygiene per the DOC checklist, task-scoped, plus a stateless rotating
     one-doc sweep (pick = commit count mod doc count; see the agent definition)
   - reviewer: process adherence (scenario gate and author gate 1 honored? real
     red→green→refactor with red evidence? arming per handbook §5 step 5? refactor commits
     behavior-free? whitespace separate? commit-msg rules?) + code criteria (clean code, SOLID,
     architecture boundaries); integrates the panel's + doc-reviewer's findings, all of which are
     in its brief
   - PASS → proceed (a round whose findings are all low files them first, handbook §9); FAIL →
     fix and re-gate under the loop rules below; logs a process-learning if a rule slipped
   <!-- MODULE gate -->
   - with the gate module the round closes only on a PASS of `gate/dossier-check`: the reviewer
     runs it last, and the orchestrator again once `reviewer.md` is saved (→ The gate dossier)
   <!-- /MODULE gate -->
5. ► AUTHOR GATE 2 (handbook §7 gateway): sync main (`sh hooks/sync-main`) and rebase onto it, run
   ./verify all on the rebased tip, present the reviewable statement, YOU review the rebased tip
   (its log too, reworded where needed); on your go the landing and the cleanup of the project's
   landing model (handbook §7)
   - a re-rebase after your review: clean and green → landing without a second review; a
     conflict resolution → panel and your review again
   - who may push: handbook §7
   <!-- MODULE agent-seat -->
   - on the seat (`AGENT_SEAT` reads `unattended`) the session that owns the strand pushes the
     rebased tip with `agent/push` before the hand-off
   - the strand's pull request is open by then
   - the pull request's link and the landing there: handbook §7, "Landing models"
   <!-- /MODULE agent-seat -->
```

> **ADAPT:** define the domain lens for your project: the core calculations, invariants, and parsing/IO fidelity that `falsifier-domain` must recompute and attack by hand (keep this in sync with `.claude/agents/falsifier-domain.md`). The robustness lens is project-agnostic.
>
The template ships `falsifier-frontend` as the lens for a **UI surface** (fake-green component
tests, user paths, async determinism, layer boundaries, API contract, each UI rule in a scenario
driven through the real UI); it is spawned only when the project has that surface and the
change touches it.

> **ADAPT:** that lens checks against `docs/knowledge-base/frontend.md`, which `/adopt` creates for
> a project with a UI surface; add a lens per further **surface** the project has (a CLI, a message
> consumer) on the same shape. One agent file per lens, spawned only when that surface is touched.

<!-- MODULE agent-seat -->
- **On the seat the brief is the approved plan,** where `AGENT_SEAT` reads `unattended`
  ([dev-environment.md](dev-environment.md), "Where agents run").
  - The brief names the strands, their proofs and their exemptions.
  - Why: an unattended seat has nobody to wait for.
  - The questions the agent may still ask are [engineering-handbook.md](engineering-handbook.md)
    §9's brakes.
<!-- /MODULE agent-seat -->

### FAIL-loop rules (when the falsifier or reviewer rejects)

A FAIL sends the work back to step 2; which gate agents the next round re-runs, and the bounds of
the loop, are canonical and tool-neutral in [engineering-handbook.md](engineering-handbook.md) §9.
Below is the harness-specific *how* of keeping the history clean while that loop runs:

- **Fold fixes into the commit they belong to; never append "fix review" commits.** The branch is
  unpushed, so rewriting local history is safe and expected (clean up before the landing), so the
  history reads as if the work were done right the first time. **The deny floor stops the plain
  reset recipes** (which commands and spellings: working-with-ai-agents.md, "Autonomy within
  guardrails"), and deleting is the author's act (handbook §7): the agent's own cleanup is
  git-native (`git worktree remove` /
  `prune`, `git branch -d`), which it runs autonomously, and anything else is handed to the author or
  removed by a committed build task. A reset is the author's to run, never the agent's in another
  spelling (working-with-ai-agents.md, "Refusals and limits"). The agent's route is the reset-free
  method that folds a fix into any commit, even a deep one: save the corrected tree on a WIP
  commit, branch off the feature base afresh, then for each logical group write each file back
  with `git show <wip-tip>:<path> > <path>` (not `git checkout`, which is `ask` written plainly and
  meets the classifier behind `git -C`; the raw-blob caveat:
  working-with-ai-agents.md, "Command shapes that keep an auto-mode session unattended"), `git add`
  it and commit, and swap names with `git branch -m`. Drop the stale WIP branch with
  `git update-ref -d refs/heads/<name>` (this recipe's named route for the agent's own scratch
  branch; drop it only once `git diff --quiet <wip-tip> <rebuilt tip>` exits 0, which shows the
  rebuilt branch holds its content). A tip-only fix still
  takes `git commit --amend`; `git rebase -i` has no interactive TTY here. `git branch -m`,
  `git update-ref` and `git rebase` have no allow and meet the auto-mode classifier
  (working-with-ai-agents.md, "Autonomy within guardrails").
<!-- MODULE guard -->
  With the guard module that redirect is refused: in an agent worktree write each file back with
  the approved `git -C <worktree> checkout <wip-tip hash> -- <path>`, in the primary checkout
  through `git show` into the output target and a copy (working-with-ai-agents.md, "The
  shell-shape guard").
<!-- /MODULE guard -->
  <!-- MODULE agent-seat -->
  - On the seat, where the strand's branch is pushed, a rewrite is followed by
    `agent/push <worktree>`, and a reset-free fold by `agent/push <worktree> <remote tip>`
    (handbook §7, "On the seat an agent pushes its own strand").
  <!-- /MODULE agent-seat -->
- **Answer a gate finding by amending the commit that owns it** (scripted `GIT_SEQUENCE_EDITOR`
  rebase-edit + `--amend`, no `fixup!`; where a later pick stops on a conflict, resolve it, `git add`
  and `git rebase --continue`, since `--amend` there folds that pick into the commit before it,
  as observed), and fold added coverage into the commit whose behavior it pins. **Fold at the
  moment of fixing, before the next build**, never at the end of the round: the
  fold is the act of fixing, not a step after it. The gate reads the message list before the diff,
  so a "gate round N fixes" commit is a FAIL on sight; a proposed message that names one hunk of a
  multi-hunk commit is the signal that the fold was skipped, and an "and" joining two clauses hints
  that the commit should split (advice, handbook §7).
  - `pre-commit` refuses an amend at a conflict stop (engineering-handbook.md §7).
  - For a commit the hooks recorded (engineering-handbook.md §7) the amend and `agent/fold` are
    refused, so the fix goes in at the recorded commit's own edit stop.
    - Start it scripted, with `edit` on the todo's first line, the recorded commit:
      `GIT_SEQUENCE_EDITOR="sed -i 1s/^pick/edit/" git rebase -i <recorded commit>^` (probe
      record, row W25), with `--root` in place of `<recorded commit>^` for a recorded root commit.
      - That form runs with the strand's worktree as working directory, as W25 ran; from the
        primary checkout each git command of this recipe takes `-C <worktree>` after `git` (W16
        ran `git -C` with a rebase, W25 the prefix; the two together are not probed).
    - At the stop, `git add` the fix, then run `GIT_EDITOR=true git rebase --continue`, as
      `hooks/breach-guard` prints it.
      - Why `GIT_EDITOR=true`: at an `edit` stop with staged changes a plain continue opens the
        commit-message editor, which an unattended session cannot answer.
    - Why: the sequencer amends without `pre-commit`, and `post-rewrite` checks the result again.
  - Fixes for several earlier commits ride in temporary commits with ordinary messages, which carry
    no `fixup!` subject (`commit-msg` refuses it; hooks/README.md); a scripted todo moves each
    behind its target as a `fixup` line (`agent/fold`, below).
  - `/workspaces/{{PROJECT_NAME}}/agent/fold <absolute worktree path> <hash of the owning commit>`
    folds the worktree's last commit into the owning one.
    - It keeps the owning commit's message and opens no editor.
    - For a commit the hooks recorded it cannot run: `pre-commit` refuses the commit of the fix it
      needs first (above).
    - It folds only in a linked worktree on a branch under its `branch_prefix` ADAPT line (handbook
      §7 names the default `claude/`) or on a detached head.
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
    - The settings allow `Bash(/workspaces/{{PROJECT_NAME}}/agent/fold*)`, with the attached `*`
      and a deny for a `..` step after it (not probed; `/adopt` probes both on the adopter's path).
    <!-- MODULE guard -->
    - With the guard module `agent/bash-shape-guard` approves it with an absolute worktree path and
      a commit hash, so a fold runs unattended.
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

**Cadence (cost vs rigor):** micro-steps run lightweight; the **reviewer gates at the feature
commit**, not every micro-commit. The **falsifier panel** runs at the feature end.

- **Spawn only the lenses the change can break.** A backend-only change gets `falsifier-domain` +
  `falsifier-robustness`; a change confined to another surface gets that surface's lens +
  `falsifier-robustness`; a full-stack change gets all of them. Spawning a lens that cannot see the
  change buys a confident PASS that means nothing. This picks round 1; from round 2 on the
  read-path table below picks the lenses of a delta round (handbook §9).
<!-- MODULE security-lens -->
- **`falsifier-security` joins wherever the diff touches the authentication surface:** a path that
  starts with a line of `gate/authentication-surface`, the surface's one path list.
  - The list names production paths, server and UI.
  - A test file under a listed directory matches its line too, so a change to it calls for the
    lens; a `*.md` file alone does not.
  - It works a fixed attack list, only the classes the diff touches; its brief holds the list.
  - Its budget, its forbidden kinds and its environment are the brief's Budget and Environment
    sections (`.claude/agents/falsifier-security.md`).
  - Every request it sends goes through `gate/lens-request`, which fixes the host and sends the
    path raw.
    - A named limit: the wrapper fixes the host alone, not the instance behind the port, the body
      file's place or the number of requests, so the brief's rules on those rest on the lens.
    - A named limit: a path holding `/..` meets the worktree deny before the wrapper runs, so the
      lens lists that attack under `notExercised:` (working-with-ai-agents.md, "Autonomy within
      guardrails").

> **ADAPT:** the authentication surface: list the path prefixes the lens is spawned for (the
> request filters, the security configuration, login, cookie and token handling, roles, passwords,
> the rate limit, the deploy secrets) in `gate/authentication-surface`, one prefix per line.
> `/adopt` with the module asks for them and writes the file; without the file the dossier check's
> security leg stays off and its PASS line says so.
<!-- /MODULE security-lens -->
<!-- MODULE agent-seat+security-lens -->
- **With the agent-seat module the host of `gate/lens-request` is `localhost` on the unattended
  seat** (`AGENT_SEAT` reads `unattended`), whatever host its ADAPT line names.
  - Why: the tip's container runs on the seat's own Docker daemon.
<!-- /MODULE agent-seat+security-lens -->
- **One detached worktree per lens**
  (`git worktree add --detach <root>/lens-<lens>-<topic> <sha>`, under the worktree root and with
  the scratch name of handbook §7) at the tip under review, named in the brief by its absolute
  path, so two lenses cannot collide with each other or with the planner. Every
  lens that writes a probe or a mutation ends its report with the status of its worktree, new
  files included (`treeDiff`), so a left-behind change shows up in its own report rather than in
  the next build.
- **Every prior verdict is an input to the next gate agent**, not a message to the author alone: the
  falsifiers' and the doc-reviewer's reports go into the reviewer's brief verbatim, and a lens
  report of an earlier tip stands as handbook §9 allows; otherwise the lens is re-run, never quoted.
- **The planner reproduces one quoted command of each doc-reviewer report** before it files the
  report's findings or hands the report on.
  - Why: a retyped or summarized output can carry evidence the command never printed.
- **The "Already decided, out of scope" list** of every lens brief, and the reviewer's, at the
  feature gate: handbook §9.
- **`[doc]`-scope changes skip the falsifier panel** (the doc fast lane): the `doc-reviewer` is the
  gate, and the `reviewer` joins only when process adherence is genuinely in doubt. A docs-only fold
  of any strand runs the same way (handbook §9).
  <!-- MODULE gate -->
  - Exception with the gate module: the `reviewer` joins every `[doc]` gate and docs-only fold too
    (handbook §9).
  <!-- /MODULE gate -->
- **Model tiers are relative to the lens default**, the frontmatter model of the code lenses; the
  planner sets a one-off `model` override per spawn, and the frontmatter defaults stay unchanged:
  - a lens runs on the lens default unless its gate escalates, and never more than one tier above
    the lens default;
    - Why: an escalated gate then has a tier to go to whatever tiers the plan offers;
  - the reviewer and the stakeholder lens run one tier above the lens default;
  - the doc-reviewer runs one tier below it;
  - a delta round spawns its lenses and its reviewer on the lens default, escalated or not.
  - Why: model names change; the tiers stay.
- **Escalation rule:** for features touching the project's high-risk areas, the planner spawns the
  gate agents of a full round one tier above the lens default; the reviewer already sits there and
  is not raised, and the stakeholder lens is never escalated.
- **The plan names the gate line** before the first lens is spawned: the lenses and their tier.
  - Why: a first round that should have escalated and did not can leave the costliest findings to
    later rounds.
- **Round 1 of a large docs-only strand splits the doc-reviewer:**
  - large means more than three docs touched or more than 100 lines added
    (`git diff --shortstat <base>..<tip>`);
  - one `doc-reviewer` per touched doc checks that doc whole and the range's hunks in it;
  - one more checks the docs the range should have touched and runs the rotating sweep;
  - all of them run in parallel, on the lens default;
  - from round 2 on, a single `doc-reviewer` on its own default runs the delta.
  - Why: a single lower-tier pass over a large range can leave findings to later rounds in text it
    had read (cause not established).

> **ADAPT:** name the high-risk areas that trigger gate escalation (typical candidates: money or
> valuation math, time/timezones, concurrency, data migration, and the auth/security surface, i.e.
> filters, token handling, what is publicly exposed).
>
> **ADAPT:** map the tiers to the models your plan offers and keep each agent's frontmatter on its
> tier (example: lens default `sonnet`, one tier above it `opus`, one tier below it `haiku`).

### Read paths of the lenses (delta rounds)

- The orchestrator finds the paths a fold changed with
  `git diff --name-only --no-renames <report tip> <tip>`.
  - The two trees are compared directly, so one command covers the fold and a rebase onto a moved
    `main` alike.
  - `--no-renames` lists a renamed path at both its ends, so code renamed into the docs still
    calls its lenses.
- The doc-reviewer and the reviewer have no row: handbook §9 says when they run again.

| Lens | Reads (example) |
|---|---|
| `falsifier-robustness` | every path outside `*.md` |
| `falsifier-domain` | the backend source and its tests (`src/`) |
| `falsifier-frontend` | the UI source and its tests (`frontend/`), the API it calls (`src/`), the step glue (`src/test/**/acceptance/**`), the grid recipe (`grid/`), and the build files and `verify` |

> **ADAPT:** replace the example paths with your tree's; a lens added per surface adds its row.

<!-- MODULE gate -->
- With the gate module the rows live in the ADAPT block of `gate/dossier-check`: a lens's `needs_`
  value calls for its report, its `reads_` value lets an earlier one stand (→ The gate dossier).
  - With the module `/adopt` deletes this section's lens table and its ADAPT note; this pointer
    takes their place.
  - The block knows the domain and frontend lenses; a lens added per surface needs its pair of
    values added to the check as well.
<!-- /MODULE gate -->

<!-- MODULE security-lens -->
- With the security-lens module, `falsifier-security` reads every path outside `*.md`, as the
  robustness lens does: it attacks the running app, so every code path is one it reads.
  - Its report stands after a fold that changes only docs and is re-run after any other fold.
  - The dossier check holds the rule in code (→ The gate dossier).
<!-- /MODULE security-lens -->

## One track: every change on a branch

Every change, a one-commit fix included, runs on a `claude/<topic>` branch in its own worktree and
lands through the gateway; no commit is made on `main`
([engineering-handbook.md](engineering-handbook.md) §7).

- The implementer commits the messages the plan names and words protocol-conform ones where it
  names none, with no per-commit approval round (real TDD cadence without N round-trips).
- Falsifier + reviewer review the **branch diff**; at the gateway (step 5) the **author + planner
  review the branch's `git log` together**, reword messages if needed (scripted rebase), and the
  branch lands by **rebase + fast-forward only, no merge commit, no squash**.
- **Every commit on `main` builds green**, so a broken branch commit is repaired by rebase before
  the landing.
- **Parallel/benchmark runs** use detached git worktrees,
  `git worktree add --detach <root>/<name> <sha>` under the worktree root (handbook §7), each named
  in its brief by absolute path.
  - Not the `Agent` tool's `isolation: "worktree"`.
    - Why: every worktree belongs under the root, and where isolation puts its checkout once the
      variable moves the root is not probed.

### Landing a branch (gateway hand-off, rebase, merge, cleanup)

- **State the full worktree identity at every review-gateway hand-off:** the branch name **and** the
  absolute worktree path, plus the compare range `<tip>..<base>` on its own line, newest first and
  nothing else in it ([working-with-ai-agents.md](working-with-ai-agents.md)). The author opens that
  path directly in their editor and pastes the range into the compare tool, so the hand-off must
  contain everything needed to do so; a hand-off missing any of the three stalls the gateway.
- **Rebase autonomously, land conditionally:** sync `main` (`sh hooks/sync-main`) and rebase a
  feature strand onto it without asking first, and run the full `./verify all` on the rebased tip
  every time (handbook §7): a clean rebase is not a green one until the build says so. If the
  strand was already author-reviewed **and** the rebase ran clean (no conflicts, no
  judgment-call resolutions) **and** that build is green, it goes to the landing without a
  further review round-trip. When the rebase
  hit a conflict: resolve it, run the full build, **re-run on the resolved tip the gate agents
  handbook §7 names for a conflict resolution**, then present exactly what was resolved plus the
  compare range and wait for the author's go. Conflict resolutions are the one place new,
  unreviewed content can appear, so the resolution gets the re-run and the author's review, and
  nothing less. Who may push: handbook §7.
- **Clean up landed strands immediately, without asking:** at the cleanup point of the project's
  landing model (handbook §7, "Landing models"), `git worktree remove <path>` and
  `git branch -d <branch>`, then `git worktree prune`, and `rmdir` a directory the removal left
  behind empty. This is the agent's own scaffolding and the agent owns it (handbook §7); the commits
  live on in `main`, the branch label and worktree are pure redundancy, and the reflog still holds
  deleted tips for a while. The safety is in the command, not in judgment: `git branch -d` refuses
  a branch that is not fully merged, and `git branch -D` stays denied. Never reach for `rm` here.
  <!-- MODULE agent-seat -->
  - On the seat, once `origin/main` holds the tip, the worktree, the branch and its copy on
    `origin` go (handbook §7, "Landing models").
  <!-- /MODULE agent-seat -->
- **Sweep the stale ones periodically.** `git worktree list` and `git branch --merged main` are the
  inventory.
  - A worktree is removed the same way only when all three hold: its branch is merged into
    `main`, its `git status` is clean, and none of its files changed within the quiet threshold
    (ADAPT: one day by default).
  - Every other worktree is reported to the author with its branch, absolute path and last file
    change, and removed only on the author's word: a session may still be running in it.
  <!-- MODULE volumes -->
  - With the volumes module the sweep, and the cleanup's `git worktree prune` above, run only in a
    session whose container mounts the volume (handbook §7, "Deleting project content").
  <!-- /MODULE volumes -->
- **A fast-forward moves a branch that a worktree holds, so run it inside that worktree** (the local
  landing and `hooks/sync-main`, in the primary checkout), not by moving the ref with plumbing: a
  ref moved under a checkout leaves the index and files on the old commit, the strand reads as if
  it had reverted itself, and `git log` cannot see it. When plumbing is genuinely unavoidable, run
  `git status` in the receiving worktree afterwards and read it; never report a landing without it.

## How to invoke

The **orchestrator is the main session** (me). I drive the pipeline with the `Agent` tool,
`subagent_type` = `implementer` / `falsifier-scenario` / `falsifier-stakeholder` /
`falsifier-domain` / `falsifier-robustness` / `falsifier-frontend` (UI surface only) /
`doc-reviewer` / `reviewer` (defined in `.claude/agents/`). The
Planner phase, every commit-plan approval and every landing go happen with **you** in the main chat.
<!-- MODULE security-lens -->

With the security-lens module `falsifier-security` is a `subagent_type` too, spawned only where
the diff touches the authentication surface ("Cadence").
<!-- /MODULE security-lens -->

## Handoff contracts (what each agent returns)

- **Scenario falsifier:** `soundSpec: yes/no`; findings `{severity, scenario, why}`; concrete
  Gherkin rewrites; coverage gaps as proposed scenario titles.
- **Stakeholder lens:** `filesRead: n of n present`; `soundSpec: yes/no`; findings `{severity,
  scenario, lens, why}`; concrete Gherkin rewrites; `merges: {keep, drop|merge, why}`, kept pairs
  included with `none`.
- **Closing run of a scenario gate:** handbook §9.
- **Implementer:** per-step status (red evidence per handbook §4; green; refactor), the commit
  message used, focused-test result, the state of the `.feature` (armed, or which scenarios still
  fail), full-suite result before hand-off, and any deviation from the approved plan (with
  reason).
- **Falsifier (each panel member):** `robust: yes/no`; a list of weaknesses `{severity, where (file:line), why}`; concrete
  counter-test snippets; `treeDiff`, the output of
  `git -C <its worktree> status --porcelain --untracked-files=all`, taken last, so a new file it
  left shows too. Bias to skepticism: if unsure, flag it.
- **Doc reviewer:** `PASS|FAIL`; findings `{rule: DOC-n, where, what, fix}`.
- **Reviewer:** `PASS|FAIL`; findings `{category: process|cleancode|solid|architecture|tests|security|docs,
  where, fix}`; a ready-to-paste `process-learnings.md` row if any process rule was missed.
<!-- MODULE gate -->
- With the gate module the doc-reviewer's report also carries its `findings:` and `sweep:` lines,
  `none` where it found nothing, and each verdict stands alone on its line (→ The gate dossier).
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
- **The check:** `gate/dossier-check <strand worktree> <tip>` refuses a dossier that breaks a rule
  below; each rule is a leg of the check, and a refusal names its leg in brackets.
  - Its values (test paths, runner lines, the read-path table) live in the ADAPT block at its top.
  - A PASS shows evidence that is consistent, not a run that happened.
    - Why: every record of the dossier is text the gated agent writes.
  - It judges by the ADAPT values and the breach guard of the checkout it runs from.
    - So `main`'s copy runs, from the primary checkout: a strand's own copy may have changed them.
  - `gate/self-test` is its proof.
    - A named limit: its `PATH` shims split at a drive colon, so a `TMPDIR` such as `C:/…` on a
      Windows host fails its stand-in cases loudly.
  - A named limit: a relative worktree argument fails the check closed where it is shaped like
    `name=value`, which `awk` reads as an assignment, and where the strand needs the security
    report, whose wrapper hash then resolves the path twice; give the worktree as an absolute path.
- **When:** every piece of evidence but the reports is in place before a round's gate agents are
  spawned; the round closes only on a PASS once every report is in.
- **Who runs it:**
  - the orchestrator, before it spawns a round and once it has saved `reviewer.md`;
    - before the spawn, a `lens` refusal of a report the round is to write is expected, and every
      other refusal is cleared first;
  - the reviewer, last in the round, every other report in;
    - the refusal of the missing `reviewer.md` is its own report's, which the orchestrator saves;
    - in a split round 1 the refusal of the missing `doc-reviewer.md` concerns the split
      doc-reviewers;
    - every other refusal is a `process` finding of a FAIL, and the reviewer's findings are traced
      as `reviewer-<n>`;
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
- **Location:** `<strand worktree>/gate.local.d/`, ignored by git; a dossier elsewhere, or a symlink
  in its place, is refused.
  - A named limit: a symlink in place of `red/`, `walk/` or `mutation/` inside it is not checked.
- **Base and freeze** are read live:
  - the strand's merge-base is the tip of the block's `base_ref`, `refs/heads/main` in the local
    landing model and `refs/remotes/origin/main` after the fetch in the hub models;
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
  hex digits, or 64 in a SHA-256 repository; one naming another tip is refused, unless:
  - it is a lens report, and no path the lens reads changed since its tip, on the strand or on
    the base (a `reads_` value names them; the robustness lens and the security lens read every
    path outside `*.md`);
  - it is the doc-reviewer's or the reviewer's passing report or a run, and only
    `docs/backlog.md` changed since, in rows `findings.txt` traces as `backlog <ID>` or in the
    doc-debt bullets of a file it traces as `debt <file>`.
- **Reports** are each agent's final message verbatim (→ Handoff contracts):
  - saved as `lens-<lens>.md` per falsifier lens (`lens-security.md` for the security lens),
    `doc-reviewer.md` and `reviewer.md`;
  - a report's own verdict is its one line that opens with `robust:` or `verdict:`, after a list
    marker, bold or a backtick, and reads `yes`, `no`, `PASS` or `FAIL` alone;
  - a choice, an echo of the contract or a verdict quoted further into a line reads as none.
- **Required reports** follow the strand's diff: the doc-reviewer's and the reviewer's always, the
  robustness lens's for any path outside `*.md`, another lens's for a path its `needs_` value
  matches.
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
- **A split round 1** (→ Cadence) writes no top-level `doc-reviewer.md`:
  - each part's report, verbatim, is `round1/doc-reviewer-<part>.md`;
  - a doc's part is its path without `.md` and without a leading dot, each `/` read as `-`
    (`docs-knowledge-base-engineering-handbook`), and the range's part is `range`;
    - a named limit: a part named like a report (a root `reviewer.md` gives `reviewer`) has its
      findings counted under that report;
  - `findings.txt` traces the split findings as `<part>-<n>`, numbered per report, for the rest of
    the gate;
  - the check tests their dispositions as any other;
  - the count of `doc-reviewer-<n>` leaves them out;
  - the reviewer counts each part's findings against its `<part>-<n>` lines;
  - after a fold, round 2's delta run writes the top-level `doc-reviewer.md`; with no fold, one
    `doc-reviewer` on its own default writes it at the tip under review, and that run belongs to
    round 1.
- **`red/`:** one record per commit that changes a test path, opening with `commit: <subject>` of a
  commit in the range:
  - then `tree: <sha>` of the state the run read and a `blob: <sha> <path>` line per test file the
    commit adds or changes, written before the run, then the failing run;
  - the `tree:` line names its tree by a full hex id alone, as a walk log's does, in a `mutation/`
    record too;
  - each blob is a test file's, the one the tree holds at that path and the commit holds;
  - a line opening with `blob: ` holds the sha, one blank and the path, and any other is refused:
    an empty one, or a path with a tab or with a blank at either end;
  - or an `exempt: <reason>` line in place of all three, whose failing lines count for no test;
  - a second record of a commit is refused;
    - its subject is the key, so two commits that change tests share none (→ Base and freeze);
  - a record that shows a diff of its commit's own production code is refused: a mutation belongs
    under `mutation/`.
- **`mutation/`:** the mutation's diff, its `tree:` and `blob:` lines (the test file at the tip,
  held by that tree) and the failing run; a line `older: <reason>` claims a new test over behavior
  older than the strand.
- **New tests:** each test a commit adds shows on a failing line, as its runner prints it, in a
  record whose `tree:` and `blob:` lines the check has proven.
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
  - A named limit: a test a reader does not see adds no new test either, so no red run is asked of
    it.
    - The `sh` reader sees only literal `check "<name>"` lines, not a case declared through a
      helper, and reads the `<<` of `$((1 << 2))` as a here-document, skipping the lines after it.
    - The unittest and pytest readers see a class only where its own header names `TestCase`.
    - The new-test leg reads JUnit tests in `*.java` and vitest's bare `it` and `test`; a Kotlin
      test or a fixture test made by `test.extend` is not read, so its red record is the
      reviewer's to check.
- **`walk/`:** one full-suite log per code tree (the commit's tree without `*.md`), at the path
  `--walk-log <worktree> <commit>` prints, opening with `tree: <sha>` of a tree that holds the
  commit's code tree, whose last line that opens with `[gate/walk] ` is exactly
  `[gate/walk] verify exited 0`, and showing the block's walk proof lines.
  - A log without the tree line, with a tree the repository does not hold, with another commit's
    code tree or without that last line is refused, each for its own reason.
    - So a log of a build that failed, or left the walk worktree moved or dirty, after that line
      is refused.
  - The walk leg and `--walk-green` judge a log by one definition.
  - `<abs worktree>/gate/walk <strand worktree> <walk worktree>` writes those logs: it builds each
    commit of the strand's range, from its merge-base with the block's `base_ref`, whose code tree
    has no green log yet, in a detached walk worktree.
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
    - The test reports a failed build leaves, at each glob of the block's `walk_failed_reports`,
      are copied to `<log>.failed-reports/` before the next commit replaces them.
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
      - A named limit: the `-walk` shape is fixed in `gate/walk`, so a project that renames the
        walk shape (handbook §7, ADAPT) changes the script too.
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
- **`kb-guard.log`:** with a docs-reading test in the block, its focused run at the tip whenever a
  commit is proven by a run on other docs than its own.
  - A run the build reports up to date counts only where the build declares the docs the test reads
    as its inputs, which the block's `docs_guard_ran_re` then admits.
  - The check takes the log's `./verify focus` line only for a path whose last segment is exactly
    `<docs_guard_test>.<ext>`, so a run of a file that only holds the name proves nothing.
    - A named limit: the check puts `docs_guard_test` into a regular expression as it stands, so a
      name holding `+`, `*`, `(`, `|` or `.` misreads a log, and `gate/tip-evidence` reads a `[`
      in it as a pathspec glob.
  - A named limit: the check and `gate/tip-evidence` want a line of `docs_guard_ran_re` and one of
    `walk_success_re` in the log, so a runner whose focused run prints no walk success line (a
    vitest run under the js-ts-npm preset, which ships the guard empty) cannot pass the leg.
- **`findings.txt`:** one line per finding the reports raise, numbered per report (`<lens>-<n>`,
  `doc-reviewer-<n>`, `reviewer-<n>`):
  - a finding is a list item that opens with `{severity:` in a lens report, `{rule: DOC-` in the
    doc-reviewer's (its sweep's included) or `{category:` in the reviewer's, one per line, numbered
    1..n in the report's order;
  - each is `fixed <strand commit>`, `backlog <ID>`, `debt <file>` or `rejected <reason>`; a
    commit is named by 7 to 64 hex digits;
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
  - with a ticket system `ticket <KEY>` replaces `backlog` and `debt`, its key matching
    `ticket_key_re` of `hooks/commit-msg`;
  - a rejection that names a test file names its line.
- **`bdd.log` and `wip.expected`** (the `@wip` leg, mechanical for Cucumber-JVM under Gradle):
  - the `./verify bdd` run at the tip, and one line per expected failure,
    `<the failing line without FAILED> | <its failure line>`;
  - every scenario tagged `@wip` at the tip, by its feature and name, is among them, and no other.
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
      directory.
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
  `fingerprint: <git hash-object> <path>` line per `.feature` under the block's `features_dir`.
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

- **The working directory is not reliable.** With several worktrees checked out, a Bash call can
  silently land in the primary checkout instead of the strand it was told to work in, and a relative
  path then reads or writes the wrong tree **with no error**. `git show <sha>` keeps working from
  either place (worktrees share one object database), which makes the contradiction look like a lost
  write rather than a wrong directory. So: **never use a relative path across worktrees.** Every
  read, grep, write and build in a strand goes through `git -C <abs path>` or an absolute path,
  including the one-off greps that feel too small to bother; print `pwd` in the same command that
  reads or writes. Run `git status --short` in **both** worktrees, the strand's and the primary
  checkout, before a gateway claim. A file check that disagrees with `git show` is a directory
  problem until proven otherwise. And before the **first** edit of a session, require
  `git rev-parse --is-inside-work-tree` to answer `true`: reading files proves nothing about where
  you are, and a bare repository's directory carries a full file tree while accepting no commit.
  A drifted build run is **green**, so its exit code proves nothing: read the executed test results
  in the strand worktree's own build output and check the strand's new tests are among them, or the
  suite ran without the change.
- The deny floor (working-with-ai-agents.md, "Autonomy within guardrails") stops the reset
  recipes; §FAIL-loop holds the reset-free history rewrite and `git update-ref -d` to drop a
  scratch branch.
  Prefer Read/Grep/Glob.
- **A refused command is no obstacle to get around:** working-with-ai-agents.md, "Refusals and
  limits", which says when a rewrite or another route is right and when the step is skipped and
  named in the report.
  - Why: a refused effect reached another way is a breach the report does not show.
- **Edit only what the brief's task needs:** working-with-ai-agents.md, "Session playbook",
  step 2; what else looks wrong goes into the report.
  - Why: an edit nobody asked for reaches the review inside a diff about something else.
- **Remove or stop only what is the workflow's own:** the strand's scaffolding at its cleanup
  point ("Landing a branch"), the walk or lens worktree you were given or created, a container
  or process you started.
  - What another agent or session may still hold is left alone and reported.
  - Why: a sibling's run, branch or container looks like a leftover from the outside.
- **Throwaway code goes into a build-owned probe location, never the test tree.** An agent that needs
  to probe a library's real behavior, or reproduce something by hand, writes it where the build and
  git both ignore it: a **gitignored source set outside `check`/`build`**, with its own run task and
  a committed clean-up task. That is what makes a leftover probe harmless (it cannot turn the build
  red and `git add -A` cannot commit it) and removes the *need* to delete rather than the rule
  against it, so no `rm` and no author round-trip sits on the gate loop's critical path.
- **Never overlap a tree-mutating agent with another agent's build.** The falsifier writes probes and
  runs the build; a reviewer (or any build) hitting the same working tree at the same time builds the
  mutated tree, reports phantom failures, and the two corrupt each other's build state. Panel members
  that **run** anything get their own detached worktree under the worktree root
  (`git worktree add --detach <root>/<name> <sha>`, its absolute path named in the brief;
  handbook §7) or run serialized; only read-only lenses share one. If a sibling's run is already
  active in a tree, report that rather than racing it. (The doc-reviewer only reads docs and runs no
  build; it may run alongside the lenses.)
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

1. **Specify:** the value evaluator's happy-path scenario in the shape of handbook §5's example,
   drafted as a `.feature`, gated by the scenario gate, **confirmed by the author as complete**,
   committed `@wip`.
2. **Plan:** test list for a value evaluator, e.g. ① happy-path value follows the documented formula
   ② the variant operation uses its own rate ③ unknown input → explicit UNDEFINED result + log
   ④ the incremental-progress watermark advances. Commit messages approved.
3. **Implement:** four red→green→refactor micro-commits with those messages; the one that turns
   the scenario green also removes `@wip`, and a refactor commit tidies the evaluator under that
   net.
4. **Falsify (panel):** the domain lens recomputes ① and ② by hand; the robustness lens asks "does ③
   assert the log? does the zero-quantity edge work? is green real if I revert the production line?"
   → counter-tests.
5. **Review:** boundaries clean? KB ([domain-model.md](domain-model.md) / [testing.md](testing.md))
   updated? messages one-line verb-first? → PASS.
6. **You, author gate 2:** the rebased tip with its compare range on your review; on your go it
   lands by the project's landing model (handbook §7).

## Evolution

Start lean: the implementer, the scenario gate, the domain and robustness lenses, the doc-reviewer
and the reviewer. Add a lens per surface as the project grows one (§Roles). Widen the panel +
majority vote for high-risk changes; consider a `/tdd-step` skill in `.claude/skills/` once the loop
is proven; use the **Workflow tool** only for occasional large parallel audits (explicit opt-in),
never for the interactive, human-gated commit loop.
