import { describe, expect, it } from 'vitest'

import { describeAbstention, describeRoundTrip } from './roundTripText.ts'

describe('describing a round trip', () => {
  it('names the avatar, the quantity and the item', () => {
    const text = describeRoundTrip({
      avatar: 'Alrik',
      item: 'Federn',
      quantity: 100,
    })

    expect(text).toBe('Alrik: 100 × Federn')
  })

  it('groups the digits of a four-digit quantity the German way', () => {
    const text = describeRoundTrip({
      avatar: 'Alrik',
      item: 'Federn',
      quantity: 1234,
    })

    expect(text).toBe('Alrik: 1.234 × Federn')
  })
})

describe('describing a round-trip abstention', () => {
  it('names the avatar and the item', () => {
    const text = describeAbstention({ avatar: 'Alrik', item: 'Federn' })

    expect(text).toBe('Alrik: Federn')
  })
})
