@wip
Feature: How a suspected round trip is reported
  A suspected round trip is a note for the admin on the admin page and for the operator in the
  health report, listed by member. It changes nobody's figures: the credit the round trip earned
  stays in the member's figures until someone in the guild deals with it in the game.

  Rule: A reported round trip changes nobody's figures

    Scenario: A reported round trip leaves the member's figures as they are
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 00:00 | Alrik  | 10    | Federn     | 100      | Entnahme    |
        | 01.01.2026 01:00 | Alrik  | 10    | Federn     | 100      | Einlagerung |
      When the daily collection runs
      Then the admin page reads "Verdacht auf Warenkreislauf: Alrik: 10 × Federn"
      And the overview shows:
        | Avatar | Einlagerung | Entnahme | Nach Abzügen |
        | Alrik  | 250         | 150      | 100          |

  Rule: Both reports list suspected round trips and pairs not judgeable in German alphabetical order of the members
    Today the admin page and the health report list them letter by letter with umlauts after "z",
    so "Ärger" follows "Zorn", unlike the overview. The scenarios below state the corrected
    behavior.

    @wip
    Scenario: The suspected round trips are listed in German alphabetical order of the members
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 00:00 | Zorn   | 10    | Federn     | 100      | Entnahme    |
        | 01.01.2026 01:00 | Zorn   | 10    | Federn     | 100      | Einlagerung |
        | 01.01.2026 02:00 | Ärger  | 10    | Federn     | 100      | Entnahme    |
        | 01.01.2026 03:00 | Ärger  | 10    | Federn     | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Verdacht auf Warenkreislauf: Ärger: 10 × Federn, Zorn: 10 × Federn"

    @wip
    Scenario: The pairs not judgeable are listed in German alphabetical order of the members
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand     | Qualität | Vorgang     |
        | 01.01.2026 00:00 | Zorn   | 10    | Kristallat     | 100      | Entnahme    |
        | 01.01.2026 01:00 | Zorn   | 1     | Achat-Armbrust | 100      | Einlagerung |
        | 01.01.2026 02:00 | Ärger  | 10    | Kristallat     | 100      | Entnahme    |
        | 01.01.2026 03:00 | Ärger  | 1     | Achat-Armbrust | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Nicht beurteilbar (Rezept ungelesen): Ärger: Kristallat, Zorn: Kristallat"

    @wip
    Scenario: The health report lists the suspected round trips in German alphabetical order of the members
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 00:00 | Zorn   | 10    | Federn     | 100      | Entnahme    |
        | 01.01.2026 01:00 | Zorn   | 10    | Federn     | 100      | Einlagerung |
        | 01.01.2026 02:00 | Ärger  | 10    | Federn     | 100      | Entnahme    |
        | 01.01.2026 03:00 | Ärger  | 10    | Federn     | 100      | Einlagerung |
      And the daily collection has run
      When the operator asks the service for its health
      Then the health report lists the round trips in this order: "Ärger: 10 × Federn", "Zorn: 10 × Federn"

    @wip
    Scenario: The health report lists the pairs not judgeable in German alphabetical order of the members
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand     | Qualität | Vorgang     |
        | 01.01.2026 00:00 | Zorn   | 10    | Kristallat     | 100      | Entnahme    |
        | 01.01.2026 01:00 | Zorn   | 1     | Achat-Armbrust | 100      | Einlagerung |
        | 01.01.2026 02:00 | Ärger  | 10    | Kristallat     | 100      | Entnahme    |
        | 01.01.2026 03:00 | Ärger  | 1     | Achat-Armbrust | 100      | Einlagerung |
      And the daily collection has run
      When the operator asks the service for its health
      Then the health report lists the pairs it could not judge in this order: "Ärger: Kristallat", "Zorn: Kristallat"
