Feature: Request throttling and the request log
  The dashboard is reachable from the internet, so the operator needs the service to count the
  requests of every client, turn away a client that sends too many, and write every request it
  counts to its log, where the operator sees the network address and the browser each request came
  from. Requests without a login are counted per network address, so two browsers behind one address
  share their count; requests with a login count apart from them, as "Request throttling with a
  login" says, though a network address that is turned away there is turned away here too. By
  default a network address may send 10 requests without a login at once and then one more every 10
  seconds, and a pause never saves up more than those 10: opening the login page, mistyping the
  password once and logging in fit in that, while a scanner or a password guesser gets no more than
  six requests a minute. A request with a login the service did not issue, or with one that has
  ended, counts as one without a login. A page open to anyone counts and is logged like any closed
  one. An address too long for the server is counted and logged like any other. Not covered here: a
  client that keeps sending while it is turned away, and how many network addresses the service
  keeps count of at once.

  Rule: Without a login, a network address may send 10 requests at once and then one every 10 seconds
    The 10 seconds count from the address's first request, not from its last.

    Background:
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      And the daily collection has run
      And Aurora has chosen the password "Sommer-2026!"

    Scenario Outline: One more request without a login <delay> after the client's first <outcome>
      Given a client without a login has sent 10 requests at once
      When the client sends one more request <delay> after its first
      Then the operator finds <result>

      Examples:
        | delay      | outcome        | result                                               |
        | 9 seconds  | is turned away | the client turned away for sending too many requests |
        | 10 seconds | is served      | the request served                                   |

    Scenario: A client that has spent its 10 requests gets one more every 10 seconds, not 10 more
      Given a client without a login has sent 10 requests at once
      When the client sends 2 requests 10 seconds after its first
      Then the operator finds the first of them served and the second turned away for sending too many requests

    Scenario: A pause saves up no more than 10 requests
      Given a client without a login has sent 10 requests at once
      When the client sends 11 requests 5 minutes after its first
      Then the operator finds the first 10 of them served and the 11th turned away for sending too many requests

    Scenario: A client without a login that asks every 10 seconds is never turned away
      When a client without a login asks for the health check every 10 seconds for 5 minutes
      Then the operator finds every one of its requests served

    Scenario: Without a login, a request from another network address is counted apart
      Given a client at the network address "203.0.113.7" was turned away for sending too many requests without a login
      When a client at the network address "203.0.113.8" asks for the login page without a login
      Then the operator finds the request served

    Scenario Outline: A request without a login for <page> counts like any other
      Given a client without a login has sent 10 requests at once
      When the client asks for <page> without a login 5 seconds after its first request
      Then the operator finds the client turned away for sending too many requests

      Examples:
        | page                                         |
        | the login page                               |
        | a file the login page loads                  |
        | the health check                             |
        | the overview                                 |
        | a page whose address is 5000 characters long |
        | a login as "Aurora" with her password        |

    Scenario Outline: A request with a login <kind> counts like one without a login
      Given a client without a login has sent 10 requests at once
      When the client asks for the health check with a login <kind> 5 seconds after its first request
      Then the operator finds the client turned away for sending too many requests

      Examples:
        | kind                                                                |
        | that names "Aurora" but is signed by someone other than the service |
        | that names "Aurora" and carries no signature at all                 |
        | of "Aurora" that has ended with her logging out                     |
        | of "Aurora" that has run its 7 days                                 |
        | of "Aurora" from before a restart of the service                    |
        | whose value is empty                                                |

    Scenario: A member who opens a bookmark, mistypes the password once and logs in is not turned away
      Given a member who is not logged in has opened a bookmark of the overview
      And the member has tried to log in as "Aurora" with the password "Winter-2026!"
      When the member logs in as "Aurora" with the password "Sommer-2026!"
      Then the page is headed "Übersicht"

  Rule: Every request is written to the log

    Background:
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      And the daily collection has run
      And Aurora has chosen the password "Sommer-2026!"

    Scenario Outline: The log names the network address and the browser of a request for <page> <how>
      When a client at the network address "203.0.113.7" asks for <page> <how> with the browser "Firefox/128.0"
      Then the operator finds a line naming "203.0.113.7" and "Firefox/128.0" in the service's log

      Examples:
        | page                                         | how                   |
        | the login page                               | without a login       |
        | the overview                                 | without a login       |
        | the overview                                 | logged in as "Aurora" |
        | a page whose address is 5000 characters long | without a login       |

    Scenario: A request that is turned away is still written to the log
      Given a client at the network address "203.0.113.7" with the browser "curl/8.9" was turned away for sending too many requests without a login
      When the client asks for the login page without a login with the browser "Firefox/128.0"
      Then the operator finds the client turned away for sending too many requests
      And the operator finds a line naming "203.0.113.7" and "Firefox/128.0" in the service's log
