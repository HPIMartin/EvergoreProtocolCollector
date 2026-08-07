export class Unauthorized extends Error {
  constructor() {
    super('The API refused the token')
  }
}

export class RequestFailed extends Error {
  constructor(status: number) {
    super(`The API answered ${String(status)}`)
  }
}

export class MalformedResponse extends Error {}
