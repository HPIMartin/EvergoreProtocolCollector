---
description: Adopt the process template into this repo (environment check and starting situation, baseline, analysis of the existing tree, interview with landing model, modules and tooling directories, env variant, module filter and module values, settings paths and permission probes, build check, stack presets, verify script, ticket system, modernisation track, task 0, placeholder resolution, Project choices, seeded docs, activation, cleanup and the adoption commit)
argument-hint: [optional: answers you know, e.g. "java-gradle, devcontainer, local, gate guard"]
---

You are adopting the AI-assisted development process template into this repository. The template
files are already extracted in (adoption path A or B per `TEMPLATE-README.md`). Your job: turn the
template into THIS project's process layer, in one reviewed pass.

Ground rules for the whole run: ask, don't guess (multiple-choice options, recommended one first);
detect what the tree already tells you and ask only for confirmation; at the end propose exactly ONE
one-line commit message and wait for confirmation; never push; hand deletions of project content to
the author as exact commands instead of running them yourself (your own worktrees and landed
branches are yours to clean up, git-natively, handbook §7); stop on a refusal of the module filter
and never edit around one (step 5). Nothing of the modernisation track is executed during
adoption: adoption sets the process up, the track runs as ordinary strands afterwards.

- The commit message is one line with no body and no attribution footer: `commit-msg` refuses
  both.
<!-- MODULE guard -->
- The shipped shell-shape guard checks your commands from the first one on, wherever `python3` 3.9
  or newer runs, until step 5 keeps or drops it: one plain command per call, no loop over a
  variable, no command substitution (working-with-ai-agents.md, "Command shapes that keep an
  auto-mode session unattended").
<!-- /MODULE guard -->

Answers the author already provided (skip re-asking those): $ARGUMENTS

## 1. Preflight, the starting situation and the baseline

- **Where this session runs:** on the host, or in a container (`/.dockerenv` exists). A session in
  a container cannot see its host: ask the author which system the host runs and whether Docker
  Desktop runs there, for questions 5 and 13.
- **A pass handed over from the host** (step 4): the arguments above start with `handover:`, the
  marker the host pass writes into the line it prints. That marker alone makes a pass a
  hand-over; the tree's shape never does, since a first path-B run can look the same. The first
  sitting did steps 1 to 4: take its answers and its step-1 facts from the rest of the line, run
  the environment check below, skip the rest of this step and steps 2 to 4, and go on at step 5.
  Check the line against the tree first: `.template/baseline/` exists, untracked; `.devcontainer/`
  stands at the root; a commit (`git rev-list -n 1 HEAD` prints one) means path B, on the strand
  the line names, checked out. Where the tree does not match, stop and show the author what
  differs.
- **Environment check, before anything is touched:** `sh`, `awk`, `git` 2.31 or newer
  (`git --version`; the hooks need it: dev-environment.md, "git version"), `mktemp` and a JSON
  check must run where steps 5 to 11 run. Name every missing one.
  - The JSON check is `jq . <file>`; on a host without `jq`, as Git Bash on Windows is,
    `python3 -m json.tool <file>` serves, and every `jq .` below reads so.
  <!-- MODULE agent-seat -->
  - With the agent-seat module, once the author asks for it (question 13): `jq` and `openssl`,
    which `agent/self-test` and `land/self-test` need; `ssh-keygen` and `git` 2.32 or newer, which
    the agent image's `build` and its self-test need (dev-environment.md, "The agent image").
    - `npx` runs where the image is built, on the seat machine: a precondition the seeded build
      item names (step 9), no stop of this pass.
  <!-- /MODULE agent-seat -->
  - A missing tool stops the pass, since nothing is installed ad hoc (dev-environment.md), with
    one way on: the devcontainer variant moves steps 5 to 11 into the container (step 4), whose
    image holds all of them. Say which tool is missing and go on to the interview; stop after it
    where question 5 picks another variant.
  - `python3` 3.9 or newer and `/usr/bin/perl` stop nothing here: note whether each runs, for
    question 13 and step 5.
- Confirm you are at the repo root: `TEMPLATE-VERSION` and `docs/knowledge-base/README.md` must
  exist, and `git rev-parse --show-toplevel` names this folder. Without a repository the author
  runs `git init` first (`START-HERE.md`, step 1).
- **The starting situation** is one of four; detect it from the tree and confirm with the author:
  1. **A brand-new project:** nothing but the payload, no commit yet. Path A. A build skeleton
     that already stands beside the payload is no part of it: the baseline below then comes from
     the archive.
  2. **An existing repository:** path B, the drop-in. Existing docs are kept and slotted into the
     KB map, never overwritten (`TEMPLATE-README.md`, path-B notes).
  3. **An existing repository with its own `CLAUDE.md`, `.claude/` or knowledge base:** path B
     plus a hand merge.
     - List every path that both the tree and the payload hold and put each to the author: keep
       the project's, take the template's, or merge by hand. Never overwrite one unasked.
     - `CLAUDE.md` ends as the template's wrapper with its SHARED section verbatim (`pre-commit`
       compares it); the project's own rules move into the KB doc they belong to, its tool
       mechanics under "Claude-Code mechanics" (agent-entry-template.md, "How to build a wrapper
       from this template").
     - In `.claude/settings.json` the project's own rules join the template's; never weaken the
       deny floor.
  4. **An adopted project:** the last commit holds the baseline,
     `git ls-tree -r --name-only HEAD -- .template/baseline/ TEMPLATE-VERSION` prints a path; a
     repository without a commit is none. Stop and name `/template-upgrade`: its new archive is
     extracted into `.template/incoming/`, never into the root. A baseline staged but not
     committed is a pass of this adoption waiting at step 11, not an adopted project.
- **A Windows host:** `uname -s` starts with `MINGW`, `MSYS` or `CYGWIN`, or the author named it
  above. Note it for questions 5 and 13 and for step 6; nothing is adapted here.
- **The branch that takes the landings is called `main`** in every process text and check. Where
  the repository's has another name (path B: `git branch --show-current` before the strand is
  cut; confirm), ask: the author renames it to `main` first (recommended), or the name stays and
  "write the branch's name into the process texts" becomes the first seeded backlog item
  (step 9).
  <!-- MODULE agent-seat -->
  - Where the author asks for the agent-seat module, "the name stays" is no option: `agent/push`
    and `land/land` name `main` themselves, so the branch is renamed to `main` first.
  <!-- /MODULE agent-seat -->
- **The adoption commit's place** follows handbook §7, "No commit is made on `main`". In a
  repository that holds a commit, so path B always, run
  `git switch -c claude/adopt-template` in this checkout before the first edit (another prefix
  where the answers above name one). The files the author copied in travel with it, uncommitted.
    - On a first pass `git status` must show nothing but those files: other uncommitted work is
      the author's to commit or put aside first. A later pass of the same adoption also finds
      what the earlier one changed.
    - Where the command is refused, say which way and what it means:
      - git refuses it because the branch exists: an earlier pass of this adoption cut it. Where
        `git branch --show-current` names it, this checkout stands on it: go on there. Otherwise
        stop and show the author; never delete, reset or move that branch.
      - the shell-shape guard refuses it, since a `switch` in the primary checkout is refused
        whatever its branch: the author runs the command, then this pass goes on.
- **Keep the pristine payload before touching anything.** Copy the extracted template files, exactly
  as they came out of the archive, into `.template/baseline/`:
  - The baseline is copied once, by the first pass: where `.template/baseline/` already exists,
    untracked, an earlier pass of this adoption made it, on either path, before an interruption
    or the hand-over. Never copy into it again; go on with the copy it holds.
  - Where `.template/baseline.partial/` exists, an earlier copy was cut short: stop and ask the
    author to remove that folder, then copy afresh.
  - path A, where the tree holds nothing but the payload: the tree as it stands now, `.git/` and
    `user-scope/` excluded;
  - path B, and path A with anything else beside the payload: from the directory the author
    extracted the archive into, ask for it. On path B this pass makes the baseline; the author
    copies none (`TEMPLATE-README.md`, "Adoption path B").
  - Copy entry by entry into a folder of its own, renamed once the copy is whole: list the
    top-level entries with `ls -A` first, then `mkdir -p .template/baseline.partial`, then one
    `cp -a <entry> .template/baseline.partial/` for each listed entry except `.git`, `.template`
    and `user-scope`, then `mv .template/baseline.partial .template/baseline`.
  - `/template-upgrade` merges three ways against this copy later; without it an upgrade is a
    guess. The baseline is committed with the adoption.
- `env/`, `stacks/` or `ci/` missing from the root: on path A that step was done earlier, skip it;
  on path B the hand copy left them out, so read them under `.template/baseline/` wherever a step
  below names them.

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
| Environment | a devcontainer, Dockerfiles, a version manifest, host-only tooling (an editor a container cannot run), a host Docker socket mounted into a container |
| Documentation | README, docs folders, ADRs, a wiki or ticket system referenced anywhere |
| UI surface | a single-page app, a desktop view or a game view, and its source tree |
| Remote | `git remote -v`: an `origin` or none, and whether its URL is GitHub's |
| Host | Windows or not (step 1), whether Docker Desktop runs here (`docker info` names it; a session in a container takes both from the author's answer of step 1), whether `python3` 3.9 or newer and `/usr/bin/perl` run |
| Authentication surface | request filters, the security configuration, login, cookie and token handling, roles, passwords, a rate limit, deploy secrets, and the path prefixes they live under |
| Earlier Claude setup | a tracked `CLAUDE.md`, `.claude/` (settings, agents, skills, commands), another tool's entry file, an own knowledge base under `docs/`; compare against `HEAD`, since path B's hand copy may have replaced one |
| Tooling | a directory of the project's own scripts that holds a `self-test` |

## 3. Interview (one round; multiple-choice, recommended first; prefilled from the analysis)

Questions 10 and 11 are for path B only. No question asks for the commit-message confirmation or
for the instruction channels: each of those two slots has one variant (step 9).

1. **Project name** (`{{PROJECT_NAME}}`) and **one-liner** (`{{PROJECT_ONE_LINER}}`: what it does,
   for whom, what it deliberately showcases). The name is one word of letters, digits, `_`, `.`
   and `-`: it lands in paths (the devcontainer mount, the build lock, the worktree volume). A
   name with blanks belongs in the one-liner.
2. **Stacks**, multi-select from `stacks/`: java-gradle, js-ts-npm, python, cpp, csharp-dotnet
   (with the Unity adapter addendum where the project has a Unity view), other (then the author
   names the tools and the nearest preset is the shape to extend). Then **how they compose**: one
   build tool wraps the others (as Gradle can drive an npm frontend), or the root `verify` calls
   each stack in turn. A Windows-only project (MSVC, WPF, a full Unity project) must be named here;
   the default variants assume the devcontainer hybrid (`stacks/csharp-dotnet/README.md`).
3. **Per stack, the tools**: unit test framework, formatter, linter, package manager. Offer the
   preset's default first; on path B offer "keep the existing tool and tighten it" before "replace
   with the preset's", and record a replacement as a backlog item with its rationale, never as a
   silent swap.
4. **The acceptance runner, one per system** (handbook §5): the primary stack's runner
   (cucumber-jvm, cucumber-js, pytest-bdd, Reqnroll); for a C++ system pytest-bdd out of process
   unless the author wants the in-process alternative; for a Unity project Reqnroll against the
   engine-free core. Ask where the feature files live if the runner's convention does not fit, and
   the **scenario language**: English (default) or `# language: de` because the stakeholders read
   German.
   Then the **harness model** (handbook §5): the shipped rule (recommended: each actor through its
   real surface, a fresh real service per scenario where the system runs as a service) or the
   in-memory model of that section's ADAPT note (scenarios at the application boundary, through
   ports with in-memory fakes).
5. **Environment variant and where Claude Code runs.**
   - The variant: devcontainer (recommended: keeps the toolchain off the host, within the limits
     dev-environment.md names) or native pinned toolchain (the `env/` READMEs state the
     trade-off).
   - Where the sessions run: in the devcontainer (recommended with that variant); on the host
     beside the container; natively with `python` pinned in `.tool-versions`; natively without a
     `python3`.
   - On a Windows host (step 1) the devcontainer with the sessions inside it comes first: nothing
     is adapted there. A native Windows checkout is the narrowed alternative that step 6 sets up;
     its self-tests (step 10, and again in the adoption commit's hooks) run far slower in Git Bash
     than in a container, hours against minutes for the larger suites.
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
   (B) always latest through the bot's pull requests (landing: build-run-deploy.md, "Dependency
   updates"); (C) pinned, upgraded on need only.
9. **Conversation language** for the SHARED language line (code, comments, identifiers, docs and
   feature files stay English unless question 4 decided otherwise).
10. **Path B only, the modernisation track.** For each topic of the analysis table (vulnerability
    scan, format and lint with warnings as errors, CI, dependency upgrades, containerisation) offer
    keep / tighten the existing tool / adopt the preset's, and propose an order by risk. The result
    is a backlog track (or tickets), nothing is executed now.
11. **Path B only, the scenarios.** Ask explicitly whether `/bdd-catch-up` becomes **task 0**,
    before any feature work, and in which order its clusters run (handbook §5, "Projects that
    predate their scenarios"; recommended: by risk and by how often the code changes). The code is
    the truth for what the software does; the author confirms what it should do.
12. **Remote, landing model and strand prefix** (handbook §7).
    - **Remote:** `origin` or none; read it from `git remote` and confirm.
    - **Landing model:** local (after the author's review the agent fast-forwards `main`) or hub
      without seat (the author pushes the reviewed tip to `origin/main`). Without `origin` local
      is the only one. Recommend local where only the author moves `main`, hub without seat
      where others land on `origin/main` too.
      <!-- MODULE agent-seat -->
      - **Hub with seat** exists only with the agent-seat module, and keeping that module in
        question 13 sets it, unasked: a seat strand lands by the author's `/land` comment on its
        pull request (handbook §7, "Landing models"). Its preconditions are the module's
        (question 13), an `origin` on GitHub among them.
      <!-- /MODULE agent-seat -->
    - **Strand prefix:** `claude/` (recommended) or the author's own.
13. **Modules.** `sh .claude/skills/adopt/module-filter list` prints each module with the module
    it needs and with what the skill has to check. Offer each with its precondition and the
    recommendation below; where a precondition fails, say which one and do not offer the module.
    A name that `list` does not print is no part of this payload and cannot be kept.
    <!-- MODULE gate -->
    - `gate` (the gate dossier, its check and its helpers): recommended for every project and
      every preset.
    <!-- /MODULE gate -->
    <!-- MODULE guard -->
    - `guard` (the shell-shape guard): recommended wherever `python3` 3.9 or newer runs where the
      sessions run (question 5); not on a native Windows checkout, where it stays inactive.
    <!-- /MODULE guard -->
    <!-- MODULE security-lens -->
    - `security-lens`: only with `gate`; recommended where the analysis shows an authentication
      surface. It brings two questions:
      - the surface's path prefixes, prefilled from the analysis (an empty answer leaves the
        security leg off);
      - the host its requests reach: `localhost` (recommended), or `host.docker.internal` where
        the sessions run in a container whose Docker socket is the host's.
    <!-- /MODULE security-lens -->
    <!-- MODULE acceptance -->
    - `acceptance` (the browser half of the acceptance scenarios): only with a UI surface, and
      recommended there. On java-gradle it brings the sweep form: `marker` (recommended) or
      `ancestry`, offered in the devcontainer variant only (testing.md, "The browser half").
    <!-- /MODULE acceptance -->
    <!-- MODULE volumes -->
    - `volumes` (the agent worktrees on a named volume): only with the devcontainer variant;
      recommended where the host runs Docker Desktop (dev-environment.md, "Worktrees on a
      volume").
    <!-- /MODULE volumes -->
    <!-- MODULE agent-seat -->
    - `agent-seat` (the unattended agent seat on GitHub): never offered unasked. On the author's
      request, offer it where all five preconditions hold, else name the one that fails.
      - The preconditions:
        - `guard` is kept;
        - `origin` is on GitHub: its configured URL, `git config --get remote.origin.url` (an
          `insteadOf` rewrite changes nothing here), takes one of these forms, each with or
          without `.git`, all of which `agent/gh` takes: `<user>@github.com:<owner>/<repository>`,
          `https://github.com/<owner>/<repository>` or
          `ssh://<user>@github.com/<owner>/<repository>`;
          - any other form fails, such as `https://<user>@github.com/...`,
            `ssh://git@ssh.github.com:443/...` or `https://github.com.example/...`;
        - the environment variant is the devcontainer (question 5): the seat's container comes
          from the agent image, which layers on the devcontainer image;
        - that devcontainer image is a Debian one whose user is `vscode`, as the agent image's
          `provision` and `Dockerfile` require; the payload ships so;
          - a project's own `.devcontainer/` (path B, step 4) holds it where its one
            `devcontainer.json` stands at `.devcontainer/devcontainer.json` (the agent image's
            `build` reads no other), the last `FROM` line of the `Dockerfile` its `build`
            names, else its `image`, names a Debian release as a literal tag, and its
            `remoteUser` is `vscode` (the devcontainers images set it in their metadata);
          - a compose file, a tag set by a build argument, or a `FROM` line step 4 changes:
            ask the author, and offer the seat only on a Debian image whose user is `vscode`;
        - the landing branch is called `main` (step 1).
      - Keeping it sets the landing model of question 12 to hub with seat.
      - It brings four questions:
        - the names of the agent seat and of the supervised machine, for the seat marker (KB
          README, DOC-5) and the table in dev-environment.md, "Where agents run";
        - the dependency bot's branch prefix, prefilled from question 8: `dependabot/` for
          Dependabot, `renovate/` for Renovate, none without a bot;
        - `LAND_TARGET`, the branch the first landings reach: `<strand prefix>land-probe`
          (recommended: the first live trial lands there, then `main`) or `main`; it is
          GitHub's setup and goes into no file;
        - whether the landing App exists yet and, where it does, its actor id
          (`ci/github/README.md`, "Adoption", step 5).
    <!-- /MODULE agent-seat -->
    - **The KB guard** is preset material and no module: offer it to every project on java-gradle,
      with or without `acceptance`, and recommend it. It brings the frontend source root (the
      sources, as in `frontend/src`; empty without a frontend).
    - **The Java base package**, where an answer above brings Java files to copy (step 7),
      prefilled from the tree under `src/main/java`.
14. **The project's own tooling directories with a self-test** (a deploy script, an ops runbook):
    each keeps its comments and joins `comment_exempt_dirs` in step 7 (handbook §3). Prefill from
    the analysis table's "Tooling" row; none is the first option where the row is empty.

## 4. Apply the environment variant

- Copy the chosen variant's payload from `env/<variant>/` to the repo root (devcontainer:
  `.devcontainer/`; native: `.tool-versions`) and work through that variant README's adoption
  steps. The devcontainer `name` takes the project name; pinned versions come from the stack
  presets.
  - Copy `.devcontainer/` whole, in one command that names the folder and nothing under it. A
    copy file by file names `.devcontainer/.claude-home/`, which the deny on Claude Code's home
    refuses (dev-environment.md, "Claude Code's home"); the tree then lacks
    `.devcontainer/.claude-home/.gitignore`, the one tracked file of the folder the container's
    bind mount takes as its source.
- Four things of those READMEs wait for their own step: the lines that belong to a module (step 5
  keeps or drops them), the path-bearing rules of `.claude/settings.json` (step 6), the hooks and
  the push default (step 10) and the deletion of `env/` (step 11).
- **A `.devcontainer/` the project already has** (path B) is kept, never replaced. Merge into it,
  with the author, what the payload's `devcontainer.json` and `Dockerfile` add: the links of
  `postCreateCommand` that wire the hooks and the push default, `CLAUDE_CONFIG_DIR` with its
  `.claude-home` mount, and each fenced block of a module kept in question 13, fence lines
  included, with the `postCreateCommand` links such a block's note names. Copy no block of a
  module that is not kept.
  - The mount's source, the payload's `.claude-home/` with its `.gitignore`, is the author's to
    copy where the deny on Claude Code's home refuses the session's copy.
  <!-- MODULE agent-seat -->
  - With the agent-seat module also copy the payload's `env/devcontainer/.devcontainer/agent/`
    into the project's `.devcontainer/agent/`, which steps 5, 8 and 10 work on. Where the project
    already has a folder there, put it to the author first.
  <!-- /MODULE agent-seat -->
- Record the choice in `docs/knowledge-base/dev-environment.md` (its first ADAPT note), with where
  the sessions run (question 5).
- **The devcontainer variant in a session on the host:** steps 5 to 11 belong in the container.
  Its image holds the tools of step 1, the rules' paths are written for it, and the build and the
  self-tests run there. Unless question 5 put the sessions on the host beside the container, stop
  here and ask the author to build the container, open the folder in it and start
  `/adopt <line>` there, where `<line>` is one line you print that starts with the marker
  `handover:` (step 1) and carries:
  - every answer of the interview;
  - from step 1: path A or B, the starting situation, the landing branch's name and what the
    author decided about it, whether the host runs Windows and Docker Desktop, and on path B the
    strand's name.
  - Nothing is committed yet, and the files stay as they are.

## 5. Apply the modules

`module-filter`, beside this file, drops what belongs to a module the author did not keep, and
`module-manifest` says what that is. `<kept>` is the modules kept in question 13: their names,
separated by blanks; `--keep ""` keeps none.

- **Check the tools two modules need**, where the sessions run (question 5):
  <!-- MODULE guard -->
  - `guard`: `python3` 3.9 or newer,
    `python3 -c 'import sys; sys.exit(0 if sys.version_info >= (3, 9) else 1)'`.
  <!-- /MODULE guard -->
  <!-- MODULE acceptance -->
  - `acceptance` on java-gradle: `/usr/bin/perl`, which the sweep test calls (Linux).
  <!-- /MODULE acceptance -->
  - Name a missing one and put it to the author: drop the module, or install the tool first.
  - Where that environment does not exist yet (a devcontainer before its first build), the check
    stays open: seed it as a backlog item (step 9).
- **Path B first:** copy from `.template/baseline/` what the root lacks: `agent/`, and each `path`
  row of `module-manifest` whose module is kept. Leave out the `path` rows of a module that is not
  kept.
- `sh .claude/skills/adopt/module-filter check --keep "<kept>"` must print `ok`. It refuses a
  name it does not know and a module whose needed module is missing: go back to question 13 with
  its reason. The other preconditions of `list` (a UI surface, the devcontainer variant) are
  yours to check against the answers.
- **The python stack without `guard`:** before `apply` cuts the guard fence of
  `.devcontainer/devcontainer.json`, keep the python feature line and move it out of the fence:
  it is the stack's interpreter pin too.
- `sh .claude/skills/adopt/module-filter apply --keep "<kept>"` plans first and changes nothing
  where it refuses. It cuts every fenced block, settings rule and devcontainer line of a module
  that is not kept, and prints:
  - `edited <file>` for each file it changed;
  - `remove <path>` for each file or directory of a dropped module. It deletes nothing: keep
    these lines for the removal list of step 11;
    - Named limit: a line names the path, whoever wrote the file. On path B a project's own
      file at a module's `path` row is listed where that module is dropped: hold each line
      against the project's own files before step 11.
  - `note:` for a fence whose name it does not know (leave that block and pass the line on to the
    author), for a row that the project's own copy of a devcontainer file does not hold (nothing
    was there to cut), and for a module none of whose rows is left (`no row of it is left to
    cut`: an earlier `apply` of this pass cut it, or the project never held them), so `apply`
    runs again after an interruption or to drop one more module.
  <!-- MODULE guard -->
  - Without the guard, `apply` cuts the guard's two entries out of the top-level `hooks` key of
    `.claude/settings.json`:
    - each handler whose `"command"` is word for word the template's (the `hook` rows of
      `module-manifest`);
    - then the group, the event and the key that this leaves empty, each with what it holds
      besides, such as a matcher the project widened and the comments on its lines;
    - the project's own hooks stay, read like every other line for the traces of the modules not
      kept;
    - a guard command the project has changed is refused (below).
    - Named limit: a guard command written with JSON escapes (`agent\/bash-shape-guard`,
      `/`) is neither cut nor traced; `apply` and `verify` pass it, and the hook then runs
      no guard.
  <!-- /MODULE guard -->
- **A refusal** (exit 1, `refused: <reason>` on stderr; exit 2 is a usage error of your call)
  stops this step. Show the author the line. Never cut a block, a rule or a line by hand to get
  past it, and never edit the file it names.
  - Five refusals are the author's to clear, in a file the project brought or merged by hand (a
    devcontainer of its own, its settings):
    - a line that looks like a fence and is none;
    - a `trace` of a module that is not kept (that module's text in another spelling, such as a
      guard hook command the project has extended: the author puts back the template's command
      or takes the hook out by hand);
      - Named limit: a project's own path that holds such a text behind a `/` counts as a
        trace too, such as a hook `sh scripts/gate/lint` while `gate` is not kept: the author
        renames the path or keeps the module.
    - a settings key whose value opens on a later line or is never closed;
    - an entry or a key of a module that is not kept which shares a line with what stays, or
      whose comma stands on the next line: the author puts it on lines of its own;
    - a row found 0 times while other rows of its module stand (a rule or a guard hook left out
      in a hand merge): the author puts it back as `.template/baseline/` holds it.
    - Once the author has corrected what the refusal names, run `apply` again.
  - A file that cannot be written is refused with every file as it was: the author makes it
    writable, then `apply` runs again.
- `jq . .claude/settings.json` must pass afterwards.
- A kept module keeps its fences and its blocks whole (KB README, DOC-4).
- `module-filter verify` passes only once the author has removed the `remove` paths: step 11.

Then fill what stays:

- **Every project:** the `branch_prefix` line of `agent/fold` takes the strand prefix of
  question 12 (shipped: `claude/`).
- **Every project:** the `landing_model` line of `hooks/sync-main` takes the landing model of
  question 12: `local`, or `hub` for either hub model (shipped: `local`).
<!-- MODULE gate -->
- **`gate`:**
  - The ADAPT block at the top of `gate/dossier-check`: each assignment of the chosen preset's
    `stacks/<preset>/gate.env` replaces the block's line of the same name; a line the file does
    not name keeps its shipped value.
  - Two presets under a composed `verify`: merge the two files value by value. Alternations are
    joined with `|`, each stack's path patterns under the stack's root as the composed `verify`
    dispatches them; `test_readers` are united. Where two values exclude each other (one
    `features_dir`, one `walk_success_re`, one `walk_count_re`, one `bdd_leg`), ask the author.
    - Their `walk_failed_reports` globs are joined with a blank, each under its stack's root as
      the composed `verify` runs it (a Gradle root with a js-ts-npm `frontend/`:
      `build/test-results/test/*.xml frontend/artifacts/test-results/*.xml`).
  - `base_ref`, from the landing model of question 12: `refs/heads/main` for local,
    `refs/remotes/origin/main` for a hub model; a landing branch that kept another name (step 1)
    stands in place of `main`.
  - `frontend_root`, `needs_frontend` and `reads_frontend`, from the analysis table's "UI
    surface" row: the UI's source root, its paths, and for `reads_frontend` also what the
    playbook's read-path table names for the frontend lens (the API it calls, the build files and
    `verify`; with the acceptance module the step glue and, where copied, the grid recipe).
    Without a UI surface all three stay empty.
  - With a UI surface also `bdd_paths_re` gains the UI's root and `walk_task_lines` the UI's
    test task; the shipped block shows both for a `frontend/` that Gradle drives
    (`^(src|frontend)/`, `> Task :frontend:npmTest`).
  - With the KB guard of question 13: `docs_guard_test='KbCitationGuardTest'` and the
    `docs_guard_ran_re` that the note in `stacks/java-gradle/gate.env` gives.
  - In multi-agent-playbook.md, "Read paths of the lenses (delta rounds)", delete the table and
    the ADAPT note under it: the pointer of the gate block below them takes their place.
<!-- /MODULE gate -->
<!-- MODULE guard -->
- **`guard`:**
  - `ENVIRONMENT` in the ADAPT block of `agent/bash-shape-guard`, from question 5: `devcontainer`
    for sessions in the container or on the host beside it; `native` for a native setup that pins
    `python`; `inactive` for a native setup without a `python3` and for a native Windows checkout
    (step 6).
  - The block's other values keep what ships: `OUTPUT_DIRECTORY` (`/tmp/claude-<uid>`),
    `WORKTREE_ROOT` (`.claude/worktrees`, the default root of handbook §7, which a set
    `AGENT_WORKTREES` outranks), `APPROVALS` (the module filter takes `push` and `gh` out of
    `APPROVALS` and its comment where agent-seat is not kept), and `GUIDE_FILE` with
    `GUIDE_HEADING`. Change one only where the project moved what it names.
  - `volume_root` of `agent/session-check`: `/worktrees` with the volumes module; without it the
    module filter has emptied it, a `cut` row of the volumes module.
  - Python: the devcontainer keeps its pinned `python` feature; a native setup pins `python` 3.9
    or newer in `.tool-versions`.
<!-- /MODULE guard -->
<!-- MODULE security-lens -->
- **`security-lens`:**
  - `gate/authentication-surface`: the path prefixes of question 13, one per line. An empty
    answer writes no file and leaves the dossier check's security leg off.
  - The `host` line of `gate/lens-request`, from questions 5 and 13: `localhost` where the tip
    runs on the machine the session runs on; `host.docker.internal` where the sessions run in a
    container whose Docker socket is the host's.
<!-- /MODULE security-lens -->
<!-- MODULE acceptance -->
- **`acceptance`:** its files and its values come with the preset (step 7). The `firefox-esr`
  layer of `.devcontainer/Dockerfile` stays as shipped; natively the host's own Firefox runs the
  scenarios (dev-environment.md, variant B).
<!-- /MODULE acceptance -->
<!-- MODULE volumes -->
- **`volumes`:**
  - The volume's name: the project name with every character outside letters, digits, `_`, `.`
    and `-` replaced by `-`, then `-worktrees`. Write it in place of `{{PROJECT_NAME}}-worktrees`
    in `mounts` of `.devcontainer/devcontainer.json` and in dev-environment.md, "Worktrees on a
    volume", before step 8 resolves the token. Two clones of the project on one machine share it.
  - The `MSYS_NO_PATHCONV=1` ADAPT note of that section stays on a Windows host (step 1) and goes
    elsewhere.
<!-- /MODULE volumes -->
<!-- MODULE agent-seat -->
- **`agent-seat`:**
  - The strand prefix of question 12 goes into the `branch_prefix` lines of `agent/push`,
    `land/land` and `hooks/pre-push`, as into `agent/fold`'s; `mainline` of `hooks/pre-push` keeps
    `main` (step 1).
  - `bot_prefix` of `agent/gh`: the dependency bot's branch prefix of question 13, empty without a
    bot.
  - `required_check` of `land/land` stays empty and `required_check_app` keeps `github-actions`:
    no CI run exists yet to name (seeded in step 9).
  - The agent prefix and the bot line of the `agent-namespace` ruleset follow in step 7, with its
    copy.
  - In `.devcontainer/agent/`:
    - `cache_dirs` of `provision`: the chosen stacks' cache folders in the seat user's home,
      separated by blanks (`.npm`, the node feature's, ships; `.gradle` for Gradle, say).
    - `debian_release` of `provision`: the codename the tag of the last `FROM` line of
      `.devcontainer/Dockerfile` names, else the tag of the `image` of `devcontainer.json`
      (`bookworm` ships); where the tag names none, ask.
      - A codename that is no Debian release (step 4 moved the base image, say) fails the
        fourth precondition of question 13 (the Debian image): say so, and the author moves the
        base image back or drops the seat.
    - The pins, each set to the current version, read from its registry where this session
      reaches it, read-only:
      - `version` of `install-claude-code`: `npm view @anthropic-ai/claude-code version`;
      - `devcontainer_cli` of `build`: `@devcontainers/cli@` and the version
        `npm view @devcontainers/cli version` prints;
      - `docker_version` (`docker-ce` and `docker-ce-cli`), `containerd_version`
        (`containerd.io`), `buildx_version` (`docker-buildx-plugin`) and `compose_version`
        (`docker-compose-plugin`) of `provision`: the highest version of each package in Docker's
        package index for that codename, `<index>` being
        `https://download.docker.com/linux/debian/dists/<codename>/stable/binary-amd64/Packages`
        (another architecture where the seat machine runs one). Read it by command, never by eye:
        - `curl -fsSL --create-dirs <index> -o <output target>/packages`, the guard's output
          target;
        - where the list mixes versions with and without an epoch (`5:`), `sort -V` orders them
          wrongly: ask the author;
        - per package, its highest version:

          ```sh
          awk -v p=<package> '$1 == "Package:" { ok = ($2 == p) }
            ok && $1 == "Version:" { print $2 }' <output target>/packages | sort -V | tail -n 1
          ```

        - `docker_version`, the highest version that both `docker-ce` and `docker-ce-cli` list:

          ```sh
          awk '$1 == "Package:" { p = $2 }
            $1 == "Version:" && p == "docker-ce" { ce[$2] = 1 }
            $1 == "Version:" && p == "docker-ce-cli" { cli[$2] = 1 }
            END { for (v in ce) if (v in cli) print v }' <output target>/packages |
            sort -V | tail -n 1
          ```

      - A registry out of reach (no network, no `npm`) leaves the shipped pin, and step 9 seeds
        it.
  - `AGENT_SEAT`, `AGENT_APP_ID`, `AGENT_APP_KEY`, `AGENT_SNAPSHOT_DIR`, `LAND_TARGET`,
    `LAND_APP_CLIENT_ID` and `LAND_APP_PRIVATE_KEY` go into no file of the repository: they are
    the seat machine's and GitHub's setup (step 9).
<!-- /MODULE agent-seat -->

## 6. Settings paths and permission probes

- **The path-bearing rules** of `.claude/settings.json` (working-with-ai-agents.md, the ADAPT note
  "State your threat model": the worktree `verify` allow and its two `..` denies, the `agent/fold`
  allow and its deny, and each kept module's allows under the same prefix) ship as
  `/workspaces/{{PROJECT_NAME}}`, the devcontainer mount convention. That is only correct when the
  checkout folder name equals the project name; a native checkout usually lives elsewhere
  entirely. Replace the prefix in every rule by the absolute root as the sessions
  of question 5 see it, then run `jq . .claude/settings.json`:
  - where this session is one of them: the output of `pwd`;
  - for a devcontainer that this session runs outside of: `/workspaces/<checkout folder name>`.
- **A native Windows checkout** (step 1 and question 5) is a named narrowing, not a port:
  - write the root into the rules in the form this session's shell prints it (`pwd` in Git Bash:
    `/c/...`), and write no script that converts paths (dev-environment.md, "A Windows host");
  <!-- MODULE guard -->
  - with the guard module `ENVIRONMENT` is `inactive` (step 5): the guard compares Linux paths
    only, and the session-start report says at every start that it is off;
  <!-- /MODULE guard -->
  - record the narrowing in dev-environment.md, variant B, under "A Windows host": the form the
    rules' paths take, and that the probes below ran on this host;
  - probe the rules on this host as below.
- **Probe the core rules** by the rule probe of
  `docs/knowledge-base/permission-probe-record.md`, "Method":
  - `<project root>/.claude/worktrees/no-such-wt/verify all` runs and fails with "no such file";
  - `<project root>/.claude/worktrees/../verify` and `<project root>/.claude/worktrees/x/../verify`
    are refused;
  - `<project root>/agent/fold <project root>/.claude/worktrees/no-such-wt 0000000` runs, and
    fold itself refuses the worktree; the same call with `/..` behind `no-such-wt` is refused.
- **Add one block per kept module** to the same run:
  <!-- MODULE gate -->
  - `gate`: `sh gate/dossier-check` runs and prints its usage; `sh gate/../gate/dossier-check` is
    refused; `<project root>/.claude/worktrees/no-such-wt/gate/<helper>` runs and fails with "no
    such file" for each of `dossier-check`, `walk`, `tip-evidence` and `scenario-check`;
    `<project root>/.claude/worktrees/x/../gate/walk` is refused.
  <!-- /MODULE gate -->
  <!-- MODULE security-lens -->
  - `security-lens`: `<project root>/.claude/worktrees/no-such-wt/gate/lens-request` runs and
    fails with "no such file"; the same path with `x/..` in place of `no-such-wt` is refused.
    - One question is open: whether the ask on `Bash(curl:*)` reaches the `curl` inside the
      wrapper. Run `sh gate/lens-request <port> GET /` with a port from 1024 up that nothing
      listens on: a run that ends in curl's connection error says no, a prompt says yes. Record
      which.
  <!-- /MODULE security-lens -->
  <!-- MODULE guard -->
  - `guard`, where step 5 left it active: `W=/x; ls $W` is refused by the guard (the PreToolUse
    hook), and a plain prompt shows "agent guard active" in the session-start context (the
    SessionStart hook). Where it is inactive, the start report says so and why.
  <!-- /MODULE guard -->
  <!-- MODULE volumes -->
  - `volumes`, in the container: `/worktrees/no-such-wt/verify all` runs and fails with "no such
    file"; `/worktrees/../worktrees/x/verify all` and `/worktrees/x/../x/verify all` are refused;
    with the gate module each helper under `/worktrees/no-such-wt/gate/` runs the same way.
  <!-- /MODULE volumes -->
  <!-- MODULE agent-seat -->
  - `agent-seat`, where step 5 left the guard active; this session runs off the seat:
    - `<project root>/agent/push --delete <strand prefix>no-such-probe` runs, the guard approving
      its shape, and `agent/push` itself refuses it with exit 2: `AGENT_SEAT` does not read
      `unattended`;
    - `<project root>/agent/push <project root>/.claude/worktrees/no-such-wt` prompts: the guard
      approves the push of a linked worktree that exists, and no rule names the program;
    - `<project root>/agent/gh pr list` runs, the guard approving it, and refuses itself with exit
      2 before any call: `AGENT_SEAT` does not read `unattended`, so no token is minted;
    - `git fetch --prune origin` runs, the module's exact allow, and fetches as `hooks/sync-main`
      does on the seat; `git fetch --prune --tags origin` prompts.
  <!-- /MODULE agent-seat -->
- **Record the runs** in permission-probe-record.md as its ADAPT note says: the method stays, the
  shipped tables give way to this project's runs, each whole. A module's rows stand in a section
  of their own inside that module's fence, as the shipped guard and volumes sections do.
- **The record's workflow probe waits for the first session after the adoption commit:** its
  throwaway worktree is cut from a commit, and that commit has to hold the payload, an installed
  `verify` and active hooks. Seed it as a backlog item (step 9).
  <!-- MODULE guard -->
  - With the guard module it ends with the approved shapes of the shipped guard probe: the two
    `git -C <worktree> checkout` forms and `<project root>/agent/fold <worktree> <tip>`.
  <!-- /MODULE guard -->
  <!-- MODULE agent-seat -->
  - With the agent-seat module it adds `<project root>/agent/push <worktree>`, off the seat: it
    runs, and `agent/push` refuses it with exit 2.
  <!-- /MODULE agent-seat -->
- **A rule probe that cannot run here**, where this session runs outside the devcontainer the
  rules are written for, is seeded as a backlog item with its reason (step 9).

## 7. Apply the stack presets and compose `verify`

- **First the build check.** `hooks/format-gate` calls `./verify format` at every commit once the
  root `verify` is executable, the adoption commit included, and the bullets below wire into a
  build that has to exist. Every preset's `verify` exits 1 with "required tool not available"
  where the tool it requires is missing: for java-gradle the build's own `./gradlew`; for the
  others `npm`, `uv`, `dotnet` or, for cpp, `clang-format`, which then run on what the build
  holds (`npm run format:check`, `uv run ruff`, `dotnet format`). So for each chosen stack whose
  build does not exist yet (no build manifest of the analysis table's "Stacks" row; java-gradle:
  no executable `./gradlew`), stop and ask:
  - the author creates the stack's build skeleton now, with the stack's own init command
    (recommended), and this step goes on once it stands;
  - or the project is adopted without that stack's preset: skip the preset's bullets below and
    seed "apply the `<preset>` preset from `.template/baseline/stacks/<preset>/`" as the first
    backlog item (step 9).
  - Never write a build yourself.
  - A project left with no preset at all gets no root `verify`: the format gate stays silent
    until one exists, and step 10 leaves out every `verify` line.

Per `stacks/<preset>/README.md`, for every preset the build check lets through:

- Merge its allow block into `permissions.allow` of `.claude/settings.json`; never weaken the deny
  floor. Validate: `jq . .claude/settings.json`. The committed `Bash(./verify:*)` rule covers every
  subcommand.
- Install `verify`: one stack copies `stacks/<preset>/verify` to the repo root; several stacks
  compose one root `verify` per `stacks/README.md` (dispatch `focus <path>` by path, run every stack
  for `all`, `outdated`, `vuln`, `stop`). Set the executable bit in the index
  (`git update-index --chmod=+x verify`) and run `./verify format` once.
- Wire the acceptance runner of question 4 with the `@wip` filter the preset describes, the
  feature-file location and the step-definition shape of the harness model (the preset README's
  shape for the shipped rule, its ADAPT note for the in-memory model); add the runner's
  dependencies to the build.
- Configure the gate: `hooks/content-gate`'s config block (`production_trees`, `test_trees`,
  `print_allow_paths` for CLI output adapters), `hooks/commit-msg`'s ticket settings from
  question 7; the gate scans every stack family by default, so nothing else changes.
  `hooks/format-gate` calls `./verify format` and needs no adaptation.
  - Shell scripts (`*.sh`, or a shebang of `sh`, `bash`, `dash` or `zsh`) and YAML are
    comment-scanned like source. A file this pass copies or writes in either form (`verify`, the
    CI file, the bot's config) gains no comment beyond its `# > **ADAPT:**` notes.
  - `comment_exempt_dirs`: add each tooling directory of question 14 to the shipped list.
  - `email_allow`: its ADAPT note stands. Add the domains the project's fixtures use, never one
    that receives mail; on path B list the addresses the existing tests hold and ask.
- Apply the preset's format and lint wiring, its warnings-as-errors and its pinning notes to the
  build; on path B what question 10 put on the modernisation track waits there. Whatever
  `./verify format` calls must exist in the build, or the format gate refuses the adoption
  commit: adapt the script along its ADAPT notes to the tools question 3 kept.
- Set up the **probe location** for agent throwaway code (build-run-deploy.md's ADAPT and the
  preset's section) and keep only the `.gitignore` probe entries of the chosen stacks, removing the
  scaffolding-prose lines that name the stacks not chosen (handbook §3's ignore-file carve-out).
- CI: copy `ci/gitlab/.gitlab-ci.yml` when chosen and work through its ADAPT notes; the dependency
  bot's config file from the preset's snippet when chosen.
<!-- MODULE agent-seat -->
- **With the agent-seat module** (`ci/github/README.md`, "Adoption"):
  - Copy `ci/github/.github/` to `.github/`, before step 11 removes `ci/`. Into a `.github/` the
    project has, copy entry by entry, and put each file both hold to the author.
  - In `.github/rulesets/agent-namespace.json`, `claude/` of `refs/heads/claude/**/*` takes the
    strand prefix of question 12 and `dependabot/` of `refs/heads/dependabot/**/*` the bot prefix
    of question 13; without a bot delete that line and the comma before it.
  - `{{CREATE_GITHUB_APP_TOKEN_COMMIT}}` in `.github/workflows/land.yml`: the commit of the current
    release of `actions/create-github-app-token`, read-only with
    `git ls-remote --tags https://github.com/actions/create-github-app-token`.
    - Of the tags `v<major>.<minor>.<patch>` take the highest: the hash of its `^{}` line where it
      has one, else its own.
    - Where `github.com` is out of reach, the token stays, step 8 expects it, and step 9 seeds it.
  - `{{LANDING_APP_ACTOR_ID}}` in `.github/rulesets/main.json` and `agent-namespace.json`: the
    actor id of question 13 where the landing App exists; then `jq .` on each ruleset.
    - Without the App the token stays in both, step 8 expects it, and step 9 seeds the App with
      the rest of the setup.
    - The token stands unquoted, so both rulesets are no valid JSON until it is filled.
  - The rulesets are imported into GitHub by hand and never reformatted: where `./verify format`
    runs a formatter over the whole tree, add `**/.github/rulesets/` to its ignore file (with
    js-ts-npm create `.prettierignore` with that line), which also covers the copies under
    `ci/` and `.template/baseline/`; keep it whether or not the token is filled.
<!-- /MODULE agent-seat -->

Where the java-gradle preset is applied, per its README:

<!-- MODULE acceptance -->
- **With the acceptance module** ("Web-UI acceptance (browser half)"):
  - Copy `stacks/java-gradle/acceptance/` to `src/test/java/<base package as a path>/acceptance/`
    and resolve `{{BASE_PACKAGE}}` in every copy to the base package of question 13.
  - Run the project's own formatter over the copies.
  - Merge the section's dependency lines, its `junit-platform.properties` lines and its `test`
    block lines into the project's files; never replace a file the project has.
  - Set `OrphanedBrowserSweep.SELECTOR` to the sweep form of question 13: `MARKER` ships,
    `ANCESTRY` is for the devcontainer variant only.
  - Copy `stacks/java-gradle/grid/` to `grid/` only on the author's request.
  - On another preset nothing is copied: the project's own browser library drives the UI
    (stacks/README.md, "What a preset supplies").
<!-- /MODULE acceptance -->
- **With the KB guard** ("KB guard (optional)"):
  - Copy `stacks/java-gradle/kb-guard/` to `src/test/java/<base package as a path>/kbguard/`,
    resolve `{{BASE_PACKAGE}}` the same way and run the project's formatter over the copies.
  - Set `KbCitationGuardTest.FRONTEND_SOURCE_ROOT` to the frontend source root of question 13,
    `""` without a frontend.
  - Merge the section's `inputs.dir` lines into the build file's `test` task, the second one only
    where that root is set and with the same path. A subproject that runs the tests: the
    section's closing bullets.

## 8. Resolve the placeholders

- First delete the "Placeholder key" section from `docs/knowledge-base/README.md`: it is an
  adoption-time aid that documents the tokens as literal text and must not survive the replace.
- Then search-replace the three tokens with the interview values, EXCEPT in `.template/`, `env/`,
  `stacks/`, `ci/`, `TEMPLATE-README.md`, `START-HERE.md`, `.claude/skills/adopt/` and the
  `remove` paths of step 5 (deleted or kept pristine in step 11).
  <!-- MODULE agent-seat -->
  - With the agent-seat module also `{{REPOSITORY_URL}}`: `https://github.com/<owner>/<repository>`,
    built from the `<owner>/<repository>` of the configured URL that question 13 accepted, without
    `.git` and without any user part.
  <!-- /MODULE agent-seat -->
- The fourth token, `{{BASE_PACKAGE}}`, was resolved in step 7, where the Java files were copied.
- Verify: `grep -rnE "[{][{][A-Z_]+[}][}]" .` finds hits only inside those excluded paths. The
  pattern matches a token alone, so the braces of another tool (a Docker format string, a
  workflow expression) are no hits.
  <!-- MODULE agent-seat -->
  - With the agent-seat module the hits step 7 left are expected too, each seeded in step 9:
    `{{LANDING_APP_ACTOR_ID}}` in `.github/rulesets/main.json` and `agent-namespace.json` where
    the landing App does not exist yet, `{{CREATE_GITHUB_APP_TOKEN_COMMIT}}` in
    `.github/workflows/land.yml` where `github.com` was out of reach.
  <!-- /MODULE agent-seat -->

## 9. Work the ADAPT notes, write "Project choices", seed the living docs

`grep -rn "ADAPT:" .` outside `.template/` and `.claude/skills/adopt/` and split the hits:

- **Resolve now** (the interview answered them): the SHARED language line, changed identically in
  `CLAUDE.md` AND `docs/knowledge-base/agent-entry-template.md` (the `pre-commit` hook checks the
  identity); the one-liner spots; the devcontainer, module, hooks and settings values from steps 4
  to 7; the acceptance runner, feature-file location and scenario language in `testing.md`; the
  `verify` subcommand table in `build-run-deploy.md`; the ticket system and canonical split in
  `open-questions.md`.
  - The harness model of question 4: with the in-memory model apply handbook §5's ADAPT note to
    every place it lists (the rule, its completeness item, §6's acceptance line, testing.md "The
    acceptance runner", the scenario falsifier's completeness item, the frontend falsifier's
    description and UI bullet, the playbook's UI paragraph; the step shape was wired in step 7);
    with the shipped rule the texts stand. Either way the answered note goes.
  - Another strand prefix than `claude/` (question 12): handbook §7's ADAPT on the prefix, its
    `grep -rn "claude/<topic>"` across the docs, agents and skills, and `CLAUDE.md`.
  - No `origin` (question 12): handbook §7's ADAPT note under "`main` follows `origin/main`": the
    fetch, the sync and its stops go, and with them every call of `hooks/sync-main`, which
    refuses to run without `origin`.
  - `CLAUDE.md`, "Claude-Code mechanics": delete the template-dev bullet; keep the Bash-stdout
    note only where sessions run on a Windows host; then its ADAPT note goes.
  <!-- MODULE agent-seat -->
  - With the agent-seat module, the two names of question 13: in the seat marker of the KB
    README, DOC-5, as one bullet in place of its ADAPT note (`(seat: <seat>)` for the agent seat,
    `(seat: <machine>)` for the supervised machine), and as the "Machine" cells of the table in
    dev-environment.md, "Where agents run", in place of its two example rows' and the ADAPT note
    under it.
  <!-- /MODULE agent-seat -->
- **Write "Project choices"** in `CLAUDE.md`, outside SHARED: this project's choice in each of the
  six slots of agent-entry-template.md, "PROJECT CHOICES", in place of the template repository's
  own; then the ADAPT note under them goes.
  - **Landing model:** the model of question 12.
    <!-- MODULE agent-seat -->
    - With the agent-seat module: hub with seat (handbook §7, "Landing models").
    <!-- /MODULE agent-seat -->
  - **Strand prefix:** the prefix of question 12.
  - **Remote and sync:** `origin` with its fixed fetch points, or none (question 12).
  - **Commit-message confirmation:** the slot's one variant, worded as the template words it.
  - **Instruction channels:** the slot's one variant, worded as the template words it.
    <!-- MODULE agent-seat -->
    - With the agent-seat module the channel it names joins it: the strand's own pull request
      (working-with-ai-agents.md, "Instruction sources").
      - Ask the author whether an organization owns the repository; where one does, ask for the
        author's own GitHub account and name it in that line as the channel's owner, since
        without it no comment counts.
    <!-- /MODULE agent-seat -->
  - **Modules installed:** the modules kept in step 5, by fence name, or none. The KB guard is no
    module and is not listed.
  - agent-entry-template.md keeps its slots and their ADAPT note: another tool's wrapper is built
    from them.
- **Leave in place** (project content the author fills over time): the skeleton KB docs (01-05,
  07), the domain-falsifier lens, the high-risk review areas, the lenses' read paths in the
  playbook's table where the gate module is not kept, the model tiers and the scenario gate's
  round limit, the scenario calendar, host quirks, the sections of `frontend.md` that `/adopt`
  creates for the author to fill, the Definition of Done's item on texts that explain rules.
  <!-- MODULE gate -->
  - With the gate module: the block of `gate/dossier-check` holds the read paths, so none is left
    in the playbook; its test paths, runner lines, read paths and backlog shapes are the
    author's to check.
  <!-- /MODULE gate -->
  <!-- MODULE security-lens -->
  - With the security-lens module: the security lens's attack list and the way the tip starts
    for it.
  <!-- /MODULE security-lens -->
  <!-- MODULE acceptance -->
  - With the acceptance module: the parallelism and its measured time, and the grid recipe.
  <!-- /MODULE acceptance -->
  - With the java-gradle KB guard: its platform list and the example and stop lists of DOC-12.
  <!-- MODULE volumes -->
  - With the volumes module: the worktree root `/worktrees`, which the devcontainer, the settings
    and `CLAUDE.md` name literally.
  <!-- /MODULE volumes -->
  <!-- MODULE agent-seat -->
  - With the agent-seat module: the seat's reach beyond its container, what runs on its Docker
    daemon, and how many snapshots it keeps (dev-environment.md, "Where agents run" and "Docker on
    the agent seat").
  <!-- /MODULE agent-seat -->

  Collect them as seeded backlog items or tickets instead of resolving them now.
- `docs/backlog.md`, "Current status": adopted template version N (read `TEMPLATE-VERSION`) on
  today's date; first each preset step 7 did not apply and the landing branch's name where it
  stayed (step 1); then **task 0** where question 11 chose it
  (the `/bdd-catch-up` prompt, the cluster order); then the modernisation track in the order of
  question 10; then the probes and tool checks left open in steps 5 and 6, on path A the
  project's own `README.md` (`CLAUDE.md` links it, and `TEMPLATE-README.md` goes once it stands),
  the skeleton docs and the remaining ADAPT items. With a ticket system the items go there and
  the status section points at them.
  <!-- MODULE agent-seat -->
  - With the agent-seat module also these items, which no file of the repository holds the values
    of:
    - the landing on GitHub: `.template/baseline/ci/github/README.md`, "Adoption", steps 2 to 6,
      with `LAND_TARGET` of question 13, the actor id where step 7 left its token, and the token
      action's pin where step 7 left its token; then that file's "The first live trial";
      - until the actor id stands in them, the two rulesets are no valid JSON (step 7);
    - the seat machine: its container from the agent image, its deploy key, the seat's own GitHub
      App, and `AGENT_SEAT=unattended`, `AGENT_APP_ID`, `AGENT_APP_KEY` and, for snapshots,
      `AGENT_SNAPSHOT_DIR` in its environment, never in the repository (dev-environment.md,
      "Where agents run");
    - building the agent image once on the seat machine, which needs `git` 2.32 or newer, `npx`
      and Docker there (dev-environment.md, "The agent image");
    - each pin step 5 left as shipped, with the registry that was out of reach;
    - the `required_check` of `land/land`, once CI exists.
  <!-- /MODULE agent-seat -->
- `docs/open-questions.md`: every interview answer as a dated decision with a one-line rationale
  (stacks and composition, tools kept or replaced, acceptance runner, language and harness model,
  environment variant and where the sessions run, ticket system and canonical split, commit key
  pattern, CI, dependency bot, upgrade policy, conversation language, remote, landing model and
  strand prefix, each module kept or dropped with the answers it brought, the KB guard, the
  tooling directories, and on a native Windows checkout its narrowing).
- `docs/process-learnings.md`: stays an empty table.
- `docs/roadmap.md` and `docs/risks.md`: only when the seeded backlog already outgrows one screen.
  - Copy them from `.claude/skills/adopt/skeletons/` and resolve `{{PROJECT_NAME}}` in the copies.
  - The module filter skips that folder: cut from each copy by hand every block of a module that
    is not kept, both fence lines included (in `risks.md` the `agent-seat` block); a kept
    module's block stays whole (KB README, DOC-4).
  - Otherwise create neither.
  - The KB README's "Living documents" says when a project adds them.
- `docs/knowledge-base/frontend.md`: only for a project with a UI surface.
  - The analysis table's "UI surface" row shows it for an existing tree.
  - For a new project, the one-liner and stacks of questions 1 and 2 may show it; where they leave
    it open, a later session adds the file (the KB README, "Read in this order").
  - Copy it from `.claude/skills/adopt/skeletons/frontend.md`, resolve `{{PROJECT_NAME}}` in the
    copy, and add its row to the KB map.
  - Otherwise create none.

## 10. Activate and verify

- Hooks: `git config core.hooksPath hooks` (the devcontainer `postCreate` also does it; run it for
  this checkout anyway), then verify with `git config core.hooksPath`.
- Push default: `git config push.default nothing` (the devcontainer `postCreate` also does it;
  handbook §7, "`main` follows `origin/main`"), then verify with `git config --get push.default`.
- **Stage the hooks, the agent tools and `verify` executable BEFORE the self-test**, or its
  `100755` case has nothing to check and reports `skip`. On a fresh repo nothing is tracked yet,
  and a host without a filesystem exec bit (Windows) then records them `100644` on the first
  commit, whereupon git silently runs no hook at all on the next POSIX checkout, a gate that does
  not even narrate, and a tool the settings allow by its path no longer starts.

  ```sh
  git add hooks/ agent/ verify
  git update-index --chmod=+x verify
  git update-index --chmod=+x hooks/pre-commit
  git update-index --chmod=+x agent/fold
  git ls-files -s hooks/ agent/ verify
  ```

  One `git update-index --chmod=+x <file>` call per file, never a loop: repeat it for each script
  under `hooks/` except `README.md` and for each file under `agent/`. Without a root `verify`
  (step 7's build check) leave `verify` out of every line.
  <!-- MODULE gate -->
  - With the gate module also `git add gate/` and the same call for each script under `gate/`
    (not for `gate/authentication-surface`, a list).
  <!-- /MODULE gate -->
  <!-- MODULE agent-seat -->
  - With the agent-seat module also `git add land/` and the same call for `hooks/pre-push` and
    each file under `land/`, then `git add .devcontainer/agent/` and the call for its `build` and
    `self-test` (not for `Dockerfile`, `provision` and
    `install-claude-code`, which the image runs with `sh` or reads).
  <!-- /MODULE agent-seat -->
  - On path B the same holds for these paths as the strand holds them.
- **Run `sh hooks/self-test` and require every case to pass.** This is the acceptance test of the
  adoption: it proves the adapted gate still blocks what it claims to block, in *this* repo. A FAIL
  means a config value and a fixture drifted apart; fix it before committing. Check syntax too:
  `sh -n` on each hook file and on `verify`, one invocation per file.
- **Run `sh stacks/self-test` too**, before `stacks/` is deleted: it proves the `verify` scripts
  keep the contract (exit codes, caller-relative paths, the executed-test proof, missing tools)
  against stub tools, so a copied and adapted script is checked the same way the hooks are. Where
  the root holds no `stacks/` (path B), run the baseline's:
  `sh .template/baseline/stacks/self-test`.
- **Run `sh agent/self-test`:** it proves `agent/fold` in throwaway repositories. `pre-commit`
  runs it again at the adoption commit, which stages `agent/`, and shows none of its output.
  <!-- MODULE guard -->
  - With the guard module it proves the guard too and needs `timeout` and `python3` 3.9 or
    newer; without them it runs no case and says why.
  <!-- /MODULE guard -->
  <!-- MODULE gate -->
  - With the gate module run `sh gate/self-test` the same way, before `stacks/` is deleted: its
    preset cases read the `gate.env` files there.
  <!-- /MODULE gate -->
  <!-- MODULE agent-seat -->
  - With the agent-seat module `agent/self-test` proves `agent/push`, `agent/gh` and
    `agent/github-token` too. Run `sh land/self-test` and `sh .devcontainer/agent/self-test` the
    same way, the latter's `skip` line for a locale being no failure; `pre-commit` runs both again
    at the adoption commit, which stages their folders.
  <!-- /MODULE agent-seat -->
- Where step 7 installed a root `verify`: run `./verify format`, which the commit's format gate
  runs again, then `./verify all`, and read the executed-test proof. A session outside the
  devcontainer lacks its toolchain; dev-environment.md, "How to work in it", says where such a
  commit is made.
- Verify the rest of the pass: the step-8 grep is clean; `jq` clean on settings.json; relative doc
  links resolve, with two exceptions: the root-relative links inside agent-entry-template.md are
  correct as documented there, and the links inside `.claude/skills/adopt/skeletons/` resolve once
  copied to `docs/` (`frontend.md`: `docs/knowledge-base/`); the SHARED sections of `CLAUDE.md`
  and the template are identical.
  - Where step 9 copied `risks.md` or `roadmap.md`, `grep -n "MODULE " docs/risks.md
    docs/roadmap.md` lists no fence of a module that is not kept.

## 11. Cleanup (author-run) and the commit

- Hand the author exact removal commands for: `env/` (both variants; the choice is recorded),
  `stacks/`, `ci/`, `user-scope/` (if present), `START-HERE.md`, `.claude/skills/adopt/` (this
  skill with its module filter), and every `remove <path>` line of step 5. All of them but
  `user-scope/`, which never ships into a project, live on in `.template/baseline/` for the next
  upgrade. On path A also remind them to replace
  `TEMPLATE-README.md` (or the template `README.md`) with the project's own README.
  **Keep `TEMPLATE-VERSION` and `.template/baseline/`**: they are what `/template-upgrade` needs.
  - Named limit: the KB's links into `stacks/` break once the list is run; the files live on
    under `.template/baseline/stacks/`.
  <!-- MODULE agent-seat -->
  - With the agent-seat module `.github/`, which step 7 filled, stays whole: no command of the
    list names it or a file in it.
  - Named limit: the KB's two links into `ci/github/README.md` break the same way; the file
    lives on as `.template/baseline/ci/github/README.md`.
  <!-- /MODULE agent-seat -->
- Once the author has run the list, this folder is gone, so the baseline's copy proves the result:
  `sh .template/baseline/.claude/skills/adopt/module-filter verify --keep "<kept>"` must exit 0
  before the commit. It prints one line per leftover of a dropped module; show the author each,
  and never edit around one.
- Stage the whole tree after the removal list: `git add -A`, then show the author `git status`.
  - `git add -A` stages each script again with the mode the work tree reads, which can undo a
    `git update-index --chmod=+x` of step 10: run `git ls-files -s hooks/ agent/ verify` again
    and repeat step 10's call for each script that no longer reads `100755`.
  Step 10 staged only `hooks/`, `agent/`, `verify` and the kept modules' folders; the baseline,
  the docs, the settings and `.devcontainer/` come in here.
  - `git ls-files .template/baseline/TEMPLATE-VERSION TEMPLATE-VERSION` prints both paths before
    the commit: without them `/template-upgrade` has nothing to merge against.
  - `git ls-files --others .template/baseline/` prints nothing: a file the project's ignore rules
    keep out of the index (an `env/` line, say) is staged with `git add -f .template/baseline/`.
- Propose ONE one-line, present-tense commit message (e.g. `Adopt the AI-assisted development
  process template`; path B may prefer a `[doc]` tag when no build file changed), wait for the
  author's confirmation, commit. Never push.
  - A repository without a commit: this is its first commit, the one commit handbook §7 lets
    stand on the branch `git init` made.
  - On the strand of step 1: hand the author the review (the branch, this checkout's absolute
    path and the compare range: working-with-ai-agents.md, "Handing the author a review"), and
    after the author's review land it by the landing model of question 12 (handbook §7, "Landing
    models"). No worktree stands here: the local model checks `sh hooks/breach-guard --reaches
    <tip>` while the strand is still checked out (the branch it was cut from may hold no hooks),
    then switches back to that branch, runs `git merge --ff-only <tip>` and deletes the strand
    with `git branch -d`.
- Point the author at the first session: `/bdd-catch-up` where task 0 was chosen, otherwise
  `/continue`.
