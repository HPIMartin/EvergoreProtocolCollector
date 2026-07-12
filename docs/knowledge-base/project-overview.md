# 01: Project Overview

## The game

- **Evergore** (evergore.de): German-language, browser-based text/strategy MMO.
- Players control an **Avatar**; Avatars join a **Gilde** (guild).
- A guild shares two communal stores:
  - **Gildenbank**: in-game currency (gold); members pay in / take out.
  - **Gildenlager** ("Lager" = warehouse/storage): **items** (raw materials, hunt loot, crafted
    goods, gems, equipment …); members deposit and withdraw.
- Every movement in either store is recorded in a **Transaktionsbericht** (transaction report /
  protocol), viewable as paginated web pages when logged in.

## The problem this project solves

- A guild officer wants to know **who is pulling their weight**: each member's net contribution
  to the guild's shared wealth over time. The game shows raw transaction logs but no aggregation,
  and items have no obvious gold value in the log.
- Historically tracked **by hand in a Google Sheet** (see [02-google-sheet.md](google-sheet.md)):
  someone read the protocol, valued each deposited/withdrawn item, summed it per member. Tedious,
  error-prone, always out of date.
- **This software automates that sheet:**
  1. Logs into Evergore and scrapes the bank + storage protocol pages (Selenium).
  2. Parses each protocol entry into a structured transaction (who, when, what, how much, in/out).
  3. Values item movements via a built-in item catalog (`EvergoreItem`) with gold values +
     crafting recipes.
  4. Aggregates per Avatar: total paid into the bank, taken out, value deposited to storage,
     value withdrawn, and from those the member's **erzeugter Gildenmehrwert** ("generated guild
     value").
  5. Persists results to SQLite; exposes a small token-protected HTML/REST view.

## Who it's for

- **Primary user:** the guild officer / the project author (a solo developer).
- **Audience of the output:** guild leadership deciding on member standing.
- **Scale:** tiny (one guild, dozens of members, a handful of HTTP requests), not high-traffic;
  correctness and maintainability matter more than performance.

## Project intent (per the author)

- Current effort (on `main`) is a **clean-up / rebuild** of a codebase written across several
  restarts with loose discipline, aiming for: **clean code, TDD, BDD (product-owner
  perspective), SOLID, hexagonal architecture.**
- This knowledge base + backlog give that rebuild a durable, shared plan.

## Open questions that shape scope

See [../open-questions.md](../open-questions.md). Biggest unknowns, driving backlog priority:

- Is the game/guild still active (production vs. learning project)?
- How far does "replace the sheet" extend (data collector vs. full dashboard with feature parity)?
