# 12 — Dev Environment & Virtualization

**Principle: the dev environment is fully virtualized. Nothing — JDK, Maven, Firefox — is installed
or run on the host.** All work (builds, tests, the app, and the AI agents) runs inside the
**devcontainer** or via Docker. This keeps the host clean and makes toolchain upgrades (e.g. a future
Java bump) a one-line image change instead of a host installation.

## The devcontainer (`.devcontainer/devcontainer.json`)

- Base image `mcr.microsoft.com/devcontainers/base:bookworm` (a stable, always-available base) with
  **JDK 17 + Maven installed via the `java` feature** (`version: 17`, Maven on, Gradle off — this is
  a Maven project). The JDK version is single-sourced here (the feature `version`).
- `postCreateCommand` pre-downloads dependencies (`mvn dependency:go-offline`, offline-tolerant).
- VS Code extensions: Claude Code + Java pack (the Java pack is the *editor* language server /
  IntelliSense — distinct from the JDK; optional for the mvn/agent-driven flow).
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
2. **Reopen in Container** (`F1 → Dev Containers: Reopen in Container`). First build provisions JDK 17 + Maven.
3. Run **Claude Code from the container's integrated terminal** → all agents then execute **inside the
   container**, never on the host. Builds/tests run there too.

> Note: a Claude Code session started on the *host* (like the bootstrap session) runs on the host —
> that's why the host has a Bash-stdout quirk and CRLF history. Inside the container, Bash behaves
> normally. "Work in the container" means starting the session from the in-container terminal.

## Rule for all agents

**Never install or run toolchains (JDK/Maven/Firefox) natively on the host.** Run `mvn …`, the app,
and the scraper only inside the devcontainer or via Docker. This rule is in `CLAUDE.md` and in each
agent definition under `.claude/agents/`.

## JDK version is single-sourced (upgrade procedure)

The Java version is pinned in **three** places that must stay in sync:

1. `.devcontainer/devcontainer.json` → the `java` feature `version` (currently `17`).
2. `Dockerfile` → build stage base (`maven:…-openjdk-18-slim`) and runtime JRE (`openjdk-17-jre`).
   *(These are currently inconsistent — 18 build / 17 runtime — and are cleaned up in backlog H3/H4.)*
3. `pom.xml` → `<jdk.version>17</jdk.version>`.

**To upgrade (e.g. to Java 25):** bump all three together, rebuild the container, run `mvn verify`.
Nothing is installed on the host. Tracked as backlog **H4** (make this a single, documented switch).

## Production image (`Dockerfile`)

Multi-stage: Maven build → `selenium/standalone-firefox` runtime + JRE. It works but is fragile and
**bakes `zugang.txt` (secrets) into the image**, hardcodes the jar name, and uses `dos2unix`
workarounds (only needed because of CRLF — removable once LF is enforced via `.gitattributes`).
Cleanup is backlog **H3** (and secret handling ties to **C3**).

## Selenium in-container (deferred — backlog H2)

Scraping needs a browser. Plan: a **docker-compose** dev setup with a `selenium/standalone-firefox`
service; tests connect via `RemoteWebDriver` to it. This retires the bundled Windows
`gecko-*-win.exe` drivers and lets scraping/integration tests run anywhere. **Near-term unit/TDD
work (evaluator, parser) needs no browser**, so this isn't blocking. (Re-adding the
`docker-outside-of-docker` feature is part of H2.)

See backlog **Epic H** for the open items, and [build-run-deploy.md](build-run-deploy.md) for the
production runtime details.
