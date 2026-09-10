import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import type { Stat } from './StatHeader.tsx'
import { StatHeader } from './StatHeader.tsx'

const BANK: Stat = {
  key: 'bank',
  label: 'Gildenbank',
  value: 119334247,
  positiveTone: 'credit',
  absentNote: 'noch nicht berechnet',
}

const renderStats = (stats: readonly Stat[]) => {
  render(<StatHeader stats={stats} />)
}

describe('StatHeader', () => {
  afterEach(cleanup)

  it('names each figure beside its value', () => {
    renderStats([BANK])

    expect(screen.getByText('Gildenbank')).toBeTruthy()
  })

  it('groups thousands the way the tables do', () => {
    renderStats([BANK])

    expect(screen.getByTestId('stat-bank').textContent).toBe('119.334.247')
  })

  it('shows every figure it is given', () => {
    renderStats([BANK, { ...BANK, key: 'donation', label: 'Gildenspende' }])

    expect(
      screen.getByTestId('stat-header').querySelectorAll('dd'),
    ).toHaveLength(2)
  })

  it('reads a negative figure as a debit', () => {
    renderStats([{ ...BANK, value: -5 }])

    expect(screen.getByTestId('stat-bank').dataset.tone).toBe('debit')
  })

  it('leaves a figure of zero without a tone of its own', () => {
    renderStats([{ ...BANK, value: 0 }])

    expect(screen.getByTestId('stat-bank').dataset.tone).toBe('neutral')
  })

  it('says a figure is not computed yet rather than showing it as zero', () => {
    renderStats([{ ...BANK, value: null }])

    expect(screen.getByTestId('stat-bank').textContent).toBe(
      'noch nicht berechnet',
    )
  })

  it('marks an absent figure so it can be styled apart from a number', () => {
    renderStats([{ ...BANK, value: null }])

    expect(screen.getByTestId('stat-bank').dataset.absent).toBe('true')
  })
})
