import type { ReactNode } from 'react'

import type { HttpGet } from '../api'
import { httpProtocolApi } from '../api'
import type { NavigationLink } from '../ui'
import { PageFrame, StatusPanel } from '../ui'

import { AdminView } from './AdminView.tsx'
import { BankLedgerView } from './BankLedgerView.tsx'
import { OverviewView } from './OverviewView.tsx'
import { StorageLedgerView } from './StorageLedgerView.tsx'
import type { Route, RouteVisitor } from './route.ts'
import { bankPath, overviewPath, storagePath } from './route.ts'
import { hrefOf } from './token.ts'
import { useRouting } from './useRouting.ts'

const BRAND = 'Evergore Protocol Collector'

export interface AppProps {
  readonly get: HttpGet
}

export function App({ get }: AppProps) {
  const { route, token, follow } = useRouting()
  const api = httpProtocolApi(get, token)

  const view: RouteVisitor<ReactNode> = {
    overview: () => <OverviewView api={api} token={token} onFollow={follow} />,
    admin: () => <AdminView api={api} />,
    bank: (avatar, page) => (
      <BankLedgerView
        api={api}
        avatar={avatar}
        token={token}
        page={page}
        onFollow={follow}
      />
    ),
    storage: (avatar, page) => (
      <StorageLedgerView
        api={api}
        avatar={avatar}
        token={token}
        page={page}
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
    <PageFrame
      brand={BRAND}
      navigation={navigationOf(route, token)}
      onFollow={follow}
    >
      {route.accept(view)}
    </PageFrame>
  )
}

function navigationOf(
  route: Route,
  token: string | null,
): readonly NavigationLink[] {
  const overview: NavigationLink = {
    label: 'Übersicht',
    href: hrefOf(overviewPath(), token),
    current: false,
  }

  return route.accept<readonly NavigationLink[]>({
    overview: () => [{ ...overview, current: true }],
    admin: () => [overview],
    bank: (avatar) => [overview, ...ledgersOf(avatar, token, 'bank')],
    storage: (avatar) => [overview, ...ledgersOf(avatar, token, 'storage')],
    unknownPath: () => [overview],
  })
}

function ledgersOf(
  avatar: string,
  token: string | null,
  shown: 'bank' | 'storage',
): readonly NavigationLink[] {
  return [
    {
      label: 'Bank',
      href: hrefOf(bankPath(avatar), token),
      current: shown === 'bank',
    },
    {
      label: 'Lager',
      href: hrefOf(storagePath(avatar), token),
      current: shown === 'storage',
    },
  ]
}
