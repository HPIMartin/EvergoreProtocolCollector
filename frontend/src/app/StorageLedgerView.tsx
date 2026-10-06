import type { LedgerSort, ProtocolApi } from '../api'
import { windowOf } from '../api'
import type { StorageEntry } from '../domain'
import { germanTransferOfStorageEntry } from '../domain'
import type { Column } from '../ui'

import { LedgerView } from './LedgerView.tsx'
import { requestKeyOf } from './requestKey.ts'
import { storagePath } from './route.ts'
import { useLoad } from './useLoad.ts'

export interface StorageLedgerViewProps {
  readonly api: ProtocolApi
  readonly avatar: string
  readonly token: string | null
  readonly page: number
  readonly sort: LedgerSort | null
  readonly onFollow: (href: string) => void
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
    value: (entry) => germanTransferOfStorageEntry(entry),
  },
]

export function StorageLedgerView({
  api,
  avatar,
  token,
  page,
  sort,
  onFollow,
}: StorageLedgerViewProps) {
  const load = useLoad(
    () => api.storageEntries(avatar, windowOf(page), sort),
    requestKeyOf('storage', avatar, token, page, sort),
  )

  return (
    <LedgerView
      heading={`Lager von ${avatar}`}
      avatar={avatar}
      load={load}
      columns={columns}
      token={token}
      pathOf={storagePath}
      onFollow={onFollow}
    />
  )
}
