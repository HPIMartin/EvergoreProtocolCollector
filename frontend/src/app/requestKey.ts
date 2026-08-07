export function requestKeyOf(
  view: string,
  avatar: string | null,
  token: string | null,
): string {
  return JSON.stringify([view, avatar, token])
}
