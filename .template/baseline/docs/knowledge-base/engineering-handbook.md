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
  connect), accepted in **test** code; in production code, justify it hard.
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
    comment line to a listed source extension (see [`hooks/README.md`](../../hooks/README.md)).
    **There is no opt-out.** What the gate refuses is deleted, not reworded.
  - The one carve-out is this repo's own process scaffolding: `hooks/*` and the `ADAPT` notes are
    prose by design and sit outside the scanned extensions.
  - Ignore files carry scaffolding prose for the adopter; `/adopt` trims it.
- **No dead code & no undeclared dependencies** (e.g. an unused class importing a library absent
  from the build manifest: delete it).
- **The code/build is the single source of truth; docs must never duplicate volatile facts.**
  Versions of {{TECH_STACK}} components, dependency lists, file/test counts, and other build-derived
  details live in the build manifest and the code; docs point to them, never restate them (restated
  facts rot silently). Prose states only stable, conceptual things. If a doc ever contradicts the
  build, the build wins and the doc is fixed in the same change.
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

> **ADAPT:** wire warnings-as-errors into your build and list your deliberate lint exclusions with
> their rationale.

## 4. TDD workflow (red → green → refactor → commit)

Runs **inside** the approved `@wip` scenarios of a feature (§5): the scenarios state what "done"
means, the TDD cycles drive them green, step definitions included.

1. **Red**: write the smallest failing test that expresses the next behavior. Run it and **watch it
   fail for the right reason**; a slow full build is no licence to skip red, run the *focused* test.
   **The red run is evidence**: the failing test's name and its failure line go into the step's
   report (or the plan) before the implementation exists. A test written beside its implementation
   is anchored to that implementation's shape, which is what produces assertions fitted to the data.
2. **Green**: make it pass with the simplest code.
3. **Refactor**: improve the design with tests green.
4. **Commit**: **one full cycle = one commit**, via the §7 protocol (propose message → confirm →
   commit; never push). Red and green stay uncommitted, local steps *inside* the cycle; only the
   refactored, green result is committed, so every commit is atomic and independently revertable.

- Domain/application logic is unit-tested **without the framework**: a plain test runner plus an
  assertion library, fast (example: JUnit + AssertJ on a Java stack). The inner loop runs
  `./verify focus <path>`; the feature's scenarios run through `./verify bdd`, and the last cycle of
  a feature arms them (§5).
- One behavior per test; arrange/act/assert; test-authoring rules in §6.
- When a class's tests repeat the same act, wrap it in a short-named helper (e.g. `snapshot()`), so
  the call itself reads as the Act rather than an expression buried inside each assertion.
- Bug fix ⇒ first write a test that reproduces it, then fix.
- **An existing test adapted to a changed behavior** cannot be seen red first; it pays that debt with
  a **recorded mutation**: mutate the production code so the adapted assertion must fail, run only
  that test class (`./verify focus <path>`), keep the output, revert. The record (what was mutated,
  the failing line) goes into the step's report like red evidence. Where a harness exists this is
  the only substitution; a *new* test is always seen red.
- Where a deliverable genuinely has no test harness the stack supports (an ops shell script, say),
  the substitution is **mutation testing** and it is *declared*: name it and every mutation in
  [`open-questions.md`](../open-questions.md) in the same change, so a later gate re-runs them
  instead of taking a claim on trust. A mutation that lives only in a conversation is not a proof.
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
> Feature: Monthly contribution overview
>   Scenario: A customer's orders count toward the contribution column
>     Given customer "Miller" placed orders worth 1200 this month
>     When the manager opens the monthly overview
>     Then Miller's contribution column shows 1200
> ```

- **Declarative, never imperative.** A step names a domain fact or outcome ("the order is paid"),
  never a UI gesture ("click Save"); the step definition owns the *how*. Technical vocabulary
  (type names, hashes, endpoints) in a scenario is a finding.
- **One acceptance runner per system.** The runner is chosen at adoption and named in
  [testing.md](testing.md); in a multi-stack system it drives the whole system, and the other stacks
  keep unit tests only. The runner per stack, and the C++ rule (acceptance through an out-of-process
  Python runner), live in the template's stack presets.
- **Feature files live where that runner expects them**, one place per system. A file over about
  ten scenarios is a split signal; `Rule:` groups the scenarios of one business rule and
  `Scenario Outline` carries the variants of one behavior.
- **English by default.** A project whose stakeholders read German may decide `# language: de` at
  adoption ([open-questions.md](../open-questions.md)); code, step definitions and docs stay English.
- **Step definitions are test code** and follow every test rule (§6; no comments; no logic that
  belongs in the domain). Reuse an existing step phrasing before inventing one; the runner's dry run
  lists the undefined steps, and that list is the glue work before TDD starts.
- Scenarios run at the application boundary, driven through ports with **in-memory fakes** (a fake
  external source, in-memory persistence): no network, no live systems. A feature whose value is
  user-visible earns **one** scenario through the real artifact (the real UI, the real bundle):
  "the shell is served" is not "the page shows data".

**Completeness (what the author confirms).** The planner declares the scenario set to be the
*whole* acceptance of the feature, checked against this list, and names what is deliberately out of
scope:

- the happy path;
- every business rule and each of its variants (`Rule:`, `Scenario Outline`);
- the error, empty and boundary cases a stakeholder would name;
- for a user-visible feature, the one scenario through the real artifact.

An implementation that later needs a behavior no scenario names has found an incomplete set; the set
goes back to the author before that behavior is written.

**The flow (mandatory order):**

1. **Draft the `.feature` up-front**, before any production code, and run it through the
   **scenario gate**: `falsifier-scenario` attacks it as a specification (ambiguity, tautology,
   untestable steps, technical language, coverage), the `reviewer` in scenario mode gates on this
   section; fix and re-gate.
2. **► Author gate 1: the author confirms the scenarios and their completeness.** A real gate, not a
   notification: implementation does not start until the author has read the scenarios and said yes
   to the set. Wrong scenarios are cheap here and expensive after three TDD cycles have been built
   on them.
3. **Commit the confirmed scenarios tagged `@wip`**: the default run excludes `@wip` (each runner's
   filter is the stack preset's business), so the build stays green while intent is captured. The
   tag stays committed until step 5.
4. **Drive them green with §4 TDD cycles**: step definitions and production code, one committed
   cycle at a time. The scenarios run through `./verify bdd` (every `@wip` scenario) or
   `./verify focus <feature file>`; removing the tag locally to run them is a fallback and is never
   committed. A scenario that turns out to be wrong goes **back to the author** as a changed
   scenario; it is never quietly reinterpreted to match what the code now does.
5. **Arm the scenarios**, the last TDD step: when every scenario of the feature passes, remove `@wip`
   in its own commit (`Arm the <feature> scenarios`). From here `./verify all` runs them on every
   commit and at every gate.
6. **Refactor the feature** under the armed net: improve the design across the feature with the
   scenarios and unit tests green, one green commit per step, behavior unchanged. A change that
   needs new behavior or a new port is not a refactoring; it goes back to planning.
7. **The panel and the review gate** (§9, the playbook), then **author gate 2**, the review of the
   rebased tip, then the landing (§7).

**Projects that predate their scenarios.** An existing codebase adopting this process gets its
scenarios before any feature work: the `/bdd-catch-up` skill derives them from the code, which is
the truth for what *is*, while the author decides what *should be*. Derived scenarios carry
`@characterization` until the author confirms them, and `./verify all` excludes that tag like
`@wip`; a rejected one becomes a bug in the backlog plus the corrected scenario tagged `@wip`.

## 6. Test strategy (pyramid)

- **Many** fast unit tests (domain values, parsing, core computations).
- **Some** adapter/integration tests (each adapter against an in-memory or lightweight double of its
  real technology, e.g. an in-memory database).
- **Few** acceptance tests (the end-to-end use-case flow via fakes) and a thin smoke test for wiring.
- Assert behind clean ports, not against raw external formats: fuzzy assertions on raw output
  (HTML similarity matching and the like) are brittle; put the output behind a port, then assert on
  the port's types.
- **Deterministic, never wall-clock-dependent.** A test must pass on *any* hardware, however slow
  (a slow run may simply take longer); correctness must never hinge on a `sleep`/timeout threshold.
  Waiting on an async event means waiting on a **real completion signal** (a latch awaited with no
  timeout, a future, a condition), released on both the success *and* the failure path so a bad run
  fails an assertion instead of hanging. A timeout or retry is a *mitigation*, not a fix; never
  "stabilize" a flaky test by bumping a timeout or adding retries. (See [testing.md](testing.md).)
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
- **Tests rank above production code** and get the greater care of the two. Strict
  arrange/act/assert with **the act as its own named value**: `int status = statusOfGet(path);` then
  assert on `status`, never `assertThat(get(path).getStatus())`. A custom assert helper is the other
  allowed shape and holds the same structure inside.
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
- **Proving a run really executed.** A test run is judged by proof that it *executed*, never by its
  exit code. Deleting outputs is not enough where a build cache is on: a restored cache entry
  rewrites the result files, so exit code and result count are both an earlier run's artifacts. The
  gate build therefore disables the cache, and it counts only when the test task's line stands bare
  of any `UP-TO-DATE`/`FROM-CACHE` marker **and** the result count is read alongside it. This matters
  beyond bookkeeping: load-sensitive failures only appear when the suites really run, and a cache hit
  hides exactly that class. For a load-sensitive change, two consecutive clean runs are the bar, and
  a cached second run is not one of them.

## 7. Git & commits (author's rules, strict)

- **Propose exactly one commit message, get the author's confirmation, THEN commit. Never `git push`.**
- Message = **single line**, **starts with a present-tense verb** (after an optional `[doc]` tag),
  briefly states what the commit actively changes. **No body. No `Co-Authored-By` or tool footer.** English.
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
- **Every commit compiles and is green on its own, not just the tip.** A build-file or dependency
  change belongs in the **first** commit that uses it, not the one it was written for. Verify it, do
  not derive it: the "the rebased tip is byte-identical to `main`" argument covers the tip only, so
  walk the commits before making the claim
  (`for c in $(git rev-list --reverse main..HEAD); do git checkout $c && ./verify all; done`).
  A doc or test reference likewise lands no earlier than its referent.
- **A track's shape follows the work, not the plan it started with.** "Small track, one commit on
  `main`" is an estimate, not a constraint. When the work turns out to need several commits, switch
  to a worktree and the rebase gateway below; never inflate one commit to honor the original
  estimate. Clean code covers the history too.
- **Always able to build, not always allowed to deliver.** `./verify all` must be unconditionally
  able to run green: never attach vulnerability or quality gates (CVE thresholds, quality metrics)
  to the build/check tasks the inner dev loop runs. Such gates live on separate on-demand tasks or
  CI stages, each with an explicit suppression escape hatch for consciously accepted findings.
  **Deliver only CI-produced artifacts, never a local build.** (Why: an external or transitive issue
  the developer cannot fix on the spot must never block the TDD loop; gating is a delivery concern
  and belongs where delivery happens.)
- **No secrets, no personal/host data, ever (hard rule).** Never commit sensitive information of any
  kind: passwords, tokens, API keys, private keys/keystores (`*.pfx`/`*.p12`/`*.jks`/`*.pem`/`*.key`),
  credentials, real personal/work emails or usernames, or machine/host/workspace details (absolute
  paths, host layout, infra). Treat the repo as public. Such data lives in gitignored `*.local.*`
  files or a secret store and is injected at runtime, never the repo. If a secret ever reaches
  history, purge it with a history rewrite **and rotate it** (deletion alone doesn't un-leak it).
  Watch the Claude Code "always allow" flow: it can write path-bearing rules into the committed
  `settings.json`; prefer bare commands that match the existing portable `Bash(<cmd>:*)` rules, and
  keep machine-specific permissions (e.g. an absolute-path `cd`) in `.claude/settings.local.json`.
  For the **worktree workflow**, give a `git -C <worktree> …` command its **portable, path-eliding**
  form: the portable `Bash(git <sub>:*)` rules don't match it (they prefix-match `git <sub>`, not
  `git -C …`), so without a wildcard the always-allow flow re-pins a dead, host-path-bearing rule on
  every new worktree hash. A single committed `Bash(git -C *.claude/worktrees/*)` (the `*` elides the
  absolute path) covers **every** worktree with no host detail; `-C` deny variants (`push`, `reset`
  in all forms, `clean`, `branch -D`) keep that broad allow from auto-running destructive git.
- **The commit log is the changelog.** Don't keep a separate `CHANGELOG`, and don't mirror git
  history or diffs in the docs; `git log` / `git diff` are the source of truth for *what changed*.
- **Git enforcement hooks (must be active).** The rules above are enforced mechanically by POSIX-sh
  hooks shipped in [`hooks/`](../../hooks/README.md) and wired via `core.hooksPath`; *ensure they are
  active* in any checkout (`git config core.hooksPath hooks`; the devcontainer variant wires that
  into `postCreate`, the native variant runs it once per checkout, see dev-environment.md).
  `commit-msg` enforces the message rules above; `content-gate` scans for secrets, personal/host
  data, per-language smells and **every comment line a diff adds** (§3, no opt-out); `format-gate`
  runs the stack's fast format/style check. The test run and full build are **deliberately excluded**
  so the TDD micro-commit loop stays fast (commits are already green by the time they are made).
  - **A hook that only prints is not a gate.** **git asks `pre-commit` only for `git commit`**, so
    every commit the sequencer builds itself (rebase, `git rebase --continue`, `cherry-pick`,
    `revert`) is ungated at the moment it is made. `post-rewrite` and `post-commit` therefore
    *record* such a sha after the fact, and `pre-commit` refuses every further commit while a
    recorded sha is reachable from `HEAD`; the record clears itself once it is not, so the reset-free
    fold is a valid way out. `git am` and a merge commit are refused **outright**, by
    `pre-applypatch` and `pre-merge-commit`, which run before their commit exists.
  - **Never read a hook's output as the verdict on the commit that follows it;** ask the history.
    `hooks/self-test` asserts exactly that (resulting history, never printed text), and `pre-commit`
    runs it on every commit that touches `hooks/`, so a weakened gate cannot land.
  - Hooks are a git-level safety net, not a substitute for the rules; `--no-verify` bypasses them and
    is for genuine emergencies only. Details: [`hooks/README.md`](../../hooks/README.md).

  > **ADAPT:** `hooks/format-gate` and `hooks/content-gate` carry the stack-specific spots (build
  > wrapper + format command, scanned source extensions, host-path allowlist, production-source tree,
  > smell patterns), and `hooks/self-test`'s fixtures must match them. `grep -rn "ADAPT:" hooks/`.
- **`[wip]` parking commits (pause only).** `/pause` (shipped as a skill in
  [`.claude/skills/pause/`](../../.claude/skills/pause/SKILL.md)) may autonomously park uncommitted
  work as ONE local commit `[wip] <one-line state>` via `git commit --no-verify` (WIP legitimately
  fails the hooks; this is the one sanctioned non-emergency bypass, and the no-secrets rule applies
  unchanged, so the diff is eyeballed before parking). A `[wip]` commit is a checkpoint, not history:
  `/continue` ([`.claude/skills/continue/`](../../.claude/skills/continue/SKILL.md)) resolves it
  first (finish or rework, then replace it through the normal confirmed-message protocol; reset-free
  rewrite recipe → [multi-agent-playbook.md](multi-agent-playbook.md)). It never survives to the
  review gateway or a push; parked directly on `main`, it is resolved before any new work starts.
- **Deleting project content is the author's act; cleaning up the scaffolding is not.** The rule
  protects the author's ownership of *his* files, and a deletion no rebase undoes.
  - **The agent never runs `rm`** (it is on the permission deny floor, `rm -rf` included). Anything
    tracked by git and anything the author wrote is handed over as an exact command for the author to
    run, or removed by a committed, reviewed build task whose scope lives in code rather than in a
    permission rule.
  - **The development scaffolding is the agent's own, and it cleans it up itself, without asking:**
    the worktrees it created under `.claude/worktrees/` (`git worktree remove [--force]`,
    `git worktree prune`) and the branches that have landed (`git branch -d`, and its `-C` form). This
    is deliberately *git-native*, never `rm`: `git branch -d` refuses a branch that is not fully
    merged, so the safety lives in the command instead of in the agent's judgment. After a
    `--ff-only` landing, removing the worktree and deleting the branch is part of the landing, not a
    separate request; the commits live on in `main`, the label and directory are pure redundancy, and
    the reflog still holds deleted tips for a while.
  - **`git branch -D` stays off the table** (deny floor, and a Claude Code built-in besides). A branch
    that is not merged holds work nobody reviewed; discarding it is the author's call. Drop a
    genuinely stale scratch ref with `git update-ref -d refs/heads/<name>` only where the playbook's
    reset-free fold calls for it.

### Branching, merge & the review gateway

**Isolate the working tree, not just the history.** A branch isolates *commits*; it does **not**
isolate the working directory: a clone has one working tree, so two writers on the same checkout race
(index/HEAD flicker, one commit clobbering another's staged work). **git worktrees** isolate it: each
is its own directory + HEAD + index. That, not the commit count, is what makes parallel work safe.

- **Default: a worktree per context.** Every problem/feature/context gets its **own branch in its own
  git worktree** (under `.claude/worktrees/`). Parallel contexts (incl. agents from other models) =
  parallel worktrees, no collision; the **primary checkout stays on `main`**, untouched. Worktrees
  branch from the **current local `main`** (`worktree.baseRef: "head"`; `origin` can lag because push
  is the author's alone, so local `HEAD` is the freshest base).
- **Carve-out: direct on `main`.** Allowed **only when you are the sole writer of the primary `main`
  checkout *and* expect exactly one commit.** The moment (a) a **second commit** becomes necessary
  **or** (b) work runs **in parallel** → move to a worktree+branch and land via the gateway. Promote
  *before* commit #1 if foreseeable (`git switch -c` / into a worktree); if #1 already landed as a
  standalone-valid commit, the rest continues on a branch off the now-current `main`. *(Two worktrees
  can never both have `main` checked out, git forbids it, so parallel work is automatically off the
  direct-on-`main` path; background/tool agents are auto-isolated into worktrees, while a second
  **foreground** session creates its worktree with `git worktree` and drives it by absolute path.)*
- **A session never relocates itself into a worktree** (`EnterWorktree`, denied in
  `.claude/settings.json`): a harness that files session history per working directory drops a
  relocated session out of the primary checkout's resume list, unreachable from the editor window
  that opened it, and it rules out driving parallel strands from one window. Sessions stay in the
  primary checkout and reach the worktree by **absolute path**, the same discipline the
  working-directory-drift rule already demands (playbook, environment gotchas). Subagent worktree
  isolation is untouched: it isolates the agent, it does not move the session.
- **On the branch:** the implementer runs the full TDD loop and **commits each red→green→refactor step
  itself** with protocol-conform messages (no per-commit pre-approval on the branch).
- **The review gateway (per feature, serialized):** (1) everything committed in the worktree;
  (2) **rebase onto current local `main`**, resolving conflicts there; (3) **review the rebased tip**:
  with rebase + fast-forward there are no merge commits and no automerge, so the rebased tip is
  **byte-identical to what `main` becomes**; reviewing it *is* reviewing the final state, scoped to one
  feature, with no merge artifacts (falsifier + reviewer gate here). **When presenting work at this
  gateway, always state the full worktree name (its branch and absolute path) so the author can open
  that worktree directly in their editor and review every changed file in one place before the merge.**
  The hand-off also carries the compare range, `<tip>..<base>`
  ([working-with-ai-agents.md](working-with-ai-agents.md), "Handing the author a review").
  (4) on PASS, **fast-forward `main`** by running, from the primary checkout,
  `git merge --ff-only <branch>` (precondition: the primary checkout is **clean**, else stop and
  surface it); (5) remove the worktree and delete the landed branch (`git worktree remove` +
  `git branch -d`), autonomously, as part of the landing.
- **The gateway is two steps and is never collapsed into one.** (1) Present the full reviewable
  statement (the rebased branch log **and** the full diff) and *wait* for the author's explicit
  review verdict; (2) only then `--ff-only`. An "execute the gateway" approval is not the review.
- **Freeze the tree for the length of a gate round.** Before spawning a gate agent, the strand's
  `git status --porcelain` is empty and quoted in the briefing; no rebase, no amend and no build runs
  in that worktree until every agent of the round has reported. A finding that arrives while the gate
  is open is folded in **after** it. A verdict is worth only what the tree it read was.
- **Merge strategy (hard rule): rebase + fast-forward only.** No merge commits, no squashing (rare
  exceptions only). History stays **linear** *and* keeps the small commits.
- **Landings are author-serialized.** Only one feature sits in rebase→review→fast-forward at a time;
  when several are ready, the **author picks the order**. `git merge --ff-only` is the hard guard: a
  branch not rebased onto the latest `main` is refused (re-rebase, re-review the delta). **No automatic
  cross-session coordination**: heterogeneous tools/contexts can't discover each other; the author is
  the single serialization point.
- **Every rebase is followed by the full gateway build.** `./verify all` runs again on the rebased
  tip, cache disabled, executed-proof read (§6), before anything else happens: a rebase that applied
  cleanly can still break semantically. When `main` moved after the author's review, a re-rebase
  that ran clean and builds green fast-forwards without a second review; a re-rebase that needed a
  conflict resolution re-opens the **panel** (falsifiers and reviewer on the resolved tip) and then
  the **author's review**, because resolutions are the one place unreviewed content enters.
- **Every commit that lands on `main` is verified-good, not merely green.** Defective = builds red,
  **OR** the falsifier exposes it as *fake-green* (tests pass but assert nothing real), **OR** the
  reviewer rejects it as not implementing what it claims. None may land. Red states are never committed
  (TDD's red is transient; the commit comes after green, `@wip`-tagged Gherkin keeps the build green).
  A defect at the gateway is **repaired by rebase into the commit it belongs to, before the merge**:
  never left on `main`, never a follow-up "fix" commit.
- **Reword at the gateway** non-interactively via scripted `GIT_SEQUENCE_EDITOR` / `GIT_EDITOR` (no
  interactive TTY). **`git push` stays the author's alone** (enforced by the `deny` in
  `.claude/settings.json`).
- **Tool-neutral:** the model is plain git (worktree · branch · rebase · `--ff-only`). Claude Code
  adds `Agent` worktree-isolation for tool agents; other tools/humans use `git worktree` directly.

## 8. Definition of Done (checklist)

- [ ] **BDD scenario(s) written up-front and confirmed by the author** (§5), and now passing, or the
      exemption explicitly claimed and named in the plan
- [ ] Compiles, `./verify all` green locally, **and each commit green on its own**, proven by a walk
      over the commits rather than derived from the tip
- [ ] Test run proven to have *executed* (§6), not read off an exit code or a cached result
- [ ] Every TDD cycle carries its **red evidence** in the hand-off (the failing test's name and
      failure line), or the recorded mutation for an adapted existing test (§4)
- [ ] No compiler/lint warnings (the build fails on any; the suite stays warning-clean)
- [ ] Unit tests for new logic; no comment or doc comment added anywhere in the diff (§3)
- [ ] No secrets or personal/host data (passwords, tokens, keys/keystores, credentials, real
      emails/usernames, absolute paths, infra) in committed files
- [ ] Relevant `docs/knowledge-base/` doc updated, riding in the commit whose change it describes,
      each hunk checked in both directions (§7)
- [ ] Every identifier, path or backlog ID the diff **deletes or renames** grepped across `docs/`, and
      every hit fixed in the same commit; every **figure** the change moves grepped repo-wide, and
      every hit outside a dated decision or learnings row rewritten in the same commit
- [ ] Commit message proposed (one line, present-tense verb) **and confirmed** before committing;
      **never pushed**
- [ ] Whitespace/format separate from logic; LF endings
- [ ] Decisions/assumptions logged in [`open-questions.md`](../open-questions.md) if any were made

## 9. Review gate & process learnings

Before each commit, a reviewer (human, or a dedicated review agent, see
[working-with-ai-agents.md](working-with-ai-agents.md)) checks **process adherence** (was it a real
red→green→refactor micro-step? whitespace kept separate? KB updated? commit-message rules met?) and
**code criteria** (clean code, SOLID, hexagonal boundaries). If something is off: send back with
concrete feedback, and **record the lesson in [`process-learnings.md`](../process-learnings.md)** so
future work, and future agents, drift toward the process, not away from it.

**Bounded review loop → re-plan (governance).** A rejection sends the work back to re-implement, then
a *fresh* falsifier and reviewer re-check it. Keep that loop bounded:

- **Cap at two re-implement rounds (three attempts total), then re-plan.** On hitting the cap, stop
  and escalate to the author with the findings: repeated FAILs usually mean the *plan/spec* is wrong,
  not the code, so go back to planning rather than re-implement again. **Early-escalate** when two
  consecutive rounds raise the *same* finding (same signal, re-plan now).
- **Process-only FAILs** (whitespace not separated, KB not updated, commit-message format) are cheap
  mechanical fixes and **do not consume a round**.
- Fixes are **folded into the commit they belong to** (§7), never appended as a "fix review" commit,
  and folded at the moment of fixing rather than at the end of the round. The gate reads the branch
  log before the diff: a commit that names a repair of an earlier commit is itself a FAIL, and an
  "and" joining two clauses in a proposed message is the signal to split.
- **A gate agent's report is an input to the next gate agent**, not a message to the author alone:
  every prior verdict travels into the reviewer's brief, and a lens whose tip is no longer the one
  under review is re-run.
- **The gate verifies the claim, not the intent.** Every claimed fix is stated with the evidence that
  it landed on the branch (`git show <sha> -- <file>`, produced from the strand's worktree with
  `git -C`, never from an inherited working directory), and the reviewer ticks off each of its own
  prior findings **by name**, so a fix that never arrived shows up as a missing tick rather than as
  an unread absence.
- **A causal claim is made only after the mechanism is shown**, e.g. by a probe that widens the
  suspected window and demonstrates the failure. Absent that, say "cause not established", keep the
  hardening as hardening, and file the unexplained failure as live work instead of closing it. The
  same holds for a claim about how the code behaved *before* a change: probe a worktree at that
  commit (`git worktree add <base> --detach`), do not narrate it.
- **A finding the panel raises but does not fix is live work.** Every "pre-existing, not this strand"
  gets its backlog row or `open-questions.md` entry **in the same strand**, written *before* the
  reviewer is spawned, so the gate sees the trace instead of having to create it.
- **A negative claim owes the search that would have found the thing.** "It is not in the repo" is
  stated together with the command that came up empty, looked where the docs say the thing lives and
  where its siblings live, so the search itself can be judged.
- **A claim may claim no more than what was produced.** Absence on one surface is evidence about
  that surface, never about the whole: name the surfaces searched before writing "X does not exist",
  and grep the evidence already on disk for a counterexample to the sentence being written.
- **A measurement states its coverage beside its result**: items read against items present, and the
  comparison fails when the two disagree; a parser that silently matches a subset reads exactly like
  a clean result.
- **A rule is never widened by the strand that needs the widening.** Propose the change as an author
  decision like any other; if it is not granted, drop the thing that needed it.

This governance is tool-neutral; the agent-team mechanics that exercise it (who runs falsify vs.
review, harness-specific history-rewrite recipes) live in
[multi-agent-playbook.md](multi-agent-playbook.md).
