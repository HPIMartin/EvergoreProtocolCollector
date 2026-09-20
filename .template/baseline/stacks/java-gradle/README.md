# Preset: Java / Gradle

## Placeholder value

| Token | Value |
|---|---|
| `{{TECH_STACK}}` | `Java/Gradle` |

## `verify`

Copy [verify](verify) to the repo root and work through its `# ADAPT` lines. What each subcommand
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
  @SelectClasspathResource("features")
  class RunAcceptanceScenariosTest {
  }
  ```

- **Step-definition shape**, scenarios driven through ports with in-memory fakes (handbook §5), no
  comments (`hooks/content-gate` refuses them):

  ```java
  public final class MonthlyContributionSteps {

      private final InMemoryOrders orders = new InMemoryOrders();
      private MonthlyOverview overview;

      @Given("customer {string} placed orders worth {int} this month")
      public void customerPlacedOrdersWorth(String customer, int total) {
          orders.record(new Order(customer, total));
      }

      @When("the manager opens the monthly overview")
      public void theManagerOpensTheMonthlyOverview() {
          overview = new MonthlyOverviewQuery(orders).run();
      }

      @Then("the contribution column for {string} shows {int}")
      public void theContributionColumnShows(String customer, int expected) {
          assertThat(overview.contributionOf(customer)).isEqualTo(expected);
      }
  }
  ```

- **A scenario under development** carries `@wip` on the `Scenario` (or on the `Feature` while the
  whole file is new), and one derived from legacy code carries `@characterization` until the author
  confirms it, so `verify all` stays green either way.

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

## Toolchain pinning (single-sourcing, dev-environment.md)

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
