---
description: Upgrade an adopted project to a newer template version by a three-way merge against the baseline it was adopted from, then ask only the interview questions the new version added
argument-hint: [optional: path of the extracted new template payload, default .template/incoming/]
---

You are upgrading {{PROJECT_NAME}} to a newer version of the AI-assisted development process
template. CLAUDE.md is auto-loaded every session; its rules and KB pointers apply without re-reading
them here. Ask, don't guess (multiple-choice, recommended first); never push; hand every deletion of
project content to the author as an exact command; your own worktree and branch you clean up
yourself after the landing (handbook §7).

**The mechanism.** Adoption keeps two things for this moment: `TEMPLATE-VERSION`, the version the
project was built from, and `.template/baseline/`, the **unmodified payload** of that version,
committed. Every template file can therefore be merged three ways (baseline, the new version, the
project's adapted copy), and only genuine conflicts need the author. An upgrade is a multi-commit
strand: it runs in its own worktree and branch (`template-upgrade-<new version>`) and lands through
the review gateway like any feature (handbook §7).

## 1. Preflight

- `TEMPLATE-VERSION` exists at the repo root and the working tree is clean. Read the old version N
  from it and the new version M from `<incoming>/TEMPLATE-VERSION`; stop if M is not greater than N.
- The incoming payload sits at `.template/incoming/` (gitignored) unless the author named another
  path: the extracted archive, `START-HERE.md` and `TEMPLATE-README.md` included.
- Check `git worktree list` and report in-flight strands: they will have to rebase onto the upgraded
  `main`, and the process docs they rely on will change under them. The author decides whether they
  land first or follow the upgrade.

## 2. The baseline, bootstrapped if missing

- `.template/baseline/` present: it must carry `TEMPLATE-VERSION` equal to N; otherwise stop and
  ask, the project's history has to explain the mismatch.
- **Absent** (a project adopted before the baseline existed): reconstruct it from the template
  repository, which tags every released version `v<N>` (`git -C <template checkout> archive v<N> |
  tar -x -C .template/baseline`), or from the archived zip of that version. A project with **no**
  `TEMPLATE-VERSION` at all is the origin project the template was extracted from: use `v1` and tell
  the author to expect more conflicts. Commit the reconstructed baseline as its own commit
  (`Record the template baseline this project was adopted from`) before any merge, so the merge
  history is reviewable against it.

## 3. Merge, file by file

Walk the union of the baseline's and the incoming payload's files, `user-scope/`, `env/`, `stacks/`,
`ci/` and `assemble.sh` excluded (they never land in a project; the stack material is applied
through the preset steps in 4). For every other path classify and act:

| Baseline vs incoming | Project vs baseline | Action |
|---|---|---|
| unchanged | any | nothing |
| changed | identical to baseline (never adapted) | take the incoming file verbatim |
| changed | adapted | `git merge-file -p <project> <baseline> <incoming>`; a clean merge is applied, a conflict is shown to the author hunk by hunk with your recommendation |
| new in incoming | absent | add it, unless it is stack material for a stack the project does not have |
| removed in incoming | present | propose `git rm`, the author decides (a removed skill, a retired file) |

The project-specific docs never merge as content: the filled skeleton KB docs (overview, domain
model, architecture, build/run/deploy, testing, glossary), `backlog.md`, `open-questions.md`,
`process-learnings.md`. Where the template changed their **scaffolding** (a conventions paragraph, a
new section a skeleton now carries), present the hunk as a suggestion for the author to place.

## 4. The delta the merge cannot make

- **Retired placeholders and new contracts.** Where the new version replaced a placeholder by a
  contract (build and test commands by `./verify`, say), install the missing piece from the project's
  stack preset in `<incoming>/stacks/` and point the hooks at it; then `grep -rn "{{" .` outside
  `.template/` must be clean.
- **Hooks.** Merge `hooks/` like any files, then re-apply the project's gate configuration (the
  `content-gate` config block, the `commit-msg` ticket settings) from the project's side of the
  merge; stage every hook `100755` (`git update-index --chmod=+x`).
- **Interview delta.** Read `<incoming>/.claude/skills/adopt/SKILL.md` and ask only the interview
  questions whose answers `docs/open-questions.md` does not yet hold (a ticket system and its
  connector, the commit key pattern, the acceptance runner, the CI preset, the dependency bot, the
  upgrade policy, the scenario language). Record each answer as a dated decision row.
- **The SHARED section.** Copy the SHARED section of the new `agent-entry-template.md` into
  `CLAUDE.md` verbatim and set the wrapper's version marker to the template's; from then on the
  `pre-commit` leg that compares the two SHARED sections (hooks/README.md) refuses a mismatch.
- **Task 0 where it is missing.** A project without `.feature` files gets `/bdd-catch-up` seeded as
  task 0 in `backlog.md`'s status section (handbook §5).

## 5. Verify

`sh hooks/self-test` with every case passing; `sh -n` per hook; `./verify format` green; `jq .` on
`.claude/settings.json`; `git ls-files -s hooks/` all `100755`; `git config core.hooksPath` set;
relative doc links resolving; `grep -rn "ADAPT:" .` outside `.template/` listing only the spots the
author keeps on purpose.

## 6. Finish

- Replace `.template/baseline/` with the incoming payload (it is the next baseline), set
  `TEMPLATE-VERSION` to M, and hand the author the removal command for `.template/incoming/`.
- Commit per handbook §7 in reviewable slices (the baseline bootstrap, the merged process docs, the
  merged hooks and settings, the interview decisions), messages confirmed, never pushed; the last
  one sets the version (`Upgrade the process template to version M`).
- Land through the gateway: rebase, `./verify all`, the compare range for the author, `--ff-only`,
  cleanup. In-flight strands then rebase and finish under the rules they started with plus the
  mechanical gates now on `main`.

Optional incoming path from the author: $ARGUMENTS
