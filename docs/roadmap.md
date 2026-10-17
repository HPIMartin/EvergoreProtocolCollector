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
> **Ordering rule** (each decision and its why in [open-questions.md](open-questions.md)):
>
> - the next release and the work it carries go first;
> - then what the author placed right after it;
> - then what changes what the numbers are;
> - then the bottlenecks;
> - then product build-out and operations;
> - the skills milestone follows 1.0.
>
> The explanation page changes in the same commit as any rule it explains, so a rule that lands
> after it extends the page instead of waiting for it. A parked set stays explicitly out of this
> stage.

## Order

| # | Milestone | Items | Why this position |
|---|-----------|-------|-------------------|
| M17 | 0.5.0: members log in | before the login: the timestamps (D14) and the preparation: the unreadable-head test (B47), the React hook rules (B24), the act-in-assertion check (G84), the fingerprint check (G66), the security lens (G85); the login (C11) with the message form (C14), the password delivery (C15), the user management (C12) and the usage records (C13); the deploy script (F21, F20) and the release tooling (F24); the release (F25) | **After 0.4.0** (author decisions 2026-10-14 and 2026-10-15): auth is the feature 0.5.0 carries |
| M18 | Right after 0.5.0 | HTTPS through a reverse proxy (C16), the manual scrape trigger (E17), the admin page's member links (E28), dropping the old timestamp columns (D26) | **After 0.5.0** (author decisions 2026-10-15): HTTPS follows the login over plain HTTP, the roles of 0.5.0 carry the admin page's additions, and the old timestamp columns go once 0.5.0 is stable |
| M8 | The numbers hold up | the remaining unvalued names and the ammunition credit, round-trip detection, the rounding, the opening balance (E18); the consent text in the ledger (F8) | The header named the figures, which is what makes what they omit a defect; the opening balance updates the explanation page in the same commit as its rule |
| M9 | Clear the last bottleneck | the atomic bank+storage ingest (D24) with the job outliving the closed context (F10); the slow-read warning (F19) | The transactional ingest rests on the unified repositories, which every later read method needed first |
| M10 | What the bottlenecks release | the unreadable meta value (B36) → exact fixed-point sums (D25), with the naming follow-ups (D9) → the aggregation in SQL (D18) | Measured request-path cost |
| M11 | Product build-out | the time filter (E12) → the member of the month (E13); the estimated hunt deposits (E3); failure pages with a picture (E27); recurring round trips flagged (E30); a ledger keeping its place on reload (E37); the collation of punctuation (F13) | The time filter inherits the SQL aggregation's query shape, so it follows it |
| M12 | Ops, security & environment | removing the bundled webdrivers (F1); Dependabot for docker and gradle (H11); a real `Configuration` (C1); vulnScan at zero (C8); CI on every pull request (A4); a Selenium Firefox service (H2); Micronaut 5.0 (H9) | Removing the webdrivers before the author's history rewrite; Micronaut 5.0 last, on the nets built above |
| M14 | The guild's skills | reading the ranking (E31) → skill levels every 7 days (E32) → a skills page (E33); skill progress marked (E34) | **After 1.0** (author decision 2026-09-30; where 1.0 is cut is open (D-16)). The ranking is read before anything is built on it; the progress mark waits on about six months of readings |

## M17: 0.5.0, members log in

Slice: every guild member logs in with an own account, the admin manages the accounts on the admin
page, and the author sees who uses the app and how.

- [ ] Timestamps and `last_updated` round-trip timezone-independently as instants (backlog D14).
- [ ] The new timestamp columns stand beside the old, so that 0.4.0 still runs on the database
      (backlog D14).
- [ ] The preparation has landed before the login's first implementation commit: the
      unreadable-head test, the React hook rules and the act-in-assertion check (backlog B47, B24,
      G84).
- [ ] The scenario gate holds its reviewer to the files the falsifiers judged (backlog G66).
- [ ] A security lens reads the login from its first feature-gate panel (backlog G85).
- [ ] The database directory belongs to the runtime user (backlog F21).
- [ ] A failed ssh connection is named as such (backlog F20).
- [ ] Accounts, login and the password delivery are in place (backlog C11, C14, C15).
- [ ] The admin page manages the accounts (backlog C12).
- [ ] The admin page shows who logged in when and which views and options each member uses
      (backlog C13).
- [ ] The release comparison and the deploy check speak the login (backlog F24).
- [ ] 0.5.0 is deployed and tagged (backlog F25).

## M18: Right after 0.5.0

Slice: what the author placed right after the login release.

- [ ] The app answers the guild over HTTPS (backlog C16).
- [ ] The admin page leads to the members it names, on the roles of M17 (backlog E28).
- [ ] The admin page offers a manual scrape and recompute, on the roles of M17 (backlog E17).
- [ ] The old timestamp columns are gone once 0.5.0 is stable (backlog D26).

## M8: The numbers hold up

Slice: the figures the header named are complete, and the rule behind them is decided where the
ledger cannot decide it.

- [ ] The consent-banner text is purged from the ledger and the path that let it in is closed
      (backlog F8).
- [ ] One opening entry per avatar books the counted stock at a chosen instant, marked as such on the
      wire and in the view, and the recomputed sums equal that stock (backlog E18).
- [ ] Avatar name matching folds case in the ledger lookups and the meta keys, not only in the union,
      beside the opening entries (backlog E18).

## M9: Clear the last bottleneck

Slice: a scrape that aborts midway leaves both ledgers where they were.

- [ ] A scrape that aborts after the bank step leaves neither ledger table changed (backlog D24).
      It needs a transaction spanning both repositories, which is why it follows the unification.
- [ ] A scheduled job run does not outlive the closed context (backlog F10).

## M10: What the bottlenecks release

Slice: the measured costs come down.

- [ ] A stored meta value that cannot be read costs only its own avatar, on the overview and in the
      recompute, before any migration of the meta store meets one (backlog B36).
- [ ] The value sums are exact fixed-point gold, converted by one migration (backlog D25).
- [ ] The recompute reads pre-grouped sums instead of loading whole ledgers into the JVM; per-avatar
      values stay identical on the production snapshot, and the round-trip detection keeps the
      per-entry rows it needs (backlog D18).
- [ ] The `withdrawl` → `withdrawal` rename runs as a migration, values preserved 1:1, in the same
      migration as the fixed-point sums (backlog D9).

## M11: Product build-out

Slice: the dashboard answers windowed questions, and the work the value model rates at zero gets
its due.

- [ ] A windowed aggregation answers per-avatar sums for an arbitrary `[from,to]`; the filter
      round-trips through the URL on all three views (backlog E12, absorbing the date-range
      reporting). It shares its query shape with the aggregation in SQL (D18), which is why it
      follows it.
- [ ] "Gildenmitglied des Monats": four awards per calendar month, last completed month and running
      standing (backlog E13).
- [ ] Hunt-loot estimate (backlog E3).
  - Its valuation half is decided; only the display estimate, the hunt-loot estimate formula (D-4),
    still gates it.
- [ ] A load failure and a dead link each show a picture of their own, the "link dead" among
      them, before 1.0 (backlog E27).
- [ ] A member whose suspected round trips recur is flagged (backlog E30).
- [ ] A ledger keeps the reader's place when it reloads (backlog E37).
- [ ] Names sort with one punctuation order in Java and in the SPA, or the difference is accepted
      by decision (backlog F13).

## M12: Ops, security & environment

Slice: the stand deploys, scans and authenticates the way a showcase should, and the framework
reaches its current major.

- [ ] The bundled webdriver binaries and the local-browser machinery are gone (backlog F1).
  - The author's history rewrite follows, once **no** worktree is open, no pushed `claude/` branch
    or open pull request remains, both machines clone afresh, and the author moves the protected
    tags `v0.1.0`, `v0.2.0` and `v0.3.0`.
- [ ] Dependabot covers all three ecosystems and one refresh pass has run (backlog H11).
- [ ] `Configuration` is real and immutable (C1); `vulnScan` is at zero and gated (C8).
- [ ] Every pull request head is built by CI, and the landing refuses a head without its green named
      check (backlog A4), after the gate mechanics.
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
  - Agent-environment polish (G11), including the shared probe result directory that makes
    concurrent falsifier runs flaky and the two worktree costs of the dissolved build-performance
    analysis.
  - The SessionStart hook that injects the lessons deterministically (G10).
  - The Gradle daemons' memory under parallel strands (H13).
  - The gate, agent-tool and doc rows (from G21 on), pulled when a gate meets one.
  - After 0.5.0, the gate mechanics (G51 with G32, G40, G65).
- **Request handling at the edge:** the browser lost for a target with a broken escape (F18).
- **Deploy and environment:** the browser grid on the agent seat (H14).
- **Test-suite hygiene:** the test debt two feature gates
  left (B34, B38), the release comparison's remaining edges (B42, B43, B44, B45), style
  alignment (B8), the shared boot fixture (B18), the parser's residual shapes (B22), deterministic
  fixture ids (B23), the unexplained load-sensitive failure (B20).
- **Docs & code hygiene:** the doc-debt collection row (G18), exception/logging hygiene and dead code (D11).
- **Craftsmanship, when a gap allows:** catalog refactor (D6), full repackaging (D3) last, on top
  of a tested core.
- **Creative:** weekly guild report / delivery channel (F3).

## Parked: explicitly not in this stage (decision 2026-09-04)

Kept in the backlog with their rationale, not deleted; revisit only on a new reason.

- **Static-analysis / Sonar gate** (G6): waits on a condition that is not arriving.
- **Multi-guild / multi-world** (F4): speculative, since there is one guild and no second consumer.
