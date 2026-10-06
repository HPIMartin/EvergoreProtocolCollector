import type {
  AdminStatus,
  AvatarSummary,
  BankEntry,
  GuildTotals,
  Overview,
  Page,
  RoundTrip,
  RoundTripAbstention,
  StorageEntry,
  TransferType,
} from '../domain'
import { isTransferType } from '../domain'

import { MalformedResponse } from './apiErrors.ts'

type WireObject = Record<string, unknown>

export function overviewFrom(body: unknown): Overview {
  const envelope = objectFrom(body)
  const page = pageFrom(envelope, summaryFrom)
  const totals = totalsFrom(envelope['totals'])
  if (totals.net !== null && page.items.some((item) => item.net === null)) {
    throw new MalformedResponse(
      'The API answered guild-wide sums beside a row without sums',
    )
  }
  if (
    totals.net === null &&
    page.items.length === page.totalCount &&
    page.items.every((item) => item.net !== null)
  ) {
    throw new MalformedResponse(
      'The API answered no guild-wide sums for a whole guild whose every row carries them',
    )
  }

  return {
    ...page,
    totals,
  }
}

function totalsFrom(value: unknown): GuildTotals {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new MalformedResponse(
      'The API answered an envelope without guild-wide totals',
    )
  }
  const totals = value as WireObject

  const sums = sumsFrom(totals)
  const share = {
    donation: optionalNumberFrom(totals, 'donation'),
    craftSubsidy: optionalNumberFrom(totals, 'craftSubsidy'),
    balance: optionalNumberFrom(totals, 'balance'),
    storageValue: optionalNumberFrom(totals, 'storageValue'),
  }
  refuseFiguresWithoutSums(sums, share)

  return {
    ...sums,
    ...share,
    containsStaleSums: booleanFrom(totals, 'containsStaleSums'),
  }
}

export function adminStatusFrom(body: unknown): AdminStatus {
  const envelope = objectFrom(body)

  return {
    lastUpdated: optionalInstantFrom(envelope, 'lastUpdated'),
    lastSuccessfulScrape: optionalInstantFrom(envelope, 'lastSuccessfulScrape'),
    lastScrapeFailure: optionalInstantFrom(envelope, 'lastScrapeFailure'),
    lastSuccessfulRecompute: optionalInstantFrom(
      envelope,
      'lastSuccessfulRecompute',
    ),
    lastRecomputeFailure: optionalInstantFrom(envelope, 'lastRecomputeFailure'),
    unknownItemNames: stringArrayFrom(envelope, 'unknownItemNames'),
    failedAvatarNames: stringArrayFrom(envelope, 'failedAvatarNames'),
    roundTrips: arrayFrom(envelope, 'roundTrips', roundTripFrom),
    roundTripAbstentions: arrayFrom(
      envelope,
      'roundTripAbstentions',
      roundTripAbstentionFrom,
    ),
  }
}

function roundTripFrom(item: unknown): RoundTrip {
  const roundTrip = objectFrom(item)
  const quantity = numberFrom(roundTrip, 'quantity')
  if (quantity <= 0) {
    throw new MalformedResponse(
      `The API answered a round trip of ${quantity} pieces`,
    )
  }

  return {
    avatar: stringFrom(roundTrip, 'avatar'),
    item: stringFrom(roundTrip, 'item'),
    quantity,
  }
}

function roundTripAbstentionFrom(item: unknown): RoundTripAbstention {
  const abstention = objectFrom(item)

  return {
    avatar: stringFrom(abstention, 'avatar'),
    item: stringFrom(abstention, 'item'),
  }
}

function arrayFrom<E>(
  source: WireObject,
  field: string,
  itemFrom: (item: unknown) => E,
): E[] {
  const value = source[field]
  if (!Array.isArray(value)) {
    throw new MalformedResponse(
      `The API answered a ${field} that is not an array`,
    )
  }

  return value.map(itemFrom)
}

function stringArrayFrom(source: WireObject, field: string): string[] {
  const value = source[field]
  if (
    !Array.isArray(value) ||
    value.some((entry) => typeof entry !== 'string')
  ) {
    throw new MalformedResponse(
      `The API answered a ${field} that is not an array of strings`,
    )
  }

  return value
}

export function bankPageFrom(body: unknown): Page<BankEntry> {
  return pageFrom(objectFrom(body), bankEntryFrom)
}

export function storagePageFrom(body: unknown): Page<StorageEntry> {
  return pageFrom(objectFrom(body), storageEntryFrom)
}

function pageFrom<E>(
  envelope: WireObject,
  itemFrom: (item: unknown) => E,
): Page<E> {
  return {
    page: numberFrom(envelope, 'page'),
    size: numberFrom(envelope, 'size'),
    totalCount: numberFrom(envelope, 'totalCount'),
    items: itemsFrom(envelope).map(itemFrom),
  }
}

function summaryFrom(item: unknown): AvatarSummary {
  const summary = objectFrom(item)
  const sums = sumsFrom(summary)
  const share = {
    donation: optionalNumberFrom(summary, 'donation'),
    craftSubsidy: optionalNumberFrom(summary, 'craftSubsidy'),
    balance: optionalNumberFrom(summary, 'balance'),
  }
  refuseFiguresWithoutSums(sums, share)

  return {
    avatar: stringFrom(summary, 'avatar'),
    ...sums,
    ...share,
    lastBankActivity: optionalInstantFrom(summary, 'lastBankActivity'),
    lastStorageActivity: optionalInstantFrom(summary, 'lastStorageActivity'),
    staleSumsFrom: optionalInstantFrom(summary, 'staleSumsFrom'),
  }
}

interface Sums {
  readonly bankWithdrawn: number | null
  readonly bankDeposited: number | null
  readonly storageWithdrawn: number | null
  readonly storageDeposited: number | null
  readonly net: number | null
}

const SUM_FIELDS = [
  'bankWithdrawn',
  'bankDeposited',
  'storageWithdrawn',
  'storageDeposited',
  'net',
] as const

function sumsFrom(source: WireObject): Sums {
  const absent = SUM_FIELDS.filter((field) => source[field] === null)
  if (absent.length === SUM_FIELDS.length) {
    return {
      bankWithdrawn: null,
      bankDeposited: null,
      storageWithdrawn: null,
      storageDeposited: null,
      net: null,
    }
  }
  if (absent.length > 0) {
    throw new MalformedResponse(
      `The API answered a row without its ${absent.join(', ')} but with its other sums`,
    )
  }

  return {
    bankWithdrawn: numberFrom(source, 'bankWithdrawn'),
    bankDeposited: numberFrom(source, 'bankDeposited'),
    storageWithdrawn: numberFrom(source, 'storageWithdrawn'),
    storageDeposited: numberFrom(source, 'storageDeposited'),
    net: numberFrom(source, 'net'),
  }
}

function refuseFiguresWithoutSums(
  sums: Sums,
  figures: Record<string, number | null>,
): void {
  if (sums.net !== null) {
    return
  }
  const present = Object.keys(figures).filter(
    (field) => figures[field] !== null,
  )
  if (present.length > 0) {
    throw new MalformedResponse(
      `The API answered a row without sums but with its ${present.join(', ')}`,
    )
  }
}

function bankEntryFrom(item: unknown): BankEntry {
  const entry = objectFrom(item)

  return {
    timestamp: instantFrom(entry, 'timestamp'),
    avatar: stringFrom(entry, 'avatar'),
    amount: numberFrom(entry, 'amount'),
    transferType: transferTypeFrom(entry, 'transferType'),
  }
}

function storageEntryFrom(item: unknown): StorageEntry {
  const entry = objectFrom(item)

  return {
    timestamp: instantFrom(entry, 'timestamp'),
    avatar: stringFrom(entry, 'avatar'),
    quantity: numberFrom(entry, 'quantity'),
    name: stringFrom(entry, 'name'),
    quality: numberFrom(entry, 'quality'),
    transferType: transferTypeFrom(entry, 'transferType'),
  }
}

function objectFrom(value: unknown): WireObject {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new MalformedResponse(
      'The API answered something that is not an object',
    )
  }

  return value as WireObject
}

function itemsFrom(envelope: WireObject): unknown[] {
  const items = envelope['items']
  if (!Array.isArray(items)) {
    throw new MalformedResponse(
      'The API answered an envelope without an items array',
    )
  }

  return items
}

function numberFrom(source: WireObject, field: string): number {
  const value = source[field]
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new MalformedResponse(
      `The API answered a ${field} that is not a number`,
    )
  }

  return value
}

function booleanFrom(source: WireObject, field: string): boolean {
  const value = source[field]
  if (typeof value !== 'boolean') {
    throw new MalformedResponse(
      `The API answered a ${field} that is not a boolean`,
    )
  }

  return value
}

function stringFrom(source: WireObject, field: string): string {
  const value = source[field]
  if (typeof value !== 'string') {
    throw new MalformedResponse(
      `The API answered a ${field} that is not a string`,
    )
  }

  return value
}

function instantFrom(source: WireObject, field: string): Date {
  const instant = new Date(stringFrom(source, field))
  if (Number.isNaN(instant.getTime())) {
    throw new MalformedResponse(
      `The API answered a ${field} that is not an instant`,
    )
  }

  return instant
}

function optionalInstantFrom(source: WireObject, field: string): Date | null {
  return source[field] === null ? null : instantFrom(source, field)
}

function optionalNumberFrom(source: WireObject, field: string): number | null {
  return source[field] === null ? null : numberFrom(source, field)
}

function transferTypeFrom(source: WireObject, field: string): TransferType {
  const value = source[field]
  if (!isTransferType(value)) {
    throw new MalformedResponse(
      `The API answered a ${field} the contract does not name`,
    )
  }

  return value
}
