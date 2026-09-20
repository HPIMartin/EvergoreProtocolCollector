# 04: Build, Run, Deploy

> **ADAPT:** this doc is a skeleton: fill in your build commands, quality gates, runtime
> configuration, endpoints, and deployment path. Keep volatile facts (versions, dependency lists)
> single-sourced in the build file; this prose points at them, never copies them (handbook §3).

## Build

- Build and test: `{{BUILD_CMD}}`. Tests alone: `{{TEST_CMD}}`. The build must **always run green**
  locally; quality and vulnerability *gating* belongs to CI/delivery, never the local build.
- **Warnings are errors:** compiler/lint warnings fail the build; the default response is fix, not
  suppress (handbook §3).

  > **ADAPT:** how your stack enables warnings-as-errors, and any categories excluded deliberately,
  > each with a one-line reason.
- **Formatting:** one shared formatter config is the single source of truth, consumed by every IDE
  *and* the build; the build fails on any deviation, and a companion task auto-fixes.

  > **ADAPT:** the formatter engine, the config file path, and how IDEs and the build consume it.
- **Linting:** lint rules stay disjoint from the formatter (no rule lives in two tools, which avoids
  parallel upkeep).

  > **ADAPT:** the linter, its config path, and which gaps it covers that the formatter cannot.
- **Coverage:** decide report-only vs. threshold-gated; a hard threshold on a young suite gates the
  wrong thing.

  > **ADAPT:** the coverage tool, report location, and whether a threshold is enforced.
- **Dependency vulnerability scan:** an on-demand task, report-only locally; gating on severity is a
  CI/delivery concern. Continuous alerting (e.g. Dependabot) complements the on-demand scan.

  > **ADAPT:** the scanner, how to run it, and where its severity gate is configured.

## Git hooks (local enforcement)

The repo ships POSIX-sh git hooks in [`hooks/`](../../hooks/README.md), activated via
`core.hooksPath` (hooks are **not** shared by clone). Ensure they are active in every checkout:

```sh
git config core.hooksPath hooks
```

The devcontainer variant wires that command into `postCreate` so fresh containers get it
automatically; on the native variant, and in any existing checkout, run it once by hand
([dev-environment.md](dev-environment.md)).

- **`pre-commit`**: the fast quality gate: a scan of staged content for secrets and personal/host
  data, plus your stack's format/style checks once wired (its ADAPT spot; ships as a commented
  example). It deliberately **excludes the test run and the full build** so the TDD micro-commit
  loop stays fast (commits are already green when made).

  > **ADAPT:** wire your stack's fast format/style commands into `hooks/pre-commit`; they must run
  > in seconds.
- **`commit-msg`**: enforces the handbook §7 message rules: one single line, present-tense verb
  first (optional leading `[doc] ` tag), no body, no footers.

`--no-verify` bypasses both; reserve it for genuine emergencies (the one sanctioned non-emergency
use is the `/pause` `[wip]` parking commit, handbook §7). The hooks are a git-level safety net; the
full `{{BUILD_CMD}}` with tests remains the gate for landing on `main`.

## Run

> **ADAPT:** how to run the app locally and via container: the run command or image build, exposed
> ports, mounted volumes, and where state persists. Remember the pinned-environment rule
> ([dev-environment.md](dev-environment.md)): nothing runs through an ad-hoc host toolchain.

## Runtime configuration & secrets

Secrets (credentials, tokens, keys) are injected via the environment or a secret store at runtime,
**never** committed and never baked into an image (handbook §3). Machine-specific values live in
gitignored `*.local.*` files.

> **ADAPT:** a table of runtime settings: name, default, how it is bound (env var, config file),
> and what it controls. Mark anything still hard-coded as a tracked backlog item.

## Endpoints / entry points

> **ADAPT:** a table of the HTTP endpoints (or CLI commands) the app exposes: method + path,
> purpose, and auth requirements. Include the health/monitoring endpoint if any.

## Scheduled jobs

> **ADAPT:** each scheduled/background job: trigger, interval, what it runs, and how tests bypass
> the schedule. Delete this section if none exist.

## CI / dev environment

> **ADAPT:** the actual CI status (pipelines, or explicitly "none yet" with the rationale). The
> principle stands regardless: CI gates, the local build stays green and ungated.

Dev environment: the project's chosen variant, devcontainer or native pinned toolchain; see
[dev-environment.md](dev-environment.md).
