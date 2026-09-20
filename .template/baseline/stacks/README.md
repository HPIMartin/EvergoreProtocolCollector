# Stack presets

The template core is stack-neutral; a preset supplies the stack-specific values in one place. Pick
the preset for your stack at adoption (the `/adopt` skill applies it); mixed projects (e.g. a Java
service with a TypeScript frontend) combine two presets.

## What a preset supplies

- **Placeholder values** for `{{TECH_STACK}}`, `{{BUILD_CMD}}`, `{{TEST_CMD}}` (placeholder key:
  [docs/knowledge-base/README.md](../docs/knowledge-base/README.md)).
- **A permissions allow block** to merge into `permissions.allow` of `.claude/settings.json` (the
  deny floor is stack-neutral and stays untouched). When the resolved `{{BUILD_CMD}}`/`{{TEST_CMD}}`
  are compound or parameterized, the block replaces the generic `Bash({{BUILD_CMD}}:*)` /
  `Bash({{TEST_CMD}}:*)` rules: a `Bash(...)` rule only matches as a clean command prefix.
- **Lint/format & warnings-as-errors wiring**: which tools gate the build and how warnings fail it
  (engineering-handbook §3), plus the fast format command for the `hooks/pre-commit` format gate
  (its ADAPT spot).
- **Toolchain pinning spots** for dev-environment.md's single-sourcing rule: the build-file
  declaration plus the devcontainer feature (variant A) or the version-manifest entry (variant B).

## Presets

- [java-gradle/](java-gradle/README.md) (full)
- [js-ts-npm/](js-ts-npm/README.md) (full)
- [python/](python/README.md) (stub)
- [csharp-unity/](csharp-unity/README.md) (stub)
- [cpp/](cpp/README.md) (stub)

The stubs carry the placeholder values and pointers; extend one to the shape of the two full presets
when you first use it (and consider contributing the result back to the template).

After adoption the preset's values live in the project (settings.json, hooks, build files, KB docs);
delete `stacks/` along with the rest of the template scaffolding.
