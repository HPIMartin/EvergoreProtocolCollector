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
- Small methods, early returns, no deep nesting. No commented-out code in commits.
- **Avoid comments — make the code say it.** No comments in code, config, or infrastructure unless
  intent genuinely can't be expressed in names/structure (rare; then explain *why*, not *what*).
  Self-documenting names + small functions replace comments. The reviewer flags unnecessary comments.
- **No dead code & no undeclared dependencies** (e.g. an unused class importing a library absent from
  `pom.xml` — delete it).
- Prefer `record`s / immutability for value objects. Return `Optional` instead of sentinels
  (replace `getNewest()`'s `MIN_VALUE` object).
- Constants/config over magic values; **no secrets in source** (token, credentials → config).
- **Warnings are errors.** Compiler and lint warnings are treated as build failures (`-Xlint:all` +
  `failOnWarning`). The default is to **fix** them, not suppress; genuinely obsolete lint may be
  excluded deliberately (e.g. `-serial` for missing `serialVersionUID`, irrelevant on modern Java).
  When unsure whether a warning should be fixed or excluded, **ask the author**. The compiler flip
  itself rides with the Gradle migration (build config); until it lands the rule is upheld by author/agents.

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
- Message = **single line**, **starts with a present-tense verb**, briefly states what the commit
  actively changes. **No body. No `Co-Authored-By` or tool footer.** English.
  - ✅ `Add storage value calculation to data evaluator` · `Normalize line endings to LF` · `Remove dead CsvParser`
  - ❌ `feat: …` (no prefixes) · multi-line bodies · `Co-Authored-By:` lines
- **LF line endings** enforced via `.gitattributes` (`* text=auto eol=lf`; binaries marked).
  Whitespace/format changes go in their **own** commit, never mixed with logic.
- One logical change per commit. Keep `master` releasable; feature work on branches; CI
  (`mvn -B verify`) green to merge.
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

## 8. Definition of Done (checklist)

- [ ] Compiles, `mvn verify` green locally
- [ ] No compiler/lint warnings (build is warning-clean once `failOnWarning` lands)
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
