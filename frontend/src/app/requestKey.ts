import type { LedgerSort } from '../api'

export function requestKeyOf(
  view: string,
  avatar: string | null,
  token: string | null,
  page = 0,
  sort: LedgerSort | null = null,
): string {
  return JSON.stringify([
    view,
    avatar,
    token,
    page,
    sort?.column ?? null,
    sort?.direction ?? null,
  ])
}
