@wip
Feature: The health report: what the last recompute found
  Beside its verdict the health report names what the last successful recompute found that the
  operator needs to act on: item names the price list does not know, items known to be worth
  nothing, members whose figures could not be refreshed, suspected round trips and the pairs it
  could not judge. Each finding is counted and listed; a finding with nothing in it is left out.

  Rule: The report names what the last successful recompute found

    Scenario: The health report names unknown items apart from items known to be worth nothing
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand                | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 1     | Unobtainium               | 100      | Einlagerung |
        | 15.01.2024 11:00 | Aurora | 1     | Übungsstück-Kupferschwert | 100      | Einlagerung |
      And the daily collection has run
      When the operator asks the service for its health
      Then the health report names "Unobtainium" as an unknown item
      And the health report names "Übungsstück-Kupferschwert" as an item worth nothing

    Scenario: The health report names the members whose figures could not be refreshed
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 10    | Eisenbarren | 100      | Einlagerung |
        | 15.01.2024 11:00 | Boreas | 10    | Eisenbarren | 100      | Einlagerung |
      And Aurora's stored movements cannot be read
      And the daily collection has run
      When the operator asks the service for its health
      Then the service reports its health as "UP"
      And the health report names "Aurora" as a member whose figures could not be refreshed

    Scenario: The health report describes the suspected round trips and the pairs it could not judge
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand     | Qualität | Vorgang     |
        | 01.01.2026 00:00 | Alrik  | 100   | Kristallat     | 100      | Entnahme    |
        | 01.01.2026 01:00 | Alrik  | 1     | Achat-Armbrust | 100      | Einlagerung |
        | 01.01.2026 02:00 | Alrik  | 100   | Kristallat     | 100      | Einlagerung |
        | 01.01.2026 03:00 | Alrik  | 50    | Federn         | 100      | Entnahme    |
        | 01.01.2026 04:00 | Alrik  | 50    | Federn         | 100      | Einlagerung |
      And the daily collection has run
      When the operator asks the service for its health
      Then the health report describes the round trip "Alrik: 50 × Federn"
      And the health report describes the pair it could not judge as "Alrik: Kristallat"

  Rule: A finding with nothing in it is left out

    Scenario: After a recompute that found nothing, the health report names no finding
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 10    | Eisenbarren | 100      | Einlagerung |
      And the daily collection has run
      When the operator asks the service for its health
      Then the health report names no unknown item
      And the health report names no item worth nothing
      And the health report names no member whose figures could not be refreshed
      And the health report describes no round trip
      And the health report describes no pair it could not judge

  Rule: A count says how many different names its list holds
    Today a count says how often a name occurred in the ledgers, so it can be larger than its list.
    The two scenarios below state the corrected behavior.

    @wip
    Scenario: An unknown item that occurs twice is counted once
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 1     | Unobtainium | 100      | Einlagerung |
        | 16.01.2024 10:00 | Boreas | 1     | Unobtainium | 100      | Entnahme    |
      And the daily collection has run
      When the operator asks the service for its health
      Then the health report counts 1 unknown item

    @wip
    Scenario: An item worth nothing that occurs twice is counted once
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand                | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 1     | Übungsstück-Kupferschwert | 100      | Einlagerung |
        | 16.01.2024 10:00 | Aurora | 1     | Übungsstück-Kupferschwert | 100      | Entnahme    |
      And the daily collection has run
      When the operator asks the service for its health
      Then the health report counts 1 item worth nothing

  Rule: The lists name their entries in German alphabetical order
    Today the lists are sorted letter by letter with umlauts after "z", so "Ärger" follows "Zorn".
    The three scenarios below state the corrected behavior; the round-trip lists are in "How a
    suspected round trip is reported".

    @wip
    Scenario: The health report lists unknown item names in German alphabetical order
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 1     | Zunder     | 100      | Einlagerung |
        | 15.01.2024 11:00 | Aurora | 1     | Äxtchen    | 100      | Einlagerung |
      And the daily collection has run
      When the operator asks the service for its health
      Then the health report lists the unknown items in this order: "Äxtchen", "Zunder"

    @wip
    Scenario: The health report lists the members whose figures could not be refreshed in German alphabetical order
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Zorn   | 10    | Eisenbarren | 100      | Einlagerung |
        | 15.01.2024 11:00 | Ärger  | 10    | Eisenbarren | 100      | Einlagerung |
        | 15.01.2024 12:00 | Boreas | 10    | Eisenbarren | 100      | Einlagerung |
      And Zorn's stored movements cannot be read
      And Ärger's stored movements cannot be read
      And the daily collection has run
      When the operator asks the service for its health
      Then the health report lists the members whose figures could not be refreshed in this order: "Ärger", "Zorn"

    @wip
    Scenario: The health report lists the items worth nothing in German alphabetical order
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand               | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 1     | Übungsstück-Wollrüstung  | 100      | Einlagerung |
        | 15.01.2024 11:00 | Aurora | 1     | Übungsstück-Ätherrüstung | 100      | Einlagerung |
      And the daily collection has run
      When the operator asks the service for its health
      Then the health report lists the items worth nothing in this order: "Übungsstück-Ätherrüstung", "Übungsstück-Wollrüstung"
