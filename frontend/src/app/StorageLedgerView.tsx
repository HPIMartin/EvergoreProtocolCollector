import type { ProtocolApi } from '../api'
import { FIRST_PAGE } from '../api'
import type { StorageEntry } from '../domain'
import { germanNameOf } from '../domain'
import type { Column } from '../ui'

import { LedgerView } from './LedgerView.tsx'
import { requestKeyOf } from './requestKey.ts'
import { useLoad } from './useLoad.ts'

export interface StorageLedgerViewProps {
  readonly api: ProtocolApi
  readonly avatar: string
  readonly token: string | null
}

const columns: readonly Column<StorageEntry>[] = [
  {
    key: 'timestamp',
    header: 'Zeitpunkt',
    kind: 'timestamp',
    value: (entry) => entry.timestamp.toISOString(),
  },
  {
    key: 'avatar',
    header: 'Avatar',
    kind: 'text',
    value: (entry) => entry.avatar,
  },
  {
    key: 'quantity',
    header: 'Menge',
    kind: 'number',
    tone: 'neutral',
    value: (entry) => entry.quantity,
  },
  {
    key: 'name',
    header: 'Gegenstand',
    kind: 'text',
    value: (entry) => entry.name,
  },
  {
    key: 'quality',
    header: 'Qualität',
    kind: 'number',
    tone: 'neutral',
    value: (entry) => entry.quality,
  },
  {
    key: 'transferType',
    header: 'Vorgang',
    kind: 'text',
    value: (entry) => germanNameOf(entry.transferType),
  },
]

export function StorageLedgerView({
  api,
  avatar,
  token,
}: StorageLedgerViewProps) {
  const load = useLoad(
    () => api.storageEntries(avatar, FIRST_PAGE),
    requestKeyOf('storage', avatar, token),
  )

  return (
    <LedgerView
      heading={`Lager von ${avatar}`}
      avatar={avatar}
      load={load}
      columns={columns}
    />
  )
}
