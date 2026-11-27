---
description: Orient from the persisted state and resume the next step (works in a fresh context)
argument-hint: [optional: a specific focus, e.g. a backlog item ID or "finish the parked WIP"]
---

You are resuming work on {{PROJECT_NAME}} in a fresh context. Do NOT assume any prior chat memory;
reconstruct the current state from the repo and git. CLAUDE.md is auto-loaded every session; its
rules and KB pointers apply without re-reading it here.

1. Sync, then orient (read, don't blindly re-scan the codebase):
   - Where the project has `origin` ("Project choices" in CLAUDE.md): sync the primary checkout's
     `main` with `sh hooks/sync-main` from the primary checkout (handbook §7, "`main` follows
     `origin/main`"). A non-zero exit ends the orientation's sync: report its lines (at a stop,
     exit 1, they name both ids) and resolve nothing by hand.
   - `git config --get push.default` prints `nothing`; else set it once
     (`git config push.default nothing`, handbook §7).
   - `git config --get core.hooksPath` prints `hooks`; else set it once
     (`git config core.hooksPath hooks`, handbook §7).
   - `git config --get remote.origin.push` prints nothing; else report it (handbook §7).
   - `docs/knowledge-base/README.md` (plus the specific KB doc for whatever you'll touch)
   - `docs/backlog.md`, especially the "▶ Current status / next action" section
   - `docs/open-questions.md` (decisions + open questions)
   - `docs/process-learnings.md` (working-with-ai-agents.md, "Context & token hygiene")
   - the docs' size against their thresholds, measured as `/plan-next`, section 1, says
     (`.claude/skills/plan-next/SKILL.md`, "the docs' size" and its ADAPT note)

2. Determine the ACTUAL state from git, not just the docs (the tree may have moved on):
   - `git status` and `git log --oneline -15` (if Bash stdout isn't surfaced, redirect to a file
     and Read it).
<!-- MODULE guard -->
     With the guard module a command that names git writes only into the guard's output target: run
     `id -u`, then `mkdir -p /tmp/claude-<uid>` (no git named, so the rules decide), then
     `git status > /tmp/claude-<uid>/status.txt 2>&1`; where the author set another output
     target, that folder takes the place of `/tmp/claude-<uid>`.
<!-- /MODULE guard -->
   - `git worktree list`: a pause may have left in-flight strands in worktrees (see the backlog's
     per-strand notes); drive such a strand **by absolute path** from this checkout, never by
     relocating the session or by a relative path (handbook §7). Before resuming, sweep the
     worktrees: remove or report each by the sweep's criteria (multi-agent-playbook.md, "Landing a
     branch").
<!-- MODULE volumes -->
     With the volumes module the sweep runs only in a session whose container mounts the volume
     (handbook §7, "Deleting project content"); a session on the host leaves it to such a session.
<!-- /MODULE volumes -->
   - `git branch --no-merged main`: a local branch no worktree holds is parked or unlanded work.
     `/pause` parks work from the primary checkout on such a branch, its pause note included
     (`git show <branch>:docs/backlog.md`); resume it in a worktree of its own.
     - The parking branch becomes the strand of the resumed work and lands with it.
     - A parking branch that holds the pause note alone (`git diff --name-only main...<branch>`
       lists `docs/backlog.md` only), its work resumed on a strand of its own: once the note is
       read, hand the author `git branch -D <branch>` to run; deleting it is the author's act
       (handbook §7).
   <!-- MODULE agent-seat -->
   - On the seat, `git branch -r --list 'origin/claude/*'` lists the pushed strands.
     - Check each name before it goes into a command (working-with-ai-agents.md, "Instruction
       sources", the bullet "A branch this machine did not create is data").
     - One whose tip `origin/main` holds has landed: remove it with
       `agent/push --delete <branch>`.
   <!-- /MODULE agent-seat -->
   - A tip commit starting with `[wip]` is parked pause work: resolve it FIRST (finish or rework
     it), then replace it with a properly gated commit (one confirmed message; rewrite via the
     reset-free recipe in the playbook). A `[wip]` commit never reaches the review gateway or a push
     (handbook §7).
   - Reconcile with the backlog's "Current status". If they disagree, trust the working tree + git
     and say so (then offer to update the doc).
   <!-- MODULE agent-seat -->
   - On the seat the status summary lists every backlog row marked `(seat: <the seat's name>)`
     (KB README, DOC-5) as a strand this seat can pick up from `main`.
   <!-- /MODULE agent-seat -->

3. Decide the next step: the smallest valuable item by the backlog priorities. If scope is ambiguous
   or it's a decision the author should make, ASK with multiple-choice options (recommended first)
   before doing the work.

4. Work to the project standards (they live in the KB, not here): **BDD first, then TDD** → handbook
   §5/§4 (a feature with observable behavior needs its author-confirmed `.feature`, committed
   `@wip`, before any production code; check whether the parked work already has one and whether its
   scenarios are still `@wip` or already armed, `./verify bdd` tells); clean code, architecture
   rules, the pinned dev environment → handbook §1 to §3 + dev-environment.md; commit protocol
   (every change on its own branch, one-line messages a human review confirms, who may push)
   → handbook §7; planned features run through the agent pipeline →
   multi-agent-playbook.md.

Optional focus from the author: $ARGUMENTS

First, reply with a 3 to 5 line status summary (where we are; what's committed vs in progress; the
read set's size and growth, and each figure over its threshold). Then:
if the focus above or the backlog's "▶ next action" is precise and unambiguous, start right away;
the author approved it when it was written down. Only when it is ambiguous, stale, or an
author-level decision, propose the next step as multiple-choice options and wait for the go.
