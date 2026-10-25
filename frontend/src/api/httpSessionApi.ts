import type { Me } from '../domain'

import { MalformedResponse, RequestFailed, Unauthorized } from './apiErrors.ts'
import { fieldOf } from './fieldOf.ts'
import type { HttpPost } from './httpLoginApi.ts'
import type { HttpGet } from './httpProtocolApi.ts'

export interface SessionApi {
  readonly me: () => Promise<Me>
  readonly logOut: () => Promise<void>
}

const ME = '/api/v1/me'
const LOGOUT = '/api/v1/logout'
const UNAUTHORIZED = 401

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
  }
}

async function bodyOf(response: Response): Promise<unknown> {
  try {
    return await response.json()
  } catch {
    throw new MalformedResponse('The API answered a body that is not JSON')
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
