# 04: Build, Run, Deploy

> **ADAPT:** this doc is a skeleton for your build commands, quality gates, runtime
> configuration, endpoints, and deployment path: fill those in. Keep volatile facts (versions,
> dependency lists) single-sourced in the build file; this prose points at them, never copies them
> (handbook §3). The parts that carry the template's rules stay as shipped: the verify contract
> and its lock rules ("The verify script (the one entry point)") and, where the project deploys,
> the release principles ("Release & deploy").
<!-- MODULE agent-seat -->
>
> With the agent-seat module the landing workflow ("The landing workflow (`/land`)") stays as
> shipped too.
<!-- /MODULE agent-seat -->

## Build

- One entry point, `./verify` (contract below): `./verify all` is the full build with every test,
  `./verify focus <path>` the focused run. The build must **always run green** locally; quality and
  vulnerability *gating* belongs to CI/delivery, never the local build.
- **Warnings are errors:** compiler/lint warnings fail the build; the default response is fix, not
  suppress (handbook §3).

  > **ADAPT:** how your stack enables warnings-as-errors, and any categories excluded deliberately,
  > each with a one-line reason.
- **Formatting:** one shared formatter config is the single source of truth, consumed by every IDE
  *and* the build; the build fails on any deviation, and a companion task auto-fixes.

  > **ADAPT:** the formatter engine, the config file path, and how IDEs and the build consume it.
- **Linting:** lint rules stay disjoint from the formatter (no rule lives in two tools, which avoids
  parallel upkeep).

  > **ADAPT:** the linter, its config path, and which gaps it covers that the formatter cannot.
- **Coverage:** decide report-only vs. threshold-gated; a hard threshold on a young suite gates the
  wrong thing.

  > **ADAPT:** the coverage tool, report location, and whether a threshold is enforced.
- **Dependency vulnerability scan:** an on-demand task, report-only locally; gating on severity is a
  CI/delivery concern.
  - A dependency bot's version updates complement it only for the ecosystems and directories the
    bot covers (Dependabot: its listed entries; Renovate: the managers it detects).

  > **ADAPT:** the scanner, how to run it, and where its severity gate is configured.
- **Dependency updates:** a dependency bot's update lands like any other change.
  - It lands as a strand commit through the hooks, the gate and the fast-forward, never by the
    merge button.
    - Why: the merge button skips the hooks and the gate, and lands outside every landing model
      of handbook §7.
  - The strand reproduces the update with the project's own tools instead of checking out or
    cherry-picking the bot's branch, which is data (working-with-ai-agents.md, "Instruction
    sources").
  - It takes the bot's newest grouped update, plus every newer version the declared ranges allow
    when the strand lands.
  - It raises each declared range to the version it locks.
    - Why: the bot's in-range pull requests are then superseded, and none opens again right after
      the landing.
  - It claims handbook §5's build/infra exemption in its plan, as §5 requires.
  - The bot's pull request is superseded; `/plan-next` (check 4) names it.
  <!-- MODULE agent-seat -->
  - With the agent-seat module the seat closes it in that check, with
    `agent/gh pr close <number>`.
  <!-- /MODULE agent-seat -->
  - Minor and patch updates arrive grouped, a major alone.
    - Why: a pair that must move together, such as `react` and `react-dom`, moves in one pull
      request, and a major the project holds back blocks no group.

## The verify script (the one entry point)

Every project commits one executable POSIX-sh script `verify` at the repo root. Developers, agents,
the git hooks and the CI pipeline call it identically, so "local behaves like remote" is a property
of the script rather than a discipline. The stack presets in the template's `stacks/` folder ship one
per stack; a multi-stack project composes them into one root script.

| Subcommand | Does | Who calls it |
|---|---|---|
| `./verify format` | the fast format/style check; seconds, no tests | `hooks/format-gate`, in `pre-commit`, `pre-applypatch`, `post-commit` and `post-rewrite` |
| `./verify focus <path>` | the focused test run for the stack of that file; a `.feature` path runs those scenarios, `@wip` included | the TDD inner loop; counter-tests of the falsifiers |
| `./verify bdd` | the `@wip` scenarios only, i.e. the feature under development (handbook §5) | the TDD loop while the scenarios are still tagged |
| `./verify all` | the gateway build: format, lint, typecheck, unit tests, acceptance scenarios without `@wip` or `@characterization` (handbook §5), caches disabled; prints proof that the tests executed (handbook §6) | before every gate, after every rebase, the CI verify job |
| `./verify outdated` | dependency freshness report | the modernisation track, on demand |
| `./verify vuln` | vulnerability scan, report-only locally | CI on a schedule; gating is a delivery decision |
| `./verify stop` | stops what this worktree's builds leave running, such as the Gradle daemons of its own registry, never another worktree's; 0 where nothing runs | a caller that builds in a throwaway tree, when it ends; a session, before it removes a worktree |

- Exit code 0 or 1; a usage error exits 2. The output of `all` names the executed test count or the
  bare task lines, so a cached or skipped run cannot pass as a green one.
- `format` stays fast: the test run and the full build never move into it, or the TDD micro-commit
  loop pays for every commit.
- **The commands that build or test take one machine-wide lock**, so only one of them runs on the
  machine at a time (what that means for the sessions: [dev-environment.md](dev-environment.md),
  "Several sessions on one machine").
  - Why: sessions in parallel worktrees share one machine, and two builds side by side slow each
    other and can fail one.
  - `focus`, `bdd`, `all` and `vuln` take it once their arguments and tools have checked out.
  - They hold it until the run ends.
  - A usage error or a missing tool is refused before the queue.
  - `format`, which the commit hooks run, `stop` and `outdated` never wait for it.
  - A later run says that it waits, names the lock file, and waits its turn.
  - A run started inside a process that already holds the lock (one `verify` under another, or
    under `flock <lock file>`) finds the held descriptor among the ones it inherited and goes on
    instead of waiting for itself.
    - It finds it only where every process in between kept the descriptor open: a program that
      closes inherited descriptors before it starts the inner run (Python's `subprocess` by
      default, a JVM) makes the inner run wait for the outer one for good.
  - The lock file is an ADAPT line of the script, `/tmp/{{PROJECT_NAME}}-build.lock`.
    - On Docker Desktop for Windows, engine 29.9.2, the re-entry above did not hold for a lock
      file on a bind mount from the host (probed), so a moved lock file stays off such a mount;
      the default under the container's `/tmp` is safe.
  - Every worktree of the machine names the same file.
  - The script opens the file for reading only, so a file another user created still serves.
  - An unreadable lock file stops the run with exit 1.
  - The lock needs `flock` and `/proc/<pid>/fdinfo`; where either is missing (Git Bash has no
    `fdinfo`), the script prints one note and runs without it.
  - The run that took the lock releases it when it ends, also where a process it started lives on
    with the descriptor, such as a build tool's compiler server.
  - A hangup, an interrupt or a termination signal releases it too, once the foreground tool has
    ended.
    - The run then ends by that same signal and starts no further step.
      - Why: its caller sees the run killed, so a shell loop over `./verify` stops at Ctrl-C.
      - Where the signal cannot end the run, as for a container's first process, it exits 1.
    - A termination sent to the script alone waits for that tool.
    - `timeout` and Ctrl-C signal the tool as well.
    - A process that keeps building after the run then builds beside the next run.
    - A run killed without its exit handler (`SIGKILL`) leaves the lock to the processes holding
      the descriptor, until the last of them ends.
    - A named limit: `all` of the java-gradle preset and of the composed script keeps its results
      in a temporary file, and a signal between the file's creation and the trap that removes it
      leaves the file behind (argued, not run).
  - The lock serializes the runs of one machine or container.
    - Why: each container has its own `/tmp`, so two containers (two clones of one project on
      Docker Desktop) hold two locks.
  - Never delete the file while a build runs.
    - Why: the next run would start beside the holder.
  - A run that waited on a deleted file notices and queues on the new one.

> **ADAPT:** the stack preset supplies the script body; list here the commands each subcommand runs
> in this project and how `focus` maps a path to its runner.

## Git hooks (local enforcement)

The repo ships POSIX-sh git hooks in [`hooks/`](../../hooks/README.md), activated via
`core.hooksPath` (hooks are **not** shared by clone). Ensure they are active in every checkout:

```sh
git config core.hooksPath hooks
```

- The path is relative on purpose.
  - Why: git resolves it against the worktree it runs in, so every worktree runs its own `hooks/`.

Beside it every checkout sets `git config push.default nothing`, so a bare `git push` pushes
nothing (engineering-handbook.md §7, "`main` follows `origin/main`"). The devcontainer variant
wires both commands into `postCreate` so fresh containers get them automatically; on the native
variant, and in any existing checkout, run them once by hand
([dev-environment.md](dev-environment.md)).

The suite is described in [`hooks/README.md`](../../hooks/README.md); the short version:
`content-gate` and `format-gate` hold the checks, the hooks that run before a commit exists
(`pre-commit`, `pre-applypatch`, `pre-merge-commit`) refuse outright, and the ones that can only run
after git moved the refs (`post-rewrite`, `post-commit`, `post-merge`) **record** the offending sha
so `pre-commit` refuses to build on it. `commit-msg` enforces the handbook §7 message rules, and
`self-test` proves the whole thing blocks rather than narrates.

`pre-commit` also runs `agent/self-test` whenever a commit touches `agent/`, as it does for the
other tooling directories ([hooks/README.md](../../hooks/README.md), "The hooks").
<!-- MODULE guard -->
With the guard module that self-test covers the shell-shape guard too and needs `python3`
([dev-environment.md](dev-environment.md), "Python for the guard"): a host without one commits such
a change in the container, and `assemble.sh` packages there too.
<!-- /MODULE guard -->
<!-- MODULE agent-seat -->
- With the agent-seat module that self-test also covers `agent/push`, `agent/gh` and
  `agent/github-token`.
- It then needs `jq` and `openssl` too, and refuses without them before any case runs.
- `pre-commit` also runs `land/self-test` whenever a commit touches `land/` or the landing
  workflow `land.yml` ("The landing workflow" below), and refuses such a commit when it is missing.
  - It needs `jq`: a host without one commits such a change in the container, and `assemble.sh`
    packages there too.
- `pre-commit` also runs the agent image's `self-test` whenever a commit touches its folder, and
  refuses such a commit when that `self-test` is missing
  ([dev-environment.md](dev-environment.md), "The agent image").
  - The folder is `env/devcontainer/.devcontainer/agent/` in the template and
    `.devcontainer/agent/` in an adopted project; each is a leg of its own.
  - It needs `git` and `ssh-keygen`, and refuses without them before any case runs.
- `hooks/pre-push` gates every push, the author's and an agent's alike: what it refuses and its
  limits, [`hooks/README.md`](../../hooks/README.md), `pre-push`.
<!-- /MODULE agent-seat -->

> **ADAPT:** `hooks/format-gate` calls `./verify format` and needs no adaptation; `hooks/content-gate`
> (host-path allowlist, production-source tree, smell patterns) and `hooks/self-test`'s matching
> fixtures do. `grep -rn "ADAPT:" hooks/`.

`--no-verify` bypasses them; reserve it for a genuine emergency on the author's word (the one
sanctioned non-emergency use is the `/pause` `[wip]` parking commit, handbook §7). The hooks are a
git-level safety net; the full `./verify all` with tests remains the gate for landing on `main`.

<!-- MODULE agent-seat -->
## The landing workflow (`/land`)

The author lands a reviewed seat strand by commenting `/land <sha>` on its pull request; the
workflow pushes exactly that commit onto the target as a fast-forward
([engineering-handbook.md](engineering-handbook.md) §7, "Landing models").

- **Where it lives:**
  - [`land/land`](../../land/land) holds every decision; its header lists what it lands and every
    refusal.
  - [`land/github-api`](../../land/github-api) makes the REST calls.
  - [`land/self-test`](../../land/self-test) proves both against a local bare remote and a stubbed
    API, with no network: `sh land/self-test`, which needs `jq`.
  - The workflow `land.yml` and the rulesets ship as the CI preset
    [`ci/github/`](../../ci/github/README.md), which `/adopt` copies to `.github/` (its step 7).
- **Trigger:** a new comment (`issue_comment`, `created`).
  - GitHub runs it with the default branch's workflow file, and the job fetches `land/` from the
    default branch's head (`github.sha`), never from the pull request.
  - Why: a strand cannot rewrite the landing it is landed by.
- **It ignores, with no answer,** a comment that is not new, not on a pull request, not `/land`
  alone or followed by ASCII white space, not the repository owner's, not a `User`'s, or posted
  through an app.
  - Why: an agent's comment, through the seat's App or any other, is never the owner's command.
  - A `/land` followed by a no-break or other Unicode space passes the job's `if` and is then
    ignored with no answer.
- **It lands only when all of these hold, checked in this order;** it answers every refusal on the
  pull request:
  - the run is the workflow run's first attempt (`GITHUB_RUN_ATTEMPT` is 1, which a re-run of
    any of its jobs raises);
    - Why: a re-run replays the stored comment, one the owner has since deleted included, under
      the `land/` scripts of the first run's commit; the answer asks for a new `/land`.
  - the `branch_prefix` line of `land/land` is not empty;
  - the comment reads `/land`, one space and all 40 lowercase hex digits, on one line, with
    nothing after them but ASCII white space;
    - Why: an agent can craft a second commit that shares a short prefix.
  - the target is `main` or `<prefix>land-probe`;
  - the pull request is open, its head lives in this repository and its base is the target;
  - the head branch is `<prefix><topic>`, a name `git check-ref-format` accepts, and not the target;
  - the head is the commanded commit;
  - the head's check runs can be read and fit on one page of 100;
    - an answer of the wrong shape (no JSON, a `total_count` that is no number, `check_runs` no
      array, an element no object) is refused as unreadable;
  - where `required_check` names a check, a run of that name from the app `required_check_app`
    names is completed as success;
  - every check run is completed as success, neutral or skipped;
  - the commit statuses can be read, and their combined status is success where any status
    exists;
    - an answer whose `total_count` is no number or whose `state` is no string is refused as
      unreadable;
  - the fetched head branch still holds the commit;
  - the target is an ancestor of the head.
- **An answer that names check runs** reduces each name to letters, digits, spaces and `._()+-`,
  every other character becoming `_`, and posts it inside a code span.
  - Why: a run's name then carries no landing line, mention or link into the App's answer.
- **It then** pushes the commit onto the target without force, deletes the head branch with a
  lease on that commit, and answers with the result on the pull request.
  - A branch that moved after the landing is kept, and the answer names it.
  - It never pushes a tag and never deletes the target.
- **Target:** the repository variable `LAND_TARGET`, `main` or `<prefix>land-probe`; any other
  value refuses.
  - An unset or empty `LAND_TARGET` stops `land/land` with exit 2 before it reads the event, so
    the pull request gets no answer.
  - The probe branch proves the workflow end to end without touching `main`.
- **Required check:** the `required_check` ADAPT line of `land/land`, empty by default, so a head
  with no check run lands.
  - Once CI exists the project names one there, and the head must carry a run of that name
    completed as success.
  - The `required_check_app` ADAPT line, `github-actions` by default, names the app that run must
    come from, so a run of the same name from another app does not count.
    - A named limit: a workflow the seat pushes on a strand's branch runs as `github-actions` too
      and can post a run of that name, so the check does not guard against the seat itself
      ([git-state.md](git-state.md), "What GitHub enforces").
  - `land/land` reads the check runs that exist when the command runs: a workflow GitHub has not
    started yet, or a job waiting behind `needs`, has none yet, and every run that exists may read
    green.
    - So `required_check` names the pipeline's last job, or a job that needs every other one;
      only such a check holds a landing until the whole pipeline has finished.
- **Token:** an installation token of a landing GitHub App the author owns, minted in the workflow
  by `actions/create-github-app-token`, pinned by commit.
  - The App's client id is the repository variable `LAND_APP_CLIENT_ID`; its private key is the
    secret `LAND_APP_PRIVATE_KEY` of the environment `landing`.
  - That environment's deployment branch rule admits `main` alone, so a workflow on a strand's
    branch never reads the key.
  - The workflow's own `GITHUB_TOKEN` gets no permission.
  - The token carries the App's Workflows write permission.
    - Why: GitHub refuses an App's push that changes a file under `.github/workflows/` without
      it, so a strand that changes a workflow could not land.
  - The token never stands on a command line ([engineering-handbook.md](engineering-handbook.md)
    §7, "No secrets"):
    - git reads it from an `http.extraheader` in `GIT_CONFIG_*` variables, masked in the log;
    - `land/github-api` hands it to curl in a header file of mode 0600.
  - The App bypasses the `main` and `agent-namespace` rulesets ([git-state.md](git-state.md),
    "What GitHub enforces"), so `land/land` alone keeps its push to a fast-forward.
    - A bypass covers a whole ruleset, and both hold `update`, `deletion` and `non_fast_forward`
      together, so the App's token may force-push or delete `main` and every branch outside
      `<prefix>` and the bot's prefix; no rule on GitHub refuses that.
    - A narrower ruleset, which lets the App bypass `update` on `main` alone, is on the first live
      trial's list ([`ci/github/README.md`](../../ci/github/README.md)).
- **Serialized:** one landing at a time: the job, not the workflow, joins the concurrency group
  `land`.
  - Why: a run whose job the `if` skips then joins no group, so a comment that is no `/land`
    cancels no waiting landing.
    - Derived from GitHub's documentation, not seen live: the first live trial checks it.
  - A waiting landing whose base the running one moved is refused as no fast-forward.
  - GitHub keeps one job waiting and cancels an older waiting one for a newer one.
    - Any owner comment that starts with `/land`, in any case (`startsWith` ignores it), passes
      the job's `if`, so a newer one replaces a waiting `/land`, which is then commented again.
- **An organisation's repository:** its owner is no user, so no `/land` is honoured until the
  project lists its permitted landers in a repository variable.
  - The commenter is then checked against it in place of the owner check, in `land/land` and in
    the job's `if` of `land.yml`; an ADAPT note in both says so.
- **Proof:** the template's own repository runs no workflow, so the first live run is the first
  adopter's ([`ci/github/README.md`](../../ci/github/README.md), "The first live trial").
<!-- /MODULE agent-seat -->

## Probe location (throwaway agent code)

Falsifier agents and the implementer need somewhere to put code that is **not** meant to stay:
probing a library's real behavior before writing a helper, reproducing a suspected race by hand.

- It lives **outside the test tree, outside `check`/`build`, and outside git**: a leftover probe then
  cannot turn the build red, and `git add -A` cannot commit it.
- It has **two committed tasks**: one to run it, one to clear it. The clean-up task is what lets an
  agent get rid of its own probes without ever running `rm` (handbook §7): the scope lives in
  reviewed, committed build code instead of in a permission rule.
- Nothing under it is reviewed or shipped.

> **ADAPT:** name the directory and the two tasks for your stack, and gitignore the directory
> (example, Gradle: a `src/probe/java` source set on the test classpath, `./gradlew probe` to run it,
> a `Delete` task `./gradlew clearProbes` to clear it). Delete this section only if your agents have
> no way to run code at all.

## Run

> **ADAPT:** how to run the app locally and via container: the run command or image build, exposed
> ports, mounted volumes, and where state persists. Remember the pinned-environment rule
> ([dev-environment.md](dev-environment.md)): nothing runs through an ad-hoc host toolchain.

## Runtime configuration & secrets

Secrets (credentials, tokens, keys) are injected via the environment or a secret store at runtime,
**never** committed and never baked into an image (handbook §3). Machine-specific values live in
gitignored `*.local.*` files.

> **ADAPT:** a table of runtime settings: name, default, how it is bound (env var, config file),
> and what it controls. Mark anything still hard-coded as a tracked backlog item.

## Endpoints / entry points

> **ADAPT:** a table of the HTTP endpoints (or CLI commands) the app exposes: method + path,
> purpose, and auth requirements. Include the health/monitoring endpoint if any.

## Scheduled jobs

> **ADAPT:** each scheduled/background job: trigger, interval, what it runs, and how tests bypass
> the schedule. Delete this section if none exist.

## Release & deploy

> **ADAPT:** delete this section if nothing is deployed. Otherwise keep the principles and add this
> project's commands to each group; container, ssh or host details stay this project's examples.

**Versioning**

- **One number, one source:** the build file holds the version; the git tag `v<version>` and the
  artifact's tag repeat it.
- **The release tag goes on the revision the deployed artifact names**, read off the running
  artifact, not on the commit that set the version.
  - Why: the version commit may sit far behind the release and carry none of its state.
- **The build labels the artifact with its version and revision** (for an image, the OCI labels
  `org.opencontainers.image.version` and `org.opencontainers.image.revision`).
- **A build between releases is tagged `<version>-<short sha>`.**
  - The bare `<version>` tag is reserved for the release revision, so it always names one source
    state.
- **Annotated tags are the author's to push** (handbook §7).
  - Why annotated: the tag carries its own author and date.
- **Tagging is the author's release step, on the released revision.**
  - An agent documents the tag's command and never runs it.

**The release checklist, in order**

1. Gate: the release revision is green in `./verify all`, and its artifact builds
   ([dev-environment.md](dev-environment.md), "Production image").
2. Dry run, as a step of its own, scheduled with time to read its log.
   - A dry run is a rehearsal, not a print-out: every read really runs, and every change is
     printed with its resolved arguments and skipped.
3. Rehearsal of the deploy and the rollback ("Rehearsal" below).
4. Release comparison of the previous release against the candidate
   ([testing.md](testing.md), "Release comparison"), where the project keeps that frame.
5. Rollback statement: what a rollback moves back, for the one step it was proven on ("Rollback"
   below).
6. Deploy, behind its pre-flights ("Deploy pre-flights" below).
7. Verify ("Verify" below).
8. Tag ("Versioning" above).
9. The running state recorded in a trailing `[doc]` commit ("Which release runs" below).

**Deploy pre-flights**

- **Every refusal comes before the stop**, so a refused run leaves the service up.
- **A check that needs no transferred artifact comes before the first transfer**, and a run
  refused there ends saying "Nothing was transferred or stopped".
- **A failed transport is reported with the tool's own reason.**
- **A first remote command that fails after the transport signed in is reported with its exit
  status.**
- **A guard is never weakened to get a release out.** The way past a refusal is the author's call,
  recorded as a decision row in [open-questions.md](../open-questions.md).
- **A copy is checked by its own exit status first.**
- **A size that cannot be read stops the run.**
  - Why: two unreadable sizes compare equal, so a size comparison after a failed copy passes.
- **A refusal over file rights names the narrowest ownership that works as its remedy**, never a
  mode open to every account such as `777` or `666`.
  - Why: an open mode lets every account on the host rewrite or delete the data.
- **A deploy script carries a self-test of its guards:**
  - stubs stand in for the transport and the target;
  - each guard gets a good state that passes and a bad state that stops the run;
  - the assertions read the journal of commands that reached the target, never what the script
    printed.

**Rehearsal** (before every release; nothing on the target is touched)

- A local stand-in replaces the target host.
- The predecessor runs from the previous release's artifact, on a copy of a snapshot taken before
  the candidate's migrations.
- A transport shim replaces the remote shell and runs each command locally.
- Throwaway credentials, never the production ones.
- Both the deploy and the rollback run.
- The blind spot is named: what the stand-in shares with the workstation cannot show what differs
  on the target (example: one container engine for both sides cannot show the target engine's
  image store).
- The copy gets the narrowest ownership that works (the runtime user's), never a mode open to every
  account such as `777` or `666`.
  - Why: "Deploy pre-flights" above, the bullet on a refusal over file rights.

**Rollback**

- **A rollback moves two things back, and they are separate decisions:** the artifact and the data.
  - The artifact going back is safe on the data the release ran on while schema changes expand
    only ("Schema changes expand only" below).
  - The data going back discards every write since its backup.
- **The rollback statement holds for one proven step**, from this release back to the previous one.
  - The rollback step reads the target artifact's version label and refuses any other target
    before anything stops.
  - A rollback to another release, or onto an artifact without labels, is the author's decision
    and runs by hand.
- **Restore the data before the old artifact starts:** the new release may have written what the
  old one does not expect.
- **Keep the rollback target:** the previous release's artifact is never deleted on the target.

**Verify**

- **Read the top-level health status only.**
  - Why: a composite health answer nests component statuses that can read healthy while the
    service has not yet run once, so a check over the whole body passes too early.

**Schema changes expand only**

- The new column or table goes beside the old, never in its place.
- The release writes both, the old exactly as the previous release writes it.
- At every start the new release fills each value the new still lacks, reading it from the old,
  so rows the previous release wrote after a rollback catch up.
- The way from the new release back to the previous one and forward again is proven on a copy of a
  snapshot, each release running its own code.
- The old goes later, as its own backlog row, once the release is stable.
- Why: while the old stands, the previous release stays a working rollback target; the drop ends
  that way back.

**Which release runs**

- **The revision label is the authoritative answer**, read off the running artifact; a tag can
  move or be reused, the label cannot.

> **ADAPT:** one line, kept current by each release's trailing `[doc]` commit: the version that
> runs, its revision label and the rollback target. Git shows when it changed.

**Delivering without CI**

- The default is handbook §7's deliver rule.
- Only a project without CI or a registry to deliver from may ship a locally built artifact, and
  only when all three hold:
  - it was built from a revision that passed the gate;
  - it carries the labels that the bullet "The build labels the artifact with its version and
    revision" names;
  - after the transfer, a fingerprint of its layer digests and those labels is compared on both
    machines before the running release is touched.
- An image ID is no basis for that comparison: one image can carry a different ID in each
  machine's image store.

## CI / dev environment

**Local behaves like remote** because both run the same script in the same image: the pipeline's
verify job executes `./verify all` inside the image built from `.devcontainer/`, so a green local
`./verify all` in the devcontainer predicts the pipeline. The template ships this as the GitLab CI
preset (`ci/gitlab/`, applied at adoption): an image job that rebuilds the devcontainer image into
the project registry when `.devcontainer/` changes, the verify job on every branch and merge
request with the test reports as artifacts (`verify all` writes them under `artifacts/test-results/`,
a Gradle project under `build/test-results/`), a scheduled `./verify vuln` that reports rather than
gates, and a manual deploy placeholder, wired along "Release & deploy" above. Other CI systems get
presets on demand. The principle
stands regardless: CI gates, the local build stays green and ungated.

> **ADAPT:** the actual CI status (the pipeline file, or explicitly "none yet" with the rationale),
> the registry the image job pushes to, and which findings of the scheduled scan gate delivery.

Dev environment: the project's chosen variant, devcontainer or native pinned toolchain; see
[dev-environment.md](dev-environment.md).

<!-- MODULE acceptance -->
## The browser grid (acceptance scenarios in other browsers)

- A Selenium Grid on the Docker daemon the session reaches through the host's socket, running the
  scenarios in Firefox, Chrome and Edge ([testing.md](testing.md), "The browser half").
  - Test-only; a project that never runs its scenarios in another browser skips it.

```sh
grid/grid up
ACCEPTANCE_GRID=http://selenium-hub:4444 ACCEPTANCE_BROWSER=chrome ./verify bdd
grid/grid down
```

- `grid/grid up` starts a hub and one node per browser, all on one pinned release tag.
  - It waits until the hub reports the grid ready.
  - Ready is the hub's health check, the images' own `check-grid.sh`.
  - The check asks `/status` for `ready`.
  - The wait lasts at most `wait_seconds` (120 s).
  - It then reads the hub's health itself: on compose 5.1.3, with the images at
    `4.49.0-20260909`, compose's wait returned 0 on a hub still starting.
  - Where the hub is not ready, it exits 1, joins nothing and names the hub's log and
    `grid/grid down`.
  - Then it joins the session's own container to the grid network under the service-host alias,
    the name a browser in the grid opens the scenario's service by.
  - `grid/grid down` leaves the network and stops the grid.
  - A session the grid cannot place yet waits in its queue, 300 s in the pinned hub image
    (`SE_SESSION_REQUEST_TIMEOUT`).
  - It refuses with exit 1, naming the problem and starting nothing, where the `docker` command is
    missing or it cannot name its own container.
  - `ACCEPTANCE_BROWSER` is `firefox` (the default), `chrome` or `edge`; `focus` and `all` take the
    same variables. Without a grid only the local Firefox runs.
- The service under test listens on all interfaces: a browser in another container opens it by
  the alias.
- An attended-session recipe only: it connects the session's own container to the grid network
  through the host's Docker daemon, so no unattended agent runs it
  ([dev-environment.md](dev-environment.md), what the host socket hands over).
- Details, the values and the self-test: the java-gradle preset
  ([stacks/java-gradle/README.md](../../stacks/java-gradle/README.md), "Web-UI acceptance
  (browser half)").

> **ADAPT:** the pinned Selenium image tag (`SELENIUM_TAG` in `grid/grid`), the bound of the wait
> for a ready grid (`wait_seconds`, 120 s, with the hub's `start_period` in `grid/compose.yaml` at
> least as long, so a slow start reads starting rather than unhealthy), the grid network name
> and the service-host alias (shared with the preset's `SERVICE_HOST_ON_THE_GRID`). The compose
> project, the network and the alias are constants: on a Docker daemon that two projects share,
> make the names unique per project, or both use one grid. Two sessions of one project on one
> daemon share them too: both join under one alias, so a browser may open the other session's
> service, and one session's `grid/grid down` stops the other's grid; run the grid from one
> session at a time.
<!-- /MODULE acceptance -->
