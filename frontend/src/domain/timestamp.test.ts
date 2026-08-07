import { describe, expect, it } from 'vitest'

import { berlinTimestampOf } from './timestamp.ts'

describe('timestamp', () => {
  it('shows a winter instant in Berlin standard time', () => {
    const shown = berlinTimestampOf(new Date('2026-01-15T12:34:56Z'))

    expect(shown).toBe('15.01.2026 13:34')
  })

  it('shows a summer instant in Berlin summer time', () => {
    const shown = berlinTimestampOf(new Date('2026-07-15T12:34:56Z'))

    expect(shown).toBe('15.07.2026 14:34')
  })

  it('shows midnight as hour zero of the following day', () => {
    const shown = berlinTimestampOf(new Date('2026-01-15T23:00:00Z'))

    expect(shown).toBe('16.01.2026 00:00')
  })

  it('shows the repeated hour of the DST fall-back with its later offset', () => {
    const shown = berlinTimestampOf(new Date('2026-10-25T02:30:00Z'))

    expect(shown).toBe('25.10.2026 03:30')
  })
})
