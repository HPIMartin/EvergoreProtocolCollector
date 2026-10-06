import { describe, expect, it } from 'vitest'

import type { LedgerSort } from '../api'

import type { RouteVisitor } from './route.ts'
import {
  adminPath,
  bankPath,
  overviewPath,
  routeOf,
  storagePath,
} from './route.ts'

function sortedBy(sort: LedgerSort | null): string {
  return sort === null ? '' : ` sorted by ${sort.column} ${sort.direction}`
}

const describingVisitor: RouteVisitor<string> = {
  overview: () => 'the overview',
  admin: () => 'the admin status page',
  bank: (avatar, page, sort) =>
    `the bank of ${avatar} at page ${String(page)}${sortedBy(sort)}`,
  storage: (avatar, page, sort) =>
    `the storage of ${avatar} at page ${String(page)}${sortedBy(sort)}`,
  unknownPath: (path) => `no view for ${path}`,
}

describe('route', () => {
  it('shows the overview at the path the shell is loaded from', () => {
    const described = routeOf('/', '').accept(describingVisitor)

    expect(described).toBe('the overview')
  })

  it('shows the overview at the dashboard path the HTML pages used', () => {
    const described = routeOf('/overview', '').accept(describingVisitor)

    expect(described).toBe('the overview')
  })

  it('ignores a trailing slash', () => {
    const described = routeOf('/overview/', '').accept(describingVisitor)

    expect(described).toBe('the overview')
  })

  it('shows the admin status page at its own path', () => {
    const described = routeOf('/admin', '').accept(describingVisitor)

    expect(described).toBe('the admin status page')
  })

  it('shows one avatar bank ledger at page 0 when the address names none', () => {
    const described = routeOf('/avatars/Calix/bank', '').accept(
      describingVisitor,
    )

    expect(described).toBe('the bank of Calix at page 0')
  })

  it('shows one avatar storage ledger at page 0 when the address names none', () => {
    const described = routeOf('/avatars/Calix/storage', '').accept(
      describingVisitor,
    )

    expect(described).toBe('the storage of Calix at page 0')
  })

  it('reads the page a ledger address names', () => {
    const described = routeOf('/avatars/Calix/bank', '?page=3').accept(
      describingVisitor,
    )

    expect(described).toBe('the bank of Calix at page 3')
  })

  it('passes an unusable page value through unchanged instead of clamping it', () => {
    const described = routeOf('/avatars/Calix/bank', '?page=-1').accept(
      describingVisitor,
    )

    expect(described).toBe('the bank of Calix at page -1')
  })

  it('passes a non-numeric page value through as NaN instead of clamping it', () => {
    const described = routeOf('/avatars/Calix/bank', '?page=nonsense').accept(
      describingVisitor,
    )

    expect(described).toBe('the bank of Calix at page NaN')
  })

  it('passes a present but blank page value through as NaN instead of defaulting it', () => {
    const described = routeOf('/avatars/Calix/bank', '?page=').accept(
      describingVisitor,
    )

    expect(described).toBe('the bank of Calix at page NaN')
  })

  it('reads the sort a ledger address names', () => {
    const described = routeOf(
      '/avatars/Calix/storage',
      '?page=2&sort=quantity&direction=descending',
    ).accept(describingVisitor)

    expect(described).toBe(
      'the storage of Calix at page 2 sorted by quantity descending',
    )
  })

  it('completes a sort that names only its column with the descending direction', () => {
    const described = routeOf('/avatars/Calix/bank', '?sort=amount').accept(
      describingVisitor,
    )

    expect(described).toBe(
      'the bank of Calix at page 0 sorted by amount descending',
    )
  })

  it('completes a sort that names only its direction with the time', () => {
    const described = routeOf(
      '/avatars/Calix/bank',
      '?direction=ascending',
    ).accept(describingVisitor)

    expect(described).toBe(
      'the bank of Calix at page 0 sorted by timestamp ascending',
    )
  })

  it('passes a sort the ledger does not know through unchanged instead of dropping it', () => {
    const described = routeOf(
      '/avatars/Calix/storage',
      '?sort=Menge&direction=up',
    ).accept(describingVisitor)

    expect(described).toBe('the storage of Calix at page 0 sorted by Menge up')
  })

  it('decodes an avatar name that had to be escaped in the path', () => {
    const described = routeOf('/avatars/Erde%2FEibe/bank', '').accept(
      describingVisitor,
    )

    expect(described).toBe('the bank of Erde/Eibe at page 0')
  })

  it.each([
    '/avatars',
    '/avatars/Calix',
    '/avatars//bank',
    '/avatars/Calix/bank/2',
    '/avatars/Calix/gold',
    '/overview/Calix',
    '/nonsense',
    '/avatars/%E0%A4%A/bank',
  ])('has no view for %s', (path) => {
    const described = routeOf(path, '').accept(describingVisitor)

    expect(described).toBe(`no view for ${path}`)
  })

  it('names the path the overview is reached at', () => {
    const path = overviewPath()

    expect(path).toBe('/overview')
  })

  it('names the path the admin status page is reached at', () => {
    const path = adminPath()

    expect(path).toBe('/admin')
  })

  it('names the path a bank ledger is reached at', () => {
    const path = bankPath('Calix')

    expect(path).toBe('/avatars/Calix/bank')
  })

  it('names the path a storage ledger is reached at', () => {
    const path = storagePath('Calix')

    expect(path).toBe('/avatars/Calix/storage')
  })

  it('escapes an avatar name that would otherwise change the path', () => {
    const path = bankPath('Erde/Eibe')

    expect(path).toBe('/avatars/Erde%2FEibe/bank')
  })

  it('reads back the avatar of a path it built', () => {
    const described = routeOf(storagePath('Erde/Eibe'), '').accept(
      describingVisitor,
    )

    expect(described).toBe('the storage of Erde/Eibe at page 0')
  })
})
