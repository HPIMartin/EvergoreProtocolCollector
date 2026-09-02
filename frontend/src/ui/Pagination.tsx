import { Link } from './Link.tsx'

export interface PaginationProps {
  readonly previousHref: string | null
  readonly nextHref: string | null
  readonly onFollow?: (href: string) => void
}

export function Pagination({
  previousHref,
  nextHref,
  onFollow,
}: PaginationProps) {
  return (
    <nav className="pagination">
      {previousHref === null ? null : (
        <Link
          href={previousHref}
          onFollow={onFollow}
          className="pagination__previous"
          testId="pagination-previous"
        >
          Zurück
        </Link>
      )}
      {nextHref === null ? null : (
        <Link
          href={nextHref}
          onFollow={onFollow}
          className="pagination__next"
          testId="pagination-next"
        >
          Weiter
        </Link>
      )}
    </nav>
  )
}
