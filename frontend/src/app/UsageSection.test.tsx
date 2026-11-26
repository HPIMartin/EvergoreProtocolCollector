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
import type { Usage, UsageCounts, UsageWindow } from '../domain'
import { LOAD_FAILED_MESSAGE } from '../ui'

import { LoginNavigation } from './LoginRequired.tsx'
import { UsageSection } from './UsageSection.tsx'

afterEach(cleanup)

const NONE: UsageCounts = {
  logins: 0,
  pageLoads: 0,
  logouts: 0,
  daysUsed: 0,
  latestUse: null,
}

const AURORA: UsageCounts = {
  logins: 3,
  pageLoads: 7,
  logouts: 1,
  daysUsed: 2,
  latestUse: new Date('2026-12-30T20:14:00Z'),
}

const LAST_THIRTY_DAYS: UsageWindow = {
  accounts: [
    { name: 'Aurora', counts: AURORA },
    { name: 'Boreas', counts: NONE },
  ],
  guild: AURORA,
  adminKey: {
    logins: 1,
    pageLoads: 2,
    logouts: 0,
    daysUsed: 1,
    latestUse: new Date('2026-12-31T08:02:00Z'),
  },
}

const USAGE: Usage = {
  lastThirtyDays: LAST_THIRTY_DAYS,
  allTheTime: LAST_THIRTY_DAYS,
  recordedSince: '2026-11-01',
  lostSinceStart: 0,
}

const ALL_THE_TIME: UsageWindow = {
  accounts: [
    { name: 'Aurora', counts: { ...AURORA, logins: 9, daysUsed: 5 } },
    { name: 'Boreas', counts: NONE },
  ],
  guild: { ...AURORA, logins: 9, daysUsed: 5 },
  adminKey: NONE,
}

const unused = (): Promise<never> => Promise.reject(new Error('unused'))

function apiAnswering(usage: Usage, asked: string[] = []): ProtocolApi {
  return {
    overview: unused,
    adminStatus: unused,
    adminAccounts: unused,
    adminUsage: () => {
      asked.push('usage')

      return Promise.resolve(usage)
    },
    resetPassword: unused,
    bankEntries: unused,
    storageEntries: unused,
  }
}

function apiRefusing(reason: Error): ProtocolApi {
  return { ...apiAnswering(USAGE), adminUsage: () => Promise.reject(reason) }
}

function shownMessages(): string[] {
  return textsOf(screen.queryAllByTestId('status-panel'))
}

async function usageSectionWith(api: ProtocolApi): Promise<void> {
  render(<UsageSection api={api} />)
  await act(async () => undefined)
}

function textsOf(elements: HTMLElement[]): string[] {
  return elements.map((element) => element.textContent ?? '')
}

function shownTable(): { headers: string[]; rows: string[][] } {
  const table = screen.getByTestId('usage')
  const rows = within(table).getAllByRole('row').slice(1)

  return {
    headers: textsOf(within(table).getAllByRole('columnheader')),
    rows: rows.map((row) => textsOf(within(row).getAllByRole('cell'))),
  }
}

function selectedWindow(): string {
  const selected = screen.getByRole('radio', { checked: true })

  return selected.parentElement?.textContent ?? ''
}

describe('the usage section', () => {
  it('shows each account in the order given with its figures, then the guild and below it the admin key', async () => {
    await usageSectionWith(apiAnswering(USAGE))
    const shown = shownTable()

    expect(shown).toStrictEqual({
      headers: [
        'Name',
        'Anmeldungen',
        'Seite geladen',
        'Tage genutzt',
        'Zuletzt',
        'Abgemeldet',
      ],
      rows: [
        ['Aurora', '3', '7', '2', '30.12.2026 21:14', '1'],
        ['Boreas', '0', '0', '0', 'noch nie', '0'],
        ['Gilde', '3', '7', '2', '30.12.2026 21:14', '1'],
        ['admin', '1', '2', '1', '31.12.2026 09:02', '0'],
      ],
    })
  })

  it('keeps the order the service sent, where the browser would sort otherwise', async () => {
    const sent: UsageWindow = {
      ...LAST_THIRTY_DAYS,
      accounts: [
        { name: 'Zwerg', counts: NONE },
        { name: 'Ärger', counts: NONE },
        { name: 'Aurora', counts: NONE },
      ],
    }
    await usageSectionWith(apiAnswering({ ...USAGE, lastThirtyDays: sent }))
    const names = shownTable().rows.map((row) => row[0])

    expect(names).toStrictEqual(['Zwerg', 'Ärger', 'Aurora', 'Gilde', 'admin'])
  })

  it('shows the guild and the admin key alone while no account is listed', async () => {
    const noAccounts: UsageWindow = { ...LAST_THIRTY_DAYS, accounts: [] }
    await usageSectionWith(
      apiAnswering({ ...USAGE, lastThirtyDays: noAccounts }),
    )
    const names = shownTable().rows.map((row) => row[0])

    expect(names).toStrictEqual(['Gilde', 'admin'])
  })

  it('starts on the last 30 days and shows all the time on the switch without asking again', async () => {
    const asked: string[] = []
    await usageSectionWith(
      apiAnswering({ ...USAGE, allTheTime: ALL_THE_TIME }, asked),
    )
    const selectedFirst = selectedWindow()
    const auroraFirst = shownTable().rows[0]

    fireEvent.click(screen.getByLabelText('Alles'))
    const selectedThen = selectedWindow()
    const rowsThen = shownTable().rows

    expect({
      selectedFirst,
      auroraFirst,
      selectedThen,
      rowsThen,
      asked,
    }).toStrictEqual({
      selectedFirst: 'Letzte 30 Tage',
      auroraFirst: ['Aurora', '3', '7', '2', '30.12.2026 21:14', '1'],
      selectedThen: 'Alles',
      rowsThen: [
        ['Aurora', '9', '7', '5', '30.12.2026 21:14', '1'],
        ['Boreas', '0', '0', '0', 'noch nie', '0'],
        ['Gilde', '9', '7', '5', '30.12.2026 21:14', '1'],
        ['admin', '0', '0', '0', 'noch nie', '0'],
      ],
      asked: ['usage'],
    })
  })

  it('names the day the records start and no lost records while none are lost', async () => {
    await usageSectionWith(apiAnswering(USAGE))

    expect({
      recordedSince: screen.getByTestId('usage-recorded-since').textContent,
      lost: screen.queryByTestId('usage-lost'),
    }).toStrictEqual({
      recordedSince: 'Aufgezeichnet seit 01.11.2026',
      lost: null,
    })
  })

  it('says that nothing is recorded yet and how many records were lost since the start', async () => {
    await usageSectionWith(
      apiAnswering({ ...USAGE, recordedSince: null, lostSinceStart: 1 }),
    )

    expect({
      recordedSince: screen.getByTestId('usage-recorded-since').textContent,
      lost: screen.getByTestId('usage-lost').textContent,
    }).toStrictEqual({
      recordedSince: 'Noch keine Aufzeichnung.',
      lost: 'Seit dem Start verloren: 1',
    })
  })

  it.each([
    {
      reason: new Forbidden(),
      message: 'Diese Seite ist dem Admin vorbehalten.',
    },
    { reason: new RequestFailed(500), message: LOAD_FAILED_MESSAGE },
  ])('answers a refused or failed load with „$message“', async (refusal) => {
    await usageSectionWith(apiRefusing(refusal.reason))
    const messages = shownMessages()

    expect(messages).toStrictEqual([refusal.message])
  })

  it('sends to the login page when the login has ended', async () => {
    const navigatedTo: string[] = []
    render(
      <LoginNavigation.Provider
        value={(target) => {
          navigatedTo.push(target)
        }}
      >
        <UsageSection api={apiRefusing(new Unauthorized())} />
      </LoginNavigation.Provider>,
    )
    await act(async () => undefined)
    const pages = navigatedTo.map((target) => target.split('?')[0])

    expect(pages).toStrictEqual(['/login'])
  })
})
