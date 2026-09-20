# Environment variant A: devcontainer (recommended)

Fully virtualized development: no toolchain is installed or run on the host. Everything (builds,
tests, the app, the AI agents) runs inside the container defined in
[.devcontainer/devcontainer.json](.devcontainer/devcontainer.json).

## Adoption

1. Copy the `.devcontainer/` folder (including `.claude-home/`) to the repo root.
2. Work through the ADAPT comments in `devcontainer.json`: image, pinned toolchain features for your
   stack (the `stacks/` presets name them), editor extensions, and the `./verify all` warm-up
   appended to `postCreateCommand`.
3. Keep the root `.gitignore` rules for `.devcontainer/.claude-home/` (shipped with the template).
4. Record the choice in `docs/knowledge-base/dev-environment.md` (its ADAPT note), then delete `env/`.

## What this variant guarantees

- The toolchain version is pinned in the devcontainer features and single-sourced with the build's
  toolchain declaration (dev-environment.md).
- `postCreateCommand` activates the git hooks (`git config core.hooksPath hooks`) in every fresh
  container.
- The per-project Claude home (the `.claude-home/` mount) survives container rebuilds: auth,
  auto-memory, `settings.local.json`.
- Agents execute inside the container, isolated from the host.

## CI parity

The CI preset ([ci/gitlab/](../../ci/gitlab/README.md)) builds **this folder** into the pipeline
image and runs `./verify all` in it, so local and remote execute the same commands on the same
toolchain. Two consequences for what belongs where:

- A tool the build needs goes into `features` or a Dockerfile, never into `postCreateCommand`:
  `devcontainer build` bakes the image and never runs the lifecycle commands.
- A change under `.devcontainer/**` rebuilds the pipeline image, so the toolchain moves in one
  reviewed commit for every developer and for CI at once.
