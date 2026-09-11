import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from './http'
import { login, logout, register, sendCode, sendRegisterCode } from './user'

describe('用户认证 API 契约', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('使用后端实际路径发送登录和注册验证码', async () => {
    const post = vi.spyOn(http, 'post').mockResolvedValue(undefined)
    await sendCode('13812345678')
    await sendRegisterCode('13812345678')
    expect(post).toHaveBeenNthCalledWith(1, '/user/login/code', null, { params: { phone: '13812345678' } })
    expect(post).toHaveBeenNthCalledWith(2, '/user/register/code', null, { params: { phone: '13812345678' } })
  })

  it('登录、注册和退出请求与 DTO 对齐', async () => {
    const post = vi.spyOn(http, 'post').mockResolvedValue(undefined)
    const loginForm = { phone: '13812345678', loginType: 'password' as const, password: 'Test123456' }
    const registerForm = { phone: '13812345678', verifyCode: '123456', password: 'Test123456', confirmPassword: 'Test123456' }
    await login(loginForm)
    await register(registerForm)
    await logout('refresh-token')
    expect(post).toHaveBeenNthCalledWith(1, '/user/login', loginForm)
    expect(post).toHaveBeenNthCalledWith(2, '/user/register', registerForm)
    expect(post).toHaveBeenNthCalledWith(3, '/user/logout', { refreshToken: 'refresh-token' })
  })
})
