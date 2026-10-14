Feature: Explaining the rules
  Every answer on the explanation page (see "The explanation page") opens with its rule, written in
  the words a member reads on the overview, before the example "Explaining the figures" works out.
  The rule is the text between a question and the paragraph that opens with "Beispiel:", and it says
  the words when that text contains them as a member reads them, in upper and lower case as written,
  whatever spacing stands between them.

  Rule: Every answer states its rule

    Scenario Outline: The rule in the answer to "<question>" says "<words>"
      When a member opens the explanation page
      Then the rule in the answer to "<question>" says "<words>"

      Examples:
        | question                                               | words                                                                                       |
        | Was zählt Gold in der Gildenbank?                      | Gold zählt, wie es ist                                                                      |
        | Was zählt Gold in der Gildenbank?                      | Jede Einzahlung in die Gildenbank steht voll unter „Bank-Einzahlung“                        |
        | Was zählt Gold in der Gildenbank?                      | jede Auszahlung voll unter „Bank-Auszahlung“                                                |
        | Was bringt mir eine Einlagerung?                       | je nach Art der Ware                                                                        |
        | Was bringt mir eine Einlagerung?                       | Rohstoffe, Jagdbeute und Edelsteine bringen nichts                                          |
        | Was bringt mir eine Einlagerung?                       | Handwerksmaterial bringt den vollen Marktwert                                               |
        | Was bringt mir eine Einlagerung?                       | Pfeile, Bolzen und Magieessenz bringen den vollen Marktwert, auch selbst gefertigte         |
        | Was bringt mir eine Einlagerung?                       | alles andere bringt 60 % des Marktwerts                                                     |
        | Was bringt mir eine Einlagerung?                       | Ausrüstung im Verhältnis zu ihrer Qualität                                                  |
        | Und wenn ich etwas herausnehme?                        | Jede Entnahme kostet 60 % des Marktwerts                                                    |
        | Und wenn ich etwas herausnehme?                        | egal welche Ware                                                                            |
        | Und wenn ich etwas herausnehme?                        | im Verhältnis zur Qualität                                                                  |
        | Und wenn ich etwas herausnehme?                        | Qualität gibt es nur bei Ausrüstung                                                         |
        | Lohnt sich Handwerk für mich?                          | abzüglich 60 % der Zutaten                                                                  |
        | Was steht unter „Nach Abzügen“?                        | Bank-Einzahlung und Einlagerung abzüglich Bank-Auszahlung und Entnahme                      |
        | Was zeigt „Vor Abzügen“?                               | jede Einlagerung zum Gildenpreis von 60 %                                                   |
        | Was zeigt „Vor Abzügen“?                               | „Nach Abzügen“ zählt Gekauftes voll                                                         |
        | Was ist die „Gildenbank“?                              | seit dem ersten Protokoll vom 22.01.2023                                                    |
        | Was ist die „Gildenbank“?                              | kein gezählter Kontostand                                                                   |
        | Was ist die „Gildenbank“?                              | die Summe aller Bank-Einzahlungen abzüglich aller Bank-Auszahlungen                         |
        | Was ist der „Gildenlagerwert“?                         | seit dem ersten Protokoll vom 22.01.2023                                                    |
        | Was ist der „Gildenlagerwert“?                         | zum Gildenpreis von 60 %                                                                    |
        | Was ist der „Gildenlagerwert“?                         | kein gezählter Bestand                                                                      |
        | Was ist der „Gildenlagerwert“?                         | die Summe aller Einlagerungen abzüglich aller Entnahmen                                     |
        | Was ist die „Gildenspende“?                            | seit dem ersten Protokoll vom 22.01.2023                                                    |
        | Was ist die „Gildenspende“?                            | was die Gilde zu ihrem Preis von 60 % gezahlt hätte, aber nicht gutschrieb                  |
        | Was sind „Handwerkssubventionen“?                      | seit dem ersten Protokoll vom 22.01.2023                                                    |
        | Was sind „Handwerkssubventionen“?                      | über ihren Preis hinaus                                                                     |
        | Was sind „Handwerkssubventionen“?                      | für Handwerksmaterial und für Pfeile, Bolzen und Magieessenz                                |
        | Was zeigt die Zeile „Gilde“?                           | alle Mitglieder zusammen, auch die ruhenden                                                 |
        | Was zeigt die Zeile „Gilde“?                           | „!“ bei der Gilde heißt: mindestens eine Zeile ist noch nicht berechnet oder veraltet       |
        | Warum steht beim Gildenhändler „Vor Abzügen“ im Minus? | ohne die Handwerkssubventionen                                                              |
        | Warum passt eine Summe manchmal um 1 Gold nicht?       | einmal gerundet                                                                             |
        | Warum passt eine Summe manchmal um 1 Gold nicht?       | auf das nächste ganze Gold                                                                  |
        | Was bedeuten die Farben, „–“ und „!“?                  | Bank-Einzahlung und Einlagerung in Grün, der Farbe einer Gutschrift                         |
        | Was bedeuten die Farben, „–“ und „!“?                  | Bank-Auszahlung und Entnahme in Rot, der Farbe einer Belastung                              |
        | Was bedeuten die Farben, „–“ und „!“?                  | Die letzte Spalte färbt sich erst im Minus                                                  |
        | Was bedeuten die Farben, „–“ und „!“?                  | Gildenbank, Gildenlagerwert und Gildenspende färben sich nach ihrem Vorzeichen              |
        | Was bedeuten die Farben, „–“ und „!“?                  | Handwerkssubventionen nie in der Farbe einer Gutschrift                                     |
        | Was bedeuten die Farben, „–“ und „!“?                  | Eine Null bleibt ohne Farbe                                                                 |
        | Was bedeuten die Farben, „–“ und „!“?                  | „–“ steht, wo es keine Zahl oder kein Datum gibt                                            |
        | Was bedeuten die Farben, „–“ und „!“?                  | „!“ steht bei einer Zeile, die noch nicht berechnet oder veraltet ist                       |
        | Was heißt „Noch nicht berechnet.“?                     | noch nie berechnet                                                                          |
        | Was heißt „Noch nicht berechnet.“?                     | „–“                                                                                         |
        | Was heißt „Noch nicht berechnet.“?                     | solange ein Mitglied noch nicht berechnet ist, steht es auch bei den vier Zahlen der Gilde  |
        | Was heißt „Veraltete Zahlen“?                          | Stand der letzten erfolgreichen Aktualisierung                                              |
        | Was heißt „Veraltete Zahlen“?                          | Letzte erfolgreiche Aktualisierung vom                                                      |
        | Wer steht unter „Aktiv“, wer unter „Ruhend“?           | Unter „Aktiv“ steht, wer in den 30 Tagen vor dem letzten Vorgang der Gilde etwas bewegt hat |
        | Wer steht unter „Aktiv“, wer unter „Ruhend“?           | alle anderen stehen unter „Ruhend“                                                          |
        | Kann ich die letzte Spalte umstellen?                  | zwischen „Nach Abzügen“ und „Vor Abzügen“                                                   |
        | Kann ich die letzte Spalte umstellen?                  | Beim Neuladen und beim Zurückkehren zur Übersicht steht wieder „Nach Abzügen“               |
        | Wie sortiere ich die Übersicht?                        | nach jeder Spalte                                                                           |
        | Wie sortiere ich die Übersicht?                        | Ein Klick auf die Spaltenüberschrift sortiert, ein zweiter kehrt die Reihenfolge um         |
        | Wie sortiere ich die Übersicht?                        | Jede Tabelle sortiert für sich                                                              |
        | Wie sortiere ich die Übersicht?                        | Die Zeile „Gilde“ bleibt unten                                                              |
        | Wie sortiere ich die Übersicht?                        | Die Sortierung folgt dem Umschalten der letzten Spalte                                      |
        | Wo finde ich meine eigenen Vorgänge?                   | Euer Name führt zu euren Vorgängen in der Bank                                              |
        | Wo finde ich meine eigenen Vorgänge?                   | „Letzte Bankaktivität“ zeigt euren neuesten Vorgang in der Bank und führt dorthin           |
        | Wo finde ich meine eigenen Vorgänge?                   | „Letzte Lageraktivität“ zeigt euren neuesten Vorgang im Lager und führt dorthin             |
        | Wie sortiere und blättere ich in meinen Vorgängen?     | Ein Klick auf die Spaltenüberschrift sortiert alle Vorgänge, nicht nur die Seite            |
        | Wie sortiere und blättere ich in meinen Vorgängen?     | 100 Vorgänge je Seite                                                                       |
        | Wie sortiere und blättere ich in meinen Vorgängen?     | Mit „Weiter“ und „Zurück“ blättert ihr                                                      |
        | Wie sortiere und blättere ich in meinen Vorgängen?     | Die Sortierung steht in der Adresse                                                         |
        | Kann ich mir eine Seite merken?                        | Jede Seite lässt sich als Lesezeichen merken                                                |
        | Kann ich mir eine Seite merken?                        | Sortierung und Seite eurer Vorgänge bleiben im Lesezeichen erhalten                         |

    Scenario: The answer on rounding says nothing of half a gold
      The game's prices never give a figure of exactly half a gold (see "Guild contribution
      overview"), so a word on halves would only puzzle a member. A word such as "deshalb" that only
      holds the letters is no word on halves.

      When a member opens the explanation page
      Then neither the rule nor the example in the answer to "Warum passt eine Summe manchmal um 1 Gold nicht?" has a word that begins with "halb" or "hälft", in upper or lower case

  Rule: The page names the three things the protocol cannot show

    Scenario: The answer to "Was kann das Protokoll nicht zeigen?" lists exactly three points
      A point is one item of the one list in that answer and matches when its text contains the
      words as a member reads them, whatever spacing stands between them; their order is free.

      When a member opens the explanation page
      Then the answer to "Was kann das Protokoll nicht zeigen?" lists exactly these points:
        | point                                                  |
        | Gold, das ein Mitglied behält                          |
        | ob Munition gekauft oder selbst gefertigt wurde        |
        | ob eine Entnahme ins Handwerk ging oder verkauft wurde |
