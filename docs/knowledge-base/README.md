# Evergore Protocol Collector: Knowledge Base

Persistent knowledge base. Purpose: any human or AI agent gets up to speed **without re-scanning
the codebase**. Tool-neutral, **single source of truth**; per-tool entry files only point here.

## Step 0: onboarding a tool (per-tool entry file)

- A tool that auto-loads a native entry file (Claude Code: [`../../CLAUDE.md`](../../CLAUDE.md))
  gets a thin wrapper built from [agent-entry-template.md](agent-entry-template.md).
- Rules are never duplicated into a wrapper.
- A tool with no native entry file gets a wrapper built from the template:
  - copy the SHARED section;
  - fill PROJECT CHOICES with this project's choices;
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
| 13 | [agent-entry-template.md](agent-entry-template.md) | Per-tool session-bootstrap template: SHARED rules + PROJECT CHOICES + TOOL-SPECIFIC skeleton + template version |
| 14 | [frontend.md](frontend.md) | The React/TypeScript SPA and the JSON API it reads: stack, module structure & dependency rule, the wire contract, TDD conventions, Gradle/Docker/vulnScan wiring |
| 15 | [permission-probe-record.md](permission-probe-record.md) | What the permission rules did on the build in use: the headless rule and workflow probes, a dated table per command of refused, prompted, blocked or ran, and the normal workflow's steps in auto mode. Re-run on this project's own settings; its dated sections are this project's own and follow with the first run (probe the upgraded permission rules, G94) |

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

- Applies to every `*.md` in the repo (root, `docs/`, `.claude/`).
- Findings cite rule IDs.
- A named limit: no lint ships for the mechanical rules (table splits, cell counts, an unescaped
  `|`, dashes, a reused ID), so the `doc-reviewer` checks them by eye.

**Scope of a finding:**

- The lines a strand writes are checked against every rule.
- Older text in lines the strand does not write counts only where it is wrong:
  - a claim the code or the repository contradicts (DOC-2, DOC-6);
  - a pointer to nothing;
  - a bare shortcode (DOC-8);
  - a wording error (DOC-9).
- Older DOC-1, DOC-3 and DOC-4 text in lines the strand does not write is no finding and is filed
  nowhere.
  - Why: filed style debt grows faster than anyone reads it.

**The rules:**

- **DOC-1 Terse:** bullets and tables over paragraphs.
  - One fact per bullet.
  - No filler.
  - No restating context the reader already has.
  - Prose only where a causal chain genuinely needs it.
  - Read strictly:
    - every bullet and every sub-bullet holds one fact;
    - a fact's own short reason stays in its bullet only when it is no second fact.
  - The rationale of a rule is a `Why:` sub-bullet of its own, which DOC-2 keeps.
  - Exception: a filing cell, a table cell that files several findings at once, joins its facts with
    `;`, each with its location.
    - So does the lows row of a passing round ([engineering-handbook.md](engineering-handbook.md)
      §9).
    - A decision row that excepts a text is recorded at decision time (DOC-7).
  - From the third gate round on, a remaining DOC-1 finding is a low: listed, never a FAIL.
- **DOC-2 Keep the why:** a rule carries at most a one-line rationale.
  - Never strip it entirely (rules without a why get misapplied).
  - The rationale has to be true: a worked example cited in a doc is checked against the code before
    it is written down.
- **DOC-3 Git is history:** docs state current truth only.
  - No changelogs.
  - No commit narration.
  - No was/now diffs.
  - No session references.
  - No dated review reference, and a probe reference only under the second exception below.
  - The two dated logs, open-questions.md and process-learnings.md, record events by date; DOC-3
    does not apply to their rows (decision 2026-10-14, [open-questions.md](../open-questions.md)).
    - The dated probe record ([permission-probe-record.md](permission-probe-record.md)) records
      events by date too, and DOC-3 does not apply to its rows.
  - Second exception: a measurement bound to a version may stand in any doc, either with the version
    in the sentence ("on Claude Code `<version>`") or by citing a row of the dated probe record,
    whose section names the version.
- **DOC-4 Single home:** each rule/fact lives in exactly one doc.
  - Everything else cross-links.
  - Why: duplication drifts.
  - Exception: `.claude/agents/*.md` are prompts, not docs, and an agent only ever reads its own
    brief, so an operative instruction may repeat verbatim across briefs.
  - That instruction's rationale still lives only in the KB.
  - Second exception: `CLAUDE.md` may name the literal paths, scratch worktree names and commands
    the auto-mode classifier has to recognise as the agent's local scratch, since the classifier
    reads `CLAUDE.md` and not the KB (decision 2026-10-13, [open-questions.md](../open-questions.md);
    working-with-ai-agents.md, "Where auto-mode rules live").
    - Under "Project choices" it may also restate each slot's value.
    - The why stays in the KB.
  - **Module text** stands in a block fenced by `<!-- MODULE <name> -->` and
    `<!-- /MODULE <name> -->`, each on a line of its own, never nested; `<name>` is the module's
    name.
    - The block is that module's text inside a core file: it goes with the module, and a project
      that keeps the module keeps the block whole.
    - Text that holds only with several modules names them joined by `+`, in both fences:
      `<!-- MODULE <name>+<name> -->`; the block goes as soon as one of them goes.
    - The fences do not render; `grep -rn '^ *<!-- MODULE '` lists the blocks.
    - In a core shell file the fences are the comment lines `# MODULE <name>` and
      `# /MODULE <name>`; `grep -rn '^ *# MODULE '` lists those blocks.
    - In a JSON file with comments (`devcontainer.json`) the fences are the comment lines
      `// MODULE <name>` and `// /MODULE <name>`, indented as the file is;
      `grep -rn '^[[:space:]]*// MODULE '` lists those blocks.
- **DOC-5 Backlog holds only live work:** open and in-progress items only, plus the two named
  sections (Considered and rejected; Doc debt).
  - Completing an item removes its row **and** every shortcode reference repo-wide.
  - A new ID is the highest its group ever held, plus one.
    - Check it with
      `git log --oneline --branches --remotes -E -G'(^|[^[:alnum:]-])<ID>([^[:alnum:]]|$)'`.
    - An empty result frees the ID; otherwise take the next number.
    - The command has no pathspec, so it covers every backlog file.
    - The pattern finds the ID as a whole word, in a table cell, in bold or in parentheses, so it
      never matches a longer ID that starts with the same characters.
    - `git log` sees only the refs this clone holds, so an ID used only on a deleted, unlanded
      branch can come back.
  - A row's title carries no origin bracket (gate, lens, strand, finder, date).
    - Why: `git log -S` finds the origin.
  <!-- MODULE agent-seat -->
  - A row that a session on the PC files carries a seat marker after its bold title.
    - `(seat: agent)` where the agent seat can take the row as a strand of its own from `main`.
    - `(seat: PC)` where the row needs the PC.
    - `/plan-next` and `/continue` read the marker on the seat.
  <!-- /MODULE agent-seat -->
  - A deferred row stays.
    - Its title starts with **Parked**, without a date.
    - Its Why cell cites the deciding row by words (open-questions.md, the topic).
  - A rejected item leaves the table for the backlog's "Considered and rejected" section: its title,
    the reason, and the deciding row cited by words.
  - Wrong doc text a strand meets ("Scope of a finding" above) is cleared along the way.
    - What is left after two fix attempts by the strand goes to the backlog's "Doc debt" section,
      without a question to the author.
    - One bullet per file, one wrong fact per sub-bullet with its location.
    - Before filing, grep the file's bullet for the passage.
      - A finding a sub-bullet already names is traced to it, and no text is added.
    - A sub-bullet leaves in the commit that fixes its text.
- **DOC-6 KB-current:** a behavior/config change updates the relevant KB doc in the same change.
  - A doc hunk describing a code change rides **in that commit**, never in a trailing `[doc]`
    follow-up, which cannot stand alone for revert or cherry-pick.
    - A template upgrade is the one exception: its merged docs ride in one `[doc]` commit behind
      its tool commits (author decision 2026-11-23).
      - Why: the merged docs describe every tool set at once, across the same files, so no hunk
        can ride with one tool commit.
  - A doc claim also lands no earlier than the thing it names.
  - The check is per hunk, in both directions:
    - still true after reverting only the code commit (then misplaced);
    - true at the commit it rides in (`git grep <symbol> <sha>`, never the working tree).
- **DOC-7 Decisions land in open-questions.md:** the decision plus its why, at decision time.
  - A new decision amends, in the same commit, every earlier row it makes false, with the marker
    the log's intro shows.
  - A decision row lands in the commit that applies its consequence, or its `→` names the later
    strand or backlog item that will apply it.
    - Why: a row that states a consequence its own commit lacks is false at that commit (DOC-6).
- **DOC-8 No backlog IDs outside the backlog:** identify things by symbol/behavior/concept.
  - A shortcode may appear only as an optional pointer to a still-live item.
  - Lead with the distinguishing noun, then carry the shortcode once in parentheses: words first,
    pointer kept.
    - Why: so this rule and DOC-5 compose instead of one deleting what the other needs.
- **DOC-9 Language & tone:** English.
  - Plain, technically correct wording.
  - No inflated language.
  - No em dashes or spaced dash-asides (restructure instead).
  - Ordinary hyphens fine.
- **DOC-10 Stable anchors:** keep §-numbers and heading text stable (they are link targets).
  - When a heading must change, update every inbound link in the same change.
- **DOC-11 Verify a table edit by re-reading the file, not the diff.**
  - Why: a diff shows the text you added, not the missing newline that swallowed it: a row appended
    onto the previous row's line renders as its trailing cells and the entry is effectively absent.
  - After editing a Markdown table, read the file's rows back.
  - After editing a Markdown table, assert no blank line sits between two rows of one table.
  - The table-split check covers every `.md` the strand touches, not only the table a finding named.
  - After appending to a cell or striking from it, read the join of the old and the new text in
    the file.
    - After a strike, read the whole sentence.
  - Every row has as many cells as the header.
  - A `|` inside a cell is written `\|`, inside backticks too.
  - An unescaped `|` splits the cell.
