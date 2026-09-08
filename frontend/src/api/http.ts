import axios, { type AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResult } from '@/types/api'

export class ApiError extends Error {
  code?: number
  constructor(message: string, code?: number) { super(message); this.name = 'ApiError'; this.code = code }
}

export function unwrapApiResult<T>(result: ApiResult<T>): T {
  if (result.code !== 1) throw new ApiError(result.msg || '请求失败', result.code)
  return result.data as T
}

export function createHttpClient(onUnauthorized?: () => void): AxiosInstance {
  const client = axios.create({ baseURL: '/api', timeout: 5000 })
  client.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    const token = sessionStorage.getItem('token')
    if (token) config.headers.set('Authorization', `Bearer ${token}`)
    return config
  })
  client.interceptors.response.use(
    response => unwrapApiResult(response.data as ApiResult<unknown>) as any,
    (error: AxiosError) => {
      const status = error.response?.status
      if (status === 401) {
        sessionStorage.removeItem('token'); sessionStorage.removeItem('userInfo'); onUnauthorized?.()
        return Promise.reject(new ApiError('登录状态已失效，请重新登录', 401))
      }
      if (error instanceof ApiError) return Promise.reject(error)
      const message = error.response?.data && typeof error.response.data === 'object' && 'msg' in error.response.data
        ? String((error.response.data as { msg?: string }).msg || '请求失败')
        : (error.message || '网络异常，请稍后重试')
      return Promise.reject(new ApiError(message, status))
    },
  )
  return client
}

let unauthorizedHandler: (() => void) | undefined
/** Bind once after Pinia is created so 401 also clears reactive auth state. */
export function setUnauthorizedCallback(handler?: () => void) { unauthorizedHandler = handler }
/** @deprecated Use setUnauthorizedCallback; retained for callers created before Task 3 wiring. */
export const setUnauthorizedHandler = setUnauthorizedCallback
export let http: AxiosInstance = createHttpClient(() => unauthorizedHandler?.())

/** Domain APIs expose the unwrapped response body while retaining AxiosInstance's contract. */
export const dataOf = <T>(request: Promise<unknown>) => request as Promise<T>
