const TOKEN = 'token'

export function tokenIn(search: string): string | null {
  return new URLSearchParams(search).get(TOKEN)
}

export function hrefOf(path: string, token: string | null): string {
  if (token === null) {
    return path
  }

  const parameters = new URLSearchParams()
  parameters.set(TOKEN, token)

  return `${path}?${parameters.toString()}`
}
