# Preset: C# / Unity (stub)

Placeholder values to start from; extend this stub to the shape of the full presets
([java-gradle](../java-gradle/README.md), [js-ts-npm](../js-ts-npm/README.md)) when you first use it.

| Token | Value |
|---|---|
| `{{TECH_STACK}}` | `C#/Unity` |
| `{{BUILD_CMD}}` | e.g. one script wrapping a Unity batch-mode build plus the EditMode/PlayMode test run |
| `{{TEST_CMD}}` | e.g. Unity Test Framework in batch mode filtered to one class (`-testFilter`) |

Pointers for the full preset: warnings-as-errors via `TreatWarningsAsErrors` (csproj / Unity player
settings); Roslyn analyzers plus `dotnet format` for the pre-commit format gate; pinning via the
Unity version in `ProjectSettings/ProjectVersion.txt`, single-sourced with the CI/devcontainer
image; an allow block for your build script and the needed `dotnet` subcommands.
