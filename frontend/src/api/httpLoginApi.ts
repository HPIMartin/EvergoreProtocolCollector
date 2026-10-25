import type { LoginAnswer, NewPasswordAnswer, PasswordRefusal } from '../domain'

import { MalformedResponse, RequestFailed } from './apiErrors.ts'

export type HttpPost = (url: string, body: string) => Promise<Response>

export interface LoginApi {
  readonly logIn: (name: string, password: string) => Promise<LoginAnswer>
  readonly chooseNewPassword: (
    name: string,
    issuedPassword: string,
    newPassword: string,
  ) => Promise<NewPasswordAnswer>
}

const LOGIN = '/api/v1/login'
const NEW_PASSWORD = '/api/v1/login/new-password'
const BAD_REQUEST = 400
const UNAUTHORIZED = 401
const TOO_MANY_REQUESTS = 429
const LOGIN_STEPS: readonly LoginAnswer[] = ['logged-in', 'choose-new-password']
const NEW_PASSWORD_STEPS: readonly NewPasswordAnswer[] = ['logged-in']
const PASSWORD_REFUSALS: readonly PasswordRefusal[] = [
  'breaks-the-rule',
  'same-as-before',
]

type Refused = 'wrong-name-or-password' | 'too-many-attempts'

export function httpLoginApi(post: HttpPost): LoginApi {
  return {
    logIn: async (name, password) => {
      const response = await post(LOGIN, JSON.stringify({ name, password }))

      return (
        refusalOf(response) ??
        knownValueOf(await bodyOf(okOrThrown(response)), 'step', LOGIN_STEPS)
      )
    },
    chooseNewPassword: async (name, issuedPassword, newPassword) => {
      const response = await post(
        NEW_PASSWORD,
        JSON.stringify({ name, issuedPassword, newPassword }),
      )
      if (response.status === BAD_REQUEST) {
        return knownValueOf(
          await bodyOf(response),
          'refusal',
          PASSWORD_REFUSALS,
        )
      }

      return (
        refusalOf(response) ??
        knownValueOf(
          await bodyOf(okOrThrown(response)),
          'step',
          NEW_PASSWORD_STEPS,
        )
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

function knownValueOf<T extends string>(
  body: unknown,
  field: string,
  known: readonly T[],
): T {
  const value =
    typeof body === 'object' && body !== null
      ? Object.entries(body).find(([key]) => key === field)?.[1]
      : undefined
  const found = known.find((candidate) => candidate === value)
  if (found === undefined) {
    throw new MalformedResponse(`The login answered no ${field} it knows`)
  }

  return found
}
