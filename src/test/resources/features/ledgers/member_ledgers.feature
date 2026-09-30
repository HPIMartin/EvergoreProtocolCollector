Feature: A member's bank and storage ledgers
  Every member has two ledgers: the gold they moved through the guild bank and the goods they moved
  through the guild storage, newest first. Each movement shows its time, member, amount or quantity,
  item and quality as the collection read them from the game's protocol (see "Reading the entries
  of the game's protocols"); its direction reads "Einlagerung" or "Entnahme" in the storage ledger.
  The bank ledger names a deposit "Einzahlung", as the game does. A member can check their own row
  of the overview against the two ledgers. A ledger that "shows exactly" some movements holds these
  and no others, newest first; movements of the same minute may stand in either order.

  Scenario: The bank ledger lists a member's gold movements newest first, in the game's words
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
      | 02.01.2026 12:00 | Aurora | 500    | Einzahlung |
      | 03.01.2026 12:00 | Aurora | 200    | Entnahme   |
      | 04.01.2026 12:00 | Boreas | 750    | Einzahlung |
    When a member opens the bank ledger of "Aurora"
    Then the page is headed "Bank von Aurora"
    And the bank ledger of "Aurora" shows exactly:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 03.01.2026 12:00 | Aurora | 200    | Entnahme   |
      | 02.01.2026 12:00 | Aurora | 500    | Einzahlung |
      | 01.01.2026 12:00 | Aurora | 1.000  | Einzahlung |
    And the ledger's caption reads "3 von 3 Einträgen"

  Scenario: The storage ledger lists a member's item movements newest first
    Given the guild storage ledger holds:
      | Zeitpunkt        | Avatar | Menge | Gegenstand          | Qualität | Vorgang     |
      | 01.01.2026 12:00 | Aurora | 10    | Kupfererz           | 100      | Einlagerung |
      | 02.01.2026 12:00 | Aurora | 2     | Magische Ätherbinde | 50       | Einlagerung |
      | 03.01.2026 12:00 | Aurora | 1     | Kristall            | 100      | Entnahme    |
    When a member opens the storage ledger of "Aurora"
    Then the page is headed "Lager von Aurora"
    And the storage ledger of "Aurora" shows exactly:
      | Zeitpunkt        | Avatar | Menge | Gegenstand          | Qualität | Vorgang     |
      | 03.01.2026 12:00 | Aurora | 1     | Kristall            | 100      | Entnahme    |
      | 02.01.2026 12:00 | Aurora | 2     | Magische Ätherbinde | 50       | Einlagerung |
      | 01.01.2026 12:00 | Aurora | 10    | Kupfererz           | 100      | Einlagerung |
    And the ledger offers neither "Zurück" nor "Weiter"

  Rule: A long ledger is shown a hundred movements a page, counted from page 1

    Background:
      Given Aurora has 250 movements in the guild storage ledger

    Scenario: A long ledger is shown a hundred entries at a time
      When a member opens the storage ledger of "Aurora"
      Then the ledger shows her 100 newest movements
      And the ledger's caption reads "100 von 250 Einträgen"
      And the ledger offers "Weiter" but not "Zurück"

    Scenario: The last page of a long ledger shows what is left
      When a member opens page 3 of the storage ledger of "Aurora"
      Then the ledger shows her 50 oldest movements
      And the ledger's caption reads "50 von 250 Einträgen"
      And the ledger offers "Zurück" but not "Weiter"

    Scenario: A page of a ledger can be bookmarked
      Given a member has followed "Weiter" in the storage ledger of "Aurora"
      When the member later opens their bookmark of page 2 of the storage ledger of "Aurora"
      Then the ledger shows her movements 101 to 200, counted from the newest

    Scenario: A page past the end of a ledger is an empty page, not a failure
      When a member opens page 4 of the storage ledger of "Aurora"
      Then the ledger says "Für Aurora ist hier kein Vorgang gespeichert."
      And the ledger's caption reads "0 von 250 Einträgen"
      And the ledger offers "Zurück" but not "Weiter"

  Scenario: A member who never used one of the ledgers has an empty ledger there
    Given the guild storage ledger holds:
      | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
      | 01.01.2026 12:00 | Brynja | 4     | Eisenbarren | 100      | Einlagerung |
    When a member opens the bank ledger of "Brynja"
    Then the ledger says "Für Brynja ist hier kein Vorgang gespeichert."
    And the ledger's caption reads "0 von 0 Einträgen"

  Scenario Outline: A name the guild ledgers do not know is not a member, in the <ledger> ledger
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
    When a member opens the <ledger> ledger of "Nobody"
    Then the page says "Kein Avatar mit dem Namen Nobody."

    Examples:
      | ledger  |
      | bank    |
      | storage |

  Scenario Outline: A bookmark of page <page>, <why>, says in plain German that the page does not exist
    A ledger's pages count from 1. A bookmark names a page no ledger can have only when someone has
    edited its address by hand. A page number past the end is another case: it shows an empty page
    (see "A page past the end of a ledger is an empty page, not a failure").

    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
    When a member opens a bookmark of page <page> of the bank ledger of "Aurora"
    Then the page says "Diese Seite gibt es nicht."

    Examples:
      | page | why                         |
      | 0    | a page before the first one |
      | zwei | a page number in letters    |
