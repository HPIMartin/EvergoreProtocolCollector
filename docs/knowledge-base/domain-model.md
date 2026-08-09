# 03: Domain Model

All domain types live framework-free under `…/domain` and `…/businessLogic`. (`…` =
`src/main/java/dev/schoenberg/evergore/protocolParser`.)

## Core concepts

| Concept | Type | Notes |
|---------|------|-------|
| **Avatar** | `String` (a member's name) | Not a class; identified by name throughout. |
| **TransferType** | enum `businessLogic/base/TransferType` | `EINLAGERUNG` (deposit / "place") or `ENTNAHME` (withdrawal). Uses a **visitor** (`TransferTypeVisitor<T>` with `place()` / `withdrawl()`) instead of `switch`/`instanceof`. |
| **Entry** | record `domain/Entry(avatar, Instant date, List<Item> items, TransferType type)` | A single parsed protocol line group: who did what, when, in which direction. |
| **Item** | record `domain/Item(int quantity, String name, int quality)` | A raw parsed item line (name as scraped, quality %, quantity). |
| **BankEntry** | `businessLogic/banking/BankEntry(date, avatar, int amount, TransferType)` | A bank (gold) transaction. |
| **StorageEntry** | `businessLogic/storage/StorageEntry(date, avatar, quantity, name, quality, TransferType)` | A storage (item) transaction. |
| **EvergoreItem** | enum `domain/EvergoreItem` | The **item catalog** (see below). The heart of the value logic. |
| **MetaInformation / MetaInformationKey** | `businessLogic/metaInformation/*` | Typed key/value store for computed results & the display-only `last_updated` timestamp. |

## Parser characterization pins (author-confirmed)

- A headline of type `Einzahlung` parses to `TransferType.EINLAGERUNG`, same as `Einlagerung`:
  `Einzahlung` occurs in real protocols but warrants no distinct behavior (decision 2026-07-17,
  see [open-questions.md](../open-questions.md)).
- The parser strips a trailing `+1` off an item line before dedup, so `3 X +1` and `5 X` merge into
  one `Item(8, "X", 100)`: a `+1` item is value-equal to its base, so merging loses nothing
  (decision 2026-07-17).
- An item line whose amount or quality is no parseable number (empty, or beyond `int`) is skipped
  and logged, never thrown: a single corrupt line must not abort the whole ingest.
- A recognized entry whose item lines all fail the item regex (e.g. a thousands separator,
  `1.000 Gold`) is kept and logged, not dropped: it contributes nothing downstream, so the warning
  is the only trace it leaves.
- Every dropped block head is logged, whichever way it failed (unknown transfer type vs. malformed
  headline): a block head loud enough to open an entry must never vanish silently.

## EvergoreItem: the item catalog

A large enum (~600 entries) capturing the game's craftable + gatherable items. Each value:

```java
EvergoreItem(String ingameName, int marketValue, Category category, Recipe recipe)
```

- **`ingameName`**: the exact German name as it appears in the scraped protocol (the parser
  matches on this).
- **`marketValue`**: base gold value (Goldwert).
- **`category`**: one of ~29 `Category` values (weapon/armor families, `ROHSTOFFE`,
  `JAGDBEUTEN` (hunt loot), `EDELSTEINE` (gems), `HANDWERKSMATERIAL`, …). Each category carries
  one multiplier: `withdrawl`.
- **`recipe`**: either `Recipe.NOT_CRAFTABLE` (gathered raw item) or a `Recipe(amount, Ingredient…)`
  where each `Ingredient(amount, EvergoreItem)` references other catalog items, and `amount` is
  how many units the recipe yields.

### The value math (verified against `EvergoreItemTest`)

**Withdrawal value**: what *taking an item out* is worth:
```
getWithdrawlValue() = marketValue × category.withdrawl      // withdrawl multiplier is 0.6 for all categories
```
- `KUPFERERZ`: 20 × 0.6 = **12** ✓
- `KRISTALL` (gem): 500 × 0.6 = **300** ✓

**Storage (deposit) value**: what *depositing an item* is worth:
```
getStorageValue() = getRecipeStorageValue() / recipe.amount
getRecipeStorageValue() = 0                          if NOT_CRAFTABLE
                        = Σ ingredient.amount × ingredient.item.getWithdrawlValue()   otherwise
```
- Raw/gathered items (`NOT_CRAFTABLE`) have **storage value 0**: depositing raw mats counts as zero contribution.
- A crafted item is valued at the **withdrawal-cost of its ingredients, per unit produced**.
- `MAGISCHE_AETHERBINDE` (recipe yields 100; ingredients 14·Äthertuch + 7·Drachenleder +
  37·Nähgarn + 17·Phasenkraut + 3·Erdenblut): (3024+1512+1776+2040+900)/100 = **92.52** ✓

> **Note on hunt loot (`JAGDBEUTEN`):** all `JAGDBEUTEN` items are `NOT_CRAFTABLE`, so their
> storage value is 0: depositing them counts as zero contribution. The Google Sheet has a
> "geschätzte Jagdeinlagerungen" (estimated hunt-deposit value) column that values hunt loot
> differently; that is a semantic gap between this service and the sheet, tracked separately.

## How the metrics are computed: `EvergoreDataEvaluator`

`application/EvergoreDataEvaluator` is the **aggregation use case**. Every `evaluateData()` call is
a **full recompute** (design decision 2026-07-17, see [open-questions.md](../open-questions.md)):
per avatar, sums start at **zero** and aggregate over **every stored entry** for that avatar
(`BankRepository.getAllFor(avatar)` / `StorageRepository.getAllFor(avatar)`, no time cutoff), then
**overwrite** the stored meta values (no accumulation onto whatever was there before):

- **Bank:** sum entry `amount` into `placement` (EINLAGERUNG) or `withdrawl` (ENTNAHME), via
  `TransferTypeBankEntryVisitor`.
- **Storage:** for each entry, look up its `EvergoreItem` by `ingameName`
  (unknown name → `UNDEFINED`, valued 0, **logged at WARN**; every miss is collected into the
  `EvaluationResult` returned by `evaluateData()` and surfaced via `/health`'s `lastRun` detail as
  `unknownItemCount` + distinct `unknownItemNames`, so a catalog gap is loud, not silent; since
  evaluation is a full recompute, this count is unknown-item rows across **the entire stored
  history**, recomputed each run, not just those new since the previous run), then add
  `itemValue × quantity × (quality / 100)` into `placement` / `withdrawl`, where `itemValue` is
  `getStorageValue()` for deposits and `getWithdrawlValue()` for withdrawals
  (`TransferTypeStorageEntryVisitor`). **Quality scales value linearly.**
- Results are written per-Avatar to `MetaInformationRepository` under typed keys
  (`getBankPlacement(avatar)`, `getBankWithdrawl`, `getStoragePlacement`, `getStorageWithdrawl`).
- The `last_updated` key is **display-only** (the overview's "last updated" timestamp): written
  **once**, after every avatar updated successfully, as `LocalDateTime.now(clock)`; a failed run
  (an avatar's repository call throws) writes nothing, so a retry starts clean and self-heals.

This makes evaluation **idempotent** (a second run yields identical sums) and **self-healing**
(a failed run never leaves a partial watermark advance behind; the next successful run recomputes
correctly from the stored entries regardless of what a prior failed run wrote).

This maps directly to the Google Sheet's columns 1–4 (see [02-google-sheet.md](google-sheet.md)).
The net **erzeugter Gildenmehrwert** (col 5) is `placement − withdrawl` summed across bank + storage,
derivable but not yet stored as a single metric.

## Identity / equality quirks

- `BankEntry` and `StorageEntry` are records, so equality is value-based (all fields); this is
  load-bearing for the ingest window dedup (`surplusOverStored` matches scraped vs. stored rows
  by equality).

See [04-architecture.md](architecture.md) for how these types flow through the system, and
[08-glossary.md](glossary.md) for the German terms.
