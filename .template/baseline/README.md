# AI-assisted development process template

A reusable, project-agnostic **process layer** for clean, test-driven, AI-assisted development with
Claude Code, extracted from a working project. It bundles a tool-neutral knowledge base (KB), a
multi-agent team (implementer, two falsifiers, reviewer), skills for adopting the template and for
pausing/resuming sessions, a committed permission policy, git enforcement hooks, two
dev-environment variants, and stack presets. Adopt it as the starting point for a new project or as
a drop-in for an existing repo.

## Contents

```
CLAUDE.md                          Claude Code entry file (thin wrapper, points into the KB)
README.md                          this file (replace with your project README after adoption)
TEMPLATE-VERSION                   the template version this tree ships (see "Updates" below)
.gitattributes                     LF normalization (handbook §7); mark your binaries
.gitignore                         ignores settings.local.json and other machine-local files
docs/
  backlog.md                       prioritized backlog + "current status / next action"
  open-questions.md                decisions log (ask, don't guess)
  process-learnings.md             where the process slipped and how it is prevented now
  knowledge-base/                  the single source of truth; start at its README.md (the map)
    project-overview.md, domain-model.md, architecture.md,
    build-run-deploy.md, testing.md, glossary.md          skeletons: fill in per project
    git-state.md                   git conventions (git is history, docs are knowledge)
    engineering-handbook.md        clean code, TDD/BDD, commit protocol, review gate, DoD
    working-with-ai-agents.md      memory layers, session playbook, permissions, token hygiene
    multi-agent-playbook.md        Planner/Implementer/Falsifier/Reviewer pipeline and gates
    dev-environment.md             the pinned dev environment (both variants, single-sourcing)
    agent-entry-template.md        per-tool entry-file template (tool-neutral bootstrap)
.claude/
  settings.json                    committed permission policy (portable allows + deny floor)
  agents/                          implementer, falsifier-domain, falsifier-robustness, reviewer
  skills/                          adopt, pause, continue, optimizeSettings
env/                               dev-environment variants; pick ONE at adoption, then delete
  devcontainer/                    variant A (recommended): fully virtualized, ships .devcontainer/
  native/                          variant B: pinned local toolchain via a version manifest
stacks/                            per-stack presets: java-gradle, js-ts-npm (full);
                                   python, csharp-unity, cpp (stubs); applied at adoption
hooks/                             commit-msg + pre-commit enforcement (see hooks/README.md)
user-scope/                        reference only, NOT committed to your repo (see below)
```

## Adoption path A: new project

1. Copy everything except `user-scope/` into your new repo root, `git init`.
2. Open the repo in Claude Code and run **`/adopt`**: it interviews you (name, one-liner, stack,
   environment variant, commands), applies the env variant and stack preset, resolves all
   placeholders, seeds the living docs, activates the hooks, and ends with the cleanup list and one
   confirmed commit.

Without Claude Code, the same pass by hand:

1. Pick the environment variant: copy the payload of [env/devcontainer/](env/devcontainer/README.md)
   or [env/native/](env/native/README.md) to the repo root and follow its README.
2. Apply your stack preset from [stacks/](stacks/README.md): placeholder values, settings.json
   allow block, pre-commit format gate, pinning spots.
3. Delete the KB map's "Placeholder key" section (an adoption-time aid), then search-replace the
   five placeholders (key: [KB map](docs/knowledge-base/README.md)); `env/`, `stacks/`, this
   README, and `.claude/skills/adopt/` need no replacing, they are deleted in step 7.
4. `grep -rn "ADAPT:" .` and work through every hit.
5. Fill the skeleton KB docs (01-05 and 07 in the KB map).
6. Activate the hooks: `git config core.hooksPath hooks` (the devcontainer's `postCreate` also does
   this; see [hooks/README.md](hooks/README.md)).
7. Delete `env/`, `stacks/`, and `.claude/skills/adopt/`; replace this README with your project's
   README. Keep `TEMPLATE-VERSION`.

## Adoption path B: drop-in for an existing repo

Copy these pieces: `CLAUDE.md`, the KB map (`docs/knowledge-base/README.md`; renumber and extend
its reading order as you merge), the KB process docs (06, 08-12 in that order), the living docs
(`docs/backlog.md`, `docs/open-questions.md`, `docs/process-learnings.md`), `.claude/`, `hooks/`,
`TEMPLATE-VERSION`, plus your `env/` variant's payload if you have no equivalent yet.
Merge the `.gitignore` and `.gitattributes` entries. `/adopt` drives this path too; the merging
judgment stays with you.

Merging with existing docs: keep what you have. If you already have an overview, architecture, or
testing doc, slot it into the KB map instead of duplicating it into the skeletons; renumber and
extend the map's reading order as needed. Use the skeletons (01-05, 07) only to seed docs you are
missing. Then run the same preset, placeholder, and ADAPT pass and activate the hooks as in path A.

## Environment variants (env/)

The process assumes a **pinned, single-sourced dev environment**
([dev-environment.md](docs/knowledge-base/dev-environment.md)); `env/` ships both implementations:

- **[devcontainer](env/devcontainer/README.md) (variant A, recommended):** fully virtualized, no
  toolchain on the host, agents run isolated inside the container.
- **[native](env/native/README.md) (variant B):** pinned local toolchain via a committed version
  manifest; no isolation, so the settings deny floor matters even more.

Pick one at adoption, record it in dev-environment.md, then delete `env/`.

## Stack presets (stacks/)

The core is stack-neutral; [stacks/](stacks/README.md) supplies the per-stack values in one place:
placeholder values, a settings.json allow block, lint/format and warnings-as-errors wiring, and the
toolchain pinning spots. Full presets exist for Java/Gradle and JS-TS/npm; Python, C#/Unity, and C++
ship as stubs to extend. Mixed projects combine two presets. Applied at adoption, then deleted.

## Placeholders and ADAPT notes

Five inline tokens (`{{PROJECT_NAME}}`, `{{PROJECT_ONE_LINER}}`, `{{TECH_STACK}}`, `{{BUILD_CMD}}`,
`{{TEST_CMD}}`) are meant for a global search-replace; the key lives in the
[KB map](docs/knowledge-base/README.md) and the stack presets supply ready values.

Everything else an adopter must rewrite is marked with a `> **ADAPT:** ...` blockquote;
`grep -rn "ADAPT:"` finds every spot. The main ones, grouped:

**KB map** (`docs/knowledge-base/README.md`)
- Title heading takes `{{PROJECT_NAME}}`; the one-sentence description takes `{{PROJECT_ONE_LINER}}`
  and states what the software does, for whom, and which problem or manual process it addresses.
- Once the skeleton docs (01-05, 07) are filled in, add a dated freshness note.

**Workflow docs** (`working-with-ai-agents.md`, `multi-agent-playbook.md`)
- Permissions & autonomy: state your threat model and name your project's secret files in the deny rules.
- Define the domain-falsifier lens (core calculations, invariants, parsing/IO fidelity) and keep it
  in sync with `.claude/agents/falsifier-domain.md`.
- Name the high-risk areas that escalate the review gate (examples given: money/valuation math,
  time/timezones, concurrency, data migration).
- List machine-specific environment gotchas (example kept: a host that does not surface Bash stdout,
  so redirect to a file and Read it).
- Commands: `{{TEST_CMD}}` for focused micro-step runs; `{{BUILD_CMD}}` for the full pre-gate build,
  the worktree cd-fallback example, and the stdout-redirect example.
- The origin project's KB-bootstrap story is kept, marked "(example)", as a generalized worked
  example of the gather, persist, ask, decide, build loop. Read it, no rewrite needed.

**Agent definitions** (`.claude/agents/`)
- All four role intros take `{{PROJECT_NAME}}`; the implementer's also takes `{{TECH_STACK}}`.
- Commands: `{{TEST_CMD}}` for focused runs and counter-tests (implementer, both falsifiers);
  `{{BUILD_CMD}}` for the full suite (implementer) and the full-build gate check (reviewer).
- falsifier-domain: replace the domain attack checklist with your project's invariants; the original
  bullets stay as "(example)" material.
- falsifier-robustness: extend the checklist with `{{TECH_STACK}}`-specific pitfalls; the JVM and
  scraper items are marked examples.
- implementer and reviewer: adjust the Environment section's host-specific quirks (the Windows
  stdout redirect is kept as an example).

`CLAUDE.md`, `hooks/pre-commit`, the `env/` payloads (`devcontainer.json`, `.tool-versions`), and
`.gitattributes` carry further ADAPT notes (conversation language, host-path allowlist, smell
patterns, format gate, pinned toolchain versions, binary types); the grep pass covers them. Plain
JSON cannot carry the marker, so `.claude/settings.json` has none: its adaptation (the preset allow
block, the `cd` paths, secret-file denies) is described in working-with-ai-agents.md and driven by
`/adopt`.

## Updates (TEMPLATE-VERSION)

Adoption is **copy once**, not a framework dependency. `TEMPLATE-VERSION` records the template
version your repo was built from and stays committed. To update later, diff a newer template
checkout against your tree, port what you want, and set the file to the new number. Re-sync is
manual and selective, the same mechanism the SHARED section of
[agent-entry-template.md](docs/knowledge-base/agent-entry-template.md) uses (its own version marker
covers only that section).

## user-scope/ is not part of the drop-in

`user-scope/` never lands in an adopting repo. It documents what belongs in each contributor's
`~/.claude` (personal global instructions, chat-tone preferences): see
[user-scope/README.md](user-scope/README.md). Repo docs stay team-facing; personal taste stays in
user scope.

## Settings split

[.claude/settings.json](.claude/settings.json) is committed: the portable policy, including the deny
floor (blocked git commands, secret files). `.claude/settings.local.json` is untracked and holds
machine-specific or taste rules; it may widen the allow list but must never weaken the deny floor.
The `optimizeSettings` skill helps promote proven local rules into the committed policy.

## Hooks

Enforcement is mechanical, not aspirational: `commit-msg` guards the commit-message format,
`pre-commit` scans for secrets and smells. Activate once per checkout with
`git config core.hooksPath hooks`; details and adaptation points in [hooks/README.md](hooks/README.md).

## Tool-neutral by design

The KB is the single source of truth and readable by any human or AI tool. `CLAUDE.md` is only the
Claude Code wrapper; other tools bootstrap their own thin entry file from
[docs/knowledge-base/agent-entry-template.md](docs/knowledge-base/agent-entry-template.md).
