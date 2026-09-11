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

## Order

| # | Milestone | Items | Why this position |
|---|-----------|-------|-------------------|
| M5 | Overview truth | author steps only | Closing out; the code landed, two restated author checks remain |
| M7 | The overview states the guild's actual position | the active/dormant split, the stone aliases, the release run (E16, D23 in part, F6) | **The `0.2.0` cut.** The most visible defect was the total row saying the opposite of the truth; the aliases join because the release publishes `Gildenspende` for the first time |
| M8 | The numbers hold up | the remaining unvalued names and the ammunition question, round-trip detection, opening balance, explanation page with the trader's figure (D23, D-12, F7, E18, E19, E20) | The header named the figures, which is what makes what they omit a defect; the explanation page closes the milestone because it must follow every rule it explains |
| M9 | Clear the last bottleneck | D12, D24 | The item every later read method waits on; it also gates the transactional ingest |
| M10 | What the bottlenecks release | D17, D18, D22→D14, D9 | Measured request-path cost and the timezone fix at its root |
| M11 | Product build-out | E12→E13, E9, E3, E6 | E12 inherits D18's query shape, so it follows it |
| M12 | Ops, security & environment | F1, H11, C1, C8, C10, C11, H2→H6, H9 | F1 before the author's history rewrite; H9 last, on the nets built above |

## M5: Overview truth

Slice: the overview answers "what did this member contribute" completely, instead of showing gold
only. **Code complete; only author steps remain.**

- [ ] The guild is told the valuation rule and what the four header figures mean (author step,
      restated 2026-09-11: it supersedes the narrower announcement of the corrected storage sums
      decided 2026-09-02, because the rule itself changed afterwards).
- [ ] On a real sample, the columns that **must** match the sheet do (bank in and out, storage
      withdrawal), and the two deliberate divergences on the deposit column do **not** (author
      check, restated 2026-09-11: the software diverges from the sheet by decision, so the old
      "values match the sheet" wording could never be ticked).

## M7: The overview states the guild's actual position

Slice: the overview stops adding measured gold to modelled material, the roster puts the members who
matter above the fold, and the release goes out driven rather than typed. **This is `0.2.0`.**

- [x] What the guild keeps of a member's deposits is named rather than hidden inside a number shaped
      like a balance: a stat header of four figures (`Gildenbank`, `Gildenlagerwert`,
      `Gildenspende`, `Handwerkssubventionen`) plus a `Nach Abzügen` / `Vor Abzügen` switch on the
      table's sixth column, leaving its column count and density untouched. It carried the valuation
      rule with it, from the recipe-based stand-in to the guild's announced category rule
      (decided 2026-09-10).
- [ ] The roster splits into active and dormant, cut against the data's own timestamp and never
      against the viewer's clock (backlog E16, on the view the header just rebuilt).
- [ ] `Marmor`, `Granit` and `Schiefer` carry the value of their catalogued `*stein` forms (backlog
      D23, first step, decided 2026-09-11): the release publishes `Gildenspende` for the first time
      and it is `2.478.114` (2,4 %) too low without them, with a further `371.016` missing from
      storage withdrawal.
- [ ] A committed deploy script drives a full deploy and rollback over ssh, carrying every check that
      caught the `0.1.0` defects, with a self-test that fails each of them against a faked bad state.
      The script and its self-test have landed; the box closes when it has driven this release and a
      rollback against the home server (backlog F6), so `0.2.0` is the first deploy nobody types by hand.

## M8: The numbers hold up

Slice: the figures the header named are complete, the rule behind them is decided where the ledger
cannot decide it, and a member can follow how his own row comes about.

- [ ] Each of the remaining unvalued storage names is classified as *must carry a value* or
      *deliberately zero*, the deliberate ones stop counting as unknown in `/health`, and the
      recompute is re-run so the corrected sums are measured rather than assumed (backlog D23).
- [ ] Whether bought ammunition is credited in full is decided, with the double payment to crafters
      addressed by a rule the ledger can actually apply (open question D-12). It belongs here
      because it changes what a deposit credits.
- [ ] A member cycling trader goods through the storage is named with the item and the overlapping
      quantity, and a test proves the warning stays silent for a crafter who withdraws material and
      deposits the product (backlog F7).
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

Slice: the item that every later read method waits on.

- [ ] The duplicated bank/storage repositories are unified, one managed connection source, no
      cross-entity constant use (backlog D12). **Before** D18 and E12, or the same query lands
      duplicated a fourth and fifth time.
- [ ] A scrape that aborts after the bank step leaves neither ledger table changed (backlog D24).
      It needs a transaction spanning both repositories, which is why it follows the unification.

## M10: What the bottlenecks release

Slice: the measured costs come down and the timezone hazard is fixed at its root.

- [ ] Both ledgers carry an index on `(avatar, timeStamp)`, reaching an existing database through a
      migration; the query plans show `SEARCH … USING INDEX` (backlog D17). Two of the three
      affected queries sit in the request path.
- [ ] The recompute reads pre-grouped sums instead of loading whole ledgers into the JVM; per-avatar
      values stay identical on the production snapshot (backlog D18).
- [ ] Timestamps and `last_updated` round-trip timezone-independently as instants (backlog D14),
      retiring the interim startup guard (backlog D22).
- [ ] The `withdrawl` → `withdrawal` rename runs as a migration, values preserved 1:1 (backlog D9).

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

## M12: Ops, security & environment

Slice: the stand deploys, scans and authenticates the way a showcase should, and the framework
reaches its current major.

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

- **Mechanical error prevention, the showcase's thesis:** the tool-level enforcement hooks, whose
  working-directory-drift guard addresses a failure mode that has already cost work (G7); the one
  remaining hole in the git gate, a prefix-squattable host-path scan (G13); the React hook rules,
  which nothing enforces today (B24); the wildcard-import ban (G17, only in a gap with **no** open
  strand); agent-environment polish, including the shared probe result directory that makes
  concurrent falsifier runs flaky and the two worktree costs folded in from the dissolved
  build-performance analysis (G11); and the SessionStart hook that injects the lessons
  deterministically (G10). The learnings repeatedly show a session forgetting a written rule.
- **Test-suite hygiene:** the scripted 1:1 value comparison (B19), repository tests (B4), style
  alignment (B8), the shared boot fixture (B18), the two parser residuals (B21, B22), deterministic
  fixture ids (B23), the unexplained load-sensitive failure (B20), AssertJ in `SmokeTest` (B7).
- **Docs & code hygiene:** KB in lockstep with code (G3), KB accuracy sweep (G18), the parser
  entrypoints made injectable (D15), exception/logging hygiene and dead code (D11).
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
