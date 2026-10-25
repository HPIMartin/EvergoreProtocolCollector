import { describe, expect, it } from 'vitest'

import { loginAddressFor } from './loginAddress.ts'

describe('loginAddressFor', () => {
  it.each([
    ['the overview', '/overview', '/login?next=%2Foverview'],
    [
      'a ledger with its page and sort',
      '/avatars/Aurora/bank?page=2&sort=amount',
      '/login?next=%2Favatars%2FAurora%2Fbank%3Fpage%3D2%26sort%3Damount',
    ],
    ['the root', '/', '/login?next=%2F'],
  ])('names %s as the way back', (_, address, expected) => {
    const login = loginAddressFor(address)

    expect(login).toBe(expected)
  })
})
