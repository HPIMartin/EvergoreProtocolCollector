import { describe, expect, it } from 'vitest'

import type { BankEntry } from './bankEntry.ts'
import { entriesOf, unknownAvatar } from './ledger.ts'
import type { LedgerVisitor } from './ledger.ts'
import type { Page } from './page.ts'

const describingVisitor: LedgerVisitor<BankEntry, string> = {
  entries: (page) =>
    `${String(page.items.length)} of ${String(page.totalCount)} entries`,
  unknownAvatar: (avatar) => `no avatar named ${avatar}`,
}

const oneEntry: BankEntry = {
  timestamp: new Date('2026-01-15T12:34:00Z'),
  avatar: 'Calix',
  amount: 500,
  transferType: 'DEPOSIT',
}

function pageOf(
  items: readonly BankEntry[],
  totalCount: number,
): Page<BankEntry> {
  return { page: 0, size: 100, totalCount, items }
}

describe('ledger', () => {
  it('hands the visitor the entries of a known avatar', () => {
    const described = entriesOf(pageOf([oneEntry], 1)).accept(describingVisitor)

    expect(described).toBe('1 of 1 entries')
  })

  it('hands the visitor an empty page when a known avatar has no entries', () => {
    const described = entriesOf(pageOf([], 0)).accept(describingVisitor)

    expect(described).toBe('0 of 0 entries')
  })

  it('hands the visitor the requested name when the avatar is unknown', () => {
    const described =
      unknownAvatar<BankEntry>('Nobody').accept(describingVisitor)

    expect(described).toBe('no avatar named Nobody')
  })

  it('keeps a page beyond the last entry a page of a known avatar', () => {
    const described = entriesOf(pageOf([], 340)).accept(describingVisitor)

    expect(described).toBe('0 of 340 entries')
  })
})
