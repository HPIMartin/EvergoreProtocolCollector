# 07: Git conventions & source of truth

- **Git is the source of truth for history**: no changelog, no commit-by-commit narration, no
  file-by-file diff snapshots in docs.
- To see what changed, when, or by whom: use git itself (`git log`, `git diff`, `git blame`).
- Docs hold durable knowledge, decisions, and plans, not history.

## Conventions

- Active branch: **`main`**, the single mainline (old `master`/`Rebuild` consolidated into it).
- GitHub (`origin`) is the hub: `main` is pushed after every landing and protected by a ruleset (no
  direct push, no force push, no deletion; only the author may bypass it). Strand branches are
  `claude/<topic>` ([engineering-handbook.md](engineering-handbook.md) §7).
- Fine-grained, focused commits, one logical change each.
- Commit messages: **single line, present-tense verb first, no body, no footers** (see
  [engineering-handbook.md](engineering-handbook.md) §7); **the commit log *is* the changelog.**
- The author confirms each commit message; agents push only their own `claude/<topic>` branches,
  and **only the author pushes `main`** ([engineering-handbook.md](engineering-handbook.md) §7).
- **LF line endings** enforced via `.gitattributes` (`* text=auto eol=lf`; binaries marked
  `binary`). Why: the repo originally had no `.gitattributes` and mixed CRLF/LF, making
  working-tree diffs look enormous (~95% line-ending churn). If a diff ever looks huge again,
  check line endings first (`git diff --ignore-all-space`).
- The IDE re-saves edited files as **CRLF**; `.gitattributes` normalizes them to LF on commit,
  so the warning is expected (`git diff --check` if unsure). A working tree holding CRLF templates
  makes the running build **serve** CRLF; a clean LF checkout serves LF, and the 1:1 parity check
  normalizes line endings before it compares.
