# Open Questions & Decisions Log

A living record of decisions and the questions that shape the plan.

- Agents: read this before changing scope.
- An answered question moves to **Decisions**: one dated row per decision.
- A row stays terse; the KB doc is the canonical home of each rule, and this table is the dated
  index, not a second copy.
- The decision cell: the decision in bold, then its origin in parentheses.
  - The origin: "author decision", or who else decided.
  - Once a later row changes it: `author decision; amended by the <date> row on <topic>`.
- The rationale cell: the why, then "Alternatives offered:" with the options not taken, then `→` and
  where the decision is codified.

## Decisions (answered)

| Date | Decision | Rationale |
|------|----------|-----------|
| YYYY-MM-DD | *(example)* **Conversation language: the author's language for chat; code, comments, identifiers and docs stay English** (author decision) | Author preference. Alternative offered: English for chat too → CLAUDE.md |

> **ADAPT:** Replace the example row with your own decisions as they are made, in the row shape
> above.

## Recorded mutations (red evidence where a new case could not start red)

A test adapted to a changed behavior, or a deliverable without a test harness, cannot be seen red on
its own. The substitute is a temporary mutation of the code under test, recorded here so a later gate
re-runs it instead of taking the claim on trust (handbook §4).

| What the cases pin | Mutation | Cases that flip to FAIL |
|---|---|---|

## Open questions

> **ADAPT:** Questions awaiting the author's call. Present each as multiple choice with a recommended option first ("Ask, don't guess", see [CLAUDE.md](../CLAUDE.md)). Move answered ones to Decisions.

## Assumptions currently baked into the plan (challenge if wrong)

> **ADAPT:** List the assumptions the current plan rests on, so agents can challenge them instead of silently inheriting them.
