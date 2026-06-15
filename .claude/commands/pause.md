---
description: Checkpoint the current state to docs so a later /continue resumes cleanly. Never pushes.
argument-hint: [optional: a note about what you were doing]
---

You are pausing work on the Evergore Protocol Collector. Save state so a fresh session can resume
cleanly via /continue. Be quick and safe — assume little budget remains.

1. Capture reality:
   - `git status` and `git diff --stat` (redirect to a file + Read if Bash stdout isn't surfaced).
   - Note what is committed vs uncommitted-on-disk, and where a TDD cycle stands (e.g. "test X is
     red, mid red→green") and whether the tree currently compiles.

2. Update `docs/backlog.md` → "▶ Current status / next action" to reflect reality:
   - what just got done, what is in progress (uncommitted, on disk), and the SINGLE precise next action,
   - plus any gotcha needed to resume (half-applied edit, failing test, pending decision).
   Keep it short — a pointer, not a changelog (git holds history).

3. Do NOT commit half-finished or red work. Only if there is a clean, complete, green unit ready,
   propose ONE single-line present-tense-verb commit message and commit after the author confirms.
   Otherwise leave the WIP uncommitted on disk (it survives closing the IDE) and say so.

4. Never `git push`. Never discard the working tree (`git reset --hard`, `rm -rf`).

Optional note from the author: $ARGUMENTS

End with a 3–5 line "resume from here" summary (what's done, what's uncommitted on disk, the exact
next step) so /continue — or you, later — can pick it up.
