# Frontend: {{PROJECT_NAME}}

The UI surface (a single-page app, a desktop view, a game view) and the API it reads.

- `falsifier-frontend` checks the UI against this doc.
- The rules for every stack live in [engineering-handbook.md](engineering-handbook.md); this doc
  holds the UI's own facts.

> **ADAPT:** fill each section from the UI's source, verified against it, and add the doc's row to
> the KB map ([README.md](README.md)).

## Stack

> **ADAPT:** the UI's language, framework and build tool; its test runner and test environment;
> its linter and formatter, and which lint rules fail the build. Versions stay in the build files
> (handbook §3).

## Structure & the dependency rule

- Dependencies point inward here too.
  - The UI's domain layer imports nothing from the rest of the UI.
  - Only the composition root composes.
  - Why: handbook §1, the dependency rule.
- A lint or architecture rule enforces the table below and fails the build on a violation.
  - Why: handbook §1, the automated architecture test.

| Folder | May import from | Purpose |
|--------|-----------------|---------|

> **ADAPT:** one row per top-level folder of the UI source, and the name of the rule that enforces
> the table.

## Conventions

> **ADAPT:** the UI's test conventions, e.g. a stable test-id attribute instead of text or style
> selectors, semantic markup for tables, and the lint rule that keeps the act a named value outside
> the assertion (handbook §6).

## The UI layer: theme and primitives

> **ADAPT:** the theme (where the design tokens live, and the test that guards them) and the
> presentational primitives: props in, callbacks out, no knowledge of routes or the API.

## The views

> **ADAPT:** one bullet per route or view: what it shows and loads, its loading, empty and error
> states, and what an unknown route shows. A page of text pinned to an approved file names that
> file (handbook §5).

## The API the UI reads

- The API layer translates every wire shape into domain types and refuses a shape it cannot read.
  - Why: a shape let through unread would surface later as a missing value far from its cause.

| Route | Answers |
|-------|---------|

> **ADAPT:** one row per route the UI calls, and where the published contract types live.

## Build integration

> **ADAPT:** how `./verify all` runs the UI's install, build, tests and lint; where the built UI
> lands in the artifact; the dev server for driving the UI against a running service.
