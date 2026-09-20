# Personal global instructions (all projects)

> **User scope, example.** This file lives at `~/.claude/CLAUDE.md`, applies to every project of one
> person, and is deliberately not team-facing. Adapt it to your own taste; do not commit it to a
> project repo. (See [README.md](README.md) for the scope split.)

## Answer style
- Keep answers short and precise. Lead with the outcome; expand only on request.
- Use technically correct terms, but no pompous or inflated language. Write plainly.
- Write naturally, colleague to colleague.
- Avoid em dashes and dash asides in normal prose; restructure with commas, parentheses, or separate
  sentences. Ordinary hyphens in compound words (e.g. "cross-session") are fine.

## Conversation language (example: German / Denglisch)
- Converse in German; drop in English technical terms (merge, rebase, review, branch, refinement)
  rather than forcing stilted German equivalents.
- Say "Refinement", never "Grooming".
- For git, say "auf main mergen" / "in main integrieren", never "einen Branch landen".

## Workflow safety
- Deleting files: the agent never runs `rm` itself. It hands over the exact command so the author
  runs it in their own terminal and owns the deletion.
