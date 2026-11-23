# {{PROJECT_NAME}}: Knowledge Base

The project's persistent knowledge base: any human or AI agent gets up to speed **without
re-scanning the whole codebase**.

## Step 0: onboarding a tool (per-tool entry file)

- This KB is **tool-neutral and the single source of truth**.
- Any human or AI tool reads it directly.
- A tool that auto-loads a native entry file gets a thin wrapper that *points* here (Claude Code
  loads [`../../CLAUDE.md`](../../CLAUDE.md)).
  - The rules themselves are never duplicated into the wrapper.
- **No native entry file?** Build a thin wrapper from
  [agent-entry-template.md](agent-entry-template.md):
  - copy its SHARED section;
  - fill its PROJECT CHOICES section with this project's choices;
  - fill the TOOL-SPECIFIC section with only that tool's mechanics;
  - record `Based on agent-entry-template version: N`.
- **Already have a wrapper?** At session start, **quick-check** that its SHARED section still
  matches the template's current `Template version`.
  - On a version bump, **re-sync** the wrapper's SHARED section.
  - The version marker is the signal.
  - The `pre-commit` leg that compares the wrapper's SHARED section with the template's refuses a
    commit that lets them drift (hooks/README.md).

## What this project is, in one sentence

{{PROJECT_ONE_LINER}}

> **ADAPT:** one sentence stating what the software does, for whom, and what problem it solves or
> what manual process it replaces.

## Read in this order

- Docs 01-05 and 07 ship as **skeletons**: fill them in for your project.
  - Doc 04 keeps the parts that carry the template's rules as shipped; its ADAPT note names them.
- Docs 06 and 08-12 carry the reusable process knowledge and normally need only light adaptation.
- Doc 13 is a dated record each adopter re-runs and replaces.

| # | Doc | What you'll learn |
|---|-----|-------------------|
| 01 | [project-overview.md](project-overview.md) | The product, who it's for, the problem it solves (skeleton: fill in your product's purpose, users, and scope) |
| 02 | [domain-model.md](domain-model.md) | The core domain concepts and how they relate (skeleton: fill in your entities, value objects, and the key invariants/math) |
| 03 | [architecture.md](architecture.md) | Layers, data flow, module boundaries (skeleton: fill in your layering, e.g. ports & adapters, and any known gaps) |
| 04 | [build-run-deploy.md](build-run-deploy.md) | How to build, configure, run, and deploy (skeleton: fill in build commands, config/secrets handling, endpoints, scheduled jobs, release & deploy) |
| 05 | [testing.md](testing.md) | Test inventory, coverage map, biggest gaps (skeleton: fill in your test layout and where coverage is thin) |
| 06 | [git-state.md](git-state.md) | Git conventions & the rule that **git is the source of truth** for history (docs don't duplicate it) |
| 07 | [glossary.md](glossary.md) | Domain glossary (skeleton: fill in domain terms, especially where the domain language differs from English) |
| 08 | [engineering-handbook.md](engineering-handbook.md) | **How the code should look & how we develop**: clean code, SOLID, architecture rules, TDD, BDD, commits, Definition of Done |
| 09 | [working-with-ai-agents.md](working-with-ai-agents.md) | The **AI-assisted workflow**: memory layers, session playbook, sub-agents, asking style |
| 10 | [multi-agent-playbook.md](multi-agent-playbook.md) | The **agent team**: Planner (you+me) · Implementer · Falsifier panel · Doc reviewer · Reviewer. Pipeline, roles, how to invoke |
| 11 | [dev-environment.md](dev-environment.md) | **The pinned dev environment**: the devcontainer and native-toolchain variants, the environment rule for agents, the toolchain's pins & upgrade procedure |
| 12 | [agent-entry-template.md](agent-entry-template.md) | **Per-tool session-bootstrap template**: the SHARED rules + TOOL-SPECIFIC skeleton each tool's entry file (e.g. `CLAUDE.md`) is built from, plus the template version |
| 13 | [permission-probe-record.md](permission-probe-record.md) | **What the permission rules did on the build in use**: the headless rule and workflow probes, a dated table per command of refused, prompted, blocked or ran, and the normal workflow's steps in auto mode |

- Projects can add further domain docs to this folder as needed and slot them into the reading
  order.
  - Example: the project this template was extracted from had a doc describing the legacy
    spreadsheet its software replaced: columns, formulas, and the gap between sheet and software.
  - A project with a UI surface gets `frontend.md`, the UI doc `falsifier-frontend` checks against.
    - `/adopt` creates it from `.claude/skills/adopt/skeletons/frontend.md`.
    - Later, a session copies it from `.template/baseline/.claude/skills/adopt/skeletons/` and
      fills in the project name.

## Living documents (outside this folder)

- **[../../CLAUDE.md](../../CLAUDE.md)**: the Claude Code per-tool entry file, built from
  [agent-entry-template.md](agent-entry-template.md) and auto-loaded each session.
  - Other tools build their own equivalent from the template ("Step 0" above).
- **[../../.claude/agents/](../../.claude/agents/)**: the subagent definitions (implementer,
  scenario falsifier, stakeholder lens, code falsifiers, doc-reviewer, reviewer).
- **The `.feature` files**: the executable specification (handbook §5).
  - They are the author's primary review artifact.
  - They live where the acceptance runner expects them.
  - [testing.md](testing.md) names the place.
- **[../backlog.md](../backlog.md)**: the "▶ Current status / next action" section every session
  reads first, plus the prioritized items (PO / architect / engineering hats).
  - With a ticket system the items live there, and what this file keeps: DOC-5 below.
  - Temporary personal tasks then go to the gitignored `docs/backlog.local.md`.
- **[../open-questions.md](../open-questions.md)**: open questions & decisions log.
- **[../process-learnings.md](../process-learnings.md)**: where the process slipped & how we
  prevent it.

**Where knowledge is canonical when the team has a ticket system and a wiki** (Jira and Confluence,
GitLab issues and its wiki, or equivalents; recorded at adoption in open-questions.md):

- The **repository KB stays canonical for engineering knowledge** that must travel with the code:
  architecture, build/run/deploy, testing, the handbook, and the decisions that shape code.
  - It is what a session reads without network access.
- The **wiki holds stakeholder documentation**: product overview, requirements, release notes,
  meeting notes.
  - The repo links to it, never copies it.
- The **ticket system holds the work**.
  - `backlog.md` keeps the status section and points at it.
- Agents read tickets and wiki pages through the tool's connector when the project has one
  configured.
  - Without a connector the author pastes what a task needs.

**Further living docs** pay off once the backlog outgrows one screen; add them then, not before:

- `roadmap.md`: the cut into milestones, their **order**, and the milestone-level "done".
  - Why: a backlog answers *what*, not *in which order and why that order*.
- `risks.md`: what can hurt the project or its data, with likelihood/impact and the countermeasure.
- Both start from the template's skeletons, which carry their rules.
  - `/adopt` creates them from `.claude/skills/adopt/skeletons/` when the backlog already outgrows
    one screen at adoption.
  - Later, a session copies them from `.template/baseline/.claude/skills/adopt/skeletons/` and fills
    in the project name.
- `adr/`: one file per architecture decision, MADR-lite (status, context, options or decision,
  consequences), while open-questions.md stays the dated index.
  - Only for a project **without** a wiki.
  - With a wiki, the analyses live there and open-questions.md links to them.
  - A superseded ADR is **removed** or marked superseded with a pointer; git is the history.

## Doc conventions (the checklist; the `doc-reviewer` agent enforces it)

- Applies to every `*.md` in the repo (root, `docs/`, `.claude/`).
- Findings cite rule IDs.
- A named limit: no lint ships for the mechanical rules (table splits, cell counts, an unescaped
  `|`, dashes, a reused ID), so the `doc-reviewer` checks them by eye.
- This is the repo's shared style.
- A contributor's personal chat-tone preferences live in their own tool's user-level config, not in
  the repo.

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
  - One fact per bullet, and per sub-bullet.
  - No filler.
  - No restating context the reader already has.
  - Prose only where a causal chain genuinely needs it.
  - The rationale of a rule is a `Why:` sub-bullet of its own, which DOC-2 keeps.
  - Exception: a filing cell, a table cell that files several findings at once, joins its facts with
    `;`, each with its location.
    - So does the lows row of a passing round ([engineering-handbook.md](engineering-handbook.md)
      §9).
    - A decision row that excepts a text is recorded at decision time (DOC-7).
  - From the third gate round on, a remaining DOC-1 finding is a low: listed, never a FAIL.
- **DOC-2 Keep the why:** a rule carries at most a one-line rationale.
  - Never strip it entirely: rules without a why get misapplied.
  - The rationale has to be true: a worked example cited in a doc is checked against the code before
    it is written down.
- **DOC-3 Git is history:** docs state current truth only.
  - No changelogs.
  - No commit narration.
  - No was/now diffs.
  - No session references.
  - No dated review reference, and a probe reference only under the second exception below.
  - Exception: open-questions.md, process-learnings.md and the dated probe record
    ([permission-probe-record.md](permission-probe-record.md)) record events by date.
    - DOC-3 does not apply to their rows.
  - Second exception: a measurement bound to a version may stand in any doc, either with the version
    in the sentence ("on Claude Code `<version>`") or by citing a row of the dated probe record,
    whose section names the version.
- **DOC-4 Single home:** each rule/fact lives in exactly one doc.
  - Everything else cross-links.
  - Why: duplication drifts.
  - Exception: `.claude/agents/*.md` are **prompts, not docs**, and an agent only ever reads its own
    brief, so an operative instruction may repeat verbatim across briefs.
    - Its rationale still lives only in the KB.
  - Second exception: `CLAUDE.md` alone, the wrapper the auto-mode classifier reads as context
    (working-with-ai-agents.md, "Where auto-mode rules live"), may restate the scratch worktree
    names and commands literally, and under "Project choices" each slot's value.
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
  - A row that a session on a supervised machine files carries a seat marker after its bold
    title: `(seat: <name>)`.
    - It names the agent seat where the seat can take the row as a strand of its own from `main`.
    - It names the supervised machine where the row needs that machine.
    - `/plan-next` and `/continue` read the marker on the seat.

    > **ADAPT:** put in the project's names for the agent seat and the supervised machine.
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
  - With a ticket system the items live there.
    - `backlog.md` keeps only the status section and its pointers: the items,
      "Considered and rejected" and "Doc debt".
    - The removal duty then covers local shortcodes only.
    - "Considered and rejected" points at the tickets closed as rejected.
    - Doc debt goes to one standing ticket per file, which the "Doc debt" section points at.
- **DOC-6 KB-current:** a behavior/config change updates the relevant KB doc in the same change.
  - A doc hunk describing a code change rides **in that commit**, never in a trailing `[doc]`
    follow-up.
  - A doc claim lands no earlier than the thing it names.
  - The check is per hunk, in both directions:
    - still true after reverting only the code commit (then misplaced);
    - true at the commit it rides in (`git grep <symbol> <sha>`, never the working tree).
- **DOC-7 Decisions land in open-questions.md:** the decision plus its why, at decision time.
  - A new decision amends, in the same commit, every earlier row it makes false, with the marker
    the log's intro shows.
  - A decision row lands in the commit that applies its consequence, or its `→` names the later
    strand, backlog item or ticket that will apply it.
    - Why: a row that states a consequence its own commit lacks is false at that commit (DOC-6).
- **DOC-8 No backlog IDs outside the backlog:** identify things by symbol/behavior/concept.
  - A shortcode may appear only as an optional pointer to a still-live item.
  - Lead with the distinguishing noun, then carry the shortcode once in parentheses: words first,
    pointer kept.
    - Why: so this rule and DOC-5 compose instead of one deleting what the other needs.
  - A ticket key of the team's ticket system stays resolvable after the item closes and may stand
    as a pointer without the removal duty.
- **DOC-9 Language & tone:** English.
  - Plain, technically correct wording.
  - No inflated language.
  - No em dashes or spaced dash-asides (restructure instead).
  - Ordinary hyphens fine.
- **DOC-10 Stable anchors:** keep §-numbers and heading text stable (they are link targets).
  - When a heading must change, update every inbound link in the same change.
- **DOC-11 Verify a table edit by re-reading the file, not the diff.**
  - Why: a diff shows the text you added, not the missing newline that swallowed it.
  - After editing a Markdown table, read the file's rows back.
  - The table-split check (no blank line between two rows of one table) covers every `.md` the
    strand touches, not only the table a finding named.
  - After appending to a cell or striking from it, read the join of the old and the new text in
    the file.
    - After a strike, read the whole sentence.
  - No blank line sits between two rows of one table.
  - Every row has as many cells as the header.
  - A `|` inside a cell is written `\|`, inside backticks too.
  - An unescaped `|` splits the cell.
- **DOC-12 A backticked type name names a real type:** in the KB, a type name that opens a code
  span names a type that the project, its dependencies or the platform declare.
  - Why: a renamed or deleted type leaves a citation that still reads as true.
  - A name counts as a type name when it starts with a capital, has a lowercase letter, and either
    has two or more capitalised humps (`TransferType`) or heads a dotted access (`Date.now()`).
  - What the rule leaves unchecked:
    - a name of one hump outside a dotted access, as most entities have (`Order`);
    - a qualified name (`com.example.OrderBook`), which opens with a lowercase word.
  - A name the KB uses only as an example stands on the example list below, and a name that looks
    like a type but is not one stands on the stop list; each entry carries its reason, and its name
    counts as a type name by the rule above.
  - Options with a dash and commands stay backticked (`-o Name=value`, `git log`): they are no type
    names.
  - Where the project's test sources hold a guard of this rule (the java-gradle preset ships one), a
    commit that touches the KB runs it, a docs-only commit included.
    - Why: a KB edit alone can break the guard, and a commit that skips it hands the break to the
      next build.
  - Where they hold none, the `doc-reviewer` checks the rule by eye on the KB lines a strand writes.
  - Example list:
    - `PriceCalculationAcceptanceTest`: the handbook's example of a test named after its behavior.
    - `TransferType`: the glossary's example of the code name a domain term maps to.
  - Stop list:
    - `D2AcceptanceTest`: the handbook's counter-example of a test named after a backlog ID.
    - `JavaCompile`: a Gradle API class, on the build script's classpath only.
    - `EnterWorktree`: a Claude Code tool name.
    - `TestCase`: Python's unittest base class, which the playbook's test readers name.

## Placeholder key (template adoption)

- Three tokens (with the agent-seat module also `{{REPOSITORY_URL}}`, `/adopt` step 8) are
  resolved once, at adoption, by a global search-replace.
  - The `/adopt` skill automates it (its step 8).
  - The stack presets in the template's `stacks/` folder supply the stack value.
- A fourth token, `{{BASE_PACKAGE}}`, stands only in the java-gradle preset's Java files (the
  acceptance module's and the KB guard's).
  - It is no global search-replace: `/adopt` resolves it in its step 7, in the copies it makes in
    the project's test tree.
- Build and test commands are not tokens: every project exposes them as `./verify all` and
  `./verify focus <path>` ([build-run-deploy.md](build-run-deploy.md)).
  - The stack lives inside that script.

| Token | Meaning |
|---|---|
| `{{PROJECT_NAME}}` | project name |
| `{{PROJECT_ONE_LINER}}` | one sentence: what it does, for whom, what it deliberately showcases |
| `{{TECH_STACK}}` | language and toolchain, e.g. "Java/Gradle" or "TypeScript/Node" |
| `{{BASE_PACKAGE}}` | the base Java package of the copied Java files, e.g. `com.example.shop` |

- The token check, in the adopted tree after the replace of `/adopt` step 8:
  `grep -rnE "[{][{][A-Z_]+[}][}]" .` finds hits only in the paths that step leaves out:
  `.template/`, `env/`, `stacks/`, `ci/`, `TEMPLATE-README.md`, `START-HERE.md`,
  `.claude/skills/adopt/` and the `remove` paths of its step 5.
  <!-- MODULE agent-seat -->
  - With the agent-seat module also the tokens `/adopt` step 7 left in `.github/`, where the
    landing App did not exist yet or its token action's release could not be read; each is a
    seeded backlog item.
  <!-- /MODULE agent-seat -->
  - The pattern matches a token alone, so the braces of another tool are no hits.
  - A hit elsewhere means the adoption pass is incomplete: finish the replace and work through
    `grep -rn "ADAPT:"`.
- `/adopt` deletes this section before the replace (its step 8): it holds the tokens as literal
  text.
