Feature: Request throttling with a login
  The operator needs the service to turn away a client that sends too many requests with a login
  too, whether it stays at one network address or not. Requests with a login count in pots: a pot
  holds the network addresses and accounts its requests link. A request with the same account from
  another network address counts in the same pot, and so does a request with another account from
  the same network address; two pots that one request links count together from then on. By default
  a pot may send 30 requests with a login in a count that starts with its first such request and
  runs for 10 seconds; the 31st within that count turns away every network address and every account
  in the pot for one minute; a network address that is turned away is turned away for its requests
  without a login too, the login page included. A pot whose count has run out and in which nothing
  is turned away is forgotten, so its network addresses and accounts are counted apart again; a pot
  that was turned away is forgotten once its minute is over. Requests without a login, the login
  itself among them, count apart from all pots, as "Request throttling and the request log" says.
  Not covered here: a client that keeps sending while it is turned away, and how many network
  addresses and accounts the service keeps count of at once.

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      | 02.01.2026 12:00 | Boreas | 750    | Einzahlung |
      | 03.01.2026 12:00 | Calix  | 300    | Einzahlung |
    And the daily collection has run
    And Aurora has chosen the password "Sommer-2026!"
    And Boreas has chosen the password "Sommer-2026!"
    And Calix has chosen the password "Sommer-2026!"

  Rule: A pot may send 30 requests in 10 seconds, and the 31st turns it away for one minute

    Scenario: A pot may send 30 requests, and the 31st is turned away
      When a client logged in as "Aurora" sends 31 requests within its first second
      Then the operator finds the first 30 of them served and the 31st turned away for sending too many requests

    Scenario Outline: One more request <delay> after the pot's first <outcome>
      Given a client logged in as "Aurora" has sent 30 requests within its first second
      When the client sends one more request <delay> after its first
      Then the operator finds <result>

      Examples:
        | delay      | outcome              | result                                               |
        | 9 seconds  | is turned away       | the client turned away for sending too many requests |
        | 10 seconds | starts a fresh count | the request served                                   |

    Scenario Outline: A client with a login that was turned away <outcome> <delay> later
      Given a client logged in as "Aurora" was turned away for sending too many requests
      When the client sends a request <delay> after it was turned away
      Then the operator finds <result>

      Examples:
        | outcome              | delay      | result                                               |
        | is still turned away | 59 seconds | the client turned away for sending too many requests |
        | is served again      | one minute | the request served                                   |

    Scenario: Requests with a login do not draw from the count of the same address without one
      The login and nine more requests without a login use up the 10 at once of this address.

      Given a client at the network address "198.51.100.1" has logged in as "Aurora"
      And the client has sent 9 requests without a login within its first second
      When the client asks for the overview's figures with its login 5 seconds after its first request
      Then the operator finds the request served

    Scenario: Requests without a login do not draw from the pot of the same address
      Counted in one pot, the 25 requests with the login and the 9 without would pass its 30 before
      the request 5 seconds later.

      Given a client at the network address "198.51.100.1" logged in as "Aurora" has sent 25 requests within its first second
      And the client has sent 9 requests without a login within that second
      When the client asks for the overview's figures with its login 5 seconds after its first request
      Then the operator finds the request served

    Scenario Outline: A network address that is turned away <outcome> for the login page <delay> later
      Given a client at the network address "198.51.100.1" logged in as "Aurora" was turned away for sending too many requests
      When a client at the network address "198.51.100.1" asks for the login page without a login <delay> after the first was turned away
      Then the operator finds <result>

      Examples:
        | outcome              | delay      | result                                               |
        | is still turned away | 59 seconds | the client turned away for sending too many requests |
        | is served again      | one minute | the request served                                   |

  Rule: A network address or an account links requests into one pot

    Scenario Outline: While a pot is turned away, a request <link> <outcome>
      Given a client at the network address "198.51.100.1" logged in as "Aurora" was turned away for sending too many requests
      When a client at the network address "<address>" logged in as "<account>" sends a request 30 seconds after the first was turned away
      Then the operator finds <result>

      Examples:
        | link                                       | outcome            | address      | account | result                                               |
        | with the same account from another address | is turned away too | 198.51.100.2 | Aurora  | the client turned away for sending too many requests |
        | with another account from the same address | is turned away too | 198.51.100.1 | Boreas  | the client turned away for sending too many requests |
        | with another account from another address  | is served          | 198.51.100.2 | Boreas  | the request served                                   |

    Scenario: Every network address of a pot that is turned away is turned away for its requests without a login
      Aurora has used both network addresses within one count, so they stand in one pot; its 31st
      request comes from the first.

      Given a client at the network address "198.51.100.2" logged in as "Aurora" has sent a request
      And a client at the network address "198.51.100.1" logged in as "Aurora" has sent 30 requests within that second, the last of them turned away for sending too many requests
      When a client at the network address "198.51.100.2" asks for the login page without a login 30 seconds after the first request
      Then the operator finds the client turned away for sending too many requests

    Scenario: Two pots that one request links count together
      Aurora's pot has sent 20 requests and Boreas's 10, so the request that links them is the 31st.

      Given a client at the network address "198.51.100.1" logged in as "Aurora" has sent 20 requests within its first second
      And a client at the network address "198.51.100.2" logged in as "Boreas" has sent 10 requests within that second
      When a client at the network address "198.51.100.2" logged in as "Aurora" sends a request 5 seconds after the first
      Then the operator finds the client turned away for sending too many requests

    Scenario: A pot that was turned away no longer links its network addresses once its minute is over
      Aurora's pot was turned away at the first address. A minute later her 30 requests from the
      second address start a pot of their own, and Boreas at the first address no longer counts in
      it.

      Given a client at the network address "198.51.100.1" logged in as "Aurora" was turned away for sending too many requests
      And a client at the network address "198.51.100.2" logged in as "Aurora" has sent 30 requests one minute after it was turned away
      When a client at the network address "198.51.100.1" logged in as "Boreas" sends a request within that second
      Then the operator finds the request served

    Scenario: A forgotten pot no longer links its network addresses
      Aurora linked both network addresses in the first count. Ten seconds later that count has run
      out, so Boreas's 30 requests from the first address start a pot of their own, and a request
      from the second address no longer counts in it.

      Given a client at the network address "198.51.100.1" logged in as "Aurora" has sent a request
      And a client at the network address "198.51.100.2" logged in as "Aurora" has sent a request within that second
      And a client at the network address "198.51.100.1" logged in as "Boreas" has sent 30 requests 10 seconds after the first
      When a client at the network address "198.51.100.2" logged in as "Calix" sends a request 11 seconds after the first
      Then the operator finds the request served
