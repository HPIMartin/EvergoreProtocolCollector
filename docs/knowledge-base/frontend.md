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
- `theme.test.ts` guards the stylesheet rather than the pixels: no rule may set a longhand and then
  reset it with a later shorthand (`border-top` before `border` silently loses the longhand, and
  jsdom computes no layout, so no component test can see it); every `var()` resolves, every declared
  token is used, no token is declared twice, no literal colour stands outside the token block, and every
  colour-bearing property is painted from a token, so a CSS keyword colour cannot slip past the literal
  check. Together they keep "one place" true as the sheet grows.
- **Primitives.** All presentational: everything arrives as props, none of them knows `fetch` or a
  route. They live in `src/ui/` and are re-exported from `src/ui/index.ts`.

| Component | Renders |
|-----------|---------|
| `PageFrame` | Banner with brand and navigation (`aria-current="page"` marks the current link), `<main>` for the view. |
| `Link` | An `<a href>` that reports a **plain** click to its `onFollow` and leaves a modified or middle click to the browser, so in-app navigation costs no reload while bookmarking and open-in-new-tab keep working. `PageFrame` and a `link` column render through it. |
| `SortableTable<Row>` | Semantic `<table>`; a column is `text`, `number`, `timestamp` or `link`; a `timestamp` may carry an `href` and then links what it shows; a header click sorts, a second click reverses; given a `deliveredOrder`, it sorts nothing itself and hands the chosen sort to its source instead, marking the column the rows arrived sorted by; an optional `total` adds a `tfoot` row; an optional `mark` flags single rows. |
| `StatusPanel` | The `loading` / `empty` / `error` states; `role="alert"` for the error, `role="status"` otherwise. |

- `format.ts` carries the German domain notation: gold with `de-DE` grouping, instants as Berlin
  wall-clock `dd.MM.yyyy HH:mm`. The zone is pinned to `Europe/Berlin` instead of taken from the
  runtime, so a browser in another zone still shows the time the game showed.
- **Sorting**: text by German collation (`Intl.Collator('de-DE')`, so `Ärger` sorts under `A`), numbers
  numerically, timestamps by instant (an offset other than `Z` still lands in the right place). Missing
  values sort last in **both** directions, equal keys keep their given order, and a table without
  `initialSort` renders the order it was handed, which is the API's newest-first. This is the
  roster tables' sort, which covers the loaded rows; a ledger sorts on the server (below), because
  its rows are one page of many.
- **Tone**: a number column declares itself `credit`, `debit` or `neutral`; a negative value is always
  `debit` and a zero always `neutral`, so "nothing moved" stays uncoloured.
- **The row mark**: an optional `mark` (`(row) => string | null`) flags single rows without adding a
  column. A row it answers a text for gets `data-stale` for the stripe, and an `!` in its **first**
  cell, **ahead** of what that cell already shows; `total.mark` does the same for the `tfoot` row, so
  a total can state something about the rows it sums. The text stands in the DOM at all times inside
  a focusable `role="note"`, and CSS only collapses it visually until hover **or** keyboard focus:
  a screen reader reads it while going through the row, and no hidden duplicate has to be kept in
  step with a visible one. Revealing on `:focus` rather than `:focus-visible` keeps a tap on a
  touch device working, where there is no hover at all, and the revealed note takes pointer events
  back, so reading it with the mouse does not dismiss it. A mark whose text is blank counts as
  **no** mark: an empty note would render a focusable element with no accessible name, a tab stop
  that announces nothing.
- **The total row**: an optional `total` (`{label, row}`) renders one `tfoot` row, outside the sorted
  body, so a sum can never be mistaken for a member or reordered into the middle of the table. Its
  first column carries the label, number columns carry their total, and any other kind carries the
  missing-value dash: a link or a timestamp has no total. A noted column's total carries the same
  hint its rows do when it cannot answer either, because a total that sums rows which cannot answer
  cannot answer itself.
- An `initialSort` naming a column the table does not have **throws**, for the reason the API answers
  400 instead of clamping a bad page size: a client bug stays visible.
- **`StatHeader`** renders a row of named figures, each formatted by `formatGold` and toned by the
  shared rule; a figure of `null` renders the note the caller supplies and is marked `data-absent`,
  so "not computed yet" can never be read as a zero.
- **A number column may carry a `missingNote`**, and then a `null` in it renders the same `!` mark
  with a hover note that a stale row carries at its name, instead of the bare missing-value dash
  (author rule 2026-09-10). The dash keeps its single meaning, "nothing happened here": a figure the
  view cannot compute says so.
- **`OptionSwitch`** is a radio `fieldset` over two or more labelled options, so switching a view's
  figure needs no library and stays keyboard-reachable and announced.
- **`tone.ts` holds the one tone rule** both the table cells and the header figures read: zero is
  neutral, a negative value is a debit, and a positive value takes the tone its caller declares.
- **The gallery**: `gallery.html` plus `src/ui/gallery/` shows every primitive with fixture rows modeled
  on the guild sheet's columns ([google-sheet.md](google-sheet.md)). `npm run dev` serves it at
  `/gallery.html`; `vite build` ignores it, because `index.html` is the only build input, so it never
  reaches the jar. It replaces a Storybook dependency: pure props-in components need no second toolchain.
- `tsconfig.app.json` lists the `node` types because `theme.test.ts` reads the stylesheet from disk.

## The SPA's views

- **Client routes:** `/` and `/overview` show the guild overview, `/avatars/{avatar}/bank` and
  `/avatars/{avatar}/storage` show one avatar's ledger. Every other path renders "no view", so a typo
  in a bookmark says so instead of showing an empty page.
- **`/admin` shows the collection status** (`AdminView.tsx`): `lastUpdated` plus the last successful
  scrape and recompute as freshness text (each with its own "never ran" wording), and the two failure
  instants and two name lists from `/api/v1/admin/status`, each shown only when the server reports one.
  Two further lists, also shown only when non-empty, read the round-trip facts the same envelope
  carries: "Verdacht auf Warenkreislauf" lists each `avatar: quantity × item` round trip, and "Nicht
  beurteilbar (Rezept ungelesen)" lists each `avatar: item` the guild could not judge for lack of a
  read recipe. A round trip of fewer than one piece is refused as a malformed answer
  (`roundTripFrom`), as the server refuses to build one.
  It states facts only; the UP/DOWN verdict over them stays `/health`'s job.
  Deliberately not in `navigationOf`'s link set, so a guild member never sees it; reachable only by
  its direct URL. Needs no token: the page and the endpoint it reads are both in
  `evergore.security.public-paths`, at the same trust level as `/health`.
- The avatar segment is percent-encoded when a link is built and decoded when a path is read; a
  malformed escape is "no view", not a crash.
- **Every view passes through one of five outcomes** (`useLoad` + `LoadedView`): loading, loaded,
  token refused, no such page (a ledger read the API answers 400), failed. A failure shows one
  fixed German text and never the API's status or message. A ledger view additionally tells
  **"known avatar, no entries"** from **"unknown avatar"**, which is the client side of the 404
  decision below.
- **The token is read once from the address** (`?token=`) and carried into every request and every
  in-app link; it is kept nowhere else (no cookie, no `localStorage`), so a link is the whole
  credential and closing the tab ends the session.
- **Ledger paging is server-side and bookmarkable.** A ledger view fetches `windowOf(page)`
  (`?page=&size=100`) and shows `items.length` of `totalCount`; the page number round-trips through
  the address (`route.ts`'s `PAGE` param), so `Pagination` (`ui/Pagination.tsx`) can build "Zurück"/
  "Weiter" links from it via `hrefOf`. A page past the end is not a failure: the API answers 200 with
  `items: []`, rendered as the existing empty state. An invalid page (negative or non-numeric) is
  passed through **unclamped**, so the API's `@Min(0)` violation answers 400 and the ledger says
  "Diese Seite gibt es nicht.", because clamping it client-side would hide a bad link instead of
  showing it. Only a ledger read maps a 400 that way, because the page number and the sort are
  the only request values the client does not fix (the size is constant, a bad token answers
  401); on the overview or the admin page a 400 is a plain failure.
- **A ledger's sort is address state too** (decision 2026-10-05): `route.ts` reads `sort` and
  `direction` (`SORT`, `DIRECTION`), completes an address that names only one of them with the
  API's defaults (`timestamp`, `descending`), and the view asks the API for that order. A value
  the API does not know is passed through unchanged, like an invalid page, so its 400 reads "Diese
  Seite gibt es nicht."; an address without either asks for no sort and gets the newest first.
  A ledger's column head therefore sorts the **whole ledger**: the ledger view hands its table a
  `deliveredOrder`, so a click follows a link to the chosen order on the **first page**, and the
  table shows the rows in the order the API answered rather than sorting them again, since the
  SPA's ICU collation and the server's `GermanOrder` order punctuation differently (**F13**).
  "Zurück" and "Weiter" carry the sort, so a page of a sorted ledger is a bookmark of that order.
  The "Bank" and "Lager" links in the frame carry none: the two ledgers sort by different columns.
- **The views own their columns, the primitives own the rendering.** A view declares its
  `Column` list and hands `SortableTable` the domain rows; timestamps go in as ISO strings, which is
  what the column kind reads, and `format.ts` is the one place that turns them into Berlin
  wall-clock. A transfer type is worded by the ledger's own entry type in the domain: the bank says
  `Einzahlung`/`Entnahme` (`domain/bankEntry.ts`), the storage `Einlagerung`/`Entnahme`
  (`domain/storageEntry.ts`). The headers are German, like the sheet's.
- **The overview's columns are the sheet's, in the sheet's order, and both roster tables share the
  one definition:** `Bank-Einzahlung`,
  `Bank-Auszahlung`, `Einlagerung`, `Entnahme`, the switched figure `Nach Abzügen`/`Vor Abzügen` and then the
  sheet's two right-hand columns `Letzte Lageraktivität` and `Letzte Bankaktivität`, so a member
  reconciles his own row against the sheet column by column. A ledger the avatar never used shows the
  missing-value dash, not a fabricated date. Deposits carry the credit tone,
  withdrawals the debit tone, and the switched figure carries neither until it turns negative, which
  the shared tone rule in `ui/tone.ts` does for every number column and for the header's figures.
- **The guild's position is a header above the table, not a number inside it** (decision
  2026-09-04, four figures since 2026-09-10): `StatHeader` states `Gildenbank`, `Gildenlagerwert`,
  `Gildenspende` and `Handwerkssubventionen` side by side, so measured gold is never added to
  modelled material and no figure has to net a donation against a payout to fit one label.
  `OptionSwitch` toggles the table's sixth column between `Nach Abzügen`, what the guild credits the
  member, and `Vor Abzügen`, what he moved before the guild's share is taken off; the two differ by
  that row's `donation - craftSubsidy`, give or take a gold, since each is rounded from its exact
  value. The table keeps its eight columns and its density either way, and the sixth column keeps
  **one key** (`figure`), changing only its header and value, because the table remembers a sort by
  column key and would otherwise throw on a sort the switch renamed away. `domain/guildPosition.ts`
  gathers the header's figures from the served numbers and derives only the bank, so the view holds
  no arithmetic and `ui` stays presentational. A figure the flows are missing for renders the note
  from `domain` instead of a number, marked `data-absent`, while the bank still answers because it
  is measured rather than modelled; only while a member is not yet computed, and the totals come
  without sums, does the bank read the same note, since leaving his gold out would be wrong without
  a trace. The note names no next run, because an avatar whose recompute keeps failing would never
  bring one.
- **The chosen figure is view state, not address state:** it resets to `Nach Abzügen` on a reload and on
  a route round trip, unlike the ledger's page number, which `route.ts` round-trips on purpose. A
  shared link therefore always opens on `Nach Abzügen`.
- **A row whose sums are older than the last collection is marked, and its activity columns are
  not** (decision 2026-09-09): `staleSumsFrom` becomes `SortableTable`'s `mark`, so that row gets
  the stripe and an `!` ahead of the avatar name reading "Veraltete Zahlen. Letzte
  erfolgreiche Aktualisierung vom `<Berlin wall clock>`."; `totals.containsStaleSums` becomes
  `total.mark`, so the guild row states that it contains such a row even when the served page does
  not show it. The two activity columns of a marked row keep showing what the ledger says:
  suppressing a true fact to prevent a wrong inference is the wrong trade, and the marker is what
  removes the inference. The wording lives in `domain/staleSums.ts` beside the entry types' German
  names, and takes an already formatted instant, so the German stays in `domain` while the Berlin
  wall clock stays in `ui`'s `format.ts`. What the row cannot say, and why, is under the wire contract below.
- **A row served without its five sums is marked "Noch nicht berechnet.", and that mark wins over
  the outdated one** (decision 2026-09-23): its five number columns show the dash, so no zero reads
  as "moved nothing", and the guild row served without sums reads "Enthält mindestens eine Zeile,
  die noch nicht berechnet ist." even when it also contains an outdated row. `domain/uncomputedSums.ts`
  holds the guild wording and `isUncomputed`; the row's wording is the header's `UNCOMPUTED_NOTE`.
  When the server serves such a row is under the wire contract below.
- **The guild-wide total row comes from the envelope, not from the loaded rows** (decision
  2026-09-02): the overview hands `totals` to the table's `total` prop and does no arithmetic, so the
  row keeps meaning the guild once the overview pages or a time window narrows the body. It is
  rendered **once**, in the active table's `tfoot` (decision 2026-09-11): served across all avatars,
  it is the sum of neither half, so it keeps its `Gilde` label under the table a reader actually
  reads instead of stating the same guild-wide figure twice.
- **The roster stands in two tables, active above and dormant below** (decision 2026-09-11):
  `domain/rosterSplit.ts` divides the loaded rows, both tables render the **same** `Column` list, and
  each keeps the German-collation order the API handed over, so the split reorders nothing. A row is
  active when the later of its two activity timestamps is no older than `ACTIVITY_WINDOW_DAYS` before
  the **newest activity in the loaded rows themselves**. The reference is the data's, never the
  reader's clock: tied to the wall clock the whole roster falls into "dormant" as soon as the scraper
  stalls, which is exactly when someone opens the page, whereas the row holding the maximum is active
  by construction. With no activity anywhere there is no reference and every row stays active: the
  dormant table is an exception list, and nothing moves into it without a measurable reason. The
  window is 30 days rather than a calendar month because subtracting a month normalizes 31.07 to
  01.07 and would change the window's length with the reference date. `lastUpdated` is deliberately
  **not** read for this: it lives on `/api/v1/admin/status`, and the members' view does not reach
  into an operator surface for a presentation question.
- **The split is client-side and therefore covers only the loaded page.** Both captions state their
  half against `totalCount` (`Aktiv (30 Tage vor dem letzten Vorgang): 24 von 42 Avataren`), so the
  two never claim to have sorted the whole guild; today `size` 100 against 42 avatars means one page
  holds all of them. The active caption names the window's **reference** rather than reading "letzte
  30 Tage", which a member would take to mean 30 days from today and which is false on exactly the
  stale data the cut exists for.
- **Navigation lives in the frame and in the overview's own cells.** `PageFrame` carries
  "Übersicht" plus, on a ledger, that avatar's "Bank" and "Lager". In the overview the avatar cell
  links into the bank, and the two activity cells link into the ledger each of them reports on, so
  every column head sorts something real instead of offering a sort on a column of identical words.
  A cell without a timestamp links nowhere: nothing happened there to open. All of it goes through
  `Link`, so the shell is never reloaded.
- **Tests reach no network.** The faked seam is `HttpGet`, answering a real `Response`, so status
  handling and URL building are exercised for real. Asynchronous assertions flush microtasks with
  `act`; no test uses a timer, a `waitFor` poll or a wall-clock wait.
- **The one test that does run the real thing** is on the Java side: `DashboardBrowserSmokeTest`
  drives headless Firefox against the booted server and reads the painted rows back through the
  `data-testid` hooks, so "the bundle reaches the API and shows its data" is proven somewhere
  (`testing.md`). It is the reason the hooks are a contract, not a convenience. It reads the two
  roster tables apart through `active-roster` / `dormant-roster`, and its fixture is from 2024, so a
  cut against the reader's clock would empty its active table.

## The JSON API the SPA reads

Shape and field names decided 2026-08-04 (open-questions.md). The controllers live in
`rest/controller/api/`, the published contract types in `rest/controller/api/wire/`; it is the
service's only read surface.

| Route | Answers |
|-------|---------|
| `GET /api/v1/avatars` | Overview: one `AvatarSummary` (`avatar`, the four ledger sums `bankWithdrawn`, `bankDeposited`, `storageWithdrawn`, `storageDeposited`, the derived `net`, the two flows `donation` and `craftSubsidy`, plus `lastBankActivity`, `lastStorageActivity` and `staleSumsFrom`) per avatar **known to either ledger** (`KnownAvatars`, so a member who only ever moved items is listed too, with zero gold), sorted by **German collation** (`Ärger` before `Zorn`, the order the SPA's own text sorting uses); `totalCount` counts that union. |
| `GET /api/v1/avatars/{avatar}/bank` | That avatar's bank entries, newest first unless `sort` names another order. |
| `GET /api/v1/avatars/{avatar}/storage` | That avatar's storage entries, newest first unless `sort` names another order. |
| `GET /api/v1/admin/status` | Anonymous, `token`-exempt (same trust level as `/health`): `lastUpdated`, `lastSuccessfulScrape`, `lastScrapeFailure`, `lastSuccessfulRecompute`, `lastRecomputeFailure`, `unknownItemNames`, `failedAvatarNames`, `roundTrips`, `roundTripAbstentions`, every key always rendered (the two round-trip arrays as `[]` rather than `null` when empty). The operator-facing facts that used to sit on the overview; see below. |

- **One envelope for every collection**: `page`, `size`, `totalCount`, `items`. `/api/v1/avatars`
  adds `totals`.
- **`totals` sums every known avatar, not the served page** (decision 2026-09-02), the reading
  `totalCount` already has. It carries the same seven numbers as a row, so the SPA renders its total
  row without arithmetic of its own, and neither paging nor a later time window can turn a guild
  total into a page total behind the reader's back. Its last field `containsStaleSums` is over the
  same union, so the total row still states that it contains a stale row when the served page does
  not show that row.
- **`staleSumsFrom` is `null` unless the row's sums are older than the last collection**, and then it
  is the instant they were last recomputed, or, for a failed avatar whose stored sums carried no
  instant, the seeded bound his sums predate (domain-model.md). One nullable field rather than a flag beside a
  timestamp: present means both "stale" and "this old". The comparison happens **server-side**,
  against the newest per-avatar recompute instant in the meta store, so no guild-wide collection
  timestamp returns to this envelope; the one that used to sit here moved to
  `/api/v1/admin/status`. A row a recompute failure skipped therefore states its own age, while
  scrape time and the failure's own record stay operator's data on the admin surface. What the
  instant means, and why it is stored as epoch millis, is in
  [domain-model.md](domain-model.md).
- **Paging**: `?page=` (zero-based, `@Min(0)`) and `?size=` (default 100, `1..1000`); a violation is
  a **400**, not a clamp, so a client bug stays visible. `totalCount` is the unpaged total, so the
  SPA can size its navigation instead of inferring the end from a short page.
- **Sorting a ledger** (decision 2026-10-05): `?sort=` names a column as the wire names its field
  (bank: `timestamp`, `avatar`, `amount`, `transferType`; storage: `timestamp`, `avatar`,
  `quantity`, `name`, `quality`, `transferType`) and `?direction=` is `ascending` or `descending`;
  they default to `timestamp` and `descending`. The server sorts the whole ledger before it pages,
  in a total order whose last keys are the time and the row id
  ([architecture.md](architecture.md)), and item names in German order. An unknown column, a
  column of the other ledger, an unknown direction or any other spelling is a **400**, checked
  before the avatar is looked up; `SortRequest` holds the only list of accepted names, so no name
  from the request reaches the SQL. Deposits sort before withdrawals in ascending order.
- **The four sums are the sheet's columns 1 to 4, `net` its column 5, and all of them are whole gold**
  (decision 2026-09-02): serving the raw `double` would put every value from 10^7 upward,
  where the real sums sit, on the wire in exponential notation. `net` is **derived per request** and
  stored nowhere. Each is its exact value rounded once, the totals' included, so a served figure can
  differ from the sum of those beside or above it; the rule lives in
  [domain-model.md](domain-model.md).
- **The five sums are `null` together exactly when no recompute has reached the avatar**
  (decisions 2026-09-23 and 2026-10-06): such a row is "not yet computed", never zeros, and its
  flows, `balance` and `staleSumsFrom` are `null` too, so the server tells "never computed" from
  "outdated" and the SPA reads it off the wire. `totals` serves every figure as `null` as soon as
  one avatar is not yet computed, since a guild figure must not leave him out, while
  `containsStaleSums` still answers. How the SPA shows the state is under the views above.
- **The SPA reads a row or `totals` whose five sums are all `null` as carrying none**
  (`sumsFrom` in `api/wire.ts`), and refuses one with only some of them absent, or without them
  but with a flow, `balance` or `storageValue` (`refuseFiguresWithoutSums`), as a malformed answer,
  and so a page whose row carries no sums while its `totals` still do, or one that holds the whole
  guild with every row computed while its `totals` carry none. An age beside absent sums is
  tolerated rather than refused, because the not-yet-computed mark wins over the outdated one anyway.
- **`donation` and `craftSubsidy` are the two flows between what a deposit credited and what it is
  worth to the guild** (decision 2026-09-10): what a member gave for nothing, and what the guild
  paid above its own price for bought trader goods. They are served per avatar and in `totals`,
  derived per request from the exact sums, rounded once and stored nowhere, and they are the two
  numbers the header needs that the other five cannot yield. Both are **`null` while no recompute
  has produced them** (a fresh deployment before its first run), and `null` together rather than one
  at a time, because the read path only forms the pair when both are stored; guild-wide they are
  `null` as soon as they are missing for a single avatar. The SPA forms only the bank by
  subtraction, `bankDeposited - bankWithdrawn`, which is measured gold and exact, so no separate
  header object is served. The valuation itself, and the identity behind the header, live in
  [domain-model.md](domain-model.md).
- **`balance` is the figure before the guild's share, served per avatar and in `totals`, and
  `storageValue`, the header's `Gildenlagerwert`, only in `totals`** (decision 2026-09-27): each is
  its exact value rounded once, which the SPA could not work out from the rounded figures beside it
  without a second rounding. Both are `null` exactly when `donation` and `craftSubsidy` are.
- **`lastBankActivity` / `lastStorageActivity` are `null` when the avatar never appeared in that
  ledger**, which is the case the sheet leaves blank. They are read from the ledger rows rather than
  from the meta store, so they are as fresh as the last ingest instead of as fresh as the last
  evaluation; why they come from there is in [architecture.md](architecture.md) (decision
  2026-09-02). They inherit the ledger's storage format, so like `/api/v1/admin/status`'s
  `lastUpdated` they can read back an hour late for an activity inside the Berlin DST fall-back hour
  until the epoch/UTC storage format lands (backlog D14).
- **`/api/v1/admin/status`'s `lastUpdated: null`** means no collection run has completed. A sentinel
  instant is not an option: Java 25 throws when converting an extreme instant into
  `java.sql.Timestamp`.
- **`/api/v1/admin/status`'s `lastUpdated` is display-only and up to an hour off inside the DST
  fall-back hour.** It is stored as a Berlin wall-clock time, so the instant behind it is
  unrecoverable while the local hour repeats and the conversion resolves to the earlier offset. The
  epoch/UTC storage format fixes this at the root. **Entry timestamps are not exempt** (falsifier
  proof 2026-09-02): they are real instants in the domain but persist as wall-clock text too, so two
  entries an hour apart inside the fall-back hour store the same text and both read back as the later
  one. Only the container's UTC default keeps every timestamp the API serves correct today (backlog
  D14).
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
