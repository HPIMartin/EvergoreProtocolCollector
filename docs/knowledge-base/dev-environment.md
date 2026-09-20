# 12: Dev Environment & Virtualization

- **Principle: fully virtualized.** Nothing (JDK, Gradle, Firefox) is installed or run on the host;
  all work (builds, tests, the app, the AI agents) runs in the **devcontainer** or via Docker.
- Why: clean host; toolchain upgrades (e.g. a future Java bump) become a one-line image change.

## The devcontainer (`.devcontainer/`)

Two files: `Dockerfile` (base image + apt tools) and `devcontainer.json` (features, mounts,
postCreate). The base image is pinned by digest, every feature by digest in
`devcontainer-lock.json` — regenerate it with `devcontainer upgrade --workspace-folder .`
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
| `java` | `version: 25`, Maven off, Gradle off | The JDK; version single-sourced here (see below). |
| `node` | `version: 24.18.0` | IDE tooling only; **must match `gradle.properties` → `nodeVersion`**. |
| `python` | defaults | Utility scripting. |
| `github-cli` | defaults | `gh` for PR/issue metadata (Dependabot triage needs more than the git refs). |
| `docker-outside-of-docker` | `moby: false` | Docker CE CLI against the **host** daemon: image build, deploy and H2's Selenium service from inside the container. |
| `trivy` | defaults | `./gradlew vulnScan` ([build-run-deploy.md](build-run-deploy.md)). |

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
  from the `node` feature's yarn apt repo, which node 2.x no longer uses by default — see backlog
  **G6**.

### Caches persisted across rebuilds

- Named volumes `evergore-gradle` → `~/.gradle` and `evergore-npm` → `~/.npm`. Without them a
  rebuild discards ~1 GB of Gradle caches (deps, wrapper dists, build cache).
- Docker creates a fresh volume **root-owned**, which is what broke the earlier `~/.m2` attempt, so
  `postCreate` starts with `sudo chown -R vscode:vscode` on both paths.

### `postCreateCommand`

Chained with `&&`, no `|| true`: `chown` → `git config core.hooksPath hooks` → `./gradlew
--no-daemon build -x test`. It **aborts on the first failure by design**; the old `;` chain
swallowed errors and left the container half-configured. Consequence: a first start without network
now fails visibly instead of silently.

### VS Code extensions

Claude Code, Java pack (the *editor* language server, distinct from the JDK), Checkstyle, SonarLint,
GitLens.

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
