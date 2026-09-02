import type { ProtocolApi } from '../api'
import { windowOf } from '../api'
import type { BankEntry } from '../domain'
import { germanNameOf } from '../domain'
import type { Column } from '../ui'

import { LedgerView } from './LedgerView.tsx'
import { requestKeyOf } from './requestKey.ts'
import { bankPath } from './route.ts'
import { useLoad } from './useLoad.ts'

export interface BankLedgerViewProps {
  readonly api: ProtocolApi
  readonly avatar: string
  readonly token: string | null
  readonly page: number
  readonly onFollow: (href: string) => void
}

const columns: readonly Column<BankEntry>[] = [
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
    key: 'amount',
    header: 'Betrag',
    kind: 'number',
    tone: 'neutral',
    value: (entry) => entry.amount,
  },
  {
    key: 'transferType',
    header: 'Vorgang',
    kind: 'text',
    value: (entry) => germanNameOf(entry.transferType),
  },
]

export function BankLedgerView({
  api,
  avatar,
  token,
  page,
  onFollow,
}: BankLedgerViewProps) {
  const load = useLoad(
    () => api.bankEntries(avatar, windowOf(page)),
    requestKeyOf('bank', avatar, token, page),
  )

  return (
    <LedgerView
      heading={`Bank von ${avatar}`}
      avatar={avatar}
      load={load}
      columns={columns}
      token={token}
      pathOf={bankPath}
      onFollow={onFollow}
    />
  )
}
