import type { LedgerSort } from '../api'

import { DIRECTION, PAGE, SORT } from './route.ts'

const FIRST_PAGE = 0

export function hrefOf(
  path: string,
  page?: number,
  sort?: LedgerSort | null,
): string {
  const parameters = new URLSearchParams()
  if (page !== undefined && page !== FIRST_PAGE) {
    parameters.set(PAGE, String(page))
  }
  if (sort !== undefined && sort !== null) {
    parameters.set(SORT, sort.column)
    parameters.set(DIRECTION, sort.direction)
  }

  const query = parameters.toString()
  return query === '' ? path : `${path}?${query}`
}
