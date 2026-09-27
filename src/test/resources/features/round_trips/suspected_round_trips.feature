@wip
Feature: Suspected round trips of trader goods
  Goods bought from the guild trader credit their full market value when deposited but cost only
  60 % when withdrawn, so taking them out and putting them back in earns 40 % out of nothing. The
  guild's rule relies on trust; the service does not prevent a round trip, it makes one visible:
  after each collection the admin page names every member who withdrew a trader good and deposited
  it again within 48 hours, with the quantity that went round, unless a crafted product explains it
  (see "Crafting is not a round trip"). All times are German wall-clock time, as the game shows
  them.

  Rule: A trader good withdrawn and deposited again within 48 hours is reported

    Scenario Outline: Only the quantity that went out and came back counts, whatever its quality: <out> out, <in> in
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität      | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | <out> | Federn     | <out quality> | Entnahme    |
        | 01.01.2026 13:00 | Alrik  | <in>  | Federn     | <in quality>  | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Verdacht auf Warenkreislauf: Alrik: <round trip> × Federn"

      Examples:
        | out | out quality | in  | in quality | round trip |
        | 100 | 100         | 100 | 100        | 100        |
        | 100 | 100         | 60  | 100        | 60         |
        | 50  | 100         | 80  | 100        | 50         |
        | 100 | 50          | 100 | 100        | 100        |

    Scenario Outline: A deposit <when> is still reported
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 100   | Federn     | 100      | Entnahme    |
        | <deposit time>   | Alrik  | 100   | Federn     | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Verdacht auf Warenkreislauf: Alrik: 100 × Federn"

      Examples:
        | when                                  | deposit time     |
        | exactly 48 hours after the withdrawal | 03.01.2026 12:00 |
        | in the same minute as the withdrawal  | 01.01.2026 12:00 |

    Scenario: A deposit 48 hours and one minute after a withdrawal no longer answers it, a younger withdrawal still does
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 100   | Federn     | 100      | Entnahme    |
        | 02.01.2026 12:00 | Alrik  | 50    | Federn     | 100      | Entnahme    |
        | 03.01.2026 12:01 | Alrik  | 100   | Federn     | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Verdacht auf Warenkreislauf: Alrik: 50 × Federn"

    Scenario: Open withdrawals are matched oldest first
      Matched newest first, the first deposit would use up the second withdrawal, and the first
      withdrawal would be too old for the second deposit: 50 instead of 100.

      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 50    | Federn     | 100      | Entnahme    |
        | 02.01.2026 12:00 | Alrik  | 50    | Federn     | 100      | Entnahme    |
        | 02.01.2026 13:00 | Alrik  | 50    | Federn     | 100      | Einlagerung |
        | 03.01.2026 12:01 | Alrik  | 50    | Federn     | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Verdacht auf Warenkreislauf: Alrik: 100 × Federn"

    Scenario: A member's round trips of one good add up
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 100   | Federn     | 100      | Entnahme    |
        | 01.01.2026 13:00 | Alrik  | 100   | Federn     | 100      | Einlagerung |
        | 04.01.2026 12:00 | Alrik  | 50    | Federn     | 100      | Entnahme    |
        | 04.01.2026 13:00 | Alrik  | 50    | Federn     | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Verdacht auf Warenkreislauf: Alrik: 150 × Federn"

    Scenario: A deposit only answers the same member's withdrawal of the same good
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 100   | Federn     | 100      | Entnahme    |
        | 01.01.2026 13:00 | Brynn  | 100   | Federn     | 100      | Einlagerung |
        | 01.01.2026 14:00 | Alrik  | 100   | Harz       | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page lists no suspected round trip

  Rule: Only goods bought from the guild trader are watched
    Any other good credits at most the 60 % its withdrawal costs, so taking it out and bringing it
    back earns nothing.

    Scenario Outline: "<item>" going out and coming back is not reported
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Alrik  | 100   | <item>     | 100      | Entnahme    |
        | 01.01.2026 13:00 | Alrik  | 100   | <item>     | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page lists no suspected round trip

      Examples:
        | item        |
        | Kupfererz   |
        | Eisenbarren |
