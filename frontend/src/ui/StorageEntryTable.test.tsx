import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import type { StorageEntry } from '../domain'

import { StorageEntryTable } from './StorageEntryTable.tsx'

const entries: readonly StorageEntry[] = [
  {
    timestamp: new Date('2026-08-05T10:15:00Z'),
    avatar: 'Calix',
    quantity: 3,
    name: 'Erde-Eibenlanze',
    quality: 7,
    transferType: 'WITHDRAWAL',
  },
]

function textsIn(cells: NodeListOf<HTMLElement>): string[] {
  return Array.from(cells).map((cell) => cell.textContent ?? '')
}

function headerTexts(): string[] {
  return screen
    .getAllByRole('columnheader')
    .map((header) => header.textContent ?? '')
}

function rowTexts(): string[][] {
  return screen
    .getAllByTestId('storage-entry-row')
    .map((row) => textsIn(row.querySelectorAll('td')))
}

describe('StorageEntryTable', () => {
  afterEach(cleanup)

  it('names its columns in German', () => {
    render(<StorageEntryTable entries={entries} />)

    expect(headerTexts()).toStrictEqual([
      'Zeitpunkt',
      'Avatar',
      'Menge',
      'Gegenstand',
      'Qualität',
      'Vorgang',
    ])
  })

  it('shows the item, its quantity and its quality of every entry', () => {
    render(<StorageEntryTable entries={entries} />)

    expect(rowTexts()).toStrictEqual([
      ['05.08.2026 12:15', 'Calix', '3', 'Erde-Eibenlanze', '7', 'Entnahme'],
    ])
  })

  it('shows no row at all for an empty ledger', () => {
    render(<StorageEntryTable entries={[]} />)

    expect(screen.queryAllByTestId('storage-entry-row')).toStrictEqual([])
  })
})
