import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'
import axios from 'axios'
import { http, setUnauthorizedCallback } from '@/api/http'

const api = vi.hoisted(() => ({
  login: vi.fn(), logout: vi.fn(), getCurrentUser: vi.fn(),
}))
vi.mock('../api/user', () => api)

describe('auth store', () => {
  beforeEach(() => { sessionStorage.clear(); setActivePinia(createPinia()); vi.clearAllMocks() })

  it('restores token and cached user, removing malformed JSON', () => {
    sessionStorage.setItem('token', 'abc'); sessionStorage.setItem('userInfo', '{bad')
    const store = useAuthStore(); store.restore()
    expect(store.token).toBe('abc'); expect(store.user).toBeNull(); expect(sessionStorage.getItem('userInfo')).toBeNull()
  })

  it('signs in, stores token, then fetches current user', async () => {
    api.login.mockResolvedValue('jwt'); api.getCurrentUser.mockResolvedValue({ id: 2, nickName: '星' })
    const store = useAuthStore(); await store.signIn({ phone: '1', code: '2' })
    expect(store.token).toBe('jwt'); expect(store.user?.id).toBe(2); expect(sessionStorage.getItem('token')).toBe('jwt')
  })

  it('always clears local session when logout fails', async () => {
    const store = useAuthStore(); store.setSession('jwt', { id: 1 }); api.logout.mockRejectedValue(new Error('offline'))
    await expect(store.signOut()).rejects.toThrow('offline')
    expect(store.token).toBeNull(); expect(store.user).toBeNull(); expect(sessionStorage.length).toBe(0)
  })

  it('clears the existing store memory when a bound client receives 401', async () => {
    const store = useAuthStore(); store.setSession('jwt', { id: 1 })
    setUnauthorizedCallback(store.clearSession)
    http.defaults.adapter = async (config) => {
      throw new axios.AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, { status: 401, statusText: 'Unauthorized', headers: {}, config, data: {} })
    }
    await expect(http.get('/private')).rejects.toMatchObject({ code: 401 })
    expect(store.token).toBeNull(); expect(store.user).toBeNull(); expect(store.isAuthenticated).toBe(false)
  })
})
