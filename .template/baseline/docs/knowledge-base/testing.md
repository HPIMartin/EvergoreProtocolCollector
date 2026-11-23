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
  - A test the author excepted to wait on real time (handbook §6) says so in its inventory row,
    "waits on the wall clock, by author decision", with the reason.
- **No shared static state.** When tests need state written by the application at startup and read
  back in the test body, observe it through an injected, DI-shared recorder object, never static
  flags. Static state bleeds across contexts and produces order-dependent failures (handbook §3/§6).
  - **The run-scoped exception: a registry of costly resources** that the runner has no run scope
    for, so that a close after the run reaches what a scenario never closed.
    - Only by author decision, recorded in [open-questions.md](../open-questions.md).
    - A scenario closes its own resource and takes it out of the registry first; nothing in the
      registry is handed to another scenario.
    - The final close-all closes every instance left, each in its own `try`, and rethrows the
      first failure with the others suppressed.

  > **ADAPT:** name your recorder/seam classes here once they exist, so agents reuse them instead
  > of reinventing the bridge, and each run-scoped exception with its decision.
- **Robust assertions.** Assert on structure behind a helper (e.g. parse rendered output into
  header + rows), never on raw markup or fuzzy string similarity; all format coupling lives in that
  one helper, so restyling never breaks assertions (handbook §6).
- **Synthetic committed fixtures, zero PII.** Committed test data is generated, never a production
  snapshot; a fixture generator keeps it reproducible and derives values from the production
  catalog/rules rather than inventing them. Real-data snapshots stay gitignored.
  - **The one other kind of committed data is facts about an external system**, derived from what
    was read, never the raw dumps.
    - One fact per line, each with its source and the date it was read.
    - No personal data.
    - An absence the source shows is a dated row of its own, so it reads as a claim, not a gap.
    - A guard test rejects, in the facts file itself, the personal-data markers the raw source
      carries.
    - The guard test holds the code to the facts one way: every recorded fact holds in the code,
      and the code may know more.
      - It reads each fact through the production class that holds its rule (below).
  - **A real-data snapshot is opened read-only**, or copied into the worktree's build output
    before anything writes to it (a migration check, the app, a test on real data).
  - **Every result names the snapshot it used.**
  - **A snapshot, or data drawn from one, is never committed.**
    - What may leave the repository of it, an upload included: handbook §7, "No secrets, no
      personal/host data".
  - **An agent takes no snapshot itself.**
    - Where the newest is too old for the task, it says so and asks the author.

  > **ADAPT:** name your fixture generator and the committed fixture files, and how to regenerate
  > them. With external facts, name the facts file, its guard test and the markers it rejects: no
  > hook knows the project's own personal-data markers (names, IDs), so the guard test is their
  > barrier; for e-mail addresses and host paths, `hooks/content-gate` applies
  > ([hooks/README.md](../../hooks/README.md), "The checks").
- **One behavior, pinned once, at one level.** When a dedicated test class takes a behavior over, the
  older ad-hoc assertions about it go in the same change; duplicate coverage is maintenance cost, not
  safety (handbook §6).
- **A test for an injected seam asserts that the seam was driven**, not only the outcome it enables:
  count the fake's calls, read the fake clock. If the assertion would still pass with the production
  collaborator wired in, the test is fake-green (handbook §6).
- **A tool, a guard or an on-demand check resolves a rule through the production class that holds
  it**, never through a copy of the rule.
  - A guard written against its own copy reports differences production does not have.
  - Why: handbook §3, "A rule lives in one place".
- **A page of text is pinned 1:1 to an approved file** (handbook §5).
  - The file holds the text the author approved.
  - A test renders the page and compares its whole visible text with the file.
  - A changed word fails that test until the file changes in the same commit, for the author to
    approve.
  - Why: handbook §5, the page-of-text bullet.

  > **ADAPT:** name each pinned page with its approved file and its test; if the product shows no
  > such page, delete this bullet, the page-of-text bullet of handbook §5 and its Definition of
  > Done line on rules the product explains to its users.

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
- **A file a test reads outside its source set is a declared input of the test task** and in the
  image's build context; run the mutation once (change the file, see the task run again).

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
Gherkin scenarios in product language, gated by the scenario gate, **confirmed by the author as
the complete acceptance**, and committed tagged `@wip` before any production code; TDD cycles
(handbook §4) then drive them green, step definitions included, and the cycle that turns the last
one green arms them (`@wip` removed). Only a pure refactoring, a `[doc]` commit or build/infra work
is exempt, and the exemption is claimed explicitly.

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
> test, and one example scenario in domain language, marked "(example)". Name the actors your
> scenarios use and the surface each reads (e.g. a customer reads their screens, an operator reads
> the service's responses, its health report and its log; handbook §5, "Reading as the
> stakeholder"), and the one clock seam through which the time steps set the clocks (handbook §5,
> TIME-5).

## The acceptance runner

What a scenario runs against (handbook §5, "One acceptance runner per system"):

- **A fresh service per scenario** where the system runs as a service: its own port and its own
  store, started before the scenario and stopped after it.
  - Why: in-memory state, restarts and startup behavior need a service no earlier scenario touched.
  - Rejected: one service per run with the store emptied between scenarios; whatever the emptying
    misses leaks into the next scenario.
- **The scenario's world lives outside the service's container.**
  - It is handed in when the service starts, so it outlives a restart.
  - The runner's own DI shares it between the step classes of one scenario.
  - An external system is a fake behind its port, never the live system.
  - A `Given` writes through the real write path, so "the store holds" means the stored data.
- **Faults come from a proxy around the ports** that knows the port types only, no method or
  schema of one adapter.
- **A scheduled job is recorded, not started, and fired by its step** through the runnable the
  scheduler would call; the step asserts that exactly one job ran, so no scenario waits on a timer.
- **"The service's log" is the recording at the project's own logger port**, taken before the line
  reaches the logging framework; the framework's own lines are not in it.
  - A run against a reachable source fails its step unless the log says it read that source, so a
    crash cannot pass for "found nothing".
- **The operator's scenarios start the service as the operator ships it:** the production defaults
  and the settings from the environment the operator sets, never the test configuration.
  - A start the service refuses never reaches the server's start, so no port stays bound.
- **A step that changes process-wide state requires its tag** (the default time zone is the usual
  case).
  - The project's step refuses a scenario without the tag.
  - The runner holds the tag as an exclusive resource, so no other scenario runs beside one.
  - An after-hook restores the state.
- **A raw client request** is written on a socket where the standard URL type refuses the address.
  - A test-only client address travels in a header that a test-source filter takes before the
    socket's own address; production never trusts it.
- **A test context never reaches a live system**, also after it is closed.
- **Leaked external processes:** before a run starts, it removes the leftovers of a dead run
  (external processes the tests start, a browser and its driver say, that no living test process
  descends from) and fails once, naming them.
  - It acts only on processes that a run of this checkout provably started, or, in the devcontainer
    variant where nothing else runs, on every one of that kind (dev-environment.md).
  - A shutdown hook is no substitute: a killed test process runs none.
- **A tool binary that is resolved lazily is resolved once per test process,** while the resolved
  file stays a driver.
  - Why: parallel first starts race on a binary that is published before it is complete.
  - An architecture test forbids the bypass: no code but the one resolver builds the tool's service.

> **ADAPT:** name the runner's classes (the service hooks, the world, the fault proxy, the
> scheduler replacement, the log recording, the one clock seam), the number of scenarios run at a
> time, and the measured wall times (what ran, how many at a time, the machine, the date) as a
> table; keep the number where more parallel scenarios stop paying, since cold starts dominate a
> short run. A project whose tests resolve no tool binary lazily drops that rule.

<!-- MODULE acceptance -->
### The browser half

- **Each scenario starts its own browser and quits it on leave;** no browser is handed to a
  second scenario.
  - The first use starts it: the choice's browser, or the dedicated time-zone browser.
  - Scenarios running at once each have their own.
  - Why: a browser reused by the next scenario carried what the scenario left in it, in more kinds
    than any guard listed; a fresh browser starts from an empty profile.
  - The cost is a cold start per browser-driven scenario.
  - Steps get the driver as started; nothing narrows what a step may call on it.
  - Each local start gets its own download folder under `build/tmp/downloads/`; a step finds it
    through `ScenarioBrowser.downloads()`. The leave keeps it for the scenario's last steps, and
    `clean` removes it. Nothing goes to the home folder, the desktop's recent documents
    included. On the grid the node keeps downloads, and `downloads()` refuses.
  - The leave runs after the steps' own `@After` hooks of default order, so a hook that reads the
    page (a failure screenshot) sees the scenario's browser.
  - A failed quit fails the leave; a session the scenario ended itself (it closed its only window)
    is no failure.
  - A browser that fails after it started (the zone check, the clock) is quit before the failure
    is rethrown.
  - Gaps, found only by the next run's sweep, which fails that run: a quit that returns while the
    browser hangs (Selenium waits for the driver, then stops only the driver) leaves the browser
    and its profile; so does the leave whose quit failed, since its browser is not tried again.
- **The registry of started browsers is the named instance of the exception to "No shared static
  state".**
  - After all scenarios it quits any browser whose scenario never reached its leave
    (`Browsers.quitAll`); a browser whose quit failed is not tried again.
  - Why: a scenario that never reaches its leave keeps its browser alive until the run ends; the
    sweep finds leftovers only at the next run's start.
- **A browser in its own real-world time zone** is started for the scenario that asks.
  - The driver has no session capability for the browser process's zone, so the zone goes into the
    environment of the driver process, which takes effect at process start only.
  - Only Firefox without the grid runs its own zone; a zone requested after the browser opened
    fails, naming the order.
  - Right after the start the browser's own resolved zone is asserted equal to the requested one,
    so a silent fallback to the container's zone fails loudly.
- **The browser's clock is shifted with a WebDriver BiDi preload script** and checked by
  evaluating in the page's realm, since the classic script execution runs in a sandbox that does
  not see the page's own `Date`.
  - The script replaces `Date` by a proxy of the real one, so `Date()` without `new`, `Date.now()`
    and `instanceof` keep their meaning, and gives the `Intl` date formatter the shifted now where
    no date is passed.
  - The script runs in every new document, so each page load starts the clock at the scenario's
    instant again.
  - Setting the clock again within a scenario adds a script on top of the first; the next page
    shows the later instant.
  - A clock set while a page is open does not reach that page: it takes effect at the next page
    load.
  - Timers, `performance.now`, `Temporal.Now`, `document.lastModified` and workers keep real time.
- **Page reads:** one script per view returns the rendered text as data.
  - Elements are found by stable test ids.
  - A wait is on rendered content and only a hang guard, never the assertion.
    - Its timeout is the hang guard of one exception for the class of browser waits (handbook §6,
      "Exception: a test that must wait on real elapsed time"), which the project records once: a
      dated decision row and one inventory row.
  - A colour is read as the browser computes it and compared with the design tokens; an uncoloured
    figure equals the text colour, and so does its parent's, so a rule painting a whole row fails.
  - A note is read as displayed: the text the driver reports as shown, so a note the stylesheet
    never shows fails.
- **The link walk:** a step reads every link of a page and opens each; each opens its page.
- **The glue default for a new cross-cutting precondition** (a signed-in session, say): a
  scenario that names none runs under a default that reaches everything, one that names a
  precondition uses it, so adding the precondition touches no existing scenario.
- **No silent skip:** without a browser a browser-driven scenario fails, naming the missing browser
  and `ACCEPTANCE_NO_BROWSER`.
  - Only a run that sets `ACCEPTANCE_NO_BROWSER=true` skips them, and it prints once how many it
    skipped.
  - Set it only where no browser exists by design (an image's build stage).
- **The leftover sweep has two forms,** chosen at adoption.
  - `marker` (the default): only a driver or browser with no living test-process ancestor whose
    command line names this checkout's own `build/` directory, so a native session's own browser
    and another checkout's survive.
  - `ancestry`: every such process on the machine; only in the devcontainer variant.
  - The sweep takes its candidate processes: production passes every process, its test only the
    ones it planted, so a foreign browser survives the whole test class.

> **ADAPT:** the sweep form, the browser's locale and the grid's image tag. The files and the grid
> recipe: the java-gradle preset
> ([stacks/java-gradle/README.md](../../stacks/java-gradle/README.md), "Web-UI acceptance
> (browser half)").
<!-- /MODULE acceptance -->

## Release comparison

The release gate before a deploy ([build-run-deploy.md](build-run-deploy.md), "Release & deploy"):
the previous release and the candidate run side by side on copies of the same production snapshot,
and what both serve is compared.

- Why the real snapshot: it catches drift the synthetic fixtures cannot, through the real data
  volume and mix.
- **Copies only:** each side runs on its own copy; the original is copied once per side and never
  opened.
- **The sides are isolated:** each on its own port, in its own process group, listening on the
  loopback interface only, without the caller's runtime options or environment.
- **Throwaway credentials per run**, passed in the environment, never in a command's arguments.
- **Wait for readiness:** the comparison waits, with a timeout, for each side's own signal that its
  startup work is done.
  - A side that fails or stops ends the run.
- **Pacing:** the requests stay under the service's rate limit.
  - A refusal is waited out before a bounded retry.
- **Exact values, not tolerances:** every served figure and every stored exact value both sides
  hold is compared; any difference is a finding.
- **Expected deviations: none by default.** A deviation the release intends is the author's
  decision, recorded with its reason.
- **A hard exit contract:**
  - `0`: no finding.
  - `1`: at least one finding.
  - A code of its own for a call refused before anything started.
  - A code of its own for a side that never became ready or could not be stopped.
  - A code of its own for a comparison that crashed.
  - Both sides are gone after every exit path.

> **ADAPT:** delete this section if nothing is deployed or no production data exists to copy; a
> project without that area drops the frame. Otherwise name the tool that runs the
> comparison, written in the language the project chooses, what it reads from each side, and how
> each side signals readiness.
