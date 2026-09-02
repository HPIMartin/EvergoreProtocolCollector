import { PAGE } from './route.ts'

const TOKEN = 'token'
const FIRST_PAGE = 0

export function tokenIn(search: string): string | null {
  return new URLSearchParams(search).get(TOKEN)
}

export function hrefOf(
  path: string,
  token: string | null,
  page?: number,
): string {
  const parameters = new URLSearchParams()
  if (token !== null) {
    parameters.set(TOKEN, token)
  }
  if (page !== undefined && page !== FIRST_PAGE) {
    parameters.set(PAGE, String(page))
  }

  const query = parameters.toString()
  return query === '' ? path : `${path}?${query}`
}
