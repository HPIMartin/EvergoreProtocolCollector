Feature: Giving and taking the admin role on the admin page
  Beside each account the admin page offers the field "Rolle" with "Mitglied" and "Admin"
  ("The admin page lists the accounts"); choosing the other one gives the account the admin role or
  takes it. A change of role ends every open login of that account, as a reset does, so the new role
  holds from the member's next login on; choosing the role the account already holds changes
  nothing. An admin cannot take their own admin role: beside their own account the page shows the
  role without a choice, and the service refuses such a change. Should no account hold the admin
  role any more, the admin key, which is no account, still logs in as the admin and gives it again.
  The service's log names each change and who made it; a refused change changes
  nothing and is not logged. On the admin page only the admin role changes a role ("Accounts for the
  members, and the admin"), and only to "member" or "admin"; the operator may still change it by
  hand. A role asked for a name without an account is refused as a reset is ("Resetting a password
  on the admin page"). In these scenarios "the admin" is the admin key's login unless a step names
  an account; a login that only stands to be ended or kept is written "a member", whatever its role.

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      | 02.01.2026 12:00 | Boreas | 750    | Einzahlung |
      | 03.01.2026 12:00 | Calix  | 300    | Einzahlung |
    And the daily collection ran at 04.01.2026 05:00
    And Boreas has chosen the password "Winter-2026!"
    And the admin has logged in as "admin" with the admin key the service's log names

  Rule: The admin gives and takes the admin role, and the new role holds from the next login

    Scenario: Beside an account the field "Rolle" offers exactly "Mitglied" and "Admin"
      When the admin opens the admin page
      Then the admin page offers exactly "Mitglied" and "Admin" as the role of "Aurora"

    Scenario: A choice of role applies at once, without a question first
      When the admin chooses "Admin" as the role of "Aurora" on the admin page
      Then the admin page gives "Admin" as the role of "Aurora"
      And the admin page has asked no question

    Scenario Outline: After the admin page set Boreas's role to "<role>", his next login <outcome>
      Given Boreas's account has the role "<before>"
      And the admin has chosen "<role>" as the role of "Boreas" on the admin page
      When <who> logs in as "Boreas" with the password "Winter-2026!" on another browser and opens the admin page there
      Then <result>

      Examples:
        | before | role     | who       | outcome                | result                                                                     |
        | member | Admin    | the admin | opens the admin page   | the admin page on that browser reads "Stand: 04.01.2026 05:00"             |
        | admin  | Mitglied | a member  | is told it is reserved | the page on that browser says "Diese Seite ist dem Admin vorbehalten."     |

    Scenario Outline: The admin role <change> on the admin page before a restart is still <change> after it
      Given Boreas's account has the role "<before>"
      And the admin has chosen "<role>" as the role of "Boreas" on the admin page
      And the service has been restarted since
      And the admin has logged in as "admin" with the admin key the service's log names since the restart
      When the admin opens the admin page
      Then the admin page gives "<role>" as the role of "Boreas"

      Examples:
        | change | before | role     |
        | given  | member | Admin    |
        | taken  | admin  | Mitglied |

  Rule: An account with the admin role changes a role like the admin key

    Scenario: An account with the admin role takes another admin's role
      Given Boreas's account has the role "admin"
      And Calix's account has the role "admin"
      And the admin has logged in as "Boreas" with the password "Winter-2026!" on another browser
      And the admin has chosen "Mitglied" as the role of "Calix" on the admin page on that browser
      When the admin opens the admin page on that browser
      Then the admin page gives "Mitglied" as the role of "Calix"

  Rule: A change of role ends every open login of the account, and only of that account

    Scenario Outline: The login of a member whose admin role was <change> asks the <which> browser to log in again
      Given Boreas's account has the role "<before>"
      And a member has logged in as "Boreas" with the password "Winter-2026!" on two browsers
      And the admin has chosen "<role>" as the role of "Boreas" on the admin page
      When the member opens the overview on the <which> browser
      Then the page is headed "Anmeldung"

      Examples:
        | change | before | role     | which  |
        | given  | member | Admin    | first  |
        | given  | member | Admin    | second |
        | taken  | admin  | Mitglied | first  |
        | taken  | admin  | Mitglied | second |

    Scenario: A change of role leaves another member's login standing
      Given Aurora has chosen the password "Sommer-2026!"
      And a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the admin has chosen "Admin" as the role of "Boreas" on the admin page
      When the member opens the overview
      Then the page is headed "Übersicht"

    Scenario: Choosing the role an account already holds, from a page read before, leaves its logins standing
      Boreas got the admin role from the admin key on another browser after the admin opened the
      page, which still shows "Mitglied" beside him and so still offers "Admin" as a change.

      Given the admin has opened the admin page
      And the admin has chosen "Admin" as the role of "Boreas" on another browser
      And a member has logged in as "Boreas" with the password "Winter-2026!"
      And the admin has chosen "Admin" as the role of "Boreas" on the page opened before
      When the member opens the overview
      Then the page is headed "Übersicht"

  Rule: An admin cannot take their own admin role

    Scenario: Beside the admin's own account the admin page shows the role without a choice
      Calix holds the admin role too, so Boreas is not the last admin; the page withholds the choice
      only because the account is his own.

      Given Boreas's account has the role "admin"
      And Calix's account has the role "admin"
      And the admin has logged in as "Boreas" with the password "Winter-2026!" on another browser
      When the admin opens the admin page on that browser
      Then the admin page shows "Admin" without a choice beside "Boreas"

    Scenario: An admin's login that asks to take its own admin role, skipping the page, is refused and changes nothing
      Given Boreas's account has the role "admin"
      And a client has logged in as "Boreas" with the password "Winter-2026!"
      When the client asks for the role "member" for "Boreas" with its login
      Then the operator finds the request refused for taking one's own admin role
      And the operator finds the client's login still opening the admin's status figures
      And the operator finds the list of accounts, asked for with the admin key, giving "Boreas" the role "admin"
      And the operator finds no line "Role of Boreas set to" in the service's log

  Rule: A refused change of role, or one to the role the account holds, changes nothing and is not logged

    Scenario Outline: A role asked for <how> leaves the role as it was and writes no line in the service's log
      Given Aurora has chosen the password "Sommer-2026!"
      And <asking>
      When the operator asks for the list of accounts with the admin key and reads the service's log
      Then the operator finds that list giving "<name>" the role "<role>"
      And the operator finds no line "Role of <name> set to" in the service's log

      Examples:
        | how                                         | asking                                                                                                                                           | name   | role   |
        | with a member's login                       | a client logged in as "Aurora" with the password "Sommer-2026!" has asked for the role "admin" for "Aurora" with its login                       | Aurora | member |
        | without a login                             | a client without a login has asked for the role "admin" for "Aurora"                                                                             | Aurora | member |
        | as the role the account holds               | the admin key's login has asked for the role "member" for "Aurora"                                                                               | Aurora | member |
        | by an admin for the admin role they hold    | Boreas's account has the role "admin" and a client logged in as "Boreas" with the password "Winter-2026!" has asked for the role "admin" for "Boreas" with its login | Boreas | admin  |

    Scenario: A role the service does not know is refused and changes nothing
      Given a client has logged in as "admin" with the admin key the service's log names
      When the client asks for the role "superuser" for "Aurora" with its login
      Then the operator finds the request refused for naming a role other than "member" or "admin"
      And the operator finds the list of accounts, asked for with the admin key, giving "Aurora" the role "member"
      And the operator finds no line "Role of Aurora set to" in the service's log

  Rule: The service's log names each change of role and who made it

    Scenario Outline: A change of role made by <who> is logged with its maker's name
      Given <change>
      When the operator reads the service's log
      Then the operator finds the line "Role of Aurora set to <role> by <by>." in the service's log

      Examples:
        | who                   | by     | role   | change                                                                                                                                                 |
        | the admin key's login | admin  | admin  | the admin has chosen "Admin" as the role of "Aurora" on the admin page                                                                                 |
        | the admin key's login | admin  | member | Aurora's account has the role "admin" and the admin has chosen "Mitglied" as the role of "Aurora" on the admin page                                    |
        | Boreas, an admin      | Boreas | admin  | Boreas, whose account has the role "admin", has chosen "Admin" as the role of "Aurora" on the admin page                                               |
