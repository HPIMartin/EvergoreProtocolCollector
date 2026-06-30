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
| **MetaInformation / MetaInformationKey** | `businessLogic/metaInformation/*` | Typed key/value store for computed results & the `last_updated` watermark. |

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

`dataExtraction/EvergoreDataEvaluator` is the **aggregation use case**. For each Avatar, since the
stored `last_updated` watermark:

- **Bank:** sum entry `amount` into `placement` (EINLAGERUNG) or `withdrawl` (ENTNAHME), via
  `TransferTypeBankEntryVisitor`.
- **Storage:** for each entry, look up its `EvergoreItem` by `ingameName`
  (unknown name → `UNDEFINED`, logged), then add
  `itemValue × quantity × (quality / 100)` into `placement` / `withdrawl`, where `itemValue` is
  `getStorageValue()` for deposits and `getWithdrawlValue()` for withdrawals
  (`TransferTypeStorageEntryVisitor`). **Quality scales value linearly.**
- Results are written per-Avatar to `MetaInformationRepository` under typed keys
  (`getBankPlacement(avatar)`, `getBankWithdrawl`, `getStoragePlacement`, `getStorageWithdrawl`)
  and the `last_updated` key is advanced to "now".

This maps directly to the Google Sheet's columns 1–4 (see [02-google-sheet.md](google-sheet.md)).
The net **erzeugter Gildenmehrwert** (col 5) is `placement − withdrawl` summed across bank + storage,
derivable but not yet stored as a single metric.

## Identity / equality quirks

- `BankEntry` equality is implemented via a `stupidMerge()` string concatenation (self-described).
  This is a smell; a value-based `equals`/record would be cleaner.
- Repositories' `getNewest()` returns `Optional<BankEntry>` / `Optional<StorageEntry>`, empty when
  the table has no rows. Callers treat `Optional.empty()` as "no watermark; keep every parsed entry".

See [04-architecture.md](architecture.md) for how these types flow through the system, and
[08-glossary.md](glossary.md) for the German terms.
