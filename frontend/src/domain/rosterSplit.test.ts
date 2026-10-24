import { describe, expect, it } from 'vitest'

import type { AvatarSummary } from './avatarSummary.ts'
import { rosterSplitOf } from './rosterSplit.ts'

function summaryOf(
  avatar: string,
  active: boolean,
  lastActivity: Date | null = null,
): AvatarSummary {
  return {
    avatar,
    bankWithdrawn: 0,
    bankDeposited: 0,
    storageWithdrawn: 0,
    storageDeposited: 0,
    net: 0,
    donation: 0,
    craftSubsidy: 0,
    balance: 0,
    lastBankActivity: lastActivity,
    lastStorageActivity: lastActivity,
    staleSumsFrom: null,
    active,
  }
}

const namesOf = (summaries: readonly AvatarSummary[]): string[] =>
  summaries.map((summary) => summary.avatar)

describe('the roster split behind the overview', () => {
  it('puts the rows the service marked active above and the rest below', () => {
    const split = rosterSplitOf([
      summaryOf('Newest', true),
      summaryOf('Old', false),
      summaryOf('Recent', true),
    ])

    const activeNames = namesOf(split.active)
    const dormantNames = namesOf(split.dormant)

    expect([activeNames, dormantNames]).toStrictEqual([
      ['Newest', 'Recent'],
      ['Old'],
    ])
  })

  it('reads no date, so a row marked active stays active however old its activity', () => {
    const marked = summaryOf('Alt', true, new Date('2020-01-01T00:00:00Z'))

    const split = rosterSplitOf([
      summaryOf('Neu', true, new Date('2026-07-31T21:05:00Z')),
      marked,
    ])

    const dormantNames = namesOf(split.dormant)

    expect(dormantNames).toStrictEqual([])
  })

  it('reads no date, so a row marked dormant stays dormant however recent its activity', () => {
    const marked = summaryOf('Neu', false, new Date('2026-07-31T21:05:00Z'))

    const split = rosterSplitOf([marked, summaryOf('Alt', true)])

    const dormantNames = namesOf(split.dormant)

    expect(dormantNames).toStrictEqual(['Neu'])
  })

  it('leaves every row active when the service marked every row active', () => {
    const split = rosterSplitOf([
      summaryOf('Erster', true),
      summaryOf('Zweiter', true),
    ])

    const activeNames = namesOf(split.active)
    const dormantNames = namesOf(split.dormant)

    expect([activeNames, dormantNames]).toStrictEqual([
      ['Erster', 'Zweiter'],
      [],
    ])
  })

  it('splits an empty roster into two empty halves', () => {
    const split = rosterSplitOf([])

    expect([split.active, split.dormant]).toStrictEqual([[], []])
  })

  it('keeps the order it was handed inside each half', () => {
    const split = rosterSplitOf([
      summaryOf('Aaron', false),
      summaryOf('Bea', true),
      summaryOf('Cara', false),
      summaryOf('Dane', true),
    ])

    const activeNames = namesOf(split.active)
    const dormantNames = namesOf(split.dormant)

    expect([activeNames, dormantNames]).toStrictEqual([
      ['Bea', 'Dane'],
      ['Aaron', 'Cara'],
    ])
  })
})
