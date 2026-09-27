@wip
Feature: A member's bank and storage ledgers
  Every member has two ledgers: the gold they moved through the guild bank and the goods they moved
  through the guild storage, newest first. Each movement shows its time, member, amount or quantity,
  item and quality as the collection read them from the game's protocol (see "Reading the entries
  of the game's protocols"); its direction reads "Einlagerung" or "Entnahme" in the storage ledger.
  The bank ledger is to name a deposit "Einzahlung", as the game does; today it shows "Einlagerung"
  there too. A member can check their own row of the overview against the two ledgers. A ledger
  that "shows exactly" some movements holds these and no others, newest first; movements of the
  same minute may stand in either order.

  @wip
  Scenario: The bank ledger lists a member's gold movements newest first, in the game's words
    Today the bank ledger shows the two deposits as "Einlagerung". This states the corrected
    behavior.

    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 10.01.2024 10:00 | Aurora | 1000   | Einzahlung |
      | 11.01.2024 11:00 | Aurora | 500    | Einzahlung |
      | 12.01.2024 12:00 | Aurora | 200    | Entnahme   |
      | 13.01.2024 09:00 | Boreas | 750    | Einzahlung |
    When a member opens the bank ledger of "Aurora"
    Then the page is headed "Bank von Aurora"
    And the bank ledger of "Aurora" shows exactly:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 12.01.2024 12:00 | Aurora | 200    | Entnahme   |
      | 11.01.2024 11:00 | Aurora | 500    | Einzahlung |
      | 10.01.2024 10:00 | Aurora | 1.000  | Einzahlung |
    And the ledger's caption reads "3 von 3 Einträgen"

  Scenario: The storage ledger lists a member's item movements newest first
    Given the guild storage ledger holds:
      | Zeitpunkt        | Avatar | Menge | Gegenstand          | Qualität | Vorgang     |
      | 15.01.2024 10:00 | Aurora | 10    | Kupfererz           | 100      | Einlagerung |
      | 16.01.2024 11:00 | Aurora | 2     | Magische Ätherbinde | 50       | Einlagerung |
      | 17.01.2024 12:00 | Aurora | 1     | Kristall            | 100      | Entnahme    |
    When a member opens the storage ledger of "Aurora"
    Then the page is headed "Lager von Aurora"
    And the storage ledger of "Aurora" shows exactly:
      | Zeitpunkt        | Avatar | Menge | Gegenstand          | Qualität | Vorgang     |
      | 17.01.2024 12:00 | Aurora | 1     | Kristall            | 100      | Entnahme    |
      | 16.01.2024 11:00 | Aurora | 2     | Magische Ätherbinde | 50       | Einlagerung |
      | 15.01.2024 10:00 | Aurora | 10    | Kupfererz           | 100      | Einlagerung |
    And the ledger offers neither "Zurück" nor "Weiter"

  Scenario: A long ledger is shown a hundred entries at a time
    Given Aurora has 250 movements in the guild storage ledger
    When a member opens the storage ledger of "Aurora"
    Then the ledger shows her 100 newest movements
    And the ledger's caption reads "100 von 250 Einträgen"
    And the ledger offers "Weiter" but not "Zurück"

  Scenario: The last page of a long ledger shows what is left
    Given Aurora has 250 movements in the guild storage ledger
    When a member opens the third page of the storage ledger of "Aurora"
    Then the ledger shows her 50 oldest movements
    And the ledger's caption reads "50 von 250 Einträgen"
    And the ledger offers "Zurück" but not "Weiter"

  Scenario: A page of a ledger can be bookmarked
    Given Aurora has 250 movements in the guild storage ledger
    And a member has followed "Weiter" in the storage ledger of "Aurora"
    When the member opens that page again later from a bookmark
    Then the ledger shows her movements 101 to 200, counted from the newest

  Scenario: A page past the end of a ledger is an empty page, not a failure
    Given Aurora has 250 movements in the guild storage ledger
    When a member opens the fourth page of the storage ledger of "Aurora"
    Then the ledger says "Für Aurora ist hier kein Vorgang gespeichert."
    And the ledger's caption reads "0 von 250 Einträgen"
    And the ledger offers "Zurück" but not "Weiter"

  Scenario: A member who never used one of the ledgers has an empty ledger there
    Given the guild storage ledger holds:
      | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
      | 06.02.2024 10:00 | Brynja | 4     | Eisenbarren | 100      | Einlagerung |
    When a member opens the bank ledger of "Brynja"
    Then the ledger says "Für Brynja ist hier kein Vorgang gespeichert."
    And the ledger's caption reads "0 von 0 Einträgen"

  @characterization
  Scenario Outline: A name the guild ledgers do not know is not a member, in the <ledger> ledger
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 10.01.2024 10:00 | Aurora | 1000   | Einzahlung |
    When a member opens the <ledger> ledger of "Nobody"
    Then the page says "Kein Avatar mit dem Namen Nobody."

    Examples:
      | ledger  |
      | bank    |
      | storage |

  @wip @characterization
  Scenario Outline: A bookmark of a ledger page that <which page> says so in plain German
    Today the page says "Fehler: The API answered 400". This states the corrected behavior.

    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 10.01.2024 10:00 | Aurora | 1000   | Einzahlung |
    When a member opens a bookmark of the bank ledger of "Aurora" that <which page>
    Then the page says "Diese Seite gibt es nicht."

    Examples:
      | which page                        |
      | names a page before the first one |
      | names its page in letters         |
