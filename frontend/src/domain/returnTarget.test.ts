import { describe, expect, it } from 'vitest'

import { returnTarget } from './returnTarget.ts'

describe('returnTarget', () => {
  it.each([
    ['the overview', '/overview'],
    ['a member ledger', '/avatars/Aurora/storage'],
    [
      'a ledger with its page and sort',
      '/avatars/Aurora/bank?page=2&sort=amount',
    ],
    ['an encoded avatar name', '/avatars/%C3%84rger/bank'],
    ['the explanation page', '/explanation'],
    ['the root', '/'],
  ])('returns to %s of this service', (_, next) => {
    const target = returnTarget(next)

    expect(target).toBe(next)
  })

  it.each([
    ['no next at all', null],
    ['an empty next', ''],
    ['a full address', 'https://evil.example/overview'],
    ['a full address of this scheme without slashes', 'http:evil.example'],
    ['a script address', 'javascript:alert(1)'],
    ['a path without its leading slash', 'overview'],
    ['an address without a scheme', '//evil.example'],
    ['an address without a scheme behind a backslash', '/\\evil.example'],
    ['an address without a scheme behind two backslashes', '\\\\evil.example'],
    ['a percent-encoded second slash', '/%2Fevil.example'],
    ['a percent-encoded second slash in capitals', '/%2fevil.example'],
    ['a percent-encoded backslash', '/%5Cevil.example'],
    ['a percent-encoded leading slash pair', '%2F%2Fevil.example'],
    ['a tab between the slashes', '/\t/evil.example'],
    ['a line feed between the slashes', '/\n/evil.example'],
    ['a malformed escape', '/%E0%A4%A'],
  ])('returns to the overview for %s', (_, next) => {
    const target = returnTarget(next)

    expect(target).toBe('/')
  })
})
