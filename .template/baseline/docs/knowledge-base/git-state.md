# 06: Git conventions & source of truth

**Git is the source of truth for history.** We do **not** duplicate it in the docs: no changelog,
no commit-by-commit narration, no file-by-file diff snapshots. To see what changed, when, or by whom,
use git itself (`git log`, `git diff`, `git blame`). Docs hold durable knowledge, decisions, and
plans, not history.

## Conventions

- Active branch: **`main`**, the single mainline. Fine-grained, focused commits, one logical change
  each, made on a strand's own branch and landed through the gateway, never on `main` (handbook §7).
- Commit messages: **single line, present-tense verb first, no body, no footers** (see
  [engineering-handbook.md](engineering-handbook.md) §7). **The commit log *is* the changelog.** An
  optional ticket key, prefix or suffix, follows the team's pattern set in `hooks/commit-msg`.
- A human review confirms each commit message; who lands and who may push: handbook §7.
- **LF line endings** are enforced via `.gitattributes` (`* text=auto eol=lf`; binaries marked
  `binary`). *Durable lesson (example, from the origin project):* a repo without `.gitattributes` and with
  mixed CRLF/LF makes working-tree diffs look enormous when they are ~95% line-ending churn. If you
  ever see a giant diff, check line endings first (`git diff --ignore-all-space`). An IDE that
  re-saves edited files as CRLF is normal: `.gitattributes` normalizes them on commit, so the warning
  is expected (`git diff --check` if unsure). A working tree holding CRLF templates can make a
  running build *serve* CRLF while a clean checkout serves LF; a parity check normalizes line
  endings before it compares.

> **ADAPT:** adjust the mainline branch name if yours differs; keep the rest, it is process, not
> project.

<!-- MODULE agent-seat -->
## What GitHub enforces

With the agent-seat module three rulesets guard `origin` on GitHub. Claude Code's rules read command
text, so they are the only push guard that holds whatever command runs, and only against a
credential that is no bypass.

- **The three rulesets** (`.github/rulesets/`, `/adopt` step 7, imported in the repository's
  Settings, Rules, Rulesets):
  - `main`: no update, deletion or force push on the default branch;
  - `agent-namespace`: no creation, update, deletion or force push on any branch outside `claude/`
    and `dependabot/`, at any depth, the default branch included, so a push to `main` must pass
    both;
  - `tags`: no creation, update or deletion of a tag.
- **Branches and tags only:** no ruleset covers another ref, such as `refs/notes/*`, so a
  credential that is no bypass can push one; `hooks/pre-push` refuses that push, short of
  `--no-verify`.
- **Their bypass lists:** the admin role in all three, and the landing App in `main` and
  `agent-namespace` (its actor id: `/adopt` step 7), each in mode "always".
  - The landing pushes no tag, so `tags` needs no second bypass.
- **Whom they bind:** every credential that is no bypass.
  - The seat's deploy key and its App are none, so all three rulesets bind them
    ([dev-environment.md](dev-environment.md), "Where agents run").
  - The author's own credential carries the admin role's bypass, so nothing on GitHub binds a push
    from a supervised machine.
  - The landing App's token is a bypass too: `land/land` alone keeps its push to a fast-forward of
    the commanded commit, without force and without deleting the target
    ([build-run-deploy.md](build-run-deploy.md), "The landing workflow").
- **An agent on the seat can rely on:**
  - a push to `main` or a tag with the seat's deploy key being refused, as long as the bypass
    lists are as shipped;
    - a push with that key seen refused proves it, the first adopter's check;
  - a landing needing the owner's own `/land <sha>` comment, which the seat cannot post: its
    comments come from its App and are ignored.
- **An agent cannot rely on:**
  - a push or a `/land` comment made with the author's own credential being refused or ignored;
  - a `claude/` branch staying as it was left, another session's included;
  - a `dependabot/` branch holding only the bot's commits, since `agent-namespace` leaves both
    writable for the deploy key.
- **A strand's own workflow runs on GitHub:** neither `agent/push` nor `hooks/pre-push` refuses a
  workflow file, so one the seat pushes on a `claude/` branch runs with what the repository grants
  a push there, its Actions secrets and its own token included.
  - `hooks/pre-push` cannot tell the agent from the author, so it refuses no such change.
  - So every secret sits in an environment bound to a branch the seat cannot write, as the landing
    key does; that part holds.
  - The default `GITHUB_TOKEN` set to read only binds only a workflow that asks for no
    permission: a strand's workflow can raise its own token with its `permissions:` key
    (GitHub's documentation, not probed).
    - So such a workflow can write what that token reaches, a check run of the required
      check's name under the app `github-actions` included, and the required check does not
      guard a landing against the seat itself.
- **No rule reads a commit message:** on a personal repository nothing on GitHub stops a `[wip]`
  commit on a `claude/` branch.
  - The message rules stay local: `hooks/commit-msg` and `hooks/pre-push`.
- **The local half:** `hooks/pre-push`, for every pusher ([hooks/README.md](../../hooks/README.md),
  `pre-push`); `--no-verify` skips it, the rulesets hold regardless.
- **Checking the live state** needs no credential on a public repository, but `curl` and `gh api`
  sit in the `ask` tier, so it is the author's check, or an agent's with the author's go:
  - `https://api.github.com/repos/<owner>/<repo>/rulesets` lists the rulesets;
  - `https://api.github.com/repos/<owner>/<repo>/rules/branches/<branch>` lists the rules in force
    on one branch, a slash in its name written `%2F`;
  - `main` lists `update`, `deletion` and `non_fast_forward` from `main`, and `creation`, `update`,
    `deletion` and `non_fast_forward` from `agent-namespace`;
  - a `claude/` or `dependabot/` branch at any depth lists nothing, any other branch the four of
    `agent-namespace`;
  - any other answer means the live state moved: stop and ask the author.
  - The bypass lists are readable by an admin only.
- **Changing it** is the author's act, in Settings, Rules, Rulesets; the files under
  `.github/rulesets/` and this section change with it.
- **GitLab:** protected branches and push rules are the counterpart; no preset ships for them.
<!-- /MODULE agent-seat -->
