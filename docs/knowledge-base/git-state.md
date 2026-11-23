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
- Fine-grained, focused commits, one logical change each, made on a strand's own branch and landed
  through the gateway, never on `main` ([engineering-handbook.md](engineering-handbook.md) §7).
- Commit-message rules: [engineering-handbook.md](engineering-handbook.md) §7.
- **The commit log *is* the changelog.**
- Agents word their commit messages themselves; the author **alone moves `origin/main`**; on the
  agent seat agents push only their own `claude/<topic>` branches, through `agent/push`, each with
  its pull request, and on a local machine a strand is never pushed
  ([engineering-handbook.md](engineering-handbook.md) §7).
- **LF line endings** enforced via `.gitattributes` (`* text=auto eol=lf`; binaries marked
  `binary`). Why: mixed CRLF/LF makes working-tree diffs look enormous.
- If a diff looks huge, check line endings first (`git diff --ignore-all-space`).
- The IDE re-saves edited files as **CRLF**; `.gitattributes` normalizes them to LF on commit,
  so the warning is expected (`git diff --check` if unsure). A working tree holding CRLF templates
  makes the running build **serve** CRLF; a clean LF checkout serves LF. The scripted 1:1 comparison
  compares values, so line endings never reach it.

<!-- MODULE agent-seat -->
## What GitHub enforces

What the public API shows of `origin`. Claude Code's rules read command text, so this is the only
push guard that holds whatever command runs, and it holds only against a credential that is not
its bypass.

- **Three live rulesets:**
  - `main`: no update, deletion or force push on the default branch, `main`.
  - `agent-namespace`: no creation, update, deletion or force push on any branch outside `claude/`
    and `dependabot/`, at any depth, `main` included, so a push to `main` must pass both.
  - `tags`: no tag creation, update or deletion.
- **Branches and tags only:** no ruleset covers another ref, such as `refs/notes/*`, so a
  credential that is not a bypass can push one; `hooks/pre-push` refuses that push, short of
  `--no-verify`.
- **Their bypass lists**, readable only by an admin, as the author states them:
  - `main` and `agent-namespace` each hold two actors, each in mode "always":
    - the repository admin role (the author);
    - the landing App (below).
  - `tags` holds the admin role alone.
    - Why: the landing pushes no tag, so `tags` needs no second bypass.
  - The author's push of a tip passes only while the role's bypass runs in that mode.
- **In [`.github/rulesets/`](../../.github/rulesets/):** `main.json`, `agent-namespace.json` and
  `tags.json` reproduce the target and rules the API shows for each live ruleset, their bypass
  entries being the author's statement; they are not imported again.
- **The landing App:** the landing workflow pushes `main` with the token of an author-owned GitHub
  App ([build-run-deploy.md](build-run-deploy.md), "The landing workflow").
  - The `main` and `agent-namespace` rulesets list it as their second bypass, as the author
    states it.
  - The files record that entry, as the author states it.
  - Its key sits in the environment `landing`.
  - `landing` admits `main` alone, as the author has set it.
    - So no workflow on a `claude/` branch can push with the key.
- **Whom it binds:** only a credential that is not a bypass (each machine's credential:
  [dev-environment.md](dev-environment.md), "Where agents run").
  - The agent machine's deploy key and its App `epc-agent` are no bypass, so all three rulesets
    bind them.
  - A push from the author's PC carries the bypass, so nothing binds it.
  - The landing App's token is a bypass too, so no ruleset rule binds its push.
  - `land/land` alone keeps that push to a fast-forward of the commanded commit, without force and
    without a deletion of `main`.
- **An agent can rely on:**
  - A push to `main` or a tag made with the agent machine's deploy key is refused, as long as the
    bypass list is as the author states it.
    - A push with that key seen refused would prove it.
  - A landing through the workflow needs the owner's own `/land <sha>` comment, which an agent on
    the agent machine cannot post.
    - Its comments come from `epc-agent[bot]`, or go through the Claude GitHub App and carry
      `performed_via_github_app`.
- **An agent cannot rely on:**
  - A push from the PC being refused anywhere on GitHub.
  - A `/land` comment posted with the PC's credential, which is the author's own, being ignored.
  - A `claude/` branch staying as it was left, another session's included.
  - A `dependabot/` branch holding only Dependabot's commits.
  - Why for the last two: `agent-namespace` leaves `claude/` and `dependabot/` branches writable
    for the agent machine's key.
- **A strand's own workflow runs on GitHub:** neither `agent/push` nor `hooks/pre-push` refuses a
  workflow file, so one the agent machine pushes on a `claude/` branch runs with what the
  repository grants a push there, its Actions secrets and its own token included.
  - `hooks/pre-push` cannot tell the agent from the author, so it refuses no such change.
  - So every secret sits in an environment bound to a branch the agent machine cannot write, as the
    landing key sits in `landing`; that part holds: the repository holds no Actions secret outside
    `landing` (author decision 2026-11-23).
  - The default `GITHUB_TOKEN` set to read only binds only a workflow that asks for no permission:
    a strand's workflow can raise its own token with its `permissions:` key (GitHub's
    documentation, not probed).
    - So such a workflow can write what that token reaches, a check run of the required check's
      name under the app `github-actions` included, and the required check does not guard a
      landing against the agent machine itself.
- **No rule reads a commit message:**
  - Nothing on GitHub stops a `[wip]` commit on a `claude/` branch.
    - Why: GitHub offers commit-metadata rules only to organizations on Enterprise plans, and this
      repository is personal.
  - The message rules stay local: `hooks/commit-msg` and `hooks/pre-push`.
- **The local half:** `hooks/pre-push`, for every pusher ([hooks/README.md](../../hooks/README.md),
  `pre-push`); `--no-verify` skips it, the rulesets hold regardless.
- **Checking the live state** needs no credential.
  - `curl` and `gh api` sit in the `ask` tier, so it is the author's check, or an agent's with the
    author's go.
  - `https://api.github.com/repos/<owner>/<repo>/rulesets` lists the rulesets.
  - `…/rules/branches/<branch>` lists the rules in force on one branch.
    - Owner and repository come from `git remote get-url origin`.
    - A slash in the branch name is written `%2F`.
  - `main` lists `update`, `deletion` and `non_fast_forward` from the `main` ruleset and
    `creation`, `update`, `deletion` and `non_fast_forward` from `agent-namespace`.
  - A `claude/` or `dependabot/` branch at any depth lists nothing.
  - Any other branch lists the four of `agent-namespace`.
  - Any other answer means the live state moved: stop and ask the author.
- **Changing it** is the author's act, in the repository's Settings, Rules, Rulesets; the file in
  `.github/rulesets/` and this section change with it.
<!-- /MODULE agent-seat -->
