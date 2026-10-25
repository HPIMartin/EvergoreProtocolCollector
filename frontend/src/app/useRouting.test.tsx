import { act, cleanup, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { RouteVisitor } from './route.ts'
import { useRouting } from './useRouting.ts'

const describingVisitor: RouteVisitor<string> = {
  overview: () => 'the overview',
  admin: () => 'the admin status page',
  explanation: () => 'the explanation page',
  password: () => 'the password change page',
  bank: (avatar) => `the bank of ${avatar}`,
  storage: (avatar) => `the storage of ${avatar}`,
  unknownPath: (path) => `no view for ${path}`,
}

describe('useRouting', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/')
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('starts at the view the browser path names', () => {
    window.history.replaceState(null, '', '/avatars/Calix/bank')

    const routing = renderHook(() => useRouting())

    const shownRoute = routing.result.current.route.accept(describingVisitor)

    expect(shownRoute).toBe('the bank of Calix')
  })

  it('shows the followed view without asking the server for a page', () => {
    const routing = renderHook(() => useRouting())

    act(() => {
      routing.result.current.follow('/avatars/Calix/storage?page=2')
    })

    const shownRoute = routing.result.current.route.accept(describingVisitor)

    expect(shownRoute).toBe('the storage of Calix')
  })

  it('replaces the address without adding a history entry', () => {
    const routing = renderHook(() => useRouting())
    const entriesBefore = window.history.length

    act(() => {
      routing.result.current.replace('/avatars/Calix/storage')
    })

    expect({
      path: window.location.pathname,
      entriesAdded: window.history.length - entriesBefore,
    }).toStrictEqual({ path: '/avatars/Calix/storage', entriesAdded: 0 })
  })

  it('leaves the browser at the followed address', () => {
    const routing = renderHook(() => useRouting())

    act(() => {
      routing.result.current.follow('/avatars/Calix/storage?page=2')
    })

    expect(window.location.pathname + window.location.search).toBe(
      '/avatars/Calix/storage?page=2',
    )
  })

  it('reads the path again when the browser moves through its history', () => {
    const routing = renderHook(() => useRouting())

    act(() => {
      window.history.pushState(null, '', '/avatars/Calix/bank')
      window.dispatchEvent(new PopStateEvent('popstate'))
    })

    const shownRoute = routing.result.current.route.accept(describingVisitor)

    expect(shownRoute).toBe('the bank of Calix')
  })

  it('stops listening to history once the shell is gone', () => {
    const removing = vi.spyOn(window, 'removeEventListener')
    const routing = renderHook(() => useRouting())

    routing.unmount()

    expect(removing).toHaveBeenCalledWith('popstate', expect.any(Function))
  })
})
