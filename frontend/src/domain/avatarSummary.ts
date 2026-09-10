import type { Page } from './page.ts'

export interface AvatarSummary {
  readonly avatar: string
  readonly bankWithdrawn: number
  readonly bankDeposited: number
  readonly storageWithdrawn: number
  readonly storageDeposited: number
  readonly net: number
  readonly donation: number | null
  readonly craftSubsidy: number | null
  readonly lastBankActivity: Date | null
  readonly lastStorageActivity: Date | null
  readonly staleSumsFrom: Date | null
}

export interface GuildTotals {
  readonly bankWithdrawn: number
  readonly bankDeposited: number
  readonly storageWithdrawn: number
  readonly storageDeposited: number
  readonly net: number
  readonly donation: number | null
  readonly craftSubsidy: number | null
  readonly containsStaleSums: boolean
}

export interface Overview extends Page<AvatarSummary> {
  readonly totals: GuildTotals
}
