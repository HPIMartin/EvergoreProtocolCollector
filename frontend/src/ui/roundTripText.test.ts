import { describe, expect, it } from 'vitest'

import { describeAbstention, roundTripDetail } from './roundTripText.ts'

describe('describing a round trip', () => {
  it('names the quantity and the item', () => {
    const text = roundTripDetail({
      avatar: 'Alrik',
      item: 'Federn',
      quantity: 100,
    })

    expect(text).toBe('100 × Federn')
  })

  it('groups the digits of a four-digit quantity the German way', () => {
    const text = roundTripDetail({
      avatar: 'Alrik',
      item: 'Federn',
      quantity: 1234,
    })

    expect(text).toBe('1.234 × Federn')
  })
})

describe('describing a round-trip abstention', () => {
  it('names the avatar and the item', () => {
    const text = describeAbstention({ avatar: 'Alrik', item: 'Federn' })

    expect(text).toBe('Alrik: Federn')
  })
})
