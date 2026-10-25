import type { ReactNode } from 'react'

import { Link } from './Link.tsx'
import { LOG_OUT_FAILED_MESSAGE } from './logOutFailedMessage.ts'
import { StatusPanel } from './StatusPanel.tsx'

export type NavigationLink = {
  readonly label: string
  readonly href: string
  readonly current: boolean
}

export type PageFrameAccount = {
  readonly name: string
  readonly canChangePassword: boolean
  readonly logOutFailed: boolean
  readonly onLogOut: () => void
  readonly onChangePassword: () => void
}

export type PageFrameProps = {
  readonly brand: string
  readonly navigation: readonly NavigationLink[]
  readonly account?: PageFrameAccount
  readonly onFollow?: (href: string) => void
  readonly children: ReactNode
}

export function PageFrame({
  brand,
  navigation,
  account,
  onFollow,
  children,
}: PageFrameProps) {
  return (
    <div className="page-frame">
      <header className="page-frame__banner">
        <p className="page-frame__brand" data-testid="page-brand">
          {brand}
        </p>
        <nav className="page-frame__navigation" aria-label="Hauptnavigation">
          <ul>
            {navigation.map((link) => (
              <li key={link.href}>
                <Link
                  className="page-frame__link"
                  href={link.href}
                  current={link.current}
                  onFollow={onFollow}
                >
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
        {account === undefined ? null : <AccountArea account={account} />}
      </header>
      <main className="page-frame__content" data-testid="page-content">
        {children}
      </main>
    </div>
  )
}

function AccountArea({ account }: { readonly account: PageFrameAccount }) {
  return (
    <div className="page-frame__account" data-testid="account-area">
      <span data-testid="logged-in-as">{`Angemeldet als ${account.name}`}</span>
      <button type="button" onClick={account.onLogOut}>
        Abmelden
      </button>
      {account.canChangePassword ? (
        <button type="button" onClick={account.onChangePassword}>
          Passwort ändern
        </button>
      ) : null}
      {account.logOutFailed ? (
        <StatusPanel variant="error" message={LOG_OUT_FAILED_MESSAGE} />
      ) : null}
    </div>
  )
}
