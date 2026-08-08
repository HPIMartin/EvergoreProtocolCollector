import type { ReactNode } from 'react'

import { Link } from './Link.tsx'

export type NavigationLink = {
  readonly label: string
  readonly href: string
  readonly current: boolean
}

export type PageFrameProps = {
  readonly brand: string
  readonly navigation: readonly NavigationLink[]
  readonly onFollow?: (href: string) => void
  readonly children: ReactNode
}

export function PageFrame({
  brand,
  navigation,
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
      </header>
      <main className="page-frame__content" data-testid="page-content">
        {children}
      </main>
    </div>
  )
}
