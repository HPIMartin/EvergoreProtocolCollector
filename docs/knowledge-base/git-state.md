# 07: Git conventions & source of truth

**Git is the source of truth for history.** We do **not** duplicate it in the docs: no changelog,
no commit-by-commit narration, no file-by-file diff snapshots. To see what changed, when, or by whom,
use git itself (`git log`, `git diff`, `git blame`). Docs hold durable knowledge, decisions, and
plans, not history.

## Conventions

- Active branch: **`main`**, the single mainline (the old `master`/`Rebuild` were consolidated). Fine-grained, focused commits, one logical change each.
- Commit messages: **single line, present-tense verb first, no body, no footers** (see
  [engineering-handbook.md](engineering-handbook.md) §7). **The commit log *is* the changelog.**
- The author confirms each commit message; **only the author pushes** (agents never `git push`).
- **LF line endings** are enforced via `.gitattributes` (`* text=auto eol=lf`; binaries marked
  `binary`). *Durable lesson:* the repo originally had no `.gitattributes` and mixed CRLF/LF, which
  made working-tree diffs look enormous when they were ~95% line-ending churn. That's fixed now; if
  you ever see a giant diff again, check line endings first (`git diff --ignore-all-space`).
