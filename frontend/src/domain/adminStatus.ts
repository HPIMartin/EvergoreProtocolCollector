export interface AdminStatus {
  readonly lastUpdated: Date | null
  readonly lastSuccessfulScrape: Date | null
  readonly lastScrapeFailure: Date | null
  readonly lastSuccessfulRecompute: Date | null
  readonly lastRecomputeFailure: Date | null
  readonly unknownItemNames: readonly string[]
  readonly failedAvatarNames: readonly string[]
}
