---
description: Re-derive the plan: find what has overtaken the backlog since the last ordering decision, then propose the next cut
argument-hint: [optional: a scope, e.g. "only the valuation items" or "what must land before the release"]
---

You are re-deriving the plan for the Evergore Protocol Collector. CLAUDE.md is auto-loaded every
session; its rules and KB pointers apply without re-reading them here.

**What this is for.** `/continue` executes the next action the docs already name. This skill asks the
question in front of that one: **is that next action still right?** Use it when events may have
overtaken the plan, which is the normal case after a feature lands, a decision changes what earlier
items mean, or a release approaches.

**Rules for this run**

- **Read-only until the author gives a go.** Produce an analysis and a proposal; change no doc before
  the author has decided. The analysis is worthless if it also commits its own conclusions.
- **Never re-scan the codebase**, and read docs section-scoped, not whole
  (working-with-ai-agents.md, "Context & token hygiene"). Verify a specific fact in code when a
  finding turns on it, and only then.
- **Name every item in words**; a shortcode may follow once, in parentheses. An analysis written in
  bare shortcodes is unreadable to the person who has to decide on it.

## 1. Orient

- `docs/knowledge-base/README.md` (the map; then only the KB docs your findings actually touch)
- `docs/backlog.md`, the "▶ Current status / next action" section
- `docs/open-questions.md`, newest decisions first, plus the newest rows of `docs/process-learnings.md`
- `docs/roadmap.md` and `docs/risks.md`

## 2. Build the difference list

This is the deliverable. It is **not** a summary of the backlog, which the author can read. Seven
checks, each of which has found something real in practice:

1. **Items younger than the plan.** Take the date of the last ordering decision in
   `open-questions.md`. Every backlog row filed after it that sits in no milestone is work the plan
   does not know about.
2. **Work without a row.** Prose bullets inside the backlog, plan or design documents under `docs/`
   that live outside it, and anything the status section describes that no row owns. Work without a
   row is invisible to every planning step, including this one.
3. **Acceptance that can no longer be met.** For each unticked checkbox and each acceptance column
   you are weighing, ask whether a later decision made it unachievable or meaningless as written. A
   check nobody can perform is worse than no check, because it silently blocks its milestone.
4. **Unlanded commits.** `git worktree list`, then `git rev-list --count main..<branch>` per branch,
   and read whatever is ahead. A leftover strand can hold knowledge or work that never landed.
5. **Recurrences.** Look for the same failure twice in the newest process-learnings rows. A
   recurrence is the argument for giving a mechanical fix a real slot instead of writing the rule
   down a third time.
6. **Questions a recent change made load-bearing.** An open question that was harmless becomes
   urgent once a shipped feature depends on its answer.
7. **Ordering traps.** Find items whose cost is paid twice in the wrong order: anything that
   documents, explains or publishes a number against anything that still changes that number. Name
   the dependency, not just the item.

## 3. Deliver the analysis as a page

Publish it with the `Artifact` tool (load the `artifact-design` skill first). A difference list
carrying measured figures is overview-able as a page and tiring as terminal scrollback, and the
author has said which they prefer. Keep the terminal reply to the headline finding, the
recommendation, and the decisions you need.

## 4. Recommend, do not survey

Propose **one** order, saying why each position is where it is. Where a call is genuinely the
author's, offer options recommended-first and named in words
(working-with-ai-agents.md, "How to ask questions"). State plainly what it costs to decide the other
way, with figures where you measured them.

## 5. After the go, and only then

- Record each decision in `docs/open-questions.md` as its own dated row (decision, why, alternatives
  offered). **One row per line**; never append onto the previous row's line, which has silently
  swallowed five decisions before.
- Apply the consequences: the backlog's rows and status, the roadmap's milestones, and every inbound
  pointer to a heading you renumber (DOC-10). Completed work leaves the docs; git is the history.
- Commit per handbook §7: one confirmed single-line message per cohesive change, no body, no footer,
  never push.

Optional scope from the author: $ARGUMENTS

Start by orienting, then report the difference list. Do not propose an order before you have run all
seven checks: the finding that changes the plan is usually the one nobody was looking for.
