# 07: Git conventions & source of truth

- **Git is the source of truth for history**: no changelog, no commit-by-commit narration, no
  file-by-file diff snapshots in docs.
- To see what changed, when, or by whom: use git itself (`git log`, `git diff`, `git blame`).
- Docs hold durable knowledge, decisions, and plans, not history.

## Conventions

- Active branch: **`main`**, the single mainline (old `master`/`Rebuild` consolidated into it).
- GitHub (`origin`) is the hub: every landing reaches `main` there first; what GitHub itself refuses
  is below ("What GitHub enforces"). Strand branches are `claude/<topic>`
  ([engineering-handbook.md](engineering-handbook.md) §7).
- Fine-grained, focused commits, one logical change each.
- Commit messages: **single line, present-tense verb first, no body, no footers** (see
  [engineering-handbook.md](engineering-handbook.md) §7); **the commit log *is* the changelog.**
- The author confirms each commit message and **alone moves `origin/main`**; agents push only their
  own `claude/<topic>` branches ([engineering-handbook.md](engineering-handbook.md) §7).
- **LF line endings** enforced via `.gitattributes` (`* text=auto eol=lf`; binaries marked
  `binary`). Why: the repo originally had no `.gitattributes` and mixed CRLF/LF, making
  working-tree diffs look enormous (~95% line-ending churn). If a diff ever looks huge again,
  check line endings first (`git diff --ignore-all-space`).
- The IDE re-saves edited files as **CRLF**; `.gitattributes` normalizes them to LF on commit,
  so the warning is expected (`git diff --check` if unsure). A working tree holding CRLF templates
  makes the running build **serve** CRLF; a clean LF checkout serves LF, and the 1:1 parity check
  normalizes line endings before it compares.

## What GitHub enforces

What the public API shows of `origin`. Claude Code's rules read command text, so this is the only
push guard that holds whatever command runs, and it holds only against a credential that is not
its bypass.

- **One live ruleset, `main`:** no update, deletion or force push on the default branch, `main`.
  Its bypass list, readable only by an admin, holds the repository admin role alone, the author,
  as the author states it. The landing is a direct push, which passes only while that bypass runs
  in mode "always" (an admin-only read too).
- **In [`.github/rulesets/`](../../.github/rulesets/):** `main.json` reproduces the target and
  rules the API shows for the live `main` ruleset (its bypass entry is the author's statement) and
  is not imported again; `agent-namespace` (no creation, update, deletion or force push on any
  branch outside `claude/` and `dependabot/`, at any depth) and `tags` (no tag creation, update or
  deletion), each with the author as its only bypass, wait for the author's import and are not
  live.
- **Whom it binds:** only a credential that is not the bypass. The agent machine's is one, so the
  `main` ruleset binds it and the prepared two will once imported; a push from the author's PC
  carries the bypass, so nothing binds it (each machine's credential:
  [dev-environment.md](dev-environment.md), "Where agents run").
- **An agent can rely on:** a push to `main` made with the agent machine's deploy key is refused, as
  long as the bypass list is as the author states it; a push with that key seen refused would prove
  it.
- **An agent cannot rely on:** a push from the PC being refused anywhere on GitHub; a branch or tag
  other than `main` staying as it was left, another session's `claude/` branch included; nor on a
  `dependabot/` branch holding only Dependabot's commits, since the prepared `agent-namespace`
  leaves it writable for the agent machine's key.
- **No rule reads a commit message:** GitHub offers commit-metadata rules only to organizations on
  Enterprise plans, and this repository is personal, so nothing on GitHub stops a `[wip]` commit
  on a `claude/` branch (the `pre-push` hook for it is deferred: open-questions.md, D-19).
- **Checking the live state** needs no credential, but `curl` sits in the `ask` tier, so it is the
  author's check, or an agent's with the author's go:
  `https://api.github.com/repos/<owner>/<repo>/rulesets` lists the rulesets and
  `…/rules/branches/<branch>` the rules in force on one branch (owner and repository from `git
  remote get-url origin`, a slash in the branch name written `%2F`). `main` lists `update`,
  `deletion` and `non_fast_forward`, every other branch nothing; any other answer means the live
  state moved: stop and ask the author.
- **Changing it** is the author's act, in the repository's Settings, Rules, Rulesets; the file in
  `.github/rulesets/` and this section change with it.
