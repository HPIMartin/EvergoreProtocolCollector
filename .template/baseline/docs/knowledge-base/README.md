# {{PROJECT_NAME}}: Knowledge Base

This folder is the persistent knowledge base for the project. It exists so that any
human or AI agent can get up to speed **without re-scanning the whole codebase**.

> **ADAPT:** optionally add a short dated status note here once the project-specific docs (01-05, 07)
> are filled in: when the facts were captured and from which branch, so readers know how fresh they are.

## Step 0: onboarding a tool (per-tool entry file)

This KB is **tool-neutral and the single source of truth**. Any human or AI tool reads it directly.
Tools that auto-load a native entry file get a thin wrapper that *points* here (Claude Code loads
[`../../CLAUDE.md`](../../CLAUDE.md)); the rules themselves are never duplicated into the wrapper.

- **No native entry file?** Build a thin wrapper from
  [agent-entry-template.md](agent-entry-template.md): copy its SHARED section, fill the TOOL-SPECIFIC
  section with only that tool's mechanics, and record `Based on agent-entry-template version: N`.
- **Already have a wrapper?** At session start, **quick-check** that its SHARED section still matches
  the template's current `Template version`. On a version bump, **re-sync** the wrapper's SHARED
  section. (Lightweight: a version marker plus a manual quick-check; a session-start hook can
  enforce it later.)

## What this project is, in one sentence

{{PROJECT_ONE_LINER}}

> **ADAPT:** one sentence stating what the software does, for whom, and what problem it solves or
> what manual process it replaces.

## Read in this order

Docs 01-05 and 07 ship as **skeletons**: fill them in for your project. Docs 06 and 08-12 carry the
reusable process knowledge and normally need only light adaptation.

| # | Doc | What you'll learn |
|---|-----|-------------------|
| 01 | [project-overview.md](project-overview.md) | The product, who it's for, the problem it solves (skeleton: fill in your product's purpose, users, and scope) |
| 02 | [domain-model.md](domain-model.md) | The core domain concepts and how they relate (skeleton: fill in your entities, value objects, and the key invariants/math) |
| 03 | [architecture.md](architecture.md) | Layers, data flow, module boundaries (skeleton: fill in your layering, e.g. ports & adapters, and any known gaps) |
| 04 | [build-run-deploy.md](build-run-deploy.md) | How to build, configure, run, and deploy (skeleton: fill in build commands, config/secrets handling, endpoints, scheduled jobs) |
| 05 | [testing.md](testing.md) | Test inventory, coverage map, biggest gaps (skeleton: fill in your test layout and where coverage is thin) |
| 06 | [git-state.md](git-state.md) | Git conventions & the rule that **git is the source of truth** for history (docs don't duplicate it) |
| 07 | [glossary.md](glossary.md) | Domain glossary (skeleton: fill in domain terms, especially where the domain language differs from English) |
| 08 | [engineering-handbook.md](engineering-handbook.md) | **How the code should look & how we develop**: clean code, SOLID, architecture rules, TDD, BDD, commits, Definition of Done |
| 09 | [working-with-ai-agents.md](working-with-ai-agents.md) | The **AI-assisted workflow**: memory layers, session playbook, sub-agents, asking style |
| 10 | [multi-agent-playbook.md](multi-agent-playbook.md) | The **agent team**: Planner (you+me) · Implementer · Falsifier · Reviewer. Pipeline, roles, how to invoke |
| 11 | [dev-environment.md](dev-environment.md) | **The pinned dev environment**: the devcontainer and native-toolchain variants, the environment rule for agents, toolchain single-sourcing & upgrade procedure |
| 12 | [agent-entry-template.md](agent-entry-template.md) | **Per-tool session-bootstrap template**: the SHARED rules + TOOL-SPECIFIC skeleton each tool's entry file (e.g. `CLAUDE.md`) is built from, plus the template version |

Projects can add further domain docs to this folder as needed and slot them into the reading order.
(Example: the project this template was extracted from had a doc describing the legacy spreadsheet
its software replaced: columns, formulas, and the gap between sheet and software.)

## Living documents (outside this folder)

- **[../../CLAUDE.md](../../CLAUDE.md)**: the Claude Code per-tool entry file (built from
  [agent-entry-template.md](agent-entry-template.md)); auto-loaded each session. Other tools build their
  own equivalent from the template (see "Step 0" above).
- **[../../.claude/agents/](../../.claude/agents/)**: the subagent definitions (implementer, falsifiers, reviewer).
- **[../backlog.md](../backlog.md)**: prioritized backlog (PO / architect / engineering hats).
- **[../open-questions.md](../open-questions.md)**: open questions & decisions log.
- **[../process-learnings.md](../process-learnings.md)**: where the process slipped & how we prevent it.

## Conventions for agents editing this KB

- **Writing style:** terse and factual; no filler prose (a dev should not have to wade through it).
  Plain, technically-correct language, no inflated wording, natural voice. Avoid em dashes and
  dash-asides; restructure with commas, parentheses, colons, or separate sentences. Ordinary hyphens in
  compound words are fine. This is the repo's shared style; a contributor's personal chat-tone
  preferences live in their own tool's user-level config, not in the repo.
- Keep each doc focused on its topic; cross-link rather than duplicate.
- When you change behavior in the code, update the relevant KB doc in the same change.
- Record *why* decisions were made in `../open-questions.md`, not in code comments only.
- **Don't duplicate git:** no changelogs, no commit-history narration, no diff snapshots in docs.
  Use `git log`/`git diff`. Docs hold durable knowledge, not history.

## Placeholder key (template adoption)

Five tokens are resolved once, at adoption, by a global search-replace (the `/adopt` skill automates
it; stack presets in the template's `stacks/` folder supply ready values):

| Token | Meaning |
|---|---|
| `{{PROJECT_NAME}}` | project name |
| `{{PROJECT_ONE_LINER}}` | one sentence: what it does, for whom, what it deliberately showcases |
| `{{TECH_STACK}}` | language and toolchain, e.g. "Java/Gradle" or "TypeScript/Node" |
| `{{BUILD_CMD}}` | full build including all tests |
| `{{TEST_CMD}}` | focused test run scoped to a single test class/file |

If any token is still visible in an adopted project, the adoption pass is incomplete: finish the
search-replace and work through `grep -rn "ADAPT:"`. Delete this section once adoption is complete
(the `/adopt` skill does).
