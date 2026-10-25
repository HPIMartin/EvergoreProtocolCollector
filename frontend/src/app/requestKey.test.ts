import { describe, expect, it } from 'vitest'

import { requestKeyOf } from './requestKey.ts'

describe('requestKey', () => {
  it('tells two views apart', () => {
    const bank = requestKeyOf('bank', 'Calix')
    const storage = requestKeyOf('storage', 'Calix')

    expect(bank).not.toBe(storage)
  })

  it('tells two avatars apart', () => {
    const calix = requestKeyOf('bank', 'Calix')
    const nobody = requestKeyOf('bank', 'Nobody')

    expect(calix).not.toBe(nobody)
  })

  it('tells a view without an avatar apart from one of an avatar', () => {
    const overview = requestKeyOf('overview', null)
    const ledger = requestKeyOf('overview', 'Calix')

    expect(overview).not.toBe(ledger)
  })

  it('tells avatar names apart that only differ in where the parts end', () => {
    const first = requestKeyOf('bank', 'Erde', 1)
    const second = requestKeyOf('bank', 'Erde-1')

    expect(first).not.toBe(second)
  })

  it('names the same request the same way twice', () => {
    const asked = requestKeyOf('bank', 'Calix')

    expect(asked).toBe(requestKeyOf('bank', 'Calix'))
  })

  it('tells two pages of the same ledger apart', () => {
    const first = requestKeyOf('bank', 'Calix', 0)
    const second = requestKeyOf('bank', 'Calix', 1)

    expect(first).not.toBe(second)
  })

  it('tells two sorts of the same page apart', () => {
    const byQuantity = requestKeyOf('storage', 'Calix', 0, {
      column: 'quantity',
      direction: 'descending',
    })
    const byName = requestKeyOf('storage', 'Calix', 0, {
      column: 'name',
      direction: 'descending',
    })

    expect(byQuantity).not.toBe(byName)
  })

  it('tells the two directions of one column apart', () => {
    const ascending = requestKeyOf('storage', 'Calix', 0, {
      column: 'quantity',
      direction: 'ascending',
    })
    const descending = requestKeyOf('storage', 'Calix', 0, {
      column: 'quantity',
      direction: 'descending',
    })

    expect(ascending).not.toBe(descending)
  })

  it('treats an omitted page as the first page', () => {
    const omitted = requestKeyOf('bank', 'Calix')
    const explicit = requestKeyOf('bank', 'Calix', 0)

    expect(omitted).toBe(explicit)
  })
})
