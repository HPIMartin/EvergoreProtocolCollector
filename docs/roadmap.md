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
> **Order re-cut 2026-09-11** (author decision, see open-questions.md): settle what the numbers
> *are*, then explain them, then the bottlenecks. The previous cut predates the valuation change and
> knew none of the five items it produced. The explanation page must be revised in the same change
> as any valuation rule it explains, so everything that changes what the numbers are lands before
> it. A parked set stays explicitly out of this stage.
> **Re-cut 2026-09-27** (author go on the plan after the scenario review, see open-questions.md):
> the specification catch-up (M13) comes first and runs beside the other lanes; the row rounding
> joins M8, ahead of the explanation page.
> **Re-cut 2026-10-10** (author go on the plan of 2026-10-09, see open-questions.md): 0.3.0 is
> released first, beside M13 (M15); M7 is done and removed; M5 is struck, as no sheet covers the
> time after July 2022 and the guild hears nothing before 1.0.

## Order

| # | Milestone | Items | Why this position |
|---|-----------|-------|-------------------|
| M13 | The specification runs | the corrected behaviors (C10) | Task 0 of the adopted process: every later strand builds on executable scenarios, and condensing before the step definitions keeps their steps from being written twice |
| M15 | 0.3.0 reaches the guild | the release (F14), with the scripted comparison (B19), the deep ledger page (E25) and the event-loop question (F11) | **First, beside M13** (author decision 2026-10-10): production runs `0.2.0` while everything since landed on `main`, and the last item of M13 changes nothing in the release build |
| M8 | The numbers hold up | the remaining unvalued names and the ammunition credit, round-trip detection, the rounding, opening balance, explanation page with the trader's figure (E18, E19, E20); the note of a row not yet computed (E36) and the consent text in the ledger (F8) | The header named the figures, which is what makes what they omit a defect; the explanation page closes the milestone because it must follow every rule it explains |
| M9 | Clear the last bottleneck | D24 with F12, F10 | The transactional ingest rests on the unified repositories, which every later read method needed first |
| M10 | What the bottlenecks release | B36→D25 with D9→D18, D22→D14 | Measured request-path cost and the timezone fix at its root |
| M11 | Product build-out | E12→E13, E9, E3, E6, E27, E30, E37, F13 | E12 inherits D18's query shape, so it follows it |
| M12 | Ops, security & environment | F1, H11, C1, C8, A4, C11→E28, E17, H2→H6, H9 | F1 before the author's history rewrite; H9 last, on the nets built above |
| M14 | The guild's skills | E31→E32→E33, E34 | **After 1.0** (author decision 2026-09-30; where 1.0 is cut is open, D-16). The ranking is read before anything is built on it; the progress mark waits on about six months of readings |

## M13: The specification runs

Slice: the scenarios the catch-up derived and the author reviewed run under `./verify all`, and a
scenario gate that reads as the stakeholder guards every new one.

- [x] A falsifier reads each draft as its stakeholder and against the whole suite (`falsifier-stakeholder`).
- [x] Every point in time in a scenario can be placed without working it out: the time rules of
  handbook §5 applied by the condensation.
- [x] The catch-up scenarios are condensed and carry the author's review notes.
- [x] Every confirmed scenario runs green, the member and the admin through the browser, the
      operator over HTTP and JSON, and the unclear ones are settled.
- [ ] Each corrected behavior is green and armed (backlog C10).

## M15: 0.3.0 reaches the guild

Slice: what landed since 0.2.0 runs on the home server, through a release gate that compares the
figures the valuation changes move.

- [ ] A script compares every figure the overview and the ledgers show, per member, between the
      running version and the candidate on the same data, and reports any difference no list
      names (backlog B19).
- [ ] The whole-ledger sort is measured on the 2026-10-08 snapshot, the author has set the bound
      for the deepest page, and the reads on Netty's event loop are decided for or against this
      release (backlog E25, F11).
- [ ] 0.3.0 is deployed and tagged: the V3 migration checked on a snapshot copy, the rollback text
      fitting the step back to 0.2.0, the release recorded as running (backlog F14). The guild is
      not told before 1.0.

## M8: The numbers hold up

Slice: the figures the header named are complete, the rule behind them is decided where the ledger
cannot decide it, and a member can follow how his own row comes about.

- [x] Every storage name carries the value the game gives it, is recorded as a parser miss, or is
      recorded as unpriced by the game with the date of the read that found no price (2026-09-22):
      of the 15 names over 31 rows, 13 are gem pieces no complete read priced and 2 are parser
      misses. The recompute was re-run on the 03.09.2026 snapshot and the guild position did not
      move ([testing.md](knowledge-base/testing.md)).
- [x] The three ammunition sorts the NPC sells are credited in full, every other sort at 60 %, as
      decided 2026-09-27 (2026-10-01): a rule the ledger can apply that bounds the double payment
      to crafters; on the 03.09.2026 snapshot the guild trader moves from `-2.189.599` to
      `+1.456.343` ([testing.md](knowledge-base/testing.md)). It belongs here because it changes what a deposit credits.
- [x] A member's row, the guild row and the header figures are each rounded once from the exact
      values, as decided 2026-09-27, halves away from zero (2026-09-30). It changes what a row shows,
      so it precedes the explanation page like every rule above it. On the 03.09.2026 snapshot 7 of
      42 rows and four guild figures differ by one gold ([testing.md](knowledge-base/testing.md)).
- [x] A member cycling trader goods through the storage is named with the item and the overlapping
      quantity, and a test proves the warning stays silent for a crafter who withdraws material and
      deposits the product.
- [ ] The note under "Vor Abzügen" of a row not yet computed names that state, its scenario
      reconfirmed by the author (backlog E36).
- [ ] The consent-banner text is purged from the ledger and the path that let it in is closed
      (backlog F8).
- [ ] One opening entry per avatar books the counted stock at a chosen instant, marked as such on the
      wire and in the view, and the recomputed sums equal that stock (backlog E18).
- [ ] A page reachable from the overview explains every header figure and every credit tier with one
      worked example each, in the style of the guild's own announcements, and a KB rule makes a
      valuation change update it in the same commit (backlog E19). **Last in this milestone:** it
      must follow every rule above it or be written twice.
- [ ] The trader's figure is explained down to the transactions that make it, and each of the three
      facts the ledger cannot see is either represented or recorded as out of the model's reach
      (backlog E20), carried by the explanation page rather than standing alone.

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
      retiring the interim startup guard (backlog D22).
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
- [ ] Avatar name matching folds case in the ledger lookups and the meta keys, not only in the union
      (backlog E9).
- [ ] Hunt-loot estimate (backlog E3). Its valuation half is decided; only the display estimate of
      open question D-4 still gates it.
- [ ] History / time-series per avatar (backlog E6).
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
      `v0.1.0` and `v0.2.0`.
- [ ] Dependabot covers all three ecosystems and one refresh pass has run (backlog H11).
- [ ] `Configuration` is real and immutable (C1); `vulnScan` is at zero and gated (C8).
- [ ] Every pull request head is built by CI, and the landing refuses a head without its green named
      check (backlog A4), after the gate mechanics.
- [ ] Auth, session and rate limiting move to JWT, so no credential travels in a URL (backlog C11).
- [ ] The admin page leads to the members it names, on the roles above (backlog E28).
- [ ] The admin page offers a manual scrape and recompute, beside the JWT move (backlog E17).
- [ ] A `selenium/standalone-firefox` service backs an integration test (H2), and the
      server-booting tests split into their own Gradle set (H6).
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

- **Mechanical error prevention, the showcase's thesis:** the tool-level enforcement hooks, whose
  working-directory-drift guard addresses a failure mode that has already cost work (G7); the one
  remaining hole in the git gate, a prefix-squattable host-path scan (G13); the React hook rules,
  which nothing enforces today (B24); agent-environment polish, including the shared probe result directory that makes
  concurrent falsifier runs flaky and the two worktree costs folded in from the dissolved
  build-performance analysis (G11); and the SessionStart hook that injects the lessons
  deterministically (G10). The learnings repeatedly show a session forgetting a written rule. Also
  the build I/O off the workspace disk (H13), and the gate and agent tool rows (G21 to G50), pulled
  when a gate meets one; first the gate mechanics (G51 with G30, G32, G40, G48) and the permission
  facts (G52).
- **Test-suite hygiene:** the browser's first-launch race (B35), the test debt two feature gates
  left (B33, B34, B38), the headline shapes the parser still misreads (B39, B40, B41), repository tests (B4), style
  alignment (B8), the shared boot fixture (B18), the parser residual (B22), deterministic
  fixture ids (B23), the unexplained load-sensitive failure (B20), AssertJ in `SmokeTest` (B7).
- **Docs & code hygiene:** KB in lockstep with code (G3), KB accuracy sweep (G18), the parser
  entrypoints made injectable (D15), exception/logging hygiene and dead code (D11).
- **Craftsmanship, when a gap allows:** catalog refactor (D6), full repackaging (D3) last, on top
  of a tested core.
- **Creative:** weekly guild report / delivery channel (F3).

## Parked — explicitly not in this stage (decision 2026-09-04)

Kept in the backlog with their rationale, not deleted; revisit only on a new reason.

- **G6** static-analysis / Sonar gate — waits on a condition that is not arriving.
- **F4** multi-guild / multi-world and **F5** public read-only API — speculative: there is one
  guild and no second consumer.
- **G5** case study — worth writing once there is something finished to tell.
- **G8** `/commit` slash command and **G9** the WebFetch doc convention — neither prevents an error
  mechanically, which is the bar this stage applies to the G epic.
