const SEPARATOR = '/'
const AVATARS = 'avatars'
const OVERVIEW = 'overview'
const BANK = 'bank'
const STORAGE = 'storage'
const LEDGER_SEGMENT_COUNT = 3

export interface RouteVisitor<R> {
  overview: () => R
  bank: (avatar: string) => R
  storage: (avatar: string) => R
  unknownPath: (path: string) => R
}

export interface Route {
  accept: <R>(visitor: RouteVisitor<R>) => R
}

export function overviewPath(): string {
  return `${SEPARATOR}${OVERVIEW}`
}

export function bankPath(avatar: string): string {
  return ledgerPath(avatar, BANK)
}

export function storagePath(avatar: string): string {
  return ledgerPath(avatar, STORAGE)
}

export function routeOf(path: string): Route {
  const segments = path.split(SEPARATOR).filter((segment) => segment !== '')
  if (segments.length === 0 || onlyTheOverview(segments)) {
    return OVERVIEW_ROUTE
  }

  return ledgerRouteOf(segments) ?? unknownPathRoute(path)
}

const OVERVIEW_ROUTE: Route = { accept: (visitor) => visitor.overview() }

function bankRoute(avatar: string): Route {
  return { accept: (visitor) => visitor.bank(avatar) }
}

function storageRoute(avatar: string): Route {
  return { accept: (visitor) => visitor.storage(avatar) }
}

function unknownPathRoute(path: string): Route {
  return { accept: (visitor) => visitor.unknownPath(path) }
}

function ledgerPath(avatar: string, view: string): string {
  return `${SEPARATOR}${AVATARS}${SEPARATOR}${encodeURIComponent(avatar)}${SEPARATOR}${view}`
}

function onlyTheOverview(segments: readonly string[]): boolean {
  return segments.length === 1 && segments[0] === OVERVIEW
}

function ledgerRouteOf(segments: readonly string[]): Route | null {
  if (segments.length !== LEDGER_SEGMENT_COUNT || segments[0] !== AVATARS) {
    return null
  }

  const avatar = decodedAvatar(segments[1])
  if (avatar === null) {
    return null
  }
  if (segments[2] === BANK) {
    return bankRoute(avatar)
  }
  if (segments[2] === STORAGE) {
    return storageRoute(avatar)
  }

  return null
}

function decodedAvatar(segment: string | undefined): string | null {
  if (segment === undefined || segment === '') {
    return null
  }

  try {
    return decodeURIComponent(segment)
  } catch {
    return null
  }
}
