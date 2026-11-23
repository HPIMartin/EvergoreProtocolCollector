# Preset: C# / .NET

## Placeholder value

| Token | Value |
|---|---|
| `{{TECH_STACK}}` | `C#/.NET` |

## `verify`

Copy [verify](verify) to the repo root.

| Subcommand | Command |
|---|---|
| `format` | `dotnet format --verify-no-changes` |
| `focus <Class.cs>` | `dotnet test <Project.csproj> --filter FullyQualifiedName~<Class> -- RunConfiguration.TreatNoTestsAsError=true`; a substring match on the basename, so same-named classes in different namespaces of that project all run |
| `focus <x.feature>` | `dotnet test <Project.csproj> --filter FullyQualifiedName~<Stem>Feature -- RunConfiguration.TreatNoTestsAsError=true` |
| `bdd` | `dotnet test --filter "TestCategory=wip"` |
| `all` | format check, `dotnet build --no-incremental`, `dotnet test --no-build --filter "TestCategory!=wip&TestCategory!=characterization"` |
| `outdated` | `dotnet list package --outdated` |
| `vuln` | `dotnet list package --vulnerable --include-transitive` |
| `stop` | nothing, exit 0: the build servers `dotnet` starts are shared by every tree of the machine, so no tree stops them alone |

- `--no-incremental` disables the incremental build for the gateway run; `dotnet test` re-runs every
  selected test, it restores no results.
- Proof of execution for `all`: the `Passed! - Failed: 0, Passed: N` summary per test assembly, plus
  `artifacts/test-results/<assembly>.xml`, which is what CI publishes.
- A relative `focus` path resolves against the **caller's** directory, so the script behaves the same
  from a subdirectory as from the root.
- `bdd` on an empty `@wip` set passes: VSTest's `TreatNoTestsAsError` is false by default, so a
  filter that matches nothing warns and returns 0. `bdd` leaves it false, since it would fail
  between features otherwise; `focus` sets it, so a focused run that selects nothing fails.
  - VSTest applies it per test project, so `focus` runs only `<Project.csproj>`, the nearest
    `*.csproj` at or above the target's folder (a `.feature` lives in its acceptance project's
    folder); a second test project that holds no match cannot fail the run.
  - A target with no `*.csproj` in its folder or above it in the tree is refused with exit 2
    and a message naming it, before any tool runs.
  - `all` leaves it false as well, since a test project that holds only `@wip` scenarios would
    fail it otherwise. So `all` passes on a solution with no test project at all: the summary per
    test assembly is what shows that tests ran.
  - The nearest project must be a test project: it references `Microsoft.NET.Test.Sdk` or
    `MSTest.Sdk`, or sets `IsTestProject` to true. A target whose nearest project is none, such
    as a production class, is refused with exit 2, so focus a test file.
    - Why: `dotnet test` on a class library runs no test and exits 0 (an SDK 8 run).
    - Known limit: the check reads the project file itself, as text. A test project whose test
      packages come from a `Directory.Build.props`, or whose `IsTestProject` is spelled `True`,
      is refused: set `<IsTestProject>true</IsTestProject>` in its project file. A marker in a
      comment or under a false `Condition` still counts.
- Reqnroll names a feature's generated class `<Stem>Feature`, which is what lets `focus` select a
  feature file. That run carries no category filter, so `@wip` scenarios execute.
  - Known limit: `focus <x.feature>` selects the class `<Stem>Feature`; name the file after its
    `Feature:` title, or a focused run selects nothing and fails.

## settings.json allow block

Merge into `permissions.allow`:

```json
"Bash(dotnet build:*)",
"Bash(dotnet test:*)",
"Bash(dotnet format:*)",
"Bash(dotnet list:*)",
"Bash(dotnet restore:*)",
"Bash(dotnet run --project probe/Probe.csproj:*)",
"Bash(dotnet --version)"
```

Deliberately no blanket `Bash(dotnet:*)`: `dotnet run` and `dotnet tool` are arbitrary code
execution. The one scoped exception is the probe project.

## Build settings, lint, format, warnings-as-errors

- **SDK pinning:** `global.json` at the repo root:

  ```json
  {
    "sdk": {
      "version": "9.0.100",
      "rollForward": "latestFeature"
    }
  }
  ```

- **`Directory.Build.props`** at the repo root carries the settings for every project, so no csproj
  repeats them:

  ```xml
  <Project>
    <PropertyGroup>
      <Nullable>enable</Nullable>
      <TreatWarningsAsErrors>true</TreatWarningsAsErrors>
      <EnforceCodeStyleInBuild>true</EnforceCodeStyleInBuild>
      <AnalysisLevel>latest-all</AnalysisLevel>
      <BaseOutputPath>$(MSBuildThisFileDirectory)artifacts/$(MSBuildProjectName)/bin/</BaseOutputPath>
      <BaseIntermediateOutputPath>$(MSBuildThisFileDirectory)artifacts/$(MSBuildProjectName)/obj/</BaseIntermediateOutputPath>
    </PropertyGroup>
  </Project>
  ```

  The redirected output keeps `bin/` and `obj/` out of the source tree; `artifacts/` is gitignored.
  Both output properties have to sit in `Directory.Build.props`, which MSBuild imports before the
  NuGet targets read them.
- **Format and style:** `.editorconfig` is the single style source for IDEs, analyzers and the gate;
  `dotnet format --verify-no-changes` is the gate, `dotnet format` fixes.
- **Nullable reference types are on, and the null-forgiving operator `!` is banned in production
  source** (`hooks/content-gate` refuses it): suppressing the compiler's null analysis hides exactly
  the defect the analysis exists to find. Model the absence instead.
- **`Console.Write*` is a smell** outside a CLI adapter listed in `print_allow_paths`; production
  code logs through the injected logger.

## Test framework and Reqnroll wiring

- **NUnit** is the default unit framework: the Unity Test Framework is NUnit-based, so a project that
  may later grow a Unity view keeps one attribute and assertion dialect. xUnit is the alternative,
  chosen at adoption.
- **Reqnroll** (`Reqnroll.NUnit`) runs the acceptance scenarios; its version lives in the test
  project's csproj or in `Directory.Packages.props`.
- **Layout:** `tests/<System>.AcceptanceTests/Features/*.feature` and
  `tests/<System>.AcceptanceTests/Steps/*.cs`, `[Binding]` classes named by domain concept, never by
  feature file.
- **Tags become categories:** `@wip` generates `[Category("wip")]`, which is what
  `--filter "TestCategory!=wip&TestCategory!=characterization"` and `--filter "TestCategory=wip"`
  select on.
- **`@ignore` is optional and costs something.** Reqnroll's built-in `@ignore` makes an unfiltered
  run (an IDE "run all") skip instead of fail, but an ignored scenario stays ignored under
  `verify bdd` too, so it has to come off before the scenario can be driven green. Default to `@wip`
  alone and let the filter do the excluding.
- **Step-definition shape**, each actor driven through its real surface against a fresh real
  service (handbook §5): a page object over the browser for a screen, a client for an API. No
  comments (`hooks/content-gate` refuses them):

  ```csharp
  [Binding]
  public sealed class ContributionSteps(RunningService service, ScenarioBrowser browser)
  {
      private ContributionOverviewPage? overview;

      [Given(@"the order book holds an order from ""(.*)"" worth (\d+) placed on (.*)")]
      public Task TheOrderBookHoldsAnOrder(string customer, int worth, string placedAt) =>
          service.Orders.Place(new Order(customer, worth, ScenarioTime.Parse(placedAt)));

      [When(@"the manager opens the contribution overview")]
      public async Task TheManagerOpensTheContributionOverview() =>
          overview = await ContributionOverviewPage.OpenIn(browser, service);

      [Then(@"the manager sees (\d+) in (.*)'s contribution column")]
      public async Task TheManagerSeesInTheContributionColumn(int expected, string customer) =>
          Assert.That(await overview!.ContributionOf(customer), Is.EqualTo(expected));
  }
  ```

  The field holds nothing until the `When` step opens the page, so under
  `<Nullable>enable</Nullable>` and warnings as errors it is declared nullable, and the `Then` step
  dereferences it with `!`, which the content gate refuses in production source only.

  `ScenarioBrowser` is the project's own wrapper over its browser driver (the java-gradle preset
  ships one). `RunningService` is its own class: a `[BeforeScenario]` hook starts a fresh service
  and an `[AfterScenario]` hook stops it, and `Orders.Place` writes through the service's real
  write path.

  > **ADAPT:** a project that chose the in-memory model at adoption drives ports with in-memory
  > fakes instead: the `Given` records into an in-memory port and the `When` runs the query over it.

## Probe location (agent throwaway code, build-run-deploy.md)

- A console project **outside the solution**, `probe/Probe.csproj`, so no solution-wide build ever
  compiles it and a broken probe leaves `./verify all` green.
- The csproj is committed; the sources are not. `.gitignore`: `/probe/*` with `!/probe/Probe.csproj`.
- Run one: `dotnet run --project probe/Probe.csproj`.
- Clear them: a committed `scripts/clear-probes` (`find probe -name '*.cs' -delete`), so an agent
  clears its own probes without ever running `rm` (handbook §7).

## Git gate settings

- `production_trees='src/'` and `test_trees='tests/'` for a solution with that layout; a Unity
  project adds `Assets/` to `production_trees` (addendum below).
- `print_allow_paths` covers a console adapter whose stdout is the interface.

## Toolchain pinning (dev-environment.md, "Toolchain version")

- `global.json` pins the SDK feature band; `rollForward: latestFeature` lets a patch-level newer SDK
  serve without a repo change while a feature-band jump stays a decision.
- Central package versions in `Directory.Packages.props` (`ManagePackageVersionsCentrally`), so a
  version appears once.
- Variant A: the devcontainer dotnet feature states the same SDK version.
- Variant B: the `dotnet` entry in `.tool-versions` states it.

## Modernisation measurements

- `verify outdated`: `dotnet list package --outdated`.
- `verify vuln`: `dotnet list package --vulnerable --include-transitive`.
- Dependency bot: Dependabot `package-ecosystem: nuget`, or the Renovate `nuget` manager (snippets:
  [stacks/README.md](../README.md)).

## Unity adapter addendum

Unity is **not** a separate stack. A Unity project under this process is the .NET solution above plus
an engine-bound view layer, and the split is what keeps the process applicable.

- **The engine-free core stays a normal .NET solution**, built and tested headlessly in the
  devcontainer with the same `./verify all`. All domain and application logic lives there.
- **The Unity Editor runs on the host**, pinned by `ProjectSettings/ProjectVersion.txt`
  (single-sourced with whatever the build farm installs). There is no devcontainer variant of it.
- **`LangVersion` is bound to the Unity release**, set in `Directory.Build.props` (example: C# 9 for
  Unity 2022.3). The core must not use language features the engine's compiler rejects.
- **The core is consumed as a UPM package** (or a local package folder) with an `.asmdef` carrying
  `noEngineReferences: true`, which makes the dependency direction mechanical: the view may reference
  the core, never the reverse.
- **`artifacts/` must never be imported by Unity.** Keep the solution and its output outside
  `Assets/` and `Packages/`; an imported build output makes the editor compile the core twice and
  ship duplicate assemblies.
- **Unity Test Framework covers the view only** (EditMode and PlayMode), in batch mode:
  `-runTests -testPlatform EditMode -testResults <path>`. Core behavior is tested in the .NET
  solution, not through the editor.
- **Gherkin for the view**, if at all, through `UnitySpec` (SpecFlow-derived, on the Unity Test
  Framework). It is the only known option and it is niche; the system's scenarios normally live in
  the core's Reqnroll project.
- **`Assets/` counts as production source** for the content gate: the comment ban, the debug-print
  smell and the `!` ban apply to `MonoBehaviour` code exactly as to the core.
- **Agents in the devcontainer cannot run the editor.** Every editor-side step (importing the
  package, running PlayMode tests, building the player) is an **author step**; an agent prepares it
  and reports what the author has to run.
