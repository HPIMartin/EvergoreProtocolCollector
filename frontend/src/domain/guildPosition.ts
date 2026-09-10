import type { AvatarSummary, GuildTotals } from './avatarSummary.ts'

export interface GuildPosition {
  readonly bank: number
  readonly storageValue: number | null
  readonly donation: number | null
  readonly craftSubsidy: number | null
}

export const guildPositionOf = (totals: GuildTotals): GuildPosition => ({
  bank: totals.bankDeposited - totals.bankWithdrawn,
  storageValue: storageValueOf(totals),
  donation: totals.donation,
  craftSubsidy: totals.craftSubsidy,
})

export const balanceOf = (summary: AvatarSummary): number | null =>
  summary.donation === null || summary.craftSubsidy === null
    ? null
    : summary.net + summary.donation - summary.craftSubsidy

export const UNCOMPUTED_NOTE = 'Noch nicht berechnet.'

export const UNCOMPUTED_BALANCE_NOTE =
  'Kein Saldo: für diese Zeile sind Gildenspende und Handwerkssubventionen noch nicht berechnet.'

function storageValueOf(totals: GuildTotals): number | null {
  if (totals.donation === null || totals.craftSubsidy === null) {
    return null
  }
  return (
    totals.storageDeposited +
    totals.donation -
    totals.craftSubsidy -
    totals.storageWithdrawn
  )
}
