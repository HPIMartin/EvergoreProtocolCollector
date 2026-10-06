import type { GuildTotals } from './avatarSummary.ts'

export interface GuildPosition {
  readonly bank: number | null
  readonly storageValue: number | null
  readonly donation: number | null
  readonly craftSubsidy: number | null
}

export const guildPositionOf = (totals: GuildTotals): GuildPosition => ({
  bank: bankOf(totals),
  storageValue: totals.storageValue,
  donation: totals.donation,
  craftSubsidy: totals.craftSubsidy,
})

const bankOf = (totals: GuildTotals): number | null =>
  totals.bankDeposited === null || totals.bankWithdrawn === null
    ? null
    : totals.bankDeposited - totals.bankWithdrawn

export const UNCOMPUTED_NOTE = 'Noch nicht berechnet.'

export const UNCOMPUTED_BALANCE_NOTE =
  'Kein Saldo: für diese Zeile sind Gildenspende und Handwerkssubventionen noch nicht berechnet.'
