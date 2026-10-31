Feature: The admin page lists the accounts
  The admin page lists every account, in German alphabetical order like every name list there
  ("The health report: what the last recompute found"), with its role under "Rolle": "Mitglied" for
  the role "member" every account starts with, "Admin" for the role "admin" the admin gives on
  the page ("Giving and taking the admin role on the admin page") or the operator by hand; "ruht"
  beside a resting one; and the moment of its latest login, or "noch nie". The list shows the role
  last given or taken, even when the operator's change by hand has not yet reached the member's
  open login ("Accounts for the members, and the admin"). The admin key's login is no account and has no
  row. In these scenarios "the admin" is the admin key's login; a login to a member's account,
  whatever its role, is written "a member".
  Every login the service lets in counts, each on its own, as the latest login and under
  "Anmeldungen" ("Who uses the app, and when"): a wrong password does not count,
  nor a password a reset issued until the member has chosen a new one in its place, nor a login the
  service refuses because the account rests. A reset keeps the role and the latest login shown. Only the admin
  role sees the list ("Accounts for the members, and the admin"); a reset is in "Resetting a
  password on the admin page".

  Background:
    Given the clocks of the service and of the admin's browser read 31.12.2026 12:00
    And the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 00:00 | Calix  | 300    | Entnahme   |
      | 01.01.2026 00:00 | Dorn   | 300    | Entnahme   |
      | 29.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      | 31.01.2026 12:00 | Boreas | 750    | Einzahlung |
    And the daily collection has run
    And the admin has logged in as "admin" with the admin key the service's log names

  Rule: The admin page lists every account by name with its role, a resting one marked, and its latest login

    @wip
    Scenario: The admin page lists every account with its role, a resting one marked, none logged in yet
      The movements of Calix and Dorn of 01.01. at 00:00 lie more than 30 days before Boreas's of
      31.01., so their accounts rest. The operator has given Boreas and Calix the admin role.

      Given Boreas's account has the role "admin"
      And Calix's account has the role "admin"
      When the admin opens the admin page
      Then the admin page lists exactly these accounts:
        | Name   | Rolle    | Ruht | Letzte Anmeldung |
        | Aurora | Mitglied |      | noch nie         |
        | Boreas | Admin    |      | noch nie         |
        | Calix  | Admin    | ruht | noch nie         |
        | Dorn   | Mitglied | ruht | noch nie         |

    @wip
    Scenario Outline: A role <change> by hand after the member's login shows as "<shown>"
      Boreas logs in on his own browser; the admin reads the admin page with the admin key. The
      operator's change by hand leaves Boreas's login standing until it ends; a change on the admin
      page ends it ("Giving and taking the admin role on the admin page").

      Given Boreas's account has the role "<before>"
      And Boreas has chosen the password "Winter-2026!"
      And a member has logged in as "Boreas" with the password "Winter-2026!"
      And the operator has given Boreas's account the role "<role>" by hand
      When the admin opens the admin page
      Then the admin page gives "<shown>" as the role of "Boreas"

      Examples:
        | change | before | role   | shown    |
        | given  | member | admin  | Admin    |
        | taken  | admin  | member | Mitglied |

    Scenario Outline: After <what>, the admin page gives <avatar>'s latest login as <shown> and counts <logins> under "Anmeldungen"
      Given the admin has reset the password of "<avatar>"
      And <what>
      When the admin opens the admin page
      Then the admin page gives "<shown>" as the latest login of "<avatar>"
      And the admin page gives "<logins>" as <avatar>'s "Anmeldungen" over the last 30 days

      Examples:
        | avatar | what                                                                                                                      | shown            | logins |
        | Boreas | a member has tried to log in as "Boreas" with a wrong password                                                            | noch nie         | 0      |
        | Boreas | a member has entered the name "Boreas" and the password the reset issued on the login page                                | noch nie         | 0      |
        | Boreas | Boreas has chosen "Winter-2026!" in place of the password the reset issued                                                | 31.12.2026 12:00 | 1      |
        | Boreas | Boreas has chosen "Winter-2026!" in place of the password the reset issued and logged in with it again 2 hours later      | 31.12.2026 14:00 | 2      |
        | Calix  | a member has entered the name "Calix" and the password the reset issued on the login page and been told the account rests | noch nie         | 0      |

    @wip
    Scenario: A reset keeps the role and the latest login shown
      "Boreas has chosen the password" says only which password the account holds, as "Accounts
      for the members, and the admin" explains; the login is the step after it. The operator has
      given Boreas the admin role; Boreas logs in on his own browser, and the admin resets his
      password with the admin key 2 hours later.

      Given Boreas's account has the role "admin"
      And Boreas has chosen the password "Winter-2026!"
      And a member has logged in as "Boreas" with the password "Winter-2026!"
      And the admin has reset the password of "Boreas" 2 hours after that login
      When the admin opens the admin page
      Then the admin page gives "Admin" as the role of "Boreas"
      And the admin page gives "31.12.2026 12:00" as the latest login of "Boreas"
