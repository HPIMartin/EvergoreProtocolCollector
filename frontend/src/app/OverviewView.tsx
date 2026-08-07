import type { AvatarSummary } from '../domain'
import { berlinTimestampOf } from '../domain'
import type { ProtocolApi } from '../api'
import { FIRST_PAGE } from '../api'
import type { AvatarSummaryRow } from '../ui'
import { AvatarSummaryTable } from '../ui'

import { LoadedView } from './LoadedView.tsx'
import { requestKeyOf } from './requestKey.ts'
import { bankPath, storagePath } from './route.ts'
import { hrefOf } from './token.ts'
import { useLoad } from './useLoad.ts'

export interface OverviewViewProps {
  readonly api: ProtocolApi
  readonly token: string | null
  readonly onFollow: (href: string) => void
}

export function OverviewView({ api, token, onFollow }: OverviewViewProps) {
  const load = useLoad(
    () => api.overview(FIRST_PAGE),
    requestKeyOf('overview', null, token),
  )

  return (
    <section>
      <h2 data-testid="view-title">Übersicht</h2>
      <LoadedView load={load}>
        {(overview) => (
          <>
            <p data-testid="last-updated">{`Stand: ${freshnessOf(overview.lastUpdated)}`}</p>
            {overview.totalCount === 0 ? (
              <p data-testid="view-empty" role="status">
                Noch kein Avatar erfasst.
              </p>
            ) : (
              <>
                <p data-testid="avatar-count">{`${String(overview.items.length)} von ${String(overview.totalCount)} Avataren`}</p>
                <AvatarSummaryTable
                  rows={rowsOf(overview.items, token)}
                  onFollow={onFollow}
                />
              </>
            )}
          </>
        )}
      </LoadedView>
    </section>
  )
}

function freshnessOf(lastUpdated: Date | null): string {
  return lastUpdated === null
    ? 'noch kein Abgleich gelaufen'
    : berlinTimestampOf(lastUpdated)
}

function rowsOf(
  summaries: readonly AvatarSummary[],
  token: string | null,
): AvatarSummaryRow[] {
  return summaries.map((summary) => ({
    summary,
    bankHref: hrefOf(bankPath(summary.avatar), token),
    storageHref: hrefOf(storagePath(summary.avatar), token),
  }))
}
