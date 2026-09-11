import { describe, expect, it } from 'vitest'

import type { AvatarSummary } from './avatarSummary.ts'
import { ACTIVITY_WINDOW_DAYS, rosterSplitOf } from './rosterSplit.ts'

const DAY = 24 * 60 * 60 * 1000

const NEWEST = new Date('2026-07-31T21:05:00Z')

const cutBy = (millis: number): Date =>
  new Date(NEWEST.getTime() - ACTIVITY_WINDOW_DAYS * DAY + millis)

function summaryOf(
  avatar: string,
  lastBankActivity: Date | null,
  lastStorageActivity: Date | null,
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
    lastBankActivity,
    lastStorageActivity,
    staleSumsFrom: null,
  }
}

const namesOf = (summaries: readonly AvatarSummary[]): string[] =>
  summaries.map((summary) => summary.avatar)

describe('the roster split behind the overview', () => {
  it('measures every row against the newest activity in the data, not against the clock', () => {
    const newest = summaryOf('Newest', null, NEWEST)
    const recent = summaryOf('Recent', new Date('2026-07-20T08:00:00Z'), null)
    const old = summaryOf('Old', new Date('2026-05-02T08:00:00Z'), null)

    const split = rosterSplitOf([newest, recent, old])

    expect([namesOf(split.active), namesOf(split.dormant)]).toStrictEqual([
      ['Newest', 'Recent'],
      ['Old'],
    ])
  })

  it('counts a row that moved exactly on the boundary as active', () => {
    const onTheBoundary = summaryOf('Grenzfall', cutBy(0), null)

    const split = rosterSplitOf([
      summaryOf('Newest', null, NEWEST),
      onTheBoundary,
    ])

    expect(namesOf(split.active)).toStrictEqual(['Newest', 'Grenzfall'])
  })

  it('counts a row one millisecond before the boundary as dormant', () => {
    const justOutside = summaryOf('Grenzfall', cutBy(-1), null)

    const split = rosterSplitOf([
      summaryOf('Newest', null, NEWEST),
      justOutside,
    ])

    expect([namesOf(split.active), namesOf(split.dormant)]).toStrictEqual([
      ['Newest'],
      ['Grenzfall'],
    ])
  })

  it('reads a row by the later of its two ledgers', () => {
    const staleBankFreshStorage = summaryOf(
      'Lagerer',
      new Date('2026-05-02T08:00:00Z'),
      cutBy(0),
    )
    const freshBankStaleStorage = summaryOf(
      'Bankier',
      cutBy(0),
      new Date('2026-05-02T08:00:00Z'),
    )

    const split = rosterSplitOf([
      summaryOf('Newest', null, NEWEST),
      staleBankFreshStorage,
      freshBankStaleStorage,
    ])

    expect(split.dormant).toStrictEqual([])
  })

  it('never counts the only row of a roster as dormant against itself', () => {
    const alone = summaryOf('Allein', new Date('2020-01-01T00:00:00Z'), null)

    const split = rosterSplitOf([alone])

    expect([namesOf(split.active), namesOf(split.dormant)]).toStrictEqual([
      ['Allein'],
      [],
    ])
  })

  it('counts a row that never appeared in either ledger as dormant', () => {
    const neverSeen = summaryOf('Namenlos', null, null)

    const split = rosterSplitOf([summaryOf('Newest', null, NEWEST), neverSeen])

    expect([namesOf(split.active), namesOf(split.dormant)]).toStrictEqual([
      ['Newest'],
      ['Namenlos'],
    ])
  })

  it('leaves every row active while the data holds no activity to measure against', () => {
    const split = rosterSplitOf([
      summaryOf('Erster', null, null),
      summaryOf('Zweiter', null, null),
    ])

    expect([namesOf(split.active), namesOf(split.dormant)]).toStrictEqual([
      ['Erster', 'Zweiter'],
      [],
    ])
  })

  it('splits an empty roster into two empty halves', () => {
    const split = rosterSplitOf([])

    expect([split.active, split.dormant]).toStrictEqual([[], []])
  })

  it('keeps the order it was handed inside each half', () => {
    const dormantOne = summaryOf(
      'Aaron',
      new Date('2026-01-01T00:00:00Z'),
      null,
    )
    const activeOne = summaryOf('Bea', new Date('2026-07-20T08:00:00Z'), null)
    const dormantTwo = summaryOf('Cara', new Date('2026-02-01T00:00:00Z'), null)
    const activeTwo = summaryOf('Dane', null, NEWEST)

    const split = rosterSplitOf([dormantOne, activeOne, dormantTwo, activeTwo])

    expect([namesOf(split.active), namesOf(split.dormant)]).toStrictEqual([
      ['Bea', 'Dane'],
      ['Aaron', 'Cara'],
    ])
  })
})
