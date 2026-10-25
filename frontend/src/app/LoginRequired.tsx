import { createContext, useContext, useEffect } from 'react'

import { loginAddressFor } from '../domain'
import { StatusPanel } from '../ui'

export const LoginNavigation = createContext<(target: string) => void>(
  () => undefined,
)

export function LoginRequired() {
  const navigate = useContext(LoginNavigation)

  useEffect(() => {
    navigate(loginAddressFor(window.location.pathname + window.location.search))
  }, [navigate])

  return <StatusPanel variant="loading" message="Weiter zur Anmeldung…" />
}
