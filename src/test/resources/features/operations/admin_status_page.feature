Feature: The admin page: how the daily collection is going
  The admin page tells the admin how the daily collection is going: the date of the figures on show
  ("Stand"), when the game was last read ("Letzter Scrape") and the figures last recomputed
  ("Letzte Neuberechnung"), and when either last failed. The health report gives the operator the
  same dates, so the scenarios dating the runs speak for both. What the last recompute found is in
  "The admin page: what the last recompute found". The page states facts; judging the service
  healthy or not is the health check's job. The run outcomes are kept only while the service runs;
  the figures' date is stored with the figures. All times are German wall-clock time, as the game
  shows them; the health report writes them in UTC, as "The service's health check: its verdict"
  explains.

  Rule: The page dates the figures on show with the last recompute that succeeded

    Scenario: Before the first collection the page says that nothing has run yet
      Given no collection has ever run
      When the admin opens the admin page
      Then the admin page reads:
        | Stand: noch kein Abgleich gelaufen                      |
        | Letzter Scrape: noch kein Scrape gelaufen               |
        | Letzte Neuberechnung: noch keine Neuberechnung gelaufen |
      And the admin page reports no failure
      And the admin page names no finding

    Scenario Outline: The figures' date follows the recompute, not the read of the game
      Given the daily collection ran at 02.01.2026 05:00
      And <trouble>
      And the daily collection ran at 03.01.2026 05:00
      When the admin opens the admin page
      Then the admin page reads "Stand: <Stand>"

      Examples:
        | trouble                             | Stand            |
        | the game cannot be reached          | 03.01.2026 05:00 |
        | the guild's figures cannot be saved | 02.01.2026 05:00 |

    Scenario: A recompute that fails on the service's very first run leaves the page without figures
      Given no collection has ever run
      And the guild's figures cannot be saved
      And the daily collection ran at 02.01.2026 05:00
      When the admin opens the admin page
      Then the admin page reads "Stand: noch kein Abgleich gelaufen"

    Scenario: After a restart the page knows the figures' date but not yet the run outcomes
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Unobtainium | 100      | Einlagerung |
      And the daily collection ran at 02.01.2026 05:00
      And the service has been restarted since
      When the admin opens the admin page
      Then the admin page reads:
        | Stand: 02.01.2026 05:00                                 |
        | Letzter Scrape: noch kein Scrape gelaufen               |
        | Letzte Neuberechnung: noch keine Neuberechnung gelaufen |
      And the admin page lists no unknown item

  Rule: The admin page opens without the guild's token
    This holds until the admin gets a login of their own.

    Scenario: The admin page opens without the guild's token
      Given the daily collection ran at 02.01.2026 05:00
      When the admin opens the admin page without the guild's token
      Then the admin page reads "Stand: 02.01.2026 05:00"

  Rule: The admin page and the health report date each step of the daily collection
    The health check's verdict on the same runs is in "The service's health check: its verdict".

    Scenario Outline: After a successful collection <surface> dates the read of the game and the recompute at that collection
      Given the daily collection ran at 02.01.2026 05:00
      When <reading>
      Then <surface> dates the last successful scrape and the last successful recompute at 02.01.2026 05:00
      And <surface> reports no failure

      Examples:
        | surface           | reading                                      |
        | the admin page    | the admin opens the admin page               |
        | the health report | the operator asks the service for its health |

    Scenario Outline: A collection that could not reach the game is dated as a scrape failure on <surface>, and the figures are still recomputed
      Given the daily collection ran at 02.01.2026 05:00
      And the game cannot be reached
      And the daily collection ran at 03.01.2026 05:00
      When <reading>
      Then <surface> dates the last successful scrape at 02.01.2026 05:00
      And <surface> dates the last successful recompute at 03.01.2026 05:00
      And <surface> dates the last scrape failure at 03.01.2026 05:00

      Examples:
        | surface           | reading                                      |
        | the admin page    | the admin opens the admin page               |
        | the health report | the operator asks the service for its health |

    Scenario Outline: A recompute that failed as a whole is dated on <surface>, and the last successful recompute stays the run before
      Given the daily collection ran at 02.01.2026 05:00
      And the guild's figures cannot be saved
      And the daily collection ran at 03.01.2026 05:00
      When <reading>
      Then <surface> dates the last successful scrape at 03.01.2026 05:00
      And <surface> dates the last successful recompute at 02.01.2026 05:00
      And <surface> dates the last recompute failure at 03.01.2026 05:00

      Examples:
        | surface           | reading                                      |
        | the admin page    | the admin opens the admin page               |
        | the health report | the operator asks the service for its health |

    Scenario Outline: A recompute that fails on the service's very first run is dated on <surface> while no recompute has succeeded
      Given no collection has ever run
      And the guild's figures cannot be saved
      And the daily collection ran at 02.01.2026 05:00
      When <reading>
      Then <surface> dates the last successful scrape at 02.01.2026 05:00
      And <surface> dates no successful recompute
      And <surface> dates the last recompute failure at 02.01.2026 05:00

      Examples:
        | surface           | reading                                      |
        | the admin page    | the admin opens the admin page               |
        | the health report | the operator asks the service for its health |

    Scenario Outline: A failure stays dated on <surface> after the next successful collection
      Given the daily collection ran at 02.01.2026 05:00
      And the game cannot be reached
      And the guild's figures cannot be saved
      And the daily collection ran at 03.01.2026 05:00
      And the game can be reached again
      And the guild's figures can be saved again
      And the daily collection ran at 04.01.2026 05:00
      When <reading>
      Then <surface> dates the last successful scrape and the last successful recompute at 04.01.2026 05:00
      And <surface> dates the last scrape failure at 03.01.2026 05:00
      And <surface> dates the last recompute failure at 03.01.2026 05:00

      Examples:
        | surface           | reading                                      |
        | the admin page    | the admin opens the admin page               |
        | the health report | the operator asks the service for its health |
