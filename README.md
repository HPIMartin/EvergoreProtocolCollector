# Evergore Protocol Collector

A Java / Micronaut service that scrapes the browser game **Evergore** for a guild's bank and
storage (*Lager*) transaction logs, values item movements via a built-in item catalog, and computes
each member's contribution, **automating a manual Google Sheet**. It also serves as a showcase of
clean, test-driven, AI-assisted development.

## Documentation

Everything durable lives in the knowledge base. **Start at
[docs/knowledge-base/README.md](docs/knowledge-base/README.md)**:

- *What & why:* project overview, the Google Sheet it replaces, the domain model & value math.
- *How it's built:* architecture, build/run/deploy, testing, glossary.
- *How we work:* [engineering handbook](docs/knowledge-base/engineering-handbook.md) ·
  [working with AI agents](docs/knowledge-base/working-with-ai-agents.md) ·
  [multi-agent playbook](docs/knowledge-base/multi-agent-playbook.md) ·
  [dev environment](docs/knowledge-base/dev-environment.md).

Plan & decisions: [backlog](docs/backlog.md) · [open questions](docs/open-questions.md).
Per-session bootstrap for AI agents: [CLAUDE.md](CLAUDE.md).

## Develop & run (fully virtualized, nothing installed on the host)

- Opening the repo in its devcontainer:
  [dev-environment.md](docs/knowledge-base/dev-environment.md), "How to work in it (recommended)".
- Build and test, inside the container, through the one entry point:

  ```bash
  ./verify all
  ```

- The `./verify` modes: [build-run-deploy.md](docs/knowledge-base/build-run-deploy.md), "The verify
  script (the one entry point)".
- The JDK-upgrade procedure: [dev-environment.md](docs/knowledge-base/dev-environment.md), "JDK
  version (upgrade procedure)".
- Production image and runtime config:
  [build-run-deploy.md](docs/knowledge-base/build-run-deploy.md).

## Conventions

- Engineering rules: the [engineering handbook](docs/knowledge-base/engineering-handbook.md).
- Documentation rules: the DOC checklist, [knowledge base README](docs/knowledge-base/README.md),
  "Doc conventions".
