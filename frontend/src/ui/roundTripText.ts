import type { RoundTrip, RoundTripAbstention } from '../domain'

import { formatCount } from './format.ts'

export const roundTripDetail = (roundTrip: RoundTrip): string =>
  `${formatCount(roundTrip.quantity)} × ${roundTrip.item}`

export const describeAbstention = (abstention: RoundTripAbstention): string =>
  `${abstention.avatar}: ${abstention.item}`
