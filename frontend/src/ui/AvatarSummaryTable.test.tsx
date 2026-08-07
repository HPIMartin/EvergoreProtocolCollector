import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import type { AvatarSummaryRow } from './AvatarSummaryTable.tsx'
import { AvatarSummaryTable } from './AvatarSummaryTable.tsx'

const rows: readonly AvatarSummaryRow[] = [
  {
    summary: { avatar: 'Calix', withdrawn: 1200, deposited: 3400 },
    bankHref: '/avatars/Calix/bank?token=a-test-token',
    storageHref: '/avatars/Calix/storage?token=a-test-token',
  },
  {
    summary: { avatar: 'Erde-Eibenlanze', withdrawn: 0, deposited: 50 },
    bankHref: '/avatars/Erde-Eibenlanze/bank?token=a-test-token',
    storageHref: '/avatars/Erde-Eibenlanze/storage?token=a-test-token',
  },
]

interface FollowRecorder {
  readonly followed: string[]
  readonly follow: (href: string) => void
}

function recordingFollow(): FollowRecorder {
  const followed: string[] = []

  return {
    followed,
    follow: (href) => {
      followed.push(href)
    },
  }
}

function ignoredFollow(): void {
  return undefined
}

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
    .getAllByTestId('avatar-summary-row')
    .map((row) => textsIn(row.querySelectorAll('td')))
}

function hrefsOf(name: string): (string | null)[] {
  return screen
    .getAllByRole('link', { name })
    .map((link) => link.getAttribute('href'))
}

describe('AvatarSummaryTable', () => {
  afterEach(cleanup)

  it('names its columns in German', () => {
    render(<AvatarSummaryTable rows={rows} onFollow={ignoredFollow} />)

    expect(headerTexts()).toStrictEqual([
      'Avatar',
      'Entnommen',
      'Eingelagert',
      'Bank',
      'Lager',
    ])
  })

  it('shows one row per avatar, in the order it was given', () => {
    render(<AvatarSummaryTable rows={rows} onFollow={ignoredFollow} />)

    expect(rowTexts()).toStrictEqual([
      ['Calix', '1200', '3400', 'Bank', 'Lager'],
      ['Erde-Eibenlanze', '0', '50', 'Bank', 'Lager'],
    ])
  })

  it('links every avatar to its own bank ledger', () => {
    render(<AvatarSummaryTable rows={rows} onFollow={ignoredFollow} />)

    expect(hrefsOf('Bank')).toStrictEqual([
      '/avatars/Calix/bank?token=a-test-token',
      '/avatars/Erde-Eibenlanze/bank?token=a-test-token',
    ])
  })

  it('links every avatar to its own storage ledger', () => {
    render(<AvatarSummaryTable rows={rows} onFollow={ignoredFollow} />)

    expect(hrefsOf('Lager')).toStrictEqual([
      '/avatars/Calix/storage?token=a-test-token',
      '/avatars/Erde-Eibenlanze/storage?token=a-test-token',
    ])
  })

  it('reports the ledger a click asked for', () => {
    const recorder = recordingFollow()
    render(<AvatarSummaryTable rows={rows} onFollow={recorder.follow} />)

    fireEvent.click(screen.getAllByRole('link', { name: 'Lager' })[1])

    expect(recorder.followed).toStrictEqual([
      '/avatars/Erde-Eibenlanze/storage?token=a-test-token',
    ])
  })

  it('shows no row at all for a guild without avatars', () => {
    render(<AvatarSummaryTable rows={[]} onFollow={ignoredFollow} />)

    expect(screen.queryAllByTestId('avatar-summary-row')).toStrictEqual([])
  })
})
