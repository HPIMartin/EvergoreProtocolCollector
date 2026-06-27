# 02 — The Google Sheet (the thing being replaced)

Source: the guild's Google Sheet — the document ID is intentionally kept out of the repo
(it lives in local notes alongside the Evergore credentials). It was reverse-engineered via the
CSV export. Data snapshot is from **July 2022**.

**Reading the sheet (how to pull it):** use the gviz CSV endpoint
`https://docs.google.com/spreadsheets/d/<DOC_ID>/gviz/tq?tqx=out:csv&gid=<GID>` — the plain
`/export?format=csv` redirect expires before it can be followed, so gviz is the reliable form. The
CSV export only sees the one visible tab; the workbook may have more (see *Unknowns* below).

## What the sheet is

A per-Avatar **guild contribution dashboard**. One row per guild member, with a "Datum von / bis"
(date from / to) range at the top — so the whole sheet represents contributions **over a chosen
time window** (the snapshot covers everything up to 10.07.2022).

The guild tag visible in the sheet is **`[Boten]`**.

## Column model (reverse-engineered)

Each member row has the avatar name plus 11 value columns. Headers are merged/German, so the
mapping below was derived by **checking the arithmetic against the data** (confidence noted).

| Col | Header group | Meaning | Sign | Confidence |
|----:|--------------|---------|------|------------|
| 0 | — | **Avatar** (member name) | — | certain |
| 1 | Transaktionsbericht | **Bank-Einzahlung** — gold paid *into* the guild bank | + | high |
| 2 | Transaktionsbericht | **Bank-Auszahlung** — gold taken *out* of the bank | − | high |
| 3 | Transaktionsbericht | **Einlagerung** — gold-value of items deposited to storage | + | high |
| 4 | Transaktionsbericht | **Entnahme** — gold-value of items withdrawn from storage | − | high |
| 5 | erzeugter Gildenmehrwert | **Net guild value generated = col1 + col2 + col3 + col4** | ± | **verified** |
| 6 | geschätzte Jagdeinlagerungen | **Estimated value of hunt-loot deposited to storage** | + | medium |
| 7 | (under Jagdeinlagerungen) | **% of storage deposits that are hunt loot** ≈ `col6 / col3`, capped 100% | % | medium |
| 8 | (under Jagdeinlagerungen) | A second percentage — exact formula unconfirmed | % | **low / open** |
| 9 | (under Jagdeinlagerungen) | A small count (mostly 0; one member = 1) — meaning unconfirmed | int | **low / open** |
| 10 | letzte Lageraktivität | **Last storage activity** timestamp (`dd.MM HH:mm`) | — | high |
| 11 | letzte Bankaktivität | **Last bank activity** timestamp (empty if member never used the bank) | — | high |

### Verification of the core formula (col5)

`erzeugter Gildenmehrwert = Bank-Einzahlung + Bank-Auszahlung + Einlagerung + Entnahme`
(withdrawals are negative, so this is *deposits minus withdrawals*). Checked against rows:

- **Alessia:** 57 938 + 0 + 45 120 − 200 608 = **−97 550** ✓
- **Bambor:** 58 410 + 0 + 169 254 − 226 494 = **1 170** ✓
- **Evildead:** 45 217 + 0 + 292 118 − 239 370 = **97 965** ✓
- **Fugger:** 0 − 247 053 + 1 171 710 − 1 978 211 = **−1 053 554** ✓
- **Aargh** (storage-only, no bank): 0 + 0 + 44 208 − 44 208 = **0** ✓ (and col11 is empty)

### Verification of col7

`col7 ≈ col6 / col3`: Bambor 92 082 / 169 254 = 54% ✓, Evildead 151 792 / 292 118 = 52% ✓,
Aargh 19 314 / 44 208 = 44% ✓. (Capped at 100% where hunt-loot estimate exceeds deposits.)

## Sheet ↔ software mapping (the gap)

| Sheet concept | Software status |
|---------------|-----------------|
| Bank-Einzahlung / -Auszahlung (col1/2) | ✅ Computed by `EvergoreDataEvaluator` (bank placement / withdrawl), stored in `MetaInformation`, shown in `/overview`. |
| Einlagerung / Entnahme value (col3/4) | 🟡 **In progress** — this is exactly the uncommitted feature (storage value calc in `EvergoreDataEvaluator` + `EvergoreItem.getStorageValue/getWithdrawlValue`). Not yet shown in UI. |
| erzeugter Gildenmehrwert (col5) | ❌ Not yet computed as a single net metric (would be a trivial sum once col3/4 land). |
| geschätzte Jagdeinlagerungen + % (col6/7/8) | ❌ Not implemented. `EvergoreItem` *has* a `JAGDBEUTEN` (hunt-loot) category, so the data exists to compute it. |
| count (col9) | ❌ Meaning unknown; not implemented. |
| letzte Lager-/Bankaktivität (col10/11) | 🟡 Per-entry timestamps are stored; a "last activity per avatar" is derivable but not surfaced as such. |
| Date-range filter (Datum von/bis) | ❌ Software accumulates from a `last_updated` watermark; no arbitrary date-range reporting yet. |

**Bottom line:** the software currently reproduces roughly the left third of the sheet (bank
totals) and is mid-way through the storage-value third. Full sheet parity = remaining storage
metrics + Gildenmehrwert + hunt-loot estimates + last-activity surfacing + date-range queries.

## Unknowns to confirm with the author

1. Exact meaning of **col8** and **col9**.
2. Are there **other tabs** in the workbook (raw protocol, item price list, per-month history)?
   The CSV export only sees the one tab; the workbook may have more.
3. Are the **item gold values** in `EvergoreItem` the source of truth, or were sheet values
   maintained separately (and possibly drifted)?
