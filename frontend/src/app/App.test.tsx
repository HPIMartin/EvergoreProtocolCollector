import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { StrictMode } from 'react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'

import type { HttpGet } from '../api'

import { App } from './App.tsx'

const TOKEN = 'a-test-token'

const OVERVIEW_BODY = JSON.stringify({
  lastUpdated: '2026-08-05T10:15:00Z',
  page: 0,
  size: 100,
  totalCount: 2,
  items: [
    { avatar: 'Calix', withdrawn: 1200, deposited: 3400 },
    { avatar: 'Erde-Eibenlanze', withdrawn: 0, deposited: 50 },
  ],
})

const BANK_BODY = JSON.stringify({
  page: 0,
  size: 100,
  totalCount: 1,
  items: [
    {
      timestamp: '2026-08-05T10:15:00Z',
      avatar: 'Calix',
      amount: 500,
      transferType: 'DEPOSIT',
    },
  ],
})

const STORAGE_BODY = JSON.stringify({
  page: 0,
  size: 100,
  totalCount: 1,
  items: [
    {
      timestamp: '2026-08-05T10:15:00Z',
      avatar: 'Calix',
      quantity: 3,
      name: 'Erde-Eibenlanze',
      quality: 7,
      transferType: 'WITHDRAWAL',
    },
  ],
})

const EMPTY_LEDGER_BODY = JSON.stringify({
  page: 0,
  size: 100,
  totalCount: 0,
  items: [],
})

interface Answer {
  readonly status: number
  readonly body: string | null
}

interface FakeServer {
  readonly askedFor: string[]
  readonly get: HttpGet
}

function serving(answerFor: (path: string) => Answer): FakeServer {
  const askedFor: string[] = []

  return {
    askedFor,
    get: (url) => {
      askedFor.push(url)
      const answer = answerFor(new URL(url, 'http://localhost').pathname)

      return Promise.resolve(new Response(answer.body, answer))
    },
  }
}

function alwaysServing(status: number, body: string | null): FakeServer {
  return serving(() => ({ status, body }))
}

async function shellAt(address: string, server: FakeServer): Promise<void> {
  window.history.replaceState(null, '', address)
  render(<App get={server.get} />)
  await act(async () => undefined)
}

function textsIn(testId: string): string[] {
  return screen.getAllByTestId(testId).map((row) => row.textContent ?? '')
}

describe('App', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/')
  })

  afterEach(cleanup)

  it('names the application', async () => {
    await shellAt(`/?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(screen.getByTestId('app-title').textContent).toBe(
      'Evergore Protocol Collector',
    )
  })

  it('shows the overview at the address the shell is served from', async () => {
    await shellAt(`/?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(textsIn('avatar-summary-row')).toStrictEqual([
      'Calix12003400BankLager',
      'Erde-Eibenlanze050BankLager',
    ])
  })

  it('carries the token of the deep link into every request', async () => {
    const server = alwaysServing(200, OVERVIEW_BODY)

    await shellAt(`/overview?token=${TOKEN}`, server)

    expect(server.askedFor).toStrictEqual([
      '/api/v1/avatars?token=a-test-token&page=0&size=100',
    ])
  })

  it('says how fresh the numbers are', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(screen.getByTestId('last-updated').textContent).toBe(
      'Stand: 05.08.2026 12:15',
    )
  })

  it('says when no collection run has finished yet', async () => {
    const body = JSON.stringify({
      lastUpdated: null,
      page: 0,
      size: 100,
      totalCount: 0,
      items: [],
    })

    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, body))

    expect(screen.getByTestId('last-updated').textContent).toBe(
      'Stand: noch kein Abgleich gelaufen',
    )
  })

  it('says that no avatar has been collected yet', async () => {
    const body = JSON.stringify({
      lastUpdated: null,
      page: 0,
      size: 100,
      totalCount: 0,
      items: [],
    })

    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, body))

    expect(screen.getByTestId('view-empty').textContent).toBe(
      'Noch kein Avatar erfasst.',
    )
  })

  it('counts the avatars of the window against the whole guild', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(screen.getByTestId('avatar-count').textContent).toBe(
      '2 von 2 Avataren',
    )
  })

  it('shows the bank ledger the path names', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}`,
      alwaysServing(200, BANK_BODY),
    )

    expect(textsIn('bank-entry-row')).toStrictEqual([
      '05.08.2026 12:15Calix500Einlagerung',
    ])
  })

  it('shows the storage ledger the path names', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}`,
      alwaysServing(200, STORAGE_BODY),
    )

    expect(textsIn('storage-entry-row')).toStrictEqual([
      '05.08.2026 12:15Calix3Erde-Eibenlanze7Entnahme',
    ])
  })

  it('asks the bank route of the avatar the path names', async () => {
    const server = alwaysServing(200, BANK_BODY)

    await shellAt(`/avatars/Erde%2FEibe/bank?token=${TOKEN}`, server)

    expect(server.askedFor).toStrictEqual([
      '/api/v1/avatars/Erde%2FEibe/bank?token=a-test-token&page=0&size=100',
    ])
  })

  it('says that a known avatar has no entries in this ledger', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}`,
      alwaysServing(200, EMPTY_LEDGER_BODY),
    )

    expect(screen.getByTestId('view-empty').textContent).toBe(
      'Für Calix ist hier kein Vorgang gespeichert.',
    )
  })

  it('says that an avatar is unknown, which is not the same as an empty ledger', async () => {
    await shellAt(
      `/avatars/Nobody/storage?token=${TOKEN}`,
      alwaysServing(404, null),
    )

    expect(screen.getByTestId('view-unknown-avatar').textContent).toBe(
      'Kein Avatar mit dem Namen Nobody.',
    )
  })

  it('shows a refused token instead of an empty page', async () => {
    await shellAt('/overview?token=a-wrong-token', alwaysServing(401, null))

    expect(screen.getByTestId('view-unauthorized').textContent).toBe(
      'Kein gültiges Token: der Link braucht ein token in der Adresse.',
    )
  })

  it('asks without a token when the address carries none, and says so', async () => {
    const server = alwaysServing(401, null)

    await shellAt('/overview', server)

    expect({
      askedFor: server.askedFor,
      shown: screen.getByTestId('view-unauthorized').textContent,
    }).toStrictEqual({
      askedFor: ['/api/v1/avatars?page=0&size=100'],
      shown: 'Kein gültiges Token: der Link braucht ein token in der Adresse.',
    })
  })

  it('shows a failure of the API with its reason', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(500, null))

    expect(screen.getByTestId('view-failed').textContent).toBe(
      'Fehler: The API answered 500',
    )
  })

  it('is loading before the first answer arrives', () => {
    window.history.replaceState(null, '', `/overview?token=${TOKEN}`)
    const server = serving(() => ({ status: 200, body: OVERVIEW_BODY }))

    render(<App get={server.get} />)

    expect(screen.getByTestId('view-loading').textContent).toBe('Wird geladen…')
  })

  it('has no view for a path it does not know', async () => {
    await shellAt(`/nonsense?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(screen.getByTestId('view-unknown-path').textContent).toBe(
      'Für /nonsense gibt es keine Ansicht.',
    )
  })

  it('follows an avatar link to that avatar ledger without leaving the shell', async () => {
    const server = serving((path) =>
      path === '/api/v1/avatars'
        ? { status: 200, body: OVERVIEW_BODY }
        : { status: 200, body: BANK_BODY },
    )
    await shellAt(`/overview?token=${TOKEN}`, server)

    await act(async () => {
      fireEvent.click(screen.getAllByRole('link', { name: 'Bank' })[0])
    })

    expect({
      shown: textsIn('bank-entry-row'),
      address: window.location.pathname + window.location.search,
      lastAsked: server.askedFor.at(-1),
    }).toStrictEqual({
      shown: ['05.08.2026 12:15Calix500Einlagerung'],
      address: '/avatars/Calix/bank?token=a-test-token',
      lastAsked:
        '/api/v1/avatars/Calix/bank?token=a-test-token&page=0&size=100',
    })
  })

  it('gets back to the overview from a ledger', async () => {
    const server = serving((path) =>
      path === '/api/v1/avatars'
        ? { status: 200, body: OVERVIEW_BODY }
        : { status: 200, body: BANK_BODY },
    )
    await shellAt(`/avatars/Calix/bank?token=${TOKEN}`, server)

    await act(async () => {
      fireEvent.click(screen.getByRole('link', { name: 'Zur Übersicht' }))
    })

    expect({
      shown: textsIn('avatar-summary-row').length,
      address: window.location.pathname + window.location.search,
    }).toStrictEqual({ shown: 2, address: '/overview?token=a-test-token' })
  })

  it('asks again with the token of the address the browser moved to', async () => {
    const server = alwaysServing(200, OVERVIEW_BODY)
    await shellAt(`/overview?token=${TOKEN}`, server)

    await act(async () => {
      window.history.pushState(null, '', '/overview?token=another-token')
      window.dispatchEvent(new PopStateEvent('popstate'))
    })

    expect(server.askedFor).toStrictEqual([
      '/api/v1/avatars?token=a-test-token&page=0&size=100',
      '/api/v1/avatars?token=another-token&page=0&size=100',
    ])
  })

  it('shows the view the browser moves back to in its history', async () => {
    const server = serving((path) =>
      path === '/api/v1/avatars'
        ? { status: 200, body: OVERVIEW_BODY }
        : { status: 200, body: BANK_BODY },
    )
    await shellAt(`/overview?token=${TOKEN}`, server)
    await act(async () => {
      fireEvent.click(screen.getAllByRole('link', { name: 'Bank' })[0])
    })

    await act(async () => {
      window.history.pushState(null, '', `/overview?token=${TOKEN}`)
      window.dispatchEvent(new PopStateEvent('popstate'))
    })

    expect(screen.getByTestId('view-title').textContent).toBe('Übersicht')
  })

  it('shows an avatar whose name has to be escaped in the path', async () => {
    const body = JSON.stringify({
      page: 0,
      size: 100,
      totalCount: 1,
      items: [
        {
          timestamp: '2026-08-05T10:15:00Z',
          avatar: 'Öde/Grüße',
          quantity: 1,
          name: 'Magische Ätherbinde',
          quality: 100,
          transferType: 'DEPOSIT',
        },
      ],
    })
    const server = alwaysServing(200, body)

    await shellAt(
      `/avatars/${encodeURIComponent('Öde/Grüße')}/storage?token=${TOKEN}`,
      server,
    )

    expect({
      askedFor: server.askedFor,
      heading: screen.getByTestId('view-title').textContent,
      rows: textsIn('storage-entry-row'),
    }).toStrictEqual({
      askedFor: [
        '/api/v1/avatars/%C3%96de%2FGr%C3%BC%C3%9Fe/storage?token=a-test-token&page=0&size=100',
      ],
      heading: 'Lager von Öde/Grüße',
      rows: ['05.08.2026 12:15Öde/Grüße1Magische Ätherbinde100Einlagerung'],
    })
  })

  it('shows the data once even though StrictMode mounts the shell twice', async () => {
    const server = alwaysServing(200, OVERVIEW_BODY)
    window.history.replaceState(null, '', `/overview?token=${TOKEN}`)

    render(
      <StrictMode>
        <App get={server.get} />
      </StrictMode>,
    )
    await act(async () => undefined)

    expect({
      rows: textsIn('avatar-summary-row').length,
      counts: screen.getAllByTestId('avatar-count').length,
    }).toStrictEqual({ rows: 2, counts: 1 })
  })

  it('counts the entries of the window against the whole ledger', async () => {
    const body = JSON.stringify({
      page: 0,
      size: 100,
      totalCount: 340,
      items: [
        {
          timestamp: '2026-08-05T10:15:00Z',
          avatar: 'Calix',
          amount: 500,
          transferType: 'DEPOSIT',
        },
      ],
    })

    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}`,
      alwaysServing(200, body),
    )

    expect(screen.getByTestId('entry-count').textContent).toBe(
      '1 von 340 Einträgen',
    )
  })
})
