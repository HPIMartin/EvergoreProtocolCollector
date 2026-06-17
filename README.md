# Evergore Protocol Collector

A Java 17 / Micronaut service that scrapes the browser game **Evergore** for a guild's bank and
storage (*Lager*) transaction logs, values item movements via a built-in item catalog, and computes
each member's contribution — **automating a manual Google Sheet**. It also serves as a showcase of
clean, test-driven, AI-assisted development.

## Documentation

Everything durable lives in the knowledge base — **start at
[docs/knowledge-base/README.md](docs/knowledge-base/README.md)**:

- *What & why:* project overview, the Google Sheet it replaces, the domain model & value math.
- *How it's built:* architecture, build/run/deploy, testing, glossary.
- *How we work:* [engineering handbook](docs/knowledge-base/engineering-handbook.md) ·
  [working with Claude](docs/knowledge-base/working-with-claude.md) ·
  [multi-agent playbook](docs/knowledge-base/multi-agent-playbook.md) ·
  [dev environment](docs/knowledge-base/dev-environment.md).

Plan & decisions: [backlog](docs/backlog.md) · [open questions](docs/open-questions.md).
Per-session bootstrap for AI agents: [CLAUDE.md](CLAUDE.md).

## Develop & run (fully virtualized — nothing installed on the host)

Open the repo in **VS Code → Dev Containers: Reopen in Container** (provisions the JDK), then:

```bash
./gradlew build    # build & test, inside the container
```

Dev-environment details and the JDK-upgrade procedure: [dev-environment.md](docs/knowledge-base/dev-environment.md).
Production image & runtime config: [build-run-deploy.md](docs/knowledge-base/build-run-deploy.md).

## Conventions

Clean code · TDD · BDD (for docs/showcase) · SOLID · hexagonal architecture. **Git is the source of
truth for history** — fine-grained commits and their messages are the changelog; the docs don't
duplicate them. See the [engineering handbook](docs/knowledge-base/engineering-handbook.md).
