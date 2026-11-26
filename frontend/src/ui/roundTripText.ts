import type { RoundTrip } from '../domain'

import { formatCount } from './format.ts'

export const roundTripDetail = (roundTrip: RoundTrip): string =>
  `${formatCount(roundTrip.quantity)} × ${roundTrip.item}`
