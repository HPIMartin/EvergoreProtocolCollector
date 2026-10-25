export type LoginAnswer =
  | 'logged-in'
  | 'choose-new-password'
  | 'wrong-name-or-password'
  | 'too-many-attempts'

export type PasswordRefusal = 'breaks-the-rule' | 'same-as-before'

export type NewPasswordAnswer =
  'logged-in' | 'wrong-name-or-password' | 'too-many-attempts' | PasswordRefusal
