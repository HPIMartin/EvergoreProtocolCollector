import { describe, expect, it } from 'vitest'

import { DEPOSIT, WITHDRAWAL, isTransferType } from './transferType.ts'

describe('transferType', () => {
  it.each([DEPOSIT, WITHDRAWAL])('recognizes the wire name %s', (wireName) => {
    const recognized = isTransferType(wireName)

    expect(recognized).toBe(true)
  })

  it.each(['deposit', 'EINLAGERUNG', 'TRANSFER', '', 0, null, undefined, {}])(
    'refuses %o as a transfer type',
    (value) => {
      const recognized = isTransferType(value)

      expect(recognized).toBe(false)
    },
  )
})
