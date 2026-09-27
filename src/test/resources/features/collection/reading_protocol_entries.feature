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
        01.01.2026 12:00 Aurora <kind>
        <line>
        """
      And the daily collection ran at 02.01.2026 05:00
      When a member opens the overview
      Then Aurora's "<column>" is <value>

      Examples:
        | protocol | kind        | line          | column          | value |
        | storage  | Einlagerung | 1 Eisenbarren | Einlagerung     | 72    |
        | storage  | Entnahme    | 1 Eisenbarren | Entnahme        | 72    |
        | bank     | Einzahlung  | 100 Gold      | Bank-Einzahlung | 100   |
        | bank     | Entnahme    | 100 Gold      | Bank-Auszahlung | 100   |

    Scenario Outline: A member's name may <shape>
      Given the game's storage protocol shows:
        """
        01.01.2026 12:00 <name> Einlagerung
        1 Eisenbarren
        """
      And the daily collection ran at 02.01.2026 05:00
      When a member opens the storage ledger of "<name>"
      Then the storage ledger of "<name>" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | <name> | 1     | Eisenbarren | 100      | Einlagerung |

      Examples:
        | shape                                   | name           |
        | consist of several words                | Hans Meyer     |
        | contain the word for a kind of movement | Entnahmefreund |

    Scenario: Every headline opens an entry of its own
      Given the game's storage protocol shows:
        """
        02.01.2026 12:00 Boreas Entnahme
        1 Kupfererz
        01.01.2026 12:00 Aurora Einlagerung
        1 Eisenbarren
        """
      And the daily collection ran at 03.01.2026 05:00
      When a member opens the storage ledgers of "Boreas" and "Aurora"
      Then the storage ledger of "Boreas" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang  |
        | 02.01.2026 12:00 | Boreas | 1     | Kupfererz  | 100      | Entnahme |
      And the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |

  Rule: Each item line is one item of the entry, in its quantity and quality

    Scenario Outline: The item line "<line>" is <quantity> "<item>" of quality <quality>
      Given the game's storage protocol shows:
        """
        01.01.2026 12:00 Aurora Einlagerung
        <line>
        """
      And the daily collection ran at 02.01.2026 05:00
      When a member opens the storage ledger of "Aurora"
      Then the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge      | Gegenstand | Qualität  | Vorgang     |
        | 01.01.2026 12:00 | Aurora | <quantity> | <item>     | <quality> | Einlagerung |

      Examples:
        | line                           | quantity | item                    | quality |
        | 2 Kurzschwert (80)             | 2        | Kurzschwert             | 80      |
        | 10 Eisenbarren                 | 10       | Eisenbarren             | 100     |
        | 200 Heilsamer Seidenverband +1 | 200      | Heilsamer Seidenverband | 100     |
        | 2 Kurzschwert (80) +1          | 2        | Kurzschwert             | 80      |

    Scenario Outline: Lines of the same item in the same quality are added together
      An item marked "+1" counts as the same item without the mark.

      Given the game's storage protocol shows:
        """
        01.01.2026 12:00 Aurora Einlagerung
        <first line>
        <second line>
        """
      And the daily collection ran at 02.01.2026 05:00
      When a member opens the storage ledger of "Aurora"
      Then the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge      | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | <quantity> | <item>     | 100      | Einlagerung |

      Examples:
        | first line                   | second line               | quantity | item                    |
        | 1 Kupfererz                  | 2 Kupfererz               | 3        | Kupfererz               |
        | 1 Heilsamer Seidenverband +1 | 2 Heilsamer Seidenverband | 3        | Heilsamer Seidenverband |

    Scenario: Lines of the same item in different qualities stay apart
      Given the game's storage protocol shows:
        """
        01.01.2026 12:00 Aurora Einlagerung
        1 Kurzschwert (50)
        2 Kurzschwert (60)
        """
      And the daily collection ran at 02.01.2026 05:00
      When a member opens the storage ledger of "Aurora"
      Then the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Kurzschwert | 50       | Einlagerung |
        | 01.01.2026 12:00 | Aurora | 2     | Kurzschwert | 60       | Einlagerung |

    Scenario: Text above the first entry and from the "Impressum" line on is not read
      Given the game's storage protocol shows:
        """
        Transaktionsbericht
        01.01.2026 12:00 Aurora Einlagerung
        1 Eisenbarren
        Impressum
        1 Kupfererz
        """
      And the daily collection ran at 02.01.2026 05:00
      When a member opens the storage ledger of "Aurora"
      Then the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
