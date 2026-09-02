import { describe, expect, it } from 'vitest'

import type { BankEntry, LedgerVisitor, StorageEntry } from '../domain'

import { MalformedResponse, RequestFailed, Unauthorized } from './apiErrors.ts'
import type { HttpGet } from './httpProtocolApi.ts'
import { httpProtocolApi } from './httpProtocolApi.ts'
import { FIRST_PAGE } from './pageWindow.ts'

const TOKEN = 'a-test-token'

const OVERVIEW_BODY = JSON.stringify({
  lastUpdated: '2026-08-05T10:15:00Z',
  page: 0,
  size: 100,
  totalCount: 1,
  items: [
    {
      avatar: 'Calix',
      bankWithdrawn: 1200,
      bankDeposited: 3400,
      storageWithdrawn: 200,
      storageDeposited: 500,
      net: 2500,
    },
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

interface RecordingFetch {
  readonly urls: string[]
  readonly get: HttpGet
}

function answering(status: number, body: string | null): RecordingFetch {
  const urls: string[] = []

  return {
    urls,
    get: (url) => {
      urls.push(url)
      return Promise.resolve(new Response(body, { status }))
    },
  }
}

function describingVisitor<E>(): LedgerVisitor<E, string> {
  return {
    entries: (page) => `${String(page.totalCount)} entries`,
    unknownAvatar: (avatar) => `unknown avatar ${avatar}`,
  }
}

describe('the HTTP protocol API', () => {
  it('asks the overview route for the requested window, carrying the token', async () => {
    const fetched = answering(200, OVERVIEW_BODY)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    await tested.overview(FIRST_PAGE)

    expect(fetched.urls).toStrictEqual([
      '/api/v1/avatars?token=a-test-token&page=0&size=100',
    ])
  })

  it('asks the bank route of one avatar, carrying the token', async () => {
    const fetched = answering(200, BANK_BODY)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    await tested.bankEntries('Calix', FIRST_PAGE)

    expect(fetched.urls).toStrictEqual([
      '/api/v1/avatars/Calix/bank?token=a-test-token&page=0&size=100',
    ])
  })

  it('asks the storage route of one avatar, carrying the token', async () => {
    const fetched = answering(200, STORAGE_BODY)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    await tested.storageEntries('Calix', FIRST_PAGE)

    expect(fetched.urls).toStrictEqual([
      '/api/v1/avatars/Calix/storage?token=a-test-token&page=0&size=100',
    ])
  })

  it('asks for the window it is given, not always the first one', async () => {
    const fetched = answering(200, BANK_BODY)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    await tested.bankEntries('Calix', { page: 3, size: 25 })

    expect(fetched.urls).toStrictEqual([
      '/api/v1/avatars/Calix/bank?token=a-test-token&page=3&size=25',
    ])
  })

  it('escapes an avatar name that would otherwise change the route', async () => {
    const fetched = answering(200, BANK_BODY)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    await tested.bankEntries('Erde/Eibe?token=stolen', FIRST_PAGE)

    expect(fetched.urls).toStrictEqual([
      '/api/v1/avatars/Erde%2FEibe%3Ftoken%3Dstolen/bank?token=a-test-token&page=0&size=100',
    ])
  })

  it('sends no token parameter at all when it has no token', async () => {
    const fetched = answering(200, OVERVIEW_BODY)
    const tested = httpProtocolApi(fetched.get, null)

    await tested.overview(FIRST_PAGE)

    expect(fetched.urls).toStrictEqual(['/api/v1/avatars?page=0&size=100'])
  })

  it('reads the overview of the guild', async () => {
    const fetched = answering(200, OVERVIEW_BODY)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    const overview = await tested.overview(FIRST_PAGE)

    expect(overview.items[0]?.avatar).toBe('Calix')
  })

  it('reads a bank ledger as the entries of a known avatar', async () => {
    const fetched = answering(200, BANK_BODY)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    const ledger = await tested.bankEntries('Calix', FIRST_PAGE)

    expect(ledger.accept(describingVisitor<BankEntry>())).toBe('1 entries')
  })

  it('reads a storage ledger as the entries of a known avatar', async () => {
    const fetched = answering(200, STORAGE_BODY)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    const ledger = await tested.storageEntries('Calix', FIRST_PAGE)

    expect(ledger.accept(describingVisitor<StorageEntry>())).toBe('1 entries')
  })

  it('tells an empty ledger of a known avatar apart from an unknown avatar', async () => {
    const fetched = answering(200, EMPTY_LEDGER_BODY)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    const ledger = await tested.storageEntries('Calix', FIRST_PAGE)

    expect(ledger.accept(describingVisitor<StorageEntry>())).toBe('0 entries')
  })

  it('reads a 404 on the bank route as an unknown avatar', async () => {
    const fetched = answering(404, null)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    const ledger = await tested.bankEntries('Nobody', FIRST_PAGE)

    expect(ledger.accept(describingVisitor<BankEntry>())).toBe(
      'unknown avatar Nobody',
    )
  })

  it('reads a 404 on the storage route as an unknown avatar', async () => {
    const fetched = answering(404, null)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    const ledger = await tested.storageEntries('Nobody', FIRST_PAGE)

    expect(ledger.accept(describingVisitor<StorageEntry>())).toBe(
      'unknown avatar Nobody',
    )
  })

  it('refuses a 404 on the overview, where no avatar was named', async () => {
    const fetched = answering(404, null)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    const reading = tested.overview(FIRST_PAGE)

    await expect(reading).rejects.toBeInstanceOf(RequestFailed)
  })

  it('reports a rejected token on the overview route', async () => {
    const fetched = answering(401, null)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    const reading = tested.overview(FIRST_PAGE)

    await expect(reading).rejects.toBeInstanceOf(Unauthorized)
  })

  it('reports a rejected token on the bank route', async () => {
    const fetched = answering(401, null)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    const reading = tested.bankEntries('Calix', FIRST_PAGE)

    await expect(reading).rejects.toBeInstanceOf(Unauthorized)
  })

  it('reports a rejected token on the storage route', async () => {
    const fetched = answering(401, null)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    const reading = tested.storageEntries('Calix', FIRST_PAGE)

    await expect(reading).rejects.toBeInstanceOf(Unauthorized)
  })

  it('reports a paging constraint violation with its status', async () => {
    const fetched = answering(400, '{"message":"page: must be at least 0"}')
    const tested = httpProtocolApi(fetched.get, TOKEN)

    const reading = tested.bankEntries('Calix', { page: -1, size: 100 })

    await expect(reading).rejects.toThrow('The API answered 400')
    expect(fetched.urls).toStrictEqual([
      '/api/v1/avatars/Calix/bank?token=a-test-token&page=-1&size=100',
    ])
  })

  it('keeps the token out of what it reports about a failure', async () => {
    const fetched = answering(500, null)
    const tested = httpProtocolApi(fetched.get, TOKEN)

    const failure = await tested
      .overview(FIRST_PAGE)
      .catch((reason: unknown) => String(reason))

    expect(failure).toBe('Error: The API answered 500')
  })

  it('refuses a body that is not JSON', async () => {
    const fetched = answering(200, '<html lang="de">the login page</html>')
    const tested = httpProtocolApi(fetched.get, TOKEN)

    const reading = tested.overview(FIRST_PAGE)

    await expect(reading).rejects.toBeInstanceOf(MalformedResponse)
  })
})
