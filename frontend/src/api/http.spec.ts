import { describe, expect, it, vi, beforeEach } from 'vitest'
import axios from 'axios'
import { ApiError, createHttpClient, unwrapApiResult } from './http'

describe('http client', () => {
  beforeEach(() => sessionStorage.clear())

  it('unwraps successful ApiResult and rejects failed result', () => {
    expect(unwrapApiResult({ code: 1, data: { ok: 1 } })).toEqual({ ok: 1 })
    expect(() => unwrapApiResult({ code: 0, msg: '失败' })).toThrow('失败')
  })

  it('reads token for every request and adds bearer header', async () => {
    const client = createHttpClient()
    const seen: string[] = []
    client.defaults.adapter = async (config) => {
      seen.push(String(config.headers?.Authorization ?? ''))
      return { data: { code: 1, data: 'ok' }, status: 200, statusText: 'OK', headers: {}, config }
    }
    sessionStorage.setItem('token', 'first')
    await client.get('/a')
    sessionStorage.setItem('token', 'second')
    await client.get('/b')
    expect(seen).toEqual(['Bearer first', 'Bearer second'])
  })

  it('handles 401 by clearing session and invoking callback', async () => {
    const unauthorized = vi.fn()
    const client = createHttpClient(unauthorized)
    client.defaults.adapter = async (config) => {
      throw new axios.AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, { status: 401, statusText: 'Unauthorized', headers: {}, config, data: {} })
    }
    sessionStorage.setItem('token', 't'); sessionStorage.setItem('userInfo', '{}')
    await expect(client.get('/private')).rejects.toMatchObject({ message: '登录状态已失效，请重新登录', code: 401 })
    expect(sessionStorage.getItem('token')).toBeNull()
    expect(sessionStorage.getItem('userInfo')).toBeNull()
    expect(unauthorized).toHaveBeenCalledOnce()
  })

  it('uses the unified msg field for non-401 HTTP errors', async () => {
    const client = createHttpClient()
    client.defaults.adapter = async (config) => {
      throw new axios.AxiosError('Bad Request', 'ERR_BAD_REQUEST', config, undefined, {
        status: 400,
        statusText: 'Bad Request',
        headers: {},
        config,
        data: { code: 0, msg: '参数错误' },
      })
    }

    await expect(client.get('/bad')).rejects.toMatchObject({ message: '参数错误', code: 400 })
  })
})
