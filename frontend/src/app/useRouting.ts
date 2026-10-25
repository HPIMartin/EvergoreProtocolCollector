import { useCallback, useEffect, useState } from 'react'

import type { Route } from './route.ts'
import { routeOf } from './route.ts'

interface Address {
  readonly path: string
  readonly search: string
}

export interface Routing {
  readonly route: Route
  readonly follow: (href: string) => void
}

export function useRouting(): Routing {
  const [address, setAddress] = useState(addressInBrowser)

  useEffect(() => {
    const rereadAddress = () => {
      setAddress(addressInBrowser())
    }

    window.addEventListener('popstate', rereadAddress)

    return () => {
      window.removeEventListener('popstate', rereadAddress)
    }
  }, [])

  const follow = useCallback((href: string) => {
    window.history.pushState(null, '', href)
    setAddress(addressInBrowser())
  }, [])

  return {
    route: routeOf(address.path, address.search),
    follow,
  }
}

function addressInBrowser(): Address {
  return { path: window.location.pathname, search: window.location.search }
}
