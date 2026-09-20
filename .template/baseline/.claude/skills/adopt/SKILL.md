---
description: Adopt the process template into this repo (interview, env variant, stack preset, placeholder resolution, seeded docs)
argument-hint: [optional: answers you already know, e.g. "java-gradle, devcontainer"]
---

You are adopting the AI-assisted development process template into this repository. The template
files are already copied in (adoption path A or B per the template README). Your job: turn the
template into THIS project's process layer, in one reviewed pass.

Ground rules for the whole run: ask, don't guess (multiple-choice options, recommended one first);
at the end propose exactly ONE one-line commit message and wait for confirmation; never push; hand
deletions to the author as exact commands instead of running them yourself.

Answers the author already provided (skip re-asking those): $ARGUMENTS

## 1. Preflight

- Confirm you are at the repo root: `TEMPLATE-VERSION` and `docs/knowledge-base/README.md` must
  exist. If `env/` or `stacks/` are already gone, those steps were done earlier; skip them.
- Path A (fresh repo from the template) or path B (drop-in into an existing repo)? Detect from the
  tree (sources/docs that are not template files) and confirm with the author. On path B, existing
  docs are kept and slotted into the KB map, never overwritten (template README, path-B notes).

## 2. Interview (one round, multiple-choice where possible)

1. **Project name** (`{{PROJECT_NAME}}`) and **one-liner** (`{{PROJECT_ONE_LINER}}`: what it does,
   for whom, what it deliberately showcases).
2. **Stack**: one preset from `stacks/` (java-gradle, js-ts-npm, python, csharp-unity, cpp), or two
   for a mixed project. For a stub preset, tell the author it must be extended per
   `stacks/README.md`.
3. **Environment variant**: devcontainer (recommended: isolation for agents) or native pinned
   toolchain (no container; the `env/` READMEs state the trade-off).
4. **Build and test commands**: prefill `{{BUILD_CMD}}`/`{{TEST_CMD}}` from the preset, have the
   author confirm or adjust.
5. **Conversation language** for the SHARED language line (code, comments, identifiers, and docs
   stay English regardless).

## 3. Apply the environment variant

- Copy the chosen variant's payload from `env/<variant>/` to the repo root (devcontainer:
  `.devcontainer/`; native: `.tool-versions`) and work through that variant README's adoption
  steps. The devcontainer `name` takes the project name; pinned versions come from the stack preset.
- Both variants: verify the two `cd` allow rules in `.claude/settings.json` against the real
  absolute repo root (`pwd`). They ship as `/workspaces/{{PROJECT_NAME}}`, the devcontainer mount
  convention, which is only correct when the checkout folder name equals the project name; a
  native checkout usually lives elsewhere entirely.
- Record the choice in `docs/knowledge-base/dev-environment.md` (its first ADAPT note).

## 4. Apply the stack preset(s)

Per `stacks/<preset>/README.md`:

- Merge its allow block into `permissions.allow` of `.claude/settings.json`; never touch the deny
  floor. Validate: `jq . .claude/settings.json`.
- If the resolved build/test commands are compound or parameterized (e.g. `--tests <Class>`, a
  `&&` chain), replace the generic `Bash({{BUILD_CMD}}:*)`/`Bash({{TEST_CMD}}:*)` rules with the
  preset's allow block: a `Bash(...)` rule only matches as a clean command prefix.
- Wire the format gate and the production-source tree in `hooks/pre-commit` (its ADAPT markers).
- Where the project build already exists, apply the warnings-as-errors and pinning notes; where it
  does not, record them as seeded backlog items (step 7).

## 5. Resolve the placeholders

- First delete the "Placeholder key" section from `docs/knowledge-base/README.md`: it is an
  adoption-time aid that documents the tokens as literal text and must not survive the replace.
- Then search-replace all five tokens with the interview values, EXCEPT in `env/`, `stacks/`, the
  template root `README.md`, and `.claude/skills/adopt/` (all deleted in step 9).
- Verify: `grep -rn "{{" .` finds hits only inside those excluded paths.

## 6. Work the ADAPT notes

`grep -rn "ADAPT:" .` and split the hits:

- **Resolve now** (the interview answered them): the SHARED language line, changed identically in
  `CLAUDE.md` AND `docs/knowledge-base/agent-entry-template.md` (the two SHARED sections must stay
  textually identical); the one-liner spots; the devcontainer / pre-commit / settings values from
  steps 3-4.
- **Leave in place** (project content the author fills over time): the skeleton KB docs (01-05,
  07), the domain-falsifier lens, the high-risk review areas, host quirks. Collect them as seeded
  backlog items instead of resolving them now.

## 7. Seed the living docs

- `docs/backlog.md`, "Current status": adopted template version N (read `TEMPLATE-VERSION`) on
  today's date; next actions: fill the skeleton docs, work the remaining ADAPT items (list them).
- `docs/open-questions.md`: record the interview decisions (stack, env variant, commands) as dated
  decisions with a one-line rationale each.
- `docs/process-learnings.md`: stays an empty table.

## 8. Activate and verify

- Hooks: `git config core.hooksPath hooks` (the devcontainer `postCreate` also does it; run it for
  this checkout anyway), then verify with `git config core.hooksPath`.
- Verify the pass: the step-5 grep is clean; `jq` clean on settings.json; `sh -n hooks/commit-msg`
  and `sh -n hooks/pre-commit` as two invocations (`sh -n` syntax-checks only its first file);
  relative doc links resolve (the root-relative links inside agent-entry-template.md are correct
  as documented there).

## 9. Cleanup (author-run) and the one commit

- Hand the author exact removal commands for: `env/` (both variants; the choice is recorded),
  `stacks/`, `user-scope/` (if present), and `.claude/skills/adopt/` (this skill). On path A also
  remind them to replace the template `README.md` with the project's own. **Keep
  `TEMPLATE-VERSION`**: it is the update marker (template README, "Updates").
- Propose ONE one-line, present-tense commit message (e.g. `Adopt the AI-assisted development
  process template`; path B may prefer a `[doc]` tag), wait for the author's confirmation, commit.
  Never push.
