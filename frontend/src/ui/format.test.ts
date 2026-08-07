import { describe, expect, it } from 'vitest'

import { formatGold, formatTimestamp } from './format.ts'

describe('formatGold', () => {
  it.each([
    { value: 0, expected: '0' },
    { value: 420, expected: '420' },
    { value: 1234, expected: '1.234' },
    { value: 1234567, expected: '1.234.567' },
    { value: -200608, expected: '-200.608' },
    { value: -1053554, expected: '-1.053.554' },
  ])('renders $value as $expected', ({ value, expected }) => {
    const rendered = formatGold(value)

    expect(rendered).toBe(expected)
  })
})

describe('formatTimestamp', () => {
  it.each([
    { instant: '2022-07-10T12:23:00Z', expected: '10.07.2022 14:23' },
    { instant: '2022-01-10T12:23:00Z', expected: '10.01.2022 13:23' },
    { instant: '2022-07-10T22:30:00Z', expected: '11.07.2022 00:30' },
    { instant: '2022-03-27T00:59:00Z', expected: '27.03.2022 01:59' },
    { instant: '2022-03-27T01:00:00Z', expected: '27.03.2022 03:00' },
    { instant: '2022-07-10T15:23:00+05:00', expected: '10.07.2022 12:23' },
  ])('renders $instant as $expected', ({ instant, expected }) => {
    const rendered = formatTimestamp(instant)

    expect(rendered).toBe(expected)
  })

  it('rejects a value that is no instant', () => {
    const rejecting = () => formatTimestamp('the day before yesterday')

    expect(rejecting).toThrowError('Not an instant: the day before yesterday')
  })
})
