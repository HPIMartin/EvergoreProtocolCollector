# 02: The Google Sheet (the thing being replaced)

- **Source:** the guild's Google Sheet (CSV export, reverse-engineered). Snapshot: **July 2022**.
- **Document ID:** kept out of the repo (local notes, alongside the Evergore credentials).
- **Reading the sheet:** use the gviz CSV endpoint
  `https://docs.google.com/spreadsheets/d/<DOC_ID>/gviz/tq?tqx=out:csv&gid=<GID>`. The plain
  `/export?format=csv` redirect expires first; gviz is reliable.
- The CSV export only sees the one visible tab; the workbook may have more (see *Unknowns* below).

## What the sheet is

- A **guild contribution dashboard**: one row per guild member (Avatar).
- A "Datum von / bis" (date from/to) range at the top defines the time window; snapshot covers
  up to 10.07.2022.
- The guild tag visible in the sheet is **`[Boten]`**.

## Column model (reverse-engineered)

Each row: avatar name plus 11 value columns. Headers are merged/German; mapping below derived by
**checking arithmetic against data** (confidence noted).

| Col | Header group | Meaning | Sign | Confidence |
|----:|--------------|---------|------|------------|
| 0 | (none) | **Avatar** (member name) | (n/a) | certain |
| 1 | Transaktionsbericht | **Bank-Einzahlung**: gold paid *into* the guild bank | + | high |
| 2 | Transaktionsbericht | **Bank-Auszahlung**: gold taken *out* of the bank | − | high |
| 3 | Transaktionsbericht | **Einlagerung**: gold-value of items deposited to storage | + | high |
| 4 | Transaktionsbericht | **Entnahme**: gold-value of items withdrawn from storage | − | high |
| 5 | erzeugter Gildenmehrwert | **Net guild value generated = col1 + col2 + col3 + col4** | ± | **verified** |
| 6 | geschätzte Jagdeinlagerungen | **Estimated value of hunt-loot deposited to storage** | + | medium |
| 7 | (under Jagdeinlagerungen) | **% of storage deposits that are hunt loot** ≈ `col6 / col3`, capped 100% | % | medium |
| 8 | (under Jagdeinlagerungen) | A second percentage; exact formula unconfirmed | % | **low / open** |
| 9 | (under Jagdeinlagerungen) | A small count (mostly 0; one member = 1); meaning unconfirmed | int | **low / open** |
| 10 | letzte Lageraktivität | **Last storage activity** timestamp (`dd.MM HH:mm`) | (n/a) | high |
| 11 | letzte Bankaktivität | **Last bank activity** timestamp (empty if member never used the bank) | (n/a) | high |

### Verification of the core formula (col5)

`erzeugter Gildenmehrwert = Bank-Einzahlung + Bank-Auszahlung + Einlagerung + Entnahme`
(withdrawals negative: deposits minus withdrawals). Checked against rows:

- **Alessia:** 57 938 + 0 + 45 120 − 200 608 = **−97 550** ✓
- **Bambor:** 58 410 + 0 + 169 254 − 226 494 = **1 170** ✓
- **Evildead:** 45 217 + 0 + 292 118 − 239 370 = **97 965** ✓
- **Fugger:** 0 − 247 053 + 1 171 710 − 1 978 211 = **−1 053 554** ✓
- **Aargh** (storage-only, no bank): 0 + 0 + 44 208 − 44 208 = **0** ✓ (and col11 is empty)

### Verification of col7

`col7 ≈ col6 / col3` (capped 100%):

- **Bambor:** 92 082 / 169 254 = **54%** ✓
- **Evildead:** 151 792 / 292 118 = **52%** ✓
- **Aargh:** 19 314 / 44 208 = **44%** ✓

## Sheet ↔ software mapping (the gap)

| Sheet concept | Software status |
|---------------|-----------------|
| Bank-Einzahlung / -Auszahlung (col1/2) | ✅ Computed by `EvergoreDataEvaluator` (bank placement / withdrawl), stored in `MetaInformation`, shown in `/overview`. |
| Entnahme value (col4) | ✅ Computed by `EvergoreDataEvaluator` (storage withdrawl, quality-scaled), stored in `MetaInformation`, served as `storageWithdrawn`, on the sheet's own rule of 60 % of market value. |
| Einlagerung value (col3) | ⚠️ Computed and served as `storageDeposited`, on the same category rule the sheet was built on, with **two deliberate divergences** (author decisions 2026-09-10, see [open-questions.md](../open-questions.md)): goods bought from the guild trader (`HANDWERKSMATERIAL`) credit 100 % of market value rather than 60 %, so a trader's own gold is not confiscated, and boards and bars (`VERARBEITETE_ROHSTOFFE`) credit 60 % rather than nothing, because the sheet excluded them for want of treating them separately. The rule itself is in [domain-model.md](domain-model.md). |
| erzeugter Gildenmehrwert (col5) | ✅ Derived per request by `Contribution.net()` over the four sums and served as `net`, shown as *Nach Abzügen*; stored nowhere, so it cannot drift from its summands. |
| (no sheet column) | ➕ `donation` and `craftSubsidy`, what a member gave for nothing and what the guild credited above its own price, plus the *Vor Abzügen* figure the overview switches to. The sheet had no column for either: together they are the gap its col5 silently carried. |
| geschätzte Jagdeinlagerungen + % (col6/7/8) | ❌ Not implemented. `EvergoreItem` *has* a `JAGDBEUTEN` (hunt-loot) category, so the data exists to compute it. |
| count (col9) | ❌ Meaning unknown; not implemented. |
| letzte Lager-/Bankaktivität (col10/11) | ✅ Queried from the ledger rows per avatar (`latestTimestampPerAvatar`) and served as `lastStorageActivity` / `lastBankActivity`; `null` for a ledger the avatar never used, the case the sheet leaves blank. |
| Date-range filter (Datum von/bis) | ❌ Software recomputes sums from all stored entries each run; no arbitrary date-range reporting yet. |

**Bottom line:** software reproduces the sheet's columns 1, 2, 4, 5 and 10/11 exactly, per avatar
and as a guild-wide total row, and column 3 on the sheet's own rule save the two divergences named
above. Full parity = hunt-loot estimates (col6/7/8), the still unexplained count (col9) and
date-range queries.

## Unknowns to confirm with the author

1. Exact meaning of **col8** and **col9**.
2. **Whether the 2022 sheet still credited hunt loot in col3.** The guild's 2020 rule says raw
   materials and gems credit nothing, yet `Aargh`'s row deposits and withdraws the same `44 208`
   for a col5 of exactly 0 while col6 puts 44 % of those deposits in hunt loot, which only adds up
   if his hunt-loot deposits carried value. Either the sheet drifted from the announced rule, or
   col6 estimates a share of something other than col3, which is the other half of **D-4**.
3. Are there **other tabs** in the workbook (raw protocol, item price list, per-month history)?
4. Are the **item gold values** in `EvergoreItem` the source of truth, or were sheet values
   maintained separately (and possibly drifted)?
