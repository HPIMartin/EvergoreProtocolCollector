export const staleSumsNoteOf = (lastSuccessfulUpdate: string): string =>
  `Veraltete Zahlen. Letzte erfolgreiche Aktualisierung vom ${lastSuccessfulUpdate}.`

export const GUILD_STALE_SUMS_NOTE =
  'Enthält mindestens eine Zeile mit veralteten Zahlen.'
