Feature: The password a reset issued is shown once
  A reset of a password on the admin page ("Resetting a password on the admin page") issues a new
  random password, and the admin page shows it beside the account's name: it stays there until the
  admin leaves the page, and no later page or answer shows it again: neither the service's answer
  listing the accounts nor its log holds it. A password lost that way is replaced by another
  reset.

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      | 02.01.2026 12:00 | Boreas | 750    | Einzahlung |
    And the daily collection has run
    And the admin has logged in as "admin" with the admin key the service's log names

  Rule: A reset shows the password it issued beside the account's name until the admin leaves the page

    Scenario: The password of the first reset stays beside its name while the admin resets another
      Given the admin has reset the password of "Aurora"
      When the admin resets the password of "Boreas"
      Then the admin page shows a password beside "Aurora" and another beside "Boreas"

    Scenario Outline: The password shown beside <avatar> is <avatar>'s
      Given the admin has reset the password of "Aurora"
      And the admin has reset the password of "Boreas"
      When a member logs in as "<avatar>" with the password the admin page shows beside "<avatar>"
      Then the page is headed "Neues Passwort wählen"

      Examples:
        | avatar |
        | Aurora |
        | Boreas |

  Rule: No page shows a password but the one right after a reset

    Scenario Outline: <where> shows no password
      Given <before>
      When <looking>
      Then the admin page lists "Aurora" and "Boreas" and shows no password

      Examples:
        | where                                               | before                                                               | looking                                                                                           |
        | the admin page after a cancelled reset              | the admin has asked the admin page to reset the password of "Aurora" | the admin answers "Abbrechen"                                                                     |
        | the admin page reloaded after a reset               | the admin has reset the password of "Aurora"                         | the admin reloads the admin page                                                                  |
        | the admin page, left and opened again after a reset | the admin has reset the password of "Aurora"                         | the admin opens the overview and then the admin page                                              |
        | the admin page on another browser after a reset     | the admin has reset the password of "Aurora"                         | the admin logs in as "admin" with the admin key on another browser and opens the admin page there |

    Scenario: A reload of the admin page after a reset does not reset again
      Given the admin has reset the password of "Aurora"
      And the admin has reloaded the admin page
      When a member logs in as "Aurora" with the password the reset issued
      Then the page is headed "Neues Passwort wählen"

  Rule: Neither the service's answer listing the accounts nor its log holds the password a reset issued

    Scenario Outline: <where> does not hold the password a reset issued
      Given the admin has reset the password of "Aurora"
      When <looking>
      Then the operator finds the password the reset issued to Aurora nowhere in <it>

      Examples:
        | where                                     | looking                                                   | it         |
        | the service's answer listing the accounts | a client asks for the list of accounts with the admin key | the answer |
        | the service's log                         | the operator reads the service's log                      | the log    |

  Rule: A resting account's password is reset and shown like any other

    Scenario: The admin resets a resting account's password and sees it
      Calix's movement of 03.01. lies more than 30 days before Boreas's of 15.02., so his account
      rests.

      Given the guild bank ledger also holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 03.01.2026 12:00 | Calix  | 300    | Entnahme   |
        | 15.02.2026 12:00 | Boreas | 750    | Einzahlung |
      And the daily collection has run
      When the admin resets the password of "Calix"
      Then the admin page shows a password beside "Calix"
