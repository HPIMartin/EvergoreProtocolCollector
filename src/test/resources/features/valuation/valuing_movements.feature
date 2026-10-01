Feature: Valuing the guild's movements
  Gold counts at face value. Goods are valued from the price list the service ships: a withdrawal
  costs the member 60 % of the item's market value, and a deposit credits them according to the kind
  of item, and for ammunition according to whether the game's own trader sells it. Most of this is
  the rule the guild announced in 2020; the full credit for goods bought from the guild trader and
  for the ammunition the game's own trader sells, and the 60 % credit for boards and bars depart
  from that announcement on purpose. The price list step names the prices an example relies on; it
  cannot set a price, it states the one the service ships.

  Rule: Gold paid into and taken out of the guild bank counts at face value

    Scenario: Gold moved through the guild bank counts one to one
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Bambor | 1000   | Einzahlung |
        | 02.01.2026 12:00 | Bambor | 100    | Entnahme   |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Bank-Einzahlung | Bank-Auszahlung | Nach Abzügen |
        | Bambor | 1.000           | 100             | 900          |

  Rule: A withdrawal costs 60 % of the item's market value, whatever kind of item it is

    Scenario Outline: Withdrawing <item> costs 60 % of its market value
      Given the price list the service ships values:
        | item   | kind   | market value   |
        | <item> | <kind> | <market value> |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge      | Gegenstand | Qualität | Vorgang  |
        | 01.01.2026 12:00 | Aurora | <quantity> | <item>     | 100      | Entnahme |
      And the daily collection has run
      When a member opens the overview
      Then Aurora's "Entnahme" is <withdrawal>

      Examples:
        | item               | kind                   | market value | quantity | withdrawal |
        | Kupfererz          | Rohstoffe              | 20           | 1        | 12         |
        | Drachenhaut        | Jagdbeuten             | 120          | 1        | 72         |
        | Kristall           | Edelsteine             | 500          | 1        | 300        |
        | Federn             | Handwerksmaterial      | 25           | 1        | 15         |
        | Eisenbarren        | verarbeitete Rohstoffe | 120          | 1        | 72         |
        | Gute Baumwollbinde | Bandagen               | 65           | 1        | 39         |
        | Kriegspfeile       | Munition Bögen         | 7            | 5        | 21         |
        | Götterstich        | Munition Bögen         | 9            | 5        | 27         |

  Rule: A deposit credits the member according to the kind of item, ammunition according to whether the game's own trader sells it
    Raw materials ("Rohstoffe": ore, stone and wood), hunt loot and gems credit nothing: depositing
    them is the guild's tax. Goods bought from the guild trader credit their full market value,
    because the member paid for them in gold. So do "Pfeile", "Bolzen" and "Magieessenz": the
    game's own trader sells them at their market value, and the ledger cannot tell a bought one
    from a crafted one, so a crafted one credits in full too. Everything else, boards, bars and
    every other ammunition included, credits 60 %. The guild's 2020 announcement credits all
    ammunition at 60 %. "Jagdpfeile" are worth 5 and "Pfeile" 3, so at 60 % the one credits what
    the other credits in full; "Jagdbolzen" (20) and "Bolzen" (12) likewise.

    Scenario Outline: Depositing <item> credits <share> of its market value
      Given the price list the service ships values:
        | item   | kind   | market value   |
        | <item> | <kind> | <market value> |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | <item>     | 100      | Einlagerung |
      And the daily collection has run
      When a member opens the overview
      Then Aurora's "Einlagerung" is <credit>

      Examples:
        | item               | kind                   | market value | share | credit |
        | Kupfererz          | Rohstoffe              | 20           | 0 %   | 0      |
        | Drachenhaut        | Jagdbeuten             | 120          | 0 %   | 0      |
        | Kristall           | Edelsteine             | 500          | 0 %   | 0      |
        | Federn             | Handwerksmaterial      | 25           | 100 % | 25     |
        | Eisenbarren        | verarbeitete Rohstoffe | 120          | 60 %  | 72     |
        | Gute Baumwollbinde | Bandagen               | 65           | 60 %  | 39     |

      Examples: ammunition the game's own trader sells
        | item        | kind                | market value | share | credit |
        | Pfeile      | Munition Bögen      | 3            | 100 % | 3      |
        | Bolzen      | Munition Armbrüste  | 12           | 100 % | 12     |
        | Magieessenz | Munition Magiestäbe | 4            | 100 % | 4      |

      Examples: ammunition that can only be crafted
        | item          | kind                | market value | share | credit |
        | Jagdpfeile    | Munition Bögen      | 5            | 60 %  | 3      |
        | Jagdbolzen    | Munition Armbrüste  | 20           | 60 %  | 12     |
        | Sternenessenz | Munition Magiestäbe | 15           | 60 %  | 9      |

  Rule: An item's quality scales its value in proportion

    Scenario Outline: One "Kurzbogen" of quality <quality> is worth <value> either way
      Given the price list the service ships values:
        | item      | kind  | market value |
        | Kurzbogen | Bögen | 2000         |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität  | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Kurzbogen  | <quality> | Einlagerung |
        | 02.01.2026 12:00 | Aurora | 1     | Kurzbogen  | <quality> | Entnahme    |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Entnahme |
        | Aurora | <value>     | <value>  |

      Examples:
        | quality | value |
        | 100     | 1.200 |
        | 50      | 600   |
        | 1       | 12    |

  Rule: Where a deposit's credit differs from the guild's own price, the difference is shown apart
    The guild's own price for goods is 60 % of their market value. A deposit credited below it gave
    the guild the rest for nothing ("Gildenspende"); a deposit credited above it made the guild pay
    the difference ("Handwerkssubventionen").

    Scenario Outline: Depositing <item> books <what the guild booked> for the guild
      Given the price list the service ships values:
        | item   | kind   | market value   |
        | <item> | <kind> | <market value> |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge      | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | <quantity> | <item>     | 100      | Einlagerung |
      And the daily collection has run
      When a member opens the overview
      Then the guild's position reads:
        | Gildenspende | Handwerkssubventionen |
        | <gift>       | <subsidy>             |
      And the overview shows:
        | Avatar | Einlagerung | Nach Abzügen |
        | Aurora | <credit>    | <credit>     |

      Examples:
        | item        | kind                   | market value | quantity | what the guild booked | credit | gift | subsidy |
        | Kupfererz   | Rohstoffe              | 20           | 1        | a gift                | 0      | 12   | 0       |
        | Federn      | Handwerksmaterial      | 25           | 1        | a subsidy             | 25     | 0    | 10      |
        | Eisenbarren | verarbeitete Rohstoffe | 120          | 1        | neither               | 72     | 0    | 0       |

      Examples: ammunition the game's own trader sells
        | item   | kind           | market value | quantity | what the guild booked | credit | gift | subsidy |
        | Pfeile | Munition Bögen | 3            | 5        | a subsidy             | 15     | 0    | 6       |

    Scenario: A withdrawal is neither a gift nor a subsidy
      Given the price list the service ships values:
        | item      | kind      | market value |
        | Kupfererz | Rohstoffe | 20           |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang  |
        | 01.01.2026 12:00 | Aurora | 1     | Kupfererz  | 100      | Entnahme |
      And the daily collection has run
      When a member opens the overview
      Then the guild's position reads:
        | Gildenspende | Handwerkssubventionen |
        | 0            | 0                     |

  Rule: Crafting earns what the product credits less what its ingredients cost

    Scenario: The guild's announced example: arrows crafted from beech and feathers
      The guild announced this example with the arrows credited at 60 %: 243 for a gain of 96.
      "Pfeile" are ammunition the game's own trader sells and credit in full.

      Given the price list the service ships values:
        | item       | kind              | market value |
        | Buchenholz | Rohstoffe         | 20           |
        | Federn     | Handwerksmaterial | 25           |
        | Pfeile     | Munition Bögen    | 3            |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 6     | Buchenholz | 100      | Entnahme    |
        | 01.01.2026 12:00 | Aurora | 5     | Federn     | 100      | Entnahme    |
        | 02.01.2026 12:00 | Aurora | 135   | Pfeile     | 100      | Einlagerung |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Entnahme | Nach Abzügen |
        | Aurora | 405         | 147      | 258          |

    Scenario: Smelting iron bars from ore and coal
      Given the price list the service ships values:
        | item        | kind                   | market value |
        | Eisenerz    | Rohstoffe              | 40           |
        | Steinkohle  | Handwerksmaterial      | 100          |
        | Eisenbarren | verarbeitete Rohstoffe | 120          |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 5     | Eisenerz    | 100      | Entnahme    |
        | 01.01.2026 12:00 | Aurora | 2     | Steinkohle  | 100      | Entnahme    |
        | 02.01.2026 12:00 | Aurora | 5     | Eisenbarren | 100      | Einlagerung |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Entnahme | Nach Abzügen |
        | Aurora | 360         | 240      | 120          |

    Scenario: Goods credited at 60 % are neutral when they go out and come back unchanged
      Given the price list the service ships values:
        | item        | kind                   | market value |
        | Eisenbarren | verarbeitete Rohstoffe | 120          |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Entnahme    |
        | 02.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
      And the daily collection has run
      When a member opens the overview
      Then Aurora's "Nach Abzügen" is 0
