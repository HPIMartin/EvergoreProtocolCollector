Feature: The guild's position
  Above the member table the overview states where the guild stands, in four figures kept side by
  side so that the header does not add gold, counted at face value, to goods valued at the guild's
  price: the gold paid into the guild bank less the gold taken out since the first protocol
  ("Gildenbank"), the goods deposited into the guild storage less those withdrawn since then, at the
  guild's price ("Gildenlagerwert"), neither of them a counted balance, what members
  deposited that the guild credited below its own price ("Gildenspende"), and what the guild
  credited above its own price when members deposited goods bought at the market stall, or
  ammunition the market stall sells ("Handwerkssubventionen"). The guild's own price for goods
  is 60 % of their market value. The table's last column can switch between a member's figure after
  the guild's share ("Nach Abzügen") and before it ("Vor Abzügen").

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1500   | Einzahlung |
      | 02.01.2026 12:00 | Aurora | 200    | Entnahme   |
    And the guild storage ledger holds:
      | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
      | 03.01.2026 12:00 | Aurora | 10    | Kupfererz   | 100      | Einlagerung |
      | 04.01.2026 12:00 | Aurora | 10    | Federn      | 100      | Einlagerung |
      | 05.01.2026 12:00 | Aurora | 10    | Eisenbarren | 100      | Einlagerung |
      | 06.01.2026 12:00 | Aurora | 5     | Eisenbarren | 100      | Entnahme    |
    And the daily collection has run

  Scenario: The header states the guild's position in four figures that reconcile with the guild row
    The guild row's "Nach Abzügen" is the bank plus the storage value, less what was given for
    nothing, plus what the guild paid above its price: 1.300 + 630 - 120 + 100 = 1.910.

    When a member opens the overview
    Then the guild's position reads:
      | Gildenbank | Gildenlagerwert | Gildenspende | Handwerkssubventionen |
      | 1.300      | 630             | 120          | 100                   |
    And the guild row shows:
      | Avatar | Bank-Einzahlung | Bank-Auszahlung | Einlagerung | Entnahme | Nach Abzügen |
      | Gilde  | 1.500           | 200             | 970         | 360      | 1.910        |

  Rule: The last column switches between the figure after and before the guild's share

    Scenario: The last column switches to what a member moved before the guild's share
      "Vor Abzügen" adds back what the member gave for nothing and takes off what the guild paid
      above its price: 1.910 + 120 - 100 = 1.930.

      Given a member has opened the overview
      When the member switches the last column to "Vor Abzügen"
      Then the overview shows:
        | Avatar | Vor Abzügen |
        | Aurora | 1.930       |
      And the guild row shows:
        | Avatar | Vor Abzügen |
        | Gilde  | 1.930       |

    Scenario Outline: The overview comes back on the figure after the guild's share when the member <comes back>
      Given a member has switched the overview's last column to "Vor Abzügen"
      When the member <comes back>
      Then the last column reads "Nach Abzügen"

      Examples:
        | comes back                                                |
        | reloads the overview                                      |
        | follows "Übersicht" back from the bank ledger of "Aurora" |

  Rule: While a member's figures are not computed yet, the guild's position says so, and "Vor Abzügen" gives no reason of its own
    Boreas's movement was collected after the last recompute, which therefore never reached him.
    His row's mark, the guild row's mark and the dashes in his row while the last column reads "Nach
    Abzügen" stand under the rule "A member no run has computed yet is shown as not yet computed";
    what the switched last column shows, and that both marks stay after the switch, stands here.

    Background:
      Given the guild bank ledger also holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 07.01.2026 12:00 | Boreas | 750    | Einzahlung |

    Scenario: While a member is not yet computed, the guild's position says so
      When a member opens the overview
      Then the guild's position reads:
        | Gildenbank            | Gildenlagerwert       | Gildenspende          | Handwerkssubventionen |
        | Noch nicht berechnet. | Noch nicht berechnet. | Noch nicht berechnet. | Noch nicht berechnet. |

    Scenario: A row not yet computed, and the guild row holding it, show a dash under "Vor Abzügen" and keep their marks as the only reason
      A note naming "Gildenspende" and "Handwerkssubventionen" as not yet computed would blame two
      figures while the whole row is not computed. A mark is the "!" a row carries, which shows its
      text when the member points at it; a note would be such a "!" in the cell itself. Boreas's row
      is marked "Noch nicht berechnet." and the guild row "Enthält mindestens eine Zeile, die noch
      nicht berechnet ist."; those marks give the reason. No note means the cell holds the dash and
      nothing else, neither written beside it nor shown when the member points at the cell; the
      marks stay while the last column reads "Vor Abzügen", and a computed row keeps its figure.
      Aurora's 1.930 is her 1.300 in gold plus 990 in goods at the guild's price less the 360 she
      withdrew; Boreas's deposit does not change it.

      Given a member has opened the overview
      When the member switches the last column to "Vor Abzügen"
      Then the overview shows:
        | Avatar | Vor Abzügen |
        | Aurora | 1.930       |
        | Boreas | –           |
      And the guild row shows:
        | Avatar | Vor Abzügen |
        | Gilde  | –           |
      And Boreas's "Vor Abzügen" carries no note
      And the guild row's "Vor Abzügen" carries no note
      And Boreas's row is marked "Noch nicht berechnet."
      And the guild row is marked "Enthält mindestens eine Zeile, die noch nicht berechnet ist."
