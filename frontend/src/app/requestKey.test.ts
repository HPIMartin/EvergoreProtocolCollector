import { describe, expect, it } from 'vitest'

import { requestKeyOf } from './requestKey.ts'

describe('requestKey', () => {
  it('tells two views apart', () => {
    const bank = requestKeyOf('bank', 'Calix', 'a-test-token')
    const storage = requestKeyOf('storage', 'Calix', 'a-test-token')

    expect(bank).not.toBe(storage)
  })

  it('tells two avatars apart', () => {
    const calix = requestKeyOf('bank', 'Calix', 'a-test-token')
    const nobody = requestKeyOf('bank', 'Nobody', 'a-test-token')

    expect(calix).not.toBe(nobody)
  })

  it('tells two tokens apart, because they answer different data', () => {
    const mine = requestKeyOf('overview', null, 'a-test-token')
    const yours = requestKeyOf('overview', null, 'another-token')

    expect(mine).not.toBe(yours)
  })

  it('tells a missing token apart from an empty one', () => {
    const missing = requestKeyOf('overview', null, null)
    const empty = requestKeyOf('overview', null, '')

    expect(missing).not.toBe(empty)
  })

  it('tells avatar names apart that only differ in where the parts end', () => {
    const first = requestKeyOf('bank', 'Erde', 'Eibe')
    const second = requestKeyOf('bank', 'Erde-Eibe', null)

    expect(first).not.toBe(second)
  })

  it('names the same request the same way twice', () => {
    const asked = requestKeyOf('bank', 'Calix', 'a-test-token')

    expect(asked).toBe(requestKeyOf('bank', 'Calix', 'a-test-token'))
  })
})
