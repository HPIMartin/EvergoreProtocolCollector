import { useState } from 'react'

import { formatGold, formatTimestamp, instantOf } from './format.ts'
import { Link } from './Link.tsx'
import { StatusPanel } from './StatusPanel.tsx'

export type ColumnTone = 'neutral' | 'credit' | 'debit'

export type SortDirection = 'ascending' | 'descending'

export type Sort = {
  readonly columnKey: string
  readonly direction: SortDirection
}

type ColumnHead = {
  readonly key: string
  readonly header: string
}

export type Column<Row> =
  | (ColumnHead & {
      readonly kind: 'text'
      readonly value: (row: Row) => string
    })
  | (ColumnHead & {
      readonly kind: 'number'
      readonly tone: ColumnTone
      readonly value: (row: Row) => number | null
    })
  | (ColumnHead & {
      readonly kind: 'timestamp'
      readonly value: (row: Row) => string | null
    })
  | (ColumnHead & {
      readonly kind: 'link'
      readonly value: (row: Row) => string
      readonly href: (row: Row) => string
    })

export type Total<Row> = {
  readonly label: string
  readonly row: Row
}

export type SortableTableProps<Row> = {
  readonly caption: string
  readonly columns: readonly Column<Row>[]
  readonly rows: readonly Row[]
  readonly rowKey: (row: Row) => string
  readonly emptyMessage: string
  readonly total?: Total<Row>
  readonly initialSort?: Sort
  readonly onFollow?: (href: string) => void
}

const missingValue = '–'

const collator = new Intl.Collator('de-DE')

type SortKey = string | number | null

const sortKeyOf = <Row,>(column: Column<Row>, row: Row): SortKey => {
  switch (column.kind) {
    case 'text':
      return column.value(row)
    case 'number':
      return column.value(row)
    case 'timestamp': {
      const timestamp = column.value(row)
      return timestamp === null ? null : instantOf(timestamp)
    }
    case 'link':
      return column.value(row)
  }
}

const compareKeys = (left: SortKey, right: SortKey): number => {
  if (typeof left === 'string' && typeof right === 'string') {
    return collator.compare(left, right)
  }
  if (typeof left === 'number' && typeof right === 'number') {
    return left - right
  }
  return 0
}

const sortedBy = <Row,>(
  rows: readonly Row[],
  column: Column<Row>,
  direction: SortDirection,
): readonly Row[] =>
  [...rows].sort((leftRow, rightRow) => {
    const left = sortKeyOf(column, leftRow)
    const right = sortKeyOf(column, rightRow)
    if (left === null) {
      return right === null ? 0 : 1
    }
    if (right === null) {
      return -1
    }
    return direction === 'ascending'
      ? compareKeys(left, right)
      : compareKeys(right, left)
  })

type Cell = {
  readonly text: string
  readonly tone: ColumnTone
  readonly href: string | null
}

const toneOf = (value: number, columnTone: ColumnTone): ColumnTone => {
  if (value === 0) {
    return 'neutral'
  }
  return value < 0 ? 'debit' : columnTone
}

const cellOf = <Row,>(column: Column<Row>, row: Row): Cell => {
  switch (column.kind) {
    case 'text':
      return { text: column.value(row), tone: 'neutral', href: null }
    case 'number': {
      const value = column.value(row)
      if (value === null) {
        return { text: missingValue, tone: 'neutral', href: null }
      }
      return {
        text: formatGold(value),
        tone: toneOf(value, column.tone),
        href: null,
      }
    }
    case 'timestamp': {
      const timestamp = column.value(row)
      return {
        text: timestamp === null ? missingValue : formatTimestamp(timestamp),
        tone: 'neutral',
        href: null,
      }
    }
    case 'link':
      return {
        text: column.value(row),
        tone: 'neutral',
        href: column.href(row),
      }
  }
}

const totalCellOf = <Row,>(column: Column<Row>, total: Total<Row>): Cell =>
  column.kind === 'number'
    ? cellOf(column, total.row)
    : { text: missingValue, tone: 'neutral', href: null }

const columnOf = <Row,>(
  columns: readonly Column<Row>[],
  columnKey: string,
): Column<Row> => {
  const column = columns.find((candidate) => candidate.key === columnKey)
  if (!column) {
    throw new TypeError(`Unknown sort column: ${columnKey}`)
  }
  return column
}

const nextSort = (current: Sort | null, columnKey: string): Sort => {
  if (current?.columnKey !== columnKey) {
    return { columnKey, direction: 'ascending' }
  }
  return {
    columnKey,
    direction: current.direction === 'ascending' ? 'descending' : 'ascending',
  }
}

const indicatorOf = (sort: Sort | null, columnKey: string): string => {
  if (sort?.columnKey !== columnKey) {
    return ''
  }
  return sort.direction === 'ascending' ? '▲' : '▼'
}

export function SortableTable<Row>({
  caption,
  columns,
  rows,
  rowKey,
  emptyMessage,
  total,
  initialSort,
  onFollow,
}: SortableTableProps<Row>) {
  const [sort, setSort] = useState<Sort | null>(initialSort ?? null)

  const visibleRows = sort
    ? sortedBy(rows, columnOf(columns, sort.columnKey), sort.direction)
    : rows

  return (
    <table className="data-table">
      <caption className="data-table__caption">{caption}</caption>
      <thead>
        <tr>
          {columns.map((column) => (
            <th
              key={column.key}
              scope="col"
              className="data-table__header"
              data-kind={column.kind}
              aria-sort={
                sort?.columnKey === column.key ? sort.direction : 'none'
              }
            >
              <button
                type="button"
                className="data-table__sort"
                onClick={() => {
                  setSort((current) => nextSort(current, column.key))
                }}
              >
                <span data-testid="column-label">{column.header}</span>
                <span className="data-table__indicator" aria-hidden="true">
                  {indicatorOf(sort, column.key)}
                </span>
              </button>
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {visibleRows.length === 0 ? (
          <tr>
            <td className="data-table__empty" colSpan={columns.length}>
              <StatusPanel variant="empty" message={emptyMessage} />
            </td>
          </tr>
        ) : (
          visibleRows.map((row) => (
            <tr
              className="data-table__row"
              key={rowKey(row)}
              data-testid="data-row"
            >
              {columns.map((column) => {
                const cell = cellOf(column, row)
                return (
                  <td
                    key={column.key}
                    className="data-table__cell"
                    data-kind={column.kind}
                    data-tone={cell.tone}
                    data-testid={`cell-${column.key}`}
                  >
                    {cell.href === null ? (
                      cell.text
                    ) : (
                      <Link href={cell.href} onFollow={onFollow}>
                        {cell.text}
                      </Link>
                    )}
                  </td>
                )
              })}
            </tr>
          ))
        )}
      </tbody>
      {total === undefined || visibleRows.length === 0 ? null : (
        <tfoot>
          <tr className="data-table__total" data-testid="total-row">
            {columns.map((column, index) =>
              index === 0 ? (
                <th
                  key={column.key}
                  scope="row"
                  className="data-table__cell"
                  data-kind={column.kind}
                  data-testid={`total-${column.key}`}
                >
                  {total.label}
                </th>
              ) : (
                <td
                  key={column.key}
                  className="data-table__cell"
                  data-kind={column.kind}
                  data-tone={totalCellOf(column, total).tone}
                  data-testid={`total-${column.key}`}
                >
                  {totalCellOf(column, total).text}
                </td>
              ),
            )}
          </tr>
        </tfoot>
      )}
    </table>
  )
}
