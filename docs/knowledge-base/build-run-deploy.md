# 05: Build, Run, Deploy

## Build

- **Tool:** Gradle (Kotlin DSL, `build.gradle.kts`), `io.micronaut.application` plugin, Micronaut
  platform `4.10.3`, **Java 25** (Gradle toolchain, auto-provisioned via the foojay resolver),
  runtime Netty. Main class `…​.Application`. Build and test: `./gradlew build`.
- **Key deps:** Selenium 4.7.2, ORMLite-JDBC 6.1, sqlite-jdbc 3.41.2.2, commons-text 1.10,
  micronaut-openapi (Swagger/RapiDoc/ReDoc), `micronaut-management` (health endpoint + indicators),
  snakeyaml (Micronaut 4 no longer bundles it). Test only: micronaut-test-junit5, JUnit 5 (+
  `junit-platform-launcher`), unirest-java 3.11.11 (used by `SmokeTest`), AssertJ 3.27.7, ArchUnit
  1.4.1 (reads Java 25 bytecode).
- **Entry point / deployable:** the Gradle wrapper (`./gradlew`, distribution pinned in
  `gradle/wrapper/`) is the single entry point, no host toolchain needed beyond a JDK. Deployable
  is the application distribution (`./gradlew installDist` →
  `build/install/protocolParser/bin/protocolParser` + `lib/`), not a fat jar.
- **Build performance (`gradle.properties`):** `org.gradle.caching` and `org.gradle.parallel` are on,
  with `org.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=768m` for the daemon that parallel execution
  needs. The local build cache (`~/.gradle/caches/build-cache-1`) is **shared by every worktree**, so
  a fresh worktree at an already-built commit replays `compileJava`, `checkstyle*` and the
  `:frontend` tasks as cache hits instead of running them cold. The cache lives in the container
  layer, so a devcontainer rebuild discards it (backlog H5).
- **Warnings are errors:** every `JavaCompile` runs `-Xlint:all` + `-Werror`, so any compiler/lint
  warning fails the build. Excluded deliberately: `-serial` (obsolete `serialVersionUID` ceremony)
  and `-processing` (Micronaut/ORMLite/JUnit/ArchUnit annotations no processor claims, inherent to
  the stack, not our code). The code is otherwise warning-clean.
- **Java 25 native access:** Netty calls restricted `System::loadLibrary`, which the JVM warns
  about at boot (and will *block* in a future release). `--enable-native-access=ALL-UNNAMED` is set
  for the test JVM and baked into the distribution's start script (`applicationDefaultJvmArgs`), so
  both tests and the Docker runtime start clean and stay forward-compatible.
- **Formatting:** `config/eclipse/formatter.xml`, a single shared Eclipse JDT profile (tabs,
  `lineSplit=180`, empty bodies compact, enum constants one-per-line, method chains wrap
  one-per-`.` when >180, no blank before a method's closing `}`), is the source of truth for
  **VS Code** (`java.format.settings.url` + `…profile`), **Eclipse**, **IntelliJ**
  (Eclipse-formatter adapter) and the **Gradle build**.
  - Spotless applies it (`eclipse().configFile(...)`) plus `removeUnusedImports` + `importOrder`
    (java → external → `dev.schoenberg` → static last) + `trimTrailingWhitespace` +
    `endWithNewline`, wired into `check`: `./gradlew build` fails on any deviation;
    `./gradlew spotlessApply` fixes.
  - VS Code does format + organize-imports on save (`.vscode/settings.json`); star imports are
    forbidden (`java.sources.organizeImports.starThreshold: 999999` → always explicit; the
    codebase is now wildcard-free).
  - The formatter wraps `if`-bodies but cannot *insert* `{ }`; always-braces is enforced separately
    by the Checkstyle `NeedBraces` gate (next bullet).
  - Universal whitespace basics (trim, final newline) are native VS Code `files.*` settings (no
    `.editorconfig`).
  - Formatter-engine decision (Eclipse JDT, single shared profile) recorded in
    [open-questions.md](../open-questions.md).
- **Linting, Checkstyle (single-purpose):** `config/checkstyle/checkstyle.xml` (the deliberately
  minimal Gradle/Checkstyle default path) holds exactly one rule, `NeedBraces`; it enforces only
  what the formatter cannot express (formatter wraps `if`/`for`/`while` bodies but cannot *insert*
  `{ }`). Layout stays solely with the formatter; the two tools are disjoint (no rule lives in
  both, avoiding parallel upkeep).
  - Wired via the Gradle `checkstyle` plugin (toolVersion `10.21.0`, `severity=error`) into
    `check`, so `./gradlew build` fails on any braceless control statement (gate proven by a
    deliberate braceless `if`).
  - Same config drives the IDE: the **vscode-checkstyle** extension (`shengchen.vscode-checkstyle`,
    recommended via `.vscode/extensions.json`, auto-installed in the devcontainer) points at it
    (`java.checkstyle.configuration`) for live inline squiggles; the engine version there is the
    extension's own bundle (the single rule is version-stable, so build and IDE need not pin the
    same engine).
  - Checkstyle only *reports*, no auto-fix; add braces via the redhat.java "Add braces" quick-fix.
  - Stays scoped to this one gap, not a general linter (that overlap with the reviewer agent / a
    future Sonar-style static-analysis gate, backlog G6, was why it was earlier declined).
- **Frontend build (`:frontend` Gradle subproject):** a React/TypeScript SPA built with Vite, wired
  into the root build via a `frontendDist` Gradle configuration (`:frontend`'s `build/dist`
  consumed into `processResources` under `static/ui`, so the SPA ships inside the main jar). Node
  is pinned in `gradle.properties` (`nodeVersion`) and downloaded per-machine by the `node-gradle`
  plugin; `npm ci` installs from the lockfile. `npmBuild` feeds `assemble`; `npmTest` (Vitest) and
  `npmLint` (ESLint + Prettier) feed `check`, so `./gradlew build` gates the frontend too. Details,
  structure and conventions: [frontend.md](frontend.md).
- **Coverage, JaCoCo (report-only):** the `jacoco` plugin produces an HTML coverage report at
  `build/reports/jacoco/test/html/index.html`; `test` finalizes `jacocoTestReport`, so every
  `./gradlew build` regenerates it. No threshold is enforced (`jacocoTestCoverageVerification` is
  not wired): the report guides test work without gating a young suite (a threshold would come
  later under G6).
- **Dependency vulnerability scan, Trivy (on-demand):**
  - `./gradlew vulnScan` generates a CycloneDX SBOM of the resolved dependency graph
    (`org.cyclonedx.bom` plugin → `build/reports/cyclonedx/bom.json`) and scans it with **Trivy**
    (`trivy sbom`), printing the CVE report to the console.
  - Also `dependsOn` a `:frontend:vulnScan` task that scans the npm dependency graph directly
    (`trivy fs --scanners vuln --skip-dirs node_modules .`, picking up `package-lock.json`); one
    `./gradlew vulnScan` covers both dependency graphs.
  - Trivy pulls its vulnerability DB from an OCI registry on first run (small, fast, no account or
    API key) and caches it under `~/.cache/trivy`.
  - Non-gating by default: without `vulnScan.failOnSeverity` neither task ever fails. Both stay
    on-demand, not wired into `build`/`check` (gating belongs to CI/delivery, never the local
    build). Continuous alerting is handled by Dependabot.
  - **Tunable params → `gradle.properties`** (committed, central): `vulnScan.failOnSeverity` is a
    comma-separated Trivy severity list (e.g. `HIGH,CRITICAL`); when set, both tasks report only
    those severities and exit non-zero on a finding (a CI/delivery gate, since the scan never runs
    in `build`). Absent/blank = report-only. The scan needs no secrets; the `*.local.*` gitignore
    rule stays in place for future ones.
  - **Toolchain:** the `trivy` binary comes from the devcontainer feature
    `ghcr.io/dhoeric/features/trivy` (present after a container rebuild; any `trivy` on the `PATH`
    works, e.g. from the [official install script](https://trivy.dev/latest/getting-started/installation/)).

## Git hooks (local enforcement)

The repo ships POSIX-sh git hooks in `hooks/`, activated via `core.hooksPath` (hooks are **not**
shared by clone). Ensure they're active with:

```sh
git config core.hooksPath hooks
```

The devcontainer `postCreate` runs this automatically, but it only applies on the **next container
rebuild** (the devcontainer image is built outside the devcontainer; see
[dev-environment.md](dev-environment.md)), so run it once by hand in an existing checkout.

- **`pre-commit`**: the fast quality gate.
  - Runs `./gradlew spotlessCheck checkstyleMain checkstyleTest` (formatting + brace gate).
  - Scans **staged content** for: private-key blocks, AWS-style access keys, credential literals,
    real e-mail addresses, absolute user-home paths, and committed key/keystore files
    (`.pfx`/`.p12`/`.jks`/`.pem`/`.key`).
  - **Under `src/main` only**: rejects hard-coded auth tokens (a literal `?token=…` or a
    `…token = "…"` assignment; tests legitimately use a non-secret test token).
  - Rejects, in committed `.java`, `System.out`/`System.err`, `printStackTrace`, and leftover
    `TODO`/`FIXME`: use the project `Logger`, and track follow-ups in the backlog, not code
    comments.
  - The `hooks/` directory itself is excluded from the scan (the scripts hold the detection
    patterns).
  - Excludes the test run and the full build, to keep the TDD micro-commit loop fast.
- **`commit-msg`**: enforces the §7 message rules: one single line, a present-tense verb first
  (optional leading `[doc] ` tag), and no body / `Co-Authored-By` / tool footer.

`--no-verify` bypasses both; reserve it for genuine emergencies. The hooks are a git-level safety
net complementing the harness-level checks (`.claude/`); the full `./gradlew build` (with tests)
remains the gate for landing on `main`.

## Run via Docker (primary path)

- **`Dockerfile`** is multi-stage:
  - *build stage* `eclipse-temurin:25-jdk`: copies `frontend/` and `gradle.properties` (needed for
    the pinned `nodeVersion`) alongside the Java sources, then runs
    `./gradlew clean check installDist` (not `clean test installDist`: `test` matches nothing
    under `:frontend`, so `check` is what gates the image on the frontend's tests and lint too).
  - *runtime stage* `selenium/standalone-firefox:109.0` (Firefox + geckodriver for the `DOCKER`
    browser mode) with the **JDK 25 copied from the build stage** (Ubuntu base has no
    openjdk-25), the distribution copied to `/opt/protocolParser`, **`COPY zugang.txt /`** (still
    bakes credentials into the image; injecting them instead is deferred, backlog C3),
    `ENTRYPOINT /opt/protocolParser/bin/protocolParser`, `WORKDIR /` so the SQLite path
    `database/temp.sqlite` and `zugang.txt` resolve as before.
  - `.dockerignore` keeps the build context lean: excludes DBs / the gitignored benchmark, plus
    `frontend/node_modules`, `frontend/build` and `frontend/.gradle`.
- **`buildAndRun.bat`** (gitignored, machine-specific): `docker build` → `docker run -p 8080:8080
  -v "<host>/database:/database"`. The container serves on **8080** and persists SQLite to a
  mounted host `database/` dir.
- *Note:* the Docker image and devcontainer are **not built/validated in the devcontainer** (no
  docker-in-docker yet, backlog H2); validate them on the Docker host. The new-stack **app
  behaviour is verified 1:1** by running the distribution against the production DB (see
  testing.md).

## Runtime configuration & secrets

Almost everything is hard-coded in `helper/config/Configuration.java` (⚠️ **not** env-driven yet):

| Setting | Current value | Notes |
|---------|---------------|-------|
| `browser` | `"docker"` | Selenium driver selection (`Browser` enum: FIREFOX/CHROME/EDGE/DOCKER). |
| `server` | `"zyrthania"` | **Target game world.** Scrape URL = `https://evergore.de/<server>?page=…` (`Constants.SERVER`). Switching worlds = change this. |
| `credentials` | `"zugang.txt"` | **Evergore login**: line 1 = username, line 2 = password. Read by `SeleniumPageSource.tryToLogin`. Not in the repo; supplied at image build. |
| `evergoreFolder` | `c:\evergore` | Windows path; unused on the Linux container scrape path. |
| DB path | `database/temp.sqlite` (or `:memory:` if `useInMemory`) | JDBC `jdbc:sqlite:database/temp.sqlite`; under Docker → mounted `/database/temp.sqlite`. |
| Auth token | `evergore.security.api-token`, **required**, env-injected as `EVERGORE_SECURITY_API_TOKEN` (bound by the `SecurityConfiguration` `@ConfigurationProperties` bean) | Every request needs `?token=<configured token>` except `/favicon.ico` + `/health`. **Mandatory at startup**: a blank/unset token makes the app refuse to boot (`ApiTokenStartupValidator` logs an error and throws). No token value lives in the repo. |
| Rate limit | `evergore.rate-limit.*`: `max-requests-per-interval` `5`, `interval` `10s`, `block-duration` `1m` (bound by the `RateLimitConfiguration` `@ConfigurationProperties` record) | Per-client-IP request throttle in `BrowserLoggingFilter` (filter order 1, ahead of the token filter); exceeding the limit within `interval` blocks that IP for `block-duration` → **429** (`TooManyRequests`). Config-driven, no hard-coded constants; the test profile raises the limit so the suite isn't throttled. |

- **`application.yml`** holds Micronaut concerns (app name, Swagger static routes, Netty
  `max-order: 3`) plus the **rate-limit defaults** (`evergore.rate-limit.*`, bound to
  `RateLimitConfiguration`), **no `server.port`** → defaults to **8080**. The one setting bound
  from the *environment* is the **API token**: `evergore.security.api-token` ←
  `EVERGORE_SECURITY_API_TOKEN`, via the `@ConfigurationProperties` bean `SecurityConfiguration`;
  no value lives in the repo.
- **`logback.xml`**: single colored STDOUT appender, root level `verbose` (very chatty).

## HTTP endpoints (all need a valid `?token=…`, except `/favicon.ico` + `/health`)

| Method · Path | Purpose |
|---|---|
| `GET /overview` | HTML table of per-avatar bank metrics + last-updated (from `MetaInformation`). |
| `GET /avatars/{avatar}/bank?page=N` | Paged (100/page) bank entries for one avatar. |
| `GET /avatars/{avatar}/storage?page=N` | Paged storage entries for one avatar. |
| `GET /favicon.ico` | Favicon (token-exempt). |
| `GET /health` | Micronaut management health endpoint: token-exempt, anonymous. Reports UNKNOWN (no run yet) or UP + `lastSuccessfulRun` timestamp; when the last run hit unknown catalog items, the `lastRun` detail also carries `unknownItemCount` and the distinct `unknownItemNames`. Use as a liveness/last-run monitor hook. |
| `/swagger/**`, `/redoc/**`, `/rapidoc/**`, `/swagger-ui/**` | OpenAPI UIs. |

## Scheduled job

- `EvergoreDataCollectorJob`: `@Scheduled(fixedDelay = "24h")`, configurable initial delay
  (`Configuration.getCollectorInitialDelaySeconds()`).
- Runs `EvergoreDataExtractor.loadData()` then `EvergoreDataEvaluator.evaluateData()`, then records
  the completion time in `LastRunStatus` (injected `Clock`).
- Tests set the delay to 0 via a `ZeroDelayConfiguration` subclass.

## CI / dev environment

- **No real CI.** No `.github/workflows/`. `.github/` has only `dependabot.yml` (`devcontainers`
  ecosystem for the root, `npm` ecosystem for `/frontend`, both weekly) and
  `.github/modernize/java-upgrade/` (local Copilot/VS Code "Java upgrade" agent instrumentation:
  hook scripts that log tool use, not a pipeline).
- `.devcontainer/devcontainer.json`: Java dev container (base + `java` feature, **JDK 25**, Maven
  off, Gradle via the wrapper); see [dev-environment.md](dev-environment.md). Dev only.

## Notable runtime risks

- Scraping depends on live evergore.de markup/selectors and a valid login → brittle by nature.
- Bundled `gecko-*-win.exe` drivers are Windows-only and version-pinned (recently upgraded in the
  working tree); the container uses its own Firefox/driver. Consider Selenium Manager / WebDriverManager.
- `verbose` logging + credentials handling deserve a hardening pass before any real deployment.
