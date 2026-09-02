import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import { Pagination } from './Pagination.tsx'

describe('Pagination', () => {
  afterEach(cleanup)

  it('offers both directions when there is a page before and after this one', () => {
    render(
      <Pagination
        previousHref="/avatars/Calix/bank?token=a-test-token"
        nextHref="/avatars/Calix/bank?token=a-test-token&page=2"
      />,
    )

    expect({
      previous: screen.getByTestId('pagination-previous').getAttribute('href'),
      next: screen.getByTestId('pagination-next').getAttribute('href'),
    }).toStrictEqual({
      previous: '/avatars/Calix/bank?token=a-test-token',
      next: '/avatars/Calix/bank?token=a-test-token&page=2',
    })
  })

  it('offers no way back from the first page', () => {
    render(
      <Pagination
        previousHref={null}
        nextHref="/avatars/Calix/bank?token=a-test-token&page=1"
      />,
    )

    expect(screen.queryByTestId('pagination-previous')).toBeNull()
  })

  it('offers no way forward from the last page', () => {
    render(
      <Pagination
        previousHref="/avatars/Calix/bank?token=a-test-token&page=2"
        nextHref={null}
      />,
    )

    expect(screen.queryByTestId('pagination-next')).toBeNull()
  })

  it('reports the link that was clicked instead of letting the browser reload', () => {
    const followed: string[] = []
    render(
      <Pagination
        previousHref="/avatars/Calix/bank?token=a-test-token"
        nextHref="/avatars/Calix/bank?token=a-test-token&page=2"
        onFollow={(href) => {
          followed.push(href)
        }}
      />,
    )

    fireEvent.click(screen.getByTestId('pagination-next'))

    expect(followed).toStrictEqual([
      '/avatars/Calix/bank?token=a-test-token&page=2',
    ])
  })
})
