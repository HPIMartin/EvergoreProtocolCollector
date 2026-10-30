import { useEffect, useRef, useState } from 'react'

import type { Account } from '../domain'
import { formatTimestamp, LOAD_FAILED_MESSAGE, StatusPanel } from '../ui'

export interface AccountListProps {
  readonly accounts: readonly Account[]
  readonly reset: (name: string) => Promise<string>
}

export function AccountList({ accounts, reset }: AccountListProps) {
  const [asking, setAsking] = useState<string | null>(null)
  const [outcomes, setOutcomes] = useState<Readonly<Record<string, Outcome>>>(
    {},
  )
  const [closedAt, setClosedAt] = useState<string | null>(null)
  const latestReset = useRef<Record<string, number>>({})

  function closeQuestionOf(name: string): void {
    setAsking(null)
    setClosedAt(name)
  }

  function resetPasswordOf(name: string): void {
    closeQuestionOf(name)
    const mine = (latestReset.current[name] ?? 0) + 1
    latestReset.current[name] = mine
    setOutcomes((before) =>
      before[name]?.kind === 'failed' ? withoutOutcomeOf(before, name) : before,
    )
    const settle = (outcome: Outcome): void => {
      if (latestReset.current[name] === mine) {
        setOutcomes((before) => ({ ...before, [name]: outcome }))
      }
    }
    reset(name).then(
      (password) => {
        settle({ kind: 'issued', password })
      },
      () => {
        settle({ kind: 'failed' })
      },
    )
  }

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
                <div aria-live="polite">
                  <OutcomeOf outcome={outcomes[account.name]} />
                  {asking === account.name ? (
                    <ResetQuestion
                      name={account.name}
                      onConfirm={() => {
                        resetPasswordOf(account.name)
                      }}
                      onCancel={() => {
                        closeQuestionOf(account.name)
                      }}
                    />
                  ) : null}
                </div>
                {asking === account.name ? null : (
                  <ResetButton
                    focusOnMount={closedAt === account.name}
                    onClick={() => {
                      setClosedAt(null)
                      setAsking(account.name)
                    }}
                  />
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  )
}

type Outcome =
  | { readonly kind: 'issued'; readonly password: string }
  | { readonly kind: 'failed' }

function withoutOutcomeOf(
  outcomes: Readonly<Record<string, Outcome>>,
  name: string,
): Readonly<Record<string, Outcome>> {
  return Object.fromEntries(
    Object.entries(outcomes).filter(([other]) => other !== name),
  )
}

function OutcomeOf({ outcome }: { readonly outcome: Outcome | undefined }) {
  if (outcome === undefined) {
    return null
  }

  return outcome.kind === 'issued' ? (
    <span data-testid="account-issued">{outcome.password}</span>
  ) : (
    <StatusPanel variant="error" message={LOAD_FAILED_MESSAGE} />
  )
}

interface ResetButtonProps {
  readonly focusOnMount: boolean
  readonly onClick: () => void
}

function ResetButton({ focusOnMount, onClick }: ResetButtonProps) {
  const button = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (focusOnMount) {
      button.current?.focus()
    }
  }, [focusOnMount])

  return (
    <button
      ref={button}
      type="button"
      data-testid="account-reset"
      onClick={onClick}
    >
      Passwort zurücksetzen
    </button>
  )
}

interface ResetQuestionProps {
  readonly name: string
  readonly onConfirm: () => void
  readonly onCancel: () => void
}

function ResetQuestion({ name, onConfirm, onCancel }: ResetQuestionProps) {
  return (
    <>
      <span data-testid="reset-question">{`Passwort von ${name} wirklich zurücksetzen?`}</span>
      <button type="button" data-testid="reset-confirm" onClick={onConfirm}>
        Zurücksetzen
      </button>
      <button type="button" data-testid="reset-cancel" onClick={onCancel}>
        Abbrechen
      </button>
    </>
  )
}
