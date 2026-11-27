# 09: Engineering Handbook (how the code should look & how we develop)

The standard every change is held to; keeps the project clean and contributors (human or AI)
consistent. Summary in [`/CLAUDE.md`](../../CLAUDE.md).

## 1. Architecture rules (hexagonal)

**Dependency rule:** dependencies point inward; inner layers never import outer ones.

```
domain        : entities, value objects, domain logic (EvergoreItem, Entry, TransferType). NO framework.
application   : use cases (collect, evaluate, query) + PORT interfaces they need. NO framework.
adapters/in   : REST controllers/filters, the scheduled job (drive the application)
adapters/out  : Selenium (PageSource), ORMLite repos, file, logging (implement the ports)
config        : Micronaut @Factory wiring + @ConfigurationProperties
```

- `domain`, `businessLogic`, `application` never import `io.micronaut`, `selenium`, `ormlite`, or
  any adapter; enforced by `HexagonalArchitectureTest`. Keep it true.
- Every outbound dependency goes through a **port** (interface owned by the inner layer): new
  external integration ⇒ port first, then adapter.
- **A cross-cutting guarantee holds by default** (examples: access control, the thread pool a
  request runs on): a new unit is covered the moment it exists, with no annotation or list to
  remember.
  - A missing or empty configuration of it fails closed (the public paths' instance:
    [architecture.md](architecture.md), "Layers & responsibilities (condensed)").
  - Why: a guarantee each unit must opt into is missing exactly where someone forgot it.
- **"NO framework" means no application or hosting framework** in the inner layers.
  - A small, well-encapsulated library is allowed there.
  - Such a library sits behind one wrapper that exposes the domain's own types.
  - `HexagonalArchitectureTest` names that wrapper as the only place that may import the library.
  - A library that does no I/O is no outbound dependency, and therefore needs no port and adapter.
  - Why: the wrapper keeps the library's types out of the domain.
- Only the composition root (`ApplicationFactory` / `config`) knows concrete classes.

## 2. SOLID, applied here

- **S**: one reason to change per class (parsing ≠ valuing ≠ persisting). Split `SeleniumPageSource`'s
  scraping from driver lifecycle.
- **O/L**: extend via `TransferTypeVisitor` and similar, not by editing `switch`/`instanceof`.
- **I**: small, role-specific ports (`BankRepository`, `StorageRepository`, … not one fat repo).
- **D**: application code depends on ports; adapters depend on the domain, never the reverse.

## 3. Clean-code rules

- Names:
  - Intention-revealing.
  - No non-domain abbreviations.
  - No joke or placeholder names.
- Code stands on its own: understandable without backlog, tickets, chat, or design notes; name
  things by what they are or do (domain/capability role).
- Names never reference backlog/ticket IDs or the producing task: `ProtocolEvaluationAcceptanceTest`,
  not `D2AcceptanceTest`.
- Durable docs (KB, decisions log, `process-learnings.md`) follow the same rule for backlog
  shortcodes: the KB README's DOC checklist holds it (DOC-8) and the removal of every reference
  when an item completes (DOC-5) ([README.md](README.md), "Doc conventions").
- Small methods, early returns, no deep nesting; no commented-out code in commits.
- **Strict data instead of defensive reads (author rule 2026-09-06).** Every database column is
  `NOT NULL`.
  - A value that cannot be absent is declared so at the type, not checked at each read.
  - Read paths therefore carry no null branches for states the schema forbids.
  - Whoever wants to allow a nullable column owns the handling of that null everywhere it can
    surface, and says so.
  - Why: ORMLite reads a SQL `NULL` into a primitive `int` as **0**, so a nullable `amount` would
    silently change a member's sums.
- **Every stored time is a UTC instant.**
  - No code reads the host's default time zone.
  - The presentation applies the zone its readers see (`Constants.APP_ZONE`).
  - Why: a wall-clock value without its zone is ambiguous inside the hour the clocks go back.
  - A bound in days counts calendar days in that zone, not multiples of 24 hours.
    - Why: a clock change must not move a moment across the bound.
  - This project's instances: [architecture.md](architecture.md), "Layers & responsibilities
    (condensed)".
- **A paged read is in a total order:** its sort ends on a unique key (the ledger page ends on the
  row `id`: [architecture.md](architecture.md), "Layers & responsibilities (condensed)").
  - Why: a page boundary inside a run of ties neither repeats a row nor drops one.
- **A name a request supplies (a sort column, a direction) reaches a query only through an
  allowlist** that maps it to the code's own constant (the ledgers' sort keys and `columnOf`).
  - An unknown name is refused.
  - Why: no column name from a request reaches the SQL.
- **A failed read is no value:** a script checks each read's own status before it compares or uses
  the result (the deploy's instance: [build-run-deploy.md](build-run-deploy.md), "Deploy to the
  home server").
  - Why: two failed reads compare equal.
- **A rule lives in one place.**
  - A copy a client cannot do without (a check the SPA makes before the service makes it again) is
    held to the original by a parity table: the same cases with the same verdicts, run in both
    suites (the password rule's instance: [frontend.md](frontend.md), "The login page").
  - Each copy keeps its own scenario (§5, "One rule, stated once across the suite").
  - Why: a second copy drifts, and only a shared table of cases shows the drift.
- **No logic in constructors; field assignment only** (no IO, no `init()`-style calls);
  construct + initialize via static **factory method** (`SqliteDatabase.open(...)`, Effective
  Java Item 1) or a lifecycle hook. Constructor logic breaks testability and SRP.
- **`static` is a smell, mutable static state above all**, since it couples instances and tests
  through state no signature shows.
  - Prefer DI, instance state, a proper seam.
  - Boot tests sharing bean-written state use an injected DI-shared recorder: a `@Singleton` bean
    from a test `@Factory` (not the GoF static-`getInstance` antipattern), never static flags.
  - The seam (`BootSignalRecorder`) and why `@TestInstance(PER_CLASS)` is no alternative:
    [testing.md](testing.md), "Boot-signal seam".
  - Defensible only as a static initializer for setup that must precede framework boot (e.g.
    seeding/deleting a test DB), in **test** code; in production, justify hard.
  - A costly run-scoped test resource is the other defensible case in test code, under the rules
    of [testing.md](testing.md), "The acceptance runner".
- **Comments and doc comments are an absolute no-go; make the code say it** (author decision
  2026-08-04). No `//`, no `/* */`, no doc-comment block (Javadoc, JSDoc) on classes, methods,
  records or fields, and none in config or infrastructure. Non-obvious knowledge goes into an
  expressive **name**, a **test name**, the KB, or [`open-questions.md`](../open-questions.md),
  never into a comment and never into both. Where the *why* is a real constraint, restructure so
  the code carries it rather than annotating it.
  - Reviewers (human or agent) flag **every** comment a diff adds, not only the ones they judge
    unnecessary, since "is this one necessary?" is a judgment call that lets comments through
    review.
  - It is enforced mechanically, not by memory: `hooks/content-gate` refuses any diff that adds a
    comment line to a listed source extension (`.java`, `.kts`, `.ts` and `.tsx` among them) or to
    a shell script recognised by its shebang; the lists are the gate's config block
    ([`hooks/README.md`](../../hooks/README.md), "The checks").
  - **There is no opt-out.**
  - What the gate refuses is deleted, not reworded; the existing stock stands until a commit edits
    it.
  - Carve-out: every tooling directory with its own self-test keeps its comments as prose by design.
    - `hooks/` sits outside the comment scan, since it holds the gate's patterns themselves.
    - content-gate's list `comment_exempt_dirs` names the others: the shipped `stacks/` and
      `agent/`, and the project's `deploy/` and `release/` (author decision 2026-11-23).
    <!-- MODULE gate -->
    - With the gate module, `gate/` is one of them.
    <!-- /MODULE gate -->
    <!-- MODULE agent-seat -->
    - With the agent-seat module, `land/` is one of them.
    <!-- /MODULE agent-seat -->
  - The one pass the gate gives prose is an `ADAPT` note: exactly a `# > **ADAPT:**` line and the
    `# >` lines that continue it, in every scanned file.
    - A plain `# ADAPT:` line is refused like any comment.
  - YAML (`.yml`, `.yaml`) is scanned like code, so a config comment is refused there too.
  - A further file family joins the scan through the lists in content-gate's ADAPT block.
  - The ignore files carry prose by design and sit outside the scanned extensions.
- **No dead code, no undeclared dependencies** (an unused class importing a library absent from
  `build.gradle.kts`: delete it).
- **Code/build is the single source of truth; docs never duplicate volatile facts.** Versions
  (Java/Micronaut/Gradle), dependency lists, file/test counts live in `build.gradle.kts` and the
  code; docs point, never restate (restated facts rot silently). Doc contradicts build ⇒ build
  wins, doc fixed in the same change.
  - A count over the suite or a code path (its scenarios, or the calls, reads or steps a path
    makes) is such a fact too: the KB describes the suite or the path without it.
- **Immutable `record`s over getter/setter boilerplate** for value objects, DTOs, config
  (`BankEntry`, `StorageEntry`).
  - DTO public fields are fine.
  - Record accessors (`config.publicPaths()`, `BankEntry.amount()`) are not bean getters.
  - No `getX()`/`setX()` unless a framework strictly requires it (Micronaut binds
    `@ConfigurationProperties` to a record with no extra annotation).
  - Return `Optional`, not sentinels.
- **`Logger` is the last constructor parameter**: collaborators first, incidental infrastructure last.
- Constants/config over magic values; **no secrets in source** (token, credentials → config).
- **Warnings are errors**: fix, don't suppress.
  - The compiler flags, the two deliberate exclusions and the probe compile's exception:
    [build-run-deploy.md](build-run-deploy.md), "Build".
  - Unsure fix vs. exclude ⇒ ask the author.
  - Where a rule flags code that is right on purpose, restructure the code so it states that
    intent, rather than disabling the rule for the line.
    - Why: a disable for the line silences every later finding of that line too.

## 4. TDD workflow (red → green → refactor → commit)

Runs **inside** the approved `@wip` scenarios of a feature (§5): the scenarios state what "done"
means, the TDD cycles drive them green, step definitions included.

1. **Red**: smallest failing test expressing the next behavior. Run it and **watch it fail for the
   right reason**; a slow full build is no licence to skip red, run the *focused* test. **The red
   run is evidence**: the failing test's name and its failure line go into the step's report (or
   the plan) before the implementation exists; a mutation of the finished code is no red run. A
   test written beside its implementation is anchored to that implementation's shape, which is what
   produces assertions fitted to the data.
2. **Green**: simplest code that passes.
3. **Refactor**: improve design with tests green.
4. **Commit**: one full cycle = one commit, via the §7 protocol (the agent words the message and
   commits; the cycle never pushes). Red and green stay uncommitted, local steps inside the cycle;
   only the refactored, green result is committed, so every commit is atomic and independently
   revertable.

- Domain/application logic is unit-tested **without the framework** (plain JUnit + AssertJ, fast).
  The inner loop runs `./verify focus <path>`; the feature's scenarios run through `./verify bdd`,
  and the last cycle of a feature arms them (§5).
- One behavior per test; arrange/act/assert; table-driven where inputs vary (parser, item values).
  When a class's tests repeat the same act, wrap it in a short-named helper (e.g. `snapshot()`) so
  the call itself reads as the Act, not an expression buried inside each assertion.
- Bug fix ⇒ reproducing test first, then fix (e.g. `EvergoreDataEvaluatorTest`'s
  `resolvesErdeEibenlanzeByItsRealIngameSpelling`, which pins the catalog name the game spells
  Erde-Eibenlanze).
- **A new test that drives new code is always seen red**, before the production edit it needs:
  - the step's report carries its name and failure line before each production edit;
  - a phase is one implementer run over a slice of the approved plan, and its brief lists the
    cycles one test at a time;
  - before the orchestrator accepts a phase or a fold, it compares the phase's new test names with
    the red lines of its report.
  <!-- MODULE gate -->
  - With the gate module the red run is a `red/` record in the strand's gate dossier, written before
    the production edit.
  - `gate/dossier-check --phase <worktree>` then takes the place of the name compare: the
    implementer runs it before handing back a phase or a fold, the orchestrator after each phase.
  <!-- /MODULE gate -->
  - Why: a missing red run then stops the phase instead of being made up for at the gate.
- **A production line written before the red test that needs it is a FAIL of the round**
  (author decision 2026-10-17): the line comes out, the test is written and seen red, then the
  line goes back in.
  - A recorded mutation never stands in for that red run; it proves only an adapted test or a
    test that pins behavior the code already has.
  - Why: a test never seen red can pass falsely.
- **An existing test adapted to a changed behavior** cannot be seen red first; it pays that debt
  with a **recorded mutation**:
  - mutate the production code so the adapted assertion must fail;
  - run only that test class (`./verify focus <path>`) and keep the output;
  - revert the mutation;
    - in a file that holds other uncommitted work, from a saved copy of that file, never from git;
      - Why: a revert from git also discards the other uncommitted work in that file.
  - the record (what was mutated, the failing line) goes into the step's report like red evidence.
- **A new test that pins behavior the code already has** cannot start red either: one a panel asks
  for, the step definitions of a catch-up (§5), a test that pins code before it changes, the pass
  case of a guard.
  - It pays with a recorded mutation per `Then` step or per case, as above.
  - The record goes into the step's report and the gate brief; the mutation stays in the gate
    dossier (playbook, `mutation/`).
  - No row records it (author decision 2026-10-13).
    - Why: the test itself is the durable artifact, and the record lets the gate re-run the
      mutation.
  <!-- MODULE gate -->
  - With the gate module the record is a `mutation/` record that holds an `older: <reason>` line.
    - The check counts its failing lines for a new test only with that line.
    - The reviewer judges the reason as it judges an `exempt:` claim.
  <!-- /MODULE gate -->
  - Where a harness exists, this and the adapted test are the only substitutes for a red run.
- Where a deliverable genuinely has no test harness the stack supports (the deploy shell script),
  the substitution is **mutation testing** and it is *declared*: name it and every mutation in
  [`open-questions.md`](../open-questions.md), "Recorded mutations", in the same change, so a later
  gate re-runs them instead of taking a claim on trust. A mutation that lives only in a conversation
  is not a proof.
  - The commit that replaces the code a recorded mutation pins re-proves the mutation on the new
    code or strikes its row.
    - Why: a row on code that is gone claims a proof no gate can re-run.
- A reviewer (human or agent) may gate each commit on process + criteria adherence (§9).

## 5. BDD (product-owner perspective): mandatory, and gated by the author

**BDD is not optional in this process** (the template's rule). Work on observable behavior starts
with executable Gherkin scenarios, the author confirms them, and only then does implementation
begin. The scenarios, not the plan text, are what "done" means, and they stay in the repository as
the executable specification.

**Status in this project (author decision 2026-09-27): in force for every strand.** Every
scenario of the catch-up (`/bdd-catch-up`) is armed and run by
`./verify all`; a scenario carries `@wip` only while its feature is being built. Work with
observable behavior starts with its scenarios; pure refactoring, `[doc]` and build/infra claim the
exemption below explicitly.

**Scope.**

- Every feature with behavior a user, an operator or a caller can observe: new capabilities, API
  and UI changes.
- A bug fix gets a scenario when the bug is visible at the acceptance boundary.
- Otherwise a reproducing unit test (§4) is the bug fix's specification.
- A change inside the scope of an existing scenario adjusts that scenario.
- **Exempt**: pure refactorings that change no behavior, `[doc]` and process commits, and
  build/infra work.
- The exemption is *claimed explicitly* by the planner and named in the plan; it is never the
  silent default.

**The artifact: `.feature` files, executed.** Scenarios are Gherkin (`Feature`, `Rule`, `Scenario`,
`Scenario Outline`) bound to step definitions and run by **cucumber-jvm** on the JUnit platform
([testing.md](testing.md) names the suite class, the tag filter and the glue package). They are the
**primary review artifact between author and agents**, written in **product language** from its
stakeholder's view (below), understandable without reading code:

> *(example)*
>
> ```gherkin
> Feature: Guild contribution overview
>   Scenario: Gold deposited in the guild bank counts toward a member's contribution
>     Given the guild bank ledger holds:
>       | Zeitpunkt        | Avatar | Betrag | Vorgang    |
>       | 01.01.2026 12:00 | Bambor | 100    | Einzahlung |
>     And the daily collection has run
>     When a member opens the overview
>     Then Bambor's "Bank-Einzahlung" is 100
> ```

- **Declarative, never imperative.** A step names a domain fact or outcome ("the entry is
  booked"), never a UI gesture ("click Save"); the step definition owns the *how*. Technical
  vocabulary (type names, hashes, endpoints) is a finding wherever the scenario's actor would not
  see it ("One actor per scenario", below).
- **One acceptance runner for the system:** cucumber-jvm drives the whole system, backend and SPA;
  the frontend keeps its Vitest unit tests only.
- **A page of text keeps the scenarios a product owner asks for** (author decision 2026-10-15, on
  the explanation page): that it exists and is reached, its headings and structure, and what it
  must never show.
  - Its words are the text the author approved, pinned 1:1 by a test against an approved file.
  - A figure it states is recomputed by a test of its own.
  - Why: the approved file shows every change of wording in the review, while scenarios on the
    wording would hold the gate on tightenings a reader judges better.
- **Feature files live in `src/test/resources/features/`**, one place for the system, one file per
  capability. A file over about ten scenarios is a split signal; `Rule:` groups the scenarios of one
  business rule and `Scenario Outline` carries the variants of one behavior.
- **English**, with the game's German item and transfer names quoted as the game spells them
  ([open-questions.md](../open-questions.md), 2026-09-20); code, step definitions and docs stay
  English.
- **Step definitions are test code** and follow every test rule (§6; no comments; no logic that
  belongs in the domain). Reuse an existing step phrasing before inventing one; the runner's dry run
  lists the undefined steps, and that list is the glue work before TDD starts.
- **Step definitions drive each actor the way that actor meets the system** (author decision
  2026-09-23, [open-questions.md](../open-questions.md)): the member and the admin through the
  real SPA bundle in a real browser, the operator over HTTP and JSON. Behind the application
  boundary stand **test-double fakes** (a fake `PageSource`, a throwaway SQLite file), never the
  live site. Many rules live only in the SPA, and only a browser-driven step proves what the member
  sees.
  - Each scenario gets a fresh real service ([testing.md](testing.md), "The acceptance runner").

**Reading as the stakeholder** (author decisions 2026-09-23 and 2026-09-27,
[open-questions.md](../open-questions.md)). A non-technical product owner must be able to judge
every scenario without asking a developer:

- **One actor per scenario**: "a member", "the admin" or "the operator", named as the one who acts
  or reads (`When a member opens the overview`).
  - A trigger with no person behind it (the daily collection, a client's request) does not replace
    the actor who reads its outcome.
  - So a scenario the throttle or a refused address answers names the operator in its `Then`
    (`Then the operator finds the client turned away for sending too many requests`).
  - A `Then` asserts only what that actor sees or knows on their own surface.
  - The member and the admin see it through the UI; the operator through the health check (REST
    and JSON), the startup, the throttle, the log, the service's answer to a request and what it asks
    the browser to keep.
  - An address, a status code or an internal name never stands in a member's or an admin's
    scenario; it belongs to the step definitions and the unit tests.
  - A description sentence starting "Today" may quote word for word what the actor sees today,
    technical words included; the `Then` never does.
  - Why: an outcome the actor cannot see is one the product owner cannot check.
- **One rule, stated once across the suite.**
  - A rule counts wherever a scenario asserts it, in a second `When`/`Then` pair too, not only
    where a title names it.
  - Two scenarios asserting the same rule for the same stakeholder with different data become one
    scenario, or one is dropped.
  - The same rule on two surfaces (the admin page and the health report) is one
    `Scenario Outline` over the surface, each example row naming its own actor.
  - A check the member's page makes and the service makes again, for a client that skips the page,
    is two rules: each keeps its own scenario.
  - Different stakeholders seeing different consequences of one event (the game cannot be reached)
    stay apart, each asserting only what its stakeholder needs.
  - Why: duplication drifts, in a suite as in the docs.
- **Every outcome reads on its own**: a `Then` names the state it expects ("in descending order"),
  never one the reader must carry over from an earlier step ("again").
  - Why: a carried-over state makes the reader replay the scenario to judge one line.
- **Placeable time**: the "Points in time" rules below (TIME-1 to TIME-9).
- **Checkable numbers**: an expected figure follows by mental arithmetic from the scenario's data
  and the prices `valuation/` states, where alone the price-list step stands (quantity 1, round
  amounts; gold where the price is incidental).

**Points in time (author decision 2026-09-27): one calendar.** A reader places every moment
without arithmetic, because the value itself tells its role; the scenario gate checks each rule on
the scenario text alone.

- **TIME-1 One year:** every date is in 2026. A date the product shows as fixed text, such as the
  first protocol's date on the explanation page, is quoted as it stands and is no moment of the
  scenario.
- **TIME-2 One month:** dates fall in January, from 01.01. on, in the order things happen. Three
  exceptions, each only where the claim needs it: the newest movement moves to 15.02. when
  something must lie clearly outside a 30-day window or when a later collection must read a
  movement after a newest one of 31.01., 15.07. stands for summer time, and 31.12. is a "now"
  long after every movement.
  - Why: dates in one month compare at a glance.
- **TIME-3 Windows start on a round moment:** the 30 days run from 01.01. to 31.01., the 48 hours
  from 01.01. 12:00 to 03.01. 12:00. January holds no clock change, so a window is its plain length.
- **TIME-4 The time of day tells the role:** a movement at 12:00; the daily collection at 05:00 on
  the morning after the movements it reads. Another time only where the claim rests on the minute:
  a window's edge and one minute past it (11:59 beside 12:00), two movements in one minute, and
  01.01. 00:00 for a movement that must lie outside a window ending on 31.01. Steps that must fall
  inside one 48-hour window without testing its edge take consecutive hours of 01.01. (12:00,
  13:00, 14:00). The item lines under one headline of the game's protocol are one entry, not two
  movements in one minute.
- **TIME-5 "Now" only where the claim depends on it:** the clocks are set once, in the first
  `Given` of the scenario or its Background, and nowhere else.
  - Why: a clock set later shifts moments the reader has already placed.
- **TIME-6 Oldest first:** steps and ledger rows run in the order the moments happen, unless the
  claim is about the order in which the game delivers them; the game's protocol text is written
  newest first, as the game delivers it.
- **TIME-7 A displayed time repeats its source:** a `Then` that shows a date shows the movement's or
  the run's own value, and a UTC form is converted for that date's season (05:00 in January is
  04:00Z).
- **TIME-8 One keyword per time step:** cucumber-jvm binds a step without its keyword, so the tense
  carries the role: the past (`ran at`, `has run`, `has been restarted since`) is a precondition
  under `Given`, the present (`runs`) is the action under `When`, and a new time step takes one of
  the two and never appears under the other. A lasting state is no event: it stands under `Given` in the
  present (`the clocks read 31.12.2026 12:00`).
  - Why: cucumber-jvm runs a precondition reused as an action without complaint.
- **TIME-9 A moment that is neither a movement nor a run is placed by an event:** a login or a
  request stands relative to an event its step names (`7 days after logging in`, `9 seconds after
  its first`), never at a clock time of its own.
  - Where a login must still hold when a later collection runs, the 30-day window may end on the
    day before the newest movement that collection reads (30.01. before 31.01.), since a login
    lasts 7 days.

**Completeness (what the author confirms).** The planner declares the scenario set to be the
*whole* acceptance of the feature, checked against this list, and names what is deliberately out of
scope:

- the happy path;
- every business rule and each of its variants (`Rule:`, `Scenario Outline`);
- the error, empty and boundary cases a stakeholder would name;
- for a user-visible feature, the rules the SPA applies on its own beside those the server computes.

An implementation that later needs a behavior no scenario names has found an incomplete set; the
set goes back to the author before that behavior is written.

**The flow (mandatory order):**

1. **Draft the `.feature` up-front**, before any production code, and run it through the
   **scenario gate**: `falsifier-scenario` attacks it as a specification against its ticket
   (ambiguity, tautology, untestable steps, coverage), `falsifier-stakeholder` reads it as its
   stakeholder and against every `.feature` file (the rules above), the `reviewer` in scenario mode
   gates on this section with both reports; fix and re-gate (how a re-gate runs, when it passes and
   when it ends: §9).
2. **► Author gate 1: the author confirms the scenarios and their completeness.** A real gate, not
   a notification: implementation does not start until the author has read the scenarios and said
   yes to the set. Wrong scenarios are cheap here and expensive after three TDD cycles have been
   built on them. The scenario gate's open lows arrive unfolded (§9).
3. **Commit the confirmed scenarios tagged `@wip`**: the default run excludes `@wip`
   (`junit-platform.properties`), so the build stays green while intent is captured. The tag stays
   committed until the arming commit (step 5).
   - The tag sits on the level of what the strand adds or changes:
     - a new feature file carries it on its `Feature`, and so does a rewrite of a whole feature;
     - a unit the strand adds to or changes in an armed feature carries it on its own `Rule`,
       `Scenario` or `Examples` block.
     - Why: the tag then excludes only what is not built yet, and the rest stays armed.
   - In a feature that is already armed:
     - a scenario whose behavior the strand removes stays armed until the commit that removes the
       behavior, and leaves with it;
       - Why: until then it guards behavior that still runs.
     - a description sentence changes in the commit that delivers what it describes.
       - Why: an earlier change describes behavior the code does not have yet.
4. **Drive them green with §4 TDD cycles**: step definitions and production code, one committed
   cycle at a time. The scenarios run through `./verify bdd` (every `@wip` scenario) or
   `./verify focus <feature file>`; removing the tag locally to run them is a fallback and is never
   committed before the arming commit. A scenario that turns out to be wrong goes **back to the
   author** as a changed scenario; it is never quietly reinterpreted to match what the code now does.
5. **Arm the scenarios** in the last TDD cycle of step 4: that commit also removes `@wip`, and no
   commit of its own only removes the tag (author decision 2026-10-25). From here `./verify all`
   runs them on every commit and at every gate.
   - A unit tagged on its own level is armed the same way, by the commit that turns its last
     scenario green.
   - Why: a commit that only removes a tag carries no change worth reviewing alone.
6. **Refactor the feature** under the armed net: improve the design across the feature with the
   scenarios and unit tests green, one green commit per step, behavior unchanged. A change that
   needs new behavior or a new port is not a refactoring; it goes back to planning.
7. **The panel and the review gate** (§9, the playbook), then **author gate 2**, the review of the
   rebased tip, then the landing (§7).

**Projects that predate their scenarios.** An existing codebase gets its scenarios before any
feature work, through the `/bdd-catch-up` skill (author decision 2026-11-23):

- the code is the truth for what *is*; the author decides what *should be*;
- a run derives one capability's scenarios from the code, as text only, and the scenario gate
  gates them;
- the author then judges each scenario:
  - a confirmed one is committed `@wip`, awaiting its steps;
  - an unclear one carries `@characterization` until the author answers, and `./verify all`
    excludes that tag like `@wip`;
  - a rejected one becomes a bug in the backlog plus the corrected scenario tagged `@wip`, whose
    "Today" sentence records the behavior it replaces;
- once every cluster (a group of related capability files, such as a feature folder) is gated, a
  closing run condenses the suite: the stakeholder lens reads it whole and proposes merges, and
  the author confirms every changed claim;
- then each cluster's step definitions are a strand of their own, with a plan go, TDD cycles and
  the gate; their tests pin behavior older than the strand (§4).

## 6. Test strategy (pyramid)

- **Many** fast unit tests (domain values, parser, evaluator math).
- **Some** adapter/integration tests (repositories vs a throwaway SQLite file).
- **Few** acceptance tests: the armed scenarios (§5), each through the real surfaces against a
  fresh service, the collect→evaluate→overview flow via fakes, and a thin smoke test for wiring.
- **Deterministic, never wall-clock-dependent**: must pass on any hardware (a first-gen Raspberry
  Pi may just take longer).
  - Correctness never hinges on a `sleep`/`timeout` threshold.
  - Wait on a real signal (`CountDownLatch.await()` without timeout, a future, a condition).
  - Release the signal on both success and failure paths, so a bad run fails an assertion instead
    of hanging.
  - Timeout/retry is mitigation, not fix; never "stabilise" a flake by bumping one
    ([testing.md](testing.md), boot-signal seam).
  - **Exception: a test that must wait on real elapsed time** (the suite's two instances:
    `ReadWaitsOutAWritersLockTest`, and the hang guard of the held answer in `GameProtocols`).
    - The plan's test list names it, or a repair brings it;
    - the author decides it before its red.
    - A dated decision row in [`open-questions.md`](../open-questions.md) records it, and so does
      its entry in [testing.md](testing.md).
    - One exception may cover a class of such waits the project names, recorded once the same
      way: one decision row and one entry in testing.md.
    - Its stated reason is its only licence.
    - A hang guard, a bounded wait that only stops a run that would otherwise hang, stands only
      inside such an exception and never decides the verdict.
    - Why: some behavior shows only against real time, such as a read that waits out a lock
      another connection holds; deciding each case by name keeps the rule strict elsewhere.
- **A production parameter that is slow on purpose is a setting whose default is the production
  value** (the password hash's cost, pinned by `PasswordHashConfigurationTest`).
  - The test environment sets a cheap value.
  - A test of its own pins the production default.
  - Why: the full cost in every test would slow each run of the suite, and the pinned default
    keeps the cheap value out of production.
- **Arrange discipline**: shared, non-test-relevant preconditions (waiting for boot/collection,
  base-URL config) go in `@BeforeEach`, not each test body; a body holds only the arranges that
  make its act + assert legible. Exception: a precondition that *is* the behaviour under test
  (e.g. `SmokeTest.applicationIsStarting`) stays as the test's act/assert.
- **A test builds a value with several components of the same type by name, never by position.**
  - Where the language has named arguments, it uses them.
  - Otherwise a fixture builds the value, with one method per component and an empty default for
    each one it is not told.
  - Why: two same-typed values swapped by position still compile, and the test passes while it
    proves the wrong thing.
- **A test rule a linter can express is enforced by the linter in the build, not by review alone.**
  - Why: review alone let an act inside the assertion through three times, the last after five
    panel rounds ([open-questions.md](../open-questions.md), 2026-10-15).
  - Until a linter enforces the act rule, each commit that adds or edits a test first greps the
    staged test files for an act inside an assertion (`expect(<call>(`, `expect(() =>`,
    `assertThat(<call>(`) and splits each hit into a named act.
- **A refusal test asserts its own reason**, not only that the run was refused.
  - A strand that adds a refusal earlier in a sequence re-runs the existing refusal cases against
    their reasons.
  - Why: a case that checks only the refusal passes when a new, earlier refusal takes it over for
    another cause.
  - `hooks/self-test` is its stated instance: one reason case per refusing leg
    ([hooks/README.md](../../hooks/README.md), "The hooks").
- **Test-authoring rules** (the unit under test named `tested`, the act as its own named value, the
  name over an assertion message, a behavior pinned once at one level, a seam asserted as driven, a
  run judged by proof that it executed): canonical in [testing.md](testing.md), "Testing direction"
  and "Proving a run really executed".

## 7. Git & commits (author's rules, strict)

- **The agent words exactly one commit message itself and commits** (standing approval, author
  decision 2026-10-07): no confirmation round, the rules below are the check, and the gate reads
  the branch log. Pushing follows the push rules below: an agent pushes only its own
  `claude/<topic>` branch, never `main`. Domain decisions stay questions to the author.
  - Where the approved plan names a commit's message, the implementer commits it as written and
    stops to report when it no longer fits; where the plan names none, it words a protocol-conform
    one itself.
  - The author's review of the branch log at the gateway may reword any message ("Reword at the
    gateway" below); on the agent seat that review reads the log on the strand's pull request.
    - Why: the hooks check a message's form; only a person checks that it says the right thing.
- Message = **single line**, **present-tense verb first** (after an optional `[doc]` tag), states
  what the commit actively changes. No body, no `Co-Authored-By` or tool footer. English. The
  `commit-msg` hook's ticket-key leg stays off: the project has no ticket system
  ([open-questions.md](../open-questions.md)).
  - Advice, which no hook measures: keep the subject brief.
  - ✅ `Add storage value calculation to data evaluator` · `Remove dead CsvParser` · `[doc] Groom the backlog current-status section`
  - ❌ `feat:`/`fix:`/other conventional-commit prefixes · multi-line bodies · `Co-Authored-By:` lines
- **`[doc]` prefix** for pure documentation/process/meta commits (only `docs/`, `.claude/`, or
  top-level `*.md`; no product-code/build change); code & build commits stay bare. `[doc]` is the
  only permitted prefix (keeps `git log` two-tier and scannable).
- Docs describing a code change ride in that commit, and each doc hunk passes the **revert check in
  both directions** before it is staged: the KB README's DOC checklist holds both (DOC-6,
  [README.md](README.md), "Doc conventions").
  - Only backlog, roadmap and decision hygiene, and a measurement over the combined effect of
    several commits, earn a standalone `[doc]` commit, and its message says which it is.
- **LF line endings**: [git-state.md](git-state.md) holds the rule.
- Whitespace/format changes in their **own** commit, never mixed with logic.
- One logical change per commit; cohesion test: group parts only if reverting/cherry-picking one
  alone would break the build or leave the tree inconsistent. Keep `main` releasable; local
  `./verify all` green before committing.
- **Slice the commits before the first edit** (author decision 2026-08-15). The plan names the
  commits it will produce, in order, each one green on its own and reviewable in one sitting. A
  commit that renames, changes behaviour, moves config and carries unrelated doc work at once is bad
  style however green it is: **35 changed files is a smell, not an achievement**. Rules of thumb:
  a rename or move is its own commit; a behaviour change is its own commit; documentation describing
  a code change rides with it, unrelated documentation gets its own `[doc]` commit; a finding that
  falls out along the way is its own commit rather than a passenger. Where a slice cannot stand
  green alone, that is the signal to reorder, not to merge it into a bigger one.
  - A replacement may pass through states no release carries, the old and the new side by side.
    - Each commit stays green and reviewable alone.
    - The plan names the commit that removes the old.
    - Why: the switch then moves in reviewable steps instead of one commit that swaps everything.
- **The author reviews the plan and gives the go on it before the first edit** (author decisions
  2026-10-15 and 2026-11-23).
  - A `[doc]` or build/infra strand may get the go at the start and the plan's review only at the
    gateway, with the rest of the strand.
  - A refactoring keeps the review before the first edit.
  - Why: a refactoring cuts production code; a `[doc]` or build/infra strand does not.
  - An early go may come before the author has seen a commit message; the author's review of the
    branch log at the gateway then reads the messages (the first bullet of this section).
- **Every commit compiles and is green on its own, not just the tip.**
  - A build-file or dependency change belongs in the **first** commit that uses it, not the one it
    was written for.
  - A test reference likewise lands no earlier than its referent.
  - Verify it, do not derive it: the "the rebased tip is byte-identical to `main`" argument covers
    the tip only, so walk the commits before making the claim:
    - list them with `git rev-list --reverse main..claude/<topic>`;
    - create a detached walk worktree, `git worktree add --detach <root>/<topic>-walk <tip>`, with
      `<root>` the worktree root (below), by absolute path, since a relative one lands below
      whatever the working directory is;
    - then, per commit, `git -C <walk worktree> switch --detach <sha>` and
      `<walk worktree>/verify all`;
    - each is a call of its own with the SHA written out, without a `$(…)` or a loop variable,
      since such lines were refused at times (observed, cause unknown: working-with-ai-agents.md,
      "Command shapes that keep a session unattended"); the `git -C` step meets the auto-mode
      classifier (working-with-ai-agents.md, "A rule that pairs an inner `*` with the `:*` suffix
      does not fire").
    <!-- MODULE guard -->
    - With the guard module the step is `git -C <walk worktree> checkout --detach <sha>`, the
      spelling the guard approves (working-with-ai-agents.md, "The shell-shape guard").
    <!-- /MODULE guard -->
    <!-- MODULE gate -->
    - `gate/walk <strand worktree> <walk worktree>` does all of this, by absolute path and as one
      command, and writes the log of each commit's code tree into the strand's dossier
      (multi-agent-playbook.md, "The gate dossier", `walk/`, which lists its refusals and logs).
    - After a fold, `gate/walk` builds only the commits whose code tree has no green log (author
      decision 2026-09-29).
    <!-- /MODULE gate -->
- **A strand's shape follows the work, not the plan it started with** (author decision
  2026-08-15). A planned commit count is an estimate: when the work needs more commits, it gets
  them; never inflate one commit to honour the original estimate. Clean code covers the history
  too.
- **Always able to build, not always allowed to deliver.** `./verify all` must be unconditionally
  able to run green: never attach vulnerability or quality gates (CVE thresholds, quality metrics)
  to the build/check tasks the inner dev loop runs. Such gates live on separate on-demand tasks
  (`./verify vuln`, `./gradlew vulnScan` with `vulnScan.failOnSeverity`) or in CI/delivery
  ([build-run-deploy.md](build-run-deploy.md)).
  - Deliver only CI-produced artifacts, never a local build.
    - The one exception, for a project without CI or a registry, as this one is:
      [build-run-deploy.md](build-run-deploy.md), "Release & deploy".
- **No secrets, no personal/host data, ever (hard rule); the repo is a public showcase.** Never
  commit passwords, tokens, API keys, private keys/keystores
  (`*.pfx`/`*.p12`/`*.jks`/`*.pem`/`*.key`), credentials, real personal/work emails or usernames,
  or machine/host/workspace details (absolute paths, host layout, infra).
  - Such data: gitignored `*.local.*` files or a secret store, injected at runtime.
  - A secret in history is purged by history rewrite **and rotated** (deletion doesn't un-leak).
  - The Claude Code "always allow" flow can write path-bearing rules into the committed
    `settings.json`: prefer bare commands matching the portable `Bash(<cmd>:*)` rules;
    machine-specific permissions (e.g. absolute-path `cd`) go in `.claude/settings.local.json`.
    - After using the flow, diff the committed `settings.json` against `HEAD`.
  - A `git -C …` command matches no `Bash(git <sub>:*)` rule, so always-allow would pin a
    host-path rule per worktree hash: never always-allow one.
    - It meets the auto-mode classifier instead, or a prompt outside auto mode
      (working-with-ai-agents.md, "Which rule applies").
  - **An agent uses only the access its environment was set up with,** for the project's own
    remote and services.
    - In the devcontainer that is the forwarded access [dev-environment.md](dev-environment.md)
      names; on the agent seat, what "Where agents run" there names.
    - It opens no credential store, key file or token it was not handed, looks for none and works
      with none it comes across.
    - A task that needs access the session lacks is reported (working-with-ai-agents.md,
      "Refusals and limits").
    - Why: a credential found on the machine was given to someone else, for something else.
  - **Nothing of the project goes outside unasked:** beyond the project's own remote and the tools
    the KB's workflow names, an agent hands nothing of the project, its KB or the session to anyone
    or anything without the author's word for that item.
    - A documentation lookup carries no project name, path or data.
    - Why: what has left the machine cannot be called back.
    <!-- MODULE agent-seat -->
    - The seat's push of its own strand, its pull request and its comments there are the
      workflow's own ("Pushing" below).
    <!-- /MODULE agent-seat -->
  - Outside the repository (a pull request, a ticket) a post quotes only the excerpts a finding
    needs, without personal data, and a snapshot of production data, or data drawn from one, is
    never uploaded.
  - A branch pushed before its review is published, and deleting it does not unpublish it.
    - A secret or host detail the review then finds is purged by a history rewrite, and the secret
      rotated, as on `main`.
  - A secret reaches a child process only through its environment or a file only its user can read
    (mode 0600), never as an argument.
    - Why: `ps`, `/proc/<pid>/cmdline` and a shell trace show a process's arguments.
    - A tool that hands a secret to a child process pins the way it does so in its self-test.
    <!-- MODULE agent-seat -->
    - `agent/self-test` pins it for `agent/github-token` and `agent/gh`, and `land/self-test` for
      `land/github-api`.
    <!-- /MODULE agent-seat -->
- **The commit log is the changelog**: [git-state.md](git-state.md) holds the rule.
- **Git enforcement hooks (must be active)**: the POSIX-sh hooks in `hooks/` gate every commit.
  - Every checkout keeps them active with `git config core.hooksPath hooks`.
  - The devcontainer `postCreate` sets it.
  - Which hook checks and records what: [`hooks/README.md`](../../hooks/README.md).
  - This project's gate configuration and its self-test legs in `pre-commit`:
    [build-run-deploy.md](build-run-deploy.md), "Git hooks (local enforcement)".
  - `commit-msg` enforces the message rules above.
  - `content-gate` refuses **every comment line a diff adds** to a listed source extension (§3, no
    opt-out).
  - `content-gate` refuses the conflict markers a diff adds, in any file.
  - Tests and full build are deliberately excluded, since they would slow the TDD micro-commit loop
    and its commits are already green.
  - **A hook that only prints is not a gate:** **git asks `pre-commit` only for `git commit`**, so
    every commit the sequencer creates (rebase, `git rebase --continue`, `cherry-pick`, `revert`)
    is ungated at the moment it is made.
  - `post-rewrite`, `post-commit` and `post-merge` record such a commit afterwards when it breaks a
    rule.
  - A gate-failing `git am` and any merge commit are refused outright, before their commit exists:
    by `pre-applypatch`, `pre-merge-commit` and, for a merge left in progress, `pre-commit`.
    - A merge commit written past them, with `--no-verify` or by a rebase todo, is recorded like an
      ungated commit.
  - Mechanics: [`hooks/README.md`](../../hooks/README.md), "The gate".
  - **A recorded commit refuses every commit whose `HEAD` reaches it.**
    - It refuses in every worktree, whatever is committed elsewhere.
    - It refuses until a rewrite takes it out of the history or the author releases it.
      - A rewrite that replays it with its change unchanged, such as a rebase onto a moved `main`,
        takes nothing out: `post-rewrite` records the replayed commit in its place.
    - Why: the record is append-only, so a clean commit on another branch cannot clear it.
    - This holds once every worktree checks out these hooks, since each worktree runs the hooks of
      its own checkout.
      - An agent can edit its worktree's hooks without a prompt; what keeps them is the rule and
        the review.
    - Open worktrees are therefore rebased right after a change to the hooks lands.
    - A new commit, a `git commit --amend` and `agent/fold` are all refused while `HEAD` reaches it,
      since the recorded commit is its own ancestor.
    - The way out is a rewrite at the recorded commit's `edit` stop that ends in
      `GIT_EDITOR=true git rebase --continue` (the recipe: multi-agent-playbook.md, "FAIL-loop
      rules").
    - A recorded merge commit: rebuild the branch without it, or `git rebase --abort` when a rebase
      wrote it.
    - **Only the author releases a recorded commit that already sits on `main`**, after reading it;
      never an agent.
      - The command is `sh hooks/breach-release <id>`.
      - The fix follows as an ordinary commit.
  - **A conflict stop is finished by `git rebase --continue`, never by a commit or an amend**, which
    would fold the conflicted state into the wrong commit.
    - `pre-commit` refuses both there.
    - An amend at an `edit` stop is the playbook's fold and passes.
    - An amend at a `reword` stop passes.
    - A commit at a `break` stop passes.
    - A commit after a finished rebase that left its `REBASE_HEAD` behind passes.
  - **Never read a hook's output as the verdict on the commit that follows it;** ask the history.
  - `hooks/self-test` takes the verdict of every case that commits from the resulting history,
    never from printed text; such a case may also check the reason a hook printed.
  - `pre-commit` runs it and each other self-test leg (`hooks/self-test-legs`), as staged, on every
    commit that touches that leg's directory, and `pre-applypatch` on every `git am` that does, so
    no weakened gate lands through `git commit` or `git am`.
  - A rewrite (a rebase, `git rebase --continue`, a cherry-pick) runs no self-test; the suites' run
    at the tip before landing ("Landing models" below) stands behind it.
  - Safety net, not substitute.
  - `--no-verify` only for a genuine emergency on the author's word and the `/pause` parking commit
    (below).
  - A `--no-verify` commit other than a merge commit is left alone, so the emergency valve stays
    one.
  - A commit written past every hook, by `git -c core.hooksPath=<other>` or by plumbing, is a rule
    break, never a route (working-with-ai-agents.md, "Refusals and limits").
    - Why: no hook sees such a commit, so the check it skips never runs.
- **`[wip]` parking commits (pause only)**: `/pause` may autonomously park uncommitted work as ONE
  local commit `[wip] <one-line state>` via `git commit --no-verify`.
  - WIP legitimately fails the hooks; this is the one sanctioned non-emergency bypass.
  - The no-secrets rule is unchanged: eyeball the diff first.
  - It is a checkpoint, not history.
  - `/resume-work` resolves it before any new work: it finishes or reworks the work, then replaces
    the commit through the normal commit protocol.
  - Reset-free rewrite recipe → multi-agent playbook.
  - It never survives to the review gateway or a push.
  - It is parked on the strand's branch, never on `main`.
  - Work in the primary checkout moves to a new `claude/<topic>` branch first (`git switch -c`).
  - The checkout then returns to `main`.
- **Deleting project content is the author's act; cleaning up the scaffolding is not.** The rule
  protects the author's ownership of *his* files, and a deletion no rebase undoes.
  - A file its own strand retires is the one exception, below.
  - **The agent never runs `rm`** (deny floor, `rm -rf` included).
    - Outside a strand's own commits, anything tracked by git and anything the author wrote is
      handed over as an exact command for the author to run.
    - Or it is removed by a committed, reviewed build task whose scope lives in code rather than in
      a permission rule.
    - Probes under the gitignored `src/probe/java` go through `./gradlew clearProbes`
      ([build-run-deploy.md](build-run-deploy.md)).
  - **A tracked file its own strand retires, the agent removes with `git rm`** (author decision
    2026-10-14).
    - Why: a reviewed commit can be reverted.
    - A file a template upgrade removes stays the author's call (`template-upgrade`).
    - The old `.template/baseline/` an upgrade replaces is its strand's own to retire.
  - **An empty directory a removal leaves behind is handed to the author** like any other leftover
    (author decision 2026-11-23).
    - Why: the managed policy denies `rmdir` on every seat, and a managed deny outranks the
      project's allow (working-with-ai-agents.md, "Managed settings").
  - **A copy of production data a strand works on stays under its worktree's own `build/`**, never
    in `/tmp` or a session scratchpad, so it goes with the worktree.
  - **The development scaffolding is the agent's own, and it cleans it up itself, without asking:**
    the worktrees it created under the worktree root, defined below (`git worktree remove
    [--force]`, `git worktree prune`) and the branches that have landed (`git branch -d` and
    `git -C <path> branch -d`, both of which meet the auto-mode classifier; derived: no allow covers
    them).
    - Deliberately *git-native*, never `rm`: `git branch -d` refuses a branch that is not fully
      merged, so the safety lives in the command instead of in the agent's judgment.
    - A directory a removal leaves behind under the worktree root goes to the author (above).
    - After a landing, at the cleanup point of its model ("Landing models" below), removing the
      worktree and deleting the branch is part of the landing, not a separate request.
    - The commits live on in `main`, and the reflog still holds deleted tips for a while.
    <!-- MODULE volumes -->
    - `git worktree prune` and the sweep (multi-agent-playbook.md, "Gateway hand-off and cleanup
      (the agent's side)") run only in a session whose container mounts the volume.
      - Why: git lists a worktree it cannot see as `prunable`, and a prune drops its registration
        for good; how automatic gc is kept from doing the same: dev-environment.md, "Worktrees on a
        volume".
    <!-- /MODULE volumes -->
  - **`git branch -D` stays off the table** (deny floor, and a Claude Code built-in besides). A
    branch that is not merged holds work nobody reviewed; discarding it is the author's call. Drop a
    genuinely stale scratch ref with `git update-ref -d refs/heads/<name>` only where the playbook's
    reset-free fold calls for it.

### Branching, merge & the review gateway

**Isolate the working tree, not just the history.** A branch isolates commits, not the working
directory; two writers on one checkout race (index/HEAD flicker, clobbered staged work). git
worktrees (own directory + HEAD + index) make parallel work safe.

- **A worktree per context.** Each problem/feature/context gets its own branch in its own
  worktree; parallel contexts (incl. other-model agents) = parallel
  worktrees; the primary checkout stays on `main`. Worktrees branch from a freshly synced `main`
  (below), a chained strand from its start point (sub-bullet). `worktree.baseRef: "head"`
  makes the harness's subagent worktrees branch from that same start point; `"fresh"` would branch from
  `origin`'s default branch as Claude Code last fetched it, up to a day old, and miss that day's
  landings.
  - The command is `git worktree add -b claude/<topic> <root>/<topic> <start>`, where `<start>` is
    `main` or, for a chained strand, the tip of the strand before it ("Pushing").
  - `git worktree add` and `git switch -c` have no allow, so cutting a strand meets the auto-mode
    classifier, or a prompt outside auto mode (working-with-ai-agents.md, "Permissions &
    autonomy").
  - The prefix `claude/` is held as the `branch_prefix` line of `agent/fold` too.
    <!-- MODULE agent-seat -->
    - So do the `branch_prefix` lines of `agent/push`, `land/land` and `hooks/pre-push`, and the
      `agent-namespace` ruleset's exclude ([git-state.md](git-state.md), "What GitHub enforces").
    <!-- /MODULE agent-seat -->
- **The worktree root holds every worktree an agent creates:** strand, walk, lens and run worktrees
  alike.
  - The root is `$AGENT_WORKTREES` where the environment sets it to a path the guard takes, else
    `<project root>/.claude/worktrees/`; an empty value reads as unset.
  - The variable names an absolute directory outside every checkout.
    - Why: `.gitignore` covers the default alone; anywhere else inside a checkout, a worktree is
      that checkout's untracked content.
  - A session reads it once (`printenv AGENT_WORKTREES`) and writes the path out literally in every
    command.
    - Why: the permission rules and the auto-mode classifier cannot expand a variable.
    - On the PC it is read inside the container; a session on the Windows host asks
      `docker exec -u vscode <container> printenv AGENT_WORKTREES`.
  - Every recipe writes `<root>/<name>` for a worktree under the root; `<project root>` names the
    repository root.
  <!-- MODULE volumes -->
  - On the PC the devcontainer sets the variable to `/worktrees`, the named volume
    `evergore-worktrees`, and pruning stays inside the container ("Deleting project content"
    above; why and which seat sets it: [dev-environment.md](dev-environment.md), "Worktrees on a
    volume").
  <!-- /MODULE volumes -->
  <!-- MODULE agent-seat+volumes -->
  - With the agent-seat module the seat's image sets it empty (dev-environment.md, "The agent
    image").
  <!-- /MODULE agent-seat+volumes -->
  - Where Claude Code puts an `Agent` subagent's `isolation: "worktree"` checkout, and why strands
    avoid it: [multi-agent-playbook.md](multi-agent-playbook.md), "One track".
- **`main` follows `origin/main` on every machine** (author decision 2026-10-04): `origin/main`
  moves only by the author's landings, so a machine that does not fetch works on a stale `main`.
  - A dependency bot's update lands as a strand like any other change, never by GitHub's merge
    button (author decision 2026-11-23; the rule: [build-run-deploy.md](build-run-deploy.md)).
  - **The sync is `sh hooks/sync-main`,** run from the primary checkout: it fetches, runs every
    check below on the refs that fetch brought, and fast-forwards `main` (its exits and what it
    refuses: [`hooks/README.md`](../../hooks/README.md), "`sync-main`").
    - Why a script: the checks are many and exact, and a sync that skips one lets a stand-in decide
      what is synced and landed.
    - Its `landing_model` line reads `hub` ("Landing models" below).
  - **Fetch at fixed points:** at the start of every session (`/resume-work`, `/plan-next`),
    before a strand is cut and before the gateway's rebase the fetch is the sync's own; before
    every review panel and before every landing it is `git fetch origin`; fetching is allowed at
    any other time too, since it moves no local branch.
    - A subagent does not fetch: the session that spawned it did.
    <!-- MODULE agent-seat -->
    - On the agent seat every fetch is `git fetch --prune origin`, the script's too, which also
      drops the remote-tracking ref of every branch `origin` no longer holds, a landed strand's
      among them.
    <!-- /MODULE agent-seat -->
  - **A moved `main` is taken in before the next panel** (author decision 2026-10-09): when the
    fetch before a panel shows `origin/main` ahead, the agent syncs `main`, rebases the strand and
    rebuilds the gate's evidence before spawning any gate agent, without asking while it works on
    its own, since the dossier check refuses any base but `main`:
    - a lens report whose read paths `main` left unchanged stands across the rebase;
    - the doc-reviewer, the reviewer and the runs repeat at the rebased tip.
  - **Local `main` only fast-forwards**, in the clean primary checkout, on `main`, to exactly the
    commit the checks read; never a merge, a rebase or a pull on `main`.
    - Why exactly that commit: `git pull --ff-only` fetches again, so a ref that second fetch
      brings would land unchecked.
    - Why the checks below: git resolves a file of the name directly in the common git dir,
      `refs/<name>`, a tag and a local branch before a remote-tracking ref.
      - So a ref `refs/main` or `refs/origin/main`, a tag `main` or `origin/main` or a local branch
        `origin/main` would stand in for the branch.
      - So would a file `main` or `origin/main` directly in the common git dir, which no
        `for-each-ref` lists; the same file in a linked worktree's own git dir takes over nothing
        (probed on git 2.30.6 to 2.55.0).
      - So would a line `main` or `origin/main` in `packed-refs` without the `refs/` prefix, which
        neither `for-each-ref` nor a loose file shows.
      - On a case-insensitive file system, such as the author's PC's checkout, so would a name that
        differs from one of them only in case: a fetched branch `Main` takes over
        `refs/remotes/origin/main`, and a tag `MAIN` stands in for `main`.
    - Why the refspec: the script takes only refspecs that map a branch onto its own name under
      `refs/remotes/origin/`.
      - A fetch refspec that maps a tag or another branch onto `origin/main`, or anything onto a
        local branch, would move what the checks read, or `main` itself.
      - A negative one that leaves `main` out would leave `origin/main` where it was.
      - A remote group named `origin` would have other remotes write it.
    - Why the history as committed: a replace ref or a graft gives a commit other parents, so a
      diverged `main` would read as behind `origin/main` and lose its commits to the fast-forward.
  - **Nothing is resolved by hand:** the sync stops, and both SHAs go to the author,
    - on a failed fetch;
    - on a refused fast-forward (`main` and `origin/main` diverged);
    - in the hub models, on a `main` holding what `origin/main` lacks; in the local model `main`
      may run ahead;
    - in every model, on a stand-in for `main` or `origin/main`, in each shape "Why the checks
      below" names; a ref below a name (`refs/main/x`) stands in for nothing.
      <!-- MODULE agent-seat -->
      - A checkout on a case-insensitive file system can move its refs to reftable, with
        `git refs migrate --ref-format=reftable`.
        - Why: git's file-based refs collide there where names differ only in case, and a fetch
          between this check and a later use of a ref goes unchecked.
        - Reftable keeps the refs out of the file system.
        - Every git that opens the checkout, the host's and a container's alike, must then read
          reftable.
        - The command came with git 2.46.0 (its release notes).
        - It migrates no repository that has worktrees (`git help refs`, "KNOWN LIMITATIONS", at
          git 2.55.0; not probed), so it runs while the checkout has no linked worktree.
      <!-- /MODULE agent-seat -->
  - **No commit is made on `main`**, on any machine: every change, a one-commit fix included, runs
    on a `claude/<topic>` branch and reaches `main` only through the gateway below; it is a rule, no
    hook enforces it.
    - The one exception is the adoption commit on adoption path A: the repository's first commit,
      on the branch `git init` made (`/adopt`, step 11:
      [`.template/baseline/.claude/skills/adopt/SKILL.md`](../../.template/baseline/.claude/skills/adopt/SKILL.md)).
      - Why: a repository without a commit has no `main` to protect and no base for a gateway.
    - On path B the adoption is a strand: `/adopt` cuts `claude/adopt-template` before its first
      edit, in the primary checkout with no worktree of its own, since the files the author copied
      in lie there uncommitted; it lands by the landing model after the author's review.
- **Walk and lens worktrees are detached scratch** under the worktree root (above), on a path with
  no `..`:
  - a walk's is named `<topic>-walk`, a lens's `lens-<lens>-<topic>`; a strand topic never ends in
    `-walk` or starts with `lens-`, a naming rule nothing enforces, so the name alone tells them
    apart;
  - each is created with `--detach` at a SHA and by absolute path, so no branch points into it;
  - file edits, builds and `git switch --detach <sha>` inside one change nothing shared;
  - a file goes back by `git show <rev>:<path> > <path>` (the raw blob: working-with-ai-agents.md,
    "Command shapes that keep a session unattended") or by creating the worktree anew
    (`git worktree remove --force`, then `git worktree add --detach`), never by `git reset` or
    `git clean`, which the deny floor refuses in their plain and their `git -C` form;
  <!-- MODULE guard -->
  - with the guard module that restore is refused:
    - a write from a command that names git goes only into the guard's output target;
    - in a walk worktree a file goes back by the approved
      `git -C <worktree> checkout <hash> -- <path>`;
    - in the primary checkout it goes back by `git show` into the output target, then a copy;
  <!-- /MODULE guard -->
  - a ref or config command run there (`branch`, `update-ref`, `stash`, `worktree`, `push`,
    `reflog`, `tag`, `config`) acts on the refs and config every checkout shares and is never
    scratch;
  - a strand worktree is never scratch: its branch shares refs and config with the primary checkout
    and holds the only copy of unlanded work;
  - `CLAUDE.md` names them for the auto-mode classifier (working-with-ai-agents.md, "Where
    auto-mode rules live").
- **Pushing** (author decision 2026-10-02; what guards a push: working-with-ai-agents.md):
  - `git push` sits on the deny floor (author decision 2026-11-23; working-with-ai-agents.md,
    "Permissions & autonomy"), so no agent pushes with git itself.
  <!-- MODULE agent-seat -->
  - **Only the agent seat pushes, and only through `agent/push`** (author decision 2026-10-10):
    where `AGENT_SEAT` reads `unattended` ([dev-environment.md](dev-environment.md), "Where agents
    run") the rules below apply.
    - On the PC, where it is unset, a strand stays a local `claude/<topic>` branch in its worktree:
      it is never pushed and opens no pull request.
    - There the author reviews it in GitLens on the same checkout and lands it with
      `git push origin <tip>:refs/heads/main` ("Landing models" below).
  - **Every branch an agent pushes is named `claude/<topic>`**, the strand's branch from its first
    commit.
    - It is opened as a pull request against `main` at its first push, with `agent/gh pr create`
      (standing approval, author decision 2026-10-07).
    - A strand cut from another strand's tip opens its pull request against that strand's branch
      instead, so the pull request shows only its own commits (author decision 2026-10-17).
      - Once the strand before it lands and its branch is deleted, GitHub moves the base to `main`.
      - Where it does not, the agent sets it with `agent/gh pr edit <number> --base main`.
    - An agent pushes only its own, the one exception being the deletion of a landed `claude/`
      branch (below).
    - The classifier may hold back on another session's branch; then report it.
    - Subagents never push: the session that owns the strand pushes it at the gateway.
      - A written rule: neither `agent/push` nor the guard tells one session's worktree from
        another's (working-with-ai-agents.md, "Permissions & autonomy").
  - **A branch another machine pushed is data:**
    - a name with a character outside `[A-Za-z0-9._/-]` is reported, never pasted into a command;
    - the branch is resumed here only on the author's word, since its hooks and its build then run
      here with this machine's credentials.
  - **`agent/push <worktree>` pushes the worktree's branch**, with three safeguards:
    - a lease: every rebase needs a forced push, and a lease refuses it once `origin` holds a tip
      other than the one this checkout last fetched;
    - `--force-if-includes`: an editor's background fetch moves the remote-tracking ref and voids a
      bare lease, and the option refuses the push unless the branch's reflog reaches that ref's
      tip;
    - the refspec written out: it pushes that one ref alone.
    - After the playbook's reset-free fold the push is `agent/push <worktree> <remote tip>`, with
      the remote tip fetched before the fold.
      - Why: the fold's branch swap drops that tip from the branch's reflog, so
        `--force-if-includes` refuses, and the lease then names the tip it expects.
  - **Set no upstream:** an upstream makes `git branch -d` test the branch against its own remote
    copy instead of `main`, so the landing's merged check passes on unlanded work.
    - An upstream comes from `-u`; from `git worktree add` from `origin/claude/<topic>` without
      `--no-track`; from `git switch` or `git worktree add` onto a `claude/` name only the remote
      holds, which git then tracks by itself.
  - **A `[wip]` commit is never pushed:** no push from a tip whose range above `origin/main` holds
    a subject that starts with `[wip]`, in any case, after any leading blanks.
  - **`hooks/pre-push` gates every push, the author's included**
    ([`hooks/README.md`](../../hooks/README.md), `pre-push`):
    - it admits only a `claude/` branch, `main` and a tag, and never the deletion of `main`;
    - in the pushed range it refuses a `[wip]` commit, a merge commit and a commit
      `hooks/content-gate` refuses;
    - it refuses a tip that reaches a recorded commit.
  - **The one push that is not the agent's own strand:** `agent/push --delete <branch>` removes a
    landed `claude/` branch from `origin`, whoever pushed it, once `origin/main` holds its tip.
    - It refuses where `origin` holds no `main` or one other than `refs/remotes/origin/main`
      (fetch first).
      - Why: a stale or hand-moved remote-tracking ref would let an unlanded branch go.
    - It refuses where a local branch of that name holds a commit `origin/main` lacks.
      - Why: a chained strand pushed at its base's tip has not landed when its base has.
  - Each of the three forms refuses where a push URL of `origin` differs from its fetch URL, or
    `origin` names a second URL.
    - Why: `origin/main` and the lease are read through the fetch URL, so what they show is not
      where the push goes.
  <!-- /MODULE agent-seat -->
  - **`push.default` is `nothing` in every checkout:** a bare `git push` from a checkout on `main`
    would push `main`, and with `nothing` git refuses a push that names no refspec.
    - `--all`, `--branches`, `--mirror` and `--tags` still push: each pushes refs that are never
      written out, and `nothing` does not stop them.
    - The devcontainer's `postCreateCommand` sets it; an older checkout runs
      `git config push.default nothing` once ([build-run-deploy.md](build-run-deploy.md), "Git
      hooks (local enforcement)").
    - `/resume-work` checks it at session start.
    - No checkout configures `remote.origin.push`, which git would use first.
    - A `-c push.default=<value>` on the command line overrides the setting for that one command
      (`git -c push.default=current push origin`).
  - **A refspec still pushes what it names:** `@` the checked-out branch, `:` and `refs/heads/*`
    every matching or local branch, `main` among them.
  - **`main` is the author's, tags too:** no agent moves either in any form (a push, a merged pull
    request, an API call, a `/land` comment).
    - A strand reaches `main` only by the author's landing of the reviewed tip ("Landing models"
      below; [git-state.md](git-state.md) names the ruleset on it and its bypass).
    <!-- MODULE agent-seat -->
    - On GitHub the rulesets refuse the seat's credentials every push to `main` or a tag
      ([git-state.md](git-state.md), "What GitHub enforces").
    <!-- /MODULE agent-seat -->
- **On the branch**: the implementer runs the full TDD loop and commits each red→green→refactor
  step itself, with the plan's message or, where the plan names none, a protocol-conform one it
  words (the first bullet of this section); no per-commit pre-approval.
- **The review gateway (per feature, serialized):**
  1. Everything committed in the worktree.
  2. Sync `main` (`sh hooks/sync-main`, above, with its stops), then rebase onto it, resolving
     conflicts there.
  3. Review the rebased tip (falsifier + reviewer gate here).
     - With rebase + fast-forward it is byte-identical to what `main` becomes, so reviewing it *is*
       reviewing the final state, one feature, no merge artifacts.
     <!-- MODULE agent-seat -->
     - On the agent seat the session pushes before the hand-off: after the gateway build it pushes
       the rebased tip with `agent/push` ("Pushing" above) and opens or updates the strand's pull
       request, so the author can review it on its pull request, in GitLens or in GitHub's compare
       view, from any machine.
     - The push publishes the branch before that review ("No secrets" above).
     <!-- /MODULE agent-seat -->
     - On the PC nothing is pushed: the author reviews the local branch in GitLens.
     - What the hand-off carries: the bullet "Asking the author to review" below.
  4. On PASS and the author's review, the author lands the tip by the project's landing model
     ("Landing models" below): a seat strand by a `/land <tip>` comment on its pull request, a PC
     strand by `git push origin <tip>:refs/heads/main`. Either is refused once `origin/main` has
     moved (then back to step 2).
  5. Its cleanup, autonomously, as part of the landing, once `origin/main` holds the tip
     ("Landing models" below).
     - A remote copy that moved past the tip is reported: it is unlanded work.
     - Every other machine fast-forwards at its next sync.
- **Landing models:** the handbook carries every landing model; "Project choices" in `CLAUDE.md`
  names the one this project lands by: hub with seat, a seat strand by the author's `/land <tip>`
  comment, a PC strand by the author's `git push origin <tip>:refs/heads/main`.
  - In every model the self-tests of the tooling folders the strand touches (`hooks/` and each
    module's folder with a self-test) pass at the reviewed tip before it lands.
    - The reviewed tip is the one the author reviewed, or the tip of a re-rebase after that review
      which ran clean and builds green ("Every rebase is followed by the full gateway build" below).
    - Why: a rewrite runs no self-test leg, so a suite weakened in a rebase reaches the tip unseen
      by the hooks ([`hooks/README.md`](../../hooks/README.md), "Bypass").
  - **Local** (no hub): the agent fast-forwards `main` to the reviewed tip by running, from the
    primary checkout, `git merge --ff-only <tip>`.
    - Precondition: the primary checkout is **clean**, else stop and surface it.
    - Precondition: the reviewed tip's own guard, `sh <worktree>/hooks/breach-guard --reaches
      <tip>` run from the primary checkout with the strand's worktree clean at `<tip>`, exits 0
      for that same commit, else stop: the strand is rewritten first.
      - Why the check: the fast-forward would carry a recorded commit onto `main`, where only the
        author's release removes the refusal.
      - Why one commit: the check and the fast-forward name the same commit, so a branch that moves
        in between cannot land unchecked.
      - Why the strand's copy: the guard reads the repository of the directory it runs in, and the
        copy `main` checks out may predate `--reaches`.
    - Then it removes the worktree and deletes the landed branch at once (`git worktree remove` +
      `git branch -d`).
    - With `origin`, `main` may run ahead of `origin/main`, and the author pushes it at will,
      naming the refspec (`git push origin refs/heads/main:refs/heads/main`), since
      `push.default nothing` refuses a bare `git push` (above).
  - **Hub without seat:** the author lands the reviewed tip: `git fetch origin`, then
    `git push origin <tip>:refs/heads/main`, a fast-forward without force, which `origin` refuses
    once `origin/main` has moved (then back to the rebase: "Every rebase is followed by the full
    gateway build" below).
    - Before the push, the tip's own guard (`sh <worktree>/hooks/breach-guard --reaches <tip>`, as
      in "Local") exits 0, else the strand is rewritten first.
    - The destination is written out because git looks a short `main` up among `origin`'s refs,
      and a tag `main` there beside the branch makes the push fail as ambiguous (`git help push`,
      `<refspec>`).
    - A landing is never an instruction to the agent: a go in chat is answered with that exact
      command for the author to run.
    - The cleanup waits until `origin/main` holds the tip: the sync (`sh hooks/sync-main`), then
      `git merge-base --is-ancestor <tip> refs/heads/main`; then `git worktree remove` and
      `git branch -d`.
      - A sync that exits non-zero stops the cleanup, and its lines go to the author.
  <!-- MODULE agent-seat -->
  - **Hub with seat:** the `agent-seat` module's model; a seat strand lands by the author's
    `/land <tip>` comment on the strand's pull request.
    - The landing is a fast-forward of exactly that commit, pushed by the landing workflow
      ([build-run-deploy.md](build-run-deploy.md), "The landing workflow").
    - The hub's rulesets hold `main` and tags against every credential but the admin's and the
      landing App's ([git-state.md](git-state.md), "What GitHub enforces").
    - Before the line `/land <tip>` goes to the author, the tip's own guard (as in "Local") exits
      0 on the seat, else the strand is rewritten first: the hub's fast-forward cannot read the
      seat's record.
    - Where `origin/main` has moved since the review, the strand goes back to the rebase ("Every
      rebase is followed by the full gateway build" below).
    - A strand of the PC lands as in "Hub without seat".
    - A landing is never an instruction to the agent: on the seat a go in chat is answered with
      the line `/land <tip>` and the pull request's link, for the author to post.
      - An agent posts no `/land` comment: that would be one more form of moving `main` ("`main`
        is the author's, tags too" above).
    - The cleanup waits until `origin/main` holds the tip, by the sync and the check of "Hub
      without seat"; then `git worktree remove`, `git branch -d`, and
      `agent/push --delete <branch>` where `origin` still holds the branch.
  <!-- /MODULE agent-seat -->
- **The gateway is two steps and is never collapsed into one.** (1) Present the full reviewable
  statement (the rebased branch log **and** the full diff) and *wait* for the author's explicit
  review verdict; (2) only then the landing (step 4). An "execute the gateway" approval is not the
  review.
- **Freeze the tree for the length of a gate round.** Before spawning a gate agent, the strand's
  `git status --porcelain` is empty and quoted in the briefing; no rebase, no amend and no build
  runs in that worktree until every agent of the round has reported. A finding that arrives while
  the gate is open is folded in **after** it. A verdict is worth only what the tree it read was.
- **Merge strategy (hard rule): rebase + fast-forward only.** No merge commits, no squashing (an
  exception is the author's act): history stays linear and keeps the small commits.
  - `pre-merge-commit` refuses the merge commit `git merge` builds, `pre-commit` the one that would
    conclude a merge in progress, and the post-hooks record one written past them
    ([`hooks/README.md`](../../hooks/README.md)).
- **Landings are author-serialized**: one feature at a time in rebase→review→fast-forward; several
  ready ⇒ the author picks the order. The fast-forward is the guard, since the author lands without
  force: a landing that is not a fast-forward of `origin/main` is refused (re-rebase; whether a
  review follows: "Every rebase is followed by the full gateway build" below). No automatic
  cross-session coordination (heterogeneous tools can't discover each other); the author is the
  single serialization point.
  <!-- MODULE agent-seat -->
  - For a seat strand the guard is the fast-forward of the author's `/land` landing, which lands
    exactly the reviewed commit.
  <!-- /MODULE agent-seat -->
- **Every rebase is followed by the full gateway build.** `./verify all` runs again on the rebased
  tip, cache disabled, executed-proof read ([testing.md](testing.md)), before anything else happens:
  a rebase that applied cleanly can still break semantically. When `main` moved after the author's
  review, a re-rebase that ran clean and builds green goes to the landing without a second review; a
  re-rebase that needed a conflict resolution re-opens the **panel** for what it touched and then
  the **author's review**, because resolutions are the one place unreviewed content enters:
  - the reviewer, the doc-reviewer and every lens whose read paths the resolution touches
    ([multi-agent-playbook.md](multi-agent-playbook.md), "Read paths of the lenses (delta
    rounds)") run on the resolved tip, the resolution as their delta (§9, delta rounds);
  - this run counts as no panel round, on every seat (author decisions 2026-10-13 and
    2026-11-23).
- **Every commit landing on `main` is verified-good, not merely green.** Defective = builds red,
  OR falsifier-exposed fake-green (tests pass, assert nothing real), OR reviewer-rejected as not
  implementing what it claims; none may land. Red states are never committed (TDD red is transient;
  `@wip`-tagged Gherkin keeps the build green). A gateway defect is repaired by rebase into the
  commit it belongs to, before the landing: never left on `main`, never a follow-up "fix" commit.
- **Asking the author to review** means handing over the GitLens compare statement for the range
  (`<tip>..<base>`), the branch with its worktree's absolute path and, on the agent seat, its pull
  request's link, never a description of where to look →
  working-with-ai-agents.md.
- **Reword at the gateway** non-interactively via scripted `GIT_SEQUENCE_EDITOR` / `GIT_EDITOR`
  (no interactive TTY).
- **Tool-neutral**: plain git (worktree · branch · rebase · `--ff-only`). Claude Code adds `Agent`
  worktree-isolation for tool agents; other tools/humans use `git worktree` directly.
- **A session never relocates into a worktree** (`EnterWorktree`, denied in
  `.claude/settings.json`): session history is filed per working directory, so a relocated session
  drops out of the primary checkout's resume list and is unreachable from the editor window that
  opened it. Sessions stay in the primary checkout and reach the worktree by absolute path, the
  discipline the bullet "The shell's working directory drifts between worktrees" already demands
  ([multi-agent-playbook.md](multi-agent-playbook.md), "Environment gotchas").

## 8. Definition of Done (checklist)

- [ ] **BDD scenario(s) written up-front and confirmed by the author** (§5), and now passing, or the
      exemption explicitly claimed and named in the plan
- [ ] Compiles, `./verify all` green locally, **and each commit green on its own**, proven by a walk
      over the commits rather than derived from the tip
      <!-- MODULE gate -->
      (the proof is the walk log of each code tree, which `gate/dossier-check` reads)
      <!-- /MODULE gate -->
- [ ] Test run proven to have *executed* ([testing.md](testing.md)), not read off an exit code or a
      cached result
- [ ] Every TDD cycle carries its **red evidence** in the hand-off (the failing test's name and
      failure line), or, where §4 allows it, a recorded mutation in its place (§4)
- [ ] No compiler/lint warnings (`-Werror` fails the build on any; the suite is warning-clean)
- [ ] Unit tests for new logic; no comment or doc comment added anywhere in the diff (§3)
- [ ] No secrets or personal/host data (passwords, tokens, keys/keystores, credentials, real emails/usernames, absolute paths, infra) in committed files
- [ ] Relevant `docs/knowledge-base/` doc updated, riding in the commit whose change it describes,
      each hunk checked in both directions (§7)
- [ ] A change to a valuation rule, to the meaning or label of a figure the overview shows, or to an
      option a member can switch changes the explanation page (`frontend/src/app/explanation/`)
      in the same commit
  - `ExplanationExamplesTest` makes the page's figures fail the build when they drift
  - the approved-text test fails on a changed word until `explanation.approved.txt` changes with
    it, for the author to approve
- [ ] Every identifier, path or backlog ID the diff **deletes or renames** grepped across `docs/`, and
      every hit fixed in the same commit; every **figure** the change moves grepped repo-wide, and
      every hit outside a dated decision or learnings row rewritten in the same commit; a changed
      **rule**'s key terms grepped across `docs/` and `.claude/`, and every hit moved in the same
      commit
- [ ] Commit message one line, present-tense verb (§7); never pushed on the PC, never as a `[wip]`
      commit, never to `main` (§7)
      <!-- MODULE agent-seat -->
      (on the agent seat pushed only on the strand's own `claude/<topic>` branch with its pull
      request open, through `agent/push`, plus the deletion of a landed one, §7)
      <!-- /MODULE agent-seat -->
- [ ] Whitespace/format separate from logic; LF endings
- [ ] Decisions/assumptions logged in [`open-questions.md`](../open-questions.md) if any were made
- [ ] A release follows the checklist in [build-run-deploy.md](build-run-deploy.md), "Release &
      deploy"

## 9. Review gate & process learnings

Before each commit, a reviewer (human or review agent, see
[working-with-ai-agents.md](working-with-ai-agents.md)) checks **process adherence** (real
red→green→refactor micro-step? whitespace separate? KB updated? commit-message rules?) and **code
criteria** (clean code, SOLID, hexagonal boundaries). If off: send back with concrete feedback and
record the lesson in `docs/process-learnings.md` (so future work and agents drift toward the
process, not away from it).

**Bounded review loop (governance).** A rejection sends work back to re-implement; from the
second panel round on, only the gate agents the fold concerns re-check it. Bounds:

- **The panel bounds below govern the feature gate** (author decision 2026-10-13): delta rounds,
  docs-only folds, the end on passing lows, the PC's cap, the reviewer beside the lenses and the
  "already decided" list.
  - At the scenario gate, `falsifier-scenario`, `falsifier-stakeholder` and the reviewer re-run in
    full on every re-gate, with no delta and no "already decided" list, since a draft `.feature`
    is small and reread whole anyway; their briefs carry the refuted list below.
  - The reviewer's verdict stands only on the features both falsifiers read: a missing report, or
    a stakeholder report that read fewer `.feature` files than `git ls-files '*.feature'` lists
    plus the draft where it is not yet tracked, stops the gate.
    - The stakeholder lens reads every `.feature` file on every run and states how many it read.
      - Why: a lens that sees one file or one cluster cannot see an overlap with another.
    <!-- MODULE gate -->
    - `gate/scenario-check` makes that comparison, by the falsifiers' fingerprints
      ([multi-agent-playbook.md](multi-agent-playbook.md), "The scenario-gate dossier").
    <!-- /MODULE gate -->
  - A scenario-gate lens's finding is `high` or `med` only for a rule or behaviour of the backlog
    item that no scenario states, a scenario a wrong or hollow implementation would pass, a
    scenario a product owner cannot judge, a `Then` asserting what its actor cannot see, or a rule
    stated twice; anything else is `low`.
    - Why: lows reach author gate 1 unfolded, so only a finding that makes the set incomplete,
      hollow, unjudgeable or redundant holds the gate.
  - A lens reports `soundSpec: no` exactly when a `high` or `med` finding of its own stands.
    - Why: lows go to author gate 1 unfolded, so a lens with only lows does not hold the gate.
  - A lens finding the reviewer refutes counts as settled where the refutation names the rule of §5
    it rests on, and goes to the author at once where it names none.
    - A settled finding holds the gate no longer, and neither does a lens's `soundSpec: no` that
      rests on settled findings alone.
    - The settled findings, each with its refutation, go into the next round's briefs as the
      refuted list, and a lens raises one again only on a ground its refutation does not answer.
    - Why: a fresh lens raises a refuted finding again, so without the list the round bound, not
      the refutation, ends the dispute.
  - The scenario gate passes when no `high` or `med` finding is open and every report carries its
    own PASS (a lens's `soundSpec: yes`, the reviewer's `PASS`), a lens's report counting as a pass
    where it fails on settled findings alone.
  - Its open lows go unfolded to author gate 1, and a low the author adopts there is a fold and a
    full re-gate.
  - A scenario gate that has not passed after round 9 ends (author decision 2026-11-23):
    - one more full run follows that round's fold;
    - where that run does not pass, only the reviewer's mandatory folds follow, the minimal set its
      FAIL names, and the fold brief names nothing else;
    - then a closing run, in which every report names the `.feature` files it read, by count and
      path, and the orchestrator compares them with `git ls-files '*.feature'` plus the draft
      where it is not yet tracked;
      <!-- MODULE gate -->
      - `gate/scenario-check` is that comparison, by fingerprint.
      <!-- /MODULE gate -->
    - what is still open goes to author gate 1 beside the scenarios.
    - Why: a fold can seed the next round's findings, so the loop needs an end that does not wait
      for convergence.

- **Older doc debt in a touched doc is a finding in rounds 1 and 2 only, and only where it is
  false** (author decisions 2026-10-15 and 2026-10-25):
  - false means a claim the code or the repository contradicts (DOC-2, DOC-6), a pointer to
    nothing, a bare shortcode (DOC-8) or a wording error (DOC-9);
  - older style debt (DOC-1, DOC-3, DOC-4) in lines the strand does not write is no finding and is
    filed nowhere, since filing it grew one backlog row past anyone's reading;
  - the strand tries twice to clear what is false, so it is cleared along the way when it can be;
  - what remains then goes to a backlog row, a bare shortcode to the bare-shortcode row (G21), so
    it never drives the time of the strand at hand;
  - the strand files what a doc-reviewer lists there in its own last fold, without a question to
    the author (author decision 2026-10-17).
- **The doc-reviewer's rotating sweep reports only older text that is false** in the sense above,
  never DOC-1, DOC-3 or DOC-4 debt (author decision 2026-10-25).

- **From round 3 on, a DOC-1 residue in a line an earlier round of the same panel had split is a
  low** (author decision 2026-10-19):
  - it goes onto the passing round's lows row instead of into another fold;
  - it does not fail the doc-reviewer's round;
  - the why: each fold of such a residue seeds the next round ([open-questions.md](../open-questions.md),
    2026-10-19).

- **A delta round re-runs only what the fold concerns** (author decision 2026-10-12), since a full
  panel after every tip change was the largest cost of a strand:
  - from round 2 on, every lens whose read paths the fold touches runs again, fresh (author
    decision 2026-10-13), so the robustness lens runs again on every fold of code outside `*.md`
    (read paths: [multi-agent-playbook.md](multi-agent-playbook.md), "Read paths of the lenses
    (delta rounds)");
  - its brief carries the delta (`<previous tip>..<tip>`) and the dispositions so far;
  - a re-run lens or the reviewer raises only what the delta introduces or a fix left open, and the
    rest of the strand stands as reviewed;
  - every other lens report stands while the dossier accepts it, a rebase onto a moved `main`
    included ([multi-agent-playbook.md](multi-agent-playbook.md), "The gate dossier");
  - why: a verdict on unchanged paths still holds, and a re-run costs a full lens.
- **A docs-only fold** (code tree unchanged) re-runs only the doc-reviewer and the reviewer:
  - it checks a docs fix of the reviewer's finding too;
  - the lens verdicts stand;
  - a lens judges the code, and such a fold changes none.
- **Every lens brief, and the reviewer's, at the feature gate carries the "Already decided, out of
  scope" list** (author decisions 2026-10-12 and 2026-11-23; its contents:
  [multi-agent-playbook.md](multi-agent-playbook.md), "Cadence"):
  - an entry is raised again only where the delta changed the code it names, and the agent names
    the entry with the change;
  - why: the entry judged that code as it was.
- **Round 1 of a large docs-only strand splits the doc-reviewer** (author decision 2026-10-15),
  since one `haiku` full pass left findings in text it had read to later rounds (cause not
  established):
  - large means more than three docs touched or more than 100 lines added
    (`git diff --shortstat main..<tip>`);
  - one `doc-reviewer` per touched doc checks that doc whole;
  - the same run checks the range's hunks in that doc;
  - one more checks the docs the range should have touched;
  - that one also runs the rotating sweep;
  - all of them run in parallel;
  - each runs on `sonnet`;
  - a failing round 1's findings fold once;
  - from round 2 on, a single `doc-reviewer` on its default model runs the delta
    (`.claude/agents/doc-reviewer.md`, Mode 1);
  - how the dossier holds the split reports and their findings:
    [multi-agent-playbook.md](multi-agent-playbook.md), "The gate dossier".
- **A passing round whose findings are all low ends the panel** (author decision 2026-10-12):
  - every report passes;
  - each finding is a lens's `low`, or one a passing doc-reviewer or reviewer raised;
  - the lows become one backlog row naming each low with its location, which `findings.txt` names
    for each of them;
  - the commit that adds the row needs no new review while the tip differs from the reviewed one in
    `docs/backlog.md` alone (`git diff --name-only <reviewed tip> <tip>`);
  - the doc-reviewer's and the reviewer's reports and the runs of the passing tip stand beside
    that row
    ([multi-agent-playbook.md](multi-agent-playbook.md), "The gate dossier");
  - no fold follows, since a fold would only seed the next round's findings.
- **Cap on the PC: two panel rounds, then the hand-off** (author decision 2026-10-12), since the PC
  draws on the agent seat's quota:
  - every seat but the agent seat gives a strand at most two panel rounds;
  - only a dated author row in [`open-questions.md`](../open-questions.md) lifts the cap (author
    decision 2026-11-23);
  - the release comparison keeps the lift granted on 2026-10-11, before this cap (author decision
    2026-10-13), since its remaining edges were cut into backlog rows under that lift;
  - after round 2, only a finding of the severe class is folded: one that can let a scenario or a
    test pass falsely, leak a resource or a secret, lose data or break security;
    - a reviewer finding that the change does not do what it claims is of that class too, since §7
      lets no such commit land (author decision 2026-11-23);
    - so is a production line written before its red test (§4), since a test never seen red can
      pass falsely;
  - from round 2 on, each finding's disposition names its class, severe or not, before any fold,
    so a fold outside the class shows before it is made;
  - a delta run of the gate agent that raised it re-proves that fold;
  - a finding of that class the re-proof run raises goes to the author, not into another fold;
  - every other finding becomes a backlog row;
    <!-- MODULE agent-seat -->
    - the row carries its seat marker ([README.md](README.md), DOC-5);
    <!-- /MODULE agent-seat -->
  - then the strand goes to the author for landing, the hand-off carrying a time estimate.
  - Why: the severe class is what does the most harm once landed; the rest costs less as a backlog
    row than as another round.
- **Every status to the author carries a time estimate** (author decision 2026-10-13): a full
  panel round costs about an hour, so the author can plan around the rounds left.
- **Early-escalate** when two consecutive rounds raise the *same* finding: stop and take it to the
  author, and re-plan rather than fold again.
  - Why: a repeated finding usually means the plan or the spec is wrong, not the code.
<!-- MODULE agent-seat -->
- **No cap on the agent seat** (author decision 2026-10-07): where `AGENT_SEAT` reads
  `unattended` ([dev-environment.md](dev-environment.md), "Where agents run"), the loop runs
  until PASS, an unattended seat having nobody to wait for. Three brakes replace the cap and early
  escalation there (author decision 2026-11-23):
  - a status update in the session's chat every three rounds (the phase, rounds run, findings
    open, recurring findings and reversed fixes, a time estimate);
  - a question to the author only where a fix would reverse an author decision or the brief
    leaves the scope open (author decision 2026-10-17);
  - a finding that recurs, or a fix that reverses an earlier round's, gets its root cause fixed.
<!-- /MODULE agent-seat -->
- **Process-only FAILs** (whitespace not separated, KB not updated, commit-message format) are
  cheap mechanical fixes; they don't consume a round.
  - On the PC, such a fix after round 2 forces no further panel round (author decision
    2026-10-13): the doc-reviewer, the reviewer and the runs are repeated at the fixed tip, no
    lens runs again, and the dossier check closes it.
- Fixes are folded into the commit they belong to (§7), never a "fix review" commit.
  - They are folded at the moment of fixing rather than at the end of the round.
  - The gate reads the branch log before the diff.
  - A commit that names a repair of an earlier commit is itself a FAIL.
  - An "and" joining two clauses of a commit message is the signal to split.
- **A gate agent's report is an input to the next gate agent**, not a message to the author alone:
  every lens, doc-reviewer and reviewer verdict of an earlier round reaches the next reviewer run
  verbatim in the gate dossier. The gate dossier check refuses an incomplete dossier before a round closes
  ([multi-agent-playbook.md](multi-agent-playbook.md), "The gate dossier").
- **The reviewer runs in round 1 beside the lenses** (author decision 2026-10-12), so its findings
  ride in the same round instead of restarting the panel at its end.
  - From round 2 on, it runs again on the delta after every fold (author decision 2026-10-30),
    since its report names the tip under review.
  - It also runs when a panel finding was rejected rather than fixed or filed, to judge the
    rejection.
  - The dossier check requires its report and traces its findings as it does the lenses' and
    the doc-reviewer's.
<!-- MODULE security-lens -->
- **The security lens joins the panel** (author decision 2026-10-15), since no other lens reads the
  app as an attacker; when and how: [multi-agent-playbook.md](multi-agent-playbook.md), "Cadence".
<!-- /MODULE security-lens -->
- **The gate verifies the claim, not the intent.** Every claimed fix is stated with the evidence
  that it landed on the branch (`git show <sha> -- <file>`, produced from the strand's worktree with
  `git -C`, never from an inherited working directory), and the reviewer ticks off each of its own
  prior findings **by name**, so a fix that never arrived shows up as a missing tick rather than as
  an unread absence.
- **A causal claim is made only after the mechanism is shown**, e.g. by a probe that widens the
  suspected window and demonstrates the failure. Absent that, say "cause not established", keep the
  hardening as hardening, and file the unexplained failure as live work instead of closing it. The
  same holds for a claim about how the code behaved *before* a change: probe a worktree at that
  commit (`git worktree add --detach <root>/lens-<lens>-<topic>-base <base>`, under the worktree
  root of §7), do not narrate it.
- **A finding the panel raises but does not fix is live work.** Every "pre-existing, not this
  strand" gets its backlog row or `open-questions.md` entry **in the same strand**, written *before*
  the round closes, so the gate sees the trace instead of having to create it.
  - Every low of a round that passes, a lens's and the doc-reviewer's included, goes into the lows
    row after the round instead (above).
  - A trace to an existing row names the finding in that row, unless the row already states it.
    - "Covered by the row's acceptance" is no trace.
    - Why: a finding the row does not name is one no later pass over the row rediscovers.
  - The reviewer checks each trace at its row, at the tip.
- **A negative claim owes the search that would have found the thing.** "It is not in the repo" is
  stated together with the command that came up empty, looked where the docs say the thing lives
  and where its siblings live, so the search itself can be judged.
- **A claim may claim no more than what was produced.** Absence on one surface is evidence about
  that surface, never about the whole: name the surfaces searched before writing "X does not
  exist", and grep the evidence already on disk for a counterexample to the sentence being written.
- **A measurement states its coverage beside its result**: items read against items present, and
  the comparison fails when the two disagree; a parser that silently matches a subset reads exactly
  like a clean result.
- **A bound is set on a measurement of real data.**
  - A performance or limit decision first measures on a copy of real data.
  - The author then sets the bound it must meet.
  - The copy stays under the worktree's own `build/` (§7, "Deleting project content").
  - Why: a bound set without the real figure is a guess.
- **A rule is never widened by the strand that needs the widening.** Propose the change as an
  author decision like any other; if it is not granted, drop the thing that needed it.

Governance is tool-neutral; agent-team mechanics (who runs falsify vs. review, harness-specific
history-rewrite recipes) live in [multi-agent-playbook.md](multi-agent-playbook.md).
