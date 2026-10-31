# 14: Frontend

- The dashboard is a React single-page app (SPA) over a JSON API (decision 2026-07-04).
- This doc covers both the SPA and the JSON API it reads from.

## Stack

- **React 19** + **TypeScript**, built with **Vite** (`frontend/`, its own Gradle subproject).
- **Vitest** + **React Testing Library** for component tests (`npm run test`, jsdom environment).
- **ESLint** (`typescript-eslint`'s `strict` config, `eslint-plugin-import-x`,
  `eslint-plugin-react-hooks`) lints, **Prettier** formats.
- **Spotless formats no TypeScript**: Java and the Gradle scripts only
  ([build-run-deploy.md](build-run-deploy.md), "Build").
- `react-hooks/rules-of-hooks` and `react-hooks/exhaustive-deps` are errors over `src/`: a hook
  called conditionally, or an effect missing a dependency, fails `npmLint`.
- `import-x/no-restricted-paths` enforces the layer boundary below at lint time; the frontend's
  analog of the backend's `HexagonalArchitectureTest` (ArchUnit), via ESLint instead of a JVM test.

## Structure & the dependency rule

Five top-level folders under `frontend/src/`:

| Folder | May import from | Purpose |
|--------|------------------|---------|
| `domain` | (nothing under `src/`) | Framework-free types/logic; the core, like the backend's `domain`/`businessLogic`. |
| `api` | `domain` | Talks to the backend JSON API; translates wire shapes to domain types. |
| `ui` | `domain` | Presentational React components; no direct `api` or `app` imports. |
| `app` | `domain`, `api`, `ui` | The dashboard's composition root: wires `api` implementations into `ui`, routing, the app shell. |
| `login` | `domain`, `api`, `ui` | The login page's composition root, a bundle of its own (below); `app` and `login` never import each other. |

- Enforced per the table above (no sideways/upward imports); mirrors the backend's inward-only
  rule (`application` depends only inward, never on adapters/config).
- Each of `domain`, `api`, `ui` and `app` has an `index.ts` as its **public surface**; cross-layer
  imports go through it.
  - Inside a layer modules import each other directly.
  - `login` has no `index.ts`: `main.tsx` is its entry.
- Responsibilities, so a reader knows which folder to open:
  - `domain`: the wire contract's concepts as types, plus the framework-free logic over them (the
    `Ledger` visitor, the German and Berlin-local wording).
  - `api`: the `ProtocolApi` port and its `fetch` adapter, which validates every wire body before
    translating it into domain types; `HttpGet` and `HttpPost` are the seams the tests fake.
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
- **The act-as-named-value rule of `testing.md` is a lint rule here:** in
  `src/**/*.test.{ts,tsx}`, ESLint refuses any call inside the argument of `expect(…)`,
  `expect.soft(…)` or `expect.poll(…)`, however deep, and a function passed to them directly,
  so a thrower is bound to a name first.
- A `new`, a tagged template and an `import()` inside that argument are refused too.
- A Testing Library query (`screen.…`, `within(…).…`) may stand inside an `expect(…)`.
  - Its own arguments must be plain: a call or `new` there is refused.
- The rule keys on the names `expect`, `screen` and `within`; outside it are:
  - an alias of `expect`, and `expect['soft']`;
  - a call in a matcher argument, and other assertion APIs;
  - a `screen` method that is no query;
  - an assignment, `++`, `--` or `delete` inside the argument.
- Components expose a stable **`data-testid`** instead of brittle text/CSS selectors (e.g.
  `App.test.tsx` asserts on `screen.getByTestId('page-brand')`).
- Tables render **semantic HTML** (`<table>`/`<thead>`/`<tbody>`/`<th>`/`<td>`), not div-grids,
  for accessibility and testability.

## The ui layer: theme and primitives

- **One stylesheet.** `src/index.css` holds the design tokens (in `:root`) and every component rule.
  Components carry class names only: no inline styles, no per-component stylesheet, so a colour or a
  spacing exists in exactly one place.
- Token groups: `--color-*`, `--font-*`, `--space-*`, plus `--radius`, `--border-width`,
  `--content-width`. The look they encode (dark tavern climate, compact rows, serif display type over
  a sans body) is the 2026-08-07 decision in [open-questions.md](../open-questions.md).
- `theme.test.ts` guards the stylesheet rather than the pixels:
  - no rule may set a longhand and then reset it with a later shorthand (`border-top` before
    `border` silently loses the longhand, and jsdom computes no layout, so no component test can
    see it);
  - every `var()` resolves;
  - every declared token is used;
  - no token is declared twice;
  - no literal colour stands outside the token block;
  - every colour-bearing property is painted from a token, so a CSS keyword colour cannot slip past
    the literal check.
  - Together these checks keep "one place" true as the sheet grows.
- **Primitives.** All presentational: everything arrives as props, none of them knows `fetch` or a
  route. They live in `src/ui/` and are re-exported from `src/ui/index.ts`.

| Component | Renders |
|-----------|---------|
| `PageFrame` | Banner with brand and navigation (`aria-current="page"` marks the current link), `<main>` for the view. Its optional `account` prop adds an account area after the navigation (below). |
| `Link` | An `<a href>` that reports a **plain** click to its `onFollow` and leaves a modified or middle click to the browser, so in-app navigation costs no reload while bookmarking and open-in-new-tab keep working. `PageFrame` and a `link` column render through it. |
| `SortableTable<Row>` | Semantic `<table>` whose header click sorts and whose second click reverses; its columns, `deliveredOrder`, `total` and `mark` are in the bullets below. |
| `StatusPanel` | The `loading` / `empty` / `error` states; `role="alert"` for the error, `role="status"` otherwise. |

- **The account area of the page frame** (`data-testid="account-area"`):
  - it says „Angemeldet als <name>“ (`logged-in-as`);
  - it offers the button „Abmelden“;
  - it offers the button „Passwort ändern“ unless the login is the admin key's;
  - they are buttons, not anchors, so the navigation reader collects none of them;
  - the area holds no `p[data-testid]`, which the admin page reader collects;
  - only after a failed logout it holds the `status-panel` that says so.
- **The sortable table's columns**: a column is `text`, `number`, `timestamp` or `link`.
  - A `timestamp` may carry an `href` and then links what it shows.
- **A `deliveredOrder`** makes the table sort nothing itself: it hands the chosen sort to its
  source instead and marks the column the rows arrived sorted by.
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
  column.
  - A row it answers a text for gets `data-stale` for the stripe, and an `!` in its **first** cell,
    **ahead** of what that cell already shows.
  - `total.mark` does the same for the `tfoot` row, so a total can state something about the rows
    it sums.
  - The text stands in the DOM at all times inside a focusable `role="note"`; CSS only collapses it
    visually until hover **or** keyboard focus.
  - So a screen reader reads it while going through the row, and no hidden duplicate has to be kept
    in step with a visible one.
  - Revealing on `:focus` rather than `:focus-visible` keeps a tap on a touch device working, where
    there is no hover at all.
  - The revealed note takes pointer events back, so reading it with the mouse does not dismiss it.
  - A mark whose text is blank counts as **no** mark: an empty note would render a focusable
    element with no accessible name, a tab stop that announces nothing.
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
- **A number column may carry a `missingNote`**, a function of the row, and then a `null` in it
  renders the note it returns as the same `!` mark with a hover note that a stale row carries at its
  name, instead of the bare missing-value dash (author rule 2026-09-10). The dash keeps its single
  meaning, "nothing happened here": a figure the view cannot compute says so.
  - The overview's "Vor Abzügen" returns no note for a row not yet computed, or the guild row while
    one is: the row's mark already names that state (author decision 2026-10-14).
- **`OptionSwitch`** is a radio `fieldset` over two or more labelled options, so switching a view's
  figure needs no library and stays keyboard-reachable and announced.
- **`tone.ts` holds the one tone rule** both the table cells and the header figures read: zero is
  neutral, a negative value is a debit, and a positive value takes the tone its caller declares.
- **The gallery**: `gallery.html` plus `src/ui/gallery/` shows every primitive with fixture rows modeled
  on the guild sheet's columns ([google-sheet.md](google-sheet.md)). `npm run dev` serves it at
  `/gallery.html`; the builds ignore it, because `index.html` and `login.html` are their only inputs, so it never
  reaches the jar. It replaces a Storybook dependency: pure props-in components need no second toolchain.
- `tsconfig.app.json` lists the `node` types because `theme.test.ts` reads the stylesheet from disk.

## The SPA's views

- **Client routes:** `/` and `/overview` show the guild overview, `/avatars/{avatar}/bank` and
  `/avatars/{avatar}/storage` show one avatar's ledger, and `/explanation` the explanation page
  (`ExplanationView.tsx`).
  - `/admin` and `/password` are below.
  - Every path not named here renders the notice `Für <path> gibt es keine Ansicht.`, so a typo in
    a bookmark says so instead of showing an empty page.
- **`/explanation` loads no data**: it stays behind a login because the server refuses a page
  without one, not because a request of its own would fail (decisions 2026-10-14 and 2026-10-21).
  - Its frame offers the overview only.
  - Its content is one data file, `app/explanation/explanation.json`: the personas, the five
    groups and each answer's question, rule, points, example ledger, calculation lines and "why".
  - `domain/explanation.ts`'s `explanationFrom` reads that file, refusing what it cannot read, a
    figure whose operators do not join its terms and a term naming a calculation that does not
    come before its figure.
  - Its `calculationTexts` writes each calculation line in German, rounding a result that is no
    whole gold as the overview does, beside its exact value.
  - `ExplanationExamplesTest` recomputes the same file through the backend's valuation, so the page
    and the overview cannot drift apart unnoticed (testing.md).
  - `ExplanationView.test.tsx` renders the page and compares its whole visible text with
    `app/explanation/explanation.approved.txt`, the text the author approved; a changed word fails
    until that file changes in the same commit (decision 2026-10-15).
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
  its direct URL. The page and the endpoint it reads need a login.
- **`/admin` lists the accounts** (decision 2026-10-26):
  - Under „Konten“ a table with „Name“, „Ruht“ and „Letzte Anmeldung“ holds one row per account.
  - Rows stand in the order the service sent (its German order).
  - The table is not sortable, so the SPA's collation never reorders it.
  - The column of the buttons has an empty header cell with the `aria-label` „Aktion“.
  - A list without an account shows the heading and the table head, no row and no error.
  - „ruht“ marks a resting account.
  - The latest login is shown in the page's timestamp format (`formatTimestamp`).
  - „noch nie“ stands for no latest login.
  - The page loads the status and the accounts together (`Promise.all`), so it has one outcome of
    the six.
  - Beside each account stands the button „Passwort zurücksetzen“ (`account-reset`).
  - The button asks first, on the page itself and not in a `window.confirm`.
  - The question reads „Passwort von <Name> wirklich zurücksetzen?“ (`reset-question`).
  - The question offers „Zurücksetzen“ (`reset-confirm`) and „Abbrechen“ (`reset-cancel`).
  - „Abbrechen“ closes the question and sends nothing.
  - The question, the issued password and a failure stand in a polite `aria-live` region of the
    row.
  - The focus moves to that row's „Zurücksetzen“ when the question opens, so a keyboard user is
    not left on `body` when the row's „Passwort zurücksetzen“ is replaced by the question.
  - The focus moves to that row's „Passwort zurücksetzen“ when „Abbrechen“ closes the question.
  - The focus moves to that row's „Passwort zurücksetzen“ when the answer of „Zurücksetzen“
    settles.
  - After „Zurücksetzen“ the issued password stands in that account's row (`account-issued`).
  - The issued password lives only in the list's component state, so it is gone when the admin
    leaves the page.
  - The issued password is never put in the browser's storage.
  - The issued password is never read behind a second request.
  - A second reset of another account leaves the first password beside its own name.
  - A second reset of the same account replaces the password.
  - A reset the service refuses with 401 or 403 replaces the page's content as a load answered with
    it does (the page hands the refusal to `failureOf`, the same outcome `useLoad` yields).
  - A reset the service refuses with 401 shows the login (above).
  - A reset the service refuses with 403 shows „Diese Seite ist dem Admin vorbehalten.“.
  - Any other failed reset shows „Die Daten konnten nicht geladen werden. Bitte später erneut
    versuchen.“ (`LOAD_FAILED_MESSAGE`) in that account's row.
  - A 404 for a name without an account shows the same text.
  - A failed reset removes the password shown in that row: it may be dead, as the service may have
    applied the reset and lost only the answer.
  - The failure stands until the next „Zurücksetzen“ of that account.
  - The failure and the password are state per account, so another account's reset changes
    neither.
  - While an account's reset is in flight its row offers no „Passwort zurücksetzen“, so at most one
    reset of an account is under way and its answer is the one shown.
  - That holds within one open admin page: a second page, or a page opened again while a reset is
    under way, can start another.
  - A failed reset leaves the list as it was.
  - An admin who resets the own account sees the password like any other.
  - The reset of the own account ended the login, so the next navigation's load answers 401 and the
    page asks to log in as it does for any ended login (above).
  - The page does nothing of its own for the own account.
  - Hooks: `accounts-title`, `accounts`, per row `account-row` with `account-name`,
    `account-rests` and `account-last-login`.
  - None is a `p`, so the admin page reader that collects `p[data-testid]` reads the status facts
    only.
- The avatar segment is percent-encoded when a link is built and decoded when a path is read; a
  malformed escape is "no view", not a crash.
- **Every view that loads data passes through one of six outcomes** (`useLoad` + `LoadedView`):
  loading, loaded, login refused, role refused, no such page, failed:
  - a role refused is a 403, shown as „Diese Seite ist dem Admin vorbehalten.“;
  - no such page is a ledger read the API answers 400;
  - a failure shows one fixed German text and never the API's status or message;
  - a 401 sends the browser to `/login?next=<the current path and query, URL-encoded>`
    (`LoginRequired`, which calls `App`'s `navigate` prop, the browser's `window.location.assign`
    by default);
  - so a login that ends while a page is open leads back to that page after the next login, and
    the page shows „Weiter zur Anmeldung…“ meanwhile;
  - a ledger view additionally tells **"known avatar, no entries"** from **"unknown avatar"**,
    which is the client side of the 404 decision below.
- **`useLoad` starts a request on its request key alone**, the key being the request's full
  identity (`requestKeyOf`): it calls `load` through `useEffectEvent`, so a new `load` closure
  starts no request.
- **The SPA carries no credential.**
  - The login is the browser's `epc_session` cookie.
  - The script of a page cannot read it.
  - Every request of the SPA sends it by itself, so a link is an address and nothing more.
- **Ledger paging is server-side and bookmarkable.**
  - A ledger view fetches `windowOf(page)` (`?page=&size=100`) and shows `items.length` of
    `totalCount`.
  - The page number round-trips through the address (`route.ts`'s `PAGE` param), so `Pagination`
    (`ui/Pagination.tsx`) can build "Zurück"/"Weiter" links from it via `hrefOf`.
  - A page past the end is not a failure: the API answers 200 with `items: []`, rendered as the
    existing empty state.
  - An invalid page (negative or non-numeric) is passed through **unclamped**, so the API's
    `@Min(0)` violation answers 400 and the ledger says "Diese Seite gibt es nicht.".
  - Why unclamped: clamping it client-side would hide a bad link instead of showing it.
  - Only a ledger read maps a 400 that way, because the page number and the sort are the only
    request values the client does not fix (the size is constant, a missing login answers 401).
  - On the overview or the admin page a 400 is a plain failure.
- **A ledger's sort is address state too** (decision 2026-10-05): `route.ts` reads `sort` and
  `direction` (`SORT`, `DIRECTION`), completes an address that names only one of them with the
  API's defaults (`timestamp`, `descending`), and the view asks the API for that order. A value
  the API does not know is passed through unchanged, like an invalid page, so its 400 reads "Diese
  Seite gibt es nicht."; an address without either asks for no sort and gets the newest first.
  A ledger's column head therefore sorts the **whole ledger**: the ledger view hands its table a
  `deliveredOrder`, so a click follows a link to the chosen order on the **first page**, and the
  table shows the rows in the order the API answered rather than sorting them again, since the
  SPA's ICU collation and the server's `GermanOrder` order punctuation differently (backlog F13).
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
  2026-09-04, four figures since 2026-09-10):
  - `StatHeader` states `Gildenbank`, `Gildenlagerwert`, `Gildenspende` and
    `Handwerkssubventionen` side by side, so measured gold is never added to modelled material and
    no figure has to net a donation against a payout to fit one label.
  - Below the four figures the overview's content links to the explanation page ("Wie entstehen
    diese Zahlen?"); the frame keeps its one link (decision 2026-10-14).
  - `OptionSwitch` toggles the table's sixth column between `Nach Abzügen`, what the guild credits
    the member, and `Vor Abzügen`, what he moved before the guild's share is taken off.
  - The two differ by that row's `donation - craftSubsidy`, give or take a gold, since each is
    rounded from its exact value.
  - The table keeps its eight columns and its density either way.
  - The sixth column keeps **one key** (`figure`), changing only its header and value, because the
    table remembers a sort by column key and would otherwise throw on a sort the switch renamed
    away.
  - `domain/guildPosition.ts` gathers the header's figures from the served numbers and derives only
    the bank, so the view holds no arithmetic and `ui` stays presentational.
  - A figure the flows are missing for renders the note from `domain` instead of a number, marked
    `data-absent`, while the bank still answers because it is measured rather than modelled.
  - Only while a member is not yet computed, and the totals come without sums, does the bank read
    the same note, since leaving his gold out would be wrong without a trace.
  - The note names no next run, because an avatar whose recompute keeps failing would never bring
    one.
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
  - `domain/rosterSplit.ts` divides the loaded rows by the `active` mark the service gave each of
    them; the page draws no line of its own and reads no date for it.
  - Both tables render the **same** `Column` list.
  - Each keeps the German-collation order the API handed over, so the split reorders nothing.
  - The rule behind the mark, the reference, the empty case and the window: the `active` bullet
    under the endpoint table below.
- **The two tables hold only the loaded page, while the `active` mark covers the whole guild.**
  - Both captions state their half against `totalCount`
    (`Aktiv (30 Tage vor dem letzten Vorgang): 24 von 42 Avataren`), so the two never claim to have
    sorted the whole guild.
  - The overview asks for a page `size` of 100, so one page holds the whole guild while it has at
    most 100 avatars.
  - The active caption names the window's **reference** rather than reading "letzte 30 Tage",
    which a member would take to mean 30 days from today and which is false on exactly the stale
    data the cut exists for.
- **Navigation lives in the frame, in the overview's own cells and in its link to the explanation
  page.** `PageFrame` carries "Übersicht" plus, on a ledger, that avatar's "Bank" and "Lager". In
  the overview the avatar cell links into the bank, and the two activity cells link into the ledger
  each of them reports on, so every column head sorts something real instead of offering a sort on a
  column of identical words. A cell without a timestamp links nowhere: nothing happened there to
  open. All of it goes through `Link`, so the shell is never reloaded.
- **The account area is fed by the session** (`useAccount`, `App`'s `session` prop, built by
  `httpSessionApi`):
  - it reads `GET /api/v1/me`;
  - it shows nothing while that is loading or failed, so the page's own load decides about a login
    that has ended;
  - „Abmelden“ posts `/api/v1/logout` and then navigates to `/login`;
  - a logout the service answers 401 navigates to `/login` too, because the login had ended
    already (after a restart or the 7 days);
  - any other failure leaves the page where it is and shows „Die Abmeldung hat nicht geklappt.
    Bitte erneut versuchen.“ in the account area until the next click on „Abmelden“.
- **Tests reach no network.** The faked seams are `HttpGet` and `HttpPost`, answering a real `Response`, so status
  handling and URL building are exercised for real. Asynchronous assertions flush microtasks with
  `act`; no test uses a timer, a `waitFor` poll or a wall-clock wait.
- **The one test that does run the real thing** is on the Java side: `DashboardBrowserSmokeTest`
  drives headless Firefox against the booted server and reads the painted rows back through the
  `data-testid` hooks, so "the bundle reaches the API and shows its data" is proven somewhere
  (`testing.md`). It is the reason the hooks are a contract, not a convenience. It reads the two
  roster tables apart through `active-roster` / `dormant-roster`, and its fixture is from 2024, so a
  cut against the reader's clock would empty its active table.

## The login page

- `LoginPage` is headed „Anmeldung“.
- It asks for the name and the password.
- It posts both through `httpLoginApi` (`api`, the `HttpPost` seam the tests fake) to
  `/api/v1/login` ([build-run-deploy.md](build-run-deploy.md), "The login API").
- The button „Anmelden“ is disabled while the request is on its way and a second submit posts
  nothing, so a double click spends one of the address's requests, not two.
- The service's answer becomes a `LoginAnswer` (`domain`):
  - `logged-in` navigates to the `next` the answer names;
  - a refused login (401) says „Name oder Passwort stimmt nicht.“ and stays;
  - a turned-away request (429) says „Zu viele Versuche. Bitte warte einen Moment.“;
  - an account that rests (403) says „Dein Konto ruht, bis das Protokoll wieder einen Vorgang von
    dir zeigt.“ (`RESTING_MESSAGE`, `ui`), in the new-password step too;
  - `choose-new-password` turns the page to the step „Neues Passwort wählen“ (`NewPasswordStep`).
- **The new-password step** asks for the new password and its repetition:
  - the page checks first and posts nothing for:
    - two different entries: „Die beiden Eingaben stimmen nicht überein.“;
    - a broken password rule: „Das Passwort braucht mindestens 8 Zeichen, darunter eine Zahl und
      ein Sonderzeichen.“;
    - the issued password repeated: „Das neue Passwort muss sich vom bisherigen unterscheiden.“;
  - the rule is `refusalOfChoosing` (`domain`), the page's own copy of the service's
    `PasswordRule`: handbook §5 counts the page's check and the service's as two rules;
  - the button „Passwort speichern“ is disabled while the request is on its way and a second
    submit posts nothing;
  - both copies judge the NFC form of what was typed;
  - both count a code point of a Unicode punctuation (`P*`) or symbol (`S*`) category as the
    special character, so no space of any kind (no-break and zero-width ones included), control,
    lone surrogate or combining mark is one;
  - a parity table of twelve verdicts stands in `passwordRule.test.ts` and in the service's
    `PasswordRuleTest`;
  - otherwise it posts `/api/v1/login/new-password` with the name and the issued password of the
    first step and the new password;
  - the service's two refusals (400) show the same two texts, a 429 the waiting text;
  - a 401 goes back to the first step with „Name oder Passwort stimmt nicht.“;
  - `logged-in` navigates to the `next` the answer names;
  - the issued password lives only in the page's component state, never in the browser's storage
    or the address.
- **A server error, an answer the page cannot read or a request that never arrived** shows „Die
  Daten konnten nicht geladen werden. Bitte später erneut versuchen.“ (`LOAD_FAILED_MESSAGE`,
  `ui`, the text of the dashboard's failed load) in both steps and stays (decision 2026-10-25).
- **The return target is the service's rule**, not the page's: the page sends the `next` query
  parameter of its own address with both steps and navigates to the `next` the service answers
  ([build-run-deploy.md](build-run-deploy.md), "The login API"), so the rule exists once.

## The page to change the password

- The page opens at `/password` from the frame's „Passwort ändern“.
- It is headed „Passwort ändern“.
- It asks for „Bisheriges Passwort“, „Neues Passwort“ and „Neues Passwort wiederholen“:
  - the three labels are the page's own wording;
  - the wording matches the login page's.
- It saves with „Passwort speichern“.
- It runs the same page checks as the new-password step (`refusalOfChoosing`, with the current
  password as the one to differ from) and posts nothing for two different entries, a broken rule or
  the current password again; the texts are shared (`PASSWORD_REFUSAL_TEXTS`, `ui`).
- It posts `/api/v1/password` through `httpSessionApi`
  ([build-run-deploy.md](build-run-deploy.md), "The login API"):
  - a change leads to the overview; the service's new cookie keeps this browser logged in;
  - a 401 (the login ended) sends the browser to `/login?next=%2Fpassword` like any other page;
  - a 403 `no-account` (the console admin key's login) leads to the overview with no failure text,
    and so does opening `/password` when `/api/v1/me` says `canChangePassword: false`;
  - that leading away replaces the entry of `/password` in the history, so „Back“ is not trapped
    there;
  - the button „Passwort speichern“ is disabled while the request is on its way, and a second
    submit posts nothing;
  - a wrong current password says „Das bisherige Passwort stimmt nicht.“, the service's two other
    refusals show the texts above;
  - a 429 says „Zu viele Versuche. Bitte warte einen Moment.“ (`TOO_MANY_ATTEMPTS_MESSAGE`, `ui`,
    shared with the login page);
  - any other failure says „Die Daten konnten nicht geladen werden. Bitte später erneut
    versuchen.“

## The JSON API the SPA reads

Shape and field names decided 2026-08-04 (open-questions.md). The controllers live in
`rest/controller/api/`, the published contract types in `rest/controller/api/wire/`; it is the
service's only read surface.

| Route | Answers |
|-------|---------|
| `GET /api/v1/avatars` | Overview: one `AvatarSummary` (`avatar`, the four ledger sums `bankWithdrawn`, `bankDeposited`, `storageWithdrawn`, `storageDeposited`, the derived `net`, the two flows `donation` and `craftSubsidy`, `balance` (the figure before the guild's share), plus `lastBankActivity`, `lastStorageActivity`, `staleSumsFrom` and the boolean `active`) per avatar **known to either ledger** (`KnownAvatars`, so a member who only ever moved items is listed too, with zero gold), sorted by **German collation** (`Ärger` before `Zorn`, the order the SPA's own text sorting uses); `totalCount` counts that union. |
| `GET /api/v1/avatars/{avatar}/bank` | That avatar's bank entries, newest first unless `sort` names another order. |
| `GET /api/v1/avatars/{avatar}/storage` | That avatar's storage entries, newest first unless `sort` names another order. |
| `GET /api/v1/admin/accounts` | Behind the admin role like the status; its shape is in build-run-deploy.md, "HTTP endpoints" (`accountsFrom` reads it; the page is under the views above). |
| `GET /api/v1/admin/status` | Behind a login with the admin role (a member's login is answered 403, which the admin page shows as „Diese Seite ist dem Admin vorbehalten.“): `lastUpdated`, `lastSuccessfulScrape`, `lastScrapeFailure`, `lastSuccessfulRecompute`, `lastRecomputeFailure`, `unknownItemNames`, `failedAvatarNames`, `roundTrips`, `roundTripAbstentions`, every key always rendered (the two round-trip arrays as `[]` rather than `null` when empty). The operator-facing facts; see below. |

- **One envelope for every collection**: `page`, `size`, `totalCount`, `items`. `/api/v1/avatars`
  adds `totals`.
- **`totals` sums every known avatar, not the served page** (decision 2026-09-02), the reading
  `totalCount` already has. It carries the row's numbers plus `storageValue` (below), so the SPA renders its total
  row without arithmetic of its own, and neither paging nor a later time window can turn a guild
  total into a page total behind the reader's back. Its last field `containsStaleSums` is over the
  same union, so the total row still states that it contains a stale row when the served page does
  not show that row.
- **`staleSumsFrom` is `null` unless the row's sums are older than the last collection**, and then it
  is the instant they were last recomputed, or, for a failed avatar whose stored sums carried no
  instant, the seeded bound his sums predate (domain-model.md). One nullable field rather than a flag beside a
  timestamp: present means both "stale" and "this old". The comparison happens **server-side**,
  against the newest per-avatar recompute instant in the meta store, so this envelope carries no
  guild-wide collection timestamp; `/api/v1/admin/status` serves it as `lastUpdated`. A row a recompute failure skipped therefore states its own age, while
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
  (`sumsFrom` in `api/wire.ts`).
  - It refuses one with only some of the sums absent as a malformed answer.
  - It refuses one without the sums but with a flow, `balance` or `storageValue` as a malformed
    answer (`refuseFiguresWithoutSums`).
  - It therefore refuses a page whose row carries no sums while its `totals` still do.
  - It therefore refuses a page that holds the whole guild with every row computed while its
    `totals` carry none.
  - An age beside absent sums is tolerated rather than refused, because the not-yet-computed mark
    wins over the outdated one anyway.
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
  2026-09-02).
- **`active` is the service's own mark of the 30-day line** (`ActivityLine`, over every avatar the
  ledgers name, not over the served page): `true` when the later of the avatar's two last
  activities is at or after 30 calendar days on the Berlin clock before the newest activity of any
  avatar, and `true` for every avatar while the ledgers hold no activity at all:
  - The reference is the data's, never the reader's clock: tied to the wall clock the whole roster
    falls into "dormant" as soon as the scraper stalls, which is exactly when someone opens the
    page, whereas the row holding the maximum is active by construction.
  - With no activity anywhere there is no reference and every row stays active: the dormant table
    is an exception list, and nothing moves into it without a measurable reason.
  - The window is 30 calendar days rather than a calendar month, because subtracting a month
    normalizes 31.07 to 01.07 and would change the window's length with the reference date.
  - `lastUpdated` is deliberately **not** read for this: it lives on `/api/v1/admin/status`, and the
    members' view does not reach into an operator surface for a presentation question.
- **`/api/v1/admin/status`'s `lastUpdated: null`** means no collection run has completed. A sentinel
  instant is not an option, because of how Java 25 converts an extreme instant
  ([dev-environment.md](dev-environment.md), "JDK version (upgrade procedure)").
- **`/api/v1/admin/status`'s `lastUpdated` is the recompute's instant cut to the minute**, also
  inside the hour the clocks go back.
  - After a release before `V4` recomputed in the second pass of that hour, the start fills it with
    the first pass ([domain-model.md](domain-model.md)).
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
- **A deep link opened without a login is redirected to the login page.** What the service answers:
  [build-run-deploy.md](build-run-deploy.md), "HTTP endpoints".
- **404 vs. empty page** (author decision 2026-08-05): a **404 means the avatar is unknown**, i.e. has
  no row in either ledger. A known avatar whose bank or storage ledger happens to be empty answers 200
  with `totalCount: 0` and `items: []`, like the overview does, so the SPA can tell "no storage
  activity" from "no such member" without a second request. A page past the last entry is likewise a
  valid empty window. "Known" means **the avatar has a ledger row somewhere**, deliberately not "the
  meta information mentions it": today the evaluator only writes meta keys for avatars that have rows,
  so the two coincide, and pinning the contract to the ledgers keeps it true if that ever diverges.
- **Errors carry no envelope**: 401 (no valid login) and 404 answer with an empty body;
  400 (a paging constraint violated) and 405 answer with Micronaut's own JSON error shape. The SPA
  codes against the status, not against a body.
- Both defaults that would silently break this are pinned in `application.yml`
  (`jackson.serialization-inclusion: ALWAYS`, `write-dates-as-timestamps: false`).

## Build integration

- **Node is pinned centrally**: `gradle.properties` → `nodeVersion` holds the version. The
  `com.github.node-gradle.node` plugin downloads that version per machine
  (`node { version = providers.gradleProperty("nodeVersion").get(); download = true }`); no Node
  install required on host or devcontainer (see `dev-environment.md`).
- **`npm ci`** (`npmInstallCommand = "ci"`), not `npm install`, so the build always uses the locked
  `package-lock.json` versions.
- Three Gradle tasks wrap the npm scripts, each with explicit `inputs`/`outputs` for
  skip-when-unchanged (`UP-TO-DATE`/`FROM-CACHE`):
  - `npmBuild` (`vite build` of the dashboard into `frontend/build/dist`, then `vite build --config
    vite.login.config.ts` of the login page into `frontend/build/dist/login`); wired into
    `assemble`.
  - `npmTest` (`vitest run`) and `npmLint` (`eslint` + `prettier --check`); wired into `check`.
- **`npmTest` never runs while the Java suite runs** (`mustRunAfter(":test")`).
  - Why: almost every test file boots its own jsdom, and beside Gradle's parallel `:test` the fork
    pool starves, so whole files never start ("Failed to start forks worker") while no test fails,
    and the build goes red under load.
  - The Vitest worker fan-out is bounded too (`test.maxWorkers: 2`), as a second net.
- **Dev server against a running application:** `npm run dev` serves the SPA on 5173 and proxies
  `/api` to `http://localhost:8080` (`server.proxy` in `vite.config.ts`), so the SPA can be driven
  against real data with hot reload. Deep links work there because Vite answers unknown paths with
  `index.html`, and in the packaged application because no controller owns the dashboard paths or
  `/explanation`, so the history fallback answers them with the shell.
- Run `vitest`/`eslint` from `frontend/`: the Vitest config (jsdom environment) lives in
  `frontend/vite.config.ts`, and a run started from the repo root silently uses none of it.
- The built SPA reaches the main jar via a **`frontendDist` Gradle configuration**:
  - `:frontend` exposes `build/dist` as a consumable `frontendDist` artifact; the root project
    declares a matching resolvable configuration and copies it into `processResources` under
    `static/ui`.
  - Micronaut serves the SPA as static resources, with a catch-all fallback to `index.html` for
    unknown navigation paths (client-side routing).
  - The client routes (`/overview`, `/avatars/{avatar}/bank`, `/avatars/{avatar}/storage`,
    `/explanation`, `/password`, `/admin`) are the SPA's.
- **The login page is a second bundle** (`login.html`, `src/login/main.tsx`,
  `vite.login.config.ts`):
  - it is built with the base `/login/` into `build/dist/login`, so the jar carries it under
    `static/ui/login/` and its files load from `/login/assets/…`;
  - it is a build of its own, so it shares no chunk with the dashboard: its files can be public
    while the dashboard's need not be;
  - the public paths list `/login` and `/login/**`, and no file of the dashboard's bundle;
  - `LoginPageController` serves `login.html` at `/login` and `/login/`; `SpaNavigationPaths`
    reserves `/login`, so the dashboard's fallback never answers below it.
- **Docker**: the image build gates on the frontend's tests and lint too; its build command:
  [build-run-deploy.md](build-run-deploy.md), "Run via Docker (primary path)".
- **Vulnerability scanning**:
  - `:frontend` has its own `vulnScan` task, which the root `vulnScan` depends on.
  - Its command, its gating property and why it stays out of `build`/`check`:
    [build-run-deploy.md](build-run-deploy.md), "Build", the bullet "Dependency vulnerability
    scan, Trivy (on-demand)".
  - Dependabot also watches `frontend/` (`npm` ecosystem) alongside the root `devcontainers` ecosystem.
