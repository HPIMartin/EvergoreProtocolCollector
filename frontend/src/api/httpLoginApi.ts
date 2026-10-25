import type {
  LoggedIn,
  LoginAnswer,
  NewPasswordAnswer,
  PasswordRefusal,
} from '../domain'

import { MalformedResponse, RequestFailed } from './apiErrors.ts'

export type HttpPost = (url: string, body: string) => Promise<Response>

export interface LoginApi {
  readonly logIn: (
    name: string,
    password: string,
    next: string | null,
  ) => Promise<LoginAnswer>
  readonly chooseNewPassword: (
    name: string,
    issuedPassword: string,
    newPassword: string,
    next: string | null,
  ) => Promise<NewPasswordAnswer>
}

const LOGIN = '/api/v1/login'
const NEW_PASSWORD = '/api/v1/login/new-password'
const BAD_REQUEST = 400
const UNAUTHORIZED = 401
const TOO_MANY_REQUESTS = 429
const CHOOSE_NEW_PASSWORD = 'choose-new-password'
const PASSWORD_REFUSALS: readonly PasswordRefusal[] = [
  'breaks-the-rule',
  'same-as-before',
]

type Refused = 'wrong-name-or-password' | 'too-many-attempts'

export function httpLoginApi(post: HttpPost): LoginApi {
  return {
    logIn: async (name, password, next) => {
      const response = await post(
        LOGIN,
        JSON.stringify({ name, password, next: next ?? undefined }),
      )
      const refused = refusalOf(response)
      if (refused !== null) {
        return refused
      }
      const body = await bodyOf(okOrThrown(response))

      return fieldOf(body, 'step') === CHOOSE_NEW_PASSWORD
        ? CHOOSE_NEW_PASSWORD
        : loggedInFrom(body)
    },
    chooseNewPassword: async (name, issuedPassword, newPassword, next) => {
      const response = await post(
        NEW_PASSWORD,
        JSON.stringify({
          name,
          issuedPassword,
          newPassword,
          next: next ?? undefined,
        }),
      )
      if (response.status === BAD_REQUEST) {
        return passwordRefusalFrom(await bodyOf(response))
      }

      return (
        refusalOf(response) ?? loggedInFrom(await bodyOf(okOrThrown(response)))
      )
    },
  }
}

function refusalOf(response: Response): Refused | null {
  if (response.status === UNAUTHORIZED) {
    return 'wrong-name-or-password'
  }
  if (response.status === TOO_MANY_REQUESTS) {
    return 'too-many-attempts'
  }

  return null
}

function okOrThrown(response: Response): Response {
  if (!response.ok) {
    throw new RequestFailed(response.status)
  }

  return response
}

async function bodyOf(response: Response): Promise<unknown> {
  try {
    return await response.json()
  } catch {
    throw new MalformedResponse('The login answered a body that is not JSON')
  }
}

function fieldOf(body: unknown, field: string): unknown {
  return typeof body === 'object' && body !== null
    ? Object.entries(body).find(([key]) => key === field)?.[1]
    : undefined
}

function loggedInFrom(body: unknown): LoggedIn {
  const next = fieldOf(body, 'next')
  if (fieldOf(body, 'step') !== 'logged-in' || typeof next !== 'string') {
    throw new MalformedResponse('The login answered no step it knows')
  }

  return { step: 'logged-in', next }
}

function passwordRefusalFrom(body: unknown): PasswordRefusal {
  const found = PASSWORD_REFUSALS.find(
    (candidate) => candidate === fieldOf(body, 'refusal'),
  )
  if (found === undefined) {
    throw new MalformedResponse('The login answered no refusal it knows')
  }

  return found
}
