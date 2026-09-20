# 02: Domain Model

> **ADAPT:** this doc is a skeleton: document your domain types and core business rules, verified
> against code and tests, not from memory.

> **ADAPT:** one line stating where domain types live in the source tree and that they are
> framework-free (the enforcing rule lives in [architecture.md](architecture.md)).

## Core concepts

> **ADAPT:** one row per concept: name, implementing type (record/enum/interface plus its package),
> and a one-line note on its role and any non-obvious semantics.

| Concept | Type | Notes |
|---------|------|-------|
| | | |

## Core business rules

> **ADAPT:** the heart of the domain: the invariants, formulas, or state machines that make the
> product correct. Show the actual rules with one or two worked examples, each verified against a
> unit test (cite the test). Flag known semantic gaps explicitly and track them in the backlog.

## How the outputs are computed

> **ADAPT:** walk through the central computation/aggregation use case: inputs, steps, where results
> are persisted, and any watermark/idempotency mechanism.

## Known quirks

> **ADAPT:** identity/equality oddities, legacy smells, and surprising behaviors a maintainer must
> know before touching the model, each with a backlog reference if it should be fixed.

See [architecture.md](architecture.md) for how these types flow through the system and
[glossary.md](glossary.md) for the domain terms.
