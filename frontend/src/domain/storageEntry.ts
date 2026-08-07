import type { TransferType } from './transferType.ts'

export interface StorageEntry {
  readonly timestamp: Date
  readonly avatar: string
  readonly quantity: number
  readonly name: string
  readonly quality: number
  readonly transferType: TransferType
}
