# 11: Dev Environment & Virtualization

**Principle: the dev environment is pinned in as few places as possible, every one listed. Nobody
(human or agent) builds, tests, or runs the app through an ad-hoc host toolchain.** Two variants
implement the principle; a project picks exactly one at adoption (template folder `env/`) and
records the choice here.

> **ADAPT:** state the chosen variant here and delete the other variant's section (or keep it with a
> note if a later switch is plausible).

## Variant A: devcontainer (recommended)

Fully virtualized: no toolchain is installed or run on the host. All work (builds, tests, the app,
and the AI agents) runs inside the **devcontainer** or via containers, except a session started on
the host (the note under "How to work in it"). This keeps the host clean, and a toolchain upgrade
changes the build files, never the host. The CI preset builds its job image from this same folder,
so the pipeline and the developer run the identical toolchain
([build-run-deploy.md](build-run-deploy.md), "CI / dev environment").

- **The container keeps the toolchain off the host, not the host's rights away from the agent.**
  - VS Code's Dev Containers extension forwards the host's git credentials into the container, so
    an agent there acts with the author's git credentials.
    - That forwarded access to the project's own remote is the access an agent is given
      (handbook §7, "No secrets").
  - A host Docker socket mounted into the container (the `docker-outside-of-docker` feature, which
    the template does not ship) hands over the host: a container started through it can
    bind-mount the host's file system, every credential stored there included.

> **ADAPT:** if the project mounts the host's Docker socket, record that trade-off here. Each
> alternative falls short:
>
> - docker-in-docker runs its container privileged (the docker-in-docker feature's
>   devcontainer-feature.json), and a privileged container can take control of its host
>   (Docker's `docker run` reference);
> - Enhanced Container Isolation needs a Docker Business subscription (Docker's Enhanced Container
>   Isolation docs);
> - the Claude Code sandbox cannot run `docker` inside it, and a `docker` excluded from it runs with
>   full access (the Claude Code sandboxing docs).

<!-- MODULE agent-seat -->
> Note: with the agent-seat module no unattended agent runs in a container that mounts the host's
> Docker socket ("Where agents run", below).
<!-- /MODULE agent-seat -->

### The devcontainer (`.devcontainer/devcontainer.json`; shipped in the template under `env/devcontainer/`)

> **ADAPT:** describe your container: base image, toolchain features and their pinned versions,
> editor extensions, and the `postCreateCommand` (warm the build cache, activate the git hooks and
> set `push.default` to `nothing`, see [build-run-deploy.md](build-run-deploy.md)). The feature
> version is one of the toolchain's pins and holds the same value as the build's toolchain
> declaration ("Toolchain version" below).
> Record any deferred features with the backlog item that tracks re-adding them.

- **`postCreateCommand` is one `&&` chain, without `|| true`:** it aborts on the first failure by
  design.
  - Why: a failing step stops the chain, so a broken step shows at container creation instead of
    leaving it half-configured.
- **Editor extensions:** Claude Code and GitLens are recommended.
  - Claude Code is the agent.
  - GitLens is a compare tool that reads the review hand-off's range
    ([working-with-ai-agents.md](working-with-ai-agents.md), "Handing the author a review").
  - The stack's own extensions (a language pack, SonarLint, Checkstyle) are the project's choice.
  - The `devcontainer.json` list is the one list; no `.vscode/extensions.json` repeats it.
  - Why: two lists drift.
- **The node feature, itself a stack example, runs with `nodeGypDependencies` off:** that keeps
  Debian's `python3-minimal` out, which the feature's node-gyp step would install.
  - The node feature 2.1.2 on `devcontainers/base:bookworm` installed `python3-minimal` with the
    option on and none with it off.
  - A stack that builds native node modules turns it back on.
<!-- MODULE guard -->
- **Python for the guard:** the base image has no `python3`, so the guard module's shell-shape
  guard (`agent/`) needs the pinned `python` feature of the shipped file.
  - The feature runs with `installTools` off, which keeps its unpinned pipx tools out; the guard
    needs the interpreter only (derived from the feature's install script; no image built).
  - The guard and its self-test need `python3` 3.9 or newer.
  - In the devcontainer the guard is active wherever that feature runs, and the session-start
    report says so (working-with-ai-agents.md, "The shell-shape guard").
  - A session started on the host runs on the host: its `python3` is whatever the host has, so the
    guard is active there where that is 3.9 or newer.
    - Where the host's `python3` fails (a Windows Store stub), the guard is inactive, the report
      says so and the hook never blocks.
<!-- /MODULE guard -->
<!-- MODULE acceptance -->
- **A browser for the acceptance scenarios:** `firefox-esr` comes in one fenced layer of the
  shipped `Dockerfile`, never in `postCreateCommand`, since the CI preset builds the same folder.
  - Nothing else runs in the container, so the leftover sweep may take the `ancestry` form here:
    it removes every driver or browser no living test process descends from
    ([testing.md](testing.md), "The browser half").
<!-- /MODULE acceptance -->

> **ADAPT:** pin by digest where the project wants a fresh machine to get exactly the verified
> image. The template ships neither a digest nor a lock file.
>
> - The base image is pinned by digest, its tag kept for readability.
> - Every feature is pinned by digest in a committed `devcontainer-lock.json`.
> - The lock file is regenerated with `devcontainer upgrade --workspace-folder .`.
> - A feature referenced by its exact version is bumped together with the lock file.
> - The pinned image build adds `--frozen-lockfile` (CLI 0.87.0 and later), which fails the build
>   unless the lock file exists and stays unchanged:
>   `npx --yes @devcontainers/cli@<version> build --workspace-folder . --frozen-lockfile`.
> - The CI preset's image job runs the devcontainers CLI unpinned (`npx --yes @devcontainers/cli`).
> - A digest pins the base and a lock file the features; an apt layer and the features' own
>   downloads still take the current build, so pin them by version where it matters.

### What `.devcontainer/` tracks

- `devcontainer.json`: the build, features, environment, mounts, `postCreateCommand`, editor
  extensions.
- `Dockerfile`: the base image and the layers features cannot give.
- `.claude-home/.gitignore`, which keeps the rest of that directory untracked (below).

> **ADAPT:** add each further file the project tracks there, such as a committed
> `devcontainer-lock.json`.

### Claude Code's home (`.devcontainer/.claude-home`)

- **It is bind-mounted on the container user's `~/.claude`**, so Claude Code's login, memory and
  local settings survive a rebuild.
- **`containerEnv` sets `CLAUDE_CONFIG_DIR` to that path**, so Claude Code keeps its configuration
  there.
- **Its `.gitignore` ignores everything but itself, and `content-gate` refuses the rest by name:**
  any path under `.devcontainer/.claude-home/` but that `.gitignore`, and a `.credentials.json`
  anywhere, so not even `git add -f` commits the login token or the transcripts
  ([hooks/README.md](../../hooks/README.md)).
- **The deny `Edit(**/.devcontainer/.claude-home/**)` keeps the file tools out of the home**
  ([working-with-ai-agents.md](working-with-ai-agents.md), "Autonomy within guardrails";
  [probe record](permission-probe-record.md), "Review-fixes probe of 2026-11-14 and 15", D1 to
  D4).
  - Claude Code's own writes to `~/.claude` are not file-tool edits of that path, so the deny
    leaves its login, memory and settings working; a file-tool write under `~/.claude` met only
    the harness's own check of a sensitive file (D6).
  - The folder stays readable to the Read tool; its token file is read-denied by
    `Read(**/.credentials.json)`.

<!-- MODULE volumes -->
### Worktrees on a volume

- **The named volume `{{PROJECT_NAME}}-worktrees` is mounted on `/worktrees`.**
- **`containerEnv` names that path in `AGENT_WORKTREES`**, which makes it the worktree root
  ([engineering-handbook.md](engineering-handbook.md) §7).
- **It pays where Docker Desktop runs on Windows or macOS:** there a volume performs much better
  than a bind mount from the host (Docker's volumes documentation).
  - With Docker Engine on a Linux host the bind mount is native, so the module brings nothing
    there (derived).
- **`.git` stays in the primary checkout on the bind mount.**
  - Each volume worktree's `.git` file names `/workspaces/<folder>/.git/worktrees/<name>`.
  - The admin folder's `gitdir` file names `/worktrees/<name>/.git`.
- **The volume survives a rebuild**, and a rebuild changes neither path, so the pointers hold.
- **A moved or renamed checkout folder breaks the pointers:** `git worktree repair <worktree>`, run
  in the moved primary checkout, mends them.
- **The volume holds the only copy of a worktree's uncommitted files:** unstaged changes, untracked
  files and ignored ones, such as the gate module's dossier under `gate.local.d/`.
  - Staged changes and commits live in `.git`, which holds every worktree's index and objects.
- **Deleting the volume deletes those files.**
  - Docker refuses to remove a volume that a container uses (Docker's `docker volume rm`
    reference), so a deletion follows the container's removal and is the author's act.
- **Two clones of the project on one machine share the volume**, so their worktree names must
  differ (derived).
- **Every command in the container runs as the container user `vscode`.**
  - Docker creates a fresh volume owned by root, which `vscode` cannot write, so
    `postCreateCommand` ends with `sudo chown -R vscode:vscode /worktrees`.
    - It stands last, so a failing `chown` leaves the git hooks wired.
  - A file a root shell writes on the volume stays root's, and `vscode` cannot change it; the
    `-R` of that link repairs it at the next container creation.
  - Where the checkout shows as `vscode`'s, root's git refuses it for dubious ownership, since the
    base image sets no `safe.directory`.
    - Where `safe.directory` admits root, root's git leaves objects in `.git` that `vscode` cannot
      write, so a later commit fails (derived; not probed).
    - `sudo chown -R vscode:vscode .git` in the container repairs that; the author runs it
      (working-with-ai-agents.md, "Autonomy within guardrails").
  - On Docker Desktop for Windows, engine 29.9.2, the bind-mounted checkout showed as `root:root`
    with mode `777`, `.git` included, so `vscode`'s git refused it for dubious ownership until
    `safe.directory` named the checkout (probed).
    - The probe does not show who set `safe.directory`; a devcontainer client may add it itself.
    - Where the client sets none, the `postCreateCommand` chain stops at its first `git config`
      (derived): the author runs `git config --global --add safe.directory <checkout>` in the
      container, then runs the chain again; a rebuilt container has lost that setting, so the
      step repeats after each rebuild.
- **A session on the host does not reach the volume's worktrees by their paths:** its Read, Grep,
  Edit and git do not see `/worktrees`.
  - On Windows, Docker Desktop shows the files under its WSL share, a path no worktree pointer
    names, so the host's git does not resolve them.
  - Such a session, and every subagent it starts, works in a volume worktree only through
    `docker exec -u vscode <container> …`, where the deny floor holds only as a written rule
    ([working-with-ai-agents.md](working-with-ai-agents.md), "Autonomy within guardrails").
  - `containerEnv` does not reach the host, so such a session lacks `AGENT_WORKTREES` and uses
    the default root (handbook §7).

> **ADAPT:** keep this note only where the host runs Windows: Git Bash rewrites `/worktrees/…` in a
> `docker` argument into a Windows path unless the command runs with `MSYS_NO_PATHCONV=1`.

- **A git that cannot see the volume misreads its worktrees** (the rule on prune and cleanup:
  handbook §7).
  - The host's git and a container without the volume are such gits.
  - It lists each volume worktree as `prunable`.
  - `git worktree prune` there drops the registration, and `git worktree repair` cannot bring it
    back, since the worktree's index and `HEAD` go with it.
- **`postCreateCommand` sets `gc.worktreePruneExpire` to `never`** in the repository's
  `.git/config`, which the host's git reads too, so no automatic gc drops a registration.
  - Why: a triggered automatic gc prunes the registration of a worktree it cannot see once the
    index file of that registration is older than three months by default.
    - A commit or an `add` in the worktree refreshes that clock; `git status`, `log` and `diff` in
      an untouched worktree refresh nothing (probed in a container, git 2.54).
  - It holds for the default root too, where the cleanup's explicit prune removes a forgotten
    registration (handbook §7).
<!-- /MODULE volumes -->

### How to work in it

1. Open the repo in an editor with devcontainer support (e.g. VS Code with the Dev Containers
   extension).
2. **Reopen in Container**; the first build provisions the toolchain.
3. Run the AI agent (e.g. Claude Code) **from the container's integrated terminal**, so the agent,
   its builds and its tests run with the container's toolchain (what the container does not keep
   from the agent: above).

> Note: a session started on the *host* runs on the host, with whatever quirks the host shell has.
> "Work in the container" means starting the session from the in-container terminal. A host session
> also lacks the container's toolchain, so a commit whose gate needs it (`./verify format`, a stack
> script's self-test) fails there: commit from inside the container.

<!-- MODULE volumes -->
> Note: with the volumes module strands run from a session in the container; what a host session
> can reach: "Worktrees on a volume", above.
<!-- /MODULE volumes -->

> Note: a `.devcontainer/` change takes effect only on the author's next rebuild, made by the
> host's tooling.
>
> - Where an agent reaches a container engine, it can prove the change builds with a pinned
>   devcontainers CLI: `npx --yes @devcontainers/cli@<version> build --workspace-folder .`.
>   - From CLI 0.87.0 a build writes `devcontainer-lock.json` beside the config; a project that
>     commits none adds `--no-lockfile`.
> - Only the author's rebuild shows the change works in use.

> Note: the Claude Code VS Code extension can race the editor's own file-watcher. When it edits a file
> the IDE is watching (a worktree file, especially with a second Claude session active), a read taken
> right after the edit may transiently show the old content and a follow-up edit may fail to find its
> target, even though the write landed and commits fine once things settle. It is a presentation race,
> not an external process rewriting the file (an idle file is stable; no `git restore` runs). Trust
> `git diff` / `git show` over a `grep` taken mid-edit; if writes look reverted, apply the change and
> commit in one shell process (write, `git add`, `git commit`) so the commit captures the content, then
> verify `HEAD`.

## Variant B: native pinned toolchain

No container: tools run on the developer's machine, but **only through pinned versions** from a
committed version manifest (e.g. mise/asdf `.tool-versions`; shipped in the template under
`env/native/`), never through whatever happens to be on `PATH`.

What replaces the container's guarantees:

- **Pinning:** the manifest and the build file's toolchain declaration state the same version
  ("Toolchain version" below); an upgrade changes them together.
- **Hooks activation and push default:** no `postCreate` exists; run
  `git config core.hooksPath hooks` and set the push default
  ([build-run-deploy.md](build-run-deploy.md), "Git hooks") once per checkout, or wire both into
  your bootstrap script.
- **git version:** 2.31 or newer for the hooks.
  - `hooks/self-test` refuses an older git.
  - Why: `hooks/content-gate` reads a commit with `--diff-merges`, which arrived in that version.
- **Isolation:** there is none; agents execute directly on the host, so the committed permission
  policy (`.claude/settings.json` deny floor) matters even more. Prefer variant A when isolation is
  a concern, within the limits its section names.
- **One machine's path in the settings:** `/adopt` writes the checkout's absolute root into the
  committed settings' path-bearing rules (the ones its step 6 names), so those rules hold for a
  checkout at that path alone; at any other path they match nothing.
  - They are the one exception to the committed file's portability rule
    ([working-with-ai-agents.md](working-with-ai-agents.md), "Committed and local settings").
- **A Windows host:** `/adopt` notes it (its step 1) and recommends variant A with the sessions
  inside the container (its step 3, question 5); a native Windows checkout is a named narrowing
  (its step 6).
  - The path-bearing rules of `.claude/settings.json` carry the root in the form the session's
    shell prints (Git Bash: `/c/...`); no shipped script normalises a Windows path.
  - The rules are probed on that host (permission-probe-record.md, "Method").
  - The project records the narrowing here: the form the rules' paths take, and that the probes
    ran on this host.
  - The harness may offer a PowerShell tool beside Bash there; the committed `deny` names that
    tool as a whole, and why: working-with-ai-agents.md, "Autonomy within guardrails".
    - A named limit: the template has not probed that deny on a Windows host; the project's own
      probe on its host shows whether it holds there.
<!-- MODULE guard -->
- **Python for the guard:** the guard module's shell-shape guard is active natively only where the
  manifest pins `python` (3.9 or newer); without the pin it stays out and the session-start report
  says so (working-with-ai-agents.md, "The shell-shape guard").
  - It compares Linux paths only, so on a native Windows checkout `/adopt` sets it `inactive` (its
    step 5) and the session-start report says at every start that it is off.
<!-- /MODULE guard -->
<!-- MODULE acceptance -->
- **A browser for the acceptance scenarios:** natively the host's own Firefox runs them.
  - The scenarios fail without it, naming the missing browser and `ACCEPTANCE_NO_BROWSER`
    ([testing.md](testing.md), "The browser half").
  - The leftover sweep takes the `marker` form: it removes only a driver or browser whose command
    line names this checkout's `build/` directory, so the session's own browser survives.
  - The `ancestry` form removes every such process on the machine and has no place here.
  - The shipped Java files assume Linux: Unix paths, `perl` for the sweep test, and `firefox` on
    the `PATH`; on another host system the adopter adapts them or runs variant A.
<!-- /MODULE acceptance -->

## Rule for all contributors (both variants)

**Never build, test, or run the app through an unpinned host toolchain.** Variant A: everything runs
inside the devcontainer or via containers. Variant B: everything runs through the manifest's pinned
toolchain. The rule applies to all contributors and agents; it is restated in each tool's entry file
(e.g. `CLAUDE.md`) and in the briefs of the agents that run builds, under
[`.claude/agents/`](../../.claude/agents/).

## Several sessions on one machine

Sessions in parallel worktrees share one machine: its CPU, memory, disk and `/tmp`.

- **Build and test through `./verify`:** it queues the builds and test runs of every worktree on
  one machine-wide lock, so nobody locks by hand (the mechanics:
  [build-run-deploy.md](build-run-deploy.md), "The verify script (the one entry point)").
- **A direct build-tool run takes no lock**, so it loads the machine beside a build that holds it.
- **Give a `./verify` call a timeout that covers the builds queued ahead of it**, or run it in the
  background.
  - Why: a run queued behind the lock waits before it builds, so its call's timeout has to cover
    that wait too.
- **Run `./verify stop` in a worktree before it is removed**, so nothing its builds left running
  outlives it.

<!-- MODULE agent-seat -->
## Where agents run

| Machine | Environment | GitHub credential | Reach beyond the container | Supervision |
|---|---|---|---|---|
| A supervised workstation | this devcontainer, or the pinned native toolchain | the author's own | the host's Docker socket where the project mounts it, or natively the whole host | the author starts and steers every session |
| The agent seat | an agent container | a deploy key and the App's key (below) | the project's to record (below) | unattended |

> **ADAPT:** replace the two example rows with the project's machines, one row per machine and
> environment.

- **The agent seat is marked by `AGENT_SEAT=unattended`**, set in the seat container's environment
  only, never in the repository.
  - Where the variable is unset or reads anything else, the session counts as supervised.
  - It switches pushing and pull requests on
    ([engineering-handbook.md](engineering-handbook.md) §7).
  - It lifts the review loop's round cap (handbook §9).
- **Unattended agents run only on the seat.**
  - Why: on a supervised machine an agent acts with the author's own credentials (below).
- **No unattended agent runs in a container that holds the host's Docker socket.**
  - Why: a container started through that socket can bind-mount the host's file system, every
    credential stored there included.
- **One identity per machine.**
  - The seat holds a deploy key with write access and the App's key (below).
  - The seat holds none of the author's credentials.
  - A supervised machine acts with the author's own credentials, so nothing on the hub tells its
    agent from the author.

> **ADAPT:** record the seat's reach beyond its container: what it reaches, e.g. the internet and a
> Docker daemon of its own ("Docker on the agent seat", below), and what it does not, e.g. the
> local network.

- **A commit the seat has not pushed lives only in its container.**
- **The seat's `AGENT_WORKTREES` is unset, or empty with the volumes module** ("The agent image",
  below), so its worktree root is the default of handbook §7, "The worktree root".
- **Auto mode on the seat is the seat machine's user settings**, as every `autoMode` rule is
  ([working-with-ai-agents.md](working-with-ai-agents.md), "Where auto-mode rules live").
- **A planned session runs on the seat unless its work must run on a supervised machine.**
  - The machines' load never decides where a session runs.
- **The landing workflow, the rulesets and `/land` are proven live by the first adopter**, since
  the template's own repository runs none of them ([build-run-deploy.md](build-run-deploy.md),
  "The landing workflow"; [git-state.md](git-state.md), "What GitHub enforces").
- **The seat talks to the GitHub API as an App of its own.**
  - The author owns the App.
  - It is installed on this repository only.
  - Pull requests and issues are writable for it.
  - Contents, checks and statuses are read-only for it.
  - It is on no bypass list.
  - No run of the template proves the App in use; the project that sets it up does.
    - The first check after its setup is a merge attempt by the App, seen refused.
  - `AGENT_APP_ID` names its id.
  - `AGENT_APP_KEY` names the path of its private key, mounted read-only from outside every
    repository.
  - `agent/github-token <owner>/<repository>` mints an installation token, valid for an hour and
    for that repository only.
  - `<project>/agent/gh pr …` runs a pull request command as the App.
    - It runs only where `AGENT_SEAT` reads `unattended`, as `agent/push` does; elsewhere it
      refuses before minting, exit 2, whatever App values the environment holds.
    - It mints a token for the repository that the checkout's `origin` names on GitHub.
    - It hands the token to `gh` in `GH_TOKEN`, never on a command line.
    - It runs only `create`, `edit`, `view`, `list`, `diff`, `checks`, `comment` and `close`, so a
      merge, a review and a checkout stay outside it.
    - Its options are only `--base`, `--head`, `--title`, `--json`, `--comments` and
      `--body-file`.
      - A value follows its option as the next word or after `=`.
      - `--comments` takes no value.
      - `--base` and `--head` take a branch name: made of `[A-Za-z0-9._/-]` only, without `..`
        and not starting with `-`.
      - `--json` takes field names of letters only, joined by commas.
      - `--title` takes any text.
      - `--body-file` takes a path.
    - It takes at most one selector: a number, `#<number>` or a branch name as above.
      - `create` and `list` take none.
      - `view`, `edit`, `diff`, `checks` and `comment` take exactly one: `agent/gh` passes a call
        without one on, and `gh` refuses it once `-R` is set (read off `gh`'s source, not run).
      - `close` takes a number and nothing else.
    - It refuses every other word before minting, exit 2, with a line that names the word.
      - So no URL, `--search`, `-R` or `--repo` of the caller names another repository.
    - Every `gh` call runs as `gh pr <subcommand> -R github.com/<owner>/<repository> …`: the
      subcommand first, then `origin`'s repository with its host.
    - Every `gh` call gets an empty private configuration folder in `GH_CONFIG_DIR`, removed on
      exit and on a signal.
      - Both are there so that `gh` runs its own `pr`, not an alias or an extension named `-R`,
        and no alias, socket or host of a `gh` configuration applies.
    - `gh` runs without `GH_FORCE_TTY`, `GH_HOST`, `GH_REPO`, `GH_DEBUG`, `DEBUG` and every other
      token variable of the environment.
    - `gh` runs with `GH_NO_UPDATE_NOTIFIER=1`, `GH_PROMPT_DISABLED=1` and `GH_PAGER=cat`.
      - Why: no pager the environment names receives the token.
    - Named limits (derived, not probed):
      - `GH_TOKEN` reaches every child process `gh` starts, such as the `git` that `pr create`
        and `checks` run in the working directory, whose configured programs (`core.fsmonitor`)
        inherit it;
      - `gh` follows `HTTPS_PROXY` with `SSL_CERT_FILE` or `SSL_CERT_DIR`, which can route the
        token through a proxy that reads it;
      - each call mints a token that carries all of the App's permissions, and the token is not
        revoked when the call ends, so it stays valid for its hour.
    - `close` closes only a bot's pull request: it reads the head branch and whether the head
      lies in another repository first, and only a same-repository head that starts with the
      prefix in the `bot_prefix` ADAPT line of `agent/gh` closes.
      - With an empty prefix no pull request closes.
      - A fork's pull request stays open, whatever its head is named.
    - `edit` and `comment` act on any pull request of the repository: `close` alone reads the
      head first.
    - The shell-shape guard's approval of it:
      [working-with-ai-agents.md](working-with-ai-agents.md), "Permissions & autonomy".
  - `agent/self-test` proves the minting against a throwaway key and a stubbed `curl`.
  - It proves `agent/gh` against a stubbed `gh`.
- **The seat may offer read-only snapshots of production data.**
  - Where `AGENT_SNAPSHOT_DIR` is set, it names a read-only mount that the seat's administration
    fills.
  - The variable is unset by default, and then there are no snapshots.
  - The rules for using a snapshot: [testing.md](testing.md), "Synthetic committed fixtures, zero
    PII".

> **ADAPT:** how many snapshots the mount keeps and the name that stands for the newest, e.g. the
> newest three and `latest`.

### Docker on the agent seat

- **The seat container runs a Docker daemon of its own**, so it needs no socket of the host's
  daemon (above).
- **Root in the seat container should be unprivileged on the host.**
  - A user-namespace runtime, such as Sysbox, maps it to an unprivileged host user; it is one way,
    not a requirement.
  - A privileged seat container can take control of its host: the docker-in-docker point of the
    ADAPT note under "Variant A: devcontainer (recommended)".
- **The seat machine starts and configures `dockerd`** (storage driver, log rotation, address
  pools, DNS, build-cache limit), never the repository.
  - The agent image installs Docker Engine and starts no daemon ("The agent image", below).
- **The seat machine starts the seat container with an init as its first process**, such as
  `docker run --init` (Docker's `docker run` reference) or compose's `init: true` (the Compose
  file reference).
  - Why: the image's command, `sleep infinity`, never collects a finished child, and each
    `git commit` leaves one when git's automatic maintenance detaches, its default; the zombies
    pile up until no new process starts in the container
    ([permission-probe-record.md](permission-probe-record.md), "Seat-start probe of
    2026-11-26").
  - A container's init is set when it is created (derived: `--init` is an option of `docker run`
    and `docker create`, not of `docker restart`): a seat started without one is created anew
    with it; a restart only clears the zombies until they pile up again.
- **`vscode` reaches the daemon through the `docker` group.**
- **The inner daemon shares the seat container's filesystem and network.**
  - A bind-mount source is a path inside the seat container.
  - A published port answers on the seat container's `localhost`.
- **With the acceptance module the browser grid stays off the seat:** the grid is a recipe for
  attended sessions.

> **ADAPT:** record what runs on the seat's daemon, e.g. a release image build or a service a test
> starts, and what does not.

### The agent image (`.devcontainer/agent/`; shipped in the template under `env/devcontainer/`)

The image the seat runs: a layer on the devcontainer image, so the seat runs the devcontainer's
toolchain.

- **`Dockerfile`** builds on the devcontainer image, which `build` passes as `ARG BASE`.
  - `ARG BASE` defaults to `base-image-required-pass-build-arg-base`, which names no image, so a
    build without the argument fails at `FROM`.
  - It inherits every layer of that image: the base image, the features and the layers of the
    devcontainer's `Dockerfile`, with the acceptance module its `firefox-esr` layer.
  - It blanks the inherited `devcontainer.metadata` label to `[]` (below).
  - With the volumes module it sets `AGENT_WORKTREES` empty ("What the blank label does not
    undo", below).
  - It runs `provision` as root, then `install-claude-code` as `vscode`, each bind-mounted for its
    step only, with `~/.local/bin` on the `PATH`.
  - It sets `DISABLE_AUTOUPDATER=1`, so Claude Code does not update itself at run time and the
    seat runs the pinned version.
  - It ends as `vscode` in `/workspaces` with `CMD ["sleep", "infinity"]` and no entrypoint of its
    own, so a plain `docker run` stays alive for a look inside; the seat's start adds the init
    ("Docker on the agent seat", above).
- **Why the label is blank:** the devcontainers CLI writes the devcontainer's configuration into
  that label, and a devcontainer tool that starts the image applies it (the Dev Container
  specification, "Image Metadata").
  - For the template's devcontainer it holds the `.claude-home` bind mount, `CLAUDE_CONFIG_DIR`,
    the `postCreateCommand` and the editor customizations, beside the base image's and each
    feature's entries.
  - With the volumes module it also holds the named volume on `/worktrees` and `AGENT_WORKTREES`.
  - A feature that mounts the host's Docker socket, such as `docker-outside-of-docker`, adds that
    mount to it.
  - With the label blank no devcontainer tool applies any of them: what the seat container mounts
    is the seat machine's.
- **What the blank label does not undo:** the devcontainers CLI also writes the `containerEnv`
  entries and each feature's environment into the devcontainer image as `ENV`, and the agent
  image inherits them ([permission-probe-record.md](permission-probe-record.md), "Agent-image
  probe of 2026-11-24").
  - Docker cannot remove an inherited variable, only set it anew (Dockerfile reference, `ENV`).
  - The features' variables, `PATH` and the toolchains' homes, are the toolchain the seat runs.
  - `CLAUDE_CONFIG_DIR` names `/home/vscode/.claude`, Claude Code's default folder (Claude Code's
    settings documentation) and the seat's mount point for it, so it stays.
    - Why not empty: how Claude Code reads an empty value is not documented and not probed.
  - With the volumes module `AGENT_WORKTREES=/worktrees` would make `/worktrees` the seat's
    worktree root, where the seat mounts no volume, so the agent `Dockerfile` sets it empty; an
    empty value counts as unset ([engineering-handbook.md](engineering-handbook.md) §7, "The
    worktree root").

> **ADAPT:** a `containerEnv` entry of your own reaches the seat image too: set it anew in the
> agent `Dockerfile` where the seat must not have it, and add the line to the `Dockerfile` case of
> `.devcontainer/agent/self-test`.

- **`provision`**, as root:
  - refuses before any call unless `/etc/os-release` names `ID=debian` and the codename of its
    `debian_release` line, a missing file included;
  - installs `tmux`, which keeps a session alive when its connection drops;
  - installs Docker Engine from Docker's Debian repository: `docker-ce`, `docker-ce-cli`,
    `containerd.io`, `docker-buildx-plugin` and `docker-compose-plugin`, each at its pin;
  - fetches Docker's apt key over HTTPS only and refuses it unless the file holds exactly one
    primary key, public or secret, with the fingerprint `9DC858229FC7DD38854AE2D88D81803C0EBFCD88`;
    - apt trusts nothing from Docker before that check;
  - writes the keyring and a `docker.list` that names it as `signed-by`, for the Debian release in
    its `debian_release` line;
  - sets `core.hooksPath hooks` and `push.default nothing` in the system git configuration, so
    every clone on the seat runs its tracked `hooks/` and a `git push` without a refspec pushes
    nothing ([build-run-deploy.md](build-run-deploy.md), "Git hooks (local enforcement)");
    - Why: a clone the seat makes gets both without a `postCreateCommand`.
    - A global or repository setting overrides it, and a checkout without `hooks/` runs none.
    - The path is relative, so every repository on the seat that holds an executable `hooks/`
      runs it, a third party's included: clone only trusted repositories there, or override the
      setting in a repository.
  - adds `vscode` to the `docker` group;
  - creates the mount points, owned by `vscode`: `/workspaces`, `~/.claude`, every folder its
    `cache_dirs` line names (`~/.npm` as shipped), and `~/.ssh` with mode `0700`;
    - `~/.local` is none: Claude Code is installed there, and a volume on it would hide the pinned
      version behind the volume's.
  - empties the apt lists.
- **`install-claude-code`**, as `vscode`:
  - refuses before any call a `version` line that is not `<digits>.<digits>.<digits>`;
  - fetches Claude Code's native installer from `claude.ai` over HTTPS only and installs the
    version its `version` line pins;
  - fails unless `claude --version` then reports that version;
  - a volume the seat machine mounts on `~/.local` all the same hides the image's Claude Code:
    the seat then runs the volume's version, not the pin.
- **The pins** are constants, each under an ADAPT note in the script that uses it, so a rebuild
  installs the same versions of what they pin:
  - Claude Code in the `version` line of `install-claude-code`;
  - the devcontainers CLI in the `devcontainer_cli` line of `build`;
  - the Docker packages in the `docker_version` (`docker-ce`, `docker-ce-cli`),
    `containerd_version`, `buildx_version` and `compose_version` lines of `provision`, bumped
    together;
  - the Debian release they are built for in the `debian_release` line of `provision`, the release
    of the base image the devcontainer's `Dockerfile` names.
  - Three inputs float, so two builds of one commit can differ under the same revision labels:
    the base image's tag, the features (their tags where no lockfile is committed, and their own
    downloads), and the apt packages.
  - Docker's key fingerprint is Docker's, no ADAPT point.
  - `/adopt` sets the pins and the Debian release above (its step 5).
- **`build --tag <image:tag>`** builds the image of the checked-out commit.
  - It runs from the repository root of a clean, full clone:
    `env/devcontainer/.devcontainer/agent/build` in the template, `.devcontainer/agent/build` in
    an adopted project.
  - It finds the devcontainer folder from its own path, the folder above `agent/`, and the
    devcontainers CLI's workspace folder above that.
  - It needs `git` 2.32 or later, `npx` (for the devcontainers CLI) and Docker where it runs.
    - Why: the snapshot's repository keeps the global configuration out through
      `GIT_CONFIG_GLOBAL`, which git reads from 2.32 on.
  - It builds both images from a snapshot of `HEAD` in a temporary folder of its own, never from
    the checkout.
    - A repository of the build's own in that folder writes it: it borrows the checkout's object
      store (`objects/info/alternates`), reads `HEAD` into its index (`git read-tree`) and writes
      that out (`git checkout-index -a --prefix`).
    - That repository runs with no system or global configuration, no system attributes file, no
      `core.attributesFile` and no inherited `GIT_*` variable, so only the commit's own
      `.gitattributes` applies, as in a fresh clone on a machine without git configuration.
    - A git-ignored `.gitattributes`, `.git/info/attributes`, the system and the global
      attributes files, and a filter driver from the repository's, the global (the XDG one
      included) or an inherited configuration change nothing in it.
    - A relative `TMPDIR` is resolved once, against the working directory.
    - `export-ignore` and `export-subst` attributes change nothing in it.
    - Git-ignored files, such as the content of `.claude-home/`, are not in it.
  - It builds the devcontainer image first, through `npx --yes` with the pinned CLI, with the
    snapshot's copy of the workspace folder, under a temporary tag of its own,
    `agent-seat-base:<HEAD>-<random>`.
  - Then it builds the agent layer on top of it, from the snapshot's `agent/` folder, tagged as
    asked.
  - It removes the temporary tag and the temporary folder on every exit: a refusal, a failure,
    and `HUP`, `INT`, `PIPE`, `QUIT` or `TERM`; a removal that fails or cannot be checked fails
    the run.
  - It pushes nothing and logs in nowhere.
  - Named limits:
    - a submodule's content is not in the snapshot, so it reaches neither image;
    - a filter the commit's `.gitattributes` names has no driver in the snapshot's repository and
      does not run, so an LFS-tracked file reaches both images as its pointer file;
    - the clean check compares file stats: under `core.trustctime=false` or
      `core.checkStat=minimal` it misses a same-size edit, and both builds still get the
      committed bytes;
    - a `SIGKILL` leaves the temporary folder and the temporary tag behind;
    - `build` does not check git's version: under a git before 2.32 the user's global
      configuration applies in the snapshot's repository.
  - It reaches the network: npm for the CLI, the registries of the base image and the features,
    Docker's key and packages, and the Claude Code installer.
- **The lockfile:** `build` passes `--frozen-lockfile` where `HEAD`'s tree holds a
  `devcontainer-lock.json` beside `devcontainer.json`, and `--no-lockfile` where it holds none.
  - Why: the CLI's `--frozen-lockfile` fails without a lockfile, and a lockfile the CLI wrote
    would make the image differ from the labelled commit.
- **The labels**, each commit named by its full hash:
  - `org.opencontainers.image.revision`: `HEAD`;
  - `agent-seat.devcontainer.revision`: the last commit that changed the devcontainer folder;
  - `org.opencontainers.image.source`: the value of the `source_url` ADAPT line, the repository's
    URL (`/adopt` step 8);
    - a line still holding the placeholder builds, and the label then holds the placeholder.
- **What `build` refuses before it calls `npx` or `docker`**, in this order:
  - with exit 2 and its usage line, a call other than `--tag <image:tag>`, or a tag outside
    Docker's reference grammar;
  - with exit 1 and a line opening `[agent-build]`:
    - a temporary folder that cannot be made;
    - a folder `build` sits in that cannot be entered;
    - a working directory that is not the root of a git checkout;
    - a temporary folder that cannot be entered, or one inside the checkout, where the snapshot
      would read as a change (`TMPDIR` then names a folder outside it);
    - a `build` outside `env/devcontainer/.devcontainer/agent/` and `.devcontainer/agent/` of
      that root;
    - a devcontainer folder without `devcontainer.json`;
    - a checkout whose status git cannot read;
    - a changed or untracked file, ignored files aside;
      - a changed submodule counts, also where the repository tells `git status` to ignore it;
      - the check runs with fsmonitor off, so an fsmonitor hook hides nothing;
    - a checkout whose index git cannot read;
    - a change hidden from `git status`: an assume-unchanged or skip-worktree entry, or a sparse
      checkout;
    - a shallow clone, which reads the last commit of the devcontainer folder as its boundary
      commit;
    - a devcontainer folder git never committed;
    - a committed `devcontainer-lock.json` git cannot look up;
    - at the snapshot: an object format or object store git cannot name, a snapshot repository
      git cannot make or that cannot borrow the object store, a commit git cannot read into its
      index, and a snapshot git cannot write.
- **What `build` refuses after the devcontainer image**, before the agent layer: a change to the
  checkout and a moved `HEAD`.
- **Neither a replace ref nor a graft changes the commit a label names**, and a replace ref cannot
  make a changed checkout look clean.
- **Nothing machine-specific:** no daemon configuration, address, path of the seat machine or
  credential; how the seat runs the image is the seat machine's own administration.
- **`self-test`** proves the three scripts and the `Dockerfile` with every outside command stubbed,
  without network or Docker:
  - `build` in both layouts against throwaway repositories: the interface, the snapshot both
    builds get, the lockfile rule, the three labels, the removal of the temporary tag and folder,
    and every refusal above but three, which have no case of their own: the folder `build` sits
    in that cannot be entered, an index and a lockfile git cannot read;
    - the snapshot holds the committed bytes and no git-ignored file, and the commit's own
      `.gitattributes` applies;
    - the export attributes, a git-ignored `.gitattributes`, `.git/info/attributes`, a global
      attributes file, and a filter driver from the repository's, the global (the XDG one
      included) or an inherited configuration change nothing in it;
    - the snapshot's checkout runs with the system attributes file off, checked by its
      environment, since the suite writes no system file;
  - `provision` under a scratch root (its `--root` option): the `/etc/os-release` refusals, the
    key's refusals, a failure at each step, the keyring and `docker.list`, the pinned installs,
    the system git configuration, the group and the mount points;
  - `install-claude-code`: the pinned version asked for, the refusal of another, and the refusal
    of a malformed `version` line;
  - the `Dockerfile`: the blanked label, `AGENT_WORKTREES` set empty where `devcontainer.json`
    sets it, the two scripts with their users, and its end.
  - It needs `git` and `ssh-keygen`; without one it exits 2 before any case, with a line opening
    `agent image self-test: `.
  - Its case for a tag with a letter outside ASCII runs under a UTF-8 locale that `locale -a`
    lists and under which `[a-z]` matches a letter outside ASCII.
    - Where none is listed (`C.UTF-8` alone does not do), it prints a `skip` line with the case
      and the reason, counted neither as passed nor as failed, and the suite still exits 0.
    - Git Bash on Windows lists none, so there a container run proves the case.
  - A system git configuration, kept out by `GIT_CONFIG_NOSYSTEM`, has no case: it would need a
    root that writes `/etc/gitconfig`.
  - `pre-commit` runs it ([build-run-deploy.md](build-run-deploy.md), "Git hooks (local
    enforcement)").
  - It builds no image: a real build is checked by hand, with `docker image inspect <tag>` for the
    labels and `Config.Env`, and `claude --version` in the image.
<!-- /MODULE agent-seat -->

## Toolchain version (upgrade procedure)

Pin the toolchain version in as few places as possible, and list **every** place it appears so an
upgrade touches them together in one change: typically the build file's toolchain declaration, plus
the devcontainer feature version (variant A) or the version manifest (variant B), and any production
image base. The standing goal: a version bump is one documented change, rebuilt and verified with
`./verify all`, with nothing installed ad hoc.

- **Pin the distribution as well as the version:** the dev toolchain comes from the distribution
  the production image builds on (example: the same JDK vendor).
  - Why: two distributions of one version can differ in bundled components and defaults.

> **ADAPT:** list the concrete pinned locations for your stack and the exact upgrade steps.

## Production image

> **ADAPT:** how the deployable container image is built and where (in the devcontainer, on a
> separate container host if the devcontainer has no nested container support, or on the native
> machine's container runtime for variant B). Secrets are injected at runtime, never baked into the
> image (handbook §3). Delete this section if the project does not ship a container. Runtime
> details: [build-run-deploy.md](build-run-deploy.md).

- **An image build that runs the tests needs every file the build and its tests read in its build
  context**, such as a test that reads the docs; the image's build-context ignore file (for
  Docker, `.dockerignore`) lets exactly those through.
