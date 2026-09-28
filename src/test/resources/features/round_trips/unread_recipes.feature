Feature: Products of unread recipe
  Gem-forged gear is crafted from blueprints whose ingredients no page of the game shows. When such
  a product is deposited, nobody can say whether it consumed a trader good the member had withdrawn,
  so the service abstains instead of accusing: that member and good are listed as not judgeable
  ("Nicht beurteilbar (Rezept ungelesen)") on the admin page and in the health report. All times
  are German wall-clock time, as the game shows them.

  Rule: A product of unread recipe leaves every trader good still open unjudged
    A withdrawal is still open while it lies at most 48 hours back and has not come back or gone
    into a product of published recipe.

    Scenario: Every trader good still open when the product arrives is left unjudged
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand     | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 100   | Kristallat     | 100      | Entnahme    |
        | 01.01.2026 12:00 | Alrik  | 20    | Federn         | 100      | Entnahme    |
        | 01.01.2026 13:00 | Alrik  | 1     | Achat-Armbrust | 100      | Einlagerung |
        | 01.01.2026 14:00 | Alrik  | 100   | Kristallat     | 100      | Einlagerung |
        | 01.01.2026 14:00 | Alrik  | 20    | Federn         | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page lists no suspected round trip
      And the admin page reads "Nicht beurteilbar (Rezept ungelesen): Alrik: Federn, Alrik: Kristallat"

    Scenario: Abstaining on a good also silences the member's earlier and later round trips of it
      Each withdrawal lies more than 48 hours after the one before.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand     | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 100   | Kristallat     | 100      | Entnahme    |
        | 01.01.2026 13:00 | Alrik  | 100   | Kristallat     | 100      | Einlagerung |
        | 04.01.2026 12:00 | Alrik  | 50    | Kristallat     | 100      | Entnahme    |
        | 04.01.2026 13:00 | Alrik  | 1     | Achat-Armbrust | 100      | Einlagerung |
        | 07.01.2026 12:00 | Alrik  | 30    | Kristallat     | 100      | Entnahme    |
        | 07.01.2026 13:00 | Alrik  | 30    | Kristallat     | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page lists no suspected round trip
      And the admin page reads "Nicht beurteilbar (Rezept ungelesen): Alrik: Kristallat"

    Scenario: A product of unread recipe deposited before the withdrawal changes nothing
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand     | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 1     | Achat-Armbrust | 100      | Einlagerung |
        | 01.01.2026 13:00 | Alrik  | 100   | Kristallat     | 100      | Entnahme    |
        | 01.01.2026 14:00 | Alrik  | 100   | Kristallat     | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Verdacht auf Warenkreislauf: Alrik: 100 × Kristallat"
      And the admin page lists no pair as not judgeable

    Scenario: A withdrawal older than 48 hours is no longer open when a product of unread recipe arrives
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand     | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 100   | Kristallat     | 100      | Entnahme    |
        | 03.01.2026 12:01 | Alrik  | 1     | Achat-Armbrust | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page lists no pair as not judgeable

    Scenario Outline: Abstaining on one good leaves the member's other goods judged, on <surface>
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand     | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 100   | Kristallat     | 100      | Entnahme    |
        | 01.01.2026 13:00 | Alrik  | 1     | Achat-Armbrust | 100      | Einlagerung |
        | 01.01.2026 14:00 | Alrik  | 100   | Kristallat     | 100      | Einlagerung |
        | 01.01.2026 15:00 | Alrik  | 50    | Federn         | 100      | Entnahme    |
        | 01.01.2026 16:00 | Alrik  | 50    | Federn         | 100      | Einlagerung |
      And the daily collection has run
      When <reading>
      Then <surface> names the suspected round trip "Alrik: 50 × Federn"
      And <surface> names the pair not judgeable "Alrik: Kristallat"

      Examples:
        | surface           | reading                                      |
        | the admin page    | the admin opens the admin page               |
        | the health report | the operator asks the service for its health |
