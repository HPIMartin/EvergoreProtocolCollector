import type { AccountUsage, Usage, UsageCounts, UsageWindow } from '../domain'

import { MalformedResponse } from './apiErrors.ts'
import type { WireObject } from './wire.ts'
import {
  arrayFrom,
  objectFrom,
  optionalInstantFrom,
  stringFrom,
} from './wire.ts'

const CALENDAR_DAY = /^\d{4}-\d{2}-\d{2}$/

export function usageFrom(body: unknown): Usage {
  const usage = objectFrom(body)

  return {
    lastThirtyDays: windowFrom(usage['lastThirtyDays']),
    allTheTime: windowFrom(usage['allTheTime']),
    recordedSince:
      usage['recordedSince'] === null
        ? null
        : calendarDayFrom(usage, 'recordedSince'),
    lostSinceStart: countFrom(usage, 'lostSinceStart'),
  }
}

function windowFrom(value: unknown): UsageWindow {
  const window = objectFrom(value)

  return {
    accounts: arrayFrom(window, 'accounts', accountUsageFrom),
    guild: countsFrom(window['guild']),
    adminKey: countsFrom(window['adminKey']),
  }
}

function accountUsageFrom(item: unknown): AccountUsage {
  const account = objectFrom(item)

  return {
    name: stringFrom(account, 'name'),
    counts: countsFrom(account['counts']),
  }
}

function countsFrom(value: unknown): UsageCounts {
  const counts = objectFrom(value)

  return {
    logins: countFrom(counts, 'logins'),
    pageLoads: countFrom(counts, 'pageLoads'),
    logouts: countFrom(counts, 'logouts'),
    daysUsed: countFrom(counts, 'daysUsed'),
    latestUse: optionalInstantFrom(counts, 'latestUse'),
  }
}

function countFrom(source: WireObject, field: string): number {
  const value = source[field]
  if (typeof value !== 'number' || !Number.isInteger(value) || value < 0) {
    throw new MalformedResponse(
      `The API answered a ${field} that is not a whole number of at least 0`,
    )
  }

  return value
}

function calendarDayFrom(source: WireObject, field: string): string {
  const value = stringFrom(source, field)
  if (!CALENDAR_DAY.test(value)) {
    throw new MalformedResponse(
      `The API answered a ${field} that is not a calendar day`,
    )
  }

  return value
}
