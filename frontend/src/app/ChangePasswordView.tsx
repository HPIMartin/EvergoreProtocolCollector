import type { FormEvent } from 'react'
import { useState } from 'react'

import type { SessionApi } from '../api'
import { Unauthorized } from '../api'
import type { ChangePasswordAnswer, ChoiceRefusal } from '../domain'
import { refusalOfChoosing } from '../domain'
import {
  LOAD_FAILED_MESSAGE,
  PASSWORD_REFUSAL_TEXTS,
  StatusPanel,
  TOO_MANY_ATTEMPTS_MESSAGE,
} from '../ui'

import { LoginRequired } from './LoginRequired.tsx'

export interface ChangePasswordViewProps {
  readonly session: SessionApi
  readonly onChanged: () => void
  readonly onNoAccount: () => void
}

type Refusal =
  ChoiceRefusal | 'wrong-current-password' | 'too-many-attempts' | 'unavailable'

const REFUSAL_TEXTS: Readonly<Record<Refusal, string>> = {
  ...PASSWORD_REFUSAL_TEXTS,
  'wrong-current-password': 'Das bisherige Passwort stimmt nicht.',
  'too-many-attempts': TOO_MANY_ATTEMPTS_MESSAGE,
  unavailable: LOAD_FAILED_MESSAGE,
}

const LOGIN_ENDED = 'login-ended'

export function ChangePasswordView({
  session,
  onChanged,
  onNoAccount,
}: ChangePasswordViewProps) {
  const [current, setCurrent] = useState('')
  const [replacement, setReplacement] = useState('')
  const [repeated, setRepeated] = useState('')
  const [refusal, setRefusal] = useState<Refusal | null>(null)
  const [loginEnded, setLoginEnded] = useState(false)
  const [saving, setSaving] = useState(false)

  async function answerOfChanging(): Promise<
    ChangePasswordAnswer | typeof LOGIN_ENDED | null
  > {
    try {
      return await session.changePassword(current, replacement)
    } catch (failure) {
      return failure instanceof Unauthorized ? LOGIN_ENDED : null
    }
  }

  async function change(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    if (saving) {
      return
    }
    const refusedHere = refusalOfChoosing(current, replacement, repeated)
    if (refusedHere !== null) {
      setRefusal(refusedHere)
      return
    }
    setSaving(true)
    const answer = await answerOfChanging()
    setSaving(false)
    if (answer === null) {
      setRefusal('unavailable')
    } else if (answer === LOGIN_ENDED) {
      setLoginEnded(true)
    } else if (answer === 'no-account') {
      onNoAccount()
    } else if (answer === 'changed') {
      onChanged()
    } else {
      setRefusal(answer)
    }
  }

  if (loginEnded) {
    return <LoginRequired />
  }

  return (
    <section className="login-page">
      <h2 data-testid="view-title">Passwort ändern</h2>
      <form
        className="login-page__form"
        onSubmit={(event) => void change(event)}
      >
        <label className="login-page__field">
          Bisheriges Passwort
          <input
            type="password"
            autoComplete="current-password"
            value={current}
            onChange={(event) => setCurrent(event.target.value)}
          />
        </label>
        <label className="login-page__field">
          Neues Passwort
          <input
            type="password"
            autoComplete="new-password"
            value={replacement}
            onChange={(event) => setReplacement(event.target.value)}
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
    </section>
  )
}
