import type { ChoiceRefusal } from '../domain'

export const PASSWORD_REFUSAL_TEXTS: Readonly<Record<ChoiceRefusal, string>> = {
  'entries-differ': 'Die beiden Eingaben stimmen nicht überein.',
  'breaks-the-rule':
    'Das Passwort braucht mindestens 8 Zeichen, darunter eine Zahl und ein Sonderzeichen.',
  'same-as-before': 'Das neue Passwort muss sich vom bisherigen unterscheiden.',
}
