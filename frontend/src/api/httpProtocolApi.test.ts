import { describe, expect, it } from 'vitest'

import type { BankEntry, LedgerVisitor, StorageEntry } from '../domain'

import {
  Forbidden,
  MalformedResponse,
  NoSuchPage,
  RequestFailed,
  Unauthorized,
} from './apiErrors.ts'
import type { HttpGet, HttpPost } from './httpProtocolApi.ts'
import { httpProtocolApi } from './httpProtocolApi.ts'
import { FIRST_PAGE } from './pageWindow.ts'

const OVERVIEW_BODY = JSON.stringify({
  page: 0,
  size: 100,
  totalCount: 1,
  totals: {
    bankWithdrawn: 1200,
    bankDeposited: 3400,
    storageWithdrawn: 200,
    storageDeposited: 500,
    net: 2500,
    donation: 400,
    craftSubsidy: 100,
    balance: 2800,
    storageValue: 600,
    containsStaleSums: false,
  },
  items: [
    {
      avatar: 'Calix',
      bankWithdrawn: 1200,
      bankDeposited: 3400,
      storageWithdrawn: 200,
      storageDeposited: 500,
      net: 2500,
      donation: 400,
      craftSubsidy: 100,
      balance: 2800,
      lastBankActivity: '2026-08-04T09:30:00Z',
      lastStorageActivity: '2026-08-05T10:15:00Z',
      staleSumsFrom: null,
      active: true,
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

const ADMIN_STATUS_BODY = JSON.stringify({
  lastUpdated: '2026-08-05T10:15:00Z',
  lastSuccessfulScrape: '2026-08-05T10:15:20Z',
  lastScrapeFailure: null,
  lastSuccessfulRecompute: '2026-08-05T10:15:30Z',
  lastRecomputeFailure: null,
  unknownItemNames: ['Unobtainium'],
  failedAvatarNames: [],
  roundTrips: [],
  roundTripAbstentions: [],
})

const ACCOUNTS_BODY = JSON.stringify([
  { name: 'Alrik', rests: false, lastLogin: '2026-10-20T08:15:00Z' },
  { name: 'Zwerg', rests: true, lastLogin: null },
])

interface Posted {
  readonly url: string
  readonly body: string
}

interface RecordingFetch {
  readonly urls: string[]
  readonly posted: Posted[]
  readonly get: HttpGet
  readonly post: HttpPost
}

function answering(status: number, body: string | null): RecordingFetch {
  const urls: string[] = []
  const posted: Posted[] = []

  return {
    urls,
    posted,
    get: (url) => {
      urls.push(url)
      return Promise.resolve(new Response(body, { status }))
    },
    post: (url, sent) => {
      posted.push({ url, body: sent })
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
  it('asks the overview route for the requested window', async () => {
    const fetched = answering(200, OVERVIEW_BODY)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    await tested.overview(FIRST_PAGE)

    expect(fetched.urls).toStrictEqual(['/api/v1/avatars?page=0&size=100'])
  })

  it('asks the bank route of one avatar', async () => {
    const fetched = answering(200, BANK_BODY)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    await tested.bankEntries('Calix', FIRST_PAGE)

    expect(fetched.urls).toStrictEqual([
      '/api/v1/avatars/Calix/bank?page=0&size=100',
    ])
  })

  it('asks the storage route of one avatar', async () => {
    const fetched = answering(200, STORAGE_BODY)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    await tested.storageEntries('Calix', FIRST_PAGE)

    expect(fetched.urls).toStrictEqual([
      '/api/v1/avatars/Calix/storage?page=0&size=100',
    ])
  })

  it('asks the storage route for the sort it is given', async () => {
    const fetched = answering(200, STORAGE_BODY)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    await tested.storageEntries('Calix', FIRST_PAGE, {
      column: 'quantity',
      direction: 'descending',
    })

    expect(fetched.urls).toStrictEqual([
      '/api/v1/avatars/Calix/storage?page=0&size=100&sort=quantity&direction=descending',
    ])
  })

  it('asks the bank route for the sort it is given', async () => {
    const fetched = answering(200, BANK_BODY)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    await tested.bankEntries('Calix', FIRST_PAGE, {
      column: 'amount',
      direction: 'ascending',
    })

    expect(fetched.urls).toStrictEqual([
      '/api/v1/avatars/Calix/bank?page=0&size=100&sort=amount&direction=ascending',
    ])
  })

  it('escapes a sort that would otherwise change the query', async () => {
    const fetched = answering(200, STORAGE_BODY)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    await tested.storageEntries('Calix', FIRST_PAGE, {
      column: 'quantity&size=1',
      direction: 'descending',
    })

    expect(fetched.urls).toStrictEqual([
      '/api/v1/avatars/Calix/storage?page=0&size=100&sort=quantity%26size%3D1&direction=descending',
    ])
  })

  it('asks for the window it is given, not always the first one', async () => {
    const fetched = answering(200, BANK_BODY)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    await tested.bankEntries('Calix', { page: 3, size: 25 })

    expect(fetched.urls).toStrictEqual([
      '/api/v1/avatars/Calix/bank?page=3&size=25',
    ])
  })

  it('escapes an avatar name that would otherwise change the route', async () => {
    const fetched = answering(200, BANK_BODY)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    await tested.bankEntries('Erde/Eibe?page=9', FIRST_PAGE)

    expect(fetched.urls).toStrictEqual([
      '/api/v1/avatars/Erde%2FEibe%3Fpage%3D9/bank?page=0&size=100',
    ])
  })

  it('reads the overview of the guild', async () => {
    const fetched = answering(200, OVERVIEW_BODY)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const overview = await tested.overview(FIRST_PAGE)

    expect(overview.items[0]?.avatar).toBe('Calix')
  })

  it('reads the admin status without any parameter', async () => {
    const fetched = answering(200, ADMIN_STATUS_BODY)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const status = await tested.adminStatus()

    expect({
      urls: fetched.urls,
      unknownItemNames: status.unknownItemNames,
    }).toStrictEqual({
      urls: ['/api/v1/admin/status'],
      unknownItemNames: ['Unobtainium'],
    })
  })

  it('reads a bank ledger as the entries of a known avatar', async () => {
    const fetched = answering(200, BANK_BODY)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const ledger = await tested.bankEntries('Calix', FIRST_PAGE)
    const described = ledger.accept(describingVisitor<BankEntry>())

    expect(described).toBe('1 entries')
  })

  it('reads a storage ledger as the entries of a known avatar', async () => {
    const fetched = answering(200, STORAGE_BODY)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const ledger = await tested.storageEntries('Calix', FIRST_PAGE)
    const described = ledger.accept(describingVisitor<StorageEntry>())

    expect(described).toBe('1 entries')
  })

  it('tells an empty ledger of a known avatar apart from an unknown avatar', async () => {
    const fetched = answering(200, EMPTY_LEDGER_BODY)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const ledger = await tested.storageEntries('Calix', FIRST_PAGE)
    const described = ledger.accept(describingVisitor<StorageEntry>())

    expect(described).toBe('0 entries')
  })

  it('reads a 404 on the bank route as an unknown avatar', async () => {
    const fetched = answering(404, null)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const ledger = await tested.bankEntries('Nobody', FIRST_PAGE)
    const described = ledger.accept(describingVisitor<BankEntry>())

    expect(described).toBe('unknown avatar Nobody')
  })

  it('reads a 404 on the storage route as an unknown avatar', async () => {
    const fetched = answering(404, null)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const ledger = await tested.storageEntries('Nobody', FIRST_PAGE)
    const described = ledger.accept(describingVisitor<StorageEntry>())

    expect(described).toBe('unknown avatar Nobody')
  })

  it('refuses a 404 on the overview, where no avatar was named', async () => {
    const fetched = answering(404, null)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const reading = tested.overview(FIRST_PAGE)

    await expect(reading).rejects.toBeInstanceOf(RequestFailed)
  })

  it('reports a refused login on the overview route', async () => {
    const fetched = answering(401, null)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const reading = tested.overview(FIRST_PAGE)

    await expect(reading).rejects.toBeInstanceOf(Unauthorized)
  })

  it('reports a refused role on the admin status route', async () => {
    const fetched = answering(403, null)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const reading = tested.adminStatus()

    await expect(reading).rejects.toBeInstanceOf(Forbidden)
  })

  it('reports a refused login on the bank route', async () => {
    const fetched = answering(401, null)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const reading = tested.bankEntries('Calix', FIRST_PAGE)

    await expect(reading).rejects.toBeInstanceOf(Unauthorized)
  })

  it('reports a refused login on the storage route', async () => {
    const fetched = answering(401, null)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const reading = tested.storageEntries('Calix', FIRST_PAGE)

    await expect(reading).rejects.toBeInstanceOf(Unauthorized)
  })

  it('reports a page the bank ledger cannot have as a page that does not exist', async () => {
    const fetched = answering(400, '{"message":"page: must be at least 0"}')
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const reading = tested.bankEntries('Calix', { page: -1, size: 100 })

    await expect(reading).rejects.toBeInstanceOf(NoSuchPage)
    expect(fetched.urls).toStrictEqual([
      '/api/v1/avatars/Calix/bank?page=-1&size=100',
    ])
  })

  it('reports a page the storage ledger cannot have as a page that does not exist', async () => {
    const fetched = answering(400, '{"message":"page: must be at least 0"}')
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const reading = tested.storageEntries('Calix', { page: -1, size: 100 })

    await expect(reading).rejects.toBeInstanceOf(NoSuchPage)
  })

  it('keeps any other ledger failure a failed request with its status', async () => {
    const fetched = answering(500, null)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const reading = tested.bankEntries('Calix', FIRST_PAGE)

    await expect(reading).rejects.toThrow('The API answered 500')
  })

  it('keeps a refused overview request a failed request with its status', async () => {
    const fetched = answering(400, '{"message":"page: must be at least 0"}')
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const reading = tested.overview({ page: -1, size: 100 })

    await expect(reading).rejects.toThrow('The API answered 400')
  })

  it('keeps the address out of what it reports about a failure', async () => {
    const fetched = answering(500, null)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const failure = await tested
      .overview(FIRST_PAGE)
      .catch((reason: unknown) => String(reason))

    expect(failure).toBe('Error: The API answered 500')
  })

  it('refuses a body that is not JSON', async () => {
    const fetched = answering(200, '<html lang="de">the login page</html>')
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const reading = tested.overview(FIRST_PAGE)

    await expect(reading).rejects.toBeInstanceOf(MalformedResponse)
  })

  it('reads the accounts from the admin accounts route without any parameter', async () => {
    const fetched = answering(200, ACCOUNTS_BODY)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const accounts = await tested.adminAccounts()

    expect({ urls: fetched.urls, accounts }).toStrictEqual({
      urls: ['/api/v1/admin/accounts'],
      accounts: [
        {
          name: 'Alrik',
          rests: false,
          lastLogin: new Date('2026-10-20T08:15:00Z'),
        },
        { name: 'Zwerg', rests: true, lastLogin: null },
      ],
    })
  })

  it.each([
    [401, Unauthorized],
    [403, Forbidden],
    [500, RequestFailed],
  ])('reports a %d on the admin accounts route', async (status, error) => {
    const fetched = answering(status, null)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const reading = tested.adminAccounts()

    await expect(reading).rejects.toBeInstanceOf(error)
  })

  it('refuses an account list that is no list', async () => {
    const fetched = answering(200, '{"items":[]}')
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const reading = tested.adminAccounts()

    await expect(reading).rejects.toBeInstanceOf(MalformedResponse)
  })

  it('posts the reset to the route of the account and answers the issued password', async () => {
    const fetched = answering(200, '{"password":"Rotkehlchen-42!"}')
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const issued = await tested.resetPassword('Alrik')

    expect({ posted: fetched.posted, issued }).toStrictEqual({
      posted: [{ url: '/api/v1/admin/accounts/Alrik/reset', body: '' }],
      issued: 'Rotkehlchen-42!',
    })
  })

  it('encodes the name of the account in the route of the reset', async () => {
    const fetched = answering(200, '{"password":"Rotkehlchen-42!"}')
    const tested = httpProtocolApi(fetched.get, fetched.post)

    await tested.resetPassword('Erde Eibe/Ärger?')

    expect(fetched.posted).toStrictEqual([
      {
        url: '/api/v1/admin/accounts/Erde%20Eibe%2F%C3%84rger%3F/reset',
        body: '',
      },
    ])
  })

  it.each([
    [401, Unauthorized],
    [403, Forbidden],
    [404, RequestFailed],
    [500, RequestFailed],
  ])('reports a %d on the reset route', async (status, error) => {
    const fetched = answering(status, null)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const resetting = tested.resetPassword('Alrik')

    await expect(resetting).rejects.toBeInstanceOf(error)
  })

  it.each([
    ['holds no password', '{}'],
    ['holds a password that is no string', '{"password":7}'],
    ['is no JSON', 'reset'],
  ])('refuses a reset answer that %s', async (_label, body) => {
    const fetched = answering(200, body)
    const tested = httpProtocolApi(fetched.get, fetched.post)

    const resetting = tested.resetPassword('Alrik')

    await expect(resetting).rejects.toBeInstanceOf(MalformedResponse)
  })
})
