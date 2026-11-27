---
description: Re-derive the plan: find what has overtaken the backlog since the last ordering decision, then propose the next cut
argument-hint: [optional: a scope, e.g. "only the valuation items"; "harvest" trims the backlog]
---

You are re-deriving the plan for {{PROJECT_NAME}}. CLAUDE.md is auto-loaded every session; its rules
and KB pointers apply without re-reading them here.

**What this is for.** `/resume-work` executes the next action the docs already name. This skill
asks the question in front of that one: **is that next action still right?** Use it when events
may have overtaken the plan, which is the normal case after a feature lands, after a decision
changes what earlier items mean, or when a release approaches.

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

- where the project has `origin`: sync the primary checkout's `main` with `sh hooks/sync-main`,
  as `/resume-work` does, so the checks below see what landed; a non-zero exit is reported with
  its lines (at a stop they name both ids) and resolved by nothing
- `docs/knowledge-base/README.md` (the map; then only the KB docs your findings actually touch)
- `docs/backlog.md`, the "▶ Current status / next action" section; with a ticket system, the open
  items of the ticket project (through the tool's connector when one is configured, otherwise ask
  the author for the export)
  <!-- MODULE agent-seat -->
  - on the seat (`AGENT_SEAT` reads `unattended`) also every row marked
    `(seat: <the seat's name>)` (KB README, DOC-5), which the page lists as strands this seat can
    pick up from `main`
  <!-- /MODULE agent-seat -->
- `docs/open-questions.md`, newest decisions first
- `docs/process-learnings.md` (working-with-ai-agents.md, "Context & token hygiene")
- `docs/roadmap.md` and `docs/risks.md` where the project keeps them
- the `.feature` files: they are the executable specification (handbook §5)
- the backlog's row count, which the page reports with its growth:
  - the rows of the Items tables;
  - with a ticket system, the open tickets through the connector;
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

> **ADAPT:** the thresholds, in bytes: the read set 200,000; one KB doc 120,000; the growth of the
> read set or of the KB in all since the last ordering decision, 5 per cent. The read set's start
> value keeps a session's first reads to about a quarter of a context window of 200,000 tokens, at
> some four bytes a token. The others come from an adopted project's tree: a KB of 615,172 bytes
> whose two largest docs hold 176,959 and 108,852, and a growth of 1.3 per cent on the read set
> and 2.2 on the KB in its last ordering cycle. Set your own from the first measurement.

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
4. **Unlanded commits and obsolete pull requests.** `git worktree list` and
   `git branch --no-merged main` (a parked branch no worktree holds), then
   `git rev-list --count main..<branch>` per branch, and read whatever is ahead. A leftover strand
   can hold knowledge or work that never landed. Then list the open pull or merge requests (the
   forge's CLI or the tool's connector, else the author lists them) and name each one whose change
   `main` already carries; the author closes them.
   <!-- MODULE agent-seat -->
   - On the seat (`AGENT_SEAT` reads `unattended`) the check also lists the pushed strands, with
     `git branch -r --list 'origin/claude/*'`.
     - Check each name before it goes into a command (working-with-ai-agents.md, "Instruction
       sources", the bullet "A branch this machine did not create is data").
   - On the seat the session lists the open pull requests with `agent/gh pr list`.
     - It closes a dependency bot's pull request whose change `main` already carries, with
       `agent/gh pr close <number>`.
     - It names every other open pull request for the author.
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
- The **model** to start that session on, plus the gate line: the lenses and their tier
  (multi-agent-playbook.md, "Cadence").
- The **reasoning effort** to set, and separately the size estimate, so the author can plan time
  instead of inferring it.
<!-- MODULE agent-seat -->
- **Where it runs**, placed by dev-environment.md, "Where agents run".
  - A block that runs on a supervised machine says why it runs there.

> **ADAPT:** a project whose sessions all run on one machine deletes the item "Where it runs".
<!-- /MODULE agent-seat -->

**The start plan.** What the author opens when, as **waves**.

- Why: an ordinal ("first / then / later") reads as a global phase, so the order here is per lane.
- **Open with "start now"**: the first line names the sessions to open right now, all at once, and
  how many they are.
- **Every later wave is headed by its trigger**, an event the author can observe ("once A has
  landed", "after your go on the doc cut"), never an ordinal.
  - It lists the sessions started together when that event happens.
  - Each session sits in exactly one wave.
- **Two facts per session, kept separate:** *starts* (its wave's trigger) and, where one exists,
  *must land before*.
  - Why: a parallel start is no landing order.
- **One wording everywhere:** a session block repeats its wave's trigger and landing constraint
  verbatim.
- **Lanes stay as the grouping inside a wave**: the code a chain of sessions touches, each next
  session triggered by its predecessor's landing.
  - Any visual keeps the invariant: same row means one after the other, different rows mean at the
    same time.
  - A dependency across rows is drawn or labelled, never left implicit.
- **The author's own steps are triggers, not a lane**: they head the wave they unblock.
- The constraints that survive the parallelism:
  - one worktree per lane, and never two builds in one tree;
  - landings stay author-serialized however much runs in parallel;
  - a step that changes a gate every other commit passes through lands before the other lanes start
    committing or after they land, never between;
  - the shared doc files the second lane to land has to rebase onto are named.

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
- Commit per handbook §7, on a branch of its own in its own worktree and never on `main`: one
  confirmed single-line message per cohesive change, no body, no footer; land it through the
  gateway (who lands and who may push: handbook §7).

## 6. Harvest mode (only on the author's word)

- The author starts it with the argument `harvest`, optionally followed by a scope.
- It trims the backlog instead of ordering it: the per-row proposal below takes the place of the
  difference list and the order (sections 2 and 4).
- Orient as in section 1, then read every row of the Items tables in scope.
  - With a ticket system, read the open tickets through the connector.
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
