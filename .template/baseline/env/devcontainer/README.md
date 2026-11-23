# Environment variant A: devcontainer (recommended)

Fully virtualized development: no toolchain is installed or run on the host. Everything (builds,
tests, the app, the AI agents) runs inside the container defined in
[.devcontainer/devcontainer.json](.devcontainer/devcontainer.json).

## Adoption

1. Copy the `.devcontainer/` folder (including `.claude-home/`) to the repo root.
   <!-- MODULE agent-seat -->
   - With the agent-seat module its `agent/` folder, the agent image, comes along; without the
     module, leave `agent/` out.
   <!-- /MODULE agent-seat -->
2. Work through the ADAPT comments in `Dockerfile` (the base image) and `devcontainer.json`: pinned
   toolchain features for your stack (the `stacks/` presets name them), editor extensions, and the
   warm-up build appended to `postCreateCommand`.
   - A module's lines in `devcontainer.json` stand in `// MODULE <name>` fences; `/adopt` keeps or
     drops each block with its module (its step 5).
   <!-- MODULE guard -->
   - The guard module's block is the pinned `python` feature.
   <!-- /MODULE guard -->
   <!-- MODULE volumes -->
   - The volumes module puts the agent worktrees on a named volume; where it pays:
     dev-environment.md, "Worktrees on a volume".
   - Its blocks are `AGENT_WORKTREES` and the volume `{{PROJECT_NAME}}-worktrees` on `/worktrees`;
     its `chown` and `gc.worktreePruneExpire` links of `postCreateCommand` stand in no fence, and
     `/adopt` cuts them out of that line without the module.
   <!-- /MODULE volumes -->
   <!-- MODULE agent-seat -->
   - The agent image's pins in `agent/` and the `{{REPOSITORY_URL}}` in the `source_url` line of
     `agent/build` are ADAPT points too
     ([dev-environment.md](../../docs/knowledge-base/dev-environment.md), "The agent image";
     `/adopt` steps 5 and 8).
   <!-- /MODULE agent-seat -->
3. Keep the root `.gitignore` rules for `.devcontainer/.claude-home/` (shipped with the template).
4. Record the choice in `docs/knowledge-base/dev-environment.md` (its ADAPT note), then delete `env/`.

## What this variant guarantees

- The toolchain version is pinned in the devcontainer features, at the value the build's toolchain
  declaration states (dev-environment.md, "Toolchain version").
- `postCreateCommand` activates the git hooks (`git config core.hooksPath hooks`) and sets the
  push default (build-run-deploy.md, "Git hooks") in every fresh container.
- The per-project Claude home (the `.claude-home/` mount) survives container rebuilds; its
  contents are in dev-environment.md, "Claude Code's home".
- Agents execute inside the container, with its toolchain; its limits are in dev-environment.md,
  variant A.
<!-- MODULE volumes -->
- With the volumes module, the agent worktrees live on the named volume
  `{{PROJECT_NAME}}-worktrees` on `/worktrees`, which survives container rebuilds
  (dev-environment.md, "Worktrees on a volume").
  - `postCreateCommand` hands the volume to the container user and keeps git's automatic gc from
    dropping the registration of a worktree there.
<!-- /MODULE volumes -->
<!-- MODULE agent-seat -->
- With the agent-seat module, the agent image (`agent/`) is a layer on this folder's image for the
  agent seat, so the seat runs this folder's toolchain
  ([dev-environment.md](../../docs/knowledge-base/dev-environment.md), "The agent image").
<!-- /MODULE agent-seat -->

## CI parity

The CI preset ([ci/gitlab/](../../ci/gitlab/README.md)) builds **this folder** into the pipeline
image and runs `./verify all` in it, so local and remote execute the same commands on the same
toolchain. Two consequences for what belongs where:

- A tool the build needs goes into `features` or a Dockerfile, never into `postCreateCommand`:
  `devcontainer build` bakes the image and never runs the lifecycle commands.
- A change under `.devcontainer/**` rebuilds the pipeline image, so the toolchain moves in one
  reviewed commit for every developer and for CI at once.

For a project that commits a `devcontainer-lock.json`:

- the image build follows dev-environment.md's ADAPT note on digest pins.

<!-- MODULE acceptance -->
## Browser and grid (web-UI acceptance)

- The browser is part of the image, the fenced `firefox-esr` layer of the `Dockerfile`, never
  `postCreateCommand`: the CI preset builds this same folder (CI parity, above).
- `grid/grid up` (the java-gradle preset's grid recipe) joins this container to the grid network
  under the service-host name, `acceptance-service`, the alias the browser start shares with
  `SERVICE_HOST_ON_THE_GRID` ([build-run-deploy.md](../../docs/knowledge-base/build-run-deploy.md),
  "The browser grid").
<!-- /MODULE acceptance -->
