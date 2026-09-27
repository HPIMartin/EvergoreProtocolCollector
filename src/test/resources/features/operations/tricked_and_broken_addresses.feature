@wip
Feature: Tricked and broken addresses do not open the guild's figures
  The dashboard's pages open only with the guild's token. An address that tries to get around that
  check is refused like any other request without the token; an address that is garbled is refused
  as garbled. This is the operator's security requirement. Each row names a kind of address, not
  the address itself.

  Scenario Outline: An address that <kind> does not open the guild's figures
    When a client asks for an address that <kind> without the guild's token
    Then the operator finds the address <answer>

    Examples:
      | kind                                                                     | answer                                     |
      | climbs up out of the dashboard's own files and back down to the overview | refused like any request without the token |
      | makes that same climb in a disguised spelling                            | refused like any request without the token |
      | starts the overview's address with a doubled slash                       | refused like any request without the token |
      | leads to the overview but contains a garbled character                   | refused as garbled                         |
      | leads to the bank ledger of "Aurora" but contains a garbled character    | refused as garbled                         |
