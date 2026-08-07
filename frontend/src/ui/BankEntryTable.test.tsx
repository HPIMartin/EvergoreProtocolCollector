import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import type { BankEntry } from '../domain'

import { BankEntryTable } from './BankEntryTable.tsx'

const entries: readonly BankEntry[] = [
  {
    timestamp: new Date('2026-08-05T10:15:00Z'),
    avatar: 'Calix',
    amount: 500,
    transferType: 'DEPOSIT',
  },
  {
    timestamp: new Date('2026-01-05T10:15:00Z'),
    avatar: 'Calix',
    amount: 120,
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
    .getAllByTestId('bank-entry-row')
    .map((row) => textsIn(row.querySelectorAll('td')))
}

describe('BankEntryTable', () => {
  afterEach(cleanup)

  it('names its columns in German', () => {
    render(<BankEntryTable entries={entries} />)

    expect(headerTexts()).toStrictEqual([
      'Zeitpunkt',
      'Avatar',
      'Betrag',
      'Vorgang',
    ])
  })

  it('shows every entry with a local timestamp and a German transfer type', () => {
    render(<BankEntryTable entries={entries} />)

    expect(rowTexts()).toStrictEqual([
      ['05.08.2026 12:15', 'Calix', '500', 'Einlagerung'],
      ['05.01.2026 11:15', 'Calix', '120', 'Entnahme'],
    ])
  })

  it('shows no row at all for an empty ledger', () => {
    render(<BankEntryTable entries={[]} />)

    expect(screen.queryAllByTestId('bank-entry-row')).toStrictEqual([])
  })
})
