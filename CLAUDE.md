# CLAUDE.md — project instructions for AI sessions

> This file is auto-loaded by Claude Code at the start of every session. Keep it short and
> point to the knowledge base for detail. **Conversation may be in German; code, comments,
> identifiers, and docs are always English.**

## What this is

A Java 17 / Micronaut service that scrapes the browser game **Evergore** for a guild's bank +
storage transaction logs, values item movements, and computes each member's contribution —
**automating a manual Google Sheet**. It is also, deliberately, a **showcase of clean,
test-driven, AI-assisted development**.

## Start here (every session)

1. **Read the knowledge base first — do not re-scan the codebase blindly:**
   [`docs/knowledge-base/README.md`](docs/knowledge-base/README.md).
2. Check the backlog [`docs/backlog.md`](docs/backlog.md) and open decisions
   [`docs/open-questions.md`](docs/open-questions.md).
3. Follow the engineering standards in
   [`docs/knowledge-base/engineering-handbook.md`](docs/knowledge-base/engineering-handbook.md)
   and the AI workflow in
   [`docs/knowledge-base/working-with-claude.md`](docs/knowledge-base/working-with-claude.md).
4. For implementation, run the agent team in
   [`docs/knowledge-base/multi-agent-playbook.md`](docs/knowledge-base/multi-agent-playbook.md)
   (Planner = you+me → `implementer` → `falsifier` → `reviewer`; defined in `.claude/agents/`).

## Golden rules

- **KB-first, KB-current:** when you change behavior, update the relevant KB doc *in the same change*.
- **Git is history; docs are knowledge:** never duplicate git in docs (no changelog, no
  commit-history narration, no diff snapshots) — answer history questions with `git log`/`git diff`.
- **TDD:** new logic starts with a failing test. The `domain` + `businessLogic` packages stay
  framework-free and fast to unit-test.
- **BDD (PO view):** user-facing capabilities get plain given/when/then scenarios in product language.
- **Hexagonal:** depend on ports, not adapters. Micronaut / Selenium / ORMLite live only at the edges.
- **Ask, don't guess:** when a decision is the author's to make, ask with **multiple-choice options**
  (recommended option first). Record the outcome in `open-questions.md`.
- **Commit protocol (strict):** see the section below — propose **one** commit message, get the
  author's confirmation, then commit. **Never `git push`.**

## TDD / BDD cycle (every change)

1. **Red** — write the smallest failing test.
2. **Green** — minimal code to make it pass.
3. **Refactor** — clean up; tests stay green.
4. **Commit** — one cycle ≈ one commit (see protocol). A reviewer may gate the commit.

BDD here is **for documentation / the showcase template only** — there is no runtime BDD framework
in this project. Flow for a user-facing capability: write the **Gherkin scenarios first,
`@Ignore`-disabled → commit**; then un-ignore and drive them green with the TDD cycle above → commit.

## Commit protocol (strict — author's rules)

1. **Propose exactly one commit message and wait for confirmation; only then commit. Never `git push`.**
2. Message = **one line**, **starts with a present-tense verb**, briefly states what the commit
   actively changes. **No body, no `Co-Authored-By` / tool footer.** English.
   - e.g. `Normalize line endings to LF` · `Add storage value calculation to data evaluator` · `Remove dead CsvParser`
3. One logical change per commit; keep whitespace/format churn out of feature commits.

## Definition of Done

Compiles · unit-tested (and `@Ignore`-first Gherkin if user-facing) · relevant KB doc updated ·
no new hard-coded secrets · commit message proposed & confirmed (not pushed) · CI green.

## Environment notes

- **In-container only:** all builds/tests/run happen in the **devcontainer** or via Docker — **never
  install or run toolchains (JDK/Maven/Firefox) natively on the host.** This applies to you **and every
  spawned agent.** See [`docs/knowledge-base/dev-environment.md`](docs/knowledge-base/dev-environment.md).
- Branch in progress: **`Rebuild`**. Domain language is German (see the glossary).
- **Bash tool quirk (host only):** a Claude session started on the Windows host may not surface Bash
  stdout — redirect to a file (`cmd > out.txt 2>&1`) and `Read` it. Inside the devcontainer, Bash is normal.
- Build & run: see [`docs/knowledge-base/build-run-deploy.md`](docs/knowledge-base/build-run-deploy.md).
