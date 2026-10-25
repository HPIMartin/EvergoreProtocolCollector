import { useState } from 'react'

import type { SessionApi } from '../api'
import { Unauthorized } from '../api'
import type { PageFrameAccount } from '../ui'

import { useLoad } from './useLoad.ts'

const LOGIN_PAGE = '/login'
const PASSWORD_PAGE = '/password'

export function useAccount(
  session: SessionApi,
  navigate: (target: string) => void,
  follow: (href: string) => void,
): PageFrameAccount | undefined {
  const load = useLoad(() => session.me(), 'me')
  const [logOutFailed, setLogOutFailed] = useState(false)

  return load.accept<PageFrameAccount | undefined>({
    loading: () => undefined,
    loaded: (me) => ({
      name: me.name,
      canChangePassword: me.canChangePassword,
      logOutFailed,
      onLogOut: () => {
        setLogOutFailed(false)
        session.logOut().then(
          () => {
            navigate(LOGIN_PAGE)
          },
          (failure: unknown) => {
            if (failure instanceof Unauthorized) {
              navigate(LOGIN_PAGE)
            } else {
              setLogOutFailed(true)
            }
          },
        )
      },
      onChangePassword: () => {
        follow(PASSWORD_PAGE)
      },
    }),
    unauthorized: () => undefined,
    forbidden: () => undefined,
    noSuchPage: () => undefined,
    failed: () => undefined,
  })
}
