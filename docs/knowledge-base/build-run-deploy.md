# 05: Build, Run, Deploy

## Build

- **Tool:** Gradle (Kotlin DSL, `build.gradle.kts`), `io.micronaut.application` plugin, Micronaut
  platform `4.10.3`, **Java 25** (Gradle toolchain, auto-provisioned via the foojay resolver),
  runtime Netty. Main class `…​.Application`. Build and test: `./gradlew build`.
- **Key deps:** Selenium 4.7.2, ORMLite-JDBC 6.1, sqlite-jdbc 3.41.2.2,
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
  needs. The local build cache (`~/.gradle/caches/build-cache-1`) is **shared by every worktree** —
  both compute the same cache key, so entries are portable, not path-bound. Verified 2026-08-01: a
  fresh worktree at an already-built commit builds green in **11s** with `compileJava`,
  `checkstyle*`, `spotless*`, the `:frontend` tasks, `test` and `jacocoTestReport` all `FROM-CACHE`.
  Gradle stores an entry only when a task really *executes*, so a task that has stayed UP-TO-DATE
  since caching was enabled has nothing stored yet: the **first** cold worktree still runs it and
  only the next one hits. `~/.gradle` sits on a named Docker
  volume, so it survives devcontainer rebuilds (see [dev-environment.md](dev-environment.md)).
- **Wrapper integrity:** `gradle-wrapper.properties` carries `distributionSha256Sum` next to
  `distributionUrl`; the wrapper aborts if the downloaded distribution does not match. Bump both
  together on a Gradle upgrade.
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
  - The **Gradle scripts** (`build.gradle.kts`, `settings.gradle.kts`, `frontend/build.gradle.kts`)
    are covered by a `kotlinGradle` block carrying the whitespace basics only:
    `leadingSpacesToTabs()` + `trimTrailingWhitespace()` + `endWithNewline()`. Deliberately *no*
    Kotlin formatter (ktlint/ktfmt): each of them indents with spaces while this codebase is
    tab-indented, and reconciling that needs an `.editorconfig`, which is declined (below).
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
  - The Checkstyle tasks run with an **empty classpath** (`classpath = files()`): the active rules are
    not type-aware, and the default classpath would drag `classes → processResources →
    :frontend:npmBuild` into every `pre-commit` run. The gate itself is unaffected, it still fails on a
    braceless `if`.
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
- **Throwaway probes, `src/probe/java` (gitignored, on-demand):**
  - `./gradlew probe` compiles and runs that source set as a JUnit suite. Its compile and runtime
    classpath is the **test** classpath plus `sourceSets.test.output`, so a probe can use
    `@MicronautTest`, the `@MockBean` boot setup, `LoggerSpy`, `RawHttpClient` and the repository
    stubs exactly as a test does.
  - `probeAnnotationProcessor` extends `testAnnotationProcessor`: without it Micronaut's processor
    never runs over the probe and `@MicronautTest` finds no beans.
  - **Deliberately not wired into `check`/`build`**, and out of the format gates' reach
    (`spotless` `targetExclude`; `checkstyle.sourceSets` drops this one source set through a live
    `matching {}` view, so one added later is gated by default, and `check` never depends on
    `checkstyleProbe`, which would pull `compileProbeJava` in with it): a
    failing, unformatted or uncompilable probe leaves `./gradlew build` green, and `/src/probe/` is
    gitignored, so `git add -A` cannot commit one. That combination is what makes a leftover probe
    harmless hygiene instead of a step in the review gate (decision in
    [open-questions.md](../open-questions.md)).
  - JVM args and the JUnit platform come from the shared `tasks.withType<Test>` block, so `probe`
    boots with `--enable-native-access=ALL-UNNAMED` like `test`; `forkEvery` stays unset here too.
  - `compileProbeJava` drops `-Werror` (it keeps `-Xlint:all`): a probe measuring a deprecated API is
    a normal probe, and the task reaches no `build` graph, so nothing else loosens.
  - The task pins `failOnNoDiscoveredTests` (Gradle 9.5.1 defaults it to `true`; pinned so a default
    flip cannot re-open the hole), so a run that discovers **no** test fails instead of reporting a
    green one nobody ran. It is task-scoped: a `@Test`-less class beside a discovered sibling still
    passes, and an empty `src/probe/java` leaves the task `NO-SOURCE` for Gradle to skip.
  - `./gradlew clearProbes` deletes `src/probe` with everything in it, so a falsifier agent gets rid
    of its own probes without ever running `rm` (the deletion rule keeps no exception, handbook §7);
    nothing under `src/probe/` is reviewed or shipped.
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
  - *build stage* `eclipse-temurin:25-jdk`: copies `frontend/`, `gradle.properties` (needed for
    the pinned `nodeVersion`) and **`config/`** alongside the Java sources, then runs
    `./gradlew clean check installDist` (not `clean test installDist`: `test` matches nothing
    under `:frontend`, so `check` is what gates the image on the frontend's tests and lint too).
    `config/` is not optional: `check` runs Checkstyle and Spotless, which read
    `config/checkstyle/checkstyle.xml` and `config/eclipse/formatter.xml`, and both fail the image
    build with "Unable to create Root Module" / "File signature can only be created for existing
    regular files" when the directory is missing.
  - The build stage has **no Gradle cache**, so every image build downloads the Gradle distribution
    and all dependencies again (measured: ~4 min for the Gradle leg alone). The wrapper makes
    exactly **one** download attempt with a 10 s read timeout, so a slow network fails the build
    with `Downloading …/gradle-9.5.1-bin.zip failed: timeout (10000ms)` / `Attempt 1/1 failed`.
    That is transient — repeat the build.
  - *runtime stage* `selenium/standalone-firefox:109.0` (Firefox + geckodriver for the `DOCKER`
    browser mode) with the **JDK 25 copied from the build stage** (Ubuntu base has no
    openjdk-25), the distribution copied to `/opt/protocolParser`,
    `ENTRYPOINT /opt/protocolParser/bin/protocolParser`, `WORKDIR /` so the SQLite path
    `database/temp.sqlite` resolves against the mounted `/database`. The image holds **no secret**:
    both the API token and the Evergore login arrive as environment variables at `docker run`.
  - `.dockerignore` keeps the build context lean: excludes DBs / the gitignored benchmark, plus
    `frontend/node_modules`, `frontend/build` and `frontend/.gradle`.
- **`buildAndRun.bat`** (gitignored, machine-specific): `docker build` → `docker run -p 8080:8080
  -v "<host>/database:/database"`. The container serves on **8080** and persists SQLite to a
  mounted host `database/` dir. Being gitignored, it cannot carry the tagging rules: the image tag
  and the OCI labels it has to pass are specified in "Versioning & release tags" below.
- *Note:* the production image **can** be built from inside the devcontainer — the
  `docker-outside-of-docker` feature puts a `docker` CLI on the host daemon. The **devcontainer's own
  image** is still built by the host's Dev Containers extension, so `.devcontainer/` changes only
  land on the next rebuild. The new-stack **app behaviour is verified 1:1** by running the
  distribution against the production DB (see testing.md).
- **Build context vs. mount source (the devcontainer trap):** the two are resolved by *different*
  filesystems. The **build context** is read by the `docker` CLI, so `docker build .` inside the
  devcontainer sends the devcontainer's files and works as expected. A **`-v` source** is resolved
  by the **daemon**, which does not know the devcontainer's `/workspaces/...` paths: it silently
  creates a new empty directory there instead of failing, and the app then starts against an **empty
  database** and begins collecting from scratch. So from the devcontainer the mount source must be
  the workspace path **as the Docker host sees it** (a Windows path here), and the pre-flight check
  below is not optional.
- **The runtime does not run as root:** `selenium/standalone-firefox` runs as `seluser`, **uid 1200**.
  The mounted `database/` directory *and* `temp.sqlite` inside it must be writable by that uid, or
  SQLite cannot write. A directory created by another user (the devcontainer's `vscode`, uid 1000,
  writes `755`/`644`) is readable but not writable, which is why the check below writes rather than
  reads.

## Versioning & release tags

- **One number in three places:** `version` in `build.gradle.kts` is the source, the git tag is
  `v<version>`, the image tag is `evergore-protocol-collector:<version>`. So a running container's
  image tag names its source commit: `git checkout v<that tag>`.
- **The image name is lowercase and therefore not `rootProject.name`:** a Docker repository name may
  not contain uppercase letters (`invalid tag "protocolParser:…": repository name must be
  lowercase`), so the image is named `evergore-protocol-collector` after the repository, while the
  Gradle project and the distribution keep `protocolParser` and the container on the home server
  runs as `epc`.
- **SemVer, no `-SNAPSHOT`:** nothing is published to an artifact repository, so a snapshot suffix
  would gate nothing. `version` holds the number of the **next** release, and the commit that sets
  that number only *declares* it. One version commit per release.
- **The tagged commit is the released state, not the version commit:** the tag goes on the **tip of
  `main` at release time** — the stand that passed the release gate (green
  `clean build --no-build-cache`, buildable image, 1:1 check) and whose image is deployed. The
  version commit may sit far behind that tip and carry none of it. Tagging it instead would name a
  stand nobody built or ran (the `0.1.0` case: the version commit's image build was broken and its
  credential handling superseded).
- **Consequence — a build off an untagged `main` commit is not identified by its version alone**
  (between releases `version` still names the release being prepared). Such a build is tagged
  `evergore-protocol-collector:<version>-<short sha>`; a **bare**
  `evergore-protocol-collector:<version>` tag is reserved for the tagged release commit, so it
  always means exactly one source state.
- **Tagging is the author's act at release time, on the released tip** (agents document it, never
  run it):

  ```sh
  git tag -a v0.1.0 -m "Release 0.1.0"
  git push origin v0.1.0      # a push is the author's decision alone, tags included
  ```

  Annotated, not lightweight, so the tag carries its own author and date.
- **The image build bakes version and commit in as OCI labels.** `buildAndRun.bat` is gitignored and
  machine-specific, so this is the contract it implements:

  ```sh
  docker build -t evergore-protocol-collector:0.1.0 \
    --label org.opencontainers.image.version=0.1.0 \
    --label org.opencontainers.image.revision=$(git rev-parse HEAD) .
  ```

  The labels answer "which stand runs" even for an interim image, where the tag alone cannot. The
  base image contributes an empty `authors` label of its own; ignore it.
- **Never delete the previous release's image** on the home server — it *is* the rollback target.
  `docker image prune -a` and `docker rmi` on the old tag remove the ability to roll back.

## Which stand is running?

```sh
docker inspect --type container -f '{{.Config.Image}}' epc
docker inspect --type container -f '{{index .Config.Labels "org.opencontainers.image.version"}} {{index .Config.Labels "org.opencontainers.image.revision"}}' epc
docker images evergore-protocol-collector    # which tags are available to roll back to
```

- The container runs under the fixed name **`epc`** on the home server (see the deploy steps), so
  every command here and in the rollback needs no container id. The image keeps the long name; only
  the container is short.
- **`--type container` is not decoration.** `docker inspect` resolves a name against containers
  **first and images second**, and the home server has an *image* named after the service too. So a
  typo'd or renamed container does not fail: the command answers about the image and looks like a
  valid reply. The tells are an **empty** `.Config.Image` and an `.Id` carrying a `sha256:` prefix —
  a container's `.Id` is bare hex. Cost a snapshot in the `0.1.0` deploy: the `stop` that should have
  preceded it failed with `No such container` while the inspect before it had "answered".
- `.Config.Image` is the reference the container was **started with** (a name, possibly untagged);
  `.Image` is the resolved image **ID**. Read the first to learn what it claims to run, the second
  when you need something `docker tag` can take.
- The `revision` label is the authoritative answer: it is a commit sha and survives any tag
  confusion. An image built before the labels existed reports empty labels — that alone dates it as
  pre-`0.1.0`.
- On a host where the invoking user is not in the `docker` group, every command in this file needs
  `sudo`.

## Deploy to the home server

Runs against the Docker host daemon — from a host shell or from the devcontainer, whose `docker`
CLI targets that same daemon. Steps 1–3 must be done **before** the running container is replaced.

1. **Back up the live database first.** Evergore serves only the last 30 days of logs, so
   `database/temp.sqlite` is the only history. A first run recomputes the meta sums from all stored
   entries and ingests still-visible entries missing from the database, both in place and not
   reversible: `cp database/temp.sqlite database/temp.sqlite.bak-<yyyymmdd>`.
   - Copy it with the container **stopped**, and prove the stop rather than assuming it: `docker stop`
     must echo the name, and `docker ps --filter name=epc` must come back empty. A `No such container`
     is easy to read past, and the copy then runs against a live database.
   - One file is enough: the database runs in rollback-journal mode, so no `-wal`/`-shm` sidecar
     outlives a write and there is no second file to keep consistent with it.
2. **Secure the rollback target:** confirm the currently running image carries a tag you can start
   again (`docker inspect --type container -f '{{.Config.Image}}' epc`). If it is untagged,
   `<none>`, or a tag the next build overwrites, tag it now — the tag goes into the **repository the
   image already has**, not the one the next release uses:

   ```sh
   docker tag "$(docker inspect --type container -f '{{.Image}}' epc)" <its repository>:pre-0.1.0
   ```

   An image the build orphans is still startable by id, but nothing left on the host says what it
   was. **This is the situation at the `0.1.0` release**, whose predecessor was built before the
   tagging scheme existed: it runs as the bare, untagged `evergore_protocol_collector`
   (**underscores** — the pre-`0.1.0` name), so its rollback tag is
   `evergore_protocol_collector:pre-0.1.0` while the release image is
   `evergore-protocol-collector:0.1.0`. The two names sit in different repositories, which is what
   keeps the new build from overwriting the rollback target.
3. **Check the mount before starting anything** — both failures below are silent, and both leave a
   *running, healthy-looking* container serving wrong data:

   ```sh
   docker run --rm --entrypoint /bin/bash -v "<host>/database:/database" \
     evergore-protocol-collector:<version> -c 'ls -l /database; touch /database/.probe && \
     echo DIR-OK && rm /database/.probe; : >> /database/temp.sqlite && echo FILE-OK'
   ```

   - `temp.sqlite` must be **listed**. An empty listing means the mount source did not resolve (see
     the devcontainer trap above) and the app would start on an empty database.
   - `DIR-OK` and `FILE-OK` must both appear. Without them the runtime user (`seluser`, uid 1200)
     cannot write and SQLite fails; fix the mode on the host (`chmod 777 database`,
     `chmod 666 database/temp.sqlite`) rather than starting the app to find out.
   - **The redirection is `>>`, never `>`.** Both prove the same write permission, but `: >` opens
     the file with `O_TRUNC` and empties the live database **while printing `FILE-OK`** — the check
     would destroy exactly what it is run to protect, and the printed line would report success.
     Measured 2026-08-16 (23 bytes → 0); the version of this step carrying `>` was never run against
     a populated database. `: >>` opens for append and writes nothing: size and mtime stay put.
4. **Build the image** on the Docker host (`buildAndRun.bat`, gitignored and machine-specific) with
   the tag and labels from "Versioning & release tags". The build context carries **no credentials**;
   the image is secret-free and the same image runs with any account.
5. **Replace the container:**

   ```sh
   docker stop epc && docker rm epc
   docker run -d --name epc -p 80:8080 \
     -e EVERGORE_SECURITY_API_TOKEN=<token> \
     -e EVERGORE_CREDENTIALS_USERNAME=<evergore login> \
     -e EVERGORE_CREDENTIALS_PASSWORD=<evergore password> -e TZ=UTC \
     -v "<host>/database:/database" evergore-protocol-collector:0.1.0
   ```

   - The token is **mandatory**: a blank or unset value makes the app refuse to boot
     (`ApiTokenStartupValidator`). Keeping the value stable keeps existing bookmark URLs valid — but
     stability is not a reason to keep a **known** value. The pre-`0.1.0` stand ran with
     `secret_token`, the literal placeholder its own paging script shipped inside every detail page,
     on an internet-reachable instance; it was rotated with this release (`openssl rand -hex 24`).
     A token that appears in a served page is public, and the bookmarks are the cheaper loss.
   - The **Evergore login is mandatory too**, and for the same reason: either variable unset or
     blank and the app refuses to boot (`CredentialsStartupValidator`), instead of starting healthy
     and scraping logged-out 30 seconds later. Neither value has a default; both must be set
     explicitly, and they are the credentials of the game account the scraper signs in with.
   - The three secrets are the **only** thing separating an image from a running stand. They are
     visible in `docker inspect` and in the shell history of this command, which is accepted here
     (single-admin home server); nothing writes them to the log.
   - `TZ=UTC` keeps the runtime off a DST zone while timestamps persist as default-timezone
     wall-clock text (backlog D14). The container default is already UTC; setting it explicitly
     pins it.
   - The fixed `--name` is what makes every command in "Which stand is running?" and in the
     rollback runnable as written.
   - **The published port is the target machine's choice, and it is not `8080` here.** The home
     server maps **`-p 80:8080`** (the container always serves on 8080 inside), and the router
     forwards the external port onto that 80. Read it off the running container **before** replacing
     it — `docker inspect --type container -f '{{json .HostConfig.PortBindings}}' epc` — because a
     wrong host port yields a healthy container that every existing URL misses. The same inspection
     answers the **restart policy** (`{{.HostConfig.RestartPolicy.Name}}`), which this command does
     not set: the home server runs with `no`, so the service does not come back by itself after a
     host reboot.
   - **Timeline** (measured 2026-08-16 on both machines): the server answers after ~1 s, the first
     collection starts 30 s after startup (`getCollectorInitialDelaySeconds`), and extraction plus
     evaluation together took **16 s** on the work machine and **42 s** on the home server. So
     `/health` turned `UP` **46 s** and **71 s** after container start — the older "~2.5 minutes"
     is a conservative upper bound, not the expected value. `UNKNOWN` before that is the documented
     state, not a failure; past ~3 minutes, read the log instead of waiting.
6. **Verify**, in order — `<token>` is the same value passed in step 5:

   ```sh
   curl -s -o /dev/null -w '%{http_code}\n' http://<host>/health          # 200
   curl -s -o /dev/null -w '%{http_code}\n' http://<host>/                # 200, no token
   curl -s -o /dev/null -w '%{http_code}\n' "http://<host>/api/v1/avatars?token=<token>"
   curl -s -o /dev/null -w '%{http_code}\n' http://<host>/api/v1/avatars  # 401
   ```

   - `/health` is anonymous. It reports `UNKNOWN` until the first collection finishes, then `UP`;
     the run's timestamp sits at **`details.lastRun.details.lastSuccessfulRun`**, next to
     `unknownItemCount` and `unknownItemNames`. Neither is an error: they are the catalog gap
     (testing.md). **`unknownItemCount` counts occurrences, `unknownItemNames` distinct names**, so
     the two differ by an order of magnitude — measured on the production snapshot 2026-08-16:
     **1674 occurrences over 123 names**, and the 1674 matches the snapshot's own row count for
     those names exactly. A four-digit count is normal at this volume.
   - `/` serves the SPA shell without a token (~480 bytes, `text/html`, carrying `<div id="root">`
     and the bundle `<script>`); the SPA then fetches the API with the token from its URL.
   - `/api/v1/avatars?token=…` answers `{lastUpdated, page, size, totalCount, items[]}` with
     `{avatar, deposited, withdrawn}` per item. Check `lastUpdated` and `totalCount` against what
     the previous stand served — that is the cheapest proof the mounted database is the intended
     one and not an empty new file.
   - **Rate limit:** 30 requests per 10 s per client IP, then a 1-minute block. Nothing is exempt,
     so the four checks above cost **four** counted requests and fit in one window. A renewed burst
     while blocked pushes the block out again, so back off after a 429 instead of retrying.
7. **Rollback** — the previous release's tag (`docker images evergore-protocol-collector` lists the
   candidates; the pre-`0.1.0` predecessor lists under `evergore_protocol_collector`, see step 2):

   ```sh
   docker stop epc && docker rm epc
   cp database/temp.sqlite.bak-<yyyymmdd> database/temp.sqlite
   docker run -d --name epc -p 80:8080 \
     -e EVERGORE_SECURITY_API_TOKEN=<token> \
     -e EVERGORE_CREDENTIALS_USERNAME=<evergore login> \
     -e EVERGORE_CREDENTIALS_PASSWORD=<evergore password> -e TZ=UTC \
     -v "<host>/database:/database" evergore-protocol-collector:<previous version>
   ```

   - **An image built before the credentials moved out** carries `zugang.txt` inside and ignores the
     two variables, so passing them to such a rollback target is harmless but pointless. Rolling
     *forward* again without them fails at startup, which is the intended noise.

   - Restore the backup **before** starting the old image: the new version may have written entries
     or meta sums the old one does not expect, and that write is not reversible.
   - `cp` onto an existing `temp.sqlite` keeps that file's mode, but a `cp` that *creates* the file
     takes the backup's mode, which can be non-writable for uid 1200 again — re-run the step 3 check
     after restoring.
   - The restored state is only observable in the **first 30 seconds**: the collection then runs
     again and writes the restored database forward. Verify `lastUpdated` right after startup.

A scrape failure is contained: Micronaut's task exception handler logs it, the app keeps serving,
and the database is left untouched, so a broken scrape degrades to stale data rather than downtime.

**Database compatibility:** the `bankEntries` / `storageEntries` columns and the `last_updated`,
`bank_placement_<avatar>` and `bank_withdrawl_<avatar>` meta keys are stable, and the
`storage_placement_<avatar>` / `storage_withdrawl_<avatar>` keys are additive, so deploying needs no
schema migration (a migration framework is backlog D10).

## Runtime configuration & secrets

Almost everything is hard-coded in `helper/config/Configuration.java` (⚠️ **not** env-driven yet):

| Setting | Current value | Notes |
|---------|---------------|-------|
| `browser` | `"docker"` | Selenium driver selection (`Browser` enum: FIREFOX/CHROME/EDGE/DOCKER). |
| `server` | `"zyrthania"` | **Target game world.** Scrape URL = `https://evergore.de/<server>?page=…` (`Constants.SERVER`). Switching worlds = change this. |
| Evergore login | `evergore.credentials.username` / `.password`, **both required**, env-injected as `EVERGORE_CREDENTIALS_USERNAME` / `EVERGORE_CREDENTIALS_PASSWORD` (bound by the `CredentialsConfiguration` `@ConfigurationProperties` record) | The game account `SeleniumPageSource.tryToLogin` signs in with. **Mandatory at startup**: either value unset or blank and the app refuses to boot (`CredentialsStartupValidator` logs an error naming the variable and throws), so a missing login cannot degrade into a silent logged-out scrape. No value lives in the repo or in the image. |
| `evergoreFolder` | `c:\evergore` | Windows path; unused on the Linux container scrape path. |
| DB path | `database/temp.sqlite` (or `:memory:` if `useInMemory`) | JDBC `jdbc:sqlite:database/temp.sqlite`; under Docker → mounted `/database/temp.sqlite`. |
| Auth token | `evergore.security.api-token`, **required**, env-injected as `EVERGORE_SECURITY_API_TOKEN` (bound by the `SecurityConfiguration` `@ConfigurationProperties` bean) | **Every** request needs `?token=<configured token>` except the configured public paths below. **Mandatory at startup**: a blank/unset token makes the app refuse to boot (`ApiTokenStartupValidator` logs an error and throws). No token value lives in the repo. |
| Public paths | `evergore.security.public-paths` in `application.yml`: `/`, `/index.html`, `/assets/**`, `/favicon.ico`, `/health`, `/swagger/**`, `/swagger-ui/**`, `/redoc/**`, `/rapidoc/**` (same `SecurityConfiguration` bean) | The **only** token-free surface; Ant patterns, matched against the canonicalized path by `PublicPaths`. Everything not listed needs a token, so a new controller is protected by default. Empty or unset ⇒ everything is protected (fail closed), which makes a misconfiguration a visible 401 on `/` rather than a silent hole. |
| Rate limit | `evergore.rate-limit.*`: `max-requests-per-interval` `30`, `interval` `10s`, `block-duration` `1m`, `max-tracked-clients` `10000` (bound by the `RateLimitConfiguration` `@ConfigurationProperties` record) | Per-client-IP request throttle in `RateLimitFilter` (filter order 2, behind the audit log and ahead of the token filter); exceeding the limit within `interval` blocks that IP for `block-duration` → **429** (`TooManyRequests`). Applies to **every** path. The limit carries a full page load (shell + bundle + favicon + API call ≈ 5 requests) several times over; below ~10 the SPA would throttle itself. `max-tracked-clients` bounds the counter map (see `RateLimitCounters` below). `RateLimitStartupValidator` refuses to boot on any value that would silently disable the throttle: a request budget or client budget below `1`, or a non-positive `interval`/`block-duration`. Config-driven, no hard-coded constants; the test profile raises the limit so the suite isn't throttled. |

- **Environment YAMLs hold overrides only** (author decision 2026-08-15). Micronaut merges the
  property sources, so `application-<env>.yml` is an overlay on `application.yml`, not a replacement:
  a key it repeats with the same value is duplication that silently rots when the base changes.
  `application-test.yml` therefore carries the test token and the raised request budget,
  `application-ratelimit.yml` the lowered one, and nothing else.
- **`application.yml`** holds Micronaut concerns (app name, Swagger static routes, Netty
  `max-order: 3`) plus the **rate-limit defaults** (`evergore.rate-limit.*`, bound to
  `RateLimitConfiguration`) and the **public-path list** (`evergore.security.public-paths`, next to
  the static-resource mappings it mirrors), **no `server.port`** → defaults to **8080**. The settings bound
  from the *environment* are the three secrets: the **API token** (`evergore.security.api-token` ←
  `EVERGORE_SECURITY_API_TOKEN`, via `SecurityConfiguration`) and the **Evergore login**
  (`evergore.credentials.username` / `.password` ← `EVERGORE_CREDENTIALS_USERNAME` /
  `EVERGORE_CREDENTIALS_PASSWORD`, via `CredentialsConfiguration`). None of them has a value in
  `application.yml`, so none lives in the repo; `application-test.yml` carries dummy values because
  every boot test would otherwise fail the startup validation.
- **`logback.xml`**: single colored STDOUT appender, root level `info`. Nothing logs a request URI's
  query string, so the `?token=…` credential never reaches the log: `RequestAuditLogFilter` logs only
  client IP and user-agent, and the exception handler logs `request.getPath()`, which excludes the
  query (pinned by `ApplicationExceptionHandlerTest`). An expected client error (401, 404, 429) is
  one `info` line; only a server error logs at `error` with its stack trace, so probing the token
  cannot bury real errors in traces.

## HTTP endpoints

The token scope is **default-deny** (author decision 2026-08-04): a valid `?token=…` is required
everywhere except the paths configured under `evergore.security.public-paths`. The decision is made
on the **canonicalized** path (`PathCanonicalizer`), so `/assets/../overview` is protected like
`/overview`. Consequence for the SPA: a deep link opened without a token answers **401**, the shell
loads from `/` and the client carries `?token=` across its routes.

**Public means token-free, nothing else** (author decision 2026-08-14, superseding the static-surface
exemption of 2026-08-04): the rate limit and the audit log apply to every path, so
`evergore.security.public-paths` is the *only* list of paths with a special status. There is no second,
code-side list that could drift out of sync with it.

The per-IP counter map is bounded by `RateLimitCounters`: a client whose `interval` elapsed and that is
not blocked is forgotten (its counter would reset anyway, so nothing is lost), and once
`max-tracked-clients` is reached the least recently seen client is dropped. A running block therefore
survives normally and yields only to the configured bound, which takes that many distinct IPs to
reach. Counters never leave the map, and `RateLimitFilter` asks a single question per request
(`blocks(clientIp)`), which counts, blocks and answers under one lock: an offender cannot be evicted
between exceeding its budget and being blocked, and no request lands on an already-evicted counter.

A **malformed request target** (an invalid percent-escape such as `/overview%zz`) reaches the filters
and is answered **400** only afterwards: the invalid escape breaks the request URI, so no path-based
decision can be made and the token filter never gets to answer 401. Neither the audit log nor the rate
limiter reads the path, which is why both still see the request. That it is **counted** is measured
(2026-08-14) and pinned by `RateLimitFilterTest`; that it is **logged** follows from filter order (the
audit filter sits at order 1, ahead of the counter that demonstrably runs) plus the audit line being
built from client IP and user-agent only, pinned literally by `RequestAuditLogFilterTest`. A framework
upgrade that answers such a target ahead of the filter chain turns that test red.

What the filters really cannot see is a request the **server** answers on its own: an oversized request
target (5000 characters) is answered **413** and never reaches them, measured 2026-08-14 by three such
requests in a row not earning a 429. That gap needs a Netty-level seam and is tracked as backlog
**C10**; `RateLimitFilterTest` pins the current behaviour so an upgrade that changes it shows up.

| Method · Path | Purpose |
|---|---|
| `GET /api/v1/avatars` | JSON overview: per-avatar bank totals + `lastUpdated`. Contract in [frontend.md](frontend.md). |
| `GET /api/v1/avatars/{avatar}/bank?page=N&size=M` | JSON bank entries for one avatar, newest first. |
| `GET /api/v1/avatars/{avatar}/storage?page=N&size=M` | JSON storage entries for one avatar, newest first. |
| `GET /overview`, `/avatars/{avatar}/bank`, `/avatars/{avatar}/storage` | SPA client routes. No controller owns them: with a token they fall through to the shell, so a deep link or a bookmark works. |
| `GET /`, `/index.html`, `/assets/**` | The SPA shell and its bundle. **Public** (no token), but rate-limited and logged like any other request. An unknown navigation path **with a token** falls back to the shell; a missing asset and an unknown `/api` path keep their 404. |
| `GET /favicon.ico` | Favicon: public, but rate-limited and logged like any other request. |
| `GET /health` | Micronaut management health endpoint: token-exempt, anonymous. Reports UNKNOWN (no run yet) or UP + `lastSuccessfulRun` timestamp; when the last run hit unknown catalog items, the `lastRun` detail also carries `unknownItemCount` and the distinct `unknownItemNames`. Use as a liveness/last-run monitor hook. |
| `/swagger/**`, `/redoc/**`, `/rapidoc/**`, `/swagger-ui/**` | OpenAPI UIs: public, but rate-limited and logged. |

## Scheduled job

- `EvergoreDataCollectorJob`: `@Scheduled(fixedDelay = "24h")`, configurable initial delay
  (`Configuration.getCollectorInitialDelaySeconds()`).
- Runs `EvergoreDataExtractor.loadData()` then `EvergoreDataEvaluator.evaluateData()`, then records
  the completion time in `LastRunStatus` (injected `Clock`).
- Tests set the delay to 0 via a `ZeroDelayConfiguration` subclass.

## CI / dev environment

- **No real CI.** No `.github/workflows/`. `.github/` has only `dependabot.yml` (`devcontainers`
  ecosystem for the root, `docker` for `/.devcontainer` — the features lock does not cover the
  base image — and `npm` for `/frontend`, all weekly) and
  `.github/modernize/java-upgrade/` (local Copilot/VS Code "Java upgrade" agent instrumentation:
  hook scripts that log tool use, not a pipeline).
- `.devcontainer/`: Java dev container (digest-pinned bookworm base + apt tools, `java` feature with
  **JDK 25**, Maven off, Gradle via the wrapper); see [dev-environment.md](dev-environment.md).
  Dev only.

## Notable runtime risks

- Scraping depends on live evergore.de markup/selectors and a valid login → brittle by nature.
- Bundled `gecko-*-win.exe` drivers are Windows-only and version-pinned (recently upgraded in the
  working tree); the container uses its own Firefox/driver. Consider Selenium Manager / WebDriverManager.
- A login that is *present but wrong* (typo, rotated password) still passes the startup check, which
  only tests for a value. It then fails at scrape time: the login wait times out, `tryToLogin` logs
  one `warn` naming the login, and the outer wait fails the run with `Failed to scrape Evergore`.
