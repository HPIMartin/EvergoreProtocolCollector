@wip
Feature: Changing the password
  A member who is logged in changes their own password at any time, giving the current one first and
  the new one twice. The new password follows the rule of "Choosing a password", which also says
  that both entries must match. Whichever password was set last logs in, and the one it replaced no
  longer does. A change ends the member's logins on every other browser and keeps the one on which
  they changed it.

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      | 02.01.2026 12:00 | Boreas | 750    | Einzahlung |
    And the daily collection has run
    And Boreas has chosen the password "Herbst-2026!"

  Rule: The password set last logs in, and the one it replaced no longer does

    Scenario Outline: Once <replaced>, a login with <which> <outcome>
      Given <replacement>
      When a member logs in as "Aurora" with <password>
      Then <result>

      Examples:
        | replaced                            | which               | outcome            | replacement                                                                  | password                      | result                                           |
        | a password was chosen after a reset | the chosen password | opens the overview | Aurora has chosen "Winter-2026!" in place of the password the reset issued   | the password "Winter-2026!"   | the page is headed "Übersicht"                   |
        | a password was chosen after a reset | the issued password | is refused         | Aurora has chosen "Winter-2026!" in place of the password the reset issued   | the password the reset issued | the page says "Name oder Passwort stimmt nicht." |
        | the member has changed the password | the new password    | opens the overview | Aurora has changed the password she chose, "Sommer-2026!", to "Winter-2026!" | the password "Winter-2026!"   | the page is headed "Übersicht"                   |
        | the member has changed the password | the old password    | is refused         | Aurora has changed the password she chose, "Sommer-2026!", to "Winter-2026!" | the password "Sommer-2026!"   | the page says "Name oder Passwort stimmt nicht." |

  Rule: A change needs the current password

    Scenario: A change with a wrong current password is refused and keeps the password
      Given a member has logged in as "Aurora" after choosing "Sommer-2026!" in place of the password the reset issued
      When the member changes the password from "Herbst-2026!" to "Winter-2026!"
      Then the page says "Das bisherige Passwort stimmt nicht."
      And a login as "Aurora" with the password "Sommer-2026!" still opens the overview

    Scenario Outline: A client with Aurora's login that skips the page and gives <current> cannot change the password
      Given a client has logged in as "Aurora" after choosing "Sommer-2026!" in place of the password the reset issued
      When a client sets "Winter-2026!" as Aurora's new password with her login, giving <current>, skipping the page
      Then the operator finds the request refused for a wrong current password
      And the operator finds a login as "Aurora" with the password "Sommer-2026!" opening the overview's figures

      Examples:
        | current                             |
        | the current password "Herbst-2026!" |
        | no current password                 |

    Scenario: The frame offers the member the way to change the password
      Given a member has logged in as "Aurora" after choosing "Sommer-2026!" in place of the password the reset issued
      When the member opens the overview
      Then the page's frame offers the button "Passwort ändern"

  Rule: A change ends the member's logins on every other browser

    Scenario: A change leaves another member's password and login as they are
      Given a member has logged in as "Aurora" after choosing "Sommer-2026!" in place of the password the reset issued
      And another member has logged in as "Boreas" with the password "Herbst-2026!"
      And the first member has changed the password from "Sommer-2026!" to "Winter-2026!"
      When the other member opens the overview
      Then the page is headed "Übersicht"
      And a login as "Boreas" with the password "Herbst-2026!" still opens the overview

    Scenario Outline: After a change on the first browser, the <which> browser <outcome>
      Given a member has logged in as "Aurora" on two browsers after choosing "Sommer-2026!" in place of the password the reset issued
      And the member has changed the password from "Sommer-2026!" to "Winter-2026!" on the first browser
      When the member opens the overview on the <which> browser
      Then the page is headed "<heading>"

      Examples:
        | which  | outcome                  | heading   |
        | first  | still shows the overview | Übersicht |
        | second | asks to log in again     | Anmeldung |
