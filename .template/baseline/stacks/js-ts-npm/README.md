# Preset: JavaScript-TypeScript / npm

## Placeholder values

| Token | Value |
|---|---|
| `{{TECH_STACK}}` | `TypeScript/Node` (or `JavaScript/Node`) |
| `{{BUILD_CMD}}` | `npm run verify` (one script for the full gate, see below) |
| `{{TEST_CMD}}` | `npx vitest run <file>` (or `npm test -- <file>` for your runner) |

Define one script so the full gate is a single command:

```json
"scripts": {
  "verify": "npm run build && npm run typecheck && npm run lint && npm test"
}
```

## settings.json allow block

Merge into `permissions.allow`:

```json
"Bash(npm run:*)",
"Bash(npm test:*)",
"Bash(npm ci)",
"Bash(npx vitest:*)",
"Bash(npx tsc:*)",
"Bash(npx eslint:*)",
"Bash(npx prettier:*)",
"Bash(node --version)"
```

Deliberately no blanket `Bash(node:*)` or `Bash(npx:*)`: arbitrary script execution stays behind the
approval prompt; widen locally if your loop needs it.

## Lint, format, warnings-as-errors

- **Typecheck:** `tsc --noEmit` with `"strict": true`; type errors fail `verify` (handbook §3).
- **Lint:** ESLint with `--max-warnings 0`, so every warning fails the gate.
- **Format:** Prettier; `prettier --check .` inside `verify`, `prettier --write` to fix.
- **pre-commit format gate:** `npx prettier --check .` (or your lint command if it runs in seconds;
  the hook's ADAPT marker). Keep test runs out of the hook.
- **Production-source tree** for the hook's hard-coded-token scan: `src/` (adjust the shipped
  `src/main/*` example in `hooks/pre-commit`).

## Toolchain pinning (single-sourcing, dev-environment.md)

- `package.json` `"engines"` plus the committed `package-lock.json`; install with `npm ci`, never
  bare `npm install`, so the lockfile stays authoritative.
- Variant A: the devcontainer node feature pins the same version (an exact one, not `"lts"`).
- Variant B: the `nodejs` entry in `.tool-versions` pins the same version.
