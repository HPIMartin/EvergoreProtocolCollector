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

function loggedInTo(next: string): Answer {
  return { status: 200, body: JSON.stringify({ step: 'logged-in', next }) }
}

const LOGGED_IN: Answer = loggedInTo('/')
const CHOOSE_NEW_PASSWORD: Answer = {
  status: 200,
  body: '{"step":"choose-new-password"}',
}
const WRONG_NAME_OR_PASSWORD: Answer = {
  status: 401,
  body: '{"refusal":"wrong-name-or-password"}',
}
const TOO_MANY_REQUESTS: Answer = { status: 429, body: null }
const RESTING: Answer = { status: 403, body: '{"refusal":"resting"}' }
const RESTING_TEXT =
  'Dein Konto ruht, bis das Protokoll wieder einen Vorgang von dir zeigt.'

function answering(answer: Answer): FakeService {
  return answeringIn(() => answer)
}

function answeringIn(answerFor: (url: string) => Answer): FakeService {
  const posted: Posted[] = []

  return {
    posted,
    post: (url, body) => {
      posted.push({ url, body: JSON.parse(body) })
      const answer = answerFor(url)

      return Promise.resolve(new Response(answer.body, answer))
    },
  }
}

function afterTheIssuedPassword(newPasswordAnswer: Answer): FakeService {
  return answeringIn((url) =>
    url === '/api/v1/login' ? CHOOSE_NEW_PASSWORD : newPasswordAnswer,
  )
}

async function chooseNewPassword(newPassword: string): Promise<void> {
  fireEvent.change(screen.getByLabelText('Neues Passwort'), {
    target: { value: newPassword },
  })
  fireEvent.change(screen.getByLabelText('Neues Passwort wiederholen'), {
    target: { value: newPassword },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Passwort speichern' }))
  await act(async () => undefined)
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

  it('sends the next of its own address with the login', async () => {
    const service = answering(LOGGED_IN)
    loginPageAt('?next=%2Favatars%2FAurora%2Fstorage', service)

    await logIn('Aurora', CHOSEN)

    expect(service.posted).toEqual([
      {
        url: '/api/v1/login',
        body: {
          name: 'Aurora',
          password: CHOSEN,
          next: '/avatars/Aurora/storage',
        },
      },
    ])
  })

  it('navigates to what the service answered', async () => {
    const shown = loginPageAt(
      '?next=%2F%2Fevil.example',
      answering(loggedInTo('/avatars/Aurora/storage')),
    )

    await logIn('Aurora', CHOSEN)

    expect(shown.navigatedTo).toEqual(['/avatars/Aurora/storage'])
  })

  it('says that the name or the password is wrong and stays', async () => {
    const shown = loginPageAt('', answering(WRONG_NAME_OR_PASSWORD))

    await logIn('Aurora', ANOTHER)

    const messages = shownMessages()
    expect(messages).toEqual(['Name oder Passwort stimmt nicht.'])
    expect(shown.navigatedTo).toEqual([])
  })

  it.each([
    ['a server error', { status: 500, body: null }],
    ['an answer it cannot read', { status: 200, body: 'not JSON' }],
    ['an answer without a step it knows', { status: 200, body: '{}' }],
  ])(
    'says in German that the data could not be loaded after %s and stays',
    async (_, answer) => {
      const shown = loginPageAt('', answering(answer))

      await logIn('Aurora', CHOSEN)

      const messages = shownMessages()
      expect(messages).toEqual([
        'Die Daten konnten nicht geladen werden. Bitte später erneut versuchen.',
      ])
      expect(shown.navigatedTo).toEqual([])
    },
  )

  it('says the same after a request that never arrived', async () => {
    const unreachable: FakeService = {
      posted: [],
      post: () => Promise.reject(new TypeError('Failed to fetch')),
    }
    loginPageAt('', unreachable)

    await logIn('Aurora', CHOSEN)

    const messages = shownMessages()
    expect(messages).toEqual([
      'Die Daten konnten nicht geladen werden. Bitte später erneut versuchen.',
    ])
  })

  it('says the same after a server error on the choice of a new password', async () => {
    loginPageAt('', afterTheIssuedPassword({ status: 500, body: null }))
    await logIn('Boreas', ISSUED)

    await chooseNewPassword(CHOSEN)

    const messages = shownMessages()
    expect(messages).toEqual([
      'Die Daten konnten nicht geladen werden. Bitte später erneut versuchen.',
    ])
  })

  it('says the account rests and stays', async () => {
    const shown = loginPageAt('', answering(RESTING))

    await logIn('Calix', CHOSEN)

    const messages = shownMessages()
    expect(messages).toEqual([RESTING_TEXT])
    expect(shown.navigatedTo).toEqual([])
  })

  it('says the account rests when the choice of a new password is refused for it', async () => {
    const shown = loginPageAt('', afterTheIssuedPassword(RESTING))
    await logIn('Calix', ISSUED)

    await chooseNewPassword(CHOSEN)

    const messages = shownMessages()
    expect(messages).toEqual([RESTING_TEXT])
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

  it('chooses the new password with the name and the issued password of the first step', async () => {
    const service = afterTheIssuedPassword(LOGGED_IN)
    loginPageAt('', service)
    await logIn('Boreas', ISSUED)

    await chooseNewPassword(CHOSEN)

    const lastPosted = service.posted.at(-1)
    expect(lastPosted).toEqual({
      url: '/api/v1/login/new-password',
      body: { name: 'Boreas', issuedPassword: ISSUED, newPassword: CHOSEN },
    })
  })

  it('sends the next of its own address with the new password', async () => {
    const service = afterTheIssuedPassword(LOGGED_IN)
    loginPageAt('?next=%2Favatars%2FBoreas%2Fbank', service)
    await logIn('Boreas', ISSUED)

    await chooseNewPassword(CHOSEN)

    const lastPosted = service.posted.at(-1)
    expect(lastPosted).toEqual({
      url: '/api/v1/login/new-password',
      body: {
        name: 'Boreas',
        issuedPassword: ISSUED,
        newPassword: CHOSEN,
        next: '/avatars/Boreas/bank',
      },
    })
  })

  it('navigates to what the service answered once the new password is chosen', async () => {
    const shown = loginPageAt(
      '?next=%2F%2Fevil.example',
      afterTheIssuedPassword(loggedInTo('/avatars/Boreas/bank')),
    )
    await logIn('Boreas', ISSUED)

    await chooseNewPassword(CHOSEN)

    expect(shown.navigatedTo).toEqual(['/avatars/Boreas/bank'])
  })

  it('goes back to the login and says the name or the password is wrong when the service refuses the issued password', async () => {
    const shown = loginPageAt(
      '',
      afterTheIssuedPassword(WRONG_NAME_OR_PASSWORD),
    )
    await logIn('Boreas', ISSUED)

    await chooseNewPassword(CHOSEN)

    const messages = shownMessages()
    expect(screen.getByTestId('view-title')).toHaveProperty(
      'textContent',
      'Anmeldung',
    )
    expect(messages).toEqual(['Name oder Passwort stimmt nicht.'])
    expect(shown.navigatedTo).toEqual([])
  })

  it('keeps the issued password out of the address and the browser storage', async () => {
    window.history.replaceState(null, '', '/login')
    loginPageAt('', afterTheIssuedPassword(LOGGED_IN))

    await logIn('Boreas', ISSUED)

    const stored = [
      ...Object.values({ ...window.localStorage }),
      ...Object.values({ ...window.sessionStorage }),
    ]
    expect(window.location.href).not.toContain(ISSUED)
    expect(stored).not.toContain(ISSUED)
  })
})
