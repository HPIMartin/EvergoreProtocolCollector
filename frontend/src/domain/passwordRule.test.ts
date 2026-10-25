import { describe, expect, it } from 'vitest'

import { refusalOfChoosing } from './passwordRule.ts'

function text(...codePoints: number[]): string {
  return String.fromCodePoint(...codePoints)
}

const BASE = 'abcdefg1'
const COMBINING_DIAERESIS = text(0x0308)
const COMPOSED = `${text(0x00c4)}rger-Gilde9`
const DECOMPOSED = `A${COMBINING_DIAERESIS}rger-Gilde9`

const PARITY_TABLE: readonly (readonly [string, boolean])[] = [
  [`${BASE}!`, false],
  [`${BASE}${text(0x00a0)}`, true],
  [`${BASE}${text(0x202f)}`, true],
  [`${BASE}${text(0xfeff)}`, true],
  [`${BASE}${text(0x001c)}`, true],
  [`${BASE}${text(0x1f600)}`, false],
  [`${BASE}${COMBINING_DIAERESIS}`, true],
  [`A${COMBINING_DIAERESIS}bc-123`, true],
  [`A${COMBINING_DIAERESIS}bcdefg1`, true],
  [`A${COMBINING_DIAERESIS}bcdef-1`, false],
  [`${BASE}${text(0x20ac)}`, false],
  [`${BASE}${text(0x0085)}`, true],
]

const NOT_SPECIAL = [
  0x0020, 0x0009, 0x00a0, 0x2007, 0x202f, 0x2028, 0xfeff, 0x001c, 0x001f,
  0x0085, 0x200b, 0x0308,
]

const SPECIAL = [
  0x002b, 0x005e, 0x0060, 0x007e, 0x0024, 0x007c, 0x003c, 0x00a9, 0x20ac,
  0x1f600,
]

function isRefused(password: string): boolean {
  return refusalOfChoosing('', password, password) === 'breaks-the-rule'
}

describe('the password rule of the page', () => {
  it.each(PARITY_TABLE)(
    'judges %j like the service: refused is %s',
    (password, refused) => {
      const judged = isRefused(password)

      expect(judged).toBe(refused)
    },
  )

  it.each(NOT_SPECIAL)(
    'counts the code point %i as no special character',
    (codePoint) => {
      const judged = isRefused(`${BASE}${text(codePoint)}`)

      expect(judged).toBe(true)
    },
  )

  it.each(SPECIAL)(
    'counts the punctuation or symbol code point %i as special',
    (codePoint) => {
      const judged = isRefused(`${BASE}${text(codePoint)}`)

      expect(judged).toBe(false)
    },
  )

  it('counts a lone surrogate as no special character', () => {
    const judged = isRefused(`${BASE}${String.fromCharCode(0xd800)}`)

    expect(judged).toBe(true)
  })

  it('judges a decomposed letter as the one letter it shows', () => {
    const judged = isRefused(`A${COMBINING_DIAERESIS}bc-123`)

    expect(judged).toBe(true)
  })

  it('takes a decomposed and a composed entry for the same entry', () => {
    const refusal = refusalOfChoosing('Gilde-2026!', COMPOSED, DECOMPOSED)

    expect(refusal).toBeNull()
  })

  it('refuses a new password that is the previous one in another normal form', () => {
    const refusal = refusalOfChoosing(COMPOSED, DECOMPOSED, DECOMPOSED)

    expect(refusal).toBe('same-as-before')
  })

  it('refuses two entries that differ', () => {
    const refusal = refusalOfChoosing(
      'Gilde-2026!',
      'Winter-2026!',
      'Winter-2026?',
    )

    expect(refusal).toBe('entries-differ')
  })
})
