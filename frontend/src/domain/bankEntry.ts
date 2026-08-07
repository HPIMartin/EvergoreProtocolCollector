import type { TransferType } from './transferType.ts'

export interface BankEntry {
  readonly timestamp: Date
  readonly avatar: string
  readonly amount: number
  readonly transferType: TransferType
}
