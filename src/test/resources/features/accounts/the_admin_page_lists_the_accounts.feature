Feature: The admin page lists the accounts
  The admin page lists every account, in German alphabetical order like every name list there
  ("The health report: what the last recompute found"), with "ruht" beside a resting one and the
  moment of its latest login, or "noch nie".
  A login counts from the moment the service lets the member in: a wrong password does not count,
  nor a password a reset issued until the member has chosen a new one in its place, nor a login the
  service refuses because the account rests. A reset keeps the latest login shown. Only the admin
  role sees the list ("Accounts for the members, and the admin"); a reset is in "Resetting a
  password on the admin page".

  Background:
    Given the clocks of the service and of the admin's browser read 31.12.2026 12:00
    And the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 00:00 | Calix  | 300    | Entnahme   |
      | 29.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      | 31.01.2026 12:00 | Boreas | 750    | Einzahlung |
    And the daily collection has run
    And the admin has logged in as "admin" with the admin key the service's log names

  Rule: The admin page lists every account by name, a resting one marked, with its latest login

    Scenario: The admin page lists every account, a resting one marked, none logged in yet
      Calix's movement of 01.01. at 00:00 lies more than 30 days before Boreas's of 31.01., so his
      account rests.

      When the admin opens the admin page
      Then the admin page lists exactly these accounts:
        | Name   | Ruht | Letzte Anmeldung |
        | Aurora |      | noch nie         |
        | Boreas |      | noch nie         |
        | Calix  | ruht | noch nie         |

    Scenario Outline: After <what>, the admin page gives <avatar>'s latest login as <shown>
      Given the admin has reset the password of "<avatar>"
      And <what>
      When the admin opens the admin page
      Then the admin page gives "<shown>" as the latest login of "<avatar>"

      Examples:
        | avatar | what                                                                                                                      | shown            |
        | Boreas | a member has tried to log in as "Boreas" with a wrong password                                                            | noch nie         |
        | Boreas | a member has entered the name "Boreas" and the password the reset issued on the login page                                | noch nie         |
        | Boreas | Boreas has chosen "Winter-2026!" in place of the password the reset issued                                                | 31.12.2026 12:00 |
        | Boreas | Boreas has chosen "Winter-2026!" in place of the password the reset issued and logged in with it again 2 hours later      | 31.12.2026 14:00 |
        | Calix  | a member has entered the name "Calix" and the password the reset issued on the login page and been told the account rests | noch nie         |

    Scenario: A reset keeps the latest login shown
      "Boreas has chosen the password" says only which password the account holds, as "Accounts
      for the members, and the admin" explains; the login is the step after it.

      Given Boreas has chosen the password "Winter-2026!"
      And a member has logged in as "Boreas" with the password "Winter-2026!"
      And the admin has reset the password of "Boreas"
      When the admin opens the admin page
      Then the admin page gives "31.12.2026 12:00" as the latest login of "Boreas"
