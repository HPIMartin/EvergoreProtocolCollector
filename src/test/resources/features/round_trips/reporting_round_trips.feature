@wip
Feature: How a suspected round trip is reported
  A suspected round trip is a note for the admin on the admin page and for the operator in the
  health report, listed by member in German alphabetical order (see "The health report: what the
  last recompute found"). It changes nobody's figures: the credit the round trip earned stays in
  the member's figures until someone in the guild deals with it in the game.

  Rule: A reported round trip changes nobody's figures
    The admin page reports the round trip below as "Alrik: 1 × Federn" (see "Suspected round trips
    of trader goods").

    Scenario: A reported round trip leaves the member's figures as they are
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 1     | Federn     | 100      | Entnahme    |
        | 01.01.2026 13:00 | Alrik  | 1     | Federn     | 100      | Einlagerung |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Entnahme | Nach Abzügen |
        | Alrik  | 25          | 15       | 10           |
