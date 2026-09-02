import type { Page } from './page.ts'

export interface AvatarSummary {
  readonly avatar: string
  readonly bankWithdrawn: number
  readonly bankDeposited: number
  readonly storageWithdrawn: number
  readonly storageDeposited: number
  readonly net: number
}

export interface Overview extends Page<AvatarSummary> {
  readonly lastUpdated: Date | null
}
