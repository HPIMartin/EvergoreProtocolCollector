import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import type { ProtocolApi } from '../api'
import { Forbidden, RequestFailed, Unauthorized } from '../api'
import type { Account, AdminStatus } from '../domain'
import { LOAD_FAILED_MESSAGE } from '../ui'

import { AdminView } from './AdminView.tsx'
import { LoginNavigation } from './LoginRequired.tsx'

afterEach(cleanup)

const NO_FACTS: AdminStatus = {
  lastUpdated: null,
  lastSuccessfulScrape: null,
  lastScrapeFailure: null,
  lastSuccessfulRecompute: null,
  lastRecomputeFailure: null,
  unknownItemNames: [],
  failedAvatarNames: [],
  roundTrips: [],
  roundTripAbstentions: [],
}

function apiRefusingTheAdminStatus(reason: Error): ProtocolApi {
  const refused = (): Promise<never> => Promise.reject(reason)

  return {
    overview: refused,
    adminStatus: refused,
    adminAccounts: refused,
    resetPassword: refused,
    bankEntries: refused,
    storageEntries: refused,
  }
}

const unused = (): Promise<never> => Promise.reject(new Error('unused'))

function apiListing(
  accounts: Account[],
  resetPassword: (name: string) => Promise<string> = unused,
): ProtocolApi {
  return {
    overview: unused,
    adminStatus: () => Promise.resolve(NO_FACTS),
    adminAccounts: () => Promise.resolve(accounts),
    resetPassword,
    bankEntries: unused,
    storageEntries: unused,
  }
}

interface RecordedResets {
  readonly names: string[]
  readonly reset: (name: string) => Promise<string>
}

function resettingTo(issued: Record<string, string>): RecordedResets {
  const names: string[] = []

  return {
    names,
    reset: (name) => {
      names.push(name)

      return Promise.resolve(issued[name] ?? '')
    },
  }
}

function rowOf(name: string): HTMLElement {
  const row = screen
    .getAllByTestId('account-row')
    .find(
      (candidate) =>
        within(candidate).getByTestId('account-name').textContent === name,
    )
  if (row === undefined) {
    throw new Error(`no row for ${name}`)
  }

  return row
}

async function adminViewWith(api: ProtocolApi): Promise<void> {
  render(<AdminView api={api} />)
  await act(async () => undefined)
}

function shownMessages(): string[] {
  return screen
    .queryAllByTestId('status-panel')
    .map((panel) => panel.textContent ?? '')
}

function accountRows(): string[][] {
  return screen
    .getAllByTestId('account-row')
    .map((row) => [
      within(row).getByTestId('account-name').textContent ?? '',
      within(row).getByTestId('account-rests').textContent ?? '',
      within(row).getByTestId('account-last-login').textContent ?? '',
    ])
}

const ALRIK: Account = {
  name: 'Alrik',
  rests: false,
  lastLogin: new Date('2026-12-31T11:00:00Z'),
}
const ZWERG: Account = { name: 'Zwerg', rests: true, lastLogin: null }

describe('AdminView', () => {
  it('says the page is reserved for the admin when the service refuses the role', async () => {
    await adminViewWith(apiRefusingTheAdminStatus(new Forbidden()))

    const messages = shownMessages()

    expect(messages).toEqual(['Diese Seite ist dem Admin vorbehalten.'])
  })

  it('keeps the heading of the page while it says so', async () => {
    await adminViewWith(apiRefusingTheAdminStatus(new Forbidden()))

    expect(screen.getByTestId('view-title')).toHaveProperty(
      'textContent',
      'Admin',
    )
  })

  it('headlines the account list „Konten“', async () => {
    await adminViewWith(apiListing([ALRIK]))

    const heading = screen.getByTestId('accounts-title').textContent

    expect(heading).toBe('Konten')
  })

  it('heads the account table with Name, Ruht and Letzte Anmeldung', async () => {
    await adminViewWith(apiListing([ALRIK]))

    const heads = within(screen.getByTestId('accounts'))
      .getAllByRole('columnheader')
      .map((head) => head.textContent)

    expect(heads).toEqual(['Name', 'Ruht', 'Letzte Anmeldung', ''])
  })

  it('names the column of the buttons „Aktion“ for assistive technology without showing a text', async () => {
    await adminViewWith(apiListing([ALRIK]))

    const heads = within(screen.getByTestId('accounts')).getAllByRole(
      'columnheader',
    )

    const labels = heads.map((head) => head.getAttribute('aria-label'))
    expect(labels).toEqual([null, null, null, 'Aktion'])
  })

  it('lists a resting account as ruht and an account never logged in as noch nie', async () => {
    await adminViewWith(apiListing([ALRIK, ZWERG]))

    const rows = accountRows()

    expect(rows).toEqual([
      ['Alrik', '', '31.12.2026 12:00'],
      ['Zwerg', 'ruht', 'noch nie'],
    ])
  })

  it('keeps the order in which the service listed the accounts', async () => {
    await adminViewWith(apiListing([ZWERG, ALRIK]))

    const names = accountRows().map(([name]) => name)

    expect(names).toEqual(['Zwerg', 'Alrik'])
  })
})

describe('the reset of a password on the admin page', () => {
  it('offers „Passwort zurücksetzen“ beside each account', async () => {
    await adminViewWith(apiListing([ALRIK, ZWERG]))

    const buttons = screen
      .getAllByTestId('account-row')
      .map((row) => within(row).getByTestId('account-reset').textContent)

    expect(buttons).toEqual(['Passwort zurücksetzen', 'Passwort zurücksetzen'])
  })

  it('asks on the page itself before it resets', async () => {
    const resets = resettingTo({})
    await adminViewWith(apiListing([ALRIK, ZWERG], resets.reset))

    fireEvent.click(within(rowOf('Zwerg')).getByTestId('account-reset'))

    const asked = {
      question: within(rowOf('Zwerg')).getByTestId('reset-question')
        .textContent,
      confirm: within(rowOf('Zwerg')).getByTestId('reset-confirm').textContent,
      cancel: within(rowOf('Zwerg')).getByTestId('reset-cancel').textContent,
      questionsAtAlrik: within(rowOf('Alrik')).queryAllByTestId(
        'reset-question',
      ),
      reset: resets.names,
    }
    expect(asked).toEqual({
      question: 'Passwort von Zwerg wirklich zurücksetzen?',
      confirm: 'Zurücksetzen',
      cancel: 'Abbrechen',
      questionsAtAlrik: [],
      reset: [],
    })
  })

  it('puts the question in a polite live region so a screen reader reads it out', async () => {
    await adminViewWith(apiListing([ALRIK]))

    fireEvent.click(within(rowOf('Alrik')).getByTestId('account-reset'))

    const region = within(rowOf('Alrik'))
      .getByTestId('reset-question')
      .closest('[aria-live]')
    const announcing = region?.getAttribute('aria-live')
    expect(announcing).toBe('polite')
  })

  it('moves the focus to the reset button of the account when the admin answers „Abbrechen“', async () => {
    await adminViewWith(apiListing([ALRIK, ZWERG]))
    fireEvent.click(within(rowOf('Zwerg')).getByTestId('account-reset'))

    fireEvent.click(within(rowOf('Zwerg')).getByTestId('reset-cancel'))

    const focused = document.activeElement
    expect(focused).toBe(within(rowOf('Zwerg')).getByTestId('account-reset'))
  })

  it('moves the focus to the reset button of the account when the admin answers „Zurücksetzen“', async () => {
    await adminViewWith(apiListing([ALRIK, ZWERG], resettingTo(ISSUED).reset))

    await resetThrough('Zwerg')

    const focused = document.activeElement
    expect(focused).toBe(within(rowOf('Zwerg')).getByTestId('account-reset'))
  })

  it('resets nothing and asks no more when the admin answers „Abbrechen“', async () => {
    const resets = resettingTo({})
    await adminViewWith(apiListing([ALRIK, ZWERG], resets.reset))
    fireEvent.click(within(rowOf('Alrik')).getByTestId('account-reset'))

    fireEvent.click(within(rowOf('Alrik')).getByTestId('reset-cancel'))

    const afterwards = {
      reset: resets.names,
      questions: screen.queryAllByTestId('reset-question'),
      offered: within(rowOf('Alrik')).getByTestId('account-reset').textContent,
    }
    expect(afterwards).toEqual({
      reset: [],
      questions: [],
      offered: 'Passwort zurücksetzen',
    })
  })
})

const ISSUED = { Alrik: 'Tannenzapfen#47', Zwerg: 'Birkenfalter#18' }

async function resetThrough(name: string): Promise<void> {
  fireEvent.click(within(rowOf(name)).getByTestId('account-reset'))
  fireEvent.click(within(rowOf(name)).getByTestId('reset-confirm'))
  await act(async () => undefined)
}

function issuedBeside(name: string): string | undefined {
  return within(rowOf(name)).queryByTestId('account-issued')?.textContent
}

describe('the password a reset issued on the admin page', () => {
  it('stands in a polite live region so a screen reader reads it out', async () => {
    await adminViewWith(apiListing([ALRIK], resettingTo(ISSUED).reset))

    await resetThrough('Alrik')

    const region = within(rowOf('Alrik'))
      .getByTestId('account-issued')
      .closest('[aria-live]')
    const announcing = region?.getAttribute('aria-live')
    expect(announcing).toBe('polite')
  })

  it('stands beside the name of the account after „Zurücksetzen“', async () => {
    const resets = resettingTo(ISSUED)
    await adminViewWith(apiListing([ALRIK, ZWERG], resets.reset))

    await resetThrough('Zwerg')

    const shown = {
      reset: resets.names,
      besideZwerg: issuedBeside('Zwerg'),
      besideAlrik: issuedBeside('Alrik'),
      questions: screen.queryAllByTestId('reset-question'),
    }
    expect(shown).toEqual({
      reset: ['Zwerg'],
      besideZwerg: 'Birkenfalter#18',
      besideAlrik: undefined,
      questions: [],
    })
  })

  it('stays beside its own name when the admin resets another account', async () => {
    await adminViewWith(apiListing([ALRIK, ZWERG], resettingTo(ISSUED).reset))
    await resetThrough('Alrik')

    await resetThrough('Zwerg')

    const shown = {
      besideAlrik: issuedBeside('Alrik'),
      besideZwerg: issuedBeside('Zwerg'),
    }
    expect(shown).toEqual({
      besideAlrik: 'Tannenzapfen#47',
      besideZwerg: 'Birkenfalter#18',
    })
  })

  it('is gone when the admin leaves the page and comes back', async () => {
    const api = apiListing([ALRIK, ZWERG], resettingTo(ISSUED).reset)
    await adminViewWith(api)
    await resetThrough('Alrik')
    cleanup()

    await adminViewWith(api)

    const shown = {
      besideAlrik: issuedBeside('Alrik'),
      besideZwerg: issuedBeside('Zwerg'),
    }
    expect(shown).toEqual({ besideAlrik: undefined, besideZwerg: undefined })
  })

  it('is shown for no account when the reset failed, which says so instead', async () => {
    const failing = (): Promise<string> =>
      Promise.reject(new RequestFailed(404))
    await adminViewWith(apiListing([ALRIK, ZWERG], failing))

    await resetThrough('Alrik')

    const shown = {
      messages: shownMessages(),
      besideAlrik: issuedBeside('Alrik'),
      besideZwerg: issuedBeside('Zwerg'),
      questions: screen.queryAllByTestId('reset-question'),
    }
    expect(shown).toEqual({
      messages: [LOAD_FAILED_MESSAGE],
      besideAlrik: undefined,
      besideZwerg: undefined,
      questions: [],
    })
  })

  it('sends the admin to the login page with its own address as the way back when the reset is refused with 401', async () => {
    window.history.replaceState(null, '', '/admin')
    const navigatedTo: string[] = []
    const unauthorized = (): Promise<string> =>
      Promise.reject(new Unauthorized())
    render(
      <LoginNavigation.Provider
        value={(target) => {
          navigatedTo.push(target)
        }}
      >
        <AdminView api={apiListing([ALRIK], unauthorized)} />
      </LoginNavigation.Provider>,
    )
    await act(async () => undefined)

    await resetThrough('Alrik')

    const shown = { navigatedTo, messages: shownMessages() }
    expect(shown).toEqual({
      navigatedTo: ['/login?next=%2Fadmin'],
      messages: ['Weiter zur Anmeldung…'],
    })
  })

  it('says the page is reserved for the admin when the reset is refused with 403', async () => {
    const forbidden = (): Promise<string> => Promise.reject(new Forbidden())
    await adminViewWith(apiListing([ALRIK], forbidden))

    await resetThrough('Alrik')

    const messages = shownMessages()
    expect(messages).toEqual(['Diese Seite ist dem Admin vorbehalten.'])
  })

  it('stops saying so when the next reset succeeds', async () => {
    const answers = [
      () => Promise.reject(new RequestFailed(500)),
      () => Promise.resolve('Tannenzapfen#47'),
    ]
    const reset = (): Promise<string> => answers.shift()?.() ?? unused()
    await adminViewWith(apiListing([ALRIK], reset))
    await resetThrough('Alrik')
    const failed = shownMessages()

    await resetThrough('Alrik')

    const shown = {
      failed,
      afterwards: shownMessages(),
      beside: issuedBeside('Alrik'),
    }
    expect(shown).toEqual({
      failed: [LOAD_FAILED_MESSAGE],
      afterwards: [],
      beside: 'Tannenzapfen#47',
    })
  })

  it('is removed from beside the account when the next reset of it fails', async () => {
    const answers = [
      () => Promise.resolve('Tannenzapfen#47'),
      () => Promise.reject(new RequestFailed(500)),
    ]
    const reset = (): Promise<string> => answers.shift()?.() ?? unused()
    await adminViewWith(apiListing([ALRIK], reset))
    await resetThrough('Alrik')

    await resetThrough('Alrik')

    const shown = { beside: issuedBeside('Alrik'), messages: shownMessages() }
    expect(shown).toEqual({
      beside: undefined,
      messages: [LOAD_FAILED_MESSAGE],
    })
  })

  it('says beside the account whose reset failed so, and still when another account is reset', async () => {
    const answers = [
      () => Promise.reject(new RequestFailed(500)),
      () => Promise.resolve('Birkenfalter#18'),
    ]
    const reset = (): Promise<string> => answers.shift()?.() ?? unused()
    await adminViewWith(apiListing([ALRIK, ZWERG], reset))
    await resetThrough('Alrik')

    await resetThrough('Zwerg')

    const shown = {
      besideAlrik: within(rowOf('Alrik'))
        .queryAllByTestId('status-panel')
        .map((panel) => panel.textContent),
      besideZwerg: within(rowOf('Zwerg')).queryAllByTestId('status-panel'),
      issuedBesideZwerg: issuedBeside('Zwerg'),
    }
    expect(shown).toEqual({
      besideAlrik: [LOAD_FAILED_MESSAGE],
      besideZwerg: [],
      issuedBesideZwerg: 'Birkenfalter#18',
    })
  })

  it('shows the password of the latest reset of an account when the answers arrive in the other order', async () => {
    const answers: ((password: string) => void)[] = []
    const reset = (): Promise<string> =>
      new Promise((resolve) => {
        answers.push(resolve)
      })
    await adminViewWith(apiListing([ALRIK], reset))
    await resetThrough('Alrik')
    await resetThrough('Alrik')

    await act(async () => {
      answers[1]?.('Zweites#22')
      answers[0]?.('Erstes#11')
    })

    const shown = issuedBeside('Alrik')
    expect(shown).toBe('Zweites#22')
  })

  it('shows no failure when an earlier reset of the account fails after the latest one succeeded', async () => {
    const rejections: ((reason: Error) => void)[] = []
    const answers = [
      () =>
        new Promise<string>((_resolve, reject) => {
          rejections.push(reject)
        }),
      () => Promise.resolve('Zweites#22'),
    ]
    const reset = (): Promise<string> => answers.shift()?.() ?? unused()
    await adminViewWith(apiListing([ALRIK], reset))
    await resetThrough('Alrik')
    await resetThrough('Alrik')

    await act(async () => {
      rejections[0]?.(new RequestFailed(500))
    })

    const shown = { beside: issuedBeside('Alrik'), messages: shownMessages() }
    expect(shown).toEqual({ beside: 'Zweites#22', messages: [] })
  })

  it('is replaced by the one a second reset of the same account issues', async () => {
    const issued = ['Erstes#11', 'Zweites#22']
    const reset = (): Promise<string> => Promise.resolve(issued.shift() ?? '')
    await adminViewWith(apiListing([ALRIK], reset))
    await resetThrough('Alrik')

    await resetThrough('Alrik')

    const shown = issuedBeside('Alrik')
    expect(shown).toBe('Zweites#22')
  })
})
