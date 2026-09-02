import type { ReactNode } from 'react'

import type { Ledger, Page } from '../domain'
import type { Column } from '../ui'
import { SortableTable, StatusPanel } from '../ui'

import { LoadedView } from './LoadedView.tsx'
import type { Load } from './useLoad.ts'

export interface LedgerViewProps<E> {
  readonly heading: string
  readonly avatar: string
  readonly load: Load<Ledger<E>>
  readonly columns: readonly Column<E>[]
  readonly token: string | null
  readonly pathOf: (avatar: string) => string
  readonly onFollow: (href: string) => void
}

export function LedgerView<E>({
  heading,
  avatar,
  load,
  columns,
}: LedgerViewProps<E>) {
  return (
    <section>
      <h2 data-testid="view-title">{heading}</h2>
      <LoadedView load={load}>
        {(ledger) =>
          ledger.accept<ReactNode>({
            entries: (page) => entryTable(page, columns, avatar),
            unknownAvatar: (name) => (
              <StatusPanel
                variant="error"
                message={`Kein Avatar mit dem Namen ${name}.`}
              />
            ),
          })
        }
      </LoadedView>
    </section>
  )
}

function entryTable<E>(
  page: Page<E>,
  columns: readonly Column<E>[],
  avatar: string,
): ReactNode {
  return (
    <SortableTable
      caption={`${String(page.items.length)} von ${String(page.totalCount)} Einträgen`}
      columns={columns}
      rows={page.items}
      rowKey={(entry) => String(page.items.indexOf(entry))}
      emptyMessage={`Für ${avatar} ist hier kein Vorgang gespeichert.`}
    />
  )
}
