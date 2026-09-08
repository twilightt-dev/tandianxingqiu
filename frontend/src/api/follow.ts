import { dataOf, http } from './http'
import type { User } from '@/types/api'
export const getFollowStatus = (userId: number) => dataOf<boolean>(http.get<boolean>(`/follow/or/not/${userId}`))
export const setFollow = (userId: number, followed: boolean) => dataOf<void>(http.put(`/follow/${userId}/${followed}`))
export const getCommonFollows = (userId: number) => dataOf<User[]>(http.get<User[]>(`/follow/common/${userId}`))
