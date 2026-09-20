# Start here

You have the **AI-assisted development process template**: a project-agnostic process layer for
clean, test-driven, AI-assisted development. It brings a tool-neutral knowledge base, a multi-agent
review pipeline, a committed permission policy, and git hooks that enforce the rules mechanically.

It carries **no product code**. Nothing here is Java, TypeScript or Python specific until you pick a
stack preset during adoption.

## 1. Extract it into your project folder

```sh
unzip ai-dev-process-template-v*.zip -d <your-project>
```

Files land at the root, no wrapping folder. **If the folder is an existing repo**, extract into an
empty directory first and merge by hand: `.gitignore` and `.gitattributes` would otherwise be
overwritten rather than merged. `README.md` is safe, the template's own reference ships beside this
file as `TEMPLATE-README.md`.

For a brand-new project: extract, then `git init`.

## 2. Run `/adopt`

Open the folder in Claude Code and run:

```
/adopt
```

Two things matter for this one session:

- **Pick the strongest model available.** Adoption is judgment work, not typing.
- **Start it in a fresh session**, with nothing else in the context.

`/adopt` keeps the pristine payload as the merge base for later upgrades, analyses an existing tree
first, and interviews you (project name, one-liner, stacks and how they compose, the tools per
stack, the one acceptance runner, dev-environment variant, ticket system and wiki, commit-key
pattern, CI and dependency bot, upgrade policy, conversation language). It applies the stack presets
and the environment variant, installs `verify`, resolves every placeholder, seeds the living docs
and, for an existing codebase, a modernisation track and the scenario catch-up as task 0, activates
and **verifies** the git hooks, then hands you a cleanup list and one commit message to confirm. It
asks rather than guesses, so answer as you go.

Without Claude Code, `TEMPLATE-README.md` has the same pass as a manual checklist.

## 3. What is still yours afterwards

`/adopt` sets up the process. It does not invent your project:

- The skeleton knowledge-base docs (overview, domain model, architecture, build/run/deploy, testing,
  glossary) stay empty and are seeded as backlog items.
- The domain-falsifier lens (your core calculations and invariants) and the high-risk areas that
  escalate the review gate need your names for them.
- The probe location for agent throwaway code has to be wired into your build; without it the
  falsifier agents have nowhere to put a probe that does not turn the build red.
- An existing codebase gets its scenarios before any feature work: `/adopt` seeds `/bdd-catch-up`
  as task 0, and you confirm, capability by capability, what the code does and what it should do.
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
