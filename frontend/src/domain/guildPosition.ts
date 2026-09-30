import type { GuildTotals } from './avatarSummary.ts'

export interface GuildPosition {
  readonly bank: number
  readonly storageValue: number | null
  readonly donation: number | null
  readonly craftSubsidy: number | null
}

export const guildPositionOf = (totals: GuildTotals): GuildPosition => ({
  bank: totals.bankDeposited - totals.bankWithdrawn,
  storageValue: totals.storageValue,
  donation: totals.donation,
  craftSubsidy: totals.craftSubsidy,
})

export const UNCOMPUTED_NOTE = 'Noch nicht berechnet.'

export const UNCOMPUTED_BALANCE_NOTE =
  'Kein Saldo: für diese Zeile sind Gildenspende und Handwerkssubventionen noch nicht berechnet.'
