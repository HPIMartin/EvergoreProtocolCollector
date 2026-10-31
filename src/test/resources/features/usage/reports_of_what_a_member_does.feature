@wip
Feature: Reports of what a member does on the page
  The member's page tells the service each view it shows and each option the member uses, as
  "What the guild uses" counts them. The service takes a report only with a login ("Who the
  service lets in"), only of a view, an option and a value the page has, and only as large as such
  a report is. A report it cannot read as one of those is refused as unreadable, one too large as
  too large, and neither counts anywhere, so no text of a client's own reaches the records; a view
  the page never reports is refused, while the page's own "no view" for a link to a page the
  dashboard does not have counts as "Keine Ansicht". For a ledger the service itself decides
  whether it is the reporting member's own, as "What the guild uses" says; a name longer than 64
  characters, or holding a character no avatar's name holds (a line break, a control character, an
  angle bracket), is refused. A report counts like any request with a login ("Request throttling
  with a login"), and one turned away for sending too many requests counts nowhere.

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      | 02.01.2026 12:00 | Boreas | 750    | Einzahlung |
    And the daily collection has run
    And Aurora has chosen the password "Sommer-2026!"

  Rule: The service takes only what the page reports, and counts only what it takes

    Scenario Outline: A report of <what> is <outcome>
      Given a client has logged in as "Aurora" with the password "Sommer-2026!"
      When the client reports <what> with its login, skipping the page
      Then the operator finds the request <result>
      And the operator finds <tally> in the guild's usage figures, asked for with the admin key

      Examples:
        | what                                                           | outcome | result                | tally                                |
        | the overview shown                                             | taken   | served                | "1" as Aurora's "Übersicht"          |
        | the page's "no view" shown                                     | taken   | served                | "1" as Aurora's "Keine Ansicht"      |
        | the last column switched to "Vor Abzügen"                      | taken   | served                | "1" as Aurora's "Vor Abzügen"        |
        | the bank ledger of "Aurora" shown                              | taken   | served                | "1" as Aurora's "Bank (eigene)"      |
        | the bank ledger of a name of 64 characters shown               | taken   | served                | "1" as Aurora's "Bank (fremde)"      |
        | the bank ledger of a name with a line break in it shown        | refused | refused as unreadable | no view or option counted for Aurora |
        | the bank ledger of a name of 65 characters shown               | refused | refused as unreadable | no view or option counted for Aurora |
        | the bank ledger of a name with an angle bracket in it shown    | refused | refused as unreadable | no view or option counted for Aurora |
        | a view the page never reports, such as "Kalender"              | refused | refused as unreadable | no view or option counted for Aurora |
        | an option the page never reports, such as "Spalten ausblenden" | refused | refused as unreadable | no view or option counted for Aurora |
        | the overview sorted by a column it does not have               | refused | refused as unreadable | no view or option counted for Aurora |
        | the last column switched to a figure the page does not offer   | refused | refused as unreadable | no view or option counted for Aurora |
        | something that is no report at all                             | refused | refused as unreadable | no view or option counted for Aurora |
        | the overview shown, padded to 100000 characters                | refused | refused as too large  | no view or option counted for Aurora |

  Rule: A report turned away for sending too many requests counts nowhere

    Scenario: The 31st report within a pot's count is turned away and counts nowhere
      The 31st report turns Aurora's network address away for a minute, so the admin key asks for
      the usage figures from another one.

      Given a client has logged in as "Aurora" with the password "Sommer-2026!"
      When the client reports the overview shown 31 times within its first second, skipping the page
      Then the operator finds the first 30 of them served and the 31st turned away for sending too many requests
      And the operator finds "30" as Aurora's "Übersicht" in the guild's usage figures, asked for with the admin key from another network address
