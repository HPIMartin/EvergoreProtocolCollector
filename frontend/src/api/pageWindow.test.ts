import { describe, expect, it } from 'vitest'

import { FIRST_PAGE, windowOf } from './pageWindow.ts'

describe('pageWindow', () => {
  it('builds a window for an arbitrary page with the size of the first page', () => {
    const window = windowOf(3)

    expect(window).toStrictEqual({ page: 3, size: FIRST_PAGE.size })
  })
})
