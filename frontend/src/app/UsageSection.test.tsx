import { act, cleanup, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import type { ProtocolApi } from '../api'
import type { Usage, UsageCounts, UsageWindow } from '../domain'

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

const unused = (): Promise<never> => Promise.reject(new Error('unused'))

function apiAnswering(usage: Usage): ProtocolApi {
  return {
    overview: unused,
    adminStatus: unused,
    adminAccounts: unused,
    adminUsage: () => Promise.resolve(usage),
    resetPassword: unused,
    bankEntries: unused,
    storageEntries: unused,
  }
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
})
