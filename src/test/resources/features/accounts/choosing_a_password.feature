Feature: Choosing a password
  A reset issues a new random password, and whoever logs in with it must choose a password of their
  own before anything opens; the login page then asks for it under "Neues Passwort wählen". How the
  admin resets a password and passes it on is in "Resetting a password on the admin page". A chosen
  password, after a reset or as a change, has at least 8 characters, among them at least one digit
  and one special character, a character that is neither a letter, nor a digit, nor a space; and it
  differs from the password it replaces. Changing a chosen password is in "Changing the password".

  Background:
    Given the guild bank ledger holds:
      | Zeitpunkt        | Avatar | Betrag | Vorgang    |
      | 01.01.2026 12:00 | Aurora | 1000   | Einzahlung |
    And the daily collection has run
    And the admin has reset the password of every account

  Rule: After a reset the member chooses a new password before anything opens

    Scenario: Without a new password, a bookmark of the overview shows the login page
      Given a member has entered the name "Aurora" and the password the reset issued on the login page
      When the member opens a bookmark of the overview instead of choosing a new password
      Then the page is headed "Anmeldung"

    Scenario: A bookmark opened before the first login leads through the new password to its page
      Given a member who is not logged in has opened a bookmark of the storage ledger of "Aurora"
      And the member has entered the name "Aurora" and the password the reset issued on the login page
      When the member chooses "Sommer-2026!" as the new password
      Then the page is headed "Lager von Aurora"

    Scenario: The issued password alone opens no figures for a client that skips the login page
      When a client gives the name "Aurora" and the password the reset issued and asks for the overview's figures, skipping the login page
      Then the operator finds the request refused for lacking a login

    Scenario Outline: A client that gives <issued> cannot choose a new password for Aurora
      Given a client has tried to choose "Sommer-2026!" as Aurora's new password, giving <issued>
      When a member logs in as "Aurora" with the password the reset issued
      Then the page is headed "Neues Passwort wählen"

      Examples:
        | issued                             |
        | a password the reset did not issue |
        | no password                        |

  Rule: A chosen password has at least 8 characters, a digit and a special character, and differs from the one it replaces

    Scenario Outline: The new password "<password>" <outcome> <where>
      Given <state>
      When <attempt>
      Then <result>

      Examples:
        | password    | outcome                                                      | where                       | state                                                                                                      | attempt                                                          | result                                                                                               |
        | Abcd-123    | is accepted with exactly 8 characters and opens the overview | as the choice after a reset | a member has entered the name "Aurora" and the password the reset issued on the login page                 | the member chooses "Abcd-123" as the new password                | the page is headed "Übersicht"                                                                       |
        | sommer-2026 | is accepted without a capital letter                         | as the choice after a reset | a member has entered the name "Aurora" and the password the reset issued on the login page                 | the member chooses "sommer-2026" as the new password             | the page is headed "Übersicht"                                                                       |
        | 1234-5678   | is accepted without any letter                               | as the choice after a reset | a member has entered the name "Aurora" and the password the reset issued on the login page                 | the member chooses "1234-5678" as the new password               | the page is headed "Übersicht"                                                                       |
        | Abc-123     | is refused with 7 characters                                 | as the choice after a reset | a member has entered the name "Aurora" and the password the reset issued on the login page                 | the member chooses "Abc-123" as the new password                 | the page says "Das Passwort braucht mindestens 8 Zeichen, darunter eine Zahl und ein Sonderzeichen." |
        | Äbc-123     | is refused with 7 characters, an umlaut among them           | as the choice after a reset | a member has entered the name "Aurora" and the password the reset issued on the login page                 | the member chooses "Äbc-123" as the new password                 | the page says "Das Passwort braucht mindestens 8 Zeichen, darunter eine Zahl und ein Sonderzeichen." |
        | Abcdefg-!   | is refused without a digit                                   | as the choice after a reset | a member has entered the name "Aurora" and the password the reset issued on the login page                 | the member chooses "Abcdefg-!" as the new password               | the page says "Das Passwort braucht mindestens 8 Zeichen, darunter eine Zahl und ein Sonderzeichen." |
        | Abcd1234    | is refused without a special character                       | as the choice after a reset | a member has entered the name "Aurora" and the password the reset issued on the login page                 | the member chooses "Abcd1234" as the new password                | the page says "Das Passwort braucht mindestens 8 Zeichen, darunter eine Zahl und ein Sonderzeichen." |
        | Äbcdefg1    | is refused, since an umlaut is a letter                      | as the choice after a reset | a member has entered the name "Aurora" and the password the reset issued on the login page                 | the member chooses "Äbcdefg1" as the new password                | the page says "Das Passwort braucht mindestens 8 Zeichen, darunter eine Zahl und ein Sonderzeichen." |
        | Abcd 1234   | is refused, since a space is no special character            | as the choice after a reset | a member has entered the name "Aurora" and the password the reset issued on the login page                 | the member chooses "Abcd 1234" as the new password               | the page says "Das Passwort braucht mindestens 8 Zeichen, darunter eine Zahl und ein Sonderzeichen." |
        | Abc-123     | is refused with 7 characters                                 | as a change                 | a member has logged in as "Aurora" after choosing "Sommer-2026!" in place of the password the reset issued | the member changes the password from "Sommer-2026!" to "Abc-123" | the page says "Das Passwort braucht mindestens 8 Zeichen, darunter eine Zahl und ein Sonderzeichen." |

    Scenario Outline: A new password the same as the one it replaces is refused <where>
      Given <state>
      When <attempt>
      Then the page says "Das neue Passwort muss sich vom bisherigen unterscheiden."

      Examples:
        | where                       | state                                                                                                      | attempt                                                               |
        | as the choice after a reset | a member has entered the name "Aurora" and the password the reset issued on the login page                 | the member chooses the issued password as the new password            |
        | as a change                 | a member has logged in as "Aurora" after choosing "Sommer-2026!" in place of the password the reset issued | the member changes the password from "Sommer-2026!" to "Sommer-2026!" |

    Scenario Outline: A client that skips the page cannot set <password> <where>
      Given <state>
      When a client sets <password> as Aurora's new password <how>, skipping the page
      Then the operator finds the request refused for breaking the password rule

      Examples:
        | password                      | where                       | state                                                                                                      | how                                   |
        | "Abc-123"                     | as the choice after a reset | Aurora's account still holds the password the reset issued                                                 | giving the password the reset issued  |
        | "Abcd1234"                    | as the choice after a reset | Aurora's account still holds the password the reset issued                                                 | giving the password the reset issued  |
        | the password the reset issued | as the choice after a reset | Aurora's account still holds the password the reset issued                                                 | giving the password the reset issued  |
        | "Abcdefg-!"                   | as a change                 | a client has logged in as "Aurora" after choosing "Sommer-2026!" in place of the password the reset issued | with her login, giving "Sommer-2026!" |
        | "Sommer-2026!"                | as a change                 | a client has logged in as "Aurora" after choosing "Sommer-2026!" in place of the password the reset issued | with her login, giving "Sommer-2026!" |

    Scenario: A choice a client sets by skipping the page and the service refuses leaves the password its reset issued
      Given a client has set "Abc-123" as Aurora's new password, giving the password the reset issued, skipping the page
      When a member logs in as "Aurora" with the password the reset issued
      Then the page is headed "Neues Passwort wählen"

    Scenario Outline: A new password entered differently the second time is refused <where>
      Given <state>
      When <attempt>
      Then the page says "Die beiden Eingaben stimmen nicht überein."

      Examples:
        | where                       | state                                                                                                      | attempt                                                                                                |
        | as the choice after a reset | a member has entered the name "Aurora" and the password the reset issued on the login page                 | the member chooses "Sommer-2026!" as the new password but repeats it as "Sommer-2026?"                 |
        | as a change                 | a member has logged in as "Aurora" after choosing "Sommer-2026!" in place of the password the reset issued | the member changes the password from "Sommer-2026!" to "Winter-2026!" but repeats it as "Winter-2026?" |
