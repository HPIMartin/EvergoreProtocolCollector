# Evergore Protocol Collector — Knowledge Base

This folder is the persistent knowledge base for the project. It exists so that any
human or AI agent can get up to speed **without re-scanning the whole codebase**.

> Status: created 2026-06-13 by an analysis pass. Facts in docs 01–08 are derived from
> the working tree as it stood during the rebuild (branch `main`). Docs 09–10 capture how we work.
> The backlog and open questions (`../backlog.md`, `../open-questions.md`) are living documents.

## What this project is, in one sentence

A Java / Micronaut service that **scrapes the browser game [Evergore](https://evergore.de)**
for a guild's bank and storage transaction logs, stores them in SQLite, and computes
per-member contribution metrics — automating a manual Google Sheet that a guild
officer used to maintain by hand.

## Read in this order

| # | Doc | What you'll learn |
|---|-----|-------------------|
| 01 | [project-overview.md](project-overview.md) | The product, who it's for, the problem it solves |
| 02 | [google-sheet.md](google-sheet.md) | The Google Sheet being replaced: columns, formulas, the sheet↔software gap |
| 03 | [domain-model.md](domain-model.md) | Domain concepts: Avatar, Entry, Item, EvergoreItem catalog, TransferType, Bank vs Storage, the value math |
| 04 | [architecture.md](architecture.md) | Layers, data-flow pipeline, ports & adapters, hexagonal gap analysis |
| 05 | [build-run-deploy.md](build-run-deploy.md) | Docker build, config, secrets, endpoints, the scheduled job |
| 06 | [testing.md](testing.md) | Test inventory, coverage map, biggest gaps |
| 07 | [git-state.md](git-state.md) | Git conventions & the rule that **git is the source of truth** for history (docs don't duplicate it) |
| 08 | [glossary.md](glossary.md) | German ↔ English domain glossary (the domain is German) |
| 09 | [engineering-handbook.md](engineering-handbook.md) | **How the code should look & how we develop** — clean code, SOLID, hexagonal rules, TDD, BDD, commits, Definition of Done |
| 10 | [working-with-claude.md](working-with-claude.md) | The **AI-assisted workflow showcase** — memory layers, session playbook, sub-agents, asking style |
| 11 | [multi-agent-playbook.md](multi-agent-playbook.md) | The **agent team**: Planner (you+me) · Implementer · Falsifier · Reviewer — pipeline, roles, how to invoke |
| 12 | [dev-environment.md](dev-environment.md) | **Fully-virtualized dev** — the devcontainer, the in-container rule for agents, JDK single-source & upgrade procedure |

## Living documents (outside this folder)

- **[../../CLAUDE.md](../../CLAUDE.md)** — session bootstrap; auto-loaded by Claude Code each session.
- **[../../.claude/agents/](../../.claude/agents/)** — the `implementer` / `falsifier` / `reviewer` subagent definitions.
- **[../backlog.md](../backlog.md)** — prioritized backlog (PO / architect / engineering hats).
- **[../open-questions.md](../open-questions.md)** — open questions & decisions log.
- **[../process-learnings.md](../process-learnings.md)** — where the process slipped & how we prevent it.

## Conventions for agents editing this KB

- Keep each doc focused on its topic; cross-link rather than duplicate.
- When you change behavior in the code, update the relevant KB doc in the same change.
- Record *why* decisions were made in `../open-questions.md`, not in code comments only.
- **Don't duplicate git:** no changelogs, no commit-history narration, no diff snapshots in docs —
  use `git log`/`git diff`. Docs hold durable knowledge, not history.
