import type { ReactNode } from 'react'

import type { Load } from './useLoad.ts'

export interface LoadedViewProps<T> {
  readonly load: Load<T>
  readonly children: (value: T) => ReactNode
}

export function LoadedView<T>({ load, children }: LoadedViewProps<T>) {
  return load.accept<ReactNode>({
    loading: () => (
      <p data-testid="view-loading" role="status">
        Wird geladen…
      </p>
    ),
    loaded: (value) => children(value),
    unauthorized: () => (
      <p data-testid="view-unauthorized" role="alert">
        Kein gültiges Token: der Link braucht ein token in der Adresse.
      </p>
    ),
    failed: (reason) => (
      <p data-testid="view-failed" role="alert">{`Fehler: ${reason}`}</p>
    ),
  })
}
