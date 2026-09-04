import type { ProtocolApi } from '../api'
import { formatTimestamp } from '../ui'

import { LoadedView } from './LoadedView.tsx'
import { requestKeyOf } from './requestKey.ts'
import { useLoad } from './useLoad.ts'

export interface AdminViewProps {
  readonly api: ProtocolApi
}

export function AdminView({ api }: AdminViewProps) {
  const load = useLoad(
    () => api.adminStatus(),
    requestKeyOf('admin', null, null),
  )

  return (
    <section>
      <h2 data-testid="view-title">Admin</h2>
      <LoadedView load={load}>
        {(status) => (
          <>
            <p data-testid="last-updated">{`Stand: ${momentOr(status.lastUpdated, 'noch kein Abgleich gelaufen')}`}</p>
            <p data-testid="last-successful-scrape">{`Letzter Scrape: ${momentOr(status.lastSuccessfulScrape, 'noch kein Scrape gelaufen')}`}</p>
            <p data-testid="last-successful-recompute">{`Letzte Neuberechnung: ${momentOr(status.lastSuccessfulRecompute, 'noch keine Neuberechnung gelaufen')}`}</p>
            <Failure
              testId="last-scrape-failure"
              heading="Letzter Scrape-Fehler"
              instant={status.lastScrapeFailure}
            />
            <Failure
              testId="last-recompute-failure"
              heading="Letzter Fehler bei der Neuberechnung"
              instant={status.lastRecomputeFailure}
            />
            <NameList
              testId="unknown-item-names"
              heading="Unbekannte Items"
              names={status.unknownItemNames}
            />
            <NameList
              testId="failed-avatar-names"
              heading="Nicht aktualisierte Avatare"
              names={status.failedAvatarNames}
            />
          </>
        )}
      </LoadedView>
    </section>
  )
}

interface FailureProps {
  readonly testId: string
  readonly heading: string
  readonly instant: Date | null
}

function Failure({ testId, heading, instant }: FailureProps) {
  if (instant === null) {
    return null
  }

  return (
    <p data-testid={testId}>
      {heading}: {formatTimestamp(instant.toISOString())}
    </p>
  )
}

interface NameListProps {
  readonly testId: string
  readonly heading: string
  readonly names: readonly string[]
}

function NameList({ testId, heading, names }: NameListProps) {
  if (names.length === 0) {
    return null
  }

  return (
    <p data-testid={testId}>
      {heading}: {names.join(', ')}
    </p>
  )
}

function momentOr(instant: Date | null, fallback: string): string {
  return instant === null ? fallback : formatTimestamp(instant.toISOString())
}
