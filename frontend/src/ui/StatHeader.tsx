import { formatGold } from './format.ts'
import type { Tone } from './tone.ts'
import { toneOf } from './tone.ts'

export interface Stat {
  readonly key: string
  readonly label: string
  readonly value: number | null
  readonly positiveTone: Tone
  readonly absentNote?: string
}

export interface StatHeaderProps {
  readonly stats: readonly Stat[]
}

export function StatHeader({ stats }: StatHeaderProps) {
  return (
    <dl className="stat-header" data-testid="stat-header">
      {stats.map((stat) => (
        <div className="stat-header__stat" key={stat.key}>
          <dt className="stat-header__label">{stat.label}</dt>
          <dd
            className="stat-header__value"
            data-absent={stat.value === null ? 'true' : undefined}
            data-testid={`stat-${stat.key}`}
            data-tone={
              stat.value === null
                ? 'neutral'
                : toneOf(stat.value, stat.positiveTone)
            }
          >
            {stat.value === null ? stat.absentNote : formatGold(stat.value)}
          </dd>
        </div>
      ))}
    </dl>
  )
}
