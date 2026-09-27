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
        | 31.08.2026 18:00 | Boreas | 750    | Einzahlung |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 31.08.2026 19:00 | Aurora | 10    | Eisenbarren | 100      | Einlagerung |
      And the daily collection ran at 01.09.2026 05:00
      And the guild bank ledger also holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.09.2026 21:00 | Boreas | 250    | Einzahlung |
      And the guild storage ledger also holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.09.2026 20:00 | Aurora | 10    | Eisenbarren | 100      | Einlagerung |
      And Aurora's stored movements cannot be read
      And the daily collection ran at 02.09.2026 05:00

    Scenario: Everyone else is refreshed while the unreadable member keeps their figures
      When a member opens the overview
      Then the overview shows:
        | Avatar | Bank-Einzahlung | Einlagerung | Letzte Lageraktivität |
        | Aurora | 0               | 720         | 01.09.2026 20:00      |
        | Boreas | 1.000           | 0           | –                     |

    Scenario: The outdated row says since when its figures are unchanged
      When a member opens the overview
      Then Aurora's row is marked "Veraltete Zahlen. Letzte erfolgreiche Aktualisierung vom 01.09.2026 05:00."
      And Boreas's row carries no mark

    Scenario: The guild row says that it adds up an outdated row
      When a member opens the overview
      Then the guild row is marked "Enthält mindestens eine Zeile mit veralteten Zahlen."

    Scenario: The next run that can read the movements brings the member up to date
      Given Aurora's stored movements can be read again
      And the daily collection ran at 03.09.2026 05:00
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung |
        | Aurora | 1.440       |
      And Aurora's row carries no mark
      And the guild row carries no mark

  Rule: A member no run has computed yet is shown as not yet computed
    Today such a member's row shows zeros, marked as outdated since the run before when there was
    one and not marked at all on the guild's very first run. The two scenarios below state the
    corrected behavior.

    @wip
    Scenario: A new member whose first recompute fails is shown as not yet computed, even after an earlier run
      Today his row shows zeros, marked "Veraltete Zahlen. Letzte erfolgreiche Aktualisierung vom
      01.09.2026 05:00.", the date of the run before.

      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 31.08.2026 18:00 | Boreas | 750    | Einzahlung |
      And the daily collection ran at 01.09.2026 05:00
      And the guild storage ledger also holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.09.2026 22:00 | Calix  | 10    | Eisenbarren | 100      | Einlagerung |
      And Calix's stored movements cannot be read
      And the daily collection ran at 02.09.2026 05:00
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Nach Abzügen |
        | Calix  | –           | –            |
      And Calix's row is marked "Noch nicht berechnet."
      And the guild row is marked "Enthält mindestens eine Zeile, die noch nicht berechnet ist."

    @wip
    Scenario: On the guild's very first run a member whose recompute fails is shown as not yet computed
      Today his row shows zeros and carries no mark.

      Given no collection has ever run
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.09.2026 22:00 | Calix  | 10    | Eisenbarren | 100      | Einlagerung |
        | 01.09.2026 22:05 | Dorn   | 10    | Eisenbarren | 100      | Einlagerung |
      And Calix's stored movements cannot be read
      And the daily collection ran at 02.09.2026 05:00
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung |
        | Calix  | –           |
        | Dorn   | 720         |
      And Calix's row is marked "Noch nicht berechnet."
      And the guild row is marked "Enthält mindestens eine Zeile, die noch nicht berechnet ist."
