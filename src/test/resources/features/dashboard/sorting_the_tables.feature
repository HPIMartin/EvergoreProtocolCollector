Feature: Sorting the tables
  Every column of the overview and of the ledgers can sort its table, in ascending or in descending
  order. Names sort alphabetically as German is sorted, figures by amount and times by the moment
  they stand for. Until a column is chosen a table keeps the order it was delivered in: the overview
  alphabetically, a ledger newest first. One scenario per kind of column (name, figure, time)
  stands for every column of that kind.

  Rule: The overview sorts by any of its columns

    Background:
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Ärger  | 9      | Einzahlung |
        | 02.01.2026 12:00 | Zorn   | 100    | Einzahlung |
        | 03.01.2026 12:00 | Bambor | 10     | Einzahlung |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 04.01.2026 12:00 | Ärger  | 10    | Eisenbarren | 100      | Einlagerung |
        | 05.01.2026 12:00 | Bambor | 1     | Eisenbarren | 100      | Einlagerung |
        | 06.01.2026 12:00 | Calix  | 10    | Eisenbarren | 100      | Einlagerung |
        | 07.01.2026 12:00 | Zorn   | 100   | Eisenbarren | 100      | Einlagerung |
      And the daily collection has run
      And a member has opened the overview

    Scenario Outline: Figures sort by amount, in <direction> order
      When the member sorts the active table by "Bank-Einzahlung" in <direction> order
      Then the active table lists "<members>"

      Examples:
        | direction  | members                    |
        | ascending  | Calix, Ärger, Bambor, Zorn |
        | descending | Zorn, Bambor, Ärger, Calix |

    Scenario: Names sort as German is sorted
      When the member sorts the active table by "Avatar" in descending order
      Then the active table lists "Zorn, Calix, Bambor, Ärger"

    Scenario Outline: Members with the same figure keep their alphabetical order, in <direction> order
      When the member sorts the active table by "Einlagerung" in <direction> order
      Then the active table lists "<members>"

      Examples:
        | direction  | members                    |
        | ascending  | Bambor, Ärger, Calix, Zorn |
        | descending | Zorn, Ärger, Calix, Bambor |

    Scenario Outline: A member without a date in the column sorts last, in <direction> order
      When the member sorts the active table by "Letzte Bankaktivität" in <direction> order
      Then the active table lists "<members>"

      Examples:
        | direction  | members                    |
        | ascending  | Ärger, Zorn, Bambor, Calix |
        | descending | Bambor, Zorn, Ärger, Calix |

    Scenario: The guild row stays below the members however they are sorted
      When the member sorts the active table by "Einlagerung" in descending order
      Then the active table ends with the guild row

  Rule: Switching the last column keeps the table sorted by it

    Scenario: A sort by the last column follows the switch to the other figure
      Kira's figure is the same before and after the guild's share. Anton gave the guild 120 in raw
      ore, which his figure after the share does not count and his figure before it does.

      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Kira   | 100    | Einzahlung |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 02.01.2026 12:00 | Anton  | 10    | Kupfererz  | 100      | Einlagerung |
      And the daily collection has run
      And a member has sorted the active table by "Nach Abzügen" in ascending order
      When the member switches the last column to "Vor Abzügen"
      Then the active table lists "Kira, Anton"
      And the active table is sorted by "Vor Abzügen" in ascending order

  Rule: Each table sorts on its own

    Background:
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Calix  | 5      | Einzahlung |
        | 02.01.2026 12:00 | Dorn   | 30     | Einzahlung |
        | 31.01.2026 12:00 | Aurora | 10     | Einzahlung |
        | 15.02.2026 12:00 | Boreas | 20     | Einzahlung |
      And the daily collection has run
      And a member has opened the overview

    Scenario Outline: Sorting the <sorted> table leaves the <other> table as it was
      When the member sorts the <sorted> table by "Bank-Einzahlung" in descending order
      Then the <sorted> table lists "<sorted order>"
      And the <other> table lists "<other order>"

      Examples:
        | sorted  | sorted order   | other   | other order    |
        | active  | Boreas, Aurora | dormant | Calix, Dorn    |
        | dormant | Dorn, Calix    | active  | Aurora, Boreas |

  Rule: A ledger sorts all of its movements, not only the page shown
    The sort covers the whole ledger, on every page and in a bookmark of a page.

    Background:
      Given Aurora has 150 movements of "Eisenbarren" in the guild storage ledger, the newest of quantity 1 and each older one of one more

    Scenario: Sorting a ledger orders the whole ledger
      Given a member has opened the storage ledger of "Aurora"
      When the member sorts the ledger by "Menge" in descending order
      Then the ledger's first row shows a quantity of 150

    Scenario: A sorted ledger keeps its order on the next page and in a bookmark of that page
      Given a member has sorted the storage ledger of "Aurora" by "Menge" in descending order
      And the member has followed "Weiter"
      When the member later opens their bookmark of page 2 of the storage ledger of "Aurora"
      Then the ledger's first row shows a quantity of 50
