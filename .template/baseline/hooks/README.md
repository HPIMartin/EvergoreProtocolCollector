# Git enforcement hooks

POSIX-sh hooks that mechanically enforce the commit rules from the
[engineering handbook](../docs/knowledge-base/engineering-handbook.md) (git section). The handbook is
the canonical source for the rules; this file only describes the mechanics.

## Activation (required in every checkout)

```sh
git config core.hooksPath hooks
```

The devcontainer variant wires that into `postCreate` so it happens automatically (shipped in the
template under `env/devcontainer/`); on the native variant, run it once per checkout or from your
bootstrap script (see `docs/knowledge-base/dev-environment.md`).

## The hooks

- **`commit-msg`**: enforces the single-line, present-tense-verb-first subject (optional `[doc] `
  tag); rejects a body, `Co-Authored-By`, conventional-commit prefixes, and any tool footer.
  Project-agnostic, no adaptation needed.
- **`pre-commit`**: scans staged content for secrets and personal/host data (key files, private
  keys, credentials, real emails, absolute host paths, hard-coded tokens in production source),
  flags per-language code smells, and runs your stack's fast format/style gate once wired (ships
  as a commented example). The test run and full build are deliberately excluded so the TDD
  micro-commit loop stays fast.

> **ADAPT:** `pre-commit` carries four marked spots: the host-path allowlist, the production-source
> tree for the token scan, the smell patterns for your language, and your stack's format/style
> commands. Search the script for `ADAPT`.

`--no-verify` bypasses both hooks and is for genuine emergencies only; the one sanctioned
non-emergency use is the `/pause` skill's `[wip]` parking commit (handbook, git section).
