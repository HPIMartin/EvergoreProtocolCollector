Feature: Finding one's way through the dashboard
  The dashboard has the overview, a page explaining its figures and, for every member, a bank ledger
  and a storage ledger. Every page carries a frame at its top: the bar of links, the name the member
  is logged in as, the button for changing the password, except under the admin key, and the logout
  button; a page's own content can hold further links. Every page can be bookmarked. The overview's
  content links, under the guild's four figures, to the explanation page, and from a member's name
  and last-activity dates to the ledgers behind them. No page of the dashboard links to the admin
  page; the admin opens it directly.

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar     | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora     | 1000   | Einzahlung |
      | 02.01.2026 12:00 | Hans Meyer | 100    | Einzahlung |
      | 03.01.2026 12:00 | Ärger      | 100    | Einzahlung |
    And the guild storage ledger holds:
      | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
      | 04.01.2026 12:00 | Brynja | 10    | Eisenbarren | 100      | Einlagerung |
    And the daily collection has run

  Scenario: A bookmark of the overview shows the overview
    When a member opens a bookmark of the overview
    Then the page is headed "Übersicht"

  Scenario: The overview leads to the explanation of its figures
    Given a member has opened the overview
    When the member follows "Wie entstehen diese Zahlen?"
    Then the page is headed "So entstehen eure Zahlen"

  Scenario: The frame of a ledger links to the overview and to both of that member's ledgers
    When a member opens the storage ledger of "Aurora"
    Then the page's frame offers exactly the links:
      | link      | leads to                       | marked as current |
      | Übersicht | the overview                   | no                |
      | Bank      | the bank ledger of "Aurora"    | no                |
      | Lager     | the storage ledger of "Aurora" | yes               |

  Scenario: A member's last-activity dates lead to the ledgers behind them
    When a member opens the overview
    Then in Aurora's row the "Letzte Bankaktivität" leads to the bank ledger of "Aurora"
    And in Brynja's row the "Letzte Lageraktivität" leads to the storage ledger of "Brynja"

  Scenario: An activity cell without a date leads nowhere
    When a member opens the overview
    Then in Aurora's row the "Letzte Lageraktivität" shows "–" and leads nowhere

  Scenario Outline: A member's name leads to their bank ledger: "<member>"
    Given a member has opened the overview
    When the member follows the name of "<member>" in the overview
    Then the page is headed "Bank von <member>"
    And the ledger shows a movement of <gold> gold

    Examples:
      | member     | gold  |
      | Aurora     | 1.000 |
      | Hans Meyer | 100   |
      | Ärger      | 100   |

  Scenario: A link to a page the dashboard does not have says so
    When a member opens a link to a page the dashboard does not have
    Then the page says there is no view for that link

  Scenario Outline: <page> does not link to the admin page
    When a member opens <page>
    Then no link on the page leads to the admin page

    Examples:
      | page                 |
      | the overview         |
      | the explanation page |

  Scenario Outline: The frame of <page> offers the overview as its only link
    When <reading>
    Then the page's frame offers exactly the links:
      | link      | leads to     | marked as current |
      | Übersicht | the overview | <current>         |

    Examples:
      | page                 | reading                             | current |
      | the overview         | a member opens the overview         | yes     |
      | the admin page       | the admin opens the admin page      | no      |
      | the explanation page | a member opens the explanation page | no      |
