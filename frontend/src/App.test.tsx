import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import App from './App.tsx'

describe('App', () => {
  afterEach(cleanup)

  it('renders the application title', () => {
    render(<App />)

    expect(screen.getByTestId('app-title').textContent).toBe('Evergore Protocol Collector')
  })
})
