export function fieldOf(body: unknown, field: string): unknown {
  return typeof body === 'object' && body !== null
    ? Object.entries(body).find(([key]) => key === field)?.[1]
    : undefined
}
