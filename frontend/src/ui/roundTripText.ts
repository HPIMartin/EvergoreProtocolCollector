import type { RoundTrip, RoundTripAbstention } from '../domain'

import { formatCount } from './format.ts'

export const describeRoundTrip = (roundTrip: RoundTrip): string =>
  `${roundTrip.avatar}: ${formatCount(roundTrip.quantity)} × ${roundTrip.item}`

export const describeAbstention = (abstention: RoundTripAbstention): string =>
  `${abstention.avatar}: ${abstention.item}`
