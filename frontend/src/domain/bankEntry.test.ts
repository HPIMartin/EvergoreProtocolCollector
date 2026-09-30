import { describe, expect, it } from 'vitest'

import type { BankEntry } from './bankEntry.ts'
import { germanTransferOfBankEntry } from './bankEntry.ts'
import type { TransferType } from './transferType.ts'
import { DEPOSIT, WITHDRAWAL } from './transferType.ts'

function bankEntryOf(transferType: TransferType): BankEntry {
  return {
    timestamp: new Date('2026-08-05T10:15:00Z'),
    avatar: 'Calix',
    amount: 500,
    transferType,
  }
}

describe('bankEntry', () => {
  it('names a gold deposit "Einzahlung", as the game does', () => {
    const german = germanTransferOfBankEntry(bankEntryOf(DEPOSIT))

    expect(german).toBe('Einzahlung')
  })

  it('names a gold withdrawal "Entnahme"', () => {
    const german = germanTransferOfBankEntry(bankEntryOf(WITHDRAWAL))

    expect(german).toBe('Entnahme')
  })
})
