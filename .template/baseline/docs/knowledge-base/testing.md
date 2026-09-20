# 05: Testing

> **ADAPT:** this doc is a skeleton: fill in your test inventory and coverage map, verified against
> the actual test sources. The rules under "What keeps the suite trustworthy" are reusable process
> knowledge; keep them. The strategy itself (pyramid, arrange discipline, no buried logic) lives in
> [engineering-handbook.md](engineering-handbook.md) §6; this doc holds the project's concrete facts.

## Inventory

> **ADAPT:** one row per test class: what it covers (terse but concrete: the behaviors asserted,
> the fakes/stubs used), and its style. Include non-test helpers and fixture generators; a new
> session should learn the suite from this table without opening every file.

| Test | Scope | Style |
|------|-------|-------|
| | | |

## What keeps the suite trustworthy

- **Deterministic, never wall-clock-dependent.** A test passes on any hardware; waiting on an async
  event means waiting on a real completion signal (a latch awaited with **no timeout**, released on
  both the success and the failure path so a bad run fails an assertion instead of hanging). A
  timeout or retry is a mitigation, not a fix (handbook §6).
- **No shared static state.** When tests need state written by the application at startup and read
  back in the test body, observe it through an injected, DI-shared recorder object, never static
  flags. Static state bleeds across contexts and produces order-dependent failures (handbook §3/§6).

  > **ADAPT:** name your recorder/seam classes here once they exist, so agents reuse them instead
  > of reinventing the bridge.
- **Robust assertions.** Assert on structure behind a helper (e.g. parse rendered output into
  header + rows), never on raw markup or fuzzy string similarity; all format coupling lives in that
  one helper, so restyling never breaks assertions (handbook §6).
- **Synthetic committed fixtures, zero PII.** Committed test data is generated, never a production
  snapshot; a fixture generator keeps it reproducible and derives values from the production
  catalog/rules rather than inventing them. Real-data snapshots stay gitignored.

  > **ADAPT:** name your fixture generator and the committed fixture files, and how to regenerate
  > them.

## Coverage map

> **ADAPT:** two lists, kept current: "Has tests" (the behaviors genuinely covered, including
> indirect coverage via smoke/acceptance tests) and "Most important UNTESTED logic" (ranked; this
> list drives the next test work).

## Testing direction (TDD/BDD)

New behavior follows the TDD cycle (handbook §4); use cases are captured as BDD scenarios from the
product-owner perspective (handbook §5).

> **ADAPT:** your BDD scenario style (plain given/when/then tests vs. a framework) and one example
> scenario in domain language, marked "(example)".
