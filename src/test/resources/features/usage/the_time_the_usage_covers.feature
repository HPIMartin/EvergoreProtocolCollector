Feature: The time the usage covers
  The usage on the admin page ("Who uses the app, and when") counts calendar days in Berlin. The admin page opens on "Letzte 30 Tage", which runs back 30 calendar days in
  Berlin from the moment the admin looks, to the same time of day, and switches to "Alles", all the
  time recorded, and back; every figure follows the time chosen, but "Zuletzt" names the latest use
  over all the time recorded, and "Aufgezeichnet seit" names its day whichever time is chosen. The
  records are kept without a time limit and outlast a restart of the service. Each moment is on the
  service's clock. Not covered here: a service stopped without closing loses the records of its
  last moments, and a window across a change of summer time; tests below the scenarios pin both.

  Background:
    Given the clocks of the service and of the admin's browser read 10.01.2026 12:00
    And the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
    And the daily collection has run
    And Aurora has chosen the password "Sommer-2026!"

  Rule: A day of use is a calendar day in Berlin

    Scenario: Two uses on either side of midnight in Berlin make two days of use
      Aurora logs in at 12:00 on 10.01. and opens the overview 12 hours and 30 minutes later, at
      00:30 on 11.01.

      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has opened the overview 12 hours and 30 minutes after logging in
      When the admin opens the admin page
      Then the admin page gives "2" as Aurora's "Tage genutzt" over the last 30 days

  Rule: The admin page counts over the last 30 days, or over all the time recorded

    Scenario Outline: A login <age> before the admin looks <counts> over the last 30 days, which the page opens on, while "Aufgezeichnet seit" keeps its day
      Aurora logs in on 10.01. at 12:00, landing on the overview.

      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      When the admin opens the admin page <age> after Aurora logged in
      Then the admin page shows the usage over "Letzte 30 Tage"
      And the admin page shows these rows of Aurora and the guild, among the others, over the last 30 days, in this order, in these columns:
        | Name   | Anmeldungen | Seite geladen | Tage genutzt | Zuletzt          |
        | Aurora | <in 30>     | <in 30>       | <in 30>      | 10.01.2026 12:00 |
        | Gilde  | <in 30>     | <in 30>       | <in 30>      | 10.01.2026 12:00 |
      And the admin page reads "Aufgezeichnet seit 10.01.2026"

      Examples:
        | age                          | counts         | in 30 |
        | 30 days                      | still counts   | 1     |
        | one minute more than 30 days | counts no more | 0     |

    Scenario: Switched to "Alles", the admin page counts a login of 355 days before
      Aurora logs in on 10.01. at 12:00, landing on the overview; the admin looks on 31.12.

      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the admin has opened the admin page 355 days after Aurora logged in
      When the admin switches the usage to "Alles"
      Then the admin page shows these rows of Aurora and the guild, among the others, over all the time recorded, in this order, in these columns:
        | Name   | Anmeldungen | Seite geladen | Tage genutzt | Zuletzt          |
        | Aurora | 1           | 1             | 1            | 10.01.2026 12:00 |
        | Gilde  | 1           | 1             | 1            | 10.01.2026 12:00 |

    Scenario: Switched back to "Letzte 30 Tage", the admin page leaves out a login of 355 days before
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the admin has opened the admin page 355 days after Aurora logged in
      And the admin has switched the usage to "Alles"
      When the admin switches the usage to "Letzte 30 Tage"
      Then the admin page gives "0" as Aurora's "Anmeldungen" over the last 30 days

  Rule: The records outlast a restart of the service

    Scenario: A login recorded before a restart still counts after it
      The restart ends every login and names a new admin key ("The admin key"), with which the
      admin logs in as the page is opened.

      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the service has been restarted since
      When the admin opens the admin page
      Then the admin page gives "1" as Aurora's "Anmeldungen" over the last 30 days
