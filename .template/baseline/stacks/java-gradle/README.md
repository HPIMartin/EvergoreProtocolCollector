# Preset: Java / Gradle

## Placeholder value

| Token | Value |
|---|---|
| `{{TECH_STACK}}` | `Java/Gradle` |

## `verify`

Copy [verify](verify) to the repo root and work through its ADAPT notes. What each subcommand
runs:

| Subcommand | Command |
|---|---|
| `format` | `./gradlew --quiet --console=plain spotlessCheck checkstyleMain checkstyleTest` |
| `focus <Test.java>` | `./gradlew test --no-build-cache --tests <simple class name>` |
| `focus <x.feature>` | `./gradlew test --no-build-cache -Dcucumber.features=<path> -Dcucumber.filter.tags='@wip or not @wip'` |
| `bdd` | `./gradlew test --no-build-cache -Dcucumber.filter.tags='@wip'` |
| `all` | `./gradlew clean build --no-build-cache --console=plain`, then the executed test-class count from `build/test-results/test/` |
| `outdated` | `./gradlew dependencyUpdates` (com.github.ben-manes.versions plugin) |
| `vuln` | `./gradlew dependencyCheckAnalyze` (OWASP dependency-check) |
| `stop` | `./gradlew --stop` against this tree's own registry (contract: `stacks/README.md`) |

- A relative `focus` path resolves against the **caller's** directory, so the script behaves the same
  from a subdirectory as from the root.
- The `test` task must forward the two cucumber system properties into the test JVM, or `bdd` and a
  focused feature run cannot override the tag filter. The same block relaxes
  `failOnNoDiscoveredTests` (true by default since Gradle 9) for exactly those runs, so an empty
  `@wip` set is a clean pass while the gateway build still fails on a suite that discovered nothing:

  ```groovy
  tasks.named('test') {
      useJUnitPlatform()
      def tags = System.getProperty('cucumber.filter.tags')
      def features = System.getProperty('cucumber.features')
      if (tags != null) {
          systemProperty 'cucumber.filter.tags', tags
          failOnNoDiscoveredTests = false
      }
      if (features != null) {
          systemProperty 'cucumber.features', features
      }
  }
  ```

- Proof of execution for `all`: the `:test` line stands bare (no `FROM-CACHE`, no `UP-TO-DATE`) and
  the printed count matches the XML files under `build/test-results/test/`
  ([testing.md](../../docs/knowledge-base/testing.md)). The count prunes `.git`, `.claude`,
  `.template` and `node_modules`, so a worktree or a vendored tree cannot inflate it. It stays a
  deny-list: a stale nested build under an unpruned path (`tools/codegen/build/`) still counts until
  you add it, so read the `:test` line as the primary evidence.
- CI reads the same `build/test-results/test/TEST-*.xml` files, so `all` needs no extra reporter.

## settings.json allow block

Merge into `permissions.allow`:

```json
"Bash(./gradlew:*)",
"Bash(nohup ./gradlew:*)",
"Bash(java -version)",
"Bash(java --version)"
```

`./gradlew` covers build, tests, and custom tasks; `nohup ./gradlew` backgrounds a long build (a
wrapper is safe as `allow` only when pinned to one inner command like this, never unscoped). No
broader `java`/`jar` allows are needed for the normal loop; widen locally if your project runs the
app outside Gradle. Diagnostics that have proven useful once the project matures: `Bash(java:*)`,
`Bash(javac:*)`, `Bash(javap:*)`, `Bash(unzip -l:*)` (jar inspection), `Read(//usr/lib/**)` (JDK
internals in the devcontainer), and `Bash(trivy:*)` if a Trivy-based vulnerability scan replaces the
OWASP plugin.

## Lint, format, warnings-as-errors

- **Compiler:** `options.compilerArgs << "-Xlint:all" << "-Werror"` on all `JavaCompile` tasks, so
  warnings fail the build (handbook §3).
- **Style/format:** Checkstyle (style) and Spotless (format) as Gradle plugins, configured from one
  place; `./gradlew check` runs them with the tests, and `./verify format` runs them alone.
- **Versions** of the plugins and of Cucumber live in the build file (a `cucumber-bom` entry for the
  Cucumber artifacts), never in this preset.

## Cucumber-JVM wiring

Dependencies to add (versions from the Cucumber BOM in the build file):

```groovy
testImplementation platform('io.cucumber:cucumber-bom:<version>')
testImplementation 'io.cucumber:cucumber-java'
testImplementation 'io.cucumber:cucumber-junit-platform-engine'
testImplementation 'org.junit.platform:junit-platform-suite'
testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```

- **Feature files:** `src/test/resources/features/`, one file per capability, in product language.
- **Step definitions:** `src/test/java/<base package>/acceptance/`, classes named by domain concept.
- **Default tag filter:** `src/test/resources/junit-platform.properties`:

  ```properties
  cucumber.filter.tags=not @wip and not @characterization
  cucumber.glue=<base package>.acceptance
  cucumber.plugin=summary
  cucumber.publish.quiet=true
  ```

  The file is the lowest-precedence source, so the `-Dcucumber.filter.tags` of `verify bdd` and of a
  focused feature run override it. Do not move the tag filter into a `@ConfigurationParameter`:
  a suite annotation outranks the system property and would pin the filter.
- **Suite class** (Gradle hands JUnit only the test classes for discovery, so the features are
  selected explicitly), `src/test/java/<base package>/acceptance/RunAcceptanceScenariosTest.java`:

  ```java
  @Suite
  @IncludeEngines("cucumber")
  @SelectPackages("features")
  class RunAcceptanceScenariosTest {
  }
  ```

- **Step-definition shape**, each actor driven through its real surface against a fresh real
  service (handbook §5): a page object over the browser for a screen, a client for an API. No
  comments (`hooks/content-gate` refuses them):

  ```java
  public final class ContributionSteps {

      private final RunningService service;
      private final ScenarioBrowser browser;
      private ContributionOverviewPage overview;

      public ContributionSteps(RunningService service, ScenarioBrowser browser) {
          this.service = service;
          this.browser = browser;
      }

      @Given("the order book holds an order from {string} worth {int} placed on {}")
      public void theOrderBookHoldsAnOrder(String customer, int worth, String placedAt) {
          service.orders().place(new Order(customer, worth, ScenarioTime.parse(placedAt)));
      }

      @When("the manager opens the contribution overview")
      public void theManagerOpensTheContributionOverview() {
          overview = ContributionOverviewPage.openIn(browser, service);
      }

      @Then("the manager sees {int} in {word}'s contribution column")
      public void theManagerSeesInTheContributionColumn(int expected, String customer) {
          assertThat(overview.contributionOf(customer)).isEqualTo(expected);
      }
  }
  ```

  `RunningService` is the project's own class: a hook starts a fresh service before each scenario
  and stops it after, and `orders()` writes through the service's real write path.

  > **ADAPT:** a project that chose the in-memory model at adoption drives ports with in-memory
  > fakes instead: the `Given` records into an in-memory port and the `When` runs the query over it.

- **A scenario under development** carries `@wip` on the level handbook §5 names (step 3), and one
  derived from legacy code carries `@characterization` until the author confirms it, so
  `verify all` stays green either way.

<!-- MODULE acceptance -->
## Web-UI acceptance (browser half)

- **Shipped files:** [acceptance/](acceptance/) lands under
  `src/test/java/<base package>/acceptance/`, its package line `{{BASE_PACKAGE}}` resolved by
  `/adopt`.
  - `OrphanedBrowserSweep` (+ test) and `AcceptanceHooks`: the sweep before all scenarios, the
    browser left after each, `quitAll` and the skip count after all.
  - `browser/ScenarioBrowser` (a scenario's own browser and download folder, started on its first
    use and quit on its leave, + `ScenarioBrowserTest`), `Browsers` (the registry of started
    browsers that `quitAll` empties after the run, + `BrowsersTest`), `BrowserChoice` (local
    Firefox or the grid, + `BrowserChoiceTest`), `ShiftedClock` (+ `ShiftedClockTest`),
    `TimeZoneBrowser` (+ `TimeZoneBrowserTest`), `BiDiDriver` (the one WebDriver BiDi check);
    `RecordingDriver` is a test helper.
  - `ShiftedClockTest` sets the default locale under JUnit's `LOCALE` resource lock, so a project
    that runs Jupiter tests concurrently gives its own locale-reading tests that lock too.
  - `browser/GeckoDriverExecutable` (+ test), `AcceptanceBrowserStartTest` (the start rules over
    all test code of the base package), `LocalBrowserCheck` (+ test), `SkippedScenarios`,
    `OwnBuildDirectory` (+ test).
  - No comments in any of them, so the adopter's content gate accepts them in `src/test/`.
  - They assume Linux: Unix paths, `/usr/bin/perl` (the sweep test), and a `firefox` on the
    `PATH`. The adopter's formatter formats them after the copy.
  - In a container, the sweep test's kill cases need a first process that reaps orphaned
    children:
    - One that reaps none (`tail -f /dev/null`) leaves a killed plant a zombie, and the case
      hangs.
    - `docker run --init`, compose's `init: true` or devcontainer.json's `"init": true` gives one.
    - A CI job's first process is the runner's choice and unproven here, so a job that runs the
      sweep test in a container gives it an init.
- **Dependencies** (versions in the build file):

  ```groovy
  testImplementation 'io.cucumber:cucumber-picocontainer'
  testImplementation 'org.seleniumhq.selenium:selenium-java'
  testImplementation 'org.junit.jupiter:junit-jupiter'
  testImplementation 'com.tngtech.archunit:archunit-junit5'
  testImplementation 'org.assertj:assertj-core'
  testCompileOnly 'com.google.auto.service:auto-service-annotations'
  ```

  The last one only silences a `-Xlint:all` class-file warning of Selenium's driver service.
- **Parallel scenarios,** `src/test/resources/junit-platform.properties`, plus one exclusive
  resource for each tag that marks process-wide state (`time-zone` is an example tag name):

  ```properties
  cucumber.execution.parallel.enabled=true
  cucumber.execution.parallel.config.strategy=fixed
  cucumber.execution.parallel.config.fixed.parallelism=4
  cucumber.execution.parallel.config.fixed.max-pool-size=4
  cucumber.execution.exclusive-resources.time-zone.read-write=\
      org.junit.platform.engine.support.hierarchical.ExclusiveResource.GLOBAL_KEY
  ```

- **Settings reach the test JVM as properties** (inside the `test` block shown above):

  ```groovy
  tasks.named('test') {
      systemProperty 'otel.logs.exporter', 'none'
      def forwarded = [
          ACCEPTANCE_GRID       : 'acceptance.grid',
          ACCEPTANCE_BROWSER    : 'acceptance.browser',
          ACCEPTANCE_NO_BROWSER : 'acceptance.noBrowser',
      ]
      forwarded.each { variable, property ->
          def value = providers.environmentVariable(variable).orNull
          if (value != null) {
              systemProperty property, value
          }
      }
      def parallelism = providers.environmentVariable('ACCEPTANCE_PARALLELISM').orNull
      if (parallelism != null) {
          systemProperty 'cucumber.execution.parallel.config.fixed.parallelism', parallelism
          systemProperty 'cucumber.execution.parallel.config.fixed.max-pool-size', parallelism
      }
      testLogging {
          showStandardStreams = true
      }
  }
  ```

  - `otel.logs.exporter=none` keeps Selenium's OpenTelemetry log exporter quiet, which matters
    with the grid.
  - `showStandardStreams` lets the skip count below reach the console.
  - Both limits of Cucumber's worker pool follow `ACCEPTANCE_PARALLELISM`: it grows to its
    maximum while a worker blocks.
- **No silent skip,** the rule of [testing.md](../../docs/knowledge-base/testing.md), "The browser
  half"; the preset's values are `ACCEPTANCE_NO_BROWSER` and the `firefox` it looks for on the
  `PATH`.
- **The sweep** form is `OrphanedBrowserSweep.SELECTOR`, the one ADAPT value, `MARKER` (the
  default) or `ANCESTRY` (devcontainer variant only); the forms and their reach:
  testing.md, "The browser half". The local start keeps geckodriver's profiles under
  `build/tmp/browsers`, which marks them and keeps them out of `/tmp`.
  - The profile, download and marker paths resolve against the test JVM's working directory
    (`build/` there), which Gradle's `test` task sets to the project directory.
    - A moved `buildDirectory` puts them outside what `clean` removes.
    - An IDE run from another directory puts them where neither `clean` nor the Gradle run's sweep
      reaches.
- **The driver:** the local start takes geckodriver from the `webdriver.gecko.driver` property,
  else from the `PATH`. With neither, Selenium Manager downloads a driver of its own choosing at
  test time, so a reproducible or offline run pins one, by that property or by a geckodriver the
  toolchain puts on the `PATH`.
- **Other ADAPT values:** `BrowserChoice.BROWSER_LOCALE` (`en`), `SERVICE_HOST_ON_THE_GRID` (shared
  with the grid recipe's `alias`), the exclusive-resource tag above, the pinned Selenium image tag
  in the grid recipe.
- **Grid:** [grid/](grid/) lands at `grid/` in the repository; its use, its refusals and its
  attended-session limit: [build-run-deploy.md](../../docs/knowledge-base/build-run-deploy.md),
  "The browser grid". Its values here are `SELENIUM_TAG`, `wait_seconds`, the network name and
  the alias in `grid/grid`, and the hub's `start_period` in `grid/compose.yaml`.
  - An `ACCEPTANCE_GRID` that is set but empty counts as a grid, and the start fails on the empty
    address; a local run leaves the variable unset.
- **A page object opens a page only through `open(addressOf(port, path))`** of `ScenarioBrowser`:
  `open` checks the shifted clock after the load, and `addressOf` uses the grid's service host
  where a grid runs. `driver().get(...)` with a hand-built address skips both.
<!-- /MODULE acceptance -->

## KB guard (optional)

- **What it guards:** DOC-12 of the [KB README](../../docs/knowledge-base/README.md) over the
  Markdown files under `docs/knowledge-base/`, at any depth. The rule, its example list and its stop
  list live there; this section does not restate them.
  - It reads the name that opens a code span (`` `Name` ``, `` `Name.member()` ``), also where the
    span wraps onto the next line, and not a name inside a span (`List<Name>`, `new Name()`,
    `@Name`). Its blocks follow a subset of CommonMark's rules: a rare shape (a paragraph line
    that starts with `|`, a fence inside a blockquote) can hide a name or flag a well-formed line.
  - It reads each line inside a fenced block as code spans too, so a template literal or shell
    backticks there can yield a candidate: list it on the stop list or move it out of the fence.
  - Any fence line of the fence's character closes the fence, whatever its length.
  - A name resolves when any class on the test classpath or in the JDK carries it as its simple
    name, nested, local and JDK-internal classes included. So a renamed project class whose old
    name a dependency or the JDK also uses (`ConnectionPool`, `TaskQueue`) stays unflagged.
- **Shipped files:** [kb-guard/](kb-guard/) lands under `src/test/java/<base package>/kbguard/`, its
  package line `{{BASE_PACKAGE}}` resolved by `/adopt`.
  - `KbCitationGuardTest`: the guard and its tests.
  - `KbClassReferenceExtractor` (+ test): the candidate rules.
  - `ListedTypeNames` (+ test): reads DOC-12's two lists from the KB README, and fails closed on
    an entry without a reason or whose name the candidate rule yields neither on its own nor
    heading a dotted access, naming the line, or on a README without the block, naming the file.
  - `KnownJavaSymbols` (+ test): the simple names of the test classpath (directories and jars)
    and of the JDK.
  - `KnownFrontendSymbols` (+ test): the names a frontend source tree declares, plus the platform
    list.
  - No comments in any of them, so the adopter's content gate accepts them in `src/test/`. The
    adopter's formatter formats them after the copy.
  - The JDK names come from the runtime image the tests run on, so a JDK name resolves only where
    that runtime holds its module, as a full JDK does.
- **Dependencies:** JUnit Jupiter and AssertJ only, both already in the test classpath.
- **The `test` task declares the inputs,** so a change to the KB alone, or to the frontend sources
  alone, reruns the guard and the task is never `UP-TO-DATE` over a stale verdict. The second input
  only where the frontend root below is set:

  ```groovy
  tasks.named('test') {
      inputs.dir('docs/knowledge-base').withPropertyName('knowledgeBase')
              .withPathSensitivity(PathSensitivity.RELATIVE)
      inputs.dir('frontend/src').withPropertyName('frontendSources')
              .withPathSensitivity(PathSensitivity.RELATIVE)
  }
  ```

- **ADAPT values:**
  - The frontend source root, the same path in `KbCitationGuardTest.FRONTEND_SOURCE_ROOT` and in the
    second input (`""` and no second input where no frontend sits beside the backend; a root that is
    set and is no directory fails the test). Point it at the sources (`frontend/src`), not the
    frontend's project folder: build output and package caches there would resolve their names.
  - `KnownFrontendSymbols.PLATFORM_NAMES`: the platform names the project's KB cites; the shipped
    list is the ECMAScript globals plus `Temporal`.
  - DOC-12's example list and stop list grow with the project's KB.

  > **ADAPT:** the KB guard's three values above: the frontend source root, the platform list and
  > DOC-12's two lists.

- **The paths assume the root Gradle project runs the tests,** as the rest of this preset does.
  Where a subproject runs them, its `test` task declares the inputs with `rootProject.file(...)` and
  sets `workingDir = rootProject.projectDir`, and the gate block names that task, the up-to-date
  run admitted as in `gate.env`: `docs_guard_ran_re='^> Task :backend:test( UP-TO-DATE)?$'`.
- **A project with no Gradle backend** (pure js-ts-npm) runs no guard: the `doc-reviewer` checks
  DOC-12 by eye.

## Probe source set (agent throwaway code, build-run-deploy.md)

A `probe` source set whose compile and runtime classpath is the **test** classpath plus
`sourceSets.test.output`, so a probe can use the test fixtures:

- `probeAnnotationProcessor extends testAnnotationProcessor`, or an annotation-processing framework
  never runs over the probe and DI-based probes find no beans.
- Deliberately **not** wired into `check`/`build`, and excluded from the format/style tasks (a
  `checkstyleProbe` would pull `compileProbeJava` in with it): a failing, unformatted or
  uncompilable probe leaves `./verify all` green.
- `compileProbeJava` drops `-Werror` (keeps `-Xlint:all`): a probe measuring a deprecated API is a
  normal probe, and the task reaches no `build` graph.
- `./gradlew probe` runs it; `./gradlew clearProbes` is a committed `Delete` task that removes
  `src/probe`, so an agent clears its own probes without ever running `rm` (handbook §7).
- `/src/probe/` is gitignored (shipped in the template `.gitignore`), so `git add -A` cannot commit
  one, and an empty directory leaves the task `NO-SOURCE` for Gradle to skip.

## Git gate settings

- The shipped `production_trees`/`test_trees` defaults fit a single-module Gradle layout. A
  multi-module build sets `production_trees='*/src/main/'` and `test_trees='*/src/test/'`.
- `print_allow_paths` stays empty unless a module is a CLI adapter whose stdout is the interface.

## Toolchain pinning (dev-environment.md, "Toolchain version")

- Build file: the Java toolchain declaration (`java { toolchain { languageVersion = ... } }`) plus
  the Gradle wrapper version (`gradle/wrapper/gradle-wrapper.properties`).
- Variant A: the devcontainer Java feature (e.g.
  `"ghcr.io/devcontainers/features/java:1": { "version": "25" }`) states the same version.
- Variant B: the `java` entry in `.tool-versions` states the same version.

## Modernisation measurements

- `verify outdated`: `dependencyUpdates` (com.github.ben-manes.versions) reports every dependency
  and plugin with a newer release. Without the plugin, `./gradlew --refresh-dependencies dependencies`
  shows what a fresh resolution picks.
- `verify vuln`: OWASP dependency-check (`dependencyCheckAnalyze`) or `trivy fs --scanners vuln .`.
- Dependency bot: Dependabot `package-ecosystem: gradle`, or the Renovate `gradle` manager
  (snippets: [stacks/README.md](../README.md)).
