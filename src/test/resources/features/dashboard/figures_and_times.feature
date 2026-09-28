Feature: How figures and times are shown
  The dashboard writes numbers and times the way the game and the guild's sheet did: gold in whole
  pieces with German digit grouping, times as German wall-clock time. A figure's colour tells a
  credit from a debit at a glance. A dash stands in a member's row or in the guild row where there
  is no figure or no date to show; the guild's four figures at the top spell out why one is missing.

  Rule: Numbers and times are written the German way

    Scenario Outline: A figure of <gold> gold is shown as "<shown>"
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang  |
        | 01.01.2026 12:00 | Aurora | <gold> | Entnahme |
      And the daily collection has run
      When a member opens the overview
      Then Aurora's "Bank-Auszahlung" is <shown>
      And Aurora's "Nach Abzügen" is -<shown>

      Examples:
        | gold    | shown     |
        | 7       | 7         |
        | 1500    | 1.500     |
        | 1053554 | 1.053.554 |

    Scenario Outline: A time is shown as German wall-clock time in <season>, whatever the member's time zone
      Given the guild bank ledger holds:
        | Zeitpunkt | Avatar | Betrag | Vorgang    |
        | <when>    | Aurora | 100    | Einzahlung |
      And the member's browser runs on New York time
      When a member opens the bank ledger of "Aurora"
      Then the ledger shows a movement at <when>

      Examples:
        | season | when             |
        | winter | 01.01.2026 12:00 |
        | summer | 15.07.2026 12:00 |

  Rule: A member's figure is coloured by its column and its sign
    Deposits read as a credit and withdrawals as a debit; the last column is uncoloured until it
    turns negative; a figure of zero is never coloured.

    Background:
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Aurora | 100    | Einzahlung |
        | 02.01.2026 12:00 | Aurora | 300    | Entnahme   |
        | 03.01.2026 12:00 | Boreas | 100    | Einzahlung |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 04.01.2026 12:00 | Aurora | 4     | Federn      | 100      | Einlagerung |
        | 05.01.2026 12:00 | Aurora | 10    | Eisenbarren | 100      | Entnahme    |
      And the daily collection has run

    Scenario Outline: <member>'s "<column>" of <value> shows <tone>
      When a member opens the overview
      Then <member>'s "<column>" shows <value> <tone>

      Examples:
        | member | column          | value | tone                 |
        | Aurora | Bank-Einzahlung | 100   | in the credit colour |
        | Aurora | Einlagerung     | 100   | in the credit colour |
        | Aurora | Bank-Auszahlung | 300   | in the debit colour  |
        | Aurora | Entnahme        | 720   | in the debit colour  |
        | Aurora | Nach Abzügen    | -820  | in the debit colour  |
        | Boreas | Nach Abzügen    | 100   | uncoloured           |
        | Boreas | Einlagerung     | 0     | uncoloured           |

  Rule: A figure of the guild's position is coloured by its sign, the subsidies never as a credit
    The storage value works out as 100 for the four "Federn" deposited, plus 120 given in raw ore (the
    ten "Kupfererz" at 60 % of 20 each), less 40 craft subsidy (the "Federn" credited at their full
    25 instead of 60 % of it), less 720 for the ten "Eisenbarren" withdrawn (60 % of 120 each):
    100 + 120 - 40 - 720 = -540.

    Background:
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Boreas | 100    | Einzahlung |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 02.01.2026 12:00 | Boreas | 10    | Kupfererz   | 100      | Einlagerung |
        | 03.01.2026 12:00 | Boreas | 4     | Federn      | 100      | Einlagerung |
        | 04.01.2026 12:00 | Boreas | 10    | Eisenbarren | 100      | Entnahme    |
      And the daily collection has run

    Scenario Outline: The guild's "<figure>" of <value> shows <tone>
      When a member opens the overview
      Then the guild's "<figure>" shows <value> <tone>

      Examples:
        | figure                | value | tone                 |
        | Gildenbank            | 100   | in the credit colour |
        | Gildenlagerwert       | -540  | in the debit colour  |
        | Gildenspende          | 120   | in the credit colour |
        | Handwerkssubventionen | 40    | uncoloured           |
