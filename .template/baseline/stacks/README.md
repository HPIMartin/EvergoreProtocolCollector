# Stack presets

The template core is stack-neutral; a preset supplies the stack-specific values in one place. Pick
the preset for your stack at adoption (the `/adopt` skill applies it); a multi-stack project (e.g. a
Java service with a TypeScript frontend) combines presets under one root `verify`.

## The `verify` contract

Every adopted project has **one committed, executable POSIX-sh script `verify` at the repo root**.
Developers, agents, git hooks and CI call it identically, so an inner-loop run, a commit gate and a
pipeline exercise the same commands.

| Subcommand | Runs | Expected duration |
|---|---|---|
| `./verify format` | format/style check only, no tests (the hooks call exactly this) | seconds |
| `./verify focus <path>` | focused test run for the stack of that file; a `.feature` path runs those scenarios, `@wip` included | seconds |
| `./verify bdd` | the `@wip` scenarios, i.e. the feature under development | seconds |
| `./verify all` | the gateway build: format, lint, typecheck, unit tests, acceptance scenarios excluding `@wip` and `@characterization`; caches disabled, test counts printed | minutes |
| `./verify outdated` | dependency freshness report | on demand |
| `./verify vuln` | vulnerability scan, report-only | on demand |

- Exit code 0 (pass) or 1 (fail); a usage error exits 2. A tool's own exit code never reaches the
  caller, so a runner's "no tests collected" or a linter's code 3 cannot be read as either verdict.
- `outdated` and `vuln` are **report-only**: they exit 0 whenever the tool itself ran and reported
  (exit 1 to 125), and **1 when it is missing, could not be executed, or died by a signal**. Gating on findings is a CI/delivery concern
  (handbook §7), never a local build failure.
- `bdd` with no tagged scenario is a clean 0 with a printed note: the feature under development not
  existing yet is the ordinary state of that subcommand.
- `focus` resolves a relative path against the **caller's** directory, so it behaves the same from a
  subdirectory as from the root.
- `all` prints proof that tests **executed** (counts, bare task lines): an exit code cannot tell a
  real run from a restored cache entry ([testing.md](../docs/knowledge-base/testing.md)). It also
  writes machine-readable reports under `artifacts/test-results/` (or the build tool's own results
  directory), so CI publishes them without a second command.
- `./verify all` is the full build and `./verify focus <path>` the inner-loop run; the template
  carries no `{{BUILD_CMD}}`/`{{TEST_CMD}}` tokens.
- The script is committed with mode `100755`; on a host whose filesystem has no exec bit, set it in
  the index with `git update-index --chmod=+x verify`.

## What a preset supplies

- **The `verify` script**: a ready `stacks/<preset>/verify`, copied to the repo root and worked
  through along its `# ADAPT` lines.
- **Placeholder value** for `{{TECH_STACK}}` (placeholder key:
  [KB map](../docs/knowledge-base/README.md)).
- **A permissions allow block** to merge into `permissions.allow` of `.claude/settings.json`; the
  deny floor is stack-neutral and stays untouched. `Bash(./verify:*)` is the stack-neutral entry
  point and lives in the committed policy already; a block adds only the tools the inner loop
  invokes directly. A `Bash(...)` rule matches as a clean command prefix, so a compound command
  needs its own rule.
- **Lint, format and warnings-as-errors wiring**: which tools gate the build and how a warning fails
  it (handbook §3).
- **BDD runner wiring**: the runner, where feature files live, how step definitions are found, and
  how the `@wip` tag filter is configured.
- **Probe location**: the gitignored throwaway-code directory plus its run task and committed clear
  task ([build-run-deploy.md](../docs/knowledge-base/build-run-deploy.md)).
- **Toolchain pinning spots** for the single-sourcing rule
  ([dev-environment.md](../docs/knowledge-base/dev-environment.md)): the build-file declaration plus
  the devcontainer feature (variant A) or the version-manifest entry (variant B).
- **Modernisation commands**: what `verify outdated` and `verify vuln` run, and the dependency-bot
  ecosystem keys for the snippets below.

## Git gate settings

`hooks/content-gate` ships stack-complete: it scans every supported language family for added
comments, debug prints, `TODO|FIXME` and hard-coded tokens, so a preset lists neither patterns nor
extensions. What a project of any stack adjusts:

- `production_trees` / `test_trees` when its layout differs from the shipped defaults.
- `print_allow_paths` for CLI adapters whose stdout is the interface (empty by default).
- `hooks/commit-msg`: the optional ticket-key settings (`ticket_key_re`, `ticket_position`,
  `ticket_required`), filled from the team's ticket pattern at adoption.
- `hooks/format-gate` needs no adaptation: it calls `./verify format`.
- `sh hooks/self-test` must pass after every adaptation; it is the acceptance test of the gate.

## One acceptance runner per system

Scenarios are **executable Gherkin** (`.feature` files) and are the primary review artifact
(handbook §5). A deliverable system has **exactly one** BDD runner, chosen at adoption: in a Java
backend with a TypeScript frontend, `cucumber-jvm` covers the system and the frontend keeps unit
tests only. Feature files live where the chosen runner's convention expects them.

Tag convention: `@wip` marks an approved scenario awaiting implementation, `@characterization` a
scenario derived from legacy code and awaiting the author's confirmation (handbook §5). `verify all`
excludes both, `verify bdd` runs only `@wip`, `verify focus <feature>` runs that file whatever its
tags.

| Stack | Runner | Tag filter |
|---|---|---|
| Java/Gradle | `cucumber-jvm` on `cucumber-junit-platform-engine` | `cucumber.filter.tags` in `junit-platform.properties` |
| JS-TS/npm | `@cucumber/cucumber` (`vitest-cucumber` where Vitest is the runner) | `cucumber.js` profiles |
| Python | `pytest-bdd` | tags become pytest markers, `-m "not wip"` |
| C# | `Reqnroll` | tags become `TestCategory`, `dotnet test --filter` |
| C++ | `pytest-bdd`, out of process | markers as for Python |
| Unity | `UnitySpec` (SpecFlow-derived, on the Unity Test Framework), niche | its own attribute filter |

**C++ has no in-process runner by default.** Its acceptance scenarios drive the **built artifact**
from outside (CLI, IPC or API) through `pytest-bdd`, so a pure C++ project gains a test-only Python
toolchain (uv, pytest, pytest-bdd). `cucumber-cpp` is the in-process alternative and has to be
verified at adoption before it is chosen.

## Multi-stack composition

One root `verify`, one stack per subdirectory, no second entry point. The composed skeleton ships as
a file, [multi-stack/verify](multi-stack/verify) (a Java backend with a TypeScript frontend); copy
it instead of assembling one from fragments, and replace each stack's commands with the ones its
preset ships. The dispatch it implements:

- `focus <path>` dispatches on the path prefix or the extension to that stack's runner, after
  resolving the path against the caller's directory. A path under no stack root is a usage error
  (exit 2), never the default stack's runner answering for a file it does not own.
- `all`, `outdated` and `vuln` run every stack in sequence and fail on the first failure.
- `format` runs every stack's format check; the hooks call it on every commit, so keep it fast.
- `bdd` runs the one acceptance runner of the system, not one per stack.
- Every other word, including no word at all, is a usage error (exit 2). A composed script that
  silently succeeds on a subcommand it forgot is worse than one that fails.

## Proving the scripts

`stacks/self-test` runs every shipped `verify` against stub tools that log their arguments, and
asserts the contract above: the exit codes, the pruned test count, the tag filters, the missing-tool
and findings cases, the path resolution from a subdirectory, and the composed skeleton's dispatch.

```sh
sh stacks/self-test
```

Run it after adapting a preset's script, and extend the matching case with the change: a preset
whose `verify` this suite does not cover is a preset nobody proved.

## Presets

All presets are full: [java-gradle/](java-gradle/README.md), [js-ts-npm/](js-ts-npm/README.md),
[python/](python/README.md), [cpp/](cpp/README.md), [csharp-dotnet/](csharp-dotnet/README.md).
[multi-stack/](multi-stack/verify) is not a stack but the composed root script for a project that
combines two of them.

The C#/.NET preset carries a **Unity adapter addendum** rather than a separate Unity stack: the
engine-free core is a normal .NET solution and only the view layer is editor-bound.

## Dependency bots

A bot complements the on-demand `verify outdated`: it opens the update as a reviewable change
instead of waiting for someone to run the report. Pick one per project.

`.github/dependabot.yml` (GitHub-native; on GitLab, run Renovate as a scheduled pipeline job
instead):

```yaml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
```

`renovate.json` at the repo root (platform-independent):

```json
{
  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
  "extends": ["config:recommended"],
  "schedule": ["before 6am on monday"],
  "packageRules": [
    { "matchUpdateTypes": ["minor", "patch"], "automerge": false }
  ]
}
```

| Preset | Dependabot `package-ecosystem` | Renovate manager |
|---|---|---|
| java-gradle | `gradle` | `gradle` |
| js-ts-npm | `npm` | `npm` |
| python | `uv` (`pip` without uv) | `pep621` |
| cpp | none for vcpkg/conan | `cmake` for `FetchContent` pins only |
| csharp-dotnet | `nuget` | `nuget` |

C++ dependency freshness stays manual: bump the vcpkg baseline or the conan requirements through
`verify outdated`.

## After adoption

The preset's values live in the project (`verify`, `.claude/settings.json`, hooks, build files, KB
docs); delete `stacks/` along with the rest of the template scaffolding. Keep `stacks/self-test`
only if you keep `stacks/`: it asserts the shipped scripts, not your adapted one.
