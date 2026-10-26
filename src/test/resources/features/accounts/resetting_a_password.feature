@wip
Feature: Resetting a password on the admin page
  The admin page lists every account ("The admin page lists the accounts"). Beside each account
  stands the button "Passwort zurücksetzen"; the page asks "Passwort von <name> wirklich
  zurücksetzen?" and resets only on "Zurücksetzen", not on "Abbrechen". A reset issues a new random
  password, which the admin page shows once ("The password a reset issued is shown once"); the
  admin sends it to the member by in-game message from the guild's admin avatar. The password the
  account held before and every login of the member stop working, an admin's own included, and the
  member chooses a new password at the next login ("Choosing a password"). A password lost or never
  sent is replaced by another reset. Only the admin role lists the accounts and resets a password,
  the admin key included ("Accounts for the members, and the admin").

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      | 02.01.2026 12:00 | Boreas | 750    | Einzahlung |
    And the daily collection has run
    And the admin has logged in as "admin" with the admin key the service's log names

  Rule: The page asks before it resets

    Scenario: Asked to reset a password, the page asks first
      When the admin asks the admin page to reset the password of "Aurora"
      Then the admin page asks "Passwort von Aurora wirklich zurücksetzen?"

    Scenario: A reset the admin cancelled leaves the member's password as it was
      Given Aurora has chosen the password "Sommer-2026!"
      And the admin has cancelled a reset of the password of "Aurora"
      When a member logs in as "Aurora" with the password "Sommer-2026!"
      Then the page is headed "Übersicht"

  Rule: A reset issues a password that ends the one the account held and every login of the member

    Scenario: After a second reset, for a password lost, the latest reset's password leads to the choice of a new one
      Given the admin has reset the password of "Boreas" and then reset it again
      When a member logs in as "Boreas" with the password the latest reset issued
      Then the page is headed "Neues Passwort wählen"

    Scenario Outline: After a reset, <old> no longer logs in
      The Boreas row stands for a password lost or never sent: another reset replaces it, as it
      replaces one the member chose.

      Given <before>
      And the admin has reset the password of "<avatar>"
      When a member logs in as "<avatar>" with <old>
      Then the page says "Name oder Passwort stimmt nicht."

      Examples:
        | old                                          | avatar | before                                        |
        | the password "Sommer-2026!" the member chose | Aurora | Aurora has chosen the password "Sommer-2026!" |
        | the password the first reset issued          | Boreas | the admin has reset the password of "Boreas"  |

    Scenario Outline: The member's login from before the reset asks the <which> browser to log in again
      Given Aurora has chosen the password "Sommer-2026!"
      And a member has logged in as "Aurora" with the password "Sommer-2026!" on two browsers
      And the admin has reset the password of "Aurora"
      When the member opens the overview on the <which> browser
      Then the page is headed "Anmeldung"

      Examples:
        | which  |
        | first  |
        | second |

    Scenario: A reset leaves another member's login standing
      Given Aurora has chosen the password "Sommer-2026!"
      And Boreas has chosen the password "Winter-2026!"
      And a member has logged in as "Boreas" with the password "Winter-2026!"
      And the admin has reset the password of "Aurora"
      When the member opens the overview
      Then the page is headed "Übersicht"

    Scenario: An admin who resets their own account's password sees the new one before their login ends
      Boreas's account has the admin role; the reset ends his own login too.

      Given Boreas has chosen the password "Winter-2026!"
      And Boreas's account has the role "admin"
      And the admin has logged in as "Boreas" with the password "Winter-2026!" on another browser
      When the admin resets the password of "Boreas" on that browser
      Then the admin page on that browser shows a password beside "Boreas"
      And the next page the admin opens on that browser is headed "Anmeldung"

  Rule: A reset needs an account

    Scenario Outline: A reset asked for "<name>", a name without an account, is refused and creates none
      Given a client has logged in as "admin" with the admin key the service's log names
      When the client asks for the reset of the password of "<name>" with its login
      Then the operator finds the request refused for naming no account
      And the operator finds the list of accounts, asked for with the admin key, holding only "Aurora" and "Boreas"

      Examples:
        | name  |
        | Zelda |
        | admin |

  Rule: An account with the admin role resets a password like the admin key

    Scenario: An account with the admin role resets another member's password
      Given Boreas has chosen the password "Winter-2026!"
      And Boreas's account has the role "admin"
      And the admin has logged in as "Boreas" with the password "Winter-2026!" on another browser
      And the admin has reset the password of "Aurora" on that browser
      When a member logs in as "Aurora" with the password the reset issued
      Then the page is headed "Neues Passwort wählen"
