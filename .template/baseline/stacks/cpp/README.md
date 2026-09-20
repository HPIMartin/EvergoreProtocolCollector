# Preset: C++ (stub)

Placeholder values to start from; extend this stub to the shape of the full presets
([java-gradle](../java-gradle/README.md), [js-ts-npm](../js-ts-npm/README.md)) when you first use it.

| Token | Value |
|---|---|
| `{{TECH_STACK}}` | `C++/CMake` |
| `{{BUILD_CMD}}` | e.g. `cmake --build build && ctest --test-dir build` |
| `{{TEST_CMD}}` | e.g. `ctest --test-dir build -R <TestName>` |

Pointers for the full preset: warnings-as-errors via target-scoped `-Wall -Wextra -Werror`;
clang-format for the pre-commit format gate (`clang-format --dry-run --Werror`); clang-tidy as the
lint gate; pinning via the CMake toolchain file and the compiler version in the devcontainer image
or `.tool-versions`; an allow block for `cmake`/`ctest`.
