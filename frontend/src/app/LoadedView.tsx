import type { ReactNode } from 'react'

import { StatusPanel } from '../ui'

import type { Load } from './useLoad.ts'

export interface LoadedViewProps<T> {
  readonly load: Load<T>
  readonly children: (value: T) => ReactNode
}

export function LoadedView<T>({ load, children }: LoadedViewProps<T>) {
  return load.accept<ReactNode>({
    loading: () => <StatusPanel variant="loading" message="Wird geladen…" />,
    loaded: (value) => children(value),
    unauthorized: () => (
      <StatusPanel
        variant="error"
        message="Kein gültiges Token: der Link braucht ein token in der Adresse."
      />
    ),
    failed: (reason) => (
      <StatusPanel variant="error" message={`Fehler: ${reason}`} />
    ),
  })
}
