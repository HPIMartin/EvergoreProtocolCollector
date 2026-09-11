# 13: Agent entry template (per-tool session bootstrap)

Canonical template for a **per-tool entry file**: the thin wrapper an AI tool auto-loads at session
start (Claude Code: [`/CLAUDE.md`](../../CLAUDE.md); other tools their own equivalent). Every tool
bootstraps from the **same** rules without duplicating the KB: a wrapper *points* into the KB and
adds only that tool's mechanics. The KB (`docs/knowledge-base/` + `backlog.md` +
`open-questions.md`) is the single source of truth.

**Template version: 4.** Bump on any change to the SHARED section. Every wrapper records the
version it was built from and quick-checks at session start; re-sync on a bump. See the
[KB README](README.md) "step 0".

## How to build a wrapper from this template

1. Copy the **SHARED** section verbatim (links are repo-root-relative, as for `CLAUDE.md`; adjust
   if the wrapper sits elsewhere).
2. Fill the **TOOL-SPECIFIC** section with only that tool's mechanics (auto-load behaviour, harness
   quirks, sub-agent wiring). Never restate KB rules; point to the KB.
3. Record `Based on agent-entry-template version: N` near the top.

---

## SHARED section (copy into every wrapper; keep in sync with this template)

> Conversation may be in German; code, comments, identifiers, and docs are always English.

**What this is:** a Java / Micronaut service that scrapes the browser game *Evergore* for a guild's
bank + storage transaction logs, values item movements, and computes each member's contribution,
automating a manual Google Sheet; also a showcase of clean, test-driven, AI-assisted development.
(Detail: [README.md](README.md) / [project-overview.md](project-overview.md).)

**Start here (every session):**

0. No native entry file for your tool? Build one now from this template (KB README "step 0") and
   record the template version.
1. Read the KB **map** first: [`docs/knowledge-base/README.md`](docs/knowledge-base/README.md);
   then only the sections relevant to the task. Do **not** re-scan the codebase or read big docs
   whole (token hygiene → working-with-ai-agents.md).
2. Check the backlog's **"▶ Current status / next action"** in [`docs/backlog.md`](docs/backlog.md)
   and decisions touching your task in [`docs/open-questions.md`](docs/open-questions.md).
3. Standards: [`engineering-handbook.md`](docs/knowledge-base/engineering-handbook.md); AI workflow:
   [`working-with-ai-agents.md`](docs/knowledge-base/working-with-ai-agents.md); agent team:
   [`multi-agent-playbook.md`](docs/knowledge-base/multi-agent-playbook.md).

**The rules live in the KB; point, never duplicate** (duplication drifts):

- **Commit protocol, branching, merge & review gateway** → handbook §7. (Propose one one-line,
  present-tense-verb message; confirm; commit. **Never `git push`.** Worktree per context → rebase →
  review the rebased tip → `--ff-only`.)
- **TDD / BDD cycle** → handbook §4/§5.
- **Review gate & FAIL-loop governance** → handbook §9.
- **Clean code, SOLID, hexagonal, immutable records, warnings-as-errors, no secrets/host-data** →
  handbook §1–§3.
- **Definition of Done** → handbook §8.
- **In-container-only dev** (never install/run JDK/Gradle/Firefox on the host) → dev-environment.md.
- **Doc conventions** (terse bullets, git-is-history, backlog holds only live work, KB-current,
  decisions → open-questions.md) → the **DOC checklist** in the KB README; the `doc-reviewer`
  agent gates on it.
- **Ask, don't guess:** author decisions get multiple-choice options (recommended first), recorded
  in `open-questions.md`.
- **Handing the author a review:** every review request carries a GitLens compare statement,
  `<tip>..<base>` and nothing else → working-with-ai-agents.md.
- **Context & token hygiene** (section-scoped reads, no re-reads, batched tool calls, short focused
  sessions) → working-with-ai-agents.md.

---

## TOOL-SPECIFIC section (each wrapper fills its own)

Only the genuine mechanics of the specific tool, e.g.:

- How/whether the file auto-loads at session start.
- Harness quirks (Bash-stdout behaviour, permission-blocked commands).
- Sub-agent / agent-team wiring specific to the tool (e.g. the `.claude/agents/` definitions).
