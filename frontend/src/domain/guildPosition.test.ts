import { describe, expect, it } from 'vitest'

import type { AvatarSummary, GuildTotals } from './avatarSummary.ts'
import { balanceOf, guildPositionOf } from './guildPosition.ts'

const TOTALS: GuildTotals = {
  bankWithdrawn: 1400,
  bankDeposited: 3450,
  storageWithdrawn: 200,
  storageDeposited: 500,
  net: 2350,
  donation: 400,
  craftSubsidy: 100,
  containsStaleSums: false,
}

const SUMMARY: AvatarSummary = {
  avatar: 'Calix',
  bankWithdrawn: 1200,
  bankDeposited: 3400,
  storageWithdrawn: 200,
  storageDeposited: 500,
  net: 2500,
  donation: 400,
  craftSubsidy: 100,
  lastBankActivity: null,
  lastStorageActivity: null,
  staleSumsFrom: null,
}

describe('the guild position behind the overview', () => {
  it('measures the bank in the gold it actually moved', () => {
    const position = guildPositionOf(TOTALS)

    expect(position.bank).toBe(2050)
  })

  it('measures the storage in what it holds rather than in what it credited', () => {
    const position = guildPositionOf(TOTALS)

    expect(position.storageValue).toBe(600)
  })

  it('states the two flows apart, the donation and what the guild pays traders on top', () => {
    const position = guildPositionOf(TOTALS)

    expect([position.donation, position.craftSubsidy]).toStrictEqual([400, 100])
  })

  it('leaves the bank untouched by every modelled figure around it', () => {
    const position = guildPositionOf({
      ...TOTALS,
      storageDeposited: 9999,
      storageWithdrawn: 9999,
      donation: 9999,
      craftSubsidy: 9999,
    })

    expect(position.bank).toBe(2050)
  })

  it('cannot answer the storage value while no recompute has produced the two flows', () => {
    const position = guildPositionOf({ ...TOTALS, donation: null })

    expect(position.storageValue).toBeNull()
  })

  it('still answers the bank while the two flows are missing, because the bank is measured', () => {
    const position = guildPositionOf({
      ...TOTALS,
      donation: null,
      craftSubsidy: null,
    })

    expect(position.bank).toBe(2050)
  })

  it('answers a row balance as its contribution plus what the guild kept of its deposits', () => {
    const balance = balanceOf(SUMMARY)

    expect(balance).toBe(2800)
  })

  it('cannot answer a row balance while that row is missing either flow', () => {
    const balance = balanceOf({ ...SUMMARY, craftSubsidy: null })

    expect(balance).toBeNull()
  })
})
