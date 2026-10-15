Feature: The explanation page
  The overview leads to a page, written by the guild leadership to the guild, that answers the
  questions a member asks about the overview. It introduces its invented members, Mira Mitglied,
  Hanna Handwerk and Gero Gildenhändler, and the Marktstand, the game's own trader, before its first
  question. A group stands under its heading and a question under its own; the answer below a
  question gives its rule, then one example, which opens with "Beispiel:" and names at least one of
  the invented members, then a sentence on why. The page reads nothing of the guild: what it shows
  is the same for every member, and it opens even when the figures cannot be loaded. The words of
  the page are the ones the author approved, kept word for word by a test beside these scenarios;
  every figure of its examples is recomputed through the guild's valuation by a test run with every
  build.

  Rule: The page answers the questions a member asks, in five groups

    Scenario: The page asks exactly these questions in five groups, in this order
      When a member opens the explanation page
      Then the page asks exactly these questions, in this order, under these groups:
        | group               | question                                               |
        | Was zählt           | Was zählt Gold in der Gildenbank?                      |
        | Was zählt           | Was bringt mir eine Einlagerung?                       |
        | Was zählt           | Und wenn ich etwas herausnehme?                        |
        | Was zählt           | Lohnt sich Handwerk für mich?                          |
        | Eure Zahl           | Was steht unter „Nach Abzügen“?                        |
        | Eure Zahl           | Was zeigt „Vor Abzügen“?                               |
        | Die Gilde           | Was ist die „Gildenbank“?                              |
        | Die Gilde           | Was ist der „Gildenlagerwert“?                         |
        | Die Gilde           | Was ist die „Gildenspende“?                            |
        | Die Gilde           | Was sind „Handwerkssubventionen“?                      |
        | Die Gilde           | Was zeigt die Zeile „Gilde“?                           |
        | Die Gilde           | Warum steht beim Gildenhändler „Vor Abzügen“ im Minus? |
        | Die Gilde           | Was kann das Protokoll nicht zeigen?                   |
        | Die Übersicht lesen | Warum passt eine Summe manchmal nicht ganz?            |
        | Die Übersicht lesen | Was bedeuten die Farben, „–“ und „!“?                  |
        | Die Übersicht lesen | Was heißt „Noch nicht berechnet.“?                     |
        | Die Übersicht lesen | Was heißt „Veraltete Zahlen“?                          |
        | Die Übersicht lesen | Wer steht unter „Aktiv“, wer unter „Ruhend“?           |
        | Umstellen           | Kann ich die letzte Spalte umstellen?                  |
        | Umstellen           | Wie sortiere ich die Übersicht?                        |
        | Umstellen           | Wo finde ich meine eigenen Vorgänge?                   |
        | Umstellen           | Wie sortiere und blättere ich in meinen Vorgängen?     |
        | Umstellen           | Kann ich mir eine Seite merken?                        |
      And each group's title is a heading of the page, and each of its questions a heading one level below it

  Rule: The page introduces its invented members and the market stall

    Scenario: The page introduces its invented members and the market stall before its questions
      Each name and what is said of it stand in one sentence.

      When a member opens the explanation page
      Then the page says, above its first group:
        | who                | said of them                |
        | Mira Mitglied      | ein Mitglied wie ihr        |
        | Hanna Handwerk     | fertigt Waren für die Gilde |
        | Gero Gildenhändler | handelt für die Gilde       |
        | Marktstand         | der Händler des Spiels      |

  Rule: Every answer gives its rule, then one example, whether or not the figures can be loaded

    Scenario Outline: Every answer gives its rule, then one example of one of the page's invented members, <figures>
      Given <state>
      When a member opens the explanation page
      Then the page is headed "So entstehen eure Zahlen"
      And the page does not say "Die Daten konnten nicht geladen werden"
      And each of the 23 questions on the page has an answer whose rule stands before one example that opens with "Beispiel:" and names "Mira Mitglied", "Hanna Handwerk" or "Gero Gildenhändler"

      Examples:
        | figures                               | state                        |
        | when the figures can be loaded        | the daily collection has run |
        | although the figures cannot be loaded | the figures cannot be loaded |

  Rule: The page shows none of the guild's own names or figures

    Background:
      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 12:00 | Zorn   | 4711   | Einzahlung |
      And the daily collection has run

    Scenario: The page names no member of the guild's ledger and shows none of its amounts
      When a member opens the explanation page
      Then the page is headed "So entstehen eure Zahlen"
      And the page does not say "Zorn"
      And the page does not say "4.711"
      And the page does not say "4711"
