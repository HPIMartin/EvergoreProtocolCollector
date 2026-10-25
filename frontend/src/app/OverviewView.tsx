import { useState } from 'react'

import type { ProtocolApi } from '../api'
import { FIRST_PAGE } from '../api'
import type { AvatarSummary, GuildTotals, Overview } from '../domain'
import {
  GUILD_STALE_SUMS_NOTE,
  GUILD_UNCOMPUTED_SUMS_NOTE,
  UNCOMPUTED_BALANCE_NOTE,
  UNCOMPUTED_NOTE,
  guildPositionOf,
  isUncomputed,
  rosterSplitOf,
  staleSumsNoteOf,
} from '../domain'
import type { Column, Stat, SwitchOption, Total } from '../ui'
import {
  Link,
  OptionSwitch,
  SortableTable,
  StatHeader,
  formatTimestamp,
} from '../ui'

import { LoadedView } from './LoadedView.tsx'
import { requestKeyOf } from './requestKey.ts'
import { bankPath, explanationPath, storagePath } from './route.ts'
import { hrefOf } from './href.ts'
import { useLoad } from './useLoad.ts'

export interface OverviewViewProps {
  readonly api: ProtocolApi
  readonly onFollow: (href: string) => void
}

type Figure = 'contribution' | 'balance'

const GUILD_LABEL = 'Gilde'

const FIGURES: readonly SwitchOption<Figure>[] = [
  { value: 'contribution', label: 'Nach Abzügen' },
  { value: 'balance', label: 'Vor Abzügen' },
]

export function OverviewView({ api, onFollow }: OverviewViewProps) {
  const [figure, setFigure] = useState<Figure>('contribution')
  const load = useLoad(
    () => api.overview(FIRST_PAGE),
    requestKeyOf('overview', null),
  )

  return (
    <section>
      <h2 data-testid="view-title">Übersicht</h2>
      <LoadedView load={load}>
        {(overview) => (
          <>
            <StatHeader stats={statsOf(overview.totals)} />
            <p>
              <Link href={hrefOf(explanationPath())} onFollow={onFollow}>
                Wie entstehen diese Zahlen?
              </Link>
            </p>
            <OptionSwitch
              legend="Letzte Spalte"
              name="figure"
              onSelect={setFigure}
              options={FIGURES}
              selected={figure}
            />
            <RosterTables
              columns={columnsLinkedWith(figure)}
              onFollow={onFollow}
              overview={overview}
            />
          </>
        )}
      </LoadedView>
    </section>
  )
}

interface RosterTablesProps {
  readonly columns: readonly Column<AvatarSummary>[]
  readonly onFollow: (href: string) => void
  readonly overview: Overview
}

interface RosterTable {
  readonly testId: string
  readonly caption: string
  readonly rows: readonly AvatarSummary[]
  readonly emptyMessage: string
  readonly total?: Total<AvatarSummary>
}

function RosterTables({ columns, onFollow, overview }: RosterTablesProps) {
  return (
    <>
      {rosterTablesOf(overview).map((table) => (
        <div className="roster" data-testid={table.testId} key={table.testId}>
          <SortableTable
            caption={table.caption}
            columns={columns}
            rows={table.rows}
            rowKey={(summary) => summary.avatar}
            emptyMessage={table.emptyMessage}
            total={table.total}
            mark={markOf}
            onFollow={onFollow}
          />
        </div>
      ))}
    </>
  )
}

function rosterTablesOf(overview: Overview): readonly RosterTable[] {
  const roster = rosterSplitOf(overview.items)
  const ofGuild = `von ${String(overview.totalCount)} Avataren`

  return [
    {
      testId: 'active-roster',
      caption: `Aktiv (30 Tage vor dem letzten Vorgang): ${String(roster.active.length)} ${ofGuild}`,
      rows: roster.active,
      emptyMessage: 'Noch kein Avatar erfasst.',
      total: guildTotalOf(overview.totals),
    },
    {
      testId: 'dormant-roster',
      caption: `Ruhend: ${String(roster.dormant.length)} ${ofGuild}`,
      rows: roster.dormant,
      emptyMessage: 'Kein Avatar ruht.',
    },
  ]
}

function guildTotalOf(totals: GuildTotals): Total<AvatarSummary> {
  return {
    label: GUILD_LABEL,
    row: guildRowOf(totals),
    mark: guildMarkOf(totals),
  }
}

function guildMarkOf(totals: GuildTotals): string | undefined {
  if (isUncomputed(totals)) {
    return GUILD_UNCOMPUTED_SUMS_NOTE
  }
  return totals.containsStaleSums ? GUILD_STALE_SUMS_NOTE : undefined
}

function statsOf(totals: GuildTotals): readonly Stat[] {
  const position = guildPositionOf(totals)

  return [
    {
      key: 'bank',
      label: 'Gildenbank',
      value: position.bank,
      positiveTone: 'credit',
      absentNote: UNCOMPUTED_NOTE,
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

function markOf(summary: AvatarSummary): string | null {
  if (isUncomputed(summary)) {
    return UNCOMPUTED_NOTE
  }
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
    active: true,
  }
}

function columnsLinkedWith(figure: Figure): readonly Column<AvatarSummary>[] {
  return [
    {
      key: 'avatar',
      header: 'Avatar',
      kind: 'link',
      value: (summary) => summary.avatar,
      href: (summary) => hrefOf(bankPath(summary.avatar)),
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
      href: (summary) => hrefOf(storagePath(summary.avatar)),
    },
    {
      key: 'lastBankActivity',
      header: 'Letzte Bankaktivität',
      kind: 'timestamp',
      value: (summary) => summary.lastBankActivity?.toISOString() ?? null,
      href: (summary) => hrefOf(bankPath(summary.avatar)),
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
      value: (summary) => summary.balance,
      missingNote: (summary) =>
        isUncomputed(summary) ? null : UNCOMPUTED_BALANCE_NOTE,
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
