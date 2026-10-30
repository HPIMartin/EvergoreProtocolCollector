import type {
  Account,
  AdminStatus,
  BankEntry,
  Ledger,
  Overview,
  StorageEntry,
} from '../domain'

import type { LedgerSort } from './ledgerSort.ts'
import type { PageWindow } from './pageWindow.ts'

export interface ProtocolApi {
  overview: (window: PageWindow) => Promise<Overview>
  adminStatus: () => Promise<AdminStatus>
  adminAccounts: () => Promise<Account[]>
  resetPassword: (name: string) => Promise<string>
  bankEntries: (
    avatar: string,
    window: PageWindow,
    sort?: LedgerSort | null,
  ) => Promise<Ledger<BankEntry>>
  storageEntries: (
    avatar: string,
    window: PageWindow,
    sort?: LedgerSort | null,
  ) => Promise<Ledger<StorageEntry>>
}
