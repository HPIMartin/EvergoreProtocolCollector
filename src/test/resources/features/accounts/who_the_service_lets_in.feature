@wip
Feature: Who the service lets in
  This is the operator's security requirement. Without a login the service answers only the login
  page with its own files, and the health check with its verdict alone, as "The service's health
  check: its verdict" says; a page of the dashboard, one it does not have included, sends the
  browser to the login page, as "Access with a login" says, and every other address, one it does not
  serve included, is refused. Only a login the service issued, that has not ended, opens anything. A
  login gives the same answer for a name without an account as for a wrong password. The service
  keeps a login in the browser where no script of a page can read it, for 7 days from its arrival
  and, while 0.5.0 runs over plain HTTP, not only for HTTPS; the browser sends it along when a
  member follows a link from another site, such as the guild's chat, but with nothing another site's
  page sends on its own; logging out clears it. Neither the guild's old token nor a name, a password
  or a login written into an address opens anything. The admin's key is in "The admin key". Each row
  names a kind of request or of login, not its content.

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      | 02.01.2026 12:00 | Boreas | 750    | Einzahlung |
    And the daily collection has run
    And Aurora has chosen the password "Sommer-2026!"
    And Boreas has chosen the password "Winter-2026!"

  Rule: Without a login, the service serves only the login page and its files

    Scenario Outline: Without a login, the service <answer> a request for <what>
      When a client without a login asks for <what>
      Then the operator finds the request <result>

      Examples:
        | what                                                                                  | answer  | result                      |
        | the login page                                                                        | serves  | served                      |
        | a file the login page loads                                                           | serves  | served                      |
        | the overview's figures                                                                | refuses | refused for lacking a login |
        | the movements of the bank ledger of "Aurora"                                          | refuses | refused for lacking a login |
        | the admin's status figures                                                            | refuses | refused for lacking a login |
        | a file the dashboard loads                                                            | refuses | refused for lacking a login |
        | the change of a password with the current one                                         | refuses | refused for lacking a login |
        | the descriptions of the service's interface for programs (Swagger, ReDoc and RapiDoc) | refuses | refused for lacking a login |
        | an interface address the service does not serve                                       | refuses | refused for lacking a login |

    Scenario Outline: <credential> in the address opens nothing
      When a client asks for the overview's figures with <credential> in the address
      Then the operator finds the request refused for lacking a login

      Examples:
        | credential                                        |
        | the name "Aurora" and the password "Sommer-2026!" |
        | the value of a login the service issued to Aurora |

    Scenario: The guild's old token opens nothing, even where it is still set for the way back to 0.4.0
      Given the guild's old token is still set among the service's settings
      When a client asks for the overview's figures with the guild's old token in the address
      Then the operator finds the request refused for lacking a login

    Scenario: A login with a name that has no account gets the answer of a wrong password
      Given a client has asked to log in as "Aurora" with the password "Winter-2026!"
      When a client asks to log in as "Zephyr" with the password "Sommer-2026!"
      Then the operator finds the same status and message as for a login as "Aurora" with a wrong password

  Rule: Only a login the service issued, and that has not ended, opens anything

    Scenario Outline: A login <kind> opens no <what>
      Given a client has logged in as "Aurora" with the password "Sommer-2026!"
      When another client asks for <what> with a login <kind>
      Then the operator finds the request refused for lacking a login
      And the operator finds the login the service issued to Aurora opening the overview's figures

      Examples:
        | kind                                     | what                       |
        | altered to claim the admin's role        | the admin's status figures |
        | altered to name another account          | the overview's figures     |
        | signed by someone other than the service | the overview's figures     |
        | that carries no signature at all         | the overview's figures     |

    Scenario: A login copied before it was logged out opens nothing, and Aurora can log in again
      Given a client has logged in as "Aurora" with the password "Sommer-2026!"
      And another client has taken a copy of that login
      And the first client has logged out
      When the other client asks for the overview's figures with the copied login
      Then the operator finds the request refused for lacking a login
      And the operator finds a new login as "Aurora" with the password "Sommer-2026!" opening the overview's figures

  Rule: The browser keeps a login for 7 days, out of reach of the page's scripts, and forgets it on logging out

    Scenario: The service keeps a login for 7 days from its arrival, out of reach of the page's scripts and of what other sites send
      When a client logs in as "Aurora" with the password "Sommer-2026!"
      Then the operator finds the login kept for 7 days counted from its arrival in the browser, whatever date the service's clock reads
      And the operator finds the login out of reach of the page's scripts
      And the operator finds the login not sent along with what another site's page sends on its own
      And the operator finds the login not marked for HTTPS only, since the service still runs over plain HTTP

    Scenario: A link followed from another site arrives with the member's login
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      When the member follows a link to the overview from a page of another site
      Then the page is headed "Übersicht"

    Scenario: Logging out clears the login from the browser
      Given a client has logged in as "Aurora" with the password "Sommer-2026!"
      When the client logs out
      Then the operator finds the login cleared from the client's browser
