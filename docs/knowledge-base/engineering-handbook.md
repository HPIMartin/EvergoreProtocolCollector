# 09 — Engineering Handbook (how the code should look & how we develop)

This is the standard every change is held to. It exists so the project stays clean as it grows and
so any contributor (human or AI) produces consistent work. Summary lives in
[`/CLAUDE.md`](../../CLAUDE.md); this is the detail.

## 1. Architecture rules (hexagonal)

The **dependency rule**: dependencies point *inward*. Inner layers never import outer ones.

```
domain        — entities, value objects, domain logic (EvergoreItem, Entry, TransferType). NO framework.
application   — use cases (collect, evaluate, query) + PORT interfaces they need. NO framework.
adapters/in   — REST controllers/filters, the scheduled job (drive the application)
adapters/out  — Selenium (PageSource), ORMLite repos, file, logging (implement the ports)
config        — Micronaut @Factory wiring + @ConfigurationProperties
```

- `domain` and `application` must not import `io.micronaut`, `selenium`, `ormlite`, or any adapter.
  This is currently true for `domain`/`businessLogic` — **keep it true.**
- Every outbound dependency goes through a **port** (interface owned by the inner layer). New
  external integration ⇒ define a port first, implement it as an adapter. (The missing one today is
  `PageSource` for scraping — see backlog D1.)
- The composition root is the single place that knows concrete classes (`ApplicationFactory` / `config`).

## 2. SOLID, applied here

- **S** — one reason to change per class (e.g. parsing ≠ valuing ≠ persisting). Split `SeleniumPageSource`'s
  scraping from driver lifecycle.
- **O/L** — extend via the `TransferTypeVisitor` and similar, not by editing `switch`/`instanceof`.
- **I** — small, role-specific ports (`BankRepository`, `StorageRepository`, … not one fat repo).
- **D** — application code depends on ports; adapters depend on the domain, never the reverse.

## 3. Clean-code rules

- Intention-revealing names; no abbreviations that aren't domain terms. Avoid joke/placeholder names
  (e.g. the existing `stupidMerge()` / debug `"shit"` strings get cleaned up, not copied).
- **Code must be self-explanatory and stand on its own — understandable without any external document**
  (backlog, tickets, chat history, design notes). Names carry the meaning: name things by what they
  *are or do* (their domain/capability role), so a reader with only the source can understand them.
  A corollary: names must never reference tracker/backlog IDs (`D2`, `H8`, ticket numbers) or the task
  that produced them — e.g. `ProtocolEvaluationAcceptanceTest`, not `D2AcceptanceTest`.
- Small methods, early returns, no deep nesting. No commented-out code in commits.
- **No logic in constructors — field assignment only.** Constructors must not run logic or side
  effects (no IO, no `init()`/`ensureTable()`-style calls). Put "construct + initialize" in a static
  **factory method** (e.g. the `Repository` subclass `get(...)` methods — Effective Java Item 1) or a
  lifecycle hook. Logic in a constructor breaks testability and violates SRP.
- **Avoid the `static` keyword — treat it as a smell, above all mutable static state.** It creates
  hidden cross-instance / cross-test coupling and breaks testability & isolation (the D7 bug: a
  `public static int DELAY_IN_SEC` stomped between two boot-test contexts). Prefer dependency
  injection, instance state, or a proper seam. Boot tests that need state written by a bean at startup
  and read by the test use an **injected, DI-shared recorder** — a `@Singleton`-scoped bean provided by
  a test `@Factory` (DI scope, **not** the GoF static-`getInstance` Singleton antipattern), **not** static
  flags. A `@TestInstance(PER_CLASS)` + instance-fields alternative was tried and **broke `SmokeTest`**
  (the `@MockBean`+`@Scheduled` startup signal went unobserved), so the DI-shared recorder
  is the proven seam (see [testing.md](testing.md), `BootSignalRecorder`). The rare defensible
  case is a static *initializer* doing one-time setup that genuinely must run **before** the framework
  context boots (e.g. seeding/deleting a test DB before repositories connect) — accepted in **test**
  code; in production code, justify it hard.
- **Avoid comments — make the code say it.** No comments in code, config, or infrastructure unless
  intent genuinely can't be expressed in names/structure (rare; then explain *why*, not *what*).
  Self-documenting names + small functions replace comments. A reviewer (human or agent) flags unnecessary comments.
- **No dead code & no undeclared dependencies** (e.g. an unused class importing a library absent from
  `build.gradle.kts` — delete it).
- **The code/build is the single source of truth; docs must never duplicate volatile facts.** Versions
  (Java / Micronaut / Gradle), dependency lists, file/test counts, and other build-derived details live in
  `build.gradle.kts` and the code — docs point to them, never restate them (restated facts rot silently).
  Prose states only stable, conceptual things. If a doc ever contradicts the build, the build wins and the
  doc is fixed in the same change.
- **Prefer immutable `record`s; avoid getter/setter boilerplate.** Model value objects, DTOs and
  config as `record`s. For DTOs, **public fields** are acceptable instead of accessors (as in
  `BankEntry`/`StorageEntry`); record accessors (`config.apiToken()`) are fine — they are not bean
  getters. Don't write `getX()`/`setX()` unless a framework strictly requires it. (Micronaut binds
  `@ConfigurationProperties` to a record with no extra annotation.) Return `Optional` instead of sentinels.
- **`Logger` is the last constructor parameter** — collaborators first, the logger (incidental
  infrastructure) last.
- Constants/config over magic values; **no secrets in source** (token, credentials → config).
- **Warnings are errors.** Compiler and lint warnings are build failures — enforced in
  `build.gradle.kts` via `-Xlint:all` + `-Werror` on every `JavaCompile`. The default is to **fix**
  them, not suppress; genuinely obsolete lint is excluded deliberately: `-serial` (missing
  `serialVersionUID`, irrelevant on modern Java) and `-processing` (Micronaut/JUnit annotations no
  processor claims — inherent to the stack, not our code). When unsure whether a warning should be
  fixed or excluded, **ask the author**.

## 4. TDD workflow (red → green → refactor → commit)

1. **Red** — write the smallest failing test that expresses the next behavior.
2. **Green** — make it pass with the simplest code.
3. **Refactor** — improve the design with tests green.
4. **Commit** — one micro-cycle ≈ one commit, via the §7 protocol (propose message → confirm → commit; never push).

- Domain/application logic is unit-tested **without the framework** (plain JUnit + AssertJ, fast).
- One behavior per test; arrange/act/assert; table-driven where inputs vary (parser, item values).
- Bug fix ⇒ first write a test that reproduces it, then fix (e.g. the double `getAllDifferentAvatars`).
- A reviewer (human or agent) may gate each commit on process + criteria adherence (see §9).

## 5. BDD (product-owner perspective) — documentation / showcase only

This project has **no runtime BDD need**; BDD here exists for **documentation and as a reusable
template** demonstrating the practice. Scenarios are written in **product language**, given/when/then:

> *Given* member "Bambor" deposited 58 410 gold and crafted goods worth 169 254,
> *when* the officer opens the overview,
> *then* Bambor's "erzeugter Gildenmehrwert" shows 1 170.

**Authoring flow (author's rule):**
1. Write the Gherkin scenario(s) **"blind / up-front"** and **disable them with `@Ignore`** →
   **commit** (captures intent before implementation).
2. **Re-activate** the scenarios, then implement them green via the §4 TDD cycle (red→green→refactor)
   → **commit** when green.

- Scenarios run as acceptance tests at the application boundary, driven through ports with
  **in-memory fakes** (fake `PageSource`, `:memory:` SQLite) — no browser, no live site.
- Tooling default: **plain JUnit** given/when/then helpers. Cucumber `.feature` files only if we
  want non-developer-readable living docs (open question D-9, backlog G4).

## 6. Test strategy (pyramid)

- **Many** fast unit tests (domain values, parser, evaluator math).
- **Some** adapter/integration tests (repositories vs `:memory:` SQLite).
- **Few** acceptance tests (collect→evaluate→overview via fakes) and a thin smoke test for wiring.
- Replace brittle assertions (the Levenshtein HTML matching) once output is behind a clean port.
- **Deterministic, never wall-clock-dependent.** A test must pass on *any* hardware (a first-gen
  Raspberry Pi included — it may just take longer); correctness must never hinge on a `sleep`/`timeout`
  threshold. Waiting on an async event means waiting on a **real signal** (a `CountDownLatch.await()`
  with no timeout, a future, a condition), released on both the success *and* failure paths so a bad run
  fails an assertion instead of hanging. A timeout/retry is a *mitigation*, not a fix — never "stabilise"
  a flake by bumping one. (See [testing.md](testing.md), the boot-signal seam.)
- **Arrange discipline — shared, non-test-relevant preconditions live in `@BeforeEach`, not each test
  body.** A test body holds only the arranges that make its *act* + *assert* legible; common
  infrastructure setup (waiting for app boot/collection to finish, base-URL config) is hoisted to
  `@BeforeEach` rather than copy-pasted into every test. Duplicating a precondition across tests is noise.
  The exception: when the precondition *is* the behaviour under test (e.g. `SmokeTest.applicationIsStarting`
  asserting the boot collection completes), it stays in the test where it's the act/assert.

## 7. Git & commits (author's rules — strict)

- **Propose exactly one commit message, get the author's confirmation, THEN commit. Never `git push`.**
- Message = **single line**, **starts with a present-tense verb** (after an optional `[doc]` tag),
  briefly states what the commit actively changes. **No body. No `Co-Authored-By` or tool footer.** English.
  - ✅ `Add storage value calculation to data evaluator` · `Remove dead CsvParser` · `[doc] Groom backlog: pin H7`
  - ❌ `feat:`/`fix:`/other conventional-commit prefixes · multi-line bodies · `Co-Authored-By:` lines
- **`[doc]` prefix for pure documentation/process/meta commits** — touching only `docs/`, `.claude/`, or
  top-level `*.md`, with no product-code/build change. Code & build commits stay **bare** (verb-first).
  `[doc]` is the *only* permitted prefix; it keeps `git log` two-tier and scannable.
- **Docs that describe a code change ride in that commit** — never a standalone `Refresh/Update docs
  after X` follow-up (it can't stand alone for revert/cherry-pick).
- **LF line endings** enforced via `.gitattributes` (`* text=auto eol=lf`; binaries marked).
  Whitespace/format changes go in their **own** commit, never mixed with logic.
- One logical change per commit; **cohesion test** — group parts together only when they can't stand
  alone (would reverting or cherry-picking one without the others break the build or leave the tree
  inconsistent?); otherwise keep them separate. Keep `main` releasable; a local
  `./gradlew build` green before committing.
- **No secrets, no personal/host data — ever (hard rule).** Never commit sensitive information of any
  kind: passwords, tokens, API keys, private keys/keystores (`*.pfx`/`*.p12`/`*.jks`/`*.pem`/`*.key`),
  credentials, real personal/work emails or usernames, or machine/host/workspace details (absolute
  paths, host layout, infra). The repo is a public showcase. Such data lives in gitignored `*.local.*`
  files or a secret store and is injected at runtime — never the repo. If a secret ever reaches
  history, purge it with a history rewrite **and rotate it** (deletion alone doesn't un-leak it).
  Watch the Claude Code "always allow" flow: it can write path-bearing rules into the committed
  `settings.json` — prefer bare commands that match the existing portable `Bash(<cmd>:*)` rules, and
  keep machine-specific permissions (e.g. an absolute-path `cd`) in `.claude/settings.local.json`.
- **The commit log is the changelog.** Don't keep a separate `CHANGELOG`, and don't mirror git
  history or diffs in the docs — `git log` / `git diff` are the source of truth for *what changed*.
- **Git enforcement hooks (must be active).** The rules above are enforced mechanically by POSIX-sh
  hooks shipped in `hooks/` and wired via `core.hooksPath` — *ensure they are active* in any checkout
  (`git config core.hooksPath hooks`; the devcontainer `postCreate` sets this automatically). `commit-msg`
  enforces the single-line, present-tense-verb-first subject (optional `[doc] ` tag) and rejects a body,
  `Co-Authored-By`, or any tool footer; `pre-commit` runs the fast format/brace gate
  (`spotlessCheck` + `checkstyleMain`/`checkstyleTest`) and a staged-content scan for secrets and
  personal/host data. The test run and full build are **deliberately excluded** so the TDD
  micro-commit loop stays fast (commits are already green by the time they're made). They are a
  git-level safety net, not a substitute for the rules — `--no-verify` bypasses them and is for
  genuine emergencies only. Details: [build-run-deploy.md](build-run-deploy.md).

### Branching, merge & the review gateway (revised 2026-06-27)

**Isolate the working tree, not just the history.** A branch isolates *commits*; it does **not**
isolate the working directory — a clone has one working tree, so two writers on the same checkout race
(index/HEAD flicker, one commit clobbering another's staged work). **git worktrees** isolate it: each
is its own directory + HEAD + index. That — not the commit count — is what makes parallel work safe.

- **Default — a worktree per context.** Every problem/feature/context gets its **own branch in its own
  git worktree** (under `.claude/worktrees/`). Parallel contexts (incl. agents from other models) =
  parallel worktrees, no collision; the **primary checkout stays on `main`**, untouched. Worktrees
  branch from the **current local `main`** (`worktree.baseRef: "head"` — `origin` can lag because push
  is the author's alone, so local `HEAD` is the freshest base).
- **Carve-out — direct on `main`.** Allowed **only when you are the sole writer of the primary `main`
  checkout *and* expect exactly one commit.** The moment (a) a **second commit** becomes necessary
  **or** (b) work runs **in parallel** → move to a worktree+branch and land via the gateway. Promote
  *before* commit #1 if foreseeable (`git switch -c` / into a worktree); if #1 already landed as a
  standalone-valid commit, the rest continues on a branch off the now-current `main`. *(Two worktrees
  can never both have `main` checked out — git forbids it — so parallel work is automatically off the
  direct-on-`main` path; background/tool agents are auto-isolated into worktrees, while a second
  **foreground** session must `EnterWorktree` before editing.)*
- **On the branch:** the implementer runs the full TDD loop and **commits each red→green→refactor step
  itself** with protocol-conform messages (no per-commit pre-approval on the branch).
- **The review gateway (per feature, serialized):** (1) everything committed in the worktree;
  (2) **rebase onto current local `main`**, resolving conflicts there; (3) **review the rebased tip** —
  with rebase + fast-forward there are no merge commits and no automerge, so the rebased tip is
  **byte-identical to what `main` becomes**; reviewing it *is* reviewing the final state, scoped to one
  feature, with no merge artifacts (falsifier + reviewer gate here). **When presenting work at this
  gateway, always state the full worktree name — its branch and absolute path — so the author can open
  that worktree directly in their editor and review every changed file in one place before the merge.**
  (4) on PASS, **fast-forward
  `main`** by running, from the primary checkout, `git merge --ff-only <branch>` (precondition: the
  primary checkout is **clean** — else stop and surface it); (5) remove the worktree.
- **Merge strategy (hard rule): rebase + fast-forward only.** No merge commits, no squashing (rare
  exceptions only). History stays **linear** *and* keeps the small commits.
- **Landings are author-serialized.** Only one feature sits in rebase→review→fast-forward at a time;
  when several are ready, the **author picks the order**. `git merge --ff-only` is the hard guard — a
  branch not rebased onto the latest `main` is refused (re-rebase, re-review the delta). **No automatic
  cross-session coordination** — heterogeneous tools/contexts can't discover each other; the author is
  the single serialization point.
- **Every commit that lands on `main` is verified-good — not merely green.** Defective = builds red,
  **OR** the falsifier exposes it as *fake-green* (tests pass but assert nothing real), **OR** the
  reviewer rejects it as not implementing what it claims. None may land. Red states are never committed
  (TDD's red is transient; the commit comes after green, `@Ignore`-first Gherkin keeps the build green).
  A defect at the gateway is **repaired by rebase into the commit it belongs to, before the merge** —
  never left on `main`, never a follow-up "fix" commit.
- **Reword at the gateway** non-interactively via scripted `GIT_SEQUENCE_EDITOR` / `GIT_EDITOR` (no
  interactive TTY). **`git push` stays the author's alone** (enforced by the `deny` in
  `.claude/settings.json`).
- **Tool-neutral:** the model is plain git (worktree · branch · rebase · `--ff-only`). Claude Code
  implements it via `EnterWorktree`/`ExitWorktree` and `Agent` worktree-isolation; other tools/humans
  use `git worktree` directly.

## 8. Definition of Done (checklist)

- [ ] Compiles, `./gradlew build` green locally
- [ ] No compiler/lint warnings (`-Werror` fails the build on any; the suite is warning-clean)
- [ ] Unit tests for new logic (`@Ignore`-first Gherkin if user-facing)
- [ ] No secrets or personal/host data (passwords, tokens, keys/keystores, credentials, real emails/usernames, absolute paths, infra) in committed files
- [ ] Relevant `docs/knowledge-base/` doc updated
- [ ] Commit message proposed (one line, present-tense verb) **and confirmed** before committing; **never pushed**
- [ ] Whitespace/format separate from logic; LF endings
- [ ] Decisions/assumptions logged in `open-questions.md` if any were made

## 9. Review gate & process learnings

Before each commit, a reviewer (human, or a dedicated review agent — see
[working-with-ai-agents.md](working-with-ai-agents.md)) checks **process adherence** (was it a real
red→green→refactor micro-step? whitespace kept separate? KB updated? commit-message rules met?) and
**code criteria** (clean code, SOLID, hexagonal boundaries). If something is off: send back with
concrete feedback, and **record the lesson in `docs/process-learnings.md`** so future work — and
future agents — drift toward the process, not away from it.

**Bounded review loop → re-plan (governance).** A rejection sends the work back to re-implement, then
a *fresh* falsifier and reviewer re-check it. Keep that loop bounded:

- **Cap at two re-implement rounds (three attempts total), then re-plan.** On hitting the cap, stop
  and escalate to the author with the findings — repeated FAILs usually mean the *plan/spec* is wrong,
  not the code, so go back to planning rather than re-implement again. **Early-escalate** when two
  consecutive rounds raise the *same* finding (same signal — re-plan now).
- **Process-only FAILs** (whitespace not separated, KB not updated, commit-message format) are cheap
  mechanical fixes and **do not consume a round**.
- Fixes are **folded into the commit they belong to** (§7), never appended as a "fix review" commit.

This governance is tool-neutral; the agent-team mechanics that exercise it (who runs falsify vs.
review, harness-specific history-rewrite recipes) live in
[multi-agent-playbook.md](multi-agent-playbook.md).
