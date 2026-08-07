import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import { Link } from './Link.tsx'

interface FollowRecorder {
  readonly followed: string[]
  readonly follow: (href: string) => void
}

function recordingFollow(): FollowRecorder {
  const followed: string[] = []

  return {
    followed,
    follow: (href) => {
      followed.push(href)
    },
  }
}

describe('Link', () => {
  afterEach(cleanup)

  it('offers the address as a real link so it can be bookmarked', () => {
    const recorder = recordingFollow()
    render(
      <Link
        href="/avatars/Calix/bank?token=a-test-token"
        onFollow={recorder.follow}
      >
        Bank
      </Link>,
    )

    const link = screen.getByRole('link', { name: 'Bank' })

    expect(link.getAttribute('href')).toBe(
      '/avatars/Calix/bank?token=a-test-token',
    )
  })

  it('reports a plain click instead of letting the browser reload the shell', () => {
    const recorder = recordingFollow()
    render(
      <Link href="/overview?token=a-test-token" onFollow={recorder.follow}>
        Overview
      </Link>,
    )

    const notCancelled = fireEvent.click(screen.getByRole('link'))

    expect({ notCancelled, followed: recorder.followed }).toStrictEqual({
      notCancelled: false,
      followed: ['/overview?token=a-test-token'],
    })
  })

  it.each([
    { modifier: 'ctrlKey', click: { ctrlKey: true } },
    { modifier: 'metaKey', click: { metaKey: true } },
    { modifier: 'shiftKey', click: { shiftKey: true } },
    { modifier: 'altKey', click: { altKey: true } },
    { modifier: 'the middle button', click: { button: 1 } },
  ])('leaves a click with $modifier to the browser', ({ click }) => {
    const recorder = recordingFollow()
    render(
      <Link href="/overview?token=a-test-token" onFollow={recorder.follow}>
        Overview
      </Link>,
    )

    const notCancelled = fireEvent.click(screen.getByRole('link'), click)

    expect({ notCancelled, followed: recorder.followed }).toStrictEqual({
      notCancelled: true,
      followed: [],
    })
  })
})
