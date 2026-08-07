import type { Page } from './page.ts'

export interface LedgerVisitor<E, R> {
  entries: (page: Page<E>) => R
  unknownAvatar: (avatar: string) => R
}

export interface Ledger<E> {
  accept: <R>(visitor: LedgerVisitor<E, R>) => R
}

export function entriesOf<E>(page: Page<E>): Ledger<E> {
  return { accept: (visitor) => visitor.entries(page) }
}

export function unknownAvatar<E>(avatar: string): Ledger<E> {
  return { accept: (visitor) => visitor.unknownAvatar(avatar) }
}
