import { describe, expect, it } from 'vitest'

import { hrefOf, tokenIn } from './token.ts'

describe('token', () => {
  it('reads the token a deep link carries', () => {
    const token = tokenIn('?token=a-test-token')

    expect(token).toBe('a-test-token')
  })

  it('reads no token from a query that names none', () => {
    const token = tokenIn('?page=2')

    expect(token).toBeNull()
  })

  it('reads no token from an empty query', () => {
    const token = tokenIn('')

    expect(token).toBeNull()
  })

  it('reads an empty token as the empty token, not as none', () => {
    const token = tokenIn('?token=')

    expect(token).toBe('')
  })

  it('reads the token beside other parameters', () => {
    const token = tokenIn('?page=2&token=a-test-token&size=10')

    expect(token).toBe('a-test-token')
  })

  it('carries the token into a link so a reload keeps working', () => {
    const href = hrefOf('/avatars/Calix/bank', 'a-test-token')

    expect(href).toBe('/avatars/Calix/bank?token=a-test-token')
  })

  it('builds a link without a token when there is none', () => {
    const href = hrefOf('/overview', null)

    expect(href).toBe('/overview')
  })

  it('escapes a token that would otherwise change the query', () => {
    const href = hrefOf('/overview', 'a&b=c')

    expect(href).toBe('/overview?token=a%26b%3Dc')
  })

  it('omits the page when none is given, unchanged from before paging existed', () => {
    const href = hrefOf('/avatars/Calix/bank', 'a-test-token')

    expect(href).toBe('/avatars/Calix/bank?token=a-test-token')
  })

  it('omits the page when it is the first page, for a clean URL', () => {
    const href = hrefOf('/avatars/Calix/bank', 'a-test-token', 0)

    expect(href).toBe('/avatars/Calix/bank?token=a-test-token')
  })

  it('names a page beyond the first in the built link', () => {
    const href = hrefOf('/avatars/Calix/bank', 'a-test-token', 2)

    expect(href).toBe('/avatars/Calix/bank?token=a-test-token&page=2')
  })

  it('names a page beyond the first in a link built without a token', () => {
    const href = hrefOf('/avatars/Calix/bank', null, 2)

    expect(href).toBe('/avatars/Calix/bank?page=2')
  })
})
