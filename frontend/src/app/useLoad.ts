import { useEffect, useState } from 'react'

import { NoSuchPage, Unauthorized } from '../api'

export interface LoadVisitor<T, R> {
  loading: () => R
  loaded: (value: T) => R
  unauthorized: () => R
  noSuchPage: () => R
  failed: () => R
}

export interface Load<T> {
  accept: <R>(visitor: LoadVisitor<T, R>) => R
}

export function useLoad<T>(
  load: () => Promise<T>,
  requestKey: string,
): Load<T> {
  const [outcome, setOutcome] = useState<Load<T>>(stillLoading())

  useEffect(() => {
    let awaited = true
    setOutcome(stillLoading())

    load().then(
      (value) => {
        if (awaited) {
          setOutcome(loadedWith(value))
        }
      },
      (reason: unknown) => {
        if (awaited) {
          setOutcome(failureOf(reason))
        }
      },
    )

    return () => {
      awaited = false
    }
  }, [requestKey])

  return outcome
}

function stillLoading<T>(): Load<T> {
  return { accept: (visitor) => visitor.loading() }
}

function loadedWith<T>(value: T): Load<T> {
  return { accept: (visitor) => visitor.loaded(value) }
}

function failureOf<T>(reason: unknown): Load<T> {
  if (reason instanceof Unauthorized) {
    return { accept: (visitor) => visitor.unauthorized() }
  }

  if (reason instanceof NoSuchPage) {
    return { accept: (visitor) => visitor.noSuchPage() }
  }

  return { accept: (visitor) => visitor.failed() }
}
