# Build & test performance plan

> Execution plan for cutting build/test/infrastructure wall-clock time. Written 2026-07-19 from a
> measured analysis; designed to be executed step by step by an implementing agent (any tier).
> Every step is independent, has its own verification, and lands as its own commit (handbook §7).
> Delete this file when all steps have landed or been rejected (DOC-5 spirit: no dead plans).

## Baseline (measured 2026-07-19)

| Scenario | Today | Evidence |
|----------|-------|----------|
| Full `./gradlew build`, warm daemon (data-integrity worktree) | ~17 min | session stats 2026-07-19 |
| Test task alone (same worktree) | ~9 min wall for ~72 s of actual test time | JUnit XML: 26 classes, sum of `time=` attrs ≈ 72 s; timestamps span 10:19:17→10:28:01 |
| Build with no changes (up-to-date) | ~1–2 min | session stats; the smokeTest.sqlite rewrite keeps busting it |
| Pre-commit hook (spotless + checkstyle) | ~1–3 min per commit | hook pulls `:frontend:npmBuild` via checkstyle's classpath |
| New worktree cold start | full recompile + node re-download + `npm ci` | per-worktree `frontend/.gradle/nodejs` and `frontend/node_modules` observed; no build cache configured |
| Devcontainer rebuild | re-downloads Gradle dist, JDK toolchain, all deps | `~/.gradle` (1.0 GiB) is not volume-mounted |

Machine: 12 cores, 15 GiB RAM, mostly idle during builds. Headroom is not the bottleneck;
configuration is.

## Root causes, ranked by impact

1. **Per-class JVM forking**: the data-integrity branch's meta-sums recompute commit added
   `forkEvery = 1` to `tasks.test` (main does not have it). 26 test classes × (JVM boot + Micronaut
   classpath + JaCoCo agent) ≈ 7–8 min of pure overhead for 72 s of tests. This is the single
   biggest lever and it is a *new regression on the branch*, not a long-standing cost.
2. **No Gradle performance features enabled**: `gradle.properties` sets neither
   `org.gradle.caching` nor `org.gradle.parallel` nor `org.gradle.configuration-cache`. Without the
   local build cache every worktree recompiles and re-tests everything from scratch even at an
   identical commit; without parallel the frontend chain (`npmBuild`/`npmTest`/`npmLint`) and the
   backend run strictly sequentially.
3. **`smokeTest.sqlite` cache buster** (already in the backlog's test-hygiene bullet): `SmokeTest`
   deletes and rewrites `src/test/resources/smokeTest.sqlite` on every run, so an *input* of
   `processTestResources` changes every run and the whole `processTestResources → test →
   jacocoTestReport` chain can never be up-to-date or cache-hit.
4. **Pre-commit hook compiles the world**: `checkstyleMain`'s default `classpath` includes the main
   source-set output, which depends on `processResources`, which packs the SPA and therefore runs
   `:frontend:npmBuild`. Checkstyle here runs only non-type-aware rules (`NeedBraces`); it does not
   need any of that.
5. **Per-worktree node/npm state**: node-gradle downloads its own Node distribution into
   `frontend/.gradle/nodejs` *per worktree* (even though the devcontainer already ships the same
   version) and `npm ci` rebuilds `node_modules` (~130 MB) per worktree.
6. **Nothing survives a devcontainer rebuild**: `~/.gradle` (wrapper dist, JDK toolchain, dependency
   cache, and after step S1 the build cache) and `~/.npm` live in the container layer
   (relates to the still-live backlog item H5).

## Rules for the implementing agent

- Work in-container only (dev-environment.md); follow the commit protocol (handbook §7): one step =
  one commit, one-line present-tense message, never push.
- **Measure before and after every step** with the protocol below; put the numbers in the step's
  section of this file while work is in flight (they do not go into the KB; git carries them once
  the plan file is deleted).
- Build-infra steps are not TDD-able; the verification listed per step is the acceptance gate.
- A step whose verification fails and cannot be fixed quickly is *reverted and marked rejected here
  with the reason*, not left half-on.
- KB updates in the same commit (DOC-6): build behavior changes touch
  [build-run-deploy.md](knowledge-base/build-run-deploy.md); test-execution changes touch
  [testing.md](knowledge-base/testing.md).
- Decision points marked **[author]** need the author's confirmation before implementing; record
  the decision in [open-questions.md](../open-questions.md) (DOC-7).

### Measurement protocol

Run in the repo root (or the target worktree), warm daemon (run once, discard, then measure):

```sh
time ./gradlew build --console=plain            # full build
time ./gradlew build --console=plain            # immediately again = up-to-date build
time ./gradlew test --console=plain             # test task focus
./gradlew build --profile                       # per-task breakdown: build/reports/profile/
```

Cold-worktree scenario (after S1, to prove cross-worktree cache reuse):

```sh
git worktree add /tmp/egc-bench HEAD
( cd /tmp/egc-bench && time ./gradlew build --console=plain )
git worktree remove --force /tmp/egc-bench
```

## Steps, in execution order

### S1: Enable the Gradle build cache and parallel execution

- **Change**: add to `gradle.properties`:

  ```properties
  org.gradle.caching=true
  org.gradle.parallel=true
  org.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=768m
  ```

- The local build cache lives in `~/.gradle/caches/build-cache-1` and is shared by *all worktrees*
  automatically; a fresh worktree at an already-built commit gets compile, checkstyle, npmBuild,
  npmTest and npmLint results as cache hits instead of cold runs. The frontend tasks already
  declare `outputs.cacheIf { true }` and full inputs, so they cache immediately.
- **Risk**: the vitest "failed to start forks worker" flake (backlog, test hygiene) appeared under
  CPU load; parallel execution raises load. If `npmTest` flakes repeatedly, pin the vitest pool
  (`poolOptions.forks.maxForks`) in `frontend/vite.config.ts` as part of this step.
- **Verify**: up-to-date build is seconds, not minutes; the cold-worktree scenario shows `FROM-CACHE`
  for `compileJava`/`npmBuild` (`--info` or the profile report); `./gradlew build` still green.
- **KB**: document both flags and the shared-cache-across-worktrees property in build-run-deploy.md.
- **Measured** (2026-07-31, main, together with S3 since S1 only pays off for `test` once S3 lands):
  full build green in **18m 37s**; the immediately following no-change build in **4s**, all 23 tasks
  up-to-date (baseline ~1-2 min, and `test` re-ran every time). The cold-worktree `FROM-CACHE` check
  is still open: it needs these flags committed first, since a worktree reads `gradle.properties`
  from its own checkout.

### S2: Trial the configuration cache (separate commit, may be rejected)

- **Change**: add `org.gradle.configuration-cache=true` to `gradle.properties`.
- Saves the per-invocation configuration phase (matters most for the many short hook/agent
  invocations: `spotlessCheck`, focused `--tests` runs).
- **Risk**: plugin compatibility (Micronaut application plugin, node-gradle, cyclonedx, the two
  `Exec` vulnScan tasks). Trial it: run `./gradlew build`, `./gradlew vulnScan`, the pre-commit
  hook, and a focused `--tests` run. Any hard incompatibility that cannot be fixed by small task
  adjustments → reject this step, remove the flag, record the blocking plugin here.
- **Verify**: second invocation prints "Reusing configuration cache."; all four command shapes pass.
- **KB**: build-run-deploy.md notes the flag (or this section records the rejection).

### S3: Move `smokeTest.sqlite` out of `src/test/resources`

- **Change**: `SmokeTest` (constant `TEST_DATABASE_PATH`, plus wherever the path is wired; check
  `grep -rn "smokeTest.sqlite" src/`) writes its throwaway DB under the build directory (e.g.
  `build/tmp/smokeTest.sqlite`) or a JUnit `@TempDir`; delete the committed
  `src/test/resources/smokeTest.sqlite` from git.
- Restores up-to-date/cache-hit behavior for `processTestResources → test → jacocoTestReport`
  (root cause 3) and removes a cross-run flakiness risk. This also makes the `test` task actually
  *cacheable*, which S1 needs to pay off for test runs.
- Backlog: this implements the test-hygiene bullet's second half; update that bullet.
- **Verify**: two consecutive `./gradlew build` runs, the second reports `test` up-to-date (or
  `FROM-CACHE`); `git status` stays clean after a test run; full build green.
- **KB**: testing.md (SmokeTest description).
- **Correction**: the file was never committed, only gitignored, so nothing had to be removed from
  git. The cache-busting came purely from it being rewritten inside the `processTestResources` input
  directory, which is what moving it under `build/` fixes.
- **Measured** (2026-07-31): `test` reports up-to-date on the second consecutive build (see S1), and
  `git status` is clean after a test run. `build/tmp/smokeTest.sqlite` is created as expected.

### S4: Fix the test JVM strategy (undo the blanket `forkEvery = 1`)

- **Precondition**: coordinate with the in-flight data-integrity strand. Cheapest is to raise it
  in that strand's fold round (the line is not yet on main); otherwise fix immediately after it
  lands. Do not create a competing edit to `build.gradle.kts` while the strand is open.
- **[author] Decision**, options in recommended order:
  1. **Root-cause fix (recommended)**: find why the recompute tests needed per-class isolation
     (suspects: the public mutable `Configuration.useInMemory`/`DATABASE_TEMP_SQLITE` statics, a
     shared SQLite file, static state in `BootSignalRecorder`), fix the shared state, run the whole
     suite in one JVM. Expected: test task ~1.5–2 min.
  2. **Keep per-class forking but parallelize**: `forkEvery = 1` plus
     `maxParallelForks = Runtime.getRuntime().availableProcessors() / 2`. Expected: ~2–3 min; no
     shared-state work needed, but 12 concurrent Micronaut boots may contend.
  3. **Split suites**: fast unit suite in one JVM, a separate `bootTest` task with forking for the
     `@MicronautTest` classes only.
- Whatever lands must also resolve the testing.md fork-isolation discrepancy flagged by the
  conformance audit (still-live backlog item G16): the doc and the build must agree.
- **Verify**: `./gradlew test` wall time at or under the expected number; suite green in 5
  consecutive runs (isolation regressions show up as flakes); no test order dependence
  (`test.systemProperty("junit.jupiter.testclass.order.default", …)` experiments are out of scope).
- **KB**: testing.md execution-model section.

### S5: Decouple the pre-commit hook from the frontend build

- **Change**: in `build.gradle.kts`, drop the compile/resources dependency from Checkstyle:

  ```kotlin
  tasks.withType<Checkstyle>().configureEach {
      classpath = files()
  }
  ```

  The active rules (`NeedBraces`; later `AvoidStarImport`) are not type-aware and need no
  classpath. This removes `checkstyleMain → classes → processResources → :frontend:npmBuild`
  from every commit.
- **Verify**: `./gradlew checkstyleMain checkstyleTest` runs without triggering `:frontend:*` or
  `compileJava` (check with `--dry-run`); a deliberately brace-less local change still FAILS
  checkstyle (then revert it); hook wall time ~15–45 s.
- **KB**: build-run-deploy.md (checkstyle section), and the hook comment if it names timings.

### S6: Share the Node distribution across worktrees

- **Change**: in `frontend/build.gradle.kts`, point node-gradle's download directory at a
  per-user shared location instead of the per-project default:

  ```kotlin
  node {
      // ...existing config...
      workDir = file(System.getProperty("user.home") + "/.gradle/nodejs")
  }
  ```

  Keep `download = true` so `nodeVersion` in `gradle.properties` stays the single source (the
  devcontainer's own Node floats on "lts" and must not become the build's Node).
- `npm ci` per worktree stays (isolation is wanted there), but it already shares `~/.npm`; no
  further change.
- **Verify**: delete `frontend/.gradle/nodejs`, run `./gradlew :frontend:npmInstall`, confirm the
  distribution lands under `~/.gradle/nodejs` and a second worktree reuses it (no new download).
- **KB**: frontend.md build wiring section.

### S7: Worktree warm-up step in the agent pipeline

- **[author] Decision**, options in recommended order:
  1. **Explicit warm-up in the playbook (recommended)**: after creating a strand worktree the
     orchestrator runs `./gradlew --quiet classes testClasses :frontend:npmInstall` there once
     *before* handing it to the implementer. With S1's shared cache this is mostly cache-replay
     (minutes → tens of seconds) and the implementer never pays the cold start. Controllable, no
     surprise background builds (background-build collisions already bit the implementer twice).
  2. **Automatic `post-checkout` git hook**: fires on `git worktree add` since `core.hooksPath` is
     set; fully deterministic but spawns background Gradle runs that can collide with foreground
     agent builds. Not recommended for exactly that reason.
- **Change** (option 1): one bullet in multi-agent-playbook.md's worktree/build section; optionally
  a tiny `hooks/warmup` convenience script (sh, no args) the playbook references.
- **Verify**: create a scratch worktree, run the warm-up, then `time ./gradlew build` inside it and
  compare against the cold-worktree baseline.

### S8: Persist caches across devcontainer rebuilds (backlog item H5)

- **Change**: in `.devcontainer/devcontainer.json` add named volumes and fix ownership in
  `postCreateCommand` (volumes are root-owned on first creation):

  ```jsonc
  "mounts": [
      "source=${localWorkspaceFolder}/.devcontainer/.claude-home,target=/home/vscode/.claude,type=bind",
      "source=egc-gradle-home,target=/home/vscode/.gradle,type=volume",
      "source=egc-npm-cache,target=/home/vscode/.npm,type=volume"
  ],
  "postCreateCommand": "sudo chown -R vscode:vscode /home/vscode/.gradle /home/vscode/.npm && git config core.hooksPath hooks && ./gradlew --no-daemon build -x test || true"
  ```

  (The `&&` chaining is the still-live backlog item H12's postCreate fix; land it here.)
- **Caveat**: devcontainer changes cannot be validated inside the container (no docker-in-docker);
  verify on the next container rebuild on the host, per the backlog gotcha.
- **Verify** (next rebuild): rebuild the container twice; the second rebuild's postCreate build
  needs no dependency downloads and finishes in a fraction of the first.
- **KB**: dev-environment.md; close H5 (and H12's postCreate bullet) in the backlog per DOC-5.

### S9 (optional, low priority): JaCoCo report on demand

- `tasks.test { finalizedBy(tasks.jacocoTestReport) }` regenerates the coverage report after every
  test invocation, including focused `--tests` micro-loops. Move the report out of the hot path
  (e.g. only `check` depends on it) if profiling (S1's `--profile`) shows it costs more than ~10 s
  per run; otherwise reject this step here.

## Expected end state

| Scenario | Today | Target |
|----------|-------|--------|
| Full build, warm daemon | ~17 min (branch) | ≤ 5 min |
| Test task | ~9 min | ≤ 2 min |
| Up-to-date / no-change build | ~1–2 min | ≤ 20 s |
| Pre-commit hook | ~1–3 min | ≤ 45 s |
| Fresh worktree to first green focused test | many minutes (cold compile + node download + npm ci) | ≤ 2 min (cache replay), ~0 for the agent with S7 |
| Devcontainer rebuild | full re-download | dependency-free postCreate |

Falsifier-panel consequence: with S1+S7, parallel falsifiers in separate throwaway worktrees become
cheap (their builds are cache replays), so the panel no longer has to run sequentially in one
worktree for ~50 min; revisit the playbook's sequential-panel rule after S7 lands.
