import { cleanup, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import { UiGallery } from './UiGallery.tsx'

const headersOf = (sectionTestId: string): (string | null)[] =>
  within(screen.getByTestId(sectionTestId))
    .getAllByTestId('column-label')
    .map((header) => header.textContent)

const cellsOf = (sectionTestId: string, columnKey: string): (string | null)[] =>
  within(screen.getByTestId(sectionTestId))
    .getAllByTestId(`cell-${columnKey}`)
    .map((cell) => cell.textContent)

describe('UiGallery', () => {
  afterEach(cleanup)

  it('frames the page with the navigation', () => {
    render(<UiGallery />)

    const labels = within(screen.getByLabelText('Hauptnavigation'))
      .getAllByRole('link')
      .map((link) => link.textContent)

    expect(labels).toEqual(['Übersicht', 'Bank', 'Lager'])
  })

  it('shows the overview with the columns of the guild sheet', () => {
    render(<UiGallery />)

    expect(headersOf('gallery-overview')).toEqual([
      'Avatar',
      'Bank-Einzahlung',
      'Bank-Auszahlung',
      'Einlagerung',
      'Entnahme',
      'Gildenmehrwert',
      'Letzte Lageraktivität',
      'Letzte Bankaktivität',
    ])
  })

  it('sorts the overview by avatar', () => {
    render(<UiGallery />)

    expect(cellsOf('gallery-overview', 'avatar')).toEqual([
      'Aargh',
      'Alessia',
      'Bambor',
      'Evildead',
      'Fugger',
    ])
  })

  it('values the overview in German gold notation', () => {
    render(<UiGallery />)

    expect(cellsOf('gallery-overview', 'guildValue')).toEqual([
      '0',
      '-97.550',
      '1.170',
      '97.965',
      '-1.053.554',
    ])
  })

  it('closes the overview with the guild-wide total row', () => {
    render(<UiGallery />)

    const total = within(screen.getByTestId('gallery-overview')).getByTestId(
      'total-row',
    )

    expect(total.textContent).toBe(
      'Gilde161.565247.0531.722.4102.688.891-1.051.969––',
    )
  })

  it('shows the ledger with its newest entry first', () => {
    render(<UiGallery />)

    expect(cellsOf('gallery-ledger', 'occurredAt')).toEqual([
      '10.07.2022 14:23',
      '10.07.2022 09:41',
      '09.07.2022 22:07',
      '09.07.2022 21:44',
      '08.07.2022 19:12',
    ])
  })

  it('shows a loading, an empty and an error state', () => {
    render(<UiGallery />)

    const variants = within(screen.getByTestId('gallery-states'))
      .getAllByTestId('status-panel')
      .map((panel) => panel.dataset.variant)

    expect(variants).toEqual(['loading', 'empty', 'error'])
  })

  it('shows a table that has no rows', () => {
    render(<UiGallery />)

    const panel = within(screen.getByTestId('gallery-empty')).getByTestId(
      'status-panel',
    )

    expect(panel.textContent).toBe(
      'Für diesen Zeitraum sind keine Bewegungen verzeichnet.',
    )
  })
})
