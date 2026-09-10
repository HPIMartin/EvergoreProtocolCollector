import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { OptionSwitch } from './OptionSwitch.tsx'

const OPTIONS = [
  { value: 'contribution', label: 'Nach Abzügen' },
  { value: 'balance', label: 'Vor Abzügen' },
] as const

const renderSwitch = (
  selected: 'contribution' | 'balance',
  onSelect: (value: 'contribution' | 'balance') => void = () => {},
) => {
  render(
    <OptionSwitch
      legend="Letzte Spalte"
      name="figure"
      onSelect={onSelect}
      options={OPTIONS}
      selected={selected}
    />,
  )
}

afterEach(cleanup)

describe('OptionSwitch', () => {
  it('offers every option it is given', () => {
    renderSwitch('contribution')

    expect(screen.getAllByRole('radio')).toHaveLength(2)
  })

  it('names what the switch is about', () => {
    renderSwitch('contribution')

    expect(screen.getByText('Letzte Spalte')).toBeTruthy()
  })

  it('checks the selected option', () => {
    renderSwitch('balance')

    const selected = screen.getByLabelText('Vor Abzügen') as HTMLInputElement

    expect(selected.checked).toBe(true)
  })

  it('leaves the other option unchecked', () => {
    renderSwitch('balance')

    expect(
      (screen.getByLabelText('Nach Abzügen') as HTMLInputElement).checked,
    ).toBe(false)
  })

  it('marks the selected option so the stylesheet can reach it', () => {
    renderSwitch('balance')

    const marked = screen
      .getAllByRole('radio')
      .map((radio) => radio.closest('label')?.dataset.selected)

    expect(marked).toStrictEqual([undefined, 'true'])
  })

  it('reports the option a reader picks', () => {
    const picked = vi.fn()
    renderSwitch('contribution', picked)

    fireEvent.click(screen.getByLabelText('Vor Abzügen'))

    expect(picked).toHaveBeenCalledWith('balance')
  })

  it('does not report the option that is already selected as a change', () => {
    const picked = vi.fn()
    renderSwitch('contribution', picked)

    fireEvent.click(screen.getByLabelText('Nach Abzügen'))

    expect(picked).not.toHaveBeenCalled()
  })
})
