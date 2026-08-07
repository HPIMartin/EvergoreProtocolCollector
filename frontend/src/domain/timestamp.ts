const BERLIN = 'Europe/Berlin'

const berlinDate = new Intl.DateTimeFormat('de-DE', {
  timeZone: BERLIN,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

const berlinTime = new Intl.DateTimeFormat('de-DE', {
  timeZone: BERLIN,
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

export function berlinTimestampOf(instant: Date): string {
  return `${berlinDate.format(instant)} ${berlinTime.format(instant)}`
}
