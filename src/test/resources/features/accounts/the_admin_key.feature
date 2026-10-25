Feature: The admin key
  At every start the service names a new random admin key of 43 characters, too long to guess, in
  its log, so the operator of the machine always has an admin login. With the name "admin", in any
  capitals, and that key as the password, the admin logs in until the next restart, without an
  account of their own and so without a password to change; the key logs in under no other name.

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 00:00 | Calix  | 300    | Entnahme   |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
    And the daily collection ran at 02.01.2026 05:00
    And Aurora has chosen the password "Sommer-2026!"

  Rule: The admin key in the service's log logs in as the admin until the next restart, under the name "admin" only

    Scenario: At every start the service names an admin key in its log
      When the operator starts the service
      Then the operator finds the line "Console admin key, valid until the next restart: " followed by a random key of 43 characters in the service's log

    Scenario Outline: The admin key logs in under the name "<name>" and opens the admin page
      Given the admin has logged in as "<name>" with the admin key the service's log names
      When the admin opens the admin page
      Then the admin page reads "Stand: 02.01.2026 05:00"

      Examples:
        | name  |
        | admin |
        | ADMIN |

    Scenario Outline: The admin key does not log in under the name "<name>"
      When the admin logs in as "<name>" with the admin key the service's log names
      Then the page says "Name oder Passwort stimmt nicht."

      Examples:
        | name   |
        | Aurora |
        | Zephyr |

    Scenario: The frame of the admin key's login offers no way to change a password
      Given the admin has logged in as "admin" with the admin key the service's log names
      When the admin opens the overview
      Then the page's frame says "Angemeldet als admin"
      And the page's frame offers no button "Passwort ändern"

    Scenario: The admin key's login cannot change a password
      Given a client has logged in as "admin" with the admin key the service's log names
      When a client sets "Winter-2026!" as the new password with its login, giving the admin key, skipping the page
      Then the operator finds the request refused for lacking an account
      And the operator finds a login as "admin" with the password "Winter-2026!" answered as a wrong password

    Scenario: The admin key does not log in written in other capitals
      When the admin logs in as "admin" with the admin key the service's log names, written in other capitals
      Then the page says "Name oder Passwort stimmt nicht."

    Scenario: The admin key still logs in while an admin's account rests
      Calix's account has the admin role, and his movement of 01.01. at 00:00 lies more than 30 days
      before Aurora's of 31.01.

      Given Calix's account has the role "admin"
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 31.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
      And the daily collection has run
      And the admin has logged in as "admin" with the admin key the service's log names
      When the admin opens the overview
      Then the page's frame says "Angemeldet als admin"
      And the dormant table lists "Calix"

    Scenario: The admin key from before a restart no longer logs in
      Given the service's log has named its admin key
      And the service has been restarted since
      When the admin logs in as "admin" with the admin key named before the restart
      Then the page says "Name oder Passwort stimmt nicht."
