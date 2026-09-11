import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LoginView from './LoginView.vue'

const { signIn, sendCode, push } = vi.hoisted(() => ({ signIn: vi.fn(), sendCode: vi.fn(), push: vi.fn() }))

vi.mock('@/stores/auth', () => ({ useAuthStore: () => ({ signIn }) }))
vi.mock('@/api/user', () => ({ sendCode }))
vi.mock('vue-router', async (importOriginal) => ({ ...(await importOriginal<typeof import('vue-router')>()), useRoute: () => ({ query: {} }), useRouter: () => ({ push }) }))

describe('LoginView', () => {
  beforeEach(() => { setActivePinia(createPinia()); signIn.mockReset(); sendCode.mockReset(); push.mockReset() })
  const mountLogin = () => mount(LoginView, { global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } } })

  it('未同意协议时不提交', async () => {
    const wrapper = mountLogin()
    await wrapper.get('[data-test="phone"]').setValue('19112345678')
    await wrapper.get('[data-test="code"]').setValue('123456')
    await wrapper.get('form').trigger('submit.prevent')
    expect(wrapper.text()).toContain('请先同意用户协议')
    expect(signIn).not.toHaveBeenCalled()
  })

  it('无效手机号时不提交', async () => {
    const wrapper = mountLogin()
    await wrapper.get('[data-test="phone"]').setValue('123')
    await wrapper.get('[data-test="code"]').setValue('123456')
    await wrapper.get('[data-test="agreement"]').setValue(true)
    await wrapper.get('form').trigger('submit.prevent')
    expect(wrapper.text()).toContain('请输入有效的中国大陆手机号')
    expect(signIn).not.toHaveBeenCalled()
  })

  it('将字段错误关联到无效控件，并提供 44px 协议触控区', async () => {
    const wrapper = mountLogin()
    await wrapper.get('[data-test="phone"]').setValue('123')
    await wrapper.get('[data-test="code"]').setValue('x')
    await wrapper.get('form').trigger('submit.prevent')
    expect(wrapper.get('[data-test="phone"]').attributes('aria-invalid')).toBe('true')
    expect(wrapper.get('[data-test="phone"]').attributes('aria-describedby')).toBe('phone-error')
    expect(wrapper.get('[data-test="code"]').attributes('aria-describedby')).toBe('code-error')
    expect(wrapper.get('[data-test="agreement"]').attributes('aria-describedby')).toBe('agreement-error')
    expect(getComputedStyle(wrapper.get('.agreement').element).minHeight).toBe('44px')
  })

  it('合法表单调用登录', async () => {
    signIn.mockResolvedValue(undefined)
    const wrapper = mountLogin()
    await wrapper.get('[data-test="phone"]').setValue('19112345678')
    await wrapper.get('[data-test="code"]').setValue('123456')
    await wrapper.get('[data-test="agreement"]').setValue(true)
    await wrapper.get('form').trigger('submit.prevent')
    expect(signIn).toHaveBeenCalledWith({ phone: '19112345678', loginType: 'code', verifyCode: '123456' })
    expect(push).toHaveBeenCalledWith('/')
  })

  it('验证码接口失败时显示服务端返回的错误信息', async () => {
    sendCode.mockRejectedValueOnce(new Error('用户不存在，请先注册'))
    const wrapper = mountLogin()
    await wrapper.get('[data-test="phone"]').setValue('13916180491')
    await wrapper.get('[data-test="send-code"]').trigger('click')
    await vi.waitFor(() => expect(wrapper.text()).toContain('用户不存在，请先注册'))
  })

  it('仅在验证码发送成功后开始倒计时', async () => {
    sendCode.mockRejectedValueOnce(new Error('网络异常'))
    const wrapper = mountLogin()
    await wrapper.get('[data-test="phone"]').setValue('13812345678')
    await wrapper.get('[data-test="send-code"]').trigger('click')
    await Promise.resolve()
    expect(wrapper.get('[data-test="send-code"]').text()).toBe('获取验证码')
    sendCode.mockResolvedValueOnce(undefined)
    await wrapper.get('[data-test="send-code"]').trigger('click')
    await Promise.resolve()
    expect(wrapper.get('[data-test="send-code"]').text()).toContain('60 秒后重试')
    expect(sendCode).toHaveBeenLastCalledWith('13812345678')
  })
})
