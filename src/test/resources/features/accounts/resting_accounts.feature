@wip
Feature: Resting accounts
  An account rests while its member stands in the overview's dormant table, that is while the
  member's last movement lies more than 30 days before the newest movement the guild ledgers hold;
  where that line falls, to the minute, is in "Active and dormant members". A resting account cannot
  log in, not even to choose its first password, and the login page says why, but only to the right
  password; the account opens again by itself once a collection has read a new movement of the
  member. An admin's account rests like any other; the admin key from the service's log still logs
  in as the admin, as "The admin key" says. A login made before the account came to rest lasts until
  it ends.

  Rule: A member in the dormant table cannot log in, whatever password the account holds, and is told why

    Background:
      Calix and Dorn moved last on 01.01.2026 at 00:00, and Dorn chose his password while that was
      the newest movement; Calix still holds the password his reset issued. Aurora's movement of
      31.01. puts both more than 30 days behind.

      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang  |
        | 01.01.2026 00:00 | Calix  | 300    | Entnahme |
        | 01.01.2026 00:00 | Dorn   | 300    | Entnahme |
      And the daily collection has run
      And Dorn has chosen the password "Sommer-2026!"
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 31.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
      And the daily collection has run

    Scenario Outline: A resting account with the role "<role>" is told it rests, holding <which>
      Given <avatar>'s account has the role "<role>"
      When <who> logs in as "<avatar>" with <password>
      Then the page says "Dein Konto ruht, bis das Protokoll wieder einen Vorgang von dir zeigt."
      And a bookmark of the overview opened afterwards shows the login page

      Examples:
        | avatar | role   | who       | which                         | password                      |
        | Calix  | member | a member  | the password its reset issued | the password the reset issued |
        | Calix  | admin  | the admin | the password its reset issued | the password the reset issued |
        | Dorn   | member | a member  | the password he chose         | the password "Sommer-2026!"   |

    Scenario: A client that chooses a new password for a resting account is not logged in, and the issued password stays
      When a client gives the name "Calix" and the password the reset issued and chooses "Sommer-2026!" as the new password, skipping the login page
      Then the operator finds the request refused for lacking a login
      And the operator finds a login as "Calix" with the password "Sommer-2026!" answered as a wrong password

    Scenario: A resting account with a wrong password gets the answer every wrong password gets
      When a member logs in as "Calix" with the password "Winter-2026!"
      Then the page says "Name oder Passwort stimmt nicht."

  Rule: A resting account opens again once a collection has read a new movement of the member

    Background:
      Calix and Dorn moved last on 01.01.2026 at 00:00, and Dorn chose his password while that was
      the newest movement; Calix still holds the password his reset issued. Aurora's movement of
      31.01. puts both more than 30 days behind.

      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang  |
        | 01.01.2026 00:00 | Calix  | 300    | Entnahme |
        | 01.01.2026 00:00 | Dorn   | 300    | Entnahme |
      And the daily collection has run
      And Dorn has chosen the password "Sommer-2026!"
      And the guild storage ledger holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 31.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
      And the daily collection has run

    Scenario Outline: A collection that reads a new movement of <whom> on 15.02. <outcome>
      The movement of 15.02. is the newest one the guild ledgers hold.

      Given the guild bank ledger also holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 15.02.2026 12:00 | <whom> | 100    | Einzahlung |
      And the daily collection has run
      When a member logs in as "<avatar>" with <password>
      Then <result>

      Examples:
        | whom   | outcome                                       | avatar | password                      | result                                                                                 |
        | Calix  | opens his account, still to choose a password | Calix  | the password the reset issued | the page is headed "Neues Passwort wählen"                                             |
        | Dorn   | opens his account with the password he chose  | Dorn   | the password "Sommer-2026!"   | the page is headed "Übersicht"                                                         |
        | Aurora | leaves Calix's account resting                | Calix  | the password the reset issued | the page says "Dein Konto ruht, bis das Protokoll wieder einen Vorgang von dir zeigt." |

  Rule: A login made before the account came to rest lasts until it ends

    Scenario: A member keeps a login made before their account came to rest
      Calix logs in while Boreas's movement of 30.01. is the newest, within 30 days of his own of
      01.01. The next collection reads Aurora's movement of 31.01., which puts Calix more than 30
      days behind; a day after logging in he still sees the overview, with himself in the dormant
      table.

      Given the guild bank ledger holds:
        | Zeitpunkt        | Avatar | Betrag | Vorgang    |
        | 01.01.2026 00:00 | Calix  | 300    | Entnahme   |
        | 30.01.2026 12:00 | Boreas | 750    | Einzahlung |
      And the daily collection has run
      And Calix has chosen the password "Sommer-2026!"
      And a member has logged in as "Calix" with the password "Sommer-2026!"
      And the guild storage ledger also holds:
        | Zeitpunkt        | Avatar | Menge | Gegenstand  | Qualität | Vorgang     |
        | 31.01.2026 12:00 | Aurora | 1     | Eisenbarren | 100      | Einlagerung |
      And the daily collection has run
      When the member opens the overview 1 day after logging in
      Then the page is headed "Übersicht"
      And the dormant table lists "Calix"
