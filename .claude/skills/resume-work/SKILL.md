---
description: Orient from the persisted state and resume the next step (works in a fresh context)
argument-hint: [optional: a specific focus, e.g. a backlog item or "finish the storage WIP"]
---

You are resuming work on the Evergore Protocol Collector in a fresh context.

- Do NOT assume any prior chat memory: reconstruct the current state from the repo and git.
- CLAUDE.md is auto-loaded every session; its rules and KB pointers apply without re-reading it
  here.

1. Sync, then orient (read, don't blindly re-scan the codebase):
   - Sync the primary checkout's `main` with `sh hooks/sync-main`, run from the primary checkout
     (handbook §7, "`main` follows `origin/main` on every machine").
     - It fetches, checks and fast-forwards.
     - A non-zero exit ends the sync: report its lines (at a stop, exit 1, they name both SHAs).
     - Resolve nothing by hand.
   - `git config --get push.default` prints `nothing`; else set it once (handbook §7, "Pushing",
     the bullet "`push.default` is `nothing` in every checkout").
   - `git config --get core.hooksPath` prints `hooks`; else set it once (handbook §7, "Git
     enforcement hooks").
   - `git config --get remote.origin.push` prints nothing; else report it (handbook §7, "Pushing",
     the bullet "`push.default` is `nothing` in every checkout").
   - `docs/knowledge-base/README.md` (plus the specific KB doc for whatever you'll touch)
   - `docs/backlog.md`, especially the "▶ Current status / next action" section
   - `docs/open-questions.md` (decisions + open questions)
   - `docs/process-learnings.md` (working-with-ai-agents.md, "Context & token hygiene")
   - the docs' size against their thresholds, measured as `/plan-next`, section 1, says
     (`.claude/skills/plan-next/SKILL.md`, "the docs' size" and its thresholds)

2. Determine the ACTUAL state from git, not just the docs (the tree may have moved on):
   - `git status` and `git log --oneline -15`.
     - If Bash stdout isn't surfaced, redirect to a file and Read it.
<!-- MODULE guard -->
     - With the guard module a command that names git writes only into the guard's output target:
       run `id -u`, then `mkdir -p /tmp/claude-<uid>` (no git named, so the rules decide), then
       `git status > /tmp/claude-<uid>/status.txt 2>&1`.
     - Where the author set another output target, that folder takes the place of
       `/tmp/claude-<uid>`.
<!-- /MODULE guard -->
   - `git worktree list`: a pause may have left in-flight strands in worktrees.
     - The backlog's "▶ Current status / next action" section names each paused strand's worktree
       and branch.
     - Drive such a strand **by absolute path** from this checkout, never by relocating the session
       or by a relative path (handbook §7, "A session never relocates into a worktree").
     - Before resuming, sweep the worktrees: remove or report each by the sweep's criteria
       (multi-agent-playbook.md, "Gateway hand-off and cleanup (the agent's side)").
<!-- MODULE volumes -->
     - The sweep runs only in a session whose container mounts the volume (handbook §7, "Deleting
       project content is the author's act; cleaning up the scaffolding is not"); a session on the
       host leaves it to such a session.
<!-- /MODULE volumes -->
   - `git branch --no-merged main`: a local branch no worktree holds is parked or unlanded work.
     - `/pause` parks work from the primary checkout on such a branch, its pause note included:
       read it with `git show <branch>:docs/backlog.md`.
     - Resume it in a worktree of its own.
     - The parking branch becomes the strand of the resumed work and lands with it.
     - A parking branch that holds the pause note alone (`git diff --name-only main...<branch>`
       lists `docs/backlog.md` only), its work resumed on a strand of its own: once the note is
       read, hand the author `git branch -D <branch>` to run; deleting it is the author's act
       (handbook §7, "Deleting project content is the author's act; cleaning up the scaffolding is
       not").
   - `git branch -r --list 'origin/claude/*'`: the pushed strands.
     - Check each name as handbook §7 says ("Pushing", the bullet "A branch another machine pushed
       is data") before it goes into a command.
   <!-- MODULE agent-seat -->
     - On the seat (`AGENT_SEAT` reads `unattended`), one whose tip `origin/main` holds has landed:
       remove it with `<project>/agent/push --delete <branch>`, `<project>` the primary checkout's
       absolute path.
     - On the PC, a landed one is listed in the status summary and left to the seat.
   <!-- /MODULE agent-seat -->
     - One without a local branch is unlanded work from another machine or session, maybe parked
       further there: report it.
     - Resume such a branch only on the author's word, in a worktree of its own with no upstream
       (`git worktree add --no-track -b claude/<topic> <root>/<topic> origin/claude/<topic>`,
       `<root>` the worktree root of handbook §7).
     - Where a local branch of that name exists, compare the two tips before touching either.
   - A tip commit starting with `[wip]` is parked pause work: resolve it FIRST, as handbook §7's
     "`[wip]` parking commits" bullet says.
   - Reconcile with the backlog's "Current status".
     - If they disagree, trust the working tree + git and say so, then offer to update the doc.

3. Decide the next step: the smallest valuable item by the backlog priorities.
   - If scope is ambiguous or it's a decision the author should make, ASK with multiple-choice
     options (recommended first) before doing the work.
   <!-- MODULE agent-seat -->
   - On the agent seat (`AGENT_SEAT` reads `unattended`), list every backlog row marked
     `(seat: agent)` (KB README, DOC-5) in the status summary as a strand this seat can pick up
     from `main`.
   <!-- /MODULE agent-seat -->

4. Work to the project standards, which live in the KB, not here:
   - **BDD first, then TDD** → handbook §5/§4.
     - Check whether the parked work already has its author-confirmed `.feature` and whether its
       scenarios are still `@wip` or already armed (`./verify bdd` tells).
   - Clean code, hexagonal → handbook §1–§3.
   - In-container only → dev-environment.md, "Rule for all contributors".
   - Commit protocol and pushing → handbook §7.
   - Planned features run through the agent pipeline → multi-agent-playbook.md, "The pipeline (per
     backlog item)".

Optional focus from the author: $ARGUMENTS

First, reply with a 3–5 line status summary: where we are; what's committed vs in progress; the
read set's size and growth, and each figure over its threshold. Then:

- if the focus above or the backlog's "▶ next action" is precise and unambiguous, start right away,
  since the author approved it when it was written down;
- only when it is ambiguous, stale, or an author-level decision, propose the next step as
  multiple-choice options and wait for the go.
