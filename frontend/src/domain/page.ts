export interface Page<T> {
  readonly page: number
  readonly size: number
  readonly totalCount: number
  readonly items: readonly T[]
}
