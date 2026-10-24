@wip
Feature: Tricked and broken addresses do not open the guild's figures
  The dashboard's pages open only after a login. An address that tries to get around that check is
  answered like the same address without a trick: a page sends the browser to the login page, the
  guild's figures are refused for lacking a login; an address that is garbled is refused as garbled.
  This is the operator's security requirement. Each row names a kind of address, not the address
  itself.

  Scenario Outline: An address that <kind> does not open the guild's figures
    When a client without a login asks for an address that <kind>
    Then the operator finds the client <answer>

    Examples:
      | kind                                                                                  | answer                      |
      | climbs up out of the login page's own files and back down to the overview             | sent to the login page      |
      | climbs out of the login page's files and down to the overview in a disguised spelling | sent to the login page      |
      | climbs out of the login page's files, written in other capitals, to the overview      | sent to the login page      |
      | starts like the login page's address and continues to the overview's figures          | refused for lacking a login |
      | starts like the health check's address and continues to the overview's figures        | refused for lacking a login |
      | climbs up out of the health check's address and back down to the overview             | sent to the login page      |
      | starts the overview's address with a doubled slash                                    | sent to the login page      |
      | leads to the overview but contains a garbled character                                | refused as garbled          |
      | leads to the bank ledger of "Aurora" but contains a garbled character                 | refused as garbled          |
