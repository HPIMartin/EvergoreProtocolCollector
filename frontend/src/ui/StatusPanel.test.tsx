import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import { StatusPanel } from './StatusPanel.tsx'

describe('StatusPanel', () => {
  afterEach(cleanup)

  it('renders the message it is given', () => {
    render(<StatusPanel variant="loading" message="Lade Einträge" />)

    expect(screen.getByTestId('status-panel').textContent).toBe('Lade Einträge')
  })

  it.each([
    { variant: 'loading', role: 'status' },
    { variant: 'empty', role: 'status' },
    { variant: 'error', role: 'alert' },
  ] as const)('announces the $variant state as $role', ({ variant, role }) => {
    render(<StatusPanel variant={variant} message="Text" />)

    expect(screen.getByTestId('status-panel').getAttribute('role')).toBe(role)
  })

  it.each(['loading', 'empty', 'error'] as const)(
    'exposes %s as the variant it is styled by',
    (variant) => {
      render(<StatusPanel variant={variant} message="Text" />)

      expect(screen.getByTestId('status-panel').dataset.variant).toBe(variant)
    },
  )
})
