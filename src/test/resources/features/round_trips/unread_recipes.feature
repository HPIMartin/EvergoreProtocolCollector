@wip
Feature: Products of unread recipe
  Gem-forged gear is crafted from blueprints whose ingredients no page of the game shows. When such
  a product is deposited, nobody can say whether it consumed a trader good the member had withdrawn,
  so the service abstains instead of accusing: that member and good are listed as not judgeable
  ("Nicht beurteilbar (Rezept ungelesen)") on the admin page. All times are German wall-clock time,
  as the game shows them.

  Rule: A product of unread recipe leaves every trader good still open unjudged
    A withdrawal is still open while it lies at most 48 hours back and has not come back or gone
    into a product of published recipe.

    Scenario: An open withdrawal meeting a product of unread recipe is not judged
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand     | Qualität | Vorgang     |
        | 01.01.2026 00:00 | Alrik  | 100   | Kristallat     | 100      | Entnahme    |
        | 01.01.2026 01:00 | Alrik  | 1     | Achat-Armbrust | 100      | Einlagerung |
        | 01.01.2026 02:00 | Alrik  | 100   | Kristallat     | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page lists no suspected round trip
      And the admin page reads "Nicht beurteilbar (Rezept ungelesen): Alrik: Kristallat"

    Scenario: Every trader good still open when the product arrives is left unjudged
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand     | Qualität | Vorgang     |
        | 01.01.2026 00:00 | Alrik  | 100   | Kristallat     | 100      | Entnahme    |
        | 01.01.2026 00:00 | Alrik  | 20    | Federn         | 100      | Entnahme    |
        | 01.01.2026 01:00 | Alrik  | 1     | Achat-Armbrust | 100      | Einlagerung |
        | 01.01.2026 02:00 | Alrik  | 100   | Kristallat     | 100      | Einlagerung |
        | 01.01.2026 02:00 | Alrik  | 20    | Federn         | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page lists no suspected round trip
      And the admin page reads "Nicht beurteilbar (Rezept ungelesen): Alrik: Federn, Alrik: Kristallat"

    Scenario: Abstaining on a good also silences the member's earlier and later round trips of it
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand     | Qualität | Vorgang     |
        | 01.01.2026 00:00 | Alrik  | 100   | Kristallat     | 100      | Entnahme    |
        | 01.01.2026 01:00 | Alrik  | 100   | Kristallat     | 100      | Einlagerung |
        | 10.01.2026 00:00 | Alrik  | 50    | Kristallat     | 100      | Entnahme    |
        | 10.01.2026 01:00 | Alrik  | 1     | Achat-Armbrust | 100      | Einlagerung |
        | 20.01.2026 00:00 | Alrik  | 30    | Kristallat     | 100      | Entnahme    |
        | 20.01.2026 01:00 | Alrik  | 30    | Kristallat     | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page lists no suspected round trip
      And the admin page reads "Nicht beurteilbar (Rezept ungelesen): Alrik: Kristallat"

    Scenario: A product of unread recipe deposited before the withdrawal changes nothing
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand     | Qualität | Vorgang     |
        | 01.01.2026 00:00 | Alrik  | 1     | Achat-Armbrust | 100      | Einlagerung |
        | 01.01.2026 01:00 | Alrik  | 100   | Kristallat     | 100      | Entnahme    |
        | 01.01.2026 02:00 | Alrik  | 100   | Kristallat     | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Verdacht auf Warenkreislauf: Alrik: 100 × Kristallat"
      And the admin page lists no pair as not judgeable

    Scenario: A withdrawal older than 48 hours is no longer open when a product of unread recipe arrives
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand     | Qualität | Vorgang     |
        | 01.01.2026 00:00 | Alrik  | 100   | Kristallat     | 100      | Entnahme    |
        | 03.01.2026 00:01 | Alrik  | 1     | Achat-Armbrust | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page lists no pair as not judgeable

    Scenario: Abstaining on one good leaves the member's other goods judged
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand     | Qualität | Vorgang     |
        | 01.01.2026 00:00 | Alrik  | 100   | Kristallat     | 100      | Entnahme    |
        | 01.01.2026 01:00 | Alrik  | 1     | Achat-Armbrust | 100      | Einlagerung |
        | 01.01.2026 02:00 | Alrik  | 100   | Kristallat     | 100      | Einlagerung |
        | 01.01.2026 03:00 | Alrik  | 50    | Federn         | 100      | Entnahme    |
        | 01.01.2026 04:00 | Alrik  | 50    | Federn         | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Verdacht auf Warenkreislauf: Alrik: 50 × Federn"
      And the admin page reads "Nicht beurteilbar (Rezept ungelesen): Alrik: Kristallat"
