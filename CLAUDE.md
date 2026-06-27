# CLAUDE.md — Claude Code entry file

> Auto-loaded by Claude Code at the start of every session. This is a **thin wrapper**: the rules live
> in the knowledge base — the [single source of truth](docs/knowledge-base/README.md) — and this file
> only points to them and adds Claude-Code mechanics. **Built from
> [`agent-entry-template.md`](docs/knowledge-base/agent-entry-template.md) · based on template version: 1.**
> At session start, quick-check that the SHARED section below still matches the template (re-sync on a
> version bump). **Conversation may be in German; code, comments, identifiers, and docs are always English.**

## What this is

A Java / Micronaut service that scrapes the browser game **Evergore** for a guild's bank + storage
transaction logs, values item movements, and computes each member's contribution — **automating a
manual Google Sheet**. It is also, deliberately, a **showcase of clean, test-driven, AI-assisted
development**. (Detail: [README.md](README.md) / [project-overview.md](docs/knowledge-base/project-overview.md).)

## Start here (every session)

1. **Read the knowledge base first — do not re-scan the codebase blindly:**
   [`docs/knowledge-base/README.md`](docs/knowledge-base/README.md).
2. Check the backlog [`docs/backlog.md`](docs/backlog.md) and open decisions
   [`docs/open-questions.md`](docs/open-questions.md).
3. Engineering standards: [`engineering-handbook.md`](docs/knowledge-base/engineering-handbook.md);
   AI workflow: [`working-with-ai-agents.md`](docs/knowledge-base/working-with-ai-agents.md);
   agent team: [`multi-agent-playbook.md`](docs/knowledge-base/multi-agent-playbook.md).

## The rules (canonical homes in the KB — pointers, not copies)

- **Commit protocol, branching, merge & the review gateway** → handbook §7. Propose **one** one-line,
  present-tense-verb message (optional `[doc]` tag), get the author's confirmation, then commit.
  **Never `git push`.** Worktree per context → rebase → review the rebased tip → `--ff-only`.
- **TDD / BDD cycle** → handbook §4/§5.
- **Review gate & FAIL-loop governance** → handbook §9.
- **Clean code, SOLID, hexagonal, immutable records, warnings-as-errors, no secrets/host-data** →
  handbook §1–§3.
- **Definition of Done** → handbook §8.
- **In-container-only dev** (never install/run JDK/Gradle/Firefox on the host) → dev-environment.md.
- **Build / run / deploy** → [`build-run-deploy.md`](docs/knowledge-base/build-run-deploy.md).
- **KB-first / KB-current; git is history, docs are knowledge** (no changelog/diff narration in docs;
  remove completed backlog items, keep only rejected/deferred with rationale) → KB README + handbook §7.
- **Ask, don't guess** — author decisions get multiple-choice options (recommended first), recorded in
  [`open-questions.md`](docs/open-questions.md).

## Claude-Code mechanics (this tool only)

- **Auto-load:** Claude Code reads this file at session start — keep it short and KB-pointing.
- **Sub-agents:** the `implementer` / `falsifier` / `reviewer` agent team is defined in
  [`.claude/agents/`](.claude/agents/) and driven per the multi-agent playbook (Planner = you + me).
- **Bash stdout (host only):** a session started on the Windows host may not surface Bash stdout —
  redirect to a file (`cmd > out.txt 2>&1`) and `Read` it. Inside the devcontainer, Bash is normal.
- **Permission-blocked git** (`git push`, `git reset` all forms, `git branch -D`, `rm -rf`) — the
  reset-free history-rewrite recipe is in the multi-agent playbook's FAIL-loop section.
