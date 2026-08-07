import type { BankEntry, Ledger, Overview, StorageEntry } from '../domain'

import type { PageWindow } from './pageWindow.ts'

export interface ProtocolApi {
  overview: (window: PageWindow) => Promise<Overview>
  bankEntries: (
    avatar: string,
    window: PageWindow,
  ) => Promise<Ledger<BankEntry>>
  storageEntries: (
    avatar: string,
    window: PageWindow,
  ) => Promise<Ledger<StorageEntry>>
}
