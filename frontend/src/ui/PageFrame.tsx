import type { ReactNode } from 'react'

export type NavigationLink = {
  readonly label: string
  readonly href: string
  readonly current: boolean
}

export type PageFrameProps = {
  readonly brand: string
  readonly navigation: readonly NavigationLink[]
  readonly children: ReactNode
}

export function PageFrame({ brand, navigation, children }: PageFrameProps) {
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
                <a
                  className="page-frame__link"
                  href={link.href}
                  aria-current={link.current ? 'page' : undefined}
                >
                  {link.label}
                </a>
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
