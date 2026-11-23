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
| `./verify stop` | stops what this worktree's builds leave running (java-gradle: the Gradle daemons of its own registry, never another worktree's), exit 0 where nothing runs; a caller that builds in a throwaway tree ends with it | seconds |

- Exit code 0 (pass) or 1 (fail); a usage error exits 2. A tool's own exit code never reaches the
  caller, so a runner's "no tests collected" or a linter's code 3 cannot be read as either verdict.
  - A building run ended by a hangup, an interrupt or a termination signal dies by that signal and
    gives no exit code.
    - Where that signal cannot kill the run, as for a container's first process, it exits 1.
- `outdated` and `vuln` are **report-only**: they exit 0 whenever the tool itself ran and reported
  (exit 1 to 125), and **1 when it is missing, could not be executed, or the tool itself died by a
  signal**. Gating on findings is a CI/delivery concern (handbook §7), never a local build failure.
  - The script checks the wrapper (`gradlew`, `uv`, `npm`), not the scanner behind it: a missing
    plugin, a missing pip-audit or a missing lockfile reads as reported findings, so read the
    printed line.
- A Gradle stack's script starts every command's daemon in the registry `registry.local.d/` of its
  own tree, so `stop` ends only that tree's daemons.
  - Why `stop`: a Gradle daemon outlives its build and holds the tree's working directory, so a
    caller that is done with a tree stops it.
  - Why the registry: Gradle's `--stop` ends every daemon in its registry, and the default one is
    shared by every build of that Gradle version on the machine, other worktrees' builds included.
  - The directory is ignored by the template `.gitignore`'s `*.local.*`.
  - An outer `GRADLE_OPTS` is kept, and the registry option of the script comes last, so it wins.
  - A root that holds a double quote is refused with exit 2 before any command, since Gradle drops
    the option from such a path.
  - A named limit: a root holding a newline, or an outer `GRADLE_OPTS` with an unclosed quote,
    drops the registry option silently.
- `bdd` with no tagged scenario is a clean 0 with a printed note: the feature under development not
  existing yet is the ordinary state of that subcommand.
- `focus` resolves a relative path against the **caller's** directory, so it behaves the same from a
  subdirectory as from the root.
- `all` prints proof that tests **executed** (counts, bare task lines): an exit code cannot tell a
  real run from a restored cache entry ([testing.md](../docs/knowledge-base/testing.md)). It also
  writes machine-readable reports under `artifacts/test-results/` (or the build tool's own results
  directory), so CI publishes them without a second command.
- In the python and js-ts-npm presets, and for the composed script's frontend, `all` first installs
  exactly what the lock file names (`uv sync --locked`, `npm ci`), so a dependency a commit dropped
  is gone before any check runs.
- `./verify all` is the full build and `./verify focus <path>` the inner-loop run; the template
  carries no `{{BUILD_CMD}}`/`{{TEST_CMD}}` tokens.
- The script is committed with mode `100755`; on a host whose filesystem has no exec bit, set it in
  the index with `git update-index --chmod=+x verify`.
- A command takes the machine-wide build lock by calling the helper `hold_build_lock` once its
  arguments and tools have checked out (the commands that take it, and what the lock does:
  [build-run-deploy.md](../docs/knowledge-base/build-run-deploy.md), "The verify script (the one
  entry point)").
  - The helper, with its `build_lock` ADAPT line, is the same text in every preset's `verify`.
  - A branch that sets an exit trap of its own opens it with `release_build_lock`.
  - Such a branch sets the `HUP`, `INT` and `TERM` traps too: each does the exit trap's cleanup,
    then ends with `end_by_signal <signal>`, which releases the lock, re-raises the signal and
    exits 1 where the signal did not end the run.
  - Its three messages start `[verify] waiting for <file>:`, `[verify] cannot read <file>,` (exit
    1) and `[verify] this run takes no build lock:`.

## What a preset supplies

- **The `verify` script**: a ready `stacks/<preset>/verify`, copied to the repo root and worked
  through along its ADAPT notes.
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
- **Toolchain pinning spots**, every place the version stands
  ([dev-environment.md](../docs/knowledge-base/dev-environment.md)): the build-file declaration plus
  the devcontainer feature (variant A) or the version-manifest entry (variant B).
- **Modernisation commands**: what `verify outdated` and `verify vuln` run, and the dependency-bot
  ecosystem keys for the snippets below.
<!-- MODULE gate -->
- **Gate values** (`gate.env`): the values `/adopt` copies into the ADAPT block of
  `gate/dossier-check` when the project takes the gate module.
  - The java-gradle, js-ts-npm and python presets turn on the reader of their runner; csharp-dotnet
    and cpp are reduced to the red leg, since no reader knows their runners' failing lines.
  - Which leg of `gate/dossier-check` each preset's values switch on (multi-agent-playbook.md,
    "The gate dossier"); every other leg is the same in each preset:

    | Preset | New-test leg: tests it reads | Walk leg | `@wip` leg | Docs-guard leg |
    |---|---|---|---|---|
    | java-gradle | JUnit under Gradle, `sh` self-test cases | on | on | off; on with the KB guard |
    | js-ts-npm | vitest, `sh` self-test cases | on | off | off |
    | python | pytest, `sh` self-test cases | on | off | off |
    | cpp | `sh` self-test cases alone | off | off | off |
    | csharp-dotnet | `sh` self-test cases alone | off | off | off |

    - So no C++ or C# test is ever asked for a red run: the new-test leg of those two presets
      counts only `sh` self-test cases.
    - The `@wip` leg reads Cucumber-JVM's lines under Gradle alone, so it is off in the other four
      presets.
    - A leg that is off is named on the PASS line.
  - `walk_failed_reports` holds the globs, separated by blanks, of the test reports a failed walk
    build leaves, one per stack of a composed project: java-gradle's results directory and the
    `artifacts/test-results/` of js-ts-npm and python.
  - It is empty for csharp-dotnet and cpp, whose verify also writes `artifacts/test-results/*.xml`:
    their walk leg is off, so no walk runs to keep them.
  - `gate/self-test` proves each `gate.env` (its `z_<preset>` cases) while `stacks/` stands beside
    `gate/`, against sample lines in the shape the runner documents, never against the output of a
    live run.
<!-- /MODULE gate -->
<!-- MODULE acceptance -->
- **Web-UI acceptance files** (java-gradle only): the browser start and sweep as Java files
  under `acceptance/`, copied by `/adopt` with the package resolved, and the grid recipe under
  `grid/`, copied on the author's request; the section "Web-UI acceptance (browser half)" of the
  preset README lists them and their ADAPT values. The other presets drive their browser with the
  project's own library.
<!-- /MODULE acceptance -->
- **KB guard** (java-gradle only, optional): the guard of DOC-12 as Java test files under
  `kb-guard/`, copied by `/adopt` with the package resolved; the section "KB guard (optional)" of
  the preset README lists them, the `test` task's inputs and their ADAPT values. The other presets
  leave DOC-12 to the `doc-reviewer`'s eye.

## Git gate settings

`hooks/content-gate` ships stack-complete: it scans every supported language family for added
comments, debug prints, `TODO|FIXME` and hard-coded tokens, so a preset lists neither patterns nor
extensions. What a project of any stack adjusts:

- `production_trees` / `test_trees` when its layout differs from the shipped defaults.
- `print_allow_paths` for CLI adapters whose stdout is the interface (empty by default).
- `comment_exempt_dirs` for each project tooling directory whose script has a self-test of its own
  and keeps its comments (`stacks/` and `agent/` by default).
  <!-- MODULE gate -->
  - With the gate module, `gate/` is listed too.
  <!-- /MODULE gate -->
  <!-- MODULE agent-seat -->
  - With the agent-seat module, `land/` is listed too.
  <!-- /MODULE agent-seat -->
- `hooks/commit-msg`: the optional ticket-key settings (`ticket_key_re`, `ticket_position`,
  `ticket_required`), filled from the team's ticket pattern at adoption.
- `hooks/format-gate` needs no adaptation: it calls `./verify format`.
- `sh hooks/self-test` must pass after every adaptation; it is the acceptance test of the gate.

## One acceptance runner per system

Scenarios are **executable Gherkin** (`.feature` files) and are the primary review artifact
(handbook §5). A deliverable system has **exactly one** BDD runner, chosen at adoption: in a Java
backend with a TypeScript frontend, `cucumber-jvm` covers the system and the frontend keeps unit
tests only. Feature files live where the chosen runner's convention expects them.

Tag convention: `@wip` marks an approved scenario awaiting implementation, on the level handbook §5
names (step 3); `@characterization` marks a scenario derived from legacy code and awaiting the
author's confirmation (handbook §5). `verify all` excludes both, `verify bdd` runs only `@wip`,
`verify focus <feature>` runs that file whatever its tags.

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
  - `focus` takes a path as `git ls-files` prints it, with no `./` and no `..`.
- `all`, `outdated` and `vuln` run every stack in sequence and fail on the first failure.
- `stop` stops what each stack leaves running; a stack with nothing to stop adds nothing.
- `format` runs every stack's format check; the hooks call it on every commit, so keep it fast.
- `bdd` runs the one acceptance runner of the system, not one per stack.
- Every other word, including no word at all, is a usage error (exit 2). A composed script that
  silently succeeds on a subcommand it forgot is worse than one that fails.
- `all` and `vuln` check every stack's tool first, then take the build lock once for all stacks;
  `focus` and `bdd` take it for the one stack they run.

## Proving the scripts

`stacks/self-test` runs every shipped `verify` against stub tools that log their arguments, and
asserts the contract above: the pruned test count, the missing-tool and findings cases, the path
resolution from a subdirectory, and the composed skeleton's dispatch. The suite pins the exit codes,
the tag filters and the cpp format flag; which tools `format` and `all` run is the preset's table,
not a case.
Each case runs behind its stubs on a path that hides every tool the presets' `verify` scripts
`require` except `git` and their own `./gradlew`, and `docker`, so an installed copy of those
changes no verdict.

```sh
sh stacks/self-test
```

Run it after adapting a preset's script, and extend the matching case with the change: a preset
whose `verify` this suite does not cover is a preset nobody proved.

- Every copy it runs locks a file of its own in the suite's temporary directory, never the shipped
  `/tmp` path.
- Its build-lock cases need `flock`, `timeout` and `/proc/<pid>/fdinfo`; where one is missing (Git
  Bash, macOS) each prints a `skip` line, and only the no-lock note, the helpers' sameness and the
  shape of the traps are proven there.
  - A few cases also need `env --default-signal`, `bash` and `setsid`, or a user that a file of
    mode 000 keeps out, and skip without them.
  - The summary counts the skipped cases and names each one, so a run that skipped any never reads
    as all cases proven; skips alone still exit 0.
  - `STACKS_SELF_TEST_STRICT=1`, the value `1` alone, turns each skip into a failure; set it on a
    machine that has every tool above and should prove every case.
- A case that sends a signal to a run counts a failed `kill` as its own failure, so a run that
  ended too early is a named FAIL and the suite goes on.
- Its build-lock cases wait on the wall clock, by author decision (handbook §6): a poll of up to
  ten seconds decides whether a run showed its wait or freed the lock, and a run that `timeout`
  stops after 10 to 60 seconds fails its case.
  - Why: a run blocked on another's lock shows that only by what it prints while it waits, and a
    lock another process holds or frees sends no signal the suite could wait on.
- Run as root, the unreadable-file case drops the capabilities that bypass file permissions through
  `setpriv`.

## Presets

All presets are full: [java-gradle/](java-gradle/README.md), [js-ts-npm/](js-ts-npm/README.md),
[python/](python/README.md), [cpp/](cpp/README.md), [csharp-dotnet/](csharp-dotnet/README.md).
[multi-stack/](multi-stack/verify) is not a stack but the composed root script for a project that
combines two of them.

The C#/.NET preset carries a **Unity adapter addendum** rather than a separate Unity stack: the
engine-free core is a normal .NET solution and only the view layer is editor-bound.

## Dependency bots

A bot complements the on-demand `verify outdated`: it opens the update as a reviewable change
instead of waiting for someone to run the report. Pick one per project. How its update lands:
[build-run-deploy.md](../docs/knowledge-base/build-run-deploy.md), "Dependency updates".

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
    groups:
      minor-and-patch:
        patterns: ["*"]
        update-types: ["minor", "patch"]
```

`renovate.json` at the repo root (platform-independent):

```json
{
  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
  "extends": ["config:recommended"],
  "schedule": ["before 6am on monday"],
  "packageRules": [
    { "matchUpdateTypes": ["minor", "patch"], "groupName": "minor and patch", "automerge": false }
  ]
}
```

- **Both snippets group minor and patch updates** (the policy:
  [build-run-deploy.md](../docs/knowledge-base/build-run-deploy.md), "Dependency updates").
  - The Dependabot block bundles them into one pull request per ecosystem entry.
  - The Renovate rule's `groupName` groups them together, as Renovate's `group:allNonMajor` preset
    does.
- **Type packages bound to a runtime follow the pinned runtime's major**, since types for a higher
  major allow APIs the runtime lacks (example: an `ignore` entry for `@types/node` with
  `update-types: ["version-update:semver-major"]` in the npm block, until the runtime's major
  rises).

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

<!-- MODULE gate -->
With the gate module, `gate.env` lives on in the ADAPT block of `gate/dossier-check` (→ What a
preset supplies).
<!-- /MODULE gate -->
