import type { PasswordRefusal } from './loginAnswer.ts'

export type ChoiceRefusal = PasswordRefusal | 'entries-differ'

const MINIMUM_LENGTH = 8
const DIGIT = /\p{Nd}/u
const SPECIAL = /[\p{P}\p{S}]/u
const NORMAL_FORM = 'NFC'

export function refusalOfChoosing(
  previous: string,
  chosen: string,
  repeated: string,
): ChoiceRefusal | null {
  const chosenInNormalForm = chosen.normalize(NORMAL_FORM)
  if (chosenInNormalForm !== repeated.normalize(NORMAL_FORM)) {
    return 'entries-differ'
  }
  if (breaksTheRule(chosenInNormalForm)) {
    return 'breaks-the-rule'
  }
  if (chosenInNormalForm === previous.normalize(NORMAL_FORM)) {
    return 'same-as-before'
  }

  return null
}

function breaksTheRule(password: string): boolean {
  const characters = [...password]

  return (
    characters.length < MINIMUM_LENGTH ||
    !characters.some((character) => DIGIT.test(character)) ||
    !characters.some((character) => SPECIAL.test(character))
  )
}
