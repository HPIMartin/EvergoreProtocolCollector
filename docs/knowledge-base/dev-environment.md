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
  after a rebuild. Cheap enough to leave alone; add a third volume if it ever annoys.

### `devcontainer.json` features (all digest-pinned in `devcontainer-lock.json`)

| Feature | Setting | Why |
|---|---|---|
| `java` | `version: 25`, `jdkDistro: tem`, Maven off, Gradle off | The JDK; version single-sourced here (see below). Temurin, the distribution the production image builds on (`eclipse-temurin:25-jdk`). |
| `node` | `version: 24.18.0` | IDE tooling only; **must match `gradle.properties` → `nodeVersion`**. |
| `python` | defaults | Utility scripting. |
| `github-cli:1.1.3` | defaults | `gh` for PR/issue metadata (Dependabot triage needs more than the git refs). |
| `docker-outside-of-docker:1.10.1` | `moby: false` | Docker CE CLI against the **host** daemon: image build, deploy and H2's Selenium service from inside the container. |
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
  `postCreate` starts with `sudo chown -R vscode:vscode` on both paths.

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
| The agent machine | an agent container, driven through a Claude Code Remote Control server | a deploy key with write access, `origin` over SSH, and for the API the `epc-agent` App's key once the author has mounted it (below); none of the author's credentials | none, as the author states it: no Docker socket, no access to the local network | unattended |

- **Unattended agents run only on the agent machine; the PC stays simple** (decision 2026-10-04,
  [open-questions.md](../open-questions.md)).
- **An agent on the PC holds the author's rights in practice**, through the credential and the
  Docker socket in the table; accepted on purpose. What GitHub binds: [git-state.md](git-state.md),
  "What GitHub enforces".
- **The agent seat is marked by `EPC_SEAT=agent-machine`**, set in the agent container's
  environment only, never in the repository; it lifts the review round cap (handbook §9). Where it
  is unset or reads anything else, the session counts as supervised.
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
- **The agent container is built by hand;** a reproducible build from `.devcontainer/` is a strand
  of its own. A commit it has not pushed lives only in that container.

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
  under `.devcontainer/` only take effect on the author's next rebuild.) An agent therefore
  cannot validate a `.devcontainer/` change at all; only that rebuild proves it.

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
