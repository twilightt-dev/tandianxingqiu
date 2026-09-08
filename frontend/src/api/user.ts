import { dataOf, http } from './http'
import type { User, UserInfo } from '@/types/api'
export interface LoginForm { phone: string; code?: string; password?: string }
export const sendCode = (phone: string) => dataOf<void>(http.post('/user/code', null, { params: { phone } }))
export const login = (form: LoginForm) => dataOf<string>(http.post<string>('/user/login', form))
export const logout = () => dataOf<void>(http.post('/user/logout'))
export const getCurrentUser = () => dataOf<User>(http.get<User>('/user/me'))
export const getUser = (id: number) => dataOf<User>(http.get<User>(`/user/${id}`))
export const getUserInfo = (id: number) => dataOf<UserInfo>(http.get<UserInfo>(`/user/info/${id}`))
