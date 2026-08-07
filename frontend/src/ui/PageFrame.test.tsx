import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import { PageFrame } from './PageFrame.tsx'

const navigation = [
  { label: 'Übersicht', href: '/overview', current: false },
  { label: 'Bank', href: '/avatars/Bambor/bank', current: true },
  { label: 'Lager', href: '/avatars/Bambor/storage', current: false },
]

const renderFrame = () =>
  render(
    <PageFrame brand="Gildenbank" navigation={navigation}>
      <p>Der Inhalt der Seite</p>
    </PageFrame>,
  )

describe('PageFrame', () => {
  afterEach(cleanup)

  it('renders the brand it is given', () => {
    renderFrame()

    expect(screen.getByTestId('page-brand').textContent).toBe('Gildenbank')
  })

  it('renders one link per navigation entry', () => {
    renderFrame()

    const labels = screen.getAllByRole('link').map((link) => link.textContent)

    expect(labels).toEqual(['Übersicht', 'Bank', 'Lager'])
  })

  it('points each link at the target of its entry', () => {
    renderFrame()

    const targets = screen
      .getAllByRole('link')
      .map((link) => link.getAttribute('href'))

    expect(targets).toEqual([
      '/overview',
      '/avatars/Bambor/bank',
      '/avatars/Bambor/storage',
    ])
  })

  it('marks only the current entry as the current page', () => {
    renderFrame()

    const marks = screen
      .getAllByRole('link')
      .map((link) => link.getAttribute('aria-current'))

    expect(marks).toEqual([null, 'page', null])
  })

  it('names its navigation landmark', () => {
    renderFrame()

    expect(screen.getByRole('navigation').getAttribute('aria-label')).toBe(
      'Hauptnavigation',
    )
  })

  it('renders its children into the content region', () => {
    renderFrame()

    expect(screen.getByTestId('page-content').textContent).toBe(
      'Der Inhalt der Seite',
    )
  })
})
