import {
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import { SortableTable } from './SortableTable.tsx'
import type {
  Column,
  ColumnTone,
  SortableTableProps,
} from './SortableTable.tsx'

type Member = {
  readonly name: string
  readonly deposited: number | null
  readonly lastActivity: string | null
}

const columns: readonly Column<Member>[] = [
  {
    key: 'name',
    header: 'Avatar',
    kind: 'text',
    value: (member) => member.name,
  },
  {
    key: 'deposited',
    header: 'Eingezahlt',
    kind: 'number',
    tone: 'credit',
    value: (member) => member.deposited,
  },
  {
    key: 'lastActivity',
    header: 'Letzte Aktivität',
    kind: 'timestamp',
    value: (member) => member.lastActivity,
  },
]

const members: readonly Member[] = [
  { name: 'Zoe', deposited: 100, lastActivity: '2022-07-10T12:00:00Z' },
  { name: 'Ärger', deposited: 9, lastActivity: '2022-07-10T15:00:00+05:00' },
  { name: 'alessia', deposited: null, lastActivity: null },
  { name: 'Bambor', deposited: 1000, lastActivity: '2022-07-10T14:00:00Z' },
]

const renderTable = (overrides: Partial<SortableTableProps<Member>> = {}) =>
  render(
    <SortableTable
      caption="Mitglieder"
      columns={columns}
      rows={members}
      rowKey={(member) => member.name}
      emptyMessage="Keine Einträge"
      {...overrides}
    />,
  )

const cellsOf = (columnKey: string): (string | null)[] =>
  screen.getAllByTestId(`cell-${columnKey}`).map((cell) => cell.textContent)

const tonesOf = (columnKey: string): (string | undefined)[] =>
  screen.getAllByTestId(`cell-${columnKey}`).map((cell) => cell.dataset.tone)

const headerOf = (name: string): HTMLElement =>
  screen.getByRole('button', { name })

const numberColumnToned = (tone: ColumnTone): readonly Column<Member>[] => [
  {
    key: 'deposited',
    header: 'Eingezahlt',
    kind: 'number',
    tone,
    value: (member) => member.deposited,
  },
]

describe('SortableTable', () => {
  afterEach(cleanup)

  it('names itself by its caption', () => {
    renderTable()

    expect(screen.queryByRole('table', { name: 'Mitglieder' })).not.toBeNull()
  })

  it('renders one header per column', () => {
    renderTable()

    const headers = screen
      .getAllByTestId('column-label')
      .map((header) => header.textContent)

    expect(headers).toEqual(['Avatar', 'Eingezahlt', 'Letzte Aktivität'])
  })

  it('puts a native button in every header, so the keyboard reaches the sort', () => {
    renderTable()

    const controls = screen
      .getAllByRole('columnheader')
      .map((header) => within(header).getByRole('button').tagName)

    expect(controls).toEqual(['BUTTON', 'BUTTON', 'BUTTON'])
  })

  it('renders one row per entry', () => {
    renderTable()

    expect(screen.getAllByTestId('data-row')).toHaveLength(4)
  })

  it('renders the rows in their given order when no sort is asked for', () => {
    renderTable()

    expect(cellsOf('name')).toEqual(['Zoe', 'Ärger', 'alessia', 'Bambor'])
  })
})

describe('SortableTable cells', () => {
  afterEach(cleanup)

  it('renders a gold value in German notation', () => {
    renderTable({
      rows: [{ name: 'Fugger', deposited: 1234567, lastActivity: null }],
    })

    expect(cellsOf('deposited')).toEqual(['1.234.567'])
  })

  it('renders a timestamp as Berlin wall-clock time', () => {
    renderTable({
      rows: [
        {
          name: 'Fugger',
          deposited: null,
          lastActivity: '2022-07-10T12:23:00Z',
        },
      ],
    })

    expect(cellsOf('lastActivity')).toEqual(['10.07.2022 14:23'])
  })

  it.each(['deposited', 'lastActivity'])(
    'renders a placeholder for a missing %s',
    (columnKey) => {
      renderTable({
        rows: [{ name: 'Aargh', deposited: null, lastActivity: null }],
      })

      expect(cellsOf(columnKey)).toEqual(['–'])
    },
  )

  it.each([
    { tone: 'credit', value: 5, expected: 'credit' },
    { tone: 'credit', value: -5, expected: 'debit' },
    { tone: 'credit', value: 0, expected: 'neutral' },
    { tone: 'debit', value: 5, expected: 'debit' },
    { tone: 'debit', value: 0, expected: 'neutral' },
    { tone: 'neutral', value: 5, expected: 'neutral' },
    { tone: 'neutral', value: -5, expected: 'debit' },
  ] as const)(
    'tones $value in a $tone column as $expected',
    ({ tone, value, expected }) => {
      renderTable({
        columns: numberColumnToned(tone),
        rows: [{ name: 'Fugger', deposited: value, lastActivity: null }],
      })

      expect(tonesOf('deposited')).toEqual([expected])
    },
  )

  it('tones a missing value as neutral', () => {
    renderTable({
      columns: numberColumnToned('credit'),
      rows: [{ name: 'Aargh', deposited: null, lastActivity: null }],
    })

    expect(tonesOf('deposited')).toEqual(['neutral'])
  })
})

describe('SortableTable ordering', () => {
  afterEach(cleanup)

  it.each([
    {
      columnKey: 'name',
      direction: 'ascending',
      expected: ['alessia', 'Ärger', 'Bambor', 'Zoe'],
    },
    {
      columnKey: 'name',
      direction: 'descending',
      expected: ['Zoe', 'Bambor', 'Ärger', 'alessia'],
    },
    {
      columnKey: 'deposited',
      direction: 'ascending',
      expected: ['Ärger', 'Zoe', 'Bambor', 'alessia'],
    },
    {
      columnKey: 'deposited',
      direction: 'descending',
      expected: ['Bambor', 'Zoe', 'Ärger', 'alessia'],
    },
    {
      columnKey: 'lastActivity',
      direction: 'ascending',
      expected: ['Ärger', 'Zoe', 'Bambor', 'alessia'],
    },
    {
      columnKey: 'lastActivity',
      direction: 'descending',
      expected: ['Bambor', 'Zoe', 'Ärger', 'alessia'],
    },
  ] as const)(
    'orders $columnKey $direction',
    ({ columnKey, direction, expected }) => {
      renderTable({ initialSort: { columnKey, direction } })

      expect(cellsOf('name')).toEqual(expected)
    },
  )

  it('keeps the given order of rows that compare equal', () => {
    const tied: readonly Member[] = [
      { name: 'Zoe', deposited: 500, lastActivity: null },
      { name: 'Ärger', deposited: 500, lastActivity: null },
      { name: 'Bambor', deposited: 500, lastActivity: null },
    ]

    renderTable({
      rows: tied,
      initialSort: { columnKey: 'deposited', direction: 'ascending' },
    })

    expect(cellsOf('name')).toEqual(['Zoe', 'Ärger', 'Bambor'])
  })

  it('rejects a sort on a column it does not have', () => {
    const rendering = () =>
      renderTable({
        initialSort: { columnKey: 'gold', direction: 'ascending' },
      })

    expect(rendering).toThrowError('Unknown sort column: gold')
  })
})

describe('SortableTable sorting by click', () => {
  afterEach(cleanup)

  it('sorts ascending when a header is clicked', () => {
    renderTable()

    fireEvent.click(headerOf('Eingezahlt'))

    expect(cellsOf('name')).toEqual(['Ärger', 'Zoe', 'Bambor', 'alessia'])
  })

  it('reverses the order when the same header is clicked again', () => {
    renderTable()

    fireEvent.click(headerOf('Eingezahlt'))
    fireEvent.click(headerOf('Eingezahlt'))

    expect(cellsOf('name')).toEqual(['Bambor', 'Zoe', 'Ärger', 'alessia'])
  })

  it('starts ascending when another header is clicked', () => {
    renderTable({ initialSort: { columnKey: 'name', direction: 'descending' } })

    fireEvent.click(headerOf('Eingezahlt'))

    expect(cellsOf('name')).toEqual(['Ärger', 'Zoe', 'Bambor', 'alessia'])
  })

  it('marks the sorted column and leaves the others unsorted', () => {
    renderTable()

    fireEvent.click(headerOf('Letzte Aktivität'))

    const marks = screen
      .getAllByRole('columnheader')
      .map((header) => header.getAttribute('aria-sort'))

    expect(marks).toEqual(['none', 'none', 'ascending'])
  })

  it('marks the reversed column as descending', () => {
    renderTable()

    fireEvent.click(headerOf('Avatar'))
    fireEvent.click(headerOf('Avatar'))

    const marks = screen
      .getAllByRole('columnheader')
      .map((header) => header.getAttribute('aria-sort'))

    expect(marks).toEqual(['descending', 'none', 'none'])
  })
})

describe('SortableTable without rows', () => {
  afterEach(cleanup)

  it('shows the empty message it is given', () => {
    renderTable({ rows: [] })

    expect(screen.getByTestId('status-panel').textContent).toBe(
      'Keine Einträge',
    )
  })

  it('renders no data row', () => {
    renderTable({ rows: [] })

    expect(screen.queryAllByTestId('data-row')).toEqual([])
  })
})
