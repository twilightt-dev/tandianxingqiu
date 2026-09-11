import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PasswordLoginView from './PasswordLoginView.vue'

const { signIn, push } = vi.hoisted(() => ({ signIn: vi.fn(), push: vi.fn() }))
vi.mock('@/stores/auth', () => ({ useAuthStore: () => ({ signIn }) }))
vi.mock('vue-router', async (importOriginal) => ({ ...(await importOriginal<typeof import('vue-router')>()), useRoute: () => ({ query: {} }), useRouter: () => ({ push }) }))

describe('PasswordLoginView', () => {
  beforeEach(() => { signIn.mockReset(); push.mockReset() })

  it('使用 password 登录类型提交手机号和密码', async () => {
    signIn.mockResolvedValue(undefined)
    const wrapper = mount(PasswordLoginView, { global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } } })
    await wrapper.get('[data-test="phone"]').setValue('19112345678')
    await wrapper.get('[data-test="password"]').setValue('Test123456')
    await wrapper.get('form').trigger('submit.prevent')
    expect(signIn).toHaveBeenCalledWith({ phone: '19112345678', loginType: 'password', password: 'Test123456' })
    expect(push).toHaveBeenCalledWith('/me')
  })
})
