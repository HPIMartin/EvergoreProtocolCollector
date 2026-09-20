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
- **Avoid comments; make the code say it.** No comments in code, config, or infrastructure unless
  intent genuinely can't be expressed in names/structure (rare; then explain *why*, not *what*).
  Self-documenting names + small functions replace comments. A reviewer (human or agent) flags
  unnecessary comments.
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

1. **Red**: write the smallest failing test that expresses the next behavior.
2. **Green**: make it pass with the simplest code.
3. **Refactor**: improve the design with tests green.
4. **Commit**: one micro-cycle ≈ one commit, via the §7 protocol (propose message → confirm →
   commit; never push).

- Domain/application logic is unit-tested **without the framework**: a plain test runner plus an
  assertion library, fast (example: JUnit + AssertJ on a Java stack). The inner loop runs
  `{{TEST_CMD}}`.
- One behavior per test; arrange/act/assert; test-authoring rules in §6.
- Bug fix ⇒ first write a test that reproduces it, then fix.
- A reviewer (human or agent) may gate each commit on process + criteria adherence (see §9).

## 5. BDD (product-owner perspective)

> **ADAPT:** decide whether BDD is a runtime need for your project or serves documentation/showcase
> only, and record the decision in [open-questions.md](../open-questions.md).

Scenarios are written in **product language**, given/when/then:

> *(example)* *Given* customer "Miller" placed orders worth 1 200 this month,
> *when* the manager opens the monthly overview,
> *then* Miller's "contribution" column shows 1 200.

**Authoring flow (author's rule):**

1. Write the Gherkin scenario(s) **"blind / up-front"** and **disable them** with the test
   framework's skip mechanism (`@Disabled`/`@Ignore`, skip markers) → **commit** (captures intent
   before implementation).
2. **Re-activate** the scenarios, then implement them green via the §4 TDD cycle
   (red→green→refactor) → **commit** when green.

- Scenarios run as acceptance tests at the application boundary, driven through ports with
  **in-memory fakes** (a fake external source, in-memory persistence): no network, no live systems.
- Tooling default: **plain test-framework** given/when/then helpers. Dedicated Gherkin tooling
  (e.g. Cucumber `.feature` files) only if non-developer-readable living docs are wanted; record
  that as an open question before adopting it.

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

## 7. Git & commits (author's rules, strict)

- **Propose exactly one commit message, get the author's confirmation, THEN commit. Never `git push`.**
- Message = **single line**, **starts with a present-tense verb** (after an optional `[doc]` tag),
  briefly states what the commit actively changes. **No body. No `Co-Authored-By` or tool footer.** English.
  - ✅ `Add order value calculation to the evaluator` · `Remove dead legacy parser` · `[doc] Groom the backlog current-status section`
  - ❌ `feat:`/`fix:`/other conventional-commit prefixes · multi-line bodies · `Co-Authored-By:` lines
- **`[doc]` prefix for pure documentation/process/meta commits**: touching only `docs/`, `.claude/`, or
  top-level `*.md`, with no product-code/build change. Code & build commits stay **bare** (verb-first).
  `[doc]` is the *only* permitted prefix; it keeps `git log` two-tier and scannable.
- **Docs that describe a code change ride in that commit**, never a standalone "update docs after X"
  follow-up (it can't stand alone for revert/cherry-pick).
- **LF line endings** enforced via `.gitattributes` (`* text=auto eol=lf`; binaries marked).
  Whitespace/format changes go in their **own** commit, never mixed with logic.
- One logical change per commit; **cohesion test**: group parts together only when they can't stand
  alone (would reverting or cherry-picking one without the others break the build or leave the tree
  inconsistent?); otherwise keep them separate. Keep `main` releasable; `{{BUILD_CMD}}` green
  locally before committing.
- **Always able to build, not always allowed to deliver.** `{{BUILD_CMD}}` must be unconditionally
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
  `commit-msg` enforces the single-line,
  present-tense-verb-first subject (optional `[doc] ` tag) and rejects a body, `Co-Authored-By`, or
  any tool footer; `pre-commit` runs a staged-content scan for secrets and personal/host data plus
  your stack's fast format/style gate once wired (its ADAPT spot; ships as a commented example). The test run and full build are **deliberately excluded** so the TDD
  micro-commit loop stays fast (commits are already green by the time they are made). Hooks are a
  git-level safety net, not a substitute for the rules; `--no-verify` bypasses them and is for
  genuine emergencies only. Details: [build-run-deploy.md](build-run-deploy.md).

  > **ADAPT:** wire your stack's fast format/style checks into `hooks/pre-commit` (they must run in
  > seconds, so the micro-commit loop stays fast).
- **`[wip]` parking commits (pause only).** `/pause` (shipped as a skill in
  [`.claude/skills/pause/`](../../.claude/skills/pause/SKILL.md)) may autonomously park uncommitted
  work as ONE local commit `[wip] <one-line state>` via `git commit --no-verify` (WIP legitimately
  fails the hooks; this is the one sanctioned non-emergency bypass, and the no-secrets rule applies
  unchanged, so the diff is eyeballed before parking). A `[wip]` commit is a checkpoint, not history:
  `/continue` ([`.claude/skills/continue/`](../../.claude/skills/continue/SKILL.md)) resolves it
  first (finish or rework, then replace it through the normal confirmed-message protocol; reset-free
  rewrite recipe → [multi-agent-playbook.md](multi-agent-playbook.md)). It never survives to the
  review gateway or a push; parked directly on `main`, it is resolved before any new work starts.

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
  **foreground** session must `EnterWorktree` before editing.)*
- **On the branch:** the implementer runs the full TDD loop and **commits each red→green→refactor step
  itself** with protocol-conform messages (no per-commit pre-approval on the branch).
- **The review gateway (per feature, serialized):** (1) everything committed in the worktree;
  (2) **rebase onto current local `main`**, resolving conflicts there; (3) **review the rebased tip**:
  with rebase + fast-forward there are no merge commits and no automerge, so the rebased tip is
  **byte-identical to what `main` becomes**; reviewing it *is* reviewing the final state, scoped to one
  feature, with no merge artifacts (falsifier + reviewer gate here). **When presenting work at this
  gateway, always state the full worktree name (its branch and absolute path) so the author can open
  that worktree directly in their editor and review every changed file in one place before the merge.**
  (4) on PASS, **fast-forward `main`** by running, from the primary checkout,
  `git merge --ff-only <branch>` (precondition: the primary checkout is **clean**, else stop and
  surface it); (5) remove the worktree.
- **Merge strategy (hard rule): rebase + fast-forward only.** No merge commits, no squashing (rare
  exceptions only). History stays **linear** *and* keeps the small commits.
- **Landings are author-serialized.** Only one feature sits in rebase→review→fast-forward at a time;
  when several are ready, the **author picks the order**. `git merge --ff-only` is the hard guard: a
  branch not rebased onto the latest `main` is refused (re-rebase, re-review the delta). **No automatic
  cross-session coordination**: heterogeneous tools/contexts can't discover each other; the author is
  the single serialization point.
- **Every commit that lands on `main` is verified-good, not merely green.** Defective = builds red,
  **OR** the falsifier exposes it as *fake-green* (tests pass but assert nothing real), **OR** the
  reviewer rejects it as not implementing what it claims. None may land. Red states are never committed
  (TDD's red is transient; the commit comes after green, ignore-first Gherkin keeps the build green).
  A defect at the gateway is **repaired by rebase into the commit it belongs to, before the merge**:
  never left on `main`, never a follow-up "fix" commit.
- **Reword at the gateway** non-interactively via scripted `GIT_SEQUENCE_EDITOR` / `GIT_EDITOR` (no
  interactive TTY). **`git push` stays the author's alone** (enforced by the `deny` in
  `.claude/settings.json`).
- **Tool-neutral:** the model is plain git (worktree · branch · rebase · `--ff-only`). Claude Code
  implements it via `EnterWorktree`/`ExitWorktree` and `Agent` worktree-isolation; other tools/humans
  use `git worktree` directly.

## 8. Definition of Done (checklist)

- [ ] Compiles, `{{BUILD_CMD}}` green locally
- [ ] No compiler/lint warnings (the build fails on any; the suite stays warning-clean)
- [ ] Unit tests for new logic (ignore-first Gherkin if user-facing)
- [ ] No secrets or personal/host data (passwords, tokens, keys/keystores, credentials, real
      emails/usernames, absolute paths, infra) in committed files
- [ ] Relevant `docs/knowledge-base/` doc updated
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
- Fixes are **folded into the commit they belong to** (§7), never appended as a "fix review" commit.

This governance is tool-neutral; the agent-team mechanics that exercise it (who runs falsify vs.
review, harness-specific history-rewrite recipes) live in
[multi-agent-playbook.md](multi-agent-playbook.md).
