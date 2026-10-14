# Roadmap: Evergore Protocol Collector

> Milestone order and per-milestone acceptance for the open work. Items, priorities, effort and
> per-item acceptance stay canonical in [backlog.md](backlog.md) (IDs here are pointers to
> still-live items); decisions and their why in [open-questions.md](open-questions.md). This file
> adds only the cut into milestones, their order, and the milestone-level "done". A completed
> milestone is removed, same rule as backlog rows (git is history). Every milestone lands through
> the agent pipeline and the review gateway
> ([multi-agent-playbook.md](knowledge-base/multi-agent-playbook.md), handbook §7); landings stay
> author-serialized.
>
> **Ordering rule** (each decision and its why in [open-questions.md](open-questions.md)): the next
> release and the work it carries go first; then what changes what the numbers are, then the
> bottlenecks, then product build-out and operations; the skills milestone follows 1.0. The
> explanation page changes in the same commit as any rule it explains, so a rule that lands after
> it extends the page instead of waiting for it. A parked set stays explicitly out of this stage.

## Order

| # | Milestone | Items | Why this position |
|---|-----------|-------|-------------------|
| M16 | 0.4.0 reaches the guild | the explanation page with the corrected note and the invented trader (E19, E36, E20), the backlog harvest (G83), the release comparison against 0.3.0 (F22), the gate guards, the release (F23) | **First** (author decision 2026-10-13): the page goes live with 0.4.0, and nothing in it moves a figure |
| M8 | The numbers hold up | the remaining unvalued names and the ammunition credit, round-trip detection, the rounding, the opening balance (E18); the consent text in the ledger (F8) | The header named the figures, which is what makes what they omit a defect; the opening balance updates the explanation page in the same commit as its rule |
| M9 | Clear the last bottleneck | D24 with F12, F10; the reads off the event loop (F11); the slow-read warning (F19) | The transactional ingest rests on the unified repositories, which every later read method needed first |
| M10 | What the bottlenecks release | B36→D25 with D9→D18, D14 | Measured request-path cost and the timezone fix at its root |
| M11 | Product build-out | E12→E13, E3, E27, E30, E37, F13 | E12 inherits D18's query shape, so it follows it |
| M12 | Ops, security & environment | F1, H11, C1, C8, A4, C11→E28, E17, H2, H9 | F1 before the author's history rewrite; H9 last, on the nets built above |
| M14 | The guild's skills | E31→E32→E33, E34 | **After 1.0** (author decision 2026-09-30; where 1.0 is cut is open, D-16). The ranking is read before anything is built on it; the progress mark waits on about six months of readings |

## M16: 0.4.0 reaches the guild

Slice: a member can follow how his own row comes about, on a page the release carries, through a
release gate that expects no figure to move.

- [ ] A page reachable from the overview explains every header figure, every credit tier and every
      option a member can switch, with worked examples kept current as the author chose; it
      carries the corrected note of a row not yet computed and an invented trader (backlog E19,
      E36, E20).
- [ ] The backlog harvest closes, folds or narrows every row it names and adds no row unless the
      author agrees (backlog G83).
- [ ] The release comparison runs against 0.3.0 as the running side (backlog F22).
- [x] The gate guards refuse a red record that is a mutation of its commit's code, a commit at a
      rebase's conflict stop and added conflict markers.
- [ ] 0.4.0 is deployed and tagged, its comparison against `v0.3.0` reporting no finding (backlog
      F23).

## M8: The numbers hold up

Slice: the figures the header named are complete, and the rule behind them is decided where the
ledger cannot decide it.

- [x] Every storage name carries the value the game gives it, is recorded as a parser miss, or is
      recorded as unpriced by the game with the date of the read that found no price (2026-09-22).
      - Of the 15 names over 31 rows, 13 are gem pieces no complete read priced and 2 are parser
        misses.
      - Recording them moves no sum on the 03.09.2026 snapshot
        ([testing.md](knowledge-base/testing.md)).
- [x] The three ammunition sorts the NPC sells are credited in full, every other sort at 60 %, as
      decided 2026-09-27 (2026-10-01).
      - Why: a rule the ledger can apply that bounds the double payment to crafters.
      - On the 03.09.2026 snapshot the guild trader stands at `+1.456.343`
        ([testing.md](knowledge-base/testing.md)).
      - It belongs here because it changes what a deposit credits.
- [x] A member's row, the guild row and the header figures are each rounded once from the exact
      values, halves away from zero, as decided 2026-09-27 (2026-09-30).
      - On the 03.09.2026 snapshot 7 of 42 rows and four guild figures differ by one gold from
        `v0.2.0`'s ([testing.md](knowledge-base/testing.md)).
- [x] A member cycling trader goods through the storage is named with the item and the overlapping
      quantity, and a test proves the warning stays silent for a crafter who withdraws material and
      deposits the product.
- [ ] The consent-banner text is purged from the ledger and the path that let it in is closed
      (backlog F8).
- [ ] One opening entry per avatar books the counted stock at a chosen instant, marked as such on the
      wire and in the view, and the recomputed sums equal that stock (backlog E18).
- [ ] Avatar name matching folds case in the ledger lookups and the meta keys, not only in the union,
      beside the opening entries (backlog E18).

## M9: Clear the last bottleneck

Slice: a scrape that aborts midway leaves both ledgers where they were.

- [x] The duplicated bank/storage repositories are unified, one managed connection source, no
      cross-entity constant use.
- [ ] A scrape that aborts after the bank step leaves neither ledger table changed (backlog D24).
      It needs a transaction spanning both repositories, which is why it follows the unification.
- [ ] A commit SQLite refuses leaves no pooled connection inside an open transaction, and a
      scheduled job run does not outlive the closed context (backlog F12, F10).

## M10: What the bottlenecks release

Slice: the measured costs come down and the timezone hazard is fixed at its root.

- [x] Both ledgers carry an index on `(avatar, timeStamp)`, reaching an existing database through a
      migration; the query plans show `SEARCH … USING INDEX`. Two of the three affected queries sit
      in the request path.
- [ ] A stored meta value that cannot be read costs only its own avatar, on the overview and in the
      recompute, before any migration of the meta store meets one (backlog B36).
- [ ] The value sums are exact fixed-point gold, converted by one migration (backlog D25).
- [ ] The recompute reads pre-grouped sums instead of loading whole ledgers into the JVM; per-avatar
      values stay identical on the production snapshot, and the round-trip detection keeps the
      per-entry rows it needs (backlog D18).
- [ ] Timestamps and `last_updated` round-trip timezone-independently as instants (backlog D14),
      retiring the interim startup guard, `TimezoneStartupValidator`.
- [ ] The `withdrawl` → `withdrawal` rename runs as a migration, values preserved 1:1, in the same
      migration as the fixed-point sums (backlog D9).

## M11: Product build-out

Slice: the dashboard answers windowed questions, and the work the value model rates at zero gets
its due.

- [ ] A windowed aggregation answers per-avatar sums for an arbitrary `[from,to]`; the filter
      round-trips through the URL on all three views (backlog E12, absorbing the date-range
      reporting). It shares its query shape with D18, which is why it follows it.
- [ ] "Gildenmitglied des Monats": four awards per calendar month, last completed month and running
      standing (backlog E13).
- [ ] Hunt-loot estimate (backlog E3). Its valuation half is decided; only the display estimate of
      open question D-4 still gates it.
- [ ] A load failure and a dead link each show a picture of their own, the "link dead" among
      them, before 1.0 (backlog E27).
- [ ] A member whose suspected round trips recur is flagged (backlog E30).
- [ ] A ledger keeps the reader's place when it reloads (backlog E37).
- [ ] Names sort with one punctuation order in Java and in the SPA, or the difference is accepted
      by decision (backlog F13).

## M12: Ops, security & environment

Slice: the stand deploys, scans and authenticates the way a showcase should, and the framework
reaches its current major.

- [ ] The bundled webdriver binaries and the local-browser machinery are gone (backlog F1); the
      author's history rewrite follows, once **no** worktree is open, no pushed `claude/` branch or
      open pull request remains, both machines clone afresh, and the author moves the protected tags
      `v0.1.0`, `v0.2.0` and `v0.3.0`.
- [ ] Dependabot covers all three ecosystems and one refresh pass has run (backlog H11).
- [ ] The browser's first-launch race is removed or recorded as outside the project's reach, with the
      refresh (backlog H11).
- [ ] `Configuration` is real and immutable (C1); `vulnScan` is at zero and gated (C8).
- [ ] Every pull request head is built by CI, and the landing refuses a head without its green named
      check (backlog A4), after the gate mechanics.
- [ ] Auth, session and rate limiting move to JWT, so no credential travels in a URL (backlog C11).
- [ ] The admin page leads to the members it names, on the roles above (backlog E28).
- [ ] The admin page offers a manual scrape and recompute, beside the JWT move (backlog E17).
- [ ] A `selenium/standalone-firefox` service backs an integration test, and the server-booting
      tests split into their own Gradle set (H2).
- [ ] Micronaut 5, endpoints 1:1 against the prod snapshot (H9). Precondition: the nets above.

## M14: The guild's skills

Slice: every member's skill levels are read from the game each week and shown on a page of their
own, replacing the hand-kept skills sheet. **After 1.0.**

- [ ] What the game's ranking shows is read once and written down, before the collection is built on
      it (backlog E31).
- [ ] The levels are read at the start and every 7 days after, and each reading is kept with its
      date (backlog E32).
- [ ] The overview links a skills page that shows the newest reading per skill and as a member ×
      skill matrix, switchable (backlog E33).
- [ ] A member's level carries a progress mark once about six months of readings exist (backlog
      E34). **Last in this milestone**, and gated on time rather than work.

## Ongoing (no milestone; pull into any gap)

- **Mechanical error prevention, the showcase's thesis.** Why: the learnings repeatedly show a
  session forgetting a written rule.
  - The tool-level enforcement hooks (G7), whose working-directory-drift guard addresses a failure
    mode that has already cost work.
  - The one remaining hole in the git gate, a prefix-squattable host-path scan (G13).
  - The React hook rules, which nothing enforces today (B24).
  - Agent-environment polish (G11), including the shared probe result directory that makes
    concurrent falsifier runs flaky and the two worktree costs of the dissolved build-performance
    analysis.
  - The SessionStart hook that injects the lessons deterministically (G10).
  - The Gradle daemons' memory under parallel strands (H13).
  - The gate, agent-tool and doc rows (from G21 on), pulled when a gate meets one.
  - Most doc-debt rows go into the 0.4.0 harvest.
  - After 0.4.0, the gate mechanics (G51 with G32, G40, G65).
- **Request handling at the edge:** the line slicer's per-line cost (F15) and the browser lost for
  a target with a broken escape (F18).
- **Deploy and environment:** a failed ssh connection named as such (F20), the database directory
  given to the runtime user (F21), the browser grid on the agent seat (H14).
- **Test-suite hygiene:** the test debt two feature gates
  left (B33, B34, B38), the release comparison's remaining edges (B42, B43, B44, B45), style
  alignment (B8), the shared boot fixture (B18), the parser's residual shapes (B22), deterministic
  fixture ids (B23), the unexplained load-sensitive failure (B20), AssertJ in `SmokeTest` (B7).
- **Docs & code hygiene:** the doc-debt collection row (G18), the parser
  entrypoints made injectable (D15), exception/logging hygiene and dead code (D11).
- **Craftsmanship, when a gap allows:** catalog refactor (D6), full repackaging (D3) last, on top
  of a tested core.
- **Creative:** weekly guild report / delivery channel (F3).

## Parked: explicitly not in this stage (decision 2026-09-04)

Kept in the backlog with their rationale, not deleted; revisit only on a new reason.

- **G6** static-analysis / Sonar gate: waits on a condition that is not arriving.
- **F4** multi-guild / multi-world: speculative, since there is one guild and no second consumer.
