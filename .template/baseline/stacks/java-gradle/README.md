# Preset: Java / Gradle

## Placeholder values

| Token | Value |
|---|---|
| `{{TECH_STACK}}` | `Java/Gradle` |
| `{{BUILD_CMD}}` | `./gradlew build` (full build incl. all tests and quality gates) |
| `{{TEST_CMD}}` | `./gradlew test --tests <TestClass>` (focused single-class run) |

## settings.json allow block

Merge into `permissions.allow`:

```json
"Bash(./gradlew:*)",
"Bash(java -version)",
"Bash(java --version)"
```

`./gradlew` covers build, tests, and custom tasks; no broader `java`/`jar` allows are needed for the
normal loop (widen locally if your project runs the app outside Gradle).

## Lint, format, warnings-as-errors

- **Compiler:** `options.compilerArgs << "-Xlint:all" << "-Werror"` on all `JavaCompile` tasks, so
  warnings fail the build (handbook §3).
- **Style/format:** Checkstyle (style) and Spotless (format) as Gradle plugins, configured from one
  place; `./gradlew check` runs them with the tests.
- **pre-commit format gate:** `./gradlew spotlessCheck` (the hook ships this as a commented example;
  see its ADAPT markers). Keep the test run and full build out of the hook.
- **Production-source tree** for the hook's hard-coded-token scan: `src/main/` (the shipped default).

## Toolchain pinning (single-sourcing, dev-environment.md)

- Build file: the Java toolchain declaration (`java { toolchain { languageVersion = ... } }`) plus
  the Gradle wrapper version (`gradle/wrapper/gradle-wrapper.properties`).
- Variant A: the devcontainer Java feature (e.g.
  `"ghcr.io/devcontainers/features/java:1": { "version": "25" }`) states the same version.
- Variant B: the `java` entry in `.tool-versions` states the same version.
