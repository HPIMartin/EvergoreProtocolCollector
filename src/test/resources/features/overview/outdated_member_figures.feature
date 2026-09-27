@wip
Feature: A member whose figures could not be refreshed
  A collection run recomputes every member's figures. When one member's stored movements cannot be
  read, that member keeps the figures of the last run that reached them and every other member is
  still refreshed. The overview says which row is outdated and since when, so an old figure is not
  read as a current one. The row's activity columns keep showing the member's newest movement, which
  can be newer than its figures. All times are German wall-clock time, as the game shows them.

  Rule: The member keeps the figures of the last run that reached them

    Background:
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Boreas | 750    | Einzahlung |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 02.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
      And the daily collection ran at 03.01.2026 05:00
      And the guild bank ledger also holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 03.01.2026 12:00 | Boreas | 250    | Einzahlung |
      And the guild storage ledger also holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 04.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
      And Aurora's stored movements cannot be read
      And the daily collection ran at 05.01.2026 05:00

    Scenario: Everyone else is refreshed while the unreadable member keeps their figures
      When a member opens the overview
      Then the overview shows:
        | Avatar | Bank-Einzahlung | Einlagerung | Letzte Lageraktivität |
        | Aurora | 0               | 72          | 04.01.2026 12:00      |
        | Boreas | 1.000           | 0           | –                     |

    Scenario: The outdated row says since when its figures are unchanged
      When a member opens the overview
      Then Aurora's row is marked "Veraltete Zahlen. Letzte erfolgreiche Aktualisierung vom 03.01.2026 05:00."
      And Boreas's row carries no mark

    Scenario: The guild row says that it adds up an outdated row
      When a member opens the overview
      Then the guild row is marked "Enthält mindestens eine Zeile mit veralteten Zahlen."

    Scenario: The next run that can read the movements brings the member up to date
      Given Aurora's stored movements can be read again
      And the daily collection ran at 06.01.2026 05:00
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung |
        | Aurora | 144         |
      And Aurora's row carries no mark
      And the guild row carries no mark

  Rule: A member no run has computed yet is shown as not yet computed
    Dorn deposited on 01.01., Calix on 02.01., and Calix's first recompute, in the run of
    03.01.2026 05:00, fails. Whether an earlier run had computed Dorn before makes no difference.

    @wip
    Scenario Outline: A member whose first recompute fails <when> is shown as not yet computed
      Today Calix's row shows zeros: after an earlier run it is marked "Veraltete Zahlen. Letzte
      erfolgreiche Aktualisierung vom 02.01.2026 05:00.", the date of that run, and on the guild's
      very first run it carries no mark. This states the corrected behavior for both histories.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Dorn   | 1     | Eisenbarren | 100      | Einlagerung |
      And <history>
      And the guild storage ledger also holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 02.01.2026 12:00 | Calix  | 1     | Eisenbarren | 100      | Einlagerung |
      And Calix's stored movements cannot be read
      And the daily collection ran at 03.01.2026 05:00
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Nach Abzügen |
        | Calix  | –           | –            |
        | Dorn   | 72          | 72           |
      And Calix's row is marked "Noch nicht berechnet."
      And the guild row is marked "Enthält mindestens eine Zeile, die noch nicht berechnet ist."

      Examples:
        | when                          | history                                      |
        | after an earlier run          | the daily collection ran at 02.01.2026 05:00 |
        | on the guild's very first run | no collection has ever run                   |
