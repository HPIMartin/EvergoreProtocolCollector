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
      Today a headline whose date is written with single digits opens no entry of its own: it is
      taken as a line of the entry above, and the items below it are booked on that entry's member.
      The tagged example states the corrected behavior.

      Given the game's storage protocol shows:
        """
        03.01.2026 12:00 Bert Entnahme
        1 Eisenbarren
        <headline>
        1 Kupfererz
        01.01.2026 12:00 Anna Einlagerung
        1 Eisenbarren
        """
      And the daily collection ran at 04.01.2026 05:00
      When a member opens the storage ledgers of "Anna" and "Bert"
      Then the storage ledger of "Anna" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Anna   | 1     | Eisenbarren | 100      | Einlagerung |
      And the storage ledger of "Bert" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang  |
        | 03.01.2026 12:00 | Bert   | 1     | Eisenbarren | 100      | Entnahme |

      Examples:
        | what is wrong                                        | headline                                   |
        | with a garbled date                                  | 02.01X2026 12:00 Bad Einlagerung           |
        | with a date that does not exist                      | 31.13.2026 25:99 Bad Einlagerung           |
        | with a kind of movement the game never had           | 02.01.2026 12:00 Name Auszahlung           |
        | whose kind of movement is part of a word             | 02.01.2026 12:00 Anna Entnahmeübersicht    |
        | whose kind of movement is set in dashes              | 02.01.2026 12:00 XX-Entnahme-XX            |
        | whose kind of movement is glued to a name            | 02.01.2026 12:00 BobEntnahme               |
        | whose only kind of movement is the start of its name | 02.01.2026 12:00 Entnahmefreund Auszahlung |

      @wip
      Examples: corrected behavior; today its items are booked on the member of the entry above
        | what is wrong                            | headline                         |
        | whose date is written with single digits | 2.01.2026 12:00 Carl Einlagerung |

    Scenario Outline: A skipped headline <what is wrong> is named in the service's log
      Today a headline whose date is written with single digits is named nowhere: it is taken as a
      line of the entry above. The tagged example states the corrected behavior.

      Given the game's storage protocol shows:
        """
        03.01.2026 12:00 Bert Entnahme
        1 Eisenbarren
        <headline>
        1 Kupfererz
        """
      When the daily collection runs
      Then the operator finds the skipped headline "<headline>" named in the service's log

      Examples:
        | what is wrong                                        | headline                                   |
        | with a garbled date                                  | 02.01X2026 12:00 Bad Einlagerung           |
        | with a date that does not exist                      | 31.13.2026 25:99 Bad Einlagerung           |
        | with a kind of movement the game never had           | 02.01.2026 12:00 Name Auszahlung           |
        | whose kind of movement is part of a word             | 02.01.2026 12:00 Anna Entnahmeübersicht    |
        | whose kind of movement is set in dashes              | 02.01.2026 12:00 XX-Entnahme-XX            |
        | whose kind of movement is glued to a name            | 02.01.2026 12:00 BobEntnahme               |
        | whose only kind of movement is the start of its name | 02.01.2026 12:00 Entnahmefreund Auszahlung |

      @wip
      Examples: corrected behavior; today the log names nothing
        | what is wrong                            | headline                         |
        | whose date is written with single digits | 2.01.2026 12:00 Carl Einlagerung |

  Rule: A line the collection cannot read is skipped, and the rest of its entry is kept

    Scenario Outline: A line <what is wrong> is skipped and the rest of its entry is kept
      Given the game's storage protocol shows:
        """
        01.01.2026 12:00 Aurora Einlagerung
        <line>
        1 Eisenbarren
        """
      And the daily collection ran at 02.01.2026 05:00
      When a member opens the storage ledger of "Aurora"
      Then the storage ledger of "Aurora" shows exactly:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |

      Examples:
        | what is wrong                              | line                      |
        | whose quality is too large to be a number  | 1 Kupfererz (99999999999) |
        | whose quantity is too large to be a number | 99999999999 Kupfererz     |
        | without a quantity in front                | Kupfererz                 |

  Rule: The log names an item line whose number cannot be read, and stays silent on a line not shaped like an item line

    Scenario: A skipped item line whose number cannot be read is named in the service's log
      The third line below the headline is " Kupfererz", with one blank where the quantity belongs;
      the blank makes it an item line whose quantity cannot be read, unlike "Kupfererz" without it.

      Given the game's storage protocol shows:
        """
        01.01.2026 12:00 Aurora Einlagerung
        1 Kupfererz (99999999999)
        99999999999 Kupfererz
         Kupfererz
        1 Eisenbarren
        """
      When the daily collection runs
      Then the operator finds the skipped item line "1 Kupfererz (99999999999)" named in the service's log
      And the operator finds the skipped item line "99999999999 Kupfererz" named in the service's log
      And the operator finds the skipped item line " Kupfererz" named in the service's log

    Scenario: A line without a quantity in front is not named in the service's log
      Given the game's storage protocol shows:
        """
        01.01.2026 12:00 Aurora Einlagerung
        Kupfererz
        1 Eisenbarren
        """
      When the daily collection runs
      Then the operator finds no skipped item line named in the service's log

  Rule: An entry left with nothing to book books nothing and is named in the log

    Scenario: A gold amount written with a thousands separator is not read
      The line is skipped like any other line not shaped like an item line, and nothing of the entry
      is left to book.

      Given the game's bank protocol shows:
        """
        01.01.2026 12:00 Aurora Einzahlung
        1.000 Gold
        """
      And the daily collection ran at 02.01.2026 05:00
      When a member opens the bank ledger of "Aurora"
      Then the page says "Kein Avatar mit dem Namen Aurora."

    Scenario: An entry with no readable line left is named in the service's log
      Given the game's bank protocol shows:
        """
        01.01.2026 12:00 Aurora Einzahlung
        1.000 Gold
        """
      When the daily collection runs
      Then the operator finds the entry "01.01.2026 12:00 Aurora Einzahlung" named in the service's log as yielding nothing it could read
