export class Unauthorized extends Error {
  constructor() {
    super('The API refused the login')
  }
}

export class Forbidden extends Error {
  constructor() {
    super('The API refused the role of the login')
  }
}

export class RequestFailed extends Error {
  constructor(status: number) {
    super(`The API answered ${String(status)}`)
  }
}

export class NoSuchPage extends Error {
  constructor() {
    super('The API refused the page number')
  }
}

export class MalformedResponse extends Error {}

export type CollectionRefusalReason = 'running' | 'too-early'

export class CollectionRefused extends Error {
  readonly reason: CollectionRefusalReason

  constructor(reason: CollectionRefusalReason) {
    super(`The API refused the start of a collection: ${reason}`)
    this.reason = reason
  }
}
