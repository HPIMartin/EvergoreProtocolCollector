import type { ReactNode } from 'react'

import { StatusPanel } from '../ui'

import { LoginRequired } from './LoginRequired.tsx'
import type { Load } from './useLoad.ts'

export interface LoadedViewProps<T> {
  readonly load: Load<T>
  readonly children: (value: T) => ReactNode
}

export function LoadedView<T>({ load, children }: LoadedViewProps<T>) {
  return load.accept<ReactNode>({
    loading: () => <StatusPanel variant="loading" message="Wird geladen…" />,
    loaded: (value) => children(value),
    unauthorized: () => <LoginRequired />,
    noSuchPage: () => (
      <StatusPanel variant="error" message="Diese Seite gibt es nicht." />
    ),
    failed: () => (
      <StatusPanel
        variant="error"
        message="Die Daten konnten nicht geladen werden. Bitte später erneut versuchen."
      />
    ),
  })
}
