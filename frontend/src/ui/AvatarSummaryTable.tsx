import type { AvatarSummary } from '../domain'

import { Link } from './Link.tsx'

export interface AvatarSummaryRow {
  readonly summary: AvatarSummary
  readonly bankHref: string
  readonly storageHref: string
}

export interface AvatarSummaryTableProps {
  readonly rows: readonly AvatarSummaryRow[]
  readonly onFollow: (href: string) => void
}

export function AvatarSummaryTable({
  rows,
  onFollow,
}: AvatarSummaryTableProps) {
  return (
    <table data-testid="avatar-summary-table">
      <thead>
        <tr>
          <th scope="col">Avatar</th>
          <th scope="col">Entnommen</th>
          <th scope="col">Eingelagert</th>
          <th scope="col">Bank</th>
          <th scope="col">Lager</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.summary.avatar} data-testid="avatar-summary-row">
            <td>{row.summary.avatar}</td>
            <td>{row.summary.withdrawn}</td>
            <td>{row.summary.deposited}</td>
            <td>
              <Link href={row.bankHref} onFollow={onFollow}>
                Bank
              </Link>
            </td>
            <td>
              <Link href={row.storageHref} onFollow={onFollow}>
                Lager
              </Link>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
