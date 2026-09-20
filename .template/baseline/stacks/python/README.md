# Preset: Python (stub)

Placeholder values to start from; extend this stub to the shape of the full presets
([java-gradle](../java-gradle/README.md), [js-ts-npm](../js-ts-npm/README.md)) when you first use it.

| Token | Value |
|---|---|
| `{{TECH_STACK}}` | `Python` |
| `{{BUILD_CMD}}` | e.g. `uv run pytest` (or `tox`/`hatch run test`): one command for the full gate |
| `{{TEST_CMD}}` | e.g. `uv run pytest <file>::<test>` |

Pointers for the full preset: allow block for `uv`/`pytest`/`ruff`; lint and format via ruff (`ruff
check`, `ruff format --check` as the pre-commit format gate); warnings-as-errors via `pytest -W
error` plus strict typing (mypy or pyright); pinning via `.python-version`/`.tool-versions` and the
lockfile.
