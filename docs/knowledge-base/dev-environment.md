# 12 — Dev Environment & Virtualization

**Principle: the dev environment is fully virtualized. Nothing — JDK, Maven, Firefox — is installed
or run on the host.** All work (builds, tests, the app, and the AI agents) runs inside the
**devcontainer** or via Docker. This keeps the host clean and makes toolchain upgrades (e.g. a future
Java bump) a one-line image change instead of a host installation.

## The devcontainer (`.devcontainer/devcontainer.json`)

- Base image `mcr.microsoft.com/devcontainers/java:1-17-bookworm` → **JDK 17** (matches the project).
- Java feature with **Maven** enabled, **Gradle disabled** (this is a Maven project — the old
  template had it backwards).
- `docker-outside-of-docker` → the container can reach the host Docker daemon (for the planned
  selenium-firefox service and for building the production image).
- `~/.m2` mounted as a named volume `evergore-m2` → dependency cache survives rebuilds.
- `postCreateCommand` warms the dependency cache (offline-tolerant).
- VS Code extensions: Claude Code, Java pack, SonarLint. Editor forces LF (`files.eol`).

> The previous devcontainer was a generic template (installed **Gradle** for a **Maven** project,
> JDK **25** base vs the project's **17**) and would not have built the project. Fixed 2026-06-13.

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

1. `.devcontainer/devcontainer.json` → base image tag (`…/java:1-17-bookworm`).
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
work (evaluator, parser) needs no browser**, so this isn't blocking.

See backlog **Epic H** for the open items, and [build-run-deploy.md](build-run-deploy.md) for the
production runtime details.
