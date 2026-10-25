import { describe, expect, it } from 'vitest'

import { hrefOf } from './href.ts'

describe('href', () => {
  it('builds a plain link when nothing but the path is given', () => {
    const href = hrefOf('/overview')

    expect(href).toBe('/overview')
  })

  it('omits the page when none is given, unchanged from before paging existed', () => {
    const href = hrefOf('/avatars/Calix/bank')

    expect(href).toBe('/avatars/Calix/bank')
  })

  it('omits the page when it is the first page, for a clean URL', () => {
    const href = hrefOf('/avatars/Calix/bank', 0)

    expect(href).toBe('/avatars/Calix/bank')
  })

  it('names a page beyond the first in the built link', () => {
    const href = hrefOf('/avatars/Calix/bank', 2)

    expect(href).toBe('/avatars/Calix/bank?page=2')
  })

  it('names the sort in the built link, after the page', () => {
    const href = hrefOf('/avatars/Calix/storage', 2, {
      column: 'quantity',
      direction: 'descending',
    })

    expect(href).toBe(
      '/avatars/Calix/storage?page=2&sort=quantity&direction=descending',
    )
  })

  it('names the sort on the first page too', () => {
    const href = hrefOf('/avatars/Calix/storage', 0, {
      column: 'quantity',
      direction: 'ascending',
    })

    expect(href).toBe(
      '/avatars/Calix/storage?sort=quantity&direction=ascending',
    )
  })
})
