import { useState } from 'react'

import { CollectionRefused } from '../api'
import type { AdminStatus } from '../domain'
import { formatTimestamp, LOAD_FAILED_MESSAGE } from '../ui'

const ALREADY_RUNNING_MESSAGE = 'Es läuft bereits eine Sammlung.'

export interface CollectionPanelProps {
  readonly status: AdminStatus
  readonly start: () => Promise<void>
  readonly readStatus: () => Promise<AdminStatus>
}

interface CollectionState {
  readonly runningSince: Date | null
  readonly nextFrom: Date | null
}

function stateOf(status: AdminStatus): CollectionState {
  return {
    runningSince: status.collectionRunningSince,
    nextFrom: status.nextCollectionFrom,
  }
}

export function CollectionPanel({
  status,
  start,
  readStatus,
}: CollectionPanelProps) {
  const [state, setState] = useState(stateOf(status))
  const [asking, setAsking] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)

  async function ask(): Promise<void> {
    setAsking(true)
    setNotice(null)
    try {
      try {
        await start()
      } catch (failure) {
        if (!(failure instanceof CollectionRefused)) {
          throw failure
        }
        if (failure.reason === 'running') {
          setNotice(ALREADY_RUNNING_MESSAGE)
        }
      }
      const reread = stateOf(await readStatus())
      setState(reread)
      if (reread.runningSince === null && reread.nextFrom === null) {
        setNotice(null)
      }
    } catch {
      setNotice(LOAD_FAILED_MESSAGE)
    } finally {
      setAsking(false)
    }
  }

  return (
    <>
      <StateOrButton
        state={state}
        asking={asking}
        onStart={() => {
          void ask()
        }}
      />
      {notice === null ? null : <p data-testid="collection-notice">{notice}</p>}
    </>
  )
}

interface StateOrButtonProps {
  readonly state: CollectionState
  readonly asking: boolean
  readonly onStart: () => void
}

function StateOrButton({ state, asking, onStart }: StateOrButtonProps) {
  if (state.runningSince !== null) {
    return (
      <p data-testid="collection-state">
        {`Sammlung läuft seit ${formatTimestamp(state.runningSince.toISOString())}`}
      </p>
    )
  }
  if (state.nextFrom !== null) {
    return (
      <p data-testid="collection-state">
        {`Nächste Sammlung ab ${formatTimestamp(state.nextFrom.toISOString())} möglich.`}
      </p>
    )
  }

  return (
    <button
      type="button"
      data-testid="start-collection"
      disabled={asking}
      onClick={onStart}
    >
      Jetzt sammeln
    </button>
  )
}
