import type { ProtocolApi } from '../api'
import type { UsageCounts } from '../domain'
import { formatCount, formatTimestamp } from '../ui'

import { LoadedView } from './LoadedView.tsx'
import { requestKeyOf } from './requestKey.ts'
import { useLoad } from './useLoad.ts'

export interface UsageSectionProps {
  readonly api: ProtocolApi
}

export function UsageSection({ api }: UsageSectionProps) {
  const load = useLoad(() => api.adminUsage(), requestKeyOf('usage', null))

  return (
    <section>
      <h3 data-testid="usage-title">Nutzung</h3>
      <LoadedView load={load}>
        {(usage) => (
          <table className="data-table" data-testid="usage">
            <thead>
              <tr>
                <th className="data-table__cell">Name</th>
                <th className="data-table__cell">Anmeldungen</th>
                <th className="data-table__cell">Seite geladen</th>
                <th className="data-table__cell">Tage genutzt</th>
                <th className="data-table__cell">Zuletzt</th>
                <th className="data-table__cell">Abgemeldet</th>
              </tr>
            </thead>
            <tbody>
              {usage.lastThirtyDays.accounts.map((account) => (
                <UsageRow
                  key={account.name}
                  testId="usage-row"
                  name={account.name}
                  counts={account.counts}
                />
              ))}
            </tbody>
            <tfoot>
              <UsageRow
                testId="usage-guild"
                name="Gilde"
                counts={usage.lastThirtyDays.guild}
              />
              <UsageRow
                testId="usage-admin-key"
                name="admin"
                counts={usage.lastThirtyDays.adminKey}
              />
            </tfoot>
          </table>
        )}
      </LoadedView>
    </section>
  )
}

interface UsageRowProps {
  readonly testId: string
  readonly name: string
  readonly counts: UsageCounts
}

function UsageRow({ testId, name, counts }: UsageRowProps) {
  return (
    <tr className="data-table__row" data-testid={testId}>
      <td className="data-table__cell">{name}</td>
      <td className="data-table__cell">{formatCount(counts.logins)}</td>
      <td className="data-table__cell">{formatCount(counts.pageLoads)}</td>
      <td className="data-table__cell">{formatCount(counts.daysUsed)}</td>
      <td className="data-table__cell">
        {counts.latestUse === null
          ? 'noch nie'
          : formatTimestamp(counts.latestUse.toISOString())}
      </td>
      <td className="data-table__cell">{formatCount(counts.logouts)}</td>
    </tr>
  )
}
