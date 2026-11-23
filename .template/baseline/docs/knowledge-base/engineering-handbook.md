# 08: Engineering Handbook (how the code should look & how we develop)

This is the standard every change is held to. It exists so the project stays clean as it grows and
so any contributor (human or AI) produces consistent work. A summary lives in each tool's entry file
(Claude Code: [`CLAUDE.md`](../../CLAUDE.md)); this is the detail.

## 1. Architecture rules (hexagonal)

The **dependency rule**: dependencies point *inward*. Inner layers never import outer ones.

```
domain        : entities, value objects, domain logic. NO framework.
application   : use cases + the PORT interfaces they need. NO framework.
adapters/in   : inbound adapters (REST controllers, scheduled jobs, CLI) that drive the application
adapters/out  : outbound adapters (external clients, repositories, files, logging) that implement the ports
config        : the composition root: framework wiring + configuration binding
```

> **ADAPT:** map these layers to your actual packages/modules (documented in
> [architecture.md](architecture.md)) and name the automated architecture test that enforces the
> boundary.

- Inner layers (`domain`, `application`) must not import the framework, drivers, or any adapter
  package. Enforce this with an automated architecture test that fails the build on a violation.
  **Keep it true.**
- Every outbound dependency goes through a **port** (an interface owned by the inner layer). New
  external integration ⇒ define a port first, implement it as an adapter. If a legacy integration is
  still wired in without a port, track closing that gap in the backlog.
- **A cross-cutting guarantee holds by default** (examples: access control, the thread pool a
  request runs on): a new unit is covered the moment it exists, with no annotation or list to
  remember.
  - A missing or empty configuration of it fails closed.
  - Why: a guarantee each unit must opt into is missing exactly where someone forgot it.
- **"NO framework" means no application or hosting framework and no game engine** in the inner
  layers.
  - A small, well-encapsulated library is allowed there.
  - Such a library sits behind one wrapper that exposes the domain's own types.
  - The architecture test names that wrapper as the only place that may import the library.
  - A library that does no I/O is no outbound dependency, and therefore needs no port and adapter.
  - Why: the wrapper keeps the library's types out of the domain.
- The composition root is the single place that knows concrete classes.

## 2. SOLID, applied here

- **S**: one reason to change per class (e.g. parsing ≠ valuing ≠ persisting). Split a class that
  mixes its capability with resource lifecycle (e.g. scraping/parsing logic vs. driver/connection
  management).
- **O/L**: extend via polymorphic dispatch (visitor, sealed-type switch, strategy), not by editing
  `switch`/`instanceof` chains. **No default fallback in exhaustive dispatch:** declare the dispatch
  point abstract and never give it a base default implementation that silently routes to a
  catch-all. Adding a new case must be a **compile error** until its handling is explicitly wired.
  A catch-all branch is allowed only where a concrete case deliberately routes to it, never as a
  base default: a convenience default reintroduces exactly the silent fall-through bug that
  compiler-enforced exhaustiveness exists to prevent.
- **I**: small, role-specific ports, not one fat interface.
- **D**: application code depends on ports; adapters depend on the domain, never the reverse.

## 3. Clean-code rules

- Intention-revealing names; no abbreviations that aren't domain terms. No joke/placeholder names;
  when found, clean them up, never copy them.
- **Code must be self-explanatory and stand on its own, understandable without any external document**
  (backlog, tickets, chat history, design notes). Names carry the meaning: name things by what they
  *are or do* (their domain/capability role), so a reader with only the source can understand them.
  A corollary: names must never reference tracker/backlog IDs (`D2`, `H8`, ticket numbers) or the
  task that produced them (name a test after the behavior, e.g. `PriceCalculationAcceptanceTest`,
  not `D2AcceptanceTest`).
- **The same principle governs the durable docs** (the KB, the decisions log,
  [`process-learnings.md`](../process-learnings.md)): identify things by symbol/behavior/file/concept,
  **never** by a backlog shortcode. Backlog items keep their IDs **in the backlog itself**; a code may
  appear in prose only as an *optional secondary pointer* to a **still-live** item. **Completing a
  backlog item removes its row *and* every shortcode that referenced it** (in other rows' prose and
  across all docs), so no dangling code survives and no reader needs `git` archaeology to resolve a
  reference.
- Small methods, early returns, no deep nesting. No commented-out code in commits.
- **Strict data instead of defensive reads.** A value that cannot be absent is declared so at the
  type and at the schema (every database column `NOT NULL`), not checked at each read; read paths
  carry no null branches for states the schema forbids. Whoever wants to allow an absent value owns
  the handling of that absence everywhere it can surface, and says so. The rule exists because the
  quiet failures are worse than the loud ones: an ORM that reads a SQL `NULL` into a primitive
  integer as **0** raises nothing, so a nullable amount would not crash, it would silently change a
  computed total. Declaring the absence impossible removes both the defect and the code that guarded
  against it.
- **Every stored time is a UTC instant.**
  - No code reads the host's default time zone.
  - The presentation applies the zone its readers see.
  - Why: a wall-clock value without its zone is ambiguous inside the hour the clocks go back.
  - A bound in days counts calendar days in that zone, not multiples of 24 hours.
    - Why: a clock change must not move a moment across the bound.
- **A paged read is in a total order:** its sort ends on a unique key.
  - Why: a page boundary inside a run of ties neither repeats a row nor drops one.
- **A name a request supplies (a sort column, a direction) reaches a query only through an
  allowlist** that maps it to the code's own constant.
  - An unknown name is refused.
  - Why: no column name from a request reaches the SQL.
- **A failed read is no value:** a script checks each read's own status before it compares or uses
  the result (the deploy's instance: [build-run-deploy.md](build-run-deploy.md), "Deploy
  pre-flights").
  - Why: two failed reads compare equal.
- **A rule lives in one place.**
  - A copy a client cannot do without (a check a page makes before the service makes it again) is
    held to the original by a parity table: the same cases with the same verdicts, run in both
    suites (each keeps its own scenario: §5, "One rule, stated once across the suite").
  - Why: a second copy drifts, and only a shared table of cases shows the drift.
- **No logic in constructors; field assignment only.** Constructors must not run logic or side
  effects (no IO, no `init()`/`ensureTable()`-style calls). Put "construct + initialize" in a static
  **factory method** or a lifecycle hook. Logic in a constructor breaks testability and violates SRP.
- **Avoid mutable static/global state; treat your language's `static` keyword (or equivalent) as a
  smell.** It creates hidden cross-instance / cross-test coupling and breaks testability and
  isolation. Prefer dependency injection, instance state, or a proper seam. Boot tests that need
  state written by production code at startup and read by the test use an **injected, DI-shared
  recorder**: a container-scoped bean provided by a test factory (DI scope, **not** the GoF
  static-`getInstance` Singleton antipattern), never static flags (see [testing.md](testing.md)).
  The rare defensible case is a static *initializer* doing one-time setup that genuinely must run
  **before** the framework context boots (e.g. seeding/deleting a test DB before repositories
  connect), accepted in **test** code; in production code, justify it hard. A costly run-scoped
  test resource is the other case, under testing.md's rules ("No shared static state").
- **Comments and doc comments are an absolute no-go; make the code say it.** No `//`, no `/* */`, no
  doc-comment block (Javadoc, JSDoc, docstring-as-API-doc) on classes, methods, records or fields,
  and none in config or infrastructure. Non-obvious knowledge goes into an expressive **name**, a
  **test name**, the knowledge base, or [`open-questions.md`](../open-questions.md), never into a
  comment and never into both. Where the *why* is a real constraint, restructure so the code carries
  it rather than annotating it.
  - Reviewers (human or agent) flag **every** comment a diff adds, not only the ones they judge
    unnecessary: "is this one necessary?" is the judgment call that let ten of them through three
    review rounds in the origin project.
  - It is enforced mechanically, not by memory: `hooks/content-gate` refuses any diff that adds a
    comment line to a listed source extension or a shell script recognised by its shebang (see
    [`hooks/README.md`](../../hooks/README.md)).
  - **There is no opt-out.**
  - What the gate refuses is deleted, not reworded.
  - Carve-out: every shipped tooling directory with its own self-test keeps its comments as prose by
    design, `hooks/`, `stacks/` and `agent/` among them.
    - A project's own tooling directory with a self-test joins through content-gate's ADAPT list
      `comment_exempt_dirs`.
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
  - The scan covers the file families its lists name; a project adds its further ones to the lists
    in content-gate's ADAPT block (`hooks/README.md`, "The checks").
  - Ignore files carry scaffolding prose for the adopter; `/adopt` trims it.
- **No dead code & no undeclared dependencies** (e.g. an unused class importing a library absent
  from the build manifest: delete it).
- **The code/build is the single source of truth; docs must never duplicate volatile facts.**
  Versions of {{TECH_STACK}} components, dependency lists, file/test counts, and other build-derived
  details live in the build manifest and the code; docs point to them, never restate them (restated
  facts rot silently). Prose states only stable, conceptual things. If a doc ever contradicts the
  build, the build wins and the doc is fixed in the same change.
  - A count over the suite or a code path (its scenarios, or the calls, reads or steps a path
    makes) is such a fact too: the KB describes the suite or the path without it.
- **Prefer immutable value types; avoid getter/setter boilerplate.** Model value objects, DTOs, and
  config as the immutable value construct {{TECH_STACK}} offers (example: Java `record`s). Don't
  write `getX()`/`setX()` unless a framework strictly requires it; generated value accessors are
  fine, they are not bean getters, and public final fields are acceptable for plain DTOs where
  idiomatic. Return `Optional`/option types instead of sentinels.
- **The logger is the last constructor parameter**: collaborators first, the logger (incidental
  infrastructure) last.
- Constants/config over magic values; **no secrets in source** (tokens, credentials → config).
- **Warnings are errors.** Compiler and lint warnings are build failures, enforced in the build on
  every compile (example, Java/Gradle: `-Xlint:all` + `-Werror` on every `JavaCompile`). The default
  is to **fix** them, not suppress; genuinely obsolete lint may be excluded, but only deliberately,
  per rule, with recorded rationale. When unsure whether a warning should be fixed or excluded,
  **ask the author**.
  - Where a rule flags code that is right on purpose, restructure the code so it states that
    intent, rather than disabling the rule for the line.
    - Why: a disable for the line silences every later finding of that line too.

> **ADAPT:** wire warnings-as-errors into your build and list your deliberate lint exclusions with
> their rationale.

## 4. TDD workflow (red → green → refactor → commit)

Runs **inside** the approved `@wip` scenarios of a feature (§5): the scenarios state what "done"
means, the TDD cycles drive them green, step definitions included.

1. **Red**: write the smallest failing test that expresses the next behavior. Run it and **watch it
   fail for the right reason**; a slow full build is no licence to skip red, run the *focused* test.
   **The red run is evidence**: the failing test's name and its failure line go into the step's
   report (or the plan) before the implementation exists; a mutation of the finished code is no
   red run. A test written beside its implementation is anchored to that implementation's shape,
   which is what produces assertions fitted to the data.
2. **Green**: make it pass with the simplest code.
3. **Refactor**: improve the design with tests green.
4. **Commit**: **one full cycle = one commit**, via the §7 protocol (a message a human review
   confirms; never pushed). Red and green stay uncommitted, local steps *inside* the cycle; only the
   refactored, green result is committed, so every commit is atomic and independently revertable.

- Domain/application logic is unit-tested **without the framework**: a plain test runner plus an
  assertion library, fast (example: JUnit + AssertJ on a Java stack). The inner loop runs
  `./verify focus <path>`; the feature's scenarios run through `./verify bdd`, and the last cycle of
  a feature arms them (§5).
- One behavior per test; arrange/act/assert; test-authoring rules in §6.
- When a class's tests repeat the same act, wrap it in a short-named helper (e.g. `snapshot()`), so
  the call itself reads as the Act rather than an expression buried inside each assertion.
- Bug fix ⇒ first write a test that reproduces it, then fix.
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
- **A production line written before the red test that needs it is a FAIL of the round:** the line
  comes out, the test is written and seen red, then the line goes back in.
  - A recorded mutation never stands in for that red run.
  - Why: a test never seen red can pass falsely.
- **An existing test adapted to a changed behavior** cannot be seen red first; it pays that debt with
  a **recorded mutation**:
  - mutate the production code so the adapted assertion must fail;
  - run only that test class (`./verify focus <path>`) and keep the output;
  - revert the mutation;
    - in a file that holds other uncommitted work, from a saved copy of that file, never from git;
      - Why: a revert from git also discards the other uncommitted work in that file.
  - the record (what was mutated, the failing line) goes into the step's report like red evidence.
- **A new test over behavior older than the strand** cannot start red either: the step definitions
  of a catch-up (§5), a test that pins code before it changes, the pass case of a guard.
  - It pays with a recorded mutation per `Then` step or per case, as above.
  - The record goes into the step's report and the gate brief; no decision row records it.
    - Why: the test itself is the durable artifact, and the record lets the gate re-run the
      mutation.
  <!-- MODULE gate -->
  - With the gate module the record is a `mutation/` record that holds an `older: <reason>` line.
    - The check counts its failing lines for a new test only with that line.
    - The reviewer judges the reason as it judges an `exempt:` claim.
  <!-- /MODULE gate -->
  - Where a harness exists, this and the adapted test are the only substitutes for a red run.
- Where a deliverable genuinely has no test harness the stack supports (an ops shell script, say),
  the substitution is **mutation testing** and it is *declared*: name it and every mutation in
  [`open-questions.md`](../open-questions.md) in the same change, so a later gate re-runs them
  instead of taking a claim on trust. A mutation that lives only in a conversation is not a proof.
  - The commit that replaces the code a recorded mutation pins re-proves the mutation on the new
    code or strikes its row.
    - Why: a row on code that is gone claims a proof no gate can re-run.
- A reviewer (human or agent) may gate each commit on process + criteria adherence (see §9).

## 5. BDD (product-owner perspective): mandatory, and gated by the author

**BDD is not optional in this process.** Work on observable behavior starts with executable Gherkin
scenarios, the author confirms them, and only then does implementation begin. The scenarios, not
the plan text, are what "done" means, and they stay in the repository as the executable
specification.

**Scope.** Every feature with behavior a user, an operator or a caller can observe: new
capabilities, API and UI changes. A bug fix gets a scenario when the bug is visible at the
acceptance boundary; otherwise a reproducing unit test (§4) is its specification. A change inside
the scope of an existing scenario adjusts that scenario. **Exempt**: pure refactorings that change
no behavior, `[doc]` and process commits, and build/infra work. The exemption is *claimed
explicitly* by the planner and named in the plan; it is never the silent default.

**The artifact: `.feature` files, executed.** Scenarios are Gherkin (`Feature`, `Rule`, `Scenario`,
`Scenario Outline`) bound to step definitions and run by the project's acceptance runner. They are
the **primary review artifact between author and agents**, written in **product language** from the
stakeholder's perspective, understandable without reading code:

> *(example)*
>
> ```gherkin
> Feature: Contribution overview
>   Scenario: A customer's orders add up in the contribution column
>     Given the order book holds an order from "Miller" worth 700 placed on 2026-01-01 12:00
>     And the order book holds an order from "Miller" worth 500 placed on 2026-01-02 12:00
>     When the manager opens the contribution overview
>     Then the manager sees 1200 in Miller's contribution column
> ```

- **Declarative, never imperative.** A step names a domain fact or outcome ("the order is paid"),
  never a UI gesture ("click Save"); the step definition owns the *how*. Technical vocabulary
  (type names, hashes, endpoints) is a finding wherever the scenario's actor would not see it
  ("One actor per scenario", below).
- **One acceptance runner per system.** The runner is chosen at adoption and named in
  [testing.md](testing.md); in a multi-stack system it drives the whole system, and the other stacks
  keep unit tests only. The runner per stack, and the C++ rule (acceptance through an out-of-process
  Python runner), live in the template's stack presets.
- **A page of text keeps the scenarios a product owner asks for:** that it exists and is reached,
  its headings and structure, and what it must never show.
  - Its words are the text the author approved, pinned 1:1 by a test against an approved file
    ([testing.md](testing.md), "What keeps the suite trustworthy").
  - A figure it states is recomputed by a test of its own.
  - Why: the approved file shows every change of wording in the review, while scenarios on the
    wording would hold the gate on tightenings a reader judges better.
- **Feature files live where that runner expects them**, one place per system. A file over about
  ten scenarios is a split signal; `Rule:` groups the scenarios of one business rule and
  `Scenario Outline` carries the variants of one behavior.
- **English by default.** A project whose stakeholders read German may decide `# language: de` at
  adoption ([open-questions.md](../open-questions.md)); code, step definitions and docs stay English.
- **Step definitions are test code** and follow every test rule (§6; no comments; no logic that
  belongs in the domain). Reuse an existing step phrasing before inventing one; the runner's dry run
  lists the undefined steps, and that list is the glue work before TDD starts.
- **Each actor is driven the way it meets the system**: a person on a screen through the real UI in
  a real browser, a caller through the real API, an operator through the service's responses,
  health report and log.
  - Test doubles stand only behind the application boundary (a fake external source behind its
    port), never a live system.
  - Where the system runs as a service, each scenario gets a fresh real one
    ([testing.md](testing.md), "The acceptance runner").
  - Serving the page is not showing its data: the `Then` reads what the actor's surface shows.

  > **ADAPT:** a project that chose the in-memory model at adoption replaces this rule, the
  > completeness item below, §6's acceptance line, the preset step shape, testing.md "The
  > acceptance runner", the scenario falsifier's completeness item, the frontend falsifier's
  > description and UI bullet and the playbook's UI paragraph with the in-memory wording:
  > scenarios run at the application boundary through ports with in-memory fakes (a fake external
  > source, in-memory persistence), with no network and no live systems, and a user-visible feature
  > earns one scenario through the real artifact.

**Reading as the stakeholder.** The rules `falsifier-stakeholder` checks:

- **One actor per scenario**, a role such as a customer, an administrator or an operator, named as
  the one who acts or reads (`When the manager opens the contribution overview`).
  - A trigger with no person behind it (a scheduled run, another system's request) does not
    replace the actor who reads its outcome.
  - A `Then` asserts only what that actor sees or knows on their own surface: a screen, a
    response, a report, a log.
  - An address, a status code or an internal name stands only where the actor's surface shows it;
    elsewhere it belongs to the step definitions and the unit tests.
  - A description sentence starting "Today" may quote word for word what the actor sees today,
    technical words included; the `Then` never does.
  - The project's actors and the surface each reads: [testing.md](testing.md).
  - Why: an outcome the actor cannot see is one the product owner cannot check.
- **One rule, stated once across the suite.**
  - A rule counts wherever a scenario asserts it, in a second `When`/`Then` pair too, not only
    where a title names it.
  - Two scenarios asserting the same rule for the same actor with different data become one
    scenario, or one is dropped.
  - The same rule on two surfaces is one `Scenario Outline` over the surface, each example row
    naming its own actor.
  - A check a page makes and the service makes again, for a client that skips the page, is two
    rules: each keeps its own scenario.
  - Different actors seeing different consequences of one event stay apart, each asserting only
    what its own actor needs.
  - Why: duplication drifts, in a suite as in the docs.
- **Every outcome reads on its own:** a `Then` names the state it expects ("in descending
  order"), never one the reader must carry over from an earlier step ("again").
  - Why: a carried-over state makes the reader replay the scenario to judge one line.
- **Placeable time:** "Points in time" below (TIME-1 to TIME-9).
- **Checkable numbers:** an expected figure follows by mental arithmetic from the scenario's data,
  in small round amounts.

**Points in time: one calendar.** A reader places every moment without arithmetic, because the
value itself tells its role; the scenario gate checks each rule on the scenario text alone.

- **TIME-1 One year:** every date is in 2026.
  - A date the product shows as fixed text is quoted as it stands and is no moment of the
    scenario.
- **TIME-2 One month:** dates fall in January, from its first day on, in the order things happen.
  - Another date stands only where the claim needs it (a moment clearly outside a window, a "now"
    long after every entry), and each such date is one of a short list the project fixes.
  - Why: dates in one month compare at a glance.
- **TIME-3 Windows start on a round moment:** a window of days from the month's first day, a
  window of hours from 12:00 of a day.
  - The month holds no clock change, so a window is its plain length.
- **TIME-4 The time of day tells the role:** a recorded entry at 12:00; a scheduled run early in
  the morning after the entries it reads, at one time the project fixes.
  - Another time only where the claim rests on the minute:
    - a window's edge and one minute past it (11:59 beside 12:00);
    - two entries in one minute;
    - 00:00 for an entry that must fall just outside a window.
  - Steps that must fall inside one window without testing its edge take consecutive hours of
    one day (12:00, 13:00, 14:00).
- **TIME-5 "Now" only where the claim depends on it:** the clocks are set once, in the first
  `Given` of the scenario or its Background, and nowhere else.
  - Why: a clock set later shifts moments the reader has already placed.
- **TIME-6 Oldest first:** steps and table rows run in the order the moments happen, unless the
  claim is about the order in which a source delivers them.
- **TIME-7 A displayed time repeats its source:** a `Then` that shows a moment shows the entry's
  or the run's own value; where the surface shows it in another zone, the `Then` quotes it
  converted for that date's offset.
- **TIME-8 One keyword per time step,** where the runner binds a step without its keyword: the
  tense carries the role.
  - The past (`has run`, `ran at`) is a precondition under `Given`; the present (`runs`) is the
    action under `When`.
  - A time step takes one of the two and never appears under the other.
  - A lasting state is no event: it stands under `Given` in the present
    (`the clock reads 2026-01-31 12:00`).
  - Why: such a runner runs a precondition reused as an action without complaint.
- **TIME-9 A moment that is neither an entry nor a run is placed by an event:** a login or a
  request stands relative to an event its step names (`7 days after logging in`), never at a
  clock time of its own.

> **ADAPT:** keep this calendar or replace its values in place: the year (TIME-1), one month
> without a clock change (TIME-2), the roles your scenarios time and the time of day of each
> (TIME-4), and the few dates outside the month a claim may need. Dates are written
> `2026-01-01 12:00` unless the scenario language's locale writes them otherwise. A project whose
> scenarios place nothing in time drops the frame, TIME-1 to TIME-9 and the "Placeable time" rule
> with it, and the scenario gate then checks none of them; the agent definitions and
> `/bdd-catch-up` that name the TIME rules then drop that mention too.

**Completeness (what the author confirms).** The planner declares the scenario set to be the
*whole* acceptance of the feature, checked against this list, and names what is deliberately out of
scope:

- the happy path;
- every business rule and each of its variants (`Rule:`, `Scenario Outline`);
- the error, empty and boundary cases a stakeholder would name;
- for a user-visible feature, the rules the UI applies on its own beside those the service
  computes.

An implementation that later needs a behavior no scenario names has found an incomplete set; the set
goes back to the author before that behavior is written.

**The flow (mandatory order):**

1. **Draft the `.feature` up-front**, before any production code, and run it through the
   **scenario gate**: `falsifier-scenario` attacks it as a specification (ambiguity, tautology,
   untestable steps, coverage), `falsifier-stakeholder` reads it as a product owner would and
   against every `.feature` file (the rules of "Reading as the stakeholder"), the `reviewer` in
   scenario mode gates on this section with both reports; fix and re-gate (when it passes and when
   it ends: §9).
2. **► Author gate 1: the author confirms the scenarios and their completeness.** A real gate, not a
   notification: implementation does not start until the author has read the scenarios and said yes
   to the set. Wrong scenarios are cheap here and expensive after three TDD cycles have been built
   on them. The scenario gate's open lows arrive unfolded (§9).
3. **Commit the confirmed scenarios tagged `@wip`**: the default run excludes `@wip` (each runner's
   filter is the stack preset's business), so the build stays green while intent is captured. The
   tag stays committed until the arming commit (step 5).
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
   author** as a changed scenario; it is never quietly reinterpreted to match what the code now
   does.
5. **Arm the scenarios** in the last TDD cycle of step 4: that commit also removes `@wip`, and no
   commit of its own only removes the tag. From here `./verify all` runs them on every commit and
   at every gate.
   - A unit tagged on its own level is armed the same way, by the commit that turns its last
     scenario green.
   - Why: a commit that only removes a tag carries no change worth reviewing alone.
6. **Refactor the feature** under the armed net: improve the design across the feature with the
   scenarios and unit tests green, one green commit per step, behavior unchanged. A change that
   needs new behavior or a new port is not a refactoring; it goes back to planning.
7. **The panel and the review gate** (§9, the playbook), then **author gate 2**, the review of the
   rebased tip, then the landing (§7).

**Projects that predate their scenarios.** An existing codebase adopting this process gets its
scenarios before any feature work, through the `/bdd-catch-up` skill:

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

- **Many** fast unit tests (domain values, parsing, core computations).
- **Some** adapter/integration tests (each adapter against an in-memory or lightweight double of its
  real technology, e.g. an in-memory database).
- **Few** acceptance tests (the end-to-end use-case flow through the real surfaces against a fresh
  service) and a thin smoke test for wiring.
- Assert behind clean ports, not against raw external formats: fuzzy assertions on raw output
  (HTML similarity matching and the like) are brittle; put the output behind a port, then assert on
  the port's types.
- **Deterministic, never wall-clock-dependent.** A test must pass on *any* hardware, however slow
  (a slow run may simply take longer); correctness must never hinge on a `sleep`/timeout threshold.
  Waiting on an async event means waiting on a **real completion signal** (a latch awaited with no
  timeout, a future, a condition), released on both the success *and* the failure path so a bad run
  fails an assertion instead of hanging. A timeout or retry is a *mitigation*, not a fix; never
  "stabilize" a flaky test by bumping a timeout or adding retries. (See [testing.md](testing.md).)
  - **Exception: a test that must wait on real elapsed time.**
    - The plan's test list names it, and the author decides it before red.
    - A dated decision row in [`open-questions.md`](../open-questions.md) records it, and so does
      its inventory row in [testing.md](testing.md).
    - One exception may cover a class of such waits the project names, recorded once the same
      way: one decision row and one inventory row.
    - Its stated reason is its only licence.
    - A hang guard, a bounded wait that only stops a run that would otherwise hang, stands only
      inside such an exception and never decides the verdict.
    - Why: some behavior shows only against real time, such as a read that waits out a lock
      another connection holds; deciding each case by name keeps the rule strict elsewhere.
- **A production parameter that is slow on purpose is a setting whose default is the production
  value** (example: a password hash's cost).
  - The test environment sets a cheap value.
  - A test of its own pins the production default.
  - Why: the full cost in every test would slow each run of the suite, and the pinned default
    keeps the cheap value out of production.
- **No buried logic in test bodies.** Never hand-roll a case-record + loop + soft-assertion
  iteration; let the test framework iterate via parameterized tests. Default to one well-named test
  per distinct behavior with **literal expected values directly in the assertion**, not indirected
  through case-object fields; assert a composite outcome as a whole through an intention-revealing
  helper rather than one test per field. Keep test inputs minimal and noise-free so the reader sees
  exactly what matters, and extract repeated multi-field assertion blocks into small named assert
  helpers. Data-driven parameterized tests are the exception: they fit when one behavior is best
  shown by several inputs. When the fit is unclear, ask the author rather than defaulting rigidly.
  (Why: self-explanatory tests with visible expected values make each case's purpose obvious at a
  glance; buried loops and value indirection hide which behavior is under test.)
- **Arrange discipline: shared, non-test-relevant preconditions live in the per-test setup hook
  (`@BeforeEach` or equivalent), not each test body.** A test body holds only the arranges that make
  its *act* + *assert* legible; common infrastructure setup (waiting for app boot, base-URL config)
  is hoisted to the setup hook rather than copy-pasted into every test. Duplicating a precondition
  across tests is noise. The exception: when the precondition *is* the behavior under test (e.g. a
  smoke test asserting that boot completes), it stays in the test where it is the act/assert.
- **A test builds a value with several components of the same type by name, never by position.**
  - Where the language has named arguments, it uses them.
  - Otherwise a fixture builds the value, with one method per component and an empty default for
    each one it is not told.
  - Why: two same-typed values swapped by position still compile, and the test passes while it
    proves the wrong thing.
- **Tests rank above production code** and get the greater care of the two. Strict
  arrange/act/assert with **the act as its own named value**: `int status = statusOfGet(path);` then
  assert on `status`, never `assertThat(get(path).getStatus())`. A custom assert helper is the other
  allowed shape and holds the same structure inside.
  - A test rule a linter can express is enforced by the linter in the build, not by review alone.
    - Why: review alone let an act inside the assertion through five panel rounds in the origin
      project.
  - Until a linter enforces the act rule, each commit that adds or edits a test first greps the
    staged test files for an act inside an assertion (`expect(<call>(`, `expect(() =>`,
    `assertThat(<call>(`) and splits each hit into a named act.
- **The test name carries the intent, not an assertion message.** `.as(...)`/`.describedAs(...)` and
  their equivalents are for the exception (several inputs where the failure output would leave the
  reader guessing, or a proof by absence). A message saying something the name does not is a name
  that needs rewriting. Read every test name against its **own assertion**: a name claiming an empty
  result must be backed by an assertion on an empty result.
- **A behavior is pinned once, at one level.** Duplicate coverage is maintenance cost, not safety:
  when a dedicated test class takes a behavior over, the older ad-hoc assertions about it are removed
  **in the same change**. An assertion belongs only in the class whose subject it is.
- **A test for an injected seam asserts that the seam was driven**, not only the outcome it enables:
  count the fake's calls, read the fake clock. The cheap check is "would this assertion still pass
  with the production collaborator wired in?" If yes, the test is fake-green.
- **A refusal test asserts its own reason**, not only that the run was refused.
  - A strand that adds a refusal earlier in a sequence re-runs the existing refusal cases against
    their reasons.
  - Why: a case that checks only the refusal passes when a new, earlier refusal takes it over for
    another cause.
  - `hooks/self-test` is its stated instance: one reason case per refusing leg
    ([hooks/README.md](../../hooks/README.md), "The hooks").
- **Proving a run really executed.** A test run is judged by proof that it *executed*, never by its
  exit code. Deleting outputs is not enough where a build cache is on: a restored cache entry
  rewrites the result files, so exit code and result count are both an earlier run's artifacts. The
  gate build therefore disables the cache, and it counts only when the test task's line stands bare
  of any `UP-TO-DATE`/`FROM-CACHE` marker **and** the result count is read alongside it. This matters
  beyond bookkeeping: load-sensitive failures only appear when the suites really run, and a cache hit
  hides exactly that class. For a load-sensitive change, two consecutive clean runs are the bar, and
  a cached second run is not one of them.

## 7. Git & commits (author's rules, strict)

- **A human review confirms every commit message, before the commit or at the gateway.**
  - Before: the author approves the plan's ordered message list, and the implementer commits each
    message as written, stopping to report when one no longer fits.
  - At the gateway: where the plan names no message, the implementer words a protocol-conform one
    itself, and the author's review of the branch log confirms it or rewords it (scripted, below).
  - The main session's own commits: propose exactly one message, get the author's confirmation,
    THEN commit.
  <!-- MODULE agent-seat -->
  - On the seat (dev-environment.md, "Where agents run") the main session words its own messages
    itself and commits.
    - The author's review of the branch log on the strand's pull request confirms or rewords
      them.
    - No proposal waits for an answer there, since nobody answers on an unattended seat.
  <!-- /MODULE agent-seat -->
  - Why: the hooks check a message's form; only a person checks that it says the right thing.
- **No agent pushes `main` or a tag in any form** (a push, a merged pull request, an API call); who
  lands follows the project's landing model ("Landing models" below).
  - In the core `git push` sits on the deny floor, so no agent pushes at all
    (working-with-ai-agents.md, "Autonomy within guardrails").
  <!-- MODULE agent-seat -->
  - **On the seat an agent pushes its own strand,** where `AGENT_SEAT` reads `unattended`
    (dev-environment.md, "Where agents run"), and only through `agent/push`
    (working-with-ai-agents.md, "Permissions & autonomy").
    - On a supervised machine of the project a strand stays a local branch: never pushed, no
      pull request.
    - Every pushed branch is the strand's own `claude/<topic>`.
    - The session that owns the strand pushes it, never a subagent.
      - A written rule: neither `agent/push` nor the guard tells one session's worktree from
        another's (working-with-ai-agents.md, "Permissions & autonomy").
    - The branch is opened as a pull request against `main` at its first push, with
      `agent/gh pr create`.
      - A chained strand opens its pull request against the branch of the strand before it.
        - Why: the pull request then shows only the strand's own commits.
      - Once that strand lands, the base moves to `main`, by
        `agent/gh pr edit <number> --base main` where GitHub does not move it.
    - `agent/push <worktree>` pushes the worktree's branch.
      - Three safeguards:
        - A lease: every rebase needs a forced push, and a lease refuses it once `origin` holds
          a tip other than the one this checkout last fetched.
        - `--force-if-includes`: a background fetch moves the remote-tracking ref and voids a
          bare lease, and the option refuses the push unless the branch's reflog reaches that
          ref's tip.
        - The refspec written out: it pushes that one ref alone.
      - After the playbook's reset-free fold the push is `agent/push <worktree> <remote tip>`,
        with the remote tip fetched before the fold.
        - Why: the fold's branch swap drops that tip from the branch's reflog, so
          `--force-if-includes` refuses, and the lease then names the tip it expects.
    - No upstream is set.
      - Why: with one, `git branch -d` tests the branch against its own remote copy instead of
        `main`, so the landing's merged check would pass on unlanded work.
      - git sets one by itself on `git worktree add` from `origin/claude/<topic>` without
        `--no-track`, and on `git switch` or `git worktree add` onto a `claude/` name only
        `origin` holds.
    - A `[wip]` commit is never pushed: no push from a tip whose range above `origin/main` holds
      a subject that starts with `[wip]`, in any case, after any leading blanks.
    - `hooks/pre-push` holds the branch and `[wip]` rules above for every pusher, the author
      included ([`hooks/README.md`](../../hooks/README.md), `pre-push`).
      - It also refuses a tip that reaches a recorded commit.
      - It also refuses a merge commit in the pushed range.
    - On GitHub the rulesets refuse the seat's credentials every push to `main` or a tag
      ([git-state.md](git-state.md), "What GitHub enforces").
    - The one push that is not the agent's own strand: `agent/push --delete <branch>` removes a
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
  - Why: it holds in every landing model, so SHARED, which the classifier reads, can carry it.
- Message = **single line**, **starts with a present-tense verb** (after an optional `[doc]` tag),
  states what the commit actively changes. **No body. No `Co-Authored-By` or tool footer.** English.
  - Advice, which no hook measures: keep the subject brief, and read an "and" that joins two
    clauses as a hint that the commit holds two changes.
  With a ticket system the team's key pattern joins, as prefix `[KEY] ` before the optional `[doc]`
  or as suffix ` (KEY)`; `hooks/commit-msg` carries the pattern, the position and whether a key is
  required, set at adoption. A key never replaces the verb-first subject.
  - ✅ `Add order value calculation to the evaluator` · `Remove dead legacy parser` · `[doc] Groom the backlog current-status section`
  - ✅ with a ticket system: `[PROJ-12] Add order value calculation to the evaluator` · `Add order value calculation to the evaluator (PROJ-12)`
  - ❌ `feat:`/`fix:`/other conventional-commit prefixes · multi-line bodies · `Co-Authored-By:` lines
- **`[doc]` prefix for pure documentation/process/meta commits**: touching only `docs/`, `.claude/`, or
  top-level `*.md`, with no product-code/build change. Code & build commits stay **bare** (verb-first).
  `[doc]` is the *only* permitted prefix; it keeps `git log` two-tier and scannable.
- **Docs that describe a code change ride in that commit**, never a standalone "update docs after X"
  follow-up (it can't stand alone for revert/cherry-pick). Before staging a doc hunk, run the
  **revert check in both directions**: "is this sentence still true after reverting just the code
  commit?" (if yes, it belongs elsewhere) and "is it true *at* the commit it rides in?" (`git grep
  <symbol> <sha>` against that commit's own tree, never the working tree, which is the tip and
  always agrees). Only backlog, roadmap and decision hygiene, and a measurement over the combined
  effect of several commits, earn a standalone `[doc]` commit, and its message says which it is.
- **LF line endings** enforced via `.gitattributes` (`* text=auto eol=lf`; binaries marked).
  Whitespace/format changes go in their **own** commit, never mixed with logic.
- One logical change per commit; **cohesion test**: group parts together only when they can't stand
  alone (would reverting or cherry-picking one without the others break the build or leave the tree
  inconsistent?); otherwise keep them separate. Keep `main` releasable; `./verify all` green
  locally before committing.
- **Slice the commits before the first edit.** The plan names the commits it will produce, in order,
  each green on its own and reviewable in one sitting. A commit that renames, changes behavior, moves
  config and carries unrelated doc work at once is bad style however green it is: **35 changed files
  is a smell, not an achievement.** Rules of thumb: a rename or move is its own commit; a behavior
  change is its own commit; documentation describing a code change rides with it, unrelated
  documentation gets its own `[doc]` commit; a finding that falls out along the way is its own commit
  rather than a passenger. Where a slice cannot stand green alone, that is the signal to **reorder**,
  not to merge it into a bigger one.
  - A replacement may pass through states no release carries, the old and the new side by side.
    - Each commit stays green and reviewable alone.
    - The plan names the commit that removes the old.
    - Why: the switch then moves in reviewable steps instead of one commit that swaps everything.
- **The author reviews the plan and gives the go on it before the first edit.**
  - A `[doc]` or build/infra strand may get the go at the start and the plan's review only at the
    gateway, with the rest of the strand.
  - A refactoring keeps the review before the first edit.
  - Why: a refactoring cuts production code; a `[doc]` or build/infra strand does not.
  - An early go may come before the author has seen a commit message; the gateway's review of the
    log then confirms the messages (the first bullet of this section).
- **Every commit compiles and is green on its own, not just the tip.** A build-file or dependency
  change belongs in the **first** commit that uses it, not the one it was written for. Verify it, do
  not derive it: the "the rebased tip is byte-identical to `main`" argument covers the tip only, so
  walk the commits before making the claim:
  - list them with `git rev-list --reverse main..claude/<topic>`;
  - create a detached walk worktree, `git worktree add --detach <root>/<topic>-walk <tip>` with
    `<root>` the worktree root ("The worktree root" below), by absolute path, since a relative one
    lands below whatever the working directory is;
  - then, per commit, `git -C <walk worktree> switch --detach <sha>` and
    `<walk worktree>/verify all`;
  - each is a call of its own with the SHA written out, without a `$(…)` or a loop variable, since
    such lines were refused at times (observed, cause unknown: working-with-ai-agents.md, "Command
    shapes that keep an auto-mode session unattended"); the `git -C` step meets the auto-mode
    classifier (working-with-ai-agents.md, "A rule that pairs an inner `*` with the `:*` suffix
    does not fire").
  <!-- MODULE guard -->
  - With the guard module the step is `git -C <walk worktree> checkout --detach <sha>`, the spelling
    the guard approves (working-with-ai-agents.md, "The shell-shape guard").
  <!-- /MODULE guard -->
  <!-- MODULE gate -->
  - With the gate module `gate/walk <strand worktree> <walk worktree>` does all of this, by
    absolute path and as one command, and writes the log of each commit's code tree into the
    strand's dossier (multi-agent-playbook.md, "The gate dossier", which lists its refusals and
    logs).
  <!-- /MODULE gate -->

  A doc or test reference likewise lands no earlier than its referent.
- **A strand's shape follows the work, not the plan it started with.** A planned commit count is an
  estimate: when the work needs more commits, it gets them; never inflate one commit to honor the
  original estimate. Clean code covers the history too.
- **Always able to build, not always allowed to deliver.** `./verify all` must be unconditionally
  able to run green: never attach vulnerability or quality gates (CVE thresholds, quality metrics)
  to the build/check tasks the inner dev loop runs. Such gates live on separate on-demand tasks or
  CI stages, each with an explicit suppression escape hatch for consciously accepted findings.
  **Deliver only CI-produced artifacts, never a local build.** (Why: an external or transitive issue
  the developer cannot fix on the spot must never block the TDD loop; gating is a delivery concern
  and belongs where delivery happens.) The one exception, for a project without CI or a registry,
  stands in [build-run-deploy.md](build-run-deploy.md), "Release & deploy".
- **No secrets, no personal/host data, ever (hard rule).** Never commit sensitive information of any
  kind: passwords, tokens, API keys, private keys/keystores (`*.pfx`/`*.p12`/`*.jks`/`*.pem`/`*.key`),
  credentials, real personal/work emails or usernames, or machine/host/workspace details (absolute
  paths, host layout, infra). Treat the repo as public. Such data lives in gitignored `*.local.*`
  files or a secret store and is injected at runtime, never the repo. If a secret ever reaches
  history, purge it with a history rewrite **and rotate it** (deletion alone doesn't un-leak it).
  Watch the Claude Code "always allow" flow: it can write path-bearing rules into the committed
  `settings.json`; prefer bare commands that match the existing portable `Bash(<cmd>:*)` rules, and
  keep machine-specific permissions (e.g. an absolute-path `cd`) in `.claude/settings.local.json`;
  after using the flow, diff the committed `settings.json` against `HEAD`.
  For the **worktree workflow**, never "always allow" a `git -C <worktree> …` command: it matches no
  `Bash(git <sub>:*)` rule (probe record, row 11), so the flow would pin a host-path-bearing rule
  per worktree. What a `git -C` command meets instead: working-with-ai-agents.md, "Permissions &
  autonomy".
  - **An agent uses only the access its environment was set up with,** for the project's own
    remote and services.
    - In the devcontainer that is the forwarded access dev-environment.md names ("Variant A:
      devcontainer"); in a native checkout, what the author configured for the project's
      remote.
    - It opens no credential store, key file or token it was not handed, looks for none and
      works with none it comes across.
    - A task that needs access the session lacks is reported
      (working-with-ai-agents.md, "Refusals and limits").
    - Why: a credential found on the machine was given to someone else, for something else.
  - **Nothing of the project goes outside unasked:** beyond the project's own remote and the
    tools the KB's workflow names, an agent hands nothing of the project, its KB or the session
    to anyone or anything without the author's word for that item.
    - A documentation lookup carries no project name, path or data.
    - Why: what has left the machine cannot be called back.
    <!-- MODULE agent-seat -->
    - With the agent-seat module the seat's push of its own strand, its pull request and its
      comments there are the workflow's own ("On the seat an agent pushes its own strand",
      above).
    <!-- /MODULE agent-seat -->
  - Outside the repository (a pull request, a ticket) a post quotes only the excerpts a finding
    needs, without personal data, and a snapshot of production data, or data drawn from one, is
    never uploaded.
  - A branch pushed before its review is published, and deleting it does not unpublish it.
    - A secret or host detail the review then finds is purged by a history rewrite, and the
      secret rotated, as on `main`.
  - A secret reaches a child process only through its environment or a file only its user can
    read (mode 0600), never as an argument.
    - Why: `ps`, `/proc/<pid>/cmdline` and a shell trace show a process's arguments.
    - A tool that hands a secret to a child process pins the way it does so in its self-test.
    <!-- MODULE agent-seat -->
    - With the agent-seat module `agent/self-test` pins it for `agent/github-token` and
      `agent/gh`, and `land/self-test` for `land/github-api`.
    <!-- /MODULE agent-seat -->
- **The commit log is the changelog.** Don't keep a separate `CHANGELOG`, and don't mirror git
  history or diffs in the docs; `git log` / `git diff` are the source of truth for *what changed*.
- **Git enforcement hooks (must be active).** The rules above are enforced mechanically by POSIX-sh
  hooks shipped in [`hooks/`](../../hooks/README.md) and wired via `core.hooksPath`; *ensure they are
  active* in any checkout (`git config core.hooksPath hooks`; the devcontainer variant wires that
  into `postCreate`, the native variant runs it once per checkout, see dev-environment.md).
  `commit-msg` enforces the message rules above; `content-gate` scans for secrets, personal/host
  data, per-language smells, the conflict markers a diff adds in any file and **every comment line
  a diff adds** (§3, no opt-out); `format-gate`
  runs the stack's fast format/style check. The test run and full build are **deliberately excluded**
  so the TDD micro-commit loop stays fast (commits are already green by the time they are made).
  - **A hook that only prints is not a gate.** **git asks `pre-commit` only for `git commit`**, so
    every commit the sequencer builds itself (rebase, `git rebase --continue`, `cherry-pick`,
    `revert`) is ungated at the moment it is made. `post-rewrite` and `post-commit` therefore
    *record* such a sha after the fact, and `pre-commit` refuses every commit whose `HEAD` reaches a
    recorded sha. A gate-failing `git am` and any merge commit are refused
    **outright**, by `pre-applypatch`, `pre-merge-commit` and, for a merge left in progress,
    `pre-commit`, which run before their commit exists. A merge commit written past them, with
    `--no-verify` or by a rebase todo, is recorded like an ungated commit.
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
  - **A conflict stop is finished by `git rebase --continue`, never by a commit or an amend.**
    - Why: a commit or an amend there folds the conflicted state into the wrong commit.
    - `pre-commit` refuses both there.
    - An amend at an `edit` stop is the playbook's fold and passes this guard.
    - An amend at a `reword` stop passes.
    - A commit at a `break` stop passes.
  - **Never read a hook's output as the verdict on the commit that follows it;** ask the history.
    - `hooks/self-test` takes the verdict of every case that commits from the resulting history.
      - Such a case may also check the reason a hook printed.
    - `pre-commit` runs it, as staged, on every commit that touches `hooks/` or a preset's
      `verify`, and `pre-applypatch` on every `git am` that does, so a weakened gate cannot land
      through `git commit` or `git am`.
    - A rewrite (a rebase, `git rebase --continue`, a cherry-pick) runs no self-test; the suites'
      run at the tip before landing ("Landing models" below) stands behind it.
  - Hooks are a git-level safety net, not a substitute for the rules; `--no-verify` bypasses them and
    is for a genuine emergency on the author's word and the `/pause` parking commit only (below).
    Details: [`hooks/README.md`](../../hooks/README.md).
    - A commit written past every hook, by `git -c core.hooksPath=<other>` or by plumbing, is a
      rule break, never a route (working-with-ai-agents.md, "Refusals and limits").
      - Why: no hook sees such a commit, so the check it skips never runs.

  > **ADAPT:** `hooks/format-gate` and `hooks/content-gate` carry the stack-specific spots (build
  > wrapper + format command, scanned source extensions, comment-exempt tooling directories,
  > host-path allowlist, production-source tree, smell patterns), and `hooks/self-test`'s fixtures
  > must match them. `grep -rn "ADAPT:" hooks/`.
- **`[wip]` parking commits (pause only).** `/pause` (shipped as a skill in
  [`.claude/skills/pause/`](../../.claude/skills/pause/SKILL.md)) may autonomously park uncommitted
  work as ONE local commit `[wip] <one-line state>` via `git commit --no-verify` (WIP legitimately
  fails the hooks; this is the one sanctioned non-emergency bypass, and the no-secrets rule applies
  unchanged, so the diff is eyeballed before parking). A `[wip]` commit is a checkpoint, not history:
  `/continue` ([`.claude/skills/continue/`](../../.claude/skills/continue/SKILL.md)) resolves it
  first (finish or rework, then replace it through the normal confirmed-message protocol; reset-free
  rewrite recipe → [multi-agent-playbook.md](multi-agent-playbook.md)). It never survives to the
  review gateway or a push.
  - It is parked on the strand's branch, never on `main`.
  - Work sitting in the primary checkout first moves to a new `claude/<topic>` branch
    (`git switch -c`); after the parking commit the checkout returns to `main`.
- **Deleting project content is the author's act; cleaning up the scaffolding is not.** The rule
  protects the author's ownership of *his* files, and a deletion no rebase undoes.
  - **The agent never runs `rm`** (it is on the permission deny floor, `rm -rf` included). Outside
    a strand's own retirements below, anything tracked by git and anything the author wrote is
    handed over as an exact command for the author to run, or removed by a committed, reviewed build
    task whose scope lives in code rather than in a permission rule.
    - **A tracked file its own strand retires, the agent removes with `git rm`**, in a commit of
      that strand, which the gates and the author review.
      - Why: a reviewed commit can be reverted.
      - A project file a template upgrade removes stays the author's call (`/template-upgrade`).
      - The old `.template/baseline/` an upgrade replaces is its strand's own to retire.
    - **An empty directory a removal leaves behind goes with `rmdir`**, which refuses a directory
      that is not empty, so the safety sits in the command (what it meets:
      working-with-ai-agents.md, "Autonomy within guardrails").
    - **A copy of production data a strand works on stays under its worktree's own `build/`**
      (ADAPT: the stack's build output directory, which `.gitignore` covers), never in `/tmp` or a
      session scratchpad, so it goes with the worktree.
  - **The development scaffolding is the agent's own, and it cleans it up itself, without asking:**
    the worktrees it created under the worktree root ("The worktree root" below)
    (`git worktree remove [--force]`, `git worktree prune`) and the branches that have landed
    (`git branch -d` and `git -C <path> branch -d`, both of which meet the auto-mode classifier;
    derived: no allow covers them). This
    is deliberately *git-native*, never `rm`: `git branch -d` refuses a branch that is not fully
    merged, so the safety lives in the command instead of in the agent's judgment. After a landing
    (at the cleanup point of its model, "Landing models" below), removing the worktree and deleting
    the branch is part of the landing, not a separate request; the commits live on in `main`, the
    label and directory are pure redundancy, and the reflog still holds deleted tips for a while.
    <!-- MODULE volumes -->
    - With the volumes module `git worktree prune` and the sweep (multi-agent-playbook.md,
      "Landing a branch") run only in a session whose container mounts the volume.
      - Why: git lists a worktree it cannot see as `prunable`, and a prune drops its registration
        for good; how automatic gc is kept from doing the same: dev-environment.md, "Worktrees on a
        volume".
    <!-- /MODULE volumes -->
  - **`git branch -D` stays off the table** (deny floor). A branch
    that is not merged holds work nobody reviewed; discarding it is the author's call. Drop a
    genuinely stale scratch ref with `git update-ref -d refs/heads/<name>` only where the playbook's
    reset-free fold calls for it.

### Branching, merge & the review gateway

**Isolate the working tree, not just the history.** A branch isolates *commits*; it does **not**
isolate the working directory: a clone has one working tree, so two writers on the same checkout race
(index/HEAD flicker, one commit clobbering another's staged work). **git worktrees** isolate it: each
is its own directory + HEAD + index. That, not the commit count, is what makes parallel work safe.

- **One track: a worktree per context.** Every problem, feature or context, a one-commit fix
  included, gets its **own `claude/<topic>` branch in its own git worktree** (under the worktree
  root, next bullet) and reaches `main` only through the gateway below.
  - **No commit is made on `main`**, on any machine; it is a rule, no hook enforces it.
    - Why: a commit on `main` would skip the gateway, the one track every change takes.
    - The one exception is the adoption commit on adoption path A: the repository's first commit,
      on the branch `git init` made (`/adopt`, step 11).
      - Why: a repository without a commit has no `main` to protect and no base for a gateway.
    - On path B the adoption is a strand: `/adopt` cuts `claude/adopt-template` before its first
      edit, and it lands by the landing model after the author's review, since an existing
      repository has both.
      - It is cut in the primary checkout, with no worktree of its own: the files the author
        copied in lie there uncommitted.
  - Parallel contexts (incl. agents from other models) = parallel worktrees, no collision; the
    **primary checkout stays on `main`**, untouched.
  - A worktree is cut from `main` right after its sync (below), a chained strand from the tip of
    the strand before it, by absolute path:
    `git worktree add -b claude/<topic> <root>/<topic> <start>`, where `<root>` is the worktree
    root (next bullet) and `<start>` is `main` or that tip.
  - `worktree.baseRef: "head"` makes the harness's subagent worktrees branch from local `main` too:
    the sync keeps it current in the hub models, and in the local model it may run ahead of
    `origin`.
  - `git worktree add` and `git switch -c` have no allow, so cutting a strand meets the auto-mode
    classifier, or a prompt outside auto mode (working-with-ai-agents.md, "Autonomy within
    guardrails").
  - The prefix `claude/` is the default (ADAPT: to rename it, `grep -rn "claude/<topic>"` across
    the docs, agents and skills, and record the choice under "Project choices" in `CLAUDE.md`);
    `agent/fold` holds it as its `branch_prefix` ADAPT line, so rename it there too.
    <!-- MODULE agent-seat -->
    - With the agent-seat module the `branch_prefix` lines of `agent/push`, `land/land` and
      `hooks/pre-push` hold it too, and so does the `agent-namespace` ruleset's exclude
      (`ci/github/README.md`).
    <!-- /MODULE agent-seat -->
- **The worktree root holds every worktree an agent creates:** strand, walk, lens and run worktrees
  alike.
  - The root is `$AGENT_WORKTREES` where the environment sets it, else
    `<project root>/.claude/worktrees/`.
  - The variable names an absolute directory outside every checkout.
    - Why: `.gitignore` covers the default alone; anywhere else inside a checkout, a worktree is
      that checkout's untracked content.
  - A session reads it once (`printenv AGENT_WORKTREES`) and writes the path out literally in every
    command.
    - Why: the permission rules and the auto-mode classifier cannot expand a variable.
  - Every recipe writes `<root>/<name>` for a worktree under the root; `<project root>` names the
    repository root.
  <!-- MODULE volumes -->
  - With the volumes module the devcontainer sets the variable to `/worktrees`, a named volume
    (dev-environment.md, "Worktrees on a volume").
  <!-- /MODULE volumes -->

  > **ADAPT:** the environment the sessions run in sets the variable (the devcontainer's
  > `containerEnv`, a shell profile); left unset, the root is the default.
- **`main` follows `origin/main`** where the project has `origin`: `origin/main` moves only by the
  author's landings, so a checkout that does not fetch works on a stale `main`.
  - **The sync is `sh hooks/sync-main`,** run from the primary checkout: it fetches, runs every
    check below on the refs that fetch brought, and fast-forwards `main` (its exits and what it
    refuses: `hooks/README.md`, "`sync-main`").
    - Why a script: the checks are many and exact, and a sync that skips one lets a stand-in
      decide what is synced and landed.
    - Its `landing_model` ADAPT line names the project's landing model ("Landing models" below).
  - **Fetch at fixed points:** at session start (`/continue`, `/plan-next`), before a strand is
    cut and before the gateway's rebase the fetch is the sync's own; before each panel round and
    before each landing it is `git fetch origin`.
    - A subagent does not fetch: the session that spawned it did.
    - A fetch at any other time is fine too; it moves no local branch.
    <!-- MODULE agent-seat -->
    - On the seat every fetch is `git fetch --prune origin`, the script's too, which also drops
      the remote-tracking ref of every branch `origin` no longer holds, a landed strand's among
      them.
    <!-- /MODULE agent-seat -->
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
      - So would a line `main` or `origin/main` in `packed-refs` without the `refs/` prefix,
        which neither `for-each-ref` nor a loose file shows.
      - On a case-insensitive file system so would a name that differs from one of them only in
        case.
    - Why the refspec: a fetch refspec that maps a tag or another branch onto `origin/main`, or
      anything onto a local branch, would move what the checks read, or `main` itself, so the
      script takes only refspecs that map a branch onto its own name under `refs/remotes/origin/`;
      a negative one that leaves `main` out, or a remote group named `origin`, would leave
      `origin/main` where it was or have other remotes write it.
    - Why the history as committed: a replace ref or a graft gives a commit other parents, so a
      diverged `main` would read as behind `origin/main` and lose its commits to the
      fast-forward.
  - **Nothing is resolved by hand:** the sync stops, and both ids go to the author,
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
  - **`push.default` is `nothing` in every checkout:** a bare `git push` from a checkout on `main`
    would push `main`, and with `nothing` git refuses a push that names no refspec.
    - `--all`, `--branches`, `--mirror` and `--tags` still push: each pushes refs that are never
      written out, and `nothing` does not stop them.
    - The devcontainer's `postCreateCommand` sets it.
    - A native or older checkout sets it once (build-run-deploy.md, "Git hooks (local
      enforcement)").
    - `/continue` checks it at session start.
    - No checkout configures `remote.origin.push`, which git would use first.

  > **ADAPT:** a project without `origin` drops the fetch, the sync and its stops, and
  > `hooks/sync-main` refuses to run there; its local `main` is the only line, and the local
  > landing model the only one.
- **Walk and lens worktrees are detached scratch** under the worktree root (above), on a path with
  no `..`:
  - a walk's is named `<topic>-walk`, a lens's `lens-<lens>-<topic>` (ADAPT: rename both shapes
    together, here and in `CLAUDE.md`); a strand topic never ends in `-walk` or starts with `lens-`,
    a naming rule nothing enforces, so the name alone tells them apart;
  - each is created with `--detach` at a SHA and by absolute path, so no branch points into it;
  - file edits, builds and `git switch --detach <sha>` inside one change nothing shared;
  - a file goes back by `git show <rev>:<path> > <path>` (the raw blob: working-with-ai-agents.md,
    "Command shapes that keep an auto-mode session unattended") or by creating the worktree anew
    (`git worktree remove --force`, then `git worktree add --detach`), never by `git reset` or
    `git clean`, which the deny floor refuses in their plain and their `git -C` form;
  <!-- MODULE guard -->
  - with the guard module that restore is refused (a write from a command that names git goes only
    into the guard's output target); in a walk worktree a file goes back by the approved
    `git -C <worktree> checkout <hash> -- <path>`, in the primary checkout by `git show` into the
    output target, then a copy;
  <!-- /MODULE guard -->
  - a ref or config command run there (`branch`, `update-ref`, `stash`, `worktree`, `push`,
    `reflog`, `tag`, `config`) acts on the refs and config every checkout shares and is never scratch;
  - a strand worktree is never scratch: its branch shares refs and config with the primary checkout
    and holds the only copy of unlanded work;
  - `CLAUDE.md` names them for the auto-mode classifier (working-with-ai-agents.md, "Where auto-mode
    rules live").
- **A session never relocates itself into a worktree** (`EnterWorktree`, denied in
  `.claude/settings.json`): a harness that files session history per working directory drops a
  relocated session out of the primary checkout's resume list, unreachable from the editor window
  that opened it, and it rules out driving parallel strands from one window. Sessions stay in the
  primary checkout and reach the worktree by **absolute path**, the same discipline the
  working-directory-drift rule already demands (playbook, environment gotchas). Subagent worktree
  isolation is untouched: it isolates the agent, it does not move the session.
- **On the branch:** the implementer runs the full TDD loop and **commits each red→green→refactor step
  itself**, with the plan's message or, where the plan names none, a protocol-conform one it words
  (confirmation: the first bullet of this section).
- **The review gateway (per feature, serialized):** (1) everything committed in the worktree;
  (2) **sync `main` (`sh hooks/sync-main`, above) and rebase onto it**, resolving conflicts
  there; (3) **review the rebased tip**: with rebase + fast-forward there are no merge commits
  and no automerge, so the rebased tip is **byte-identical to what `main` becomes**; reviewing
  it *is* reviewing the final state, scoped to one feature, with no merge artifacts (falsifier +
  reviewer gate here). **When
  presenting work at this gateway, always state the full worktree name (its branch and absolute
  path) so the author can open that worktree directly in their editor and review every changed file
  in one place before the landing.** The hand-off also carries the compare range, `<tip>..<base>`
  ([working-with-ai-agents.md](working-with-ai-agents.md), "Handing the author a review").
  (4) on PASS and the author's explicit review, **the landing** of the project's landing model
  (below); (5) **its cleanup**, autonomously, as part of the landing.
  <!-- MODULE agent-seat -->
  - **On the seat the session pushes before the hand-off:**
    - after the gateway build it pushes the rebased tip ("On the seat an agent pushes its own
      strand" above) and opens or updates the strand's pull request;
      - Why: the author then reviews the tip on its pull request, from any machine.
    - the hand-off carries the pull request's link (working-with-ai-agents.md, "Handing the
      author a review");
    - the push publishes the branch before the review ("No secrets" above);
    - on PASS and the author's review the author lands the tip with a `/land <tip>` comment on
      the pull request ("Landing models" below; build-run-deploy.md, "The landing workflow").
  <!-- /MODULE agent-seat -->
- **Landing models:** the handbook carries every landing model; "Project choices" in `CLAUDE.md`
  names the one this project lands by.
  - In every model the self-tests of the tooling folders the strand touches (`hooks/` and each
    module's folder with a self-test) pass at the reviewed tip before it lands.
    - The reviewed tip is the one the author reviewed, or the tip of a re-rebase after that review
      which ran clean and builds green ("Every rebase is followed by the full gateway build" below).
    - Why: a rewrite runs no self-test leg, so a suite weakened in a rebase reaches the tip unseen
      by the hooks (`hooks/README.md`, "Bypass").
  - **Local** (no hub): the agent fast-forwards `main` to the reviewed tip by running, from the
    primary checkout, `git merge --ff-only <tip>`.
    - Precondition: the primary checkout is **clean**, else stop and surface it.
    - Precondition: the reviewed tip's own guard, `sh <worktree>/hooks/breach-guard --reaches
      <tip>` run from the primary checkout with the strand's worktree clean at `<tip>`, exits 0
      for that same commit, else stop: the strand is rewritten first.
      - Why: the fast-forward would carry a recorded commit onto `main`, where only the author's
        release removes the refusal; the check and the fast-forward name one commit, so a branch
        that moves in between cannot land unchecked; the strand's copy runs, since the guard reads
        the repository of the directory it runs in and the copy `main` checks out may predate
        `--reaches`.
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
    - A strand of a supervised machine lands as in "Hub without seat".
    - A landing is never an instruction to the agent: on the seat a go in chat is answered with
      the line `/land <tip>` and the pull request's link, for the author to post.
      - An agent posts no `/land` comment: that would be one more form of moving `main` ("No
        agent pushes `main` or a tag in any form" above).
    - The cleanup waits until `origin/main` holds the tip, by the sync and the check of "Hub
      without seat"; then `git worktree remove`, `git branch -d`, and
      `agent/push --delete <branch>` where `origin` still holds the branch.
  <!-- /MODULE agent-seat -->
- **The gateway is two steps and is never collapsed into one.** (1) Present the full reviewable
  statement (the rebased branch log **and** the full diff) and *wait* for the author's explicit
  review verdict; (2) only then the landing. An "execute the gateway" approval is not the review.
- **Freeze the tree for the length of a gate round.** Before spawning a gate agent, the strand's
  `git status --porcelain` is empty and quoted in the briefing; no rebase, no amend and no build runs
  in that worktree until every agent of the round has reported. A finding that arrives while the gate
  is open is folded in **after** it. A verdict is worth only what the tree it read was.
- **Merge strategy (hard rule): rebase + fast-forward only.** No merge commits, no squashing (an
  exception is the author's act); `pre-merge-commit` refuses the merge commit `git merge` builds,
  `pre-commit` the one that would conclude a merge in progress, and the post-hooks record one
  written past them (hooks/README.md). History stays **linear** *and* keeps the small commits.
- **Landings are author-serialized.** Only one feature sits in rebase→review→fast-forward at a time;
  when several are ready, the **author picks the order**. The fast-forward is the hard guard
  (`git merge --ff-only` in the local model, the author's push without force in the hub models): a
  branch not rebased onto the latest `main` is refused (re-rebase; whether a review follows: "Every
  rebase is followed by the full gateway build" below). **No automatic
  cross-session coordination**: heterogeneous tools/contexts can't discover each other; the author is
  the single serialization point.
  <!-- MODULE agent-seat -->
  - For a seat strand the guard is the fast-forward of the author's `/land` landing, which lands
    exactly the reviewed commit.
  <!-- /MODULE agent-seat -->
- **Every rebase is followed by the full gateway build.** `./verify all` runs again on the rebased
  tip, cache disabled, executed-proof read (§6), before anything else happens: a rebase that applied
  cleanly can still break semantically. When `main` moved after the author's review, a re-rebase
  that ran clean and builds green goes to the landing without a second review; a re-rebase that
  needed a conflict resolution re-opens the **panel** for what it touched and then the **author's
  review**, because resolutions are the one place unreviewed content enters:
  - the lenses whose read paths the resolution touches
    ([multi-agent-playbook.md](multi-agent-playbook.md), "Read paths of the lenses (delta rounds)"),
    the doc-reviewer and the reviewer run on the resolved tip, the resolution as their delta (§9,
    delta rounds);
  - this run counts as no panel round.
- **Every commit that lands on `main` is verified-good, not merely green.** Defective = builds red,
  **OR** the falsifier exposes it as *fake-green* (tests pass but assert nothing real), **OR** the
  reviewer rejects it as not implementing what it claims. None may land. Red states are never committed
  (TDD's red is transient; the commit comes after green, `@wip`-tagged Gherkin keeps the build green).
  A defect at the gateway is **repaired by rebase into the commit it belongs to, before the
  landing**: never left on `main`, never a follow-up "fix" commit.
- **Reword at the gateway** non-interactively via scripted `GIT_SEQUENCE_EDITOR` / `GIT_EDITOR` (no
  interactive TTY).
- **Tool-neutral:** the model is plain git (worktree · branch · rebase · `--ff-only`). Claude Code
  adds `Agent` worktree-isolation for tool agents; other tools/humans use `git worktree` directly.

## 8. Definition of Done (checklist)

- [ ] **BDD scenario(s) written up-front and confirmed by the author** (§5), and now passing, or the
      exemption explicitly claimed and named in the plan
- [ ] Compiles, `./verify all` green locally, **and each commit green on its own**, proven by a walk
      over the commits rather than derived from the tip
      <!-- MODULE gate -->
      (with the gate module the proof is the walk log of each code tree, which
      `gate/dossier-check` reads)
      <!-- /MODULE gate -->
- [ ] Test run proven to have *executed* (§6), not read off an exit code or a cached result
- [ ] Every TDD cycle carries its **red evidence** in the hand-off, or, where §4 allows it, a
      recorded mutation in its place (§4)
- [ ] No compiler/lint warnings (the build fails on any; the suite stays warning-clean)
- [ ] Unit tests for new logic; no comment or doc comment added anywhere in the diff (§3)
- [ ] No secrets or personal/host data (passwords, tokens, keys/keystores, credentials, real
      emails/usernames, absolute paths, infra) in committed files
- [ ] Relevant `docs/knowledge-base/` doc updated, riding in the commit whose change it describes,
      each hunk checked in both directions (§7)
- [ ] Every identifier, path or backlog ID the diff **deletes or renames** grepped across `docs/`, and
      every hit fixed in the same commit; every **figure** the change moves grepped repo-wide, and
      every hit outside a dated decision or learnings row rewritten in the same commit; a changed
      **rule**'s key terms grepped across `docs/` and `.claude/`, and every hit moved in the same
      commit
- [ ] Commit message one line, present-tense verb, **confirmed in the approved plan or by the
      author** (§7); nothing pushed by an agent
      <!-- MODULE agent-seat -->
      (with the agent-seat module: nothing but the seat's own strand branch and the deletion of a
      landed one, §7)
      <!-- /MODULE agent-seat -->
- [ ] Whitespace/format separate from logic; LF endings
- [ ] Decisions/assumptions logged in [`open-questions.md`](../open-questions.md) if any were made
- [ ] A release follows the checklist in [build-run-deploy.md](build-run-deploy.md), "Release &
      deploy" (ADAPT: delete this line if nothing is deployed)
- [ ] A change to a rule the product explains to its users changes that explanation and its pins
      in the same commit (§5, [testing.md](testing.md)) (ADAPT: delete this line if the product
      explains no rule in a page of text)

## 9. Review gate & process learnings

Before each commit, a reviewer (human, or a dedicated review agent, see
[working-with-ai-agents.md](working-with-ai-agents.md)) checks **process adherence** (was it a real
red→green→refactor micro-step? whitespace kept separate? KB updated? commit-message rules met?) and
**code criteria** (clean code, SOLID, hexagonal boundaries). If something is off: send back with
concrete feedback, and **record the lesson in [`process-learnings.md`](../process-learnings.md)** so
future work, and future agents, drift toward the process, not away from it.

**Bounded review loop (governance).** A rejection sends the work back to fix, then *fresh* gate
agents re-check it. Keep that loop bounded:

- **The panel bounds govern the feature gate:** the full round 1, delta rounds, the "already
  decided" list, the end on passing lows and the cap below. The scenario gate (§5 step 1) keeps
  its own:
  - every re-gate re-runs the scenario falsifier, the stakeholder lens and the reviewer in full,
    with no delta and no "already decided" list, and its briefs carry the refuted list below;
    - Why: a draft `.feature` is small and read whole anyway.
  - the stakeholder lens reads every `.feature` file on every run and states how many it read;
    - Why: a lens that sees one file cannot see an overlap with another.
  - a missing report, or a stakeholder report that read fewer `.feature` files than
    `git ls-files '*.feature'` lists plus the draft where it is not yet tracked, stops the gate;
    - Why: the reviewer's verdict stands only on the features both lenses read.
    <!-- MODULE gate -->
    - With the gate module `gate/scenario-check` makes the comparison (multi-agent-playbook.md,
      "The scenario-gate dossier").
    <!-- /MODULE gate -->
  - a lens's finding is `high` or `med` only for a rule or behavior of the backlog item or ticket
    that no scenario states, a scenario a wrong or hollow implementation would pass, a scenario a
    product owner cannot judge, a `Then` asserting what its actor cannot see, or a rule stated
    twice; anything else is `low`;
    - Why: lows reach author gate 1 unfolded, so only a finding that makes the set incomplete,
      hollow, unjudgeable or redundant holds the gate.
  - a lens reports `soundSpec: no` exactly when a `high` or `med` finding of its own stands;
    - Why: lows go to author gate 1 unfolded, so a lens with only lows does not hold the gate.
  - a lens finding the reviewer refutes counts as settled where the refutation names the rule of
    §5 it rests on, and goes to the author at once where it names none;
    - a settled finding holds the gate no longer, and neither does a lens's `soundSpec: no` that
      rests on settled findings alone;
    - the settled findings, each with its refutation, go into the next round's briefs as the
      refuted list, and a lens raises one again only on a ground its refutation does not answer;
    - Why: a fresh lens raises a refuted finding again, so without the list the round cap, not
      the refutation, ends the dispute.
  - it passes when no `high` or `med` finding is open and every report carries its own PASS, a
    lens's report counting as a pass where it fails on settled findings alone;
    - a lens's PASS is `soundSpec: yes`, the reviewer's is `PASS`;
  - its open lows go unfolded to author gate 1, and a low the author adopts there is a fold and a
    full re-gate;
  - a gate that has not passed after round 7 (ADAPT: the round) ends:
    - one more full run follows that round's fold;
    - where that run does not pass, only the reviewer's mandatory folds follow, the minimal set its
      FAIL names, and the fold brief names nothing else;
    - then a closing run, in which every report names the `.feature` files it read, by count and
      path, and the orchestrator compares them with `git ls-files '*.feature'` plus the draft
      where it is not yet tracked;
      <!-- MODULE gate -->
      - With the gate module `gate/scenario-check` is that comparison, by fingerprint.
      <!-- /MODULE gate -->
    - what is still open goes to author gate 1 beside the scenarios.
    - Why: a fold can seed the next round's findings, so the loop needs an end that does not wait
      for convergence.
- **Round 1 is a full round:** the lenses the change can break and the doc-reviewer, each fresh,
  then the reviewer with their verdicts in its brief
  ([multi-agent-playbook.md](multi-agent-playbook.md), "The pipeline (per backlog item)", step 4).
<!-- MODULE security-lens -->
- **The security lens joins the panel**, since no other lens reads the app as an attacker; when and
  how: [multi-agent-playbook.md](multi-agent-playbook.md), "Cadence".
<!-- /MODULE security-lens -->
- **From round 2 on, a delta round re-runs only what the fold concerns:**
  - a lens runs again only when the fold changed a path it reads
    ([multi-agent-playbook.md](multi-agent-playbook.md), "Read paths of the lenses (delta rounds)");
  - a lens report of an earlier tip stands while no path its lens reads has changed since that
    tip, a rebase onto a moved `main` included;
  - the doc-reviewer runs again after every fold;
  - the reviewer runs again, last, after every fold that changes a path outside `*.md`;
  - each brief carries the previous tip and the dispositions of the earlier findings;
  - a re-run lens or the reviewer raises only what the delta introduces or a fix left open, and the
    rest of the strand stands as reviewed;
  - a docs-only fold (no path outside `*.md`):
    - re-runs the doc-reviewer alone;
    - its brief carries the findings the fold answers, a reviewer's docs finding included;
    - the reviewer joins only when process adherence is in doubt.
    <!-- MODULE gate -->
    - Exception with the gate module: the reviewer joins every docs-only fold and every `[doc]`
      gate, last, and runs `gate/dossier-check` there as in any round.
      - Why: its report is a required report of every dossier.
    <!-- /MODULE gate -->
  - Why: a verdict on unchanged paths still holds, and a re-run costs a full lens.
- **Every lens brief, and the reviewer's, at the feature gate carries an
  "Already decided, out of scope" list:** the findings earlier rounds rejected or filed, the
  author's decisions bearing on the strand and what the plan leaves out.
  - Why: so no round raises them again.
  - An entry is raised again only where the delta changed the code it names, and the agent names
    the entry with the change.
    - Why: the entry judged that code as it was.
- **A passing round whose findings are all low ends the panel:**
  - every report of the round passes on its own;
  - every finding is a lens's `low`, or one a passing doc-reviewer or reviewer raised;
  - the lows become one backlog row naming each low with its location (with a ticket system, one
    ticket);
  - the commit that adds the row needs no new review while the tip differs from the reviewed one in
    `docs/backlog.md` alone (`git diff --name-only <reviewed tip> <tip>`);
  - no fold follows.
    - Why: a fold would only seed the next round's findings.
- **Cap: two panel rounds, then the hand-off.**
  - After round 2, only a finding of the severe class is folded: one that can let a scenario or a
    test pass falsely, leak a resource or a secret, lose data or break security.
  - A reviewer finding that the change does not do what it claims is of that class too, since §7
    lets no such commit land.
  - So is a production line written before its red test (§4), since a test never seen red can
    pass falsely.
  - From round 2 on, each finding's disposition names its class, severe or not, before any fold.
    - Why: a fold outside the class then shows before it is made.
  - A delta run of the gate agent that raised a severe finding re-proves its fold.
  - A severe finding of that run goes to the author, not into another fold.
  - Every other finding becomes a backlog row.
    <!-- MODULE agent-seat -->
    - With the agent-seat module the row carries its seat marker ([README.md](README.md), DOC-5).
    <!-- /MODULE agent-seat -->
  - Then the strand goes to the author for the gateway (§7).
  - Only a dated author row in [`open-questions.md`](../open-questions.md) lifts the cap.
  - Why: the severe class is what does the most harm once landed; the rest costs less as a backlog
    row than as another round.
- **Early-escalate** when two consecutive rounds raise the *same* finding: stop and take it to the
  author, since a repeated finding usually means the *plan/spec* is wrong, not the code; re-plan
  rather than fold again.
<!-- MODULE agent-seat -->
- **No cap on the seat:** where `AGENT_SEAT` reads `unattended` the loop runs until PASS
  (dev-environment.md, "Where agents run").
  - Why: an unattended seat has nobody to wait for.
  - Three brakes replace the cap and early escalation there:
    - a status in the session's chat every three rounds: the phase, rounds run, findings open,
      recurring findings and reversed fixes, a time estimate;
    - a question to the author only where a fix would reverse an author decision or the brief
      leaves the scope open;
    - a finding that recurs, or a fix that reverses an earlier round's, gets its root cause fixed.
<!-- /MODULE agent-seat -->
- **Process-only FAILs** (whitespace not separated, KB not updated, commit-message format) are cheap
  mechanical fixes and **do not consume a round**.
  - After round 2 such a fix forces no further round.
    - The gate agent that raised it re-checks it at the fixed tip.
    - No lens runs.
    - Why: such a fix changes no behavior a lens tests.
- Fixes are **folded into the commit they belong to** (§7), never appended as a "fix review" commit,
  and folded at the moment of fixing rather than at the end of the round. The gate reads the branch
  log before the diff: a commit that names a repair of an earlier commit is itself a FAIL, and an
  "and" joining two clauses in a proposed message is a hint to split (advice, §7).
- **A gate agent's report is an input to the next gate agent**, not a message to the author alone:
  every prior verdict travels into the reviewer's brief, and a report of an earlier tip stands only
  as the delta rounds above allow.
- **The gate verifies the claim, not the intent.** Every claimed fix is stated with the evidence that
  it landed on the branch (`git show <sha> -- <file>`, produced from the strand's worktree with
  `git -C`, never from an inherited working directory), and the reviewer ticks off each of its own
  prior findings **by name**, so a fix that never arrived shows up as a missing tick rather than as
  an unread absence.
- **A causal claim is made only after the mechanism is shown**, e.g. by a probe that widens the
  suspected window and demonstrates the failure. Absent that, say "cause not established", keep the
  hardening as hardening, and file the unexplained failure as live work instead of closing it. The
  same holds for a claim about how the code behaved *before* a change: probe a worktree at that
  commit (`git worktree add --detach <root>/lens-<lens>-<topic>-base <base>`, `<root>` the
  worktree root of §7), do not narrate it.
- **A finding the panel raises but does not fix is live work.** It gets its backlog row or
  `open-questions.md` entry **in the same strand**, written *before* the round closes, a lens's or
  the doc-reviewer's before the reviewer is spawned, so the gate sees the trace instead of having to
  create it.
  - Every low of a round that passes, a lens's and the doc-reviewer's included, goes into the lows
    row after the round instead (above).
  - A trace to an existing row names the finding in that row, unless the row already states it.
    - "Covered by the row's acceptance" is no trace.
    - Why: a finding the row does not name is one no later pass over the row rediscovers.
  - The reviewer checks each trace at its row, at the tip.
  - Which older text counts, and where leftovers go: [README.md](README.md), "Scope of a finding"
    and DOC-5.
- **A negative claim owes the search that would have found the thing.** "It is not in the repo" is
  stated together with the command that came up empty, looked where the docs say the thing lives and
  where its siblings live, so the search itself can be judged.
- **A claim may claim no more than what was produced.** Absence on one surface is evidence about
  that surface, never about the whole: name the surfaces searched before writing "X does not exist",
  and grep the evidence already on disk for a counterexample to the sentence being written.
- **A measurement states its coverage beside its result**: items read against items present, and the
  comparison fails when the two disagree; a parser that silently matches a subset reads exactly like
  a clean result.
- **A bound is set on a measurement of real data.**
  - A performance or limit decision first measures on a copy of real data.
  - The author then sets the bound it must meet.
  - The copy stays under the worktree's own build output (§7, "Deleting project content").
  - Why: a bound set without the real figure is a guess.
- **A rule is never widened by the strand that needs the widening.** Propose the change as an author
  decision like any other; if it is not granted, drop the thing that needed it.

This governance is tool-neutral; the agent-team mechanics that exercise it (who runs falsify vs.
review, harness-specific history-rewrite recipes) live in
[multi-agent-playbook.md](multi-agent-playbook.md).
