# 12: Dev Environment & Virtualization

- **Principle: fully virtualized.** Nothing (JDK, Gradle, Firefox) is installed or run on the host;
  builds, tests and the app run in the **devcontainer** or via Docker, and so do the AI agents,
  except a session started on the host ("Where agents run").
- Why: clean host; a toolchain upgrade changes the build files, never the host.
- **The toolchain is pinned in as few places as possible, every one listed** ("JDK version (upgrade
  procedure)", below).

## The devcontainer (`.devcontainer/`)

- **What `.devcontainer/` tracks:**
  - `Dockerfile` (base image + apt tools);
  - `devcontainer.json` (features, environment, mounts, postCreate);
  - `devcontainer-lock.json` (every feature's digest);
  - `.claude-home/.gitignore`, which keeps the rest of that directory untracked (below);
  - `agent/`, the agent image ("The agent image", below).
- **The base image is pinned by digest, every feature by digest in `devcontainer-lock.json`.**
  Regenerate the lockfile with `devcontainer upgrade --workspace-folder .` (`@devcontainers/cli`
  resolves straight from the registry, no Docker needed).
- **A `.devcontainer/` change takes effect only on the author's next rebuild,** which the host's
  Dev Containers extension makes.
  - An agent can prove it builds ("The agent image", below).
  - Only that rebuild shows it works in use.
- **The container keeps the toolchain off the host, not the host's rights away from the agent.**
  - VS Code's Dev Containers extension forwards the host's git credentials into the container, so
    an agent there acts with the author's git credentials.
    - That forwarded access to the project's own remote is the access an agent is given
      (handbook §7, "No secrets").
  - The host's Docker socket, mounted through `docker-outside-of-docker`, hands over the host: a
    container started through it can bind-mount the host's file system, every credential stored
    there included (the decision to keep it: "Where agents run", below).
- **git version:** 2.31 or newer for the hooks.
  - `hooks/self-test` refuses an older git.
  - Why: `hooks/content-gate` reads a commit with `--diff-merges`, which arrived in that version.

<!-- MODULE agent-seat -->
> Note: with the agent-seat module no unattended agent runs in a container that mounts the host's
> Docker socket ("Where agents run", below).
<!-- /MODULE agent-seat -->

### `Dockerfile`

- Base `mcr.microsoft.com/devcontainers/base:bookworm`, **pinned by digest** (tag kept for
  readability, so Dependabot's `docker` ecosystem can bump tag + digest together). Generic base +
  features is deliberate: the JDK comes from the `java` feature, not from a Java image.
- One `apt-get` layer with `bc`, `firefox-esr` ("Selenium in-container", below),
  `git-filter-repo` and `sqlite3`, each with its purpose and the layer's size in the file's comment.
  Baked into the image, **not** installed in `postCreate`, so a rebuild replays the cached layer
  instead of re-downloading.
- `~/.cache` is **not** on a volume, so the geckodriver (a few MB) and the Trivy DB are re-fetched
  after a rebuild. Cheap enough to leave alone; add a volume for it if it ever annoys.

### `devcontainer.json` features (all digest-pinned in `devcontainer-lock.json`)

| Feature | Setting | Why |
|---|---|---|
| `java` | `version`, `jdkDistro: tem`, Maven off, Gradle off | The JDK; its `version` is one of the places "JDK version (upgrade procedure)" (below) keeps in sync. Temurin, the distribution the production image builds on. |
| `node` | `version` | IDE tooling only. |
| `python` | defaults | Utility scripting; the shell-shape guard's interpreter ("Python for the guard", below). |
| `github-cli` | defaults | `gh` for PR/issue metadata (Dependabot triage needs more than the git refs). |
| `docker-outside-of-docker` | `moby: false` | Docker CE CLI against the **host** daemon: image build, deploy and a Selenium service from inside the container. The agent image adds a pinned Engine of its own on top of it (below). |
| `trivy` | defaults | `./gradlew vulnScan` ([build-run-deploy.md](build-run-deploy.md)). |

- Every feature's reference, version and setting stand in `devcontainer.json`.
- `github-cli` and `docker-outside-of-docker` are referenced by their exact version, so a rebuild
  takes no feature release the lockfile has not recorded; bump the tag and regenerate the lockfile
  together.
- The `node` feature reference is `node:2` (**major 2**, tag `1` would stay on 1.x forever). The 2.0
  break is the removal of the default yarn-v1 install; this project uses npm, so it does not apply.
- The `node` feature's exact `version` is a second place holding the Node version.
  - The `:frontend` build does **not** read it: `node-gradle` downloads its own pinned Node per
    machine (`gradle.properties` → `nodeVersion`, see [frontend.md](frontend.md)), so the build
    stays reproducible either way.
  - Pin both to the same value and bump them together.
- **No `sonarlint` feature:** the third-party feature is a **no-op** (its `install.sh` only
  `echo`s; the only effect is two VS Code extensions plus a `dependsOn` on `node`), so the
  `SonarSource.sonarlint-vscode` extension is declared directly instead.
<!-- MODULE guard -->
- **Python for the guard:** the base image has no `python3`, so the guard module's shell-shape
  guard (`agent/bash-shape-guard`) takes its interpreter from the `python` feature.
  - The guard and its self-test need `python3` 3.9 or newer.
  - In the devcontainer the guard is active wherever that feature runs, and the session-start
    report says so ([working-with-ai-agents.md](working-with-ai-agents.md), "The shell-shape
    guard").
  - A session started on the host runs on the host: its `python3` is whatever the host has, so the
    guard is active there where that is 3.9 or newer.
    - Where the host's `python3` fails (a Windows Store stub), the guard is inactive, the report
      says so and the hook never blocks.
<!-- /MODULE guard -->

### Caches persisted across rebuilds

- Named volumes `evergore-gradle` → `~/.gradle` and `evergore-npm` → `~/.npm`. Without them a
  rebuild discards ~1 GB of Gradle caches (deps, wrapper dists, build cache).
- Docker creates a fresh volume **root-owned**, so `postCreate` starts with
  `sudo chown -R vscode:vscode` on both paths.

### Claude Code's home (`.devcontainer/.claude-home`)

- **`.devcontainer/.claude-home` is bind-mounted on `~/.claude`**, so Claude Code's login, memory,
  local settings and history stay on the host and survive a rebuild.
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

- **The named volume `evergore-worktrees` is mounted on `/worktrees`.**
- **`containerEnv` names that path in `AGENT_WORKTREES`**, which makes it the worktree root of
  handbook §7 on the PC.
- **Why:** the Windows bind mount (9p/drvfs) is much slower for file-heavy steps (decision
  2026-10-12, [open-questions.md](../open-questions.md)).
- **`.git` stays in the primary checkout on the bind mount.**
  - Each volume worktree's `.git` file names
    `/workspaces/EvergoreProtocolCollector/.git/worktrees/<name>`.
  - The admin folder's `gitdir` file names `/worktrees/<name>/.git`.
- **The same `sudo chown -R vscode:vscode` of `postCreateCommand` covers `/worktrees`**, a volume
  Docker creates root-owned like the caches.
- **The volume survives a rebuild** like the caches.
- **Its worktrees keep resolving after a rebuild:** a rebuild changes neither path, so the pointers
  hold.
- **A moved or renamed checkout folder breaks the pointers:** `git worktree repair <worktree>`, run
  in the moved primary checkout, mends them.
- **The volume holds the only copy of a worktree's uncommitted files:** unstaged changes, untracked
  files and ignored ones such as the gate dossier under `gate.local.d/`.
  - Staged changes and commits survive in `.git`, which holds every worktree's index and objects.
- **Deleting the volume deletes those files:** `docker volume rm`, `docker volume prune --all`, or a
  reset of Docker Desktop or of its WSL disk.
  - Both volume commands delete the volume only once no container, running or stopped, mounts it.
  - A deletion is the author's act.
- **Two clones of the project on one machine share the volume**, so their worktree names must
  differ (derived).
- **Run every command in the container as `vscode`** (`docker exec -u vscode` from the host).
  - A session started in the container's VS Code terminal runs as `vscode`.
  - A bare `docker exec` runs as `root`.
  - `vscode` cannot edit the files of a worktree a `root` shell created.
    - The `chown -R` of `postCreateCommand` repairs the volume's files at the next container
      creation.
  - A `root` shell that writes to `.git` leaves directories `vscode` cannot write, so a later commit
    as `vscode` fails with `insufficient permission`.
  - `sudo chown -R vscode:vscode .git` in the container repairs it; the author runs it
    ([working-with-ai-agents.md](working-with-ai-agents.md), "Autonomy within guardrails").
  - Where the checkout shows as `vscode`'s, root's git refuses it for dubious ownership, since the
    base image sets no `safe.directory`.
  - On Docker Desktop for Windows, engine 29.9.2, the bind-mounted checkout showed as `root:root`
    with mode `777`, `.git` included, so `vscode`'s git refused it for dubious ownership until
    `safe.directory` named the checkout (probed by the template).
    - The probe does not show who set `safe.directory`; a devcontainer client may add it itself.
    - Where the client sets none, the `postCreateCommand` chain stops at its first `git config`
      (derived): the author runs `git config --global --add safe.directory <checkout>` in the
      container, then runs the chain again; a rebuilt container has lost that setting, so the
      step repeats after each rebuild.
- **A session on the Windows host cannot see them:** Read, Grep, Edit and host git do not reach
  `/worktrees`.
  - Docker Desktop shows the files under its WSL share, a path no worktree pointer names, so the
    host's git does not resolve them.
  - Such a session, and every subagent it starts, works in a worktree only through
    `docker exec -u vscode <container> …`.
  - What the deny floor covers inside `docker exec`:
    [working-with-ai-agents.md](working-with-ai-agents.md), "Permissions & autonomy".
  - `containerEnv` does not reach the host, so the session itself lacks `AGENT_WORKTREES` and uses
    the default root (handbook §7).
  - `docker exec` carries the container's `AGENT_WORKTREES`, so a worktree created that way lands
    on the volume.
  - From Git Bash, `MSYS_NO_PATHCONV=1` keeps it from rewriting `/worktrees/…` into a Windows
    path.
  - Which session drives a strand: "How to work in it (recommended)", below.
- **Why a container without the volume misreads the worktrees** (rule: handbook §7):
  - Git lists a worktree whose directory it cannot see as `prunable`.
  - `git worktree prune` drops the registration of a `prunable` worktree, and
    `git worktree repair` cannot bring it back, since the worktree's index and `HEAD` go with it.
  - `git gc` prunes such a registration on its own once it is older than
    `gc.worktreePruneExpire` (three months by default).
  - Host git lists every worktree under `/worktrees` as `prunable`.
  - A container without the volume does too.
- **`postCreateCommand` sets `gc.worktreePruneExpire` to `never`** in the repository's
  `.git/config`, which the host's git reads too, so no automatic gc drops a registration.
  - Why: a triggered automatic gc prunes the registration of a worktree it cannot see once the
    index file of that registration is older than three months by default.
    - A commit or an `add` in the worktree refreshes that clock; `git status`, `log` and `diff` in
      an untouched worktree refresh nothing (probed by the template in a container, git 2.54).
  - It holds for the default root too, where the cleanup's explicit prune removes a forgotten
    registration (handbook §7).
<!-- /MODULE volumes -->
<!-- MODULE agent-seat+volumes -->
- **The agent seat runs with `AGENT_WORKTREES` empty,** which the guard and
  `agent/session-check` read as unset, so its root is the default of handbook §7.
  - The devcontainers CLI writes `containerEnv` into the devcontainer image as `ENV` (measured
    2026-11-23 with `docker image inspect` on the PC's devcontainer image).
  - So the agent image inherits `AGENT_WORKTREES=/worktrees` (measured 2026-11-23 on the seat's
    image of `561491a8`).
  - The agent image sets it empty ("The agent image", below; author decision 2026-11-23).
  - The blank `devcontainer.metadata` label keeps the volume's mount off the seat, not the
    variable.
  - A volume for the seat would change its mounts and environment.
  - The agent machine's administration owns those.
<!-- /MODULE agent-seat+volumes -->

### `postCreateCommand`

- **Chained with `&&`, no `|| true`:** `chown` → `git config core.hooksPath hooks` →
  `git config push.default nothing` → `git config gc.worktreePruneExpire never` →
  `./gradlew --no-daemon build -x test`.
- **It aborts on the first failure by design.**
  - Why: a first start without network fails visibly rather than leaving the container
    half-configured.

### VS Code extensions

Claude Code, Java pack (the *editor* language server, distinct from the JDK), Checkstyle, SonarLint,
GitLens.

- Claude Code is the agent.
- GitLens is a compare tool that reads the review hand-off's range
  ([working-with-ai-agents.md](working-with-ai-agents.md), "Handing the author a review").

### The IDE's Java null analysis is off (`.vscode/settings.json`)

- `java.compile.nullAnalysis.mode` is **`disabled`**, not `automatic`.
- Why: in `automatic` the transitive jspecify turns it on and floods the panel with findings that
  are no defects (decision 2026-09-21, [open-questions.md](../open-questions.md)).

## How to work in it (recommended)

1. Open the repo in **VS Code** with the **Dev Containers** extension.
2. **Reopen in Container** (`F1 → Dev Containers: Reopen in Container`). First build provisions
   the JDK; the Gradle wrapper fetches Gradle on first use.
3. Run **Claude Code from the container's integrated terminal**: the agent, its builds and its
   tests then run with the container's toolchain (what the container does not keep from the
   agent: "The devcontainer (`.devcontainer/`)", above).

<!-- MODULE volumes -->
> Note: strands are best driven from a session in the container; what a host session can reach:
> "Worktrees on a volume", above.
<!-- /MODULE volumes -->

- **The Claude Code VS Code extension can race the editor's own file-watcher:**
  - when it edits a file the IDE is watching (a worktree file, especially with a second Claude
    session active), a read taken right after the edit may transiently show the old content;
  - a follow-up edit may fail to find its target, though the write landed;
  - it is a presentation race, not an external process rewriting the file (an idle file is stable;
    no `git restore` runs);
  - trust `git diff` / `git show` over a `grep` taken mid-edit;
  - if writes look reverted, apply the change and commit in one shell process (write, `git add`,
    `git commit`) so the commit captures the content, then verify `HEAD`.

## Several sessions on one machine

Sessions in parallel worktrees share the container: its CPU, memory, disk, `/tmp` and Gradle user
home.

- **Build and test through `./verify`:**
  - it queues the gate builds on one machine-wide lock;
  - it keeps each worktree's Gradle daemons apart;
  - nobody locks by hand or sets a registry (the mechanics:
    [build-run-deploy.md](build-run-deploy.md), "The verify script (the one entry point)").
- **A direct Gradle task or `npm` run takes neither**, so a direct test run loads the machine beside
  a gate build.
- **Give a `./verify` call a timeout that covers the builds queued ahead of it**, or run it in the
  background.
  - Why: a run queued behind the lock waits before it builds, so its call's timeout has to cover
    that wait too.
- **Stop daemons with `./verify stop`**, after a batch of builds and before a worktree is removed,
  so nothing its builds left running outlives it.
- **Never `./gradlew --stop`:** it stops every daemon of the shared registry, other sessions' direct
  Gradle runs included.

<!-- MODULE agent-seat -->
## Where agents run

| Machine | Environment | GitHub credential | Reach beyond the container | Supervision |
|---|---|---|---|---|
| The author's PC | this devcontainer | the author's own, forwarded by VS Code's Dev Containers credential helper | the host's Docker socket (`docker-outside-of-docker`) | the author starts and steers every session |
| The author's PC, a session started on the host | the Windows host, no container; Bash alone, the PowerShell tool being denied | the author's own, from the host's credential store | the whole host | the author's own session |
| The agent machine | an agent container from the agent image (below), driven through a Claude Code Remote Control server | a deploy key and the `epc-agent` App's key (below) | the internet (below) | unattended |

- **A session on the Windows host works through Bash alone:** the committed `deny` names the
  PowerShell tool as a whole ([working-with-ai-agents.md](working-with-ai-agents.md), "Autonomy
  within guardrails").
- **Unattended agents run only on the agent machine; the PC stays simple** (decision 2026-10-04,
  [open-questions.md](../open-questions.md)).
  - Why: on the PC an agent acts with the author's own credentials (below).
- **No unattended agent runs in a container that holds the host's Docker socket.**
  - Why: a container started through that socket can bind-mount the host's file system, every
    credential stored there included.
- **A planned session runs on the agent seat unless its work must run on the PC** (author direction
  2026-10-15, [open-questions.md](../open-questions.md)):
  - work that must run on the PC: a deploy, the home server, a browser login such as the game's;
  - the machines' current load never decides where a session runs.
- **An agent on the PC holds the author's rights in practice**, through the credential and the
  Docker socket in the table:
  - this is accepted on purpose;
  - nothing on the hub tells its agent from the author;
  - what GitHub binds: [git-state.md](git-state.md), "What GitHub enforces".
- **The agent machine's GitHub credentials:**
  - a deploy key with write access, `origin` over SSH;
  - for the API, the `epc-agent` App's key once the author has mounted it (below);
  - none of the author's credentials.
- **The agent machine's reach beyond the container:**
  - the internet;
  - a Docker daemon of its own inside the container, no host socket ("Docker on the agent seat",
    below);
  - no access to the local network, for the container and every container it starts, as the author
    states it.
- **The PC keeps the host's Docker socket** (decision 2026-10-13,
  [open-questions.md](../open-questions.md)): a container started through it can bind-mount the
  host's file system, every credential stored there included.
- **The agent seat is marked by `AGENT_SEAT=unattended`**, set in the agent container's environment
  only, never in the repository.
  - Where it is unset or reads anything else, the session counts as supervised.
  - It switches pushing and pull requests on
    ([engineering-handbook.md](engineering-handbook.md) §7).
  - It lifts the review round cap (handbook §9).
- **Auto mode in the agent container is the agent machine's user settings**, as every `autoMode`
  rule is ("Where auto-mode rules live" in [working-with-ai-agents.md](working-with-ai-agents.md)).
- **The agent seat talks to the GitHub API as the App `epc-agent`** (author decision 2026-10-08):
  - it is author-owned and installed on this repository only;
  - pull requests and issues are writable; contents, checks and statuses read-only;
  - it is on no bypass list;
  - it works once the author has created it and mounted its key;
  - until then the minting refuses and the author opens the agent's pull requests;
  - the container's environment names its App ID in `AGENT_APP_ID`;
  - `AGENT_APP_KEY` names the path of its private key, mounted read-only from outside every
    repository;
  - `agent/github-token <owner>/<repository>` mints an installation token, valid for an hour and
    restricted to that repository;
  - `<repo>/agent/gh pr …` runs a pull request command as the App (author decision 2026-10-17):
    - it runs only where `AGENT_SEAT` reads `unattended`, as `agent/push` does; elsewhere it
      refuses before minting, exit 2, whatever App values the environment holds;
    - it mints a token for the repository the checkout's `origin` names on GitHub;
    - it hands the token to `gh` through `GH_TOKEN`, never on the command line;
    - it runs only `create`, `edit`, `view`, `list`, `diff`, `checks`, `comment` and `close`, so a
      merge, a review and a checkout stay outside it;
    - its options are only `--base`, `--head`, `--title`, `--json`, `--comments` and
      `--body-file`:
      - a value follows its option as the next word or after `=`;
      - `--comments` takes no value;
      - `--base` and `--head` take a branch name: made of `[A-Za-z0-9._/-]` only, without `..`
        and not starting with `-`;
      - `--json` takes field names of letters only, joined by commas;
      - `--title` takes any text;
      - `--body-file` takes a path;
    - it takes at most one selector: a number, `#<number>` or a branch name as above:
      - `create` and `list` take none;
      - `view`, `edit`, `diff`, `checks` and `comment` take exactly one: `agent/gh` passes a call
        without one on, and `gh` refuses it once `-R` is set (read off `gh`'s source, not run);
      - `close` takes a number and nothing else;
    - it refuses every other word before minting, exit 2, with a line that names the word, so no
      URL, `--search`, `-R` or `--repo` of the caller names another repository;
    - every `gh` call runs as `gh pr <subcommand> -R github.com/<owner>/<repository> …`: the
      subcommand first, then `origin`'s repository with its host;
    - every `gh` call gets an empty private configuration folder in `GH_CONFIG_DIR`, removed on
      exit and on a signal;
      - both are there so that `gh` runs its own `pr`, not an alias or an extension named `-R`,
        and no alias, socket or host of a `gh` configuration applies;
    - `gh` runs without `GH_FORCE_TTY`, `GH_HOST`, `GH_REPO`, `GH_DEBUG`, `DEBUG` and every other
      token variable of the environment;
    - `gh` runs with `GH_NO_UPDATE_NOTIFIER=1`, `GH_PROMPT_DISABLED=1` and `GH_PAGER=cat`;
      - why: no pager the environment names receives the token;
    - named limits (derived, not probed):
      - `GH_TOKEN` reaches every child process `gh` starts, such as the `git` that `pr create`
        and `checks` run in the working directory, whose configured programs (`core.fsmonitor`)
        inherit it;
      - `gh` follows `HTTPS_PROXY` with `SSL_CERT_FILE` or `SSL_CERT_DIR`, which can route the
        token through a proxy that reads it;
      - each call mints a token that carries all of the App's permissions, and the token is not
        revoked when the call ends, so it stays valid for its hour;
    - `close` closes only a bot's pull request: it reads the head branch and whether the head lies
      in another repository first, and only a same-repository head that starts with the
      `bot_prefix` of `agent/gh`, `dependabot/`, closes;
      - a fork's pull request stays open, whatever its head is named;
    - `edit` and `comment` act on any pull request of the repository: `close` alone reads the head
      first;
    - the shell-shape guard's approval of it:
      [working-with-ai-agents.md](working-with-ai-agents.md), "Permissions & autonomy";
  - its comments come from `epc-agent[bot]`, a `Bot`, which the landing workflow ignores;
  - without contents write it cannot push or merge through the API;
  - as no bypass of the `main` ruleset it is expected to be refused there by any route, an
    auto-merge it enables included;
  - a merge attempt by the App, seen refused, is the first check after its setup;
  - `agent/self-test` proves the minting against a throwaway key and a stubbed `curl`: the request,
    the app token's signature, claims and encoding, and every refusal;
  - it proves `agent/gh` against a stubbed `gh` in throwaway clones.
- **A commit the agent container has not pushed lives only in that container.**
- **The agent seat may offer read-only snapshots of the production database** (author decision
  2026-10-10, [open-questions.md](../open-questions.md)):
  - where `AGENT_SNAPSHOT_DIR` is set, it holds them on a read-only mount as
    `epc-<UTC yyyymmdd-hhmmss>.sqlite`;
  - it keeps the newest three, with `latest.sqlite` pointing at the newest;
  - elsewhere the variable is unset and there are none;
  - open one read-only as `jdbc:sqlite:file:<dir>/latest.sqlite?mode=ro&immutable=1`, or copy it
    into the worktree's `build/` before anything writes to it (a migration check, the app, a test
    on real data);
  - every result names the snapshot it used;
  - a snapshot, or data drawn from one, is never committed or uploaded;
  - a pull request quotes only the small excerpts a finding needs;
  - an agent cannot take a snapshot:
    - when the newest is too old for the task, it says so;
    - it asks the author.

### Docker on the agent seat

- **The agent container runs its own Docker daemon** under Sysbox: a user namespace, so root in the
  container is unprivileged on the host, and no socket of the host's daemon. The agent machine
  starts `dockerd` and configures it (storage driver, log rotation, subnets, DNS, build-cache
  limit); that configuration is the machine's, never the repository's. `vscode` reaches the daemon
  through the `docker` group.
- **The inner daemon shares the agent container's filesystem and network:** a bind-mount source is a
  path inside the agent container, and a published port answers on its `localhost` (measured by the
  author: `selenium/standalone-firefox` with `-p 4444:4444` ready on `localhost:4444`).
- **What runs there:** the release gate's image build, the deploy rehearsal with throwaway
  credentials ([build-run-deploy.md](build-run-deploy.md), "Deploy to the home server") and a
  Selenium service, `docker run -d -p 4444:4444 --shm-size 2g
  selenium/standalone-firefox:<pinned tag>`, reached on `localhost:4444`.
- **What does not run there:**
  - the browser grid ([build-run-deploy.md](build-run-deploy.md), "The browser grid");
  - the deploy itself, since the home server sits on the local network;
  - tagging, which stays the author's ([build-run-deploy.md](build-run-deploy.md), "Versioning &
    release tags").
- **The agent machine prunes every image unused for 7 days**, so a rehearsal's predecessor image or
  an older build is rebuilt, not assumed present.
- **Why Docker there, and who owns what:** author decision 2026-10-11
  ([open-questions.md](../open-questions.md)).

### The agent image (`.devcontainer/agent/`)

- **`Dockerfile`** is a layer on the devcontainer image, passed as `ARG BASE`, so the agent seat
  runs the toolchain the PC runs.
  - `ARG BASE` defaults to the image name `base-image-required-pass-build-arg-base`, which names
    no image:
    - a build without the argument fails at `FROM`;
    - `build` always passes it.
  - It inherits every layer of that image: the base image, the features and the layers of the
    devcontainer's `Dockerfile`, its `firefox-esr` included.
  - It blanks the inherited `devcontainer.metadata` label to `[]` (author decision 2026-10-12;
    why: below).
<!-- /MODULE agent-seat -->
<!-- MODULE agent-seat+volumes -->
  - It sets `AGENT_WORKTREES` empty (author decision 2026-11-23; why: "What the blank label does
    not undo", below).
<!-- /MODULE agent-seat+volumes -->
<!-- MODULE agent-seat -->
  - It runs exactly two scripts, each bind-mounted for its step only:
    - `provision` runs as root, then `install-claude-code` as `vscode`;
    - `~/.local/bin` is on the `PATH`.
  - It sets `DISABLE_AUTOUPDATER=1`, so Claude Code does not update itself at run time and the
    seat runs the pinned version.
- **Why the label is blank:** the devcontainers CLI writes the devcontainer's configuration into
  that label, and a devcontainer tool that starts the image applies it (the Dev Container
  specification, "Image Metadata").
  - For this devcontainer it holds the mounts (the `.claude-home` bind mount and the named volumes
    on `~/.gradle` and `~/.npm`), `CLAUDE_CONFIG_DIR`, the `postCreateCommand` and the editor
    customizations, beside the base image's and each feature's entries.
<!-- /MODULE agent-seat -->
<!-- MODULE agent-seat+volumes -->
  - It holds the named volume on `/worktrees` and `AGENT_WORKTREES` too ("Worktrees on a
    volume", above).
<!-- /MODULE agent-seat+volumes -->
<!-- MODULE agent-seat -->
  - `docker-outside-of-docker` adds the host socket mount to it.
  - With the label blank no devcontainer tool applies any of them, so none mounts the host's socket
    into the agent container: what the agent container mounts is the agent machine's.
- **What the blank label does not undo:** the devcontainers CLI also writes each feature's
  environment and the devcontainer's `containerEnv` into the image as `ENV`, and a derived image
  inherits it (measured 2026-11-23 with `docker image inspect` on the PC's devcontainer image).
  - So the agent container's environment is the image's `ENV` plus what the agent machine sets.
  - A `containerEnv` entry reaches the seat this way unless the agent `Dockerfile` overrides it.
  - `CLAUDE_CONFIG_DIR` stays inherited, `/home/vscode/.claude`, the folder Claude Code uses by
    default (author decision 2026-11-23).
    - With it set, Claude Code keeps `.claude.json` in that folder too, as the PC's
      `.claude-home` shows; the running seat works with it, and a change would move its login
      state.
<!-- /MODULE agent-seat -->
<!-- MODULE agent-seat+volumes -->
  - The agent `Dockerfile` sets `AGENT_WORKTREES` empty.
    - Why: the seat's image of `561491a8` carried `AGENT_WORKTREES=/worktrees` (measured
      2026-11-23).
    - A container of it printed that value although it had no `/worktrees` directory.
    - A set value makes `/worktrees` the guard's root and leaves the worktrees under
      `.claude/worktrees/` outside it.
    - `ENV` cannot unset a variable; the guard and `agent/session-check` read an empty value as
      unset.
<!-- /MODULE agent-seat+volumes -->
<!-- MODULE agent-seat -->
- **`provision`**, as root, with every version, the Debian release and the key's fingerprint as
  constants in the script:
  - refuses before any call unless `/etc/os-release` names `ID=debian` and the codename of its
    `debian_release` line (`bookworm`), a missing file included;
  - installs `tmux`, which keeps a session alive when its connection drops;
  - installs Docker Engine from Docker's Debian repository: `docker-ce`, `docker-ce-cli`,
    `containerd.io`, `docker-buildx-plugin` and `docker-compose-plugin`:
    - each at a pinned version, so a rebuild installs the versions the agent machine was verified
      against; the pins are bumped together;
    - Docker's key, fetched over HTTPS only, is refused unless the file holds exactly one primary
      key, public or secret, with the fingerprint `9DC858229FC7DD38854AE2D88D81803C0EBFCD88`;
    - apt trusts nothing from Docker before that check;
    - its `docker.list` names that keyring as `signed-by`, for the release in its `debian_release`
      line, and replaces the one the `docker-outside-of-docker` feature wrote;
  - sets `core.hooksPath hooks` and `push.default nothing` in the system git configuration, so
    every clone on the agent seat, another repository's included, runs its tracked `hooks/` and a
    `git push` without a refspec pushes nothing ([build-run-deploy.md](build-run-deploy.md), "Git
    hooks (local enforcement)");
    - why: a clone the seat makes gets both without a `postCreateCommand`;
    - a global or repository setting overrides it, and a checkout without `hooks/` runs none;
    - the path is relative, so every repository on the seat that holds an executable `hooks/`
      runs it, a third party's included: clone only trusted repositories there, or override the
      setting in a repository;
  - adds `vscode` to the `docker` group, which reaches the inner daemon through the group's socket;
  - creates the mount points, owned by `vscode` so a volume or file the agent machine mounts there
    belongs to `vscode`: `/workspaces`, `~/.claude`, every folder its `cache_dirs` line names
    (`.gradle .npm`), and `~/.ssh` with mode `0700`;
    - `~/.local` is none: Claude Code is installed there, and a volume on it would hide the pinned
      version behind the volume's;
  - empties the apt lists.
- **`install-claude-code`**, as `vscode` (author decision 2026-10-12,
  [open-questions.md](../open-questions.md)):
  - refuses before any call a `version` line that is not `<digits>.<digits>.<digits>`;
  - installs the Claude Code version its `version` line pins through the native installer, fetched
    from `claude.ai` over HTTPS only;
  - fails unless `claude --version` then reports that version;
  - a bump is a one-line commit;
  - a volume the agent machine mounts on `~/.local` all the same hides the image's Claude Code: the
    seat then runs the volume's version, not the pin.
- **The pins** are constants, so a rebuild installs the same versions of what they pin:
  - Claude Code in the `version` line of `install-claude-code`;
  - the devcontainers CLI in the `devcontainer_cli` line of `build`;
  - the Docker packages in the `docker_version` (`docker-ce`, `docker-ce-cli`),
    `containerd_version`, `buildx_version` and `compose_version` lines of `provision`;
  - the Debian release in the `debian_release` line of `provision`, the release of the base image
    the devcontainer's `Dockerfile` names;
  - the apt packages and the features' own downloads float, so two builds of one commit can differ
    under the same revision labels;
  - Docker's key fingerprint is Docker's.
- **It ends as `vscode` in `/workspaces` with `CMD ["sleep", "infinity"]` and no entrypoint;** the
  agent machine starts it as root with a command of its own (`dockerd`, then the Remote Control
  server as `vscode`), and a plain `docker run` stays alive for a look inside.
- **Nothing machine-specific:** no daemon configuration, no address, no path of the agent machine,
  no credential. How the image runs (compose, start-up, firewall, daemon configuration) is the agent
  machine's own administration, outside this repository.
- **`build --tag <image:tag>`**, from the repository root of a clean, full (not shallow) clone,
  builds the image of the checked-out commit:
  - it finds the devcontainer folder from its own path, the folder above `agent/`, and the
    devcontainers CLI's workspace folder above that;
  - it needs `git` 2.32 or later, `npx` (for the devcontainers CLI) and Docker where it runs;
    - why: the snapshot's repository keeps the global configuration out through
      `GIT_CONFIG_GLOBAL`, which git reads from 2.32 on;
  - it builds both images from a snapshot of `HEAD` in a temporary folder of its own, never from
    the checkout:
    - a repository of the build's own in that folder writes it: it borrows the checkout's object
      store (`objects/info/alternates`), reads `HEAD` into its index (`git read-tree`) and writes
      that out (`git checkout-index -a --prefix`);
    - that repository runs with no system or global configuration, no system attributes file, no
      `core.attributesFile` and no inherited `GIT_*` variable, so only the commit's own
      `.gitattributes` applies, as in a fresh clone on a machine without git configuration;
    - a git-ignored `.gitattributes`, `.git/info/attributes`, the system and the global
      attributes files, and a filter driver from the repository's, the global (the XDG one
      included) or an inherited configuration change nothing in it;
    - a relative `TMPDIR` is resolved once, against the working directory;
    - `export-ignore` and `export-subst` attributes change nothing in it;
    - git-ignored files, such as the content of `.claude-home/`, are not in it;
  - the devcontainer image first, through `npx --yes` with the pinned CLI, with the snapshot's copy
    of the workspace folder, under a temporary tag of its own, `agent-seat-base:<HEAD>-<random>`;
  - then the agent layer on top of it, from the snapshot's `agent/` folder, tagged as asked;
  - the temporary tag and the temporary folder are removed on every exit: a refusal, a failure,
    and `HUP`, `INT`, `PIPE`, `QUIT` or `TERM`; a removal that fails or cannot be checked fails
    the run;
  - it pushes nothing and logs in nowhere;
  - named limits:
    - a submodule's content is not in the snapshot, so it reaches neither image;
    - a filter the commit's `.gitattributes` names has no driver in the snapshot's repository and
      does not run, so an LFS-tracked file reaches both images as its pointer file;
    - the clean check compares file stats: under `core.trustctime=false` or
      `core.checkStat=minimal` it misses a same-size edit, and both builds still get the
      committed bytes;
    - a `SIGKILL` leaves the temporary folder and the temporary tag behind;
    - `build` does not check git's version: under a git before 2.32 the user's global
      configuration applies in the snapshot's repository;
  - it reaches the network: npm for the CLI, the registries of the base image and the features,
    Docker's key and packages, and the Claude Code installer.
- **The lockfile:** `build` passes `--frozen-lockfile`, since `HEAD`'s tree holds
  `devcontainer-lock.json` beside `devcontainer.json`; a tree without one gets `--no-lockfile`.
  - Why: the CLI's `--frozen-lockfile` fails without a lockfile, and a lockfile the CLI wrote
    would make the image differ from the labelled commit.
- **The labels**, each commit named by its full 40-character hash:
  - `org.opencontainers.image.revision`: `HEAD`;
  - `agent-seat.devcontainer.revision`: the last commit that changed `.devcontainer/`;
  - `org.opencontainers.image.source`: the repository's URL, from the `source_url` line of
    `build`.
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
    - a shallow clone, which reads the last `.devcontainer/` commit as its boundary commit;
    - a `.devcontainer/` git never committed;
    - a committed `devcontainer-lock.json` git cannot look up;
    - at the snapshot: an object format or object store git cannot name, a snapshot repository
      git cannot make or that cannot borrow the object store, a commit git cannot read into its
      index, and a snapshot git cannot write.
- **What `build` refuses after the devcontainer image**, before the agent layer: a change to the
  checkout and a moved `HEAD` since the start.
- **Neither a replace ref nor a graft changes the commit a label names**, and a replace ref cannot
  make a changed checkout look clean.
- **The agent machine's update runs it** from a fresh clone of `main` at a detached commit, as root
  in a throwaway container from the current agent image, against the machine's own Docker daemon.
- **On the PC it runs from the devcontainer** through `docker-outside-of-docker`:
  - `.devcontainer/agent/build --tag epc-agent:test`, then `docker image inspect epc-agent:test`
    for the labels and `claude --version` in the image;
  - what that proves of a `.devcontainer/` change: "The devcontainer (`.devcontainer/`)", above.
- **`self-test`** proves the three scripts and the `Dockerfile` with every outside command stubbed,
  without network or Docker ([build-run-deploy.md](build-run-deploy.md), "Git hooks (local
  enforcement)"; `pre-commit` runs it):
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
  - the `Dockerfile`: the blanked label, the two scripts with their users, and its end;
  - it needs `git` and `ssh-keygen`; without one it exits 2 before any case, with a line opening
    `agent image self-test: `;
  - its case for a tag with a letter outside ASCII runs under a UTF-8 locale that `locale -a`
    lists and under which `[a-z]` matches a letter outside ASCII;
    - where none is listed (`C.UTF-8` alone does not do), it prints a `skip` line with the case
      and the reason, counted neither as passed nor as failed, and the suite still exits 0;
    - Git Bash on Windows lists none, so there a container run proves the case;
  - a system git configuration, kept out by `GIT_CONFIG_NOSYSTEM`, has no case: it would need a
    root that writes `/etc/gitconfig`;
  - it builds no image: a real build is checked by hand, as on the PC above.
<!-- /MODULE agent-seat -->

## Rule for all contributors

- **Never install or run toolchains (JDK/Gradle/Firefox) natively on the host**; run `./gradlew …`,
  the app and the scraper only inside the devcontainer or via Docker.
- Applies to all contributors and agents.
- Restated in the entry files and in the briefs of the agents that run builds.

## JDK version (upgrade procedure)

The Java version stands in places that must stay in sync:

1. `build.gradle.kts` → `java { toolchain { languageVersion.set(JavaLanguageVersion.of(<version>)) } }`.
2. `.devcontainer/devcontainer.json` → the `java` feature `version`.
3. The root `Dockerfile` (production image) → the build stage's `eclipse-temurin` base and the
   JDK copied into the runtime stage. (`.devcontainer/Dockerfile` carries no JDK; the feature does.)
4. `gradle/gradle-daemon-jvm.properties` → `toolchainVersion`, the Gradle daemon's JVM, a file the
   `updateDaemonJvm` task generates.

- **Pin the distribution as well as the version:** the devcontainer's JDK is Temurin, the
  distribution the production image builds on (`jdkDistro: tem`, above).
  - Why: two distributions of one version can differ in bundled components and defaults.
- The **Gradle** version is pinned separately in `gradle/wrapper/gradle-wrapper.properties`
  (the sum that guards the distribution and the bump of both: [build-run-deploy.md](build-run-deploy.md),
  "Wrapper integrity").
  - A Java bump may need a newer Gradle (Java 25 needs Gradle 9.1 or newer).
- The build toolchain JDK is auto-provisioned by the foojay resolver, so a host/devcontainer JDK
  mismatch self-heals.
- **To upgrade:** bump all four places together, rebuild the container, run `./verify all`;
  nothing lands on the host.
- **Java 25's `java.sql.Timestamp.from` is strict:** its `Math.multiplyExact` throws on overflow,
  so converting an extreme instant (`LocalDateTime.MIN`, for one) to a `java.sql.Timestamp`
  fails. Keep extreme sentinel instants out of any code that converts, or the conversion decides
  the behaviour instead of the domain.
- **ArchUnit must stay at 1.4.1 or newer** to read Java 25 bytecode. An older version checks zero
  classes without saying so, which passes as a false green.

## Production image (root `Dockerfile`)

- Its stages, its build from inside the devcontainer and its runtime:
  [build-run-deploy.md](build-run-deploy.md), "Run via Docker (primary path)".
- **Its build stage runs the tests (`check`), so its build context needs every file the build and
  its tests read**, such as a test that reads the docs; `.dockerignore` lets exactly those through.

## Selenium in-container

- **Scraping needs a browser:** `firefox-esr` from `.devcontainer/Dockerfile`, which
  `Browser.DOCKER` drives as a local headless `FirefoxDriver` (`browser=docker`), with no code
  change and no service.
- **Selenium Manager fetches the matching geckodriver** into `~/.cache/selenium` itself.
- `DockerBrowserSmokeTest` pins the path ([testing.md](testing.md)).
- **A remote Selenium service is to replace it** and drop `firefox-esr` from the image
  (backlog H2).

See backlog **Epic H** for open items; production runtime details:
[build-run-deploy.md](build-run-deploy.md).
