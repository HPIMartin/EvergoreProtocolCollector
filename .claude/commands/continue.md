---
description: Orient from the persisted state and continue the next step (works in a fresh context)
argument-hint: [optional: a specific focus, e.g. "B1" or "finish the storage WIP"]
---

You are resuming work on the Evergore Protocol Collector in a fresh context. Do NOT assume any prior
chat memory — reconstruct the current state from the repo and git.

1. Orient (read, don't blindly re-scan the codebase):
   - `CLAUDE.md`
   - `docs/knowledge-base/README.md` (plus the specific KB doc for whatever you'll touch)
   - `docs/backlog.md` — especially the "▶ Current status / next action" section
   - `docs/open-questions.md` (decisions + open questions) and `docs/process-learnings.md`

2. Determine the ACTUAL state from git, not just the docs (the tree may have moved on):
   - `git status` and `git log --oneline -15` (if Bash stdout isn't surfaced, redirect to a file and Read it).
   - Reconcile with the backlog's "Current status". If they disagree, trust the working tree + git
     and say so (then offer to update the doc).

3. Decide the next step: the smallest valuable item by the backlog priorities. If scope is ambiguous
   or it's a decision the author should make, ASK with multiple-choice options (recommended first)
   before doing the work.

4. Work to the project standards:
   - TDD red → green → refactor; BDD scenarios for user-facing capabilities (engineering-handbook §4/§5).
   - Hexagonal boundaries; clean, self-explanatory code; **no comments** unless intent truly can't be in code.
   - All builds/tests run **in the devcontainer** — never natively on the host.
   - To implement a planned feature, use the agent pipeline in
     `docs/knowledge-base/multi-agent-playbook.md` (`implementer` → `falsifier` → `reviewer`).

5. Commit protocol: propose **one** single-line, present-tense-verb message, wait for the author's
   confirmation, then commit. **Never `git push`.** Update the relevant KB doc in the same change.

Optional focus from the author: $ARGUMENTS

First, reply with a 3–5 line status summary (where we are; what's committed vs in progress) and your
proposed next step, then wait for my go — unless the focus above already tells you what to do.
