---
description: Orient from the persisted state and continue the next step (works in a fresh context)
argument-hint: [optional: a specific focus, e.g. a backlog item ID or "finish the parked WIP"]
---

You are resuming work on {{PROJECT_NAME}} in a fresh context. Do NOT assume any prior chat memory;
reconstruct the current state from the repo and git. CLAUDE.md is auto-loaded every session; its
rules and KB pointers apply without re-reading it here.

1. Orient (read, don't blindly re-scan the codebase):
   - `docs/knowledge-base/README.md` (plus the specific KB doc for whatever you'll touch)
   - `docs/backlog.md`, especially the "▶ Current status / next action" section
   - `docs/open-questions.md` (decisions + open questions) and `docs/process-learnings.md`

2. Determine the ACTUAL state from git, not just the docs (the tree may have moved on):
   - `git status` and `git log --oneline -15` (if Bash stdout isn't surfaced, redirect to a file and Read it).
   - `git worktree list`: a pause may have left in-flight strands in worktrees (see the backlog's
     per-strand notes); resume such a strand inside its worktree per the multi-agent playbook.
   - A tip commit starting with `[wip]` is parked pause work: resolve it FIRST (finish or rework it),
     then replace it with a properly gated commit (one confirmed message; rewrite via the reset-free
     recipe in the playbook). A `[wip]` commit never reaches the review gateway or a push (handbook §7).
   - Reconcile with the backlog's "Current status". If they disagree, trust the working tree + git
     and say so (then offer to update the doc).

3. Decide the next step: the smallest valuable item by the backlog priorities. If scope is ambiguous
   or it's a decision the author should make, ASK with multiple-choice options (recommended first)
   before doing the work.

4. Work to the project standards (they live in the KB, not here): TDD/BDD → handbook §4/§5; clean
   code, architecture rules, the pinned dev environment → handbook §1–§3 + dev-environment.md; commit protocol (one
   confirmed one-line message, never push) → handbook §7; planned features run through the agent
   pipeline → multi-agent-playbook.md.

Optional focus from the author: $ARGUMENTS

First, reply with a 3–5 line status summary (where we are; what's committed vs in progress). Then:
if the focus above or the backlog's "▶ next action" is precise and unambiguous, start right away;
the author approved it when it was written down. Only when it is ambiguous, stale, or an author-level
decision, propose the next step as multiple-choice options and wait for the go.
