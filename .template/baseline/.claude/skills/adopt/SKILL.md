---
description: Adopt the process template into this repo (analysis of the existing tree, interview, env variant, stack presets, verify script, ticket system, modernisation track, task 0, placeholder resolution, seeded docs, baseline)
argument-hint: [optional: answers you already know, e.g. "java-gradle + js-ts-npm, devcontainer, Jira PROJ, GitLab CI"]
---

You are adopting the AI-assisted development process template into this repository. The template
files are already extracted in (adoption path A or B per `TEMPLATE-README.md`). Your job: turn the
template into THIS project's process layer, in one reviewed pass.

Ground rules for the whole run: ask, don't guess (multiple-choice options, recommended one first);
detect what the tree already tells you and ask only for confirmation; at the end propose exactly ONE
one-line commit message and wait for confirmation; never push; hand deletions of project content to
the author as exact commands instead of running them yourself (your own worktrees and landed
branches are yours to clean up, git-natively, handbook §7). Nothing of the modernisation track is
executed during adoption: adoption sets the process up, the track runs as ordinary strands afterwards.

Answers the author already provided (skip re-asking those): $ARGUMENTS

## 1. Preflight and the baseline

- Confirm you are at the repo root: `TEMPLATE-VERSION` and `docs/knowledge-base/README.md` must
  exist. If `env/`, `stacks/` or `ci/` are already gone, those steps were done earlier; skip them.
- **Keep the pristine payload before touching anything.** Copy the extracted template files, exactly
  as they came out of the archive, into `.template/baseline/` (path A: the tree as it stands now,
  `user-scope/` excluded; path B: from the directory the author extracted the archive into, ask for
  it). `/template-upgrade` merges three ways against this copy later; without it an upgrade is a
  guess. The baseline is committed with the adoption.
- Path A (fresh repo from the template) or path B (drop-in into an existing repo)? Detect from the
  tree and confirm with the author. On path B, existing docs are kept and slotted into the KB map,
  never overwritten (`TEMPLATE-README.md`, path-B notes).

## 2. Analyse the existing tree (path B; a short version of it on path A)

Read the tree, not the whole codebase: manifests, configs and directory names answer most of this.
Present the findings as one table the author confirms or corrects; every cell you could not measure
says so.

| Topic | What to detect |
|---|---|
| Stacks | build manifests (`build.gradle*`, `pom.xml`, `package.json`, `pyproject.toml`, `CMakeLists.txt`, `*.sln`/`*.csproj`, `ProjectSettings/ProjectVersion.txt`), source trees per stack |
| Tests | unit test frameworks in use, test trees, whether `.feature` files and step definitions exist, and which runner |
| Format, lint, warnings | formatter and linter configs, whether warnings fail the build |
| CI | pipeline files (`.gitlab-ci.yml`, `.github/workflows/`, `azure-pipelines.yml`, `Jenkinsfile`) and what they run |
| Dependencies | the count of outdated dependencies and the vulnerability scan result, measured with the preset's `outdated`/`vuln` commands where the tool is present, otherwise "not measured yet" |
| Environment | a devcontainer, Dockerfiles, a version manifest, host-only tooling (an editor a container cannot run) |
| Documentation | README, docs folders, ADRs, a wiki or ticket system referenced anywhere |

## 3. Interview (one round; multiple-choice; prefilled from the analysis on path B)

1. **Project name** (`{{PROJECT_NAME}}`) and **one-liner** (`{{PROJECT_ONE_LINER}}`: what it does,
   for whom, what it deliberately showcases).
2. **Stacks**, multi-select from `stacks/`: java-gradle, js-ts-npm, python, cpp, csharp-dotnet
   (with the Unity adapter addendum where the project has a Unity view), other (then the author
   names the tools and the nearest preset is the shape to extend). Then **how they compose**: one
   build tool wraps the others (as Gradle can drive an npm frontend), or the root `verify` calls each
   stack in turn. A Windows-only project (MSVC, WPF, a full Unity project) must be named here; the
   default variants assume the devcontainer hybrid (`stacks/csharp-dotnet/README.md`).
3. **Per stack, the tools**: unit test framework, formatter, linter, package manager. Offer the
   preset's default first; on path B offer "keep the existing tool and tighten it" before "replace
   with the preset's", and record a replacement as a backlog item with its rationale, never as a
   silent swap.
4. **The acceptance runner, one per system** (handbook §5): the primary stack's runner (cucumber-jvm,
   cucumber-js, pytest-bdd, Reqnroll); for a C++ system pytest-bdd out of process unless the author
   wants the in-process alternative; for a Unity project Reqnroll against the engine-free core. Ask
   where the feature files live if the runner's convention does not fit, and the **scenario
   language**: English (default) or `# language: de` because the stakeholders read German.
5. **Environment variant**: devcontainer (recommended: isolation for agents) or native pinned
   toolchain (the `env/` READMEs state the trade-off).
6. **Ticket system and wiki**: none; Jira and Confluence; GitLab issues and its wiki; other. Ask
   for the project key, the spaces, and whether the tool has a connector configured for them. This
   decides where knowledge is canonical (KB README, "Where knowledge is canonical"), whether
   `docs/backlog.md` holds the items or only the status and a pointer with `docs/backlog.local.md`
   for temporary tasks, and whether `docs/adr/` is used (only without a wiki).
7. **Ticket keys in commit messages**: none; prefix `[KEY] `; suffix ` (KEY)`; and required or
   optional. The key pattern follows the ticket system (Jira `[A-Z][A-Z0-9]+-[0-9]+`, GitLab issues
   `#[0-9]+`). It becomes the `commit-msg` hook's setting.
8. **CI**: the GitLab CI preset (`ci/gitlab/`, runs `./verify all` in the devcontainer image); none
   yet; another system, which gets no preset now and is filed as a backlog item. **Dependency bot**:
   Dependabot, Renovate, none (the presets carry both snippets). **Upgrade policy**, per project:
   (A) security-driven plus one refresh pass per milestone, major jumps behind an acceptance net;
   (B) always latest through the bot's pull requests; (C) pinned, upgraded on need only.
9. **Conversation language** for the SHARED language line (code, comments, identifiers, docs and
   feature files stay English unless question 4 decided otherwise).
10. **Path B only, the modernisation track.** For each topic of the analysis table (vulnerability
    scan, format and lint with warnings as errors, CI, dependency upgrades, containerisation) offer
    keep / tighten the existing tool / adopt the preset's, and propose an order by risk. The result
    is a backlog track (or tickets), nothing is executed now.
11. **Path B only, the scenarios.** Ask explicitly whether `/bdd-catch-up` becomes **task 0**,
    before any feature work: (A) breadth first, one capability per run with happy path and main
    error paths (recommended); (B) exhaustive per capability as the follow-up once breadth exists.
    The code is the truth for what the software does; the author confirms what it should do.

## 4. Apply the environment variant

- Copy the chosen variant's payload from `env/<variant>/` to the repo root (devcontainer:
  `.devcontainer/`; native: `.tool-versions`) and work through that variant README's adoption
  steps. The devcontainer `name` takes the project name; pinned versions come from the stack presets.
- Both variants: verify the two `cd` allow rules in `.claude/settings.json` against the real
  absolute repo root (`pwd`). They ship as `/workspaces/{{PROJECT_NAME}}`, the devcontainer mount
  convention, which is only correct when the checkout folder name equals the project name; a
  native checkout usually lives elsewhere entirely.
- Record the choice in `docs/knowledge-base/dev-environment.md` (its first ADAPT note).

## 5. Apply the stack presets and compose `verify`

Per `stacks/<preset>/README.md`, for every chosen stack:

- Merge its allow block into `permissions.allow` of `.claude/settings.json`; never touch the deny
  floor. Validate: `jq . .claude/settings.json`. The committed `Bash(./verify:*)` rule covers every
  subcommand.
- Install `verify`: one stack copies `stacks/<preset>/verify` to the repo root; several stacks
  compose one root `verify` per `stacks/README.md` (dispatch `focus <path>` by path, run every stack
  for `all`, `outdated`, `vuln`). Set the executable bit in the index (`git update-index --chmod=+x
  verify`) and run `./verify format` once.
- Wire the acceptance runner of question 4 with the `@wip` filter the preset describes, and the
  feature-file location; add the runner's dependencies where the build already exists, otherwise
  record them as seeded backlog items.
- Configure the gate: `hooks/content-gate`'s config block (`production_trees`, `test_trees`,
  `print_allow_paths` for CLI output adapters), `hooks/commit-msg`'s ticket settings from question 7;
  the gate scans every stack family by default, so nothing else changes. `hooks/format-gate` calls
  `./verify format` and needs no adaptation.
- Where the project build already exists, apply the warnings-as-errors and pinning notes; where it
  does not, record them as seeded backlog items (step 7).
- Set up the **probe location** for agent throwaway code (build-run-deploy.md's ADAPT and the
  preset's section) and keep only the `.gitignore` probe entries of the chosen stacks, removing the
  scaffolding-prose lines that name the stacks not chosen (handbook §3's ignore-file carve-out).
- CI: copy `ci/gitlab/.gitlab-ci.yml` when chosen and work through its ADAPT notes; the dependency
  bot's config file from the preset's snippet when chosen.

## 6. Resolve the placeholders

- First delete the "Placeholder key" section from `docs/knowledge-base/README.md`: it is an
  adoption-time aid that documents the tokens as literal text and must not survive the replace.
- Then search-replace the three tokens with the interview values, EXCEPT in `.template/`, `env/`,
  `stacks/`, `ci/`, `TEMPLATE-README.md`, `START-HERE.md` and `.claude/skills/adopt/` (deleted or
  kept pristine in step 9).
- Verify: `grep -rn "{{" .` finds hits only inside those excluded paths.

## 7. Work the ADAPT notes and seed the living docs

`grep -rn "ADAPT:" .` outside `.template/` and split the hits:

- **Resolve now** (the interview answered them): the SHARED language line, changed identically in
  `CLAUDE.md` AND `docs/knowledge-base/agent-entry-template.md` (the `pre-commit` hook checks the
  identity); the one-liner spots; the devcontainer, hooks and settings values from steps 4-5; the
  acceptance runner, feature-file location and scenario language in `testing.md`; the `verify`
  subcommand table in `build-run-deploy.md`; the ticket system and canonical split in
  `open-questions.md`.
- **Leave in place** (project content the author fills over time): the skeleton KB docs (01-05,
  07), the domain-falsifier lens, the high-risk review areas, host quirks. Collect them as seeded
  backlog items or tickets instead of resolving them now.
- `docs/backlog.md`, "Current status": adopted template version N (read `TEMPLATE-VERSION`) on
  today's date; **task 0** where question 11 chose it (the `/bdd-catch-up` prompt, the chosen depth);
  then the modernisation track in the order of question 10; then the skeleton docs and the remaining
  ADAPT items. With a ticket system the items go there and the status section points at them.
- `docs/open-questions.md`: every interview answer as a dated decision with a one-line rationale
  (stacks and composition, tools kept or replaced, acceptance runner and language, environment
  variant, ticket system and canonical split, commit key pattern, CI, dependency bot, upgrade policy,
  conversation language).
- `docs/process-learnings.md`: stays an empty table.

## 8. Activate and verify

- Hooks: `git config core.hooksPath hooks` (the devcontainer `postCreate` also does it; run it for
  this checkout anyway), then verify with `git config core.hooksPath`.
- **Stage the hooks and `verify` executable BEFORE the self-test**, or its `100755` case has nothing
  to check and reports `skip`. On a fresh repo nothing is tracked yet, and a host without a
  filesystem exec bit (Windows) then records them `100644` on the first commit, whereupon git
  silently runs no hook at all on the next POSIX checkout: a gate that does not even narrate.

  ```sh
  git add hooks/ verify
  for h in hooks/* verify; do case "$h" in *.md) continue;; esac; git update-index --chmod=+x "$h"; done
  git ls-files -s hooks/ verify
  ```
- **Run `sh hooks/self-test` and require every case to pass.** This is the acceptance test of the
  adoption: it proves the adapted gate still blocks what it claims to block, in *this* repo. A FAIL
  means a config value and a fixture drifted apart; fix it before committing. Check syntax too:
  `sh -n` on each hook file and on `verify`, one invocation per file.
- **Run `sh stacks/self-test` too**, before `stacks/` is deleted: it proves the `verify` scripts keep
  the contract (exit codes, caller-relative paths, the executed-test proof, missing tools) against
  stub tools, so a copied and adapted script is checked the same way the hooks are.
- Run `./verify format`, and `./verify all` where the build exists; read the executed-test proof.
- Verify the rest of the pass: the step-6 grep is clean; `jq` clean on settings.json; relative doc
  links resolve (the root-relative links inside agent-entry-template.md are correct as documented
  there); the SHARED sections of `CLAUDE.md` and the template are identical.

## 9. Cleanup (author-run) and the one commit

- Hand the author exact removal commands for: `env/` (both variants; the choice is recorded),
  `stacks/`, `ci/`, `user-scope/` (if present), `START-HERE.md`, and `.claude/skills/adopt/` (this
  skill). All of them live on in `.template/baseline/` for the next upgrade. On path A also remind
  them to replace `TEMPLATE-README.md` (or the template `README.md`) with the project's own README.
  **Keep `TEMPLATE-VERSION` and `.template/baseline/`**: they are what `/template-upgrade` needs.
- Propose ONE one-line, present-tense commit message (e.g. `Adopt the AI-assisted development
  process template`; path B may prefer a `[doc]` tag when no build file changed), wait for the
  author's confirmation, commit. Never push.
- Point the author at the first session: `/bdd-catch-up` where task 0 was chosen, otherwise
  `/continue`.
