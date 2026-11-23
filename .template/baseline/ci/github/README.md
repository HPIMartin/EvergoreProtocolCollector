# CI preset: GitHub (the landing workflow)

- Part of the `agent-seat` module.
- How it lands and what it refuses:
  [build-run-deploy.md](../../docs/knowledge-base/build-run-deploy.md), "The landing workflow".
- What the rulesets enforce and whom they bind:
  [git-state.md](../../docs/knowledge-base/git-state.md), "What GitHub enforces".

## What the preset holds

- [`.github/workflows/land.yml`](.github/workflows/land.yml): the workflow that runs `land/land`.
- The rulesets [`main.json`](.github/rulesets/main.json),
  [`agent-namespace.json`](.github/rulesets/agent-namespace.json) and
  [`tags.json`](.github/rulesets/tags.json), under `.github/rulesets/`.
- The scripts stay at the root: [`land/land`](../../land/land),
  [`land/github-api`](../../land/github-api) and [`land/self-test`](../../land/self-test)
  (`sh land/self-test`, no network, needs `jq`).
- The template keeps the preset here, not under the root's `.github/`, so nothing runs in the
  template's own repository.

## Adoption

1. With the `agent-seat` module, copy `ci/github/.github/` to the root's `.github/` (`/adopt`
   step 7).
2. Create a GitHub App owned by the author, installed on the repository, with the repository
   permissions contents, pull requests, issues and workflows write, checks and statuses read
   (why: [build-run-deploy.md](../../docs/knowledge-base/build-run-deploy.md), "The landing
   workflow").
3. Create the environment `landing`, its deployment branch rule admitting `main` alone, holding the
   secret `LAND_APP_PRIVATE_KEY` (the App's private key): a workflow on a `claude/` branch never
   reads it.
4. Set the repository variables `LAND_APP_CLIENT_ID` (the App's client id) and `LAND_TARGET`.
5. Put the App's actor id in place of `{{LANDING_APP_ACTOR_ID}}` in `main.json` and
   `agent-namespace.json`, then import the three files in Settings, Rules, Rulesets. The admin role
   (`RepositoryRole` 5) is the other bypass. Where the App exists at adoption, `/adopt` step 7
   writes the id.
6. Pin `actions/create-github-app-token` by the full commit hash of its current release in
   `land.yml` (`/adopt` step 7).

## ADAPT points

- **The agent prefix:** `claude/` in `agent-namespace.json`'s exclude and the `branch_prefix` line
  of `land/land` name the same prefix (`/adopt` steps 5 and 7).
- **The dependency bot:** `refs/heads/dependabot/**/*` in `agent-namespace.json` is the project's
  bot's prefix; delete it where no bot pushes branches (`/adopt` step 7).
- **The required check:** the `required_check` line of `land/land`, empty until CI exists; the
  `required_check_app` line names the app whose run counts, `github-actions` for a workflow job.
  - `land/land` reads only the check runs that exist when the command runs, so name the
    pipeline's last job, or a job that needs every other one: an earlier job's success does not
    hold a landing while later jobs have no run yet.
- **An organisation's repository:** its owner is no user, so no `/land` is honoured until the
  project lists its permitted landers in a repository variable in place of the owner check, in
  `land/land` and in the job's `if` of `land.yml`.

## The first live trial

- Nothing here runs on GitHub, so the first adopter makes the first live run.
- Set `LAND_TARGET` to `<prefix>land-probe`, push that branch from `main`, open a pull request of a
  strand based on it and comment `/land <sha>` with the head's full hash.
- While a landing runs, post a comment that is no `/land`: a waiting `/land` must still land
  ([build-run-deploy.md](../../docs/knowledge-base/build-run-deploy.md), "The landing workflow").
- The probe branch moves to the head and the strand's branch is deleted.
- Then prove what the boundary refuses; each step must fail as named, and none may move `main`:
  - a push to `main` with the seat's deploy key is refused by the hub;
  - a push of a tag with the seat's deploy key is refused by the hub;
  - a workflow on a `<prefix>` branch that names `environment: landing` is refused the
    environment, so it never reads `LAND_APP_PRIVATE_KEY`;
  - a `/land <sha>` comment by any account but the owner gets no answer and lands nothing;
  - a `/land` with a SHA other than the head's, such as the head before a later push, is refused
    with an answer.
- The App's bypass reaches further than the landing needs: a bypass covers a whole ruleset, so
  the App's token may force-push or delete `main` and every branch the two rulesets cover.
  - Try a narrower set: `update` on `main` in a ruleset of its own that the App bypasses,
    `deletion` and `non_fast_forward` on `main` in one only the admin role bypasses, and `main`
    excluded from `agent-namespace`, whose bypass list then drops the App; then land on the probe
    again.
- Then set `LAND_TARGET` to `main`; with the narrower set, the first landing on `main` shows that
  it admits the App's fast-forward.
