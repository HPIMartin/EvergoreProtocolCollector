@wip
Feature: Reading the entries of the game's protocols
  The game's protocols list one entry per movement: a headline with the minute, the member and the
  kind of movement, followed by one line per item, or by the gold amount in the bank. The rules
  below say how the collection turns those lines into ledger movements, as the members' ledgers on
  the dashboard show them: a ledger that "shows exactly" some movements holds these and no others,
  newest first; movements of the same minute may stand in either order.

  Rule: Each headline opens one movement by one member

    Scenario Outline: A headline "<kind>" in the <protocol> protocol counts as "<column>"
      Given the game's <protocol> protocol shows:
        """
        11.12.2025 13:37 Aurora <kind>
        <line>
        """
      When the daily collection runs
      Then Aurora's "<column>" is <value>

      Examples:
        | protocol | kind        | line           | column          | value |
        | storage  | Einlagerung | 10 Eisenbarren | Einlagerung     | 720   |
        | storage  | Entnahme    | 10 Eisenbarren | Entnahme        | 720   |
        | bank     | Einzahlung  | 100 Gold       | Bank-Einzahlung | 100   |
        | bank     | Entnahme    | 100 Gold       | Bank-Auszahlung | 100   |

    Scenario: A member's name may consist of several words
      Given the game's storage protocol shows:
        """
        11.12.2025 13:37 Hans Meyer Einlagerung
        10 Eisenbarren
        """
      When the daily collection runs
      Then the storage ledger of "Hans Meyer" shows exactly:
        | Zeitpunkt        | Avatar     | Menge | Gegenstand  | Qualität | Vorgang     |
        | 11.12.2025 13:37 | Hans Meyer | 10    | Eisenbarren | 100      | Einlagerung |

    Scenario: A member's name may contain the word for a kind of movement
      Given the game's storage protocol shows:
        """
        11.12.2025 13:37 Entnahmefreund Einlagerung
        10 Eisenbarren
        """
      When the daily collection runs
      Then the storage ledger of "Entnahmefreund" shows exactly:
        | Zeitpunkt        | Avatar         | Menge | Gegenstand  | Qualität | Vorgang     |
        | 11.12.2025 13:37 | Entnahmefreund | 10    | Eisenbarren | 100      | Einlagerung |

    Scenario: Every headline opens an entry of its own
      Given the game's storage protocol shows:
        """
        12.12.2025 09:00 Boreas Entnahme
        2 Kupfererz
        11.12.2025 13:37 Aurora Einlagerung
        10 Eisenbarren
        """
      When the daily collection runs
      Then the storage ledger of "Boreas" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang  |
        | 12.12.2025 09:00 | Boreas | 2     | Kupfererz  | 100      | Entnahme |
      And the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 11.12.2025 13:37 | Aurora | 10    | Eisenbarren | 100      | Einlagerung |

  Rule: Each item line is one item of the entry, in its quantity and quality

    Scenario Outline: The item line "<line>" is <quantity> "<item>" of quality <quality>
      Given the game's storage protocol shows:
        """
        11.12.2025 13:37 Aurora Einlagerung
        <line>
        """
      When the daily collection runs
      Then the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge      | Gegenstand | Qualität  | Vorgang     |
        | 11.12.2025 13:37 | Aurora | <quantity> | <item>     | <quality> | Einlagerung |

      Examples:
        | line                           | quantity | item                    | quality |
        | 2 Kurzschwert (80)             | 2        | Kurzschwert             | 80      |
        | 10 Eisenbarren                 | 10       | Eisenbarren             | 100     |
        | 200 Heilsamer Seidenverband +1 | 200      | Heilsamer Seidenverband | 100     |
        | 2 Kurzschwert (80) +1          | 2        | Kurzschwert             | 80      |

    Scenario: Lines of the same item in the same quality are added together
      Given the game's storage protocol shows:
        """
        11.12.2025 13:37 Aurora Einlagerung
        2 Kupfererz
        3 Kupfererz
        """
      When the daily collection runs
      Then the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 11.12.2025 13:37 | Aurora | 5     | Kupfererz  | 100      | Einlagerung |

    Scenario: An item marked "+1" is added to the same item without the mark
      Given the game's storage protocol shows:
        """
        11.12.2025 13:37 Aurora Einlagerung
        3 Heilsamer Seidenverband +1
        5 Heilsamer Seidenverband
        """
      When the daily collection runs
      Then the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand              | Qualität | Vorgang     |
        | 11.12.2025 13:37 | Aurora | 8     | Heilsamer Seidenverband | 100      | Einlagerung |

    Scenario: Lines of the same item in different qualities stay apart
      Given the game's storage protocol shows:
        """
        11.12.2025 13:37 Aurora Einlagerung
        1 Kurzschwert (50)
        2 Kurzschwert (60)
        """
      When the daily collection runs
      Then the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 11.12.2025 13:37 | Aurora | 1     | Kurzschwert | 50       | Einlagerung |
        | 11.12.2025 13:37 | Aurora | 2     | Kurzschwert | 60       | Einlagerung |

    Scenario: Text above the first entry and from the "Impressum" line on is not read
      Given the game's storage protocol shows:
        """
        Transaktionsbericht
        11.12.2025 13:37 Aurora Einlagerung
        10 Eisenbarren
        Impressum
        5 Kupfererz
        """
      When the daily collection runs
      Then the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 11.12.2025 13:37 | Aurora | 10    | Eisenbarren | 100      | Einlagerung |
