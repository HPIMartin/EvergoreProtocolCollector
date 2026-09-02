export function requestKeyOf(
  view: string,
  avatar: string | null,
  token: string | null,
  page = 0,
): string {
  return JSON.stringify([view, avatar, token, page])
}
