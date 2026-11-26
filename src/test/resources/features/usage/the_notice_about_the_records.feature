Feature: The notice about the usage records
  Every member learns of the usage records ("Who uses the app, and when", "What the guild uses")
  from a notice on every view of the dashboard, with a link to the explanation page, which says what
  is recorded and who sees it, and the button "Verstanden"; the link is no link of the page's frame.
  The notice informs and asks for no consent: the records are written whether it stands or has been
  answered, so while it stands everything "Who uses the app, and when" counts is counted as
  usual. It stays
  until the member answers "Verstanden", once for the account: it does not come back at a later
  login, on another browser or after a restart of the service, and another member, an account with
  the admin role included, still sees it. The admin key's login has no account and sees no notice.
  An answer the service cannot keep leaves the notice gone from the page and back at the next
  loading of a page, which a test below the scenarios pins.

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      | 02.01.2026 12:00 | Boreas | 750    | Einzahlung |
    And the daily collection has run
    And Aurora has chosen the password "Sommer-2026!"
    And Boreas has chosen the password "Winter-2026!"

  Rule: The notice stands on every view until the member answers "Verstanden"

    @wip
    Scenario Outline: The notice stands on <view> for a member who has not answered it
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      When the member opens <view>
      Then the page says "Hinweis: Die App zeichnet auf, wann ihr euch anmeldet und welche Ansichten und Optionen ihr nutzt, damit wir sehen, was die Gilde braucht. IP-Adressen zeichnet sie dabei nicht auf. Mehr dazu auf der Seite „So entstehen eure Zahlen“."
      And the notice offers the button "Verstanden"

      Examples:
        | view                               |
        | the overview                       |
        | the bank ledger of "Aurora"        |
        | the storage ledger of "Boreas"     |
        | the explanation page               |
        | the page for changing the password |
        | the admin page                     |
        | a page the dashboard does not have |

    @wip
    Scenario: A notice the member has not answered stands at a later login
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has logged out
      When a member logs in as "Aurora" with the password "Sommer-2026!"
      Then the page says "Hinweis: Die App zeichnet auf, wann ihr euch anmeldet und welche Ansichten und Optionen ihr nutzt, damit wir sehen, was die Gilde braucht. IP-Adressen zeichnet sie dabei nicht auf. Mehr dazu auf der Seite „So entstehen eure Zahlen“."

    @wip
    Scenario: The notice's link leads to the explanation page
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      When the member follows the notice's link "So entstehen eure Zahlen"
      Then the page is headed "So entstehen eure Zahlen"

    @wip
    Scenario: Answering "Verstanden" removes the notice from the page
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      When the member answers the notice with "Verstanden"
      Then the page is headed "Übersicht"
      And the page does not say "Hinweis: Die App zeichnet auf"
      And the page offers no button "Verstanden"

    @wip
    Scenario Outline: After "Verstanden", the notice stands on no other view <where>
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has answered the notice with "Verstanden"
      When the member <does>
      Then the page is headed "<heading>"
      And the page does not say "Hinweis: Die App zeichnet auf"

      Examples:
        | where                         | does                                         | heading         |
        | reached by a link in the page | follows the name of "Aurora" in the overview | Bank von Aurora |
        | after a reload                | reloads the overview                         | Übersicht       |

  Rule: "Verstanden" ends the notice for the account, and for no other

    @wip
    Scenario Outline: After "Verstanden", the notice does not come back at a later login <where>
      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has answered the notice with "Verstanden"
      And <before>
      When <logging in>
      Then the page is headed "Übersicht"
      And the page does not say "Hinweis: Die App zeichnet auf"

      Examples:
        | where                          | before                               | logging in                                                                       |
        | on another browser             | the member has logged out            | a member logs in as "Aurora" with the password "Sommer-2026!" on another browser |
        | after a restart of the service | the service has been restarted since | a member logs in as "Aurora" with the password "Sommer-2026!"                    |

    @wip
    Scenario Outline: After Aurora answered the notice, <who> still sees it
      Given Boreas's account has the role "<role>"
      And a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has answered the notice with "Verstanden"
      When another member logs in as "Boreas" with the password "Winter-2026!"
      Then the page says "Hinweis: Die App zeichnet auf, wann ihr euch anmeldet und welche Ansichten und Optionen ihr nutzt, damit wir sehen, was die Gilde braucht. IP-Adressen zeichnet sie dabei nicht auf. Mehr dazu auf der Seite „So entstehen eure Zahlen“."

      Examples:
        | who                                      | role   |
        | Boreas                                   | member |
        | Boreas, whose account has the admin role | admin  |

    Scenario: The admin key's login shows no notice
      Given the admin has logged in as "admin" with the admin key the service's log names
      When the admin opens the overview
      Then the page is headed "Übersicht"
      And the page does not say "Hinweis: Die App zeichnet auf"

  Rule: The records are written after the notice has been answered too

    @wip
    Scenario: A member who answered the notice still has what she does afterwards counted
      Aurora lands on the overview after logging in, one loaded page, answers the notice and
      reloads the overview, the second.

      Given a member has logged in as "Aurora" with the password "Sommer-2026!"
      And the member has answered the notice with "Verstanden"
      And the member has reloaded the overview
      When the admin opens the admin page
      Then the admin page gives "2" as Aurora's "Seite geladen" over the last 30 days
