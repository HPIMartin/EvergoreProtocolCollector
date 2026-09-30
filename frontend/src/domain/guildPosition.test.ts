import { describe, expect, it } from 'vitest'

import type { GuildTotals } from './avatarSummary.ts'
import { guildPositionOf } from './guildPosition.ts'

const TOTALS: GuildTotals = {
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
}

describe('the guild position behind the overview', () => {
  it('measures the bank in the gold it actually moved', () => {
    const position = guildPositionOf(TOTALS)

    expect(position.bank).toBe(2050)
  })

  it('states the storage value the server rounded once rather than adding up its rounded parts', () => {
    const position = guildPositionOf({ ...TOTALS, storageValue: 599 })

    expect(position.storageValue).toBe(599)
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

  it('cannot answer the storage value while the server has none, as before its first recompute', () => {
    const position = guildPositionOf({ ...TOTALS, storageValue: null })

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
})
