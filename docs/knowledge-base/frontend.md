# 14: Frontend

The dashboard rebuild (Epic E5, decision 2026-07-04) replaces the server-rendered HTML-string
templates with a JSON API + a React single-page app (SPA). This doc covers the SPA itself; the JSON
API is a separate, not-yet-built strand (see the backlog's "Current status" section).

## Stack

- **React 19** + **TypeScript**, built with **Vite** (`frontend/`, its own Gradle subproject).
- **Vitest** + **React Testing Library** for component tests (`npm run test`, jsdom environment).
- **ESLint** (`typescript-eslint`'s `strict` config, `eslint-plugin-import-x`) lints, **Prettier**
  formats; both own the frontend. **Spotless stays Java-only** (`src/**/*.java`) and does not touch
  `frontend/`.
- `import-x/no-restricted-paths` enforces the layer boundary below at lint time; it is the frontend's
  analog of the backend's `HexagonalArchitectureTest` (ArchUnit), just enforced by ESLint instead of a
  JVM test.

## Structure & the dependency rule

`frontend/src/` has four top-level folders:

| Folder | May import from | Purpose |
|--------|------------------|---------|
| `domain` | (nothing under `src/`) | Framework-free types/logic; the core, like the backend's `domain`/`businessLogic`. |
| `api` | `domain` | Talks to the backend JSON API; translates wire shapes to domain types. |
| `ui` | `domain` | Presentational React components; no direct `api` or `app` imports. |
| `app` | `domain`, `api`, `ui` | The composition root: wires `api` implementations into `ui`, routing, the app shell. |

`domain` cannot import `api`/`ui`/`app`; `api` and `ui` cannot import each other or `app`. Only `app`
may depend on everything. This mirrors the backend's inward-only rule (`application` depends only
inward, never on adapters/config).

## Conventions

- **TDD for TypeScript too:** a component/behavior starts with a failing Vitest test.
- Tests are **deterministic and data-driven** where inputs vary (no wall-clock waits, no logic buried
  in loops); see `testing.md` for the equivalent backend rules, which apply in spirit here.
- Components expose a stable **`data-testid`** for the elements a test needs to find, instead of
  querying by text/CSS structure that's free to change (e.g. `App.test.tsx` asserts on
  `screen.getByTestId('app-title')`).
- Tables render **semantic HTML** (`<table>`/`<thead>`/`<tbody>`/`<th>`/`<td>`), not div-grids, for
  accessibility and simpler testing.
- Code, comments, and identifiers are English (same rule as the rest of the repo).

## Build integration

- **Node is pinned centrally**: `gradle.properties` → `nodeVersion` (currently `24.18.0`). The
  `com.github.node-gradle.node` plugin downloads exactly that version per machine
  (`node { version = providers.gradleProperty("nodeVersion").get(); download = true }`); no Node
  install is required on the host or in the devcontainer for the build to work (see
  `dev-environment.md`).
- **`npm ci`** (`npmInstallCommand = "ci"`), not `npm install`, so the build always uses the locked
  `package-lock.json` versions.
- Three Gradle tasks wrap the npm scripts, each with explicit `inputs`/`outputs` so Gradle can skip
  them when nothing relevant changed (`UP-TO-DATE`/`FROM-CACHE`):
  - `npmBuild` (`vite build` into `frontend/build/dist`) — wired into `assemble`.
  - `npmTest` (`vitest run`) and `npmLint` (`eslint` + `prettier --check`) — wired into `check`.
- The built SPA reaches the main jar via a **`frontendDist` Gradle configuration**: `:frontend`
  exposes its `build/dist` as a consumable `frontendDist` artifact; the root project declares a
  matching resolvable configuration and copies it into `processResources` under `static/ui`. The
  Micronaut app then serves the SPA as static resources, with a catch-all fallback to `index.html` for
  unknown navigation paths (client-side routing) and the existing dashboard URLs
  (`/overview`, `/avatars/{avatar}/bank`, `/avatars/{avatar}/storage`) staying in place as the SPA's
  client routes rather than being renamed.
- **Docker**: the image build stage copies `frontend/` and `gradle.properties` and runs
  `./gradlew clean check installDist` (not `clean test installDist`): `test` matches nothing under
  `:frontend`, so `check` is what actually gates the image on the frontend's tests and lint, in
  addition to the backend's. See `build-run-deploy.md`.
- **Vulnerability scanning**: `:frontend` has its own `vulnScan` Exec task
  (`trivy fs --scanners vuln --skip-dirs node_modules .`, picking up `package-lock.json`), honoring the
  same optional `vulnScan.failOnSeverity` Gradle property as the root task. The root `vulnScan`
  `dependsOn` it, so one `./gradlew vulnScan` covers both the JVM (CycloneDX SBOM) and npm dependency
  graphs. Like the root task, it is **not** wired into `build`/`check` (on-demand, CI/delivery-gate
  territory). Dependabot also watches `frontend/` (`npm` ecosystem) alongside the root `devcontainers`
  ecosystem.
