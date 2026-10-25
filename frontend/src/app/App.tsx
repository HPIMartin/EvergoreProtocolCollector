import type { ReactNode } from 'react'

import type { HttpGet } from '../api'
import { httpProtocolApi } from '../api'
import type { NavigationLink } from '../ui'
import { PageFrame, StatusPanel } from '../ui'

import { AdminView } from './AdminView.tsx'
import { BankLedgerView } from './BankLedgerView.tsx'
import { ExplanationView } from './ExplanationView.tsx'
import { LoginNavigation } from './LoginRequired.tsx'
import { OverviewView } from './OverviewView.tsx'
import { StorageLedgerView } from './StorageLedgerView.tsx'
import type { Route, RouteVisitor } from './route.ts'
import { bankPath, overviewPath, storagePath } from './route.ts'
import { hrefOf } from './href.ts'
import { useRouting } from './useRouting.ts'

const BRAND = 'Evergore Protocol Collector'

export interface AppProps {
  readonly get: HttpGet
  readonly navigate?: (target: string) => void
}

function navigateTheBrowser(target: string): void {
  window.location.assign(target)
}

export function App({ get, navigate = navigateTheBrowser }: AppProps) {
  const { route, follow } = useRouting()
  const api = httpProtocolApi(get)

  const view: RouteVisitor<ReactNode> = {
    overview: () => <OverviewView api={api} onFollow={follow} />,
    admin: () => <AdminView api={api} />,
    explanation: () => <ExplanationView />,
    bank: (avatar, page, sort) => (
      <BankLedgerView
        api={api}
        avatar={avatar}
        page={page}
        sort={sort}
        onFollow={follow}
      />
    ),
    storage: (avatar, page, sort) => (
      <StorageLedgerView
        api={api}
        avatar={avatar}
        page={page}
        sort={sort}
        onFollow={follow}
      />
    ),
    unknownPath: (path) => (
      <StatusPanel
        variant="error"
        message={`Für ${path} gibt es keine Ansicht.`}
      />
    ),
  }

  return (
    <LoginNavigation value={navigate}>
      <PageFrame
        brand={BRAND}
        navigation={navigationOf(route)}
        onFollow={follow}
      >
        {route.accept(view)}
      </PageFrame>
    </LoginNavigation>
  )
}

function navigationOf(route: Route): readonly NavigationLink[] {
  const overview: NavigationLink = {
    label: 'Übersicht',
    href: hrefOf(overviewPath()),
    current: false,
  }

  return route.accept<readonly NavigationLink[]>({
    overview: () => [{ ...overview, current: true }],
    admin: () => [overview],
    explanation: () => [overview],
    bank: (avatar) => [overview, ...ledgersOf(avatar, 'bank')],
    storage: (avatar) => [overview, ...ledgersOf(avatar, 'storage')],
    unknownPath: () => [overview],
  })
}

function ledgersOf(
  avatar: string,
  shown: 'bank' | 'storage',
): readonly NavigationLink[] {
  return [
    {
      label: 'Bank',
      href: hrefOf(bankPath(avatar)),
      current: shown === 'bank',
    },
    {
      label: 'Lager',
      href: hrefOf(storagePath(avatar)),
      current: shown === 'storage',
    },
  ]
}
