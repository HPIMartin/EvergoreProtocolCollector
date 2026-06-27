# 13 — Agent entry template (per-tool session bootstrap)

The canonical template for a **per-tool entry file** — the thin wrapper an AI tool auto-loads at the
start of every session (Claude Code loads [`/CLAUDE.md`](../../CLAUDE.md); another tool loads its own
equivalent). It exists so every tool bootstraps from the **same** rules without duplicating the
knowledge base: a wrapper *points* into the KB and adds only that tool's own mechanics. The KB
(`docs/knowledge-base/` + `backlog.md` + `open-questions.md`) is the single source of truth.

**Template version: 1.** Bump this on any change to the SHARED section below. Every wrapper records the
version it was built from and, at session start, quick-checks that its SHARED section still matches —
re-syncing on a bump. See the [KB README](README.md) "step 0".

## How to build a wrapper from this template

1. Copy the **SHARED** section into the new wrapper verbatim (the links are written repo-root-relative,
   as for `CLAUDE.md`; adjust the relative paths if the wrapper does not sit at the repo root).
2. Fill the **TOOL-SPECIFIC** section with only that tool's mechanics (auto-load behaviour, harness
   quirks, sub-agent wiring). Never restate KB rules there — point to the KB.
3. Record `Based on agent-entry-template version: N` near the top of the wrapper.

---

## SHARED section (copy into every wrapper — keep in sync with this template)

> Conversation may be in German; code, comments, identifiers, and docs are always English.

**What this is:** a Java / Micronaut service that scrapes the browser game *Evergore* for a guild's
bank + storage transaction logs, values item movements, and computes each member's contribution —
automating a manual Google Sheet; also a showcase of clean, test-driven, AI-assisted development.
(Detail: [README.md](README.md) / [project-overview.md](project-overview.md).)

**Start here (every session):**

0. If your tool has **no native entry file**, build one now from this template (see "step 0" in the
   KB README) and record the template version.
1. Read the knowledge base first — [`docs/knowledge-base/README.md`](docs/knowledge-base/README.md);
   do **not** blindly re-scan the codebase.
2. Check the backlog [`docs/backlog.md`](docs/backlog.md) and open decisions
   [`docs/open-questions.md`](docs/open-questions.md).
3. Engineering standards: [`engineering-handbook.md`](docs/knowledge-base/engineering-handbook.md);
   AI workflow: [`working-with-ai-agents.md`](docs/knowledge-base/working-with-ai-agents.md);
   agent team: [`multi-agent-playbook.md`](docs/knowledge-base/multi-agent-playbook.md).

**The rules live in the KB — point, never duplicate** (duplication drifts):

- **Commit protocol, branching, merge & the review gateway** → handbook §7. (Propose one one-line,
  present-tense-verb message; confirm; commit. **Never `git push`.** Worktree per context → rebase →
  review the rebased tip → `--ff-only`.)
- **TDD / BDD cycle** → handbook §4/§5.
- **Review gate & FAIL-loop governance** → handbook §9.
- **Clean code, SOLID, hexagonal, immutable records, warnings-as-errors, no secrets/host-data** →
  handbook §1–§3.
- **Definition of Done** → handbook §8.
- **In-container-only dev** (never install/run JDK/Gradle/Firefox on the host) → dev-environment.md.
- **KB-first / KB-current; git is history, docs are knowledge** (no changelog/diff narration in docs;
  remove completed backlog items, keep only rejected/deferred with rationale) → KB README + handbook §7.
- **Ask, don't guess** — author decisions get multiple-choice options (recommended first), recorded in
  `open-questions.md`.

---

## TOOL-SPECIFIC section (each wrapper fills its own)

Only the genuine mechanics of the specific tool — for example:

- How/whether this file auto-loads at session start.
- Harness quirks (e.g. a host's Bash-stdout behaviour, permission-blocked commands).
- Sub-agent / agent-team wiring specific to the tool (e.g. the `.claude/agents/` subagent definitions).

These are the *only* things a wrapper adds on top of the SHARED section.
