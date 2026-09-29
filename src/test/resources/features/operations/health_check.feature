Feature: The service's health check: its verdict
  The operator asks the service for its health, through monitoring and the deploy script. The
  verdict follows the last recompute of the guild's figures: unknown before any recompute was
  attempted since the service started, up while the last one succeeded, down once it failed. A
  failed read of the game alone does not bring the service down, since the figures are still
  recomputed from the stored movements. The dates the report gives beside the verdict are in "The
  admin page: how the daily collection is going", and what it names is in "The health report: what
  the last recompute found". The health check answers without the guild's token. The scenarios give
  every time as German wall-clock time, as the game shows it; the report itself writes it in UTC,
  so 02.01.2026 05:00 reads 2026-01-02T04:00:00Z.

  Rule: The verdict follows the last recompute of the guild's figures

    Scenario: Before any recompute the health is unknown
      Given no collection has ever run
      When the operator asks the service for its health
      Then the service reports its health as "UNKNOWN"

    Scenario: After a restart the health is unknown until the next recompute, even with figures stored
      Given the daily collection ran at 02.01.2026 05:00
      And the service has been restarted since
      When the operator asks the service for its health
      Then the service reports its health as "UNKNOWN"

    Scenario: After a successful collection the service is up
      Given the daily collection ran at 02.01.2026 05:00
      When the operator asks the service for its health
      Then the service reports its health as "UP"

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

    Scenario: A recompute that fails on the service's very first run brings it down, not to unknown
      Given no collection has ever run
      And the guild's figures cannot be saved
      And the daily collection ran at 02.01.2026 05:00
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

  Rule: The health check answers the operator without the guild's token

    Scenario: The health check answers without the guild's token
      Given the daily collection ran at 02.01.2026 05:00
      When the operator asks the service for its health without a token
      Then the service reports its health as "UP"
