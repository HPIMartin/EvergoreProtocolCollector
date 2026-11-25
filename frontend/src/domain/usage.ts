export interface UsageCounts {
  readonly logins: number
  readonly pageLoads: number
  readonly logouts: number
  readonly daysUsed: number
  readonly latestUse: Date | null
}

export interface AccountUsage {
  readonly name: string
  readonly counts: UsageCounts
}

export interface UsageWindow {
  readonly accounts: readonly AccountUsage[]
  readonly guild: UsageCounts
  readonly adminKey: UsageCounts
}

export interface Usage {
  readonly lastThirtyDays: UsageWindow
  readonly allTheTime: UsageWindow
  readonly recordedSince: string | null
  readonly lostSinceStart: number
}
