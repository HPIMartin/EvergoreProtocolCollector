import type { ProtocolApi } from '../api'
import { FIRST_PAGE } from '../api'
import type { AvatarSummary } from '../domain'
import type { Column } from '../ui'
import { SortableTable, formatTimestamp } from '../ui'

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
            <SortableTable
              caption={`${String(overview.items.length)} von ${String(overview.totalCount)} Avataren`}
              columns={columnsLinkedWith(token)}
              rows={overview.items}
              rowKey={(summary) => summary.avatar}
              emptyMessage="Noch kein Avatar erfasst."
              onFollow={onFollow}
            />
          </>
        )}
      </LoadedView>
    </section>
  )
}

function freshnessOf(lastUpdated: Date | null): string {
  return lastUpdated === null
    ? 'noch kein Abgleich gelaufen'
    : formatTimestamp(lastUpdated.toISOString())
}

function columnsLinkedWith(
  token: string | null,
): readonly Column<AvatarSummary>[] {
  return [
    {
      key: 'avatar',
      header: 'Avatar',
      kind: 'link',
      value: (summary) => summary.avatar,
      href: (summary) => hrefOf(bankPath(summary.avatar), token),
    },
    {
      key: 'withdrawn',
      header: 'Entnommen',
      kind: 'number',
      tone: 'debit',
      value: (summary) => summary.withdrawn,
    },
    {
      key: 'deposited',
      header: 'Eingelagert',
      kind: 'number',
      tone: 'credit',
      value: (summary) => summary.deposited,
    },
    {
      key: 'storage',
      header: 'Lager',
      kind: 'link',
      value: () => 'öffnen',
      href: (summary) => hrefOf(storagePath(summary.avatar), token),
    },
  ]
}
