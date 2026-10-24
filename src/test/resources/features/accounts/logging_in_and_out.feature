@wip
Feature: Logging in and out
  A member logs in on the login page with the name of their avatar and their own password; the name
  may be written in any mix of capital and small letters, umlauts included, the password only
  exactly as chosen. Logging in opens the overview, or the page the member opened before, as "Access
  with a login" says. A wrong name and a wrong password get the same answer, so the login page does
  not tell which names have an account. A login lasts 7 days from the moment it was made, however
  much it is used, on its browser; it ends sooner when the member logs out on that browser, changes
  the password on another one, or when the service restarts. How many requests a browser without a
  login may send is in "Request throttling and the request log". What the service lets in without a
  member's login is in "Who the service lets in", choosing a password in "Choosing a password", a
  resting account in "Resting accounts".

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      | 02.01.2026 12:00 | Ärger  | 100    | Einzahlung |
      | 03.01.2026 12:00 | Calix  | 300    | Einzahlung |
      | 04.01.2026 12:00 | Boreas | 750    | Einzahlung |
    And the daily collection has run
    And Aurora has chosen the password "Sommer-2026!"
    And Ärger has chosen the password "Winter-2026!"
    And Calix has chosen the password "Sommer-2026!"

  Rule: A member logs in with the avatar's name, in any capitals, and their password

    Scenario Outline: Logging in as "<name>" opens the overview for <avatar>
      When a member logs in as "<name>" with the password "<password>"
      Then the page is headed "Übersicht"
      And the page's frame says "Angemeldet als <avatar>"

      Examples:
        | name   | password     | avatar |
        | Aurora | Sommer-2026! | Aurora |
        | aurora | Sommer-2026! | Aurora |
        | AURORA | Sommer-2026! | Aurora |
        | ärger  | Winter-2026! | Ärger  |
        | ÄRGER  | Winter-2026! | Ärger  |
        | CALIX  | Sommer-2026! | Calix  |

    Scenario Outline: A login with <what> is refused without saying which part was wrong
      When a member logs in as "<name>" with the password "<password>"
      Then the page says "Name oder Passwort stimmt nicht."

      Examples:
        | what                           | name   | password     |
        | a wrong password               | Aurora | Winter-2026! |
        | the password in other capitals | Aurora | sOMMER-2026! |
        | a name that has no account     | Zephyr | Sommer-2026! |
        | an empty name                  |        | Sommer-2026! |
        | an empty password              | Aurora |              |

    Scenario: A member whose browser has used up the requests the service allows at once is asked to wait
      Given the member's browser has spent the 10 requests without a login it may send at once, the login page among them
      When the member logs in as "Aurora" with the password "Sommer-2026!" 5 seconds after the first request
      Then the page says "Zu viele Versuche. Bitte warte einen Moment."

  Rule: A login lasts 7 days, until the member logs out on that browser, changes the password on another one, or the service restarts

    Scenario Outline: <delay> after logging in, the member <outcome>
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      When the member opens the overview <delay> after logging in
      Then the page is headed "<heading>"

      Examples:
        | delay                       | outcome                  | heading   |
        | one minute less than 7 days | still sees the overview  | Übersicht |
        | 7 days                      | is asked to log in again | Anmeldung |

    Scenario: A login does not last longer for being used
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has opened the overview 6 days after logging in
      When the member opens the overview 7 days after logging in
      Then the page is headed "Anmeldung"

    Scenario: The frame offers the member the way to log out
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      When the member opens the overview
      Then the page's frame offers the button "Abmelden"

    Scenario Outline: A password <which> before a restart still <outcome> after it
      Given <state>
      And the service has been restarted since
      When a member logs in as "<avatar>" with <password>
      Then the page is headed "<heading>"

      Examples:
        | which            | outcome                          | state                                                         | avatar | password                      | heading               |
        | the member chose | logs in                          | Aurora's account holds the password she chose, "Sommer-2026!" | Aurora | the password "Sommer-2026!"   | Übersicht             |
        | a reset issued   | leads to the choice of a new one | Boreas's account still holds the password the reset issued    | Boreas | the password the reset issued | Neues Passwort wählen |

    Scenario: A restart of the service asks every member to log in again
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the service has been restarted since
      When the member opens the overview
      Then the page is headed "Anmeldung"

    Scenario Outline: After logging out on the first browser, the <which> browser <outcome>
      Given a member has logged in as "Aurora" with the password "Sommer-2026!" on two browsers
      And the member has logged out on the first browser
      When the member opens the overview on the <which> browser
      Then the page is headed "<heading>"

      Examples:
        | which  | outcome                  | heading   |
        | first  | asks to log in again     | Anmeldung |
        | second | still shows the overview | Übersicht |
