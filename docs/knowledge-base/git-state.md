# 07: Git conventions & source of truth

- **Git is the source of truth for history**: no changelog, no commit-by-commit narration, no
  file-by-file diff snapshots in docs.
- To see what changed, when, or by whom: use git itself (`git log`, `git diff`, `git blame`).
- Docs hold durable knowledge, decisions, and plans, not history.

## Conventions

- Active branch: **`main`**, the single mainline.
- GitHub (`origin`) is the hub: every landing reaches `main` there first; what GitHub itself refuses
  is below ("What GitHub enforces"). Strand branches are `claude/<topic>`
  ([engineering-handbook.md](engineering-handbook.md) §7).
- Fine-grained, focused commits, one logical change each.
- Commit messages: **single line, present-tense verb first, no body, no footers** (see
  [engineering-handbook.md](engineering-handbook.md) §7); **the commit log *is* the changelog.**
- Agents word their commit messages themselves; the author **alone moves `origin/main`**; on the
  agent seat agents push only their own `claude/<topic>` branches, each with its pull request, and
  on a local machine a strand is never pushed ([engineering-handbook.md](engineering-handbook.md)
  §7).
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

- **Three live rulesets:**
  - `main`: no update, deletion or force push on the default branch, `main`.
  - `agent-namespace`: no creation, update, deletion or force push on any branch outside `claude/`
    and `dependabot/`, at any depth, `main` included, so a push to `main` must pass both.
  - `tags`: no tag creation, update or deletion.
- **Their bypass lists**, readable only by an admin, as the author states them: `main` and
  `agent-namespace` each hold two actors, the repository admin role (the author) and the landing
  App (below), each in mode "always"; `tags` holds the admin role alone. The landing pushes no
  tag, so `tags` needs no second bypass. The author's push of a tip passes only while the role's bypass runs in that mode.
- **In [`.github/rulesets/`](../../.github/rulesets/):** `main.json`, `agent-namespace.json` and
  `tags.json` reproduce the target and rules the API shows for each live ruleset, their bypass
  entries being the author's statement; they are not imported again.
- **The landing App:** the landing workflow pushes `main` with the token of an author-owned GitHub
  App ([build-run-deploy.md](build-run-deploy.md), "The landing workflow"), which the `main`
  and `agent-namespace` rulesets list as their second bypass, the files recording the entry, as the
  author states it.
  Its key sits in the environment `landing`, which admits `main` alone as the author has set it,
  so no workflow on a `claude/` branch can push with it.
- **Whom it binds:** only a credential that is not a bypass. The agent machine's deploy key is no
  bypass, so all three rulesets bind it; a push from the author's PC
  carries the bypass, so nothing binds it (each machine's credential:
  [dev-environment.md](dev-environment.md), "Where agents run"). The landing App's token is a
  bypass too: no ruleset rule binds its push, and `land/land` alone keeps it to a fast-forward of
  the commanded commit, without force and without a deletion of `main`.
- **An agent can rely on:** a push to `main` made with the agent machine's deploy key is refused, as
  long as the bypass list is as the author states it; a push with that key seen refused would prove
  it. A landing through the workflow needs the owner's own `/land <sha>` comment, which an agent on
  the agent machine cannot post: its comments come from `epc-agent[bot]`, or go through the Claude
  GitHub App and carry `performed_via_github_app`.
- **An agent cannot rely on:** a push from the PC being refused anywhere on GitHub, nor a `/land`
  comment posted with the PC's credential, which is the author's own, being ignored; a `claude/`
  branch staying as it was left, another session's included; nor on a `dependabot/` branch holding
  only Dependabot's commits, since `agent-namespace` leaves both writable for the agent machine's
  key.
- **No rule reads a commit message:** GitHub offers commit-metadata rules only to organizations on
  Enterprise plans, and this repository is personal, so nothing on GitHub stops a `[wip]` commit
  on a `claude/` branch (the `pre-push` hook for it is deferred: open-questions.md, D-19).
- **Checking the live state** needs no credential, but `curl` sits in the `ask` tier, so it is the
  author's check, or an agent's with the author's go:
  `https://api.github.com/repos/<owner>/<repo>/rulesets` lists the rulesets and
  `…/rules/branches/<branch>` the rules in force on one branch (owner and repository from `git
  remote get-url origin`, a slash in the branch name written `%2F`). `main` lists `update`,
  `deletion` and `non_fast_forward` from the `main` ruleset and `creation`, `update`, `deletion` and
  `non_fast_forward` from `agent-namespace`, a `claude/` or `dependabot/` branch at any depth
  nothing, and any other branch the four of `agent-namespace`; any other answer means the live
  state moved: stop and ask the author.
- **Changing it** is the author's act, in the repository's Settings, Rules, Rulesets; the file in
  `.github/rulesets/` and this section change with it.
