import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from '@testing-library/react'
import { StrictMode } from 'react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'

import type { HttpGet } from '../api'

import { App } from './App.tsx'

const TOKEN = 'a-test-token'

const OVERVIEW_BODY = JSON.stringify({
  page: 0,
  size: 100,
  totalCount: 2,
  totals: {
    bankWithdrawn: 1400,
    bankDeposited: 3450,
    storageWithdrawn: 200,
    storageDeposited: 500,
    net: 2350,
    donation: 400,
    craftSubsidy: 100,
    balance: 2650,
    storageValue: 600,
    containsStaleSums: false,
  },
  items: [
    {
      avatar: 'Calix',
      bankWithdrawn: 1200,
      bankDeposited: 3400,
      storageWithdrawn: 200,
      storageDeposited: 500,
      net: 2500,
      donation: 400,
      craftSubsidy: 100,
      balance: 2800,
      lastBankActivity: '2026-08-04T09:30:00Z',
      lastStorageActivity: '2026-08-05T10:15:00Z',
      staleSumsFrom: null,
      active: true,
    },
    {
      avatar: 'Erde-Eibenlanze',
      bankWithdrawn: 200,
      bankDeposited: 50,
      storageWithdrawn: 0,
      storageDeposited: 0,
      net: -150,
      donation: 0,
      craftSubsidy: 0,
      balance: -150,
      lastBankActivity: null,
      lastStorageActivity: '2026-07-31T21:05:00Z',
      staleSumsFrom: null,
      active: true,
    },
  ],
})

const OVERVIEW_BODY_WITH_A_STALE_ROW = JSON.stringify({
  page: 0,
  size: 100,
  totalCount: 2,
  totals: {
    bankWithdrawn: 1400,
    bankDeposited: 3450,
    storageWithdrawn: 200,
    storageDeposited: 500,
    net: 2350,
    donation: 400,
    craftSubsidy: 100,
    balance: 2650,
    storageValue: 600,
    containsStaleSums: true,
  },
  items: [
    {
      avatar: 'Calix',
      bankWithdrawn: 1200,
      bankDeposited: 3400,
      storageWithdrawn: 200,
      storageDeposited: 500,
      net: 2500,
      donation: 400,
      craftSubsidy: 100,
      balance: 2800,
      lastBankActivity: '2026-08-04T09:30:00Z',
      lastStorageActivity: '2026-08-05T10:15:00Z',
      staleSumsFrom: null,
      active: true,
    },
    {
      avatar: 'Erde-Eibenlanze',
      bankWithdrawn: 200,
      bankDeposited: 50,
      storageWithdrawn: 0,
      storageDeposited: 0,
      net: -150,
      donation: 0,
      craftSubsidy: 0,
      balance: -150,
      lastBankActivity: '2026-08-05T09:58:00Z',
      lastStorageActivity: '2026-07-31T21:05:00Z',
      staleSumsFrom: '2026-07-30T01:12:00Z',
      active: true,
    },
  ],
})

const OVERVIEW_BODY_WHOSE_STALE_ROW_IS_OFF_THE_PAGE = JSON.stringify({
  page: 0,
  size: 1,
  totalCount: 2,
  totals: {
    bankWithdrawn: 1400,
    bankDeposited: 3450,
    storageWithdrawn: 200,
    storageDeposited: 500,
    net: 2350,
    donation: 400,
    craftSubsidy: 100,
    balance: 2650,
    storageValue: 600,
    containsStaleSums: true,
  },
  items: [
    {
      avatar: 'Calix',
      bankWithdrawn: 1200,
      bankDeposited: 3400,
      storageWithdrawn: 200,
      storageDeposited: 500,
      net: 2500,
      donation: 400,
      craftSubsidy: 100,
      balance: 2800,
      lastBankActivity: '2026-08-04T09:30:00Z',
      lastStorageActivity: '2026-08-05T10:15:00Z',
      staleSumsFrom: null,
      active: true,
    },
  ],
})

const OVERVIEW_BODY_WITHOUT_THE_FLOWS = JSON.stringify({
  page: 0,
  size: 100,
  totalCount: 1,
  totals: {
    bankWithdrawn: 1400,
    bankDeposited: 3450,
    storageWithdrawn: 200,
    storageDeposited: 500,
    net: 2350,
    donation: null,
    craftSubsidy: null,
    balance: null,
    storageValue: null,
    containsStaleSums: false,
  },
  items: [
    {
      avatar: 'Calix',
      bankWithdrawn: 1200,
      bankDeposited: 3400,
      storageWithdrawn: 200,
      storageDeposited: 500,
      net: 2500,
      donation: null,
      craftSubsidy: null,
      balance: null,
      lastBankActivity: '2026-08-04T09:30:00Z',
      lastStorageActivity: '2026-08-05T10:15:00Z',
      staleSumsFrom: null,
      active: true,
    },
  ],
})

const TWO_ROWS_WITHOUT_FLOWS = JSON.stringify({
  page: 0,
  size: 100,
  totalCount: 2,
  totals: {
    bankWithdrawn: 1400,
    bankDeposited: 3450,
    storageWithdrawn: 200,
    storageDeposited: 500,
    net: 2350,
    donation: null,
    craftSubsidy: null,
    balance: null,
    storageValue: null,
    containsStaleSums: false,
  },
  items: [
    {
      avatar: 'Calix',
      bankWithdrawn: 1200,
      bankDeposited: 3400,
      storageWithdrawn: 200,
      storageDeposited: 500,
      net: 2500,
      donation: null,
      craftSubsidy: null,
      balance: null,
      lastBankActivity: '2026-08-04T09:30:00Z',
      lastStorageActivity: '2026-08-05T10:15:00Z',
      staleSumsFrom: null,
      active: true,
    },
    {
      avatar: 'Erde-Eibenlanze',
      bankWithdrawn: 200,
      bankDeposited: 50,
      storageWithdrawn: 0,
      storageDeposited: 0,
      net: -150,
      donation: null,
      craftSubsidy: null,
      balance: null,
      lastBankActivity: null,
      lastStorageActivity: '2026-07-31T21:05:00Z',
      staleSumsFrom: null,
      active: true,
    },
  ],
})

const NO_SUMS = {
  bankWithdrawn: null,
  bankDeposited: null,
  storageWithdrawn: null,
  storageDeposited: null,
  net: null,
  donation: null,
  craftSubsidy: null,
  balance: null,
}

const OVERVIEW_BODY_WITH_A_ROW_NOT_YET_COMPUTED = JSON.stringify({
  page: 0,
  size: 100,
  totalCount: 2,
  totals: { ...NO_SUMS, storageValue: null, containsStaleSums: true },
  items: [
    {
      avatar: 'Calix',
      bankWithdrawn: 1200,
      bankDeposited: 3400,
      storageWithdrawn: 200,
      storageDeposited: 500,
      net: 2500,
      donation: 400,
      craftSubsidy: 100,
      balance: 2800,
      lastBankActivity: '2026-08-04T09:30:00Z',
      lastStorageActivity: '2026-08-05T10:15:00Z',
      staleSumsFrom: '2026-07-30T01:12:00Z',
      active: true,
    },
    {
      avatar: 'Erde-Eibenlanze',
      ...NO_SUMS,
      lastBankActivity: '2026-08-05T09:58:00Z',
      lastStorageActivity: null,
      staleSumsFrom: null,
      active: true,
    },
  ],
})

const OVERVIEW_BODY_WITH_A_ROW_NOT_YET_COMPUTED_THAT_ALSO_STATES_AN_AGE =
  JSON.stringify({
    ...(JSON.parse(OVERVIEW_BODY_WITH_A_ROW_NOT_YET_COMPUTED) as object),
    totalCount: 1,
    items: [
      {
        avatar: 'Erde-Eibenlanze',
        ...NO_SUMS,
        lastBankActivity: '2026-08-05T09:58:00Z',
        lastStorageActivity: null,
        staleSumsFrom: '2026-07-30T01:12:00Z',
        active: true,
      },
    ],
  })

const NO_TOTALS = {
  bankWithdrawn: 0,
  bankDeposited: 0,
  storageWithdrawn: 0,
  storageDeposited: 0,
  net: 0,
  donation: 0,
  craftSubsidy: 0,
  balance: 0,
  storageValue: 0,
  containsStaleSums: false,
}

interface RosterRow {
  readonly avatar: string
  readonly active: boolean
  readonly lastStorageActivity: string | null
}

function rosterBodyOf(
  rows: readonly RosterRow[],
  totalCount: number = rows.length,
): string {
  return JSON.stringify({
    page: 0,
    size: 100,
    totalCount,
    totals: NO_TOTALS,
    items: rows.map((row) => ({
      ...row,
      lastBankActivity: null,
      bankWithdrawn: 0,
      bankDeposited: 0,
      storageWithdrawn: 0,
      storageDeposited: 0,
      net: 0,
      donation: 0,
      craftSubsidy: 0,
      balance: 0,
      staleSumsFrom: null,
    })),
  })
}

const NEWEST_ACTIVITY = '2026-08-05T10:15:00Z'

const ROSTER_MARKED_BY_THE_SERVICE = rosterBodyOf([
  { avatar: 'Aktiv', active: true, lastStorageActivity: NEWEST_ACTIVITY },
  { avatar: 'Aktivin', active: true, lastStorageActivity: null },
  { avatar: 'Ruhender', active: false, lastStorageActivity: null },
  { avatar: 'Ruhend', active: false, lastStorageActivity: null },
])

const ROSTER_MARKED_AGAINST_ITS_DATES = rosterBodyOf([
  {
    avatar: 'Letzter',
    active: true,
    lastStorageActivity: '2021-03-01T10:00:00Z',
  },
  {
    avatar: 'Weit-Zurueck',
    active: true,
    lastStorageActivity: '2020-01-01T10:00:00Z',
  },
  { avatar: 'Neu', active: false, lastStorageActivity: NEWEST_ACTIVITY },
])

const ROSTER_INTERLEAVED_BY_NAME = rosterBodyOf([
  { avatar: 'Anna', active: true, lastStorageActivity: NEWEST_ACTIVITY },
  { avatar: 'Bert', active: false, lastStorageActivity: null },
  { avatar: 'Cara', active: true, lastStorageActivity: null },
  { avatar: 'Dora', active: false, lastStorageActivity: null },
])

const ROSTER_OF_ONE_LOADED_PAGE = rosterBodyOf(
  [
    { avatar: 'Anna', active: true, lastStorageActivity: NEWEST_ACTIVITY },
    { avatar: 'Bert', active: false, lastStorageActivity: null },
  ],
  42,
)

const BANK_BODY = JSON.stringify({
  page: 0,
  size: 100,
  totalCount: 1,
  items: [
    {
      timestamp: '2026-08-05T10:15:00Z',
      avatar: 'Calix',
      amount: 500,
      transferType: 'DEPOSIT',
    },
  ],
})

const STORAGE_BODY = JSON.stringify({
  page: 0,
  size: 100,
  totalCount: 1,
  items: [
    {
      timestamp: '2026-08-05T10:15:00Z',
      avatar: 'Calix',
      quantity: 3,
      name: 'Erde-Eibenlanze',
      quality: 7,
      transferType: 'WITHDRAWAL',
    },
  ],
})

const EMPTY_LEDGER_BODY = JSON.stringify({
  page: 0,
  size: 100,
  totalCount: 0,
  items: [],
})

const ADMIN_STATUS_BODY = JSON.stringify({
  lastUpdated: '2026-08-05T10:15:00Z',
  lastSuccessfulScrape: '2026-08-05T10:15:20Z',
  lastScrapeFailure: null,
  lastSuccessfulRecompute: '2026-08-05T10:15:30Z',
  lastRecomputeFailure: null,
  unknownItemNames: ['Unobtainium'],
  failedAvatarNames: ['Zwerg'],
  roundTrips: [{ avatar: 'Alrik', item: 'Federn', quantity: 100 }],
  roundTripAbstentions: [{ avatar: 'Zwerg', item: 'Erz' }],
})

const FAILED_RUN_ADMIN_STATUS_BODY = JSON.stringify({
  lastUpdated: '2026-08-05T10:15:00Z',
  lastSuccessfulScrape: null,
  lastScrapeFailure: '2026-08-05T10:15:20Z',
  lastSuccessfulRecompute: '2026-08-05T10:15:30Z',
  lastRecomputeFailure: null,
  unknownItemNames: [],
  failedAvatarNames: [],
  roundTrips: [],
  roundTripAbstentions: [],
})

const NEVER_COLLECTED_ADMIN_STATUS_BODY = JSON.stringify({
  lastUpdated: null,
  lastSuccessfulScrape: null,
  lastScrapeFailure: null,
  lastSuccessfulRecompute: null,
  lastRecomputeFailure: null,
  unknownItemNames: [],
  failedAvatarNames: [],
  roundTrips: [],
  roundTripAbstentions: [],
})

interface Answer {
  readonly status: number
  readonly body: string | null
}

interface FakeServer {
  readonly askedFor: string[]
  readonly get: HttpGet
}

function serving(answerFor: (path: string) => Answer): FakeServer {
  const askedFor: string[] = []

  return {
    askedFor,
    get: (url) => {
      askedFor.push(url)
      const answer = answerFor(new URL(url, 'http://localhost').pathname)

      return Promise.resolve(new Response(answer.body, answer))
    },
  }
}

function servingByUrl(answerFor: (url: string) => Answer): FakeServer {
  const askedFor: string[] = []

  return {
    askedFor,
    get: (url) => {
      askedFor.push(url)
      const answer = answerFor(url)

      return Promise.resolve(new Response(answer.body, answer))
    },
  }
}

function alwaysServing(status: number, body: string | null): FakeServer {
  return serving(() => ({ status, body }))
}

async function shellAt(address: string, server: FakeServer): Promise<void> {
  window.history.replaceState(null, '', address)
  render(<App get={server.get} />)
  await act(async () => undefined)
}

function rowTexts(): string[] {
  return screen.getAllByTestId('data-row').map((row) => row.textContent ?? '')
}

function headerTexts(): string[] {
  return screen
    .getAllByTestId('column-label')
    .map((header) => header.textContent ?? '')
}

function headerTextsIn(roster: string): string[] {
  return within(screen.getByTestId(roster))
    .getAllByTestId('column-label')
    .map((header) => header.textContent ?? '')
}

function rowNamesIn(roster: string): string[] {
  return within(screen.getByTestId(roster))
    .queryAllByTestId('cell-avatar')
    .map((cell) => cell.textContent ?? '')
}

function sortButtonIn(roster: string, name: RegExp): HTMLElement {
  return within(screen.getByTestId(roster)).getByRole('button', { name })
}

function statusIn(roster: string): string {
  return (
    within(screen.getByTestId(roster)).getByTestId('status-panel')
      .textContent ?? ''
  )
}

function totalRowsIn(roster: string): number {
  return within(screen.getByTestId(roster)).queryAllByTestId('total-row').length
}

function captionOf(roster: string): string {
  return (
    within(screen.getByTestId(roster))
      .getByRole('table')
      .querySelector('caption')?.textContent ?? ''
  )
}

function statTexts(): string[] {
  return Array.from(screen.getByTestId('stat-header').children).map(
    (stat) => stat.textContent ?? '',
  )
}

function statTonesOf(): (string | undefined)[] {
  return Array.from(screen.getByTestId('stat-header').children).map(
    (stat) => stat.querySelector('dd')?.dataset.tone,
  )
}

function tonesOf(columnKey: string): (string | undefined)[] {
  return screen
    .getAllByTestId(`cell-${columnKey}`)
    .map((cell) => cell.dataset.tone)
}

function shownStatus(): string {
  return screen.getByTestId('status-panel').textContent ?? ''
}

describe('App', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/')
  })

  afterEach(cleanup)

  it('names the application', async () => {
    await shellAt(`/?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(screen.getByTestId('page-brand').textContent).toBe(
      'Evergore Protocol Collector',
    )
  })

  it('shows the overview at the address the shell is served from', async () => {
    await shellAt(`/?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    const rows = rowTexts()

    expect(rows).toStrictEqual([
      'Calix3.4001.2005002002.50005.08.2026 12:1504.08.2026 11:30',
      'Erde-Eibenlanze5020000-15031.07.2026 23:05–',
    ])
  })

  it('marks the row whose sums the last collection did not refresh', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITH_A_STALE_ROW),
    )

    const staleFlags = screen
      .getAllByTestId('data-row')
      .map((row) => row.dataset.stale)

    expect(staleFlags).toStrictEqual([undefined, 'true'])
    expect(screen.getByTestId('row-mark').textContent).toContain(
      'Veraltete Zahlen. Letzte erfolgreiche Aktualisierung vom 30.07.2026 03:12.',
    )
  })

  it('says on the guild row that it contains a row with stale sums', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITH_A_STALE_ROW),
    )

    expect(screen.getByTestId('total-row').dataset.stale).toBe('true')
    expect(screen.getByTestId('total-mark').textContent).toContain(
      'Enthält mindestens eine Zeile mit veralteten Zahlen.',
    )
  })

  it('keeps showing the live activity of a marked row rather than hiding it', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITH_A_STALE_ROW),
    )

    const marked = screen.getAllByTestId('data-row')[1]?.textContent ?? ''

    expect(marked).toContain('05.08.2026 11:58')
    expect(marked).toContain('31.07.2026 23:05')
  })

  it('marks no row and no total while the collection refreshed every row', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(screen.queryByTestId('row-mark')).toBeNull()
    expect(screen.queryByTestId('total-mark')).toBeNull()
    expect(screen.getByTestId('total-row').dataset.stale).toBeUndefined()
  })

  it('marks no row and no guild row as not yet computed while only the guild share is unknown', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, TWO_ROWS_WITHOUT_FLOWS),
    )

    expect(screen.queryByTestId('row-mark')).toBeNull()
    expect(screen.queryByTestId('total-mark')).toBeNull()
    expect(screen.getByTestId('total-row').dataset.stale).toBeUndefined()
  })

  it('marks no row whose sums were computed as zero', async () => {
    const body = JSON.parse(OVERVIEW_BODY) as {
      items: Record<string, unknown>[]
    }
    body.items[0] = {
      ...body.items[0],
      bankWithdrawn: 0,
      bankDeposited: 0,
      storageWithdrawn: 0,
      storageDeposited: 0,
      net: 0,
    }
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, JSON.stringify(body)),
    )

    expect(screen.queryByTestId('row-mark')).toBeNull()
  })

  it('says the guild contains stale sums even when that row is off the page', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WHOSE_STALE_ROW_IS_OFF_THE_PAGE),
    )

    expect(screen.queryByTestId('row-mark')).toBeNull()
    expect(screen.getByTestId('total-row').dataset.stale).toBe('true')
    expect(screen.getByTestId('total-mark').textContent).toContain(
      'Enthält mindestens eine Zeile mit veralteten Zahlen.',
    )
  })

  it('marks a row no recompute has reached as not yet computed', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITH_A_ROW_NOT_YET_COMPUTED),
    )

    const marks = screen
      .getAllByTestId('row-mark')
      .map((mark) => mark.textContent)
    const staleFlags = screen
      .getAllByTestId('data-row')
      .map((row) => row.dataset.stale)

    expect(marks).toStrictEqual([
      '!Veraltete Zahlen. Letzte erfolgreiche Aktualisierung vom 30.07.2026 03:12.',
      '!Noch nicht berechnet.',
    ])
    expect(staleFlags).toStrictEqual(['true', 'true'])
  })

  it('shows no figure in the five number columns of a row not yet computed, rather than zeros', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITH_A_ROW_NOT_YET_COMPUTED),
    )

    const cells = Array.from(
      screen.getAllByTestId('data-row')[1]?.querySelectorAll('td') ?? [],
    ).map((cell) => cell.textContent)
    const numberCells = cells.slice(1, 6)

    expect(numberCells).toStrictEqual(['–', '–', '–', '–', '–'])
  })

  it('keeps the activity of a row not yet computed beside its dashes', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITH_A_ROW_NOT_YET_COMPUTED),
    )

    const cells = Array.from(
      screen.getAllByTestId('data-row')[1]?.querySelectorAll('td') ?? [],
    ).map((cell) => cell.textContent)
    const activityCells = cells.slice(6)

    expect(activityCells).toStrictEqual(['–', '05.08.2026 11:58'])
  })

  it('shows no figure in the guild row while a member is not yet computed, rather than a total without him', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITH_A_ROW_NOT_YET_COMPUTED),
    )

    const cells = [
      'bankDeposited',
      'bankWithdrawn',
      'storageDeposited',
      'storageWithdrawn',
      'figure',
    ].map((key) => screen.getByTestId(`total-${key}`).textContent)
    expect(cells).toStrictEqual(['–', '–', '–', '–', '–'])
  })

  it('keeps the mark of a row not yet computed and gives its figure before the deductions no note of its own', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITH_A_ROW_NOT_YET_COMPUTED),
    )

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    const row = screen.getAllByTestId('data-row')[1]
    expect(
      row === undefined
        ? null
        : within(row).getByTestId('row-mark').textContent,
    ).toBe('!Noch nicht berechnet.')
    expect(
      row === undefined ? null : within(row).queryAllByTestId('cell-mark'),
    ).toStrictEqual([])
  })

  it('gives the guild row no note before the deductions while a row is not yet computed', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITH_A_ROW_NOT_YET_COMPUTED),
    )

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(screen.queryByTestId('total-cell-mark')).toBeNull()
  })

  it('says the guild contains a row not yet computed even when that row is off the page', async () => {
    const body = JSON.parse(OVERVIEW_BODY_WITH_A_ROW_NOT_YET_COMPUTED) as {
      items: unknown[]
      totals: Record<string, unknown>
    }
    const firstPage = {
      ...body,
      size: 1,
      items: body.items.slice(0, 1),
      totals: { ...body.totals, containsStaleSums: false },
    }
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, JSON.stringify(firstPage)),
    )

    expect(screen.getByTestId('row-mark').textContent).toBe(
      '!Veraltete Zahlen. Letzte erfolgreiche Aktualisierung vom 30.07.2026 03:12.',
    )
    expect(screen.getByTestId('total-mark').textContent).toBe(
      '!Enthält mindestens eine Zeile, die noch nicht berechnet ist.',
    )
  })

  it('marks a row not yet computed as such even when it also states an age, because that mark wins', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(
        200,
        OVERVIEW_BODY_WITH_A_ROW_NOT_YET_COMPUTED_THAT_ALSO_STATES_AN_AGE,
      ),
    )

    expect(screen.getByTestId('row-mark').textContent).toBe(
      '!Noch nicht berechnet.',
    )
  })

  it('says on the guild row that it contains a row not yet computed, ahead of the outdated rows it also contains', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITH_A_ROW_NOT_YET_COMPUTED),
    )

    expect(screen.getByTestId('total-row').dataset.stale).toBe('true')
    expect(screen.getByTestId('total-mark').textContent).toBe(
      '!Enthält mindestens eine Zeile, die noch nicht berechnet ist.',
    )
  })

  it('splits the roster by the mark the service gave each member', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, ROSTER_MARKED_BY_THE_SERVICE),
    )

    const activeNames = rowNamesIn('active-roster')
    const dormantNames = rowNamesIn('dormant-roster')

    expect([activeNames, dormantNames]).toStrictEqual([
      ['Aktiv', 'Aktivin'],
      ['Ruhender', 'Ruhend'],
    ])
  })

  it('draws no line of its own, so the mark decides whatever dates the rows carry', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, ROSTER_MARKED_AGAINST_ITS_DATES),
    )

    const activeNames = rowNamesIn('active-roster')
    const dormantNames = rowNamesIn('dormant-roster')

    expect([activeNames, dormantNames]).toStrictEqual([
      ['Letzter', 'Weit-Zurueck'],
      ['Neu'],
    ])
  })

  it('keeps the name order of the response inside each of the two tables', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, ROSTER_INTERLEAVED_BY_NAME),
    )

    const activeNames = rowNamesIn('active-roster')
    const dormantNames = rowNamesIn('dormant-roster')

    expect([activeNames, dormantNames]).toStrictEqual([
      ['Anna', 'Cara'],
      ['Bert', 'Dora'],
    ])
  })

  it('gives both tables the same columns', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, ROSTER_MARKED_BY_THE_SERVICE),
    )

    const headers = headerTextsIn('dormant-roster')

    expect(headers).toStrictEqual(headerTextsIn('active-roster'))
  })

  it('states the guild total under the active table and nowhere else', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, ROSTER_MARKED_BY_THE_SERVICE),
    )

    const activeTotals = totalRowsIn('active-roster')
    const dormantTotals = totalRowsIn('dormant-roster')

    expect([activeTotals, dormantTotals]).toStrictEqual([1, 0])
  })

  it('says so in the second table while no member is dormant', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    const dormantNames = rowNamesIn('dormant-roster')
    const dormantStatus = statusIn('dormant-roster')

    expect([dormantNames, dormantStatus]).toStrictEqual([
      [],
      'Kein Avatar ruht.',
    ])
  })

  it('names the overview columns in German', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    const headers = headerTextsIn('active-roster')

    expect(headers).toStrictEqual([
      'Avatar',
      'Bank-Einzahlung',
      'Bank-Auszahlung',
      'Einlagerung',
      'Entnahme',
      'Nach Abzügen',
      'Letzte Lageraktivität',
      'Letzte Bankaktivität',
    ])
  })

  it('states each figure under its own name, in the order the header reads', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    const stats = statTexts()

    expect(stats).toStrictEqual([
      'Gildenbank2.050',
      'Gildenlagerwert600',
      'Gildenspende400',
      'Handwerkssubventionen100',
    ])
  })

  it('states the storage value in the header as the server rounded it rather than adding up its rounded parts', async () => {
    const body = JSON.parse(OVERVIEW_BODY) as {
      totals: { storageValue: number }
    }
    body.totals.storageValue = 599
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, JSON.stringify(body)),
    )

    const stats = statTexts()

    expect(stats[1]).toBe('Gildenlagerwert599')
  })

  it('tones the bank as a credit and leaves the subsidy untoned', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(screen.getByTestId('stat-bank').dataset.tone).toBe('credit')
    expect(screen.getByTestId('stat-subsidy').dataset.tone).toBe('neutral')
  })

  it('names both flows in the header rather than netting them into one number shaped like a balance', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(screen.getByText('Gildenspende')).toBeTruthy()
    expect(screen.getByText('Handwerkssubventionen')).toBeTruthy()
  })

  it('says the header cannot answer yet while no recompute has produced the flows', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITHOUT_THE_FLOWS),
    )

    expect(screen.getByTestId('stat-donation').dataset.absent).toBe('true')
    expect(screen.getByTestId('stat-storage').dataset.absent).toBe('true')
  })

  it('says a figure is not computed in words a reader cannot mistake for a number', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITHOUT_THE_FLOWS),
    )

    expect(screen.getByTestId('stat-donation').textContent).toBe(
      'Noch nicht berechnet.',
    )
    expect(screen.getByTestId('stat-storage').textContent).not.toMatch(/[0-9]/)
  })

  it('leaves an absent figure untoned rather than painting it as a credit', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITHOUT_THE_FLOWS),
    )

    expect(screen.getByTestId('stat-donation').dataset.tone).toBe('neutral')
  })

  it('still states the measured bank while the flows are missing', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITHOUT_THE_FLOWS),
    )

    expect(screen.getByTestId('stat-bank').textContent).toBe('2.050')
  })

  it('switches the last column between the figure after and before the deductions', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    const headers = headerTextsIn('active-roster')

    expect(headers).toStrictEqual([
      'Avatar',
      'Bank-Einzahlung',
      'Bank-Auszahlung',
      'Einlagerung',
      'Entnahme',
      'Vor Abzügen',
      'Letzte Lageraktivität',
      'Letzte Bankaktivität',
    ])
  })

  it('reports the picked figure back through the switch', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    const balance = screen.getByLabelText('Vor Abzügen') as HTMLInputElement
    const contribution = screen.getByLabelText(
      'Nach Abzügen',
    ) as HTMLInputElement

    expect([balance.checked, contribution.checked]).toStrictEqual([true, false])
  })

  it('keeps the two options in one radio group so the keyboard can switch them', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    const names = screen
      .getAllByRole('radio')
      .map((radio) => (radio as HTMLInputElement).name)

    expect(names).toStrictEqual(['figure', 'figure'])
  })

  it("states the guild's figure before the deductions in the total row as the server rounded it", async () => {
    const body = JSON.parse(OVERVIEW_BODY) as { totals: { balance: number } }
    body.totals.balance = 2649
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, JSON.stringify(body)),
    )

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(screen.getByTestId('total-row').textContent).toContain('2.649')
  })

  it("states a row's figure before the deductions as the server rounded it rather than adding up its rounded parts", async () => {
    const body = JSON.parse(OVERVIEW_BODY) as { items: { balance: number }[] }
    const [calix] = body.items
    if (calix === undefined) {
      throw new TypeError('The fixture lost its first row')
    }
    calix.balance = 2799
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, JSON.stringify(body)),
    )

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(screen.getByText('2.799')).toBeTruthy()
  })

  it('tones the switched column like the contribution it replaces', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    const tones = tonesOf('figure')

    expect(tones).toStrictEqual(['neutral', 'debit'])
  })

  it('says why a row has no figure before the deductions instead of leaving a bare dash', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITHOUT_THE_FLOWS),
    )

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(screen.getAllByTestId('cell-mark')[0]?.textContent).toContain(
      'Kein Saldo: für diese Zeile sind Gildenspende und Handwerkssubventionen noch nicht berechnet.',
    )
  })

  it('says it on every row that cannot answer, not only on one', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, TWO_ROWS_WITHOUT_FLOWS),
    )

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(screen.getAllByTestId('cell-mark')).toHaveLength(
      screen.getAllByTestId('data-row').length,
    )
  })

  it('says it on the guild row as well, which sums rows that cannot answer', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, TWO_ROWS_WITHOUT_FLOWS),
    )

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(screen.getByTestId('total-cell-mark').textContent).toContain(
      'Kein Saldo',
    )
  })

  it('states all four figures word for word while nothing is computed', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, TWO_ROWS_WITHOUT_FLOWS),
    )

    const stats = statTexts()

    expect(stats).toStrictEqual([
      'Gildenbank2.050',
      'GildenlagerwertNoch nicht berechnet.',
      'GildenspendeNoch nicht berechnet.',
      'HandwerkssubventionenNoch nicht berechnet.',
    ])
  })

  it('states all four figures as not yet computed while a member is not yet computed, the bank included', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITH_A_ROW_NOT_YET_COMPUTED),
    )

    const stats = statTexts()

    expect(stats).toStrictEqual([
      'GildenbankNoch nicht berechnet.',
      'GildenlagerwertNoch nicht berechnet.',
      'GildenspendeNoch nicht berechnet.',
      'HandwerkssubventionenNoch nicht berechnet.',
    ])
    expect(screen.getByTestId('stat-bank').dataset.absent).toBe('true')
  })

  it('tones each of the four figures the way its meaning asks for', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    const statTones = statTonesOf()

    expect(statTones).toStrictEqual(['credit', 'credit', 'credit', 'neutral'])
  })

  it('names what the switch controls, which is its only accessible name', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    const legend = screen
      .getByTestId('switch-figure')
      .querySelector('legend')?.textContent

    expect(legend).toBe('Letzte Spalte')
  })

  it('offers the figure after the deductions before the one before them', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    const switchLabels = screen
      .getAllByRole('radio')
      .map((radio) => radio.parentElement?.textContent)

    expect(switchLabels).toStrictEqual(['Nach Abzügen', 'Vor Abzügen'])
  })

  it('carries no hint on a row whose figure before the deductions is known', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(screen.queryByTestId('cell-mark')).toBeNull()
  })

  it("shows a row's figure before the deductions in the switched column", async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    const rows = rowTexts()

    expect(rows[0]).toContain('2.800')
  })

  it('keeps the figure after the deductions while the switch stays on it', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    const rows = rowTexts()

    expect(rows[0]).toContain('2.500')
  })

  it('keeps the table usable when the reader sorts the sixth column and then switches it', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(sortButtonIn('active-roster', /Nach Abzügen/))
    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    const shownRows = rowTexts().join(' ')

    expect(shownRows).toContain('2.800')
  })

  it('keeps a sort on the sixth column when the switch changes what it shows', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(sortButtonIn('active-roster', /Nach Abzügen/))
    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    const rows = rowTexts()

    expect(rows[0]).toContain('Erde-Eibenlanze')
  })

  it('does not widen the table when the switch changes the last column', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    const headers = headerTextsIn('active-roster')

    expect(headers).toHaveLength(8)
  })

  it('names the bank ledger columns in German', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}`,
      alwaysServing(200, BANK_BODY),
    )

    const headers = headerTexts()

    expect(headers).toStrictEqual(['Zeitpunkt', 'Avatar', 'Betrag', 'Vorgang'])
  })

  it('names the storage ledger columns in German', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}`,
      alwaysServing(200, STORAGE_BODY),
    )

    const headers = headerTexts()

    expect(headers).toStrictEqual([
      'Zeitpunkt',
      'Avatar',
      'Menge',
      'Gegenstand',
      'Qualität',
      'Vorgang',
    ])
  })

  it('tells what an avatar took out from what it put in, per ledger', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    const bankWithdrawn = tonesOf('bankWithdrawn')
    const bankDeposited = tonesOf('bankDeposited')
    const storageWithdrawn = tonesOf('storageWithdrawn')
    const storageDeposited = tonesOf('storageDeposited')

    expect({
      bankWithdrawn,
      bankDeposited,
      storageWithdrawn,
      storageDeposited,
    }).toStrictEqual({
      bankWithdrawn: ['debit', 'debit'],
      bankDeposited: ['credit', 'credit'],
      storageWithdrawn: ['debit', 'neutral'],
      storageDeposited: ['credit', 'neutral'],
    })
  })

  it('shows the guild-wide total row the server computed', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(screen.getByTestId('total-row').textContent).toBe(
      'Gilde3.4501.4005002002.350––',
    )
  })

  it('says when an avatar never used one of the two ledgers', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    const lastBankActivities = screen
      .getAllByTestId('cell-lastBankActivity')
      .map((cell) => cell.textContent)

    expect(lastBankActivities).toStrictEqual(['04.08.2026 11:30', '–'])
  })

  it('marks a negative guild value as taken from the guild', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    const tones = tonesOf('figure')

    expect(tones).toStrictEqual(['neutral', 'debit'])
  })

  it('carries the token of the deep link into every request', async () => {
    const server = alwaysServing(200, OVERVIEW_BODY)

    await shellAt(`/overview?token=${TOKEN}`, server)

    expect(server.askedFor).toStrictEqual([
      '/api/v1/avatars?token=a-test-token&page=0&size=100',
    ])
  })

  it('says that no avatar has been collected yet', async () => {
    const body = JSON.stringify({
      page: 0,
      size: 100,
      totalCount: 0,
      totals: NO_TOTALS,
      items: [],
    })

    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, body))

    const activeStatus = statusIn('active-roster')
    const dormantStatus = statusIn('dormant-roster')

    expect([activeStatus, dormantStatus]).toStrictEqual([
      'Noch kein Avatar erfasst.',
      'Kein Avatar ruht.',
    ])
  })

  it('counts each half against the whole guild, not against the loaded page', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, ROSTER_OF_ONE_LOADED_PAGE),
    )

    const activeCaption = captionOf('active-roster')
    const dormantCaption = captionOf('dormant-roster')

    expect([activeCaption, dormantCaption]).toStrictEqual([
      'Aktiv (30 Tage vor dem letzten Vorgang): 1 von 42 Avataren',
      'Ruhend: 1 von 42 Avataren',
    ])
  })

  it('shows the bank ledger the path names', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}`,
      alwaysServing(200, BANK_BODY),
    )

    const rows = rowTexts()

    expect(rows).toStrictEqual(['05.08.2026 12:15Calix500Einzahlung'])
  })

  it('shows the storage ledger the path names', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}`,
      alwaysServing(200, STORAGE_BODY),
    )

    const rows = rowTexts()

    expect(rows).toStrictEqual([
      '05.08.2026 12:15Calix3Erde-Eibenlanze7Entnahme',
    ])
  })

  it('asks the bank route of the avatar the path names', async () => {
    const server = alwaysServing(200, BANK_BODY)

    await shellAt(`/avatars/Erde%2FEibe/bank?token=${TOKEN}`, server)

    expect(server.askedFor).toStrictEqual([
      '/api/v1/avatars/Erde%2FEibe/bank?token=a-test-token&page=0&size=100',
    ])
  })

  it('says that a known avatar has no entries in this ledger', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}`,
      alwaysServing(200, EMPTY_LEDGER_BODY),
    )

    const status = shownStatus()

    expect(status).toBe('Für Calix ist hier kein Vorgang gespeichert.')
  })

  it('says that an avatar is unknown, which is not the same as an empty ledger', async () => {
    await shellAt(
      `/avatars/Nobody/storage?token=${TOKEN}`,
      alwaysServing(404, null),
    )

    const status = shownStatus()

    expect(status).toBe('Kein Avatar mit dem Namen Nobody.')
  })

  it('shows a refused token instead of an empty page', async () => {
    await shellAt('/overview?token=a-wrong-token', alwaysServing(401, null))

    const status = shownStatus()

    expect(status).toBe(
      'Kein gültiges Token: der Link braucht ein token in der Adresse.',
    )
  })

  it('asks without a token when the address carries none, and says so', async () => {
    const server = alwaysServing(401, null)

    await shellAt('/overview', server)

    const shown = shownStatus()

    expect({ askedFor: server.askedFor, shown }).toStrictEqual({
      askedFor: ['/api/v1/avatars?page=0&size=100'],
      shown: 'Kein gültiges Token: der Link braucht ein token in der Adresse.',
    })
  })

  it('says in German that a malformed overview body could not be loaded, not a blank page', async () => {
    const body = JSON.stringify({
      page: 0,
      size: 100,
      totalCount: 0,
      items: [],
    })

    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, body))

    const status = shownStatus()

    expect(status).toBe(
      'Die Daten konnten nicht geladen werden. Bitte später erneut versuchen.',
    )
  })

  it('says in German that the API could not be loaded, without its status', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(500, null))

    const status = shownStatus()

    expect(status).toBe(
      'Die Daten konnten nicht geladen werden. Bitte später erneut versuchen.',
    )
  })

  it('says in German that the data could not be loaded when the request itself is rejected', async () => {
    const unreachable: HttpGet = () =>
      Promise.reject(new TypeError('Failed to fetch'))

    window.history.replaceState(null, '', `/overview?token=${TOKEN}`)
    render(<App get={unreachable} />)
    await act(async () => undefined)

    const shown = shownStatus()
    const english = document.body.textContent.includes('Failed to fetch')

    expect({ shown, english }).toStrictEqual({
      shown:
        'Die Daten konnten nicht geladen werden. Bitte später erneut versuchen.',
      english: false,
    })
  })

  it('says in German that the admin status could not be loaded when it answers 400, without the status or its English', async () => {
    await shellAt('/admin', alwaysServing(400, '{"message":"Bad Request"}'))

    const shown = shownStatus()
    const status = document.body.textContent.includes('400')
    const english = document.body.textContent.includes('Bad Request')

    expect({ shown, status, english }).toStrictEqual({
      shown:
        'Die Daten konnten nicht geladen werden. Bitte später erneut versuchen.',
      status: false,
      english: false,
    })
  })

  it('says in German that a ledger could not be loaded when it answers 500, without the status or its English', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}`,
      alwaysServing(500, '{"message":"Internal Server Error"}'),
    )

    const shown = shownStatus()
    const status = document.body.textContent.includes('500')
    const english = document.body.textContent.includes('Internal Server Error')

    expect({ shown, status, english }).toStrictEqual({
      shown:
        'Die Daten konnten nicht geladen werden. Bitte später erneut versuchen.',
      status: false,
      english: false,
    })
  })

  it('says a storage ledger refused its token instead of calling it a failure', async () => {
    await shellAt(
      '/avatars/Calix/storage?token=a-wrong-token',
      alwaysServing(401, null),
    )

    const status = shownStatus()

    expect(status).toBe(
      'Kein gültiges Token: der Link braucht ein token in der Adresse.',
    )
  })

  it('is loading before the first answer arrives', () => {
    window.history.replaceState(null, '', `/overview?token=${TOKEN}`)
    const server = serving(() => ({ status: 200, body: OVERVIEW_BODY }))

    render(<App get={server.get} />)

    const status = shownStatus()

    expect(status).toBe('Wird geladen…')
  })

  it('shows the admin status page at its own address, without a token', async () => {
    const server = alwaysServing(200, ADMIN_STATUS_BODY)

    await shellAt('/admin', server)

    expect({
      askedFor: server.askedFor,
      unknownItemNames: screen.getByTestId('unknown-item-names').textContent,
      failedAvatarNames: screen.getByTestId('failed-avatar-names').textContent,
      roundTrips: screen.getByTestId('round-trips').textContent,
      roundTripAbstentions: screen.getByTestId('round-trip-abstentions')
        .textContent,
    }).toStrictEqual({
      askedFor: ['/api/v1/admin/status'],
      unknownItemNames: 'Unbekannte Items: Unobtainium',
      failedAvatarNames: 'Nicht aktualisierte Avatare: Zwerg',
      roundTrips: 'Verdacht auf Warenkreislauf: Alrik: 100 × Federn',
      roundTripAbstentions: 'Nicht beurteilbar (Rezept ungelesen): Zwerg: Erz',
    })
  })

  it('says in German that the admin status could not be loaded, without its status', async () => {
    await shellAt('/admin', alwaysServing(500, null))

    const status = shownStatus()

    expect(status).toBe(
      'Die Daten konnten nicht geladen werden. Bitte später erneut versuchen.',
    )
  })

  it('shows the admin status page before any collection has run', async () => {
    await shellAt(
      '/admin',
      alwaysServing(200, NEVER_COLLECTED_ADMIN_STATUS_BODY),
    )

    expect({
      lastUpdated: screen.getByTestId('last-updated').textContent,
      lastSuccessfulScrape: screen.getByTestId('last-successful-scrape')
        .textContent,
      lastSuccessfulRecompute: screen.getByTestId('last-successful-recompute')
        .textContent,
      lastScrapeFailure: screen.queryByTestId('last-scrape-failure'),
      lastRecomputeFailure: screen.queryByTestId('last-recompute-failure'),
      unknownItemNames: screen.queryByTestId('unknown-item-names'),
      failedAvatarNames: screen.queryByTestId('failed-avatar-names'),
      roundTrips: screen.queryByTestId('round-trips'),
      roundTripAbstentions: screen.queryByTestId('round-trip-abstentions'),
    }).toStrictEqual({
      lastUpdated: 'Stand: noch kein Abgleich gelaufen',
      lastSuccessfulScrape: 'Letzter Scrape: noch kein Scrape gelaufen',
      lastSuccessfulRecompute:
        'Letzte Neuberechnung: noch keine Neuberechnung gelaufen',
      lastScrapeFailure: null,
      lastRecomputeFailure: null,
      unknownItemNames: null,
      failedAvatarNames: null,
      roundTrips: null,
      roundTripAbstentions: null,
    })
  })

  it('tells a failed scrape apart from the recompute that still ran', async () => {
    await shellAt('/admin', alwaysServing(200, FAILED_RUN_ADMIN_STATUS_BODY))

    expect({
      lastSuccessfulScrape: screen.getByTestId('last-successful-scrape')
        .textContent,
      lastScrapeFailure: screen.getByTestId('last-scrape-failure').textContent,
      lastSuccessfulRecompute: screen.getByTestId('last-successful-recompute')
        .textContent,
      lastRecomputeFailure: screen.queryByTestId('last-recompute-failure'),
    }).toStrictEqual({
      lastSuccessfulScrape: 'Letzter Scrape: noch kein Scrape gelaufen',
      lastScrapeFailure: 'Letzter Scrape-Fehler: 05.08.2026 12:15',
      lastSuccessfulRecompute: 'Letzte Neuberechnung: 05.08.2026 12:15',
      lastRecomputeFailure: null,
    })
  })

  it('has no view for a path it does not know', async () => {
    await shellAt(`/nonsense?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    const status = shownStatus()

    expect(status).toBe('Für /nonsense gibt es keine Ansicht.')
  })

  it('asks again with the token of the address the browser moved to', async () => {
    const server = alwaysServing(200, OVERVIEW_BODY)
    await shellAt(`/overview?token=${TOKEN}`, server)

    await act(async () => {
      window.history.pushState(null, '', '/overview?token=another-token')
      window.dispatchEvent(new PopStateEvent('popstate'))
    })

    expect(server.askedFor).toStrictEqual([
      '/api/v1/avatars?token=a-test-token&page=0&size=100',
      '/api/v1/avatars?token=another-token&page=0&size=100',
    ])
  })

  it('shows the view the browser moves back to in its history', async () => {
    const server = serving((path) =>
      path === '/api/v1/avatars'
        ? { status: 200, body: OVERVIEW_BODY }
        : { status: 200, body: BANK_BODY },
    )
    await shellAt(`/overview?token=${TOKEN}`, server)
    await act(async () => {
      fireEvent.click(screen.getAllByRole('link', { name: 'Calix' })[0])
    })

    await act(async () => {
      window.history.pushState(null, '', `/overview?token=${TOKEN}`)
      window.dispatchEvent(new PopStateEvent('popstate'))
    })

    expect(screen.getByTestId('view-title').textContent).toBe('Übersicht')
  })

  it('shows an avatar whose name has to be escaped in the path', async () => {
    const body = JSON.stringify({
      page: 0,
      size: 100,
      totalCount: 1,
      items: [
        {
          timestamp: '2026-08-05T10:15:00Z',
          avatar: 'Öde/Grüße',
          quantity: 1,
          name: 'Magische Ätherbinde',
          quality: 100,
          transferType: 'DEPOSIT',
        },
      ],
    })
    const server = alwaysServing(200, body)

    await shellAt(
      `/avatars/${encodeURIComponent('Öde/Grüße')}/storage?token=${TOKEN}`,
      server,
    )

    const rows = rowTexts()

    expect({
      askedFor: server.askedFor,
      heading: screen.getByTestId('view-title').textContent,
      rows,
    }).toStrictEqual({
      askedFor: [
        '/api/v1/avatars/%C3%96de%2FGr%C3%BC%C3%9Fe/storage?token=a-test-token&page=0&size=100',
      ],
      heading: 'Lager von Öde/Grüße',
      rows: ['05.08.2026 12:15Öde/Grüße1Magische Ätherbinde100Einlagerung'],
    })
  })

  it('shows the data once even though StrictMode mounts the shell twice', async () => {
    const server = alwaysServing(200, OVERVIEW_BODY)
    window.history.replaceState(null, '', `/overview?token=${TOKEN}`)

    render(
      <StrictMode>
        <App get={server.get} />
      </StrictMode>,
    )
    await act(async () => undefined)

    const rows = rowTexts().length

    expect({
      rows,
      tables: screen.getAllByRole('table').length,
    }).toStrictEqual({ rows: 2, tables: 2 })
  })

  it('counts the entries of the window against the whole ledger', async () => {
    const body = JSON.stringify({
      page: 0,
      size: 100,
      totalCount: 340,
      items: [
        {
          timestamp: '2026-08-05T10:15:00Z',
          avatar: 'Calix',
          amount: 500,
          transferType: 'DEPOSIT',
        },
      ],
    })

    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}`,
      alwaysServing(200, body),
    )

    expect(
      screen.queryByRole('table', { name: '1 von 340 Einträgen' }),
    ).not.toBeNull()
  })

  it('offers the next page when more entries follow, but no way back on the first page', async () => {
    const body = JSON.stringify({
      page: 0,
      size: 100,
      totalCount: 340,
      items: [
        {
          timestamp: '2026-08-05T10:15:00Z',
          avatar: 'Calix',
          amount: 500,
          transferType: 'DEPOSIT',
        },
      ],
    })

    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}`,
      alwaysServing(200, body),
    )

    const next = screen.getByTestId('pagination-next').getAttribute('href')

    expect({
      previous: screen.queryByTestId('pagination-previous'),
      next,
    }).toStrictEqual({
      previous: null,
      next: '/avatars/Calix/bank?token=a-test-token&page=1',
    })
  })

  it('offers no next page once the last page is reached', async () => {
    const body = JSON.stringify({
      page: 3,
      size: 100,
      totalCount: 340,
      items: [
        {
          timestamp: '2026-08-05T10:15:00Z',
          avatar: 'Calix',
          amount: 500,
          transferType: 'DEPOSIT',
        },
      ],
    })

    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}&page=3`,
      alwaysServing(200, body),
    )

    const previous = screen
      .getByTestId('pagination-previous')
      .getAttribute('href')

    expect({
      previous,
      next: screen.queryByTestId('pagination-next'),
    }).toStrictEqual({
      previous: '/avatars/Calix/bank?token=a-test-token&page=2',
      next: null,
    })
  })

  it('offers no next page when the last page exactly fills the count', async () => {
    const body = JSON.stringify({
      page: 3,
      size: 100,
      totalCount: 400,
      items: [
        {
          timestamp: '2026-08-05T10:15:00Z',
          avatar: 'Calix',
          amount: 500,
          transferType: 'DEPOSIT',
        },
      ],
    })

    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}&page=3`,
      alwaysServing(200, body),
    )

    expect(screen.queryByTestId('pagination-next')).toBeNull()
  })

  it('follows the next-page link to a bookmarkable address and re-fetches', async () => {
    const server = servingByUrl((url) => {
      const body = url.includes('page=1')
        ? JSON.stringify({
            page: 1,
            size: 100,
            totalCount: 340,
            items: [
              {
                timestamp: '2026-08-05T10:15:00Z',
                avatar: 'Calix',
                amount: 700,
                transferType: 'WITHDRAWAL',
              },
            ],
          })
        : JSON.stringify({
            page: 0,
            size: 100,
            totalCount: 340,
            items: [
              {
                timestamp: '2026-08-05T10:15:00Z',
                avatar: 'Calix',
                amount: 500,
                transferType: 'DEPOSIT',
              },
            ],
          })

      return { status: 200, body }
    })

    await shellAt(`/avatars/Calix/bank?token=${TOKEN}`, server)

    await act(async () => {
      fireEvent.click(screen.getByTestId('pagination-next'))
    })

    const shown = rowTexts()
    const lastAsked = server.askedFor.at(-1)

    expect({
      shown,
      address: window.location.pathname + window.location.search,
      lastAsked,
    }).toStrictEqual({
      shown: ['05.08.2026 12:15Calix700Entnahme'],
      address: '/avatars/Calix/bank?token=a-test-token&page=1',
      lastAsked:
        '/api/v1/avatars/Calix/bank?token=a-test-token&page=1&size=100',
    })
  })

  it('follows an avatar link to that avatar ledger without leaving the shell', async () => {
    const server = serving((path) =>
      path === '/api/v1/avatars'
        ? { status: 200, body: OVERVIEW_BODY }
        : { status: 200, body: BANK_BODY },
    )
    await shellAt(`/overview?token=${TOKEN}`, server)

    await act(async () => {
      fireEvent.click(screen.getAllByRole('link', { name: 'Calix' })[0])
    })

    const shown = rowTexts()
    const lastAsked = server.askedFor.at(-1)

    expect({
      shown,
      address: window.location.pathname + window.location.search,
      lastAsked,
    }).toStrictEqual({
      shown: ['05.08.2026 12:15Calix500Einzahlung'],
      address: '/avatars/Calix/bank?token=a-test-token',
      lastAsked:
        '/api/v1/avatars/Calix/bank?token=a-test-token&page=0&size=100',
    })
  })

  it("follows the storage link of the overview to that avatar's storage ledger", async () => {
    const server = serving((path) =>
      path === '/api/v1/avatars'
        ? { status: 200, body: OVERVIEW_BODY }
        : { status: 200, body: STORAGE_BODY },
    )
    await shellAt(`/overview?token=${TOKEN}`, server)

    await act(async () => {
      fireEvent.click(
        screen.getAllByRole('link', { name: '05.08.2026 12:15' })[0],
      )
    })

    const shown = rowTexts()
    const lastAsked = server.askedFor.at(-1)

    expect({
      shown,
      address: window.location.pathname + window.location.search,
      lastAsked,
    }).toStrictEqual({
      shown: ['05.08.2026 12:15Calix3Erde-Eibenlanze7Entnahme'],
      address: '/avatars/Calix/storage?token=a-test-token',
      lastAsked:
        '/api/v1/avatars/Calix/storage?token=a-test-token&page=0&size=100',
    })
  })

  it('marks only the ledger it shows as the current page in the navigation', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}`,
      alwaysServing(200, STORAGE_BODY),
    )

    const marked = screen.getAllByRole('link').map((link) => ({
      name: link.textContent,
      current: link.getAttribute('aria-current'),
    }))

    expect(marked).toStrictEqual([
      { name: 'Übersicht', current: null },
      { name: 'Bank', current: null },
      { name: 'Lager', current: 'page' },
    ])
  })

  it('gets back to the overview from a ledger', async () => {
    const server = serving((path) =>
      path === '/api/v1/avatars'
        ? { status: 200, body: OVERVIEW_BODY }
        : { status: 200, body: BANK_BODY },
    )
    await shellAt(`/avatars/Calix/bank?token=${TOKEN}`, server)

    await act(async () => {
      fireEvent.click(screen.getByRole('link', { name: 'Übersicht' }))
    })

    const rows = rowTexts().length

    expect({
      rows,
      address: window.location.pathname + window.location.search,
    }).toStrictEqual({ rows: 2, address: '/overview?token=a-test-token' })
  })

  it('asks the bank route for the page the address names', async () => {
    const server = alwaysServing(200, BANK_BODY)

    await shellAt(`/avatars/Calix/bank?token=${TOKEN}&page=1`, server)

    expect(server.askedFor).toStrictEqual([
      '/api/v1/avatars/Calix/bank?token=a-test-token&page=1&size=100',
    ])
  })

  it('asks the storage route for the sort the address names', async () => {
    const server = alwaysServing(200, STORAGE_BODY)

    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}&page=1&sort=quantity&direction=descending`,
      server,
    )

    expect(server.askedFor).toStrictEqual([
      '/api/v1/avatars/Calix/storage?token=a-test-token&page=1&size=100&sort=quantity&direction=descending',
    ])
  })

  it('says in German that a ledger cannot be sorted the way its address names', async () => {
    const server = alwaysServing(400, '{"message":"Bad Request"}')

    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}&sort=Menge&direction=descending`,
      server,
    )

    const shown = shownStatus()

    expect({
      shown,
      askedFor: server.askedFor,
    }).toStrictEqual({
      shown: 'Diese Seite gibt es nicht.',
      askedFor: [
        '/api/v1/avatars/Calix/storage?token=a-test-token&page=0&size=100&sort=Menge&direction=descending',
      ],
    })
  })

  it('says in German that a bank ledger cannot be sorted the way its address names', async () => {
    const server = alwaysServing(400, '{"message":"Bad Request"}')

    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}&sort=Betrag&direction=descending`,
      server,
    )

    const shown = shownStatus()

    expect({
      shown,
      askedFor: server.askedFor,
    }).toStrictEqual({
      shown: 'Diese Seite gibt es nicht.',
      askedFor: [
        '/api/v1/avatars/Calix/bank?token=a-test-token&page=0&size=100&sort=Betrag&direction=descending',
      ],
    })
  })

  it('asks the API for the whole ledger sorted when a column is chosen', async () => {
    const server = alwaysServing(200, STORAGE_BODY)
    await shellAt(`/avatars/Calix/storage?token=${TOKEN}`, server)

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /Menge/ }))
    })

    const lastAsked = server.askedFor.at(-1)

    expect({
      address: window.location.pathname + window.location.search,
      lastAsked,
    }).toStrictEqual({
      address:
        '/avatars/Calix/storage?token=a-test-token&sort=quantity&direction=ascending',
      lastAsked:
        '/api/v1/avatars/Calix/storage?token=a-test-token&page=0&size=100&sort=quantity&direction=ascending',
    })
  })

  it('starts a newly chosen sort on the first page', async () => {
    const thirdPage = JSON.stringify({
      ...(JSON.parse(STORAGE_BODY) as object),
      page: 2,
      totalCount: 340,
    })
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}&page=2`,
      alwaysServing(200, thirdPage),
    )

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /Gegenstand/ }))
    })

    expect(window.location.search).toBe(
      '?token=a-test-token&sort=name&direction=ascending',
    )
  })

  it('asks for the other direction when the sorted column is chosen again', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}&sort=quantity&direction=ascending`,
      alwaysServing(200, STORAGE_BODY),
    )

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /Menge/ }))
    })

    expect(window.location.search).toBe(
      '?token=a-test-token&sort=quantity&direction=descending',
    )
  })

  it('asks the bank route for the sort the address names and again for a chosen one', async () => {
    const server = alwaysServing(200, BANK_BODY)
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}&sort=amount&direction=descending`,
      server,
    )

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /Betrag/ }))
    })

    expect(server.askedFor).toStrictEqual([
      '/api/v1/avatars/Calix/bank?token=a-test-token&page=0&size=100&sort=amount&direction=descending',
      '/api/v1/avatars/Calix/bank?token=a-test-token&page=0&size=100&sort=amount&direction=ascending',
    ])
  })

  it.each([
    ['bank', 'Zeitpunkt', 'timestamp', BANK_BODY],
    ['bank', 'Avatar', 'avatar', BANK_BODY],
    ['bank', 'Betrag', 'amount', BANK_BODY],
    ['bank', 'Vorgang', 'transferType', BANK_BODY],
    ['storage', 'Zeitpunkt', 'timestamp', STORAGE_BODY],
    ['storage', 'Avatar', 'avatar', STORAGE_BODY],
    ['storage', 'Menge', 'quantity', STORAGE_BODY],
    ['storage', 'Gegenstand', 'name', STORAGE_BODY],
    ['storage', 'Qualität', 'quality', STORAGE_BODY],
    ['storage', 'Vorgang', 'transferType', STORAGE_BODY],
  ])(
    'asks the %s ledger for its column when %s is chosen, by the wire name %s',
    async (ledger, header, wireName, body) => {
      const server = alwaysServing(200, body)
      await shellAt(`/avatars/Calix/${ledger}?token=${TOKEN}`, server)

      await act(async () => {
        fireEvent.click(screen.getByRole('button', { name: header }))
      })

      const lastRequest = server.askedFor.at(-1)

      expect(lastRequest).toContain(`&sort=${wireName}&`)
    },
  )

  it('marks the column the address sorts the bank ledger by', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}&sort=amount&direction=ascending`,
      alwaysServing(200, BANK_BODY),
    )

    const marks = screen
      .getAllByRole('columnheader')
      .map((header) => header.getAttribute('aria-sort'))

    expect(marks).toStrictEqual(['none', 'none', 'ascending', 'none'])
  })

  it('reverses the sort of the bank ledger when its column is chosen again', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}&sort=amount&direction=ascending`,
      alwaysServing(200, BANK_BODY),
    )

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /Betrag/ }))
    })

    expect(window.location.search).toBe(
      '?token=a-test-token&sort=amount&direction=descending',
    )
  })

  it('keeps the sort of the bank ledger in the link to the next page', async () => {
    const body = JSON.stringify({
      ...(JSON.parse(BANK_BODY) as object),
      totalCount: 340,
    })

    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}&sort=amount&direction=ascending`,
      alwaysServing(200, body),
    )

    const nextHref = screen.getByTestId('pagination-next').getAttribute('href')

    expect(nextHref).toBe(
      '/avatars/Calix/bank?token=a-test-token&page=1&sort=amount&direction=ascending',
    )
  })

  it('marks no column when the address names a direction the table does not know', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}&sort=quantity&direction=up`,
      alwaysServing(200, STORAGE_BODY),
    )

    const marks = screen
      .getAllByRole('columnheader')
      .map((header) => header.getAttribute('aria-sort'))

    expect(marks).toStrictEqual(Array<string>(6).fill('none'))
  })

  it('links the bank and the storage ledger without the sort of the sorted address', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}&sort=quantity&direction=descending`,
      alwaysServing(200, STORAGE_BODY),
    )

    const ledgers = ['Bank', 'Lager'].map((name) =>
      screen.getByRole('link', { name }).getAttribute('href'),
    )

    expect(ledgers).toStrictEqual([
      '/avatars/Calix/bank?token=a-test-token',
      '/avatars/Calix/storage?token=a-test-token',
    ])
  })

  it('completes an address that names only its column with the descending direction, in the request and in the mark', async () => {
    const server = alwaysServing(200, BANK_BODY)

    await shellAt(`/avatars/Calix/bank?token=${TOKEN}&sort=amount`, server)

    const marks = screen
      .getAllByRole('columnheader')
      .map((header) => header.getAttribute('aria-sort'))

    expect({ askedFor: server.askedFor, marks }).toStrictEqual({
      askedFor: [
        '/api/v1/avatars/Calix/bank?token=a-test-token&page=0&size=100&sort=amount&direction=descending',
      ],
      marks: ['none', 'none', 'descending', 'none'],
    })
  })

  it('marks the column the address sorts the ledger by', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}&sort=quantity&direction=descending`,
      alwaysServing(200, STORAGE_BODY),
    )

    const marks = screen
      .getAllByRole('columnheader')
      .map((header) => header.getAttribute('aria-sort'))

    expect(marks).toStrictEqual([
      'none',
      'none',
      'descending',
      'none',
      'none',
      'none',
    ])
  })

  it('shows the rows in the order the API sorted them rather than sorting them again', async () => {
    const body = JSON.stringify({
      page: 0,
      size: 100,
      totalCount: 2,
      items: ['Stahlbarren', 'Stahl-Rüstung'].map((name) => ({
        timestamp: '2026-08-05T10:15:00Z',
        avatar: 'Calix',
        quantity: 1,
        name,
        quality: 7,
        transferType: 'DEPOSIT',
      })),
    })

    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}&sort=name&direction=descending`,
      alwaysServing(200, body),
    )
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /Gegenstand/ }))
    })

    const names = screen
      .getAllByTestId('cell-name')
      .map((cell) => cell.textContent)

    expect(names).toStrictEqual(['Stahlbarren', 'Stahl-Rüstung'])
  })

  it('keeps the sort in the links to the next and the previous page', async () => {
    const body = JSON.stringify({
      page: 1,
      size: 100,
      totalCount: 340,
      items: [],
    })

    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}&page=1&sort=quantity&direction=descending`,
      alwaysServing(200, body),
    )

    const previous = screen
      .getByTestId('pagination-previous')
      .getAttribute('href')
    const next = screen.getByTestId('pagination-next').getAttribute('href')

    expect({ previous, next }).toStrictEqual({
      previous:
        '/avatars/Calix/storage?token=a-test-token&sort=quantity&direction=descending',
      next: '/avatars/Calix/storage?token=a-test-token&page=2&sort=quantity&direction=descending',
    })
  })

  it('renders the empty state for a page past the end, not an error', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}&page=50`,
      alwaysServing(200, EMPTY_LEDGER_BODY),
    )

    const status = shownStatus()

    expect(status).toBe('Für Calix ist hier kein Vorgang gespeichert.')
  })

  it('says in German that an unclamped invalid page does not exist', async () => {
    const body = '{"message":"page: must be at least 0"}'
    const server = alwaysServing(400, body)

    await shellAt(`/avatars/Calix/bank?token=${TOKEN}&page=-1`, server)

    const shown = shownStatus()

    expect({
      shown,
      askedFor: server.askedFor,
    }).toStrictEqual({
      shown: 'Diese Seite gibt es nicht.',
      askedFor: [
        '/api/v1/avatars/Calix/bank?token=a-test-token&page=-1&size=100',
      ],
    })
  })

  it('says in German that a storage ledger page does not exist', async () => {
    const body = '{"message":"page: must be at least 0"}'

    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}&page=-1`,
      alwaysServing(400, body),
    )

    const status = shownStatus()

    expect(status).toBe('Diese Seite gibt es nicht.')
  })

  it('passes a blank page value through to the API unchanged, not defaulted', async () => {
    const server = alwaysServing(400, '{"message":"page: must not be null"}')

    await shellAt(`/avatars/Calix/bank?token=${TOKEN}&page=`, server)

    expect(server.askedFor).toStrictEqual([
      '/api/v1/avatars/Calix/bank?token=a-test-token&page=NaN&size=100',
    ])
  })

  it('passes a page written as a word through to the API as no number and says the page does not exist', async () => {
    const server = alwaysServing(400, '{"message":"Bad Request"}')

    await shellAt(`/avatars/Calix/bank?token=${TOKEN}&page=zwei`, server)

    const shown = shownStatus()

    expect({
      shown,
      askedFor: server.askedFor,
    }).toStrictEqual({
      shown: 'Diese Seite gibt es nicht.',
      askedFor: [
        '/api/v1/avatars/Calix/bank?token=a-test-token&page=NaN&size=100',
      ],
    })
  })

  it("links from below the guild's position to the explanation of its figures", async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    const link = screen.getByRole('link', {
      name: 'Wie entstehen diese Zahlen?',
    })

    const href = link.getAttribute('href')
    const afterThePosition =
      screen.getByTestId('stat-header').compareDocumentPosition(link) &
      Node.DOCUMENT_POSITION_FOLLOWING

    expect({ href, afterThePosition }).toStrictEqual({
      href: '/explanation?token=a-test-token',
      afterThePosition: Node.DOCUMENT_POSITION_FOLLOWING,
    })
  })

  it('opens the explanation page when the member follows its link from the overview', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(
      screen.getByRole('link', { name: 'Wie entstehen diese Zahlen?' }),
    )
    await act(async () => undefined)

    expect(screen.getByTestId('view-title').textContent).toBe(
      'So entstehen eure Zahlen',
    )
  })

  it('heads the explanation page at its own address', async () => {
    await shellAt(`/explanation?token=${TOKEN}`, alwaysServing(200, '{}'))

    expect(screen.getByTestId('view-title').textContent).toBe(
      'So entstehen eure Zahlen',
    )
  })

  it('asks the server nothing for the explanation page', async () => {
    const server = alwaysServing(200, '{}')
    await shellAt(`/explanation?token=${TOKEN}`, server)

    expect(server.askedFor).toStrictEqual([])
  })

  it('heads each group one level below the page and each question one level below its group', async () => {
    await shellAt(`/explanation?token=${TOKEN}`, alwaysServing(200, '{}'))

    const groups = new Set(
      screen.getAllByTestId('group-title').map((title) => title.tagName),
    )
    const questions = new Set(
      screen.getAllByTestId('question').map((question) => question.tagName),
    )

    expect({
      page: screen.getByTestId('view-title').tagName,
      groups,
      questions,
    }).toStrictEqual({
      page: 'H2',
      groups: new Set(['H3']),
      questions: new Set(['H4']),
    })
  })

  it('introduces the invented members and the market stall before the first question', async () => {
    await shellAt(`/explanation?token=${TOKEN}`, alwaysServing(200, '{}'))

    const legend = screen
      .getAllByTestId('legend-entry')
      .map((entry) => entry.textContent)

    expect(legend).toStrictEqual([
      'Mira Mitglied ist ein Mitglied wie ihr: Sie zahlt ein, lagert ein, nimmt heraus und stellt die Übersicht um.',
      'Hanna Handwerk fertigt Waren für die Gilde aus Zutaten aus dem Gildenlager.',
      'Gero Gildenhändler handelt für die Gilde: Er nimmt Waren zum Verkauf heraus und legt Gekauftes ein.',
      'Der Marktstand ist der Händler des Spiels, mit festen Preisen und endlosem Vorrat; dort kauft ihr Handwerksmaterial und Pfeile, Bolzen und Magieessenz, und dort könnt ihr feilschen.',
    ])
  })

  it('asks its questions under their groups', async () => {
    await shellAt(`/explanation?token=${TOKEN}`, alwaysServing(200, '{}'))

    const groups = screen
      .getAllByTestId('explanation-group')
      .map((group) => [
        within(group).getByTestId('group-title').textContent,
        within(group).getAllByTestId('question').length,
      ])

    expect(groups).toStrictEqual([
      ['Was zählt', 4],
      ['Eure Zahl', 2],
      ['Die Gilde', 7],
      ['Die Übersicht lesen', 5],
      ['Umstellen', 5],
    ])
  })

  it('answers a question with its rule, its worked example and why', async () => {
    await shellAt(`/explanation?token=${TOKEN}`, alwaysServing(200, '{}'))

    const answer = screen.getAllByTestId('answer')[0]
    const calculations =
      answer === undefined
        ? []
        : within(answer)
            .getAllByTestId('calculation')
            .map((line) => line.textContent)

    expect(
      answer === undefined
        ? null
        : {
            question: within(answer).getByTestId('question').textContent,
            rule: within(answer).getByTestId('rule').textContent,
            example: within(answer).getByTestId('example-intro').textContent,
            calculations,
            why: within(answer).getByTestId('why').textContent,
          },
    ).toStrictEqual({
      question: 'Was zählt Gold in der Gildenbank?',
      rule: 'Gold zählt, wie es ist: Jede Einzahlung in die Gildenbank steht voll unter „Bank-Einzahlung“, jede Auszahlung voll unter „Bank-Auszahlung“.',
      example:
        'Beispiel: Mira Mitglied zahlt 1.000 Gold ein und hebt später 200 Gold ab.',
      calculations: [
        'Mira Mitglied Bank-Einzahlung: 1.000 Gold × 100 % = 1.000 Gold',
        'Mira Mitglied Bank-Auszahlung: 200 Gold × 100 % = 200 Gold',
      ],
      why: 'Warum? Gold ist Gold: Die Gilde kann damit genau so viel kaufen wie ihr.',
    })
  })

  it('lists the points of an answer that has them as part of its rule', async () => {
    await shellAt(`/explanation?token=${TOKEN}`, alwaysServing(200, '{}'))

    const answer = screen
      .getAllByTestId('answer')
      .find(
        (candidate) =>
          within(candidate).getByTestId('question').textContent ===
          'Was kann das Protokoll nicht zeigen?',
      )

    expect(
      answer === undefined
        ? null
        : within(within(answer).getByTestId('rule')).getAllByRole('listitem')
            .length,
    ).toBe(3)
  })

  it("offers only the overview, not marked as current, in the explanation page's navigation", async () => {
    await shellAt(`/explanation?token=${TOKEN}`, alwaysServing(200, '{}'))

    const navigation = screen
      .getAllByRole('navigation')[0]
      ?.querySelectorAll('a')

    const offered = [...(navigation ?? [])].map(
      (link) =>
        `${link.textContent} ${link.getAttribute('href') ?? ''} ${link.getAttribute('aria-current') ?? 'not current'}`,
    )

    expect(offered).toStrictEqual([
      'Übersicht /overview?token=a-test-token not current',
    ])
  })

  it('offers the ledgers of the avatar it shows in its navigation', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}`,
      alwaysServing(200, BANK_BODY),
    )

    const navigation = screen
      .getAllByRole('link')
      .map(
        (link) =>
          `${link.textContent ?? ''} ${link.getAttribute('href') ?? ''}`,
      )

    expect(navigation).toStrictEqual([
      'Übersicht /overview?token=a-test-token',
      'Bank /avatars/Calix/bank?token=a-test-token',
      'Lager /avatars/Calix/storage?token=a-test-token',
    ])
  })
})
