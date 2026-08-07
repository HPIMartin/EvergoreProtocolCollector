const goldFormat = new Intl.NumberFormat('de-DE', { maximumFractionDigits: 0 })

const berlinDateFormat = new Intl.DateTimeFormat('de-DE', {
  timeZone: 'Europe/Berlin',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

const berlinTimeFormat = new Intl.DateTimeFormat('de-DE', {
  timeZone: 'Europe/Berlin',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

export const formatGold = (value: number): string => goldFormat.format(value)

export const instantOf = (isoInstant: string): number => {
  const epochMilliseconds = Date.parse(isoInstant)
  if (Number.isNaN(epochMilliseconds)) {
    throw new TypeError(`Not an instant: ${isoInstant}`)
  }
  return epochMilliseconds
}

export const formatTimestamp = (isoInstant: string): string => {
  const instant = instantOf(isoInstant)
  return `${berlinDateFormat.format(instant)} ${berlinTimeFormat.format(instant)}`
}
