import type { MouseEvent, ReactNode } from 'react'

export interface LinkProps {
  readonly href: string
  readonly onFollow?: (href: string) => void
  readonly className?: string
  readonly current?: boolean
  readonly children: ReactNode
}

export function Link({
  href,
  onFollow,
  className,
  current,
  children,
}: LinkProps) {
  const followHere = (event: MouseEvent<HTMLAnchorElement>) => {
    if (onFollow === undefined || opensElsewhere(event)) {
      return
    }

    event.preventDefault()
    onFollow(href)
  }

  return (
    <a
      href={href}
      className={className}
      aria-current={current === true ? 'page' : undefined}
      onClick={followHere}
    >
      {children}
    </a>
  )
}

function opensElsewhere(event: MouseEvent<HTMLAnchorElement>): boolean {
  return (
    event.button !== 0 ||
    event.ctrlKey ||
    event.metaKey ||
    event.shiftKey ||
    event.altKey
  )
}
