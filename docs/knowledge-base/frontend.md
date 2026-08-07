# 14: Frontend

- The dashboard rebuild (Epic E5, decision 2026-07-04) replaces the server-rendered HTML-string
  templates with a JSON API + a React single-page app (SPA).
- This doc covers both the SPA and the JSON API it reads from.

## Stack

- **React 19** + **TypeScript**, built with **Vite** (`frontend/`, its own Gradle subproject).
- **Vitest** + **React Testing Library** for component tests (`npm run test`, jsdom environment).
- **ESLint** (`typescript-eslint`'s `strict` config, `eslint-plugin-import-x`) lints, **Prettier**
  formats. **Spotless stays Java-only** (`src/**/*.java`), not `frontend/`.
- `import-x/no-restricted-paths` enforces the layer boundary below at lint time; the frontend's
  analog of the backend's `HexagonalArchitectureTest` (ArchUnit), via ESLint instead of a JVM test.

## Structure & the dependency rule

Four top-level folders under `frontend/src/`:

| Folder | May import from | Purpose |
|--------|------------------|---------|
| `domain` | (nothing under `src/`) | Framework-free types/logic; the core, like the backend's `domain`/`businessLogic`. |
| `api` | `domain` | Talks to the backend JSON API; translates wire shapes to domain types. |
| `ui` | `domain` | Presentational React components; no direct `api` or `app` imports. |
| `app` | `domain`, `api`, `ui` | The composition root: wires `api` implementations into `ui`, routing, the app shell. |

- Enforced per the table above (no sideways/upward imports); mirrors the backend's inward-only
  rule (`application` depends only inward, never on adapters/config).

## Conventions

- **TDD for TypeScript too:** a component/behavior starts with a failing Vitest test.
- Tests are **deterministic and data-driven** where inputs vary (no wall-clock waits, no logic buried
  in loops); see `testing.md` for the backend rules, which apply in spirit here.
- Components expose a stable **`data-testid`** instead of brittle text/CSS selectors (e.g.
  `App.test.tsx` asserts on `screen.getByTestId('app-title')`).
- Tables render **semantic HTML** (`<table>`/`<thead>`/`<tbody>`/`<th>`/`<td>`), not div-grids,
  for accessibility and testability.

## The ui layer: theme and primitives

- **One stylesheet.** `src/index.css` holds the design tokens (in `:root`) and every component rule.
  Components carry class names only: no inline styles, no per-component stylesheet, so a colour or a
  spacing exists in exactly one place.
- Token groups: `--color-*`, `--font-*`, `--space-*`, plus `--radius`, `--border-width`,
  `--content-width`. The look they encode (dark tavern climate, compact rows, serif display type over
  a sans body) is the 2026-08-07 decision in [open-questions.md](../open-questions.md).
- `theme.test.ts` guards the stylesheet rather than the pixels: every `var()` resolves, every declared
  token is used, no token is declared twice, no literal colour stands outside the token block, and every
  colour-bearing property is painted from a token, so a CSS keyword colour cannot slip past the literal
  check. Together they keep "one place" true as the sheet grows.
- **Primitives.** All presentational: everything arrives as props, none of them knows `fetch` or a
  route. They live in `src/ui/` and are re-exported from `src/ui/index.ts`.

| Component | Renders |
|-----------|---------|
| `StatusPanel` | The `loading` / `empty` / `error` states; `role="alert"` for the error, `role="status"` otherwise. |

- `format.ts` carries the German domain notation: gold with `de-DE` grouping, instants as Berlin
  wall-clock `dd.MM.yyyy HH:mm`. The zone is pinned to `Europe/Berlin` instead of taken from the
  runtime, so a browser in another zone still shows the time the game showed.
- `tsconfig.app.json` lists the `node` types because `theme.test.ts` reads the stylesheet from disk.

## The JSON API the SPA reads

Shape and field names decided 2026-08-04 (open-questions.md). The controllers live in
`rest/controller/api/`, the published contract types in `rest/controller/api/wire/`; the HTML
controllers are untouched legacy.

| Route | Answers |
|-------|---------|
| `GET /api/v1/avatars` | Overview: one `AvatarSummary` (`avatar`, `withdrawn`, `deposited`) per avatar, sorted by name. |
| `GET /api/v1/avatars/{avatar}/bank` | That avatar's bank entries, newest first. |
| `GET /api/v1/avatars/{avatar}/storage` | That avatar's storage entries, newest first. |

- **One envelope for every collection**: `page`, `size`, `totalCount`, `items`. `/api/v1/avatars`
  adds `lastUpdated`; it is the only view that states how fresh the numbers are.
- **Paging**: `?page=` (zero-based, `@Min(0)`) and `?size=` (default 100, `1..1000`); a violation is
  a **400**, not a clamp, so a client bug stays visible. `totalCount` is the unpaged total, so the
  SPA can size its navigation instead of inferring the end from a short page.
- **`lastUpdated: null`** means no collection run has completed. A sentinel instant is not an option:
  Java 25 throws when converting an extreme instant into `java.sql.Timestamp`.
- **`lastUpdated` is display-only and up to an hour off inside the DST fall-back hour.** It is stored
  as a Berlin wall-clock time, so the instant behind it is unrecoverable while the local hour repeats
  and the conversion resolves to the earlier offset. The epoch/UTC storage format fixes this at the
  root; entry timestamps are unaffected because they are real instants.
- **Timestamps** are ISO-8601 UTC and **`transferType`** is one of `DEPOSIT` / `WITHDRAWAL`; the
  client localizes both. The wire names come from `TransferTypeWireNames`, a visitor over the domain
  enum, so the German domain constants (`EINLAGERUNG`/`ENTNAHME`) never reach the contract and stay
  renameable. `toGermanString()` stays with the HTML pages.
- **No field name is derived from a Java identifier.** Every wire record component carries an explicit
  `@JsonProperty`, so renaming a component cannot change the contract, and `RenameSafetyTest` fails
  the build if one is missing. The same rule covers the DB side: every `@DatabaseField` names its
  column and every `@DatabaseTable` its table. There is no JDK-standard annotation for this (JSON
  binding never landed in Java SE), so the Jackson annotation is deliberate and confined to the
  `wire` package.
- **A tokenless deep link answers 401.** Only `/` and `/index.html` are public, so the shell loads
  from there and the client must carry `?token=` across its routes; `spa-data-shell` owns that.
- **404 vs. empty page** (author decision 2026-08-05): a **404 means the avatar is unknown**, i.e. has
  no row in either ledger. A known avatar whose bank or storage ledger happens to be empty answers 200
  with `totalCount: 0` and `items: []`, like the overview does, so the SPA can tell "no storage
  activity" from "no such member" without a second request. A page past the last entry is likewise a
  valid empty window. "Known" means **the avatar has a ledger row somewhere**, deliberately not "the
  meta information mentions it": today the evaluator only writes meta keys for avatars that have rows,
  so the two coincide, and pinning the contract to the ledgers keeps it true if that ever diverges. The
  legacy HTML pages still 404 in that case and keep that quirk until `spa-data-shell` replaces them.
- **Errors carry no envelope**: 401 (missing or wrong token) and 404 answer with an empty body;
  400 (a paging constraint violated) and 405 answer with Micronaut's own JSON error shape. The SPA
  codes against the status, not against a body.
- Both defaults that would silently break this are pinned in `application.yml`
  (`jackson.serialization-inclusion: ALWAYS`, `write-dates-as-timestamps: false`).

## Build integration

- **Node is pinned centrally**: `gradle.properties` → `nodeVersion` (currently `24.18.0`). The
  `com.github.node-gradle.node` plugin downloads that version per machine
  (`node { version = providers.gradleProperty("nodeVersion").get(); download = true }`); no Node
  install required on host or devcontainer (see `dev-environment.md`).
- **`npm ci`** (`npmInstallCommand = "ci"`), not `npm install`, so the build always uses the locked
  `package-lock.json` versions.
- Three Gradle tasks wrap the npm scripts, each with explicit `inputs`/`outputs` for
  skip-when-unchanged (`UP-TO-DATE`/`FROM-CACHE`):
  - `npmBuild` (`vite build` into `frontend/build/dist`); wired into `assemble`.
  - `npmTest` (`vitest run`) and `npmLint` (`eslint` + `prettier --check`); wired into `check`.
- The built SPA reaches the main jar via a **`frontendDist` Gradle configuration**:
  - `:frontend` exposes `build/dist` as a consumable `frontendDist` artifact; the root project
    declares a matching resolvable configuration and copies it into `processResources` under
    `static/ui`.
  - Micronaut serves the SPA as static resources, with a catch-all fallback to `index.html` for
    unknown navigation paths (client-side routing).
  - Existing dashboard URLs (`/overview`, `/avatars/{avatar}/bank`, `/avatars/{avatar}/storage`)
    stay as the SPA's client routes (not renamed).
- **Docker**: the image build copies `frontend/` and `gradle.properties` and runs
  `./gradlew clean check installDist` (not `clean test installDist`): `test` matches nothing under
  `:frontend`, so `check` gates the frontend's tests and lint too. See `build-run-deploy.md`.
- **Vulnerability scanning**:
  - `:frontend` has its own `vulnScan` Exec task (`trivy fs --scanners vuln --skip-dirs node_modules .`,
    picking up `package-lock.json`), honoring the same `vulnScan.failOnSeverity` property as root.
  - The root `vulnScan` `dependsOn` it, so one `./gradlew vulnScan` covers both the JVM (CycloneDX
    SBOM) and npm dependency graphs.
  - Like the root task, it is **not** wired into `build`/`check` (on-demand, CI/delivery-gate territory).
  - Dependabot also watches `frontend/` (`npm` ecosystem) alongside the root `devcontainers` ecosystem.
