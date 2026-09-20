# Open Questions & Decisions Log

A living record of decisions and the questions that shape the plan. When a question is answered,
move it to **Decisions** with the date and rationale. Agents: read this before changing scope.
Entries stay terse: decision + why + where it is codified. The KB doc is the canonical home of each
rule; this table is the dated index, not a second copy.

## Decisions (answered)

| Date | Decision | Rationale |
|------|----------|-----------|
| YYYY-MM-DD | *(example)* **Conversation language:** the author's language for chat; code, comments, identifiers and docs stay English | Author preference → CLAUDE.md |

> **ADAPT:** Replace the example row with your own decisions as they are made. One row per decision: date, what was decided, why, and where it is codified (which KB doc).

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
