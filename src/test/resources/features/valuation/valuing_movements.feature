@wip
Feature: Valuing the guild's movements
  Gold counts at face value. Goods are valued from the price list the service ships: a withdrawal
  costs the member 60 % of the item's market value, and a deposit credits them according to the
  kind of item. Most of this is the rule the guild announced in 2020; the full credit for goods
  bought from the guild trader and the 60 % credit for boards and bars depart from that
  announcement on purpose. The price list step names the prices an example relies on; it cannot
  set a price, it states the one the service ships.

  Rule: Gold paid into and taken out of the guild bank counts at face value

    Scenario: Gold moved through the guild bank counts one to one
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 10.01.2024 10:00 | Bambor | 58410  | Einzahlung |
        | 11.01.2024 10:00 | Bambor | 1000   | Entnahme   |
      When the daily collection runs
      Then the overview shows:
        | Avatar | Bank-Einzahlung | Bank-Auszahlung | Nach Abzügen |
        | Bambor | 58.410          | 1.000           | 57.410       |

  Rule: A withdrawal costs 60 % of the item's market value, whatever kind of item it is

    Scenario Outline: Withdrawing <item> costs 60 % of its market value
      Given the price list the service ships values:
        | item   | kind   | market value   |
        | <item> | <kind> | <market value> |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang  |
        | 15.01.2024 10:00 | Aurora | 10    | <item>     | 100      | Entnahme |
      When the daily collection runs
      Then Aurora's "Entnahme" is <withdrawal>

      Examples:
        | item                | kind                   | market value | withdrawal |
        | Kupfererz           | Rohstoffe              | 20           | 120        |
        | Drachenhaut         | Jagdbeuten             | 120          | 720        |
        | Kristall            | Edelsteine             | 500          | 3.000      |
        | Federn              | Handwerksmaterial      | 25           | 150        |
        | Eisenbarren         | verarbeitete Rohstoffe | 120          | 720        |
        | Magische Ätherbinde | Bandagen               | 257          | 1.542      |

  Rule: A deposit credits the member according to the kind of item
    Raw materials ("Rohstoffe": ore, stone and wood), hunt loot and gems credit nothing: depositing
    them is the guild's tax. Goods bought from the guild trader credit their full market value,
    because the member paid for them in gold. Everything else, boards and bars included, credits
    60 %.

    Scenario Outline: Depositing <item> credits <share> of its market value
      Given the price list the service ships values:
        | item   | kind   | market value   |
        | <item> | <kind> | <market value> |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 10    | <item>     | 100      | Einlagerung |
      When the daily collection runs
      Then Aurora's "Einlagerung" is <credit>

      Examples:
        | item                | kind                   | market value | share | credit |
        | Kupfererz           | Rohstoffe              | 20           | 0 %   | 0      |
        | Drachenhaut         | Jagdbeuten             | 120          | 0 %   | 0      |
        | Kristall            | Edelsteine             | 500          | 0 %   | 0      |
        | Federn              | Handwerksmaterial      | 25           | 100 % | 250    |
        | Eisenbarren         | verarbeitete Rohstoffe | 120          | 60 %  | 720    |
        | Magische Ätherbinde | Bandagen               | 257          | 60 %  | 1.542  |

  Rule: An item's quality scales its value in proportion

    Scenario Outline: Ten "Magische Ätherbinde" of quality <quality> are worth <value> either way
      Given the price list the service ships values:
        | item                | kind     | market value |
        | Magische Ätherbinde | Bandagen | 257          |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand          | Qualität  | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 10    | Magische Ätherbinde | <quality> | Einlagerung |
        | 16.01.2024 10:00 | Aurora | 10    | Magische Ätherbinde | <quality> | Entnahme    |
      When the daily collection runs
      Then the overview shows:
        | Avatar | Einlagerung | Entnahme | Nach Abzügen |
        | Aurora | <value>     | <value>  | 0            |

      Examples:
        | quality | value |
        | 100     | 1.542 |
        | 50      | 771   |
        | 1       | 15    |

  Rule: Where a deposit's credit differs from the guild's own price, the difference is shown apart
    The guild's own price for goods is 60 % of their market value. A deposit credited below it gave
    the guild the rest for nothing ("Gildenspende"); a deposit credited above it made the guild pay
    the difference ("Handwerkssubventionen").

    Scenario Outline: Depositing ten <item> books <what the guild booked> for the guild
      Given the price list the service ships values:
        | item   | kind   | market value   |
        | <item> | <kind> | <market value> |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 10    | <item>     | 100      | Einlagerung |
      When the daily collection runs
      Then the guild's position reads:
        | Gildenspende | Handwerkssubventionen |
        | <gift>       | <subsidy>             |
      And the overview shows:
        | Avatar | Einlagerung | Nach Abzügen |
        | Aurora | <credit>    | <credit>     |

      Examples:
        | item        | kind                   | market value | what the guild booked | credit | gift | subsidy |
        | Kupfererz   | Rohstoffe              | 20           | a gift                | 0      | 120  | 0       |
        | Federn      | Handwerksmaterial      | 25           | a subsidy             | 250    | 0    | 100     |
        | Eisenbarren | verarbeitete Rohstoffe | 120          | neither               | 720    | 0    | 0       |

    Scenario: A withdrawal is neither a gift nor a subsidy
      Given the price list the service ships values:
        | item      | kind      | market value |
        | Kupfererz | Rohstoffe | 20           |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang  |
        | 15.01.2024 10:00 | Aurora | 10    | Kupfererz  | 100      | Entnahme |
      When the daily collection runs
      Then the guild's position reads:
        | Gildenspende | Handwerkssubventionen |
        | 0            | 0                     |

  Rule: Crafting earns the margin between the ingredients and the product

    Scenario: The guild's announced example: arrows crafted from beech and feathers
      Given the price list the service ships values:
        | item       | kind              | market value |
        | Buchenholz | Rohstoffe         | 20           |
        | Federn     | Handwerksmaterial | 25           |
        | Pfeile     | Munition Bögen    | 3            |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 6     | Buchenholz | 100      | Entnahme    |
        | 15.01.2024 10:00 | Aurora | 5     | Federn     | 100      | Entnahme    |
        | 15.01.2024 12:00 | Aurora | 135   | Pfeile     | 100      | Einlagerung |
      When the daily collection runs
      Then the overview shows:
        | Avatar | Einlagerung | Entnahme | Nach Abzügen |
        | Aurora | 243         | 147      | 96           |

    Scenario: Smelting iron bars from ore and coal
      Given the price list the service ships values:
        | item        | kind                   | market value |
        | Eisenerz    | Rohstoffe              | 40           |
        | Steinkohle  | Handwerksmaterial      | 100          |
        | Eisenbarren | verarbeitete Rohstoffe | 120          |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 5     | Eisenerz    | 100      | Entnahme    |
        | 15.01.2024 10:00 | Aurora | 2     | Steinkohle  | 100      | Entnahme    |
        | 15.01.2024 12:00 | Aurora | 5     | Eisenbarren | 100      | Einlagerung |
      When the daily collection runs
      Then the overview shows:
        | Avatar | Einlagerung | Entnahme | Nach Abzügen |
        | Aurora | 360         | 240      | 120          |

    Scenario: Goods credited at 60 % are neutral when they go out and come back unchanged
      Given the price list the service ships values:
        | item        | kind                   | market value |
        | Eisenbarren | verarbeitete Rohstoffe | 120          |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 15.01.2024 10:00 | Aurora | 10    | Eisenbarren | 100      | Entnahme    |
        | 18.01.2024 10:00 | Aurora | 10    | Eisenbarren | 100      | Einlagerung |
      When the daily collection runs
      Then Aurora's "Nach Abzügen" is 0
