export interface PageWindow {
  readonly page: number
  readonly size: number
}

export const FIRST_PAGE: PageWindow = { page: 0, size: 100 }

export function windowOf(page: number): PageWindow {
  return { page, size: FIRST_PAGE.size }
}
