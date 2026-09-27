@wip
Feature: The admin page: what the last recompute found
  Beside the dates, the admin page lists what the last successful recompute found that needs the
  admin's attention: item names the price list does not know ("Unbekannte Items") and members whose
  figures could not be refreshed ("Nicht aktualisierte Avatare"). Suspected round trips are in
  "Suspected round trips of trader goods". A list with nothing in it is not shown.

  Rule: The page lists what the last successful recompute found

    Scenario: Item names the price list does not know are listed once each, in alphabetical order
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 1     | Unobtainium | 100      | Einlagerung |
        | 16.01.2024 10:00 | Boreas | 1     | Unobtainium | 100      | Entnahme    |
        | 17.01.2024 10:00 | Boreas | 1     | Marmorstein | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Unbekannte Items: Marmorstein, Unobtainium"

    Scenario: Members whose figures could not be refreshed are named
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 10    | Eisenbarren | 100      | Einlagerung |
        | 15.01.2024 11:00 | Boreas | 10    | Eisenbarren | 100      | Einlagerung |
      And Aurora's stored movements cannot be read
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Nicht aktualisierte Avatare: Aurora"

    Scenario: The findings of the last successful recompute stay listed when a later recompute fails
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 1     | Unobtainium | 100      | Einlagerung |
      And the daily collection ran at 01.09.2026 05:00
      And the guild's figures cannot be saved
      And the daily collection ran at 02.09.2026 05:00
      When the admin opens the admin page
      Then the admin page reads "Unbekannte Items: Unobtainium"
      And the admin page reads "Letzter Fehler bei der Neuberechnung: 02.09.2026 05:00"

  Rule: The lists name their entries in German alphabetical order
    Today both lists are sorted letter by letter with umlauts after "z", so "Ärger" follows "Zorn",
    unlike the overview. The two scenarios below state the corrected behavior.

    @wip
    Scenario: Unknown item names are listed in German alphabetical order
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 1     | Zunder     | 100      | Einlagerung |
        | 15.01.2024 11:00 | Aurora | 1     | Äxtchen    | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Unbekannte Items: Äxtchen, Zunder"

    @wip
    Scenario: Members whose figures could not be refreshed are listed in German alphabetical order
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Zorn   | 10    | Eisenbarren | 100      | Einlagerung |
        | 15.01.2024 11:00 | Ärger  | 10    | Eisenbarren | 100      | Einlagerung |
        | 15.01.2024 12:00 | Boreas | 10    | Eisenbarren | 100      | Einlagerung |
      And Zorn's stored movements cannot be read
      And Ärger's stored movements cannot be read
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Nicht aktualisierte Avatare: Ärger, Zorn"
