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
- **No logic in constructors; field assignment only** (no IO, no `init()`/`ensureTable()`-style
  calls); construct + initialize via static **factory method** (the `Repository` subclass
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
- **No comments** in code, config, or infrastructure unless intent can't live in names/structure
  (rare; then *why*, not *what*).
- **Javadoc is an absolute no-go** (author decision 2026-08-04) and counts as a comment, not as
  documentation: no `/** */` on classes, methods, records or fields. Non-obvious knowledge goes into
  an expressive name, a **test name**, the KB, or `open-questions.md`. Reviewers flag every doc
  comment and every explanatory comment the diff adds, not only the "unnecessary" ones.
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

1. **Red**: smallest failing test expressing the next behavior.
2. **Green**: simplest code that passes.
3. **Refactor**: improve design with tests green.
4. **Commit**: one micro-cycle ≈ one commit, via the §7 protocol (propose message → confirm →
   commit; never push).

- Domain/application logic is unit-tested **without the framework** (plain JUnit + AssertJ, fast).
- One behavior per test; arrange/act/assert; table-driven where inputs vary (parser, item values).
- Bug fix ⇒ reproducing test first, then fix (e.g. the double `getAllDifferentAvatars`).
- A reviewer (human or agent) may gate each commit on process + criteria adherence (§9).

## 5. BDD (product-owner perspective): documentation / showcase only

No runtime BDD need; BDD exists as documentation and reusable template. Scenarios in **product
language**, given/when/then:

> *Given* member "Bambor" deposited 58 410 gold and crafted goods worth 169 254,
> *when* the officer opens the overview,
> *then* Bambor's "erzeugter Gildenmehrwert" shows 1 170.

**Authoring flow (author's rule):**
1. Write the Gherkin scenario(s) blind/up-front, disabled with `@Ignore` → **commit** (captures
   intent before implementation).
2. Re-activate, implement green via the §4 TDD cycle → **commit** when green.

- Scenarios run as acceptance tests at the application boundary, through ports with **in-memory
  fakes** (fake `PageSource`, `:memory:` SQLite): no browser, no live site.
- Tooling default: plain JUnit given/when/then helpers; Cucumber `.feature` files only for
  non-developer-readable living docs (open question D-9, backlog G4).

## 6. Test strategy (pyramid)

- **Many** fast unit tests (domain values, parser, evaluator math).
- **Some** adapter/integration tests (repositories vs `:memory:` SQLite).
- **Few** acceptance tests (collect→evaluate→overview via fakes) and a thin smoke test for wiring.
- Replace brittle assertions (the Levenshtein HTML matching) once output is behind a clean port.
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

## 7. Git & commits (author's rules, strict)

- **Propose exactly one commit message, get the author's confirmation, THEN commit. Never `git push`.**
- Message = **single line**, **present-tense verb first** (after an optional `[doc]` tag), states
  what the commit actively changes. No body, no `Co-Authored-By` or tool footer. English.
  - ✅ `Add storage value calculation to data evaluator` · `Remove dead CsvParser` · `[doc] Groom the backlog current-status section`
  - ❌ `feat:`/`fix:`/other conventional-commit prefixes · multi-line bodies · `Co-Authored-By:` lines
- **`[doc]` prefix** for pure documentation/process/meta commits (only `docs/`, `.claude/`, or
  top-level `*.md`; no product-code/build change); code & build commits stay bare. `[doc]` is the
  only permitted prefix (keeps `git log` two-tier and scannable).
- Docs describing a code change ride in that commit, never a standalone `Refresh/Update docs after
  X` follow-up (can't stand alone for revert/cherry-pick).
- **LF line endings** via `.gitattributes` (`* text=auto eol=lf`; binaries marked).
  Whitespace/format changes in their **own** commit, never mixed with logic.
- One logical change per commit; cohesion test: group parts only if reverting/cherry-picking one
  alone would break the build or leave the tree inconsistent. Keep `main` releasable; local
  `./gradlew build` green before committing.
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
  checkout. `commit-msg` enforces the message rules above; `pre-commit` runs the fast format gate
  (`spotlessCheck` + `checkstyleMain`/`checkstyleTest`) and a staged-content secrets/host-data
  scan. Tests and full build are deliberately excluded (keeps the TDD micro-commit loop fast;
  commits are already green). Safety net, not substitute; `--no-verify` only for genuine
  emergencies. Details: [build-run-deploy.md](build-run-deploy.md).
- **`[wip]` parking commits (pause only)**: `/pause` may autonomously park uncommitted work as ONE
  local commit `[wip] <one-line state>` via `git commit --no-verify` (WIP legitimately fails the
  hooks; the one sanctioned non-emergency bypass; no-secrets rule unchanged, eyeball the diff
  first). A checkpoint, not history: `/continue` resolves it first (finish or rework, replace via
  the normal confirmed-message protocol; reset-free rewrite recipe → multi-agent playbook). Never
  survives to the review gateway or a push; parked on `main`, resolved before any new work.

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
  into worktrees; a second **foreground** session must `EnterWorktree` before editing.)
- **On the branch**: the implementer runs the full TDD loop and commits each red→green→refactor
  step itself, protocol-conform messages, no per-commit pre-approval.
- **The review gateway (per feature, serialized):**
  1. Everything committed in the worktree.
  2. Rebase onto current local `main`, resolving conflicts there.
  3. Review the rebased tip: with rebase + fast-forward it is byte-identical to what `main`
     becomes, so reviewing it *is* reviewing the final state, one feature, no merge artifacts
     (falsifier + reviewer gate here). Always state the full worktree name (branch and absolute
     path) so the author can open it in the editor and review all changes before the merge.
  4. On PASS, fast-forward `main` from the primary checkout: `git merge --ff-only <branch>`
     (precondition: primary checkout clean, else stop and surface it).
  5. Remove the worktree.
- **Merge strategy (hard rule): rebase + fast-forward only.** No merge commits, no squashing (rare
  exceptions): history stays linear and keeps the small commits.
- **Landings are author-serialized**: one feature at a time in rebase→review→fast-forward; several
  ready ⇒ the author picks the order. `git merge --ff-only` is the hard guard: a branch not rebased
  onto latest `main` is refused (re-rebase, re-review the delta). No automatic cross-session
  coordination (heterogeneous tools can't discover each other); the author is the single
  serialization point.
- **Every commit landing on `main` is verified-good, not merely green.** Defective = builds red,
  OR falsifier-exposed fake-green (tests pass, assert nothing real), OR reviewer-rejected as not
  implementing what it claims; none may land. Red states are never committed (TDD red is transient;
  `@Ignore`-first Gherkin keeps the build green). A gateway defect is repaired by rebase into the
  commit it belongs to, before the merge: never left on `main`, never a follow-up "fix" commit.
- **Reword at the gateway** non-interactively via scripted `GIT_SEQUENCE_EDITOR` / `GIT_EDITOR`
  (no interactive TTY). `git push` stays the author's alone (deny in `.claude/settings.json`).
- **Tool-neutral**: plain git (worktree · branch · rebase · `--ff-only`). Claude Code uses
  `EnterWorktree`/`ExitWorktree` and `Agent` worktree-isolation; other tools/humans use
  `git worktree` directly.

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

Before each commit, a reviewer (human or review agent, see
[working-with-ai-agents.md](working-with-ai-agents.md)) checks **process adherence** (real
red→green→refactor micro-step? whitespace separate? KB updated? commit-message rules?) and **code
criteria** (clean code, SOLID, hexagonal boundaries). If off: send back with concrete feedback and
record the lesson in `docs/process-learnings.md` (so future work and agents drift toward the
process, not away).

**Bounded review loop → re-plan (governance).** A rejection sends work back to re-implement; a
fresh falsifier and reviewer re-check. Bounds:

- **Cap: two re-implement rounds (three attempts total), then re-plan**: stop, escalate to the
  author with findings (repeated FAILs usually mean the plan/spec is wrong, not the code).
  **Early-escalate** when two consecutive rounds raise the *same* finding.
- **Process-only FAILs** (whitespace not separated, KB not updated, commit-message format) are
  cheap mechanical fixes; they don't consume a round.
- Fixes are folded into the commit they belong to (§7), never a "fix review" commit.

Governance is tool-neutral; agent-team mechanics (who runs falsify vs. review, harness-specific
history-rewrite recipes) live in [multi-agent-playbook.md](multi-agent-playbook.md).
