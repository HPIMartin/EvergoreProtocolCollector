import type { ChangePasswordAnswer, Me, PasswordRefusal } from '../domain'

import { MalformedResponse, RequestFailed, Unauthorized } from './apiErrors.ts'
import { fieldOf } from './fieldOf.ts'
import type { HttpGet, HttpPost } from './httpProtocolApi.ts'

export interface SessionApi {
  readonly me: () => Promise<Me>
  readonly logOut: () => Promise<void>
  readonly changePassword: (
    currentPassword: string,
    newPassword: string,
  ) => Promise<ChangePasswordAnswer>
}

const ME = '/api/v1/me'
const LOGOUT = '/api/v1/logout'
const PASSWORD = '/api/v1/password'
const UNAUTHORIZED = 401
const BAD_REQUEST = 400
const FORBIDDEN = 403
const TOO_MANY_REQUESTS = 429
const WRONG_CURRENT_PASSWORD = 'wrong-current-password'
const NO_ACCOUNT = 'no-account'
const PASSWORD_REFUSALS: readonly PasswordRefusal[] = [
  'breaks-the-rule',
  'same-as-before',
]

export function httpSessionApi(get: HttpGet, post: HttpPost): SessionApi {
  return {
    me: async () => {
      const response = await get(ME)
      if (response.status === UNAUTHORIZED) {
        throw new Unauthorized()
      }
      if (!response.ok) {
        throw new RequestFailed(response.status)
      }

      return meFrom(await bodyOf(response))
    },
    logOut: async () => {
      const response = await post(LOGOUT, '')
      if (response.status === UNAUTHORIZED) {
        throw new Unauthorized()
      }
      if (!response.ok) {
        throw new RequestFailed(response.status)
      }
    },
    changePassword: async (currentPassword, newPassword) => {
      const response = await post(
        PASSWORD,
        JSON.stringify({ currentPassword, newPassword }),
      )
      if (response.status === BAD_REQUEST) {
        return refusalFrom(await bodyOf(response))
      }
      if (response.status === UNAUTHORIZED) {
        throw new Unauthorized()
      }
      if (response.status === FORBIDDEN && (await hasNoAccount(response))) {
        return NO_ACCOUNT
      }
      if (response.status === TOO_MANY_REQUESTS) {
        return 'too-many-attempts'
      }
      if (!response.ok) {
        throw new RequestFailed(response.status)
      }

      return 'changed'
    },
  }
}

async function bodyOf(response: Response): Promise<unknown> {
  try {
    return await response.json()
  } catch {
    throw new MalformedResponse('The API answered a body that is not JSON')
  }
}

async function hasNoAccount(response: Response): Promise<boolean> {
  try {
    return fieldOf(await response.json(), 'refusal') === NO_ACCOUNT
  } catch {
    return false
  }
}

function meFrom(body: unknown): Me {
  const name = fieldOf(body, 'name')
  const canChangePassword = fieldOf(body, 'canChangePassword')
  if (typeof name !== 'string' || typeof canChangePassword !== 'boolean') {
    throw new MalformedResponse('The API answered no name and no flag')
  }

  return { name, canChangePassword }
}

function refusalFrom(body: unknown): ChangePasswordAnswer {
  const refusal = fieldOf(body, 'refusal')
  if (refusal === WRONG_CURRENT_PASSWORD) {
    return WRONG_CURRENT_PASSWORD
  }
  const found = PASSWORD_REFUSALS.find((candidate) => candidate === refusal)
  if (found === undefined) {
    throw new MalformedResponse('The API answered no refusal it knows')
  }

  return found
}
