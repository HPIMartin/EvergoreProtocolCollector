# Roadmap: Evergore Protocol Collector

> Milestone order and per-milestone acceptance for the open work. Items, priorities, effort and
> per-item acceptance stay canonical in [backlog.md](backlog.md) (IDs here are pointers to
> still-live items); decisions and their why in [open-questions.md](open-questions.md). This file
> adds only the cut into milestones, their order, and the milestone-level "done". A completed
> milestone is removed, same rule as backlog rows (git is history). Every milestone lands through
> the agent pipeline and the review gateway
> ([multi-agent-playbook.md](knowledge-base/multi-agent-playbook.md), handbook §7); landings stay
> author-serialized.

## Order

| # | Milestone | Items | Why this position |
|---|-----------|-------|-------------------|
| M3 | Dashboard views (strands `spa-look`, `spa-data-shell`, `spa-legacy-out`) | The SPA over the JSON API | The officer-visible payoff of the dashboard rebuild |
| M4 | Ingest & test robustness | B16, B20 | Small hardening set, deliberately behind the dashboard strands (decision 2026-07-17) |
| M5 | Sheet parity, confirmed columns | E1, E2 | The headline metric lands on integrity-fixed data and a real dashboard |
| M6 | Real-browser integration tests | H2, H6 | Scrape coverage without host Firefox; the test split gates on the Selenium service |
| M7 | Micronaut 5 | H9 | Deferred deliberately until 1:1 is re-proven through the nets built in M3-M6 |

## M3: Dashboard views (strands `spa-look`, `spa-data-shell`, `spa-legacy-out`)

Slice: a guild officer reads the overview and avatar details in the SPA instead of the
HTML-string templates.

- [x] Overview table plus avatar bank/storage views on the existing client routes (`/overview`,
      `/avatars/{avatar}/bank`, `/avatars/{avatar}/storage`), reading the JSON API with the token
      carried across every route and every in-app link.
- [x] Every view TDD-built with Vitest/RTL (`data-testid`, semantic tables); the frontend layer
      rules hold at lint time ([frontend.md](knowledge-base/frontend.md)).
- [x] Visual style recreated without copying game assets (decision 2026-07-04); the views render
      through the shared frame, table and status panel, with sortable columns.
- [x] The legacy HTML pages deleted, so the three paths reach the shell on a deep link too
      (decision 2026-08-07).
- [ ] All columns the service already stores are visible; the full sheet handover completes with M5.

Track: worktree strands `spa-look` (theme and primitives), `spa-data-shell` (wire types, fetching,
routing) and `spa-legacy-out` (the HTML pages out, the acceptance net on the JSON API).

## M4: Ingest & test robustness

Slice: no single malformed protocol line aborts an ingest; the suite carries no wall-clock
dependency.

- [x] No single malformed protocol line aborts the ingest, mis-attributes items or vanishes
      silently: an unparseable item number skips its own line, an entry with no parseable items is
      logged, every dropped block head is logged, and the greedy avatar group can no longer latch
      onto a type word inside an avatar name.
- [ ] The `SeleniumPageSource` wait has no wall-clock dependency in tests; a both-fail test pins
      "scrape exception propagates, both failures logged" (backlog B16).
- [ ] The frontend vitest worker pool/timeout hardened (the deferred test-hygiene follow-up in the
      backlog status section).

## M5: Sheet parity, confirmed columns

Slice: the headline metric and the activity columns reach the dashboard end to end.

- [ ] Erzeugter Gildenmehrwert per avatar (deposits minus withdrawals across bank and storage, the
      verified formula in [google-sheet.md](knowledge-base/google-sheet.md)) computed, stored,
      served in the overview JSON and rendered in the SPA; the acceptance net asserts it against
      the synthetic fixture (backlog E1).
- [ ] Last bank/storage activity per avatar surfaced in the overview (backlog E2).
- [ ] Values match the sheet on a real sample (author check).

## M6: Real-browser integration tests

Slice: scraping is exercised by tests on any machine, with no host Firefox.

- [ ] docker-compose provides a `selenium/standalone-firefox` service; an integration test scrapes
      via `RemoteWebDriver`; the devcontainer regains `docker-outside-of-docker` (backlog H2).
- [ ] Server-booting/browser tests split into a Gradle integration-test set; the fast unit loop
      stays fast; [testing.md](knowledge-base/testing.md) updated in the same change (backlog H6).

## M7: Micronaut 5

Slice: the framework moves to the current major without losing 1:1.

- [ ] `./gradlew build` green on Micronaut 5; endpoints 1:1 against the prod snapshot; the offline
      acceptance net green (backlog H9).
- [ ] Precondition: M3-M6 landed (their nets are the safety for this jump).

## Later (unordered; pull between milestones when they fit)

- **Security & config good practice:** real bound `Configuration` (C1), credentials out of the
  image (C3, before any deployment beyond the home server), rate-limiter cleanup
  (C7), `vulnScan` to zero then gated (C8).
- **Schema migrations, then renames:** migration framework (D10) gates the
  `withdrawl` → `withdrawal` key migration (D9); repository unification (D12), exception/logging
  hygiene (D11), catalog refactor (D6); full repackaging (D3) last,
  on top of a tested core.
- **Ops & repo slimming:** drop the bundled webdrivers (F1; the author rewrites history
  afterwards), Dependabot ecosystems plus one refresh pass
  including the pending major PRs (H11).
- **Showcase & workflow:** enforcement hooks (G7, G13), `/commit` command (G8), SessionStart hook
  (G10), agent-environment polish (G11), wildcard-import ban (G17, only between strands),
  KB accuracy sweep and citation guard (G18, G19), BDD tooling
  decision (G4), case study (G5), static-analysis gate (G6).
- **Product growth:** hunt-loot estimate (E3, gated on the D-4 valuation rule), date-range
  reporting (E4), history/time-series (E6), delivery channel (F3), multi-guild (F4), public API (F5).
