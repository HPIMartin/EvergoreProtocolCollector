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
| `all` | `npm ci`, format, lint, typecheck, `npm test`, `npm run bdd -- --profile default`, both with a junit reporter into `artifacts/test-results/` |
| `outdated` | `npm outdated` |
| `vuln` | `npm audit` |
| `stop` | nothing, exit 0: a build leaves no process running |

- A relative `focus` path resolves against the **caller's** directory, so the script behaves the same
  from a subdirectory as from the root.
- `all` opens with `npm ci`, which replaces `node_modules` with exactly what `package-lock.json`
  names, so a package a commit dropped is gone before any check runs; the other subcommands use
  `node_modules` as it stands.
  - `format` never waits for the build lock, so a `./verify format` in the same tree while an
    `all` reinstalls, as from the pre-commit hook, can fail on a half-installed `node_modules`;
    run it again once `all` has ended.
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
  - Named limit: `.` is the whole tree, so the check also reads `.template/baseline/` and the
    template's own Markdown, files the project did not write.
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
- **Step-definition shape**, each actor driven through its real surface against a fresh real
  service (handbook §5): a page object over the browser for a screen, a client for an API. No
  comments (`hooks/content-gate` refuses them):

  ```ts
  Given(
    'the order book holds an order from {string} worth {int} placed on {}',
    async function (customer: string, worth: number, placedAt: string) {
      await this.service.orders.place({ customer, worth, placedAt: scenarioTime(placedAt) });
    },
  );

  When('the manager opens the contribution overview', async function () {
    this.overview = await ContributionOverviewPage.openIn(this.browser, this.service);
  });

  Then(
    "the manager sees {int} in {word}'s contribution column",
    async function (expected: number, customer: string) {
      expect(await this.overview.contributionOf(customer)).toBe(expected);
    },
  );
  ```

  `this.browser` is the project's own wrapper over its browser driver (the java-gradle preset ships
  one), and `this.service` is its own handle on a fresh service, started by a `Before` hook and
  stopped by an `After` hook; `orders.place` writes through the service's real write path.

  > **ADAPT:** a project that chose the in-memory model at adoption drives ports with in-memory
  > fakes instead: the `Given` records into an in-memory port and the `When` runs the query over it.

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

## Toolchain pinning (dev-environment.md, "Toolchain version")

- `package.json` `"engines"` plus the committed `package-lock.json`; install with `npm ci`, never
  bare `npm install`, so the lockfile stays authoritative.
- Variant A: the devcontainer node feature pins the same version (an exact one, not `"lts"`).
- Variant B: the `nodejs` entry in `.tool-versions` pins the same version.

## Modernisation measurements

- `verify outdated`: `npm outdated` lists every dependency behind its wanted or latest version.
- `verify vuln`: `npm audit`.
- Dependency bot: Dependabot `package-ecosystem: npm`, or the Renovate `npm` manager (snippets:
  [stacks/README.md](../README.md)).
