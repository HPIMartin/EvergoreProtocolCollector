Feature: Explaining the figures
  The explanation page, see "The explanation page", works every figure of the overview out in an
  example of its invented members moving real items of the game, so a member can follow how the
  figure comes about instead of trusting it. An example about a figure works it out in calculations,
  each one line that opens with what it works out, then a colon, terms joined by "×", "+" or "−", an
  equals sign and a result, the terms giving the result the same whether worked out from left to
  right or with "×" before "+" and "−"; the minus between two terms is written as the long minus
  sign "−", while a figure below zero carries the short minus sign the overview writes, "-"; amounts
  are written as the overview writes them, with German digit grouping and a decimal comma, each
  followed by "Gold", and a share is followed by "%"; a term is a plain amount, a share such as "60
  %", which multiplies as its fraction, or the result of an earlier calculation of the example,
  written as that calculation's exact result; a term below zero stands in parentheses. A line opens
  either with a movement or with a figure. A movement names the member, then "legt … ein" for a
  deposit or "nimmt … heraus" for a withdrawal, around the quantity and the item, with "der Qualität
  n" after the item when the quality is not 100, "vom Marktstand" when the item was bought at the
  market stall, and ", zum Gildenpreis" after it when the line values it at the guild's price; such
  a line multiplies the quantity, the item's market price and the share it is credited or charged
  at, in this order, and for equipment of a quality below 100 a last term, the quality as a share,
  and ends on the gold the movement is worth; "credited in full" is the share "100 %", "credited
  nothing" the share "0 %". Numbers, "Gold", "%" and the signs between terms are separated by single
  spaces. A figure stands as the overview labels it, after the name of its row where the row
  matters, as in "Gero Gildenhändler Nach Abzügen" or "Gilde Nach Abzügen", and its line ends on
  that figure. A result that is no whole gold is written as its exact value, an arrow and its
  nearest whole gold, as in "10,8 Gold → 11 Gold". A deposit "at 0 %" is one credited nothing. Each
  example's guild is the members its example names. A "Gildenhändler" is a member who trades for the
  guild, in the examples Gero Gildenhändler; the "Marktstand" is the game's own trader, where
  crafting material and the ammunition "Pfeile", "Bolzen" and "Magieessenz" are bought. Every figure
  an example works out in calculations is the one the overview would show for the example's
  movements, under the guild's own valuation as "Valuing the guild's movements" states it. These
  scenarios check which figure each example works out and from which terms; "The explanation page"
  checks the questions and "Explaining the rules" the words of each rule, which stand before the
  example. That every figure of an example is right is checked beside these scenarios, by a test run
  with every build that works each example's calculations out under that valuation and fails on any
  figure that differs. Any figure or date an example's text names outside its calculations, and
  every example of an answer under "Die Übersicht lesen" or "Umstellen" other than those on rounding
  and on switching the last column, is the author's to judge when reading the page, like each
  answer's closing sentence on why.

  Rule: An answer about a figure works its example out to that figure

    Scenario: Every calculation of the page is written as the overview writes figures and adds up
      When a member opens the explanation page
      Then every calculation of the page writes an amount with "Gold" after it in German digit grouping and a share with "%" after it, and adds up whether worked out from left to right or with "×" before "+" and "−"
      And every calculation of the page writes a result that is no whole gold with an arrow and its nearest whole gold after it, and any other result without an arrow
      And every calculation of the page writes a term below zero in parentheses, with the short minus sign, and no other term in parentheses
      And every calculation of the page opens with a movement or with one of these figures, alone or after "Gilde" or the name of an invented member:
        | figure                |
        | Bank-Einzahlung       |
        | Bank-Auszahlung       |
        | Einlagerung           |
        | Entnahme              |
        | Nach Abzügen          |
        | Vor Abzügen           |
        | Gildenbank            |
        | Gildenlagerwert       |
        | Gildenspende          |
        | Handwerkssubventionen |

    Scenario Outline: The example of "<question>" works out a "<figure>" figure from what it names
      An amount the example names is one its text before the calculations writes out on its own,
      not only as the digits of a larger amount.

      When a member opens the explanation page
      Then the example of "<question>" works out a calculation of at least two terms that ends on a "<figure>" figure, each term a share, an amount the example names or the result of another of its calculations

      Examples:
        | question                          | figure          |
        | Was zählt Gold in der Gildenbank? | Bank-Einzahlung |
        | Was zählt Gold in der Gildenbank? | Bank-Auszahlung |
        | Was bringt mir eine Einlagerung?  | Einlagerung     |
        | Und wenn ich etwas herausnehme?   | Entnahme        |
        | Was ist die „Gildenbank“?         | Gildenbank      |
        | Was ist der „Gildenlagerwert“?    | Gildenlagerwert |

    Scenario: The example of "Was steht unter „Nach Abzügen“?" works Mira Mitglied's figure out from the gold and the goods she moved
      The gold she paid in is the amount the example writes in "zahlt … ein", the gold she took out
      the amount it writes in "hebt … ab".

      When a member opens the explanation page
      Then the example of "Was steht unter „Nach Abzügen“?" works out, each in a calculation of its own, a deposit of goods and a withdrawal of goods by Mira Mitglied
      And the example of "Was steht unter „Nach Abzügen“?" works out a calculation that ends on the "Nach Abzügen" figure of Mira Mitglied, whose terms are the gold she paid in and the result of the calculation of her deposit of goods, both added, and the gold she took out and the result of the calculation of her withdrawal of goods, both taken off

    Scenario: The example of "Was zeigt „Vor Abzügen“?" counts goods bought at the market stall at the guild's price
      When a member opens the explanation page
      Then the example of "Was zeigt „Vor Abzügen“?" works out, in a calculation of its own, Mira Mitglied's deposit of goods bought at the market stall, at the guild's price
      And the example of "Was zeigt „Vor Abzügen“?" works out a calculation of at least two terms that ends on the "Vor Abzügen" figure of Mira Mitglied, one term the result of the calculation of her deposit bought at the market stall at the guild's price, each term a share, an amount the example names or the result of another of its calculations

    Scenario: The example of switching the last column works out one member's figure after and before the deductions
      When a member opens the explanation page
      Then the example of "Kann ich die letzte Spalte umstellen?" works out a calculation of at least two terms that ends on the "Nach Abzügen" figure of a member and one that ends on the "Vor Abzügen" figure of the same member, which differs from it, each term of both a share, an amount the example names or the result of another of its calculations

    Scenario: The deposit example works out a deposit at each share of its market value and one of equipment below quality 100
      When a member opens the explanation page
      Then the example of "Was bringt mir eine Einlagerung?" works out these six deposits, each in a different calculation of its own:
        | deposit                                                                                     |
        | credited at 0 % of its market value                                                         |
        | goods other than ammunition credited at 60 % of their market value, not scaled by a quality |
        | crafting material bought at the market stall                                                |
        | ammunition the market stall sells, bought there                                             |
        | ammunition the market stall does not sell, credited at 60 % of its market value             |
        | equipment of a quality below 100, credited at 60 % with its quality as the last share       |

    Scenario: The withdrawal example works out the same equipment at full and at half quality
      The same equipment is equipment of the same name and quantity.

      When a member opens the explanation page
      Then the example of "Und wenn ich etwas herausnehme?" works out, each in a calculation of its own, a withdrawal of equipment of quality 100 and one of the same equipment of quality 50, which gives half the result of the other

    Scenario: The example of the row "Gilde" adds up the members
      When a member opens the explanation page
      Then the example of "Was zeigt die Zeile „Gilde“?" works out a calculation that ends on the "Nach Abzügen" figure of the row "Gilde", whose terms are the results of the calculations that end on the "Nach Abzügen" figures of Mira Mitglied and Hanna Handwerk

    Scenario: The crafting example works out the ingredients taken out and the product put in
      Ingredients and product are those of a recipe of the game, in the quantities of its batches.

      When a member opens the explanation page
      Then the example of "Lohnt sich Handwerk für mich?" works out, each in a calculation of its own, Hanna Handwerk's withdrawals of the ingredients and her deposit of the product
      And the example of "Lohnt sich Handwerk für mich?" works out a calculation that ends on the "Nach Abzügen" figure of Hanna Handwerk, whose terms are the result of the calculation of her deposit of the product less the results of the calculations of her withdrawals of the ingredients

    Scenario Outline: The example of "<question>" works out "<figure>" from a deposit and that deposit at the guild's price
      When a member opens the explanation page
      Then the example of "<question>" works out, each in a calculation of its own, a deposit <kind> and the same deposit at the guild's price
      And the example of "<question>" works out a calculation that ends on a "<figure>" figure, whose terms are <terms>

      Examples:
        | question                          | figure                | kind                             | terms                                                                                                                                 |
        | Was ist die „Gildenspende“?       | Gildenspende          | credited below the guild's price | the result of the calculation of the deposit at the guild's price less the result of the calculation of the deposit credited below it |
        | Was sind „Handwerkssubventionen“? | Handwerkssubventionen | credited above the guild's price | the result of the calculation of the deposit credited above it less the result of the calculation of the deposit at the guild's price |

    Scenario: The rounding example shows a figure rounded and a "Nach Abzügen" 1 Gold off the sum of its shown figures
      When a member opens the explanation page
      Then the example of "Warum passt eine Summe manchmal um 1 Gold nicht?" works out a calculation that ends on a "Nach Abzügen" figure, has a term that is the exact result of another calculation which shows that result with an arrow and its nearest whole gold, and whose figure is 1 Gold more or less than its terms give when each term that is the result of another calculation is taken as the whole gold that calculation shows

    Scenario: The example of "Was kann das Protokoll nicht zeigen?" works Gero Gildenhändler's figure out from his two movements only
      The gold he keeps for the next round stands in the example's text, in no movement, so it is
      no term of his figure. His deposit is the crafting material he bought at the market stall. A
      figure is above zero when it is a number that is not 0 and does not carry the minus sign the
      overview writes.

      When a member opens the explanation page
      Then the example of "Was kann das Protokoll nicht zeigen?" works out, in a calculation of its own, a withdrawal of goods from the guild storage by Gero Gildenhändler
      And the example of "Was kann das Protokoll nicht zeigen?" works out a calculation that ends on the "Nach Abzügen" figure of Gero Gildenhändler, above zero, whose terms are the result of the calculation of his deposit credited in full less the result of the calculation of his withdrawal of goods from the guild storage

  Rule: Gero Gildenhändler's example explains why a guild trader's "Vor Abzügen" is below zero

    Scenario: Gero Gildenhändler's example ends above zero after the deductions and below zero before them
      A figure is below zero when it carries the minus sign the overview writes, and above zero when
      it is a number that is not 0 and does not carry that sign.

      When a member opens the explanation page
      Then the example of "Warum steht beim Gildenhändler „Vor Abzügen“ im Minus?" works out, in a calculation of its own, a withdrawal of goods from the guild storage by Gero Gildenhändler
      And the example of "Warum steht beim Gildenhändler „Vor Abzügen“ im Minus?" works out, each in a calculation of its own, a deposit of crafting material bought at the market stall by Gero Gildenhändler, credited in full, and the same deposit at the guild's price
      And the example of "Warum steht beim Gildenhändler „Vor Abzügen“ im Minus?" works out a calculation that ends on the "Nach Abzügen" figure of Gero Gildenhändler, above zero, whose terms are the result of the calculation of his deposit credited in full less the result of the calculation of his withdrawal of goods from the guild storage
      And the example of "Warum steht beim Gildenhändler „Vor Abzügen“ im Minus?" works out a calculation that ends on the "Vor Abzügen" figure of Gero Gildenhändler, below zero, whose terms are the result of the calculation of his deposit at the guild's price less the result of the calculation of his withdrawal of goods from the guild storage
