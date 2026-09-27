@wip
Feature: Access with the guild's link
  The guild's figures are for its members only. The guild hands out one link that carries the
  guild's token; whoever opens that link sees the dashboard, and every link the dashboard shows
  opens its page for them as well. Without the token the figures stay closed: the start page says
  what is missing, and any other page of the dashboard shows nothing at all. The admin page opens
  without the token (see "The admin page: how the daily collection is going").

  Scenario: The guild's link opens the overview
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 10.01.2024 10:00 | Aurora | 1000   | Einzahlung |
    And the daily collection has run
    When a member opens the guild's link
    Then the overview shows:
      | Avatar | Bank-Einzahlung |
      | Aurora | 1.000           |

  Scenario Outline: The start page opened <how> says that the link needs its token
    When a member opens the dashboard's start page <how>
    Then the page says "Kein gültiges Token: der Link braucht ein token in der Adresse."

    Examples:
      | how                |
      | without the token  |
      | with a wrong token |

  Scenario Outline: A bookmark of <page> opened <how> shows nothing
    When a member opens a bookmark of <page> <how>
    Then the browser shows no page of the dashboard

    Examples:
      | page                               | how                |
      | the overview                       | without the token  |
      | the bank ledger of "Aurora"        | without the token  |
      | the storage ledger of "Aurora"     | with a wrong token |
      | a page the dashboard does not have | without the token  |

  Scenario Outline: Every link on <page> opens its page
    Aurora's bank ledger is long enough to offer "Weiter".

    Given Aurora has 150 movements in the guild bank ledger
    And the daily collection has run
    When a member follows the guild's link to <page>
    Then every link on the page opens its page and none of them says "Kein gültiges Token: der Link braucht ein token in der Adresse."

    Examples:
      | page                        |
      | the overview                |
      | the bank ledger of "Aurora" |
