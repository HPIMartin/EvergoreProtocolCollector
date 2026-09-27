@wip
Feature: Recognising items by the names the game uses
  A storage movement names its item the way the game's protocol spells it. The price list the
  service ships recognises that spelling, a magically named variant of an item, and a few fixed
  second spellings the protocol uses for one item. A name it does not recognise is worth nothing
  and is reported to the admin on the admin page, so a gap in the price list is visible instead
  of silently costing members value.

  Rule: An item is recognised only under the spelling the game uses

    Scenario: The game's spelling of a raw stone is recognised
      Given the price list the service ships values:
        | item   | kind      | market value |
        | Marmor | Rohstoffe | 120          |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang  |
        | 01.01.2026 12:00 | Aurora | 1     | Marmor     | 100      | Entnahme |
      And the daily collection has run
      When a member opens the overview
      Then Aurora's "Entnahme" is 72

    Scenario Outline: A name the game uses is not reported as unknown: "<name>"
      Given the price list the service ships values:
        | item         | kind   | market value   |
        | <plain item> | <kind> | <market value> |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang  |
        | 01.01.2026 12:00 | Aurora | 1     | <name>     | 100      | Entnahme |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page lists no unknown item

      Examples:
        | name                                | plain item        | kind               | market value |
        | Marmor                              | Marmor            | Rohstoffe          | 120          |
        | Streitaxt des Wegelagerers          | Streitaxt         | Äxte               | 1800         |
        | Barbarenaxt der Wache               | Barbarenaxt       | Äxte               | 4900         |
        | Bidenaxt des Wegelagerers [2H]      | Bidenaxt [2H]     | Äxte [2H]          | 4600         |
        | Obsidian-Pike des Wegelagerers [2H] | Obsidian-Pike     | Stangenwaffen [2H] | 82300        |
        | Rubin-Pike [2H] des Wegelagerers    | Rubin-Pike        | Stangenwaffen [2H] | 42300        |
        | Obsidian-Pike [2H]                  | Obsidian-Pike     | Stangenwaffen [2H] | 82300        |
        | Smaragd-Pike                        | Smaragd-Pike [2H] | Stangenwaffen [2H] | 22300        |

    Scenario Outline: A spelling the game does not use is not recognised: "<spelling>"
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang  |
        | 01.01.2026 12:00 | Aurora | 1     | <spelling> | 100      | Entnahme |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Unbekannte Items: <spelling>"

      Examples:
        | spelling    |
        | Marmorstein |
        | marmor      |
        | MARMOR      |

  Rule: A magically named item is worth what the plain item is worth

    Scenario Outline: "<name>" is valued as "<plain item>"
      Given the price list the service ships values:
        | item         | kind   | market value   |
        | <plain item> | <kind> | <market value> |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang  |
        | 01.01.2026 12:00 | Aurora | 1     | <name>     | 100      | Entnahme |
      And the daily collection has run
      When a member opens the overview
      Then Aurora's "Entnahme" is <withdrawal>

      Examples:
        | name                                | plain item    | kind               | market value | withdrawal |
        | Streitaxt des Wegelagerers          | Streitaxt     | Äxte               | 1800         | 1.080      |
        | Barbarenaxt der Wache               | Barbarenaxt   | Äxte               | 4900         | 2.940      |
        | Bidenaxt des Wegelagerers [2H]      | Bidenaxt [2H] | Äxte [2H]          | 4600         | 2.760      |
        | Obsidian-Pike des Wegelagerers [2H] | Obsidian-Pike | Stangenwaffen [2H] | 82300        | 49.380     |
        | Rubin-Pike [2H] des Wegelagerers    | Rubin-Pike    | Stangenwaffen [2H] | 42300        | 25.380     |

    Scenario Outline: A magical name of an unfamiliar shape is reported rather than guessed: "<name>"
      Given the price list the service ships values:
        | item      | kind | market value |
        | Streitaxt | Äxte | 1800         |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang  |
        | 01.01.2026 12:00 | Aurora | 1     | <name>     | 100      | Entnahme |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Unbekannte Items: <name>"

      Examples:
        | name                            |
        | Streitaxt des Dunklen Waldes    |
        | Streitaxt der Wache des Nordens |
        | Streitaxt des wegelagerers      |

  Rule: A second spelling the protocol uses for one item is recognised as that item

    Scenario Outline: "<spelling>" is valued as "<item>"
      Given the price list the service ships values:
        | item   | kind               | market value   |
        | <item> | Stangenwaffen [2H] | <market value> |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang  |
        | 01.01.2026 12:00 | Aurora | 1     | <spelling> | 100      | Entnahme |
      And the daily collection has run
      When a member opens the overview
      Then Aurora's "Entnahme" is <withdrawal>

      Examples:
        | spelling           | item              | market value | withdrawal |
        | Obsidian-Pike [2H] | Obsidian-Pike     | 82300        | 49.380     |
        | Smaragd-Pike       | Smaragd-Pike [2H] | 22300        | 13.380     |

    Scenario: A name is not recognised just because a two-handed item of that name exists
      Given the price list the service ships values:
        | item              | kind        | market value |
        | Kriegshammer [2H] | Keulen [2H] | 14200        |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand   | Qualität | Vorgang  |
        | 01.01.2026 12:00 | Aurora | 1     | Kriegshammer | 100      | Entnahme |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page reads "Unbekannte Items: Kriegshammer"

  Rule: A name the price list does not know is worth nothing either way

    Scenario: An unknown item neither credits nor costs anything
      Given the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | Unobtainium | 100      | Einlagerung |
        | 02.01.2026 12:00 | Aurora | 1     | Unobtainium | 100      | Entnahme    |
      And the daily collection has run
      When a member opens the overview
      Then the overview shows:
        | Avatar | Einlagerung | Entnahme | Nach Abzügen |
        | Aurora | 0           | 0        | 0            |
      And the guild's position reads:
        | Gildenspende | Handwerkssubventionen |
        | 0            | 0                     |

  Rule: Practice pieces and quest items are known and worth nothing on purpose

    Scenario Outline: "<item>" is not reported as unknown although it is worth nothing
      Given the price list the service ships values:
        | item   | kind   | market value |
        | <item> | <kind> | 0            |
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand | Qualität | Vorgang     |
        | 01.01.2026 12:00 | Aurora | 1     | <item>     | 100      | Einlagerung |
      And the daily collection has run
      When the admin opens the admin page
      Then the admin page lists no unknown item

      Examples:
        | item                      | kind           |
        | Übungsstück-Kupferschwert | Schwerter      |
        | Mystische Bandagen        | Bandagen       |
        | Mystischer Pfeil          | Munition Bögen |
