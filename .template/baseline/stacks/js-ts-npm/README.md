# Preset: JavaScript-TypeScript / npm

## Placeholder value

| Token | Value |
|---|---|
| `{{TECH_STACK}}` | `TypeScript/Node` (or `JavaScript/Node`) |

## `verify`

Copy [verify](verify) to the repo root and add the scripts it dispatches to. The tool flags live in
`package.json`, `verify` only sequences them:

```json
"scripts": {
  "format:check": "prettier --check .",
  "format:fix": "prettier --write .",
  "lint": "eslint . --max-warnings 0",
  "typecheck": "tsc --noEmit --incremental false",
  "test": "vitest run",
  "bdd": "cucumber-js",
  "probe": "vitest run src/probe",
  "clear-probes": "node -e \"require('node:fs').rmSync('src/probe',{recursive:true,force:true})\""
}
```

| Subcommand | Command |
|---|---|
| `format` | `npm run format:check` |
| `focus <file>` | `npm test -- <file>` |
| `focus <x.feature>` | `npm run bdd -- --profile focus <path>` |
| `bdd` | `npm run bdd -- --profile wip` |
| `all` | format, lint, typecheck, `npm test`, `npm run bdd -- --profile default`, both with a junit reporter into `artifacts/test-results/` |
| `outdated` | `npm outdated` |
| `vuln` | `npm audit` |

- A relative `focus` path resolves against the **caller's** directory, so the script behaves the same
  from a subdirectory as from the root.
- Proof of execution for `all`: Vitest's verbose reporter prints one line per test plus the
  `Tests N passed` summary, and the Cucumber summary formatter prints the scenario counts. The junit
  files (`artifacts/test-results/vitest.xml`, `artifacts/test-results/cucumber.xml`) are what CI
  publishes.
- There is no result cache to disable: Vitest re-runs every selected test, ESLint's and Prettier's
  caches are opt-in and stay off, and `--incremental false` keeps `tsc` from trusting a stale
  `.tsbuildinfo`.

## settings.json allow block

Merge into `permissions.allow`:

```json
"Bash(npm run:*)",
"Bash(npm test:*)",
"Bash(npm ci)",
"Bash(npm outdated)",
"Bash(npm audit:*)",
"Bash(npx vitest:*)",
"Bash(npx tsc:*)",
"Bash(npx eslint:*)",
"Bash(npx prettier:*)",
"Bash(npx cucumber-js:*)",
"Bash(node --version)"
```

Deliberately no blanket `Bash(node:*)` or `Bash(npx:*)`: arbitrary script execution stays behind the
approval prompt; widen locally if your loop needs it.

## Lint, format, warnings-as-errors

- **Typecheck:** `tsc --noEmit` with `"strict": true`; a type error fails `verify all` (handbook §3).
- **Lint:** ESLint with `--max-warnings 0`, so every warning fails the gate.
- **Format:** Prettier; `prettier --check .` in the gate, `prettier --write .` to fix.
- Lint rules stay disjoint from the formatter's, so no rule lives in two tools.

## Cucumber-js wiring

- **Dependency:** `@cucumber/cucumber` as a dev dependency; its version lives in `package.json`.
- **Feature files:** `features/` at the package root; step definitions in `features/steps/`.
- **Profiles**, `cucumber.js` at the package root:

  ```js
  const common = {
    paths: ['features/**/*.feature'],
    import: ['features/steps/**/*.ts'],
    loader: ['tsx/esm'],
    format: ['summary'],
  };

  module.exports = {
    default: { ...common, tags: 'not @wip and not @characterization' },
    wip: { ...common, tags: '@wip' },
    focus: { ...common },
  };
  ```

  The `focus` profile carries no tag expression, so a focused feature run includes `@wip`.
- **TypeScript step definitions** need a loader, and the key follows the installed
  `@cucumber/cucumber` major: an ESM project uses `import` plus the documented loader entry (or
  `NODE_OPTIONS='--import tsx'` around the script), a CommonJS project uses `require` plus
  `requireModule: ['ts-node/register']`. The loader-free alternative compiles the steps with `tsc`
  and points `import` at the emitted JavaScript.
- **Step-definition shape**, driven through ports with in-memory fakes (handbook §5), no comments
  (`hooks/content-gate` refuses them):

  ```ts
  Given('customer {string} placed orders worth {int} this month', function (customer: string, total: number) {
    this.orders.record({ customer, total });
  });

  When('the manager opens the monthly overview', function () {
    this.overview = monthlyOverview(this.orders);
  });

  Then('the contribution column for {string} shows {int}', function (customer: string, expected: number) {
    expect(this.overview.contributionOf(customer)).toBe(expected);
  });
  ```

- **Alternative:** `@amiceli/vitest-cucumber` executes the same `.feature` files inside Vitest, which
  leaves the project with a single runner at the price of Cucumber's reporting and profile handling
  (the `@wip` filter becomes a test-name filter). Choose it at adoption, not later.

## Probe location (agent throwaway code, build-run-deploy.md)

A gitignored `src/probe/` directory, outside the test glob so the gateway run never picks it up, with
two npm scripts: `npm run probe` (the test runner pointed at `src/probe/`) and `npm run clear-probes`
(a committed script that removes the directory), so an agent clears its own probes without running
`rm` (handbook §7). The template `.gitignore` already carries `/src/probe/`.

## Git gate settings

- `production_trees='src/'` and `test_trees='tests/'` (plus `features/` if step definitions live
  there) when the layout differs from the shipped defaults.
- `print_allow_paths` covers a CLI entry point whose stdout is the interface; `console.log` stays a
  smell everywhere else.

## Toolchain pinning (single-sourcing, dev-environment.md)

- `package.json` `"engines"` plus the committed `package-lock.json`; install with `npm ci`, never
  bare `npm install`, so the lockfile stays authoritative.
- Variant A: the devcontainer node feature pins the same version (an exact one, not `"lts"`).
- Variant B: the `nodejs` entry in `.tool-versions` pins the same version.

## Modernisation measurements

- `verify outdated`: `npm outdated` lists every dependency behind its wanted or latest version.
- `verify vuln`: `npm audit`.
- Dependency bot: Dependabot `package-ecosystem: npm`, or the Renovate `npm` manager (snippets:
  [stacks/README.md](../README.md)).
