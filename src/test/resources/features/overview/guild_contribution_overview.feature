@wip
Feature: Guild contribution overview
  The overview replaces the sheet the guild used to keep by hand. It has one row per member the
  guild ledgers know, in the sheet's columns: the gold a member paid into and took out of the guild
  bank, the value of the goods they deposited into and withdrew from the guild storage, and the
  guild value they generated from those four. Its figures are those of the last collection run.

  Scenario: A member's row adds the sheet's four columns up to the guild value they generated
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 10.01.2024 10:00 | Aurora | 1500   | Einzahlung |
      | 12.01.2024 12:00 | Aurora | 200    | Entnahme   |
    And the guild storage ledger holds:
      | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
      | 15.01.2024 10:00 | Aurora | 10    | Eisenbarren | 100      | Einlagerung |
      | 17.01.2024 12:00 | Aurora | 5     | Kupfererz   | 100      | Entnahme    |
    And the daily collection has run
    When a member opens the overview
    Then the overview shows:
      | Avatar | Bank-Einzahlung | Bank-Auszahlung | Einlagerung | Entnahme | Nach Abzügen | Letzte Lageraktivität | Letzte Bankaktivität |
      | Aurora | 1.500           | 200             | 720         | 60       | 1.960        | 17.01.2024 12:00      | 12.01.2024 12:00     |

  Scenario: A member who used only one of the two ledgers is listed with nothing in the other
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang  |
      | 01.03.2024 08:00 | Calix  | 300    | Entnahme |
    And the guild storage ledger holds:
      | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
      | 06.02.2024 10:00 | Brynja | 10    | Eisenbarren | 100      | Einlagerung |
    And the daily collection has run
    When a member opens the overview
    Then the overview shows:
      | Avatar | Bank-Einzahlung | Bank-Auszahlung | Einlagerung | Entnahme | Nach Abzügen | Letzte Lageraktivität | Letzte Bankaktivität |
      | Brynja | 0               | 0               | 720         | 0        | 720          | 06.02.2024 10:00      | –                    |
      | Calix  | 0               | 300             | 0           | 0        | -300         | –                     | 01.03.2024 08:00     |

  Scenario: Members are listed in German alphabetical order
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 10.01.2024 10:00 | Zorn   | 100    | Einzahlung |
      | 10.01.2024 11:00 | Ärger  | 100    | Einzahlung |
      | 10.01.2024 12:00 | Bambor | 100    | Einzahlung |
    And the daily collection has run
    When a member opens the overview
    Then the overview lists "Ärger, Bambor, Zorn"

  Scenario: A guild without any collected movement
    Given the guild ledgers hold no movement yet
    And the daily collection has run
    When a member opens the overview
    Then the active table says "Noch kein Avatar erfasst."
    And the dormant table says "Kein Avatar ruht."
    And the overview shows no guild row
    And the guild's position reads:
      | Gildenbank | Gildenlagerwert | Gildenspende | Handwerkssubventionen |
      | 0          | 0               | 0            | 0                     |

  Scenario: The overview shows the first hundred members and says how many there are
    Given 101 members named "Mitglied 001" to "Mitglied 101" each paid 1 gold into the guild bank on 10.01.2024 at 10:00
    And the daily collection has run
    When a member opens the overview
    Then the overview lists "Mitglied 001" to "Mitglied 100"
    And "Mitglied 101" is not listed
    And the active table's caption reads "Aktiv (30 Tage vor dem letzten Vorgang): 100 von 101 Avataren"
    And the guild row shows:
      | Avatar | Bank-Einzahlung |
      | Gilde  | 101             |

  @wip
  Scenario: When the figures cannot be loaded, the overview says so in plain German
    Today the page says "Fehler: The API answered 500". This states the corrected behavior; the
    overview stands for every page of the dashboard, which all show a failure the same way.

    Given the dashboard cannot load its figures from the service
    When a member opens the overview
    Then the page says "Die Daten konnten nicht geladen werden. Bitte später erneut versuchen."

  Rule: Each figure is rounded to whole gold on its own before anything adds it up
    A member's column is rounded before the member's row is added up, and the guild row adds the
    rounded rows, so every row and the guild row match what a member can add up by hand.

    Scenario Outline: A storage value of <exact> gold is shown as <shown>
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität  | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 1     | <item>     | <quality> | Einlagerung |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Nach Abzügen |
        | Aurora | <shown>     | <shown>      |

      Examples:
        | item   | quality | exact | shown |
        | Pfeile | 100     | 1,8   | 2     |
        | Pfeile | 70      | 1,26  | 1     |
        | Federn | 2       | 0,5   | 1     |

    @characterization
    Scenario: Each column is rounded before the row adds them up
      Deposited 1,26 and withdrawn 0,54 show as 1 and 1, so the row reads 0 although the exact
      difference, 0,72, would round to 1.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 1     | Pfeile     | 70       | Einlagerung |
        | 16.01.2024 10:00 | Aurora | 1     | Pfeile     | 30       | Entnahme    |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Entnahme | Nach Abzügen |
        | Aurora | 1           | 1        | 0            |

    Scenario: The guild row adds up the rows above it exactly
      Each row is rounded to whole gold on its own, and the guild row adds the rounded rows, so it
      matches the column a member can add up by hand rather than the unrounded guild total.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 1     | Pfeile     | 70       | Einlagerung |
        | 15.01.2024 11:00 | Boreas | 1     | Pfeile     | 70       | Einlagerung |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Nach Abzügen |
        | Aurora | 1           | 1            |
        | Boreas | 1           | 1            |
      And the guild row shows:
        | Avatar | Einlagerung | Nach Abzügen | Letzte Lageraktivität | Letzte Bankaktivität |
        | Gilde  | 2           | 2            | –                     | –                    |
