# 04: Build, Run, Deploy

> **ADAPT:** this doc is a skeleton: fill in your build commands, quality gates, runtime
> configuration, endpoints, and deployment path. Keep volatile facts (versions, dependency lists)
> single-sourced in the build file; this prose points at them, never copies them (handbook §3).

## Build

- One entry point, `./verify` (contract below): `./verify all` is the full build with every test,
  `./verify focus <path>` the focused run. The build must **always run green** locally; quality and
  vulnerability *gating* belongs to CI/delivery, never the local build.
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

## The verify script (the one entry point)

Every project commits one executable POSIX-sh script `verify` at the repo root. Developers, agents,
the git hooks and the CI pipeline call it identically, so "local behaves like remote" is a property
of the script rather than a discipline. The stack presets in the template's `stacks/` folder ship one
per stack; a multi-stack project composes them into one root script.

| Subcommand | Does | Who calls it |
|---|---|---|
| `./verify format` | the fast format/style check; seconds, no tests | `hooks/format-gate`, on every commit-path hook |
| `./verify focus <path>` | the focused test run for the stack of that file; a `.feature` path runs those scenarios, `@wip` included | the TDD inner loop; counter-tests of the falsifiers |
| `./verify bdd` | the `@wip` scenarios only, i.e. the feature under development (handbook §5) | the TDD loop while the scenarios are still tagged |
| `./verify all` | the gateway build: format, lint, typecheck, unit tests, acceptance scenarios without `@wip` or `@characterization` (handbook §5), caches disabled; prints proof that the tests executed (handbook §6) | before every gate, after every rebase, the CI verify job |
| `./verify outdated` | dependency freshness report | the modernisation track, on demand |
| `./verify vuln` | vulnerability scan, report-only locally | CI on a schedule; gating is a delivery decision |

- Exit code 0 or 1; a usage error exits 2. The output of `all` names the executed test count or the
  bare task lines, so a cached or skipped run cannot pass as a green one.
- `format` stays fast: the test run and the full build never move into it, or the TDD micro-commit
  loop pays for every commit.

> **ADAPT:** the stack preset supplies the script body; list here the commands each subcommand runs
> in this project and how `focus` maps a path to its runner.

## Git hooks (local enforcement)

The repo ships POSIX-sh git hooks in [`hooks/`](../../hooks/README.md), activated via
`core.hooksPath` (hooks are **not** shared by clone). Ensure they are active in every checkout:

```sh
git config core.hooksPath hooks
```

The devcontainer variant wires that command into `postCreate` so fresh containers get it
automatically; on the native variant, and in any existing checkout, run it once by hand
([dev-environment.md](dev-environment.md)).

The suite is described in [`hooks/README.md`](../../hooks/README.md); the short version:
`content-gate` and `format-gate` hold the checks, the hooks that run before a commit exists
(`pre-commit`, `pre-applypatch`, `pre-merge-commit`) refuse outright, and the ones that can only run
after git moved the refs (`post-rewrite`, `post-commit`) **record** the offending sha so `pre-commit`
refuses to build on it. `commit-msg` enforces the handbook §7 message rules, and `self-test` proves
the whole thing blocks rather than narrates.

> **ADAPT:** `hooks/format-gate` calls `./verify format` and needs no adaptation; `hooks/content-gate`
> (host-path allowlist, production-source tree, smell patterns) and `hooks/self-test`'s matching
> fixtures do. `grep -rn "ADAPT:" hooks/`.

`--no-verify` bypasses them; reserve it for genuine emergencies (the one sanctioned non-emergency
use is the `/pause` `[wip]` parking commit, handbook §7). The hooks are a git-level safety net; the
full `./verify all` with tests remains the gate for landing on `main`.

## Probe location (throwaway agent code)

Falsifier agents and the implementer need somewhere to put code that is **not** meant to stay:
probing a library's real behavior before writing a helper, reproducing a suspected race by hand.

- It lives **outside the test tree, outside `check`/`build`, and outside git**: a leftover probe then
  cannot turn the build red, and `git add -A` cannot commit it.
- It has **two committed tasks**: one to run it, one to clear it. The clean-up task is what lets an
  agent get rid of its own probes without ever running `rm` (handbook §7): the scope lives in
  reviewed, committed build code instead of in a permission rule.
- Nothing under it is reviewed or shipped.

> **ADAPT:** name the directory and the two tasks for your stack, and gitignore the directory
> (example, Gradle: a `src/probe/java` source set on the test classpath, `./gradlew probe` to run it,
> a `Delete` task `./gradlew clearProbes` to clear it). Delete this section only if your agents have
> no way to run code at all.

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

**Local behaves like remote** because both run the same script in the same image: the pipeline's
verify job executes `./verify all` inside the image built from `.devcontainer/`, so a green local
`./verify all` in the devcontainer predicts the pipeline. The template ships this as the GitLab CI
preset (`ci/gitlab/`, applied at adoption): an image job that rebuilds the devcontainer image into
the project registry when `.devcontainer/` changes, the verify job on every branch and merge
request with the test reports as artifacts (`verify all` writes them under `artifacts/test-results/`,
a Gradle project under `build/test-results/`), a scheduled `./verify vuln` that reports rather than
gates, and a manual deploy placeholder. Other CI systems get presets on demand. The principle
stands regardless: CI gates, the local build stays green and ungated.

> **ADAPT:** the actual CI status (the pipeline file, or explicitly "none yet" with the rationale),
> the registry the image job pushes to, and which findings of the scheduled scan gate delivery.

Dev environment: the project's chosen variant, devcontainer or native pinned toolchain; see
[dev-environment.md](dev-environment.md).
