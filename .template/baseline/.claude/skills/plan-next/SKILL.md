---
description: Re-derive the plan: find what has overtaken the backlog since the last ordering decision, then propose the next cut
argument-hint: [optional: a scope, e.g. "only the valuation items" or "what must land before the release"]
---

You are re-deriving the plan for {{PROJECT_NAME}}. CLAUDE.md is auto-loaded every session; its rules
and KB pointers apply without re-reading them here.

**What this is for.** `/continue` executes the next action the docs already name. This skill asks the
question in front of that one: **is that next action still right?** Use it when events may have
overtaken the plan, which is the normal case after a feature lands, after a decision changes what
earlier items mean, or when a release approaches.

**Rules for this run**

- **Read-only until the author gives a go.** Produce an analysis and a proposal; change no doc and no
  ticket before the author has decided. The analysis is worthless if it also commits its own
  conclusions.
- **Never re-scan the codebase**, and read docs section-scoped, not whole
  (working-with-ai-agents.md, "Context & token hygiene"). Verify a specific fact in code when a
  finding turns on it, and only then.
- **Name every item in words**; a shortcode or ticket key may follow once, in parentheses. An
  analysis written in bare identifiers is unreadable to the person who has to decide on it.
- **Write the plan in the language this skill was invoked in.** A German request produces a German
  page, prompts included, an English one an English page. This is not cosmetic: a prompt on the page
  is what starts the next session, so an English prompt silently switches that session's language.
  Two things stay unaffected either way: the **repository's documentation remains English** under
  CLAUDE.md's language rule, so the decision rows, backlog rows and KB edits you write after the go
  are English even when the page is not; and **technical material keeps its own spelling** inside
  the page (file paths, identifiers, command names, the domain's own terms). The page is a
  deliverable to the author, not a repo doc, so nothing should later "correct" it into English.

## 1. Orient

- `docs/knowledge-base/README.md` (the map; then only the KB docs your findings actually touch)
- `docs/backlog.md`, the "▶ Current status / next action" section; with a ticket system, the open
  items of the ticket project (through the tool's connector when one is configured, otherwise ask
  the author for the export)
- `docs/open-questions.md`, newest decisions first, plus the newest rows of `docs/process-learnings.md`
- `docs/roadmap.md` and `docs/risks.md` where the project keeps them
- the `.feature` files: they are the executable specification (handbook §5)

## 2. Build the difference list

This is the deliverable. It is **not** a summary of the backlog, which the author can read. Eight
checks, each of which has found something real in practice:

1. **Items younger than the plan.** Take the date of the last ordering decision in
   `open-questions.md`. Every item filed after it that sits in no milestone is work the plan does
   not know about.
2. **Work without a row or ticket.** Prose bullets inside the backlog, plan or design documents
   under `docs/` that live outside the backlog, and anything the status section describes that no
   row or ticket owns. Work without an owner is invisible to every planning step, including this one.
3. **Acceptance that can no longer be met.** For each unticked checkbox and each acceptance you are
   weighing, ask whether a later decision made it unachievable or meaningless as written. A check
   nobody can perform is worse than no check, because it silently blocks its milestone.
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
8. **Specification drift.** A `.feature` that promises behavior no item owns, and an item that
   promises behavior no `.feature` covers; with executable scenarios the specification and the plan
   can drift apart in both directions.

## 3. Deliver it as a page, and make the page runnable

Publish it as a page where the harness offers one (in Claude Code the `Artifact` tool, with the
`artifact-design` skill loaded first); elsewhere write it as a Markdown file the author opens, with
the same two halves. A difference list carrying measured figures is overview-able as a page and
tiring as terminal scrollback, and the author starts the work from that page on whatever machine is
to hand. So the page carries **both** halves, and an analysis without the second half is not a
finished result:

**The plan.** The difference list with its measured figures, the proposed order, the decisions
taken, and the questions still open.

**The execution.** One block per acutely next step, each carrying:

- A **prompt that stands on its own** in a fresh session on another machine: what to read first to
  orient, the acceptance (the `.feature` where one exists), the measured figures, and the trap that
  will otherwise be walked into. Hand it the capability, never a bare identifier. Give it a copy
  button, which is what makes the page a launcher rather than a report.
- The **model** to start that session on, plus the gate's tier where the change escalates (the
  project's high-risk areas, multi-agent-playbook.md).
- The **reasoning effort** to set, and separately the size estimate, so the author can plan time
  instead of inferring it.

**The lanes.** Which steps run at the same time, grouped by the code they touch, and every
constraint that survives the parallelism: one worktree per lane and never two builds in one tree;
landings stay author-serialized however much runs in parallel; a step that changes a gate every
other commit passes through lands before the other lanes start committing or after they land, never
between; and the shared doc files the second lane to land has to rebase onto.

Keep the terminal reply to the headline finding, the recommendation, and the decisions you need.

## 4. Recommend, do not survey

Propose **one** order, saying why each position is where it is. Where a call is genuinely the
author's, offer options recommended-first and named in words (working-with-ai-agents.md, "How to
ask questions"). State plainly what it costs to decide the other way, with figures where you
measured them.

## 5. After the go, and only then

- Record each decision in `docs/open-questions.md` as its own dated row (decision, why, alternatives
  offered). **One row per line**; never append onto the previous row's line (DOC-11).
- Apply the consequences: the backlog's rows and status or the tickets, the roadmap's milestones, and
  every inbound pointer to a heading you renumber (DOC-10). Completed work leaves the docs; git is
  the history.
- Commit per handbook §7: one confirmed single-line message per cohesive change, no body, no footer,
  never push.

Optional scope from the author: $ARGUMENTS

Start by orienting, then report the difference list. Do not propose an order before you have run all
eight checks: the finding that changes the plan is usually the one nobody was looking for.
