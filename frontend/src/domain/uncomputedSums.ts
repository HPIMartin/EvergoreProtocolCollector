export const GUILD_UNCOMPUTED_SUMS_NOTE =
  'Enthält mindestens eine Zeile, die noch nicht berechnet ist.'

export const isUncomputed = (sums: { readonly net: number | null }): boolean =>
  sums.net === null
