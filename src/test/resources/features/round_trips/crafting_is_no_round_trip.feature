Feature: Crafting is not a round trip
  A crafter withdraws trader goods, deposits the product they became and later restocks the same
  goods. That is the guild working as intended, not a round trip. The published recipes tell the
  two apart: what a deposited product's recipe consumed is used up, and only what is left of a
  withdrawal can come back as a round trip. Where the recipe of a deposited product has never been
  read, see "Products of unread recipe". All times are German wall-clock time, as the game shows
  them.

  Rule: Trader goods a deposited product's recipe consumed are used up

    Scenario: A crafter who restocks the trader goods their product consumed is not reported
      One batch of 228 "Kriegspfeile" consumes 8 "Eichenholz", 7 "Federn" and 2 "Pfeilharz"; the
      last two are trader goods.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand   | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 8     | Eichenholz   | 100      | Entnahme    |
        | 01.01.2026 12:00 | Alrik  | 7     | Federn       | 100      | Entnahme    |
        | 01.01.2026 12:00 | Alrik  | 2     | Pfeilharz    | 100      | Entnahme    |
        | 01.01.2026 13:00 | Alrik  | 228   | Kriegspfeile | 100      | Einlagerung |
        | 01.01.2026 14:00 | Alrik  | 8     | Eichenholz   | 100      | Einlagerung |
        | 01.01.2026 14:00 | Alrik  | 7     | Federn       | 100      | Einlagerung |
        | 01.01.2026 14:00 | Alrik  | 2     | Pfeilharz    | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page lists no suspected round trip

    Scenario: What a product's recipe did not consume can still come back, each trader good by its own share
      One batch of 100 "Gute Baumwollbinde" consumes 19 "Nähgarn" and 2 "Phasenkraut", so 6 of the
      25 "Nähgarn" and 8 of the 10 "Phasenkraut" withdrawn are left.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand         | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 25    | Nähgarn            | 100      | Entnahme    |
        | 01.01.2026 12:00 | Alrik  | 10    | Phasenkraut        | 100      | Entnahme    |
        | 01.01.2026 13:00 | Alrik  | 100   | Gute Baumwollbinde | 100      | Einlagerung |
        | 01.01.2026 14:00 | Alrik  | 25    | Nähgarn            | 100      | Einlagerung |
        | 01.01.2026 14:00 | Alrik  | 10    | Phasenkraut        | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Verdacht auf Warenkreislauf: Alrik: 6 × Nähgarn, Alrik: 8 × Phasenkraut"

    Scenario: A product deposited in two portions consumes what one deposit of both would
      Five "Eisenbarren" consume 2 "Steinkohle", so two bars consume 0,8, which is rounded up to 1,
      whether deposited at once or one at a time; rounding up each bar on its own would count 2.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 2     | Steinkohle  | 100      | Entnahme    |
        | 01.01.2026 13:00 | Alrik  | 1     | Eisenbarren | 100      | Einlagerung |
        | 01.01.2026 14:00 | Alrik  | 1     | Eisenbarren | 100      | Einlagerung |
        | 01.01.2026 15:00 | Alrik  | 2     | Steinkohle  | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Verdacht auf Warenkreislauf: Alrik: 1 × Steinkohle"

    Scenario: Depositing something the game does not craft consumes nothing
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 100   | Federn     | 100      | Entnahme    |
        | 01.01.2026 13:00 | Alrik  | 10    | Kupfererz  | 100      | Einlagerung |
        | 01.01.2026 14:00 | Alrik  | 100   | Federn     | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Verdacht auf Warenkreislauf: Alrik: 100 × Federn"
