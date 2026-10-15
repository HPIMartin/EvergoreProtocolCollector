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
  external integration ⇒ port first, then adapter. (Missing today: a `PageSource` port for
  scraping; `SeleniumPageSource` is wired without an inner-layer interface.)
- Only the composition root (`ApplicationFactory` / `config`) knows concrete classes.

## 2. SOLID, applied here

- **S**: one reason to change per class (parsing ≠ valuing ≠ persisting). Split `SeleniumPageSource`'s
  scraping from driver lifecycle.
- **O/L**: extend via `TransferTypeVisitor` and similar, not by editing `switch`/`instanceof`.
- **I**: small, role-specific ports (`BankRepository`, `StorageRepository`, … not one fat repo).
- **D**: application code depends on ports; adapters depend on the domain, never the reverse.

## 3. Clean-code rules

- Intention-revealing names; no non-domain abbreviations; no joke/placeholder names (clean up the
  existing `stupidMerge()` / debug `"shit"`, don't copy them).
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
    comment line to a listed source extension (`.java`, `.kts`, `.ts` and `.tsx` among them; the
    list is the gate's config block, [`hooks/README.md`](../../hooks/README.md)). **There is no
    opt-out.** What the gate refuses is deleted, not reworded; the existing stock stands until a
    commit edits it.
  - The carve-out is the process scaffolding itself: `hooks/*` and the ignore files carry prose by
    design and sit outside the scanned extensions.
- **No dead code, no undeclared dependencies** (an unused class importing a library absent from
  `build.gradle.kts`: delete it).
- **Code/build is the single source of truth; docs never duplicate volatile facts.** Versions
  (Java/Micronaut/Gradle), dependency lists, file/test counts live in `build.gradle.kts` and the
  code; docs point, never restate (restated facts rot silently). Doc contradicts build ⇒ build
  wins, doc fixed in the same change.
- **Immutable `record`s over getter/setter boilerplate** for value objects, DTOs, config
  (`BankEntry`, `StorageEntry`).
  - DTO public fields are fine.
  - Record accessors (`config.apiToken()`, `BankEntry.amount()`) are not bean getters.
  - No `getX()`/`setX()` unless a framework strictly requires it (Micronaut binds
    `@ConfigurationProperties` to a record with no extra annotation).
  - Return `Optional`, not sentinels.
- **`Logger` is the last constructor parameter**: collaborators first, incidental infrastructure last.
- Constants/config over magic values; **no secrets in source** (token, credentials → config).
- **Warnings are errors**: fix, don't suppress.
  - The compiler flags, the two deliberate exclusions and the probe compile's exception:
    [build-run-deploy.md](build-run-deploy.md), "Build".
  - Unsure fix vs. exclude ⇒ ask the author.

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
- **An existing test adapted to a changed behavior** cannot be seen red first; it pays that debt
  with a **recorded mutation**:
  - mutate the production code so the adapted assertion must fail;
  - run only that test class (`./verify focus <path>`) and keep the output;
  - revert the mutation;
  - the record (what was mutated, the failing line) goes into the step's report like red evidence.
  - Where a harness exists this is the only substitution.
  - A *new* test that drives new code is always seen red.
- **A new test that pins behavior the code already has** (one a panel asks for) cannot start red
  either and pays the same way.
  - Its mutation stays in the gate dossier (playbook, `mutation/`).
  - No row records it: the test itself is the durable artifact (author decision 2026-10-13).
- Where a deliverable genuinely has no test harness the stack supports (the deploy shell script),
  the substitution is **mutation testing** and it is *declared*: name it and every mutation in
  [`open-questions.md`](../open-questions.md), "Recorded mutations", in the same change, so a later
  gate re-runs them instead of taking a claim on trust. A mutation that lives only in a conversation
  is not a proof.
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
    and JSON), the startup, the throttle and the log.
  - An address, a status code or an internal name never stands in a member's or an admin's
    scenario; it belongs to the step definitions and the unit tests.
  - A description sentence starting "Today" may quote word for word what the actor sees today,
    technical words included; the `Then` never does.
- **One rule, stated once across the suite.**
  - A rule counts wherever a scenario asserts it, in a second `When`/`Then` pair too, not only
    where a title names it.
  - Two scenarios asserting the same rule for the same stakeholder with different data become one
    scenario, or one is dropped.
  - The same rule on two surfaces (the admin page and the health report) is one
    `Scenario Outline` over the surface, each example row naming its own actor.
  - Different stakeholders seeing different consequences of one event (the game cannot be reached)
    stay apart, each asserting only what its stakeholder needs.
- **Every outcome reads on its own**: a `Then` names the state it expects ("in descending order"),
  never one the reader must carry over from an earlier step ("again").
- **Placeable time**: the "Points in time" rules below (TIME-1 to TIME-8).
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
  something must lie clearly outside a 30-day window, 15.07. stands for summer time, and 31.12.
  is a "now" long after every movement.
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
   gates on this section with both reports; fix and re-gate.
2. **► Author gate 1: the author confirms the scenarios and their completeness.** A real gate, not
   a notification: implementation does not start until the author has read the scenarios and said
   yes to the set. Wrong scenarios are cheap here and expensive after three TDD cycles have been
   built on them.
3. **Commit the confirmed scenarios tagged `@wip`**: the default run excludes `@wip`
   (`junit-platform.properties`), so the build stays green while intent is captured. The tag stays
   committed until step 5.
4. **Drive them green with §4 TDD cycles**: step definitions and production code, one committed
   cycle at a time. The scenarios run through `./verify bdd` (every `@wip` scenario) or
   `./verify focus <feature file>`; removing the tag locally to run them is a fallback and is never
   committed. A scenario that turns out to be wrong goes **back to the author** as a changed
   scenario; it is never quietly reinterpreted to match what the code now does.
5. **Arm the scenarios**, the last TDD step: when every scenario of the feature passes, remove
   `@wip` in its own commit (`Arm the <feature> scenarios`). From here `./verify all` runs them on
   every commit and at every gate.
6. **Refactor the feature** under the armed net: improve the design across the feature with the
   scenarios and unit tests green, one green commit per step, behavior unchanged. A change that
   needs new behavior or a new port is not a refactoring; it goes back to planning.
7. **The panel and the review gate** (§9, the playbook), then **author gate 2**, the review of the
   rebased tip, then the landing (§7).

**Projects that predate their scenarios.** This one does:

- the `/bdd-catch-up` skill derives the missing scenarios from the code, which is the truth for
  what *is*;
- the author decides what *should be*;
- the skill runs one capability per session when the author picks it;
- derived scenarios carry `@characterization` until the author confirms them;
- `./verify all` excludes that tag like `@wip`;
- a rejected one becomes a bug in the backlog plus the corrected scenario tagged `@wip`.

## 6. Test strategy (pyramid)

- **Many** fast unit tests (domain values, parser, evaluator math).
- **Some** adapter/integration tests (repositories vs a throwaway SQLite file).
- **Few** acceptance tests: the armed scenarios (§5), the collect→evaluate→overview flow via fakes,
  and a thin smoke test for wiring.
- **Deterministic, never wall-clock-dependent**: must pass on any hardware (a first-gen Raspberry
  Pi may just take longer).
  - Correctness never hinges on a `sleep`/`timeout` threshold.
  - Wait on a real signal (`CountDownLatch.await()` without timeout, a future, a condition).
  - Release the signal on both success and failure paths, so a bad run fails an assertion instead
    of hanging.
  - Timeout/retry is mitigation, not fix; never "stabilise" a flake by bumping one
    ([testing.md](testing.md), boot-signal seam).
- **Arrange discipline**: shared, non-test-relevant preconditions (waiting for boot/collection,
  base-URL config) go in `@BeforeEach`, not each test body; a body holds only the arranges that
  make its act + assert legible. Exception: a precondition that *is* the behaviour under test
  (e.g. `SmokeTest.applicationIsStarting`) stays as the test's act/assert.
- **Test-authoring rules** (the unit under test named `tested`, the act as its own named value, the
  name over an assertion message, a behavior pinned once at one level, a seam asserted as driven, a
  run judged by proof that it executed): canonical in [testing.md](testing.md), "Testing direction"
  and "Proving a run really executed".

## 7. Git & commits (author's rules, strict)

- **The agent words exactly one commit message itself and commits** (standing approval, author
  decision 2026-10-07): no confirmation round, the rules below are the check, and the gate reads
  the branch log. Pushing follows the push rules below: an agent pushes only its own
  `claude/<topic>` branch, never `main`. Domain decisions stay questions to the author.
- Message = **single line**, **present-tense verb first** (after an optional `[doc]` tag), states
  what the commit actively changes. No body, no `Co-Authored-By` or tool footer. English. The
  `commit-msg` hook's ticket-key leg stays off: the project has no ticket system
  ([open-questions.md](../open-questions.md)).
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
- **Every commit compiles and is green on its own, not just the tip.**
  - A build-file or dependency change belongs in the **first** commit that uses it, not the one it
    was written for.
  - A test reference likewise lands no earlier than its referent.
  - Verify it, do not derive it: the "the rebased tip is byte-identical to `main`" argument covers
    the tip only, so walk the commits before making the claim (`gate/walk`;
    multi-agent-playbook.md, "The gate dossier", `walk/`).
  - After a fold, `gate/walk` builds only the commits whose code tree has no green log (author
    decision 2026-09-29).
- **A strand's shape follows the work, not the plan it started with** (author decision
  2026-08-15). A planned commit count is an estimate: when the work needs more commits, it gets
  them; never inflate one commit to honour the original estimate. Clean code covers the history
  too.
- **Always able to build, not always allowed to deliver.** `./verify all` must be unconditionally
  able to run green: never attach vulnerability or quality gates (CVE thresholds, quality metrics)
  to the build/check tasks the inner dev loop runs. Such gates live on separate on-demand tasks
  (`./verify vuln`, `./gradlew vulnScan` with `vulnScan.failOnSeverity`) or in CI/delivery
  ([build-run-deploy.md](build-run-deploy.md)).
- **No secrets, no personal/host data, ever (hard rule); the repo is a public showcase.** Never
  commit passwords, tokens, API keys, private keys/keystores
  (`*.pfx`/`*.p12`/`*.jks`/`*.pem`/`*.key`), credentials, real personal/work emails or usernames,
  or machine/host/workspace details (absolute paths, host layout, infra).
  - Such data: gitignored `*.local.*` files or a secret store, injected at runtime.
  - A secret in history is purged by history rewrite **and rotated** (deletion doesn't un-leak).
  - The Claude Code "always allow" flow can write path-bearing rules into the committed
    `settings.json`: prefer bare commands matching the portable `Bash(<cmd>:*)` rules;
    machine-specific permissions (e.g. absolute-path `cd`) go in `.claude/settings.local.json`.
  - A `git -C …` command matches no `Bash(git <sub>:*)` rule, so always-allow would pin a
    host-path rule per worktree hash. What a `git -C` command meets instead:
    working-with-ai-agents.md, "Permissions & autonomy".
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
    every commit the sequencer creates (rebase, `cherry-pick`, `revert`) is ungated at the moment it
    is made.
  - `post-rewrite` and `post-commit` record such a commit afterwards when it breaks a rule.
  - A recorded commit blocks every further commit until it is gone.
  - Mechanics: [`hooks/README.md`](../../hooks/README.md), "The gate, in one paragraph".
  - **A conflict stop is finished by `git rebase --continue`, never by a commit or an amend**, which
    would fold the conflicted state into the wrong commit.
    - `pre-commit` refuses both there.
    - An amend at an `edit` stop is the playbook's fold and passes.
    - A commit at a `break` stop passes.
    - A commit after a finished rebase that left its `REBASE_HEAD` behind passes.
  - **Never read a hook's output as the verdict on the commit that follows it;** ask the history.
  - `hooks/self-test` asserts exactly that (resulting history, never printed text).
  - `pre-commit` runs it and each other self-test leg on every commit that touches that leg's
    directory, so no weakened gate can land.
  - Safety net, not substitute.
  - `--no-verify` only for genuine emergencies.
  - A `--no-verify` commit is left alone, so the emergency valve stays one.
- **`[wip]` parking commits (pause only)**: `/pause` may autonomously park uncommitted work as ONE
  local commit `[wip] <one-line state>` via `git commit --no-verify`.
  - WIP legitimately fails the hooks; this is the one sanctioned non-emergency bypass.
  - The no-secrets rule is unchanged: eyeball the diff first.
  - It is a checkpoint, not history.
  - `/continue` resolves it before any new work: it finishes or reworks the work, then replaces the
    commit through the normal commit protocol.
  - Reset-free rewrite recipe → multi-agent playbook.
  - It never survives to the review gateway or a push.
  - It is parked on the strand's branch, never on `main`.
  - Work in the primary checkout moves to a new `claude/<topic>` branch first.
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
    - Why: a reviewed commit the author lands.
    - A file a template upgrade removes stays the author's call (`template-upgrade`).
  - **The development scaffolding is the agent's own, and it cleans it up itself, without asking:**
    the worktrees it created under the worktree root, defined below (`git worktree remove
    [--force]`, `git worktree prune`) and the branches that have landed (`git branch -d`, and its
    `-C` form).
    - Deliberately *git-native*, never `rm`: `git branch -d` refuses a branch that is not fully
      merged, so the safety lives in the command instead of in the agent's judgment.
    - A directory a removal leaves behind empty under the worktree root goes with `rmdir`, which
      refuses anything that is not empty, so the same safety holds (author decision 2026-09-27).
    - After a landing, removing the worktree and deleting the branch is part of the landing, not a
      separate request.
    - The commits live on in `main`, and the reflog still holds deleted tips for a while.
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
  (below): `git worktree add -b claude/<topic> <root>/<topic> main`. `worktree.baseRef: "head"`
  makes the harness's subagent worktrees branch from that same `main`; `"fresh"` would branch from
  `origin`'s default branch as Claude Code last fetched it, up to a day old, and miss that day's
  landings.
- **Every strand, gate and lens worktree is created under the worktree root.**
  - The root is `$EPC_WORKTREES` where that is set, else `.claude/worktrees/` in the primary
    checkout.
  - **On the PC, the root and pruning stay inside the container** (why:
    [dev-environment.md](dev-environment.md), "Worktrees on a volume"):
    - `EPC_WORKTREES` is read there; a session on the Windows host asks
      `docker exec -u vscode <container> printenv EPC_WORKTREES`;
    - `git worktree prune` runs only in a container that has the volume mounted.
  - Which seat sets it, and why: [dev-environment.md](dev-environment.md), "Worktrees on a volume".
  - Where Claude Code puts an `Agent` subagent's `isolation: "worktree"` checkout, and why strands
    avoid it: [multi-agent-playbook.md](multi-agent-playbook.md), "One track".
- **`main` follows `origin/main` on every machine** (author decision 2026-10-04): `origin/main`
  moves only by the author's landings and the pull requests the author merges on GitHub
  (Dependabot's), so a machine that does not fetch works on a stale `main`.
  - **Fetch at fixed points:** `git fetch origin` at the start of every session (`/continue`,
    `/plan-next`), before a strand is cut, before every review panel, before the gateway's rebase
    and before every landing; fetching is allowed at any other time too.
  - **A moved `main` is taken in before the next panel** (author decision 2026-10-09): when the
    fetch before a panel shows `origin/main` ahead, the agent syncs `main`, rebases the strand and
    rebuilds the gate's evidence before spawning any gate agent, without asking while it works on
    its own, since the dossier check refuses any base but `main`:
    - a lens report whose read paths `main` left unchanged stands across the rebase;
    - the doc-reviewer and the runs repeat at the rebased tip.
  - **Local `main` only fast-forwards:** in the clean primary checkout, on `main`,
    `git merge --ff-only refs/remotes/origin/main` or `git pull --ff-only`; never a merge, a rebase
    or a plain `git pull` on `main`. The refs are spelled out because git resolves a tag or a local
    branch before a remote-tracking ref: a tag `main`, a tag `origin/main` or a local branch
    `origin/main` would stand in for the branch. On a case-insensitive file system, such as the
    author's PC's checkout, a ref that differs only in case does the same: a fetched branch `Main`
    takes over `refs/remotes/origin/main`, and a tag `MAIN` stands in for `main`.
  - **Nothing is resolved by hand:** the sync stops, and both SHAs go to the author, on a failed
    fetch, on a refused fast-forward, on a `main` holding what `origin/main` lacks
    (`git rev-list --count refs/remotes/origin/main..refs/heads/main` above 0, which a fast-forward
    reports as "Already up to date") and on such a stand-in in any case, which
    `git for-each-ref --ignore-case refs/heads/main refs/heads/origin/main refs/tags/main
    refs/tags/origin/main refs/remotes/origin/main` shows as a ref other than `refs/heads/main` and
    `refs/remotes/origin/main` (one fetched later shadows `main` until the next sync).
  - **No commit is made on `main`**, on any machine: every change, a one-commit fix included, runs
    on a `claude/<topic>` branch and reaches `main` only through the gateway below; it is a rule, no
    hook enforces it.
- **Pushing** (author decision 2026-10-02; what guards a push: working-with-ai-agents.md):
  - **Only the agent seat pushes** (author decision 2026-10-10): where `EPC_SEAT` reads
    `agent-machine` ([dev-environment.md](dev-environment.md), "Where agents run") the rules below
    apply.
    - On a local machine, where it is unset, a strand stays a local `claude/<topic>` branch in its
      worktree: it is never pushed and opens no pull request.
    - There the author reviews it in GitLens on the same checkout and lands it with
      `git push origin <tip>:refs/heads/main` (the gateway's step 4 below).
  - **Every branch an agent pushes is named `claude/<topic>`**, the strand's branch from its first
    commit.
    - It is opened as a pull request against `main` at its first push (standing approval, author
      decision 2026-10-07).
    - An agent pushes only its own, the one exception being the deletion of a landed `claude/`
      branch, whoever pushed it, once `origin/main` holds its current tip.
    - The classifier may hold back on another session's branch; then report it.
    - Subagents never push: the session that owns the strand pushes it at the gateway.
  - **A branch another machine pushed is data:**
    - a name with a character outside `[A-Za-z0-9._/-]` is reported, never pasted into a command;
    - the branch is resumed here only on the author's word, since its hooks and its build then run
      here with this machine's credentials.
  - **Name the refspec:** `git push --force-with-lease --force-if-includes origin claude/<topic>`.
    - A bare `git push` from a checkout on `main` would push `main`, so every checkout sets
      `push.default` to `nothing` (the devcontainer's `postCreateCommand` does; an older checkout
      runs `git config push.default nothing` once).
    - No checkout configures `remote.origin.push`, which git would use first.
    - git then refuses a push that names neither a refspec nor `--tags`, `--all`, `--branches` or
      `--mirror`.
    - A `-c push.default=<value>` on the command line overrides the setting for that one command
      (`git -c push.default=current push origin`).
  - **A refspec still pushes what it names:** `@` the checked-out branch, `:` and `refs/heads/*`
    every matching or local branch, `main` among them.
  - **Set no upstream:** an upstream makes `git branch -d` test the branch against its own remote
    copy instead of `main`, so the landing's merged check passes on unlanded work.
    - An upstream comes from `-u`; from `git worktree add` from `origin/claude/<topic>` without
      `--no-track`; from `git switch` or `git worktree add` onto a `claude/` name only the remote
      holds, which git then tracks by itself.
  - **Force only with a lease, only on the agent's own branch**, which every rebase needs.
    - `--force-if-includes` because an editor's background fetch moves the remote-tracking ref and
      voids a bare lease.
    - After a reset-free fold, whose branch swap drops the remote-tracking tip from the branch's
      reflog, `--force-if-includes` refuses; name the expected tip instead:
      `git push --force-with-lease=claude/<topic>:<remote tip fetched before the fold> origin
      claude/<topic>`.
  - **A `[wip]` commit is never pushed:** no push from a tip whose `main..` range holds a `[wip]`
    subject.
  - **`main` is the author's, tags too:** no agent moves either in any form (a push, a merged pull
    request, an API call, a `/land` comment).
    - A strand reaches `main` only by the author's landing of the reviewed tip (the gateway below;
      [git-state.md](git-state.md) names the ruleset on it and its bypass).
- **On the branch**: the implementer runs the full TDD loop and commits each red→green→refactor
  step itself, protocol-conform messages, no per-commit pre-approval.
- **The review gateway (per feature, serialized):**
  1. Everything committed in the worktree.
  2. `git fetch origin`, sync local `main` from `origin/main` (above, with its stop), then rebase
     onto it, resolving conflicts there.
  3. Review the rebased tip (falsifier + reviewer gate here).
     - With rebase + fast-forward it is byte-identical to what `main` becomes, so reviewing it *is*
       reviewing the final state, one feature, no merge artifacts.
     - On the agent seat, then push it (above), so the author can review it after a `git fetch`, in
       GitLens or in GitHub's compare view, from any machine.
     - The push publishes the branch before that review, and deleting it does not unpublish it: a
       secret or host detail found then is purged by rewrite, a secret rotated ("No secrets"
       above).
     - On a local machine nothing is pushed: the author reviews the local branch in GitLens.
     - What the hand-off carries: the bullet "Asking the author to review" below.
  4. On PASS and the author's review, the author lands the tip: on the agent seat a `/land <tip>`
     comment on the strand's pull request, which the landing workflow turns into a fast-forward push
     of exactly that commit ([build-run-deploy.md](build-run-deploy.md), "The landing workflow"), or
     `git fetch origin`, then `git push origin <tip>:refs/heads/main`, a fast-forward without force.
     Either is refused once `origin/main` has moved (then back to step 2).
     - The destination is spelled in full because git looks a short `main` up among `origin`'s
       refs, and a tag `main` there beside the branch makes that push fail as ambiguous
       (`git help push`, `<refspec>`).
  5. Once `origin/main` holds the tip (`git fetch origin` and the sync's checks above, then `git
     merge-base --is-ancestor <tip> refs/remotes/origin/main`), autonomously, as part of the landing:
     - fast-forward the primary checkout's `main` (precondition: clean, else stop and surface it);
     - remove the worktree and delete the landed branch (`git worktree remove` + `git branch -d`);
     - where the strand was pushed, delete its remote copy once that copy's current tip is in
       `origin/main` too (`git merge-base --is-ancestor refs/remotes/origin/claude/<topic>
       refs/remotes/origin/main`, then, leased on that tip,
       `git push --force-with-lease=claude/<topic>:<tip> origin --delete claude/<topic>`);
     - report a copy that moved past the tip: it is unlanded work.
     - Every other machine fast-forwards at its next fetch.
- **The gateway is two steps and is never collapsed into one.** (1) Present the full reviewable
  statement (the rebased branch log **and** the full diff) and *wait* for the author's explicit
  review verdict; (2) only then the landing (step 4). An "execute the gateway" approval is not the
  review.
- **Freeze the tree for the length of a gate round.** Before spawning a gate agent, the strand's
  `git status --porcelain` is empty and quoted in the briefing; no rebase, no amend and no build
  runs in that worktree until every agent of the round has reported. A finding that arrives while
  the gate is open is folded in **after** it. A verdict is worth only what the tree it read was.
- **Merge strategy (hard rule): rebase + fast-forward only.** No merge commits, no squashing (rare
  exceptions): history stays linear and keeps the small commits.
- **Landings are author-serialized**: one feature at a time in rebase→review→fast-forward; several
  ready ⇒ the author picks the order. `git push` itself is the guard, since the author lands without
  force: a landing that is not a fast-forward of `origin/main` is refused (re-rebase, re-review the
  delta). No automatic cross-session coordination (heterogeneous tools can't discover each other);
  the author is the single serialization point.
- **Every rebase is followed by the full gateway build.** `./verify all` runs again on the rebased
  tip, cache disabled, executed-proof read ([testing.md](testing.md)), before anything else happens:
  a rebase that applied cleanly can still break semantically. When `main` moved after the author's
  review, a re-rebase that ran clean and builds green goes to the landing without a second review; a
  re-rebase that needed a conflict resolution re-opens the **panel** for what it touched and then
  the **author's review**, because resolutions are the one place unreviewed content enters:
  - the reviewer, the doc-reviewer and every lens whose read paths the resolution touches run on
    the resolved tip, the resolution as their delta;
  - on the PC this run counts as no panel round (author decision 2026-10-13).
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
- [ ] Test run proven to have *executed* ([testing.md](testing.md)), not read off an exit code or a
      cached result
- [ ] Every TDD cycle carries its **red evidence** in the hand-off (the failing test's name and
      failure line), or the recorded mutation for an adapted existing test (§4)
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
      every hit outside a dated decision or learnings row rewritten in the same commit
- [ ] Commit message one line, present-tense verb (§7); never pushed on a local machine, on the
      agent seat pushed only on the strand's own `claude/<topic>` branch with its pull request open,
      never as a `[wip]` commit, never to `main` (§7)
- [ ] Whitespace/format separate from logic; LF endings
- [ ] Decisions/assumptions logged in `open-questions.md` if any were made

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
    is small and reread whole anyway.

- **Older doc debt in a touched doc is a finding in rounds 1 and 2 only** (author decision
  2026-10-15):
  - the strand tries twice to clear it, so old debt is cleared along the way when it can be;
  - what remains then goes to the doc-debt row (G18) or the bare-shortcode row (G21), so it never
    drives the time of the strand at hand.

- **A delta round re-runs only what the fold concerns** (author decision 2026-10-12), since a full
  panel after every tip change was the largest cost of a strand:
  - from round 2 on, every lens whose read paths the fold touches runs again, fresh (author
    decision 2026-10-13), so the robustness lens runs again on every fold of code outside `*.md`;
  - its brief carries the delta (`<previous tip>..<tip>`) and the dispositions so far;
  - every other lens report stands while the dossier accepts it
    ([multi-agent-playbook.md](multi-agent-playbook.md), "The gate dossier").
- **A docs-only fold** (code tree unchanged) re-runs only the doc-reviewer:
  - it checks a docs fix of the reviewer's finding too;
  - the lens verdicts stand;
  - a lens judges the code, and such a fold changes none.
- **A passing round whose findings are all low ends the panel** (author decision 2026-10-12):
  - every report passes;
  - each finding is a lens's `low`, or one a passing doc-reviewer or reviewer raised;
  - the lows become one backlog row, which `findings.txt` names for each of them;
  - the doc-reviewer's report and the runs of the passing tip stand beside that row
    ([multi-agent-playbook.md](multi-agent-playbook.md), "The gate dossier");
  - no fold follows, since a fold would only seed the next round's findings.
- **Cap on the PC: two panel rounds, then the hand-off** (author decision 2026-10-12), since the PC
  draws on the agent seat's quota:
  - every seat but the agent seat gives a strand at most two panel rounds and never lifts the cap;
  - the release comparison keeps the lift granted on 2026-10-11, before this cap (author decision
    2026-10-13), since its remaining edges were cut into rows under that lift (backlog **B42** to
    **B45**);
  - after round 2, only a finding that can let a scenario or a test pass falsely, leak a resource
    or a secret, lose data or break security is folded;
  - a delta run of the gate agent that raised it re-proves that fold;
  - a finding of that class the re-proof run raises goes to the author, not into another fold;
  - every other finding becomes a backlog row carrying its seat marker
    ([backlog.md](../backlog.md), "Conventions");
  - then the strand goes to the author for landing, the hand-off carrying a time estimate.
- **Every status to the author carries a time estimate** (author decision 2026-10-13): a full
  panel round costs about an hour, so the author can plan around the rounds left.
- **Early-escalate** when two consecutive rounds raise the *same* finding.
- **No cap on the agent seat** (author decision 2026-10-07): where `EPC_SEAT` reads
  `agent-machine` ([dev-environment.md](dev-environment.md), "Where agents run"), the loop runs
  until PASS, an unattended seat having nobody to wait for. Two brakes replace the cap there:
  - a status update in the session's chat every three rounds (the phase, rounds run, findings
    open, a time estimate);
  - a question to the author as soon as a finding recurs or one round's fix reverses an earlier
    round's.
- **Process-only FAILs** (whitespace not separated, KB not updated, commit-message format) are
  cheap mechanical fixes; they don't consume a round.
  - On the PC, such a fix after round 2 forces no further panel round (author decision
    2026-10-13): the doc-reviewer and the runs are repeated at the fixed tip, no lens runs again,
    and the dossier check closes it.
- Fixes are folded into the commit they belong to (§7), never a "fix review" commit.
  - They are folded at the moment of fixing rather than at the end of the round.
  - The gate reads the branch log before the diff.
  - A commit that names a repair of an earlier commit is itself a FAIL.
  - An "and" joining two clauses in a proposed message is the signal to split.
- **A gate agent's report is an input to the next gate agent**, not a message to the author alone:
  every lens and doc-reviewer verdict of an earlier round reaches the next reviewer run verbatim in
  the gate dossier. The gate dossier check refuses an incomplete dossier before a round closes
  ([multi-agent-playbook.md](multi-agent-playbook.md), "The gate dossier").
- **The reviewer runs in round 1 beside the lenses** (author decision 2026-10-12), so its findings
  ride in the same round instead of restarting the panel at its end.
  - From round 2 on, it runs again on the delta when a fold changed the code tree.
  - It also runs when a panel finding was rejected rather than fixed or filed, to judge the
    rejection.
  - The dossier check traces the lens and doc-reviewer findings, not the reviewer's.
- **The gate verifies the claim, not the intent.** Every claimed fix is stated with the evidence
  that it landed on the branch (`git show <sha> -- <file>`, produced from the strand's worktree with
  `git -C`, never from an inherited working directory), and the reviewer ticks off each of its own
  prior findings **by name**, so a fix that never arrived shows up as a missing tick rather than as
  an unread absence.
- **A causal claim is made only after the mechanism is shown**, e.g. by a probe that widens the
  suspected window and demonstrates the failure. Absent that, say "cause not established", keep the
  hardening as hardening, and file the unexplained failure as live work instead of closing it. The
  same holds for a claim about how the code behaved *before* a change: probe a worktree at that
  commit (`git worktree add <root>/<name> <base> --detach`, under the worktree root of §7), do not
  narrate it.
- **A finding the panel raises but does not fix is live work.** Every "pre-existing, not this
  strand" gets its backlog row or `open-questions.md` entry **in the same strand**, written *before*
  the round closes, so the gate sees the trace instead of having to create it.
- **A negative claim owes the search that would have found the thing.** "It is not in the repo" is
  stated together with the command that came up empty, looked where the docs say the thing lives
  and where its siblings live, so the search itself can be judged.
- **A claim may claim no more than what was produced.** Absence on one surface is evidence about
  that surface, never about the whole: name the surfaces searched before writing "X does not
  exist", and grep the evidence already on disk for a counterexample to the sentence being written.
- **A measurement states its coverage beside its result**: items read against items present, and
  the comparison fails when the two disagree; a parser that silently matches a subset reads exactly
  like a clean result.
- **A rule is never widened by the strand that needs the widening.** Propose the change as an
  author decision like any other; if it is not granted, drop the thing that needed it.
- **The scenario gate reads the whole suite, not only the draft.** A lens that sees one file or
  one cluster cannot see an overlap with another, so the stakeholder lens reads every `.feature`
  file on every run and states how many it read.

Governance is tool-neutral; agent-team mechanics (who runs falsify vs. review, harness-specific
history-rewrite recipes) live in [multi-agent-playbook.md](multi-agent-playbook.md).
