# 07: Git conventions & source of truth

- **Git is the source of truth for history**: no changelog, no commit-by-commit narration, no
  file-by-file diff snapshots in docs.
- To see what changed, when, or by whom: use git itself (`git log`, `git diff`, `git blame`).
- Docs hold durable knowledge, decisions, and plans, not history.

## Conventions

- Active branch: **`main`**, the single mainline (old `master`/`Rebuild` consolidated into it).
- Fine-grained, focused commits, one logical change each.
- Commit messages: **single line, present-tense verb first, no body, no footers** (see
  [engineering-handbook.md](engineering-handbook.md) §7); **the commit log *is* the changelog.**
- The author confirms each commit message; **only the author pushes** (agents never `git push`).
- **LF line endings** enforced via `.gitattributes` (`* text=auto eol=lf`; binaries marked
  `binary`). Why: the repo originally had no `.gitattributes` and mixed CRLF/LF, making
  working-tree diffs look enormous (~95% line-ending churn). If a diff ever looks huge again,
  check line endings first (`git diff --ignore-all-space`).
- The IDE re-saves edited files as **CRLF**; `.gitattributes` normalizes them to LF on commit,
  so the warning is expected (`git diff --check` if unsure). A working tree holding CRLF templates
  makes the running build **serve** CRLF; a clean LF checkout serves LF, and the 1:1 parity check
  normalizes line endings before it compares.
