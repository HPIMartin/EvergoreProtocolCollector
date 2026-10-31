@wip
Feature: The admin page leads to the members it names
  Every member the admin page names in what the last recompute found leads to that member's
  storage ledger, so the admin follows a finding without typing the name on another page: a
  suspected round trip ("Verdacht auf Warenkreislauf"), a pair not judgeable ("Nicht beurteilbar
  (Rezept ungelesen)") and a member whose figures could not be refreshed ("Nicht aktualisierte
  Avatare"). The member's name in each entry is the link, also where one member has several
  entries; the rest of an entry, and an unknown item ("Unbekannte Items"), lead nowhere. The
  ledger's frame is the one "Finding one's way through the dashboard" describes, with no way back
  to the admin page; the admin opens the admin page directly.

  Background:
    Hans Meyer withdrew "Federn" and deposited them again within 48 hours, and Ärger did the same
    with "Pfeile" and with "Bolzen". Brynja withdrew "Kristallat", then deposited an
    "Achat-Armbrust", whose recipe no page of the game shows, before the "Kristallat" came back. The
    collection could not refresh Calix's figures, though his movement stays stored and his ledger
    shows it. Nobody knows the item "Unobtainium".

    Given the guild storage ledger holds:
      | Zeitpunkt        | Avatar     | Menge | Gegenstand     | Qualität | Vorgang     |
      | 01.01.2026 12:00 | Hans Meyer | 1     | Federn         | 100      | Entnahme    |
      | 01.01.2026 12:00 | Ärger      | 100   | Pfeile         | 100      | Entnahme    |
      | 01.01.2026 12:00 | Ärger      | 100   | Bolzen         | 100      | Entnahme    |
      | 01.01.2026 12:00 | Brynja     | 100   | Kristallat     | 100      | Entnahme    |
      | 01.01.2026 12:00 | Calix      | 1     | Eisenbarren    | 100      | Einlagerung |
      | 01.01.2026 12:00 | Aurora     | 1     | Unobtainium    | 100      | Einlagerung |
      | 01.01.2026 13:00 | Hans Meyer | 1     | Federn         | 100      | Einlagerung |
      | 01.01.2026 13:00 | Ärger      | 100   | Pfeile         | 100      | Einlagerung |
      | 01.01.2026 13:00 | Ärger      | 100   | Bolzen         | 100      | Einlagerung |
      | 01.01.2026 13:00 | Brynja     | 1     | Achat-Armbrust | 100      | Einlagerung |
      | 01.01.2026 14:00 | Brynja     | 100   | Kristallat     | 100      | Einlagerung |
    And Calix's stored movements cannot be read
    And the daily collection has run

  Rule: The name in each entry leads to that member's storage ledger

    Scenario Outline: The name in "<entry>" under "<list>" leads to the storage ledger of "<member>"
      Given the admin has opened the admin page
      When the admin follows the name in the entry "<entry>" under "<list>" on the admin page
      Then the page is headed "Lager von <member>"
      And the ledger shows a movement of <quantity> "<item>"

      Examples:
        | list                                 | entry                  | member     | quantity | item           |
        | Verdacht auf Warenkreislauf          | Hans Meyer: 1 × Federn | Hans Meyer | 1        | Federn         |
        | Verdacht auf Warenkreislauf          | Ärger: 100 × Bolzen    | Ärger      | 100      | Bolzen         |
        | Verdacht auf Warenkreislauf          | Ärger: 100 × Pfeile    | Ärger      | 100      | Pfeile         |
        | Nicht beurteilbar (Rezept ungelesen) | Brynja: Kristallat     | Brynja     | 100      | Kristallat     |
        | Nicht aktualisierte Avatare          | Calix                  | Calix      | 1        | Eisenbarren    |

  Rule: In what the last recompute found, only a member's name is a link

    Scenario Outline: Beside the name, "<entry>" under "<list>" leads nowhere
      When the admin opens the admin page
      Then in the entry "<entry>" under "<list>" only "<member>" leads anywhere

      Examples:
        | list                                 | entry                  | member     |
        | Verdacht auf Warenkreislauf          | Hans Meyer: 1 × Federn | Hans Meyer |
        | Nicht beurteilbar (Rezept ungelesen) | Brynja: Kristallat     | Brynja     |

    Scenario: An unknown item on the admin page leads nowhere
      When the admin opens the admin page
      Then "Unobtainium" stands under "Unbekannte Items" on the admin page and leads nowhere
