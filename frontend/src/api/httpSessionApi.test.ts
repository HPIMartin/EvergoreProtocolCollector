import { describe, expect, it } from 'vitest'

import { MalformedResponse, RequestFailed, Unauthorized } from './apiErrors.ts'
import type { HttpGet } from './httpProtocolApi.ts'
import type { HttpPost } from './httpLoginApi.ts'
import { httpSessionApi } from './httpSessionApi.ts'

interface Posted {
  readonly url: string
  readonly body: string
}

function answeringGet(status: number, body: string | null): HttpGet {
  return () => Promise.resolve(new Response(body, { status }))
}

function recordingPost(status: number): {
  readonly posted: Posted[]
  readonly post: HttpPost
} {
  const posted: Posted[] = []

  return {
    posted,
    post: (url, body) => {
      posted.push({ url, body })

      return Promise.resolve(new Response(null, { status }))
    },
  }
}

function answeringPost(status: number, body: string | null): HttpPost {
  return () => Promise.resolve(new Response(body, { status }))
}

const NEVER_POSTED = recordingPost(204).post
const NEVER_GOT = answeringGet(500, null)

describe('the HTTP session API', () => {
  it('reads who is logged in and whether the password can be changed', async () => {
    const tested = httpSessionApi(
      answeringGet(200, '{"name":"Aurora","canChangePassword":true}'),
      NEVER_POSTED,
    )

    const me = await tested.me()

    expect(me).toEqual({ name: 'Aurora', canChangePassword: true })
  })

  it('reports a refused login', async () => {
    const tested = httpSessionApi(answeringGet(401, null), NEVER_POSTED)

    const reading = tested.me()

    await expect(reading).rejects.toBeInstanceOf(Unauthorized)
  })

  it('reports any other failure', async () => {
    const tested = httpSessionApi(answeringGet(500, null), NEVER_POSTED)

    const reading = tested.me()

    await expect(reading).rejects.toBeInstanceOf(RequestFailed)
  })

  it.each([
    ['no JSON', 'not json'],
    ['no name', '{"canChangePassword":true}'],
    ['no flag', '{"name":"Aurora"}'],
    ['a name that is no text', '{"name":1,"canChangePassword":true}'],
  ])('rejects an answer with %s', async (_, body) => {
    const tested = httpSessionApi(answeringGet(200, body), NEVER_POSTED)

    const reading = tested.me()

    await expect(reading).rejects.toBeInstanceOf(MalformedResponse)
  })

  it('posts the logout without a body', async () => {
    const service = recordingPost(204)
    const tested = httpSessionApi(NEVER_GOT, service.post)

    await tested.logOut()

    expect(service.posted).toEqual([{ url: '/api/v1/logout', body: '' }])
  })

  it('reports a refused login on a logout', async () => {
    const service = recordingPost(401)
    const tested = httpSessionApi(NEVER_GOT, service.post)

    const loggingOut = tested.logOut()

    await expect(loggingOut).rejects.toBeInstanceOf(Unauthorized)
  })

  it('posts the current and the new password to change the password', async () => {
    const service = recordingPost(204)
    const tested = httpSessionApi(NEVER_GOT, service.post)

    await tested.changePassword('Sommer-2026!', 'Winter-2026!')

    expect(service.posted).toEqual([
      {
        url: '/api/v1/password',
        body: JSON.stringify({
          currentPassword: 'Sommer-2026!',
          newPassword: 'Winter-2026!',
        }),
      },
    ])
  })

  it('reports a change the service accepted', async () => {
    const tested = httpSessionApi(NEVER_GOT, recordingPost(204).post)

    const answer = await tested.changePassword('Sommer-2026!', 'Winter-2026!')

    expect(answer).toBe('changed')
  })

  it.each([
    ['a wrong current password', 'wrong-current-password'],
    ['a new password that breaks the rule', 'breaks-the-rule'],
    ['a new password that is the current one', 'same-as-before'],
  ])('reports %s the service refused', async (_, refusal) => {
    const tested = httpSessionApi(
      NEVER_GOT,
      answeringPost(400, JSON.stringify({ refusal })),
    )

    const answer = await tested.changePassword('Sommer-2026!', 'Winter-2026!')

    expect(answer).toBe(refusal)
  })

  it('rejects a refusal it does not know', async () => {
    const tested = httpSessionApi(
      NEVER_GOT,
      answeringPost(400, '{"refusal":"unheard-of"}'),
    )

    const changing = tested.changePassword('Sommer-2026!', 'Winter-2026!')

    await expect(changing).rejects.toBeInstanceOf(MalformedResponse)
  })

  it('reports a change the service did not accept', async () => {
    const tested = httpSessionApi(NEVER_GOT, answeringPost(500, null))

    const changing = tested.changePassword('Sommer-2026!', 'Winter-2026!')

    await expect(changing).rejects.toBeInstanceOf(RequestFailed)
  })

  it('reports a refused login on a change', async () => {
    const tested = httpSessionApi(NEVER_GOT, answeringPost(401, null))

    const changing = tested.changePassword('Sommer-2026!', 'Winter-2026!')

    await expect(changing).rejects.toBeInstanceOf(Unauthorized)
  })

  it('reports a login that has no account to change the password of', async () => {
    const tested = httpSessionApi(
      NEVER_GOT,
      answeringPost(403, '{"refusal":"no-account"}'),
    )

    const answer = await tested.changePassword('Sommer-2026!', 'Winter-2026!')

    expect(answer).toBe('no-account')
  })

  it('reports a change the service throttled as too many attempts', async () => {
    const tested = httpSessionApi(NEVER_GOT, answeringPost(429, null))

    const answer = await tested.changePassword('Sommer-2026!', 'Winter-2026!')

    expect(answer).toBe('too-many-attempts')
  })

  it('reports a refusal it does not know on a change as a failure', async () => {
    const tested = httpSessionApi(
      NEVER_GOT,
      answeringPost(403, '{"refusal":"unheard-of"}'),
    )

    const changing = tested.changePassword('Sommer-2026!', 'Winter-2026!')

    await expect(changing).rejects.toBeInstanceOf(RequestFailed)
  })

  it('reports a logout the service did not accept', async () => {
    const service = recordingPost(500)
    const tested = httpSessionApi(NEVER_GOT, service.post)

    const loggingOut = tested.logOut()

    await expect(loggingOut).rejects.toBeInstanceOf(RequestFailed)
  })
})
