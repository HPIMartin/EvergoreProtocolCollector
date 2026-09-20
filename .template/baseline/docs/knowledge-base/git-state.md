# 06: Git conventions & source of truth

**Git is the source of truth for history.** We do **not** duplicate it in the docs: no changelog,
no commit-by-commit narration, no file-by-file diff snapshots. To see what changed, when, or by whom,
use git itself (`git log`, `git diff`, `git blame`). Docs hold durable knowledge, decisions, and
plans, not history.

## Conventions

- Active branch: **`main`**, the single mainline. Fine-grained, focused commits, one logical change
  each.
- Commit messages: **single line, present-tense verb first, no body, no footers** (see
  [engineering-handbook.md](engineering-handbook.md) §7). **The commit log *is* the changelog.**
- The author confirms each commit message; **only the author pushes** (agents never `git push`).
- **LF line endings** are enforced via `.gitattributes` (`* text=auto eol=lf`; binaries marked
  `binary`). *Durable lesson (example, from the origin project):* a repo without `.gitattributes` and with
  mixed CRLF/LF makes working-tree diffs look enormous when they are ~95% line-ending churn. If you
  ever see a giant diff, check line endings first (`git diff --ignore-all-space`).

> **ADAPT:** adjust the mainline branch name if yours differs; keep the rest, it is process, not
> project.
