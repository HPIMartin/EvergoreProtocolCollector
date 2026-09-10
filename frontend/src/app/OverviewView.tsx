import { useState } from 'react'

import type { ProtocolApi } from '../api'
import { FIRST_PAGE } from '../api'
import type { AvatarSummary, GuildTotals } from '../domain'
import {
  GUILD_STALE_SUMS_NOTE,
  UNCOMPUTED_BALANCE_NOTE,
  UNCOMPUTED_NOTE,
  balanceOf,
  guildPositionOf,
  staleSumsNoteOf,
} from '../domain'
import type { Column, Stat, SwitchOption } from '../ui'
import { OptionSwitch, SortableTable, StatHeader, formatTimestamp } from '../ui'

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

type Figure = 'contribution' | 'balance'

const GUILD_LABEL = 'Gilde'

const FIGURES: readonly SwitchOption<Figure>[] = [
  { value: 'contribution', label: 'Nach Abzügen' },
  { value: 'balance', label: 'Vor Abzügen' },
]

export function OverviewView({ api, token, onFollow }: OverviewViewProps) {
  const [figure, setFigure] = useState<Figure>('contribution')
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
            <StatHeader stats={statsOf(overview.totals)} />
            <OptionSwitch
              legend="Letzte Spalte"
              name="figure"
              onSelect={setFigure}
              options={FIGURES}
              selected={figure}
            />
            <SortableTable
              caption={`${String(overview.items.length)} von ${String(overview.totalCount)} Avataren`}
              columns={columnsLinkedWith(token, figure)}
              rows={overview.items}
              rowKey={(summary) => summary.avatar}
              emptyMessage="Noch kein Avatar erfasst."
              total={{
                label: GUILD_LABEL,
                row: guildRowOf(overview.totals),
                mark: overview.totals.containsStaleSums
                  ? GUILD_STALE_SUMS_NOTE
                  : undefined,
              }}
              mark={markOfStaleSums}
              onFollow={onFollow}
            />
          </>
        )}
      </LoadedView>
    </section>
  )
}

function statsOf(totals: GuildTotals): readonly Stat[] {
  const position = guildPositionOf(totals)

  return [
    {
      key: 'bank',
      label: 'Gildenbank',
      value: position.bank,
      positiveTone: 'credit',
    },
    {
      key: 'storage',
      label: 'Gildenlagerwert',
      value: position.storageValue,
      positiveTone: 'credit',
      absentNote: UNCOMPUTED_NOTE,
    },
    {
      key: 'donation',
      label: 'Gildenspende',
      value: position.donation,
      positiveTone: 'credit',
      absentNote: UNCOMPUTED_NOTE,
    },
    {
      key: 'subsidy',
      label: 'Handwerkssubventionen',
      value: position.craftSubsidy,
      positiveTone: 'neutral',
      absentNote: UNCOMPUTED_NOTE,
    },
  ]
}

function markOfStaleSums(summary: AvatarSummary): string | null {
  return summary.staleSumsFrom === null
    ? null
    : staleSumsNoteOf(formatTimestamp(summary.staleSumsFrom.toISOString()))
}

function guildRowOf(totals: GuildTotals): AvatarSummary {
  return {
    avatar: GUILD_LABEL,
    ...totals,
    lastBankActivity: null,
    lastStorageActivity: null,
    staleSumsFrom: null,
  }
}

function columnsLinkedWith(
  token: string | null,
  figure: Figure,
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
      key: 'bankDeposited',
      header: 'Bank-Einzahlung',
      kind: 'number',
      tone: 'credit',
      value: (summary) => summary.bankDeposited,
    },
    {
      key: 'bankWithdrawn',
      header: 'Bank-Auszahlung',
      kind: 'number',
      tone: 'debit',
      value: (summary) => summary.bankWithdrawn,
    },
    {
      key: 'storageDeposited',
      header: 'Einlagerung',
      kind: 'number',
      tone: 'credit',
      value: (summary) => summary.storageDeposited,
    },
    {
      key: 'storageWithdrawn',
      header: 'Entnahme',
      kind: 'number',
      tone: 'debit',
      value: (summary) => summary.storageWithdrawn,
    },
    figureColumn(figure),
    {
      key: 'lastStorageActivity',
      header: 'Letzte Lageraktivität',
      kind: 'timestamp',
      value: (summary) => summary.lastStorageActivity?.toISOString() ?? null,
      href: (summary) => hrefOf(storagePath(summary.avatar), token),
    },
    {
      key: 'lastBankActivity',
      header: 'Letzte Bankaktivität',
      kind: 'timestamp',
      value: (summary) => summary.lastBankActivity?.toISOString() ?? null,
      href: (summary) => hrefOf(bankPath(summary.avatar), token),
    },
  ]
}

function figureColumn(figure: Figure): Column<AvatarSummary> {
  const header = headerOf(figure)
  if (figure === 'balance') {
    return {
      key: 'figure',
      header,
      kind: 'number',
      tone: 'neutral',
      value: (summary) => balanceOf(summary),
      missingNote: UNCOMPUTED_BALANCE_NOTE,
    }
  }

  return {
    key: 'figure',
    header,
    kind: 'number',
    tone: 'neutral',
    value: (summary) => summary.net,
  }
}

function headerOf(figure: Figure): string {
  const chosen = FIGURES.find((option) => option.value === figure)
  if (chosen === undefined) {
    throw new TypeError(`Unknown figure: ${figure}`)
  }
  return chosen.label
}
