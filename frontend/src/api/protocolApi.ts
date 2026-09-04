import type {
  AdminStatus,
  BankEntry,
  Ledger,
  Overview,
  StorageEntry,
} from '../domain'

import type { PageWindow } from './pageWindow.ts'

export interface ProtocolApi {
  overview: (window: PageWindow) => Promise<Overview>
  adminStatus: () => Promise<AdminStatus>
  bankEntries: (
    avatar: string,
    window: PageWindow,
  ) => Promise<Ledger<BankEntry>>
  storageEntries: (
    avatar: string,
    window: PageWindow,
  ) => Promise<Ledger<StorageEntry>>
}
