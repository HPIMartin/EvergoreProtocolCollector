import type { TransferType } from './transferType.ts'
import { DEPOSIT, WITHDRAWAL } from './transferType.ts'

export interface BankEntry {
  readonly timestamp: Date
  readonly avatar: string
  readonly amount: number
  readonly transferType: TransferType
}

const germanNames: Record<TransferType, string> = {
  [DEPOSIT]: 'Einzahlung',
  [WITHDRAWAL]: 'Entnahme',
}

export function germanTransferOfBankEntry(entry: BankEntry): string {
  return germanNames[entry.transferType]
}
