@wip
Feature: Access with a login
  The guild's figures are for its members only. A member logs in on the login page; once logged in,
  the member sees the dashboard, and every link the dashboard shows opens its page as well. Without
  a login every page of the dashboard shows the login page, and after the login the page the member
  opened, so a bookmark keeps working; the login returns only to a page of this service. The
  explanation page shows none of the guild's figures but needs a login as well: the valuation rules
  it explains are meant for the members. The frame's logout is a button, not a link. How the login
  itself works is in "Logging in and out". A ledger shows 100 movements a page. Where another
  feature's member or admin opens a page, they do so with a login of an account that does not rest.

  Background:
    Given Aurora has 150 movements in the guild bank ledger
    And the daily collection has run
    And Aurora has chosen the password "Sommer-2026!"

  Rule: Without a login, a bookmark leads through the login page to its page

    Scenario Outline: A bookmark of <page> opened without a login shows the login page
      When a member who is not logged in opens a bookmark of <page>
      Then the page is headed "Anmeldung"

      Examples:
        | page                                                |
        | the dashboard's start page                          |
        | the overview                                        |
        | the bank ledger of "Aurora"                         |
        | the explanation page                                |
        | the admin page                                      |
        | a page the dashboard does not have                  |
        | the guild's old link, from before members logged in |

    Scenario Outline: After the login, a bookmark of <page> shows that page
      Given a member who is not logged in has opened a bookmark of <page>
      When the member logs in as "Aurora" with the password "Sommer-2026!"
      Then <shown>

      Examples:
        | page                                                | shown                                                              |
        | the storage ledger of "Aurora"                      | the page is headed "Lager von Aurora"                              |
        | page 2 of the bank ledger of "Aurora"               | the ledger shows her movements 101 to 150, counted from the newest |
        | the explanation page                                | the page is headed "So entstehen eure Zahlen"                      |
        | the admin page                                      | the page says "Diese Seite ist dem Admin vorbehalten."             |
        | a page the dashboard does not have                  | the page says there is no view for that link                       |
        | the guild's old link, from before members logged in | the page is headed "Übersicht"                                     |

    Scenario: A mistyped password keeps the page the member opened
      Given a member who is not logged in has opened a bookmark of the storage ledger of "Aurora"
      And the member has tried to log in as "Aurora" with the password "Winter-2026!"
      When the member logs in as "Aurora" with the password "Sommer-2026!"
      Then the page is headed "Lager von Aurora"

    Scenario Outline: A login asked to return to another site <how> leads to the overview, as <surface> shows
      Given <arrival>
      When <logging in>
      Then <result>

      Examples:
        | how                            | surface               | arrival                                                                                                         | logging in                                                      | result                                                        |
        | by its full address            | the member's page     | a member has followed a link to the login page that would lead on to another site by its full address           | the member logs in as "Aurora" with the password "Sommer-2026!" | the page is headed "Übersicht"                                |
        | disguised as this service's    | the member's page     | a member has followed a link to the login page that would lead on to another site disguised as this service's   | the member logs in as "Aurora" with the password "Sommer-2026!" | the page is headed "Übersicht"                                |
        | by its full address            | the operator's answer | a client has asked for the login page with the request to return to another site by its full address            | the client logs in as "Aurora" with the password "Sommer-2026!" | the operator finds the login's answer leading to the overview |
        | behind a doubled slash         | the operator's answer | a client has asked for the login page with the request to return to another site behind a doubled slash         | the client logs in as "Aurora" with the password "Sommer-2026!" | the operator finds the login's answer leading to the overview |
        | behind a slash and a backslash | the operator's answer | a client has asked for the login page with the request to return to another site behind a slash and a backslash | the client logs in as "Aurora" with the password "Sommer-2026!" | the operator finds the login's answer leading to the overview |

    Scenario: A page opened without a login is redirected to the login page, with the page as the way back
      When a client without a login asks for the bank ledger of "Aurora"
      Then the operator finds the client redirected to the login page, with the request to return to the bank ledger of "Aurora"

  Rule: Once logged in, every link the dashboard shows opens its page

    Scenario Outline: Every link on <page> opens its page
      Aurora's bank ledger is long enough to offer "Weiter".

      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      When the member opens <page>
      Then every link on the page opens its page and none of them shows the login page

      Examples:
        | page                        |
        | the overview                |
        | the bank ledger of "Aurora" |
        | the explanation page        |

  Rule: A page that is open when the login ends leads to the login page, and back after the login

    Scenario: A member whose login ended while the overview was open is asked to log in again and then sees the page they chose
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has opened the overview
      And the service has been restarted since
      And the member has followed the name of "Aurora" in the overview to the login page
      When the member logs in as "Aurora" with the password "Sommer-2026!"
      Then the page is headed "Bank von Aurora"
