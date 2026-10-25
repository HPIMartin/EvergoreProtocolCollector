import { act, cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import type { ProtocolApi } from '../api'
import { Forbidden } from '../api'

import { AdminView } from './AdminView.tsx'

afterEach(cleanup)

function apiRefusingTheAdminStatus(reason: Error): ProtocolApi {
  const refused = (): Promise<never> => Promise.reject(reason)

  return {
    overview: refused,
    adminStatus: refused,
    bankEntries: refused,
    storageEntries: refused,
  }
}

async function adminViewWith(api: ProtocolApi): Promise<void> {
  render(<AdminView api={api} />)
  await act(async () => undefined)
}

function shownMessages(): string[] {
  return screen
    .queryAllByTestId('status-panel')
    .map((panel) => panel.textContent ?? '')
}

describe('AdminView', () => {
  it('says the page is reserved for the admin when the service refuses the role', async () => {
    await adminViewWith(apiRefusingTheAdminStatus(new Forbidden()))

    const messages = shownMessages()

    expect(messages).toEqual(['Diese Seite ist dem Admin vorbehalten.'])
  })

  it('keeps the heading of the page while it says so', async () => {
    await adminViewWith(apiRefusingTheAdminStatus(new Forbidden()))

    expect(screen.getByTestId('view-title')).toHaveProperty(
      'textContent',
      'Admin',
    )
  })
})
