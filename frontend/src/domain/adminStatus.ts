export interface AdminStatus {
  readonly lastUpdated: Date | null
  readonly lastSuccessfulScrape: Date | null
  readonly lastScrapeFailure: Date | null
  readonly lastSuccessfulRecompute: Date | null
  readonly lastRecomputeFailure: Date | null
  readonly unknownItemNames: readonly string[]
  readonly failedAvatarNames: readonly string[]
  readonly roundTrips: readonly RoundTrip[]
  readonly roundTripAbstentions: readonly RoundTripAbstention[]
}

export interface RoundTrip {
  readonly avatar: string
  readonly item: string
  readonly quantity: number
}

export interface RoundTripAbstention {
  readonly avatar: string
  readonly item: string
}
