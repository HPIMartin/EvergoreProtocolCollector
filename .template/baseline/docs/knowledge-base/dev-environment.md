# 11: Dev Environment & Virtualization

**Principle: the dev environment is pinned and single-sourced. Nobody (human or agent) builds,
tests, or runs the app through an ad-hoc host toolchain.** Two variants implement the principle; a
project picks exactly one at adoption (template folder `env/`) and records the choice here.

> **ADAPT:** state the chosen variant here and delete the other variant's section (or keep it with a
> note if a later switch is plausible).

## Variant A: devcontainer (recommended)

Fully virtualized: no toolchain is installed or run on the host. All work (builds, tests, the app,
and the AI agents) runs inside the **devcontainer** or via containers. This keeps the host clean and
makes toolchain upgrades a one-line image change instead of a host installation. The CI preset
builds its job image from this same folder, so the pipeline and the developer run the identical
toolchain ([build-run-deploy.md](build-run-deploy.md), "CI / dev environment").

### The devcontainer (`.devcontainer/devcontainer.json`; shipped in the template under `env/devcontainer/`)

> **ADAPT:** describe your container: base image, toolchain features and their pinned versions,
> editor extensions, and the `postCreateCommand` (warm the build cache and activate the git hooks,
> see [build-run-deploy.md](build-run-deploy.md)). Keep the toolchain version **single-sourced**:
> the feature version here should be the same value the build's toolchain declaration points at.
> Record any deferred features with the backlog item that tracks re-adding them.

### How to work in it

1. Open the repo in an editor with devcontainer support (e.g. VS Code with the Dev Containers
   extension).
2. **Reopen in Container**; the first build provisions the toolchain.
3. Run the AI agent (e.g. Claude Code) **from the container's integrated terminal**, so all agents
   execute inside the container, never on the host. Builds and tests run there too.

> Note: a session started on the *host* runs on the host, with whatever quirks the host shell has.
> "Work in the container" means starting the session from the in-container terminal. A host session
> also lacks the container's toolchain, so a commit whose gate needs it (`./verify format`, a stack
> script's self-test) fails there: commit from inside the container.

> Note: an agent cannot validate a `.devcontainer/` change. The image is built by the host's
> tooling on the author's next rebuild, and only that rebuild proves the change.

> Note: the Claude Code VS Code extension can race the editor's own file-watcher. When it edits a file
> the IDE is watching (a worktree file, especially with a second Claude session active), a read taken
> right after the edit may transiently show the old content and a follow-up edit may fail to find its
> target, even though the write landed and commits fine once things settle. It is a presentation race,
> not an external process rewriting the file (an idle file is stable; no `git restore` runs). Trust
> `git diff` / `git show` over a `grep` taken mid-edit; if writes look reverted, apply the change and
> commit in one shell process (write, `git add`, `git commit`) so the commit captures the content, then
> verify `HEAD`.

## Variant B: native pinned toolchain

No container: tools run on the developer's machine, but **only through pinned versions** from a
committed version manifest (e.g. mise/asdf `.tool-versions`; shipped in the template under
`env/native/`), never through whatever happens to be on `PATH`.

What replaces the container's guarantees:

- **Pinning:** the manifest and the build file's toolchain declaration state the same version
  (single-sourcing below); an upgrade changes them together.
- **Hooks activation:** no `postCreate` exists; run `git config core.hooksPath hooks` once per
  checkout, or wire it into your bootstrap script.
- **Isolation:** there is none; agents execute directly on the host, so the committed permission
  policy (`.claude/settings.json` deny floor) matters even more. Prefer variant A when isolation is
  a concern.

## Rule for all contributors (both variants)

**Never build, test, or run the app through an unpinned host toolchain.** Variant A: everything runs
inside the devcontainer or via containers. Variant B: everything runs through the manifest's pinned
toolchain. The rule applies to all contributors and agents; it is restated in each tool's entry file
(e.g. `CLAUDE.md`) and in each agent definition under [`.claude/agents/`](../../.claude/agents/).

## Toolchain version single-sourcing (upgrade procedure)

Pin the toolchain version in as few places as possible, and list **every** place it appears so an
upgrade touches them together in one change: typically the build file's toolchain declaration, plus
the devcontainer feature version (variant A) or the version manifest (variant B), and any production
image base. The standing goal: a version bump is a single, documented switch, rebuilt and verified
with `./verify all`, with nothing installed ad hoc.

> **ADAPT:** list the concrete pinned locations for your stack and the exact upgrade steps.

## Production image

> **ADAPT:** how the deployable container image is built and where (in the devcontainer, on a
> separate container host if the devcontainer has no nested container support, or on the native
> machine's container runtime for variant B). Secrets are injected at runtime, never baked into the
> image (handbook §3). Delete this section if the project does not ship a container. Runtime
> details: [build-run-deploy.md](build-run-deploy.md).
