import type { Ledger, Page } from '../domain'
import { entriesOf, unknownAvatar } from '../domain'

import { MalformedResponse, RequestFailed, Unauthorized } from './apiErrors.ts'
import type { PageWindow } from './pageWindow.ts'
import type { ProtocolApi } from './protocolApi.ts'
import { bankPageFrom, overviewFrom, storagePageFrom } from './wire.ts'

export type HttpGet = (url: string) => Promise<Response>

const AVATARS = '/api/v1/avatars'
const UNAUTHORIZED = 401
const NOT_FOUND = 404

export function httpProtocolApi(
  get: HttpGet,
  token: string | null,
): ProtocolApi {
  function urlOf(path: string, window: PageWindow): string {
    const parameters = new URLSearchParams()
    if (token !== null) {
      parameters.set('token', token)
    }
    parameters.set('page', String(window.page))
    parameters.set('size', String(window.size))

    return `${AVATARS}${path}?${parameters.toString()}`
  }

  async function bodyOf(response: Response): Promise<unknown> {
    if (response.status === UNAUTHORIZED) {
      throw new Unauthorized()
    }
    if (!response.ok) {
      throw new RequestFailed(response.status)
    }

    try {
      return await response.json()
    } catch {
      throw new MalformedResponse('The API answered a body that is not JSON')
    }
  }

  async function ledgerOf<E>(
    view: string,
    avatar: string,
    window: PageWindow,
    pageFrom: (body: unknown) => Page<E>,
  ): Promise<Ledger<E>> {
    const path = `/${encodeURIComponent(avatar)}/${view}`
    const response = await get(urlOf(path, window))
    if (response.status === NOT_FOUND) {
      return unknownAvatar<E>(avatar)
    }

    return entriesOf(pageFrom(await bodyOf(response)))
  }

  return {
    overview: async (window) =>
      overviewFrom(await bodyOf(await get(urlOf('', window)))),
    bankEntries: (avatar, window) =>
      ledgerOf('bank', avatar, window, bankPageFrom),
    storageEntries: (avatar, window) =>
      ledgerOf('storage', avatar, window, storagePageFrom),
  }
}
