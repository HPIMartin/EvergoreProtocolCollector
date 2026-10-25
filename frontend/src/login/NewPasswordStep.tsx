import type { FormEvent } from 'react'
import { useState } from 'react'

import type { LoginApi } from '../api'
import type { ChoiceRefusal, NewPasswordAnswer } from '../domain'
import { refusalOfChoosing } from '../domain'
import { LOAD_FAILED_MESSAGE, StatusPanel } from '../ui'

export interface NewPasswordStepProps {
  readonly api: LoginApi
  readonly name: string
  readonly issuedPassword: string
  readonly next: string | null
  readonly onLoggedIn: (next: string) => void
  readonly onRefused: () => void
}

type Refusal = ChoiceRefusal | 'too-many-attempts' | 'unavailable'

const REFUSAL_TEXTS: Readonly<Record<Refusal, string>> = {
  'entries-differ': 'Die beiden Eingaben stimmen nicht überein.',
  'breaks-the-rule':
    'Das Passwort braucht mindestens 8 Zeichen, darunter eine Zahl und ein Sonderzeichen.',
  'same-as-before': 'Das neue Passwort muss sich vom bisherigen unterscheiden.',
  'too-many-attempts': 'Zu viele Versuche. Bitte warte einen Moment.',
  unavailable: LOAD_FAILED_MESSAGE,
}

export function NewPasswordStep({
  api,
  name,
  issuedPassword,
  next,
  onLoggedIn,
  onRefused,
}: NewPasswordStepProps) {
  const [newPassword, setNewPassword] = useState('')
  const [repeated, setRepeated] = useState('')
  const [refusal, setRefusal] = useState<Refusal | null>(null)
  const [saving, setSaving] = useState(false)

  async function answerOfChoosing(): Promise<NewPasswordAnswer | null> {
    try {
      return await api.chooseNewPassword(
        name,
        issuedPassword,
        newPassword,
        next,
      )
    } catch {
      return null
    }
  }

  async function choose(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    if (saving) {
      return
    }
    const refusedHere = refusalOfChoosing(issuedPassword, newPassword, repeated)
    if (refusedHere !== null) {
      setRefusal(refusedHere)
      return
    }
    setSaving(true)
    const answer = await answerOfChoosing()
    setSaving(false)
    if (answer === null) {
      setRefusal('unavailable')
    } else if (typeof answer === 'object') {
      onLoggedIn(answer.next)
    } else if (answer === 'wrong-name-or-password') {
      onRefused()
    } else {
      setRefusal(answer)
    }
  }

  return (
    <main className="login-page">
      <h2 data-testid="view-title">Neues Passwort wählen</h2>
      <form
        className="login-page__form"
        onSubmit={(event) => void choose(event)}
      >
        <label className="login-page__field">
          Neues Passwort
          <input
            type="password"
            autoComplete="new-password"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
          />
        </label>
        <label className="login-page__field">
          Neues Passwort wiederholen
          <input
            type="password"
            autoComplete="new-password"
            value={repeated}
            onChange={(event) => setRepeated(event.target.value)}
          />
        </label>
        <button type="submit" disabled={saving}>
          Passwort speichern
        </button>
      </form>
      {refusal === null ? null : (
        <StatusPanel variant="error" message={REFUSAL_TEXTS[refusal]} />
      )}
    </main>
  )
}
