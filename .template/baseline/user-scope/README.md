# user-scope: what does NOT go into the repo

Knowledge lives in three scopes. Keep them apart:

1. **Repo scope (committed):** the knowledge base, `CLAUDE.md` wrapper, `.claude/settings.json`
   (portable permission policy incl. the deny floor), agents, skills, hooks. Team-facing, reviewed.
2. **User scope (`~/.claude/CLAUDE.md`):** personal communication and workflow preferences
   (tone, language, safety habits). Deliberately not team-facing; never commit it to a project.
   Machine or taste permission rules go to the untracked `.claude/settings.local.json`.
3. **Auto-memory (per user, per project):** managed by Claude Code itself. Durable process
   learnings must graduate from memory into the KB ("KB-first"); memory is a scratchpad, not
   the source of truth.

## Install

Copy [CLAUDE.md](CLAUDE.md) to `~/.claude/CLAUDE.md` (devcontainer variant:
`/home/vscode/.claude/CLAUDE.md`, persisted via the `.claude-home` mount) and personalize it.
The shipped file is the template author's example, not a team standard.
