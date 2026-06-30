# 05: Build, Run, Deploy

## Build

- **Gradle** (Kotlin DSL, `build.gradle.kts`) via the `io.micronaut.application` plugin; Micronaut
  platform `4.10.3`, **Java 25** (Gradle toolchain, auto-provisioned via the foojay resolver),
  runtime Netty. Main class `…​.Application`. Build and test: `./gradlew build`.
- Key deps: Selenium 4.7.2, ORMLite-JDBC 6.1, sqlite-jdbc 3.41.2.2, commons-text 1.10,
  micronaut-openapi (Swagger/RapiDoc/ReDoc), `micronaut-management` (health endpoint + indicators),
  snakeyaml (Micronaut 4 no longer bundles it). Test:
  micronaut-test-junit5, JUnit 5 (+ `junit-platform-launcher`), unirest-java 3.11.11 (used by
  `SmokeTest`), AssertJ 3.27.7, ArchUnit 1.4.1 (reads Java 25 bytecode).
- The **Gradle wrapper** (`./gradlew`, distribution pinned in `gradle/wrapper/`) is the single build
  entry point; no host toolchain needed beyond a JDK. The deployable is the **application
  distribution** (`./gradlew installDist` → `build/install/protocolParser/bin/protocolParser` + `lib/`),
  not a fat jar.
- **Warnings are errors:** every `JavaCompile` runs `-Xlint:all` + `-Werror`, so any compiler/lint
  warning fails the build. Two categories are excluded deliberately: `-serial` (obsolete
  `serialVersionUID` ceremony) and `-processing` (Micronaut/ORMLite/JUnit/ArchUnit annotations that no
  processor claims, inherent to the stack, not our code). The code is otherwise warning-clean.
- **Java 25 native access:** Netty calls restricted `System::loadLibrary`, which the JVM warns about at
  boot (and will *block* in a future release). `--enable-native-access=ALL-UNNAMED` is set for the test
  JVM and baked into the distribution's start script (`applicationDefaultJvmArgs`), so both tests and the
  Docker runtime start clean and stay forward-compatible.
- **Formatting:** a **single shared Eclipse JDT profile**, `config/eclipse/formatter.xml` (tabs,
  `lineSplit=180`, empty bodies compact, enum constants one-per-line, **method chains wrap one-per-`.`
  when >180**, **no blank before a method's closing `}`**), is the one source of truth, consumed
  by **VS Code** (`java.format.settings.url` + `…profile`), **Eclipse**, **IntelliJ** (Eclipse-formatter
  adapter) and the **Gradle build**. Spotless applies it (`eclipse().configFile(...)`) plus `removeUnusedImports`
  + `importOrder` (java → external → `dev.schoenberg` → static last) + `trimTrailingWhitespace` +
  `endWithNewline`, wired into `check`, so `./gradlew build` fails on any deviation; `./gradlew spotlessApply`
  fixes. VS Code does **format + organize-imports on save** (`.vscode/settings.json`), with **star imports
  forbidden** (`java.sources.organizeImports.starThreshold: 999999` → always explicit; the codebase is now
  wildcard-free). The formatter wraps `if`-bodies but cannot *insert* `{ }`; always-braces is enforced
  separately by the Checkstyle `NeedBraces` gate (next bullet). Universal whitespace basics (trim, final newline)
  are native VS Code `files.*` settings (no `.editorconfig`). See the formatter-engine decision (Eclipse
  JDT, single shared profile) in [open-questions.md](../open-questions.md).
- **Linting, Checkstyle (single-purpose):** a deliberately minimal `config/checkstyle/checkstyle.xml`
  (the Gradle/Checkstyle default path) holds **exactly one rule, `NeedBraces`**; it enforces *only* what the
  formatter cannot express (the formatter wraps `if`/`for`/`while` bodies but cannot *insert* `{ }`). Layout
  stays solely with the formatter; the two tools are **disjoint** (no rule lives in both, which avoids parallel
  upkeep). Wired via the Gradle `checkstyle` plugin (toolVersion `10.21.0`, `severity=error`) into `check`, so
  `./gradlew build` fails on any braceless control statement (gate proven by a deliberate braceless `if`). The
  same config drives the IDE: the **vscode-checkstyle** extension (`shengchen.vscode-checkstyle`, recommended via
  `.vscode/extensions.json` + auto-installed in the devcontainer) points at it (`java.checkstyle.configuration`)
  for live inline squiggles; the engine version there is the extension's own bundle (the single rule is
  version-stable, so build and IDE need not pin the same engine). Checkstyle only *reports*: it has no
  auto-fix; add braces via the redhat.java "Add braces" quick-fix. Checkstyle stays scoped to this one gap and
  is **not** a general linter (that overlap with the reviewer agent / a future Sonar-style static-analysis
  gate, backlog G6, was why it was earlier declined).
- **Coverage, JaCoCo (report-only):** the `jacoco` plugin produces an HTML coverage report at
  `build/reports/jacoco/test/html/index.html`; `test` finalizes `jacocoTestReport`, so every
  `./gradlew build` regenerates it. **No threshold is enforced** (`jacocoTestCoverageVerification` is not
  wired): the report guides test work without gating a young suite (a threshold would come later under G6).
- **Dependency vulnerability scan, OWASP dependency-check (on-demand):** `./gradlew dependencyCheckAnalyze`
  produces a CVE report under `build/reports` from two sources: the **NVD** feed and the **Sonatype OSS
  Index** analyzer (enabled automatically when its token is present). Currently **non-gating**
  (`failOnError = false` and the CVSS threshold defaults to `11`, above the CVSS maximum of 10, so it never
  fails). It stays **on-demand, not wired into `build`**: gating belongs to CI/delivery, never the local
  build (the plugin also treats an NVD-init failure as *fatal* regardless of `failOnError`, which would
  break `build` anywhere lacking the key + feed). Continuous alerting is handled by Dependabot.
  - **Two config files, by design (minimise scattered settings):**
    - **Secrets → one gitignored file `secrets.local.properties`** (repo root, copied from the committed
      `secrets.local.properties.template`) holds *both* scan credentials together: `nvdApiKey=<key>` and
      `sonatypeOssIndexToken=<token>` (optionally `sonatypeOssIndexUsername=<account-email>`); the generic
      name leaves room for future secrets. It matches the `*.local.*` rule, so it can never be
      committed (pre-commit secret-scan is a second net). Each value also falls back to an env var
      (`NVD_API_KEY` / `OSS_INDEX_TOKEN` / `OSS_INDEX_USERNAME`) for CI. Absent → the scan still runs (NVD
      rate-limited; OSS Index analyzer off).
    - **Tunable params → `gradle.properties`** (committed, central): `dependencyCheck.failBuildOnCvss` is the
      one place to set the CVSS fail threshold (lower it toward `0.0` to enforce an up-to-date dependency
      base, a CI/delivery gate, since the scan never runs in `build`).
  - **Set up on a new machine:** `cp secrets.local.properties.template secrets.local.properties`, then fill
    in the values (request an NVD key at <https://nvd.nist.gov/developers/request-an-api-key>, an OSS Index
    token at <https://ossindex.sonatype.org/>); verify with `git check-ignore secrets.local.properties`. The
    first `dependencyCheckAnalyze` downloads the full NVD datafeed (slow, one-time; cached under `~/.gradle`).
    A leaked NVD/OSS-Index key is low-harm (rate-limit only) but is still kept out of the repo: secrets
    never land in a public showcase, no exceptions (handbook §7).

## Git hooks (local enforcement)

The repo ships POSIX-sh git hooks in `hooks/`, activated via `core.hooksPath` (hooks are **not**
shared by clone). Ensure they're active with:

```sh
git config core.hooksPath hooks
```

The devcontainer `postCreate` runs this automatically, but it only applies on the **next container
rebuild** (the devcontainer image is built outside the devcontainer; see [dev-environment.md](dev-environment.md)),
so run it once by hand in an existing checkout.

- **`pre-commit`**: the fast quality gate: `./gradlew spotlessCheck checkstyleMain checkstyleTest`
  (formatting + brace gate), plus a scan of **staged content** for private-key blocks, AWS-style
  access keys, credential literals, real e-mail addresses, absolute user-home paths, and committed
  key/keystore files (`.pfx`/`.p12`/`.jks`/`.pem`/`.key`), and, **under `src/main` only**, hard-coded
  auth tokens (a literal `?token=…` or a `…token = "…"` assignment; tests legitimately use a non-secret
  test token). It also **rejects, in committed `.java`,** `System.out`/`System.err`, `printStackTrace`,
  and leftover `TODO`/`FIXME`: use the project `Logger`, and track follow-ups in the backlog, not code
  comments. The `hooks/` directory is excluded from the scan (the scripts hold the detection patterns
  themselves). **Excludes the test run and the full build** to keep the TDD micro-commit loop fast.
- **`commit-msg`**: enforces the §7 message rules: one single line, a present-tense verb first
  (optional leading `[doc] ` tag), and no body / `Co-Authored-By` / tool footer.

`--no-verify` bypasses both; reserve it for genuine emergencies. The hooks are a git-level safety net
complementing the harness-level checks (`.claude/`); the full `./gradlew build` (with tests) remains
the gate for landing on `main`.

## Run via Docker (primary path)

- **`Dockerfile`** is multi-stage:
  - *build stage* `eclipse-temurin:25-jdk`, runs `./gradlew clean test installDist`.
  - *runtime stage* `selenium/standalone-firefox:109.0` (Firefox + geckodriver for the `DOCKER`
    browser mode) with the **JDK 25 copied from the build stage** (Ubuntu base has no openjdk-25),
    the distribution copied to `/opt/protocolParser`, **`COPY zugang.txt /`** (still bakes credentials
    into the image; injecting them instead is deferred, backlog C3), `ENTRYPOINT /opt/protocolParser/bin/protocolParser`, `WORKDIR /`
    so the SQLite path `database/temp.sqlite` and `zugang.txt` resolve as before. `.dockerignore`
    keeps the build context lean and excludes DBs / the gitignored benchmark.
- **`buildAndRun.bat`** (gitignored, machine-specific): `docker build` → `docker run -p 8080:8080 -v
  "<host>/database:/database"`. The container serves on **8080** and persists SQLite to a mounted host
  `database/` dir.
- *Note:* the Docker image and devcontainer are **not built/validated in the devcontainer** (no
  docker-in-docker yet, backlog H2); validate them on the Docker host. The new-stack **app behaviour
  is verified 1:1** by running the distribution against the production DB (see testing.md).

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
  `RateLimitConfiguration`), **no `server.port`** → defaults to **8080**. The one setting bound from the
  *environment* is the **API token**: `evergore.security.api-token` ← `EVERGORE_SECURITY_API_TOKEN`, via
  the `@ConfigurationProperties` bean `SecurityConfiguration`; no value lives in the repo.
- **`logback.xml`**: single colored STDOUT appender, root level `verbose` (very chatty).

## HTTP endpoints (all need a valid `?token=…`, except `/favicon.ico` + `/health`)

| Method · Path | Purpose |
|---|---|
| `GET /overview` | HTML table of per-avatar bank metrics + last-updated (from `MetaInformation`). |
| `GET /avatars/{avatar}/bank?page=N` | Paged (100/page) bank entries for one avatar. |
| `GET /avatars/{avatar}/storage?page=N` | Paged storage entries for one avatar. |
| `GET /favicon.ico` | Favicon (token-exempt). |
| `GET /health` | Micronaut management health endpoint: token-exempt, anonymous. Reports UNKNOWN (no run yet) or UP + `lastSuccessfulRun` timestamp. Use as a liveness/last-run monitor hook. |
| `/swagger/**`, `/redoc/**`, `/rapidoc/**`, `/swagger-ui/**` | OpenAPI UIs. |

## Scheduled job

`EvergoreDataCollectorJob`: `@Scheduled(fixedDelay = "24h")`, configurable initial delay (`Configuration.getCollectorInitialDelaySeconds()`); runs `EvergoreDataExtractor.loadData()` then `EvergoreDataEvaluator.evaluateData()`, then records the completion time in `LastRunStatus` (injected `Clock`). Tests set the delay to 0 via a `ZeroDelayConfiguration` subclass.

## CI / dev environment

- **No real CI.** No `.github/workflows/`. `.github/` has only `dependabot.yml` (devcontainers
  ecosystem) and `.github/modernize/java-upgrade/` (local Copilot/VS Code "Java upgrade" agent
  instrumentation: hook scripts that log tool use; not a pipeline).
- `.devcontainer/devcontainer.json`: Java dev container (base + `java` feature, **JDK 25**, Maven
  off, Gradle via the wrapper); see [dev-environment.md](dev-environment.md). Dev only.

## Notable runtime risks

- Scraping depends on live evergore.de markup/selectors and a valid login → brittle by nature.
- Bundled `gecko-*-win.exe` drivers are Windows-only and version-pinned (recently upgraded in the
  working tree); the container uses its own Firefox/driver. Consider Selenium Manager / WebDriverManager.
- `verbose` logging + credentials handling deserve a hardening pass before any real deployment.
