import type { ReactNode } from 'react'

import type { HttpGet } from '../api'
import { httpProtocolApi } from '../api'

import { BankLedgerView } from './BankLedgerView.tsx'
import { OverviewView } from './OverviewView.tsx'
import { StorageLedgerView } from './StorageLedgerView.tsx'
import type { RouteVisitor } from './route.ts'
import { useRouting } from './useRouting.ts'

export interface AppProps {
  readonly get: HttpGet
}

export function App({ get }: AppProps) {
  const { route, token, follow } = useRouting()
  const api = httpProtocolApi(get, token)

  const view: RouteVisitor<ReactNode> = {
    overview: () => <OverviewView api={api} token={token} onFollow={follow} />,
    bank: (avatar) => (
      <BankLedgerView
        api={api}
        avatar={avatar}
        token={token}
        onFollow={follow}
      />
    ),
    storage: (avatar) => (
      <StorageLedgerView
        api={api}
        avatar={avatar}
        token={token}
        onFollow={follow}
      />
    ),
    unknownPath: (path) => (
      <p data-testid="view-unknown-path" role="alert">
        {`Für ${path} gibt es keine Ansicht.`}
      </p>
    ),
  }

  return (
    <>
      <h1 data-testid="app-title">Evergore Protocol Collector</h1>
      {route.accept(view)}
    </>
  )
}
