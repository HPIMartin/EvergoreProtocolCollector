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
  section. (The version marker is the signal; the `pre-commit` leg that compares the wrapper's
  SHARED section with the template's refuses a commit that lets them drift, hooks/README.md.)

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
| 10 | [multi-agent-playbook.md](multi-agent-playbook.md) | The **agent team**: Planner (you+me) · Implementer · Falsifier panel · Doc reviewer · Reviewer. Pipeline, roles, how to invoke |
| 11 | [dev-environment.md](dev-environment.md) | **The pinned dev environment**: the devcontainer and native-toolchain variants, the environment rule for agents, toolchain single-sourcing & upgrade procedure |
| 12 | [agent-entry-template.md](agent-entry-template.md) | **Per-tool session-bootstrap template**: the SHARED rules + TOOL-SPECIFIC skeleton each tool's entry file (e.g. `CLAUDE.md`) is built from, plus the template version |

Projects can add further domain docs to this folder as needed and slot them into the reading order.
(Example: the project this template was extracted from had a doc describing the legacy spreadsheet
its software replaced: columns, formulas, and the gap between sheet and software.)

## Living documents (outside this folder)

- **[../../CLAUDE.md](../../CLAUDE.md)**: the Claude Code per-tool entry file (built from
  [agent-entry-template.md](agent-entry-template.md)); auto-loaded each session. Other tools build their
  own equivalent from the template (see "Step 0" above).
- **[../../.claude/agents/](../../.claude/agents/)**: the subagent definitions (implementer, scenario falsifier, code falsifiers, doc-reviewer, reviewer).
- **The `.feature` files**: the executable specification (handbook §5) and the author's primary
  review artifact; they live where the acceptance runner expects them ([testing.md](testing.md)
  names the place).
- **[../backlog.md](../backlog.md)**: the "▶ Current status / next action" section every session
  reads first, plus the prioritized items (PO / architect / engineering hats) when the team has no
  ticket system. With one, the items live in the ticket system, this file keeps the status section
  and the pointer, and temporary personal tasks go to the gitignored `docs/backlog.local.md`.
- **[../open-questions.md](../open-questions.md)**: open questions & decisions log.
- **[../process-learnings.md](../process-learnings.md)**: where the process slipped & how we prevent it.

**Where knowledge is canonical when the team has a ticket system and a wiki** (Jira and Confluence,
GitLab issues and its wiki, or equivalents; recorded at adoption in open-questions.md):

- The **repository KB stays canonical for engineering knowledge** that must travel with the code:
  architecture, build/run/deploy, testing, the handbook, and the decisions that shape code. It is
  what a session reads without network access.
- The **wiki holds stakeholder documentation**: product overview, requirements, release notes,
  meeting notes. The repo links to it, never copies it.
- The **ticket system holds the work**; `backlog.md` keeps the status section and points at it.
- Agents read tickets and wiki pages through the tool's connector when the project has one
  configured; without a connector the author pastes what a task needs.

Further living docs pay off once the backlog outgrows one screen; add them then, not before, and
keep items canonical in the backlog or the ticket system (these only point at still-live items):

- `roadmap.md`: the cut into milestones, their **order**, and the milestone-level "done". A backlog
  answers *what*, not *in which order and why that order*, and a set of 50 open items is not a work
  list until something says which come first and which are explicitly parked with their reason.
- `risks.md`: what can hurt the project or its data, with likelihood/impact and the countermeasure.
  Re-checked whenever the roadmap changes.
- `adr/`: one file per architecture decision, MADR-lite (status, context, options or decision,
  consequences), while open-questions.md stays the dated index. Only for a project **without** a
  wiki; with one, the analyses live there and open-questions.md links to them.

All follow the backlog convention: a finished milestone, a closed risk or a superseded ADR is
**removed** or marked superseded with a pointer, git is the history.

## Doc conventions (the checklist; the `doc-reviewer` agent enforces it)

Applies to every `*.md` in the repo (root, `docs/`, `.claude/`). Findings cite rule IDs.
This is the repo's shared style; a contributor's personal chat-tone preferences live in their own
tool's user-level config, not in the repo.

- **DOC-1 Terse:** bullets and tables over paragraphs; one fact per bullet; no filler, no restating
  context the reader already has. Prose only where a causal chain genuinely needs it.
- **DOC-2 Keep the why:** a rule carries at most a one-line rationale; never strip it entirely
  (rules without a why get misapplied), and the rationale has to be true: a worked example cited in
  a doc is checked against the code before it is written down.
- **DOC-3 Git is history:** no changelogs, no commit narration, no was/now diffs, no session
  references. Docs state current truth only.
- **DOC-4 Single home:** each rule/fact lives in exactly one doc; everything else cross-links.
  Duplication drifts. Exception: `.claude/agents/*.md` are **prompts, not docs**, and an agent only
  ever reads its own brief, so an operative instruction may repeat verbatim across briefs; its
  rationale still lives only in the KB.
- **DOC-5 Backlog holds only live work:** open and in-progress items only; completing an item
  removes its row **and** every shortcode reference repo-wide; rejected/deferred items stay, with
  rationale. With a ticket system the items live there, `backlog.md` keeps only the status section
  and the pointer, and the removal duty covers local shortcodes only.
- **DOC-6 KB-current:** a behavior/config change updates the relevant KB doc in the same change, and
  a doc hunk describing a code change rides **in that commit**, never in a trailing `[doc]` follow-up.
  A doc claim also lands no earlier than the thing it names. The check is per hunk, in both
  directions: still true after reverting only the code commit (then misplaced), and true at the
  commit it rides in (`git grep <symbol> <sha>`, never the working tree).
- **DOC-7 Decisions land in open-questions.md:** the decision plus its why, at decision time.
- **DOC-8 No backlog IDs outside the backlog:** identify things by symbol/behavior/concept; a
  shortcode may appear only as an optional pointer to a still-live item. Lead with the distinguishing
  noun, then carry the shortcode once in parentheses: words first, pointer kept, so this rule and
  DOC-5 compose instead of one deleting what the other needs. A ticket key of the team's ticket
  system stays resolvable after the item closes and may stand as a pointer without the removal duty.
- **DOC-9 Language & tone:** English; plain, technically correct wording; no inflated language; no
  em dashes or spaced dash-asides (restructure instead); ordinary hyphens fine.
- **DOC-10 Stable anchors:** keep §-numbers and heading text stable (they are link targets); when a
  heading must change, update every inbound link in the same change.
- **DOC-11 Verify a table edit by re-reading the file, not the diff.** A diff shows the text you
  added, not the missing newline that swallowed it: a row appended onto the previous row's line
  renders as its trailing cells and the entry is effectively absent. After editing a Markdown table,
  read the file's rows back, and assert no blank line sits between two rows of one table.

## Placeholder key (template adoption)

Three tokens are resolved once, at adoption, by a global search-replace (the `/adopt` skill automates
it; the stack presets in the template's `stacks/` folder supply the stack value). Build and test
commands are not tokens: every project exposes them as `./verify all` and `./verify focus <path>`
([build-run-deploy.md](build-run-deploy.md)), and the stack lives inside that script.

| Token | Meaning |
|---|---|
| `{{PROJECT_NAME}}` | project name |
| `{{PROJECT_ONE_LINER}}` | one sentence: what it does, for whom, what it deliberately showcases |
| `{{TECH_STACK}}` | language and toolchain, e.g. "Java/Gradle" or "TypeScript/Node" |

If any token is still visible in an adopted project, the adoption pass is incomplete: finish the
search-replace and work through `grep -rn "ADAPT:"`. Delete this section once adoption is complete
(the `/adopt` skill does).
