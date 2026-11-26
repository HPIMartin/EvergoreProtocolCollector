import { Fragment, useState } from 'react'

import type { ProtocolApi } from '../api'
import { Forbidden, Unauthorized } from '../api'
import type { Role } from '../domain'
import { formatTimestamp, Link, roundTripDetail } from '../ui'

import { AccountList } from './AccountList.tsx'
import { hrefOf } from './href.ts'
import { LoadedView } from './LoadedView.tsx'
import { requestKeyOf } from './requestKey.ts'
import { storagePath } from './route.ts'
import { UsageSection } from './UsageSection.tsx'
import { failureOf, useLoad } from './useLoad.ts'

export interface AdminViewProps {
  readonly api: ProtocolApi
  readonly onFollow: (href: string) => void
}

export function AdminView({ api, onFollow }: AdminViewProps) {
  const load = useLoad(
    async () => {
      const [status, accounts] = await Promise.all([
        api.adminStatus(),
        api.adminAccounts(),
      ])

      return { status, accounts }
    },
    requestKeyOf('admin', null),
  )
  const [refusal, setRefusal] = useState<Unauthorized | Forbidden | null>(null)
  const shown: typeof load = refusal === null ? load : failureOf(refusal)

  async function unlessRefused<T>(call: () => Promise<T>): Promise<T> {
    try {
      return await call()
    } catch (failure) {
      if (failure instanceof Unauthorized || failure instanceof Forbidden) {
        setRefusal(failure)
      }
      throw failure
    }
  }

  function reset(name: string): Promise<string> {
    return unlessRefused(() => api.resetPassword(name))
  }

  function setRole(name: string, role: Role): Promise<void> {
    return unlessRefused(() => api.setRole(name, role))
  }

  return (
    <section>
      <h2 data-testid="view-title">Admin</h2>
      <LoadedView load={shown}>
        {({ status, accounts }) => (
          <>
            <p data-testid="last-updated">{`Stand: ${momentOr(status.lastUpdated, 'noch kein Abgleich gelaufen')}`}</p>
            <p data-testid="last-successful-scrape">{`Letzter Scrape: ${momentOr(status.lastSuccessfulScrape, 'noch kein Scrape gelaufen')}`}</p>
            <p data-testid="last-successful-recompute">{`Letzte Neuberechnung: ${momentOr(status.lastSuccessfulRecompute, 'noch keine Neuberechnung gelaufen')}`}</p>
            <Failure
              testId="last-scrape-failure"
              heading="Letzter Scrape-Fehler"
              instant={status.lastScrapeFailure}
            />
            <Failure
              testId="last-recompute-failure"
              heading="Letzter Fehler bei der Neuberechnung"
              instant={status.lastRecomputeFailure}
            />
            <NameList
              testId="unknown-item-names"
              heading="Unbekannte Items"
              names={status.unknownItemNames}
            />
            <FindingList
              testId="failed-avatar-names"
              heading="Nicht aktualisierte Avatare"
              onFollow={onFollow}
              findings={status.failedAvatarNames.map((member) => ({ member }))}
            />
            <FindingList
              testId="round-trips"
              heading="Verdacht auf Warenkreislauf"
              onFollow={onFollow}
              findings={status.roundTrips.map((roundTrip) => ({
                member: roundTrip.avatar,
                detail: roundTripDetail(roundTrip),
              }))}
            />
            <FindingList
              testId="round-trip-abstentions"
              heading="Nicht beurteilbar (Rezept ungelesen)"
              onFollow={onFollow}
              findings={status.roundTripAbstentions.map((abstention) => ({
                member: abstention.avatar,
                detail: abstention.item,
              }))}
            />
            <AccountList accounts={accounts} reset={reset} setRole={setRole} />
            <UsageSection api={api} />
          </>
        )}
      </LoadedView>
    </section>
  )
}

interface FailureProps {
  readonly testId: string
  readonly heading: string
  readonly instant: Date | null
}

function Failure({ testId, heading, instant }: FailureProps) {
  if (instant === null) {
    return null
  }

  return (
    <p data-testid={testId}>
      {heading}: {formatTimestamp(instant.toISOString())}
    </p>
  )
}

interface NameListProps {
  readonly testId: string
  readonly heading: string
  readonly names: readonly string[]
}

function NameList({ testId, heading, names }: NameListProps) {
  if (names.length === 0) {
    return null
  }

  return (
    <p data-testid={testId}>
      {heading}: {names.join(', ')}
    </p>
  )
}

interface Finding {
  readonly member: string
  readonly detail?: string
}

interface FindingListProps {
  readonly testId: string
  readonly heading: string
  readonly findings: readonly Finding[]
  readonly onFollow: (href: string) => void
}

function FindingList({
  testId,
  heading,
  findings,
  onFollow,
}: FindingListProps) {
  if (findings.length === 0) {
    return null
  }

  return (
    <p data-testid={testId}>
      {heading}:{' '}
      {findings.map(({ member, detail }, position) => (
        <Fragment key={`${position}: ${member}`}>
          {position === 0 ? '' : ', '}
          <span data-testid="finding">
            <Link href={hrefOf(storagePath(member))} onFollow={onFollow}>
              {member}
            </Link>
            {detail === undefined ? null : `: ${detail}`}
          </span>
        </Fragment>
      ))}
    </p>
  )
}

function momentOr(instant: Date | null, fallback: string): string {
  return instant === null ? fallback : formatTimestamp(instant.toISOString())
}
