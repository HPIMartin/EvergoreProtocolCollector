Feature: The service's health check: its verdict
  The operator asks the service for its health, through monitoring and the deploy script. The
  verdict follows the last recompute of the guild's figures: unknown before any recompute was
  attempted since the service started, up while the last one succeeded, down once it failed. A
  failed read of the game alone does not bring the service down, since the figures are still
  recomputed from the stored movements. The dates the report gives beside the verdict are in "The
  admin page: how the daily collection is going", and what it names is in "The health report: what
  the last recompute found". Those dates and findings are for a login with the admin's role: the
  operator asks with the admin key the service names in its log at every start, while without the
  admin's login (no login, a member's login, or one the service did not issue or that has ended) the
  health check names its verdict and nothing else. "A recompute failed" means the run's figures
  could not be saved, on the service's first run. The scenarios give every time as German wall-clock
  time, as the game shows it; the report itself writes it in UTC, so 02.01.2026 05:00 reads
  2026-01-02T04:00:00Z.

  Rule: The verdict follows the last recompute of the guild's figures

    Scenario Outline: Asked <how> after <state>, the service reports its health as "<verdict>"
      Given <state>
      When <asking>
      Then the service reports its health as "<verdict>"

      Examples:
        | how                | state                                        | asking                                                          | verdict |
        | with the admin key | no collection has ever run                   | the operator asks the service for its health with the admin key | UNKNOWN |
        | without a login    | no collection has ever run                   | the operator asks the service for its health without a login    | UNKNOWN |
        | with the admin key | the daily collection ran at 02.01.2026 05:00 | the operator asks the service for its health with the admin key | UP      |
        | without a login    | the daily collection ran at 02.01.2026 05:00 | the operator asks the service for its health without a login    | UP      |
        | with the admin key | a recompute failed at 02.01.2026 05:00       | the operator asks the service for its health with the admin key | DOWN    |
        | without a login    | a recompute failed at 02.01.2026 05:00       | the operator asks the service for its health without a login    | DOWN    |

    Scenario: After a restart the health is unknown until the next recompute, even with figures stored
      Given the daily collection ran at 02.01.2026 05:00
      And the service has been restarted since
      When the operator asks the service for its health
      Then the service reports its health as "UNKNOWN"

    Scenario: A collection that could not reach the game leaves the service up
      Given the daily collection ran at 02.01.2026 05:00
      And the game cannot be reached
      And the daily collection ran at 03.01.2026 05:00
      When the operator asks the service for its health
      Then the service reports its health as "UP"

    Scenario: A recompute that could not refresh one member leaves the service up
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
        | 02.01.2026 12:00 | Boreas | 1     | Eisenbarren | 100      | Einlagerung |
      And Aurora's stored movements cannot be read
      And the daily collection has run
      When the operator asks the service for its health
      Then the service reports its health as "UP"

    Scenario: A failed recompute brings the service down, even after an earlier success
      Given the daily collection ran at 02.01.2026 05:00
      And the guild's figures cannot be saved
      And the daily collection ran at 03.01.2026 05:00
      When the operator asks the service for its health
      Then the service reports its health as "DOWN"

    Scenario: The next successful collection brings the service up again
      Given the daily collection ran at 02.01.2026 05:00
      And the game cannot be reached
      And the guild's figures cannot be saved
      And the daily collection ran at 03.01.2026 05:00
      And the game can be reached again
      And the guild's figures can be saved again
      And the daily collection ran at 04.01.2026 05:00
      When the operator asks the service for its health
      Then the service reports its health as "UP"

  Rule: Without the admin's login, the health check names its verdict and nothing else
    What an account with the admin role sees is in "Accounts for the members, and the admin".

    Scenario Outline: Asked <how> after a recompute that found an unknown item, the health check names its verdict and nothing else
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Unobtainium | 100      | Einlagerung |
      And the daily collection ran at 02.01.2026 05:00
      And Aurora has chosen the password "Sommer-2026!"
      When <asking>
      Then the operator finds the answer holding its verdict and nothing else

      Examples:
        | how                                                   | asking                                                                                                                    |
        | without a login                                       | the operator asks the service for its health without a login                                                              |
        | with a member's login                                 | a client logged in as "Aurora" with the password "Sommer-2026!" asks the service for its health                           |
        | with a login altered to claim the admin's role        | a client logged in as "Aurora" asks the service for its health with its login altered to claim the admin's role           |
        | with a login signed by someone other than the service | a client asks the service for its health with a login that names "Aurora" but is signed by someone other than the service |
        | with a login of "Aurora" that has run its 7 days      | a client asks the service for its health with a login of "Aurora" that has run its 7 days                                 |
