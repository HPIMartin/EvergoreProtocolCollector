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

function servingByUrl(answerFor: (url: string) => Answer): FakeServer {
  const askedFor: string[] = []

  return {
    askedFor,
    get: (url) => {
      askedFor.push(url)
      const answer = answerFor(url)

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

function rowTexts(): string[] {
  return screen.getAllByTestId('data-row').map((row) => row.textContent ?? '')
}

function headerTexts(): string[] {
  return screen
    .getAllByTestId('column-label')
    .map((header) => header.textContent ?? '')
}

function tonesOf(columnKey: string): (string | undefined)[] {
  return screen
    .getAllByTestId(`cell-${columnKey}`)
    .map((cell) => cell.dataset.tone)
}

function shownStatus(): string {
  return screen.getByTestId('status-panel').textContent ?? ''
}

describe('App', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/')
  })

  afterEach(cleanup)

  it('names the application', async () => {
    await shellAt(`/?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(screen.getByTestId('page-brand').textContent).toBe(
      'Evergore Protocol Collector',
    )
  })

  it('shows the overview at the address the shell is served from', async () => {
    await shellAt(`/?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(rowTexts()).toStrictEqual([
      'Calix1.2003.400öffnen',
      'Erde-Eibenlanze050öffnen',
    ])
  })

  it('names the overview columns in German', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(headerTexts()).toStrictEqual([
      'Avatar',
      'Entnommen',
      'Eingelagert',
      'Lager',
    ])
  })

  it('names the bank ledger columns in German', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}`,
      alwaysServing(200, BANK_BODY),
    )

    expect(headerTexts()).toStrictEqual([
      'Zeitpunkt',
      'Avatar',
      'Betrag',
      'Vorgang',
    ])
  })

  it('names the storage ledger columns in German', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}`,
      alwaysServing(200, STORAGE_BODY),
    )

    expect(headerTexts()).toStrictEqual([
      'Zeitpunkt',
      'Avatar',
      'Menge',
      'Gegenstand',
      'Qualität',
      'Vorgang',
    ])
  })

  it('tells what an avatar took out from what it put in', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect({
      withdrawn: tonesOf('withdrawn'),
      deposited: tonesOf('deposited'),
    }).toStrictEqual({
      withdrawn: ['debit', 'neutral'],
      deposited: ['credit', 'credit'],
    })
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

    expect(shownStatus()).toBe('Noch kein Avatar erfasst.')
  })

  it('counts the avatars of the window against the whole guild', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(
      screen.queryByRole('table', { name: '2 von 2 Avataren' }),
    ).not.toBeNull()
  })

  it('shows the bank ledger the path names', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}`,
      alwaysServing(200, BANK_BODY),
    )

    expect(rowTexts()).toStrictEqual(['05.08.2026 12:15Calix500Einlagerung'])
  })

  it('shows the storage ledger the path names', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}`,
      alwaysServing(200, STORAGE_BODY),
    )

    expect(rowTexts()).toStrictEqual([
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

    expect(shownStatus()).toBe('Für Calix ist hier kein Vorgang gespeichert.')
  })

  it('says that an avatar is unknown, which is not the same as an empty ledger', async () => {
    await shellAt(
      `/avatars/Nobody/storage?token=${TOKEN}`,
      alwaysServing(404, null),
    )

    expect(shownStatus()).toBe('Kein Avatar mit dem Namen Nobody.')
  })

  it('shows a refused token instead of an empty page', async () => {
    await shellAt('/overview?token=a-wrong-token', alwaysServing(401, null))

    expect(shownStatus()).toBe(
      'Kein gültiges Token: der Link braucht ein token in der Adresse.',
    )
  })

  it('asks without a token when the address carries none, and says so', async () => {
    const server = alwaysServing(401, null)

    await shellAt('/overview', server)

    expect({ askedFor: server.askedFor, shown: shownStatus() }).toStrictEqual({
      askedFor: ['/api/v1/avatars?page=0&size=100'],
      shown: 'Kein gültiges Token: der Link braucht ein token in der Adresse.',
    })
  })

  it('shows a failure of the API with its reason', async () => {
    await shellAt(`/overview?token=${TOKEN}`, alwaysServing(500, null))

    expect(shownStatus()).toBe('Fehler: The API answered 500')
  })

  it('is loading before the first answer arrives', () => {
    window.history.replaceState(null, '', `/overview?token=${TOKEN}`)
    const server = serving(() => ({ status: 200, body: OVERVIEW_BODY }))

    render(<App get={server.get} />)

    expect(shownStatus()).toBe('Wird geladen…')
  })

  it('has no view for a path it does not know', async () => {
    await shellAt(`/nonsense?token=${TOKEN}`, alwaysServing(200, OVERVIEW_BODY))

    expect(shownStatus()).toBe('Für /nonsense gibt es keine Ansicht.')
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
      fireEvent.click(screen.getAllByRole('link', { name: 'Calix' })[0])
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
      rows: rowTexts(),
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
      rows: rowTexts().length,
      tables: screen.getAllByRole('table').length,
    }).toStrictEqual({ rows: 2, tables: 1 })
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

    expect(
      screen.queryByRole('table', { name: '1 von 340 Einträgen' }),
    ).not.toBeNull()
  })

  it('offers the next page when more entries follow, but no way back on the first page', async () => {
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

    expect({
      previous: screen.queryByTestId('pagination-previous'),
      next: screen.getByTestId('pagination-next').getAttribute('href'),
    }).toStrictEqual({
      previous: null,
      next: '/avatars/Calix/bank?token=a-test-token&page=1',
    })
  })

  it('offers no next page once the last page is reached', async () => {
    const body = JSON.stringify({
      page: 3,
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
      `/avatars/Calix/bank?token=${TOKEN}&page=3`,
      alwaysServing(200, body),
    )

    expect({
      previous: screen.getByTestId('pagination-previous').getAttribute('href'),
      next: screen.queryByTestId('pagination-next'),
    }).toStrictEqual({
      previous: '/avatars/Calix/bank?token=a-test-token&page=2',
      next: null,
    })
  })

  it('offers no next page when the last page exactly fills the count', async () => {
    const body = JSON.stringify({
      page: 3,
      size: 100,
      totalCount: 400,
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
      `/avatars/Calix/bank?token=${TOKEN}&page=3`,
      alwaysServing(200, body),
    )

    expect(screen.queryByTestId('pagination-next')).toBeNull()
  })

  it('follows the next-page link to a bookmarkable address and re-fetches', async () => {
    const server = servingByUrl((url) => {
      const body = url.includes('page=1')
        ? JSON.stringify({
            page: 1,
            size: 100,
            totalCount: 340,
            items: [
              {
                timestamp: '2026-08-05T10:15:00Z',
                avatar: 'Calix',
                amount: 700,
                transferType: 'WITHDRAWAL',
              },
            ],
          })
        : JSON.stringify({
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

      return { status: 200, body }
    })

    await shellAt(`/avatars/Calix/bank?token=${TOKEN}`, server)

    await act(async () => {
      fireEvent.click(screen.getByTestId('pagination-next'))
    })

    expect({
      shown: rowTexts(),
      address: window.location.pathname + window.location.search,
      lastAsked: server.askedFor.at(-1),
    }).toStrictEqual({
      shown: ['05.08.2026 12:15Calix700Entnahme'],
      address: '/avatars/Calix/bank?token=a-test-token&page=1',
      lastAsked:
        '/api/v1/avatars/Calix/bank?token=a-test-token&page=1&size=100',
    })
  })

  it('follows an avatar link to that avatar ledger without leaving the shell', async () => {
    const server = serving((path) =>
      path === '/api/v1/avatars'
        ? { status: 200, body: OVERVIEW_BODY }
        : { status: 200, body: BANK_BODY },
    )
    await shellAt(`/overview?token=${TOKEN}`, server)

    await act(async () => {
      fireEvent.click(screen.getAllByRole('link', { name: 'Calix' })[0])
    })

    expect({
      shown: rowTexts(),
      address: window.location.pathname + window.location.search,
      lastAsked: server.askedFor.at(-1),
    }).toStrictEqual({
      shown: ['05.08.2026 12:15Calix500Einlagerung'],
      address: '/avatars/Calix/bank?token=a-test-token',
      lastAsked:
        '/api/v1/avatars/Calix/bank?token=a-test-token&page=0&size=100',
    })
  })

  it("follows the storage link of the overview to that avatar's storage ledger", async () => {
    const server = serving((path) =>
      path === '/api/v1/avatars'
        ? { status: 200, body: OVERVIEW_BODY }
        : { status: 200, body: STORAGE_BODY },
    )
    await shellAt(`/overview?token=${TOKEN}`, server)

    await act(async () => {
      fireEvent.click(screen.getAllByRole('link', { name: 'öffnen' })[0])
    })

    expect({
      shown: rowTexts(),
      address: window.location.pathname + window.location.search,
      lastAsked: server.askedFor.at(-1),
    }).toStrictEqual({
      shown: ['05.08.2026 12:15Calix3Erde-Eibenlanze7Entnahme'],
      address: '/avatars/Calix/storage?token=a-test-token',
      lastAsked:
        '/api/v1/avatars/Calix/storage?token=a-test-token&page=0&size=100',
    })
  })

  it('marks only the ledger it shows as the current page in the navigation', async () => {
    await shellAt(
      `/avatars/Calix/storage?token=${TOKEN}`,
      alwaysServing(200, STORAGE_BODY),
    )

    const marked = screen.getAllByRole('link').map((link) => ({
      name: link.textContent,
      current: link.getAttribute('aria-current'),
    }))

    expect(marked).toStrictEqual([
      { name: 'Übersicht', current: null },
      { name: 'Bank', current: null },
      { name: 'Lager', current: 'page' },
    ])
  })

  it('gets back to the overview from a ledger', async () => {
    const server = serving((path) =>
      path === '/api/v1/avatars'
        ? { status: 200, body: OVERVIEW_BODY }
        : { status: 200, body: BANK_BODY },
    )
    await shellAt(`/avatars/Calix/bank?token=${TOKEN}`, server)

    await act(async () => {
      fireEvent.click(screen.getByRole('link', { name: 'Übersicht' }))
    })

    expect({
      rows: rowTexts().length,
      address: window.location.pathname + window.location.search,
    }).toStrictEqual({ rows: 2, address: '/overview?token=a-test-token' })
  })

  it('asks the bank route for the page the address names', async () => {
    const server = alwaysServing(200, BANK_BODY)

    await shellAt(`/avatars/Calix/bank?token=${TOKEN}&page=1`, server)

    expect(server.askedFor).toStrictEqual([
      '/api/v1/avatars/Calix/bank?token=a-test-token&page=1&size=100',
    ])
  })

  it('renders the empty state for a page past the end, not an error', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}&page=50`,
      alwaysServing(200, EMPTY_LEDGER_BODY),
    )

    expect(shownStatus()).toBe('Für Calix ist hier kein Vorgang gespeichert.')
  })

  it('surfaces an unclamped invalid page as a visible failure', async () => {
    const body = '{"message":"page: must be at least 0"}'
    const server = alwaysServing(400, body)

    await shellAt(`/avatars/Calix/bank?token=${TOKEN}&page=-1`, server)

    expect({
      shown: shownStatus(),
      askedFor: server.askedFor,
    }).toStrictEqual({
      shown: 'Fehler: The API answered 400',
      askedFor: [
        '/api/v1/avatars/Calix/bank?token=a-test-token&page=-1&size=100',
      ],
    })
  })

  it('passes a blank page value through to the API unchanged, not defaulted', async () => {
    const server = alwaysServing(400, '{"message":"page: must not be null"}')

    await shellAt(`/avatars/Calix/bank?token=${TOKEN}&page=`, server)

    expect(server.askedFor).toStrictEqual([
      '/api/v1/avatars/Calix/bank?token=a-test-token&page=NaN&size=100',
    ])
  })

  it('offers the ledgers of the avatar it shows in its navigation', async () => {
    await shellAt(
      `/avatars/Calix/bank?token=${TOKEN}`,
      alwaysServing(200, BANK_BODY),
    )

    const navigation = screen
      .getAllByRole('link')
      .map(
        (link) =>
          `${link.textContent ?? ''} ${link.getAttribute('href') ?? ''}`,
      )

    expect(navigation).toStrictEqual([
      'Übersicht /overview?token=a-test-token',
      'Bank /avatars/Calix/bank?token=a-test-token',
      'Lager /avatars/Calix/storage?token=a-test-token',
    ])
  })
})
