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
- The transfer type is recognized only as a whitespace-delimited token, so the greedy avatar group
  cannot backtrack into a type word inside an avatar name: `… Entnahmefreund Auszahlung`,
  `… Anna Entnahmeübersicht` and `… XX-Entnahme-XX` are unknown types and get dropped instead of
  minting an `Entnahme` by the wrong avatar. A character-class boundary is not enough for this:
  `\b` counts an umlaut as a boundary and a "no letter or digit follows" check counts punctuation
  as one, so either lets a fenced type word through. An avatar name may still contain a type word.
- The recognized type words live once, in `Constants.TRANSFER_TYPE_WORDS`; the headline regex is
  built from that list, so nothing can hold a second, drifting copy of them.

## EvergoreItem: the item catalog

A large enum capturing the game's craftable and gatherable items, a few hundred of them. Each value:

```java
EvergoreItem(String ingameName, int marketValue, Category category, Recipe recipe)
```

- **`ingameName`**: the exact German name as it appears in the scraped protocol (the parser
  matches on this).
- **`marketValue`**: base gold value (Goldwert). For the simple ammunition types it is the price the
  **NPC trader charges**, confirmed by the author 2026-09-10 against the game: `PFEILE` 3, `BOLZEN` 12,
  `MAGIEESSENZ` 4, exactly the catalog's numbers. So a member can buy those for `marketValue` rather
  than craft them, which is what makes a 60 % credit a real loss of gold for whoever buys them.
- **The game prices the catalog, and the wiki is only a convenience.** The guild storage lists what
  it holds per piece at that piece's quality, so a value is read from a holding at quality 100, and a
  stored blueprint carries the gold value of the item it
  makes, which together priced 244 of the catalog's 601 entries when it was last read (backlog
  **B27**: that headline count is still a first-page-only measurement). All 244 agree with the
  catalog. **That read covered only the first page of each storage selection** and so rests
  on 28 of the storage's 43 pages; a paged re-read on 2026-09-12 found **no deviation anywhere** and
  raised the gem-forged names the storage prices from 20 to **99** of the 102 the catalog holds. The
  wiki's `Waren` table is where the numbers originally came from and still fills
  gaps the storage cannot reach, but it is not authoritative: it disagrees with the game on 4 of the
  99 gem-forged values that were checkable against the game when it was read, out of the 102 such
  names the catalog holds (98 entries plus four second spellings), pricing `Quarz-Prunkaxt` at
  `1000` against the game's `12000`, and it
  carries none of the `Mystisch*` quest consumables the ledger holds. Two catalog values are still only
  contested by the wiki and unread from the game, `Luft-Spiralstab` and `Einfacher Wollverband`
  (measured 2026-09-11).
- **It is no authority for spelling, so the name is the game's.** Those six are the raw stones,
  which it writes `Marmorstein`, `Granitstein` and `Schieferstein`, and the three essences, which
  it writes in the plural; all six carry the value it gives them, so only the name differs. The
  game's blueprints, its `Steine` page and the ledger itself say `Marmor`, `Granit` and
  `Schiefer`. A name only the wiki uses matches no ledger row, so it values every movement of a
  real item at zero and says nothing.
- **One item, several spellings.** A name is resolved against the catalog exactly first, then with
  a trailing magic affix (`des`/`der <X>`) stripped. The affix is normalised away because its space is
  open, every base item times every affix, and because the game prices `Streitaxt des Wegelagerers`
  at exactly `Streitaxt`s `1800`. A second **fixed** spelling, such as the ` [2H]` the ledger adds to
  `Obsidian-Pike` but not to `Obsidian-Kriegshammer`, is recorded as an alternative name on the entry
  itself. Deriving it by toggling the suffix instead would price 35 names the catalog has never
  seen, `Kriegshammer` and `Sense` among them, off their two-handed twin and drop them out of the
  unknown-name report (34 against the catalog as it now stands), which is the signal the rest of the catalog work depends on.
- **Two families are catalogued at zero on purpose.** `Übungsstück-*` trains a craft without
  spending materials and sells only to the trader for almost nothing; `Mystisch*` items are quest
  rewards. They are entries rather than gaps so a catalog hole stays distinguishable from a
  deliberate zero, and `/health` counts them apart as `zeroValuedItemCount`/`zeroValuedItemNames`.
- **Gem-forged gear follows a price ladder, which is a finding and not a rule the code applies.**
  Within one gem every one-handed weapon shares a price, as does every staff and every ranged
  weapon, and the armour values repeat across gem families (`Rubin-Plattenhandschuhe` and
  `Jade-Handschuhe` both `14 200`). The ratios between slots are close but not exact, so the
  ladder is never used to invent a value: an item carries the price the game gave it or none
  (measured 2026-09-11).
- **`category`**: one of the `Category` values (weapon/armor families, `ROHSTOFFE`,
  `JAGDBEUTEN` (hunt loot), `EDELSTEINE` (gems), `HANDWERKSMATERIAL`, …). Each category carries
  two multipliers, `placement` (what a deposit credits) and `withdrawl` (what a withdrawal costs),
  and the category alone decides both.
- **`recipe`**: either `Recipe.NOT_CRAFTABLE` (gathered raw item) or a `Recipe(amount, Ingredient…)`
  where each `Ingredient(amount, EvergoreItem)` references other catalog items, and `amount` is
  how many units the recipe yields. **The recipes are the game's production chains, not an input to
  the valuation** (author decision 2026-09-10): no production code reads them, and the tests that
  pin the guild's announced worked examples are their only reader.
- **Gem-forged gear is crafted from a learned blueprint, not from an academy recipe, so the catalog
  holds no ingredients for it.** The academy's craft chambers list **424 blueprints across all 17
  crafts** (`academy_craft&selection=51..67`, read 2026-09-12) and **not one is gem-forged**; the
  blueprints themselves exist as items instead, `Erlernbar ab Stufe 3` or `4`, and reach the guild
  storage, where 20 of the catalog's gem-forged names are priced through one. So the academy, which
  is the one surface that shows a blueprint's ingredients whatever the account's own skill, can
  never show a gem one, and the ingredient list of a gem blueprint is reachable nowhere yet read.
  The catalog nevertheless held recipes for `Achat-Lederbeinlinge` and `Achat-Lederstulpen`, the
  only two of its 98 gem-prefixed entries that did, and both are now `NOT_CRAFTABLE` like the other
  96 (author decision 2026-09-12): an ingredient list no source attests is not kept. No served
  figure moves, because no production code reads a recipe.
- **No catalogued craftable is worth less than the ingredients its recipe consumes**
  (`EvergoreItemTest`): a product priced under its own inputs means one of the two numbers is wrong.
  The two Achat entries were the only pair breaking it, and their ingredient lists were the
  unattested half. The rule reaches the catalog's own production chains and claims nothing about
  gem gear, which now carries none: nine tier-1 gem armour pieces are priced below the `2500` five
  tier-1 gems cost, four of those prices read straight from the game, and whether the game really
  prices them under their inputs cannot be settled until a gem blueprint's ingredients are read.

### The value math (verified against `EvergoreItemTest`)

The rule has two sources, and the difference matters. The **60 % price in both directions** and the
**zero credit for mined, hunted and gem deposits** are the guild's own rule, announced to its members
in 2020; the source document is the author's local copy of those announcements, deliberately outside
the repo (gitignored beside `zugang.txt`), so the rule itself is recorded here. The **100 % credit for
goods bought from the guild trader** and the **60 % credit for boards and bars** are author decisions
of 2026-09-10 that knowingly depart from that announcement, each for a reason given below.

**Withdrawal cost**: what *taking an item out* costs the member, for every category:
```
getWithdrawlValue() = marketValue × category.withdrawl      // withdrawl is 0.6 everywhere
```
- `KUPFERERZ`: 20 × 0.6 = **12** ✓
- `KRISTALL` (gem): 500 × 0.6 = **300** ✓

**Deposit credit**: what *putting an item in* credits the member, decided by its category alone:
```
getStorageValue() = marketValue × category.placement
```

| `placement` | Categories | Source | Why |
|---|---|---|---|
| **0** | `ROHSTOFFE`, `JAGDBEUTEN`, `EDELSTEINE` | announced | Mined or hunted, so they cost the member only time; depositing them **is** the guild's tax, which is what lets the guild run without levying one in gold |
| **1.0** | `HANDWERKSMATERIAL` | **departs from the announcement**, which names only raw materials and gems as exceptions | Bought from the guild trader with the member's own gold, and gold is measured 1:1, so a 60 % credit confiscates 40 % of every purchase and leaves the trader role unable to come out positive however well it haggles |
| **0.6** | everything else | announced | The guild's price for goods, the same in both directions, so moving something out and back is neutral and crafting earns the margin between ingredients and product |
| **0.6** | `VERARBEITETE_ROHSTOFFE` | **departs from the announcement**, which counts boards and bars as raw materials and says their gain is not credited to the character | The announcement gives two reasons for excluding them and the second is a limit of the sheet it was written for ("wir diese aktuell nicht gesondert in unserer Übersicht behandeln"), which no longer applies; their recipes also consume bought trader goods, so a zero credit would take that gold |

- `MAGISCHE_AETHERBINDE` (`BANDAGEN`, market value 257): 257 × 0.6 = **154.2** ✓
- `MAGIESPLITTER` (`HANDWERKSMATERIAL`, market value 60): 60 × 1.0 = **60** ✓
- `EISENBARREN` (`VERARBEITETE_ROHSTOFFE`, market value 120): 120 × 0.6 = **72** ✓

**How the guild's rule 2 is implemented, without a rule of its own.** The announcement lets a member
withdraw crafting material **free** as long as every product comes back. The software has no notion
of a withdrawal belonging to a later deposit, and needs none: charging the withdrawal at 60 % and
crediting the deposit of what it became cancels out, so a crafter who returns the products comes out
at least whole, and better by the craft margin. The condition holds in the other direction too: a
member who withdraws and never deposits keeps the charge, which is what "and only if" asks for. What
the software genuinely cannot see is which of the two happened, so a withdrawal that was a **sale**
rather than an input reads identically to one that will come back.

**Why crafting pays.** A crafting gain is always `0.6 × (the product's market value less its
ingredients')`, because `withdrawl` is 0.6 in every category: an ingredient's credit tier changes
what *depositing* it would earn, never what *withdrawing* it costs. The guild announced that gain
with one worked example, which holds here to the gold:
- 6 `BUCHENHOLZ` + 5 `FEDERN` withdrawn cost 6·12 + 5·15 = 147; the 135 `PFEILE` they craft credit
  135 · 1.8 = 243, a gain of **96**.

A second example, derived here rather than announced, pins the same rule one production step down:
5 `EISENERZ` + 2 `STEINKOHLE` withdrawn cost 5·24 + 2·60 = 240, the 5 `EISENBARREN` credit
5 · 72 = 360, a gain of **120**, larger only because that recipe's margin is larger.

> **Note on hunt loot (`JAGDBEUTEN`):** its deposit credit of 0 is the guild's deliberate tax, not a
> gap. The Google Sheet's "geschätzte Jagdeinlagerungen" column *estimates* what was deposited in
> hunt loot for display; that estimate's formula is still open ([open-questions.md](../open-questions.md), D-4).

**What the guild's position is made of.** The four figures the overview states, and the identity that
makes them checkable against the table's own total row:

```
Gildenbank            = bank deposited - bank withdrawn                  (measured gold)
Gildenlagerwert       = storage credited + donation - craftSubsidy - storage withdrawn
Gildenspende          = donation
Handwerkssubventionen = craftSubsidy
Nach Abzügen (net)    = Gildenbank + Gildenlagerwert - Gildenspende + Handwerkssubventionen
```

The identity holds **exactly in whole gold**, per avatar and in the guild total, for any rounding of
the four stored storage sums, because every derived figure is built from the same already-rounded
values. It holds only where the figures are *known*: the two flows are stored per avatar, so an
avatar the recompute has never reached carries neither, and the guild's `Gildenspende`,
`Handwerkssubventionen` and `Gildenlagerwert` are then **absent for the whole guild** rather than
summed over the avatars that do carry them. That state is reachable and its window is named under
the deploy in [build-run-deploy.md](build-run-deploy.md); the header says it cannot answer, and the
table's total row says the same, so the two never disagree. Measured on the 03.09.2026 snapshot,
42 avatars: `119.334.247`, `25.145.111`, `107.075.238`, `39.441.922`, and a net of `76.846.042`.

> **Why the split loses nothing:** `credited + donation - craftSubsidy` equals the deposit's goods
> value bit-for-bit, over every catalog item at every quality and quantity, because the three credit
> tiers all sit within a factor two of the `withdrawl` rate and the subtraction therefore cancels
> exactly. Measured 2026-09-10: a tier of 0.25 breaks it in 1,494 cases, one of 0.2 in 4,305. A new
> tier far from 0.6 would need that checked again.

> **Where the 100 % credit can be gamed:** a `HANDWERKSMATERIAL` deposit credits 100 % while its
> withdrawal costs 60 %, so cycling the same goods earns 40 % of their value out of nothing.
> Detecting that is filed as its own item; the guild's rule is trust-based, and the software's job is
> to make a breach visible rather than to prevent it. The unknown-item fallback `UNDEFINED` therefore
> sits in `ROHSTOFFE`, the one tier that cannot over-credit: a name the catalog does not know must
> never inherit the trader tier by default.

## How the metrics are computed: `EvergoreDataEvaluator`

`application/EvergoreDataEvaluator` is the **aggregation use case**. Every `evaluateData()` call is
a **full recompute** (design decision 2026-07-17, see [open-questions.md](../open-questions.md)):
per avatar, sums start at **zero** and aggregate over **every stored entry** for that avatar
(`BankRepository.getAllFor(avatar)` / `StorageRepository.getAllFor(avatar)`, no time cutoff), then
**overwrite** the stored meta values (no accumulation onto whatever was there before):

- **Bank:** sum entry `amount` into `placement` (EINLAGERUNG) or `withdrawl` (ENTNAHME), via
  `TransferTypeBankEntryVisitor`.
- **Storage:** for each entry, look up its `EvergoreItem` by name (tried as written against every name
  an entry answers to, then with a magic affix stripped, taking an arbitrary one of the entries that
  carry it, so two may never share a name; unknown name
  → `UNDEFINED`, valued 0, **logged at WARN**; every miss is collected into the `EvaluationResult`
  returned by `evaluateData()` and surfaced via `/health`'s `lastRun` detail as `unknownItemCount` +
  distinct `unknownItemNames`, so a catalog gap is loud, not silent; since evaluation is a full
  recompute, this count is unknown-item rows across **the entire stored history of every avatar the
  run could read**, recomputed each run, not just those new since the previous run; an avatar whose
  ledger read throws reports none, because both ledger adapters materialise their result before
  returning it, so the throw precedes every item lookup; the repository interface does not require
  that), then add `itemValue × quantity × (quality / 100)` into `placement` / `withdrawl`, where
  `itemValue` is `getStorageValue()` for deposits and `getWithdrawlValue()` for withdrawals
  (`TransferTypeStorageEntryVisitor`). **Quality scales value linearly.**
- **A deposit is valued twice**, and the gap is split into the two flows it is made of: into
  `placement` with what it credits the member, and, against `getWithdrawlValue()` as the guild's own
  price for the same goods, into **`donation`** where the credit falls short of that price (mined,
  hunted and gem deposits, which credit nothing) and into **`craftSubsidy`** where it exceeds it
  (trader goods, credited at 100 % of a price the guild values at 60 %). Exactly one of the two can
  be non-zero per deposit. A withdrawal counts into `withdrawl` alone.
- Results are keyed per avatar (`getBankPlacement(avatar)`, `getBankWithdrawl`,
  `getStoragePlacement`, `getStorageWithdrawl`, `getStorageDonation`, `getStorageCraftSubsidy`) and handed to `MetaInformationRepository.add` as
  **one batch for the whole run**, which the adapter writes in **one transaction**: a reader can
  see the state before the recompute or the state after it, never a mixture of both. The run
  computes first and writes last, so the transaction spans the write alone and no reader is blocked
  for the duration of the aggregation.
- **One avatar's failure costs that avatar, not the guild** (decision 2026-09-03): a repository call
  that throws while an avatar is being recomputed is caught per avatar, logged at `error`, and the
  avatar's name is collected into the `EvaluationResult` and surfaced via `/health`'s `lastRun`
  detail as `failedAvatarCount` + `failedAvatarNames`, the same way an unknown item is. That avatar
  keeps its previously stored sums; every healthy avatar still refreshes in the same batch. Writing
  the whole run as one batch would otherwise have widened one bad ledger row from "one avatar goes
  stale" to "no avatar ever updates again", since a single unguarded `timeStamp` dereference in
  `getAllFor(avatar)` throws before the batch is written. Two consequences, both deliberate: the
  guild-wide total then adds a stale contribution to current ones, its two guild-share flows are
  absent so the guild's three modelled figures answer nothing at all until it recomputes, and that
  avatar's row can show a
  **last activity newer than its own sums**, because the activity columns are read live from the
  ledger while the sums come from the last recompute that reached him. Confining that to one row is
  the point: when the evaluator still wrote each avatar immediately, an unreadable row aborted the
  run where it stood, leaving that avatar **and every avatar after him in collation order** stale
  while the earlier ones were already current (falsifier probe against the pre-change code,
  2026-09-03). `/health` names the avatar for the operator, and the row itself carries the second consequence: it
  states that its sums are older than the last collection, and from when they are
  ([frontend.md](frontend.md)).
- The `last_updated` key records **when data was last collected from the game**, not how complete the
  recompute was (author clarification 2026-09-03). It is part of the same batch, derived from the
  same instant the per-avatar recompute keys carry, and is written on **every** run that completed, including one in which
  an avatar failed: a scrape happened either way. It is an operator's datum rather than a per-row
  freshness claim; "how current is this member's row" is answered by that row's own
  `sums_recomputed_at_<avatar>` instant, and "when did the member last move something" by its two
  **last-activity** columns. It is served by `/api/v1/admin/status`, not by the overview.
- The `sums_recomputed_at_<avatar>` key records **when that avatar's stored sums were last
  recomputed**, as **epoch millis**, written in the same batch as his four sums. Epoch millis rather
  than the wall-clock text `last_updated` uses, because that format cannot tell the two Berlin
  fall-back hours apart (backlog **D14**) and a second wall-clock key would double the defect. An
  avatar whose recompute fails keeps the instant of the last run that reached him; one who has no
  such instant yet gets one at the end of the run, seeded from the **newest instant these keys
  already held before that run**, so the key is absent only while no avatar carries one at all.
  "Older than the last collection" is therefore decidable server-side as "older than the newest of
  these keys", with no second run-level key to keep in step.
- **The seed deliberately does not read `last_updated`** (falsifier probe 2026-09-09):
  reconstructing an instant from that wall-clock text resolves the Berlin fall-back hour to the
  earlier of its two passes, which dated a seeded avatar a full hour before the run that actually
  produced his sums and marked him stale against an avatar stamped in that very run. The store
  already holds the previous run exactly, in epoch millis, so the seed reads that instead and the
  comparison never leaves the epoch-millis domain.

This makes evaluation **idempotent** (a second run yields identical sums) and **self-healing per
avatar**: a failing avatar's own sums are withheld and recomputed cleanly on the next run that
reaches him, whatever a prior run wrote. It is not self-healing at the guild level, because the
collection timestamp advances on every completed run while that avatar's sums do not: what the run
did and did not refresh is answered per avatar by the recompute instants, and by name for the
operator in `/health`'s `failedAvatarNames`, never by the timestamp.

This maps directly to the Google Sheet's columns 1–4 (see [02-google-sheet.md](google-sheet.md)).
Column 5, the net **erzeugter Gildenmehrwert**, is **derived on the read side and never stored**
(decision 2026-09-02): a fifth meta key would persist what its four summands already say and drift
from them on any partial recompute.

The read side takes **one `MetaInformationSnapshot` per response** (`MetaInformationRepository.snapshot()`,
one statement over the whole store) and answers every avatar's four sums, the derived net, the
guild-wide total **and** `last_updated` out of it. Together with the recompute's single transaction
this makes the **computed sums** one consistent state of the meta store rather than up to four reads
per avatar that a concurrent recompute could interleave (decision 2026-09-03). There is deliberately
**no** per-key read on the port: the shape that could tear no longer exists. The scope of that
guarantee is the meta store only: the two **last-activity** columns are separate live queries against
the ledger tables (`latestTimestampPerAvatar`), so a ledger write landing between the snapshot and
those queries can show an activity timestamp newer than the sums beside it. The columns are
independent readings by design, and no figure is derived from both.

- `businessLogic/contribution/Contribution` carries the four sums and answers
  `net() = bankDeposited − bankWithdrawn + storageDeposited − storageWithdrawn`, the formula the
  sheet's own column 5 was verified against.
- `Contribution.inWholeGold()` rounds both storage sums to whole gold, and the read surface takes
  the net and the guild total from the **rounded** record, so every served row adds up and a total is
  the exact column sum of its rows (decision 2026-09-02). The unrounded record stays the domain's
  truth; only the read surface rounds. Rounding per avatar rather than once over the whole guild is
  what makes a row addable, at the price of a small deviation from the unrounded truth: up to half a
  gold piece per rounded sum, so up to about a gold on `Gildenlagerwert`, which adds three of them,
  and up to one and a half on the figure before the deductions, which adds the rounded net as well.
  Measured on the 03.09.2026 snapshot, the guild's `Gildenlagerwert` is exactly one gold above the
  unrounded value.
- `AvatarContribution` names the avatar behind one such record; `Contribution.sumOf` adds a
  collection of contributions into the guild's own, which is what the overview's total row shows.
- `businessLogic/contribution/AvatarContributions` assembles one record per **known** avatar
  (`KnownAvatars`, German collation) out of the stored keys, a missing key counting as zero, so an
  avatar who only ever moved items keeps his row.

## Identity / equality quirks

- `BankEntry` and `StorageEntry` are records, so equality is value-based (all fields); this is
  load-bearing for the ingest window dedup (`surplusOverStored` matches scraped vs. stored rows
  by equality).

See [04-architecture.md](architecture.md) for how these types flow through the system, and
[08-glossary.md](glossary.md) for the German terms.
