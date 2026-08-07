import { act, cleanup, renderHook } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import { RequestFailed, Unauthorized } from '../api'

import type { LoadVisitor } from './useLoad.ts'
import { useLoad } from './useLoad.ts'

const describingVisitor: LoadVisitor<string, string> = {
  loading: () => 'still loading',
  loaded: (value) => `loaded ${value}`,
  unauthorized: () => 'the token was refused',
  failed: (reason) => `failed: ${reason}`,
}

function neverAnswering(): Promise<string> {
  return new Promise<string>(() => undefined)
}

describe('useLoad', () => {
  afterEach(cleanup)

  it('is loading until the answer arrives', () => {
    const load = renderHook(() => useLoad(neverAnswering, 'a-key'))

    expect(load.result.current.accept(describingVisitor)).toBe('still loading')
  })

  it('shows the value once it has arrived', async () => {
    const load = renderHook(() =>
      useLoad(() => Promise.resolve('the overview'), 'a-key'),
    )

    await act(async () => undefined)

    expect(load.result.current.accept(describingVisitor)).toBe(
      'loaded the overview',
    )
  })

  it('reports a refused token as its own outcome', async () => {
    const load = renderHook(() =>
      useLoad(() => Promise.reject(new Unauthorized()), 'a-key'),
    )

    await act(async () => undefined)

    expect(load.result.current.accept(describingVisitor)).toBe(
      'the token was refused',
    )
  })

  it('reports any other failure with its reason', async () => {
    const load = renderHook(() =>
      useLoad(() => Promise.reject(new RequestFailed(500)), 'a-key'),
    )

    await act(async () => undefined)

    expect(load.result.current.accept(describingVisitor)).toBe(
      'failed: The API answered 500',
    )
  })

  it('reports a rejection that is not an error at all', async () => {
    const load = renderHook(() =>
      useLoad(() => Promise.reject('the network is gone'), 'a-key'),
    )

    await act(async () => undefined)

    expect(load.result.current.accept(describingVisitor)).toBe(
      'failed: the network is gone',
    )
  })

  it('loads again when the request key changes', async () => {
    const answers = ['the first view', 'the second view']
    const load = renderHook(
      ({ key }: { key: string }) =>
        useLoad(() => Promise.resolve(answers.shift() ?? 'nothing left'), key),
      { initialProps: { key: 'first' } },
    )

    load.rerender({ key: 'second' })
    await act(async () => undefined)

    expect(load.result.current.accept(describingVisitor)).toBe(
      'loaded the second view',
    )
  })

  it('does not load again while the request key stays the same', async () => {
    const answers = ['the first view', 'the second view']
    const load = renderHook(
      ({ key }: { key: string }) =>
        useLoad(() => Promise.resolve(answers.shift() ?? 'nothing left'), key),
      { initialProps: { key: 'first' } },
    )

    load.rerender({ key: 'first' })
    await act(async () => undefined)

    expect(load.result.current.accept(describingVisitor)).toBe(
      'loaded the first view',
    )
  })

  it('ignores the answer to a request its key has already replaced', async () => {
    let answerFirst: (value: string) => void = () => undefined
    const first = new Promise<string>((resolve) => {
      answerFirst = resolve
    })
    const load = renderHook(
      ({ key }: { key: string }) =>
        useLoad(
          () => (key === 'first' ? first : Promise.resolve('the second view')),
          key,
        ),
      { initialProps: { key: 'first' } },
    )

    load.rerender({ key: 'second' })
    await act(async () => {
      answerFirst('the first view')
    })

    expect(load.result.current.accept(describingVisitor)).toBe(
      'loaded the second view',
    )
  })
})
