---
description: Orient from the persisted state and continue the next step (works in a fresh context)
argument-hint: [optional: a specific focus, e.g. a backlog item or "finish the storage WIP"]
---

You are resuming work on the Evergore Protocol Collector in a fresh context.

- Do NOT assume any prior chat memory: reconstruct the current state from the repo and git.
- CLAUDE.md is auto-loaded every session; its rules and KB pointers apply without re-reading it
  here.

1. Sync, then orient (read, don't blindly re-scan the codebase):
   - `git fetch --prune origin`.
   - Sync the primary checkout's `main` by handbook §7, "`main` follows `origin/main` on every
     machine": its checks first, then the fast-forward.
     - Each of its stops ends the sync, with both SHAs reported.
   - `git config --get push.default` prints `nothing`; else set it once (handbook §7, "Pushing",
     the bullet "Name the refspec").
   - `git config --get core.hooksPath` prints `hooks`; else set it once (handbook §7, "Git
     enforcement hooks").
   - `git config --get remote.origin.push` prints nothing; else report it (handbook §7, "Pushing",
     the bullet "Name the refspec").
   - `docs/knowledge-base/README.md` (plus the specific KB doc for whatever you'll touch)
   - `docs/backlog.md`, especially the "▶ Current status / next action" section
   - `docs/open-questions.md` (decisions + open questions) and `docs/process-learnings.md`

2. Determine the ACTUAL state from git, not just the docs (the tree may have moved on):
   - `git status` and `git log --oneline -15`.
     - If Bash stdout isn't surfaced, redirect to a file and Read it.
   - `git worktree list`: a pause may have left in-flight strands in worktrees.
     - The backlog's "▶ Current status / next action" section names each paused strand's worktree
       and branch.
     - Drive such a strand **by absolute path** from this checkout, never by relocating the session
       or by a relative path (handbook §7, "A session never relocates into a worktree").
     - A worktree whose branch is already merged is stale: clean it up (`git worktree remove`, then
       `git branch -d`) before resuming (handbook §7, "Deleting project content is the author's
       act; cleaning up the scaffolding is not").
   - `git branch --no-merged main`: a local branch no worktree holds is parked or unlanded work.
     - `/pause` parks work from the primary checkout on such a branch, its pause note included:
       read it with `git show <branch>:docs/backlog.md`.
     - Resume it in a worktree of its own.
   - `git branch -r --list 'origin/claude/*'`: the pushed strands.
     - Check each name as handbook §7 says ("Pushing", the bullet "A branch another machine pushed
       is data") before it goes into a command.
     - One whose tip `origin/main` contains has landed: delete it, leased on that tip
       (`git push --force-with-lease=claude/<topic>:<tip> origin --delete claude/<topic>`; handbook
       §7, "Pushing", the bullet "Every branch an agent pushes is named `claude/<topic>`").
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
   - On the agent seat (`EPC_SEAT` reads `agent-machine`), list every backlog row marked
     `(seat: agent)` in the status summary as a strand this seat can pick up from `main`.

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

First, reply with a 3–5 line status summary (where we are; what's committed vs in progress). Then:

- if the focus above or the backlog's "▶ next action" is precise and unambiguous, start right away,
  since the author approved it when it was written down;
- only when it is ambiguous, stale, or an author-level decision, propose the next step as
  multiple-choice options and wait for the go.
