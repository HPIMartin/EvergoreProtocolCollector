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
- Durable docs (KB, decisions log, `process-learnings.md`) likewise: identify by
  symbol/behavior/file/concept, never backlog shortcode (`D5`/`H7`/…); IDs stay in the backlog; a
  shortcode in prose only as optional pointer to a **still-live** item. Completing an item removes
  its row **and** every shortcode reference repo-wide (no `git` archaeology to resolve references).
- Small methods, early returns, no deep nesting; no commented-out code in commits.
- **Strict data instead of defensive reads (author rule 2026-09-06).** Every database column is
  `NOT NULL`; a value that cannot be absent is declared so at the type, not checked at each read.
  Read paths therefore carry no null branches for states the schema forbids. Whoever wants to allow
  a nullable column owns the handling of that null everywhere it can surface, and says so. The rule
  exists because the quiet failures are worse than the loud ones: ORMLite reads a SQL `NULL` into a
  primitive `int` as **0** without an exception, so a nullable `amount` would not crash, it would
  silently change a member's sums.
- **No logic in constructors; field assignment only** (no IO, no `init()`-style calls);
  construct + initialize via static **factory method** (the `Repository` subclass
  `get(...)` methods, Effective Java Item 1) or a lifecycle hook. Constructor logic breaks
  testability and SRP.
- **`static` is a smell, mutable static state above all**: hidden cross-instance/cross-test
  coupling (a `public static int DELAY_IN_SEC` once stomped between two boot-test contexts).
  Prefer DI, instance state, a proper seam. Boot tests sharing bean-written state use an injected
  DI-shared recorder: a `@Singleton` bean from a test `@Factory` (not the GoF static-`getInstance`
  antipattern), never static flags; the `@TestInstance(PER_CLASS)` alternative broke `SmokeTest`,
  so the recorder is the proven seam (see [testing.md](testing.md), `BootSignalRecorder`).
  Defensible only as a static initializer for setup that must precede framework boot (e.g.
  seeding/deleting a test DB), in **test** code; in production, justify hard.
- **Comments and doc comments are an absolute no-go; make the code say it** (author decision
  2026-08-04). No `//`, no `/* */`, no doc-comment block (Javadoc, JSDoc) on classes, methods,
  records or fields, and none in config or infrastructure. Non-obvious knowledge goes into an
  expressive **name**, a **test name**, the KB, or [`open-questions.md`](../open-questions.md),
  never into a comment and never into both. Where the *why* is a real constraint, restructure so
  the code carries it rather than annotating it.
  - Reviewers (human or agent) flag **every** comment a diff adds, not only the ones they judge
    unnecessary: "is this one necessary?" is the judgment call that let ten of them through three
    review rounds (2026-09-02).
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
- **Immutable `record`s over getter/setter boilerplate** for value objects, DTOs, config. DTO
  public fields are fine (`BankEntry`/`StorageEntry`); record accessors (`config.apiToken()`) are
  not bean getters. No `getX()`/`setX()` unless a framework strictly requires it (Micronaut binds
  `@ConfigurationProperties` to a record with no extra annotation). Return `Optional`, not sentinels.
- **`Logger` is the last constructor parameter**: collaborators first, incidental infrastructure last.
- Constants/config over magic values; **no secrets in source** (token, credentials → config).
- **Warnings are errors**: `-Xlint:all` + `-Werror` on every `JavaCompile` in `build.gradle.kts`.
  Fix, don't suppress. Excluded as genuinely obsolete: `-serial` (missing `serialVersionUID`,
  irrelevant on modern Java), `-processing` (unclaimed Micronaut/JUnit annotations, inherent to the
  stack). Unsure fix vs. exclude ⇒ ask the author.

## 4. TDD workflow (red → green → refactor → commit)

Runs **inside** the approved `@wip` scenarios of a feature (§5): the scenarios state what "done"
means, the TDD cycles drive them green, step definitions included.

1. **Red**: smallest failing test expressing the next behavior. Run it and **watch it fail for the
   right reason**; a slow full build is no licence to skip red, run the *focused* test. **The red
   run is evidence**: the failing test's name and its failure line go into the step's report (or
   the plan) before the implementation exists. A test written beside its implementation is anchored
   to that implementation's shape, which is what produces assertions fitted to the data.
2. **Green**: simplest code that passes.
3. **Refactor**: improve design with tests green.
4. **Commit**: one full cycle = one commit, via the §7 protocol (propose message → confirm →
   commit; never push). Red and green stay uncommitted, local steps inside the cycle; only the
   refactored, green result is committed, so every commit is atomic and independently revertable.

- Domain/application logic is unit-tested **without the framework** (plain JUnit + AssertJ, fast).
  The inner loop runs `./verify focus <path>`; the feature's scenarios run through `./verify bdd`,
  and the last cycle of a feature arms them (§5).
- One behavior per test; arrange/act/assert; table-driven where inputs vary (parser, item values).
  When a class's tests repeat the same act, wrap it in a short-named helper (e.g. `snapshot()`) so
  the call itself reads as the Act, not an expression buried inside each assertion.
- Bug fix ⇒ reproducing test first, then fix (e.g. the double `getAllDifferentAvatars`).
- **An existing test adapted to a changed behavior** cannot be seen red first; it pays that debt
  with a **recorded mutation**: mutate the production code so the adapted assertion must fail, run
  only that test class (`./verify focus <path>`), keep the output, revert. The record (what was
  mutated, the failing line) goes into the step's report like red evidence. Where a harness exists
  this is the only substitution; a *new* test is always seen red.
- Where a deliverable genuinely has no test harness the stack supports (the deploy shell script),
  the substitution is **mutation testing** and it is *declared*: name it and every mutation in
  [`open-questions.md`](../open-questions.md), "Recorded mutations", in the same change, so a later
  gate re-runs them instead of taking a claim on trust. A mutation that lives only in a conversation
  is not a proof.
- A reviewer (human or agent) may gate each commit on process + criteria adherence (§9).

## 5. BDD (product-owner perspective): mandatory, and gated by the author

**BDD is not optional in this process** (the template's rule, adopted 2026-09-20 in place of the
earlier documentation-only stance). Work on observable behavior starts with executable Gherkin
scenarios, the author confirms them, and only then does implementation begin. The scenarios, not
the plan text, are what "done" means, and they stay in the repository as the executable
specification.

**Status in this project (author decision 2026-09-20): suspended until the author picks it up.**
The runner is wired and carries no scenario; the catch-up is a low-priority backlog item, not a task
of the current cut, and the `0.2.0` release goes first. Until the author selects that item
explicitly, every strand claims the exemption below in its plan, out loud, and runs the §4 TDD cycle
on unit and acceptance tests as before; nothing here is inferred as "BDD by default" in the
meantime.

**Scope.** Every feature with behavior a user, an operator or a caller can observe: new
capabilities, API and UI changes. A bug fix gets a scenario when the bug is visible at the
acceptance boundary; otherwise a reproducing unit test (§4) is its specification. A change inside
the scope of an existing scenario adjusts that scenario. **Exempt**: pure refactorings that change
no behavior, `[doc]` and process commits, and build/infra work. The exemption is *claimed
explicitly* by the planner and named in the plan; it is never the silent default.

**The artifact: `.feature` files, executed.** Scenarios are Gherkin (`Feature`, `Rule`, `Scenario`,
`Scenario Outline`) bound to step definitions and run by **cucumber-jvm** on the JUnit platform
([testing.md](testing.md) names the suite class, the tag filter and the glue package). They are the
**primary review artifact between author and agents**, written in **product language** from the
officer's perspective, understandable without reading code:

> *(example)*
>
> ```gherkin
> Feature: Guild contribution overview
>   Scenario: Deposits and crafted goods count toward a member's contribution
>     Given member "Bambor" deposited 58410 gold and crafted goods worth 169254
>     When the officer opens the overview
>     Then Bambor's "erzeugter Gildenmehrwert" shows 1170
> ```

- **Declarative, never imperative.** A step names a domain fact or outcome ("the entry is
  booked"), never a UI gesture ("click Save"); the step definition owns the *how*. Technical
  vocabulary (type names, hashes, endpoints) in a scenario is a finding.
- **One acceptance runner for the system:** cucumber-jvm drives the whole system, backend and SPA;
  the frontend keeps its Vitest unit tests only.
- **Feature files live in `src/test/resources/features/`**, one place for the system, one file per
  capability. A file over about ten scenarios is a split signal; `Rule:` groups the scenarios of one
  business rule and `Scenario Outline` carries the variants of one behavior.
- **English**, with the game's German item and transfer names quoted as the game spells them
  ([open-questions.md](../open-questions.md), 2026-09-20); code, step definitions and docs stay
  English.
- **Step definitions are test code** and follow every test rule (§6; no comments; no logic that
  belongs in the domain). Reuse an existing step phrasing before inventing one; the runner's dry run
  lists the undefined steps, and that list is the glue work before TDD starts.
- Scenarios run at the application boundary, driven through ports with **test-double fakes** (a
  fake `PageSource`, a throwaway SQLite file): no browser, no live site. A feature whose value is
  user-visible earns **one** scenario through the real artifact (the real SPA bundle in a real
  browser, as `DashboardBrowserSmokeTest` does today): "the shell is served" is not "the page shows
  data".

**Completeness (what the author confirms).** The planner declares the scenario set to be the
*whole* acceptance of the feature, checked against this list, and names what is deliberately out of
scope:

- the happy path;
- every business rule and each of its variants (`Rule:`, `Scenario Outline`);
- the error, empty and boundary cases a stakeholder would name;
- for a user-visible feature, the one scenario through the real artifact.

An implementation that later needs a behavior no scenario names has found an incomplete set; the
set goes back to the author before that behavior is written.

**The flow (mandatory order):**

1. **Draft the `.feature` up-front**, before any production code, and run it through the
   **scenario gate**: `falsifier-scenario` attacks it as a specification (ambiguity, tautology,
   untestable steps, technical language, coverage), the `reviewer` in scenario mode gates on this
   section; fix and re-gate.
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

**Projects that predate their scenarios.** This one does: the `/bdd-catch-up` skill derives the
missing scenarios from the code, which is the truth for what *is*, while the author decides what
*should be*; it is the backlog item named under the status above, run one capability per session
when the author picks it. Derived scenarios carry `@characterization` until the author confirms
them, and `./verify all` excludes that tag like `@wip`; a rejected one becomes a bug in the backlog
plus the corrected scenario tagged `@wip`.

## 6. Test strategy (pyramid)

- **Many** fast unit tests (domain values, parser, evaluator math).
- **Some** adapter/integration tests (repositories vs a throwaway SQLite file).
- **Few** acceptance tests: the armed scenarios (§5), the collect→evaluate→overview flow via fakes,
  and a thin smoke test for wiring.
- **Deterministic, never wall-clock-dependent**: must pass on any hardware (a first-gen Raspberry
  Pi may just take longer); correctness never hinges on a `sleep`/`timeout` threshold. Wait on a
  real signal (`CountDownLatch.await()` without timeout, a future, a condition), released on both
  success and failure paths so a bad run fails an assertion instead of hanging. Timeout/retry is
  mitigation, not fix; never "stabilise" a flake by bumping one ([testing.md](testing.md),
  boot-signal seam).
- **Arrange discipline**: shared, non-test-relevant preconditions (waiting for boot/collection,
  base-URL config) go in `@BeforeEach`, not each test body; a body holds only the arranges that
  make its act + assert legible. Exception: a precondition that *is* the behaviour under test
  (e.g. `SmokeTest.applicationIsStarting`) stays as the test's act/assert.
- **Test-authoring rules** (the unit under test named `tested`, the act as its own named value, the
  name over an assertion message, a behavior pinned once at one level, a seam asserted as driven, a
  run judged by proof that it executed): canonical in [testing.md](testing.md), "Testing direction"
  and "Proving a run really executed".

## 7. Git & commits (author's rules, strict)

- **Propose exactly one commit message, get the author's confirmation, THEN commit. Never `git push`.**
- Message = **single line**, **present-tense verb first** (after an optional `[doc]` tag), states
  what the commit actively changes. No body, no `Co-Authored-By` or tool footer. English. The
  `commit-msg` hook's ticket-key leg stays off: the project has no ticket system
  ([open-questions.md](../open-questions.md)).
  - ✅ `Add storage value calculation to data evaluator` · `Remove dead CsvParser` · `[doc] Groom the backlog current-status section`
  - ❌ `feat:`/`fix:`/other conventional-commit prefixes · multi-line bodies · `Co-Authored-By:` lines
- **`[doc]` prefix** for pure documentation/process/meta commits (only `docs/`, `.claude/`, or
  top-level `*.md`; no product-code/build change); code & build commits stay bare. `[doc]` is the
  only permitted prefix (keeps `git log` two-tier and scannable).
- Docs describing a code change ride in that commit, never a standalone `Refresh/Update docs after
  X` follow-up (can't stand alone for revert/cherry-pick). Before staging a doc hunk, run the
  **revert check in both directions**: "is this sentence still true after reverting just the code
  commit?" (if yes, it belongs elsewhere) and "is it true *at* the commit it rides in?" (`git grep
  <symbol> <sha>` against that commit's own tree, never the working tree, which is the tip and
  always agrees). Only backlog, roadmap and decision hygiene, and a measurement over the combined
  effect of several commits, earn a standalone `[doc]` commit, and its message says which it is.
- **LF line endings** via `.gitattributes` (`* text=auto eol=lf`; binaries marked).
  Whitespace/format changes in their **own** commit, never mixed with logic.
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
- **Every commit compiles and is green on its own, not just the tip.** A build-file or dependency
  change belongs in the **first** commit that uses it, not the one it was written for. Verify it, do
  not derive it: the "the rebased tip is byte-identical to `main`" argument covers the tip only, so
  walk the commits before making the claim
  (`for c in $(git rev-list --reverse main..HEAD); do git checkout $c && ./verify all; done`).
  A doc or test reference likewise lands no earlier than its referent.
- **A track's shape follows the work, not the plan it started with** (author decision 2026-08-15).
  "Small track, one commit on `main`" is an estimate. When the work turns out to need several
  commits, switch to a worktree and the rebase gateway below; never inflate one commit to honour the
  original estimate. Clean code covers the history too.
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
  - Worktree commands need the portable, path-eliding form: `Bash(git <sub>:*)` rules don't match
    `git -C …`, so always-allow would re-pin a host-path rule per worktree hash. One committed
    `Bash(git -C *.claude/worktrees/*)` covers every worktree with no host detail; `-C` deny
    variants (`push`, `reset --hard`, `clean`) keep that broad allow from destructive git.
- **The commit log is the changelog**: no separate `CHANGELOG`, no mirroring history/diffs in docs;
  `git log` / `git diff` are the source of truth for what changed.
- **Git enforcement hooks (must be active)**: POSIX-sh hooks in `hooks/`, wired via
  `git config core.hooksPath hooks` (devcontainer `postCreate` sets it); ensure active in any
  checkout. `commit-msg` enforces the message rules above; `content-gate` scans the staged content
  for secrets, personal/host data, per-language smells and **every comment line a diff adds** (§3,
  no opt-out); `format-gate` runs `./verify format`. Tests and full build are deliberately excluded
  (keeps the TDD micro-commit loop fast; commits are already green).
  - **A hook that only prints is not a gate.** **git asks `pre-commit` only for `git commit`**, so
    every commit the sequencer creates (rebase, `cherry-pick`, `revert`) is ungated at the moment it
    is made; `post-rewrite` and `post-commit` record it afterwards, and a recorded commit blocks
    every further commit until it is gone. `git am` and a merge commit are refused outright, by
    `pre-applypatch` and `pre-merge-commit`, which run before their commit exists. A `--no-verify`
    commit is left alone, so the emergency valve stays one.
  - **Never read a hook's output as the verdict on the commit that follows it;** ask the history.
    `hooks/self-test` asserts exactly that (resulting history, never printed text). `pre-commit`
    runs it on every commit that touches `hooks/`, and `deploy/self-test` on every commit that
    touches `deploy/`, so neither a weakened gate nor a weakened deploy check can land; it refuses a
    commit that lets the SHARED sections of `CLAUDE.md` and the agent entry template drift apart.
  - Safety net, not substitute; `--no-verify` only for genuine emergencies. Mechanics:
    [`hooks/README.md`](../../hooks/README.md); this project's gate configuration:
    [build-run-deploy.md](build-run-deploy.md).
- **`[wip]` parking commits (pause only)**: `/pause` may autonomously park uncommitted work as ONE
  local commit `[wip] <one-line state>` via `git commit --no-verify` (WIP legitimately fails the
  hooks; the one sanctioned non-emergency bypass; no-secrets rule unchanged, eyeball the diff
  first). A checkpoint, not history: `/continue` resolves it first (finish or rework, replace via
  the normal confirmed-message protocol; reset-free rewrite recipe → multi-agent playbook). Never
  survives to the review gateway or a push; parked on `main`, resolved before any new work.
- **Deleting project content is the author's act; cleaning up the scaffolding is not.** The rule
  protects the author's ownership of *his* files, and a deletion no rebase undoes.
  - **The agent never runs `rm`** (deny floor, `rm -rf` included). Anything tracked by git and
    anything the author wrote is handed over as an exact command for the author to run, or removed
    by a committed, reviewed build task whose scope lives in code rather than in a permission rule:
    probes under the gitignored `src/probe/java` go through `./gradlew clearProbes`
    ([build-run-deploy.md](build-run-deploy.md)).
  - **The development scaffolding is the agent's own, and it cleans it up itself, without asking:**
    the worktrees it created under `.claude/worktrees/` (`git worktree remove [--force]`,
    `git worktree prune`) and the branches that have landed (`git branch -d`, and its `-C` form).
    Deliberately *git-native*, never `rm`: `git branch -d` refuses a branch that is not fully merged,
    so the safety lives in the command instead of in the agent's judgment. After a `--ff-only`
    landing, removing the worktree and deleting the branch is part of the landing, not a separate
    request; the commits live on in `main`, and the reflog still holds deleted tips for a while.
  - **`git branch -D` stays off the table** (deny floor, and a Claude Code built-in besides). A
    branch that is not merged holds work nobody reviewed; discarding it is the author's call. Drop a
    genuinely stale scratch ref with `git update-ref -d refs/heads/<name>` only where the playbook's
    reset-free fold calls for it.

### Branching, merge & the review gateway (revised 2026-06-27)

**Isolate the working tree, not just the history.** A branch isolates commits, not the working
directory; two writers on one checkout race (index/HEAD flicker, clobbered staged work). git
worktrees (own directory + HEAD + index) make parallel work safe.

- **Default: a worktree per context.** Each problem/feature/context gets its own branch in its own
  worktree under `.claude/worktrees/`; parallel contexts (incl. other-model agents) = parallel
  worktrees; the primary checkout stays on `main`. Worktrees branch from current local `main`
  (`worktree.baseRef: "head"`; `origin` can lag since push is the author's alone).
- **Carve-out: direct on `main`**, only as sole writer of the primary `main` checkout **and**
  expecting exactly one commit. A second commit or parallel work ⇒ move to worktree+branch, land
  via the gateway. Promote before commit #1 if foreseeable (`git switch -c` / worktree); if #1
  already landed standalone-valid, continue on a branch off now-current `main`. (Git forbids `main`
  in two worktrees, so parallel work auto-avoids this path; background/tool agents auto-isolate
  into worktrees; a second **foreground** session creates its worktree with `git worktree` and
  drives it from the primary checkout by absolute path.)
- **On the branch**: the implementer runs the full TDD loop and commits each red→green→refactor
  step itself, protocol-conform messages, no per-commit pre-approval.
- **The review gateway (per feature, serialized):**
  1. Everything committed in the worktree.
  2. Rebase onto current local `main`, resolving conflicts there.
  3. Review the rebased tip: with rebase + fast-forward it is byte-identical to what `main`
     becomes, so reviewing it *is* reviewing the final state, one feature, no merge artifacts
     (falsifier + reviewer gate here). Always state the full worktree name (branch and absolute
     path) so the author can open it in the editor and review all changes before the merge; the
     hand-off also carries the compare range, `<tip>..<base>` (below).
  4. On PASS, fast-forward `main` from the primary checkout: `git merge --ff-only <branch>`
     (precondition: primary checkout clean, else stop and surface it).
  5. Remove the worktree and delete the landed branch (`git worktree remove` + `git branch -d`),
     autonomously, as part of the landing.
- **The gateway is two steps and is never collapsed into one.** (1) Present the full reviewable
  statement (the rebased branch log **and** the full diff) and *wait* for the author's explicit
  review verdict; (2) only then `--ff-only`. An "execute the gateway" approval is not the review.
- **Freeze the tree for the length of a gate round.** Before spawning a gate agent, the strand's
  `git status --porcelain` is empty and quoted in the briefing; no rebase, no amend and no build
  runs in that worktree until every agent of the round has reported. A finding that arrives while
  the gate is open is folded in **after** it. A verdict is worth only what the tree it read was.
- **Merge strategy (hard rule): rebase + fast-forward only.** No merge commits, no squashing (rare
  exceptions): history stays linear and keeps the small commits.
- **Landings are author-serialized**: one feature at a time in rebase→review→fast-forward; several
  ready ⇒ the author picks the order. `git merge --ff-only` is the hard guard: a branch not rebased
  onto latest `main` is refused (re-rebase, re-review the delta). No automatic cross-session
  coordination (heterogeneous tools can't discover each other); the author is the single
  serialization point.
- **Every rebase is followed by the full gateway build.** `./verify all` runs again on the rebased
  tip, cache disabled, executed-proof read ([testing.md](testing.md)), before anything else
  happens: a rebase that applied cleanly can still break semantically. When `main` moved after the
  author's review, a re-rebase that ran clean and builds green fast-forwards without a second
  review; a re-rebase that needed a conflict resolution re-opens the **panel** (falsifiers and
  reviewer on the resolved tip) and then the **author's review**, because resolutions are the one
  place unreviewed content enters.
- **Every commit landing on `main` is verified-good, not merely green.** Defective = builds red,
  OR falsifier-exposed fake-green (tests pass, assert nothing real), OR reviewer-rejected as not
  implementing what it claims; none may land. Red states are never committed (TDD red is transient;
  `@wip`-tagged Gherkin keeps the build green). A gateway defect is repaired by rebase into the
  commit it belongs to, before the merge: never left on `main`, never a follow-up "fix" commit.
- **Asking the author to review** means handing him the GitLens compare statement for the range
  (`<tip>..<base>`), never a description of where to look → working-with-ai-agents.md.
- **Reword at the gateway** non-interactively via scripted `GIT_SEQUENCE_EDITOR` / `GIT_EDITOR`
  (no interactive TTY). `git push` stays the author's alone (deny in `.claude/settings.json`).
- **Tool-neutral**: plain git (worktree · branch · rebase · `--ff-only`). Claude Code adds `Agent`
  worktree-isolation for tool agents; other tools/humans use `git worktree` directly.
- **A session never relocates into a worktree** (`EnterWorktree`, denied in
  `.claude/settings.json`): session history is filed per working directory, so a relocated session
  drops out of the primary checkout's resume list and is unreachable from the editor window that
  opened it. Sessions stay in the primary checkout and reach the worktree by absolute path, the
  discipline the cwd-drift gotcha (backlog) already demands.

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
- [ ] Every identifier, path or backlog ID the diff **deletes or renames** grepped across `docs/`, and
      every hit fixed in the same commit; every **figure** the change moves grepped repo-wide, and
      every hit outside a dated decision or learnings row rewritten in the same commit
- [ ] Commit message proposed (one line, present-tense verb) **and confirmed** before committing; **never pushed**
- [ ] Whitespace/format separate from logic; LF endings
- [ ] Decisions/assumptions logged in `open-questions.md` if any were made

## 9. Review gate & process learnings

Before each commit, a reviewer (human or review agent, see
[working-with-ai-agents.md](working-with-ai-agents.md)) checks **process adherence** (real
red→green→refactor micro-step? whitespace separate? KB updated? commit-message rules?) and **code
criteria** (clean code, SOLID, hexagonal boundaries). If off: send back with concrete feedback and
record the lesson in `docs/process-learnings.md` (so future work and agents drift toward the
process, not away from it).

**Bounded review loop → re-plan (governance).** A rejection sends work back to re-implement; a
fresh falsifier and reviewer re-check. Bounds:

- **Cap: two re-implement rounds (three attempts total), then re-plan**: stop, escalate to the
  author with findings (repeated FAILs usually mean the plan/spec is wrong, not the code).
  **Early-escalate** when two consecutive rounds raise the *same* finding.
- **Process-only FAILs** (whitespace not separated, KB not updated, commit-message format) are
  cheap mechanical fixes; they don't consume a round.
- Fixes are folded into the commit they belong to (§7), never a "fix review" commit, and folded at
  the moment of fixing rather than at the end of the round. The gate reads the branch log before
  the diff: a commit that names a repair of an earlier commit is itself a FAIL, and an "and" joining
  two clauses in a proposed message is the signal to split.
- **A gate agent's report is an input to the next gate agent**, not a message to the author alone:
  every prior verdict travels into the reviewer's brief, and a lens whose tip is no longer the one
  under review is re-run.
- **The gate verifies the claim, not the intent.** Every claimed fix is stated with the evidence
  that it landed on the branch (`git show <sha> -- <file>`, produced from the strand's worktree with
  `git -C`, never from an inherited working directory), and the reviewer ticks off each of its own
  prior findings **by name**, so a fix that never arrived shows up as a missing tick rather than as
  an unread absence.
- **A causal claim is made only after the mechanism is shown**, e.g. by a probe that widens the
  suspected window and demonstrates the failure. Absent that, say "cause not established", keep the
  hardening as hardening, and file the unexplained failure as live work instead of closing it. The
  same holds for a claim about how the code behaved *before* a change: probe a worktree at that
  commit (`git worktree add <base> --detach`), do not narrate it.
- **A finding the panel raises but does not fix is live work.** Every "pre-existing, not this
  strand" gets its backlog row or `open-questions.md` entry **in the same strand**, written *before*
  the reviewer is spawned, so the gate sees the trace instead of having to create it.
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

Governance is tool-neutral; agent-team mechanics (who runs falsify vs. review, harness-specific
history-rewrite recipes) live in [multi-agent-playbook.md](multi-agent-playbook.md).
