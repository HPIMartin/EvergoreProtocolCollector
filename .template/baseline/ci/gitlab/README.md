# CI preset: GitLab

**Principle: local behaves like remote.** The pipeline runs `./verify all` inside the **same image
the devcontainer uses**, so a green local run and a green pipeline mean the same thing. The pipeline
adds no build knowledge of its own: every command it runs is a `verify` subcommand
([stacks/README.md](../../stacks/README.md)).

## Jobs

| Job | Runs | When |
|---|---|---|
| `devcontainer-image` | builds `.devcontainer/` with the devcontainers CLI and pushes it to the project registry | `.devcontainer/**` changed, or manually |
| `verify` | `./verify all`, publishes the test reports as artifacts | every branch and merge request |
| `vuln` | `./verify vuln`, report-only | scheduled pipelines and the default branch |
| `deploy` | the deployment, a manual placeholder until adapted | manual, default branch only |

- The image is tagged per ref, and a default-branch build is pushed as `:latest` as well. `:latest`
  is the default `DEVCONTAINER_IMAGE`, so no job has to slugify a branch name to find an image.
- A branch that changes `.devcontainer/**` builds its own tag; `verify` and `vuln` both pick it up
  through the same `rules:variables` override, so neither ever validates against a stale image.
- `needs: [{ job: devcontainer-image, optional: true }]` keeps a job from waiting on one that the
  rules did not create.
- The job is named `devcontainer-image`, not `image`: GitLab reserves `image` and other keywords as
  job names.
- `vuln` carries `allow_failure: true` on top of `verify vuln` being report-only, so a crashing
  scanner cannot block `main` (handbook §7: gating is a delivery decision, taken deliberately).
- `deploy` sets `interruptible: false`: the pipeline default would let a newer push cancel a
  half-finished deployment.

## Adoption

1. Copy `.gitlab-ci.yml` to the repo root.
2. Requires environment variant A ([env/devcontainer/](../../env/devcontainer/README.md)): the
   pipeline image **is** the devcontainer image. A variant B project replaces the `image:` keys with
   a pinned toolchain image that matches `.tool-versions`.
3. Set the test-report paths in the `verify` job's `reports:junit` list to the ones your stack
   writes; delete the rest. Every preset's `./verify all` writes them under `artifacts/test-results/`,
   except Gradle, which keeps its own `build/test-results/test/`. Both globs start with `**/`, so a
   multi-stack project's per-stack subdirectory (`frontend/artifacts/test-results/`) is covered too.
4. **First pipeline of a fresh project:** run `devcontainer-image` manually on the default branch.
   Until it has pushed `:latest`, every other job fails on a missing image; the job's second rule is
   `when: manual` exactly for this.
5. Enable a pipeline schedule for `vuln` (weekly is the usual cadence).
6. Wire the `deploy` job, or delete it and the `deploy` stage.

## Notes that bite

- `verify` must be committed executable (`git update-index --chmod=+x verify`); a `100644` script
  fails the job with "permission denied".
- `devcontainer build` bakes the image from `image`/`features`/`Dockerfile` only. Lifecycle commands
  (`postCreateCommand`) do **not** run, so anything a job depends on has to be in the image, not in
  the lifecycle hook.
- The `docker:dind` service needs privileged runners. Where the runner cannot do that, build the
  image out of band and keep only the `verify`, `vuln` and `deploy` jobs.

## Other CI systems

GitHub Actions, Azure Pipelines and Jenkins get presets on demand. The porting rule is the same one:
one job that runs `./verify all` in the devcontainer image, plus a scheduled `./verify vuln`.
