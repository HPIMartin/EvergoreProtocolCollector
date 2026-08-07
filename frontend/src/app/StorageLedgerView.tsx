import type { ProtocolApi } from '../api'
import { FIRST_PAGE } from '../api'
import { StorageEntryTable } from '../ui'

import { LedgerView } from './LedgerView.tsx'
import { requestKeyOf } from './requestKey.ts'
import { overviewPath } from './route.ts'
import { hrefOf } from './token.ts'
import { useLoad } from './useLoad.ts'

export interface StorageLedgerViewProps {
  readonly api: ProtocolApi
  readonly avatar: string
  readonly token: string | null
  readonly onFollow: (href: string) => void
}

export function StorageLedgerView({
  api,
  avatar,
  token,
  onFollow,
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
      tableOf={(entries) => <StorageEntryTable entries={entries} />}
      overviewHref={hrefOf(overviewPath(), token)}
      onFollow={onFollow}
    />
  )
}
