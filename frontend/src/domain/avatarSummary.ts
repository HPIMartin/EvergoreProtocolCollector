import type { Page } from './page.ts'

export interface AvatarSummary {
  readonly avatar: string
  readonly withdrawn: number
  readonly deposited: number
}

export interface Overview extends Page<AvatarSummary> {
  readonly lastUpdated: Date | null
}
