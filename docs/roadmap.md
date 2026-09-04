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
> **Order re-cut 2026-09-04** (author decisions that day, see open-questions.md): correctness
> before product, the two dependency bottlenecks before the product build-out, and a parked set
> that is explicitly not in this stage.

## Order

| # | Milestone | Items | Why this position |
|---|-----------|-------|-------------------|
| M5 | Overview truth | author steps only | Closing out; the code landed, two author checks remain |
| M6 | Numbers you can trust | B24+B19, D21, E14, D20, D19 | Every feature stands on these numbers; B24 is the only `P0` |
| M7 | The overview states the guild's actual position | E15, E16 | The most visible defect: the total row says the opposite of the truth |
| M8 | Clear the two bottlenecks | D12, D10 | Two `M` items that release five others; the longer they wait, the more piles up |
| M9 | What the bottlenecks release | D17, D18, D22→D14, D9 | Measured request-path cost and the timezone fix at its root |
| M10 | Product build-out | E12→E13, E9, E3, E6 | E12 inherits D18's query shape, so it follows it |
| M11 | Ops, security & environment | F6, F1, H11, C1, C8, C10, C11, H2→H6, H9 | F1 before the author's history rewrite; H9 last, on the nets built above |

## M5: Overview truth

Slice: the overview answers "what did this member contribute" completely, instead of showing gold
only. **Code complete; only author steps remain.**

- [ ] The corrected storage sums are announced in the guild (author step, decision 2026-09-02).
- [ ] Values match the sheet on a real sample (author check).

## M6: Numbers you can trust

Slice: every number the overview shows is verified, refreshes independently of the scrape, and says
so when it did not.

- [ ] A production DB taken after the `0.1.0` deploy reproduces its own stored sums when recomputed
      from its rows; the automated comparison harness that proves it is committed, not a one-off
      (backlog B24 at `P0`, absorbing B19). The author pulls the database; the stop must be
      **proven**, per build-run-deploy.md.
- [ ] A failed scrape still runs the recompute, and `/health` tells "could not scrape" from "could
      not recompute" (backlog D21).
- [ ] The collection timestamp moves to an admin surface, so the overview stops carrying a
      guild-wide number that reads like a per-row freshness claim (backlog E14).
- [ ] A row whose sums did not refresh is marked as such on the wire and in the table, and the
      guild total states that it contains one (backlog D20, gated on E14).
- [ ] No read path dereferences a ledger `timeStamp` unguarded; an unreadable timestamp degrades
      one entry or one avatar by an explicit, tested rule (backlog D19).

## M7: The overview states the guild's actual position

Slice: the overview stops adding measured gold to modelled material, and the roster puts the
members who matter above the fold.

- [ ] The levy is named rather than hidden inside a number shaped like a balance: a stat header
      (treasury / material balance / levy collected) plus a Beitrag↔Saldo switch, leaving the
      table's density untouched (backlog E15; variant A of D-12, decided 2026-09-04).
- [ ] The roster splits into active and dormant, cut against the data's own timestamp and never
      against the viewer's clock (backlog E16, after E15 because both rebuild the same view).

## M8: Clear the two bottlenecks

Slice: the two items that every later query and every schema change waits on.

- [ ] The duplicated bank/storage repositories are unified, one managed connection source, no
      cross-entity constant use (backlog D12). **Before** D18 and E12, or the same query lands
      duplicated a fourth and fifth time.
- [ ] A schema-migration framework is wired and historical data provably survives it (backlog
      D10). It gates D17, D14 and D9; `createTableIfNotExists` skips an existing table, so nothing
      declarative reaches the live database without it.

## M9: What the bottlenecks release

Slice: the measured costs come down and the timezone hazard is fixed at its root.

- [ ] Both ledgers carry an index on `(avatar, timeStamp)`, reaching an existing database through a
      migration; the query plans show `SEARCH … USING INDEX` (backlog D17). Two of the three
      affected queries sit in the request path.
- [ ] The recompute reads pre-grouped sums instead of loading whole ledgers into the JVM; per-avatar
      values stay identical on the production snapshot (backlog D18).
- [ ] Timestamps and `last_updated` round-trip timezone-independently as instants (backlog D14),
      retiring the interim startup guard (backlog D22).
- [ ] The `withdrawl` → `withdrawal` rename runs as a migration, values preserved 1:1 (backlog D9).

## M10: Product build-out

Slice: the dashboard answers windowed questions, and the work the value model rates at zero gets
its due.

- [ ] A windowed aggregation answers per-avatar sums for an arbitrary `[from,to]`; the filter
      round-trips through the URL on all three views (backlog E12, absorbing the date-range
      reporting). It shares its query shape with D18, which is why it follows it.
- [ ] "Gildenmitglied des Monats": four awards per calendar month, last completed month and running
      standing (backlog E13).
- [ ] Avatar name matching folds case in the ledger lookups and the meta keys, not only in the union
      (backlog E9).
- [ ] Hunt-loot estimate (backlog E3) — gated on the D-4 valuation rule, still open.
- [ ] History / time-series per avatar (backlog E6).

## M11: Ops, security & environment

Slice: the stand deploys, scans and authenticates the way a showcase should, and the framework
reaches its current major.

- [ ] A committed deploy script drives a full deploy and rollback over ssh, carrying every check
      that caught the `0.1.0` defects (backlog F6).
- [ ] The bundled webdriver binaries and the local-browser machinery are gone (backlog F1); the
      author's history rewrite follows, once **no** worktree is open.
- [ ] Dependabot covers all three ecosystems and one refresh pass has run (backlog H11).
- [ ] `Configuration` is real and immutable (C1); `vulnScan` is at zero and gated (C8); every
      request is counted and logged whatever its target looks like (C10).
- [ ] Auth, session and rate limiting move to JWT, so no credential travels in a URL (backlog C11).
- [ ] A `selenium/standalone-firefox` service backs an integration test (H2), and the
      server-booting tests split into their own Gradle set (H6).
- [ ] Micronaut 5, endpoints 1:1 against the prod snapshot (H9). Precondition: the nets above.

## Ongoing (no milestone; pull into any gap)

- **Mechanical error prevention, the showcase's thesis:** enforcement hooks (G7), the git-hook
  gate-bypass root cause (G13), the wildcard-import ban (G17, only in a gap with **no** open
  strand), agent-environment polish including the shared probe result directory that makes
  concurrent falsifier runs flaky (G11), the SessionStart hook that injects the lessons
  deterministically (G10) — the learnings repeatedly show a session forgetting a written rule.
- **Test-suite hygiene:** repository tests (B4), style alignment (B8), the shared boot fixture
  (B18), the two parser residuals (B21, B22), deterministic fixture ids (B23), the unexplained
  load-sensitive failure (B20), AssertJ in `SmokeTest` (B7).
- **Docs & code hygiene:** KB in lockstep with code (G3), KB accuracy sweep (G18), the parser
  entrypoints made injectable (D15),
  exception/logging hygiene and dead code (D11).
- **Craftsmanship, when a gap allows:** catalog refactor (D6), full repackaging (D3) last, on top
  of a tested core.
- **Creative:** weekly guild report / delivery channel (F3).

## Parked — explicitly not in this stage (decision 2026-09-04)

Kept in the backlog with their rationale, not deleted; revisit only on a new reason.

- **A4** CI (GitHub Actions) — local-only deploy, no shared PRs to guard (already deprioritized
  2026-06-15).
- **G6** static-analysis / Sonar gate — waits on a condition that is not arriving.
- **F4** multi-guild / multi-world and **F5** public read-only API — speculative: there is one
  guild and no second consumer.
- **G5** case study — worth writing once there is something finished to tell.
- **G4** BDD tooling — "plain JUnit unless asked" has held since June and blocks nothing.
- **G8** `/commit` slash command and **G9** the WebFetch doc convention — neither prevents an error
  mechanically, which is the bar this stage applies to the G epic.
