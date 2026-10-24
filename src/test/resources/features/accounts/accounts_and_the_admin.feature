@wip
Feature: Accounts for the members, and the admin
  Every avatar the guild ledgers name gets one account, after the daily collection that first reads
  it; the account begins with a reset, a random password of its own, and later collections leave it
  as it is. An avatar named "admin", in any mix of capital and small letters, gets no account, since
  that name logs in with the admin key ("The admin key"). Every account has the role "member" until
  the operator gives it the admin role by hand; a role given or taken takes effect at the member's
  next login, and a restart ends every login. A member sees every member's figures and ledgers; the
  admin page, and what the health report shows beyond its verdict, are for the admin role only.

  Rule: Every avatar the guild ledgers name gets one account

    Scenario Outline: <avatar>, first read from the <ledger> ledger by <which> collection, has an account that begins with a reset
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 02.01.2026 12:00 | Brynja | 1     | Eisenbarren | 100      | Einlagerung |
      And the daily collection has run
      And the guild bank ledger also holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 03.01.2026 12:00 | Boreas | 750    | Einzahlung |
      And the daily collection has run
      When a member logs in as "<avatar>" with the password the reset issued
      Then the page is headed "Neues Passwort wählen"

      Examples:
        | avatar | ledger  | which     |
        | Aurora | bank    | the first |
        | Brynja | storage | the first |
        | Boreas | bank    | a later   |

    Scenario: An avatar both ledgers name has one account
      With an account for each ledger, the password the reset issued to the second would still lead
      to the choice of a new one.

      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 02.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
      And the daily collection has run
      And Aurora has chosen "Sommer-2026!" in place of the password the reset issued
      When a member logs in as "Aurora" with the password the reset issued
      Then the page says "Name oder Passwort stimmt nicht."

    Scenario: The password a reset issued to one account does not log in to another
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
        | 02.01.2026 12:00 | Boreas | 750    | Einzahlung |
      And the daily collection has run
      When a member logs in as "Boreas" with the password the reset issued to Aurora
      Then the page says "Name oder Passwort stimmt nicht."

    Scenario Outline: A later collection leaves an account that holds <which> as it was
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      And the daily collection ran at 02.01.2026 05:00
      And <state>
      And the daily collection ran at 03.01.2026 05:00
      When a member logs in as "Aurora" with <password>
      Then the page is headed "<heading>"

      Examples:
        | which                         | state                                                      | password                                         | heading               |
        | the password its reset issued | Aurora's account still holds the password the reset issued | the password the first collection's reset issued | Neues Passwort wählen |
        | the password the member chose | Aurora has chosen the password "Sommer-2026!"              | the password "Sommer-2026!"                      | Übersicht             |

    Scenario Outline: An avatar named "<avatar>" gets no account, and the name stays the admin key's
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar   | Betrag | Vorgang    |
        | 01.01.2026 12:00 | <avatar> | 100    | Einzahlung |
      When the daily collection runs
      Then the operator finds the line "No account for the avatar <avatar>: the name admin logs in with the admin key." in the service's log
      And the operator finds a login as "admin" with the admin key the service's log names opening the admin's status figures

      Examples:
        | avatar |
        | Admin  |
        | ADMIN  |

  Rule: A member sees every member's figures; the admin page and the health report's details are the admin's

    Background:
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
        | 02.01.2026 12:00 | Boreas | 750    | Einzahlung |
      And the daily collection ran at 03.01.2026 05:00
      And Aurora has chosen the password "Sommer-2026!"
      And Boreas has chosen the password "Winter-2026!"
      And Boreas's account has the role "admin"

    Scenario: A member opens another member's ledger
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      When the member opens the bank ledger of "Boreas"
      Then the ledger shows a movement of 750 gold

    Scenario Outline: A login as <name> reading <surface> <outcome>
      Given <who> has logged in as "<name>" with the password "<password>"
      When <reading>
      Then <result>

      Examples:
        | who       | name   | password     | surface           | outcome                   | reading                                                   | result                                                                                                                       |
        | a member  | Aurora | Sommer-2026! | the admin page    | is told it is reserved    | the member opens the admin page                           | the page says "Diese Seite ist dem Admin vorbehalten."                                                                       |
        | the admin | Boreas | Winter-2026! | the admin page    | sees it                   | the admin opens the admin page                            | the admin page reads "Stand: 03.01.2026 05:00"                                                                               |
        | a client  | Boreas | Winter-2026! | the health report | finds the dates beside it | the client asks the service for its health with its login | the operator finds the health report dating the last successful scrape and the last successful recompute at 03.01.2026 05:00 |

    Scenario: A member's login does not open the admin's status figures
      Given a client has logged in as "Aurora" with the password "Sommer-2026!"
      When the client asks for the admin's status figures with its login
      Then the operator finds the request refused for lacking the admin's role
