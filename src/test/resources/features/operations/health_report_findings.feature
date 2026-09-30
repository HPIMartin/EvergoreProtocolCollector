Feature: The health report: what the last recompute found
  Beside its verdict the health report names what the last successful recompute found that the
  operator needs to act on: item names the price list does not know, items known to be worth
  nothing, members whose figures could not be refreshed, suspected round trips and the pairs it
  could not judge. Each finding is counted and listed; a finding with nothing in it is left out.
  The members whose figures could not be refreshed are in "The admin page: what the last recompute
  found", the suspected round trips in "Suspected round trips of trader goods", and the pairs it
  could not judge in "Products of unread recipe". Where the admin page shows the same list, the
  rules below hold for it too.

  Rule: The report names what the last successful recompute found

    Scenario: The health report names unknown items apart from items known to be worth nothing
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand                | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Unobtainium               | 100      | Einlagerung |
        | 02.01.2026 12:00 | Aurora | 1     | Übungsstück-Kupferschwert | 100      | Einlagerung |
      And the daily collection has run
      When the operator asks the service for its health
      Then the health report names "Unobtainium" as an unknown item
      And the health report names "Übungsstück-Kupferschwert" as an item worth nothing

  Rule: A finding with nothing in it is left out
    The admin page leaves out a list with nothing in it in the same way.

    Scenario Outline: After a recompute that found nothing, <surface> names no finding
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
      And the daily collection has run
      When <reading>
      Then <surface> names no finding

      Examples:
        | surface           | reading                                      |
        | the admin page    | the admin opens the admin page               |
        | the health report | the operator asks the service for its health |

  Rule: A count says how many different names its list holds
    A name that occurs several times in the ledgers is listed once and counted once.

    Scenario Outline: An <finding> that occurs twice is counted once
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | <item>     | 100      | Einlagerung |
        | 02.01.2026 12:00 | Boreas | 1     | <item>     | 100      | Entnahme    |
      And the daily collection has run
      When the operator asks the service for its health
      Then the health report counts 1 <finding>

      Examples:
        | finding            | item                      |
        | unknown item       | Unobtainium               |
        | item worth nothing | Übungsstück-Kupferschwert |

  Rule: Every name list on the admin page and in the health report is in German alphabetical order
    "Ärger" comes before "Zorn", as in the overview.
    The admin page shows no list of items worth nothing.

    Scenario Outline: <surface> lists <list> in German alphabetical order
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand               | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Zorn   | 1     | Federn                   | 100      | Entnahme    |
        | 01.01.2026 13:00 | Zorn   | 1     | Federn                   | 100      | Einlagerung |
        | 01.01.2026 14:00 | Zorn   | 1     | Kristallat               | 100      | Entnahme    |
        | 01.01.2026 15:00 | Zorn   | 1     | Achat-Armbrust           | 100      | Einlagerung |
        | 01.01.2026 16:00 | Ärger  | 1     | Federn                   | 100      | Entnahme    |
        | 01.01.2026 17:00 | Ärger  | 1     | Federn                   | 100      | Einlagerung |
        | 01.01.2026 18:00 | Ärger  | 1     | Kristallat               | 100      | Entnahme    |
        | 01.01.2026 19:00 | Ärger  | 1     | Achat-Armbrust           | 100      | Einlagerung |
        | 02.01.2026 12:00 | Boreas | 1     | Zunder                   | 100      | Einlagerung |
        | 03.01.2026 12:00 | Boreas | 1     | Äxtchen                  | 100      | Einlagerung |
        | 04.01.2026 12:00 | Boreas | 1     | Übungsstück-Wollrüstung  | 100      | Einlagerung |
        | 05.01.2026 12:00 | Boreas | 1     | Übungsstück-Ätherrüstung | 100      | Einlagerung |
        | 06.01.2026 12:00 | Zeder  | 1     | Eisenbarren              | 100      | Einlagerung |
        | 07.01.2026 12:00 | Ähre   | 1     | Eisenbarren              | 100      | Einlagerung |
      And Zeder's stored movements cannot be read
      And Ähre's stored movements cannot be read
      And the daily collection has run
      When <reading>
      Then <surface> lists <list> in this order: <order>

      Examples:
        | surface           | reading                                      | list                                             | order                                                 |
        | the admin page    | the admin opens the admin page               | the unknown items                                | "Äxtchen", "Zunder"                                   |
        | the admin page    | the admin opens the admin page               | the members whose figures could not be refreshed | "Ähre", "Zeder"                                       |
        | the admin page    | the admin opens the admin page               | the suspected round trips                        | "Ärger: 1 × Federn", "Zorn: 1 × Federn"               |
        | the admin page    | the admin opens the admin page               | the pairs not judgeable                          | "Ärger: Kristallat", "Zorn: Kristallat"               |
        | the health report | the operator asks the service for its health | the unknown items                                | "Äxtchen", "Zunder"                                   |
        | the health report | the operator asks the service for its health | the members whose figures could not be refreshed | "Ähre", "Zeder"                                       |
        | the health report | the operator asks the service for its health | the suspected round trips                        | "Ärger: 1 × Federn", "Zorn: 1 × Federn"               |
        | the health report | the operator asks the service for its health | the pairs not judgeable                          | "Ärger: Kristallat", "Zorn: Kristallat"               |
        | the health report | the operator asks the service for its health | the items worth nothing                          | "Übungsstück-Ätherrüstung", "Übungsstück-Wollrüstung" |
