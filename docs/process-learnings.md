# Process Learnings

A running log of where our process slipped and how we prevent it next time. The **Reviewer** agent
(see [knowledge-base/multi-agent-playbook.md](knowledge-base/multi-agent-playbook.md)) appends a row
whenever a rule was missed — even on a PASS. The point is that future work and future agents drift
*toward* the process, not away from it. Recurring items should be promoted into `CLAUDE.md` or the
[engineering handbook](knowledge-base/engineering-handbook.md).

| Date | What slipped | Rule (handbook §) | Fix / prevention |
|------|--------------|-------------------|------------------|
| 2026-06-15 | D1 rename commit left a duplicated `architecture.md` violation item + stale `PageContentExtractor` KB refs; fixed post-hoc one commit later | KB-first/KB-current — name/behavior changes update the relevant KB doc *in the same change* (CLAUDE.md golden rules; handbook §1/§8) | On a rename, grep all of `docs/` for the old identifier within the same commit; when editing a numbered list, replace in place and re-read it to catch paste duplicates before committing |
