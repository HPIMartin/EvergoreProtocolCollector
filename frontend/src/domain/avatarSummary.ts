import type { Page } from './page.ts'

export interface AvatarSummary {
  readonly avatar: string
  readonly bankWithdrawn: number
  readonly bankDeposited: number
  readonly storageWithdrawn: number
  readonly storageDeposited: number
  readonly net: number
  readonly lastBankActivity: Date | null
  readonly lastStorageActivity: Date | null
}

export interface GuildTotals {
  readonly bankWithdrawn: number
  readonly bankDeposited: number
  readonly storageWithdrawn: number
  readonly storageDeposited: number
  readonly net: number
}

export interface Overview extends Page<AvatarSummary> {
  readonly lastUpdated: Date | null
  readonly totals: GuildTotals
}
