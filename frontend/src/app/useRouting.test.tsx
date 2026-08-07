import { act, cleanup, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { RouteVisitor } from './route.ts'
import { useRouting } from './useRouting.ts'

const describingVisitor: RouteVisitor<string> = {
  overview: () => 'the overview',
  bank: (avatar) => `the bank of ${avatar}`,
  storage: (avatar) => `the storage of ${avatar}`,
  unknownPath: (path) => `no view for ${path}`,
}

describe('useRouting', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/?token=a-test-token')
  })

  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('starts at the view the browser path names', () => {
    window.history.replaceState(null, '', '/avatars/Calix/bank')

    const routing = renderHook(() => useRouting())

    expect(routing.result.current.route.accept(describingVisitor)).toBe(
      'the bank of Calix',
    )
  })

  it('shows the followed view without asking the server for a page', () => {
    const routing = renderHook(() => useRouting())

    act(() => {
      routing.result.current.follow('/avatars/Calix/storage?token=a-test-token')
    })

    expect(routing.result.current.route.accept(describingVisitor)).toBe(
      'the storage of Calix',
    )
  })

  it('leaves the browser at the followed address', () => {
    const routing = renderHook(() => useRouting())

    act(() => {
      routing.result.current.follow('/avatars/Calix/storage?token=a-test-token')
    })

    expect(window.location.pathname + window.location.search).toBe(
      '/avatars/Calix/storage?token=a-test-token',
    )
  })

  it('reads the path again when the browser moves through its history', () => {
    const routing = renderHook(() => useRouting())

    act(() => {
      window.history.pushState(null, '', '/avatars/Calix/bank')
      window.dispatchEvent(new PopStateEvent('popstate'))
    })

    expect(routing.result.current.route.accept(describingVisitor)).toBe(
      'the bank of Calix',
    )
  })

  it('stops listening to history once the shell is gone', () => {
    const removing = vi.spyOn(window, 'removeEventListener')
    const routing = renderHook(() => useRouting())

    routing.unmount()

    expect(removing).toHaveBeenCalledWith('popstate', expect.any(Function))
  })

  it('reads the token of the address the shell was loaded from', () => {
    const routing = renderHook(() => useRouting())

    expect(routing.result.current.token).toBe('a-test-token')
  })
})
