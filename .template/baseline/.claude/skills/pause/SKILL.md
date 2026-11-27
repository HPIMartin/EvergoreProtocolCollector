---
description: Checkpoint the current state to docs and park WIP as a [wip] commit so a later /resume-work resumes cleanly. Runs autonomously and fast; signals subagents instead of killing them. Never pushes.
argument-hint: [optional: a note about what you were doing]
---

You are pausing work on {{PROJECT_NAME}}. Save state so a fresh session can resume cleanly via
/resume-work. Be quick and safe; assume little budget remains. Pause fast: do not wait for
running subagent tasks to complete, but never abort them either.

1. Signal running subagents FIRST (so they wind down while you checkpoint):
   - Send every running subagent (implementer / falsifier / reviewer / ...) a message (SendMessage):
     "PAUSE: finish only the atomic micro-step you are in (let a running build or shell command
     complete, never kill it), then stop; do not start the next step. Commit the micro-step if it is
     green, with the plan's message or your own where it names none; otherwise park all
     uncommitted work as ONE `[wip] <short state>` commit on your worktree branch via
     `git commit --no-verify`. Reply with
     3 lines: what is committed, what is parked as [wip], the exact next step."
   - NEVER abort a subagent (no TaskStop, no kill/kill -9) and never kill its shells or builds;
     no work may be lost.
   - Do NOT wait for the subagent's task to finish; you only want its short pause reply.

2. Capture reality:
   - `git status` and `git diff --stat` (redirect to a file + Read if Bash stdout isn't surfaced).
<!-- MODULE guard -->
     With the guard module a command that names git writes only into the guard's output target: run
     `id -u`, then `mkdir -p /tmp/claude-<uid>` (no git named, so the rules decide), then
     `git status > /tmp/claude-<uid>/status.txt 2>&1`; where the author set another output
     target, that folder takes the place of `/tmp/claude-<uid>`.
<!-- /MODULE guard -->
   - `git worktree list`: note every in-flight strand (worktree path + branch) a subagent works in.
   - Note what is committed vs uncommitted-on-disk, and where a TDD cycle stands (e.g. "test X is
     red, mid red→green") and whether the tree currently compiles.

3. Update `docs/backlog.md` → "▶ Current status / next action" to reflect reality:
   - what just got done, what is in progress (parked as `[wip]`), and the SINGLE precise next action,
   - one line per paused subagent: agent, task, worktree + branch, reported state; fold in pause
     replies as they arrive, but do not block the checkpoint on a missing reply; write "reply pending,
     reconstruct from the worktree" instead (the worktree is the authoritative state),
   - plus any gotcha needed to resume (half-applied edit, failing test, pending decision).
   Keep it short: a pointer, not a changelog (git holds history).

4. Park, don't gate: pause never waits for a confirmation. Anything uncommitted (green or not) is
   committed as ONE parking commit `[wip] <one-line state>` on the strand's branch, never on `main`,
   via `git commit --no-verify` (WIP legitimately fails the hooks; the only sanctioned bypass →
   handbook §7). Work sitting in the primary checkout, this pause note included, first moves to a
   new `claude/<topic>` branch (`git switch -c`); after the parking commit the checkout returns to
   `main`. The bypass skips the secret scan too, so glance over the diff for secrets or
   personal/host data before parking. A `[wip]` commit is a checkpoint, not history:
   /resume-work replaces it through the normal commit gateway; it never reaches the review gateway
   or a push.

5. Never `git push`. Never discard the working tree (`git reset --hard`, `rm -rf`).

Optional note from the author: $ARGUMENTS

End with a 3–5 line "resume from here" summary (what's done, what's parked as `[wip]` on which
branch/worktree, the exact next step) so /resume-work (or you, later) can pick it up.
