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

  Scenario: When the figures cannot be loaded, the overview says so in plain German
    The overview stands for every page of the dashboard, which all show a failure the same way.

    Given the figures cannot be loaded
    When a member opens the overview
    Then the page says "Die Daten konnten nicht geladen werden. Bitte später erneut versuchen."

  Rule: Every figure is rounded to whole gold once, from the exact values it is made of
    A member's row, the guild row and the guild's position are each worked out from the exact
    values and rounded only where they are shown, so a shown figure can differ from the sum of the
    shown figures beside or above it. A figure goes to the nearer whole gold, above zero as below
    it. Only equipment has a quality; the ledgers show 100 for every other item. The game's prices
    never give a figure of exactly half a gold: gold is whole, equipment is priced in whole hundreds
    so that its quality still leaves a whole price, and every credit or charge is such a price or
    60 % of one, so each exact figure is a multiple of 0,2. The prices used here: "Götterstich" 9,
    "Kriegspfeile" 7, "Streitaxt" 1800, "Pfeile" 3, "Magieessenz" 4 and "Kupfererz" 20.

    Scenario Outline: A member who generated <exact> gold is shown <shown>
      "Götterstich" and "Streitaxt" are credited at 60 %, the axe in proportion to its quality; a
      withdrawal of "Pfeile" or "Magieessenz" costs 60 % and takes the member's "Nach Abzügen" below
      zero.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität  | Vorgang    |
        | 01.01.2026 12:00 | Aurora | 1     | <item>     | <quality> | <movement> |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | <movement> | Nach Abzügen |
        | Aurora | <value>    | <shown>      |

      Examples:
        | item        | quality | movement    | value | exact | shown |
        | Götterstich | 100     | Einlagerung | 5     | 5,4   | 5     |
        | Streitaxt   | 1       | Einlagerung | 11    | 10,8  | 11    |
        | Pfeile      | 100     | Entnahme    | 2     | -1,8  | -2    |
        | Magieessenz | 100     | Entnahme    | 2     | -2,4  | -2    |

    Scenario: A member's row is rounded from its exact value, not from its rounded columns
      Aurora deposited goods worth 4,2 and withdrew goods worth 3,6, so she generated 0,6.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand   | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Kriegspfeile | 100      | Einlagerung |
        | 02.01.2026 12:00 | Aurora | 2     | Pfeile       | 100      | Entnahme    |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Entnahme | Nach Abzügen |
        | Aurora | 4           | 4        | 1            |

    Scenario: A figure nearer to 0 than to -1 shows as 0, with no minus sign
      Aurora took 1 gold from the guild bank, deposited goods worth 4,2 and withdrew goods worth
      3,6, so she generated -0,4.

      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang  |
        | 01.01.2026 12:00 | Aurora | 1      | Entnahme |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand   | Qualität | Vorgang     |
        | 02.01.2026 12:00 | Aurora | 1     | Kriegspfeile | 100      | Einlagerung |
        | 03.01.2026 12:00 | Aurora | 2     | Pfeile       | 100      | Entnahme    |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Nach Abzügen |
        | Aurora | 0            |

    Scenario: The figure before the guild's share is rounded from its exact value
      Aurora deposited "Magieessenz" credited in full at 4, of which the guild paid 1,6 above its own
      price, and "Kriegspfeile" worth 4,2, so after the guild's share she moved 8,2 and before it
      6,6, which shows as 7; taken from the rounded figures, the 8 after the share less the 2 of the
      subsidy, it would be 6. The guild's stock value is the same 6,6 at the guild's price, shown as
      7, and its subsidy 1,6, shown as 2.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand   | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Magieessenz  | 100      | Einlagerung |
        | 02.01.2026 12:00 | Aurora | 1     | Kriegspfeile | 100      | Einlagerung |
      And the daily collection has run
      And a member has opened the overview
      When the member switches the last column to "Vor Abzügen"
      Then the overview shows:
        | Avatar | Vor Abzügen |
        | Aurora | 7           |
      And the guild row shows:
        | Avatar | Vor Abzügen |
        | Gilde  | 7           |
      And the guild's position reads:
        | Gildenbank | Gildenlagerwert | Gildenspende | Handwerkssubventionen |
        | 0          | 7               | 0            | 2                     |

    Scenario: The guild row and the guild's position are rounded from the guild's exact values
      Aurora and Boreas each deposited goods credited 9,4: "Götterstich" worth 5,4 and "Magieessenz"
      credited in full at 4, of which the guild paid 1,6 above its own price, and "Kupfererz" credited
      nothing, which gave the guild 12. Each row shows 9. Exactly, the guild row is 18,8, the guild
      paid 3,2 above its price, was given 24 and holds 18,8 + 24 - 3,2 = 39,6.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Götterstich | 100      | Einlagerung |
        | 02.01.2026 12:00 | Aurora | 1     | Magieessenz | 100      | Einlagerung |
        | 03.01.2026 12:00 | Aurora | 1     | Kupfererz   | 100      | Einlagerung |
        | 04.01.2026 12:00 | Boreas | 1     | Götterstich | 100      | Einlagerung |
        | 05.01.2026 12:00 | Boreas | 1     | Magieessenz | 100      | Einlagerung |
        | 06.01.2026 12:00 | Boreas | 1     | Kupfererz   | 100      | Einlagerung |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Nach Abzügen |
        | Aurora | 9           | 9            |
        | Boreas | 9           | 9            |
      And the guild row shows:
        | Avatar | Einlagerung | Nach Abzügen |
        | Gilde  | 19          | 19           |
      And the guild's position reads:
        | Gildenbank | Gildenlagerwert | Gildenspende | Handwerkssubventionen |
        | 0          | 40              | 24           | 3                     |
