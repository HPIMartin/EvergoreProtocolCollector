---
description: Orient from the persisted state and continue the next step (works in a fresh context)
argument-hint: [optional: a specific focus, e.g. "B1" or "finish the storage WIP"]
---

You are resuming work on the Evergore Protocol Collector in a fresh context. Do NOT assume any prior
chat memory — reconstruct the current state from the repo and git. CLAUDE.md is auto-loaded every
session; its rules and KB pointers apply without re-reading it here.

1. Sync, then orient (read, don't blindly re-scan the codebase):
   - `git fetch --prune origin`, then sync the primary checkout's `main` by handbook §7's
     "`main` follows `origin/main` on every machine": its checks first, then the fast-forward;
     each of its stops ends the sync, with both SHAs reported.
   - `git config --get push.default` prints `nothing`, `git config --get core.hooksPath` prints
     `hooks` and `git config --get remote.origin.push` prints nothing; else set the first two
     once (build-run-deploy.md, "Git hooks") and report the third.
   - `docs/knowledge-base/README.md` (plus the specific KB doc for whatever you'll touch)
   - `docs/backlog.md` — especially the "▶ Current status / next action" section
   - `docs/open-questions.md` (decisions + open questions) and `docs/process-learnings.md`

2. Determine the ACTUAL state from git, not just the docs (the tree may have moved on):
   - `git status` and `git log --oneline -15` (if Bash stdout isn't surfaced, redirect to a file and Read it).
   - `git worktree list` — a pause may have left in-flight strands in worktrees (see the backlog's
     per-strand notes); drive such a strand **by absolute path** from this checkout, never by
     relocating the session or by a relative path (handbook §7). A worktree whose branch is already
     merged is stale: clean it up (`git worktree remove` + `git branch -d`) before resuming.
   - `git branch --no-merged main`: a local branch no worktree holds is parked or unlanded work
     (`/pause` parks work from the primary checkout on one, its pause note included: read it with
     `git show <branch>:docs/backlog.md`); resume it in a worktree of its own.
   - `git branch -r --list 'origin/claude/*'`: the pushed strands, each name checked as handbook §7
     says before it goes into a command. One whose tip `origin/main`
     contains has landed: delete it, leased on that tip
     (`git push --force-with-lease=claude/<topic>:<tip> origin --delete claude/<topic>`; the
     classifier may hold back on another session's branch, then report it). One without a local
     branch is unlanded work from another machine or session, maybe parked further there: report
     it, and resume it only on the author's word, in a worktree of its own with no upstream
     (`git worktree add --no-track -b claude/<topic> <path> origin/claude/<topic>`, handbook §7).
     Where a local branch of that name exists, compare the two tips before touching either.
   - A tip commit starting with `[wip]` is parked pause work: resolve it FIRST — finish or rework it,
     then replace it with a properly gated commit (one confirmed message; rewrite via the reset-free
     recipe in the playbook). A `[wip]` commit never reaches the review gateway or a push (handbook §7).
   - Reconcile with the backlog's "Current status". If they disagree, trust the working tree + git
     and say so (then offer to update the doc).

3. Decide the next step: the smallest valuable item by the backlog priorities. If scope is ambiguous
   or it's a decision the author should make, ASK with multiple-choice options (recommended first)
   before doing the work.

4. Work to the project standards — they live in the KB, not here: **BDD first, then TDD** → handbook
   §5/§4 (a feature with observable behavior needs its author-confirmed `.feature`, committed `@wip`,
   before any production code; check whether the parked work already has one and whether its
   scenarios are still `@wip` or already armed, `./verify bdd` tells); clean code, hexagonal,
   in-container-only → handbook §1–§3 + dev-environment.md; commit protocol (one confirmed one-line
   message; push only the strand's own `claude/<topic>` branch, never a `[wip]` commit) → handbook
   §7; planned features run through the agent pipeline →
   multi-agent-playbook.md.

Optional focus from the author: $ARGUMENTS

First, reply with a 3–5 line status summary (where we are; what's committed vs in progress). Then:
if the focus above or the backlog's "▶ next action" is precise and unambiguous, start right away —
the author approved it when it was written down. Only when it is ambiguous, stale, or an author-level
decision, propose the next step as multiple-choice options and wait for the go.
