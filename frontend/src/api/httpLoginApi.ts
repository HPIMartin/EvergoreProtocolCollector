import type { LoginAnswer } from '../domain'

import { MalformedResponse, RequestFailed } from './apiErrors.ts'

export type HttpPost = (url: string, body: string) => Promise<Response>

export interface LoginApi {
  readonly logIn: (name: string, password: string) => Promise<LoginAnswer>
}

const LOGIN = '/api/v1/login'
const UNAUTHORIZED = 401
const TOO_MANY_REQUESTS = 429
const STEPS: readonly LoginAnswer[] = ['logged-in', 'choose-new-password']

export function httpLoginApi(post: HttpPost): LoginApi {
  async function answerOf(response: Response): Promise<LoginAnswer> {
    if (response.status === UNAUTHORIZED) {
      return 'wrong-name-or-password'
    }
    if (response.status === TOO_MANY_REQUESTS) {
      return 'too-many-attempts'
    }
    if (!response.ok) {
      throw new RequestFailed(response.status)
    }

    return stepOf(await bodyOf(response))
  }

  return {
    logIn: async (name, password) =>
      answerOf(await post(LOGIN, JSON.stringify({ name, password }))),
  }
}

async function bodyOf(response: Response): Promise<unknown> {
  try {
    return await response.json()
  } catch {
    throw new MalformedResponse('The login answered a body that is not JSON')
  }
}

function stepOf(body: unknown): LoginAnswer {
  const step =
    typeof body === 'object' && body !== null && 'step' in body
      ? body.step
      : null
  const known = STEPS.find((candidate) => candidate === step)
  if (known === undefined) {
    throw new MalformedResponse('The login answered no step it knows')
  }

  return known
}
