import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as userApi from '@/api/user'
import type { User } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<User | null>(null)
  const isAuthenticated = computed(() => !!token.value)
  function restore() {
    token.value = sessionStorage.getItem('token')
    const raw = sessionStorage.getItem('userInfo')
    if (!raw) { user.value = null; return }
    try { user.value = JSON.parse(raw) as User } catch { sessionStorage.removeItem('userInfo'); user.value = null }
  }
  function setSession(newToken: string, newUser?: User) {
    token.value = newToken; sessionStorage.setItem('token', newToken)
    if (newUser !== undefined) { user.value = newUser; sessionStorage.setItem('userInfo', JSON.stringify(newUser)) }
  }
  async function fetchCurrentUser() { user.value = await userApi.getCurrentUser(); sessionStorage.setItem('userInfo', JSON.stringify(user.value)); return user.value }
  async function signIn(form: userApi.LoginForm) { const jwt = await userApi.login(form); setSession(jwt); return fetchCurrentUser() }
  async function signOut() { try { if (token.value) await userApi.logout() } finally { clearSession() } }
  function clearSession() { token.value = null; user.value = null; sessionStorage.removeItem('token'); sessionStorage.removeItem('userInfo') }
  restore()
  return { token, user, isAuthenticated, restore, setSession, fetchCurrentUser, signIn, signOut, clearSession }
})
