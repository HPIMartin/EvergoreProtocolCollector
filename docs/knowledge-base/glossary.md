# 08: Glossary (German ↔ English)

The game and all scraped data are German. Code/identifiers are mostly English, with German domain
terms kept verbatim where they're proper nouns (item names, transfer types). Use this when reading
the protocol, the `EvergoreItem` catalog, or the Google Sheet.

## Game & guild

| German | English / meaning |
|--------|-------------------|
| Evergore | The browser game (evergore.de) |
| Avatar | A player's character = a guild member (identified by name) |
| Gilde | Guild |
| Gildenbank / Bank | Guild bank (holds gold) |
| (Gilden)Lager | Guild storage / warehouse (holds items) |
| Welt / Server (z.B. "zyrthania") | Game world / server shard |
| Zugang (`zugang.txt`) | Access / login credentials |
| Impressum | "Imprint": page footer; used by the parser as an end-of-content marker |

## Transactions

| German | English / meaning |
|--------|-------------------|
| Transaktionsbericht | Transaction report: the scraped protocol of bank/storage movements |
| Einlagerung | Deposit **into storage** (`TransferType.EINLAGERUNG`, "place") |
| Entnahme | Withdrawal **from storage** (`TransferType.ENTNAHME`) |
| Einzahlung | Deposit **into the bank** (gold) |
| Auszahlung | Withdrawal **from the bank** (gold) |
| erzeugter Gildenmehrwert | "Generated guild added value": a member's net contribution (deposits − withdrawals) |
| letzte Lager-/Bankaktivität | Last storage / bank activity timestamp |
| geschätzte Jagdeinlagerungen | "Estimated hunt deposits": estimated value of hunt-loot deposited |

## Item catalog (`EvergoreItem.Category`)

| German | English / meaning |
|--------|-------------------|
| Goldwert / Marktwert (`marketValue`) | Gold / market value of an item |
| Rezept (`Recipe`) / Zutat (`Ingredient`) | Crafting recipe / ingredient |
| Handwerksmaterial | Crafting material |
| Rohstoffe | Raw resources (ores, wood, stone) |
| verarbeitete Rohstoffe | Processed resources (bars, planks, cloth, bricks) |
| Jagdbeuten | Hunt loot (pelts, dusts) |
| Edelsteine | Gems |
| Bandagen | Bandages |
| Bauteile | Building parts |
| Armbrüste / Bögen / Dolche / Keulen / Äxte / Schwerter / Stangenwaffen | Crossbows / bows / daggers / clubs / axes / swords / polearms |
| Erd-/Feuer-/Wasser-/Luftstäbe | Earth/fire/water/air (magic) staves |
| leichte Rüstung (Leder/Stoff) | Light armor (leather / cloth) |
| schwere Rüstung (Metall) | Heavy armor (metal) |
| Schilde | Shields |
| Munition | Ammunition |
| `[2H]` | Two-handed weapon variant |
| Qualität (quality) | Item quality %, scales its value linearly |

## Common item words (appear inside names)

Beinlinge=greaves · Handschuhe=gloves · Harnisch=cuirass · Haube=hood · Helm=helmet ·
Hose=trousers · Schuhe=shoes · Stiefel=boots · Stulpen=bracers · Wappenrock=tabard ·
Barren=ingot · Bretter=planks · Ziegel=bricks · Tuch=cloth · Leder=leather.
