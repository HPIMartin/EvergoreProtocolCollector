@wip
Feature: Guild contribution overview
  The overview replaces the sheet the guild used to keep by hand. It has one row per member the
  guild ledgers know, in the sheet's columns: the gold a member paid into and took out of the guild
  bank, the value of the goods they deposited into and withdrew from the guild storage, and the
  guild value they generated from those four. Its figures are those of the last collection run.

  Scenario: A member's row adds the sheet's four columns up to the guild value they generated
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1500   | Einzahlung |
      | 02.01.2026 12:00 | Aurora | 200    | Entnahme   |
    And the guild storage ledger holds:
      | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
      | 03.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
      | 04.01.2026 12:00 | Aurora | 1     | Kupfererz   | 100      | Entnahme    |
    And the daily collection has run
    When a member opens the overview
    Then the overview shows:
      | Avatar | Bank-Einzahlung | Bank-Auszahlung | Einlagerung | Entnahme | Nach Abzügen | Letzte Lageraktivität | Letzte Bankaktivität |
      | Aurora | 1.500           | 200             | 72          | 12       | 1.360        | 04.01.2026 12:00      | 02.01.2026 12:00     |

  Scenario: A member who used only one of the two ledgers is listed with nothing in the other
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang  |
      | 01.01.2026 12:00 | Calix  | 300    | Entnahme |
    And the guild storage ledger holds:
      | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
      | 02.01.2026 12:00 | Brynja | 1     | Eisenbarren | 100      | Einlagerung |
    And the daily collection has run
    When a member opens the overview
    Then the overview shows:
      | Avatar | Bank-Einzahlung | Bank-Auszahlung | Einlagerung | Entnahme | Nach Abzügen | Letzte Lageraktivität | Letzte Bankaktivität |
      | Brynja | 0               | 0               | 72          | 0        | 72           | 02.01.2026 12:00      | –                    |
      | Calix  | 0               | 300             | 0           | 0        | -300         | –                     | 01.01.2026 12:00     |

  Scenario: Members are listed in German alphabetical order
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Zorn   | 100    | Einzahlung |
      | 02.01.2026 12:00 | Ärger  | 100    | Einzahlung |
      | 03.01.2026 12:00 | Bambor | 100    | Einzahlung |
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
    All 101 moved gold at the same time, so every member is active; the caption counts the 100
    members shown against the 101 of the guild.

    Given 101 members have each paid 1 gold into the guild bank
    And the daily collection has run
    When a member opens the overview
    Then the overview lists 100 members
    And the member last in German alphabetical order is not listed
    And the active table's caption reads "Aktiv (30 Tage vor dem letzten Vorgang): 100 von 101 Avataren"
    And the guild row shows:
      | Avatar | Bank-Einzahlung |
      | Gilde  | 101             |

  @wip
  Scenario: When the figures cannot be loaded, the overview says so in plain German
    Today the page says "Fehler: The API answered 500". This states the corrected behavior; the
    overview stands for every page of the dashboard, which all show a failure the same way.

    Given the figures cannot be loaded
    When a member opens the overview
    Then the page says "Die Daten konnten nicht geladen werden. Bitte später erneut versuchen."

  Rule: Every figure is rounded to whole gold once, from the exact values it is made of
    A member's row, the guild row and the guild's position are each worked out from the exact
    values and rounded only where they are shown, so a shown figure can differ from the sum of the
    shown figures beside or above it.

    Scenario Outline: A storage value of <exact> gold is shown as <shown>
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität  | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | <item>     | <quality> | Einlagerung |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Nach Abzügen |
        | Aurora | <shown>     | <shown>      |

      Examples:
        | item         | quality | exact | shown |
        | Kriegspfeile | 70      | 2,94  | 3     |
        | Kriegspfeile | 30      | 1,26  | 1     |
        | Federn       | 2       | 0,5   | 1     |

    @wip
    Scenario: A member's row is rounded from its exact value, not from its rounded columns
      Aurora deposited goods worth 1,26 and withdrew goods worth 0,54, so she generated 0,72. Today
      each column is rounded first, to 1 and 1, and the row reads 0. This states the corrected
      behavior.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand   | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Kriegspfeile | 30       | Einlagerung |
        | 02.01.2026 12:00 | Aurora | 1     | Götterstich  | 10       | Entnahme    |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Entnahme | Nach Abzügen |
        | Aurora | 1           | 1        | 1            |

    @wip
    Scenario: The figure before the guild's share is rounded from its exact value
      Aurora deposited feathers worth 0,5, credited in full, of which the guild paid 0,2 above its
      own price, so before the guild's share she moved 0,3. Today the rounded 1 less the rounded 0
      reads 1, in her row and in the guild row. This states the corrected behavior.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Federn     | 2        | Einlagerung |
      And the daily collection has run
      And a member has opened the overview
      When the member switches the last column to "Vor Abzügen"
      Then the overview shows:
        | Avatar | Vor Abzügen |
        | Aurora | 0           |
      And the guild row shows:
        | Avatar | Vor Abzügen |
        | Gilde  | 0           |

    @wip
    Scenario: The guild row and the guild's position are rounded from the guild's exact values
      Aurora and Boreas each deposited goods credited 2,34 in all: the guild paid 0,6 above its own
      price for the feathers and was given 0,36 for nothing in copper ore. Each row shows 2.
      Exactly, the guild row is 4,68, the guild paid 1,2 above its price, was given 0,72 and holds
      4,68 + 0,72 - 1,2 = 4,2. Today the guild row adds the rounded rows and reads 4, and the
      guild's position adds rounded figures to 2, 0 and 2. This states the corrected behavior.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand   | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Kriegspfeile | 20       | Einlagerung |
        | 02.01.2026 12:00 | Aurora | 1     | Federn       | 6        | Einlagerung |
        | 03.01.2026 12:00 | Aurora | 1     | Kupfererz    | 3        | Einlagerung |
        | 04.01.2026 12:00 | Boreas | 1     | Kriegspfeile | 20       | Einlagerung |
        | 05.01.2026 12:00 | Boreas | 1     | Federn       | 6        | Einlagerung |
        | 06.01.2026 12:00 | Boreas | 1     | Kupfererz    | 3        | Einlagerung |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Nach Abzügen |
        | Aurora | 2           | 2            |
        | Boreas | 2           | 2            |
      And the guild row shows:
        | Avatar | Einlagerung | Nach Abzügen |
        | Gilde  | 5           | 5            |
      And the guild's position reads:
        | Gildenbank | Gildenlagerwert | Gildenspende | Handwerkssubventionen |
        | 0          | 4               | 1            | 1                     |
