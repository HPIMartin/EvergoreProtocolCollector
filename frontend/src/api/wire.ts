import type {
  AvatarSummary,
  BankEntry,
  GuildTotals,
  Overview,
  Page,
  StorageEntry,
  TransferType,
} from '../domain'
import { isTransferType } from '../domain'

import { MalformedResponse } from './apiErrors.ts'

type WireObject = Record<string, unknown>

export function overviewFrom(body: unknown): Overview {
  const envelope = objectFrom(body)
  const page = pageFrom(envelope, summaryFrom)

  return {
    ...page,
    lastUpdated: optionalInstantFrom(envelope, 'lastUpdated'),
    totals: totalsFrom(envelope['totals']),
  }
}

function totalsFrom(value: unknown): GuildTotals {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new MalformedResponse(
      'The API answered an envelope without guild-wide totals',
    )
  }
  const totals = value as WireObject

  return {
    bankWithdrawn: numberFrom(totals, 'bankWithdrawn'),
    bankDeposited: numberFrom(totals, 'bankDeposited'),
    storageWithdrawn: numberFrom(totals, 'storageWithdrawn'),
    storageDeposited: numberFrom(totals, 'storageDeposited'),
    net: numberFrom(totals, 'net'),
  }
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

  return {
    avatar: stringFrom(summary, 'avatar'),
    bankWithdrawn: numberFrom(summary, 'bankWithdrawn'),
    bankDeposited: numberFrom(summary, 'bankDeposited'),
    storageWithdrawn: numberFrom(summary, 'storageWithdrawn'),
    storageDeposited: numberFrom(summary, 'storageDeposited'),
    net: numberFrom(summary, 'net'),
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

function transferTypeFrom(source: WireObject, field: string): TransferType {
  const value = source[field]
  if (!isTransferType(value)) {
    throw new MalformedResponse(
      `The API answered a ${field} the contract does not name`,
    )
  }

  return value
}
