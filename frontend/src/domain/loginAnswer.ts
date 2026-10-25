export interface LoggedIn {
  readonly step: 'logged-in'
  readonly next: string
}

export type LoginAnswer =
  | LoggedIn
  | 'choose-new-password'
  | 'wrong-name-or-password'
  | 'too-many-attempts'
  | 'resting'

export type PasswordRefusal = 'breaks-the-rule' | 'same-as-before'

export type ChangePasswordAnswer =
  | 'changed'
  | 'wrong-current-password'
  | 'no-account'
  | 'too-many-attempts'
  | PasswordRefusal

export type NewPasswordAnswer =
  | LoggedIn
  | 'wrong-name-or-password'
  | 'too-many-attempts'
  | 'resting'
  | PasswordRefusal
