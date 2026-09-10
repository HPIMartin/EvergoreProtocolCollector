import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
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
      lastBankActivity: '2026-08-04T09:30:00Z',
      lastStorageActivity: '2026-08-05T10:15:00Z',
      staleSumsFrom: null,
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
      lastBankActivity: null,
      lastStorageActivity: '2026-07-31T21:05:00Z',
      staleSumsFrom: null,
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
      lastBankActivity: '2026-08-04T09:30:00Z',
      lastStorageActivity: '2026-08-05T10:15:00Z',
      staleSumsFrom: null,
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
      lastBankActivity: '2026-08-05T09:58:00Z',
      lastStorageActivity: '2026-07-31T21:05:00Z',
      staleSumsFrom: '2026-07-30T01:12:00Z',
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
      lastBankActivity: '2026-08-04T09:30:00Z',
      lastStorageActivity: '2026-08-05T10:15:00Z',
      staleSumsFrom: null,
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
      lastBankActivity: '2026-08-04T09:30:00Z',
      lastStorageActivity: '2026-08-05T10:15:00Z',
      staleSumsFrom: null,
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
      lastBankActivity: '2026-08-04T09:30:00Z',
      lastStorageActivity: '2026-08-05T10:15:00Z',
      staleSumsFrom: null,
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
      lastBankActivity: null,
      lastStorageActivity: '2026-07-31T21:05:00Z',
      staleSumsFrom: null,
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
  containsStaleSums: false,
}

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
})

const FAILED_RUN_ADMIN_STATUS_BODY = JSON.stringify({
  lastUpdated: '2026-08-05T10:15:00Z',
  lastSuccessfulScrape: null,
  lastScrapeFailure: '2026-08-05T10:15:20Z',
  lastSuccessfulRecompute: '2026-08-05T10:15:30Z',
  lastRecomputeFailure: null,
  unknownItemNames: [],
  failedAvatarNames: [],
})

const NEVER_COLLECTED_ADMIN_STATUS_BODY = JSON.stringify({
  lastUpdated: null,
  lastSuccessfulScrape: null,
  lastScrapeFailure: null,
  lastSuccessfulRecompute: null,
  lastRecomputeFailure: null,
  unknownItemNames: [],
  failedAvatarNames: [],
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

    expect(rowTexts()).toStrictEqual([
      'Calix3.4001.2005002002.50005.08.2026 12:1504.08.2026 11:30',
      'Erde-Eibenlanze5020000-15031.07.2026 23:05–',
    ])
  })

  it('marks the row whose sums the last collection did not refresh', async () => {
    await shellAt(
      `/overview?token=${TOKEN}`,
      alwaysServing(200, OVERVIEW_BODY_WITH_A_STALE_ROW),
    )

    expect(
      screen.getAllByTestId('data-row').map((row) => row.dataset.stale),
    ).toStrictEqual([undefined, 'true'])
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

  it('names the overview columns in German', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(headerTexts()).toStrictEqual([
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

    expect(statTexts()).toStrictEqual([
      'Gildenbank2.050',
      'Gildenlagerwert600',
      'Gildenspende400',
      'Handwerkssubventionen100',
    ])
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

    expect(headerTexts()).toStrictEqual([
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

  it('sums the guild balance in the total row of the switched column', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(screen.getByTestId('total-row').textContent).toContain('2.650')
  })

  it('tones the switched column like the contribution it replaces', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(tonesOf('figure')).toStrictEqual(['neutral', 'debit'])
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

    expect(statTexts()).toStrictEqual([
      'Gildenbank2.050',
      'GildenlagerwertNoch nicht berechnet.',
      'GildenspendeNoch nicht berechnet.',
      'HandwerkssubventionenNoch nicht berechnet.',
    ])
  })

  it('tones each of the four figures the way its meaning asks for', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(statTonesOf()).toStrictEqual([
      'credit',
      'credit',
      'credit',
      'neutral',
    ])
  })

  it('names what the switch controls, which is its only accessible name', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(
      screen.getByTestId('switch-figure').querySelector('legend')?.textContent,
    ).toBe('Letzte Spalte')
  })

  it('offers the figure after the deductions before the one before them', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(
      screen
        .getAllByRole('radio')
        .map((radio) => radio.parentElement?.textContent),
    ).toStrictEqual(['Nach Abzügen', 'Vor Abzügen'])
  })

  it('carries no hint on a row whose figure before the deductions is known', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(screen.queryByTestId('cell-mark')).toBeNull()
  })

  it('shows a row before the deductions as its figure after them plus what the guild kept', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(rowTexts()[0]).toContain('2.800')
  })

  it('keeps the figure after the deductions while the switch stays on it', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(rowTexts()[0]).toContain('2.500')
  })

  it('keeps the table usable when the reader sorts the sixth column and then switches it', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(screen.getByRole('button', { name: /Nach Abzügen/ }))
    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(rowTexts().join(' ')).toContain('2.800')
  })

  it('keeps a sort on the sixth column when the switch changes what it shows', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(screen.getByRole('button', { name: /Nach Abzügen/ }))
    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(rowTexts()[0]).toContain('Erde-Eibenlanze')
  })

  it('does not widen the table when the switch changes the last column', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(headerTexts()).toHaveLength(8)
  })

  it('names the bank ledger columns in German', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}`,
      alwaysServing(200, BANK_BODY),
    )

    expect(headerTexts()).toStrictEqual([
      'Zeitpunkt',
      'Avatar',
      'Betrag',
      'Vorgang',
    ])
  })

  it('names the storage ledger columns in German', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}`,
      alwaysServing(200, STORAGE_BODY),
    )

    expect(headerTexts()).toStrictEqual([
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

    expect({
      bankWithdrawn: tonesOf('bankWithdrawn'),
      bankDeposited: tonesOf('bankDeposited'),
      storageWithdrawn: tonesOf('storageWithdrawn'),
      storageDeposited: tonesOf('storageDeposited'),
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

    expect(
      screen
        .getAllByTestId('cell-lastBankActivity')
        .map((cell) => cell.textContent),
    ).toStrictEqual(['04.08.2026 11:30', '–'])
  })

  it('marks a negative guild value as taken from the guild', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(tonesOf('figure')).toStrictEqual(['neutral', 'debit'])
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

    expect(shownStatus()).toBe('Noch kein Avatar erfasst.')
  })

  it('counts the avatars of the window against the whole guild', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(
      screen.queryByRole('table', { name: '2 von 2 Avataren' }),
    ).not.toBeNull()
  })

  it('shows the bank ledger the path names', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}`,
      alwaysServing(200, BANK_BODY),
    )

    expect(rowTexts()).toStrictEqual(['05.08.2026 12:15Calix500Einlagerung'])
  })

  it('shows the storage ledger the path names', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}`,
      alwaysServing(200, STORAGE_BODY),
    )

    expect(rowTexts()).toStrictEqual([
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

    expect(shownStatus()).toBe('Für Calix ist hier kein Vorgang gespeichert.')
  })

  it('says that an avatar is unknown, which is not the same as an empty ledger', async () => {
    await shellAt(
      `/avatars/Nobody/storage?token=${TOKEN}`,
      alwaysServing(404, null),
    )

    expect(shownStatus()).toBe('Kein Avatar mit dem Namen Nobody.')
  })

  it('shows a refused token instead of an empty page', async () => {
    await shellAt('/overview?token=a-wrong-token', alwaysServing(401, null))

    expect(shownStatus()).toBe(
      'Kein gültiges Token: der Link braucht ein token in der Adresse.',
    )
  })

  it('asks without a token when the address carries none, and says so', async () => {
    const server = alwaysServing(401, null)

    await shellAt('/overview', server)

    expect({ askedFor: server.askedFor, shown: shownStatus() }).toStrictEqual({
      askedFor: ['/api/v1/avatars?page=0&size=100'],
      shown: 'Kein gültiges Token: der Link braucht ein token in der Adresse.',
    })
  })

  it('surfaces a malformed overview body as a failure, not a blank page', async () => {
    const body = JSON.stringify({
      page: 0,
      size: 100,
      totalCount: 0,
      items: [],
    })

    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, body))

    expect(shownStatus()).toBe(
      'Fehler: The API answered an envelope without guild-wide totals',
    )
  })

  it('shows a failure of the API with its reason', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(500, null))

    expect(shownStatus()).toBe('Fehler: The API answered 500')
  })

  it('is loading before the first answer arrives', () => {
    window.history.replaceState(null, '', `/overview?token=${TOKEN}`)
    const server = serving(() => ({ status: 200, body: OVERVIEW_BODY }))

    render(<App get={server.get} />)

    expect(shownStatus()).toBe('Wird geladen…')
  })

  it('shows the admin status page at its own address, without a token', async () => {
    const server = alwaysServing(200, ADMIN_STATUS_BODY)

    await shellAt('/admin', server)

    expect({
      askedFor: server.askedFor,
      unknownItemNames: screen.getByTestId('unknown-item-names').textContent,
      failedAvatarNames: screen.getByTestId('failed-avatar-names').textContent,
    }).toStrictEqual({
      askedFor: ['/api/v1/admin/status'],
      unknownItemNames: 'Unbekannte Items: Unobtainium',
      failedAvatarNames: 'Nicht aktualisierte Avatare: Zwerg',
    })
  })

  it('shows a failure of the admin status request with its reason', async () => {
    await shellAt('/admin', alwaysServing(500, null))

    expect(shownStatus()).toBe('Fehler: The API answered 500')
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
    }).toStrictEqual({
      lastUpdated: 'Stand: noch kein Abgleich gelaufen',
      lastSuccessfulScrape: 'Letzter Scrape: noch kein Scrape gelaufen',
      lastSuccessfulRecompute:
        'Letzte Neuberechnung: noch keine Neuberechnung gelaufen',
      lastScrapeFailure: null,
      lastRecomputeFailure: null,
      unknownItemNames: null,
      failedAvatarNames: null,
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

    expect(shownStatus()).toBe('Für /nonsense gibt es keine Ansicht.')
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

    expect({
      askedFor: server.askedFor,
      heading: screen.getByTestId('view-title').textContent,
      rows: rowTexts(),
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

    expect({
      rows: rowTexts().length,
      tables: screen.getAllByRole('table').length,
    }).toStrictEqual({ rows: 2, tables: 1 })
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

    expect({
      previous: screen.queryByTestId('pagination-previous'),
      next: screen.getByTestId('pagination-next').getAttribute('href'),
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

    expect({
      previous: screen.getByTestId('pagination-previous').getAttribute('href'),
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

    expect({
      shown: rowTexts(),
      address: window.location.pathname + window.location.search,
      lastAsked: server.askedFor.at(-1),
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

    expect({
      shown: rowTexts(),
      address: window.location.pathname + window.location.search,
      lastAsked: server.askedFor.at(-1),
    }).toStrictEqual({
      shown: ['05.08.2026 12:15Calix500Einlagerung'],
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

    expect({
      shown: rowTexts(),
      address: window.location.pathname + window.location.search,
      lastAsked: server.askedFor.at(-1),
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

    expect({
      rows: rowTexts().length,
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

  it('renders the empty state for a page past the end, not an error', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}&page=50`,
      alwaysServing(200, EMPTY_LEDGER_BODY),
    )

    expect(shownStatus()).toBe('Für Calix ist hier kein Vorgang gespeichert.')
  })

  it('surfaces an unclamped invalid page as a visible failure', async () => {
    const body = '{"message":"page: must be at least 0"}'
    const server = alwaysServing(400, body)

    await shellAt(`/avatars/Calix/bank?token=${TOKEN}&page=-1`, server)

    expect({
      shown: shownStatus(),
      askedFor: server.askedFor,
    }).toStrictEqual({
      shown: 'Fehler: The API answered 400',
      askedFor: [
        '/api/v1/avatars/Calix/bank?token=a-test-token&page=-1&size=100',
      ],
    })
  })

  it('passes a blank page value through to the API unchanged, not defaulted', async () => {
    const server = alwaysServing(400, '{"message":"page: must not be null"}')

    await shellAt(`/avatars/Calix/bank?token=${TOKEN}&page=`, server)

    expect(server.askedFor).toStrictEqual([
      '/api/v1/avatars/Calix/bank?token=a-test-token&page=NaN&size=100',
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
