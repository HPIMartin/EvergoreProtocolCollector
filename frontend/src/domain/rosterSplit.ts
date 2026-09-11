import type { AvatarSummary } from './avatarSummary.ts'

export interface RosterSplit {
  readonly active: readonly AvatarSummary[]
  readonly dormant: readonly AvatarSummary[]
}

export const ACTIVITY_WINDOW_DAYS = 30

const MILLIS_PER_DAY = 24 * 60 * 60 * 1000

export const rosterSplitOf = (
  summaries: readonly AvatarSummary[],
): RosterSplit => {
  const cut = cutOf(summaries)
  if (cut === null) {
    return { active: summaries, dormant: [] }
  }

  return {
    active: summaries.filter((summary) => isActiveSince(summary, cut)),
    dormant: summaries.filter((summary) => !isActiveSince(summary, cut)),
  }
}

const lastActivityOf = (summary: AvatarSummary): Date | null =>
  latestOf(summary.lastBankActivity, summary.lastStorageActivity)

function cutOf(summaries: readonly AvatarSummary[]): Date | null {
  const reference = summaries.reduce<Date | null>(
    (newest, summary) => latestOf(newest, lastActivityOf(summary)),
    null,
  )
  if (reference === null) {
    return null
  }
  return new Date(reference.getTime() - ACTIVITY_WINDOW_DAYS * MILLIS_PER_DAY)
}

function isActiveSince(summary: AvatarSummary, cut: Date): boolean {
  const activity = lastActivityOf(summary)
  return activity !== null && activity.getTime() >= cut.getTime()
}

function latestOf(left: Date | null, right: Date | null): Date | null {
  if (left === null) {
    return right
  }
  if (right === null) {
    return left
  }
  return left.getTime() >= right.getTime() ? left : right
}
