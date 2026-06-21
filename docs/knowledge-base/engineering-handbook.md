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
  (the `@MockBean`+`@Scheduled` startup signal went unobserved → 60s timeout), so the DI-shared recorder
  is the proven seam (see [testing.md](testing.md), `BootSignalRecorder`). The rare defensible
  case is a static *initializer* doing one-time setup that genuinely must run **before** the framework
  context boots (e.g. seeding/deleting a test DB before repositories connect) — accepted in **test**
  code; in production code, justify it hard.
- **Avoid comments — make the code say it.** No comments in code, config, or infrastructure unless
  intent genuinely can't be expressed in names/structure (rare; then explain *why*, not *what*).
  Self-documenting names + small functions replace comments. The reviewer flags unnecessary comments.
- **No dead code & no undeclared dependencies** (e.g. an unused class importing a library absent from
  `build.gradle.kts` — delete it).
- Prefer `record`s / immutability for value objects. Return `Optional` instead of sentinels
  (replace `getNewest()`'s `MIN_VALUE` object).
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

### Branching, merge & the review gateway (decided 2026-06-20)

Work runs on one of two tracks, chosen **up-front by the planner** and announced before starting (so
the author can veto). Size isn't always knowable in advance — if a "small" item balloons mid-flight,
move the *uncommitted* WIP onto a branch (`git switch -c`) **before** it grows, keeping `main` clean.

- **Small / single TDD cycle** (≈ effort S, one logical change, no new BDD scenarios): work **directly
  on `main`**, land **one** commit via the protocol above (propose message → confirm → commit).
- **Large / multiple cycles or new BDD scenarios** (≈ M/L): the planner creates a **feature branch**;
  the implementer runs the full TDD loop and **commits each red→green→refactor step itself** with
  protocol-conform messages (no per-commit pre-approval on the branch — best effort to the rules);
  the falsifier and reviewer review the **branch diff**; then the **author + planner review the
  branch's `git log` together as the final gateway** before it lands.

- **Merge strategy (hard rule): rebase + fast-forward only.** No merge commits, no squashing (rare
  exceptions only). `git rebase main && git switch main && git merge --ff-only <branch>` keeps history
  **linear** *and* preserves the small commits.
- **Every commit that lands on `main` is verified-good — not merely green.** "Broken" is broader than
  a red build: a commit is defective if it builds red, **OR** the falsifier exposes it as *fake-green*
  (tests pass but assert nothing real / are tautological), **OR** the reviewer rejects it because it
  doesn't implement what it claims. None of those may land. Red states are never committed in the first
  place (TDD's red is a transient working-tree step; the commit comes after green, and `@Ignore`-first
  Gherkin keeps the build green). A defect found at the gateway is **repaired by rebase into the commit
  it belongs to, before the merge** — never left on `main`, never papered over with a follow-up "fix"
  commit.
- **Reword at the gateway when needed:** the agent has **no interactive TTY** (`git rebase -i` can't be
  driven by hand), but rewording still works **non-interactively** via scripted `GIT_SEQUENCE_EDITOR` /
  `GIT_EDITOR`. Unsatisfactory messages get cleaned up at the gateway before the fast-forward.
  **`git push` stays the author's alone.**
- **Parallel development / process benchmarking:** branches + **git worktrees** let independent work —
  or two competing implementations of the same task, for benchmarking — run without collision (the
  `Agent` tool can isolate a subagent in its own worktree).

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
[working-with-claude.md](working-with-claude.md)) checks **process adherence** (was it a real
red→green→refactor micro-step? whitespace kept separate? KB updated? commit-message rules met?) and
**code criteria** (clean code, SOLID, hexagonal boundaries). If something is off: send back with
concrete feedback, and **record the lesson in `docs/process-learnings.md`** so future work — and
future agents — drift toward the process, not away from it.
