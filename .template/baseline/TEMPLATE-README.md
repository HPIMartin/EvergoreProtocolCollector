# AI-assisted development process template

A reusable, project-agnostic **process layer** for clean, test-driven, AI-assisted development with
Claude Code, extracted from working projects. It bundles a tool-neutral knowledge base (KB), a
multi-agent team (implementer, a scenario falsifier, the code falsifier lenses for domain,
robustness and, where the project has a UI, frontend, doc-reviewer, reviewer), skills for adopting
and upgrading the template, for pausing and resuming sessions, for re-deriving the plan and for
catching up an existing codebase's scenarios, a committed permission policy, git
enforcement hooks, two dev-environment variants, five stack presets and a GitLab CI preset. Adopt it
as the starting point for a new project or as a drop-in for an existing repo.

## Contents

```
CLAUDE.md                          Claude Code entry file (thin wrapper, points into the KB)
START-HERE.md                      the recipient's quickstart; the entry point of a packaged zip
README.md                          this file (ships as TEMPLATE-README.md in a package)
TEMPLATE-VERSION                   the template version this tree ships (see "Updates" below)
assemble.sh                        packages this tree into one extractable archive (not shipped)
.gitattributes                     LF normalization (handbook §7); mark your binaries
.gitignore                         ignores settings.local.json and other machine-local files
docs/
  backlog.md                       prioritized backlog + "current status / next action"
  open-questions.md                decisions log (ask, don't guess)
  process-learnings.md             where the process slipped and how it is prevented now
  template-dev/                    this repository's own backlog, decisions, learnings (not shipped)
  knowledge-base/                  the single source of truth; start at its README.md (the map)
    project-overview.md, domain-model.md, architecture.md,
    build-run-deploy.md, testing.md, glossary.md          skeletons: fill in per project
    git-state.md                   git conventions (git is history, docs are knowledge)
    engineering-handbook.md        clean code, mandatory BDD + TDD, commit protocol, review gate, DoD
    working-with-ai-agents.md      memory layers, session playbook, permissions, token hygiene
    multi-agent-playbook.md        Planner/Implementer/Falsifier/Doc-reviewer/Reviewer pipeline and gates
    dev-environment.md             the pinned dev environment (both variants, single-sourcing)
    agent-entry-template.md        per-tool entry-file template (tool-neutral bootstrap)
.claude/
  settings.json                    committed permission policy (portable allows + deny floor)
  agents/                          implementer, falsifier-scenario, falsifier-domain, falsifier-robustness,
                                   falsifier-frontend (UI surface only), doc-reviewer, reviewer
  skills/                          adopt, pause, continue, plan-next, bdd-catch-up, template-upgrade
env/                               dev-environment variants; pick ONE at adoption, then delete
  devcontainer/                    variant A (recommended): fully virtualized, ships .devcontainer/
  native/                          variant B: pinned local toolchain via a version manifest
stacks/                            per-stack presets, each with a verify script: java-gradle,
                                   js-ts-npm, python, cpp, csharp-dotnet; a multi-stack skeleton;
                                   a self-test that proves the scripts' contract; applied at adoption
ci/                                the GitLab CI preset (runs ./verify all in the devcontainer image)
.template/                         after adoption: baseline/ holds the adopted payload for upgrades
hooks/                             the git gate: content-gate + format-gate checks, five commit-path
                                   hooks, a breach record for what git leaves ungated, and a
                                   self-test that proves it blocks (see hooks/README.md)
user-scope/                        reference only, NOT committed to your repo (see below)
```

## Handing the template on (assemble.sh)

```sh
sh assemble.sh [output-dir]
```

Builds `ai-dev-process-template-v<N>.zip` (or a `.tar.gz` where `zip` is not installed) that extracts
**straight into a project folder**, no wrapping directory. It archives from `HEAD`, so the hooks keep
their `100755` even on a host whose filesystem has no exec bit, and it refuses to package a dirty
working tree, a failing `hooks/self-test`, a failing `stacks/self-test` or a root living doc that
is not the pinned skeleton ("Developing the template itself" below): neither a silently incomplete
payload, nor a gate that does not block, nor verify scripts that do not work, nor another
project's records.
`user-scope/`, `docs/template-dev/` and `assemble.sh` are left out, and `README.md` ships as
`TEMPLATE-README.md` so extracting into an existing repo cannot overwrite that repo's own README.
The recipient's entry point is [START-HERE.md](START-HERE.md).

## Adoption path A: new project

1. Copy everything except `user-scope/` into your new repo root, `git init`.
2. Open the repo in Claude Code and run **`/adopt`**: it keeps the pristine payload as
   `.template/baseline/`, interviews you (name, one-liner, stacks and how they compose, the tools per
   stack, the one acceptance runner, environment variant, ticket system and wiki, commit-key
   pattern, CI and dependency bot, upgrade policy, conversation language), applies the env variant
   and the stack presets, installs `verify`, resolves the placeholders, seeds the living docs,
   activates and verifies the hooks, and ends with the cleanup list and one confirmed commit.

Without Claude Code, the same pass by hand:

1. Copy the extracted payload, unmodified, to `.template/baseline/` (the merge base for later
   upgrades), then pick the environment variant: copy the payload of
   [env/devcontainer/](env/devcontainer/README.md) or [env/native/](env/native/README.md) to the repo
   root and follow its README.
2. Apply your stack presets from [stacks/](stacks/README.md): the `{{TECH_STACK}}` value, the
   settings.json allow block, the `verify` script (one per stack, composed into one root script for
   several), the acceptance runner with its `@wip` filter, the content-gate config block, pinning
   spots. `git update-index --chmod=+x verify`.
3. Delete the KB map's "Placeholder key" section (an adoption-time aid), then search-replace the
   three placeholders (key: [KB map](docs/knowledge-base/README.md)); `.template/`, `env/`,
   `stacks/`, `ci/`, this README, and `.claude/skills/adopt/` need no replacing, they are deleted or
   kept pristine in step 7.
4. `grep -rn "ADAPT:" .` outside `.template/` and work through every hit; record the interview
   answers as decisions in `docs/open-questions.md`.
5. Fill the skeleton KB docs (01-05 and 07 in the KB map).
6. Activate the hooks: `git config core.hooksPath hooks` (the devcontainer's `postCreate` also does
   this; see [hooks/README.md](hooks/README.md)), then run `sh hooks/self-test` and require every
   case to pass: it is the acceptance test of the adaptation. Run `./verify format`.
7. Delete `env/`, `stacks/`, `ci/`, and `.claude/skills/adopt/` from the root (the baseline keeps
   them); replace this README with your project's README. Keep `TEMPLATE-VERSION` and
   `.template/baseline/`.

## Adoption path B: drop-in for an existing repo

Extract the archive into an empty directory first and copy these pieces: `CLAUDE.md`, the KB map
(`docs/knowledge-base/README.md`; renumber and extend its reading order as you merge), the KB
process docs (06, 08-12 in that order), the living docs (`docs/backlog.md`,
`docs/open-questions.md`, `docs/process-learnings.md`), `.claude/`, `hooks/`, `TEMPLATE-VERSION`,
the whole extracted payload as `.template/baseline/`, plus your `env/` variant's payload if you have
no equivalent yet. Merge the `.gitignore` and `.gitattributes` entries. `/adopt` drives this path
too: it analyses the tree first (stacks, tests, scenarios, CI, format and lint, the measured state of
the dependencies), asks whether `/bdd-catch-up` becomes task 0 before any feature work, and seeds a
modernisation track (vulnerability scan, lint and warnings as errors, CI, dependency upgrades,
containerisation) as backlog items or tickets in the order you choose; nothing of that track runs
during adoption. The merging judgment stays with you.

Merging with existing docs: keep what you have. If you already have an overview, architecture, or
testing doc, slot it into the KB map instead of duplicating it into the skeletons; renumber and
extend the map's reading order as needed. Use the skeletons (01-05, 07) only to seed docs you are
missing. Existing tools (a linter, a CI pipeline) are kept and tightened by default; replacing one
with the preset's is a backlog item with a rationale, never a silent swap. Then run the same preset,
placeholder, and ADAPT pass and activate the hooks as in path A.

## Environment variants (env/)

The process assumes a **pinned, single-sourced dev environment**
([dev-environment.md](docs/knowledge-base/dev-environment.md)); `env/` ships both implementations:

- **[devcontainer](env/devcontainer/README.md) (variant A, recommended):** fully virtualized, no
  toolchain on the host, agents run isolated inside the container.
- **[native](env/native/README.md) (variant B):** pinned local toolchain via a committed version
  manifest; no isolation, so the settings deny floor matters even more.

Pick one at adoption, record it in dev-environment.md, then delete `env/`.

## Stack presets (stacks/) and the CI preset (ci/)

The core is stack-neutral; [stacks/](stacks/README.md) supplies the per-stack values in one place:
the `{{TECH_STACK}}` value, a settings.json allow block, the `verify` script, lint/format and
warnings-as-errors wiring, the acceptance runner with its `@wip` filter, the probe location, the
toolchain pinning spots, the `outdated`/`vuln` commands and the dependency-bot snippets. Five full
presets: Java/Gradle, JS-TS/npm, Python, C++ and C#/.NET (with a Unity adapter addendum). A
multi-stack project combines presets into one root `verify` and keeps **one** acceptance runner per
system; a C++ system runs its scenarios out of process through Python. [ci/gitlab/](ci/gitlab/README.md)
is the one CI preset: the pipeline runs `./verify all` in the same image the devcontainer uses, so
local behaves like remote; other CI systems get presets on demand. Applied at adoption, then deleted
from the root (the baseline keeps them).

## Placeholders and ADAPT notes

Three inline tokens (`{{PROJECT_NAME}}`, `{{PROJECT_ONE_LINER}}`, `{{TECH_STACK}}`) are meant for a
global search-replace; the key lives in the [KB map](docs/knowledge-base/README.md). Build and test
commands are no tokens: every project exposes them as `./verify all` and `./verify focus <path>`
([build-run-deploy.md](docs/knowledge-base/build-run-deploy.md)), and the stack presets ship the
script.

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
- Commands need no rewrite: `./verify focus <path>` for focused micro-step runs and `./verify all`
  for the full pre-gate build are fixed; only the worktree cd-fallback example and the
  stdout-redirect example are host notes to check.
- The origin project's KB-bootstrap story is kept, marked "(example)", as a generalized worked
  example of the gather, persist, ask, decide, build loop. Read it, no rewrite needed.

**Agent definitions** (`.claude/agents/`)
- Every role intro takes `{{PROJECT_NAME}}`; the implementer's also takes `{{TECH_STACK}}`.
- Commands need no rewrite: every agent runs `./verify focus <path>` for focused runs and
  counter-tests and `./verify all` for the full suite and the gate check.
- falsifier-domain: replace the domain attack checklist with your project's invariants; the original
  bullets stay as "(example)" material.
- falsifier-robustness: extend the checklist with `{{TECH_STACK}}`-specific pitfalls; the JVM and
  scraper items are marked examples.
- implementer and reviewer: adjust the Environment section's host-specific quirks (the Windows
  stdout redirect is kept as an example).

`CLAUDE.md`, `hooks/format-gate`, `hooks/content-gate`, `hooks/self-test`, the `env/` payloads (`devcontainer.json`, `.tool-versions`), and
`.gitattributes` carry further ADAPT notes (conversation language, host-path allowlist, smell
patterns, format gate, matching self-test fixtures, pinned toolchain versions, binary types); the
grep pass covers them. Plain
JSON cannot carry the marker, so `.claude/settings.json` has none: its adaptation (the preset allow
block, the `cd` paths, secret-file denies) is described in working-with-ai-agents.md and driven by
`/adopt`.

## Updates (TEMPLATE-VERSION and the baseline)

Adoption is **copy once**, not a framework dependency, but it keeps what an update needs:
`TEMPLATE-VERSION` records the version the repo was built from, and `.template/baseline/` keeps the
**unmodified payload** of that version, committed. An update is then a real three-way merge per file
(baseline, new version, your adapted copy) instead of a guess. `/template-upgrade` drives it: extract
the new archive into `.template/incoming/` (gitignored), run the skill, decide the conflicts it
presents, answer the interview questions the new version added, and confirm the commits. The skill
re-syncs the SHARED section of `CLAUDE.md` (its own version marker covers only that section), runs
`hooks/self-test`, moves the new payload into place as the next baseline and sets the version. A
project adopted before the baseline existed bootstraps it from this repository's version tags
(`v1`, `v2`, ...) on its first upgrade; the origin project, which has no marker, starts from `v1`.

A released tag never moves: a fix on top of a release ships as a point version (`v4.1` after `v4`),
so a package always names the tree it came from and `/template-upgrade` sees a greater version.

## user-scope/ is not part of the drop-in

`user-scope/` never lands in an adopting repo. It documents what belongs in each contributor's
`~/.claude` (personal global instructions, chat-tone preferences): see
[user-scope/README.md](user-scope/README.md). Repo docs stay team-facing; personal taste stays in
user scope.

## Developing the template itself (docs/template-dev/)

This repository follows its own process, so it carries a backlog, a decisions log and process
learnings of its own. Those are records of **building** the template, not part of it:

- They live in [docs/template-dev/](docs/template-dev/) (`backlog.md`, `open-questions.md`,
  `process-learnings.md`, in the layout of their root counterparts) and never ship: `assemble.sh`
  leaves the folder out of the package.
- The root living docs are the **skeletons every adopter receives**: scaffolding and example rows
  only. `assemble.sh` pins each of the three by the blob hash it has at `HEAD` and refuses to
  package any other content, printing the diff against the pin: there is no notion of "record" for
  a stray line to slip past. A deliberate change to the scaffolding updates the pin in the same
  commit (`git rev-parse HEAD:<doc>` prints it), so a reviewer sees the edit and its blessing
  together.
- The split follows one rule: a **rule or convention** goes into the KB, this README or the hooks and
  ships; a **record of making it** (an item, a dated decision, a learnings row, a recorded mutation)
  stays in `docs/template-dev/`. Recurring learnings are promoted into the handbook as in any
  project; the promoted rule ships, the row does not.
- A session in this repository reads and writes the `docs/template-dev/` files wherever the process
  names the living docs; `CLAUDE.md` carries the pointer.

## Settings split

[.claude/settings.json](.claude/settings.json) is committed: the portable policy, including the deny
floor (blocked git commands, secret files). `.claude/settings.local.json` is untracked and holds
machine-specific or taste rules; it may widen the allow list but must never weaken the deny floor.
The template assumes Claude Code's auto mode as the session default, which a project cannot set for
itself: it is a user-level setting ([user-scope/](user-scope/README.md)). The committed allow list is
therefore short and holds only the carve-outs from the `ask` and `deny` tiers plus the project's own
loop (working-with-ai-agents.md, "Permissions & autonomy").

## Hooks

Enforcement is mechanical, not aspirational, and the suite is built around one finding: **git asks
`pre-commit` only for `git commit`**, so a hook that only prints beside a surviving commit is not a
gate. The checks live in `content-gate` (secrets, host data, smells, and every comment a diff adds)
and `format-gate` (the stack's fast format check); the hooks that run before a commit exists refuse
outright, and the ones that can only run after git moved the refs record the sha so the next commit
is refused. `commit-msg` guards the message format, and `self-test` asserts the resulting history
rather than the printed text, so a weakened gate cannot land. Activate once per checkout with
`git config core.hooksPath hooks`; details and adaptation points in [hooks/README.md](hooks/README.md).

## What the process insists on

Two rules are non-negotiable in this template and shape everything else:

- **BDD first, executable.** A feature with observable behavior starts with Gherkin scenarios in
  product language, gated by a scenario falsifier and **confirmed by the author as the complete
  acceptance** before any production code; committed `@wip`, driven green by TDD cycles, armed by
  the last one (handbook §5). Pure refactorings, `[doc]` commits and build/infra work are exempt,
  explicitly.
- **No comments, mechanically.** Knowledge goes into a name, a test name or the KB; the gate refuses
  a diff that adds a comment line to a source file, with no opt-out (handbook §3).

## Tool-neutral by design

The KB is the single source of truth and readable by any human or AI tool. `CLAUDE.md` is only the
Claude Code wrapper; other tools bootstrap their own thin entry file from
[docs/knowledge-base/agent-entry-template.md](docs/knowledge-base/agent-entry-template.md).
