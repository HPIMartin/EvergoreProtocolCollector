@wip
Feature: Finding one's way through the dashboard
  The dashboard has the overview and, for every member, a bank ledger and a storage ledger. Every
  page can be bookmarked, and the overview links from a member's name and last-activity dates to the
  ledgers behind them. No page of the dashboard links to the admin page; the admin opens it
  directly.

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar     | Betrag | Vorgang    |
      | 10.01.2024 10:00 | Aurora     | 1000   | Einzahlung |
      | 10.01.2024 11:00 | Hans Meyer | 100    | Einzahlung |
      | 10.01.2024 12:00 | Ärger      | 100    | Einzahlung |
    And the guild storage ledger holds:
      | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
      | 15.01.2024 10:00 | Brynja | 10    | Eisenbarren | 100      | Einlagerung |
    And the daily collection has run

  Scenario Outline: <way> shows the overview
    When a member opens <way>
    Then the page is headed "Übersicht"

    Examples:
      | way                        |
      | the guild's link           |
      | a bookmark of the overview |

  Scenario: The frame of a ledger links to the overview and to both of that member's ledgers
    When a member opens the storage ledger of "Aurora"
    Then the page's frame offers exactly the links:
      | link      | leads to                       | marked as current |
      | Übersicht | the overview                   | no                |
      | Bank      | the bank ledger of "Aurora"    | no                |
      | Lager     | the storage ledger of "Aurora" | yes               |

  Scenario: The frame of the overview links only to the overview
    When a member opens the overview
    Then the page's frame offers exactly the links:
      | link      | leads to     | marked as current |
      | Übersicht | the overview | yes               |

  Scenario: A member's row leads to the movements behind it
    When a member opens the overview
    Then in Aurora's row the name leads to the bank ledger of "Aurora"
    And in Aurora's row the "Letzte Bankaktivität" leads to the bank ledger of "Aurora"
    And in Brynja's row the "Letzte Lageraktivität" leads to the storage ledger of "Brynja"

  Scenario: An activity cell without a date leads nowhere
    When a member opens the overview
    Then in Aurora's row the "Letzte Lageraktivität" shows "–" and leads nowhere

  Scenario Outline: The bank ledger of "<member>" opens from their name like any other
    Given a member has opened the overview
    When the member follows the name of "<member>" in the overview
    Then the page is headed "Bank von <member>"
    And the ledger shows a movement of <gold> gold

    Examples:
      | member     | gold |
      | Hans Meyer | 100  |
      | Ärger      | 100  |

  Scenario: A link to a page the dashboard does not have says so
    When a member opens a link to a page the dashboard does not have
    Then the page says there is no view for that link

  Scenario: The overview does not link to the admin page
    When a member opens the overview
    Then no link on the page leads to the admin page

  Scenario: The admin page links back to the overview only
    When the admin opens the admin page
    Then the page's frame offers exactly the links:
      | link      | leads to     | marked as current |
      | Übersicht | the overview | no                |
