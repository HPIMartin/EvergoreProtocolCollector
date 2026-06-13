# 01 — Project Overview

## The game

**Evergore** (evergore.de) is a German-language, browser-based text/strategy MMO. Players
control an **Avatar**. Avatars join a **Gilde** (guild). A guild shares two communal stores:

- **Gildenbank** — holds the in-game currency (gold). Members pay in / take out.
- **Gildenlager** ("Lager" = warehouse/storage) — holds **items** (raw materials, hunt loot,
  crafted goods, gems, equipment …). Members deposit and withdraw items.

Every movement in either store is recorded by the game in a **Transaktionsbericht**
(transaction report / protocol), viewable as paginated web pages when logged in.

## The problem this project solves

A guild officer wants to know **who is pulling their weight** — i.e. each member's net
contribution to the guild's shared wealth over time. The game shows raw transaction logs
but does no aggregation, and items have no obvious gold value attached in the log.

Historically this was tracked **by hand in a Google Sheet** (see [02-google-sheet.md](google-sheet.md)):
someone read the protocol, valued each deposited/withdrawn item, and summed it per member.
Tedious, error-prone, and always out of date.

**This software automates that sheet.** It:

1. Logs into Evergore and scrapes the bank + storage protocol pages (Selenium).
2. Parses each protocol entry into a structured transaction (who, when, what, how much, in/out).
3. Values item movements using a built-in item catalog (`EvergoreItem`) with gold values + crafting recipes.
4. Aggregates per Avatar: total paid into the bank, taken out, value deposited to storage,
   value withdrawn — and from those, the member's **erzeugter Gildenmehrwert** ("generated guild value").
5. Persists results to SQLite and exposes a small token-protected HTML/REST view.

## Who it's for

- **Primary user:** the guild officer / the project author (a solo developer).
- **Audience of the output:** guild leadership deciding on member standing.
- Scale is tiny: one guild, dozens of members, a handful of HTTP requests. This is **not** a
  high-traffic system; correctness and maintainability matter far more than performance.

## Project intent (per the author)

- The codebase was written across several restarts with admittedly loose discipline.
- The current effort (branch `Rebuild`) is a **clean-up / rebuild** aiming for:
  **clean code, TDD, BDD (from a product-owner perspective), SOLID, and hexagonal architecture.**
- This knowledge base + backlog were created to give that rebuild a durable, shared plan.

## Open questions that shape scope

See [../open-questions.md](../open-questions.md). The biggest unknowns: is the game/guild still
active (production vs. learning project), and how far the "replace the sheet" goal extends
(data collector vs. full dashboard with feature parity). These drive backlog priority.
