# 03: Architecture

> **ADAPT:** this doc is a skeleton: document your system's layering and data flow, verified against
> the code, not from memory. The layer vocabulary and the enforcement rule come from
> [engineering-handbook.md](engineering-handbook.md) §1; this doc maps them to your concrete packages.

{{TECH_STACK}}. Exact versions live in the build file, the single source of truth; this prose must
not duplicate them.

> **ADAPT:** one line naming the stack and the root package/module, plus a link to the build file.

## Data flow (the one use case that matters)

> **ADAPT:** an ASCII diagram of the central end-to-end flow: what triggers it, the steps it runs,
> the ports/adapters it crosses, and where results are persisted. Add the independent read paths
> (HTTP, monitoring) as separate lines. Keep it to one screen; a new session should grasp the system
> from this diagram alone.

## Layers & responsibilities (condensed)

> **ADAPT:** one bullet per layer, naming the concrete packages and key classes:
> entry/lifecycle plus the composition root (the single place that knows concrete classes),
> framework-free application use cases, framework-free domain types, inbound adapters, outbound
> adapters (persistence, external clients), and cross-cutting concerns (logging, config).

## Boundary rules (enforced, not aspirational)

The core layers (domain, application) are **framework-free** and depend only inward, never on
adapters or config. Enforce this with an automated architecture test that **fails the build** on any
violation, so the boundary is a build gate, not a convention (handbook §1). Verify the test is
non-vacuous once (temporarily forbid something the core genuinely uses and watch it fail).

> **ADAPT:** name your framework-free packages, the forbidden import roots (framework, drivers,
> persistence, HTTP), and the architecture test class that enforces them (example from the origin
> project: an ArchUnit test class run by the normal test task).

## Gap analysis

> **ADAPT:** a table of outbound ports and their status (exists and done right / partial / missing),
> plus a short numbered list of the top violations to fix, each tracked in
> [../backlog.md](../backlog.md).

## Target structure (if a restructuring is planned)

> **ADAPT:** the proposed package layout and what moves where; delete this section if the current
> structure is the target.
