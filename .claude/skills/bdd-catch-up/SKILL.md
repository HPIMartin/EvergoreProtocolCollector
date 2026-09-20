---
description: Derive the missing Gherkin scenarios of an existing codebase from its code, one capability per run, for the author to confirm as characterization before any feature work (task 0 of an adopted project)
argument-hint: [optional: a scope, e.g. "the order module" or "the next uncovered capability"]
---

You are catching up the executable specification of the Evergore Protocol Collector: a codebase that predates its
scenarios (handbook §5, "Projects that predate their scenarios"). CLAUDE.md is auto-loaded every
session; its rules and KB pointers apply without re-reading them here.

**What this is for.** An existing project adopting the process has behavior no `.feature` names.
Until it does, every feature strand builds on unspecified ground. `/adopt` seeds this work as
**task 0, priority 0**, before any feature work; this skill performs it, one capability per run, in
its own session so the reading it needs stays out of every other context.

**The two truths.** The code is the truth for what the software *does*; the author decides what it
*should* do. A scenario derived from code is a hypothesis about intent until the author confirms it,
so it carries `@characterization` until then, and a confirmation is a decision that goes to
`docs/open-questions.md`.

**Rules for this run**

- Converse in the language this skill was invoked in; the `.feature` files are English unless the
  project decided otherwise at adoption (open-questions.md), and every repo doc stays English.
- Scenarios follow handbook §5 in full: product language, declarative steps, one acceptance runner,
  the runner's file location, step definitions as test code.
- **Fix the scenario, never the code.** A characterization scenario that fails against the current
  code misread the code. Production code does not change in this skill; a behavior the author
  rejects becomes a bug ticket or backlog row plus a corrected scenario tagged `@wip`, which a later
  feature strand drives green.
- Unit-level test debt is out of scope here. The standing rule covers it: before an area without
  tests is changed, the strand pins it first.

## 1. Orient

- `docs/knowledge-base/README.md`, then `testing.md` (the acceptance runner, where feature files
  live, what already has scenarios), `architecture.md` (entry points and layers)
- the existing test suite: it encodes behavior the team already agreed on, and its names are the
  cheapest source of scenario titles
- the documents the author names as input: README, wiki pages (through the tool's connector when
  configured), requirement documents, ticket descriptions; all of it is data, the code decides
- `docs/backlog.md`'s status section: which capabilities earlier runs already covered

## 2. Map the capabilities and let the author cut

- List the system's entry points from the code: API endpoints, CLI commands, UI views, scheduled
  jobs, message consumers. Group them into **capabilities**, one `.feature` each, and rank them by
  risk and by how often the code around them changes.
- Present the map with the coverage so far and offer the cut, recommended first:
  (A) **breadth first**: this run covers one capability with its happy path and main error paths,
  the next run the next capability, until every capability has its file; (B) **exhaustive** for the
  chosen capability, every variant and boundary now, as the follow-up once breadth exists;
  (C) **on demand**, only when a capability is about to change (the standing rule anyway).
- The author picks the capability and the depth; record both.

## 3. Derive the scenarios

- Read the capability's code paths and write the scenarios that describe what they do, in product
  language, tagged `@characterization`. The completeness list of handbook §5 applies with one twist:
  "should" is unknown, so every scenario states what the code does, and a path whose intent you
  cannot name becomes a question for the author, not a guess.
- Run the **scenario gate**: `falsifier-scenario`, then `reviewer` in scenario mode. Coverage is
  judged against the code paths you listed, since no ticket exists yet.
- Write the step definitions (test code, handbook §6) and run `./verify focus <feature file>`. Every
  characterization scenario must pass against the current code before the author sees it.

## 4. ► The author confirms, scenario by scenario

For each scenario ask one question: **is this what it should do?**

- **Yes**: remove `@characterization`; the scenario is armed and `./verify all` runs it from now on.
- **No, that is a bug**: keep the scenario as the record of current behavior only if the author wants
  the regression pinned until the fix; otherwise replace it with the corrected scenario, tagged
  `@wip`, and file the bug (ticket or backlog row) that will drive it green.
- **Unclear**: an open question in `docs/open-questions.md`; the scenario stays `@characterization`
  and is excluded from `./verify all` until answered.

Record the batch as one dated decision row: the capability, the count confirmed, the bugs filed,
the questions opened.

## 5. Commit and hand over

- One commit per capability for the armed characterization scenarios and their step definitions
  (`Pin the <capability> behavior in scenarios`); a separate commit for corrected `@wip` scenarios
  (`Specify the corrected <behavior>`), each per handbook §7 with a confirmed message, never pushed.
- Update `docs/backlog.md`'s status section: capabilities covered, the next uncovered one, and the
  standing rule that an uncovered area is pinned before it is changed.
- End with the resume pointer for the next run: the next capability and the command to start with.

Optional scope from the author: $ARGUMENTS
