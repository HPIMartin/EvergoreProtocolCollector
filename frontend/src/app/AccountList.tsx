import { useState } from 'react'

import type { Account } from '../domain'
import { formatTimestamp } from '../ui'

export interface AccountListProps {
  readonly accounts: readonly Account[]
}

export function AccountList({ accounts }: AccountListProps) {
  const [asking, setAsking] = useState<string | null>(null)

  return (
    <>
      <h3 data-testid="accounts-title">Konten</h3>
      <table className="data-table" data-testid="accounts">
        <thead>
          <tr>
            <th className="data-table__cell">Name</th>
            <th className="data-table__cell">Ruht</th>
            <th className="data-table__cell">Letzte Anmeldung</th>
            <th className="data-table__cell" aria-label="Aktion" />
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
              <td className="data-table__cell">
                {asking === account.name ? (
                  <ResetQuestion
                    name={account.name}
                    onCancel={() => {
                      setAsking(null)
                    }}
                  />
                ) : (
                  <button
                    type="button"
                    data-testid="account-reset"
                    onClick={() => {
                      setAsking(account.name)
                    }}
                  >
                    Passwort zurücksetzen
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  )
}

interface ResetQuestionProps {
  readonly name: string
  readonly onCancel: () => void
}

function ResetQuestion({ name, onCancel }: ResetQuestionProps) {
  return (
    <>
      <span data-testid="reset-question">{`Passwort von ${name} wirklich zurücksetzen?`}</span>
      <button type="button" data-testid="reset-confirm">
        Zurücksetzen
      </button>
      <button type="button" data-testid="reset-cancel" onClick={onCancel}>
        Abbrechen
      </button>
    </>
  )
}
