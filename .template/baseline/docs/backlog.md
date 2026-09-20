# Backlog: {{PROJECT_NAME}}

> Decisions and their rationale live in [open-questions.md](open-questions.md); the knowledge base
> map is [knowledge-base/README.md](knowledge-base/README.md).

## ▶ Current status / next action

`git log` is the record of what landed; this section states only **where we are and what is next**.
Every session reads this section first (see [CLAUDE.md](../CLAUDE.md)). It stays in the repository
whether or not the team has a ticket system: it is the session's entry point.

**Conventions:** no prose and no durable knowledge here. What a later reader needs belongs in the
[knowledge base](knowledge-base/README.md), what a decision needs in
[open-questions.md](open-questions.md), and an environment quirk in the KB doc it concerns
(dev-environment.md, build-run-deploy.md, the playbook's agent gotchas). Completed items are
**removed** (the commits show them, no `DONE` tombstones); only **rejected/deferred** items stay,
with their decision + rationale. Item IDs live **only here**; removing an item also removes **every
shortcode that referenced it** across the docs. Derive a new ID from the **whole file** (the highest
per group), never from the last row you happened to read, and re-read the table after inserting: a
blank line between two rows splits the table mid-body (DOC-11).

**Where we are:**

> **ADAPT:** short statements on the current state of the codebase and process, updated as work lands.

**Next action:**

> **ADAPT:** The single next work item, concrete enough that a fresh session can start without asking.
> With a ticket system, name the ticket key.

## Items

> **ADAPT:** Without a ticket system, your open work items live here, grouped as you like (epics,
> short IDs); keep only open and rejected/deferred items. With a ticket system (Jira, GitLab issues),
> the items live there: replace this section with the pointer to the ticket project and the query
> that lists the open work, and keep temporary personal tasks in the gitignored
> `docs/backlog.local.md` instead.
