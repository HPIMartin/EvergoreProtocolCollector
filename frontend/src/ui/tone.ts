export type Tone = 'neutral' | 'credit' | 'debit'

export const toneOf = (value: number, positiveTone: Tone): Tone => {
  if (value === 0) {
    return 'neutral'
  }
  return value < 0 ? 'debit' : positiveTone
}
