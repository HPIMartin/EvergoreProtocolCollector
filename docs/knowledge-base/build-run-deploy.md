# 05 — Build, Run, Deploy

## Build

- **Gradle** (Kotlin DSL, `build.gradle.kts`) via the `io.micronaut.application` plugin; Micronaut
  platform `4.10.3`, **Java 25** (Gradle toolchain, auto-provisioned via the foojay resolver),
  runtime Netty. Main class `…​.Application`. Build & test: `./gradlew build`.
- Key deps: Selenium 4.7.2, ORMLite-JDBC 6.1, sqlite-jdbc 3.41.2.2, commons-text 1.10,
  micronaut-openapi (Swagger/RapiDoc/ReDoc), snakeyaml (Micronaut 4 no longer bundles it). Test:
  micronaut-test-junit5, JUnit 5 (+ `junit-platform-launcher`), unirest-java 3.11.11 (used by
  `SmokeTest`), AssertJ 3.27.7, ArchUnit 1.4.1 (reads Java 25 bytecode).
- The **Gradle wrapper** (`./gradlew`, distribution pinned in `gradle/wrapper/`) is the single build
  entry point — no host toolchain needed beyond a JDK. The deployable is the **application
  distribution** (`./gradlew installDist` → `build/install/protocolParser/bin/protocolParser` + `lib/`),
  not a fat jar.
- **Warnings are errors** (H8): every `JavaCompile` runs `-Xlint:all` + `-Werror`, so any compiler/lint
  warning fails the build. Two categories are excluded deliberately — `-serial` (obsolete
  `serialVersionUID` ceremony) and `-processing` (Micronaut/ORMLite/JUnit/ArchUnit annotations that no
  processor claims — inherent to the stack, not our code). The code is otherwise warning-clean.
- **Java 25 native access:** Netty calls restricted `System::loadLibrary`, which the JVM warns about at
  boot (and will *block* in a future release). `--enable-native-access=ALL-UNNAMED` is set for the test
  JVM and baked into the distribution's start script (`applicationDefaultJvmArgs`), so both tests and the
  Docker runtime start clean and stay forward-compatible.
- **Formatting (A7):** a **single shared Eclipse JDT profile**, `config/eclipse/formatter.xml` (tabs,
  `lineSplit=180`, empty bodies compact, enum constants one-per-line, **method chains wrap one-per-`.`
  when >180**, **no blank before a method's closing `}`**), is the one source of truth — consumed
  by **VS Code** (`java.format.settings.url` + `…profile`), **Eclipse**, **IntelliJ** (Eclipse-formatter
  adapter) and the **Gradle build**. Spotless applies it (`eclipse().configFile(...)`) plus `removeUnusedImports`
  + `importOrder` (java → external → `dev.schoenberg` → static last) + `trimTrailingWhitespace` +
  `endWithNewline`, wired into `check` — so `./gradlew build` fails on any deviation; `./gradlew spotlessApply`
  fixes. VS Code does **format + organize-imports on save** (`.vscode/settings.json`), with **star imports
  forbidden** (`java.sources.organizeImports.starThreshold: 999999` → always explicit; the codebase is now
  wildcard-free). The formatter wraps `if`-bodies but cannot *insert* `{ }`; always-braces is enforced
  separately by the Checkstyle `NeedBraces` gate (see below). Universal whitespace basics (trim, final newline)
  are native VS Code `files.*` settings (no `.editorconfig`). See open-question D-10.

## Run via Docker (primary path)

- **`Dockerfile`** is multi-stage:
  - *build stage* `eclipse-temurin:25-jdk`, runs `./gradlew clean test installDist`.
  - *runtime stage* `selenium/standalone-firefox:109.0` (Firefox + geckodriver for the `DOCKER`
    browser mode) with the **JDK 25 copied from the build stage** (Ubuntu base has no openjdk-25),
    the distribution copied to `/opt/protocolParser`, **`COPY zugang.txt /`** (still bakes credentials
    into the image — C3/C3-deferred), `ENTRYPOINT /opt/protocolParser/bin/protocolParser`, `WORKDIR /`
    so the SQLite path `database/temp.sqlite` and `zugang.txt` resolve as before. `.dockerignore`
    keeps the build context lean and excludes DBs / the gitignored benchmark.
- **`buildAndRun.bat`** (gitignored, machine-specific): `docker build` → `docker run -p 8080:8080 -v
  "<host>/database:/database"`. The container serves on **8080** and persists SQLite to a mounted host
  `database/` dir.
- *Note:* the Docker image and devcontainer are **not built/validated in the devcontainer** (no
  docker-in-docker yet — backlog H2); validate them on the Docker host. The new-stack **app behaviour
  is verified 1:1** by running the distribution against the production DB (see testing.md).

## Runtime configuration & secrets

Almost everything is hard-coded in `helper/config/Configuration.java` (⚠️ **not** env-driven yet):

| Setting | Current value | Notes |
|---------|---------------|-------|
| `browser` | `"docker"` | Selenium driver selection (`Browser` enum: FIREFOX/CHROME/EDGE/DOCKER). |
| `server` | `"zyrthania"` | **Target game world.** Scrape URL = `https://evergore.de/<server>?page=…` (`Constants.SERVER`). Switching worlds = change this. |
| `credentials` | `"zugang.txt"` | **Evergore login** — line 1 = username, line 2 = password. Read by `SeleniumPageSource.tryToLogin`. Not in the repo; supplied at image build. |
| `evergoreFolder` | `c:\evergore` | Windows path; unused on the Linux container scrape path. |
| DB path | `database/temp.sqlite` (or `:memory:` if `useInMemory`) | JDBC `jdbc:sqlite:database/temp.sqlite`; under Docker → mounted `/database/temp.sqlite`. |
| Auth token | `"secret_token"` (hard-coded in `TokenValidationFilter`) | Every request needs `?token=secret_token` except `/favicon.ico`. |

- **`application.yml`** holds only Micronaut concerns (app name, Swagger static routes, Netty
  `max-order: 3`). No domain/runtime settings, **no `server.port`** → defaults to **8080**.
- **`logback.xml`**: single colored STDOUT appender, root level `verbose` (very chatty).

## HTTP endpoints (all need `?token=secret_token`, except favicon)

| Method · Path | Purpose |
|---|---|
| `GET /overview` | HTML table of per-avatar bank metrics + last-updated (from `MetaInformation`). |
| `GET /avatars/{avatar}/bank?page=N` | Paged (100/page) bank entries for one avatar. |
| `GET /avatars/{avatar}/storage?page=N` | Paged storage entries for one avatar. |
| `GET /favicon.ico` | Favicon (token-exempt). |
| `/swagger/**`, `/redoc/**`, `/rapidoc/**`, `/swagger-ui/**` | OpenAPI UIs. |

## Scheduled job

`EvergoreDataCollectorJob` — `@Scheduled(fixedDelay = "24h")`, 30 s initial delay; runs
`EvergoreDataExtractor.loadData()` then `EvergoreDataEvaluator.evaluateData()`. In tests
`DELAY_IN_SEC` is set to 0.

## CI / dev environment

- **No real CI.** No `.github/workflows/`. `.github/` has only `dependabot.yml` (devcontainers
  ecosystem) and `.github/modernize/java-upgrade/` (local Copilot/VS Code "Java upgrade" agent
  instrumentation — hook scripts that log tool use; not a pipeline).
- `.devcontainer/devcontainer.json` — Java dev container (base + `java` feature, **JDK 25**, Maven
  off, Gradle via the wrapper); see [dev-environment.md](dev-environment.md). Dev only.

## Notable runtime risks

- Scraping depends on live evergore.de markup/selectors and a valid login → brittle by nature.
- Bundled `gecko-*-win.exe` drivers are Windows-only and version-pinned (recently upgraded in the
  working tree); the container uses its own Firefox/driver. Consider Selenium Manager / WebDriverManager.
- `verbose` logging + credentials handling deserve a hardening pass before any real deployment.
