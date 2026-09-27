@wip
Feature: The guild's position
  Above the member table the overview states where the guild stands, in four figures kept side by
  side so that the header does not add measured gold to modelled goods: the gold in the guild bank
  ("Gildenbank"), the value of the goods in the guild storage ("Gildenlagerwert"), what members
  deposited that the guild credited below its own price ("Gildenspende"), and what the guild
  credited above its own price when members deposited goods they had bought from the guild trader
  ("Handwerkssubventionen"). The guild's own price for goods is 60 % of their market value. The
  table's last column can switch between a member's figure after the guild's share
  ("Nach Abzügen") and before it ("Vor Abzügen").

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 10.01.2024 10:00 | Aurora | 1500   | Einzahlung |
      | 12.01.2024 12:00 | Aurora | 200    | Entnahme   |
    And the guild storage ledger holds:
      | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
      | 15.01.2024 10:00 | Aurora | 10    | Kupfererz   | 100      | Einlagerung |
      | 15.01.2024 10:05 | Aurora | 10    | Federn      | 100      | Einlagerung |
      | 15.01.2024 10:10 | Aurora | 10    | Eisenbarren | 100      | Einlagerung |
      | 17.01.2024 12:00 | Aurora | 5     | Eisenbarren | 100      | Entnahme    |

  Scenario: The header states the guild's position in four figures that reconcile with the guild row
    The guild row's "Nach Abzügen" is the bank plus the storage value, less what was given for
    nothing, plus what the guild paid above its price: 1.300 + 630 - 120 + 100 = 1.910.

    Given the daily collection has run
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

      Given the daily collection has run
      And a member has opened the overview
      When the member switches the last column to "Vor Abzügen"
      Then the overview shows:
        | Avatar | Vor Abzügen |
        | Aurora | 1.930       |
      And the guild row shows:
        | Avatar | Vor Abzügen |
        | Gilde  | 1.930       |

    Scenario Outline: The overview comes back on the figure after the guild's share when the member <comes back>
      Given the daily collection has run
      And a member has switched the overview's last column to "Vor Abzügen"
      When the member <comes back>
      Then the last column reads "Nach Abzügen"

      Examples:
        | comes back                                                |
        | reloads the overview                                      |
        | follows "Übersicht" back from the bank ledger of "Aurora" |

  Rule: While a member's figures are not computed yet, the figures that need them say so

    @wip
    Scenario: A member no recompute has reached yet is shown as not yet computed
      Boreas's movement was collected after the last recompute, which therefore never reached him.
      Today his row shows zeros without a mark, and "Gildenbank" leaves his gold out without a note
      (1.300). This states the corrected behavior.

      Given the daily collection has run
      And the guild bank ledger also holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 20.01.2024 09:00 | Boreas | 750    | Einzahlung |
      When a member opens the overview
      Then the guild's position reads:
        | Gildenbank            | Gildenlagerwert       | Gildenspende          | Handwerkssubventionen |
        | Noch nicht berechnet. | Noch nicht berechnet. | Noch nicht berechnet. | Noch nicht berechnet. |
      And the overview shows:
        | Avatar | Bank-Einzahlung | Bank-Auszahlung | Einlagerung | Entnahme | Nach Abzügen | Letzte Bankaktivität |
        | Boreas | –               | –               | –           | –        | –            | 20.01.2024 09:00     |
      And Boreas's row is marked "Noch nicht berechnet."
      And the guild row is marked "Enthält mindestens eine Zeile, die noch nicht berechnet ist."

    Scenario: Before the guild's share is known, "Vor Abzügen" says why it has no figure
      Boreas's movement was collected after the last recompute, which therefore never reached him.

      Given the daily collection has run
      And the guild bank ledger also holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 20.01.2024 09:00 | Boreas | 750    | Einzahlung |
      And a member has opened the overview
      When the member switches the last column to "Vor Abzügen"
      Then Boreas's "Vor Abzügen" shows no figure, noted "Kein Saldo: für diese Zeile sind Gildenspende und Handwerkssubventionen noch nicht berechnet."
      And the guild row's "Vor Abzügen" shows no figure, noted "Kein Saldo: für diese Zeile sind Gildenspende und Handwerkssubventionen noch nicht berechnet."
      And Aurora's "Vor Abzügen" is 1.930
