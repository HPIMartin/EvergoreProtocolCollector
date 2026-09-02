import { describe, expect, it } from 'vitest'

import { bankPageFrom, overviewFrom, storagePageFrom } from './wire.ts'
import { MalformedResponse } from './apiErrors.ts'

const overviewBody = {
  lastUpdated: '2026-08-05T10:15:00Z',
  page: 0,
  size: 100,
  totalCount: 2,
  totals: {
    bankWithdrawn: 1400,
    bankDeposited: 3450,
    storageWithdrawn: 200,
    storageDeposited: 500,
    net: 2350,
  },
  items: [
    {
      avatar: 'Calix',
      bankWithdrawn: 1200,
      bankDeposited: 3400,
      storageWithdrawn: 200,
      storageDeposited: 500,
      net: 2500,
    },
    {
      avatar: 'Erde-Eibenlanze',
      bankWithdrawn: 200,
      bankDeposited: 50,
      storageWithdrawn: 0,
      storageDeposited: 0,
      net: -150,
    },
  ],
}

const bankBody = {
  page: 1,
  size: 2,
  totalCount: 340,
  items: [
    {
      timestamp: '2026-08-05T10:15:00Z',
      avatar: 'Calix',
      amount: 500,
      transferType: 'DEPOSIT',
    },
  ],
}

const storageBody = {
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
}

function envelopeWithout(field: string): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(overviewBody).filter(([name]) => name !== field),
  )
}

function summaryWithout(field: string): Record<string, unknown> {
  const complete = overviewBody.items[0] as Record<string, unknown>

  return Object.fromEntries(
    Object.entries(complete).filter(([name]) => name !== field),
  )
}

describe('the overview wire shape', () => {
  it('reads the freshness of the numbers as an instant', () => {
    const overview = overviewFrom(overviewBody)

    expect(overview.lastUpdated).toStrictEqual(new Date('2026-08-05T10:15:00Z'))
  })

  it('reads a missing collection run as no freshness at all', () => {
    const overview = overviewFrom({ ...overviewBody, lastUpdated: null })

    expect(overview.lastUpdated).toBeNull()
  })

  it('refuses a body whose freshness field is absent', () => {
    const reading = () =>
      overviewFrom({ page: 0, size: 100, totalCount: 0, items: [] })

    expect(reading).toThrow(MalformedResponse)
  })

  it('reads the guild-wide totals of the envelope', () => {
    const overview = overviewFrom(overviewBody)

    expect(overview.totals).toStrictEqual({
      bankWithdrawn: 1400,
      bankDeposited: 3450,
      storageWithdrawn: 200,
      storageDeposited: 500,
      net: 2350,
    })
  })

  it('refuses a body without the guild-wide totals', () => {
    const reading = () => overviewFrom(envelopeWithout('totals'))

    expect(reading).toThrow(MalformedResponse)
  })

  it('reads the unpaged total independently of the item count', () => {
    const overview = overviewFrom(overviewBody)

    expect(overview.totalCount).toBe(2)
  })

  it('reads each summary with all four ledger sums and its net', () => {
    const overview = overviewFrom(overviewBody)

    expect(overview.items[0]).toStrictEqual({
      avatar: 'Calix',
      bankWithdrawn: 1200,
      bankDeposited: 3400,
      storageWithdrawn: 200,
      storageDeposited: 500,
      net: 2500,
    })
  })

  it.each([
    'bankWithdrawn',
    'bankDeposited',
    'storageWithdrawn',
    'storageDeposited',
    'net',
  ])('refuses a summary without its %s', (field) => {
    const reading = () =>
      overviewFrom({ ...overviewBody, items: [summaryWithout(field)] })

    expect(reading).toThrow(MalformedResponse)
  })

  it('refuses a summary whose sums are strings', () => {
    const reading = () =>
      overviewFrom({
        ...overviewBody,
        items: [
          {
            avatar: 'Calix',
            bankWithdrawn: '1200',
            bankDeposited: '3400',
            storageWithdrawn: 200,
            storageDeposited: 500,
            net: 2500,
          },
        ],
      })

    expect(reading).toThrow(MalformedResponse)
  })
})

describe('the bank entry wire shape', () => {
  it('reads the window the server answered with, not the one asked for', () => {
    const page = bankPageFrom(bankBody)

    expect({
      page: page.page,
      size: page.size,
      totalCount: page.totalCount,
    }).toStrictEqual({
      page: 1,
      size: 2,
      totalCount: 340,
    })
  })

  it('reads an entry into the domain shape', () => {
    const page = bankPageFrom(bankBody)

    expect(page.items[0]).toStrictEqual({
      timestamp: new Date('2026-08-05T10:15:00Z'),
      avatar: 'Calix',
      amount: 500,
      transferType: 'DEPOSIT',
    })
  })

  it('reads an empty page of a known avatar', () => {
    const page = bankPageFrom({ page: 0, size: 100, totalCount: 0, items: [] })

    expect(page.items).toStrictEqual([])
  })

  it('refuses an unreadable timestamp', () => {
    const reading = () =>
      bankPageFrom({
        ...bankBody,
        items: [{ ...bankBody.items[0], timestamp: 'yesterday' }],
      })

    expect(reading).toThrow(MalformedResponse)
  })

  it('refuses a transfer type the contract does not name', () => {
    const reading = () =>
      bankPageFrom({
        ...bankBody,
        items: [{ ...bankBody.items[0], transferType: 'EINLAGERUNG' }],
      })

    expect(reading).toThrow(MalformedResponse)
  })

  it('refuses a total count that is no finite number', () => {
    const reading = () =>
      bankPageFrom({
        page: 0,
        size: 100,
        totalCount: Number.POSITIVE_INFINITY,
        items: [],
      })

    expect(reading).toThrow(MalformedResponse)
  })

  it('refuses a body that is not an envelope', () => {
    const reading = () => bankPageFrom([])

    expect(reading).toThrow(MalformedResponse)
  })

  it('refuses an envelope whose items are missing', () => {
    const reading = () => bankPageFrom({ page: 0, size: 100, totalCount: 0 })

    expect(reading).toThrow(MalformedResponse)
  })
})

describe('the storage entry wire shape', () => {
  it('reads an entry with its item name, quantity and quality', () => {
    const page = storagePageFrom(storageBody)

    expect(page.items[0]).toStrictEqual({
      timestamp: new Date('2026-08-05T10:15:00Z'),
      avatar: 'Calix',
      quantity: 3,
      name: 'Erde-Eibenlanze',
      quality: 7,
      transferType: 'WITHDRAWAL',
    })
  })

  it('refuses an entry without an item name', () => {
    const reading = () =>
      storagePageFrom({
        ...storageBody,
        items: [{ ...storageBody.items[0], name: null }],
      })

    expect(reading).toThrow(MalformedResponse)
  })
})
