import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import type { HttpPost } from '../api'
import { httpLoginApi } from '../api'

import { NewPasswordStep } from './NewPasswordStep.tsx'

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

interface Shown {
  readonly loggedIn: number[]
  readonly refused: number[]
}

const ISSUED = 'Issued-2026!'
const CHOSEN = 'Sommer-2026!'
const MISMATCH = 'Die beiden Eingaben stimmen nicht überein.'
const RULE =
  'Das Passwort braucht mindestens 8 Zeichen, darunter eine Zahl und ein Sonderzeichen.'
const SAME = 'Das neue Passwort muss sich vom bisherigen unterscheiden.'
const TOO_MANY = 'Zu viele Versuche. Bitte warte einen Moment.'

const LOGGED_IN: Answer = { status: 200, body: '{"step":"logged-in"}' }

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

function stepFor(service: FakeService): Shown {
  const loggedIn: number[] = []
  const refused: number[] = []
  render(
    <NewPasswordStep
      api={httpLoginApi(service.post)}
      name="Boreas"
      issuedPassword={ISSUED}
      onLoggedIn={() => loggedIn.push(1)}
      onRefused={() => refused.push(1)}
    />,
  )

  return { loggedIn, refused }
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

function saveButton(): HTMLButtonElement {
  return screen.getByRole<HTMLButtonElement>('button', {
    name: 'Passwort speichern',
  })
}

async function choose(newPassword: string, repeated: string): Promise<void> {
  fireEvent.change(screen.getByLabelText('Neues Passwort'), {
    target: { value: newPassword },
  })
  fireEvent.change(screen.getByLabelText('Neues Passwort wiederholen'), {
    target: { value: repeated },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Passwort speichern' }))
  await act(async () => undefined)
}

function shownMessages(): string[] {
  return screen
    .queryAllByTestId('status-panel')
    .map((panel) => panel.textContent ?? '')
}

describe('NewPasswordStep', () => {
  afterEach(() => {
    cleanup()
  })

  it('is headed Neues Passwort wählen and asks for the new password twice', () => {
    stepFor(answering(LOGGED_IN))

    expect(screen.getByTestId('view-title')).toHaveProperty(
      'textContent',
      'Neues Passwort wählen',
    )
    expect(screen.getByLabelText('Neues Passwort')).toHaveProperty(
      'type',
      'password',
    )
    expect(screen.getByLabelText('Neues Passwort wiederholen')).toHaveProperty(
      'type',
      'password',
    )
  })

  it('posts the name, the issued password and the new password', async () => {
    const service = answering(LOGGED_IN)
    stepFor(service)

    await choose(CHOSEN, CHOSEN)

    expect(service.posted).toEqual([
      {
        url: '/api/v1/login/new-password',
        body: { name: 'Boreas', issuedPassword: ISSUED, newPassword: CHOSEN },
      },
    ])
  })

  it('keeps the button to save disabled while the request is on its way', async () => {
    const way = onItsWay()
    stepFor(way.service)

    await choose(CHOSEN, CHOSEN)

    const disabledWhileWaiting = saveButton().disabled
    way.arrive({ status: 429, body: null })
    await act(async () => undefined)
    const disabledAfterwards = saveButton().disabled
    expect({ disabledWhileWaiting, disabledAfterwards }).toEqual({
      disabledWhileWaiting: true,
      disabledAfterwards: false,
    })
  })

  it('posts nothing for a second click while the request is on its way', async () => {
    const way = onItsWay()
    stepFor(way.service)

    await choose(CHOSEN, CHOSEN)
    fireEvent.click(saveButton())
    await act(async () => undefined)

    expect(way.service.posted).toHaveLength(1)
  })

  it('reports the login the new password opened', async () => {
    const shown = stepFor(answering(LOGGED_IN))

    await choose(CHOSEN, CHOSEN)

    expect(shown.loggedIn).toEqual([1])
  })

  it.each([
    ['exactly 8 characters', 'Abcd-123'],
    ['no capital letter', 'sommer-2026'],
    ['no letter at all', '1234-5678'],
  ])('lets a new password with %s through', async (_, newPassword) => {
    const service = answering(LOGGED_IN)
    stepFor(service)

    await choose(newPassword, newPassword)

    expect(service.posted).toHaveLength(1)
  })

  it('says that the two entries differ and posts nothing', async () => {
    const service = answering(LOGGED_IN)
    stepFor(service)

    await choose(CHOSEN, 'Sommer-2026?')

    const messages = shownMessages()
    expect(messages).toEqual([MISMATCH])
    expect(service.posted).toEqual([])
  })

  it.each([
    ['7 characters', 'Abc-123'],
    ['7 characters, an umlaut among them', 'Äbc-123'],
    ['no digit', 'Abcdefg-!'],
    ['no special character', 'Abcd1234'],
    ['an umlaut, which is a letter, as its only other character', 'Äbcdefg1'],
    ['a space, which is no special character', 'Abcd 1234'],
  ])(
    'states the password rule for a new password with %s and posts nothing',
    async (_, newPassword) => {
      const service = answering(LOGGED_IN)
      stepFor(service)

      await choose(newPassword, newPassword)

      const messages = shownMessages()
      expect(messages).toEqual([RULE])
      expect(service.posted).toEqual([])
    },
  )

  it('says that the issued password cannot be the new one and posts nothing', async () => {
    const service = answering(LOGGED_IN)
    stepFor(service)

    await choose(ISSUED, ISSUED)

    const messages = shownMessages()
    expect(messages).toEqual([SAME])
    expect(service.posted).toEqual([])
  })

  it.each([
    ['breaks-the-rule', RULE],
    ['same-as-before', SAME],
  ])(
    'states the service refusal %s in the words of the page',
    async (refusal, text) => {
      const shown = stepFor(
        answering({ status: 400, body: JSON.stringify({ refusal }) }),
      )

      await choose(CHOSEN, CHOSEN)

      const messages = shownMessages()
      expect(messages).toEqual([text])
      expect(shown.loggedIn).toEqual([])
    },
  )

  it('hands back to the login when the service refuses the issued password', async () => {
    const shown = stepFor(
      answering({ status: 401, body: '{"refusal":"wrong-name-or-password"}' }),
    )

    await choose(CHOSEN, CHOSEN)

    expect(shown.refused).toEqual([1])
    expect(shown.loggedIn).toEqual([])
  })

  it('asks to wait after too many attempts', async () => {
    stepFor(answering({ status: 429, body: null }))

    await choose(CHOSEN, CHOSEN)

    const messages = shownMessages()
    expect(messages).toEqual([TOO_MANY])
  })
})
