# Evergore Protocol Collector: Knowledge Base

Persistent knowledge base. Purpose: any human or AI agent gets up to speed **without re-scanning
the codebase**. Tool-neutral, **single source of truth**; per-tool entry files only point here.

## Step 0: onboarding a tool (per-tool entry file)

- A tool that auto-loads a native entry file (Claude Code: [`../../CLAUDE.md`](../../CLAUDE.md))
  gets a thin wrapper built from [agent-entry-template.md](agent-entry-template.md).
- Rules are never duplicated into a wrapper.
- A tool with no native entry file gets a wrapper built from the template:
  - copy the SHARED section;
  - fill TOOL-SPECIFIC with that tool's mechanics only;
  - record `Based on agent-entry-template version: N`.
- Every session start: quick-check the wrapper's SHARED section against the template's current
  version.
- Re-sync the wrapper on a version bump.
  - The version marker is the signal.
- The commit guard on the two SHARED sections: [hooks/README.md](../../hooks/README.md), the
  `pre-commit` hook.

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
| 07 | [git-state.md](git-state.md) | Git conventions; **git is the source of truth for history** (docs don't duplicate it); what GitHub enforces and whom it binds |
| 08 | [glossary.md](glossary.md) | German ↔ English domain glossary (the domain is German) |
| 09 | [engineering-handbook.md](engineering-handbook.md) | **How code looks & how we develop**: clean code, SOLID, hexagonal, TDD, BDD, commits, DoD |
| 10 | [working-with-ai-agents.md](working-with-ai-agents.md) | The AI-assisted workflow: memory layers, session playbook, sub-agents, asking style |
| 11 | [multi-agent-playbook.md](multi-agent-playbook.md) | The agent team: pipeline, roles, invocation |
| 12 | [dev-environment.md](dev-environment.md) | Fully-virtualized dev: the devcontainer, the in-container rule, where agents run, Docker on the agent seat, the agent image, JDK single-source & upgrade |
| 13 | [agent-entry-template.md](agent-entry-template.md) | Per-tool session-bootstrap template: SHARED rules + TOOL-SPECIFIC skeleton + template version |
| 14 | [frontend.md](frontend.md) | The React/TypeScript SPA and the JSON API it reads: stack, module structure & dependency rule, the wire contract, TDD conventions, Gradle/Docker/vulnScan wiring |

## Living documents (outside this folder)

- [../../CLAUDE.md](../../CLAUDE.md): Claude Code entry file (from the template; auto-loaded).
- [../../.claude/agents/](../../.claude/agents/): the subagent definitions of the agent team that
  [multi-agent-playbook.md](multi-agent-playbook.md) describes.
- The `.feature` files under `src/test/resources/features/`: the executable specification (handbook
  §5).
  - They are the author's primary review artifact.
  - Their runner: [testing.md](testing.md).
- [../backlog.md](../backlog.md): the prioritized items (PO / architect / engineering hats).
  - Its "▶ Current status / next action" section is what every session reads first.
- [../roadmap.md](../roadmap.md): milestone order and per-milestone acceptance.
  - The items themselves stay in the backlog.
- [../risks.md](../risks.md): risk register with countermeasures.
- [../open-questions.md](../open-questions.md): open questions & decisions log.
- [../process-learnings.md](../process-learnings.md): where the process slipped & how we prevent it.

## Doc conventions (the checklist; the `doc-reviewer` agent enforces it)

Applies to every `*.md` in the repo (root, `docs/`, `.claude/`). Findings cite rule IDs.

- **DOC-1 Terse:** bullets and tables over paragraphs.
  - One fact per bullet.
  - No filler.
  - No restating context the reader already has.
  - Prose only where a causal chain genuinely needs it.
  - Read strictly:
    - every bullet and every sub-bullet holds one fact;
    - a fact's own short reason stays in its bullet only when it is no second fact.
- **DOC-2 Keep the why:** a rule carries at most a one-line rationale.
  - Never strip it entirely (rules without a why get misapplied).
  - The rationale has to be true: a worked example cited in a doc is checked against the code before
    it is written down.
- **DOC-3 Git is history:** docs state current truth only.
  - No changelogs.
  - No commit narration.
  - No was/now diffs.
  - No session references.
- **DOC-4 Single home:** each rule/fact lives in exactly one doc. Duplication drifts.
  - Everything else cross-links.
  - Exception: `.claude/agents/*.md` are prompts, not docs, and an agent only ever reads its own
    brief, so an operative instruction may repeat verbatim across briefs.
  - That instruction's rationale still lives only in the KB.
  - Second exception: `CLAUDE.md` may name the literal paths the auto-mode classifier has to
    recognise as the agent's local scratch, since the classifier reads `CLAUDE.md` and not the KB
    (decision 2026-10-13, [open-questions.md](../open-questions.md)).
- **DOC-5 Backlog holds only live work:** open and in-progress items only.
  - Completing an item removes its row **and** every shortcode reference repo-wide.
  - Rejected/deferred items stay, with rationale.
- **DOC-6 KB-current:** a behavior/config change updates the relevant KB doc in the same change.
  - A doc hunk describing a code change rides **in that commit**, never in a trailing `[doc]`
    follow-up, which cannot stand alone for revert or cherry-pick.
  - A doc claim also lands no earlier than the thing it names.
  - The check is per hunk, in both directions:
    - still true after reverting only the code commit (then misplaced);
    - true at the commit it rides in (`git grep <symbol> <sha>`, never the working tree).
- **DOC-7 Decisions land in open-questions.md:** the decision plus its why, at decision time.
- **DOC-8 No backlog IDs outside the backlog:** identify things by symbol/behavior/concept.
  - A shortcode may appear only as an optional pointer to a still-live item.
  - Lead with the distinguishing noun, then carry the shortcode once in parentheses: words first,
    pointer kept, so this rule and DOC-5 compose instead of one deleting what the other needs.
- **DOC-9 Language & tone:** English.
  - Plain, technically correct wording.
  - No inflated language.
  - No em dashes or spaced dash-asides (restructure instead).
  - Ordinary hyphens fine.
- **DOC-10 Stable anchors:** keep §-numbers and heading text stable (they are link targets).
  - When a heading must change, update every inbound link in the same change.
- **DOC-11 Verify a table edit by re-reading the file, not the diff.**
  - A diff shows the text you added, not the missing newline that swallowed it: a row appended onto
    the previous row's line renders as its trailing cells and the entry is effectively absent.
  - After editing a Markdown table, read the file's rows back.
  - After editing a Markdown table, assert no blank line sits between two rows of one table.
