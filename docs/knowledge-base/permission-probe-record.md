# Permission probe record

The method for recording what the committed permission rules do on the build in use, command by
command. The KB cites rows of the template's record, linked below; a claim about a shape that no
row carries is marked derived, documented or observed where it is made
([working-with-ai-agents.md](working-with-ai-agents.md), "Which rule applies").

- A row id or a dated section the KB cites names this record as the template ships it:
  [the baseline's copy](../../.template/baseline/docs/knowledge-base/permission-probe-record.md).
- This project's copy holds the method; its dated sections come with its first probe run.

> **ADAPT:** Re-run both probes after adopting, after every change to `.claude/settings.json` and
> after every Claude Code update, and replace the tables with your own runs: the core block and one
> block per installed module ("Method"). Keep the method.

## Method

- **Rule probe.** Run headless from the project root, outside auto mode, with the project's
  settings and no user or local ones: `claude -p --permission-mode default --setting-sources
  project --output-format stream-json --verbose < <probe list>`. Accept the folder's trust prompt
  once in an interactive session first, or every project `allow` is ignored (documented).
  - `--setting-sources` names the user, project and local sources only, so a managed policy the
    machine receives still applies (derived from the CLI reference).
  - **What the template's tables do not show:**
    - The probes ran in sessions that also carried an organization's managed policy.
    - A managed deny list can refuse the same command as the project's, and such a row then shows
      nothing about the project's rule.
    - That the workflow holds with no managed policy is not shown.
    - The control that shows the project's settings loaded is therefore a row that turns with
      the project's rule alone: it runs without the rule and is refused with it.
    - A row can end in no call for a reason outside the project's rules: the model may decline a
      command under the instructions its session carries, such as an organization's (the
      template's rows TU3 and TU4), and such a row shows nothing about the rules.
    - Not probed: the `git filter-branch` denies, the `shred` deny and the bypass-mode switch
      (working-with-ai-agents.md, "Autonomy within guardrails").
  - **Who may run which probe:**
    - The rule probe and the workflow probe as this section words them are the author's
      request and run in the project, whenever the ADAPT note above, `/adopt` or
      `/template-upgrade` calls for them.
    - Any other probe that moves a guard or a hook away, adds a rule through `--settings` or
      starts a session with a wider permission mode or rule set than its own runs only on the
      author's request, in a throwaway clone.
      - Why: such a probe switches a check off, and in a clone it switches it off for nobody
        else.
  - The probe list holds a block of the core rules, run always, and one block per installed
    module, which probes that module's rules (the blocks: `/adopt`, step 6).
    - A module's rows stand in a section of their own inside that module's fence.
  - A section that adds rules through `--settings` says so in its opening bullets.
  - The trust must be the probe folder's own, in a folder below no trusted one: on Claude Code
    2.1.319 a trust accepted for a parent folder did not reach a clone below it, and a session
    started below a trusted folder showed no trust prompt and left its folder untrusted
    (observed). A run that ignores the allows says so: "Ignoring <n> permissions.allow entries …
    this workspace has not been trusted".
  - The probe list asks the model to run each line as its own tool call, exactly as written, and
    to go on after a refusal. Every target a command could change is a path, branch, ref or remote
    that does not exist, so no command of the list changes anything; the template's sync probe
    touched the real `origin` and config, as its header says.
  - Read the outcome from each tool result in the stream, never from the model's reply:
    - **REFUSED**: "Permission to use Bash with command … has been denied.", or for a file tool
      "File is in a directory that is denied by your permission settings." A `deny` rule fired.
    - **PROMPTED**: "This command requires approval", "Claude requested permissions to use Bash,
      but you haven't granted it yet." (an `ask` rule fired; Claude Code 2.1.319), or one of the
      harness's own checks (path, redirect, `find -exec`). An `ask` rule fired or no rule passed
      it; a session would prompt, or in auto mode ask the classifier.
<!-- MODULE guard -->
    - **GUARD**: a PreToolUse hook error quoting `bash-shape-guard:`: the shell-shape guard
      refused the call.
<!-- /MODULE guard -->
    - **BLOCKED**: "cd in '…' was blocked", or for a PowerShell call "<command> targeting '…' was
      blocked", the harness's own working-directory check.
    - **NO CALL**: the stream holds no tool call for the row: the session was not offered the
      tool, or the model declined the call.
    - **RAN**: the command executed, by an `allow` rule or the harness's own pass for a read-only
      command or an in-project `cd`.
  - Why headless: interactively, declining a prompt ends the turn (observed), and in auto mode a
    refusal may be the classifier's.
- **Workflow probe.** The project's normal workflow, run for real in a throwaway worktree under
  `.claude/worktrees/` with the same command and `--permission-mode auto` in place of `default`.
  Run it twice, each time in a fresh worktree, since the classifier judges every call anew. Every
  step must pass without a prompt or a refusal; a git error is the step's own and does not count.
  Remove the worktrees and the probe branches afterwards.
