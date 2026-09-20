# Environment variant B: native pinned toolchain

No container: tools run on the developer's machine, but **only through pinned, single-sourced
versions** from a committed version manifest. Ad-hoc installs ("whatever java/node happens to be on
`PATH`") stay banned; the pin is the contract.

## Adoption

1. Copy [.tool-versions](.tool-versions) to the repo root and pin your stack's exact tool versions
   (the example uses mise/asdf syntax; any manager works as long as the manifest is committed and
   the versions are exact).
2. Align the build file's toolchain declaration with the manifest (single-sourcing,
   dev-environment.md): both state the same version, and an upgrade changes them together.
3. Activate the git hooks once per checkout: `git config core.hooksPath hooks`. No `postCreate`
   exists to do it for you; add it to your bootstrap script if you keep one.
4. Adjust the two `cd` allow rules in `.claude/settings.json` to your absolute repo path
   (`/workspaces/{{PROJECT_NAME}}` is the devcontainer mount; a native checkout lives elsewhere).
5. Record the choice in `docs/knowledge-base/dev-environment.md` (its ADAPT note), then delete `env/`.

## What this variant guarantees, and what it does not

- Every contributor and agent builds with the same pinned toolchain (the committed manifest).
- The same hooks, warnings-as-errors, and permission policy apply; only the isolation layer differs
  from variant A.
- There is **no host isolation**: agents execute shell commands directly on the machine, so the
  `.claude/settings.json` deny floor matters even more here. If isolation is a concern, prefer
  variant A.
