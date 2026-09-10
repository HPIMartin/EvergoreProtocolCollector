import {
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import { SortableTable } from './SortableTable.tsx'
import type { Column, SortableTableProps } from './SortableTable.tsx'
import type { Tone } from './tone.ts'

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

const notedColumns: readonly Column<Member>[] = [
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
    tone: 'neutral',
    value: (member) => member.deposited,
    missingNote: 'Kein Wert: noch nicht berechnet.',
  },
]

const memberWithoutAValue: Member = {
  name: 'Nix',
  deposited: null,
  lastActivity: null,
}

const numberColumnToned = (tone: Tone): readonly Column<Member>[] => [
  {
    key: 'deposited',
    header: 'Eingezahlt',
    kind: 'number',
    tone,
    value: (member) => member.deposited,
  },
]

describe('SortableTable', () => {
  it('explains a missing figure in a noted column instead of leaving a bare dash', () => {
    renderTable({ columns: notedColumns, rows: [memberWithoutAValue] })

    expect(screen.getByTestId('cell-mark').textContent).toContain(
      'Kein Wert: noch nicht berechnet.',
    )
  })

  it('carries no note on a figure of zero, which is a value it can answer', () => {
    renderTable({
      columns: notedColumns,
      rows: [{ ...memberWithoutAValue, deposited: 0 }],
    })

    expect(screen.queryByTestId('cell-mark')).toBeNull()
  })

  it('carries no note on a column that declares none', () => {
    renderTable({ rows: [memberWithoutAValue] })

    expect(screen.queryByTestId('cell-mark')).toBeNull()
  })

  it('notes every row that cannot answer, not only the first', () => {
    renderTable({
      columns: notedColumns,
      rows: [memberWithoutAValue, { ...memberWithoutAValue, name: 'Nix2' }],
    })

    expect(screen.getAllByTestId('cell-mark')).toHaveLength(2)
  })

  it('explains a total whose noted figure is missing, like the rows above it', () => {
    renderTable({
      columns: notedColumns,
      rows: [memberWithoutAValue],
      total: { label: 'Gilde', row: memberWithoutAValue },
    })

    expect(screen.getByTestId('total-cell-mark').textContent).toContain(
      'Kein Wert: noch nicht berechnet.',
    )
  })

  it('leaves a total that can answer its noted figure unmarked', () => {
    renderTable({
      columns: notedColumns,
      rows: [memberWithoutAValue],
      total: { label: 'Gilde', row: { ...memberWithoutAValue, deposited: 7 } },
    })

    expect(screen.queryByTestId('total-cell-mark')).toBeNull()
  })

  it('marks a stale row at its name and its missing figure in its own cell', () => {
    renderTable({
      columns: notedColumns,
      rows: [memberWithoutAValue],
      mark: () => 'Veraltete Zahlen.',
    })

    expect([
      screen.getByTestId('row-mark').textContent,
      screen.getByTestId('cell-mark').textContent,
    ]).toStrictEqual([
      '!Veraltete Zahlen.',
      '!Kein Wert: noch nicht berechnet.',
    ])
  })

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

describe('SortableTable timestamp links', () => {
  afterEach(cleanup)

  const linkedTimestamps: readonly Column<Member>[] = [
    {
      key: 'lastActivity',
      header: 'Letzte Aktivität',
      kind: 'timestamp',
      value: (member) => member.lastActivity,
      href: (member) => `/avatars/${member.name}/storage`,
    },
  ]

  it('renders a timestamp as a link to the ledger behind it', () => {
    renderTable({ columns: linkedTimestamps })

    const links = screen
      .getAllByRole('link')
      .map(
        (link) =>
          `${link.textContent ?? ''} ${link.getAttribute('href') ?? ''}`,
      )

    expect(links).toStrictEqual([
      '10.07.2022 14:00 /avatars/Zoe/storage',
      '10.07.2022 12:00 /avatars/Ärger/storage',
      '10.07.2022 16:00 /avatars/Bambor/storage',
    ])
  })

  it('links no missing timestamp, because nothing happened there', () => {
    renderTable({
      columns: linkedTimestamps,
      rows: [{ name: 'alessia', deposited: null, lastActivity: null }],
    })

    expect({
      links: screen.queryAllByRole('link').length,
      shown: screen.getByTestId('cell-lastActivity').textContent,
    }).toStrictEqual({ links: 0, shown: '–' })
  })

  it('still sorts a linked timestamp column by its instant', () => {
    renderTable({ columns: linkedTimestamps })

    fireEvent.click(headerOf('Letzte Aktivität'))

    expect(cellsOf('lastActivity')).toStrictEqual([
      '10.07.2022 12:00',
      '10.07.2022 14:00',
      '10.07.2022 16:00',
      '–',
    ])
  })
})

describe('SortableTable total row', () => {
  afterEach(cleanup)

  const guild: Member = {
    name: 'Gilde',
    deposited: 1109,
    lastActivity: null,
  }

  it('renders no total row when none is given', () => {
    renderTable()

    expect(screen.queryByTestId('total-row')).toBeNull()
  })

  it('renders no total row while there are no rows to total', () => {
    renderTable({ rows: [], total: { label: 'Gilde', row: guild } })

    expect({
      total: screen.queryByTestId('total-row'),
      empty: screen.getByTestId('status-panel').textContent,
    }).toStrictEqual({ total: null, empty: 'Keine Einträge' })
  })

  it('labels the total row in its first column, whatever kind that column is', () => {
    renderTable({
      columns: [
        {
          key: 'deposited',
          header: 'Eingezahlt',
          kind: 'number',
          tone: 'credit',
          value: (member) => member.deposited,
        },
        {
          key: 'name',
          header: 'Avatar',
          kind: 'text',
          value: (member) => member.name,
        },
      ],
      total: { label: 'Gilde', row: { ...guild, deposited: 999 } },
    })

    expect(screen.getByTestId('total-deposited').textContent).toBe('Gilde')
  })

  it("marks the label of the total row as that row's header", () => {
    renderTable({ total: { label: 'Gilde', row: guild } })

    expect(screen.getByTestId('total-name').getAttribute('scope')).toBe('row')
  })

  it('renders the given total below the rows, formatted like a cell', () => {
    renderTable({
      total: { label: 'Gilde', row: { ...guild, deposited: 1234567 } },
    })

    expect(screen.getByTestId('total-row').textContent).toBe('Gilde1.234.567–')
  })

  it('leaves the total row out of the sorting', () => {
    renderTable({ total: { label: 'Gilde', row: guild } })

    fireEvent.click(headerOf('Avatar'))

    expect({
      rows: cellsOf('name'),
      total: screen.getByTestId('total-name').textContent,
    }).toStrictEqual({
      rows: ['alessia', 'Ärger', 'Bambor', 'Zoe'],
      total: 'Gilde',
    })
  })

  it('tones the total of a number column like the column, and a negative one as a debit', () => {
    renderTable({ total: { label: 'Gilde', row: { ...guild, deposited: -5 } } })

    expect(screen.getByTestId('total-deposited').dataset.tone).toBe('debit')
  })

  it('shows no link in the total row, because the guild has no ledger of its own', () => {
    renderTable({
      columns: [
        {
          key: 'name',
          header: 'Avatar',
          kind: 'link',
          value: (member) => member.name,
          href: (member) => `/avatars/${member.name}/bank`,
        },
      ],
      total: { label: 'Gilde', row: guild },
    })

    expect({
      links: screen.getAllByRole('link').length,
      total: screen.getByTestId('total-name').textContent,
    }).toStrictEqual({ links: 4, total: 'Gilde' })
  })
})

describe('SortableTable link columns', () => {
  afterEach(cleanup)

  const linkColumns: readonly Column<Member>[] = [
    {
      key: 'name',
      header: 'Avatar',
      kind: 'link',
      value: (member) => member.name,
      href: (member) => `/avatars/${encodeURIComponent(member.name)}/bank`,
    },
  ]

  it('renders a link column as a link to the target of its row', () => {
    renderTable({ columns: linkColumns })

    const hrefs = screen
      .getAllByRole('link')
      .map((link) => link.getAttribute('href'))

    expect(hrefs).toStrictEqual([
      '/avatars/Zoe/bank',
      '/avatars/%C3%84rger/bank',
      '/avatars/alessia/bank',
      '/avatars/Bambor/bank',
    ])
  })

  it('reports a plain click on a link instead of leaving the page', () => {
    const followed: string[] = []
    renderTable({
      columns: linkColumns,
      onFollow: (href) => {
        followed.push(href)
      },
    })

    const notCancelled = fireEvent.click(screen.getAllByRole('link')[3])

    expect({ notCancelled, followed }).toStrictEqual({
      notCancelled: false,
      followed: ['/avatars/Bambor/bank'],
    })
  })

  it('leaves a click to the browser when nobody listens', () => {
    renderTable({ columns: linkColumns })

    const notCancelled = fireEvent.click(screen.getAllByRole('link')[0])

    expect(notCancelled).toBe(true)
  })

  it('sorts a link column by the text it shows', () => {
    renderTable({ columns: linkColumns })

    fireEvent.click(headerOf('Avatar'))

    expect(cellsOf('name')).toStrictEqual(['alessia', 'Ärger', 'Bambor', 'Zoe'])
  })
})

describe('SortableTable row marks', () => {
  afterEach(cleanup)

  const staleNote =
    'Veraltete Zahlen. Letzte erfolgreiche Aktualisierung vom 08.09.2026 03:12.'
  const markOfStaleMembers = (member: Member) =>
    member.name === 'Ärger' ? staleNote : null

  it('marks only the rows the mark answers a text for', () => {
    renderTable({ mark: markOfStaleMembers })

    expect(
      screen.getAllByTestId('data-row').map((row) => row.dataset.stale),
    ).toEqual([undefined, 'true', undefined, undefined])
  })

  it('leaves every row unmarked when no mark is given', () => {
    renderTable()

    expect(screen.queryByTestId('row-mark')).toBeNull()
    expect(
      screen.getAllByTestId('data-row').map((row) => row.dataset.stale),
    ).toEqual([undefined, undefined, undefined, undefined])
  })

  it('reads the mark before the text the first cell already shows', () => {
    renderTable({ mark: markOfStaleMembers })

    const marked = screen
      .getAllByTestId('cell-name')
      .find((cell) => cell.textContent?.includes('Ärger'))

    expect(marked?.textContent).toBe(`!${staleNote}Ärger`)
  })

  it('puts the mark ahead of the element a first cell wraps its value in', () => {
    renderTable({
      columns: [
        {
          key: 'name',
          header: 'Avatar',
          kind: 'link',
          value: (member: Member) => member.name,
          href: (member: Member) => `/avatars/${member.name}`,
        },
      ],
      mark: markOfStaleMembers,
    })

    const marked = screen
      .getAllByTestId('cell-name')
      .find((cell) => cell.textContent?.includes('Ärger'))

    expect(
      Array.from(marked?.children ?? []).map(
        (child) => child.getAttribute('data-testid') ?? child.tagName,
      ),
    ).toEqual(['row-mark', 'A'])
  })

  it('marks every stale row, not only the first of them', () => {
    renderTable({
      mark: (member: Member) =>
        member.name === 'Ärger' || member.name === 'Zoe' ? staleNote : null,
    })

    expect(screen.getAllByTestId('row-mark')).toHaveLength(2)
    expect(
      screen.getAllByTestId('data-row').map((row) => row.dataset.stale),
    ).toEqual(['true', 'true', undefined, undefined])
  })

  it('treats a blank mark as no mark rather than an empty note', () => {
    renderTable({ mark: () => '   ' })

    expect(screen.queryByTestId('row-mark')).toBeNull()
    expect(
      screen.getAllByTestId('data-row').map((row) => row.dataset.stale),
    ).toEqual([undefined, undefined, undefined, undefined])
  })

  it('treats a blank total mark as no mark either', () => {
    renderTable({ total: { label: 'Gilde', row: members[0], mark: '' } })

    expect(screen.queryByTestId('total-mark')).toBeNull()
    expect(screen.getByTestId('total-row').dataset.stale).toBeUndefined()
  })

  it('keeps the mark text in the document rather than behind the hover', () => {
    renderTable({ mark: markOfStaleMembers })

    expect(screen.getByTestId('row-mark').textContent).toContain(staleNote)
  })

  it('reaches the mark with the keyboard and announces it as a note', () => {
    renderTable({ mark: markOfStaleMembers })

    const mark = screen.getByTestId('row-mark')

    expect(mark.getAttribute('tabindex')).toBe('0')
    expect(mark.getAttribute('role')).toBe('note')
  })

  it('marks the total row with a text of its own', () => {
    renderTable({
      total: {
        label: 'Gilde',
        row: members[0],
        mark: 'Enthält veraltete Summen.',
      },
    })

    expect(screen.getByTestId('total-row').dataset.stale).toBe('true')
    expect(screen.getByTestId('total-mark').textContent).toContain(
      'Enthält veraltete Summen.',
    )
  })

  it('leaves the total row unmarked when it carries no mark', () => {
    renderTable({ total: { label: 'Gilde', row: members[0] } })

    expect(screen.getByTestId('total-row').dataset.stale).toBeUndefined()
    expect(screen.queryByTestId('total-mark')).toBeNull()
  })
})
