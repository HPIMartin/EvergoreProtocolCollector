Feature: Active and dormant members
  The overview splits the roster into two tables: members who moved something in the 30 days
  before the newest movement the guild ledgers hold stand in the active table above, everyone else
  in the dormant table below. The cut is measured against the newest movement, not against today,
  so a collection that stalls does not make the whole guild look dormant.

  Rule: A member who moved something in the 30 days before the newest movement is active

    Background:
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 00:00 | Brynja | 100    | Einzahlung |
        | 01.01.2026 11:59 | Calix  | 300    | Entnahme   |
        | 01.01.2026 12:00 | Boreas | 750    | Einzahlung |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 15.01.2026 12:00 | Brynja | 1     | Eisenbarren | 100      | Einlagerung |
        | 31.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
      And the daily collection has run

    Scenario: A member whose last movement lies within the 30 days before the newest one is active, from the window's first minute on
      Boreas moved gold exactly 30 days before Aurora's newest movement, Calix one minute earlier.

      When a member opens the overview
      Then the active table lists "Aurora, Boreas, Brynja"
      And the dormant table lists "Calix"

    Scenario: The later of a member's two ledgers decides
      Brynja's last bank movement is more than 30 days old, her last storage movement is not.

      When a member opens the overview
      Then "Brynja" stands in the active table

    Scenario: Long after the newest movement the member who made it is still active
      On 31.12.2026 the newest movement is eleven months old. The split reads no clock; a cut against
      the member's clock would put the whole guild in the dormant table as soon as the collection
      stalls.

      Given the clocks of the service and of the member's browser read 31.12.2026 12:00
      When a member opens the overview
      Then "Aurora" stands in the active table

    Scenario: Each table's caption counts its members against the whole guild
      When a member opens the overview
      Then the active table's caption reads "Aktiv (30 Tage vor dem letzten Vorgang): 3 von 4 Avataren"
      And the dormant table's caption reads "Ruhend: 1 von 4 Avataren"

    Scenario: The guild row stands once, under the active table, and includes the dormant members
      When a member opens the overview
      Then the active table ends with the guild row
      And the guild row shows:
        | Avatar | Bank-Einzahlung | Bank-Auszahlung |
        | Gilde  | 850             | 300             |
      And the dormant table has no guild row

  Rule: While every member moved something within the window, nobody is dormant

    Scenario: Nobody is dormant while every member moved something within the window
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Boreas | 750    | Einzahlung |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 02.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
      And the daily collection has run
      When a member opens the overview
      Then the dormant table says "Kein Avatar ruht."
