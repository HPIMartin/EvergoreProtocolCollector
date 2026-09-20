# Git enforcement hooks

POSIX-sh hooks that mechanically enforce the commit rules from the
[engineering handbook](../docs/knowledge-base/engineering-handbook.md) (git section). The handbook is
the canonical source for the rules; this file only describes the mechanics.

## Activation (required in every checkout)

```sh
git config core.hooksPath hooks
```

Hooks are not shared by clone, so this runs once per checkout. The devcontainer variant wires it into
`postCreate` (shipped in the template under `env/devcontainer/`); on the native variant, run it once
per checkout or from your bootstrap script (see `docs/knowledge-base/dev-environment.md`).

**Every hook script must be tracked as `100755`.** git skips a non-executable hook outright on POSIX,
so the gate would not narrate, it would not run at all, and `core.fileMode=false` (the default on a
Windows host) hides that from every local check. Verify with `git ls-files -s hooks/` and fix with:

```sh
git update-index --chmod=+x hooks/<name>
```

`hooks/self-test` asserts this, so a hook that loses its bit fails the gate instead of silently
disabling it.

## The gate, in one paragraph

**git asks `pre-commit` only for `git commit`.** Every commit the sequencer builds itself, a rebase
replaying a pick, `git rebase --continue`, `cherry-pick`, `revert`, is created with no `pre-commit`
hook at all: a hook that only prints is not a gate. The suite closes that by splitting the checks
from the hooks. `content-gate` and `format-gate` hold the checks; the hooks that run *before* a
commit exists (`pre-commit`, `pre-applypatch`, `pre-merge-commit`) refuse outright, and the ones that
can only run *after* git already moved the refs (`post-rewrite`, `post-commit`) **record** the
offending sha, whereupon `pre-commit` refuses every further commit until that sha is gone.

## The checks

- **`content-gate`** (not a hook, the shared content check; `--staged` reads the index, `--commit
  <sha>` reads one commit). Scans for private-key blocks, AWS-style access keys, credential
  literals, real e-mail addresses, absolute host paths, committed key/keystore files; hard-coded
  auth tokens in production source only; `TODO`/`FIXME` plus each family's debug-output constructs
  (`debug_print_re`: `System.out`/`printStackTrace`, `console.log`, `Console.Write`,
  `printf`/`std::cout`, `print(`/`breakpoint()`) in every scanned source file except the path
  prefixes listed in `print_allow_paths`, where stdout *is* the interface (default: none,
  `CONTENT_GATE_PRINT_ALLOW` overrides it for a run); and **every comment line the diff adds** to a
  listed source extension, which is what makes the no-comment rule (handbook §3) mechanical instead
  of a reviewing habit. The comment scan reads the file's post-image, marks the lines that carry a
  comment outside every literal, then intersects those with the lines the diff adds, so a URL or an
  XPath is never read as a comment and the existing stock stands until a commit edits it. **No opt-out.** Every
  category is reported together, never just the first that hits, so a new violation cannot hide
  behind an older one. The diff it reads carries forced `a/`/`b/` prefixes and its header path is
  taken without a trailing tab, so neither a repository-level `diff.noprefix` nor a path containing a
  space can quietly take a file out of the scan. Renamed paths are in scope for the same reason: a
  rename reports as `R`, so a file renamed and edited in one commit would otherwise be read by
  nothing at all. Rename detection stays on while `R` joins the scanned statuses, so a move that
  edits nothing contributes no added lines and the stock the file already carried is not re-read as
  new. One scanner per comment syntax, each keyed off an extension list in the gate's ADAPT block:
  - `comment_slash_ext` (C family, JVM, JS/TS): `//` and `/* */` outside `"…"`, `'…'`, a backtick
    template and a `"""` text block. `#include`, `#define`, `#region` and `#pragma` are code there,
    not comments, and pass. Known residue: a TypeScript regular-expression literal containing `//`.
  - `comment_hash_ext` (Python, CMake, TOML): `#` outside `"…"` and `'…'`. A line-1 shebang is the
    interpreter, not prose, and passes.
  - `docstring_ext` (Python): a line whose first non-blank characters open a triple-quoted string
    (with optional `r`/`b`/`u`/`f` prefix) is a doc comment, because a string standing where a
    statement belongs is a docstring or a no-op. `x = """…"""` is a value and passes; a `#` inside a
    triple-quoted string is not a comment.
  - `directive_allow_re`, in the `docstring_ext` family only: a comment whose entire text from its
    `#` onward is a tool directive (`# noqa`, `# type:`, `# fmt:`, `# ruff:`, `# nosec`, …) is
    configuration, not explanation, and passes wherever it sits on the line. A directive followed by
    explanatory prose (`# noqa because reasons`) is still refused.
  - `comment_xml_ext` (MSBuild project, props and targets files): every line a `<!-- … -->` block
    covers, however many it spans.

  `hooks/` and `.template/` are outside every scan: the first holds the detection patterns
  themselves, the second the adopted template's baseline, and neither is the project's own source.

  A file counts as **production source** when it starts with one of `production_trees`, starts with
  none of `test_trees` and matches none of `test_name_re`; the rules that bind production code only
  (the hard-coded-token scan and `production_smell_re`, today the C# null-forgiving operator) key
  off exactly that, so a test may carry a non-secret test token and may override the compiler's
  nullability answer where production source may not.
- **`format-gate`** (not a hook): runs the repo's own `./verify format`, the stack's fast
  format/style check, and nothing else; it is guarded on that script being present and executable,
  so a checkout without a toolchain still commits. Called by all five commit-path hooks, so the gate
  has **one** spot instead of five, and `hooks/self-test` stubs `./verify` to assert the leg
  behaviourally. The test run and the full build stay out of it, so the TDD micro-commit loop stays
  fast (commits are already green by the time they are made).

## The hooks

- **`commit-msg`**: single-line, present-tense-verb-first subject (optional `[doc] ` tag); rejects a
  body, `Co-Authored-By`, conventional-commit prefixes, and any tool footer. Optionally a tracker
  key: with `ticket_key_re` set, the sanctioned shape is `<verb …> (KEY)` with exactly one space
  before the group, or `[KEY] <verb …>` under `ticket_position=prefix`, where an optional `[doc] `
  tag follows the key (`[PROJ-12] [doc] Groom the backlog`). What remains after stripping the key
  still has to satisfy every rule above. An empty `ticket_key_re`, the shipped default, switches
  that leg off; a key in the other position, or a tag that is no valid key, is refused rather than
  waved through. The key is optional unless `ticket_required=yes`, which refuses a subject without
  one. `COMMIT_MSG_TICKET_KEY_RE`, `COMMIT_MSG_TICKET_POSITION` and `COMMIT_MSG_TICKET_REQUIRED`
  override the three settings for a run, which is how `hooks/self-test` exercises every mode without
  editing the hook.
- **`pre-commit`**: refuses while a breach is recorded (`breach-guard`), runs `content-gate
  --staged`, runs `hooks/self-test` whenever the commit touches `hooks/` (so a weakened gate cannot
  land), runs `stacks/self-test` whenever the commit touches `stacks/` and refuses when that script
  is missing (so a weakened preset cannot land either, and deleting the proof is not a way out),
  runs `format-gate`, and stamps the approved tree in `.git/gate-stamp`. Whenever a commit touches
  `CLAUDE.md` or `docs/knowledge-base/agent-entry-template.md`, it also diffs the two files' SHARED
  section, read from the staged blob and falling back to `HEAD` for the file the commit leaves
  alone, and refuses while they differ, so a wrapper cannot drift from the template it records. The
  same leg reads `**Template version: N.**` from the template and refuses a wrapper that does not
  record `version: N`, so a bump lands in the template and in every wrapper together.
- **`pre-applypatch`** / **`pre-merge-commit`**: the gates for `git am` and for a merge commit, both
  of which git builds without asking any commit hook. They run on the applied or resolved index
  *before* their commit exists, so they refuse outright. `pre-merge-commit` guards the merge somebody
  makes by mistake; the sanctioned landing is `git merge --ff-only`, which creates no commit to gate.
- **`post-rewrite`** (amend and rebase): compares the rewritten commit's findings with the
  **pre-image's**, with commit label and line numbers stripped. Identical findings are replayed
  history, not a breach, so rebasing over old commits raises nothing while a commit that adds a
  violation of its own is recorded even when the pre-image was already failing.
- **`post-commit`** (`cherry-pick` and `revert`, which fire no `post-rewrite`): tells the paths apart
  by the reflog and deliberately leaves a plain or amended `git commit` alone, so `--no-verify`
  stays the documented emergency valve rather than becoming a trap. A repository with reflogs
  disabled loses this leg.
- **`breach-guard`**: the shared refusal, called by `pre-commit` and `pre-applypatch`. The recording
  hooks only ever append; **this is the only place the record is pruned, and only by reachability**,
  so a later clean rewrite cannot wipe an older finding that is still in history, and the reset-free
  fold (playbook, FAIL-loop section) is a valid way out.
- **`self-test`**: proves the hooks **block**. Every case asserts the resulting history (a commit
  count, a sha, a recorded breach), never the text a hook printed, because a printed rejection beside
  a surviving commit is exactly the finding this suite was built from. It needs no build tool, so a
  host session can run it, and it **unsets every `GIT_*` variable first**: git exports `GIT_DIR` and
  `GIT_INDEX_FILE` to its hooks, so an unscrubbed throwaway repo commits into the repo being gated.
  Two closing cases guard the suite itself: every tracked hook is staged `100755`, and the surrounding
  repository's `HEAD` and branch are where they were.

The record lives in the **common** git dir, so it follows the commit into every worktree of the
repository; the stamp lives in the private one, being that worktree's own note about what it just
approved (an amend of a gated tree therefore pays no second format run).

## Adaptation

The marked spots are all found by `grep -rn "ADAPT:" hooks/`; three of them need a decision:

| Where | What |
|---|---|
| `content-gate` | the config block at the top (comment families, docstrings, directives, production and test trees, debug prints and their allowlist) and the host-path allowlist below it |
| `commit-msg` | the ticket-key block at the top: `ticket_key_re` (empty switches the leg off), `ticket_position`, `ticket_required` |
| `self-test` | its fixtures must use the same extensions, smells, trees and wrapper name as the two above |

`format-gate`'s note only names the repo's `./verify format`, which the stack presets supply, so
there is nothing to decide there. `pre-commit`, `pre-applypatch`, `pre-merge-commit`, `post-commit`,
`post-rewrite` and `breach-guard` need no adaptation.

**Adding a leg.** A project that ships another self-testing script (a deploy script, an ops runbook)
adds it to `pre-commit` the same way the `hooks/` and `stacks/` legs are: run that script's own
self-test whenever the commit touches its directory, so neither a weakened gate nor a weakened
script can land. Guard the leg on the staged paths, never on the presence or mode of the file it
runs, or deleting that file switches the leg off; prove the new leg in `hooks/self-test` in the same
change.

## Bypass

`--no-verify` bypasses the hooks and is for genuine emergencies only; the one sanctioned
non-emergency use is the `/pause` skill's `[wip]` parking commit (handbook, git section). It is
deliberately *not* recorded as a breach. The hooks are a git-level safety net complementing the
harness-level permission policy in `.claude/`; the full build with tests remains the gate for
landing on `main`.
