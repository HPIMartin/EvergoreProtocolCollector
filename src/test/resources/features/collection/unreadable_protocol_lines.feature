@wip
Feature: Protocol lines the collection cannot read
  No unreadable line stops the collection. A headline that starts with a date and a time in the
  game's two-digit form but cannot be read is skipped together with its items and named in the
  service's log; its items are not booked on the member of the entry above, since that would move
  value from one member to another. An item line whose number cannot be read is skipped and named
  in the log, and the rest of its entry is kept. A line that is not shaped like an item line at all
  is skipped without a word; an entry left with no readable item line is named in the log. What was
  stored shows in the members' ledgers on the dashboard: a ledger that "shows exactly" some
  movements holds these and no others, newest first; movements of the same minute may stand in
  either order. The log is the operator's view of what was skipped.

  Rule: A headline that cannot be read is skipped together with its items

    Scenario Outline: A headline <what is wrong> is skipped without touching its neighbours
      Given the game's storage protocol shows:
        """
        01.01.2025 00:00 Anna Einlagerung
        1 Eisenbarren
        <headline>
        5 Kupfererz
        02.02.2025 12:00 Bert Entnahme
        2 Eisenbarren
        """
      When the daily collection runs
      Then the storage ledger of "Anna" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2025 00:00 | Anna   | 1     | Eisenbarren | 100      | Einlagerung |
      And the storage ledger of "Bert" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang  |
        | 02.02.2025 12:00 | Bert   | 2     | Eisenbarren | 100      | Entnahme |
      And the service's log names the skipped headline "<headline>"

      Examples:
        | what is wrong                              | headline                                   |
        | with a garbled date                        | 11.12X2024 13:37 Bad Einlagerung           |
        | with a date that does not exist            | 31.13.2024 25:99 Bad Einlagerung           |
        | with a kind of movement the game never had | 11.12.2024 13:37 Name Auszahlung           |
        | whose kind of movement is part of a word   | 11.12.2024 13:37 Anna Entnahmeübersicht    |
        | whose kind of movement is set in dashes    | 11.12.2024 13:37 XX-Entnahme-XX            |
        | whose kind of movement is glued to a name  | 11.12.2024 13:37 BobEntnahme               |
        | whose name starts with a kind of movement  | 11.12.2024 13:37 Entnahmefreund Auszahlung |

    @wip
    Scenario: A headline whose date is written with single digits is skipped together with its items
      Today such a headline opens no entry of its own: it is taken as a line of the entry above, and
      the items below it are booked on that entry's member. This states the corrected behavior.

      Given the game's storage protocol shows:
        """
        01.01.2025 00:00 Anna Einlagerung
        1 Eisenbarren
        1.12.2025 13:37 Carl Einlagerung
        5 Kupfererz
        02.02.2025 12:00 Bert Entnahme
        2 Eisenbarren
        """
      When the daily collection runs
      Then the storage ledger of "Anna" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2025 00:00 | Anna   | 1     | Eisenbarren | 100      | Einlagerung |
      And the storage ledger of "Bert" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang  |
        | 02.02.2025 12:00 | Bert   | 2     | Eisenbarren | 100      | Entnahme |
      And the service's log names the skipped headline "1.12.2025 13:37 Carl Einlagerung"

  Rule: An item line whose number cannot be read is skipped and named, and the rest of the entry is kept

    Scenario Outline: An item line whose <part> is too large to be a number is skipped
      Given the game's storage protocol shows:
        """
        11.12.2025 13:37 Aurora Einlagerung
        <line>
        2 Eisenbarren
        """
      When the daily collection runs
      Then the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 11.12.2025 13:37 | Aurora | 2     | Eisenbarren | 100      | Einlagerung |
      And the service's log names the skipped item line "<line>"

      Examples:
        | part     | line                      |
        | quality  | 1 Kupfererz (99999999999) |
        | quantity | 99999999999 Kupfererz     |

    Scenario: An item line that starts with a blank instead of a quantity is skipped
      The line below the headline is " Kupfererz", with one blank in front.

      Given the game's storage protocol shows:
        """
        11.12.2025 13:37 Aurora Einlagerung
         Kupfererz
        2 Eisenbarren
        """
      When the daily collection runs
      Then the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 11.12.2025 13:37 | Aurora | 2     | Eisenbarren | 100      | Einlagerung |
      And the service's log names the skipped item line " Kupfererz"

  Rule: A line not shaped like an item line is skipped without a word

    Scenario: A line without a quantity in front is skipped and not named
      Given the game's storage protocol shows:
        """
        11.12.2025 13:37 Aurora Einlagerung
        Kupfererz
        2 Eisenbarren
        """
      When the daily collection runs
      Then the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 11.12.2025 13:37 | Aurora | 2     | Eisenbarren | 100      | Einlagerung |
      And the service's log names no skipped item line

    Scenario: A gold amount written with a thousands separator is not read
      The line is skipped like any other line not shaped like an item line; the log names the entry
      because nothing of it is left.

      Given the game's bank protocol shows:
        """
        11.12.2025 13:37 Aurora Einzahlung
        1.000 Gold
        """
      When the daily collection runs
      Then the bank ledger of "Aurora" says "Kein Avatar mit dem Namen Aurora."
      And the service's log says the entry "11.12.2025 13:37 Aurora Einzahlung" yielded nothing it could read
