@wip
Feature: Collecting the guild protocols from the game
  Once a day the service signs in to the game, reads the guild bank's and the guild storage's
  transaction protocols, adds every movement it has not stored yet to the guild ledgers, and then
  recomputes every member's figures from their whole history. The game only shows the recent past,
  so the ledgers are the guild's lasting record. What a run stored shows in the members' ledgers on
  the dashboard: a ledger that "shows exactly" some movements holds these and no others, newest
  first; movements of the same minute may stand in either order. The bank ledger's "Vorgang" column
  is left out here; how it names a deposit is settled in "A member's bank and storage ledgers".

  Scenario: A collection run stores the movements the game's protocols show
    Given the game's bank protocol shows:
      """
      01.01.2026 12:00 Aurora Einzahlung
      100 Gold
      """
    And the game's storage protocol shows:
      """
      02.01.2026 12:00 Aurora Einlagerung
      1 Eisenbarren
      """
    And the daily collection ran at 03.01.2026 05:00
    When a member opens the bank ledger and the storage ledger of "Aurora"
    Then the bank ledger of "Aurora" shows exactly:
      | Zeitpunkt        | Avatar | Betrag |
      | 01.01.2026 12:00 | Aurora | 100    |
    And the storage ledger of "Aurora" shows exactly:
      | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
      | 02.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |

  Scenario: Protocols without an entry add nothing
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 100    | Einzahlung |
    And the game's protocols show no entry
    And the daily collection ran at 02.01.2026 05:00
    When a member opens the bank ledger and the storage ledger of "Aurora"
    Then the bank ledger of "Aurora" shows exactly:
      | Zeitpunkt        | Avatar | Betrag |
      | 01.01.2026 12:00 | Aurora | 100    |
    And the storage ledger of "Aurora" says "Für Aurora ist hier kein Vorgang gespeichert."

  Scenario: Movements a run collected stay in the ledger when the guild's figures cannot be saved
    Given the game's storage protocol shows:
      """
      01.01.2026 12:00 Aurora Einlagerung
      1 Eisenbarren
      """
    And the guild's figures cannot be saved
    And the daily collection ran at 02.01.2026 05:00
    When a member opens the storage ledger of "Aurora"
    Then the storage ledger of "Aurora" shows exactly:
      | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
      | 01.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |

  Rule: Each movement the game shows is stored once, however often a run reads it

    Scenario: A movement already stored is not stored again
      Given the game's bank protocol shows:
        """
        01.01.2026 12:00 Aurora Einzahlung
        100 Gold
        """
      And the daily collection ran at 02.01.2026 05:00
      And the daily collection ran at 03.01.2026 05:00
      When a member opens the bank ledger of "Aurora"
      Then the bank ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Betrag |
        | 01.01.2026 12:00 | Aurora | 100    |

    Scenario: Two identical movements in the same minute are both kept
      Given the game's bank protocol shows:
        """
        01.01.2026 12:00 Aurora Einzahlung
        100 Gold
        01.01.2026 12:00 Aurora Einzahlung
        100 Gold
        """
      And the daily collection ran at 02.01.2026 05:00
      And the daily collection ran at 03.01.2026 05:00
      When a member opens the bank ledger of "Aurora"
      Then the bank ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Betrag |
        | 01.01.2026 12:00 | Aurora | 100    |
        | 01.01.2026 12:00 | Aurora | 100    |

    Scenario: A movement an earlier run missed is added while the game still shows it
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 02.01.2026 12:00 | Aurora | 50     | Einzahlung |
      And the game's bank protocol shows:
        """
        02.01.2026 12:00 Aurora Einzahlung
        50 Gold
        01.01.2026 12:00 Aurora Einzahlung
        100 Gold
        """
      And the daily collection ran at 03.01.2026 05:00
      When a member opens the bank ledger of "Aurora"
      Then the bank ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Betrag |
        | 02.01.2026 12:00 | Aurora | 50     |
        | 01.01.2026 12:00 | Aurora | 100    |

    Scenario: Movements the game no longer shows stay in the ledger
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Aurora | 500    | Einzahlung |
      And the game's bank protocol shows:
        """
        02.01.2026 12:00 Aurora Einzahlung
        100 Gold
        """
      And the daily collection ran at 03.01.2026 05:00
      When a member opens the bank ledger of "Aurora"
      Then the bank ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Betrag |
        | 02.01.2026 12:00 | Aurora | 100    |
        | 01.01.2026 12:00 | Aurora | 500    |

  Rule: Every run recomputes the figures from the whole history

    Scenario: A second run recomputes the figures instead of adding to them
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
      And the daily collection ran at 02.01.2026 05:00
      And the daily collection ran at 03.01.2026 05:00
      When a member opens the overview
      Then Aurora's "Einlagerung" is 72

    Scenario: When the game cannot be reached, the stored movements are still recomputed
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
      And the game cannot be reached
      And the daily collection ran at 02.01.2026 05:00
      When a member opens the overview and the storage ledger of "Aurora"
      Then Aurora's "Einlagerung" is 72
      And the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
