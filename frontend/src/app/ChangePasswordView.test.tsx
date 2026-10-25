import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import type { SessionApi } from '../api'
import { Unauthorized } from '../api'
import type { ChangePasswordAnswer } from '../domain'

import { ChangePasswordView } from './ChangePasswordView.tsx'
import { LoginNavigation } from './LoginRequired.tsx'

const CURRENT = 'Sommer-2026!'
const NEW_PASSWORD = 'Winter-2026!'
const MISMATCH = 'Die beiden Eingaben stimmen nicht überein.'
const RULE =
  'Das Passwort braucht mindestens 8 Zeichen, darunter eine Zahl und ein Sonderzeichen.'
const SAME = 'Das neue Passwort muss sich vom bisherigen unterscheiden.'
const WRONG_CURRENT = 'Das bisherige Passwort stimmt nicht.'
const FAILED =
  'Die Daten konnten nicht geladen werden. Bitte später erneut versuchen.'

interface Changed {
  readonly posted: { current: string; replacement: string }[]
  readonly arrived: number[]
  readonly withoutAccount: number[]
  readonly navigatedTo: string[]
}

function viewAnswering(answer: Promise<ChangePasswordAnswer>): Changed {
  const changed: Changed = {
    posted: [],
    arrived: [],
    withoutAccount: [],
    navigatedTo: [],
  }
  const session: SessionApi = {
    me: () => Promise.reject(new Error('not asked')),
    logOut: () => Promise.reject(new Error('not asked')),
    changePassword: (current, replacement) => {
      changed.posted.push({ current, replacement })

      return answer
    },
  }
  render(
    <LoginNavigation value={(target) => changed.navigatedTo.push(target)}>
      <ChangePasswordView
        session={session}
        onChanged={() => changed.arrived.push(1)}
        onNoAccount={() => changed.withoutAccount.push(1)}
      />
    </LoginNavigation>,
  )

  return changed
}

function pending(): {
  readonly promise: Promise<ChangePasswordAnswer>
  readonly settle: (answer: ChangePasswordAnswer) => void
} {
  const settlers: ((answer: ChangePasswordAnswer) => void)[] = []
  const promise = new Promise<ChangePasswordAnswer>((resolve) => {
    settlers.push(resolve)
  })

  return {
    promise,
    settle: (answer) => {
      settlers.forEach((settler) => {
        settler(answer)
      })
    },
  }
}

async function change(
  current: string,
  replacement: string,
  repeated: string = replacement,
): Promise<void> {
  fireEvent.change(screen.getByLabelText('Bisheriges Passwort'), {
    target: { value: current },
  })
  fireEvent.change(screen.getByLabelText('Neues Passwort'), {
    target: { value: replacement },
  })
  fireEvent.change(screen.getByLabelText('Neues Passwort wiederholen'), {
    target: { value: repeated },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Passwort speichern' }))
  await act(async () => undefined)
}

function saveButton(): HTMLButtonElement {
  return screen.getByRole<HTMLButtonElement>('button', {
    name: 'Passwort speichern',
  })
}

function shownMessages(): string[] {
  return screen
    .queryAllByTestId('status-panel')
    .map((panel) => panel.textContent ?? '')
}

describe('ChangePasswordView', () => {
  afterEach(cleanup)

  it('is headed Passwort ändern and asks for the current, the new and the repeated password', () => {
    viewAnswering(Promise.resolve('changed'))

    expect(screen.getByTestId('view-title').textContent).toBe('Passwort ändern')
    expect(screen.getByLabelText('Bisheriges Passwort')).toHaveProperty(
      'type',
      'password',
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

  it('posts the current and the new password', async () => {
    const changed = viewAnswering(Promise.resolve('changed'))

    await change(CURRENT, NEW_PASSWORD)

    expect(changed.posted).toEqual([
      { current: CURRENT, replacement: NEW_PASSWORD },
    ])
  })

  it('reports the change it was told about', async () => {
    const changed = viewAnswering(Promise.resolve('changed'))

    await change(CURRENT, NEW_PASSWORD)

    expect(changed.arrived).toEqual([1])
  })

  it.each([
    ['two different entries', CURRENT, NEW_PASSWORD, 'Winter-2026?', MISMATCH],
    ['a password that breaks the rule', CURRENT, 'Abc-123', 'Abc-123', RULE],
    ['the current password again', CURRENT, CURRENT, CURRENT, SAME],
  ])(
    'says what is wrong with %s and posts nothing',
    async (_, current, replacement, repeated, message) => {
      const changed = viewAnswering(Promise.resolve('changed'))

      await change(current, replacement, repeated)

      const messages = shownMessages()
      expect({ messages, posted: changed.posted }).toEqual({
        messages: [message],
        posted: [],
      })
    },
  )

  it.each([
    ['a wrong current password', 'wrong-current-password', WRONG_CURRENT],
    ['a password that breaks the rule', 'breaks-the-rule', RULE],
    ['the current password again', 'same-as-before', SAME],
  ] as const)(
    'says what the service found wrong with %s and stays',
    async (_, answer, message) => {
      const changed = viewAnswering(Promise.resolve(answer))

      await change(CURRENT, NEW_PASSWORD)

      const messages = shownMessages()
      expect({ messages, arrived: changed.arrived }).toEqual({
        messages: [message],
        arrived: [],
      })
    },
  )

  it('says in German that the data could not be loaded when the request fails', async () => {
    const changed = viewAnswering(Promise.reject(new Error('Failed to fetch')))

    await change(CURRENT, NEW_PASSWORD)

    const messages = shownMessages()
    expect({ messages, arrived: changed.arrived }).toEqual({
      messages: [FAILED],
      arrived: [],
    })
  })

  it('sends the browser to the login page with its own address as the way back when the login has ended', async () => {
    window.history.replaceState(null, '', '/password')
    const changed = viewAnswering(Promise.reject(new Unauthorized()))

    await change(CURRENT, NEW_PASSWORD)

    const messages = shownMessages()
    expect({ navigatedTo: changed.navigatedTo, messages }).toEqual({
      navigatedTo: ['/login?next=%2Fpassword'],
      messages: ['Weiter zur Anmeldung…'],
    })
  })

  it('hands a login without an account over to the page it came from, with no failure text', async () => {
    const changed = viewAnswering(Promise.resolve('no-account'))

    await change(CURRENT, NEW_PASSWORD)

    const messages = shownMessages()
    expect({ withoutAccount: changed.withoutAccount, messages }).toEqual({
      withoutAccount: [1],
      messages: [],
    })
  })

  it('keeps the button to save disabled while the request is on its way', async () => {
    const answer = pending()
    viewAnswering(answer.promise)

    await change(CURRENT, NEW_PASSWORD)

    const disabledWhileWaiting = saveButton().disabled
    answer.settle('wrong-current-password')
    await act(async () => undefined)
    const disabledAfterwards = saveButton().disabled
    expect({ disabledWhileWaiting, disabledAfterwards }).toEqual({
      disabledWhileWaiting: true,
      disabledAfterwards: false,
    })
  })

  it('sends a second click on the button to save while the request is on its way nowhere', async () => {
    const answer = pending()
    const changed = viewAnswering(answer.promise)

    await change(CURRENT, NEW_PASSWORD)
    fireEvent.click(saveButton())
    await act(async () => undefined)

    expect(changed.posted).toHaveLength(1)
  })

  it('says nothing before the first attempt', () => {
    viewAnswering(Promise.resolve('changed'))

    const messages = shownMessages()

    expect(messages).toEqual([])
  })
})
