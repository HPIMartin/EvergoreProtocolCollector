import type { StorageEntry } from '../domain'
import { berlinTimestampOf, germanNameOf } from '../domain'

export interface StorageEntryTableProps {
  readonly entries: readonly StorageEntry[]
}

export function StorageEntryTable({ entries }: StorageEntryTableProps) {
  return (
    <table data-testid="storage-entry-table">
      <thead>
        <tr>
          <th scope="col">Zeitpunkt</th>
          <th scope="col">Avatar</th>
          <th scope="col">Menge</th>
          <th scope="col">Gegenstand</th>
          <th scope="col">Qualität</th>
          <th scope="col">Vorgang</th>
        </tr>
      </thead>
      <tbody>
        {entries.map((entry, position) => (
          <tr key={position} data-testid="storage-entry-row">
            <td>{berlinTimestampOf(entry.timestamp)}</td>
            <td>{entry.avatar}</td>
            <td>{entry.quantity}</td>
            <td>{entry.name}</td>
            <td>{entry.quality}</td>
            <td>{germanNameOf(entry.transferType)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
