# 05: Testing

> **ADAPT:** this doc is a skeleton: fill in your test inventory and coverage map, verified against
> the actual test sources. The rules under "What keeps the suite trustworthy" are reusable process
> knowledge; keep them. The strategy itself (pyramid, arrange discipline, no buried logic) lives in
> [engineering-handbook.md](engineering-handbook.md) §6; this doc holds the project's concrete facts.

## Inventory

> **ADAPT:** one row per test class: what it covers (terse but concrete: the behaviors asserted,
> the fakes/stubs used), and its style. Include non-test helpers and fixture generators; a new
> session should learn the suite from this table without opening every file.

| Test | Scope | Style |
|------|-------|-------|
| | | |

## What keeps the suite trustworthy

- **Deterministic, never wall-clock-dependent.** A test passes on any hardware; waiting on an async
  event means waiting on a real completion signal (a latch awaited with **no timeout**, released on
  both the success and the failure path so a bad run fails an assertion instead of hanging). A
  timeout or retry is a mitigation, not a fix (handbook §6).
- **No shared static state.** When tests need state written by the application at startup and read
  back in the test body, observe it through an injected, DI-shared recorder object, never static
  flags. Static state bleeds across contexts and produces order-dependent failures (handbook §3/§6).

  > **ADAPT:** name your recorder/seam classes here once they exist, so agents reuse them instead
  > of reinventing the bridge.
- **Robust assertions.** Assert on structure behind a helper (e.g. parse rendered output into
  header + rows), never on raw markup or fuzzy string similarity; all format coupling lives in that
  one helper, so restyling never breaks assertions (handbook §6).
- **Synthetic committed fixtures, zero PII.** Committed test data is generated, never a production
  snapshot; a fixture generator keeps it reproducible and derives values from the production
  catalog/rules rather than inventing them. Real-data snapshots stay gitignored.

  > **ADAPT:** name your fixture generator and the committed fixture files, and how to regenerate
  > them.
- **One behavior, pinned once, at one level.** When a dedicated test class takes a behavior over, the
  older ad-hoc assertions about it go in the same change; duplicate coverage is maintenance cost, not
  safety (handbook §6).
- **A test for an injected seam asserts that the seam was driven**, not only the outcome it enables:
  count the fake's calls, read the fake clock. If the assertion would still pass with the production
  collaborator wired in, the test is fake-green (handbook §6).

## Proving a run really executed

A run is judged by proof that it **executed**, never by its exit code, and the evidence has to be
read, not assumed.

- **Deleting the outputs does not force execution.** With a build cache on, the tool restores the
  test task from cache and *rewrites the result files from that entry*: exit code, a full-looking
  result directory, and not a single test run. Neither the exit code nor the result count can tell
  that run from a real one.
- **Disabling the cache is the mechanism; the task lines are the evidence.** A run counts only when
  the test task's line stands **bare** (no cached, no up-to-date marker) and the result count is
  read alongside it.
- **Two consecutive clean runs** are the bar for a load-sensitive change (a suite whose worker or
  environment count just grew, anything touching concurrency or startup ordering): a cache hit hides
  exactly that class of failure, and a cached second run is not a second run.

The gateway build is `./verify all`; the stack preset makes it disable the caches and print the
executed-test proof (build-run-deploy.md, the verify contract).

> **ADAPT:** write here which lines of `./verify all`'s output are that proof in this project and
> where the result files land (example, Gradle: the `:test` line bare of `FROM-CACHE`/`UP-TO-DATE`,
> the count under `build/test-results/test/`).

## Coverage map

> **ADAPT:** two lists, kept current: "Has tests" (the behaviors genuinely covered, including
> indirect coverage via smoke/acceptance tests) and "Most important UNTESTED logic" (ranked; this
> list drives the next test work).

## Testing direction (BDD/TDD)

**BDD comes first and is mandatory** (handbook §5): a feature with observable behavior begins with
Gherkin scenarios in product language, gated by the scenario falsifier, **confirmed by the author as
the complete acceptance**, and committed tagged `@wip` before any production code; TDD cycles
(handbook §4) then drive them green, step definitions included, and the feature is armed (`@wip`
removed) when they pass. Only a pure refactoring, a `[doc]` commit or build/infra work is exempt,
and the exemption is claimed explicitly.

- **One acceptance runner for the system**, named below, with the feature files where it expects
  them; `./verify bdd` runs the `@wip` scenarios, `./verify all` the armed ones. A
  `@characterization` scenario awaiting the author's confirmation (the `/bdd-catch-up` skill) is
  excluded from `all` like `@wip`.
- **The unit under test carries one name across the whole suite** (e.g. `tested`), so every test
  reads the same way.
- **The test name carries the intent**, not an assertion message; a message that says something the
  name does not is a name that needs rewriting (handbook §6).

> **ADAPT:** the acceptance runner (e.g. cucumber-jvm, cucumber-js, pytest-bdd, Reqnroll), where the
> feature files live, the scenario language if not English, the name you use for the unit under
> test, and one example scenario in domain language, marked "(example)".
