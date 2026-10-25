const OVERVIEW = '/'
const THIS_SERVICE = 'http://this-service.invalid'
const FIRST_PRINTABLE = 0x20
const DELETE = 0x7f

export function returnTarget(next: string | null): string {
  if (next === null) {
    return OVERVIEW
  }
  const decoded = decodedOnce(next)
  if (
    decoded === null ||
    !isAPathOfThisService(next) ||
    !isAPathOfThisService(decoded)
  ) {
    return OVERVIEW
  }

  return next
}

function isAPathOfThisService(candidate: string): boolean {
  const slashed = candidate.replaceAll('\\', '/')

  return (
    slashed.startsWith('/') &&
    !slashed.startsWith('//') &&
    !hasAControlCharacter(slashed) &&
    new URL(slashed, THIS_SERVICE).origin === THIS_SERVICE
  )
}

function hasAControlCharacter(candidate: string): boolean {
  return [...candidate].some((character) => {
    const code = character.charCodeAt(0)

    return code < FIRST_PRINTABLE || code === DELETE
  })
}

function decodedOnce(next: string): string | null {
  try {
    return decodeURIComponent(next)
  } catch {
    return null
  }
}
