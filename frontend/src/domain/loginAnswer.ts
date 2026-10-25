export interface LoggedIn {
  readonly step: 'logged-in'
  readonly next: string
}

export type LoginAnswer =
  | LoggedIn
  | 'choose-new-password'
  | 'wrong-name-or-password'
  | 'too-many-attempts'

export type PasswordRefusal = 'breaks-the-rule' | 'same-as-before'

export type NewPasswordAnswer =
  LoggedIn | 'wrong-name-or-password' | 'too-many-attempts' | PasswordRefusal
