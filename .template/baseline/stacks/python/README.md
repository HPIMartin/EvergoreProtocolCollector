# Preset: Python

## Placeholder value

| Token | Value |
|---|---|
| `{{TECH_STACK}}` | `Python/uv` |

## `verify`

Copy [verify](verify) to the repo root. Every command goes through **uv**: `uv run` materializes the
locked environment before it runs the tool, so no step depends on an activated venv.

| Subcommand | Command |
|---|---|
| `format` | `uv run ruff format --check .` then `uv run ruff check .` |
| `focus <file>` | `uv run pytest -v <file>` |
| `focus <x.feature>` | `uv run pytest -v tests/steps/test_<stem>.py` |
| `bdd` | `uv run pytest -v -m wip` |
| `all` | ruff format check, ruff check, `uv run pyright`, `uv run pytest -v -m "not wip and not characterization" --junitxml=artifacts/test-results/pytest.xml` |
| `outdated` | `uv pip list --outdated` and `uv lock --upgrade --dry-run` |
| `vuln` | `uv run pip-audit` |

- Proof of execution for `all`: `-v` prints one line per test id and the `N passed` summary;
  `artifacts/test-results/pytest.xml` is what CI publishes.
- `-p no:cacheprovider` turns pytest's cache off, so no run is shaped by a previous one.
- A focused feature run carries no marker filter, so `@wip` scenarios run.
- A relative `focus` path resolves against the **caller's** directory, so the script behaves the same
  from a subdirectory as from the root.
- `bdd` maps pytest's exit code 5 (nothing collected) to a clean 0 with a printed note: no scenario
  being tagged `@wip` is the ordinary state between features. Exit code 5 anywhere else stays a
  failure, so a `focus` run that matches nothing is still red.

## settings.json allow block

Merge into `permissions.allow`:

```json
"Bash(uv sync:*)",
"Bash(uv lock:*)",
"Bash(uv pip list:*)",
"Bash(uv run pytest:*)",
"Bash(uv run ruff:*)",
"Bash(uv run pyright:*)",
"Bash(uv run pip-audit:*)",
"Bash(uv run python probe/:*)",
"Bash(sh scripts/clear-probes)",
"Bash(uv --version)"
```

Deliberately **no** `Bash(python:*)` and no blanket `Bash(uv run:*)`: either one is arbitrary code
execution behind a friendly name. The one scoped exception is `uv run python probe/`, which can only
reach the gitignored probe directory.

## Lint, format, warnings-as-errors

- **Format:** `ruff format`; the gate runs `ruff format --check .`, `ruff format .` fixes.
- **Lint:** `ruff check`; the rule selection lives in `[tool.ruff.lint]` in `pyproject.toml`, the
  single style source for IDEs and the gate alike.
- **Types:** `pyright` in strict mode (`[tool.pyright] typeCheckingMode = "strict"`). Alternative:
  `mypy --strict`, chosen at adoption, not both.
- **Warnings are errors:** `filterwarnings = ["error"]` under `[tool.pytest.ini_options]`, so every
  run including a focused one fails on a warning (the flag form is `pytest -W error`).

## pytest-bdd wiring

- **Dependencies:** `pytest`, `pytest-bdd` in the dev dependency group; versions live in
  `pyproject.toml` and `uv.lock`.
- **Layout:** feature files in `tests/features/`, step modules in `tests/steps/`. A feature
  `tests/features/<stem>.feature` pairs with `tests/steps/test_<stem>.py`, which is what lets
  `verify focus <x.feature>` find the run.
- **`pyproject.toml`:**

  ```toml
  [tool.pytest.ini_options]
  testpaths = ["tests"]
  bdd_features_base_dir = "tests/features"
  filterwarnings = ["error"]
  markers = [
    "wip: approved scenario awaiting implementation",
    "characterization: scenario derived from legacy code, awaiting the author's confirmation",
  ]
  ```

  pytest-bdd turns every Gherkin tag into a pytest marker, so `@wip` becomes `-m wip`. Registering
  both markers is mandatory here: an unregistered mark raises a warning, and warnings are errors.
- **Step-definition shape**, driven through ports with in-memory fakes (handbook §5), no comments
  and no docstrings (`hooks/content-gate` refuses both):

  ```python
  from pytest_bdd import given, parsers, scenarios, then, when

  scenarios("monthly_contribution.feature")


  @given(parsers.parse('customer "{customer}" placed orders worth {total:d} this month'), target_fixture="orders")
  def orders_of(customer: str, total: int) -> InMemoryOrders:
      return InMemoryOrders([Order(customer, total)])


  @when("the manager opens the monthly overview", target_fixture="overview")
  def opened_overview(orders: InMemoryOrders) -> MonthlyOverview:
      return monthly_overview(orders)


  @then(parsers.parse('the contribution column for "{customer}" shows {expected:d}'))
  def contribution_shows(overview: MonthlyOverview, customer: str, expected: int) -> None:
      assert overview.contribution_of(customer) == expected
  ```

## Probe location (agent throwaway code, build-run-deploy.md)

- A gitignored `probe/` directory at the repo root, outside `testpaths`, so the gateway run never
  picks it up. Add `/probe/` to `.gitignore`.
- Run one: `uv run python probe/<file>.py`.
- Clear them: a committed `scripts/clear-probes` (`find probe -mindepth 1 -delete`), so an agent
  clears its own probes without ever running `rm` (handbook §7).

## Git gate settings

- `production_trees='src/'` and `test_trees='tests/'` match the shipped defaults for a src-layout
  package; a flat layout names its package directory instead.
- `print_allow_paths` covers a CLI entry point whose stdout is the interface; `print` stays a smell
  everywhere else.

## Toolchain pinning (single-sourcing, dev-environment.md)

- `.python-version` pins the interpreter uv provisions; `requires-python` in `pyproject.toml` states
  the same floor; `uv.lock` is committed and authoritative for every dependency.
- Variant A: the devcontainer python feature and the uv feature pin the same versions.
- Variant B: the `python` and `uv` entries in `.tool-versions` pin them.

## Modernisation measurements

- `verify outdated`: `uv pip list --outdated` for the resolved environment, `uv lock --upgrade
  --dry-run` for what a fresh resolution would move.
- `verify vuln`: `pip-audit` against the environment, or `osv-scanner --lockfile uv.lock` against
  the lockfile.
- Dependency bot: Dependabot `package-ecosystem: uv` (`pip` for a project without uv), or the
  Renovate `pep621` manager (snippets: [stacks/README.md](../README.md)).
