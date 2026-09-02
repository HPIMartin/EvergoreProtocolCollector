# 14: Frontend

- The dashboard rebuild (decision 2026-07-04) replaces the server-rendered HTML-string
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
- Each folder has an `index.ts` as its **public surface**; cross-layer imports go through it, inside
  a layer modules import each other directly.
- Responsibilities, so a reader knows which folder to open:
  - `domain`: the wire contract's concepts as types, plus the framework-free logic over them (the
    `Ledger` visitor, the German and Berlin-local wording).
  - `api`: the `ProtocolApi` port and its `fetch` adapter, which validates every wire body before
    translating it into domain types; `HttpGet` is the seam the tests fake.
  - `ui`: the theme and the presentational primitives; props in, callbacks out, no knowledge of
    routes or HTTP.
  - `app`: the composition root: routing, each view's load state, and the shell that wires the
    adapter into the views.
- **`Ledger<E>` and `Route` are visitors** (an `accept` taking one object with a method per case),
  the TypeScript form of the backend's `TransferTypeVisitor`: a consumer cannot forget a case, and
  there is no tag to switch on with a `default` that swallows the next one.

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
| `PageFrame` | Banner with brand and navigation (`aria-current="page"` marks the current link), `<main>` for the view. |
| `Link` | An `<a href>` that reports a **plain** click to its `onFollow` and leaves a modified or middle click to the browser, so in-app navigation costs no reload while bookmarking and open-in-new-tab keep working. `PageFrame` and a `link` column render through it. |
| `SortableTable<Row>` | Semantic `<table>`; a column is `text`, `number`, `timestamp` or `link`; a header click sorts, a second click reverses. |
| `StatusPanel` | The `loading` / `empty` / `error` states; `role="alert"` for the error, `role="status"` otherwise. |

- `format.ts` carries the German domain notation: gold with `de-DE` grouping, instants as Berlin
  wall-clock `dd.MM.yyyy HH:mm`. The zone is pinned to `Europe/Berlin` instead of taken from the
  runtime, so a browser in another zone still shows the time the game showed.
- **Sorting**: text by German collation (`Intl.Collator('de-DE')`, so `Ärger` sorts under `A`), numbers
  numerically, timestamps by instant (an offset other than `Z` still lands in the right place). Missing
  values sort last in **both** directions, equal keys keep their given order, and a table without
  `initialSort` renders the order it was handed, which is the API's newest-first.
- **Tone**: a number column declares itself `credit`, `debit` or `neutral`; a negative value is always
  `debit` and a zero always `neutral`, so "nothing moved" stays uncoloured.
- An `initialSort` naming a column the table does not have **throws**, for the reason the API answers
  400 instead of clamping a bad page size: a client bug stays visible.
- **The gallery**: `gallery.html` plus `src/ui/gallery/` shows every primitive with fixture rows modeled
  on the guild sheet's columns ([google-sheet.md](google-sheet.md)). `npm run dev` serves it at
  `/gallery.html`; `vite build` ignores it, because `index.html` is the only build input, so it never
  reaches the jar. It replaces a Storybook dependency: pure props-in components need no second toolchain.
- `tsconfig.app.json` lists the `node` types because `theme.test.ts` reads the stylesheet from disk.

## The SPA's views

- **Client routes:** `/` and `/overview` show the guild overview, `/avatars/{avatar}/bank` and
  `/avatars/{avatar}/storage` show one avatar's ledger. Every other path renders "no view", so a typo
  in a bookmark says so instead of showing an empty page.
- The avatar segment is percent-encoded when a link is built and decoded when a path is read; a
  malformed escape is "no view", not a crash.
- **Every view passes through one of four outcomes** (`useLoad` + `LoadedView`): loading, loaded,
  token refused, failed with a reason. A ledger view additionally tells **"known avatar, no
  entries"** from **"unknown avatar"**, which is the client side of the 404 decision below.
- **The token is read once from the address** (`?token=`) and carried into every request and every
  in-app link; it is kept nowhere else (no cookie, no `localStorage`), so a link is the whole
  credential and closing the tab ends the session.
- **Ledger paging is server-side and bookmarkable.** A ledger view fetches `windowOf(page)`
  (`?page=&size=100`) and shows `items.length` of `totalCount`; the page number round-trips through
  the address (`route.ts`'s `PAGE` param), so `Pagination` (`ui/Pagination.tsx`) can build "Zurück"/
  "Weiter" links from it via `hrefOf`. A page past the end is not a failure: the API answers 200 with
  `items: []`, rendered as the existing empty state. An invalid page (negative or non-numeric) is
  passed through **unclamped**, so the API's `@Min(0)` violation answers 400 and surfaces as the
  existing generic failure, because clamping it client-side would hide a bad link instead of
  showing it.
- **The views own their columns, the primitives own the rendering.** A view declares its
  `Column` list and hands `SortableTable` the domain rows; timestamps go in as ISO strings, which is
  what the column kind reads, and `format.ts` is the one place that turns them into Berlin
  wall-clock. Transfer types are shown as `Einlagerung`/`Entnahme` from the domain, and the headers
  are German, like the sheet's.
- **Navigation lives in the frame and in one table column.** `PageFrame` carries "Übersicht" plus,
  on a ledger, that avatar's "Bank" and "Lager"; the overview's avatar column links into the bank and
  a "Lager" column into the storage. Both go through `Link`, so the shell is never reloaded.
- **Tests reach no network.** The faked seam is `HttpGet`, answering a real `Response`, so status
  handling and URL building are exercised for real. Asynchronous assertions flush microtasks with
  `act`; no test uses a timer, a `waitFor` poll or a wall-clock wait.
- **The one test that does run the real thing** is on the Java side: `DashboardBrowserSmokeTest`
  drives headless Firefox against the booted server and reads the painted rows back through the
  `data-testid` hooks, so "the bundle reaches the API and shows its data" is proven somewhere
  (`testing.md`). It is the reason the hooks are a contract, not a convenience.

## The JSON API the SPA reads

Shape and field names decided 2026-08-04 (open-questions.md). The controllers live in
`rest/controller/api/`, the published contract types in `rest/controller/api/wire/`; it is the
service's only read surface.

| Route | Answers |
|-------|---------|
| `GET /api/v1/avatars` | Overview: one `AvatarSummary` (`avatar`, the four ledger sums `bankWithdrawn`, `bankDeposited`, `storageWithdrawn`, `storageDeposited`, and the derived `net`) per avatar **known to either ledger** (`KnownAvatars`, so a member who only ever moved items is listed too, with zero gold), sorted by **German collation** (`Ärger` before `Zorn`, the order the SPA's own text sorting uses); `totalCount` counts that union. |
| `GET /api/v1/avatars/{avatar}/bank` | That avatar's bank entries, newest first. |
| `GET /api/v1/avatars/{avatar}/storage` | That avatar's storage entries, newest first. |

- **One envelope for every collection**: `page`, `size`, `totalCount`, `items`. `/api/v1/avatars`
  adds `lastUpdated` and `totals`; it is the only view that states how fresh the numbers are.
- **`totals` sums every known avatar, not the served page** (decision 2026-09-02), the reading
  `totalCount` already has. It carries the same five numbers as a row, so the SPA renders its total
  row without arithmetic of its own, and neither paging nor a later time window can turn a guild
  total into a page total behind the reader's back.
- **Paging**: `?page=` (zero-based, `@Min(0)`) and `?size=` (default 100, `1..1000`); a violation is
  a **400**, not a clamp, so a client bug stays visible. `totalCount` is the unpaged total, so the
  SPA can size its navigation instead of inferring the end from a short page.
- **The four sums are the sheet's columns 1 to 4, `net` its column 5, and all five are whole gold**
  (`long`, decision 2026-09-02): serving the raw `double` would put every value from 10^7 upward,
  where the real sums sit, on the wire in exponential notation. `net` is **derived per request** and
  stored nowhere. The rounding rule behind the numbers, and why a served row adds up while the total
  row is the exact column sum of the rows above it, lives in
  [domain-model.md](domain-model.md).
- **`lastUpdated: null`** means no collection run has completed. A sentinel instant is not an option:
  Java 25 throws when converting an extreme instant into `java.sql.Timestamp`.
- **`lastUpdated` is display-only and up to an hour off inside the DST fall-back hour.** It is stored
  as a Berlin wall-clock time, so the instant behind it is unrecoverable while the local hour repeats
  and the conversion resolves to the earlier offset. The epoch/UTC storage format fixes this at the
  root; entry timestamps are unaffected because they are real instants.
- **Timestamps** are ISO-8601 UTC and **`transferType`** is one of `DEPOSIT` / `WITHDRAWAL`; the
  client localizes both. The wire names come from `TransferTypeWireNames`, a visitor over the domain
  enum, so the German domain constants (`EINLAGERUNG`/`ENTNAHME`) never reach the contract and stay
  renameable. `toGermanString()` now serves only the database adapter's German column strings.
- **No field name is derived from a Java identifier.** Every wire record component carries an explicit
  `@JsonProperty`, so renaming a component cannot change the contract, and `RenameSafetyTest` fails
  the build if one is missing. The same rule covers the DB side: every `@DatabaseField` names its
  column and every `@DatabaseTable` its table. There is no JDK-standard annotation for this (JSON
  binding never landed in Java SE), so the Jackson annotation is deliberate and confined to the
  `wire` package.
- **A tokenless deep link answers 401.** Only `/` and `/index.html` are public, so the shell loads
  from there and the client carries `?token=` across its routes (see the SPA's views above).
- **404 vs. empty page** (author decision 2026-08-05): a **404 means the avatar is unknown**, i.e. has
  no row in either ledger. A known avatar whose bank or storage ledger happens to be empty answers 200
  with `totalCount: 0` and `items: []`, like the overview does, so the SPA can tell "no storage
  activity" from "no such member" without a second request. A page past the last entry is likewise a
  valid empty window. "Known" means **the avatar has a ledger row somewhere**, deliberately not "the
  meta information mentions it": today the evaluator only writes meta keys for avatars that have rows,
  so the two coincide, and pinning the contract to the ledgers keeps it true if that ever diverges.
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
- **`npmTest` never runs while the Java suite runs** (`mustRunAfter(":test")`) and the Vitest worker
  fan-out is bounded (`test.maxWorkers: 2`). Almost every test file boots its own jsdom, and beside
  Gradle's parallel `:test` the fork pool starved: whole files never *started* ("Failed to start forks
  worker", "Timeout waiting for worker to respond") while no single test failed, so `./gradlew build`
  went red under load and the result XML looked green with a short test count. Standalone the suite
  never failed, which is why the ordering carries the fix and the bound is only the second net.
- **Dev server against a running application:** `npm run dev` serves the SPA on 5173 and proxies
  `/api` to `http://localhost:8080` (`server.proxy` in `vite.config.ts`), so the SPA can be driven
  against real data with hot reload. Deep links work there because Vite answers unknown paths with
  `index.html`, and in the packaged application because no controller owns the three dashboard paths
  any more, so the history fallback answers them with the shell.
- Run `vitest`/`eslint` from `frontend/`: the Vitest config (jsdom environment) lives in
  `frontend/vite.config.ts`, and a run started from the repo root silently uses none of it.
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
