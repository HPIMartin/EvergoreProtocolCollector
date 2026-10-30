import type { Account } from '../domain'
import { formatTimestamp } from '../ui'

export interface AccountListProps {
  readonly accounts: readonly Account[]
}

export function AccountList({ accounts }: AccountListProps) {
  return (
    <>
      <h3 data-testid="accounts-title">Konten</h3>
      <table className="data-table" data-testid="accounts">
        <thead>
          <tr>
            <th className="data-table__cell">Name</th>
            <th className="data-table__cell">Ruht</th>
            <th className="data-table__cell">Letzte Anmeldung</th>
          </tr>
        </thead>
        <tbody>
          {accounts.map((account) => (
            <tr
              key={account.name}
              className="data-table__row"
              data-testid="account-row"
            >
              <td className="data-table__cell" data-testid="account-name">
                {account.name}
              </td>
              <td className="data-table__cell" data-testid="account-rests">
                {account.rests ? 'ruht' : ''}
              </td>
              <td className="data-table__cell" data-testid="account-last-login">
                {account.lastLogin === null
                  ? 'noch nie'
                  : formatTimestamp(account.lastLogin.toISOString())}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  )
}
