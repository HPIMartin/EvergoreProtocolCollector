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
6. ~~**Nothing survives a devcontainer rebuild**~~: fixed by S8 (named volumes for `~/.gradle` and
   `~/.npm`); pending proof on the author's next container rebuild.

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
  up-to-date (baseline ~1-2 min, and `test` re-ran every time).
- **Cold-worktree check** (2026-08-01, after S4): a fresh worktree at HEAD builds green in **11s**,
  12 of 25 tasks `FROM-CACHE` — including `compileJava` and `:frontend:npmBuild`, plus `checkstyle*`,
  `spotless*`, `npmLint`, `npmTest`, `compileTestJava`, `test` and `jacocoTestReport`. Cross-worktree
  sharing is confirmed: both worktrees compute the **same** cache key for `compileJava`
  (`3ec47124…`) and `npmBuild` (`dd73cda4…`), so entries are portable, not path-bound.
- **Caveat worth knowing** (cost the first attempt at this check a false negative): Gradle stores a
  cache entry only when a task actually *executes*. A task that has been UP-TO-DATE in the main
  worktree ever since caching was enabled has never been stored, so the first cold worktree still
  runs it cold — and stores it for the next one. The first attempt therefore saw `compileJava` and
  `npmBuild` execute (18s build); after forcing one real execution of each, the next fresh worktree
  hit both from cache.
- `:frontend:nodeSetup` and `:frontend:npmInstall` still run per worktree — that is S6's scope, not
  a cache failure.

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

- **It also breaks focused test runs, not just the full build** (measured 2026-07-31):
  `./gradlew test --tests '*OneClass*'` on a single trivial unit test ran past **14 minutes** and was
  killed before finishing. Worker PIDs churned throughout, consistent with a fresh JVM plus JaCoCo
  instrumentation per forked class rather than per *matched* class. Confirming which of the two the
  filter actually forks is part of this step. A second measurement (2026-08-01) let one run finish:
  a single-class `--tests` run took **19m33s** end to end while the matched test itself took 8.1 s,
  and it left **198** `junit-platform-unique-ids-*` files in `build/test-results/test/testlist`
  against only 27 test classes in the suite — so the fork count is not simply one per candidate
  class either, and the mechanism is still open. Either way a focused run costs minutes, which makes the
  red-green-refactor loop unusable and raises S4's priority above the remaining steps: the TDD loop,
  not the full build, is the cost that actually hurts. (Note when measuring: the XML reports and
  `in-progress-results-generic.bin` are finalized when the `test` task ends, so an empty results
  directory mid-run is normal and is not evidence of a stall.)
- **[author] Decision 2026-08-01: option 1, and it turned out to need no fix at all** — delete the
  line, plus a build-level guard against a third re-add (author's call). Options 2 (parallel forks)
  and 3 (suite split) were rejected once the investigation showed there is no shared state to work
  around; at ~31 s of test time in one JVM they would have added JVM boots, not removed them.
- **The fork mechanism, which this step had left open**: Gradle hands every **non-anonymous class
  file** of the test source set to the test-class processor, so `forkEvery = 1` restarts the JVM
  once per class file — neither per candidate test class nor per matched class. Proof: 204 `.class`
  files under `build/classes/java/test`, 6 of them anonymous (`$1`) → 198, matching the 198
  `junit-platform-unique-ids-*.txt` files exactly; only 28 of them were non-empty. `--tests` filters
  *inside* the worker at JUnit discovery, so it removes no forks at all — 170 JVMs booted, found
  nothing and exited. Without `forkEvery`: 1 file.
- **All three suspected root causes were disproven**: `BootSignalRecorder` has no static state
  whatsoever (instance fields, DI-`@Singleton` via a test `@Factory`); `Configuration.useInMemory` /
  `DATABASE_TEMP_SQLITE` are public and mutable but *instance* fields of a `@Singleton` that nothing
  in the tree writes; and the shared SQLite file was already gone when the fork landed — at
  `2f9888a` every `@MicronautTest` already pointed at its own file under `build/tmp/**`. Decisive:
  a worktree at `2f9888a`, **the commit that introduced `forkEvery = 1`**, runs green without it
  (26 classes, 122 tests, 31 s). The same crutch had already been added and removed on 2026-06-20.
- **Measured** (2026-08-01, main): `cleanTest test` (cache disabled so the tests really execute)
  **1m 01s**, five consecutive runs all green and identical — 28 classes, 134 tests, 0 failures,
  0 errors, 1 fork each. Baseline ~9 min. With the build cache on, a repeat run replays in **5 s**.
  The guard was proven to have teeth: re-adding `forkEvery = 1` fails the build in 1 s with the
  explanatory message.
- **Verify** (done): `./gradlew test` wall time at or under the expected number; suite green in 5
  consecutive runs (isolation regressions show up as flakes); no test order dependence
  (`test.systemProperty("junit.jupiter.testclass.order.default", …)` experiments are out of scope).
- **KB**: testing.md execution-model section; this also completed the testing.md inventory refresh
  (the conformance audit's remaining doc item), so that backlog row is gone.

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
- **Measured** (2026-07-31): the `--dry-run` task graph drops from **12 tasks to 2** (the whole
  `compileJava` / `processResources` / `:frontend:npmBuild` chain is gone). A deliberate brace-less
  `if` still fails `checkstyleMain` with a `NeedBraces` error, so the gate keeps its teeth without a
  compiled classpath, and that failing run took **6s**.

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

### S8: Persist caches across devcontainer rebuilds — **landed 2026-08-01, proof pending**

- **Change** (done): `.devcontainer/devcontainer.json` mounts the named volumes `evergore-gradle` →
  `~/.gradle` and `evergore-npm` → `~/.npm`, and `postCreateCommand` starts with
  `sudo chown -R vscode:vscode` on both (a fresh volume is root-owned, which is what broke the
  earlier `~/.m2` attempt). The whole chain now uses `&&` and no trailing `|| true`.
- **Caveat**: devcontainer changes cannot be validated inside the container (its image is built by
  the host's Dev Containers extension); the next rebuild is the proof.
- **Verify** (next rebuild): rebuild the container twice; the second rebuild's postCreate build
  needs no dependency downloads and finishes in a fraction of the first.
- **KB**: documented in dev-environment.md.

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
