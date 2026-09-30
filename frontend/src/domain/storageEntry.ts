import type { TransferType } from './transferType.ts'
import { DEPOSIT, WITHDRAWAL } from './transferType.ts'

export interface StorageEntry {
  readonly timestamp: Date
  readonly avatar: string
  readonly quantity: number
  readonly name: string
  readonly quality: number
  readonly transferType: TransferType
}

const germanNames: Record<TransferType, string> = {
  [DEPOSIT]: 'Einlagerung',
  [WITHDRAWAL]: 'Entnahme',
}

export function germanTransferOfStorageEntry(entry: StorageEntry): string {
  return germanNames[entry.transferType]
}
