@wip
Feature: Request throttling and the request log
  The dashboard is reachable from the internet, so the operator needs the service to count the
  requests of every client network address, turn away a client that sends too many, and write every
  request it counts to its log, where the operator sees the network address and the browser each
  request came from. A page open to anyone counts and is logged like any closed one. By default a
  client may send 30 requests in a count that starts with its first request and runs for 10 seconds;
  the 31st within that count turns it away for one minute. An address too long for the server is
  today answered before it is counted or logged; the two scenarios about it below state the
  corrected behavior. Not covered here: a client that keeps sending while it is turned away, and how
  many client addresses the service keeps count of at once.

  Rule: A client that sends too many requests is turned away for a while

    Scenario Outline: One more request <delay> after the client's first <outcome>
      Given a client has sent 30 requests within its first second
      When the client sends one more request <delay> after its first
      Then the service <result>

      Examples:
        | delay      | outcome              | result                                             |
        | 9 seconds  | is turned away       | turns the client away as sending too many requests |
        | 10 seconds | starts a fresh count | serves the request                                 |

    Scenario Outline: A client that was turned away <outcome> <delay> later
      Given a client was turned away for sending too many requests
      When the client sends a request <delay> after it was turned away
      Then the service <result>

      Examples:
        | outcome              | delay      | result                                             |
        | is still turned away | 59 seconds | turns the client away as sending too many requests |
        | is served again      | one minute | serves the request                                 |

    Scenario: Clients are counted apart
      Given a client was turned away for sending too many requests
      When another client sends a request
      Then the service serves the request

    Scenario Outline: A request for <page> counts like any other
      Given a client has sent 30 requests within its first second
      When the client asks for <page> without a token 5 seconds after its first request
      Then the service turns the client away as sending too many requests

      Examples:
        | page                       |
        | the dashboard's start page |
        | a file the dashboard loads |
        | the health check           |
        | the overview               |

    @wip
    Scenario: A request for an oversized page address counts like any other
      Given a client has sent 30 requests within its first second
      When the client asks for a page whose address is 5000 characters long without a token 5 seconds after its first request
      Then the service turns the client away as sending too many requests

  Rule: Every request is written to the log

    Scenario Outline: The log names the network address and the browser of a request for <page>
      When a client at the network address "203.0.113.7" asks for <page> without a token with the browser "Firefox/128.0"
      Then the service's log has a line naming "203.0.113.7" and "Firefox/128.0"

      Examples:
        | page                       |
        | the dashboard's start page |
        | the overview               |

    Scenario: A request that is turned away is still written to the log
      Given a client at the network address "203.0.113.7" with the browser "curl/8.9" was turned away for sending too many requests
      When the client asks for the dashboard's start page without a token with the browser "Firefox/128.0"
      Then the service turns the client away as sending too many requests
      And the service's log has a line naming "203.0.113.7" and "Firefox/128.0"

    @wip
    Scenario: A request for an oversized page address is written to the log like any other
      When a client at the network address "203.0.113.7" asks for a page whose address is 5000 characters long without a token with the browser "Firefox/128.0"
      Then the service's log has a line naming "203.0.113.7" and "Firefox/128.0"
