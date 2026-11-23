# Roadmap: {{PROJECT_NAME}}

Milestone order and per-milestone acceptance for the open work.

- Items, priorities and per-item acceptance stay canonical in [backlog.md](backlog.md).
  - An ID here is a pointer to a still-live item, after its words (DOC-8).
- Decisions and their why: [open-questions.md](open-questions.md).
- This file adds only the cut into milestones, their order, and the milestone-level "done".
- A completed milestone is removed (DOC-5).
  - Why: git is the history.
- A checked acceptance box leaves in the commit that checks it.

**Ordering rule** (each step's decision and its why in open-questions.md):

> **ADAPT:** one bullet per step of the order, e.g.:
>
> - the next release and the work it carries first;
> - then what changes what the numbers are;
> - then the bottlenecks;
> - then product build-out and operations.

## Order

| # | Milestone | Items | Why this position |
|---|-----------|-------|-------------------|

> **ADAPT:**
>
> - one row per milestone, in order;
> - Items names each item in words, its ID once in parentheses;
> - Why this position cites the deciding row by words (open-questions.md, the topic).

## Milestones

> **ADAPT:** one `###` section per milestone, in the order above:
>
> - its slice, in one sentence;
> - its acceptance as checkboxes (`- [ ]`), each observable and naming its backlog item.

## Ongoing (no milestone; pull into any gap)

> **ADAPT:** the rows no milestone carries, pulled into any gap:
>
> - grouped by theme (gate, hook, agent-tool and doc rows, test and code hygiene);
> - each group with its why.

## Parked: explicitly not in this stage

> **ADAPT:**
>
> - one bullet per parked item: its title in words, its ID once in parentheses, and why it waits;
> - the row itself stays in the backlog, its title starting with **Parked** (DOC-5).
