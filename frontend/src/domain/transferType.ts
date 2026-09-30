export const DEPOSIT = 'DEPOSIT'
export const WITHDRAWAL = 'WITHDRAWAL'

export type TransferType = typeof DEPOSIT | typeof WITHDRAWAL

const wireNames: readonly string[] = [DEPOSIT, WITHDRAWAL]

export function isTransferType(value: unknown): value is TransferType {
  return typeof value === 'string' && wireNames.includes(value)
}
