import { useEffect } from 'react'

export interface RedirectProps {
  readonly href: string
  readonly replace: (href: string) => void
}

export function Redirect({ href, replace }: RedirectProps) {
  useEffect(() => {
    replace(href)
  }, [replace, href])

  return null
}
