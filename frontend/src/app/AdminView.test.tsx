import { act, cleanup, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import type { ProtocolApi } from '../api'
import { Forbidden } from '../api'
import type { Account, AdminStatus } from '../domain'

import { AdminView } from './AdminView.tsx'

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

function apiListing(accounts: Account[]): ProtocolApi {
  const refused = (): Promise<never> => Promise.reject(new Error('unused'))

  return {
    overview: refused,
    adminStatus: () => Promise.resolve(NO_FACTS),
    adminAccounts: () => Promise.resolve(accounts),
    resetPassword: refused,
    bankEntries: refused,
    storageEntries: refused,
  }
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

    expect(heads).toEqual(['Name', 'Ruht', 'Letzte Anmeldung'])
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
