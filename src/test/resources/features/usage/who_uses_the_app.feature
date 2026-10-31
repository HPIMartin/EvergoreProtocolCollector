@wip
Feature: Who uses the app, and when
  The guild leadership wants to see how the guild uses the app before it decides what to build
  next. The service records every login, every page of the dashboard a browser loads and every
  logout, each with its account and its moment on the service's clock, and keeps nothing else of
  the request; tests below the scenarios show that no record holds a network address and that a
  record carries the service's moment whatever a browser's clock reads. Which login counts, and
  that each one counts, stands in "The admin page lists the accounts". A page is loaded when the
  member opens one from outside the dashboard: the page after logging in, a bookmark, an address
  typed in, one the dashboard does not have included, a reload; following a link inside the
  dashboard loads no page. The admin page, which the admin key and every account with the admin
  role open, shows under "Nutzung" one row per account, a resting one included, in the order "The
  health report: what the last recompute found" gives every name list there, with how often it
  logged in, loaded a page and logged out, on how many days it used the app, each of a login, a
  loaded page, a logout and a view or option of "What the guild uses" alone making a day while
  nothing else does, and the moment it last did so. Above the table it reads "Aufgezeichnet seit"
  and the day of the oldest record it holds, the admin key's included. The days and the time the
  table covers are in "The time the usage covers". The guild row below the accounts adds up their
  counts, an account with the admin role like every other, counts the days on which any member
  used the app, and names the latest use of any member. The use of the admin key stands in a row
  of its own below the guild row, with "noch nie" while the key has not been used, and counts in
  no figure of the guild. The admin's login is as "Access with a login" says. Not covered here:
  under a flood of requests some records are lost, and no request waits for its record; a record
  the service cannot save is lost, and the request is still answered; and what the admin page
  shows when the usage cannot be loaded. Tests below the scenarios pin all of them.

  Background:
    Given the clocks of the service and of the admin's browser read 10.01.2026 12:00
    And the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      | 02.01.2026 12:00 | Boreas | 750    | Einzahlung |
      | 03.01.2026 12:00 | Calix  | 100    | Einzahlung |
    And the daily collection has run
    And Aurora has chosen the password "Sommer-2026!"
    And Boreas has chosen the password "Winter-2026!"
    And Calix has chosen the password "Herbst-2026!"

  Rule: Every login, loaded page and logout counts for its account, and the guild row adds up the members

    Scenario: The admin page counts each account's use and adds up the members in the guild row
      Aurora and Boreas, whose account has the admin role, log in on 10.01. at 12:00, both landing on
      the overview. A day later, on 11.01. at 12:00, Aurora opens a bookmark of the overview and logs
      out. Calix has never logged in.

      Given Boreas's account has the role "admin"
      And a member has logged in as "Aurora" with the password "Sommer-2026!"
      And another member has logged in as "Boreas" with the password "Winter-2026!"
      And Aurora has opened a bookmark of the overview 1 day after logging in
      And Aurora has logged out
      When the admin opens the admin page
      Then the admin page shows exactly these rows of the members and the guild over the last 30 days, in this order, in these columns:
        | Name   | Anmeldungen | Seite geladen | Tage genutzt | Zuletzt          | Abgemeldet |
        | Aurora | 1           | 2             | 2            | 11.01.2026 12:00 | 1          |
        | Boreas | 1           | 1             | 1            | 10.01.2026 12:00 | 0          |
        | Calix  | 0           | 0             | 0            | noch nie         | 0          |
        | Gilde  | 2           | 3             | 2            | 11.01.2026 12:00 | 1          |

    Scenario: An account with the admin role sees the usage on the admin page
      Given Boreas's account has the role "admin"
      And a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the admin has logged in as "Boreas" with the password "Winter-2026!" on another browser
      When the admin opens the admin page
      Then the admin page gives "1" as Aurora's "Anmeldungen" over the last 30 days
      And the admin page gives "noch nie" as admin's "Zuletzt", in the row below the guild's

    Scenario: Each logout counts, two on one day as two
      Aurora logs in and out twice, all on 10.01.

      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And Aurora has logged out
      And a member has logged in as "Aurora" with the password "Sommer-2026!"
      And Aurora has logged out
      When the admin opens the admin page
      Then the admin page gives "2" as Aurora's "Abgemeldet" over the last 30 days

    Scenario Outline: A <record> alone makes a day of use and the latest use
      Aurora logs in on 10.01. at 12:00, landing on the overview, and a day later, on 11.01. at
      12:00, <does> without loading a page.

      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has <done> 1 day after logging in
      When the admin opens the admin page
      Then the admin page gives "2" as Aurora's "Tage genutzt" over the last 30 days
      And the admin page gives "11.01.2026 12:00" as Aurora's "Zuletzt"

      Examples:
        | record | does     | done       |
        | logout | logs out | logged out |

      @wip
      Examples: a view and an option the member's page reports ("What the guild uses")
        | record | does                                                  | done                                      |
        | view   | follows "Wie entstehen diese Zahlen?" on the overview | followed "Wie entstehen diese Zahlen?"    |
        | option | switches the last column to "Vor Abzügen"             | switched the last column to "Vor Abzügen" |

  Rule: An account that rests keeps its row, and the use it made before resting stays counted

    Scenario: A resting account keeps its row and its earlier use, in the guild row too
      Calix logs in on 10.01. at 12:00. A collection on 16.02. at 05:00 reads a movement of Aurora's
      of 15.02., more than 30 days after Calix's last one, so Calix's account rests. The admin looks
      40 days after Calix's login, switched to "Alles".

      Given a member has logged in as "Calix" with the password "Herbst-2026!"
      And the guild bank ledger also holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 15.02.2026 12:00 | Aurora | 1000   | Einzahlung |
      And the daily collection ran at 16.02.2026 05:00
      And the admin has opened the admin page 40 days after Calix logged in
      When the admin switches the usage to "Alles"
      Then the admin page lists these accounts, among the others:
        | Name  | Ruht | Letzte Anmeldung |
        | Calix | ruht | 10.01.2026 12:00 |
      And the admin page shows these rows of Calix and the guild, among the others, over all the time recorded, in this order, in these columns:
        | Name  | Anmeldungen | Zuletzt          |
        | Calix | 1           | 10.01.2026 12:00 |
        | Gilde | 1           | 10.01.2026 12:00 |

  Rule: The admin key's use stands in its own row, below the guild's and in none of its figures

    Scenario Outline: The admin key's use stands in its own row below the guild's, and the guild's "<figure>" counts none of it
      The admin logs in with the admin key on 10.01. at 12:00, landing on the overview, opens the
      explanation page and opens a bookmark of the overview two days later, on 12.01. at 12:00, when
      Aurora logs in too, so the admin key's own row counts 10.01. and 12.01. The admin then opens the admin page
      with the login it already has.

      Given the admin has logged in as "admin" with the admin key the service's log names
      And the admin has opened the explanation page
      And the admin has opened a bookmark of the overview 2 days after logging in
      And a member has logged in as "Aurora" with the password "Sommer-2026!" 2 days after the admin logged in
      When the admin opens the admin page
      Then the admin page gives "<admin>" as admin's "<figure>" over the last 30 days, in the row below the guild's
      And the admin page gives "<guild>" as the guild's "<figure>" over the last 30 days

      Examples:
        | figure       | admin | guild |
        | Anmeldungen  | 1     | 1     |
        | Tage genutzt | 2     | 1     |

      @wip
      Examples: a view the page reports ("What the guild uses")
        | figure      | admin | guild |
        | Erklärseite | 1     | 0     |

    Scenario: "Aufgezeichnet seit" names the day of the oldest record, the admin key's included
      The admin logs in with the admin key on 10.01. at 12:00; Aurora logs in two days later, on
      12.01. at 12:00, and the admin looks then.

      Given the admin has logged in as "admin" with the admin key the service's log names
      And a member has logged in as "Aurora" with the password "Sommer-2026!" 2 days after the admin logged in
      When the admin opens the admin page
      Then the admin page reads "Aufgezeichnet seit 10.01.2026"

  Rule: A page counts as loaded only when the member opens it from outside the dashboard

    Scenario Outline: A member who logs in and then <does> has "<pages>" loaded pages counted
      The overview after logging in is the first loaded page; a bookmark is in the first scenario.

      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has <done>
      When the admin opens the admin page
      Then the admin page gives "<pages>" as Aurora's "Seite geladen" over the last 30 days

      Examples:
        | does                                                                     | done                                                                      | pages |
        | reloads the overview                                                     | reloaded the overview                                                     | 2     |
        | opens a link from the guild's chat to a page the dashboard does not have | opened a link from the guild's chat to a page the dashboard does not have | 2     |
        | follows her own name in the overview                                     | followed the name of "Aurora" in the overview                             | 1     |

    Scenario Outline: A request with a member's login for <what> counts as no loaded page
      A client logs in without a browser on 10.01. at 12:00, so its login lands on no page: the login
      counts, and no page is loaded until one is asked for. Its request a day later is no use of the
      app: it makes no day of use and moves no latest moment.

      Given a client has logged in as "Aurora" with the password "Sommer-2026!"
      When the client asks for <what> with its login 1 day after logging in
      Then the operator finds "0" as Aurora's "Seite geladen" in the guild's usage figures, asked for with the admin key
      And the operator finds "1" as Aurora's "Anmeldungen" in the guild's usage figures, asked for with the admin key
      And the operator finds "1" as Aurora's "Tage genutzt" in the guild's usage figures, asked for with the admin key
      And the operator finds "10.01.2026 12:00" as Aurora's "Zuletzt" in the guild's usage figures, asked for with the admin key

      Examples:
        | what                           |
        | the overview's figures         |
        | a file the dashboard loads     |
        | the name it is logged in under |
        | the health check               |
        | the login page                 |
        | the admin's status figures     |
