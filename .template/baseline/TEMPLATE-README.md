# AI-assisted development process template

A reusable, project-agnostic **process layer** for clean, test-driven, AI-assisted development with
Claude Code, extracted from working projects. It bundles a tool-neutral knowledge base (KB), a
multi-agent team (implementer, a scenario falsifier, a stakeholder lens, the code falsifier lenses
for domain, robustness and, for a UI, frontend, doc-reviewer, reviewer), skills for adopting
and upgrading the template, for pausing and resuming sessions, for re-deriving the plan and for
catching up an existing codebase's scenarios, a committed permission policy, git
enforcement hooks, two dev-environment variants, five stack presets, a GitLab CI preset and six
optional modules. Adopt it as the starting point for a new project or as a drop-in for an existing
repo.

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
  template-dev/                    this repository's own backlog, decisions, learnings and doc
                                   lint, the maintainer's guide and the roll-out note (not shipped)
  knowledge-base/                  the single source of truth; start at its README.md (the map)
    project-overview.md, domain-model.md, architecture.md,
    build-run-deploy.md, testing.md, glossary.md          skeletons: fill in per project
    git-state.md                   git conventions (git is history, docs are knowledge)
    engineering-handbook.md        clean code, mandatory BDD + TDD, commit protocol, review gate, DoD
    working-with-ai-agents.md      memory layers, session playbook, permissions, token hygiene
    multi-agent-playbook.md        Planner/Implementer/Falsifier/Doc-reviewer/Reviewer pipeline and gates
    dev-environment.md             the pinned dev environment (both variants, the toolchain's pins)
    agent-entry-template.md        per-tool entry-file template (tool-neutral bootstrap)
    permission-probe-record.md     what the permission rules did, probe by probe (dated tables)
.claude/
  settings.json                    committed permission policy (portable allows + deny floor)
  agents/                          implementer, falsifier-scenario, falsifier-stakeholder,
                                   falsifier-domain, falsifier-robustness,
                                   falsifier-frontend (UI surface only), doc-reviewer, reviewer
  skills/                          adopt, pause, continue, plan-next, bdd-catch-up, template-upgrade
                                   (adopt/ also holds the module filter, its manifest and
                                   self-test, and the skeletons /adopt creates docs from)
env/                               dev-environment variants; pick ONE at adoption, then delete
  devcontainer/                    variant A (recommended): fully virtualized, ships .devcontainer/
  native/                          variant B: pinned local toolchain via a version manifest
stacks/                            per-stack presets, each with a verify script: java-gradle,
                                   js-ts-npm, python, cpp, csharp-dotnet; a multi-stack skeleton;
                                   a self-test that proves the scripts' contract; applied at adoption
ci/                                CI presets; gitlab/ runs ./verify all in the devcontainer image
agent/                             the agent's own tools: fold, which folds a worktree's last
                                   commit into the commit that owns it, and a self-test
.template/                         after adoption: baseline/ holds the adopted payload for upgrades
hooks/                             the git gate: content-gate + format-gate checks, the commit-path
                                   hooks, a breach record for what git leaves ungated, the sync of
                                   main with origin/main, and a self-test that proves it blocks
                                   (see hooks/README.md)
user-scope/                        reference only, NOT committed to your repo (see below)
```

The optional modules add to that tree:

<!-- MODULE gate -->
- `gate/` (gate module): the dossier check, the walk and its lock, the scenario check, the tip
  evidence and a self-test.
<!-- /MODULE gate -->
<!-- MODULE security-lens -->
- `gate/lens-request` and the `falsifier-security` agent (security-lens module): the lens and
  its request wrapper.
<!-- /MODULE security-lens -->
<!-- MODULE guard -->
- `agent/bash-shape-guard` and `agent/session-check` (guard module): the shell-shape guard and
  the session check.
<!-- /MODULE guard -->
<!-- MODULE agent-seat -->
- The agent-seat module:
  - `agent/push`, `agent/gh` and `agent/github-token`;
  - `land/`: the landing the workflow runs, its GitHub calls and a self-test;
  - `ci/github/`: the landing workflow and the rulesets;
  - `env/devcontainer/.devcontainer/agent/`: the agent image's build and its self-test;
  - `hooks/pre-push`.
<!-- /MODULE agent-seat -->

## Handing the template on (assemble.sh)

```sh
sh assemble.sh [output-dir]
```

Builds `ai-dev-process-template-v<N>-<short sha>.zip` (or a `.tar.gz` where `zip` is not installed)
that extracts **straight into a project folder**, no wrapping directory.

- Only where `HEAD` carries the tag `v<N>` is the archive named `ai-dev-process-template-v<N>.zip`:
  that one is the release, every other a build between releases, and it prints which of the two
  it built.

It archives from `HEAD`, so the hooks keep
their `100755` even on a host whose filesystem has no exec bit, and it refuses to package a dirty
working tree, a failing self-test (`hooks/`, `stacks/`, `gate/`, `agent/`, `land/` with its `jq`,
the module filter under `.claude/skills/adopt/`), a root living doc that is not the pinned
skeleton (`docs/template-dev/README.md`, "The rules of this repository"), a `TEMPLATE-VERSION`
that at `HEAD` is missing or holds anything but one line of digits and dots, or a `git archive`
that fails:
neither a silently incomplete payload, nor a gate that does not block, nor verify scripts that do
not work, nor another project's records.
<!-- MODULE agent-seat -->
It also refuses a failing agent image self-test, `env/devcontainer/.devcontainer/agent/self-test`.
<!-- /MODULE agent-seat -->
`user-scope/`, `docs/template-dev/` and `assemble.sh` are left out, and `README.md` ships as
`TEMPLATE-README.md` so extracting into an existing repo cannot overwrite that repo's own README.
The recipient's entry point is [START-HERE.md](START-HERE.md).

## Adoption path A: new project

The workflow runs without prompts only where Claude Code's auto mode is the session default, a
user setting no project can set: switch it on before you start ("Settings split" below).

1. Copy everything except `user-scope/` into your new repo root and `git init`
   ([START-HERE.md](START-HERE.md), step 1).
2. Open the repo in Claude Code and run **`/adopt`**: START-HERE.md, step 2, says what it does,
   and [.claude/skills/adopt/SKILL.md](.claude/skills/adopt/SKILL.md) holds its steps.

Without Claude Code, the same pass by hand. Each step names the step of the skill that holds its
detail:

1. Check that `sh`, `awk`, `git` 2.31 or newer, `mktemp` and a JSON check (`jq`, or
   `python3 -m json.tool`) run where you work; install nothing ad hoc
   ([dev-environment.md](docs/knowledge-base/dev-environment.md)). Skill step 1.
   <!-- MODULE agent-seat -->
   - With the agent-seat module also `jq`, `openssl`, `ssh-keygen` and `git` 2.32 or newer.
   <!-- /MODULE agent-seat -->
2. On a repo that holds a commit (path B), cut the strand first:
   `git switch -c claude/adopt-template` (handbook §7). Skill step 1.
3. Copy the extracted payload, unmodified and without `user-scope/`, to `.template/baseline/`: the
   merge base for later upgrades. A build skeleton is no part of it. By hand you copy it on
   either path; `/adopt` copies it itself. Skill step 1.
4. Pick the environment variant: copy the payload of
   [env/devcontainer/](env/devcontainer/README.md) or [env/native/](env/native/README.md) to the
   repo root, follow its README and record the choice in dev-environment.md; merge it into a
   `.devcontainer/` you already have. With the devcontainer the steps below run in the container.
   Skill step 4.
5. Pick the modules with the filter beside the skill, `<kept>` being their names:
   - `sh .claude/skills/adopt/module-filter list` prints each module with its preconditions;
   - `sh .claude/skills/adopt/module-filter check --keep "<kept>"` must print `ok`;
   - `sh .claude/skills/adopt/module-filter apply --keep "<kept>"` cuts the dropped modules' text
     and prints a `remove <path>` line per file of theirs: keep these for step 13;
   - on a refusal stop: never cut a block by hand. Skill step 5.
6. Fill what the kept modules leave open, and the strand prefix in `agent/fold`. Skill step 5.
7. Replace `/workspaces/{{PROJECT_NAME}}` in the path-bearing rules of `.claude/settings.json` by
   the root as the sessions see it, run `jq . .claude/settings.json`, and probe the rules as
   [permission-probe-record.md](docs/knowledge-base/permission-probe-record.md), "Method", says.
   Skill step 6.
8. Check that each stack's build exists (java-gradle: an executable `./gradlew`); create a missing
   skeleton now with the stack's own init command, or leave out that stack's preset and record it
   as a backlog item. Skill step 7.
9. Apply your stack presets from [stacks/](stacks/README.md): the settings.json allow block, the
   `verify` script (one per stack, composed into one root script for several), the acceptance
   runner with its `@wip` filter, the content-gate config block, pinning spots.
   `git update-index --chmod=+x verify`. Skill step 7.
10. Delete the KB map's "Placeholder key" section (an adoption-time aid), then search-replace the
    three placeholders (key: [KB map](docs/knowledge-base/README.md)) outside the paths skill
    step 8 names: they are deleted or kept pristine in step 13.
    <!-- MODULE agent-seat -->
    - With the agent-seat module also `{{REPOSITORY_URL}}`, the repository's URL, in the
      `source_url` line of the agent image's `build`.
    <!-- /MODULE agent-seat -->
11. `grep -rn "ADAPT:" .` outside `.template/` and `.claude/skills/adopt/` and work through every
    hit; write "Project choices" in `CLAUDE.md`, one value per slot of
    [agent-entry-template.md](docs/knowledge-base/agent-entry-template.md); record the answers as
    decisions in `docs/open-questions.md`; seed `docs/backlog.md` with the items left open, the
    skeleton KB docs (01-05 and 07 in the KB map) among them, which stay as shipped until then.
    Skill step 9.
    - build-run-deploy.md keeps the parts that carry the template's rules as shipped for good,
      which its ADAPT note names.
12. Activate the hooks (`git config core.hooksPath hooks`) and set the push default, both as
    [build-run-deploy.md](docs/knowledge-base/build-run-deploy.md), "Git hooks", says (the
    devcontainer's `postCreate` also does both; see [hooks/README.md](hooks/README.md)); stage the
    hooks, the agent tools and `verify` executable; then run the self-tests the skill's step 10
    names and require every case to pass: they are the acceptance test of the adaptation. Run
    `./verify format` and `./verify all`. Skill step 10.
13. Delete `env/`, `stacks/`, `ci/`, `START-HERE.md`, `.claude/skills/adopt/` and every `remove`
    path of step 5 from the root (the baseline keeps them); replace this README with your
    project's README. Keep `TEMPLATE-VERSION` and `.template/baseline/`. Skill step 11.
    <!-- MODULE agent-seat -->
    - With the agent-seat module, copy `ci/github/.github/` to the root's `.github/` before `ci/`
      goes, or the landing workflow and the rulesets go with it
      ([ci/github/README.md](ci/github/README.md), "Adoption").
    <!-- /MODULE agent-seat -->
14. `sh .template/baseline/.claude/skills/adopt/module-filter verify --keep "<kept>"` must exit 0;
    then commit, and on the strand of step 2 land it by your landing model after review (handbook
    §7). Skill step 11.

## Adoption path B: drop-in for an existing repo

Which situation you are in, including a repo with its own `CLAUDE.md` or knowledge base and one
already adopted: [START-HERE.md](START-HERE.md), step 1. Auto mode is a prerequisite here too
(path A, its first paragraph).

Extract the archive into an empty directory first and copy these pieces: `CLAUDE.md`, the KB map
(`docs/knowledge-base/README.md`; renumber and extend its reading order as you merge), the KB
process docs (06, 08 to 13 in that order), the living docs (`docs/backlog.md`,
`docs/open-questions.md`, `docs/process-learnings.md`), `.claude/`, `hooks/`, `agent/` (the fold
tool is core), the files of the modules you keep (the `path` rows of
`.claude/skills/adopt/module-manifest`), `TEMPLATE-VERSION`, plus your `env/` variant's payload if
you have no equivalent yet. Merge the `.gitignore` and `.gitattributes` entries.

`/adopt` drives this path too:

- Keep the extracted directory: `/adopt` asks for it.
- `/adopt` copies the whole payload from it as `.template/baseline/`, once; you copy no baseline.
- `/adopt` copies from that baseline whatever of `agent/` and the kept modules' files the root
  still lacks.
- Before its first edit the strand `claude/adopt-template` is cut, which carries the copied
  files and lands by your landing model after your review (handbook §7): `/adopt` runs
  `git switch -c` for it, and where the shell-shape guard refuses a switch in the primary
  checkout, you run that command.
- It analyses the tree first and puts each file that both the tree and the payload hold to you.
- A `.devcontainer/` you already have is kept: it merges the payload's lines into it with you.
- It asks whether `/bdd-catch-up` becomes task 0 before any feature work, and seeds a
  modernisation track as backlog items or tickets in the order you choose; nothing of that track
  runs during adoption.
- The merging judgment stays with you.

Merging with existing docs: keep what you have. If you already have an overview, architecture, or
testing doc, slot it into the KB map instead of duplicating it into the skeletons; renumber and
extend the map's reading order as needed. Use the skeletons (01-05, 07) only to seed docs you are
missing. Existing tools (a linter, a CI pipeline) are kept and tightened by default; replacing one
with the preset's is a backlog item with a rationale, never a silent swap. Without Claude Code,
run path A's manual pass from its step 1, with the strand of its step 2.

## Environment variants (env/)

The process assumes a **pinned dev environment**
([dev-environment.md](docs/knowledge-base/dev-environment.md)); `env/` ships both implementations:

- **[devcontainer](env/devcontainer/README.md) (variant A, recommended):** fully virtualized, no
  toolchain on the host, agents run inside the container (its limits: dev-environment.md,
  variant A).
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
<!-- MODULE agent-seat -->
- [ci/github/](ci/github/README.md) is the agent-seat module's preset: the landing workflow `/land`
  and the hub's rulesets, copied to `.github/` before `ci/` leaves the root.
<!-- /MODULE agent-seat -->

## Placeholders and ADAPT notes

Three inline tokens (`{{PROJECT_NAME}}`, `{{PROJECT_ONE_LINER}}`, `{{TECH_STACK}}`) are meant for a
global search-replace; the key lives in the [KB map](docs/knowledge-base/README.md). Build and test
commands are no tokens: every project exposes them as `./verify all` and `./verify focus <path>`
([build-run-deploy.md](docs/knowledge-base/build-run-deploy.md)), and the stack presets ship the
script.
<!-- MODULE agent-seat -->

With the agent-seat module `{{REPOSITORY_URL}}`, the repository's URL, joins them, in the
`source_url` line of the agent image's `build`.
<!-- /MODULE agent-seat -->

The java-gradle preset's copied Java files carry a fourth token, `{{BASE_PACKAGE}}`; `/adopt`
resolves it to your base package when it copies them (skill step 7).

Everything else an adopter must rewrite is marked with a `> **ADAPT:** ...` blockquote;
`grep -rn "ADAPT:"` finds every spot. The main ones, grouped:

**KB map** (`docs/knowledge-base/README.md`)
- Title heading takes `{{PROJECT_NAME}}`; the one-sentence description takes `{{PROJECT_ONE_LINER}}`
  and states what the software does, for whom, and which problem or manual process it addresses.

**Workflow docs** (`working-with-ai-agents.md`, `multi-agent-playbook.md`)
- Permissions & autonomy: state your threat model and name your project's secret files in the deny rules.
- Define the domain-falsifier lens (core calculations, invariants, parsing/IO fidelity) and keep it
  in sync with `.claude/agents/falsifier-domain.md`.
- Name the high-risk areas that escalate the review gate (examples given: money/valuation math,
  time/timezones, concurrency, data migration).
- Map the model tiers to the models your plan offers (example given: `sonnet` as the lens default).
- Fill in the read-path table, the paths each code lens reads, which picks the lenses of a delta
  round.
- Keep or change the round at which a scenario gate that does not converge ends
  (`engineering-handbook.md` §9).
- Keep or change the scenario calendar, the year, month and times of day the TIME rules use
  (`engineering-handbook.md` §5, "Points in time").
- Keep or delete the Definition of Done's item on texts that explain the product's rules to its
  users (`engineering-handbook.md` §8).
- The harness model of the acceptance scenarios: the shipped rule (each actor through its real
  surface, a fresh real service per scenario) or the in-memory alternative of handbook §5's ADAPT
  note; `/adopt` asks every project and applies the answer (skill step 9).
- List machine-specific environment gotchas (example kept: a host that does not surface Bash stdout,
  so redirect to a file and Read it).
- Commands need no rewrite: `./verify focus <path>` for focused micro-step runs and `./verify all`
  for the full pre-gate build are fixed; only the stdout-redirect example is a host note to
  check.
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
- falsifier-frontend: fill `docs/knowledge-base/frontend.md`, which `/adopt` creates from its
  skeleton for a project with a UI surface; the lens checks the UI against it.
- implementer and reviewer: adjust the Environment section's host-specific quirks (the Windows
  stdout redirect is kept as an example).

<!-- MODULE gate -->
**Gate module** (`gate/dossier-check`)
- Its ADAPT block holds the gate's values: the base ref of the landing model, test paths, runner
  lines, the readers on, the read-path table, the features directory, the walk's proof lines and
  failed-report path, the docs guard test, and the backlog's row and doc-debt shapes.
- `gate/walk`, `gate/tip-evidence` and `gate/scenario-check` read them from there.
- `/adopt` fills it in its step 5 ([stacks/README.md](stacks/README.md), "What a preset
  supplies"): the preset's `gate.env`, the base ref of your landing model, the UI's paths and,
  with the KB guard, the docs guard test; it then deletes the playbook's read-path table, whose
  place the block takes.
- The test paths, runner lines, read paths and backlog shapes stay yours to check.
<!-- /MODULE gate -->

**Fold tool** (`agent/fold`, core)
- Its ADAPT line holds the agent branch prefix of handbook §7; `/adopt` writes the strand prefix
  of your answer (skill step 5).

<!-- MODULE guard -->
**Guard module** (`agent/bash-shape-guard`, `agent/session-check`)
- Its ADAPT block holds the guard's values: the output target for a command that names git, the
  worktree root, the approvals switched on, the environment (devcontainer, native or inactive) and
  the heading of the KB section every refusal names.
- `/adopt` sets the environment from where Claude Code runs (skill step 5); the other values keep
  what ships, yours to change where the project moved what they name.
  - A named limit: `agent/self-test` takes a value of the block only where it is written
    `NAME = value`, with a blank on each side of `=`; one written without them turns most guard
    cases red.
- `volume_root` of `agent/session-check` names the worktree volume's root, and `/adopt` empties
  it where no such volume is kept.
- The guard needs `python3` 3.9 or newer with its standard library: the devcontainer keeps the
  pinned python feature, a native setup pins `python` in `.tool-versions`.
<!-- /MODULE guard -->

<!-- MODULE security-lens -->
**Security-lens module** (`gate/lens-request`, `falsifier-security`)
- `gate/authentication-surface`: the path prefixes of your authentication surface, one per line;
  `/adopt` writes it from your answer, and without it the security leg of the dossier check is off.
- `gate/lens-request`: its ADAPT line holds the host the lens's requests reach, `localhost` by
  default; `/adopt` sets it from where the sessions run (skill step 5).
- `falsifier-security`, yours to fill: the attack list, the way the tip starts (its image on a free
  port, or an embedded-server probe) and the form of the counter-tests.
<!-- /MODULE security-lens -->
<!-- MODULE agent-seat+security-lens -->
- With the agent-seat module the unattended seat overrides that host (multi-agent-playbook.md,
  "Cadence").
<!-- /MODULE agent-seat+security-lens -->

<!-- MODULE acceptance -->
**Acceptance module** (`stacks/java-gradle/acceptance/`, `stacks/java-gradle/grid/`)
- `/adopt` copies the files into your test tree, resolves the base package and sets the sweep
  form, `MARKER` or `ANCESTRY` (`ancestry` only in the devcontainer variant), from your answer
  (skill step 7); it copies the grid recipe on request only.
- Yours: the browser's locale, and the parallelism and the measured wall times of the runner
  (testing.md, "The acceptance runner").
- Yours: the pinned Selenium image tag of the grid recipe, the exclusive-resource tag, and the
  service-host alias that the browser start and the grid recipe share.
<!-- /MODULE acceptance -->

**KB guard** (`stacks/java-gradle/kb-guard/`, optional, java-gradle)
- The frontend source root the guard reads, in `KbCitationGuardTest` and in the `test` task's
  inputs, the same path in both; empty for a project without a frontend. `/adopt` copies the guard,
  resolves the base package and sets the root from your answer (skill step 7).
- Yours to extend: the guard's platform list in `KnownFrontendSymbols`, and the example and stop
  lists of DOC-12 in the KB map, which grow with your KB.

<!-- MODULE volumes -->
**Volumes module** (`devcontainer.json`, `.claude/settings.json`)
- `AGENT_WORKTREES` names the worktree root of handbook §7; the devcontainer's `containerEnv` sets
  it to `/worktrees`, which stays as shipped.
- The named volume `{{PROJECT_NAME}}-worktrees` is mounted on `/worktrees` (its limits:
  dev-environment.md, "Worktrees on a volume"); `/adopt` writes its name from the project name
  (skill step 5).
- The container user is `vscode`.
  - The `chown` in `postCreateCommand`, every `docker exec -u vscode` in the docs and briefs, and
    the report of `agent/session-check` name it.
  - Another image's user replaces it in those places.
- Git Bash on Windows: the ADAPT note of dev-environment.md, "Worktrees on a volume", which
  `/adopt` keeps on a Windows host only.
- `.claude/settings.json` carries a rule set of its own for the root, naming `/worktrees`
  literally (working-with-ai-agents.md, "Permissions & autonomy").
- With the guard module, `agent/session-check` holds `/worktrees` as its `volume_root` value: in a
  session without `AGENT_WORKTREES` its report says the guard approves nothing under that root.
<!-- /MODULE volumes -->

<!-- MODULE agent-seat -->
**Agent-seat module** (`agent/push`, `agent/gh`, `agent/github-token`, `land/`, `ci/github/`,
`hooks/pre-push`, `env/devcontainer/.devcontainer/agent/`)
- `/adopt` offers it only on your request; its preconditions and questions: skill step 3,
  question 13.
- The seat's environment sets `AGENT_SEAT`, `AGENT_APP_ID` and `AGENT_APP_KEY`, and
  `AGENT_SNAPSHOT_DIR` where the seat offers snapshots.
  - None of them stands in the repository.
- The `branch_prefix` ADAPT lines of `agent/push`, `land/land` and `hooks/pre-push` hold the agent
  branch prefix, the same value as `agent/fold`'s (skill step 5).
- The `mainline` ADAPT line of `hooks/pre-push` stays `main`: the module needs that name.
- The `bot_prefix` ADAPT line of `agent/gh` holds the dependency bot's branch prefix (skill
  step 5).
- The `required_check` ADAPT line of `land/land` is yours to fill once CI exists.
- The `required_check_app` ADAPT line of `land/land` names the app that run comes from,
  `github-actions` by default.
- `.github/` holds the landing workflow and the rulesets, copied from `ci/github/.github/` and
  filled (skill step 7).
- The landing App, its key, its environment and the repository's variables are yours to set up
  on GitHub, none of them in the repository's files ([ci/github/README.md](ci/github/README.md),
  "Adoption").
- `APPROVALS` in the ADAPT block of `agent/bash-shape-guard` holds `push` and `gh`.
- The backlog's seat marker (KB map, DOC-5) and the table in dev-environment.md, "Where agents
  run", take your names for the seat and the supervised machine (skill step 9).
- The agent image's pins, its repository URL and the stack's cache folders stand in ADAPT lines
  of its scripts (dev-environment.md, "The agent image"; skill steps 5 and 8).
- The seat machine and building the agent image on it are yours.
<!-- /MODULE agent-seat -->

`CLAUDE.md`, `hooks/format-gate`, `hooks/content-gate`, `hooks/self-test`, the `env/` payloads
(`devcontainer.json`, `Dockerfile`, `.tool-versions`), and `.gitattributes` carry further ADAPT
notes (conversation language, host-path and e-mail allowlists, comment-exempt tooling directories,
smell patterns, format gate, matching self-test fixtures, pinned toolchain versions, binary types);
the grep pass covers them. `devcontainer.json` also holds a module's lines between
`// MODULE <name>` fences (KB map, DOC-4), which `/adopt`'s module filter cuts for a module you
drop (skill step 5). Plain JSON cannot carry the marker, so `.claude/settings.json` has none: its
adaptation (the preset allow block, the path-bearing rules, the secret-file denies) follows the
ADAPT note "State your threat model" in working-with-ai-agents.md and is driven by `/adopt`.

## Updates (TEMPLATE-VERSION and the baseline)

Adoption is **copy once**, not a framework dependency, but it keeps what an update needs:
`TEMPLATE-VERSION` records the version the repo was built from, and `.template/baseline/` keeps the
**unmodified payload** of that version, committed. An update is then a real three-way merge per file
(baseline, new version, your adapted copy) instead of a guess.

- To start one, follow `START-HERE.md`, "A repo that holds `TEMPLATE-VERSION`";
  `/template-upgrade` says the rest.

A released tag never moves: a fix on top of a release ships as a point version (`v5.1` after `v5`),
so a package always names the tree it came from and `/template-upgrade` sees a greater version.

- Versions compare as numbers, field by field between the dots: `5` equals `5.0`, `4.10` is
  greater than `4.9`, and the shortest form is the one written.
- The template repository's author tags each released version it still serves: `v1`, `v2` and
  `v4.2`, set with the release of version 5, and from `v5` on every release. 3, 4 and 4.1 carry
  no tag: no project is known to stand on them.
  - Where a tag is missing, the commit that set that value in `TEMPLATE-VERSION` stands in
    (`/template-upgrade`, step 2).
- A tag's tree is the repository, not the package: the payload is that tree without `user-scope/`,
  `assemble.sh` and `docs/template-dev/`, with `README.md` named `TEMPLATE-README.md`
  (`/template-upgrade`, step 2).

## user-scope/ is not part of the drop-in

`user-scope/` never lands in an adopting repo, and no package holds it: the folder is found in the
template repository alone. It documents what belongs in each contributor's `~/.claude` (personal
global instructions, chat-tone preferences): see [user-scope/README.md](user-scope/README.md)
there. Repo docs stay team-facing; personal taste stays in user scope.

## Developing the template itself (docs/template-dev/)

How this repository builds the template, its records and its release steps never ship:
`docs/template-dev/README.md`, the maintainer's guide in the template repository, holds them.

## Settings split

[.claude/settings.json](.claude/settings.json) is committed: the portable policy, including the deny
floor (blocked git commands, secret files). `.claude/settings.local.json` is untracked and holds
machine-specific or taste rules; it may widen the allow list but must never weaken the deny floor.
The template assumes Claude Code's auto mode as the session default, which a project cannot set for
itself: it is a user-level setting. The committed allow list is
therefore short and holds only the narrow forms of commands a deny guards, the project's own loop
and the `Edit` rights the agent works with (working-with-ai-agents.md, "Permissions & autonomy").
The same section holds the examples for what sits outside the project: the user-level mode
setting, auto-mode entries and a managed policy that restates the floor one layer above it.

## Hooks

Enforcement is mechanical, not aspirational, and the suite is built around one finding: **git asks
`pre-commit` only for `git commit`**, so a hook that only prints beside a surviving commit is not a
gate. The checks live in `content-gate` (secrets, host data, smells, and every comment a diff adds)
and `format-gate` (the stack's fast format check); the hooks that run before a commit exists refuse
outright, and the ones that can only run after git moved the refs record the sha, so every commit
whose `HEAD` reaches a recorded commit is refused until a rewrite takes it out or the author
releases it. `commit-msg` guards the message format, and `self-test` takes the verdict of every
case that commits from the resulting history (such a case may also check the printed reason), so a
weakened gate cannot land by a commit or `git am`.
Activate once per checkout with
`git config core.hooksPath hooks`; details and adaptation points in [hooks/README.md](hooks/README.md).

## What the process insists on

Two rules are non-negotiable in this template and shape everything else:

- **BDD first, executable.** A feature with observable behavior starts with Gherkin scenarios in
  product language, gated by the scenario gate and **confirmed by the author as the complete
  acceptance** before any production code; committed `@wip`, driven green by TDD cycles, armed by
  the last one (handbook §5). Pure refactorings, `[doc]` commits and build/infra work are exempt,
  explicitly.
- **No comments, mechanically.** Knowledge goes into a name, a test name or the KB; the gate refuses
  a diff that adds a comment line to a source file, with no opt-out (handbook §3).

## Tool-neutral by design

The KB is the single source of truth and readable by any human or AI tool. `CLAUDE.md` is only the
Claude Code wrapper; other tools bootstrap their own thin entry file from
[docs/knowledge-base/agent-entry-template.md](docs/knowledge-base/agent-entry-template.md).
