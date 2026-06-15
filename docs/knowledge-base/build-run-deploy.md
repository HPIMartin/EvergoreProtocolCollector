# 05 — Build, Run, Deploy

## Build

- **Maven**, parent `io.micronaut:micronaut-parent:3.8.4`, Java 17, runtime Netty.
  Main class `…​.Application`.
- Key deps: Selenium 4.7.2, ORMLite-JDBC 6.1, sqlite-jdbc 3.41.2.2, commons-text 1.10,
  micronaut-openapi (Swagger/RapiDoc/ReDoc). Test: micronaut-test-junit5, JUnit 5,
  unirest-java 3.11.11 (used by `SmokeTest`), AssertJ 3.27.7.
- A `mvnw` exists but there is **no `mvnw.cmd`**; the real build path is Docker (below).
  On Windows, build via local Maven or the container.

## Run via Docker (primary path)

- **`Dockerfile`** is multi-stage:
  - *build stage* `maven:3.8.7-openjdk-18-slim` + `firefox-esr` + `dos2unix`, runs `mvn … package`.
  - *runtime stage* `selenium/standalone-firefox:109.0` + `openjdk-17-jre`, copies the jar to
    `/app.jar`, **`ADD zugang.txt /`** (bakes credentials into the image), `ENTRYPOINT java -jar /app.jar`.
- **`buildAndRun.bat`**: `docker build -t test .` → prune → `docker run -p 8080:8080 -v "<host>/database:/database" test`.
  So the container serves on **8080** and persists SQLite to a mounted host `database/` dir.

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
- `.devcontainer/devcontainer.json` — Java dev container (Java 25 image, Gradle feature,
  docker-outside-of-docker, SonarLint, Claude Code). Dev only.

## Notable runtime risks

- Scraping depends on live evergore.de markup/selectors and a valid login → brittle by nature.
- Bundled `gecko-*-win.exe` drivers are Windows-only and version-pinned (recently upgraded in the
  working tree); the container uses its own Firefox/driver. Consider Selenium Manager / WebDriverManager.
- `verbose` logging + credentials handling deserve a hardening pass before any real deployment.
