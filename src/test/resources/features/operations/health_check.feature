@wip
Feature: The service's health check: its verdict
  The operator asks the service for its health, through monitoring and the deploy script. The
  verdict follows the last recompute of the guild's figures: unknown before any recompute was
  attempted since the service started, up while the last one succeeded, down once it failed. A
  failed read of the game alone does not bring the service down, since the figures are still
  recomputed from the stored movements. What the report names beside the verdict is in "The health
  report: what the last recompute found". The health check answers without the guild's token. The
  scenarios give every time as German wall-clock time, as the game shows it; the report itself
  writes it in UTC, so 01.09.2026 05:00 reads 2026-09-01T03:00:00Z.

  Rule: The verdict follows the last recompute of the guild's figures

    Scenario: Before any recompute the health is unknown
      Given no collection has ever run
      When the operator asks the service for its health
      Then the service reports its health as "UNKNOWN"

    Scenario: After a restart the health is unknown until the next recompute, even with figures stored
      Given the daily collection ran at 01.09.2026 05:00
      And the service has been restarted since
      When the operator asks the service for its health
      Then the service reports its health as "UNKNOWN"

    Scenario: After a successful collection the service is up
      Given the daily collection ran at 01.09.2026 05:00
      When the operator asks the service for its health
      Then the service reports its health as "UP"
      And the health report dates the last successful scrape and the last successful recompute at 01.09.2026 05:00

    Scenario: A collection that could not reach the game leaves the service up and says so
      Given the daily collection ran at 01.09.2026 05:00
      And the game cannot be reached
      And the daily collection ran at 02.09.2026 05:00
      When the operator asks the service for its health
      Then the service reports its health as "UP"
      And the health report dates the last scrape failure at 02.09.2026 05:00

    Scenario: A failed recompute brings the service down, even after an earlier success
      Given the daily collection ran at 01.09.2026 05:00
      And the guild's figures cannot be saved
      And the daily collection ran at 02.09.2026 05:00
      When the operator asks the service for its health
      Then the service reports its health as "DOWN"
      And the health report dates the last successful recompute at 01.09.2026 05:00
      And the health report dates the last recompute failure at 02.09.2026 05:00

    Scenario: A recompute that fails on the service's very first run brings it down, not to unknown
      Given no collection has ever run
      And the guild's figures cannot be saved
      And the daily collection ran at 01.09.2026 05:00
      When the operator asks the service for its health
      Then the service reports its health as "DOWN"
      And the health report dates the last recompute failure at 01.09.2026 05:00
      And the health report dates no successful recompute

    Scenario: The next successful collection brings the service up again and keeps the failures on record
      Given the daily collection ran at 01.09.2026 05:00
      And the game cannot be reached
      And the guild's figures cannot be saved
      And the daily collection ran at 02.09.2026 05:00
      And the game can be reached again
      And the guild's figures can be saved again
      And the daily collection ran at 03.09.2026 05:00
      When the operator asks the service for its health
      Then the service reports its health as "UP"
      And the health report dates the last recompute failure at 02.09.2026 05:00
      And the health report dates the last scrape failure at 02.09.2026 05:00

  Rule: The health check answers the operator without the guild's token

    Scenario: The health check answers without the guild's token
      Given the daily collection ran at 01.09.2026 05:00
      When the operator asks the service for its health without a token
      Then the service reports its health as "UP"
