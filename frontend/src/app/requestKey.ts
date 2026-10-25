import type { LedgerSort } from '../api'

export function requestKeyOf(
  view: string,
  avatar: string | null,
  page = 0,
  sort: LedgerSort | null = null,
): string {
  return JSON.stringify([
    view,
    avatar,
    page,
    sort?.column ?? null,
    sort?.direction ?? null,
  ])
}
