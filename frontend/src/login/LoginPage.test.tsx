import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import type { HttpPost } from '../api'

import { LoginPage } from './LoginPage.tsx'

interface Answer {
  readonly status: number
  readonly body: string | null
}

interface Posted {
  readonly url: string
  readonly body: unknown
}

interface FakeService {
  readonly posted: Posted[]
  readonly post: HttpPost
}

const CHOSEN = 'Sommer-2026!'
const ANOTHER = 'Winter-2026!'
const ISSUED = 'Issued-2026!'

const LOGGED_IN: Answer = { status: 200, body: '{"step":"logged-in"}' }
const CHOOSE_NEW_PASSWORD: Answer = {
  status: 200,
  body: '{"step":"choose-new-password"}',
}
const WRONG_NAME_OR_PASSWORD: Answer = {
  status: 401,
  body: '{"refusal":"wrong-name-or-password"}',
}
const TOO_MANY_REQUESTS: Answer = { status: 429, body: null }

function answering(answer: Answer): FakeService {
  const posted: Posted[] = []

  return {
    posted,
    post: (url, body) => {
      posted.push({ url, body: JSON.parse(body) })

      return Promise.resolve(new Response(answer.body, answer))
    },
  }
}

interface Shown {
  readonly navigatedTo: string[]
}

function loginPageAt(search: string, service: FakeService): Shown {
  const navigatedTo: string[] = []
  render(
    <LoginPage
      search={search}
      post={service.post}
      navigate={(target) => navigatedTo.push(target)}
    />,
  )

  return { navigatedTo }
}

function onItsWay(): {
  readonly service: FakeService
  readonly arrive: (answer: Answer) => void
} {
  const posted: Posted[] = []
  const waiting: ((response: Response) => void)[] = []

  return {
    service: {
      posted,
      post: (url, body) => {
        posted.push({ url, body: JSON.parse(body) })

        return new Promise<Response>((resolve) => {
          waiting.push(resolve)
        })
      },
    },
    arrive: (answer) => {
      waiting.forEach((resolve) => {
        resolve(new Response(answer.body, answer))
      })
    },
  }
}

function loginButton(): HTMLButtonElement {
  return screen.getByRole<HTMLButtonElement>('button', { name: 'Anmelden' })
}

async function logIn(name: string, password: string): Promise<void> {
  fireEvent.change(screen.getByLabelText('Name'), { target: { value: name } })
  fireEvent.change(screen.getByLabelText('Passwort'), {
    target: { value: password },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Anmelden' }))
  await act(async () => undefined)
}

function shownMessages(): string[] {
  return screen
    .queryAllByTestId('status-panel')
    .map((panel) => panel.textContent ?? '')
}

describe('LoginPage', () => {
  afterEach(() => {
    cleanup()
  })

  it('is headed Anmeldung and asks for the name and the password', () => {
    loginPageAt('', answering(LOGGED_IN))

    expect(screen.getByTestId('view-title')).toHaveProperty(
      'textContent',
      'Anmeldung',
    )
    expect(screen.getByLabelText('Name')).toHaveProperty('type', 'text')
    expect(screen.getByLabelText('Passwort')).toHaveProperty('type', 'password')
    expect(screen.getByRole('button', { name: 'Anmelden' })).toBeDefined()
  })

  it('posts the name and the password to the login', async () => {
    const service = answering(LOGGED_IN)
    loginPageAt('', service)

    await logIn('Aurora', CHOSEN)

    expect(service.posted).toEqual([
      {
        url: '/api/v1/login',
        body: { name: 'Aurora', password: CHOSEN },
      },
    ])
  })

  it('returns to the page the member opened before', async () => {
    const shown = loginPageAt(
      '?next=%2Favatars%2FAurora%2Fstorage',
      answering(LOGGED_IN),
    )

    await logIn('Aurora', CHOSEN)

    expect(shown.navigatedTo).toEqual(['/avatars/Aurora/storage'])
  })

  it('opens the overview after a login without a page to return to', async () => {
    const shown = loginPageAt('', answering(LOGGED_IN))

    await logIn('Aurora', CHOSEN)

    expect(shown.navigatedTo).toEqual(['/'])
  })

  it('opens the overview instead of an address of another site', async () => {
    const shown = loginPageAt('?next=%2F%2Fevil.example', answering(LOGGED_IN))

    await logIn('Aurora', CHOSEN)

    expect(shown.navigatedTo).toEqual(['/'])
  })

  it('says that the name or the password is wrong and stays', async () => {
    const shown = loginPageAt('', answering(WRONG_NAME_OR_PASSWORD))

    await logIn('Aurora', ANOTHER)

    const messages = shownMessages()
    expect(messages).toEqual(['Name oder Passwort stimmt nicht.'])
    expect(shown.navigatedTo).toEqual([])
  })

  it('asks to wait after too many attempts', async () => {
    const shown = loginPageAt('', answering(TOO_MANY_REQUESTS))

    await logIn('Aurora', CHOSEN)

    const messages = shownMessages()
    expect(messages).toEqual(['Zu viele Versuche. Bitte warte einen Moment.'])
    expect(shown.navigatedTo).toEqual([])
  })

  it('keeps the button to log in disabled while the request is on its way', async () => {
    const way = onItsWay()
    loginPageAt('', way.service)

    await logIn('Aurora', CHOSEN)

    const disabledWhileWaiting = loginButton().disabled
    way.arrive(WRONG_NAME_OR_PASSWORD)
    await act(async () => undefined)
    const disabledAfterwards = loginButton().disabled
    expect({ disabledWhileWaiting, disabledAfterwards }).toEqual({
      disabledWhileWaiting: true,
      disabledAfterwards: false,
    })
  })

  it('posts nothing for a second click while the request is on its way', async () => {
    const way = onItsWay()
    loginPageAt('', way.service)

    await logIn('Aurora', CHOSEN)
    fireEvent.click(loginButton())
    await act(async () => undefined)

    expect(way.service.posted).toHaveLength(1)
  })

  it('says nothing before the first attempt', () => {
    loginPageAt('', answering(LOGGED_IN))

    const messages = shownMessages()

    expect(messages).toEqual([])
  })

  it('turns to the choice of a new password after a password the service issued', async () => {
    const shown = loginPageAt('', answering(CHOOSE_NEW_PASSWORD))

    await logIn('Boreas', ISSUED)

    expect(screen.getByTestId('view-title')).toHaveProperty(
      'textContent',
      'Neues Passwort wählen',
    )
    expect(shown.navigatedTo).toEqual([])
  })
})
