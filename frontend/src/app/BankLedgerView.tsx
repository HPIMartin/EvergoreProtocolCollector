import type { ProtocolApi } from '../api'
import { FIRST_PAGE } from '../api'
import { BankEntryTable } from '../ui'

import { LedgerView } from './LedgerView.tsx'
import { requestKeyOf } from './requestKey.ts'
import { overviewPath } from './route.ts'
import { hrefOf } from './token.ts'
import { useLoad } from './useLoad.ts'

export interface BankLedgerViewProps {
  readonly api: ProtocolApi
  readonly avatar: string
  readonly token: string | null
  readonly onFollow: (href: string) => void
}

export function BankLedgerView({
  api,
  avatar,
  token,
  onFollow,
}: BankLedgerViewProps) {
  const load = useLoad(
    () => api.bankEntries(avatar, FIRST_PAGE),
    requestKeyOf('bank', avatar, token),
  )

  return (
    <LedgerView
      heading={`Bank von ${avatar}`}
      avatar={avatar}
      load={load}
      tableOf={(entries) => <BankEntryTable entries={entries} />}
      overviewHref={hrefOf(overviewPath(), token)}
      onFollow={onFollow}
    />
  )
}
