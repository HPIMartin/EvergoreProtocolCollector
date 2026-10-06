import type { Page } from './page.ts'

export interface AvatarSummary {
  readonly avatar: string
  readonly bankWithdrawn: number | null
  readonly bankDeposited: number | null
  readonly storageWithdrawn: number | null
  readonly storageDeposited: number | null
  readonly net: number | null
  readonly donation: number | null
  readonly craftSubsidy: number | null
  readonly balance: number | null
  readonly lastBankActivity: Date | null
  readonly lastStorageActivity: Date | null
  readonly staleSumsFrom: Date | null
}

export interface GuildTotals {
  readonly bankWithdrawn: number | null
  readonly bankDeposited: number | null
  readonly storageWithdrawn: number | null
  readonly storageDeposited: number | null
  readonly net: number | null
  readonly donation: number | null
  readonly craftSubsidy: number | null
  readonly balance: number | null
  readonly storageValue: number | null
  readonly containsStaleSums: boolean
}

export interface Overview extends Page<AvatarSummary> {
  readonly totals: GuildTotals
}
