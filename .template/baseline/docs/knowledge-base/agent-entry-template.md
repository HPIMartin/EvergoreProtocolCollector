# 12: Agent entry template (per-tool session bootstrap)

The canonical template for a **per-tool entry file**: the thin wrapper an AI tool auto-loads at the
start of every session (Claude Code loads [`/CLAUDE.md`](../../CLAUDE.md); another tool loads its own
equivalent). It exists so every tool bootstraps from the **same** rules without duplicating the
knowledge base: a wrapper *points* into the KB and adds only that tool's own mechanics. The KB
(`docs/knowledge-base/` + `backlog.md` + `open-questions.md`) is the single source of truth.

**Template version: 1.** Bump this on any change to the SHARED section below. Every wrapper records the
version it was built from and, at session start, quick-checks that its SHARED section still matches,
re-syncing on a bump. See the [KB README](README.md).

## How to build a wrapper from this template

1. Copy the **SHARED** section into the new wrapper verbatim, heading included. It must stay
   textually identical across all wrappers and this template; that identity is the sync mechanism.
   The links inside are written repo-root-relative, correct for a wrapper sitting at the repo root
   like `CLAUDE.md` (they do not resolve from this file's own location). Adjust the relative paths
   only if the wrapper does not sit at the repo root, and note the adjustment in the wrapper.
2. Fill the **TOOL-SPECIFIC** section with only that tool's mechanics (auto-load behaviour, harness
   quirks, sub-agent wiring). Never restate KB rules there; point to the KB.
3. Record `Built from agent-entry-template.md · based on template version: N` near the top of the
   wrapper.

---

## SHARED section (keep textually identical in every wrapper and in agent-entry-template.md)

> Conversation may be in German; code, comments, identifiers, and docs are always English.
>
> **ADAPT:** Set the conversation language to the author's preference. Code, comments, identifiers, and docs stay English regardless.

**What this is:** {{PROJECT_ONE_LINER}}
(Detail: [README.md](README.md) / [project-overview.md](docs/knowledge-base/project-overview.md).)

> **ADAPT:** Replace {{PROJECT_ONE_LINER}} with one or two sentences: what the project does and what it deliberately showcases.

**Start here (every session):**

0. If your tool has **no native entry file**, build one now from
   [`agent-entry-template.md`](docs/knowledge-base/agent-entry-template.md) and record the template
   version (bootstrap step in the [KB README](docs/knowledge-base/README.md)).
1. Read the knowledge base **map** first, [`docs/knowledge-base/README.md`](docs/knowledge-base/README.md);
   then read only the sections relevant to the task. Do **not** blindly re-scan the codebase or read
   the big docs whole (token hygiene → working-with-ai-agents.md).
2. Check the backlog's **"▶ Current status / next action"** section in
   [`docs/backlog.md`](docs/backlog.md) and any decisions touching your task in
   [`docs/open-questions.md`](docs/open-questions.md).
3. Engineering standards: [`engineering-handbook.md`](docs/knowledge-base/engineering-handbook.md);
   AI workflow: [`working-with-ai-agents.md`](docs/knowledge-base/working-with-ai-agents.md);
   agent team: [`multi-agent-playbook.md`](docs/knowledge-base/multi-agent-playbook.md).

**The rules live in the KB; point, never duplicate** (duplication drifts):

- **Commit protocol, branching, merge & the review gateway** → handbook §7. Propose **one** one-line,
  present-tense-verb message (optional `[doc]` tag), get the author's confirmation, then commit.
  **Never `git push`.** Worktree per context → rebase → review the rebased tip → `--ff-only`.
- **TDD / BDD cycle** → handbook §4/§5.
- **Review gate & FAIL-loop governance** → handbook §9.
- **Clean code, design principles, architecture rules, warnings-as-errors, no secrets/host-data** →
  handbook §1–§3.
- **Definition of Done** → handbook §8.
- **Pinned dev environment** (work only through the project's chosen variant, devcontainer or
  pinned native toolchain; no ad-hoc host installs) → dev-environment.md.
- **Build / run / deploy** → [`build-run-deploy.md`](docs/knowledge-base/build-run-deploy.md).
- **KB-first / KB-current; git is history, docs are knowledge** (no changelog/diff narration in docs;
  remove completed backlog items, keep only rejected/deferred with rationale) → KB README + handbook §7.
- **Ask, don't guess:** author decisions get multiple-choice options (recommended first), recorded in
  [`docs/open-questions.md`](docs/open-questions.md).
- **Context & token hygiene** (section-scoped reads, no re-reads, batched tool calls, short focused
  sessions) → working-with-ai-agents.md.

---

## TOOL-SPECIFIC section (each wrapper fills its own)

Only the genuine mechanics of the specific tool, for example:

- How/whether the wrapper file auto-loads at session start.
- Harness quirks (e.g. a host's Bash-stdout behaviour, permission-blocked commands).
- Sub-agent / agent-team wiring specific to the tool (e.g. the `.claude/agents/` subagent definitions).

These are the *only* things a wrapper adds on top of the SHARED section.
