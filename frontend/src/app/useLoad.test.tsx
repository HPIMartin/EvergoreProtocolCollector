import { act, cleanup, renderHook } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import { Forbidden, NoSuchPage, RequestFailed, Unauthorized } from '../api'

import type { LoadVisitor } from './useLoad.ts'
import { useLoad } from './useLoad.ts'

const describingVisitor: LoadVisitor<string, string> = {
  loading: () => 'still loading',
  loaded: (value) => `loaded ${value}`,
  unauthorized: () => 'the login was refused',
  forbidden: () => 'the role was refused',
  noSuchPage: () => 'the page does not exist',
  failed: () => 'the load failed',
}

function neverAnswering(): Promise<string> {
  return new Promise<string>(() => undefined)
}

describe('useLoad', () => {
  afterEach(cleanup)

  it('is loading until the answer arrives', () => {
    const load = renderHook(() => useLoad(neverAnswering, 'a-key'))

    const outcome = load.result.current.accept(describingVisitor)

    expect(outcome).toBe('still loading')
  })

  it('shows the value once it has arrived', async () => {
    const load = renderHook(() =>
      useLoad(() => Promise.resolve('the overview'), 'a-key'),
    )

    await act(async () => undefined)

    const outcome = load.result.current.accept(describingVisitor)

    expect(outcome).toBe('loaded the overview')
  })

  it('reports a refused login as its own outcome', async () => {
    const load = renderHook(() =>
      useLoad(() => Promise.reject(new Unauthorized()), 'a-key'),
    )

    await act(async () => undefined)

    const outcome = load.result.current.accept(describingVisitor)

    expect(outcome).toBe('the login was refused')
  })

  it('reports a refused role as its own outcome', async () => {
    const load = renderHook(() =>
      useLoad(() => Promise.reject(new Forbidden()), 'a-key'),
    )

    await act(async () => undefined)

    const outcome = load.result.current.accept(describingVisitor)

    expect(outcome).toBe('the role was refused')
  })

  it('reports a page that does not exist as its own outcome', async () => {
    const load = renderHook(() =>
      useLoad(() => Promise.reject(new NoSuchPage()), 'a-key'),
    )

    await act(async () => undefined)

    const outcome = load.result.current.accept(describingVisitor)

    expect(outcome).toBe('the page does not exist')
  })

  it('reports any other failure as a failed load', async () => {
    const load = renderHook(() =>
      useLoad(() => Promise.reject(new RequestFailed(500)), 'a-key'),
    )

    await act(async () => undefined)

    const outcome = load.result.current.accept(describingVisitor)

    expect(outcome).toBe('the load failed')
  })

  it('reports a rejection that is not an error at all', async () => {
    const load = renderHook(() =>
      useLoad(() => Promise.reject('the network is gone'), 'a-key'),
    )

    await act(async () => undefined)

    const outcome = load.result.current.accept(describingVisitor)

    expect(outcome).toBe('the load failed')
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

    const outcome = load.result.current.accept(describingVisitor)

    expect(outcome).toBe('loaded the second view')
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

    const outcome = load.result.current.accept(describingVisitor)

    expect(outcome).toBe('loaded the first view')
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

    const outcome = load.result.current.accept(describingVisitor)

    expect(outcome).toBe('loaded the second view')
  })
})
