# Evergore Protocol Collector: Knowledge Base

Persistent knowledge base. Purpose: any human or AI agent gets up to speed **without re-scanning
the codebase**. Tool-neutral, **single source of truth**; per-tool entry files only point here.

## Step 0: onboarding a tool (per-tool entry file)

- Tool auto-loads a native entry file (Claude Code: [`../../CLAUDE.md`](../../CLAUDE.md))? It is a
  thin wrapper built from [agent-entry-template.md](agent-entry-template.md); rules are never
  duplicated into it.
- No native entry file? Build a wrapper from the template: copy the SHARED section, fill
  TOOL-SPECIFIC with that tool's mechanics only, record `Based on agent-entry-template version: N`.
- Every session start: quick-check the wrapper's SHARED section against the template's current
  version; re-sync on a bump.

## What this project is, in one sentence

A Java / Micronaut service that scrapes the browser game [Evergore](https://evergore.de) for a
guild's bank and storage transaction logs, stores them in SQLite, and computes per-member
contribution metrics, automating a hand-maintained Google Sheet.

## Read in this order

| # | Doc | What you'll learn |
|---|-----|-------------------|
| 01 | [project-overview.md](project-overview.md) | The product, who it's for, the problem it solves |
| 02 | [google-sheet.md](google-sheet.md) | The Google Sheet being replaced: columns, formulas, the sheet↔software gap |
| 03 | [domain-model.md](domain-model.md) | Domain concepts: Avatar, Entry, Item, EvergoreItem catalog, TransferType, Bank vs Storage, the value math |
| 04 | [architecture.md](architecture.md) | Layers, data-flow pipeline, ports & adapters, hexagonal gap analysis |
| 05 | [build-run-deploy.md](build-run-deploy.md) | Docker build, config, secrets, endpoints, the scheduled job |
| 06 | [testing.md](testing.md) | Test inventory, coverage map, biggest gaps |
| 07 | [git-state.md](git-state.md) | Git conventions; **git is the source of truth for history** (docs don't duplicate it) |
| 08 | [glossary.md](glossary.md) | German ↔ English domain glossary (the domain is German) |
| 09 | [engineering-handbook.md](engineering-handbook.md) | **How code looks & how we develop**: clean code, SOLID, hexagonal, TDD, BDD, commits, DoD |
| 10 | [working-with-ai-agents.md](working-with-ai-agents.md) | The AI-assisted workflow: memory layers, session playbook, sub-agents, asking style |
| 11 | [multi-agent-playbook.md](multi-agent-playbook.md) | The agent team: Planner · Implementer · Falsifier panel · Doc reviewer · Reviewer; pipeline, roles, invocation |
| 12 | [dev-environment.md](dev-environment.md) | Fully-virtualized dev: the devcontainer, the in-container rule, JDK single-source & upgrade |
| 13 | [agent-entry-template.md](agent-entry-template.md) | Per-tool session-bootstrap template: SHARED rules + TOOL-SPECIFIC skeleton + template version |
| 14 | [frontend.md](frontend.md) | The React/TypeScript SPA and the JSON API it reads: stack, module structure & dependency rule, the wire contract, TDD conventions, Gradle/Docker/vulnScan wiring |

## Living documents (outside this folder)

- [../../CLAUDE.md](../../CLAUDE.md): Claude Code entry file (from the template; auto-loaded).
- [../../.claude/agents/](../../.claude/agents/): subagent definitions (implementer, falsifiers, doc-reviewer, reviewer).
- [../backlog.md](../backlog.md): prioritized backlog (PO / architect / engineering hats).
- [../roadmap.md](../roadmap.md): milestone order + per-milestone acceptance (items stay in the backlog).
- [../risks.md](../risks.md): risk register with countermeasures.
- [../open-questions.md](../open-questions.md): open questions & decisions log.
- [../process-learnings.md](../process-learnings.md): where the process slipped & how we prevent it.

## Doc conventions (the checklist; the `doc-reviewer` agent enforces it)

Applies to every `*.md` in the repo (root, `docs/`, `.claude/`). Findings cite rule IDs.

- **DOC-1 Terse:** bullets and tables over paragraphs; one fact per bullet; no filler, no restating
  context the reader already has. Prose only where a causal chain genuinely needs it.
- **DOC-2 Keep the why:** a rule carries at most a one-line rationale; never strip it entirely
  (rules without a why get misapplied).
- **DOC-3 Git is history:** no changelogs, no commit narration, no was/now diffs, no session
  references. Docs state current truth only.
- **DOC-4 Single home:** each rule/fact lives in exactly one doc; everything else cross-links.
  Duplication drifts. Exception: `.claude/agents/*.md` are prompts, not docs, and an agent only ever
  reads its own brief, so an operative instruction may repeat verbatim across briefs; its rationale
  still lives only in the KB.
- **DOC-5 Backlog holds only live work:** open and in-progress items only; completing an item
  removes its row **and** every shortcode reference repo-wide; rejected/deferred items stay, with
  rationale.
- **DOC-6 KB-current:** a behavior/config change updates the relevant KB doc in the same change.
- **DOC-7 Decisions land in open-questions.md:** the decision plus its why, at decision time.
- **DOC-8 No backlog IDs outside the backlog:** identify things by symbol/behavior/concept; a
  shortcode may appear only as an optional pointer to a still-live item.
- **DOC-9 Language & tone:** English; plain, technically correct wording; no inflated language; no
  em dashes or spaced dash-asides (restructure instead); ordinary hyphens fine.
- **DOC-10 Stable anchors:** keep §-numbers and heading text stable (they are link targets); when a
  heading must change, update every inbound link in the same change.
