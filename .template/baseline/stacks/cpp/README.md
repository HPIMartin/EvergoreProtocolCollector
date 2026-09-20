# Preset: C++ / CMake

## Placeholder value

| Token | Value |
|---|---|
| `{{TECH_STACK}}` | `C++/CMake` |

## `verify`

Copy [verify](verify) to the repo root.

| Subcommand | Command |
|---|---|
| `format` | `clang-format --dry-run --Werror` over `git ls-files '*.cpp' '*.cc' '*.hpp' '*.h'` |
| `focus <file>` | configure, build, `ctest -R "^<stem>\."`; the stem is the file's basename, so same-named test files in different directories all run |
| `focus <x.feature>` | configure, build, `uv run --project tests/acceptance pytest -v tests/acceptance/steps/test_<stem>.py` |
| `bdd` | configure, build, `uv run --project tests/acceptance pytest -v -m wip` |
| `all` | format, configure, `cmake --build build --clean-first`, `run-clang-tidy`, `ctest --output-junit`, acceptance run with `-m "not wip and not characterization"` |
| `outdated` | `vcpkg x-update-baseline --dry-run` (or the Conan equivalent) |
| `vuln` | `osv-scanner -r .` |

- `--clean-first` is what disables the incremental build for the gateway run; CTest restores nothing
  and re-runs every selected test.
- `--no-tests=error` is the guard against the vacuous green: a run that discovers no test fails.
- Proof of execution for `all`: CTest's `N tests passed` line plus pytest's `-v` output.
  `artifacts/test-results/ctest.xml` and `artifacts/test-results/acceptance.xml` are what CI
  publishes.
- A relative `focus` path resolves against the **caller's** directory, so the script behaves the same
  from a subdirectory as from the root.
- `bdd` maps pytest's exit code 5 (nothing collected) to a clean 0 with a printed note: no scenario
  being tagged `@wip` is the ordinary state between features.
- Only tracked files reach `clang-format`, so the gitignored probe directory is excluded by
  construction.

## settings.json allow block

Merge into `permissions.allow`:

```json
"Bash(cmake:*)",
"Bash(ctest:*)",
"Bash(ninja:*)",
"Bash(clang-format:*)",
"Bash(clang-tidy:*)",
"Bash(run-clang-tidy:*)",
"Bash(uv sync:*)",
"Bash(uv run --project tests/acceptance pytest:*)",
"Bash(uv --version)"
```

No allow for the built binary: an agent reaches it through the acceptance scenarios, not by hand.

## Build, lint, format, warnings-as-errors

- **Build system:** CMake with the Ninja generator, build tree in `build/` (gitignored),
  `CMAKE_EXPORT_COMPILE_COMMANDS=ON` so clang-tidy and the editors read one compilation database.
- **Warnings are errors** through a target-scoped interface library, never a global flag, so a
  third-party target never inherits the project's strictness:

  ```cmake
  add_library(project_warnings INTERFACE)
  if(MSVC)
    target_compile_options(project_warnings INTERFACE /W4 /WX)
  else()
    target_compile_options(project_warnings INTERFACE -Wall -Wextra -Wpedantic -Werror)
  endif()
  ```

  Every project target links it: `target_link_libraries(app PRIVATE project_warnings)`.
- **Format:** `clang-format` with a committed `.clang-format`, the single style source for the gate
  and the editors.
- **Lint:** `clang-tidy` with a committed `.clang-tidy`; either through `run-clang-tidy -p build` in
  `verify all` or inline via `CMAKE_CXX_CLANG_TIDY`, one of the two, not both.
- **Unit tests:** GoogleTest, registered per test target so a focused run selects by file stem:

  ```cmake
  include(GoogleTest)
  add_executable(monthly_contribution_test tests/monthly_contribution_test.cpp)
  target_link_libraries(monthly_contribution_test PRIVATE domain project_warnings GTest::gtest_main)
  gtest_discover_tests(monthly_contribution_test TEST_PREFIX "monthly_contribution_test.")
  ```

  Catch2 and doctest are the interview alternatives; one framework per project.
- Bazel is out of scope for this preset.

## Acceptance scenarios (pytest-bdd, out of process)

C++ has no in-process Gherkin runner the template endorses. The scenarios drive the **built
artifact** from outside, through a test-only Python toolchain:

- `tests/acceptance/` is its own uv project (`pyproject.toml`, `uv.lock`, `pytest`, `pytest-bdd`),
  with `features/` and `steps/`. A feature `features/<stem>.feature` pairs with
  `steps/test_<stem>.py`.
- **The boundary is the artifact's interface**: steps start the binary, write to its stdin, read its
  stdout, or call its IPC/API surface. They never link, include or reach into the internals, so an
  internal refactoring cannot break a scenario.
- `verify` exports `APP_BINARY`; one fixture in `conftest.py` reads it, and it is the only knowledge
  the steps have about the build.
- Tags become pytest markers, so `@wip` is `-m wip`; register `wip` and `characterization` in the
  acceptance project's `pyproject.toml` (`markers = [...]`) because warnings are errors there too.
- `cucumber-cpp` is the in-process alternative. It needs its own Boost/CMake wiring and has to be
  verified at adoption before it is chosen.

## Probe location (agent throwaway code, build-run-deploy.md)

- A gitignored `probe/` directory at the repo root, wired as an `EXCLUDE_FROM_ALL` target so a
  broken probe cannot turn `./verify all` red. Add `/probe/` to `.gitignore`.

  ```cmake
  file(GLOB probe_sources CONFIGURE_DEPENDS probe/*.cpp)
  if(probe_sources)
    add_executable(probe EXCLUDE_FROM_ALL ${probe_sources})
    target_link_libraries(probe PRIVATE domain GTest::gtest_main)
  endif()
  add_custom_target(clear-probes COMMAND ${CMAKE_COMMAND} -E rm -rf ${CMAKE_SOURCE_DIR}/probe)
  ```

- Run one: `cmake --build build --target probe && ./build/probe`.
- Clear them: `cmake --build build --target clear-probes`, CMake's own file command in committed
  build code, so an agent never runs the shell's `rm` (handbook §7).

## Git gate settings

- `production_trees='src/ include/'` and `test_trees='tests/'` match the shipped defaults.
- `print_allow_paths` covers a CLI adapter whose stdout is the interface, which for this preset is
  also the surface the acceptance steps drive; `std::cout` stays a smell everywhere else.

## Toolchain pinning (single-sourcing, dev-environment.md)

- `cmake_minimum_required(VERSION ...)` plus `CMAKE_CXX_STANDARD` state the language and tool floor;
  the dependency versions are pinned by the vcpkg `builtin-baseline` in `vcpkg.json` or by the
  `conan.lock`.
- Variant A: the devcontainer feature pins the compiler, CMake, Ninja and uv versions.
- Variant B: the same versions live in `.tool-versions`.
- A cross-compiling project adds a CMake toolchain file and pins the target triplet there.

## Modernisation measurements

- `verify outdated`: vcpkg reports a stale manifest baseline with `vcpkg x-update-baseline
  --dry-run`; a Conan project compares its `conanfile` requirements against
  `conan list "<name>/*" -r=conancenter`.
- `verify vuln`: `osv-scanner` over the dependency manifests (`--lockfile conan.lock` for Conan).
- Dependency bot: none covers vcpkg or Conan; the Renovate `cmake` manager only tracks
  `FetchContent` pins, so a baseline bump stays a manual step driven by `verify outdated`
  (snippets: [stacks/README.md](../README.md)).
