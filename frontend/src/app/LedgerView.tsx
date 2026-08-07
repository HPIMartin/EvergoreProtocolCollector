import type { ReactNode } from 'react'

import type { Ledger } from '../domain'
import { Link } from '../ui'

import { LoadedView } from './LoadedView.tsx'
import type { Load } from './useLoad.ts'

export interface LedgerViewProps<E> {
  readonly heading: string
  readonly avatar: string
  readonly load: Load<Ledger<E>>
  readonly tableOf: (entries: readonly E[]) => ReactNode
  readonly overviewHref: string
  readonly onFollow: (href: string) => void
}

export function LedgerView<E>({
  heading,
  avatar,
  load,
  tableOf,
  overviewHref,
  onFollow,
}: LedgerViewProps<E>) {
  return (
    <section>
      <h2 data-testid="view-title">{heading}</h2>
      <p>
        <Link href={overviewHref} onFollow={onFollow}>
          Zur Übersicht
        </Link>
      </p>
      <LoadedView load={load}>
        {(ledger) =>
          ledger.accept<ReactNode>({
            entries: (page) =>
              page.totalCount === 0 ? (
                <p data-testid="view-empty" role="status">
                  {`Für ${avatar} ist hier kein Vorgang gespeichert.`}
                </p>
              ) : (
                <>
                  <p data-testid="entry-count">{`${String(page.items.length)} von ${String(page.totalCount)} Einträgen`}</p>
                  {tableOf(page.items)}
                </>
              ),
            unknownAvatar: (name) => (
              <p data-testid="view-unknown-avatar" role="alert">
                {`Kein Avatar mit dem Namen ${name}.`}
              </p>
            ),
          })
        }
      </LoadedView>
    </section>
  )
}
