import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { ProtocolApi } from '../api'
import {
  CollectionRefused,
  Forbidden,
  RequestFailed,
  Unauthorized,
} from '../api'
import type { Account, AdminStatus, Role, Usage, UsageCounts } from '../domain'
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
  collectionRunningSince: null,
  nextCollectionFrom: null,
}

const NOTHING_USED: UsageCounts = {
  logins: 0,
  pageLoads: 0,
  logouts: 0,
  daysUsed: 0,
  latestUse: null,
}

const NO_USAGE: Usage = {
  lastThirtyDays: { accounts: [], guild: NOTHING_USED, adminKey: NOTHING_USED },
  allTheTime: { accounts: [], guild: NOTHING_USED, adminKey: NOTHING_USED },
  recordedSince: null,
  lostSinceStart: 0,
}

function apiRefusingTheAdminStatus(reason: Error): ProtocolApi {
  const refused = (): Promise<never> => Promise.reject(reason)

  return {
    overview: refused,
    adminStatus: refused,
    adminAccounts: refused,
    adminUsage: refused,
    resetPassword: refused,
    setRole: refused,
    startCollection: refused,
    bankEntries: refused,
    storageEntries: refused,
  }
}

const unused = (): Promise<never> => Promise.reject(new Error('unused'))

function apiListing(
  accounts: Account[],
  resetPassword: (name: string) => Promise<string> = unused,
  setRole: (name: string, role: Role) => Promise<void> = unused,
): ProtocolApi {
  return {
    overview: unused,
    adminStatus: () => Promise.resolve(NO_FACTS),
    adminAccounts: () => Promise.resolve(accounts),
    adminUsage: () => Promise.resolve(NO_USAGE),
    resetPassword,
    setRole,
    startCollection: unused,
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

function shownRoleOf(row: HTMLElement): string | null {
  const field = within(row).getByTestId('account-role')

  return field instanceof HTMLSelectElement
    ? (field.selectedOptions[0]?.textContent ?? null)
    : field.textContent
}

async function adminViewWith(api: ProtocolApi): Promise<void> {
  render(<AdminView api={api} onFollow={() => undefined} />)
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
  role: 'member',
  own: false,
  rests: false,
  lastLogin: new Date('2026-12-31T11:00:00Z'),
}
const ZWERG: Account = {
  name: 'Zwerg',
  role: 'member',
  own: false,
  rests: true,
  lastLogin: null,
}

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

  it('shows the usage of the app below the accounts', async () => {
    await adminViewWith(apiListing([ALRIK]))

    const heading = screen.getByTestId('usage-title').textContent
    const order = screen
      .getAllByTestId(/^(accounts|usage-title)$/)
      .map((element) => element.dataset['testid'])

    expect({ heading, order }).toStrictEqual({
      heading: 'Nutzung',
      order: ['accounts', 'usage-title'],
    })
  })

  it('keeps the status and the accounts standing when the usage fails to load', async () => {
    await adminViewWith({
      ...apiListing([ALRIK]),
      adminUsage: () => Promise.reject(new RequestFailed(500)),
    })

    const shown = {
      lastUpdated: screen.getByTestId('last-updated').textContent,
      names: accountRows().map(([name]) => name),
      messages: shownMessages(),
    }

    expect(shown).toStrictEqual({
      lastUpdated: 'Stand: noch kein Abgleich gelaufen',
      names: ['Alrik'],
      messages: [LOAD_FAILED_MESSAGE],
    })
  })

  it('headlines the account list „Konten“', async () => {
    await adminViewWith(apiListing([ALRIK]))

    const heading = screen.getByTestId('accounts-title').textContent

    expect(heading).toBe('Konten')
  })

  it('heads the account table with Name, Rolle, Ruht and Letzte Anmeldung', async () => {
    await adminViewWith(apiListing([ALRIK]))

    const heads = within(screen.getByTestId('accounts'))
      .getAllByRole('columnheader')
      .map((head) => head.textContent)

    expect(heads).toEqual(['Name', 'Rolle', 'Ruht', 'Letzte Anmeldung', ''])
  })

  it('shows Mitglied or Admin under Rolle for each account', async () => {
    await adminViewWith(apiListing([ALRIK, { ...ZWERG, role: 'admin' }]))

    const roles = screen.getAllByTestId('account-row').map(shownRoleOf)

    expect(roles).toEqual(['Mitglied', 'Admin'])
  })

  it("offers Mitglied and Admin under Rolle beside every account but the viewer's own", async () => {
    const own: Account = { ...ZWERG, role: 'admin', own: true }
    await adminViewWith(apiListing([ALRIK, own]))

    const field = within(rowOf('Alrik')).getByRole('combobox', {
      name: 'Rolle von Alrik',
    })
    const options = Array.from(field.querySelectorAll('option')).map(
      (option) => option.textContent,
    )
    const atTheOwn = within(rowOf('Zwerg')).queryAllByRole('combobox')

    expect({ options, atTheOwn }).toEqual({
      options: ['Mitglied', 'Admin'],
      atTheOwn: [],
    })
  })

  it('applies a chosen role at once, asks no question and shows it once answered', async () => {
    const chosen: [string, Role][] = []
    const setRole = (name: string, role: Role): Promise<void> => {
      chosen.push([name, role])

      return Promise.resolve()
    }
    await adminViewWith(apiListing([ALRIK], unused, setRole))

    await act(async () => {
      fireEvent.change(within(rowOf('Alrik')).getByTestId('account-role'), {
        target: { value: 'admin' },
      })
    })

    const shown = shownRoleOf(rowOf('Alrik'))
    const questions = screen.queryAllByTestId('reset-question')
    expect({ chosen, shown, questions }).toEqual({
      chosen: [['Alrik', 'admin']],
      shown: 'Admin',
      questions: [],
    })
  })

  it('offers no second change of a role while one is in flight', async () => {
    const setRole = (): Promise<void> => new Promise(() => undefined)
    await adminViewWith(apiListing([ALRIK, ZWERG], unused, setRole))

    await act(async () => {
      fireEvent.change(within(rowOf('Alrik')).getByTestId('account-role'), {
        target: { value: 'admin' },
      })
    })

    const atAlrik = within(rowOf('Alrik')).getByTestId('account-role')
    const atZwerg = within(rowOf('Zwerg')).getByTestId('account-role')
    const alrikDisabled = atAlrik.hasAttribute('disabled')
    const zwergDisabled = atZwerg.hasAttribute('disabled')
    expect({ alrikDisabled, zwergDisabled }).toEqual({
      alrikDisabled: true,
      zwergDisabled: false,
    })
  })

  it('keeps the held role while the change is in flight', async () => {
    const setRole = (): Promise<void> => new Promise(() => undefined)
    await adminViewWith(apiListing([ALRIK], unused, setRole))

    await act(async () => {
      fireEvent.change(within(rowOf('Alrik')).getByTestId('account-role'), {
        target: { value: 'admin' },
      })
    })

    const held = shownRoleOf(rowOf('Alrik'))
    expect(held).toBe('Mitglied')
  })

  it('offers the field again once the change is answered', async () => {
    const setRole = (): Promise<void> => Promise.resolve()
    await adminViewWith(apiListing([ALRIK], unused, setRole))

    await act(async () => {
      fireEvent.change(within(rowOf('Alrik')).getByTestId('account-role'), {
        target: { value: 'admin' },
      })
    })

    const field = within(rowOf('Alrik')).getByTestId('account-role')
    const disabled = field.hasAttribute('disabled')
    expect(disabled).toBe(false)
  })

  it('sends the admin to the login page with its own address as the way back when a change of role is refused with 401', async () => {
    window.history.replaceState(null, '', '/admin')
    const navigatedTo: string[] = []
    const unauthorized = (): Promise<void> => Promise.reject(new Unauthorized())
    render(
      <LoginNavigation.Provider
        value={(target) => {
          navigatedTo.push(target)
        }}
      >
        <AdminView
          api={apiListing([ALRIK], unused, unauthorized)}
          onFollow={() => undefined}
        />
      </LoginNavigation.Provider>,
    )
    await act(async () => undefined)

    await act(async () => {
      fireEvent.change(within(rowOf('Alrik')).getByTestId('account-role'), {
        target: { value: 'admin' },
      })
    })

    const messages = shownMessages()
    expect({ navigatedTo, messages }).toEqual({
      navigatedTo: ['/login?next=%2Fadmin'],
      messages: ['Weiter zur Anmeldung…'],
    })
  })

  it('says the page is reserved for the admin when a change of role is refused with 403', async () => {
    const forbidden = (): Promise<void> => Promise.reject(new Forbidden())
    await adminViewWith(apiListing([ALRIK], unused, forbidden))

    await act(async () => {
      fireEvent.change(within(rowOf('Alrik')).getByTestId('account-role'), {
        target: { value: 'admin' },
      })
    })

    const messages = shownMessages()
    expect(messages).toEqual(['Diese Seite ist dem Admin vorbehalten.'])
  })

  it('says so when a change of role fails and keeps the role held', async () => {
    const failing = (): Promise<void> => Promise.reject(new RequestFailed(500))
    await adminViewWith(apiListing([ALRIK, ZWERG], unused, failing))

    await act(async () => {
      fireEvent.change(within(rowOf('Alrik')).getByTestId('account-role'), {
        target: { value: 'admin' },
      })
    })

    const besideAlrik = within(rowOf('Alrik'))
      .queryAllByTestId('status-panel')
      .map((panel) => panel.textContent)
    const besideZwerg = within(rowOf('Zwerg')).queryAllByTestId('status-panel')
    const held = shownRoleOf(rowOf('Alrik'))
    expect({ besideAlrik, besideZwerg, held }).toEqual({
      besideAlrik: [LOAD_FAILED_MESSAGE],
      besideZwerg: [],
      held: 'Mitglied',
    })
  })

  it('offers the field again once a change of role has failed', async () => {
    const failing = (): Promise<void> => Promise.reject(new RequestFailed(500))
    await adminViewWith(apiListing([ALRIK], unused, failing))

    await act(async () => {
      fireEvent.change(within(rowOf('Alrik')).getByTestId('account-role'), {
        target: { value: 'admin' },
      })
    })

    const field = within(rowOf('Alrik')).getByTestId('account-role')
    const disabled = field.hasAttribute('disabled')
    expect(disabled).toBe(false)
  })

  it('stops saying a change of role failed once the next change is chosen', async () => {
    const answers = [
      (): Promise<void> => Promise.reject(new RequestFailed(500)),
      (): Promise<void> => new Promise(() => undefined),
    ]
    const setRole = (): Promise<void> => answers.shift()?.() ?? unused()
    await adminViewWith(apiListing([ALRIK], unused, setRole))
    await act(async () => {
      fireEvent.change(within(rowOf('Alrik')).getByTestId('account-role'), {
        target: { value: 'admin' },
      })
    })
    const field = within(rowOf('Alrik')).getByTestId('account-role')
    const enabledBefore = !field.hasAttribute('disabled')
    const messagesBefore = shownMessages()

    await act(async () => {
      fireEvent.change(field, { target: { value: 'admin' } })
    })

    const messages = shownMessages()
    expect({ enabledBefore, messagesBefore, messages }).toEqual({
      enabledBefore: true,
      messagesBefore: [LOAD_FAILED_MESSAGE],
      messages: [],
    })
  })

  it("shows the own account's role without a choice", async () => {
    const own: Account = { ...ZWERG, role: 'admin', own: true }
    await adminViewWith(apiListing([ALRIK, own]))

    const field = within(rowOf('Zwerg')).getByTestId('account-role')

    expect({ tag: field.tagName, text: field.textContent }).toEqual({
      tag: 'SPAN',
      text: 'Admin',
    })
  })

  it('names the column of the buttons „Aktion“ for assistive technology without showing a text', async () => {
    await adminViewWith(apiListing([ALRIK]))

    const heads = within(screen.getByTestId('accounts')).getAllByRole(
      'columnheader',
    )

    const labels = heads.map((head) => head.getAttribute('aria-label'))
    expect(labels).toEqual([null, null, null, null, 'Aktion'])
  })

  it('lists a resting account as ruht and an account never logged in as noch nie', async () => {
    await adminViewWith(apiListing([ALRIK, ZWERG]))

    const rows = accountRows()

    expect(rows).toEqual([
      ['Alrik', '', '31.12.2026 12:00'],
      ['Zwerg', 'ruht', 'noch nie'],
    ])
  })

  it('lists no rows and shows no error when the service lists no account', async () => {
    await adminViewWith(apiListing([]))

    const shown = {
      rows: screen.queryAllByTestId('account-row'),
      messages: shownMessages(),
    }
    expect(shown).toEqual({ rows: [], messages: [] })
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

  it('moves the focus to „Zurücksetzen“ of the account when the question opens', async () => {
    await adminViewWith(apiListing([ALRIK, ZWERG]))

    fireEvent.click(within(rowOf('Zwerg')).getByTestId('account-reset'))

    const focused = document.activeElement
    expect(focused).toBe(within(rowOf('Zwerg')).getByTestId('reset-confirm'))
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
        <AdminView
          api={apiListing([ALRIK], unauthorized)}
          onFollow={() => undefined}
        />
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

  it('is replaced by the one a second reset of the same account issues', async () => {
    const issued = ['Erstes#11', 'Zweites#22']
    const reset = (): Promise<string> => Promise.resolve(issued.shift() ?? '')
    await adminViewWith(apiListing([ALRIK], reset))
    await resetThrough('Alrik')

    await resetThrough('Alrik')

    const shown = issuedBeside('Alrik')
    expect(shown).toBe('Zweites#22')
  })

  it('is removed from beside the account as soon as the next reset of it starts', async () => {
    const answers = [
      () => Promise.resolve('Erstes#11'),
      () => new Promise<string>(() => undefined),
    ]
    const reset = (): Promise<string> => answers.shift()?.() ?? unused()
    await adminViewWith(apiListing([ALRIK], reset))
    await resetThrough('Alrik')

    await resetThrough('Alrik')

    const shown = { beside: issuedBeside('Alrik'), messages: shownMessages() }
    expect(shown).toEqual({ beside: undefined, messages: [] })
  })

  it.each(['constructor', '__proto__', 'toString'])(
    'stands beside an account named %s without a failure shown before or after its reset',
    async (name) => {
      const account: Account = {
        name,
        role: 'member',
        own: false,
        rests: false,
        lastLogin: null,
      }
      await adminViewWith(apiListing([account], () => Promise.resolve('Pw#1')))
      const before = shownMessages()

      await resetThrough(name)

      const shown = {
        before,
        beside: issuedBeside(name),
        after: shownMessages(),
      }
      expect(shown).toEqual({ before: [], beside: 'Pw#1', after: [] })
    },
  )

  it('offers no second reset of an account while its reset is in flight', async () => {
    const reset = (): Promise<string> => new Promise(() => undefined)
    await adminViewWith(apiListing([ALRIK, ZWERG], reset))

    await resetThrough('Alrik')

    const offered = {
      atAlrik: within(rowOf('Alrik')).queryAllByTestId('account-reset'),
      atZwerg: within(rowOf('Zwerg')).queryAllByTestId('account-reset').length,
    }
    expect(offered).toEqual({ atAlrik: [], atZwerg: 1 })
  })
})

function apiReporting(status: AdminStatus): ProtocolApi {
  return { ...apiListing([]), adminStatus: () => Promise.resolve(status) }
}

interface ShownFindings {
  readonly paragraph: string | null
  readonly entries: readonly {
    readonly text: string | null
    readonly links: readonly (readonly [string | null, string | null])[]
  }[]
}

function findingsIn(listTestId: string): ShownFindings {
  const list = screen.getByTestId(listTestId)

  return {
    paragraph: list.textContent,
    entries: within(list)
      .getAllByTestId('finding')
      .map((entry) => ({
        text: entry.textContent,
        links: within(entry)
          .queryAllByRole('link')
          .map((link) => [link.textContent, link.getAttribute('href')]),
      })),
  }
}

describe('the findings of the last recompute on the admin page', () => {
  it("leads the name in each suspected round trip, and only the name, to that member's storage ledger", async () => {
    const roundTrips = [
      { avatar: 'Hans Meyer', item: 'Federn', quantity: 1 },
      { avatar: 'Ärger', item: 'Bolzen', quantity: 100 },
      { avatar: 'Ärger', item: 'Pfeile', quantity: 100 },
    ]
    await adminViewWith(apiReporting({ ...NO_FACTS, roundTrips }))

    const shown = findingsIn('round-trips')

    expect(shown).toEqual({
      paragraph:
        'Verdacht auf Warenkreislauf: Hans Meyer: 1 × Federn, Ärger: 100 × Bolzen, Ärger: 100 × Pfeile',
      entries: [
        {
          text: 'Hans Meyer: 1 × Federn',
          links: [['Hans Meyer', '/avatars/Hans%20Meyer/storage']],
        },
        {
          text: 'Ärger: 100 × Bolzen',
          links: [['Ärger', '/avatars/%C3%84rger/storage']],
        },
        {
          text: 'Ärger: 100 × Pfeile',
          links: [['Ärger', '/avatars/%C3%84rger/storage']],
        },
      ],
    })
  })

  it("leads the name in each pair not judgeable, and only the name, to that member's storage ledger", async () => {
    const roundTripAbstentions = [{ avatar: 'Brynja', item: 'Kristallat' }]
    await adminViewWith(apiReporting({ ...NO_FACTS, roundTripAbstentions }))

    const shown = findingsIn('round-trip-abstentions')

    expect(shown).toEqual({
      paragraph: 'Nicht beurteilbar (Rezept ungelesen): Brynja: Kristallat',
      entries: [
        {
          text: 'Brynja: Kristallat',
          links: [['Brynja', '/avatars/Brynja/storage']],
        },
      ],
    })
  })

  it("leads each member whose figures could not be refreshed to that member's storage ledger", async () => {
    await adminViewWith(
      apiReporting({ ...NO_FACTS, failedAvatarNames: ['Calix'] }),
    )

    const shown = findingsIn('failed-avatar-names')

    expect(shown).toEqual({
      paragraph: 'Nicht aktualisierte Avatare: Calix',
      entries: [
        { text: 'Calix', links: [['Calix', '/avatars/Calix/storage']] },
      ],
    })
  })

  it('encodes a name that holds a reserved character in the address of its storage ledger', async () => {
    await adminViewWith(
      apiReporting({ ...NO_FACTS, failedAvatarNames: ['a/b?c#d%e'] }),
    )

    const shown = findingsIn('failed-avatar-names')

    expect(shown.entries).toEqual([
      {
        text: 'a/b?c#d%e',
        links: [['a/b?c#d%e', '/avatars/a%2Fb%3Fc%23d%25e/storage']],
      },
    ])
  })

  it('lets no unknown item lead anywhere', async () => {
    await adminViewWith(
      apiReporting({ ...NO_FACTS, unknownItemNames: ['Unobtainium'] }),
    )

    const shown = {
      paragraph: screen.getByTestId('unknown-item-names').textContent,
      links: screen.queryAllByRole('link'),
    }

    expect(shown).toEqual({
      paragraph: 'Unbekannte Items: Unobtainium',
      links: [],
    })
  })

  it('shows two identical findings, each under a key of its own', async () => {
    const errors = vi
      .spyOn(console, 'error')
      .mockImplementation(() => undefined)
    const same = { avatar: 'Ärger', item: 'Bolzen', quantity: 100 }
    await adminViewWith(
      apiReporting({ ...NO_FACTS, roundTrips: [same, { ...same }] }),
    )

    const shown = {
      entries: findingsIn('round-trips').entries.map((entry) => entry.text),
      complaints: errors.mock.calls.length,
    }

    errors.mockRestore()
    expect(shown).toEqual({
      entries: ['Ärger: 100 × Bolzen', 'Ärger: 100 × Bolzen'],
      complaints: 0,
    })
  })

  it('offers the start of a collection to the admin and to no login the admin status refuses', async () => {
    await adminViewWith(apiReporting(NO_FACTS))
    const toTheAdmin = shownCollection()
    cleanup()

    await adminViewWith(apiRefusingTheAdminStatus(new Forbidden()))
    const toAMember = shownCollection()

    expect({ toTheAdmin, toAMember }).toEqual({
      toTheAdmin: { state: null, button: 'Jetzt sammeln' },
      toAMember: { state: null, button: null },
    })
  })

  it('says since when a collection runs in place of the button', async () => {
    const collectionRunningSince = new Date('2026-01-02T12:00:00Z')
    await adminViewWith(apiReporting({ ...NO_FACTS, collectionRunningSince }))

    const shown = shownCollection()

    expect(shown).toEqual({
      state: 'Sammlung läuft seit 02.01.2026 13:00',
      button: null,
    })
  })

  it('says since when a collection runs and not from when the next start is possible when both are reported', async () => {
    const collectionRunningSince = new Date('2026-01-02T12:00:00Z')
    const nextCollectionFrom = new Date('2026-01-02T13:00:00Z')
    await adminViewWith(
      apiReporting({ ...NO_FACTS, collectionRunningSince, nextCollectionFrom }),
    )

    const shown = shownCollection()

    expect(shown).toEqual({
      state: 'Sammlung läuft seit 02.01.2026 13:00',
      button: null,
    })
  })

  it('says from when the next start is possible in place of the button', async () => {
    const nextCollectionFrom = new Date('2026-01-02T13:00:00Z')
    await adminViewWith(apiReporting({ ...NO_FACTS, nextCollectionFrom }))

    const shown = shownCollection()

    expect(shown).toEqual({
      state: 'Nächste Sammlung ab 02.01.2026 14:00 möglich.',
      button: null,
    })
  })
})

const RUNNING_SINCE_13_00 = {
  ...NO_FACTS,
  collectionRunningSince: new Date('2026-01-02T12:00:00Z'),
}

const NEXT_FROM_14_00 = {
  ...NO_FACTS,
  nextCollectionFrom: new Date('2026-01-02T13:00:00Z'),
}

interface StartingApi {
  readonly api: ProtocolApi
  readonly starts: () => number
}

function apiStarting(
  statusReads: readonly AdminStatus[],
  startCollection: () => Promise<void>,
): StartingApi {
  let starts = 0
  let reads = 0

  return {
    api: {
      ...apiListing([]),
      adminStatus: () =>
        Promise.resolve(
          statusReads[Math.min(reads++, statusReads.length - 1)] ?? NO_FACTS,
        ),
      startCollection: () => {
        starts++

        return startCollection()
      },
    },
    starts: () => starts,
  }
}

async function clickStart(): Promise<void> {
  await act(async () => {
    fireEvent.click(screen.getByTestId('start-collection'))
  })
}

function shownNotice(): string | null {
  return screen.queryByTestId('collection-notice')?.textContent ?? null
}

describe('starting a collection from the admin page', () => {
  it('asks the service once and then says since when the collection runs', async () => {
    const starting = apiStarting([NO_FACTS, RUNNING_SINCE_13_00], () =>
      Promise.resolve(),
    )
    await adminViewWith(starting.api)

    await clickStart()

    const starts = starting.starts()
    const shown = shownCollection()
    expect({ starts, shown }).toEqual({
      starts: 1,
      shown: { state: 'Sammlung läuft seit 02.01.2026 13:00', button: null },
    })
  })

  it('offers no second start while the first one is still asked for', async () => {
    let answer: () => void = () => undefined
    const pending = new Promise<void>((resolve) => {
      answer = resolve
    })
    const starting = apiStarting([NO_FACTS, RUNNING_SINCE_13_00], () => pending)
    await adminViewWith(starting.api)
    await clickStart()

    const button = screen.getByTestId('start-collection')
    await act(async () => {
      fireEvent.click(button)
    })
    const whileAsking = {
      starts: starting.starts(),
      disabled: button.hasAttribute('disabled'),
    }
    await act(async () => {
      answer()
    })

    const afterwards = shownCollection()
    expect({ whileAsking, afterwards }).toEqual({
      whileAsking: { starts: 1, disabled: true },
      afterwards: {
        state: 'Sammlung läuft seit 02.01.2026 13:00',
        button: null,
      },
    })
  })

  it('says a collection already runs when the service refuses the start for it, and since when', async () => {
    const starting = apiStarting([NO_FACTS, RUNNING_SINCE_13_00], () =>
      Promise.reject(new CollectionRefused('running')),
    )
    await adminViewWith(starting.api)

    await clickStart()

    const notice = shownNotice()
    const shown = shownCollection()
    expect({ notice, shown }).toEqual({
      notice: 'Es läuft bereits eine Sammlung.',
      shown: { state: 'Sammlung läuft seit 02.01.2026 13:00', button: null },
    })
  })

  it('drops the notice that a collection runs when the status read afterwards shows none', async () => {
    const starting = apiStarting([NO_FACTS, NO_FACTS], () =>
      Promise.reject(new CollectionRefused('running')),
    )
    await adminViewWith(starting.api)

    await clickStart()

    const notice = shownNotice()
    const shown = shownCollection()
    expect({ notice, shown }).toEqual({
      notice: null,
      shown: { state: null, button: 'Jetzt sammeln' },
    })
  })

  it('says from when a start is possible when the service refuses it as too early', async () => {
    const starting = apiStarting([NO_FACTS, NEXT_FROM_14_00], () =>
      Promise.reject(new CollectionRefused('too-early')),
    )
    await adminViewWith(starting.api)

    await clickStart()

    const notice = shownNotice()
    const shown = shownCollection()
    expect({ notice, shown }).toEqual({
      notice: null,
      shown: {
        state: 'Nächste Sammlung ab 02.01.2026 14:00 möglich.',
        button: null,
      },
    })
  })

  it('shows the refusal of the role when the start is refused with 403', async () => {
    const starting = apiStarting([NO_FACTS], () =>
      Promise.reject(new Forbidden()),
    )
    await adminViewWith(starting.api)

    await clickStart()

    const messages = shownMessages()
    expect(messages).toEqual(['Diese Seite ist dem Admin vorbehalten.'])
  })

  it('sends the admin to the login page with its own address as the way back when the start is refused with 401', async () => {
    window.history.replaceState(null, '', '/admin')
    const navigatedTo: string[] = []
    const starting = apiStarting([NO_FACTS], () =>
      Promise.reject(new Unauthorized()),
    )
    render(
      <LoginNavigation.Provider
        value={(target) => {
          navigatedTo.push(target)
        }}
      >
        <AdminView api={starting.api} onFollow={() => undefined} />
      </LoginNavigation.Provider>,
    )
    await act(async () => undefined)

    await clickStart()

    const messages = shownMessages()
    expect({ navigatedTo, messages }).toEqual({
      navigatedTo: ['/login?next=%2Fadmin'],
      messages: ['Weiter zur Anmeldung…'],
    })
  })

  it('says the data could not be loaded and keeps the button when the status read after an accepted start fails', async () => {
    let reads = 0
    const api: ProtocolApi = {
      ...apiStarting([NO_FACTS], () => Promise.resolve()).api,
      adminStatus: () =>
        ++reads === 1
          ? Promise.resolve(NO_FACTS)
          : Promise.reject(new RequestFailed(500)),
    }
    await adminViewWith(api)

    await clickStart()

    const notice = shownNotice()
    const shown = shownCollection()
    expect({ notice, shown }).toEqual({
      notice: LOAD_FAILED_MESSAGE,
      shown: { state: null, button: 'Jetzt sammeln' },
    })
  })

  it('says the start failed and keeps the button when the service fails otherwise', async () => {
    const starting = apiStarting([NO_FACTS], () =>
      Promise.reject(new RequestFailed(500)),
    )
    await adminViewWith(starting.api)

    await clickStart()

    const notice = shownNotice()
    const shown = shownCollection()
    expect({ notice, shown }).toEqual({
      notice: LOAD_FAILED_MESSAGE,
      shown: { state: null, button: 'Jetzt sammeln' },
    })
  })
})

function shownCollection(): {
  readonly state: string | null
  readonly button: string | null
} {
  return {
    state: screen.queryByTestId('collection-state')?.textContent ?? null,
    button: screen.queryByTestId('start-collection')?.textContent ?? null,
  }
}
