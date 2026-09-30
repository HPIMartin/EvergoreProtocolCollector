import { describe, expect, it } from 'vitest'

import type { StorageEntry } from './storageEntry.ts'
import { germanTransferOfStorageEntry } from './storageEntry.ts'
import type { TransferType } from './transferType.ts'
import { DEPOSIT, WITHDRAWAL } from './transferType.ts'

function storageEntryOf(transferType: TransferType): StorageEntry {
  return {
    timestamp: new Date('2026-08-05T10:15:00Z'),
    avatar: 'Calix',
    quantity: 1,
    name: 'Kupfererz',
    quality: 100,
    transferType,
  }
}

describe('storageEntry', () => {
  it('names an item deposit "Einlagerung"', () => {
    const german = germanTransferOfStorageEntry(storageEntryOf(DEPOSIT))

    expect(german).toBe('Einlagerung')
  })

  it('names an item withdrawal "Entnahme"', () => {
    const german = germanTransferOfStorageEntry(storageEntryOf(WITHDRAWAL))

    expect(german).toBe('Entnahme')
  })
})
