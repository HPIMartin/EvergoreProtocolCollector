@wip
Feature: The admin page: how the daily collection is going
  The admin page tells the admin how the daily collection is going: the date of the figures on show
  ("Stand"), when the game was last read ("Letzter Scrape") and the figures last recomputed
  ("Letzte Neuberechnung"), and when either last failed. What the last recompute found is in "The
  admin page: what the last recompute found". It states facts; judging the service healthy or not
  is the health check's job. The run outcomes are kept only while the service runs; the figures'
  date is stored with the figures. All times are German wall-clock time, as the game shows them.

  Rule: The page dates each step of the daily collection

    Scenario: Before the first collection the page says that nothing has run yet
      Given no collection has ever run
      When the admin opens the admin page
      Then the admin page reads:
        | Stand: noch kein Abgleich gelaufen                      |
        | Letzter Scrape: noch kein Scrape gelaufen               |
        | Letzte Neuberechnung: noch keine Neuberechnung gelaufen |
      And the admin page reports no failure
      And the admin page lists no unknown item
      And the admin page lists no member left unrefreshed
      And the admin page lists no suspected round trip
      And the admin page lists no pair as not judgeable

    @characterization
    Scenario: The admin page opens without the guild's token
      Given the daily collection ran at 01.09.2026 05:00
      When the admin opens the admin page without the guild's token
      Then the admin page reads "Stand: 01.09.2026 05:00"

    Scenario: After a successful collection the page dates each step and lists nothing
      Given the daily collection ran at 01.09.2026 05:00
      When the admin opens the admin page
      Then the admin page reads:
        | Stand: 01.09.2026 05:00                |
        | Letzter Scrape: 01.09.2026 05:00       |
        | Letzte Neuberechnung: 01.09.2026 05:00 |
      And the admin page reports no failure
      And the admin page lists no unknown item
      And the admin page lists no member left unrefreshed
      And the admin page lists no suspected round trip
      And the admin page lists no pair as not judgeable

    Scenario: A collection that could not reach the game is reported, and the figures are still recomputed
      Given the daily collection ran at 01.09.2026 05:00
      And the game cannot be reached
      And the daily collection ran at 02.09.2026 05:00
      When the admin opens the admin page
      Then the admin page reads:
        | Stand: 02.09.2026 05:00                 |
        | Letzter Scrape: 01.09.2026 05:00        |
        | Letzte Neuberechnung: 02.09.2026 05:00  |
        | Letzter Scrape-Fehler: 02.09.2026 05:00 |

    Scenario: A recompute that failed as a whole is reported, and the figures stay those of the run before
      Given the daily collection ran at 01.09.2026 05:00
      And the guild's figures cannot be saved
      And the daily collection ran at 02.09.2026 05:00
      When the admin opens the admin page
      Then the admin page reads:
        | Stand: 01.09.2026 05:00                                |
        | Letzter Scrape: 02.09.2026 05:00                       |
        | Letzte Neuberechnung: 01.09.2026 05:00                 |
        | Letzter Fehler bei der Neuberechnung: 02.09.2026 05:00 |

    Scenario: A recompute that fails on the service's very first run is reported while the page has no figures yet
      Given no collection has ever run
      And the guild's figures cannot be saved
      And the daily collection ran at 01.09.2026 05:00
      When the admin opens the admin page
      Then the admin page reads:
        | Stand: noch kein Abgleich gelaufen                      |
        | Letzter Scrape: 01.09.2026 05:00                        |
        | Letzte Neuberechnung: noch keine Neuberechnung gelaufen |
        | Letzter Fehler bei der Neuberechnung: 01.09.2026 05:00  |

    Scenario: A failure stays on the page after the next successful collection
      Given the daily collection ran at 01.09.2026 05:00
      And the game cannot be reached
      And the guild's figures cannot be saved
      And the daily collection ran at 02.09.2026 05:00
      And the game can be reached again
      And the guild's figures can be saved again
      And the daily collection ran at 03.09.2026 05:00
      When the admin opens the admin page
      Then the admin page reads:
        | Stand: 03.09.2026 05:00                                |
        | Letzter Scrape: 03.09.2026 05:00                       |
        | Letzte Neuberechnung: 03.09.2026 05:00                 |
        | Letzter Scrape-Fehler: 02.09.2026 05:00                |
        | Letzter Fehler bei der Neuberechnung: 02.09.2026 05:00 |

    Scenario: After a restart the page knows the figures' date but not yet the run outcomes
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 1     | Unobtainium | 100      | Einlagerung |
      And the daily collection ran at 01.09.2026 05:00
      And the service has been restarted since
      When the admin opens the admin page
      Then the admin page reads:
        | Stand: 01.09.2026 05:00                                 |
        | Letzter Scrape: noch kein Scrape gelaufen               |
        | Letzte Neuberechnung: noch keine Neuberechnung gelaufen |
      And the admin page lists no unknown item
