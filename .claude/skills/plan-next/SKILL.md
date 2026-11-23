---
description: Re-derive the plan: find what has overtaken the backlog since the last ordering decision, then propose the next cut
argument-hint: [optional: a scope, e.g. "only the valuation items"; "harvest" trims the backlog]
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
- **Write the plan in the language this skill was invoked in.** A German request produces a German
  page, prompts included, and an English one an English page. This is not cosmetic: a prompt on the
  page is what starts the next session, so an English prompt silently switches that session's
  language, whatever the author was working in. Two things stay unaffected either way. The
  **repository's documentation remains English** under CLAUDE.md's language rule (conversation in
  any language; code, comments, identifiers and docs always English), so the decision rows, backlog
  rows and KB edits you write after the go are English even when the page is not; and **technical
  material keeps its own spelling** inside the page, meaning file paths, identifiers, command names
  and the German domain terms. The page is a deliverable to the author, not a repo doc, so nothing
  should later "correct" it into English.

## 1. Orient

- sync the primary checkout's `main` with `sh hooks/sync-main`, as `/continue` does, so the checks
  below see what landed and what other machines pushed
  - a non-zero exit is reported with its lines (at a stop they name both SHAs) and resolved by
    nothing
- `docs/knowledge-base/README.md` (the map; then only the KB docs your findings actually touch)
- `docs/backlog.md`, the "▶ Current status / next action" section
  <!-- MODULE agent-seat -->
  - on the agent seat (`AGENT_SEAT` reads `unattended`) also every row marked `(seat: agent)`
    (KB README, DOC-5), which the page lists as strands this seat can pick up from `main`
  <!-- /MODULE agent-seat -->
- `docs/open-questions.md`, newest decisions first, plus the newest rows of `docs/process-learnings.md`
- `docs/roadmap.md` and `docs/risks.md`
- the `.feature` files under `src/test/resources/features/`: they are the executable specification
  (handbook §5)
- the backlog's row count, which the page reports with its growth:
  - the rows of the backlog's item tables;
  - growth against the "after" count of the newest harvest decision row (section 6), else against
    the count at the commit of the last ordering decision.
- the docs' size, which the page reports beside its thresholds:
  - the session-start read set, in all:
    `wc -lc CLAUDE.md docs/knowledge-base/README.md docs/backlog.md docs/open-questions.md
    docs/process-learnings.md`;
  - each KB doc and the KB in all: `wc -lc docs/knowledge-base/*.md`;
  - the growth of the read set and of the KB in all since the last ordering decision: `<rev>` is
    the commit that added its row
    (`git log -1 --format=%h -S'<its bold text>' -- docs/open-questions.md`), and
    `git show <rev>:<path> | wc -c` gives a file's size there;
  - each figure over its threshold is a finding of the difference list, named with its doc.
- the thresholds, in bytes:
  - the read set: 200,000;
    - why: it keeps a session's first reads to about a quarter of a context window of 200,000
      tokens, at some four bytes a token;
  - one KB doc: 120,000;
  - the growth of the read set or of the KB in all since the last ordering decision: 5 per cent.

## 2. Build the difference list

This is the deliverable. It is **not** a summary of the backlog, which the author can read. Eight
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
4. **Unlanded commits and obsolete pull requests.** `git worktree list`,
   `git branch --no-merged main` (a parked branch no worktree holds) and
   `git branch -r --list 'origin/claude/*'` (each name checked as handbook §7 says), then
   `git rev-list --count refs/heads/main..<branch>` per branch (`refs/remotes/origin/main..` for a
   pushed one), and read whatever is ahead. A leftover strand can hold knowledge or work that never
   landed. Then list the open pull requests and name each one whose change `main` already carries.
   <!-- MODULE agent-seat -->
   - On the agent seat (`AGENT_SEAT` reads `unattended`) the session lists them with
     `<project>/agent/gh pr list`, `<project>` the primary checkout's absolute path (the
     `epc-agent` App, dev-environment.md, "Where agents run").
     - It closes a dependency bot's pull request whose change `main` already carries, with
       `<project>/agent/gh pr close <number>` (author decision 2026-11-23).
     - It names every other open pull request for the author.
   - On the PC the author lists them, and the session names each one whose change `main` already
     carries, for the author to close.
   <!-- /MODULE agent-seat -->
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

Publish with the `Artifact` tool (load the `artifact-design` skill first). A difference list
carrying measured figures is overview-able as a page and tiring as terminal scrollback, and the
author starts the work from that page on whatever machine is to hand. So the page carries **both**
halves, and an analysis without the second half is not a finished result:

**The plan.** The difference list with its measured figures, the proposed order, the decisions
taken, and the questions still open.

**The execution.** One block per acutely next step, each carrying:

- A **prompt that stands on its own** in a fresh session on another machine: what to read first to
  orient, the acceptance (the `.feature` where one exists), the measured figures, and the trap that
  will otherwise be walked into.
  Hand it the capability, never the backlog ID. Give it a copy button, which is what makes the page
  a launcher rather than a report.
- The **model** to start that session on, plus the gate line: the lenses and their tier, the tier
  escalating where the change touches valuation math, time and timezones, concurrency, data
  migration, or the auth and security surface (multi-agent-playbook.md, "Cadence").
- The **reasoning effort** to set, and separately the size estimate, so the author can plan time
  instead of inferring it.
<!-- MODULE agent-seat -->
- **Where it runs**, placed by dev-environment.md, "Where agents run".
- A block on the PC says why it runs there.
<!-- /MODULE agent-seat -->

**The start plan.** What the author opens when, as **waves**, not as a lane grid. A grid of lanes
by ordinal columns ("first / then / later") failed in practice: the columns read as global phases
while meaning per-lane order, two parallel sessions sat in one row as if sequential, and the
session blocks gave start conditions the grid contradicted.

- **Open with "start now".** The section's first line names the sessions to open right now, all
  at once, and how many they are.
- **Every later wave is headed by its trigger**, an event the author can observe ("once A has
  landed", "once Z1 has landed", "after your go on the doc cut"), never an ordinal. It lists the
  sessions started together when that event happens. Each session sits in exactly one wave.
- **Two facts per session, kept separate:** *starts* (its wave's trigger) and, where one exists,
  *must land before* (e.g. "before any lane's first feature gate"). Parallel start is not the
  same thing as landing order, and mixing the two causes the confusion.
- **One wording, everywhere.** The session block repeats its wave's trigger and landing
  constraint verbatim. A block that says "now, parallel to A" while the plan files it under "then"
  is a defect of the page.
- **Lanes stay as the grouping inside a wave**: the code a chain of sessions touches, where each
  next session's trigger is its predecessor's landing. Any visual keeps the invariant: *same row
  means one after the other, different rows mean at the same time*, and a dependency across rows
  is drawn or labelled, never left implicit.
- **The author's own steps are triggers, not a lane**: they head the wave they unblock.

Then every constraint that survives the parallelism: one worktree per lane and never two Gradle
runs in one tree; landings stay author-serialized however much runs in parallel; a step that
changes a gate every other commit passes through lands before the other lanes start committing or
after they land, never between; and the shared doc files the second lane to land has to rebase
onto.

Keep the terminal reply to the headline finding, the recommendation, and the decisions you need.

## 4. Recommend, do not survey

Propose **one** order, saying why each position is where it is. Where a call is genuinely the
author's, offer options recommended-first and named in words
(working-with-ai-agents.md, "How to ask questions"). State plainly what it costs to decide the other
way, with figures where you measured them.

## 5. After the go, and only then

- Record each decision in `docs/open-questions.md` as its own dated row (decision, why, alternatives
  offered). **One row per line**; never append onto the previous row's line, which has silently
  swallowed five decisions before (DOC-11).
- Apply the consequences: the backlog's rows and status, the roadmap's milestones, and every inbound
  pointer to a heading you renumber (DOC-10). Completed work leaves the docs; git is the history.
- Commit per handbook §7 on a `claude/<topic>` branch in its own worktree, never on `main`: one
  single-line message per cohesive change, worded by the agent, no body, no footer; on the agent
  seat the branch is pushed through `agent/push`, on the PC it stays local (handbook §7,
  "Pushing").

## 6. Harvest mode (only on the author's word)

- The author starts it with the argument `harvest`, optionally followed by a scope.
- It trims the backlog instead of ordering it: the per-row proposal below takes the place of the
  difference list and the order (sections 2 and 4).
- Orient as in section 1, then read every row of the backlog's item tables in scope.
- Propose one result per row, with its reason, from five classes:
  - **close** by decision: the row moves to "Considered and rejected" with its reason;
  - **fold** into the row that carries it on;
  - **narrow**: clauses dropped, the rest stays;
  - **schedule** as a strand of its own;
  - **keep**, with the reason it stays.
- No new ID without the author's go.
- Nothing closes, folds or narrows before the author has decided ("Read-only until the author gives
  a go").
- Every hand-over states the row count before and after.
- After the go, each outcome is a dated decision row (section 5).
  - The harvest's own row names the row count after.
  - Section 1 measures growth against it.

Optional scope from the author: $ARGUMENTS

Outside harvest mode, start by orienting, then report the difference list. Do not propose an order
before you have run all eight checks: the finding that changes the plan is usually the one nobody
was looking for.
