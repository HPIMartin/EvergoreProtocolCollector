# Git enforcement hooks

- POSIX-sh hooks that mechanically enforce the commit rules of the
  [engineering handbook](../docs/knowledge-base/engineering-handbook.md) (git section).
- The handbook is the canonical source for the rules; this file describes the mechanics only.

## Activation (required in every checkout)

```sh
git config core.hooksPath hooks
```

- Hooks are not shared by clone, so this runs once per checkout.
- `.devcontainer/devcontainer.json` wires it into `postCreateCommand`
  (`docs/knowledge-base/dev-environment.md`, "`postCreateCommand`").
- The hooks need the minimum git version that `docs/knowledge-base/dev-environment.md` names
  ("The devcontainer (`.devcontainer/`)", **git version**).
- **Every hook script must be tracked as `100755`.**
  - Why: git skips a non-executable hook outright on POSIX, so the gate would not run at all.
  - `core.fileMode=false` (the default on a Windows host) hides a lost bit from every local check.
  - Verify with `git ls-files -s hooks/` and fix with:

    ```sh
    git update-index --chmod=+x hooks/<name>
    ```

  - `hooks/self-test` asserts the bit.
    - Why: a hook that loses its bit then fails the gate instead of silently disabling it.

## The gate

- **git asks `pre-commit` only for `git commit`.**
- Commits the sequencer builds itself (a rebase's picks, `git rebase --continue`, `cherry-pick`,
  `revert`) run no `pre-commit` hook.
  - The rule (a hook that only prints is not a gate) is in handbook §7.
- The suite splits the checks from the hooks: `content-gate` and `format-gate` hold the checks.
- The hooks that run *before* a commit exists (`pre-commit`, `pre-applypatch`, `pre-merge-commit`)
  refuse outright.
- The ones that can only run *after* git already moved the refs (`post-rewrite`, `post-commit`,
  `post-merge`) **record** the offending sha.
- `pre-commit` then refuses on the recorded shas (`breach-guard` below; the rule: handbook §7).
- A merge commit that `git merge`, `git pull`, `git commit` or a rebase wrote is recorded too.
  - Why: no merge commit belongs in the history.

## The checks

- **`content-gate`**: not a hook; the shared content check.
  - `--staged` reads the index.
  - `--commit <sha>` reads one commit.
  - Every scanned file is checked whole for private-key blocks, AWS-style access keys, Anthropic
    provider tokens, credential literals, real e-mail addresses and absolute host paths.
  - The credential scan matches `password`, `passwd`, `secret` and `api_key`.
    - It matches them regardless of case (`PASSWORD`, `apiKey`, `clientSecret`).
    - It matches them before `:` or `=`.
    - A quoted value of three characters or more counts.
    - In `*.properties`, `*.yml`, `*.yaml`, `.env` and `.env.*` an unquoted value of eight
      characters or more counts too.
    - Why: a config format needs no quotes around a value.
    - A reference to the environment (`${NAME}`, `$NAME`) is no value and passes, quoted or not.
    - A value counts only when its first character is no `$`.
    - Known limit: a literal secret that opens with `$` therefore passes.
    - The private-key and AWS patterns keep their case, in a grep of their own.
  - It also matches an Anthropic provider token (`sk-ant-<kind>-` and twenty characters or more),
    case kept; a line that names only the prefix passes.
    - The token counts at a line start or after any character but a letter, digit, `_` or `-`,
      so kebab-case text such as `task-ant-...` passes; behind a JSON escape (`\n`, `\r`, `\t`)
      or a URL escape (`%20`) it counts too.
  - Known limit: beyond that one, the scan knows assignment shapes, not token formats: another
    provider's token with no `password`, `secret` or `key` word beside it passes.
  - Committed key/keystore files are refused by name.
  - Claude Code's home is refused by name: any path under a `.devcontainer/.claude-home/` folder,
    at the root or deeper, except that folder's own `.gitignore`, and a `.credentials.json`
    anywhere, `hooks/` and `.template/` included.
    - Why: Claude Code keeps its login token and transcripts there, and only `.gitignore` keeps
      them out, which `git add -f` passes.
    - A deletion is not refused: the by-name checks read added, modified, renamed and type-changed
      paths only, so removing such a file from the repo commits.
  - A file named `.env` or `.env.<anything>` is refused by name, except `.env.example`,
    `.env.sample` and `.env.template`.
    - Why: an `.env` holds the values of one machine; its template holds placeholders.
    - The unquoted-value rule reads those templates too: a placeholder of eight characters or more
      after a credential key is refused there; leave it empty, keep it shorter, or write a
      reference (`${NAME}`).
  - `email_allow` lets the reserved example, test and invalid domains and localhost through.
  - It lets `git@` through too, the user of an SSH remote (`git@github.com:org/repo.git`): only
    an address whose local part is `git`, so `gitlab@` and `agit@` stay refused.
  - Each entry of `email_allow` is matched against `<line>:<address>`, without regard to case.
    - The local part `git` therefore passes in any case, `GIT@` too.
  - An emptied `email_allow` or `host_allow` keeps its scan on.
    - It then lets nothing through.
    - Why: `grep -v ''` drops every line, so the filter runs only for a non-empty value.
  - Production source alone is checked for hard-coded auth tokens.
  - Every line the diff adds to a file whose extension a comment family lists is checked for
    `TODO`/`FIXME`, shell scripts and YAML included.
  - A script recognised only by its shebang gets no `TODO`/`FIXME` check (probed: such a script's
    `TODO` passes, while the same line in `*.sh` or `*.yml` is refused).
  - Every line the diff adds to such a file is checked for its family's debug-output constructs
    (`debug_print_re`: `System.out`/`printStackTrace`, `console.log`, `Console.Write`,
    `printf`/`puts`/`std::cout`, `print(`/`breakpoint()`).
    - Path prefixes in `print_allow_paths` are exempt.
    - Why: stdout is the interface there.
  - A free name (`print(`, `printf(`, `puts(`, `breakpoint()`) counts only after a character that
    is no letter, digit, `_` or `.`.
    - `Blueprint(`, `fingerprint(`, `snprintf(`, `sprintf(`, `fprintf(`, `inputs(` and `fputs(`
      therefore pass, while `std::printf(` is refused.
    - A member call such as `log.printf(` or `self.print(` passes too.
  - A qualified construct (`System.out.`, `printStackTrace`, `console.log`, `Console.Write`,
    `std::cout`) counts only after a character that is no letter, digit or `_`.
    - `e.printStackTrace()` and `java.lang.System.out` are therefore refused.
  - A smell the file already carried stands until a commit edits its line.
    - Why: an unrelated edit of the file is then never refused for it.
  - The smell scan keeps the matches whose line number the diff adds to the file's path, the same
    added lines the comment scan reads.
  - `print_allow_paths` lists none by default; `CONTENT_GATE_PRINT_ALLOW` overrides it for a run.
  - **Every comment line the diff adds** to a listed source extension or a shell script recognised
    by its shebang is refused.
    - Why: it makes the no-comment rule (handbook §3) mechanical instead of a reviewing habit.
  - The scan covers the listed file families only; a project adds its further ones to the lists in
    the ADAPT block.
    - A `Dockerfile`, a `Makefile`, `*.gradle`, `*.bash`, `*.xml`, `*.sql`, `*.properties` and
      `*.go` are outside the shipped lists, so their comments pass unseen.
  - The comment scan marks the post-image's lines that carry a comment outside every literal and
    intersects them with the lines the diff adds.
    - Why: a URL or an XPath is then never read as a comment.
  - A comment the file already carried stands until a commit edits its line.
  - **No opt-out.**
  - Every category is reported together, never just the first that hits.
    - Why: a new violation cannot hide behind an older one.
  - A git read that fails refuses in both modes and names what git could not read.
    - Why: an empty read would otherwise pass as an empty change.
  - The diff it reads carries forced `a/`/`b/` prefixes.
    - Why: a repository-level `diff.noprefix` cannot quietly take a file out of the scan.
  - Its header path is taken without a trailing tab.
    - Why: a path containing a space cannot quietly take a file out of the scan.
  - A `+++ ` line counts as a file header only before the file's first hunk.
    - Why: an added line that opens with `++ ` would otherwise rename the file being read.
  - Renamed paths (`R`) are in scope.
    - Why: a file renamed and edited in one commit would otherwise be read by nothing at all.
  - Rename detection stays on.
    - Why: a move that edits nothing then contributes no added lines, and the stock the file
      already carried is not re-read as new.
  - One scanner per comment syntax, each keyed off an extension list in content-gate's ADAPT block:
    - `comment_slash_ext` (C family, JVM, JS/TS): `//` and `/* */` outside `"…"`, `'…'`, a
      backtick template and a `"""` text block.
    - In that family `#include`, `#define`, `#region` and `#pragma` are code, not comments, and
      pass.
    - Known residue of that family: a TypeScript regular-expression literal containing `//` is
      read as a comment (probed: `/[//]/` is refused).
    - `comment_hash_ext` (Python, CMake, TOML, shell, YAML): `#` outside `"…"` and `'…'`.
    - In that family a line-1 shebang is the interpreter, not prose, and passes.
    - In a shell script, by extension or by shebang, a `#` opens a comment only at the start of a
      word: after a blank, `;`, `&`, `|`, `(` or `)`.
      - Why: `$#` and `${x#y}` are code.
    - In YAML a `#` opens a comment only after a blank.
    - In a shell script a backslash escapes the next character.
    - In a shell script a single-quoted string knows no escapes, while a `$'…'` string does.
    - In YAML a single-quoted string knows no escapes.
    - In YAML `''` is a quote inside a single-quoted string.
    - In a shell script a here-document's lines are its data, up to its whole delimiter word.
    - In a shell script a here-string (`<<<`) is a word, not a here-document.
    - A file without an extension whose first line runs `sh`, `bash`, `dash` or `zsh`, directly or
      through `env`, is scanned as a shell script.
    - An ADAPT note passes in every file of the family: a full-line comment that opens with exactly
      `# > **ADAPT:**`, together with the full-line comments right after it that are `# >` alone or
      `# >` and a blank.
    - The note ends at the first line of any other form, and that line is scanned as usual.
    - A plain `# ADAPT:` line is refused, and so is a `# prose` line right after a note.
    - This note is the one pass the comment scan gives prose; the scan has no opt-out.
    - Known residue of that family: a script without an extension whose shebang names another
      interpreter is not scanned (probed: a `python3` script's comment passes).
    - Known residue of that family: each line is read as if it started outside any string (but for
      the `docstring_ext` rules below), so a string that spans lines misleads the scan on its later
      lines.
      - Probed: a `#` at the start of a word on such a line is refused as a comment.
      - Probed: a comment after the string's closing quote on such a line passes.
    - Known residue of that family: an arithmetic `<<` before a name is read as a here-document
      (probed: a comment after `$((1 << n))` passes; after `$((1 << 2))` it is refused).
    - Known residue of that family: a double quote inside a double-quoted command substitution
      ends the outer string for the scan, so a `#` after a blank in the inner argument reads as a
      comment: `n="$(grep -c "^ #" file)"` is refused.
      - The scan has no opt-out, so the line is rewritten.
      - Single quotes around the inner argument: `n="$(grep -c '^ #' file)"`.
      - Or a variable set beforehand: `pattern='^ #'`, then `n="$(grep -c "$pattern" file)"`.
    - `docstring_ext` (Python): a line whose first non-blank characters open a triple-quoted string
      (with optional `r`/`b`/`u`/`f` prefix) outside every bracket is a doc comment.
      - Why: a string standing where a statement belongs is a docstring or a no-op.
    - In that family the scanner counts `(`, `[` and `{` against `)`, `]` and `}` outside strings
      and comments.
      - A triple-quoted string that opens a line inside a parenthesis, a call or a dict is
        therefore a value and passes.
      - A docstring after `def …:` and a bare string statement outside every bracket are refused.
    - `docstring_ext` applies inside the `comment_hash_ext` family only: an extension listed there
      alone is not scanned.
    - In that family a backslash that ends a line inside a quote carries the string onto the next
      line, as in Python.
      - Why: a bracket in the continued string then does not count, so it cannot hide every later
        docstring of the file.
    - In that family `x = """…"""` is a value and passes.
    - In that family a `#` inside a triple-quoted string is not a comment.
    - `directive_allow_re`, in the `docstring_ext` family only: a comment whose entire text from its
      `#` onward is a tool directive with its usual body passes wherever it sits on the line.
      - Why: such a directive is configuration, not explanation.
      - One token after the keyword's colon: `# noqa: E501`, `# type: ignore[x]`, `# fmt: off`.
      - A comma list of rule codes after `noqa:` or `ruff: noqa:`: `# noqa: E401, F401`.
      - A comma list of Bandit IDs after `nosec`: `# nosec B404, B603`.
      - A comma list of names after `pylint: <option>=` or inside `ignore[…]`:
        `# pylint: disable=a, b`, `# type: ignore[attr-defined, union-attr]`.
      - `# pragma: no cover` and `# pragma: no branch`.
      - A bare keyword: `# noqa`, `# nosec`.
    - A directive followed by explanatory prose (`# noqa because reasons`,
      `# nosec because it is safe`) is still refused.
    - `comment_xml_ext` (MSBuild project, props and targets files): every line a `<!-- … -->`
      block covers, however many it spans.
  - `comment_exempt_dirs` names the tooling directories whose scripts keep their comments
    (handbook §3): `stacks/` and `agent/` as shipped, and this project's own `deploy/` and
    `release/`.
    - This project holds no `stacks/` folder; the entry stays because the template's suite uses
      it (author decision 2026-11-23).
    <!-- MODULE gate -->
    - With the gate module, `gate/` joins it, between `# MODULE gate` fence lines.
    <!-- /MODULE gate -->
    <!-- MODULE agent-seat -->
    - With the agent-seat module, `land/` joins it, between `# MODULE agent-seat` fence lines.
    <!-- /MODULE agent-seat -->
    - Only the comment scan skips them.
    - Every other scan reads them.
    - A project edits the list in content-gate's ADAPT block.
    - No variable widens the list for a run.
  - `hooks/` and `.template/` are outside every scan but the conflict-marker scan and the
    Claude-home refusal by name.
    - Why: neither is the project's own source.
    - `hooks/` holds the detection patterns themselves.
    - `.template/` holds the adopted template's baseline.
  - **Conflict markers** are refused in every path, `.md`, `hooks/` and `.template/` included.
    - A marker is a line the diff adds that opens with `<<<<<<<`, `|||||||` or `>>>>>>>`.
    - `post-rewrite` runs the same check, so it records a rebase commit that carries one.
    - A lone `=======` (a Markdown heading underline) does not count.
    - A marker quoted after the start of a line does not count.
  - **The diff scans** (comments, smells and conflict markers) read a plain diff:
    - no external driver, colour or textconv;
    - binary files as text;
    - type changes included;
    - renames detected, no copies;
    - non-ASCII paths unquoted.
    - Why: each of those would otherwise hide the lines a commit adds or misname their file.
  - The file list the secret, key-file, `.env` and smell scans read takes the same flags.
    - Why: a copied, type-changed or non-ASCII path is then scanned too.
  - That file list is NUL-separated (`-z`), so it names every path verbatim.
  - A path holding a newline is refused (probed).
    - Why: the list is split at newlines, which would scan the wrong file or none.
  - A gitlink, a submodule's commit, is not content of this repository and is not read.
  - Known residue: a patch header still quotes a path that holds a double quote, a backslash or a
    control character.
    - Probed: a listed source file under such a path, with a comment, passes the comment scan.
    - A listed source file under such a path, with a debug print, passes the smell scan too.
    - Probed: a shell script without an extension under such a path is refused as unreadable.
  - A file counts as **production source** when it starts with one of `production_trees`, starts
    with none of `test_trees` and matches none of `test_name_re`.
  - The rules that bind production code only (the hard-coded-token scan and `production_smell_re`,
    shipped with the C# null-forgiving operator) key off exactly that.
    - A test may therefore carry a non-secret test token where production source may not.
    - A test may therefore override the compiler's nullability answer where production source may
      not.
  - The C# null-forgiving operator counts before `.`, `;`, `)`, `,`, `[` or `]`: `x!.y`, `x!;`,
    `x!)`, `x!,` and `x![0]` are refused, while `!=` and the `!` of `"Done!"` pass.
    - Known residue: a production string holding `!` before one of those characters is refused,
      such as `"Stop!, please"`.
- **`format-gate`**: not a hook; it runs the repo's own `./verify format`, the stack's fast
  format/style check, and nothing else.
  - It is guarded on that script being present and executable.
    - Why: a checkout without a toolchain still commits.
  - Four hooks call it (`pre-commit`, `pre-applypatch`, `post-commit`, `post-rewrite`).
    - Why: the gate has **one** spot instead of four.
  - `hooks/self-test` stubs `./verify` to assert the leg behaviourally.
  - The test run and the full build stay out of it.
    - Why: the TDD micro-commit loop stays fast, and commits are already green when they are made.
  - Known limit: the format check reads the work tree: with a partly staged file it judges the
    copy on disk, not the staged one, so stage whole files or run `./verify format` after the
    commit.
- **`self-test-legs`**: not a hook; the self-test legs that `pre-commit` and `pre-applypatch` both
  run (described under `pre-commit` below).

## The hooks

- **`commit-msg`**: checks the subject line.
  - The subject is a single line with a present-tense verb first and an optional `[doc] ` tag.
  - It rejects:
    - a body;
    - `Co-Authored-By`;
    - conventional-commit prefixes;
    - any tool footer.
  - A conventional-commit prefix (`feat:`, `fix(core):`) is refused with its own reason, ahead of
    the check on the first word.
  - It refuses an empty message.
  - It refuses a `fixup!` subject, which starts with no capitalized present-tense verb; a fix is
    folded into its owner by `agent/fold` (multi-agent-playbook.md, "FAIL-loop rules").
  - It refuses a first word from a fixed list of past-tense and gerund forms (`Added`, `Adding`, …);
    a form missing from the list passes.
  - It cuts a message at its first `#` line holding `>8`, git's scissors line, before judging it.
  - After an editor session it drops every `#` line before judging, as git's default cleanup does.
  - A message no editor saw (`-m`, `-F`, `--no-edit`) is judged with its `#` lines.
    - Why: git stores them (git 2.54 and 2.55 store
      `git commit -m "Add the m class" -m "# Co-Authored-By: x"` with its second line).
    - It tells the two apart by `GIT_EDITOR=:`, which git sets for its commit hooks when it opens
      no editor (githooks(5)).
    - Known residue: the scissors cut applies here too, so what follows a scissors line in such a
      message goes unjudged.
  - Known residue: the hook sees neither `--cleanup` nor `commit.cleanup`, so a `-m` message with
    `--cleanup=strip` is judged with the `#` lines git then drops (git 2.54 and 2.55).
  - Known residue: a caller's own `GIT_EDITOR=:` cannot be told from the one git sets, so an editor
    session under it (`GIT_EDITOR=: git commit --amend`) is judged with git's template lines and
    refused (`hooks/self-test` pins it).
    - `--no-edit` or `GIT_EDITOR=true` is the way.
  - Known limit: the message rules run only where git asks `commit-msg`, for `git commit` and a
    rebase `reword`.
    - A merge asks too, and `pre-merge-commit` refuses it first.
    - A revert, a cherry-pick and a rebase's picks and squashes write their message past it (git
      2.54 and 2.55).
    - `git am` writes its message past it too: there git asks `applypatch-msg`, which `hooks/`
      does not ship.
    - Revert with `git revert --no-commit` and commit with a one-line message.
    - Fold with `fixup`, which keeps one subject.
    - `cherry-pick -x` adds a second line.
  - An empty `ticket_key_re`, the shipped default, switches the tracker-key leg off.
    - This project keeps it empty: it has no ticket system.
  - With `ticket_key_re` set, the sanctioned shape is `<verb …> (KEY)` with exactly one space
    before the group.
  - Under `ticket_position=prefix` the shape is `[KEY] <verb …>`, where an optional `[doc] ` tag
    follows the key (`[PROJ-12] [doc] Groom the backlog`).
  - What remains after stripping the key still has to satisfy every rule above.
  - A key in the other position, or a tag that is no valid key, is refused rather than waved
    through.
  - The key is optional unless `ticket_required=yes`, which refuses a subject without one.
  - `COMMIT_MSG_TICKET_KEY_RE`, `COMMIT_MSG_TICKET_POSITION` and `COMMIT_MSG_TICKET_REQUIRED`
    override the three settings for a run.
    - `hooks/self-test` exercises every mode through them, without editing the hook.
- **`pre-commit`**: the gate of a plain `git commit`.
  - It refuses while a merge is in progress (`MERGE_HEAD`), left so by `pre-merge-commit`,
    `--no-commit` or a conflict.
    - Why: the commit would conclude that merge as a merge commit.
    - `git merge --abort` is the way out.
    - No allow covers it, so it meets the auto-mode classifier, or a prompt outside auto mode
      (docs/knowledge-base/permission-probe-record.md, row 69).
  - It refuses while a rebase stands at a conflict stop, which `git rebase --continue` finishes.
    - A conflict stop is a `rebase-merge` or `rebase-apply` directory together with a
      `REBASE_HEAD`.
    - An `edit` or `reword` stop is none: it has a `rebase-merge/amend` and a last
      `rebase-merge/done` line that opens with `edit` or `reword`.
    - The amend file alone does not tell them apart.
      - Why: git writes it at a `fixup` or `squash` conflict stop too.
    - Each path is resolved by `git rev-parse --git-path`, so the guard holds in a linked worktree.
    - Known residue: a `fixup` or `squash` whose message editor failed stops with the same files
      as its conflict stop.
      - An amend there is refused alike.
      - Probed: `git rebase --continue` with a working editor finishes such a stop.
  - It refuses while `HEAD` reaches a recorded commit (`breach-guard`).
  - It runs `content-gate --staged`.
  - It runs `hooks/self-test` whenever the commit touches `hooks/`.
    - Why: a weakened gate cannot land by a commit.
  - It runs `hooks/self-test` too whenever the commit touches a preset's `stacks/*/verify`.
    - Why: a hooks case stages each preset's `verify` at the root, as the adoption commit does.
  - It runs `stacks/self-test` whenever the commit touches `stacks/`.
    - Why: a weakened preset cannot land by a commit.
  - It runs `agent/self-test` whenever the commit touches `agent/`.
    - Why: a weakened agent tool cannot land by a commit.
  - It runs `.claude/skills/adopt/self-test` whenever the commit touches `.claude/skills/adopt/`.
    - Why: a weakened module filter cannot land by a commit.
    - A file moved out of `.claude/skills/adopt/` counts as touching it.
  - While the index holds anything under `.claude/skills/adopt/`, it runs that self-test too
    whenever the commit touches `.claude/settings.json` or a file under
    `env/devcontainer/.devcontainer/`.
    - Why: the manifest's rows name lines of these files, so an edit there can break a row.
    - An adopted project holds no such folder, so these files commit there without the self-test
      and without a skip line.
  - A fence that goes wrong in any other file is caught at the next run of the module filter
    self-test, at the latest by `assemble.sh`.
  <!-- MODULE guard -->
  - With the guard module the self-test needs `python3` (dev-environment.md, "Python for the
    guard"); a host without one commits in the container, which the leg's `cannot run here` and
    failure refusals both name.
  <!-- /MODULE guard -->
  <!-- MODULE agent-seat -->
  - With the agent-seat module the same refusals name `jq` and `openssl` too, which its cases need.
  <!-- /MODULE agent-seat -->
  <!-- MODULE gate -->
  - It runs `gate/self-test` whenever the commit touches `gate/`.
    - Why: a weakened dossier check cannot land by a commit.
  <!-- /MODULE gate -->
  <!-- MODULE agent-seat -->
  - It runs `land/self-test` whenever the commit touches `land/`,
    `ci/github/.github/workflows/land.yml` or `.github/workflows/land.yml`.
    - Why: neither a weakened landing nor the workflow that runs it can land by a commit.
    - The suite needs `jq`; a host without it commits in the container, which the leg's
      `cannot run here` and failure refusals both name.
  <!-- /MODULE agent-seat -->
  <!-- MODULE agent-seat -->
  - It runs `env/devcontainer/.devcontainer/agent/self-test` whenever the commit touches
    `env/devcontainer/.devcontainer/agent/`, and `.devcontainer/agent/self-test` whenever it
    touches `.devcontainer/agent/`.
    - Why: a weakened agent image build cannot land by a commit, in the template's layout or in an
      adopted project's.
    - Each path is a leg of its own: a commit that touches one folder runs only that folder's
      image suite; a commit under `env/devcontainer/.devcontainer/agent/` also runs the module
      filter's leg (next bullet).
    - While the index holds anything under `.claude/skills/adopt/`, a commit under
      `env/devcontainer/.devcontainer/agent/` runs the module filter self-test too.
  <!-- /MODULE agent-seat -->
  - It runs this project's own legs, `deploy/self-test` whenever the commit touches `deploy/` and
    `release/self-test` whenever it touches `release/`
    ([build-run-deploy.md](../docs/knowledge-base/build-run-deploy.md), "Git hooks (local
    enforcement)").
    - Why: neither a weakened deploy check nor a weakened release comparison can land by a commit.
  - Those are its self-test legs. They live in `self-test-legs`, which `pre-commit` and
    `pre-applypatch` both run, passing their own name, which opens every line it prints.
    - Why: `git am` cannot land a weakened suite either.
  - Every leg works alike (`run_leg`):
    - A path counts as touched when the commit adds, changes, deletes, renames or type-changes it
      (a file turned into a link).
    - The leg runs the self-test of the staged tree, never the work tree's copy.
      - Why: a weakened suite or tool staged beside a good copy on disk would otherwise land.
    - The legs share one copy of the staged tree, made from `git write-tree` the first time a leg
      reaches its suite.
    - The copy is a repository of its own in a scratch directory that borrows this repository's
      object store, so its files and its `git ls-files -s` carry the staged modes.
    - It takes this repository's object format (`git rev-parse --show-object-format`), so a
      sha256 repository runs its legs too.
    - It sets `core.autocrlf=false`, so a global setting cannot write its scripts with CRLF.
    - The staged tree's attributes and filters still apply to its checkout.
    - After the checkout it runs `git update-index -q --refresh`, so its index matches its files.
      - Why: a suite that asks git whether its tree is clean gets a true answer.
    - Its git commands, and every suite run there, go without the `GIT_*` variables git exports to
      its hooks.
    - The copy goes at the exit of `self-test-legs`, after the legs, a refusal, `HUP`, `INT` or
      `TERM`.
    - Its cost is one checkout of the whole staged tree, paid by a commit that runs a leg.
    - The leg refuses when the index keeps files of its folder but not the folder's `self-test`,
      whatever the work tree holds.
      - Why: deleting the proof, or only untracking it, is not a way out.
    - It refuses a staged `self-test` that is no regular file, such as a link.
    - It refuses a link staged at the folder's own path (mode `120000`), and a change at that path
      runs the leg like a change inside the folder.
      - Why: the index then holds nothing under the folder, so the skip below would let the link
        bring a weakened copy of the folder's tools back at the same path.
    - It refuses alike a link staged at a folder above it, such as `.claude/skills` above
      `.claude/skills/adopt`.
      - Why: the folder's files then leave the index, so the leg runs.
    - A commit that leaves nothing of the folder in the index skips the leg and prints one line.
      - Why: a project that drops a module (`git rm -r gate`) leaves no tool to prove.
      - Known residue: a submodule (a gitlink) staged at the folder's path or at a folder above it
        takes this skip too, since no scan reads a submodule's content.
    - Before the suite runs, the leg runs `sh -n` on it: a suite that does not parse fails, and
      the refusal names the syntax error.
    - A suite that exits 2, with a last non-blank line that opens with a lower-case name and
      ` self-test: `, cannot run here, as on an old git or without `python3`.
      - `hooks/self-test`, `agent/self-test`, `gate/self-test` and
        `.claude/skills/adopt/self-test` open that line with `hook self-test: `,
        `agent self-test: `, `gate self-test: ` and `module filter self-test: `.
      <!-- MODULE agent-seat -->
      - `land/self-test` opens it with `land self-test: `, as where `jq` is missing.
      - The agent image's `self-test` opens it with `agent image self-test: `.
      <!-- /MODULE agent-seat -->
      - `stacks/self-test` and `release/self-test` have no exit 2 of their own.
      - `deploy/self-test` exits with its failure count and closes with
        `deploy self-test: <n> case(s) FAILED`.
        - So exactly two failures read as `cannot run here` to its leg until the script is
          corrected (the deploy self-test's exit, F30).
      - The leg then refuses with `<folder>/self-test cannot run here:` and that line.
      - Why: a shell's own exit 2 (an unset variable under `set -u`, a redirection that fails)
        prints the script's path first, so a broken suite reads as a failure.
    - Any other non-zero exit is a failure: the leg prints the suite's `FAIL` lines, at most 20
      and then a count of the rest, each behind the calling hook's name, and refuses.
  - It runs `format-gate`.
  - It stamps the approved tree in `.git/gate-stamp`.
  - Whenever a commit touches `CLAUDE.md` or `docs/knowledge-base/agent-entry-template.md`, it
    refuses while the two files' SHARED sections differ.
    - Why: a wrapper cannot drift from the template it records.
  - That comparison reads each file from the staged blob, falling back to `HEAD` for the file the
    commit leaves alone.
  - The same leg reads `**Template version: N.**` from the template and refuses a wrapper that does
    not record `version: N`.
    - Why: a bump lands in the template and in every wrapper together.
  - The wrapper's `version: N` counts only above its `## SHARED section` heading.
- **`pre-applypatch`** / **`pre-merge-commit`**: the gates for `git am` and for a merge commit, both
  of which git builds without asking any commit hook.
  - They run *before* their commit exists, so they refuse outright.
  - `pre-applypatch` refuses when the applied index fails a gate.
  - It runs the self-test legs of `pre-commit` (`self-test-legs`) on the applied index, so a patch
    that weakens a suite leaves no commit behind.
  - `pre-merge-commit` refuses every merge commit.
    - Why: work lands by rebase and fast-forward, which writes no commit (handbook §7).
  - `main` advances only by fast-forwards (the author's landing, the sync `sh hooks/sync-main`),
    which create no commit to gate.
- **`post-merge`** (`git merge` and `git pull`, `--no-verify` included): records a merge commit the
  merge wrote itself.
  - That happens only when `--no-verify` skipped `pre-merge-commit`.
  - A fast-forward or a squash writes no commit and records nothing, even onto a merge already in
    the base.
  - After every merge but a squash, a fast-forward included, it asks
    `breach-guard --reaches HEAD`.
  - When the merged `HEAD` reaches a recorded commit, it prints a warning with that answer: the ids
    and the way out.
    - Why: git already moved the ref, so the merge cannot be refused, and the next commit there
      is.
- **`post-rewrite`** (amend and rebase): compares the rewritten commit's findings with the
  **pre-image's**, with commit label and line numbers stripped.
  - Identical findings are replayed history, not a breach.
    - Why: rebasing over old commits then raises nothing.
  - A pre-image that is a live entry of the record is no replayed history.
    - Why: the rewrite takes the recorded id out of the history, so a rebase onto a moved `main`
      would otherwise end the block and `breach-guard --reaches` would answer 0 at the landing.
    - Replayed with its change unchanged, it is recorded under its new id whatever the content
      gate says of it now.
      - Why: a commit recorded for its format, or for what a later commit mended, passes that
        gate.
      - Its change is its patch without context lines, index lines and hunk positions.
    - Rewritten with another change, it is recorded where the content gate still refuses it.
    - A released entry is not live, so its replay raises nothing.
    - The hook asks `breach-guard --reaches` about the pre-image.
    - It reads the recorded ids on the guard's line, wherever that line stands in what the
      guard printed.
    - A guard that cannot answer counts as a live entry.
    - So does an answer of 1 without that line.
  - A commit that adds a violation of its own is recorded even when the pre-image was already
    failing.
  - Each line git hands in is compared alone, so a `fixup` or `squash` that folds a clean commit
    into an already failing one is recorded.
    - Why: git hands in one line per folded commit, and the clean one's findings differ from the
      result's.
    - Unless the result's tree is the one the worktree's gate stamp holds, which `pre-commit`
      approved: the content gate then does not read it.
  - It keeps the ids it finds in a variable and appends them to the record in one write at its end.
    - Why: a scratch file in the common dir would be emptied by a rewrite in another worktree at
      the same time, and the commit it held would never be recorded.
- **`post-commit`**: covers `cherry-pick` and `revert`, which fire no `post-rewrite`.
  - It tells the paths apart by the reflog.
  - It deliberately leaves a plain or amended `git commit` alone.
    - Why: `--no-verify` stays the documented emergency valve rather than becoming a trap.
  - A repository with reflogs disabled loses this leg.
  - An exported `GIT_REFLOG_ACTION` renames the reflog subject, so a cherry-pick or revert run
    under it is neither checked nor recorded.
  - It records every commit with two parents, whatever wrote it:
    - a plain commit;
    - an amended one;
    - one made with `--no-verify`;
    - one a rebase todo's `merge` line wrote.
    - Why: a merge commit never belongs in the history.
  - A rebase that wrote one stops at its next commit.
  - `git rebase --abort` is the way out.
<!-- MODULE agent-seat -->
- **`pre-push`**: the last gate before commits leave the checkout, for every pusher, the author and
  an agent alike; it refuses the whole push on any finding and reports every finding first.
  - It refuses a remote ref other than `refs/heads/<branch_prefix>*`, `refs/heads/<mainline>` and
    `refs/tags/*`.
    - `branch_prefix` (`claude/`) and `mainline` (`main`) are its ADAPT lines.
    - With an empty `branch_prefix` no agent branch passes.
  - It refuses deleting `refs/heads/<mainline>`.
    - Why: a mainline that must go is removed on the remote itself.
  - It refuses, in the pushed range, a commit whose subject starts with `[wip]`, in any case,
    after any leading blanks.
  - It refuses, in the pushed range, a commit `content-gate --commit` refuses.
  - It refuses, in the pushed range, a merge commit.
    - Why: a merge commit never belongs in the history, and one written past the hooks is
      recorded in this clone alone.
  - It refuses a pushed tip for which `breach-guard --reaches` answers 1.
    - It prints the guard's lines: the recorded ids and the way out.
    - Why: a commit recorded for a format failure passes `content-gate`.
    - A record the guard cannot read refuses the push.
  - The pushed range of a ref is every commit its local oid reaches that no remote-tracking ref of
    the remote reaches, nor the remote oid where the repository holds that commit.
    - For a push to a URL that is no configured remote, every remote-tracking ref of any remote
      bounds it.
    - A commit an earlier ref of the same push reached is not read twice.
  - A deletion, and a tag that points at no commit, carry no range: the ref rules alone judge them.
  - A git read that fails refuses the push, never an empty range that passes.
  - It keeps the environment git hands it, which names the pushing repository `content-gate`
    reads, and adds `GIT_NO_REPLACE_OBJECTS=1` for itself and `content-gate`.
    - Why: the push sends the real objects, so a replace ref must not show either of them another
      commit in their place.
  - Named limits:
    - the range trusts the remote-tracking refs: a commit one still holds after the remote lost
      it, or, for a push to a URL, one another remote holds, is left out;
    - a first push to an empty remote, or to one with no remote-tracking refs, reads the whole
      history the tip reaches, one `content-gate` run per commit: slow on a long history, and it
      refuses an old commit that fails today's gate;
    - such a first push refuses an old merge commit too;
    - a push from a bare repository is refused wherever it carries a commit, since `content-gate`
      and `breach-guard` need a work tree;
    - the breach record is this clone's own ("State locations" below), so a commit recorded in
      another clone passes here;
    - `--no-verify` skips it, as it skips every hook;
    - two failure paths have no case: a hook folder that cannot be entered, and ref lines that
      cannot be read from standard input.
  - `hooks/self-test` pins the other failure paths by calling the hook directly: the ref lines on
    its standard input, and for a failing git read a stub `git` first on `PATH`.
    - Why: git puts its exec path ahead of `PATH` for a hook it runs, so under `git push` a stub
      never runs in git's place.
  - The GitHub rulesets are the server half: docs/knowledge-base/git-state.md, "What GitHub
    enforces".
<!-- /MODULE agent-seat -->
- **`breach-guard`**: the shared refusal, called by `pre-commit` and `pre-applypatch`.
  - It refuses while `HEAD` reaches a live recorded commit (the rule: handbook §7; the record:
    "State locations" below).
  - It never writes the record.
  - A recorded commit that no `HEAD` reaches refuses nothing and stays in the record.
  - An unborn `HEAD` reaches nothing, and neither does a recorded id whose object is gone.
  - It asks git once per live recorded id, so a record that grows over a project's life makes
    every commit a little slower (the record is never pruned: handbook §7).
  - A record that exists but cannot be read refuses every commit, naming the file.
  - A `gate-record` that exists but cannot be written refuses every commit, naming the file.
    - Why: the hooks could not record a later breach.
  - `--reaches` answers all the same where the record cannot be written.
  - A record line that is neither an entry nor a release refuses every commit, naming the file and
    the line number, until the author repairs that line (format in "State locations" below).
    - Why: such a line may be an entry a writer cut short, as an interrupted write or a full disk
      leaves it; skipping it would let the commit it names through.
  - Where git names no object format, or one other than `sha1` or `sha256`, a record that exists
    refuses every commit.
    - Why: the id length of an entry is known for those two formats alone.
  - The refusal names the recorded commits.
  - It then prints handbook §7's rules for them: what is refused, the way out, and the author's
    release.
    - The way out names `GIT_EDITOR=true git rebase --continue`.
      - Why: at an `edit` stop with staged changes a plain continue opens the editor for the
        message, which a session without one cannot answer.
  - **`--reaches <rev>`** asks the same about `<rev>` and writes nothing; the landing step
    (handbook §7) and the gate ask it about a strand tip.
    - Exit 0: `<rev>` reaches no live recorded commit, also where nothing is recorded.
    - Exit 1: it reaches at least one.
      - The first stderr line is `[breach-guard] <rev> reaches recorded commit(s): <id> <id> ...`.
      - That line holds the rev as given and every full id, one blank apart.
      - The way out follows on the later lines.
      - Stdout stays empty.
    - Exit 2: it cannot answer.
      - An argument shape other than none or `--reaches <rev>`, with the usage line.
      - A rev that is no commit.
      - A record it cannot read.
      - A record line that is neither an entry nor a release.
      - An object format it does not know, where a record exists.
      - A current directory outside a work tree.
    - It reads the repository of the current directory, from any worktree and any subdirectory,
      never the one the script sits in.
      - Why: the gate runs its own copy of the script in the strand's worktree.
- **`breach-release <commit>`**: the author's way out for a recorded commit that already sits on
  the protected branch (`protected_branch`: `main`), where no rewrite may remove it.
  - It refuses, with the reason on stderr and the record untouched:
    - where `CLAUDECODE` is set and not empty, as Claude Code sets it for the commands it runs;
    - without exactly one argument, or for an argument that is no commit;
    - for a commit that is no live entry of the record: unrecorded, or already released;
    - for a commit `refs/heads/<protected_branch>` does not reach;
      - Why: a recorded commit only a strand reaches leaves by a rewrite of that strand.
    - where `user.name` is empty or holds blanks alone;
      - Why: the release line names who released the commit.
    - where `user.name` holds a newline or a CR;
      - Why: the release must stay one line of the record.
    - where `breach-guard --reaches` exits 2 (its list above);
    - where the record cannot be read;
    - where the current directory is outside a work tree.
  - An entry only `gate-breach` holds counts as recorded ("State locations" below).
  - Otherwise it appends `released <id> <date> <user.name>` to `gate-record`, the date in UTC, and
    prints what it released.
  - The fix then follows as an ordinary commit.
  - `breach-guard` ignores a released id, also when a recorder appends that id again later.
  - It takes whether an entry is live from `breach-guard --reaches`, so one script reads the
    release lines.
  - The `CLAUDECODE` check is a courtesy check, not a boundary: an agent that clears the variable
    gets past it.
    - The release is the author's act (handbook §7); the check only stops an agent that runs it by
      mistake.
  - The permission layer refuses an agent's call (the deny rules: working-with-ai-agents.md,
    "Autonomy within guardrails").
    - No rule covers the record file itself: a write to `gate-record` by the file tools or a shell
      command meets the classifier, or a prompt outside auto mode.
    - Any appended `released <id> <date> <name>` line releases.
      - `breach-guard` reads the line, not who wrote it.
      - It asks no protected branch.
- **`sync-main`**: the sync of the primary checkout's `main` with `origin/main` (the rule and its
  fixed points: handbook §7, "`main` follows `origin/main`"), run as `sh hooks/sync-main` from the
  primary checkout; it is no git hook, and nothing calls it but a session.
  - It fetches `origin`, runs every check of the sync on the refs that fetch brought, and
    fast-forwards `main` to exactly the `origin/main` it checked, named by its object id.
    <!-- MODULE agent-seat -->
    - Where `AGENT_SEAT` reads `unattended` the fetch is `git fetch --prune origin`.
    <!-- /MODULE agent-seat -->
  - Exit 0: `main` is in step, fast-forwarded, already at `origin/main` or, in the local landing
    model, ahead of it; one line on stdout says which, with the ids.
  - Exit 1: a stop of the sync, nothing resolved; the reason, then `main` and `origin/main` by
    their ids on the last stderr line.
    - Each stop handbook §7 names under "Nothing is resolved by hand".
    - Beside them: `origin/main` is missing after the fetch, or that fetch did not bring origin's
      `main` at the id `origin/main` holds (its `FETCH_HEAD`); `main` is not at the commit
      checked, or `HEAD` left `main`, after the fast-forward.
  - Exit 2: it does not sync, and fetches nothing where the reason lies before the fetch.
    - An argument; a landing model that is neither `local` nor `hub`; a git older than 2.31, the
      minimum the hooks need, or one whose version it cannot read as one pair of numbers.
    - No work tree; a linked worktree; a non-empty `info/grafts`; `HEAD` not on
      `refs/heads/main`; `main` without a commit.
    - A change, a staged change or an untracked file, under any `status.showUntrackedFiles`.
    - No remote `origin`; a remote group named `origin`; a read of git that fails.
    - A fetch refspec of `origin` that maps anything but a branch onto its own name under
      `refs/remotes/origin/`, none that brings `main`, or a negative one that leaves `main` out
      (why: handbook §7, "`main` follows `origin/main`").
  - Its writes are the fetch's (remote-tracking refs, the tags it follows, `FETCH_HEAD`) and the
    fast-forward; it takes no argument.
    - Why: it is the one write to `main` an agent makes outside a landing, so it must not widen
      what an agent can do to `main`.
  - It drops git's own repository variables (`git rev-parse --local-env-vars`: `GIT_DIR`,
    `GIT_GRAFT_FILE`, `GIT_CONFIG_PARAMETERS` and the rest) and `GIT_NAMESPACE`, which changes
    the refs a file-path `origin` serves, and sets `GIT_NO_REPLACE_OBJECTS`.
    - So it reads the repository of the current directory, from any subdirectory, its history as
      committed, and its configuration as the files hold it.
  - Named limits:
    - "Clean" is what `git status` reports: a file under `assume-unchanged` or `skip-worktree`
      passes, and where the fast-forward touches it git refuses, a stop.
    - The fast-forward overwrites an ignored file at a path `origin` has started to track, as git
      does.
    - It trusts the configured `origin`: a `url.<base>.insteadOf` that rewrites its url sends the
      fetch elsewhere, as an edited `remote.origin.url` would.
    - A fetch whose credentials come in `GIT_CONFIG_*` variables (an `http.extraheader`) fails
      without them, a stop.
    - The environment beyond git's repository variables and `GIT_NAMESPACE` is trusted, as `PATH`
      is: a `GIT_EXEC_PATH`, `GIT_SSH_COMMAND` or `GIT_PROXY_COMMAND` can serve the fetch from
      another repository.
  - It names `main` and `origin` itself, as every process text does.
  - The `self-test` cases run copies of it whose `landing_model` line each case names, so a
    project's own value leaves them as they are.
    - Where the file system folds case (`core.ignorecase`) or git writes no symlinks
      (`core.symlinks`), the cases that need either print a skip, and the check stands untested
      on that host.
- **`self-test`**: proves the hooks **block**.
  - A case that commits takes its verdict from the resulting history (a commit count, a sha, a
    recorded breach).
    - Why: a printed rejection beside a surviving commit lets the commit through.
  - A case that calls a gate or a hook directly takes its verdict from the exit status and the
    output, or from the record the hook writes.
  - A case may also check the reason: `refuses` takes the text the refusing leg prints.
  - Each refusing leg has one case that checks its reason.
  - The cases of the `hooks/` leg run in a repository laid out like this one: copies of the hooks
    under `hooks/`, wired by `core.hooksPath=hooks`, beside a stub suite.
    - Why: the suite that leg runs is then the stub, never this suite inside itself.
  - A case that makes one git call of a leg fail commits with a stub git as `GIT_EXEC_PATH`.
    - Why: git puts its exec path, which holds a git of its own, first on the `PATH` it hands its
      hooks, so a stub that is only first on `PATH` never runs there.
  - It needs no build tool, so a host session can run it.
  - It exits with 2 on a git older than the minimum, before its first case.
  - A FAIL line names its case in brackets, and what the hooks last printed in that case follows
    it, each line behind a bar.
    - The calling leg shows the FAIL lines alone; a direct run shows the reason beside them.
  - It exits with 1 when a case fails, whatever the count.
    - Why: the `hooks/` leg reads exit 2 with a last line that opens `hook self-test: ` as a suite
      that cannot run here, and the failure summary opens so too.
  - Its one mode, `--version-only`, stops it after that check.
  - It takes that mode as an argument and reads no variable that skips a case.
    - Why: the `hooks/` leg and `assemble.sh` pass no argument, so no environment they inherit can
      cut the run short.
  - It **unsets every `GIT_*` variable first**.
    - Why: git exports `GIT_DIR` and `GIT_INDEX_FILE` to its hooks, so an unscrubbed throwaway repo
      commits into the repo being gated.
  - Its cases read no git configuration of the user: `HOME` and `XDG_CONFIG_HOME` point at an
    empty scratch folder.
    - Why: a `merge.ff=only` there aborts a merge before any hook runs, and a global ignore lets a
      refusal pass with nothing staged.
  - They read none of the system's either, `GIT_CONFIG_NOSYSTEM=1` being set, but in the copies
    the `hooks/` leg cases build.
    - Why there: `self-test-legs` unsets every `GIT_*` variable before it runs a suite.
  - It reads the surrounding repository with the caller's own configuration, at its start and in
    its two closing checks, where `HOME` may be unset.
    - Why: a checkout another user owns opens to git only through a `safe.directory` of that
      configuration.
  - It unsets the hooks' own seam variables: `CONTENT_GATE_PRINT_ALLOW` and the three
    `COMMIT_MSG_TICKET_*`.
  - It runs under `LC_ALL=C`.
    - Why: the checks for a leaked shell message read its English wording.
  - A failing control case, a clean commit, ends the run.
    - Why: every refusal would then pass for a reason of the host, such as its identity or a
      signing setup.
  - A setup step that fails marks its case FAIL and the run goes on.
    - Why: past the control case, the summary and both closing cases always report.
  - Its scripted rebases rewrite the todo through a second file, never by `sed -i`, a GNU
    extension.
  - One closing case asserts that every tracked hook is staged `100755`.
  - That case prints a skip instead while nothing under `hooks/` is tracked, as before `git init`
    on adoption path A.
  - The other closing case asserts that the surrounding repository's `HEAD` and branch are where
    they were.
  - Run by the `pre-commit` leg, the suite sits in the copy of the staged tree.
    - The mode case then reads the staged modes, and the other closing case the copy's unborn
      `HEAD`, which a case that committed there would move.
  - The stack-preset case runs only where `stacks/` sits beside `hooks/`, as in the template.
    - An adopted project, which has removed `stacks/`, gets a skip.
- **State locations:**
  - The breach record, `gate-record`, lives in the **common** git dir.
    - Why: it then follows the commit into every worktree of the repository.
  - The record holds for one clone: no push, fetch or clone carries it.
    - A fresh clone or a CI checkout answers `breach-guard --reaches` with 0 for a commit another
      clone recorded.
  - It is append-only: every writer appends, and nothing deletes, truncates or rewrites it.
  - Every writer here (`post-commit`, `post-rewrite`, `post-merge`, `breach-release`) puts a
    newline before its line as well as after it.
    - Why: a line a writer cut short, or one the author saved without its final newline, then
      stays alone on its line and refuses; joined to the next line, a 24-digit stub and a 40-digit
      entry would pass as one 64-digit id, and an entry after a release line would become part of
      the release's name.
  - Every writer checks its append.
    - Where the append fails, as on a record the committer cannot write, it prints that the commit
      (for `breach-release`, the release) is NOT recorded, naming the record.
  - `breach-guard` and `breach-release` read a `gate-breach` that stands as part of the record.
  - No hook here writes or deletes `gate-breach`.
    - Why: hooks without `gate-record`, in a worktree that checks them out, write and delete
      `gate-breach` alone, so they cannot clear the record.
  - Each worktree runs the hooks of its own checkout (what follows for the open worktrees:
    handbook §7).
  - Its format is private to `hooks/`; every other script asks `breach-guard --reaches` instead.
    - An entry is one full commit id alone on a line, at the id length of the repository's object
      format (`git rev-parse --show-object-format`): 40 hex digits under sha1, 64 under sha256.
    - A hex line of another length is no entry.
    - A release is `released <id> <YYYY-MM-DD> <name>`; the name may hold blanks, and a line
      without one is no release.
    - An entry is live while no release line names its id, wherever that line stands, in either
      file.
    - A line is read without its trailing blanks and CR, and an empty line is skipped.
    - Any other line refuses every commit until the author repairs it (`breach-guard` above).
    - A duplicate entry is harmless.
  - The stamp lives in the private git dir, being that worktree's own note about what it just
    approved.
    - Why: an amend of a gated tree then pays no second format run.

## Adaptation

The marked spots are all found by `grep -rn "ADAPT:" hooks/`; five of them need a decision:

| Where | What |
|---|---|
| `content-gate` | the config block at the top (comment families, docstrings, directives, comment-exempt tooling directories, production and test trees, debug prints and their allowlist), `production_smell_re` for what production source alone may not carry, and the e-mail and host-path allowlists below it |
| `commit-msg` | the ticket-key block at the top: `ticket_key_re` (empty switches the leg off), `ticket_position`, `ticket_required` |
| `self-test` | its fixtures must use the same extensions, smells, trees and wrapper name as the two above; its release fixtures check out the branch `main`, so a project that changes `protected_branch` changes them along |
| `breach-release` | `protected_branch`, the branch whose recorded commits the author releases instead of rewriting them away (`main` by default) |
| `sync-main` | `landing_model`, `local` or `hub` as "Project choices" in `CLAUDE.md` names the landing model; this project's value is `hub` |

- `format-gate`'s note only names the repo's `./verify format`, which the stack presets supply.
  - Why: there is nothing to decide there.
- `pre-commit`, `pre-applypatch`, `self-test-legs`, `pre-merge-commit`, `post-commit`,
  `post-merge`, `post-rewrite` and `breach-guard` need no adaptation.
<!-- MODULE agent-seat -->
- `pre-push` holds the agent branch prefix and the mainline in its `branch_prefix` and `mainline`
  ADAPT lines.
  - The `pre-push` cases of `self-test` push to `main`, so a project that changes `mainline`
    changes them along.
<!-- /MODULE agent-seat -->

**Adding a leg:**

- A project shipping another self-testing script (a deploy script, an ops runbook) adds a leg for
  it: one `run_leg` call in `self-test-legs` naming its directory, like the `stacks/` leg's.
  - This project's further legs: [build-run-deploy.md](../docs/knowledge-base/build-run-deploy.md),
    "Git hooks (local enforcement)".
- The leg runs that directory's own self-test, as staged, whenever the commit touches the
  directory.
  - Why: neither a weakened gate nor a weakened script can land.
- `run_leg` guards the leg on the staged paths, never on the presence or mode of the file it runs.
  - Why: deleting that file would otherwise switch the leg off.
- The self-test exits 0 when every case passes and 2 when it cannot run here, its last line opening
  with its own name and ` self-test: ` and saying why:
  `runbook self-test: no kubectl on PATH, so no case ran`.
- Any other exit, or an exit 2 without that line, is a failure, whose `FAIL` lines the leg prints;
  a suite that does not parse fails too.
- Prove the new leg in `hooks/self-test` in the same change.
- Add its directory to `comment_exempt_dirs` in the same change.
  - A directory whose scripts carry no comments joins no exempt list (`.claude/skills/adopt/`).
- A rename or a type change inside a leg's directory runs that leg, as an added, changed or deleted
  file does.
  - `hooks/self-test` proves the rename for the `agent/`, `stacks/`, `gate/` and
    `.claude/skills/adopt/` legs and the type change for the `agent/` leg; every leg runs through
    the same `run_leg`.
  <!-- MODULE agent-seat -->
  - It proves both for the `land/` leg too, and for the agent image leg in the template's layout.
  <!-- /MODULE agent-seat -->

## Bypass

- `--no-verify` bypasses the hooks.
- When it may be used is handbook §7's rule.
- It skips the conflict-stop guard too, like every other `pre-commit` check.
- It is deliberately *not* recorded as a breach, except for a merge commit.
  - A merge commit is recorded whichever of `git merge`, `git pull`, `git commit` or a rebase wrote
    it.
  - An amend made with `--no-verify` that adds a finding is recorded by `post-rewrite`, like any
    rewrite the hooks did not see; only a plain commit with `--no-verify` is not.
- Known limit: a rewrite runs no self-test leg.
  - `post-rewrite` and `post-commit` run the content and format checks only.
  - What stands behind it: handbook §7.
- Routes that stay outside every hook:
  - `git -c core.hooksPath=<other> …`, which runs none;
    <!-- MODULE guard -->
    - with the guard module the guard refuses it in an agent's command, and its `--config-env`
      form;
    <!-- /MODULE guard -->
  - plumbing that builds a commit no hook sees: `git commit-tree` with `git update-ref`;
  - `git fast-import`.
- The hooks are a git-level safety net complementing the harness-level permission policy in
  `.claude/`.
- The full build with tests remains the gate for landing on `main`.
