export type OverviewRow = {
  readonly avatar: string
  readonly bankDeposited: number
  readonly bankWithdrawn: number
  readonly stored: number
  readonly retrieved: number
  readonly guildValue: number
  readonly lastStorageActivity: string | null
  readonly lastBankActivity: string | null
}

export type LedgerRow = {
  readonly id: string
  readonly occurredAt: string
  readonly item: string
  readonly amount: number
  readonly stored: number | null
  readonly retrieved: number | null
}

export const overviewRows: readonly OverviewRow[] = [
  {
    avatar: 'Alessia',
    bankDeposited: 57938,
    bankWithdrawn: 0,
    stored: 45120,
    retrieved: 200608,
    guildValue: -97550,
    lastStorageActivity: '2022-07-10T12:23:00Z',
    lastBankActivity: '2022-07-09T18:05:00Z',
  },
  {
    avatar: 'Bambor',
    bankDeposited: 58410,
    bankWithdrawn: 0,
    stored: 169254,
    retrieved: 226494,
    guildValue: 1170,
    lastStorageActivity: '2022-07-10T07:41:00Z',
    lastBankActivity: '2022-07-08T20:15:00Z',
  },
  {
    avatar: 'Evildead',
    bankDeposited: 45217,
    bankWithdrawn: 0,
    stored: 292118,
    retrieved: 239370,
    guildValue: 97965,
    lastStorageActivity: '2022-07-09T20:07:00Z',
    lastBankActivity: '2022-07-05T11:32:00Z',
  },
  {
    avatar: 'Fugger',
    bankDeposited: 0,
    bankWithdrawn: 247053,
    stored: 1171710,
    retrieved: 1978211,
    guildValue: -1053554,
    lastStorageActivity: '2022-07-10T05:12:00Z',
    lastBankActivity: '2022-07-10T04:58:00Z',
  },
  {
    avatar: 'Aargh',
    bankDeposited: 0,
    bankWithdrawn: 0,
    stored: 44208,
    retrieved: 44208,
    guildValue: 0,
    lastStorageActivity: '2022-06-28T16:44:00Z',
    lastBankActivity: null,
  },
]

export const ledgerRows: readonly LedgerRow[] = [
  {
    id: 'ledger-1',
    occurredAt: '2022-07-10T12:23:00Z',
    item: 'Heiltrank',
    amount: 12,
    stored: 5040,
    retrieved: null,
  },
  {
    id: 'ledger-2',
    occurredAt: '2022-07-10T07:41:00Z',
    item: 'Eisenerz',
    amount: 250,
    stored: null,
    retrieved: 12500,
  },
  {
    id: 'ledger-3',
    occurredAt: '2022-07-09T20:07:00Z',
    item: 'Wolfspelz',
    amount: 8,
    stored: 3200,
    retrieved: null,
  },
  {
    id: 'ledger-4',
    occurredAt: '2022-07-09T19:44:00Z',
    item: 'Mondkraut',
    amount: 4,
    stored: null,
    retrieved: 880,
  },
  {
    id: 'ledger-5',
    occurredAt: '2022-07-08T17:12:00Z',
    item: 'Eisenerz',
    amount: 195,
    stored: 9750,
    retrieved: null,
  },
]
