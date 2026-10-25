import type { FormEvent } from 'react'
import { useState } from 'react'

import type { HttpPost } from '../api'
import { httpLoginApi } from '../api'
import type { LoginAnswer } from '../domain'
import { StatusPanel } from '../ui'

import { NewPasswordStep } from './NewPasswordStep.tsx'

export interface LoginPageProps {
  readonly search: string
  readonly post: HttpPost
  readonly navigate: (target: string) => void
}

type Refusal = Extract<
  LoginAnswer,
  'wrong-name-or-password' | 'too-many-attempts'
>

const REFUSAL_TEXTS: Readonly<Record<Refusal, string>> = {
  'wrong-name-or-password': 'Name oder Passwort stimmt nicht.',
  'too-many-attempts': 'Zu viele Versuche. Bitte warte einen Moment.',
}

const NEXT = 'next'

export function LoginPage({ search, post, navigate }: LoginPageProps) {
  const [name, setName] = useState('')
  const [password, setPassword] = useState('')
  const [refusal, setRefusal] = useState<Refusal | null>(null)
  const [choosingANewPassword, setChoosingANewPassword] = useState(false)
  const [loggingIn, setLoggingIn] = useState(false)

  const api = httpLoginApi(post)

  const next = new URLSearchParams(search).get(NEXT)

  function backToTheLogin(): void {
    setChoosingANewPassword(false)
    setPassword('')
    setRefusal('wrong-name-or-password')
  }

  async function logIn(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault()
    if (loggingIn) {
      return
    }
    setLoggingIn(true)
    const answer = await api.logIn(name, password, next)
    setLoggingIn(false)
    if (typeof answer === 'object') {
      navigate(answer.next)
    } else if (answer === 'choose-new-password') {
      setRefusal(null)
      setChoosingANewPassword(true)
    } else {
      setRefusal(answer)
    }
  }

  if (choosingANewPassword) {
    return (
      <NewPasswordStep
        api={api}
        name={name}
        issuedPassword={password}
        next={next}
        onLoggedIn={navigate}
        onRefused={backToTheLogin}
      />
    )
  }

  return (
    <main className="login-page">
      <h2 data-testid="view-title">Anmeldung</h2>
      <form
        className="login-page__form"
        onSubmit={(event) => void logIn(event)}
      >
        <label className="login-page__field">
          Name
          <input
            type="text"
            autoComplete="username"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
        </label>
        <label className="login-page__field">
          Passwort
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </label>
        <button type="submit" disabled={loggingIn}>
          Anmelden
        </button>
      </form>
      {refusal === null ? null : (
        <StatusPanel variant="error" message={REFUSAL_TEXTS[refusal]} />
      )}
    </main>
  )
}
