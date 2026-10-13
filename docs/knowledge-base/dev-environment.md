# 12: Dev Environment & Virtualization

- **Principle: fully virtualized.** Nothing (JDK, Gradle, Firefox) is installed or run on the host;
  builds, tests and the app run in the **devcontainer** or via Docker, and so do the AI agents,
  except a session started on the host ("Where agents run").
- Why: clean host; toolchain upgrades (e.g. a future Java bump) become a one-line image change.

## The devcontainer (`.devcontainer/`)

Two files: `Dockerfile` (base image + apt tools) and `devcontainer.json` (features, mounts,
postCreate). The base image is pinned by digest, every feature by digest in
`devcontainer-lock.json`. Regenerate it with `devcontainer upgrade --workspace-folder .`
(`@devcontainers/cli` resolves straight from the registry, no Docker needed).

### `Dockerfile`

- Base `mcr.microsoft.com/devcontainers/base:bookworm`, **pinned by digest** (tag kept for
  readability, so Dependabot's `docker` ecosystem can bump tag + digest together). Generic base +
  features is deliberate: pinned Java image tags proved unreliable (e.g. `java:1-17-bookworm` does
  not exist, `No manifest found`).
- One `apt-get` layer with `bc`, `firefox-esr`, `git-filter-repo`, `sqlite3` (~103 MB / 84
  packages). Baked into the image, **not** installed in `postCreate`, so a rebuild replays the
  cached layer instead of re-downloading.
- `firefox-esr` makes the scrape path (`browser=docker`) locally runnable with **no code change**:
  `Browser.DOCKER` builds a local headless `FirefoxDriver` and Selenium Manager fetches the matching
  geckodriver into `~/.cache/selenium` itself (0.37.1 against Firefox ESR 140, verified 2026-08-01).
  `DockerBrowserSmokeTest` pins it ([testing.md](testing.md)). This is the interim step; the planned
  end state stays the remote `selenium/standalone-firefox` service (backlog **H2**), which retires it.
- `~/.cache` is **not** on a volume, so the geckodriver (a few MB) and the Trivy DB are re-fetched
  after a rebuild. Cheap enough to leave alone; add a volume for it if it ever annoys.

### `devcontainer.json` features (all digest-pinned in `devcontainer-lock.json`)

| Feature | Setting | Why |
|---|---|---|
| `java` | `version: 25`, `jdkDistro: tem`, Maven off, Gradle off | The JDK; version single-sourced here (see below). Temurin, the distribution the production image builds on (`eclipse-temurin:25-jdk`). |
| `node` | `version: 24.18.0` | IDE tooling only; **must match `gradle.properties` → `nodeVersion`**. |
| `python` | defaults | Utility scripting. |
| `github-cli:1.1.3` | defaults | `gh` for PR/issue metadata (Dependabot triage needs more than the git refs). |
| `docker-outside-of-docker:1.10.1` | `moby: false` | Docker CE CLI against the **host** daemon: image build, deploy and H2's Selenium service from inside the container. The agent image adds a pinned Engine of its own on top of it (below). |
| `trivy` | defaults | `./gradlew vulnScan` ([build-run-deploy.md](build-run-deploy.md)). |

- `github-cli` and `docker-outside-of-docker` are referenced by their exact version, so a rebuild
  takes no feature release the lockfile has not recorded; bump the tag and regenerate the lockfile
  together.
- The `node` feature reference is `node:2` (**major 2**, tag `1` would stay on 1.x forever). The 2.0
  break is the removal of the default yarn-v1 install; this project uses npm, so it does not apply.
- The exact `24.18.0` is a second place holding the Node version. The `:frontend` build does **not**
  read it: `node-gradle` downloads its own pinned Node per machine (`gradle.properties` →
  `nodeVersion`, see [frontend.md](frontend.md)), so the build stays reproducible either way. Pin
  both to the same value and bump them together.
- **Not deferred any more:** `docker-outside-of-docker` (re-added, above) and the cross-rebuild
  caches (below). `sonarlint`: the third-party feature is a **no-op** (its `install.sh` only
  `echo`s; the only effect is two VS Code extensions plus a `dependsOn` on `node`), so the
  `SonarSource.sonarlint-vscode` extension is declared directly instead. Its old GPG failure came
  from the `node` feature's yarn apt repo, which node 2.x no longer uses by default (backlog
  **G6**).

### Caches persisted across rebuilds

- Named volumes `evergore-gradle` → `~/.gradle` and `evergore-npm` → `~/.npm`. Without them a
  rebuild discards ~1 GB of Gradle caches (deps, wrapper dists, build cache).
- Docker creates a fresh volume **root-owned**, which is what broke the earlier `~/.m2` attempt, so
  `postCreate` starts with `sudo chown -R vscode:vscode` on both paths and on `/worktrees` (below).

### Worktrees on a volume

- **The named volume `evergore-worktrees` is mounted on `/worktrees`.**
- **`containerEnv` names that path in `EPC_WORKTREES`**, which makes it the worktree root of
  handbook §7 on the PC.
- **Why:** the Windows bind mount (9p/drvfs) is much slower for file-heavy steps (decision
  2026-10-12, [open-questions.md](../open-questions.md)).
- **`.git` stays in the primary checkout on the bind mount;** only the checked-out files move.
- **The volume survives a rebuild** like the caches.
- **Its worktrees keep resolving after a rebuild:** each one's `.git` file points at
  `/workspaces/EvergoreProtocolCollector/.git`, a path the rebuild does not change.
- **The volume holds the only copy of a worktree's uncommitted files:** unstaged changes, untracked
  files and ignored ones such as the gate dossier under `gate.local.d/`.
  - Staged changes and commits survive in `.git`, which holds every worktree's index and objects.
- **Deleting the volume deletes those files:** `docker volume rm`, `docker volume prune --all`, or a
  reset of Docker Desktop or of its WSL disk.
- **Run every command in the container as `vscode`** (`docker exec -u vscode` from the host).
  - A session started in the container's VS Code terminal runs as `vscode`; a bare `docker exec`
    runs as `root`.
  - `vscode` cannot edit the files of a worktree a `root` shell created.
  - A `root` shell that writes to `.git` leaves directories `vscode` cannot write, so a later commit
    as `vscode` fails with `insufficient permission`.
  - `sudo chown -R vscode:vscode .git` in the container repairs it.
- **A session on the Windows host cannot see them:** Read, Grep, Edit and host git do not reach
  `/worktrees`.
  - Such a session, and every subagent it starts, works in a worktree only through
    `docker exec -u vscode <container> …`.
  - No deny rule sees the command inside `docker exec`; the deny floor binds there by rule
    ([working-with-ai-agents.md](working-with-ai-agents.md), "Permissions & autonomy"), since the
    volume holds the only copy of uncommitted files.
  - `docker exec` carries the container's `EPC_WORKTREES`, so a worktree created that way lands on
    the volume.
  - From Git Bash, `MSYS_NO_PATHCONV=1` keeps it from rewriting `/worktrees/…` into a Windows
    path.
  - A strand is best driven from a session in the container ("How to work in it", below).
- **Why a container without the volume misreads the worktrees** (rule: handbook §7):
  - Git lists a worktree whose directory it cannot see as `prunable`, and `git worktree prune`
    drops its registration.
  - `git gc` prunes such a registration on its own once it is older than
    `gc.worktreePruneExpire` (three months by default).
  - Host git lists every worktree under `/worktrees` as `prunable`.
  - A container without the volume does too.
- **The agent seat leaves `EPC_WORKTREES` unset,** so its root is the default of handbook §7.
  - The agent image blanks the `devcontainer.metadata` label, which carries `containerEnv` and the
    mounts ("The agent image", below), so it inherits neither.
  - A volume for the seat would change its mounts and environment, which the agent machine's
    administration owns.

### `postCreateCommand`

Chained with `&&`, no `|| true`: `chown` → `git config core.hooksPath hooks` →
`git config push.default nothing` → `./gradlew --no-daemon build -x test`. It **aborts on the
first failure by design**, so a first start without network fails visibly rather than leaving the
container half-configured.

### VS Code extensions

Claude Code, Java pack (the *editor* language server, distinct from the JDK), Checkstyle, SonarLint,
GitLens.

### The IDE's Java null analysis is off (`.vscode/settings.json`)

`java.compile.nullAnalysis.mode` is **`disabled`**, not `automatic` (the IDE null-analysis item, decided
2026-09-21, open-questions.md). `automatic` turns itself on because jspecify 1.0.0 sits transitively
on the compile classpath, then the language server's JDT compiler runs annotation-based null *type*
analysis over a codebase that carries no null annotation at all: 136 of 152 findings were `@NonNull`
type arguments JDT infers itself and immediately reports as unchecked conversions, not defects. A
panel that size trains the reader to ignore it, hiding the handful of findings `javac` genuinely
cannot see. Adopting jspecify project-wide (`@NullMarked` + annotations) was the alternative; nothing
enforces it today since `javac` reads none of it (that would be backlog **G6**).

## How to work in it (recommended)

1. Open the repo in **VS Code** with the **Dev Containers** extension.
2. **Reopen in Container** (`F1 → Dev Containers: Reopen in Container`). First build provisions
   JDK 25; the Gradle wrapper fetches Gradle on first use.
3. Run **Claude Code from the container's integrated terminal**: all agents, builds and tests then
   execute inside the container, never on the host.

> A session started on the *host* (like the bootstrap session) runs on the host: hence the host's
> Bash-stdout quirk and CRLF history; in-container Bash is normal. "Work in the container" means
> starting the session from the in-container terminal.

> Note: the Claude Code VS Code extension can race the editor's own file-watcher. When it edits a file
> the IDE is watching (a worktree file, especially with a second Claude session active), a read taken
> right after the edit may transiently show the old content and a follow-up edit may fail to find its
> target, even though the write landed and commits fine once things settle. It is a presentation race,
> not an external process rewriting the file (an idle file is stable; no `git restore` runs). Trust
> `git diff` / `git show` over a `grep` taken mid-edit; if writes look reverted, apply the change and
> commit in one shell process (write, `git add`, `git commit`) so the commit captures the content, then
> verify `HEAD`.

## Several sessions on one machine

Sessions in parallel worktrees share the container: its CPU, memory, disk and Gradle user home.

- **Build and test through `./verify`.** Its `focus`, `bdd`, `all` and `vuln` queue on one
  machine-wide lock, so a gate build never runs beside another, while the commit hooks' `format`
  check never waits; its Gradle runs keep their daemons in the worktree's own registry, so no other
  session can reach them. Nobody locks by hand or sets a registry. The direct Gradle tasks
  (`./gradlew probe`, `./gradlew clearProbes`) and a direct `npm` run take neither, and a direct
  test run loads the machine beside a gate build. The mechanics are in
  [build-run-deploy.md](build-run-deploy.md), "The verify script".
- **A run may first wait out the builds queued ahead of it**, a full `./verify all` each, and says
  so (`[verify] waiting for …`); give the call a timeout that covers the wait, or run it in the
  background.
- **Stop daemons with `./verify stop`**, after a batch of builds and before a worktree is removed;
  it stops this worktree's daemons only. Never `./gradlew --stop`: it stops every daemon of the
  shared registry, other sessions' direct Gradle runs included.

## Where agents run

| Machine | Environment | GitHub credential | Reach beyond the container | Supervision |
|---|---|---|---|---|
| The author's PC | this devcontainer | the author's own, forwarded by VS Code's Dev Containers credential helper | the host's Docker socket (`docker-outside-of-docker`) | the author starts and steers every session |
| The author's PC, a session started on the host | the Windows host, no container | the author's own, from the host's credential store | the whole host | the author's own session |
| The agent machine | an agent container from the agent image (below), driven through a Claude Code Remote Control server | a deploy key with write access, `origin` over SSH, and for the API the `epc-agent` App's key once the author has mounted it (below); none of the author's credentials | the internet; a Docker daemon of its own inside the container, no host socket; no access to the local network, for the container and every container it starts, as the author states it | unattended |

- **Unattended agents run only on the agent machine; the PC stays simple** (decision 2026-10-04,
  [open-questions.md](../open-questions.md)).
- **An agent on the PC holds the author's rights in practice**, through the credential and the
  Docker socket in the table; accepted on purpose. What GitHub binds: [git-state.md](git-state.md),
  "What GitHub enforces".
- **The PC keeps the host's Docker socket** (decision 2026-10-13,
  [open-questions.md](../open-questions.md)): a container started through it can bind-mount the
  host's file system, every credential stored there included.
- **The agent seat is marked by `EPC_SEAT=agent-machine`**, set in the agent container's
  environment only, never in the repository; it lifts the review round cap (handbook §9). Where it
  is unset or reads anything else, the session counts as supervised.
- **Auto mode in the agent container is the agent machine's user settings**, as every `autoMode`
  rule is ("Where auto-mode rules live" in [working-with-ai-agents.md](working-with-ai-agents.md)).
- **The agent seat talks to the GitHub API as the App `epc-agent`**, author-owned and installed on
  this repository only, with pull requests and issues writable and contents, checks and statuses
  read-only, on no bypass list (author decision 2026-10-08), once the author has created it and
  mounted its key; until then the minting refuses and the author opens the agent's pull requests.
  The container's environment names its App ID in `EPC_AGENT_APP_ID` and the path of its private
  key, mounted read-only from outside every repository, in `EPC_AGENT_KEY`. `agent/github-token
  <owner>/<repository>` mints an installation token, valid for an hour and restricted to that
  repository, which `gh` reads from `GH_TOKEN`: `GH_TOKEN=$(<repo>/agent/github-token
  <owner>/<repository>) gh pr create …`. Its comments come from `epc-agent[bot]`, a `Bot`, which the
  landing workflow ignores. Without contents write it cannot push or merge through the API, and as
  no bypass of the `main` ruleset it is expected to be refused there by any route, an auto-merge it
  enables included; a merge attempt by the App, seen refused, is the first check after its setup.
  `agent/self-test` proves the minting against a throwaway key and a stubbed `curl`: the request,
  the app token's signature, claims and encoding, and every refusal.
- **A commit the agent container has not pushed lives only in that container.**
- **The agent seat may offer read-only snapshots of the production database** (author decision
  2026-10-10, [open-questions.md](../open-questions.md)): where `EPC_SNAPSHOT_DIR` is set, it holds
  them as `epc-<UTC yyyymmdd-hhmmss>.sqlite`, the newest three, with `latest.sqlite` pointing at the
  newest, on a read-only mount; elsewhere the variable is unset and there are none. Open one
  read-only as `jdbc:sqlite:file:<dir>/latest.sqlite?mode=ro&immutable=1`, or copy it into the
  worktree's `build/` before anything writes to it (a migration check, the app, a test on real
  data). Every result names the snapshot it used. A snapshot, or data drawn from one, is never
  committed or uploaded; a pull request quotes only the small excerpts a finding needs. An agent
  cannot take a snapshot: when the newest is too old for the task, it says so and asks the author.

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
    no image, so a build without the argument fails at `FROM`; `build` always passes it.
  - It blanks the inherited `devcontainer.metadata` label, which names the host socket mount of
    `docker-outside-of-docker`, so no devcontainer tooling mounts the host's socket into the agent
    container (author decision 2026-10-12).
  - It runs exactly two scripts, each bind-mounted for its step only: `provision` as root, then
    `install-claude-code` as `vscode`, with `~/.local/bin` on the `PATH`.
- **`provision`** adds, with every version and the key's fingerprint as constants in the script:
  - `tmux`, which keeps a session alive when its connection drops;
  - Docker Engine from Docker's Debian `bookworm` repository: `docker-ce`, `docker-ce-cli`,
    `containerd.io`, `docker-buildx-plugin`, `docker-compose-plugin`:
    - each at a pinned version, so a rebuild installs the versions the agent machine was verified
      against; the pins are bumped together;
    - Docker's key, fetched over HTTPS only, is refused unless the file holds one primary key,
      public or secret, with the fingerprint `9DC858229FC7DD38854AE2D88D81803C0EBFCD88`;
    - its `docker.list` names that keyring as `signed-by` and replaces the one the
      `docker-outside-of-docker` feature wrote;
  - `vscode` in the `docker` group, which reaches the inner daemon through the group's socket;
  - `git config --system` with `core.hooksPath hooks` and `push.default nothing`, so every clone on
    the agent seat, another repository's included, runs its tracked `hooks/` and pushes only what
    a command names ([build-run-deploy.md](build-run-deploy.md), "Git hooks (local enforcement)");
    - a global or repository setting overrides it, and a checkout without `hooks/` runs none;
  - the mount points, owned by `vscode` so a volume or file the agent machine mounts there belongs
    to `vscode`: `/workspaces`, `~/.claude`, `~/.gradle`, `~/.npm`, `~/.local`, and `~/.ssh` with
    mode `0700`.
- **`install-claude-code`** installs the Claude Code version pinned in it through the native
  installer, fetched over HTTPS only, and fails unless `claude --version` then reports that
  version (author decision 2026-10-12, [open-questions.md](../open-questions.md)).
  - A bump is a one-line commit; Claude Code may still update itself at run time.
  - A volume the agent machine mounts on `~/.local` hides the image's Claude Code: the seat then
    runs the volume's version, not the pin.
- **It ends as `vscode` in `/workspaces` with `CMD ["sleep", "infinity"]` and no entrypoint;** the
  agent machine starts it as root with a command of its own (`dockerd`, then the Remote Control
  server as `vscode`), and a plain `docker run` stays alive for a look inside.
- **Nothing machine-specific:** no daemon configuration, no address, no path of the agent machine,
  no credential. How the image runs (compose, start-up, firewall, daemon configuration) is the agent
  machine's own administration, outside this repository.
- **`build --tag <image:tag>`**, from the repository root of a clean, full (not shallow) clone,
  builds the image of that commit:
  - a tag outside Docker's reference grammar is refused before any call;
  - the devcontainer image first, through `npx --yes @devcontainers/cli@<pinned>` with
    `--frozen-lockfile`, under a temporary tag;
  - then the agent layer on top of it, tagged as asked; the temporary tag is removed, and a run
    that cannot check whether it is gone fails;
  - labels, each a full 40-character hash where it names a commit:
    `org.opencontainers.image.revision` (`HEAD`), `epc.devcontainer.revision` (the last commit
    that changed `.devcontainer/`) and `org.opencontainers.image.source` (the repository's URL);
  - it exits non-zero on any failure, a changed or untracked file and a `.devcontainer/` git never
    committed included;
  - a change hidden from `git status` by an assume-unchanged or skip-worktree entry is refused too;
  - the checkout is checked again after the devcontainer image, and a change or a moved `HEAD`
    since the start is refused before the agent layer;
  - a shallow clone is refused because it reads the last `.devcontainer/` commit as its boundary
    commit;
  - it pushes nothing and logs in nowhere.
- **The agent machine's update runs it** from a fresh clone of `main` at a detached commit, as root
  in a throwaway container from the current agent image, against the machine's own Docker daemon.
- **On the PC it runs from the devcontainer** through `docker-outside-of-docker`:
  `.devcontainer/agent/build --tag epc-agent:test`, then `docker image inspect epc-agent:test`
  for the labels. So an agent can prove that a `.devcontainer/` change builds.
- **`self-test`** proves all three scripts with their outside commands stubbed
  ([build-run-deploy.md](build-run-deploy.md), "Git hooks (local enforcement)"):
  - `build` against a throwaway repository: the interface, the three labels, the temporary tag's
    removal and every refusal;
  - `provision` in a scratch root: the key's refusals, a failure at each step, the keyring and
    `docker.list` apt trusts, the pinned installs, the system git configuration, the group and the
    mount points;
  - `install-claude-code`: the exact version asked for and the refusal of another;
  - that the `Dockerfile` is the devcontainer image, its metadata label blanked, plus the two
    scripts and nothing else.

## Rule for all contributors

**Never install or run toolchains (JDK/Gradle/Firefox) natively on the host**; run `./gradlew …`,
the app and the scraper only inside the devcontainer or via Docker. Applies to all contributors and
agents; restated in each tool's entry file (e.g. `CLAUDE.md`) and each agent definition under
`.claude/agents/`.

## JDK version is single-sourced (upgrade procedure)

Java version pinned in places that must stay in sync (**currently `25`**):

1. `build.gradle.kts` → `java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }`.
2. `.devcontainer/devcontainer.json` → the `java` feature `version`.
3. The root `Dockerfile` (production image) → build stage base (`eclipse-temurin:25-jdk`) and the
   JDK copied into the runtime stage. (`.devcontainer/Dockerfile` carries no JDK; the feature does.)

- The **Gradle** version is pinned separately in `gradle/wrapper/gradle-wrapper.properties`
  (currently `9.5.1`; Java 25 needs Gradle ≥ 9.1), together with `distributionSha256Sum`, so the
  wrapper refuses a tampered or swapped distribution. **On a version bump both must change**: take
  the new sum from `<distributionUrl>.sha256` (Gradle publishes one per distribution).
- The build toolchain JDK is auto-provisioned by the foojay resolver, so a host/devcontainer JDK
  mismatch self-heals.
- **To upgrade:** bump the toolchain in `build.gradle.kts` plus the devcontainer and Dockerfile
  bases together, rebuild the container, run `./verify all`; nothing lands on the host.
  (Standing goal: keep this bump a single, documented switch.)
- **Java 25 made `java.sql.Timestamp.from` strict:** its `Math.multiplyExact` throws where JDK 17
  silently wrapped, so converting an extreme instant (`LocalDateTime.MIN`, for one) to a
  `java.sql.Timestamp` now fails. Keep extreme sentinel instants out of any code that converts,
  or the conversion decides the behaviour instead of the domain.
- **ArchUnit must stay at 1.4.1 or newer** to read Java 25 bytecode. An older version checks zero
  classes without saying so, which passes as a false green.

## Production image (root `Dockerfile`)

- Multi-stage: `eclipse-temurin:25-jdk` build (`./gradlew clean check installDist`, gating on the
  frontend's tests and lint too) → `selenium/standalone-firefox` runtime with JDK 25 copied in and
  the application distribution at `/opt/protocolParser`.
- No `dos2unix`/jar-name hacks (LF enforced via `.gitattributes`; version-independent distribution
  dir name); `.dockerignore` keeps the context lean.
- **Carries no secret**: API token and Evergore login are injected as environment variables at
  `docker run` (build-run-deploy.md), so the image is the same for every account.
- Buildable **from inside the devcontainer** since the `docker-outside-of-docker` feature returned:
  the `docker` CLI targets the host daemon, so `docker build` / `docker run` need no host shell.
  (The devcontainer image itself is still built by the host's Dev Containers extension, so changes
  under `.devcontainer/` only take effect on the author's next rebuild.) An agent can prove such a
  change builds, through the agent image's build ("The agent image" above); whether it works in
  use, only that rebuild shows.

## Selenium in-container

- Scraping needs a browser. **Today:** `firefox-esr` from `.devcontainer/Dockerfile`; `browser=docker`
  drives it locally and Selenium Manager supplies the geckodriver. No code change, no service.
- **Planned end state (backlog H2):** a `selenium/standalone-firefox` service with tests connecting
  via `RemoteWebDriver`. That needs `Browser.DOCKER` rebuilt (it constructs a *local* `FirefoxDriver`
  today) plus a configurable hub URL; in exchange the dev browser matches the production runtime and
  `firefox-esr` drops out of the image again. Retires the bundled Windows `gecko-*-win.exe` drivers
  (backlog **F1**).

See backlog **Epic H** for open items; production runtime details:
[build-run-deploy.md](build-run-deploy.md).
