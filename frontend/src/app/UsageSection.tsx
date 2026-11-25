import { useState } from 'react'

import type { ProtocolApi } from '../api'
import type { UsageCounts, UsageWindow } from '../domain'
import type { SwitchOption } from '../ui'
import { formatCount, formatTimestamp, OptionSwitch } from '../ui'

import { LoadedView } from './LoadedView.tsx'
import { requestKeyOf } from './requestKey.ts'
import { useLoad } from './useLoad.ts'

export interface UsageSectionProps {
  readonly api: ProtocolApi
}

type WindowName = 'lastThirtyDays' | 'allTheTime'

const WINDOWS: readonly SwitchOption<WindowName>[] = [
  { value: 'lastThirtyDays', label: 'Letzte 30 Tage' },
  { value: 'allTheTime', label: 'Alles' },
]

export function UsageSection({ api }: UsageSectionProps) {
  const load = useLoad(() => api.adminUsage(), requestKeyOf('usage', null))
  const [shown, setShown] = useState<WindowName>('lastThirtyDays')

  return (
    <section>
      <h3 data-testid="usage-title">Nutzung</h3>
      <LoadedView load={load}>
        {(usage) => (
          <>
            <OptionSwitch
              legend="Zeitraum"
              name="usage-window"
              onSelect={setShown}
              options={WINDOWS}
              selected={shown}
            />
            <UsageTable window={usage[shown]} />
          </>
        )}
      </LoadedView>
    </section>
  )
}

interface UsageTableProps {
  readonly window: UsageWindow
}

function UsageTable({ window }: UsageTableProps) {
  return (
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
        {window.accounts.map((account) => (
          <UsageRow
            key={account.name}
            testId="usage-row"
            name={account.name}
            counts={account.counts}
          />
        ))}
      </tbody>
      <tfoot>
        <UsageRow testId="usage-guild" name="Gilde" counts={window.guild} />
        <UsageRow
          testId="usage-admin-key"
          name="admin"
          counts={window.adminKey}
        />
      </tfoot>
    </table>
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
