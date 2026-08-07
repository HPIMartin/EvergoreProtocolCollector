import type { BankEntry } from '../domain'
import { berlinTimestampOf, germanNameOf } from '../domain'

export interface BankEntryTableProps {
  readonly entries: readonly BankEntry[]
}

export function BankEntryTable({ entries }: BankEntryTableProps) {
  return (
    <table data-testid="bank-entry-table">
      <thead>
        <tr>
          <th scope="col">Zeitpunkt</th>
          <th scope="col">Avatar</th>
          <th scope="col">Betrag</th>
          <th scope="col">Vorgang</th>
        </tr>
      </thead>
      <tbody>
        {entries.map((entry, position) => (
          <tr key={position} data-testid="bank-entry-row">
            <td>{berlinTimestampOf(entry.timestamp)}</td>
            <td>{entry.avatar}</td>
            <td>{entry.amount}</td>
            <td>{germanNameOf(entry.transferType)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
