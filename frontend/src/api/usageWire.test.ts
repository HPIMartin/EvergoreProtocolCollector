import { describe, expect, it } from 'vitest'

import { MalformedResponse } from './apiErrors.ts'
import { usageFrom } from './usageWire.ts'

function countsBody(overrides: Record<string, unknown> = {}) {
  return {
    logins: 0,
    pageLoads: 0,
    logouts: 0,
    daysUsed: 0,
    latestUse: null,
    ...overrides,
  }
}

function windowBody(overrides: Record<string, unknown> = {}) {
  return {
    accounts: [],
    guild: countsBody(),
    adminKey: countsBody(),
    ...overrides,
  }
}

function usageBody(overrides: Record<string, unknown> = {}) {
  return {
    lastThirtyDays: windowBody(),
    allTheTime: windowBody(),
    recordedSince: null,
    lostSinceStart: 0,
    ...overrides,
  }
}

describe('the usage wire shape', () => {
  it('reads both windows with their accounts, the guild, the admin key, the first day and the lost records', () => {
    const usage = usageFrom(
      usageBody({
        lastThirtyDays: windowBody({
          accounts: [
            {
              name: 'Aurora',
              counts: countsBody({
                logins: 3,
                pageLoads: 7,
                logouts: 2,
                daysUsed: 2,
                latestUse: '2026-01-09T20:14:00Z',
              }),
            },
            { name: 'Boreas', counts: countsBody() },
          ],
          guild: countsBody({
            logins: 3,
            pageLoads: 7,
            logouts: 2,
            daysUsed: 2,
            latestUse: '2026-01-09T20:14:00Z',
          }),
          adminKey: countsBody({
            logins: 1,
            daysUsed: 1,
            latestUse: '2026-01-10T08:02:00Z',
          }),
        }),
        allTheTime: windowBody({
          accounts: [{ name: 'Aurora', counts: countsBody({ logins: 5 }) }],
        }),
        recordedSince: '2025-11-01',
        lostSinceStart: 4,
      }),
    )

    expect(usage).toStrictEqual({
      lastThirtyDays: {
        accounts: [
          {
            name: 'Aurora',
            counts: {
              logins: 3,
              pageLoads: 7,
              logouts: 2,
              daysUsed: 2,
              latestUse: new Date('2026-01-09T20:14:00Z'),
            },
          },
          {
            name: 'Boreas',
            counts: {
              logins: 0,
              pageLoads: 0,
              logouts: 0,
              daysUsed: 0,
              latestUse: null,
            },
          },
        ],
        guild: {
          logins: 3,
          pageLoads: 7,
          logouts: 2,
          daysUsed: 2,
          latestUse: new Date('2026-01-09T20:14:00Z'),
        },
        adminKey: {
          logins: 1,
          pageLoads: 0,
          logouts: 0,
          daysUsed: 1,
          latestUse: new Date('2026-01-10T08:02:00Z'),
        },
      },
      allTheTime: {
        accounts: [
          {
            name: 'Aurora',
            counts: {
              logins: 5,
              pageLoads: 0,
              logouts: 0,
              daysUsed: 0,
              latestUse: null,
            },
          },
        ],
        guild: {
          logins: 0,
          pageLoads: 0,
          logouts: 0,
          daysUsed: 0,
          latestUse: null,
        },
        adminKey: {
          logins: 0,
          pageLoads: 0,
          logouts: 0,
          daysUsed: 0,
          latestUse: null,
        },
      },
      recordedSince: '2025-11-01',
      lostSinceStart: 4,
    })
  })

  it('reads a service without records as recorded since no day', () => {
    const usage = usageFrom(usageBody())

    expect(usage.recordedSince).toBeNull()
  })

  it.each(['lastThirtyDays', 'allTheTime'])(
    'refuses a body without the window %s',
    (window) => {
      const reading = () => usageFrom(usageBody({ [window]: undefined }))

      expect(reading).toThrow(MalformedResponse)
    },
  )

  it.each([-1, 1.5, '3', null])(
    'refuses a count of %s, which is no whole number of at least 0',
    (count) => {
      const body = usageBody({
        lastThirtyDays: windowBody({
          accounts: [
            { name: 'Aurora', counts: countsBody({ pageLoads: count }) },
          ],
        }),
      })
      const reading = () => usageFrom(body)

      expect(reading).toThrow(MalformedResponse)
    },
  )

  it.each(['logins', 'pageLoads', 'logouts', 'daysUsed'])(
    'refuses a %s of "3", which is no number',
    (field) => {
      const body = usageBody({
        lastThirtyDays: windowBody({ guild: countsBody({ [field]: '3' }) }),
      })
      const reading = () => usageFrom(body)

      expect(reading).toThrow(MalformedResponse)
    },
  )

  it.each(['recordedSince', 'lostSinceStart'])(
    'refuses a body without %s',
    (field) => {
      const reading = () => usageFrom(usageBody({ [field]: undefined }))

      expect(reading).toThrow(MalformedResponse)
    },
  )

  it.each(['guild', 'adminKey'])('refuses a window without the %s', (row) => {
    const body = usageBody({ allTheTime: windowBody({ [row]: undefined }) })
    const reading = () => usageFrom(body)

    expect(reading).toThrow(MalformedResponse)
  })

  it.each([undefined, 7])('refuses an account with the name %s', (name) => {
    const body = usageBody({
      allTheTime: windowBody({ accounts: [{ name, counts: countsBody() }] }),
    })
    const reading = () => usageFrom(body)

    expect(reading).toThrow(MalformedResponse)
  })

  it.each([-1, 2.5, '4'])('refuses a count of lost records of %s', (lost) => {
    const reading = () => usageFrom(usageBody({ lostSinceStart: lost }))

    expect(reading).toThrow(MalformedResponse)
  })

  it.each(['10.01.2026', '2026-01-10T00:00:00Z', 42])(
    'refuses a first day of %s, which is no calendar day',
    (day) => {
      const reading = () => usageFrom(usageBody({ recordedSince: day }))

      expect(reading).toThrow(MalformedResponse)
    },
  )

  it('refuses an account without counts', () => {
    const body = usageBody({
      allTheTime: windowBody({ accounts: [{ name: 'Aurora' }] }),
    })
    const reading = () => usageFrom(body)

    expect(reading).toThrow(MalformedResponse)
  })

  it('refuses a latest use that is no instant', () => {
    const body = usageBody({
      lastThirtyDays: windowBody({
        guild: countsBody({ latestUse: 'gestern' }),
      }),
    })
    const reading = () => usageFrom(body)

    expect(reading).toThrow(MalformedResponse)
  })
})
