@wip
Feature: What the guild uses
  Beside each account's logins and loaded pages ("Who uses the app, and when") the admin page
  shows how often the account opened each view of the dashboard and used each of the four options
  the explanation page explains under "Umstellen", so the next features can be read off it. The
  views are "Übersicht", the bank and the storage ledger, each counted apart as the member's own
  or another's, "Erklärseite", "Passwort ändern", "Admin-Seite", which counts also where the page
  tells a member it is reserved for the admin, and "Keine Ansicht", where a link names a page the
  dashboard does not have. A ledger counts as that ledger whichever page of it is asked for, one
  that does not exist included; a ledger of a name that has no account, where the page says there
  is no avatar of that name, is not the member's own and counts as another's. A view counts each
  time the page shows it, the first after loading a page included; paging or sorting within one
  ledger and using an option of the overview open no new view. The options are "Vor Abzügen",
  which counts each switch of the last column to it and none back, "Übersicht sortiert", "Vorgänge
  sortiert" and "Vorgänge geblättert", each counted at every use, the same column sorted again
  included. Below the table the admin page names, in a line for each member who opened a ledger of
  another member, the members in German alphabetical order, whose ledgers and how often: the
  avatars in German alphabetical order, each avatar's bank before its storage; a name that has no
  account is named nowhere. Under "Nie genutzt" it names, in the order of every name list on the
  admin page, every view and option no member used over the time chosen. The admin key's views and
  options stand in its own row and in no member's count ("Who uses the app, and when"), and the
  lines below the table name no ledger the admin key opened. The moments, the time chosen, the
  guild row and the admin's login are as in "Who uses the app, and when" and "The time the usage
  covers"; which views and options the service takes is in "Reports of what a member does on the
  page", and one it does not take changes nothing the member sees, which a test below the
  scenarios pins.

  Background:
    Given the clocks of the service and of the admin's browser read 10.01.2026 12:00
    And the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      | 02.01.2026 12:00 | Boreas | 750    | Einzahlung |
      | 03.01.2026 12:00 | Ärger  | 100    | Einzahlung |
    And the daily collection has run
    And Aurora has chosen the password "Sommer-2026!"
    And Boreas has chosen the password "Winter-2026!"
    And Ärger has chosen the password "Herbst-2026!"

  Rule: Each view counts each time the page shows it, a ledger as the member's own or another's

    Scenario: The views the members open count per view, and the guild row adds them up
      Aurora lands on the overview after logging in, follows her name to her bank and "Lager" to her
      storage, then opens Boreas's storage ledger, the explanation page, the page for changing the
      password and a link to a page the dashboard does not have. Boreas lands on the overview and
      opens the explanation page.

      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has followed the name of "Aurora" in the overview
      And the member has followed "Lager"
      And the member has opened the storage ledger of "Boreas"
      And the member has opened the explanation page
      And the member has opened the page for changing the password
      And the member has opened a link to a page the dashboard does not have
      And another member has logged in as "Boreas" with the password "Winter-2026!"
      And the other member has opened the explanation page
      When the admin opens the admin page
      Then the admin page shows these rows of Aurora, Boreas and the guild, among the others, over the last 30 days, in this order, in these columns:
        | Name   | Übersicht | Bank (eigene) | Bank (fremde) | Lager (eigenes) | Lager (fremdes) | Erklärseite | Passwort ändern | Admin-Seite | Keine Ansicht |
        | Aurora | 1         | 1             | 0             | 1               | 1               | 1           | 1               | 0           | 1             |
        | Boreas | 1         | 0             | 0             | 0               | 0               | 1           | 0               | 0           | 0             |
        | Gilde  | 2         | 1             | 0             | 1               | 1               | 2           | 1               | 0           | 1             |

    Scenario: A view counts each time the member opens it
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has followed the name of "Aurora" in the overview
      And the member has followed "Übersicht"
      And the member has followed the name of "Aurora" in the overview
      When the admin opens the admin page
      Then the admin page gives "2" as Aurora's "Übersicht" over the last 30 days
      And the admin page gives "2" as Aurora's "Bank (eigene)" over the last 30 days

    Scenario: A page of a ledger that does not exist counts as that ledger
      The page tells Aurora "Diese Seite gibt es nicht."

      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has opened a bookmark of page zwei of the bank ledger of "Aurora"
      When the admin opens the admin page
      Then the admin page gives "1" as Aurora's "Bank (eigene)" over the last 30 days

    Scenario Outline: Opening the admin page counts as "Admin-Seite" for <who>
      Boreas opens the admin page himself before the admin looks with the admin key.

      Given Boreas's account has the role "<role>"
      And a member has logged in as "Boreas" with the password "Winter-2026!"
      And the member has opened the admin page
      When the admin opens the admin page
      Then the admin page gives "1" as Boreas's "Admin-Seite" over the last 30 days

      Examples:
        | who                                                        | role   |
        | a member, whom the page tells it is reserved for the admin | member |
        | an account with the admin role                             | admin  |

    Scenario: A ledger of a name that has no account counts as another's, and the admin page names no ledger of it
      The page tells Aurora that there is no avatar named Zephyr.

      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has opened the bank ledger of "Zephyr"
      When the admin opens the admin page
      Then the admin page gives "1" as Aurora's "Bank (fremde)" over the last 30 days
      And the admin page shows no line naming whose ledgers someone opened

  Rule: Each option counts each time a member uses it, and opens no new view

    Scenario Outline: A member who <action> has "<option>" counted <times> and "<view>" counted once
      Aurora lands on the overview after logging in; for her storage ledger, long enough to offer
      "Weiter", she follows her name to her bank and "Lager" from there.

      Given Aurora has 150 movements in the guild storage ledger
      And a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has <gone>
      And the member has <done>
      When the admin opens the admin page
      Then the admin page gives "<times>" as Aurora's "<option>" over the last 30 days
      And the admin page gives "1" as Aurora's "<view>" over the last 30 days

      Examples:
        | action                                                                       | gone                                                      | done                                                                                   | option              | times | view            |
        | switches the last column to "Vor Abzügen" twice, with "Nach Abzügen" between | stayed on the overview after logging in                   | switched the last column to "Vor Abzügen", back to "Nach Abzügen" and to "Vor Abzügen" | Vor Abzügen         | 2     | Übersicht       |
        | sorts the active table by one column twice                                   | stayed on the overview after logging in                   | sorted the active table by "Einlagerung" in ascending and then in descending order     | Übersicht sortiert  | 2     | Übersicht       |
        | pages in her storage ledger                                                  | followed the name of "Aurora" in the overview and "Lager" | followed "Weiter"                                                                      | Vorgänge geblättert | 1     | Lager (eigenes) |
        | sorts her storage ledger                                                     | followed the name of "Aurora" in the overview and "Lager" | sorted the ledger by "Menge" in ascending order                                        | Vorgänge sortiert   | 1     | Lager (eigenes) |

  Rule: The admin page names, for each member, whose ledgers of others the member opened and how often

    Scenario: The admin page names whose ledgers each member opened, and how often
      Boreas comes first: he opens his own storage, which is no ledger of another member, and
      Ärger's bank. Aurora then opens Boreas's storage twice, his bank once and Ärger's storage once.
      Ärger comes last and opens Aurora's bank.

      Given a member has logged in as "Boreas" with the password "Winter-2026!"
      And the member has opened the storage ledger of "Boreas"
      And the member has opened the bank ledger of "Ärger"
      And another member has logged in as "Aurora" with the password "Sommer-2026!"
      And the other member has opened the storage ledger of "Boreas"
      And the other member has reloaded the storage ledger of "Boreas"
      And the other member has opened the bank ledger of "Boreas"
      And the other member has opened the storage ledger of "Ärger"
      And a third member has logged in as "Ärger" with the password "Herbst-2026!"
      And the third member has opened the bank ledger of "Aurora"
      When the admin opens the admin page
      Then the admin page names exactly these ledgers of others the members opened over the last 30 days, in this order:
        | Ärger: Bank von Aurora 1                                         |
        | Aurora: Lager von Ärger 1, Bank von Boreas 1, Lager von Boreas 2 |
        | Boreas: Bank von Ärger 1                                         |

    Scenario: A ledger the admin key opens is named in no line below the table
      Given the admin has logged in as "admin" with the admin key the service's log names
      And the admin has opened the bank ledger of "Aurora"
      When the admin opens the admin page
      Then the admin page shows no line naming whose ledgers someone opened

  Rule: What no member used over the time chosen stands under "Nie genutzt"

    Scenario: What a member used leaves "Nie genutzt"
      Aurora lands on the overview, switches the last column and opens the explanation page. The
      admin opens the admin page with the admin key, which is no member's use, so "Admin-Seite"
      stays under "Nie genutzt".

      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has switched the last column to "Vor Abzügen"
      And the member has opened the explanation page
      When the admin opens the admin page
      Then the admin page names exactly these under "Nie genutzt" over the last 30 days, in any order: "Admin-Seite", "Bank (eigene)", "Bank (fremde)", "Keine Ansicht", "Lager (eigenes)", "Lager (fremdes)", "Passwort ändern", "Übersicht sortiert", "Vorgänge geblättert", "Vorgänge sortiert"

    Scenario Outline: Over <time>, what was used 40 days before the admin looks <stands> under "Nie genutzt", and the lines below the table follow the time chosen too
      Aurora opens the explanation page and Boreas's bank ledger on 10.01.; the admin looks 40 days
      later, on "Letzte 30 Tage" or switched to "Alles".

      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has opened the explanation page
      And the member has opened the bank ledger of "Boreas"
      When <reading>
      Then the admin page <names> "Erklärseite" under "Nie genutzt" over <time>
      And <lines>

      Examples:
        | time                  | reading                                                                                         | stands         | names         | lines                                                                                                                           |
        | the last 30 days      | the admin opens the admin page 40 days after Aurora logged in                                   | stands         | names         | the admin page shows no line naming whose ledgers someone opened over the last 30 days                                          |
        | all the time recorded | the admin opens the admin page 40 days after Aurora logged in and switches the usage to "Alles" | does not stand | does not name | the admin page names exactly these ledgers of others the members opened over all the time recorded: "Aurora: Bank von Boreas 1" |
