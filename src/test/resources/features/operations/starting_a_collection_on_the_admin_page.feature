@wip
Feature: Starting a collection on the admin page
  Besides the daily collection, the admin starts a collection on the admin page with the button
  "Jetzt sammeln": it reads the game, recomputes the figures and creates the accounts like the daily
  one ("Collecting the guild protocols from the game", "Accounts for the members, and the admin"),
  and the admin page and the health report date it like any other ("The admin page: how the
  collections are going"). While a collection runs, daily or started by hand, the page says
  "Sammlung läuft seit <its start>" in place of the button, and no second one starts: a start asked
  for then is refused with "Es läuft bereits eine Sammlung.", and a daily collection falling due
  then is skipped and counts as no start. Since every collection asks the game for its protocols, a
  start by hand is accepted at the earliest an hour after the start of the last collection, daily or
  by hand; until then the page says "Nächste Sammlung ab <that moment> möglich." in place of the
  button, and a start asked for is refused with that sentence. The daily collection keeps its own
  schedule and runs whenever no collection is running. After a restart the service knows no last
  start until its next collection. The page shows all this as it stands when the admin opens it, and
  after it answers a start the admin asked for on it; otherwise it changes only when the admin opens
  it again. Who may start a collection is in "Accounts for the members, and the admin". In these
  scenarios the game is slow to answer: a collection a step says ran, or says has finished, is over;
  one a step says starts, started, falls due or fell due runs, unless the step says it was skipped,
  until a step says it finishes or has finished.

  Background:
    Given the guild storage ledger holds:
      | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
      | 01.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
    And the daily collection ran at 02.01.2026 05:00
    And the admin has logged in as "admin" with the admin key the service's log names

  Rule: No collection starts while another runs

    Scenario Outline: While <which> runs, the admin page says since when and offers no start
      When <starting>
      Then the admin page reads "Sammlung läuft seit <start>"
      And the admin page offers no "Jetzt sammeln"
      And the admin page reads no "Nächste Sammlung ab"

      Examples:
        | which                                  | starting                                                                                            | start            |
        | a collection started on the admin page | the admin starts a collection on the admin page at 02.01.2026 13:00                                 | 02.01.2026 13:00 |
        | the daily collection                   | the daily collection falls due at 03.01.2026 05:00 and the admin opens the admin page while it runs | 03.01.2026 05:00 |
        | a collection still running past its hour | the admin opens the admin page 61 minutes after starting a collection on it at 02.01.2026 13:00, while it runs | 02.01.2026 13:00 |

    Scenario: A start from a page opened before another collection began is refused, and the page says why
      Given the admin has opened the admin page at 02.01.2026 12:00, which offers "Jetzt sammeln"
      And the admin has started a collection on the admin page at 02.01.2026 13:00 on another browser
      When the admin starts a collection on the page opened before, while that collection runs
      Then the admin page reads "Es läuft bereits eine Sammlung."
      And the admin page reads "Sammlung läuft seit 02.01.2026 13:00"

    Scenario: A page opened while a collection runs keeps saying so after that collection has finished
      Given the admin started a collection on the admin page at 02.01.2026 13:00
      And the admin has opened the admin page while that collection was running
      When that collection finishes
      Then the page opened before still reads "Sammlung läuft seit 02.01.2026 13:00"
      And the page opened before still offers no "Jetzt sammeln"

    Scenario Outline: A start asked for while <which> runs, skipping the page, is refused, and only that collection runs
      Given a client has logged in as "admin" with the admin key the service's log names
      And <running>
      And the client has asked for a collection with its login while that collection was running
      And that collection has finished
      When the operator reads the service's log
      Then the operator finds the client's request refused for a collection already running
      And the operator finds <count> line "Collection started by hand." in the service's log since the start of that collection

      Examples:
        | which                                  | running                                                              | count |
        | a collection started on the admin page | the admin started a collection on the admin page at 02.01.2026 13:00 | one   |
        | the daily collection                   | the daily collection fell due at 03.01.2026 05:00                    | no    |

    Scenario: A daily collection falling due while a collection started on the admin page runs is skipped
      Given the admin started a collection on the admin page at 03.01.2026 04:30
      When the daily collection falls due at 03.01.2026 05:00 while that collection runs
      Then the operator finds the line "Daily collection skipped: a collection is already running." in the service's log
      And the operator finds no line "Daily collection started." in the service's log since the start of that collection

    Scenario: After a skipped daily collection, the next daily collection runs and the skipped one is not made up
      Given the admin started a collection on the admin page at 03.01.2026 04:30
      And the daily collection fell due at 03.01.2026 05:00 while that collection was running
      And that collection has finished
      When the daily collection falls due at 04.01.2026 05:00 and finishes
      Then the operator finds one line "Daily collection started." in the service's log since the start of the collection of 03.01.2026 04:30

  Rule: A start by hand is accepted at the earliest an hour after the start of the last collection, daily or by hand

    Scenario Outline: 59 minutes after the start of <last>, the admin page offers no start yet
      Given <started>
      When the admin opens the admin page 59 minutes after the start of <last>
      Then the admin page reads "Nächste Sammlung ab <next> möglich."
      And the admin page offers no "Jetzt sammeln"
      And the admin page reads no "Sammlung läuft seit"

      Examples:
        | last                                                      | started                                                                                                                                                   | next             |
        | the daily collection                                      | the daily collection was the last to start                                                                                                                | 02.01.2026 06:00 |
        | the collection started at 13:00                           | the admin started a collection on the admin page at 02.01.2026 13:00 and it finished 30 minutes later                                                    | 02.01.2026 14:00 |
        | the collection started at 13:00, which could not reach the game | the game cannot be reached and the admin started a collection on the admin page at 02.01.2026 13:00 and it finished 30 minutes later           | 02.01.2026 14:00 |
        | the collection started at 13:00, which could not save the figures | the guild's figures cannot be saved and the admin started a collection on the admin page at 02.01.2026 13:00 and it finished 30 minutes later | 02.01.2026 14:00 |
        | the daily collection of 03.01., after one started at 04:30 | the admin started a collection on the admin page at 03.01.2026 04:30 and it finished 20 minutes after its start, and then the daily collection fell due at 03.01.2026 05:00 and finished | 03.01.2026 06:00 |

    Scenario Outline: 60 minutes after the start of <last>, the admin page offers a start
      Given <started>
      When the admin opens the admin page 60 minutes after the start of <last>
      Then the admin page offers "Jetzt sammeln"
      And the admin page reads no "Nächste Sammlung ab"

      Examples:
        | last                                                  | started                                                                                                                                              |
        | the daily collection                                  | the daily collection was the last to start                                                                                                           |
        | the collection started at 13:00                       | the admin started a collection on the admin page at 02.01.2026 13:00 and it finished 30 minutes later                                               |
        | the daily collection, with a refused start since      | the daily collection was the last to start, and a start by hand was refused 30 minutes after it for coming within the hour                          |
        | the collection started at 04:30, with a skipped daily | the admin started a collection on the admin page at 03.01.2026 04:30, the daily collection fell due at 03.01.2026 05:00 while it was running and was skipped, and it finished 50 minutes after its start |

    Scenario: A collection started on the admin page that ran longer than the hour leaves the page offering a start once it has finished
      Given the admin started a collection on the admin page at 02.01.2026 13:00 and it finished 80 minutes later
      When the admin opens the admin page 90 minutes after the start of that collection
      Then the admin page offers "Jetzt sammeln"
      And the admin page reads no "Nächste Sammlung ab"
      And the admin page reads no "Sammlung läuft seit"

    Scenario: A start from a page that offered it, asked for after another start within the hour, is refused, and the page says until when
      Given the admin has opened the admin page 60 minutes after the start of the daily collection, which offers "Jetzt sammeln"
      And the admin started a collection on the admin page on another browser at 02.01.2026 07:00
      And that collection has finished
      When the admin starts a collection on the page opened before, 30 minutes after the start of that collection
      Then the admin page reads "Nächste Sammlung ab 02.01.2026 08:00 möglich."

    Scenario Outline: A start <after> after the start of the daily collection, skipping the page, is <answer>
      Given a client has logged in as "admin" with the admin key the service's log names
      And the client has asked for a collection with its login <after> after the start of the daily collection
      When the operator reads the service's log
      Then the operator finds the client's request <result>
      And the operator finds <count> line "Collection started by hand." in the service's log

      Examples:
        | after      | answer   | result                                                | count |
        | 59 minutes | refused  | refused for a collection started within the last hour | no    |
        | 60 minutes | accepted | accepted                                              | one   |

    Scenario: A daily collection falling due within the hour after a collection started on the admin page runs
      Given the admin started a collection on the admin page at 03.01.2026 04:30
      And that collection has finished
      When the daily collection falls due at 03.01.2026 05:00 and finishes
      Then the operator finds the health report dating the last successful scrape at 03.01.2026 05:00

    Scenario: After a restart the admin page offers a start, even within the hour of the last one
      Given the service has been restarted 30 minutes after the start of the daily collection
      And the admin has logged in as "admin" with the admin key the service's log names since the restart
      When the admin opens the admin page 59 minutes after the start of the daily collection
      Then the admin page offers "Jetzt sammeln"

  Rule: A start nobody with the admin role asked for starts no collection

    Scenario Outline: A start asked for <how>, skipping the page, starts no collection
      The request comes 60 minutes after the daily collection's start, when a login with the admin
      role would start one.

      Given Aurora has chosen the password "Sommer-2026!"
      And <asking>
      When the operator reads the service's log
      Then the operator finds no line "Collection started by hand." in the service's log

      Examples:
        | how                   | asking                                                                                                    |
        | with a member's login | a client logged in as "Aurora" with the password "Sommer-2026!" has asked for a collection with its login 60 minutes after the start of the daily collection |
        | without a login       | a client without a login has asked for a collection 60 minutes after the start of the daily collection    |
