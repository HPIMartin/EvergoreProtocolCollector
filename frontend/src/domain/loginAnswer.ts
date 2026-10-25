export type LoginAnswer =
  | 'logged-in'
  | 'choose-new-password'
  | 'wrong-name-or-password'
  | 'too-many-attempts'
