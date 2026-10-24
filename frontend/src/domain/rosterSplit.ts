import type { AvatarSummary } from './avatarSummary.ts'

export interface RosterSplit {
  readonly active: readonly AvatarSummary[]
  readonly dormant: readonly AvatarSummary[]
}

export const rosterSplitOf = (
  summaries: readonly AvatarSummary[],
): RosterSplit => ({
  active: summaries.filter((summary) => summary.active),
  dormant: summaries.filter((summary) => !summary.active),
})
