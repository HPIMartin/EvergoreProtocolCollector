Feature: The admin page: what the last recompute found
  Beside the dates, the admin page lists what the last successful recompute found that needs the
  admin's attention: item names the price list does not know ("Unbekannte Items") and members whose
  figures could not be refreshed ("Nicht aktualisierte Avatare"). Suspected round trips are in
  "Suspected round trips of trader goods", and the pairs it could not judge ("Nicht beurteilbar
  (Rezept ungelesen)") in "Products of unread recipe". A list with nothing in it is not shown, and
  every list is in German alphabetical order; both rules are in "The health report: what the last
  recompute found".

  Rule: The page lists what the last successful recompute found

    Scenario: An item name the price list does not know is listed once
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Unobtainium | 100      | Einlagerung |
        | 02.01.2026 12:00 | Boreas | 1     | Unobtainium | 100      | Entnahme    |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Unbekannte Items: Unobtainium"

    Scenario: The findings of the last successful recompute stay listed when a later recompute fails
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Unobtainium | 100      | Einlagerung |
      And the daily collection ran at 02.01.2026 05:00
      And the guild's figures cannot be saved
      And the daily collection ran at 03.01.2026 05:00
      When the admin opens the admin page
      Then the admin page reads "Unbekannte Items: Unobtainium"

  Rule: The admin page and the health report name a member whose figures could not be refreshed

    Scenario Outline: A member whose figures could not be refreshed is named on <surface>
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
        | 02.01.2026 12:00 | Boreas | 1     | Eisenbarren | 100      | Einlagerung |
      And Aurora's stored movements cannot be read
      And the daily collection has run
      When <reading>
      Then <surface> names "Aurora" as a member whose figures could not be refreshed

      Examples:
        | surface           | reading                                      |
        | the admin page    | the admin opens the admin page               |
        | the health report | the operator asks the service for its health |
