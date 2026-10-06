import type { ReactNode } from 'react'

import type { LedgerSort } from '../api'
import type { Ledger, Page } from '../domain'
import type { Column, DeliveredOrder, Sort } from '../ui'
import { Pagination, SortableTable, StatusPanel } from '../ui'

import { LoadedView } from './LoadedView.tsx'
import { hrefOf } from './token.ts'
import type { Load } from './useLoad.ts'

export interface LedgerViewProps<E> {
  readonly heading: string
  readonly avatar: string
  readonly load: Load<Ledger<E>>
  readonly columns: readonly Column<E>[]
  readonly token: string | null
  readonly sort: LedgerSort | null
  readonly pathOf: (avatar: string) => string
  readonly onFollow: (href: string) => void
}

export function LedgerView<E>({
  heading,
  avatar,
  load,
  columns,
  token,
  sort,
  pathOf,
  onFollow,
}: LedgerViewProps<E>) {
  const linkTo = (page: number, pageSort: LedgerSort | null): string =>
    hrefOf(pathOf(avatar), token, page, pageSort)

  return (
    <section>
      <h2 data-testid="view-title">{heading}</h2>
      <LoadedView load={load}>
        {(ledger) =>
          ledger.accept<ReactNode>({
            entries: (page) =>
              entryTable(page, columns, avatar, sort, linkTo, onFollow),
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
  sort: LedgerSort | null,
  linkTo: (page: number, sort: LedgerSort | null) => string,
  onFollow: (href: string) => void,
): ReactNode {
  const deliveredOrder: DeliveredOrder = {
    sort: tableSortOf(sort),
    choose: (next) => {
      onFollow(linkTo(0, { column: next.columnKey, direction: next.direction }))
    },
  }

  return (
    <>
      <SortableTable
        caption={`${String(page.items.length)} von ${String(page.totalCount)} Einträgen`}
        columns={columns}
        rows={page.items}
        rowKey={(entry) => String(page.items.indexOf(entry))}
        emptyMessage={`Für ${avatar} ist hier kein Vorgang gespeichert.`}
        deliveredOrder={deliveredOrder}
      />
      <Pagination
        previousHref={page.page === 0 ? null : linkTo(page.page - 1, sort)}
        nextHref={
          (page.page + 1) * page.size >= page.totalCount
            ? null
            : linkTo(page.page + 1, sort)
        }
        onFollow={onFollow}
      />
    </>
  )
}

function tableSortOf(sort: LedgerSort | null): Sort | null {
  if (sort === null) {
    return null
  }
  if (sort.direction !== 'ascending' && sort.direction !== 'descending') {
    return null
  }

  return { columnKey: sort.column, direction: sort.direction }
}
