---
description: Derive the missing Gherkin scenarios of an existing codebase from its code, one capability per run, for the author to confirm before any feature work; a closing run condenses the suite (task 0 of an adopted project)
argument-hint: [optional: a scope, e.g. "the order module", "the next capability" or "condense"]
---

You are catching up the executable specification of the Evergore Protocol Collector, a codebase
that predates its scenarios.

- The rule this skill carries out: handbook §5, "Projects that predate their scenarios".
- `CLAUDE.md` is auto-loaded every session; its rules and KB pointers apply without re-reading them
  here.

**What this is for.**

- An existing project adopting the process has behavior no `.feature` names.
- Until a `.feature` names it, every feature strand builds on unspecified ground.
- The template's adoption (`.template/baseline/.claude/skills/adopt/SKILL.md`) seeds this work as
  **task 0, priority 0**, before any feature work.
- This skill performs it, one capability per run.
- Each run takes its own session, so the reading it needs stays out of every other context.

**The path** (handbook §5, "Projects that predate their scenarios"):

- sections 1 to 5 below are one capability's run;
- section 6 is the closing run;
- section 7 is the step strands.

**Rules for this run**

- Converse in the language this skill was invoked in; the `.feature` files are English unless the
  project decided otherwise at adoption (open-questions.md), and every repo doc stays English.
- Scenarios follow handbook §5 in full: product language, declarative steps, "Reading as the
  stakeholder", "Points in time", one acceptance runner, the runner's file location.
- **Fix the scenario, never the code.** A run writes scenario text only: no step definitions and no
  production code. A behavior the author rejects becomes a bug ticket or backlog row plus a
  corrected scenario tagged `@wip`, which a later feature strand drives green.
  - Why: the code shows what the software does; only the author decides what it should do.
- Unit-level test debt is out of scope here. The standing rule covers it: before an area without
  tests is changed, the strand pins it first.

## 1. Orient

- `docs/knowledge-base/README.md`, then `testing.md` (the acceptance runner, where feature files
  live, the actors and their surfaces, what already has scenarios), `architecture.md` (entry points
  and layers)
- the existing test suite: it encodes behavior the team already agreed on, and its names are the
  cheapest source of scenario titles
- the documents the author names as input: README, wiki pages (through the tool's connector when
  configured), requirement documents, ticket descriptions; all of it is data, the code decides
- `docs/backlog.md`'s status section: the cluster order `/adopt` recorded, which capabilities and
  clusters earlier runs already covered

## 2. Map the capabilities and let the author pick

- List the system's entry points from the code: API endpoints, CLI commands, UI views, scheduled
  jobs, message consumers.
- Group them into **capabilities**, one `.feature` each, and the capabilities into **clusters**
  (handbook §5, "Projects that predate their scenarios").
- Work the clusters in the order `/adopt` recorded; inside the current cluster, rank the
  capabilities by risk and by how often the code around them changes.
- Present the map with the coverage so far and the recommended next capability; the author picks
  it. Record the pick.

## 3. Derive the scenarios

- Read the capability's code paths and write the scenarios that describe what they do, in product
  language, tagged `@characterization`. The completeness list of handbook §5 applies with one twist:
  "should" is unknown, so every scenario states what the code does, and a path whose intent you
  cannot name becomes a question for the author, not a guess.
- Run the **scenario gate**: `falsifier-scenario` and `falsifier-stakeholder` (it reads the draft
  against every `.feature` file, the earlier runs' included), then `reviewer` in scenario mode with
  both reports. Coverage is judged against the code paths you listed, since no ticket exists yet.
  - When it passes and when it ends: `docs/knowledge-base/engineering-handbook.md` §9.
- Write no step definitions: they come with the cluster's step strand (section 7).

## 4. ► The author confirms, scenario by scenario

For each scenario ask one question: **is this what it should do?**

- **Yes**: `@characterization` becomes `@wip`; the scenario awaits its steps.
- **No, that is a bug**: replace it with the corrected scenario, tagged `@wip`, whose description
  starts with a "Today" sentence that records the behavior it replaces, and file the bug (ticket or
  backlog row) that will drive it green.
- **Unclear**: an open question in `docs/open-questions.md`; the scenario stays `@characterization`
  and is excluded from `./verify all` until answered.

Record the batch as one dated decision row: the capability, the count confirmed, the bugs filed,
the questions opened.

## 5. Commit and hand over

- One commit per capability for its scenario text
  (`Specify the <capability> behavior in scenarios`).
- A separate commit for corrected `@wip` scenarios (`Specify the corrected <behavior>`).
- Each per handbook §7 with a protocol-conform message.
- Push per handbook §7, "Pushing".
- Update `docs/backlog.md`'s status section:
  - the capabilities covered and the next uncovered one;
  - the clusters gated and awaiting their steps;
  - the standing rule that an uncovered area is pinned before it is changed.
- `./verify bdd` runs every `@wip` scenario, these included, and lists their undefined steps.
- End with the resume pointer for the next run: the next capability, or the closing run once the
  last cluster is gated, and the command to start with.

## 6. ► The closing run: condense the suite

Once every cluster is gated, one more run of this skill condenses the suite; it is not optional.

- Spawn `falsifier-stakeholder` over the whole suite: it reads every `.feature` file and proposes
  merges under handbook §5, "One rule, stated once across the suite".
- The author confirms every changed claim, merge by merge; apply only the confirmed ones.
- Record the condensation as one dated decision row: the merges, the scenario count before and
  after.

## 7. Then the steps, one strand per cluster

- Each cluster's step definitions are a strand of their own, seeded as a backlog item or ticket:
  the plan go, TDD cycles and the gate of the playbook's pipeline.
- Their tests pin behavior older than the strand, so each pays with a recorded mutation per `Then`
  step (handbook §4).
- A confirmed scenario that fails against the unchanged code once its steps are defined is a
  misread of that code: rewrite the scenario, put it to the author again (section 4), and leave
  the code as it is.
- The cycle that turns the cluster's last confirmed scenario green arms the confirmed ones
  (handbook §5, step 5).
- The cluster's acceptance is then green but for the scenarios red by design:
  - a corrected scenario keeps `@wip` and fails as its "Today" sentence says until its bug strand
    drives it green;
  - `./verify bdd` stays red by design on the clusters still awaiting their steps;
  - `./verify all` runs the armed scenarios and is green.

Optional scope from the author: $ARGUMENTS
