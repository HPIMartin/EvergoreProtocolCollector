@wip
Feature: Sorting the tables
  Every column of the overview and of the ledgers can sort its table: choosing a column sorts by it
  in ascending order, choosing it again reverses the order, so a descending sort takes two choices.
  Names sort alphabetically as German is sorted, figures by amount and times by the moment they
  stand for. Until a column is chosen a table keeps the order it was delivered in: the overview
  alphabetically, a ledger newest first. One scenario per kind of column (name, figure, time)
  stands for every column of that kind.

  Rule: The overview sorts by any of its columns

    Background:
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 10.01.2024 10:00 | Ärger  | 9      | Einzahlung |
        | 12.02.2024 10:00 | Bambor | 10     | Einzahlung |
        | 01.02.2024 10:00 | Zorn   | 100    | Einzahlung |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Ärger  | 10    | Eisenbarren | 100      | Einlagerung |
        | 15.01.2024 10:00 | Bambor | 1     | Eisenbarren | 100      | Einlagerung |
        | 15.01.2024 10:00 | Calix  | 10    | Eisenbarren | 100      | Einlagerung |
        | 15.01.2024 10:00 | Zorn   | 100   | Eisenbarren | 100      | Einlagerung |
      And the daily collection has run
      And a member has opened the overview

    @characterization
    Scenario: Figures sort by amount, and choosing the column again reverses the order
      When the member sorts the active table by "Bank-Einzahlung"
      Then the active table lists "Calix, Ärger, Bambor, Zorn"
      When the member sorts the active table by "Bank-Einzahlung" again
      Then the active table lists "Zorn, Bambor, Ärger, Calix"

    Scenario: Names sort as German is sorted
      When the member sorts the active table by "Avatar" in descending order
      Then the active table lists "Zorn, Calix, Bambor, Ärger"

    @characterization
    Scenario: Members with the same figure keep their alphabetical order in either direction
      When the member sorts the active table by "Einlagerung"
      Then the active table lists "Bambor, Ärger, Calix, Zorn"
      When the member sorts the active table by "Einlagerung" again
      Then the active table lists "Zorn, Ärger, Calix, Bambor"

    @characterization
    Scenario: A member without a date in the column sorts last in either direction
      When the member sorts the active table by "Letzte Bankaktivität"
      Then the active table lists "Ärger, Zorn, Bambor, Calix"
      When the member sorts the active table by "Letzte Bankaktivität" again
      Then the active table lists "Bambor, Zorn, Ärger, Calix"

    Scenario: The guild row stays below the members however they are sorted
      When the member sorts the active table by "Einlagerung" in descending order
      Then the active table ends with the guild row

  Rule: Switching the last column keeps the table sorted by it

    Scenario: A sort by the last column follows the switch to the other figure
      Kira's figure is the same before and after the guild's share. Anton gave the guild 120 in raw
      ore, which his figure after the share does not count and his figure before it does.

      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 10.01.2024 10:00 | Kira   | 100    | Einzahlung |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 10.01.2024 11:00 | Anton  | 10    | Kupfererz  | 100      | Einlagerung |
      And the daily collection has run
      And a member has sorted the active table by "Nach Abzügen"
      When the member switches the last column to "Vor Abzügen"
      Then the active table lists "Kira, Anton"
      And the active table is still sorted by its last column

  Rule: Each table sorts on its own

    Scenario: Sorting the active table leaves the dormant table as it was
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 10.03.2024 10:00 | Aurora | 10     | Einzahlung |
        | 10.03.2024 11:00 | Boreas | 20     | Einzahlung |
        | 01.01.2024 10:00 | Calix  | 5      | Einzahlung |
        | 01.01.2024 11:00 | Dorn   | 30     | Einzahlung |
      And the daily collection has run
      And a member has opened the overview
      When the member sorts the active table by "Bank-Einzahlung" in descending order
      Then the active table lists "Boreas, Aurora"
      And the dormant table lists "Calix, Dorn"

    Scenario: Sorting the dormant table leaves the active table as it was
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 10.03.2024 10:00 | Aurora | 10     | Einzahlung |
        | 10.03.2024 11:00 | Boreas | 20     | Einzahlung |
        | 01.01.2024 10:00 | Calix  | 5      | Einzahlung |
        | 01.01.2024 11:00 | Dorn   | 30     | Einzahlung |
      And the daily collection has run
      And a member has opened the overview
      When the member sorts the dormant table by "Bank-Einzahlung" in descending order
      Then the dormant table lists "Dorn, Calix"
      And the active table lists "Aurora, Boreas"

  Rule: A ledger sorts all of its movements, not only the page shown
    Today a ledger sorts only the hundred movements of the page shown. The two scenarios below state
    the corrected behavior: the sort covers the whole ledger and stays when the page is opened
    again from a bookmark.

    @wip
    Scenario: Sorting a ledger orders the whole ledger
      Given Aurora has 150 movements of "Eisenbarren" in the guild storage ledger, the newest of quantity 1 and each older one of one more
      And a member has opened the storage ledger of "Aurora"
      When the member sorts the ledger by "Menge" in descending order
      Then the ledger's first row shows a quantity of 150

    @wip
    Scenario: A sorted ledger keeps its order on the next page and when that page is opened again
      Given Aurora has 150 movements of "Eisenbarren" in the guild storage ledger, the newest of quantity 1 and each older one of one more
      And a member has sorted the storage ledger of "Aurora" by "Menge" in descending order
      And the member has followed "Weiter"
      When the member opens that page again later from a bookmark
      Then the ledger's first row shows a quantity of 50
