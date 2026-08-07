export const DEPOSIT = 'DEPOSIT'
export const WITHDRAWAL = 'WITHDRAWAL'

export type TransferType = typeof DEPOSIT | typeof WITHDRAWAL

const germanNames: Record<TransferType, string> = {
  [DEPOSIT]: 'Einlagerung',
  [WITHDRAWAL]: 'Entnahme',
}

export function isTransferType(value: unknown): value is TransferType {
  return typeof value === 'string' && Object.hasOwn(germanNames, value)
}

export function germanNameOf(type: TransferType): string {
  return germanNames[type]
}
