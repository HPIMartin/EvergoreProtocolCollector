# 12: Dev Environment & Virtualization

- **Principle: fully virtualized.** Nothing (JDK, Gradle, Firefox) is installed or run on the host;
  all work (builds, tests, the app, the AI agents) runs in the **devcontainer** or via Docker.
- Why: clean host; toolchain upgrades (e.g. a future Java bump) become a one-line image change.

## The devcontainer (`.devcontainer/devcontainer.json`)

- Base image `mcr.microsoft.com/devcontainers/base:bookworm` (stable, always available) + **JDK 25
  via the `java` feature** (`version: 25`, Maven off, Gradle off). The JDK version is single-sourced
  in that feature `version`. Generic base + feature is deliberate: pinned Java image tags proved
  unreliable (e.g. `java:1-17-bookworm` does not exist, `No manifest found`).
- Build runs through the committed **Gradle wrapper** (`./gradlew`), which needs only a JDK;
  `postCreateCommand` warms up with `./gradlew build -x test`.
- VS Code extensions: Claude Code + Java pack (the *editor* language server, distinct from the JDK;
  optional for the Gradle/agent-driven flow).
- **Node.js (LTS) via the `node` feature, IDE tooling only** (IntelliSense, manual npm scripts).
  The `:frontend` Gradle build does not use it: `node-gradle` downloads its own pinned Node
  (`gradle.properties` → `nodeVersion`, see [frontend.md](frontend.md)) per machine, so the build
  is reproducible regardless of the feature's version. The feature installs via **nvm** (downloaded
  binaries), not the broken yarn apt repo (see the `sonarlint` deferral below), so it should be
  safe; the image builds on the **host** (no docker-in-docker, backlog H2), so it takes effect on
  the next rebuild only; verify there.
- **Deferred** (removed to get a building container; re-add when needed):
  - `docker-outside-of-docker` → selenium-firefox compose service (backlog **H2**).
  - `sonarlint` → static analysis (backlog **G6**): it pulls `node` via the yarn apt repo, whose
    broken GPG key breaks `apt-get update` (and with it the `docker-outside-of-docker` install).
  - cross-rebuild **Maven cache**: the named volume at `~/.m2` was root-owned and broke the
    `vscode` user's `~/.m2` (re-add with correct ownership, backlog **H5**).

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
3. `Dockerfile` → build stage base (`eclipse-temurin:25-jdk`) and the JDK copied into the runtime
   stage.

- The **Gradle** version is pinned separately in `gradle/wrapper/gradle-wrapper.properties`
  (currently `9.5.1`; Java 25 needs Gradle ≥ 9.1).
- The build toolchain JDK is auto-provisioned by the foojay resolver, so a host/devcontainer JDK
  mismatch self-heals.
- **To upgrade:** bump the toolchain in `build.gradle.kts` plus the devcontainer and Dockerfile
  bases together, rebuild the container, run `./gradlew build`; nothing lands on the host.
  (Standing goal: keep this bump a single, documented switch.)

## Production image (`Dockerfile`)

- Multi-stage: `eclipse-temurin:25-jdk` build (`./gradlew clean check installDist`, gating on the
  frontend's tests and lint too) → `selenium/standalone-firefox` runtime with JDK 25 copied in and
  the application distribution at `/opt/protocolParser`.
- No `dos2unix`/jar-name hacks (LF enforced via `.gitattributes`; version-independent distribution
  dir name); `.dockerignore` keeps the context lean.
- Still **bakes `zugang.txt` (secrets) into the image**; secret injection is backlog **C3**.
- Not built inside the devcontainer (no docker-in-docker, backlog **H2**); build/validate on the
  Docker host.

## Selenium in-container (deferred, backlog H2)

- Scraping needs a browser. Plan: **docker-compose** dev setup with a
  `selenium/standalone-firefox` service; tests connect via `RemoteWebDriver`. Retires the bundled
  Windows `gecko-*-win.exe` drivers; scraping/integration tests run anywhere.
- Not blocking: near-term unit/TDD work (evaluator, parser) needs no browser.
- Re-adding the `docker-outside-of-docker` feature is part of backlog H2.

See backlog **Epic H** for open items; production runtime details:
[build-run-deploy.md](build-run-deploy.md).
