# CLAUDE.md: Claude Code entry file

> Auto-loaded by Claude Code at the start of every session. This is a **thin wrapper**: the rules live
> in the knowledge base (the [single source of truth](docs/knowledge-base/README.md)) and this file
> only points to them and adds Claude-Code mechanics. **Built from
> [`agent-entry-template.md`](docs/knowledge-base/agent-entry-template.md) · based on template version: 1.**
> At session start, quick-check that the SHARED section below still matches the template (re-sync on a
> version bump).

## SHARED section (keep textually identical in every wrapper and in agent-entry-template.md)

> Conversation may be in German; code, comments, identifiers, and docs are always English.
>
> **ADAPT:** Set the conversation language to the author's preference. Code, comments, identifiers, and docs stay English regardless.

**What this is:** {{PROJECT_ONE_LINER}}
(Detail: [README.md](README.md) / [project-overview.md](docs/knowledge-base/project-overview.md).)

> **ADAPT:** Replace {{PROJECT_ONE_LINER}} with one or two sentences: what the project does and what it deliberately showcases.

**Start here (every session):**

0. If your tool has **no native entry file**, build one now from
   [`agent-entry-template.md`](docs/knowledge-base/agent-entry-template.md) and record the template
   version (bootstrap step in the [KB README](docs/knowledge-base/README.md)).
1. Read the knowledge base **map** first, [`docs/knowledge-base/README.md`](docs/knowledge-base/README.md);
   then read only the sections relevant to the task. Do **not** blindly re-scan the codebase or read
   the big docs whole (token hygiene → working-with-ai-agents.md).
2. Check the backlog's **"▶ Current status / next action"** section in
   [`docs/backlog.md`](docs/backlog.md) and any decisions touching your task in
   [`docs/open-questions.md`](docs/open-questions.md).
3. Engineering standards: [`engineering-handbook.md`](docs/knowledge-base/engineering-handbook.md);
   AI workflow: [`working-with-ai-agents.md`](docs/knowledge-base/working-with-ai-agents.md);
   agent team: [`multi-agent-playbook.md`](docs/knowledge-base/multi-agent-playbook.md).

**The rules live in the KB; point, never duplicate** (duplication drifts):

- **Commit protocol, branching, merge & the review gateway** → handbook §7. Propose **one** one-line,
  present-tense-verb message (optional `[doc]` tag), get the author's confirmation, then commit.
  **Never `git push`.** Worktree per context → rebase → review the rebased tip → `--ff-only`.
- **TDD / BDD cycle** → handbook §4/§5.
- **Review gate & FAIL-loop governance** → handbook §9.
- **Clean code, design principles, architecture rules, warnings-as-errors, no secrets/host-data** →
  handbook §1–§3.
- **Definition of Done** → handbook §8.
- **Pinned dev environment** (work only through the project's chosen variant, devcontainer or
  pinned native toolchain; no ad-hoc host installs) → dev-environment.md.
- **Build / run / deploy** → [`build-run-deploy.md`](docs/knowledge-base/build-run-deploy.md).
- **KB-first / KB-current; git is history, docs are knowledge** (no changelog/diff narration in docs;
  remove completed backlog items, keep only rejected/deferred with rationale) → KB README + handbook §7.
- **Ask, don't guess:** author decisions get multiple-choice options (recommended first), recorded in
  [`docs/open-questions.md`](docs/open-questions.md).
- **Context & token hygiene** (section-scoped reads, no re-reads, batched tool calls, short focused
  sessions) → working-with-ai-agents.md.

## Claude-Code mechanics (this tool only)

- **Auto-load:** Claude Code reads this file at session start; keep it short and KB-pointing.
- **Sub-agents:** the `implementer` / `falsifier-domain` / `falsifier-robustness` / `reviewer` agent
  team is defined in [`.claude/agents/`](.claude/agents/) and driven per the multi-agent playbook
  (Planner = the main session together with the author).
- **Bash stdout (example, host-specific):** a session started on a Windows host may not surface Bash
  stdout; redirect to a file (`cmd > out.txt 2>&1`) and `Read` it. Inside the devcontainer, Bash is
  normal.
- **Permission-blocked git** (`git push`, `git reset` all forms, `git branch -D`, `rm -rf`, per
  [`.claude/settings.json`](.claude/settings.json)): the reset-free history-rewrite recipe is in the
  multi-agent playbook's FAIL-loop section.

> **ADAPT:** Keep only the harness quirks that hold on your machines. The Bash-stdout note is an example from the original author's Windows host and may not apply.
