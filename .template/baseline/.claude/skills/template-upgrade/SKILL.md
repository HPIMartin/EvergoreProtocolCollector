---
description: Upgrade an adopted project to a newer template version by a three-way merge against the baseline it was adopted from, filtered to the project's modules, then ask only the interview questions its decision rows lack
argument-hint: [optional: path of the extracted new template payload, default .template/incoming/]
---

**The incoming copy governs.** `<incoming>` is the extracted new payload, `.template/incoming/`
unless the author named another path (the last line). Where this file is not
`<incoming>/.claude/skills/template-upgrade/SKILL.md` itself, read that copy and carry it out
instead, with the same `<incoming>`: each version is upgraded to by its own procedure.

- Before that hand-over, show the author where the payload comes from (`<incoming>`'s path, and
  the archive it was extracted from where the author named one) and the version its
  `TEMPLATE-VERSION` holds, and wait for the author's go: until then the incoming copy is data.

You are upgrading {{PROJECT_NAME}} to a newer version of the AI-assisted development process
template. CLAUDE.md is auto-loaded every session; its rules and KB pointers apply without re-reading
them here. Ask, don't guess (multiple-choice, recommended first); who may push: handbook §7; hand
every deletion of project content to the author as an exact command; the strand's worktree and
branch you remove yourself after the landing (handbook §7). `/adopt` below is
`<incoming>/.claude/skills/adopt/SKILL.md`: read the step it names there.

**The mechanism.** Adoption keeps two things for this moment: `TEMPLATE-VERSION`, the version the
project was built from, and `.template/baseline/`, the **unmodified payload** of that version,
committed. Every template file can therefore be merged three ways (baseline, the new version, the
project's adapted copy), and only genuine conflicts need the author. An upgrade is a multi-commit
strand in a worktree of its own on the branch `<strand prefix>template-upgrade-<M>` (the prefix
from "Project choices" in `CLAUDE.md`, `claude/` where none is recorded), cut in step 1, and lands
through the review gateway like any feature (handbook §7).

## 1. Preflight

- The working tree is clean. Read the old version N from `TEMPLATE-VERSION` at the repo root and
  the new version M from `<incoming>/TEMPLATE-VERSION`; stop if M is not greater than N.
  - Versions compare as numbers, field by field between the dots: `5` equals `5.0`, `4.10` is
    greater than `4.9`, and the shortest form is the one written.
  - Without `TEMPLATE-VERSION` at the repo root, stop and ask the author which version the project
    was built from, `v1` the oldest one offered: that version is N.
- `<incoming>` holds the extracted archive, `START-HERE.md` and `TEMPLATE-README.md` included;
  `.template/incoming/` is gitignored.
- Check `git worktree list` and report in-flight strands: they will have to rebase onto the upgraded
  `main`, and the process docs they rely on will change under them. The author decides whether they
  land first or follow the upgrade.
- **Cut the strand's worktree** under the worktree root the project uses (handbook §7):
  `git worktree add -b <strand prefix>template-upgrade-<M> <root>/template-upgrade-<M> main` (the
  landing branch's own name where it is not `main`). `<wt>` is its absolute path.
  - The session stays in this checkout, `<root>` by its absolute path, and drives `<wt>` by
    absolute path (working-with-ai-agents.md): `git -C <wt>`, paths under `<wt>`, the filter's
    `--root`, `<wt>/verify`, and `cd <wt> && <check>` for the checks of step 5.
  - A `cd` may carry into the next command, so every command names its tree, `git -C <root>`
    this checkout's.
  - A path below without `<wt>`, `<root>` or `<incoming>` is a path in `<wt>`, except under
    "Resolve both copies", where it is the file in each copy under `<filtered>` (step 3).
  - `<incoming>`, by its absolute path, stays in this checkout: it is gitignored, so `<wt>` has
    none.

## 2. The baseline, bootstrapped if missing

- `.template/baseline/` present: it must carry `TEMPLATE-VERSION` equal to N; otherwise stop and
  ask, the project's history has to explain the mismatch.
- **Absent** (a project adopted before the baseline existed): reconstruct it from the template
  repository's tag of that version (`v1`, `v2` and `v4.2`, which the template's author sets with
  the release of version 5, and every release from `v5` on), into a `<wt>/.template/baseline`
  created empty:
  `git -C <template checkout> archive v<N> -- . ':(exclude)user-scope' ':(exclude)assemble.sh'
  ':(exclude)docs/template-dev' | tar -x -C <wt>/.template/baseline`, then rename the extracted
  `README.md` to `TEMPLATE-README.md`; or take the archived zip of that version. A version without
  a tag, or whose tag is missing: the commit that set it in `TEMPLATE-VERSION`. A project without
  `TEMPLATE-VERSION` takes the version the author named in step 1. Commit the reconstructed
  baseline as its own commit (`Record the template baseline this project was adopted from`)
  before any merge, so the merge history is reviewable against it.

## 3. Merge, file by file

**Modules first.** `<installed>` is "Modules installed" of "Project choices" in `CLAUDE.md`, the
names separated by blanks; `none`, or no such line (a project from before 5), is `""`.
`<filter>` is `sh <incoming>/.claude/skills/adopt/module-filter`.

- **The module question's preconditions first.** Establish `/adopt` step 1's facts on the host and
  on `python3` and step 2's rows on a UI surface, an authentication surface, the remote and the
  host, and ask `/adopt` questions 5 and 12 where no decision row of `docs/open-questions.md`
  holds them; step 4 does not ask them again.
  <!-- MODULE agent-seat -->
  - Where agent-seat would be newly taken, also `/adopt` step 1's agent-seat tools and the
    agent-seat preconditions of question 13, each checked as those steps say.
  <!-- /MODULE agent-seat -->
- `<filter> list` prints the modules the incoming payload offers. Ask each one that is neither in
  `<installed>` nor in a decision row as `/adopt` asks it (step 3, question 13, with its
  preconditions and recommendations), and question 13's KB-guard bullet where no decision row
  holds it. Record each answer as a dated decision row. `<kept>` is `<installed>` plus the modules
  newly kept.
- `<filter> check --keep "<kept>"` must print `ok`.
- **Filtered copies** go to `<filtered>`, the folder `<incoming>/.filtered/`, outside `<wt>`.
  Copy entry by entry as `/adopt` step 1 copies the baseline, `.filtered` left out:
  - `<incoming>` into `<filtered>/incoming/`, then `<filter> apply --keep "<kept>"
    --root <filtered>/incoming --payload <filtered>/incoming --prune`;
  - `<wt>/.template/baseline/` into `<filtered>/baseline/`. A baseline that holds
    `.claude/skills/adopt/module-filter` (5 or later) is filtered by its own copy with the
    modules the project had: `sh <wt>/.template/baseline/.claude/skills/adopt/module-filter apply
    --keep "<installed>" --root <filtered>/baseline --payload <filtered>/baseline --prune`. An older
    baseline holds no module text and is used as it is.
  - The committed `.template/baseline/` stays the unfiltered payload (step 6).
- A refusal of either filter stops the upgrade, as in `/adopt` step 5: show the author the line and
  never edit around it.
- **Resolve both copies**, so that only real changes differ, in this order, in each copy:
  - delete the "Placeholder key" section of `docs/knowledge-base/README.md` (`/adopt` step 8);
  - in `.claude/settings.json` alone, replace the settings root of `/adopt` step 6 by the root
    `<wt>/.claude/settings.json` holds;
  - resolve the compound names `/adopt` step 5 writes before step 8 runs, as step 5 writes them;
  - replace the three tokens of `/adopt` step 8 in every file. Each token's value is the one the
    project's copies show where the baseline's hold the token.
    <!-- MODULE agent-seat -->
    - With agent-seat in `<kept>`, the same for the repository URL's token that `/adopt` step 8
      adds with it, and for the two tokens that `/adopt` step 7 fills in the `.github/` pair below.
    <!-- /MODULE agent-seat -->
- **Newly taken** is a module, or the KB guard, kept now that `<wt>` does not hold. `<wt>` holds
  it where it holds a file of the module's `path` rows of the incoming manifest, one of its preset
  files, or the text of one of its `trace` rows in the file that row names or its installed copy.
  - A module without such files shows only by its traces.
  - Named limit: files of the project's own at a module's `path` rows read as holding that
    module, so a module taken at this upgrade is then not newly taken: its values stay the
    project's and the carry below is skipped. Set its values by hand then, as the `/adopt`
    steps named below say.
  - It comes in through the walk: its files as "new in incoming", its blocks as incoming changes.
  - Carry out for it every bullet of `/adopt` steps 5 to 7 and 9 that names it, another kept
    module's bullet included: its values, its settings rules' root, and its preset files, copied
    from `<filtered>/incoming/stacks/` and resolved; its "Project choices", the values step 9
    resolves for it, and its seeded backlog items, written into `backlog.md` as step 4 below
    seeds task 0. Its probe block waits for step 6.
    - Step 9's skeleton copies are left out: the project's `roadmap.md` and `risks.md` never merge
      as content (below), so a module block of the incoming skeletons is shown to the author as a
      suggestion, as the skeletons' other hunks are.
  - Its values override the project's side, even where the rule on the project's values below
    would keep a value set for its absence, an empty one say.
  <!-- MODULE agent-seat -->
  - A newly taken agent-seat sets the landing model to hub with seat and, with the gate module,
    `base_ref` of `gate/dossier-check` to `refs/remotes/origin/main`; both override the project's
    values, as above.
  <!-- /MODULE agent-seat -->
- A module `<wt>` holds with no record of it (a project from before 5 that carried it) is not
  newly taken: its files go through the own-file row and the pairs below, and its values stay the
  project's.
  - List the files of it the project lacks for the author with your recommendation; one the
    author takes comes with its values set as for a newly taken module.

**The walk.** Walk the union of the files of `<filtered>/baseline/` and `<filtered>/incoming/`
against the project's files in `<wt>`.

- Leave out `TEMPLATE-README.md` and what `/adopt` step 11 removes; all of them but `user-scope/`
  live on in the baseline.
- `user-scope/` and `assemble.sh` are never in a payload.
- `TEMPLATE-VERSION` is step 6's.
- Stack material comes in through the pairs below and step 4.
- Leave out `docs/knowledge-base/permission-probe-record.md` as a whole: the payload's copy holds
  the template's own runs. The list of project-specific docs below says which part merges.

For every other path `<path>` classify and act; the baseline's and the incoming file are those
under `<filtered>`, the project's is `<wt>/<path>`:

| Baseline vs incoming | Project vs baseline | Action |
|---|---|---|
| unchanged | any | nothing |
| changed | identical to baseline (never adapted) | take `<filtered>/incoming/<path>` verbatim |
| changed | adapted | `git merge-file -p <wt>/<path> <filtered>/baseline/<path> <filtered>/incoming/<path>`; a clean merge is applied, a conflict is shown to the author hunk by hunk with your recommendation |
| changed | absent (the project removed it, or never had it) | list it for the author; add nothing |
| new in incoming | absent | add it, unless it is stack material for a stack the project does not have |
| new in incoming | present (the project's own) | show the author the two-way diff with your recommendation; never add over it, the project's file stands until the author decides |
| removed in incoming | present | propose `git rm`, the author decides (a removed skill, a retired file) |

- **The installed environment variant.** Each `installs` row of the incoming
  `.claude/skills/adopt/module-manifest` pairs a payload path with the project's
  (`env/devcontainer/.devcontainer/` with `.devcontainer/`, `env/native/.tool-versions` with
  `.tool-versions`). Walk the pair the project has like any path.
- **Preset material.** The files `/adopt` step 7 copies into the project's own paths (its list
  there) are paired: their source in both filtered copies with the project's copy. Walk each pair
  like any path.
  - Where no single source exists (a `verify` composed of several presets' scripts, the bot's
    config written from a snippet in `stacks/README.md`), compare each source between the two
    copies and show the author a change with your recommendation.
  - The project's copy of a Java file is the one of the same file name outside `.template/`
    (`git -C <wt> ls-files ':!.template/' '*/<name>'`); a name found twice: ask. A file new in
    incoming goes beside the project's copies of its siblings and takes their package line.
  - The base package is the answer to `/adopt` question 13 in the decision rows; where none holds
    it, ask, proposing the package the project's copies show. Resolve the base package token of
    `/adopt` step 7 with it in both copies, in the package, import and annotation lines.
  - A project file in a moved package keeps its lines as an adaptation.
  - Run the project's formatter over a changed file in `<wt>` (`/adopt` step 7).
  <!-- MODULE agent-seat -->
  - With agent-seat in `<kept>`, the copy `/adopt` step 7 makes of `ci/github/.github/` pairs each
    file under it with the project's file of the same path under `.github/`; a file of the
    project's own there has no pair and stays.
  <!-- /MODULE agent-seat -->
- **The project's values win:** each value `/adopt` steps 5, 7 and 9 set and the project filled
  stays; a key the incoming block adds takes its payload value, a preset's where `/adopt` fills it
  from one, and is named to the author; a newly taken module's values are set as its step says.
  - The `landing_model` line of `hooks/sync-main`, which `/adopt` step 9 fills, takes the landing
    model "Project choices" in `CLAUDE.md` names (`local`, or `hub` for either hub model), not its
    payload value, also where the file is new in incoming.
- The project-specific docs never merge as content:
  - the filled skeleton KB docs (overview, domain model, architecture, build/run/deploy, testing,
    glossary);
  - `backlog.md`, `open-questions.md` and `process-learnings.md`;
  - `roadmap.md` and `risks.md` where the project keeps them;
  - `docs/knowledge-base/frontend.md` where the project has a UI surface;
  - `docs/knowledge-base/permission-probe-record.md`, whose "Method" section alone merges; its
    dated sections stay the project's. Take from each of the three copies the part before the
    first `## ` heading after "Method" and merge the three parts as one path. The merged part
    replaces that part of the project's file; the project's file from that heading on stays as
    it is. A project that lacks the file gets the incoming part as the file: no dated section
    of the template's comes in.
- The skeletons of `roadmap.md`, `risks.md` and `frontend.md` live in
  `.claude/skills/adopt/skeletons/`: compare them between the two filtered copies.
- Where the template changed their **scaffolding** (a conventions paragraph, a new section a
  skeleton now carries), present the hunk as a suggestion for the author to place.

## 4. The delta the merge cannot make

- **Retired placeholders and new contracts.** Where the new version replaced a placeholder by a
  contract (build and test commands by `./verify`, say), install the missing piece from the
  project's stack preset in `<filtered>/incoming/stacks/` and point the hooks at it. A token the
  merge brought in takes the project's value, as `/adopt` steps 6 and 8 resolve it.
- **Hooks.** Merge `hooks/` like any files, then re-apply the project's gate configuration from the
  project's side of the merge:
  - the `content-gate` config block, with `comment_exempt_dirs` (the project's directories, plus
    those the incoming list adds) and `email_allow`;
  - the `commit-msg` ticket settings.
  - Stage `100755` what `/adopt` step 10 makes executable, one `git -C <wt> update-index
    --chmod=+x <file>` call per file.
- **Interview delta.** Compare the questions of `/adopt` step 3 with the decision rows of
  `docs/open-questions.md` and ask each one they lack, as `/adopt` asks it. Ask none of these:
  question 1, whose name the project's copies show (step 3 above) and whose one-liner `CLAUDE.md`
  holds,
  unless the copies show more than one name or one with a blank; questions 5, 12 and 13 and the
  base package, which step 3 asked; the path-B questions (the modernisation track, the
  scenarios), which are no upgrade's.
  Set each answer's values where `/adopt` sets them, and record each answer as a dated decision
  row (`/adopt` step 9).
- **"Project choices".** A project from before 5 has none in `CLAUDE.md`: write the six slots as
  `/adopt` step 9 does. Either way "Modules installed" names `<kept>`.
- **The SHARED section.** Copy the SHARED section of the merged `agent-entry-template.md` into
  `CLAUDE.md` verbatim and set the wrapper's version marker to the template's; from then on the
  `pre-commit` leg that compares the two SHARED sections (hooks/README.md) refuses a mismatch.
- **Task 0 where it is missing.** A project without `.feature` files gets `/bdd-catch-up` seeded as
  task 0 in `backlog.md`'s status section (handbook §5).

## 5. Verify

- Run in `<wt>` the checks of `/adopt` step 10 from "Run `sh hooks/self-test`" on, of its module
  bullets only those of `<kept>`, and `stacks/self-test` as below. The token grep is `/adopt`
  step 8's, with hits under `.template/` only, besides those that step's module bullets of
  `<kept>` expect; where `jq` is missing, the JSON check falls back as `/adopt` step 1 says.
- The SHARED sections of `CLAUDE.md` and `docs/knowledge-base/agent-entry-template.md` are
  identical.
- The ADAPT notes the merge brought in are split as `/adopt` step 9 splits them.
- Show the author each `## ` heading after "Method" that `<wt>`'s
  `docs/knowledge-base/permission-probe-record.md` shares with the copy under
  `<filtered>/incoming/`: it is a section of the template's that came in, unless the project's
  own run has the same title.
- Once step 6 has put the new baseline in place:
  - `cd <wt> && sh .template/baseline/stacks/self-test` passes every case, as on path B;
  - `sh <wt>/.template/baseline/.claude/skills/adopt/module-filter verify --keep "<kept>"
    --root <wt>` exits 0;
    show the author each `leftover` line it prints and never edit around one (`/adopt` step 11).

## 6. Finish

- Retire the old baseline with `git -C <wt> rm -r -q .template/baseline`, a tracked tree this
  strand retires (handbook §7). Copy `<incoming>`, unfiltered, into `<wt>/.template/baseline/`
  entry by entry as `/adopt` step 1 copies it, `.filtered` left out, then
  `git -C <wt> add .template/baseline`:
  it is the next baseline. Run the checks of step 5 that wait for it. Set `TEMPLATE-VERSION` to
  M, and hand the author the removal command for `<incoming>`, which takes `<filtered>` with it.
- Commit per handbook §7 in reviewable slices (the baseline bootstrap, the merged process docs, the
  merged hooks and settings, the interview decisions), messages confirmed (who may push: handbook
  §7); the last one sets the version (`Upgrade the process template to version M`).
- Land through the gateway: `git -C <wt> rebase main`, `<wt>/verify all` where the project has a
  `verify`, the compare range for the author, then the landing and cleanup of the project's
  landing model (handbook §7). On the local model, with `<root>` on `main`: from `<root>`, the
  strand's own guard, `sh <wt>/hooks/breach-guard --reaches <tip>` with `<wt>` clean at `<tip>`,
  exits 0 for the reviewed tip (the copy on `main` predates `--reaches`), then
  `git -C <root> merge --ff-only <tip>`, `git -C <root> worktree remove <wt>` and
  `git -C <root> branch -d` on the strand.
  In-flight strands then rebase and finish under the rules they started with plus the mechanical
  gates now on `main`.
- The rule probes of `/adopt` step 6, the core one and each kept module's block, run in the first
  session after the landing, whose loaded rules are the new ones: seed them as a backlog item, as
  `/adopt` step 6 seeds the workflow probe.

Optional incoming path from the author: $ARGUMENTS
