export const MEMBER = 'member'
export const ADMIN = 'admin'

export type Role = typeof MEMBER | typeof ADMIN

const wireNames: readonly string[] = [MEMBER, ADMIN]

export function isRole(value: unknown): value is Role {
  return typeof value === 'string' && wireNames.includes(value)
}

export interface Account {
  readonly name: string
  readonly role: Role
  readonly own: boolean
  readonly rests: boolean
  readonly lastLogin: Date | null
}
