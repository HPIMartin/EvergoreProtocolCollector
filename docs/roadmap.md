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
| M2 | JSON API (strand `json-api`) | E5 | Unblocks the SPA; reworks the token filter and SPA fallback in one place |
| M3 | Dashboard views (strand `spa-views`) | E5 | The officer-visible payoff of the dashboard rebuild |
| M4 | Ingest & test robustness | B16, B17 | Small hardening set, deliberately behind `json-api` (decision 2026-07-17) |
| M5 | Sheet parity, confirmed columns | E1, E2 | The headline metric lands on integrity-fixed data and a real dashboard |
| M6 | Real-browser integration tests | H2, H6 | Scrape coverage without host Firefox; the test split gates on the Selenium service |
| M7 | Micronaut 5 | H9 | Deferred deliberately until 1:1 is re-proven through the nets built in M2-M6 |

## M2: JSON API (strand `json-api`)

Slice: dashboard data is served as JSON under a token-protected `/api/**`; the static SPA surface
is public.

- [ ] Token-filter scope inverted to `/api/**` (decision 2026-07-04): tests prove a token is
      required there and not for `/`, `/index.html`, `/assets/**`, `/swagger/**`.
- [ ] The public no-token/no-rate-limit/no-audit surface is documented and accepted
      (open-questions entry).
- [ ] Overview and per-avatar bank/storage data served as JSON; the offline acceptance net asserts
      the overview JSON against the synthetic fixture (sibling to `ProtocolEvaluationAcceptanceTest`).
- [ ] SPA fallback fixed both ways (trailing-extension match: a dotless missing asset 404s, a
      dotted client route gets the shell) and static-path matching canonicalizes first; tests both
      ways (the deferred `json-api` follow-ups in the backlog status section).
- [ ] `./gradlew build` green including the frontend `check`.

Track: worktree strand `json-api`.

## M3: Dashboard views (strand `spa-views`)

Slice: a guild officer reads the overview and avatar details in the SPA instead of the
HTML-string templates.

- [ ] Sortable overview table plus avatar bank/storage views, served by the real service on the
      existing routes (`/overview`, `/avatars/{avatar}/bank`, `/avatars/{avatar}/storage`).
- [ ] Every view TDD-built with Vitest/RTL (`data-testid`, semantic tables); the frontend layer
      rules hold at lint time ([frontend.md](knowledge-base/frontend.md)).
- [ ] Visual style recreated without copying game assets (decision 2026-07-04).
- [ ] All columns the service already stores are visible; the full sheet handover completes with M5.

Track: worktree strand `spa-views`.

## M4: Ingest & test robustness

Slice: no single malformed protocol line aborts an ingest; the suite carries no wall-clock
dependency.

- [ ] `parseItems` guards non-numeric amounts (skip and log); tests cover the empty-amount line
      and the single-digit-headline absorption case (backlog B17).
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
- [ ] Precondition: M2-M6 landed (their nets are the safety for this jump).

## Later (unordered; pull between milestones when they fit)

- **Security & config good practice:** real bound `Configuration` (C1), credentials out of the
  image (C3, before any deployment beyond the home server), client-error log noise (C9), rate-limiter cleanup
  (C7), `vulnScan` to zero then gated (C8).
- **Schema migrations, then renames:** migration framework (D10) gates the
  `withdrawl` → `withdrawal` key migration (D9); repository unification (D12), exception/logging
  hygiene (D11), inward-only core rule (D13), catalog refactor (D6); full repackaging (D3) last,
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
