const LOGIN_PAGE = '/login'

export function loginAddressFor(address: string): string {
  return `${LOGIN_PAGE}?next=${encodeURIComponent(address)}`
}
