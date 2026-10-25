import type { ReactNode } from 'react'

import type { HttpGet, SessionApi } from '../api'
import { httpProtocolApi } from '../api'
import type { NavigationLink } from '../ui'
import { PageFrame, StatusPanel } from '../ui'

import { AdminView } from './AdminView.tsx'
import { BankLedgerView } from './BankLedgerView.tsx'
import { ChangePasswordView } from './ChangePasswordView.tsx'
import { ExplanationView } from './ExplanationView.tsx'
import { LoginNavigation } from './LoginRequired.tsx'
import { OverviewView } from './OverviewView.tsx'
import { Redirect } from './Redirect.tsx'
import { StorageLedgerView } from './StorageLedgerView.tsx'
import type { Route, RouteVisitor } from './route.ts'
import { bankPath, overviewPath, storagePath } from './route.ts'
import { hrefOf } from './href.ts'
import { useAccount } from './useAccount.ts'
import { useRouting } from './useRouting.ts'

const BRAND = 'Evergore Protocol Collector'

export interface AppProps {
  readonly get: HttpGet
  readonly session: SessionApi
  readonly navigate?: (target: string) => void
}

function navigateTheBrowser(target: string): void {
  window.location.assign(target)
}

export function App({ get, session, navigate = navigateTheBrowser }: AppProps) {
  const { route, follow, replace } = useRouting()
  const api = httpProtocolApi(get)
  const account = useAccount(session, navigate, follow)

  const view: RouteVisitor<ReactNode> = {
    overview: () => <OverviewView api={api} onFollow={follow} />,
    admin: () => <AdminView api={api} />,
    explanation: () => <ExplanationView />,
    password: () =>
      account?.canChangePassword === false ? (
        <Redirect href={hrefOf(overviewPath())} replace={replace} />
      ) : (
        <ChangePasswordView
          session={session}
          onChanged={() => follow(hrefOf(overviewPath()))}
          onNoAccount={() => follow(hrefOf(overviewPath()))}
        />
      ),
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
        account={account}
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
    password: () => [overview],
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
