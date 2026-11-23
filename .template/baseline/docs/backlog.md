# Backlog: {{PROJECT_NAME}}

> Decisions and their rationale live in [open-questions.md](open-questions.md); the knowledge base
> map is [knowledge-base/README.md](knowledge-base/README.md).

## ▶ Current status / next action

`git log` is the record of what landed; this section states only **where we are and what is next**.
Every session reads this section first (see [CLAUDE.md](../CLAUDE.md)). It stays in the repository
whether or not the team has a ticket system: it is the session's entry point.

**Conventions:**

- No prose and no durable knowledge here:
  - what a later reader needs belongs in the [knowledge base](knowledge-base/README.md);
  - what a decision needs, in [open-questions.md](open-questions.md);
  - an environment quirk, in the KB doc it concerns (dev-environment.md, build-run-deploy.md, the
    playbook's agent gotchas).
- Item IDs live **only** here.
- Choosing an ID, completing, parking or rejecting a row and filing doc debt follow DOC-5, a table
  edit DOC-11 ([knowledge-base/README.md](knowledge-base/README.md), "Doc conventions").

**Where we are:**

> **ADAPT:** short statements on the current state of the codebase and process, updated as work lands.

**Next action:**

> **ADAPT:** The single next work item, concrete enough that a fresh session can start without asking.
> With a ticket system, name the ticket key.

**Next, in order:**

> **ADAPT:** Once more than the next action is lined up, list it here in waves:
>
> - the first wave numbered, in its order;
> - every later wave headed by a trigger the author can observe (`once <item> has landed`,
>   `after the author's go on <decision>`), never by an ordinal;
> - each item named in words, its ID once in parentheses.

## Items

| ID | Item | Why |
|----|------|-----|

> **ADAPT:** Without a ticket system, your open work items live in this table:
>
> - grouped as you like (epics, short IDs), one table per group;
> - with the columns you need added (acceptance, effort, priority);
> - open and parked rows only.
>
> With a ticket system (Jira, GitLab issues), the items live there:
>
> - replace this section with the pointer to the ticket project and the query that lists the open
>   work;
> - temporary personal tasks go to the gitignored `docs/backlog.local.md` instead.

## Considered and rejected (do not re-propose without a new reason)

> **ADAPT:** filled as items are rejected (DOC-5).

## Doc debt: wrong text left over

> **ADAPT:** filled as a strand leaves wrong text it could not clear (DOC-5).
