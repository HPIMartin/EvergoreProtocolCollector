import type { NavigationLink } from '../PageFrame.tsx'
import { PageFrame } from '../PageFrame.tsx'
import { Pagination } from '../Pagination.tsx'
import type { Column } from '../SortableTable.tsx'
import { SortableTable } from '../SortableTable.tsx'
import { StatusPanel } from '../StatusPanel.tsx'
import type { LedgerRow, OverviewRow } from './fixtures.ts'
import { ledgerRows, overviewRows } from './fixtures.ts'

const navigation: readonly NavigationLink[] = [
  { label: 'Übersicht', href: '/overview', current: true },
  { label: 'Bank', href: '/avatars/Bambor/bank', current: false },
  { label: 'Lager', href: '/avatars/Bambor/storage', current: false },
]

const overviewColumns: readonly Column<OverviewRow>[] = [
  { key: 'avatar', header: 'Avatar', kind: 'text', value: (row) => row.avatar },
  {
    key: 'bankDeposited',
    header: 'Bank-Einzahlung',
    kind: 'number',
    tone: 'credit',
    value: (row) => row.bankDeposited,
  },
  {
    key: 'bankWithdrawn',
    header: 'Bank-Auszahlung',
    kind: 'number',
    tone: 'debit',
    value: (row) => row.bankWithdrawn,
  },
  {
    key: 'stored',
    header: 'Einlagerung',
    kind: 'number',
    tone: 'credit',
    value: (row) => row.stored,
  },
  {
    key: 'retrieved',
    header: 'Entnahme',
    kind: 'number',
    tone: 'debit',
    value: (row) => row.retrieved,
  },
  {
    key: 'guildValue',
    header: 'Gildenmehrwert',
    kind: 'number',
    tone: 'neutral',
    value: (row) => row.guildValue,
  },
  {
    key: 'lastStorageActivity',
    header: 'Letzte Lageraktivität',
    kind: 'timestamp',
    value: (row) => row.lastStorageActivity,
  },
  {
    key: 'lastBankActivity',
    header: 'Letzte Bankaktivität',
    kind: 'timestamp',
    value: (row) => row.lastBankActivity,
  },
]

const ledgerColumns: readonly Column<LedgerRow>[] = [
  {
    key: 'occurredAt',
    header: 'Zeitpunkt',
    kind: 'timestamp',
    value: (row) => row.occurredAt,
  },
  { key: 'item', header: 'Gegenstand', kind: 'text', value: (row) => row.item },
  {
    key: 'amount',
    header: 'Anzahl',
    kind: 'number',
    tone: 'neutral',
    value: (row) => row.amount,
  },
  {
    key: 'stored',
    header: 'Einlagerung',
    kind: 'number',
    tone: 'credit',
    value: (row) => row.stored,
  },
  {
    key: 'retrieved',
    header: 'Entnahme',
    kind: 'number',
    tone: 'debit',
    value: (row) => row.retrieved,
  },
]

export function UiGallery() {
  return (
    <PageFrame brand="Evergore Gildenbank" navigation={navigation}>
      <h1>Bausteine der Oberfläche</h1>
      <section className="page-section" data-testid="gallery-overview">
        <h2>Übersicht</h2>
        <SortableTable
          caption="Beiträge der Gildenmitglieder"
          columns={overviewColumns}
          rows={overviewRows}
          rowKey={(row) => row.avatar}
          emptyMessage="Es ist noch kein Mitglied erfasst."
          initialSort={{ columnKey: 'avatar', direction: 'ascending' }}
        />
      </section>
      <section className="page-section" data-testid="gallery-ledger">
        <h2>Lagerbuch</h2>
        <SortableTable
          caption="Bambors Lagerbewegungen, neueste zuerst"
          columns={ledgerColumns}
          rows={ledgerRows}
          rowKey={(row) => row.id}
          emptyMessage="Für diesen Avatar ist nichts verzeichnet."
        />
      </section>
      <section className="page-section" data-testid="gallery-states">
        <h2>Zustände</h2>
        <StatusPanel variant="loading" message="Die Gildenbank wird gelesen." />
        <StatusPanel
          variant="empty"
          message="Für diesen Avatar ist nichts verzeichnet."
        />
        <StatusPanel
          variant="error"
          message="Die Gildenbank ist gerade nicht erreichbar."
        />
      </section>
      <section className="page-section" data-testid="gallery-empty">
        <h2>Tabelle ohne Zeilen</h2>
        <SortableTable<LedgerRow>
          caption="Bankbewegungen"
          columns={ledgerColumns}
          rows={[]}
          rowKey={(row) => row.id}
          emptyMessage="Für diesen Zeitraum sind keine Bewegungen verzeichnet."
        />
      </section>
      <section className="page-section" data-testid="gallery-pagination">
        <h2>Seitennavigation</h2>
        <Pagination
          previousHref="/avatars/Bambor/bank"
          nextHref="/avatars/Bambor/bank?page=2"
        />
      </section>
    </PageFrame>
  )
}
