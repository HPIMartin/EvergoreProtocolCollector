# Start here

You have the **AI-assisted development process template**: a project-agnostic process layer for
clean, test-driven, AI-assisted development. It brings a tool-neutral knowledge base, a multi-agent
review pipeline, a committed permission policy, and git hooks that enforce the rules mechanically.

It carries **no product code**. Nothing here is Java, TypeScript or Python specific until you pick a
stack preset during adoption.

## 1. Extract it into your project folder

```sh
unzip ai-dev-process-template-v*.zip -d <folder>
```

Files land at the root, no wrapping folder. Where they go depends on where you start:

- **A brand-new project:** extract into the empty folder and `git init`. Create no build yet:
  `/adopt` keeps the pristine payload first and asks for each stack's build skeleton when it
  applies the preset.
- **An existing repo without a Claude setup:** extract into an empty directory and copy the pieces
  in by hand (path B in `TEMPLATE-README.md`), so `.gitignore` and `.gitattributes` are merged.
- **A repo with its own `CLAUDE.md`, `.claude/` or knowledge base:** path B plus a hand merge of
  the files both hold, such as `CLAUDE.md`, `.claude/settings.json` and `docs/knowledge-base/`.
- **A repo that holds `TEMPLATE-VERSION`:** it is adopted already. Never extract into its root:
  extract into `.template/incoming/` and run `/template-upgrade`. Where the project's copy of that
  skill does not open with "The incoming copy governs" (every project from before 5), start the
  session with "carry out `.template/incoming/.claude/skills/template-upgrade/SKILL.md`" instead.

`README.md` is safe: the template's own reference ships beside this file as `TEMPLATE-README.md`.

## 2. Run `/adopt`

Open the folder in Claude Code and run:

```
/adopt
```

Before you start: the workflow runs without prompts only where Claude Code's auto mode is the
session default, a user setting no project can set (`TEMPLATE-README.md`, "Settings split").

Two things matter for this one session:

- **Pick the strongest model available.** Adoption is judgment work, not typing.
- **Start it in a fresh session**, with nothing else in the context.

What `/adopt` does, in short (its steps: `.claude/skills/adopt/SKILL.md`):

- It checks its tools and your starting situation, and keeps the pristine payload as the merge
  base for later upgrades.
- It interviews you in one round. It asks rather than guesses, so answer as you go.
- It turns the payload into your process layer: the environment variant, the modules you keep,
  the stack presets, your project's values and the seeded living docs.
- With the devcontainer variant it stops after the environment step: you reopen the folder in
  the container and start it again there with the answers it prints.
- It ends with a removal list for you to run and one commit message to confirm.

Without Claude Code, `TEMPLATE-README.md` has the same pass as a manual checklist.

## 3. What is still yours afterwards

`/adopt` sets up the process. It does not invent your project:

- The skeleton knowledge-base docs (overview, domain model, architecture, build/run/deploy, testing,
  glossary) stay empty and are seeded as backlog items.
  - Build/run/deploy keeps as shipped the parts that carry the template's rules, which its ADAPT
    note names: the verify contract with its lock rules, and the release principles where you
    deploy.
- A project with a UI surface also gets `frontend.md`, empty and seeded the same way.
- The Definition of Done's item on texts that explain your product's rules to its users is yours
  to keep or delete.
- The domain-falsifier lens (your core calculations and invariants), the high-risk areas that
  escalate the review gate, the paths each code lens reads and the models behind the review tiers
  need your names for them; the round at which a scenario gate that does not converge ends has a
  default you may keep, and so has the scenario calendar, the year, month and times of day your
  scenarios use.
<!-- MODULE gate -->
- With the gate module, `/adopt` fills the ADAPT block of `gate/dossier-check` from your stack
  preset's `gate.env`, the base ref of your landing model, your UI's paths and, with the KB guard,
  its docs-guard values; your test paths, runner lines, the lenses' read paths and your backlog's
  row and doc-debt shapes are yours to check there.
<!-- /MODULE gate -->
<!-- MODULE guard -->
- With the guard module, `/adopt` sets where Claude Code runs (`ENVIRONMENT` in the ADAPT block of
  `agent/bash-shape-guard`) and the volume root of `agent/session-check`; the output target for
  commands that name git and the worktree root keep their shipped values, yours to move. A native
  setup pins `python` in `.tool-versions`; a host without a working `python3` and a native Windows
  checkout leave the guard inactive, which the session-start report says out loud.
<!-- /MODULE guard -->
<!-- MODULE security-lens -->
- With the security-lens module, `/adopt` writes the path prefixes of your authentication surface
  (`gate/authentication-surface`) and the host its requests reach; the security lens's attack
  list, the way your app starts for it and the form of its counter-tests are yours to fill.
<!-- /MODULE security-lens -->
<!-- MODULE acceptance -->
- With the acceptance module on java-gradle, `/adopt` copies its files into your test tree in your
  base package, sets the sweep form and merges its build lines; the browser layer of the
  devcontainer image stays as shipped. The parallelism, its measured times and the grid recipe
  (copied on request only; its image tag and service-host alias) are yours to fill and check.
<!-- /MODULE acceptance -->
- With the java-gradle KB guard, `/adopt` copies its files into your test tree in your base
  package, sets the frontend source root it reads and merges its build inputs; its platform list
  and the example and stop lists of DOC-12 are yours to extend.
<!-- MODULE volumes -->
- With the volumes module, `/adopt` writes the name of the worktree volume and keeps the note on
  Git Bash's path rewriting on a Windows host only; the worktree root `/worktrees` stays as
  shipped, yours to move.
<!-- /MODULE volumes -->
<!-- MODULE agent-seat -->
- With the agent-seat module, which `/adopt` offers only when you ask for it (its step 3,
  question 13), three things stay yours, each a backlog item `/adopt` seeds (its step 9):
  - the seat machine;
  - building the agent image on it;
  - the landing on GitHub and the first live `/land`.
<!-- /MODULE agent-seat -->
- `/adopt` wires the probe location for agent throwaway code into the build of each preset it
  applies; a stack adopted without its preset needs it too, or the falsifier agents have nowhere
  to put a probe that does not turn the build red.
- An existing codebase gets its scenarios before any feature work: where you choose it, `/adopt`
  seeds `/bdd-catch-up` as task 0, and you confirm, capability by capability, what the code does
  and what it should do.
- Five stack presets ship full (Java/Gradle, JS-TS/npm, Python, C++, C#/.NET with a Unity adapter
  addendum) and one CI preset (GitLab). A stack outside them is adopted by extending the nearest
  preset's shape; another CI system gets its preset on demand.

## What you are signing up for

Two rules are not optional here, and they shape the rest:

- **BDD first, executable.** A feature with observable behavior starts with Gherkin scenarios in
  product language that **you confirm as the complete acceptance** before any production code. They
  are committed `@wip`, TDD cycles drive them green, and the last cycle arms them.
- **No comments.** Knowledge goes into a name, a test name, or the knowledge base. A git hook
  refuses any diff that adds a comment line to a source file, with no opt-out.

Both are deliberate and both are enforced mechanically. If you disagree with either, change the rule
in `docs/knowledge-base/engineering-handbook.md` and the hook in `hooks/` together, before you start,
rather than working around them later.

## Where things live

| Looking for | Read |
|---|---|
| The map of everything | `docs/knowledge-base/README.md` |
| How code should look, and how we develop | `docs/knowledge-base/engineering-handbook.md` |
| Working with the agent, permissions, token hygiene | `docs/knowledge-base/working-with-ai-agents.md` |
| The agent team and its gates | `docs/knowledge-base/multi-agent-playbook.md` |
| The git gate | `hooks/README.md` |
| The template itself, adoption paths, update path | `TEMPLATE-README.md` |
