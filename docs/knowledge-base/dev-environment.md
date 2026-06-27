# 12 — Dev Environment & Virtualization

**Principle: the dev environment is fully virtualized. Nothing — JDK, Gradle, Firefox — is installed
or run on the host.** All work (builds, tests, the app, and the AI agents) runs inside the
**devcontainer** or via Docker. This keeps the host clean and makes toolchain upgrades (e.g. a future
Java bump) a one-line image change instead of a host installation.

## The devcontainer (`.devcontainer/devcontainer.json`)

- Base image `mcr.microsoft.com/devcontainers/base:bookworm` (a stable, always-available base) with
  **JDK 25 via the `java` feature** (`version: 25`, Maven off, Gradle off). The build runs through the
  committed **Gradle wrapper** (`./gradlew`), which needs only a JDK; the `postCreateCommand` warms up
  with `./gradlew build -x test`. The JDK version is single-sourced here (the feature `version`).
- VS Code extensions: Claude Code + Java pack (the Java pack is the *editor* language server /
  IntelliSense — distinct from the JDK; optional for the Gradle/agent-driven flow).
- **Node.js (LTS) via the `node` feature** — for the planned frontend rewrite (Angular or React,
  undecided) and Node-based dev tooling; the Java build/runtime does not use it. The standalone `node`
  feature installs via **nvm** (downloaded binaries), *not* the yarn apt repo whose GPG key once broke
  `apt` when `sonarlint` pulled node (see the lesson below), so it should be safe — but the image builds
  on the **host** (no docker-in-docker, H2), so this only takes effect on the next rebuild; verify there.
- **Deferred** (removed to get a building container; re-add when needed):
  `docker-outside-of-docker` → selenium-firefox compose service (backlog **H2**);
  `sonarlint` → static analysis (backlog **G6**);
  a cross-rebuild **Maven cache** → a named volume at `~/.m2` was root-owned and broke the `vscode`
  user's `~/.m2` (re-add with correct ownership, backlog **H5**).

> Lessons (2026-06-13): the original template installed **Gradle** for a **Maven** project on a JDK
> **25** base. A first fix pinned `java:1-17-bookworm`, but **that image tag doesn't exist**
> (`No manifest found`). Separately, the **`sonarlint` feature pulls `node` → the yarn apt repo,
> whose GPG key failed**, breaking `apt-get update` and making `docker-outside-of-docker` fail to
> install. Resolution: a generic `base` image + the `java` feature, with the two failure-prone
> features deferred. A named-volume Maven cache then failed too (root-owned volume vs the `vscode`
> user → `Could not create local repository at /home/vscode/.m2`), so it was removed pending a
> correctly-owned cache (H5).

## How to work in it (recommended)

1. Open the repo in **VS Code** with the **Dev Containers** extension.
2. **Reopen in Container** (`F1 → Dev Containers: Reopen in Container`). First build provisions JDK 25; the Gradle wrapper fetches Gradle on first use.
3. Run **Claude Code from the container's integrated terminal** → all agents then execute **inside the
   container**, never on the host. Builds/tests run there too.

> Note: a Claude Code session started on the *host* (like the bootstrap session) runs on the host —
> that's why the host has a Bash-stdout quirk and CRLF history. Inside the container, Bash behaves
> normally. "Work in the container" means starting the session from the in-container terminal.

## Rule for all contributors

**Never install or run toolchains (JDK/Gradle/Firefox) natively on the host.** Run `./gradlew …`, the
app, and the scraper only inside the devcontainer or via Docker. This applies to all contributors and
agents; it is restated in each tool's entry file (e.g. `CLAUDE.md`) and in each agent definition under
`.claude/agents/`.

## JDK version is single-sourced (upgrade procedure)

The Java version is pinned in places that must stay in sync (**currently `25`**):

1. `build.gradle.kts` → `java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }`.
2. `.devcontainer/devcontainer.json` → the `java` feature `version`.
3. `Dockerfile` → build stage base (`eclipse-temurin:25-jdk`) and the JDK copied into the runtime stage.

The **Gradle** distribution version is pinned separately in `gradle/wrapper/gradle-wrapper.properties`
(currently `9.5.1`; Java 25 needs Gradle ≥ 9.1). The build toolchain JDK is auto-provisioned by the
foojay resolver if not already present, so a host/devcontainer JDK mismatch self-heals.

**To upgrade (e.g. to a future Java):** bump the toolchain in `build.gradle.kts` plus the devcontainer
and Dockerfile bases together, rebuild the container, run `./gradlew build`. Nothing is installed on
the host. Tracked as backlog **H4** (make it a single, documented switch).

## Production image (`Dockerfile`)

Multi-stage: `eclipse-temurin:25-jdk` build (`./gradlew clean test installDist`) → `selenium/
standalone-firefox` runtime with the JDK 25 copied in and the application distribution at
`/opt/protocolParser`. The `dos2unix`/jar-name hacks are gone (LF enforced via `.gitattributes`; the
distribution dir name is version-independent), and a `.dockerignore` keeps the context lean. It still
**bakes `zugang.txt` (secrets) into the image** — secret injection is backlog **C3**. The image is not
built inside the devcontainer (no docker-in-docker — backlog **H2**); build/validate it on the Docker host.

## Selenium in-container (deferred — backlog H2)

Scraping needs a browser. Plan: a **docker-compose** dev setup with a `selenium/standalone-firefox`
service; tests connect via `RemoteWebDriver` to it. This retires the bundled Windows
`gecko-*-win.exe` drivers and lets scraping/integration tests run anywhere. **Near-term unit/TDD
work (evaluator, parser) needs no browser**, so this isn't blocking. (Re-adding the
`docker-outside-of-docker` feature is part of H2.)

See backlog **Epic H** for the open items, and [build-run-deploy.md](build-run-deploy.md) for the
production runtime details.
