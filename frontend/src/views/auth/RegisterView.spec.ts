import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import RegisterView from './RegisterView.vue'

const { register, sendRegisterCode, push } = vi.hoisted(() => ({ register: vi.fn(), sendRegisterCode: vi.fn(), push: vi.fn() }))
vi.mock('@/api/user', () => ({ register, sendRegisterCode }))
vi.mock('vue-router', async (importOriginal) => ({ ...(await importOriginal<typeof import('vue-router')>()), useRouter: () => ({ push }) }))

describe('RegisterView', () => {
  beforeEach(() => { register.mockReset(); sendRegisterCode.mockReset(); push.mockReset() })

  it('密码不一致时就近提示且不提交', async () => {
    const wrapper = mount(RegisterView, { global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } } })
    await wrapper.get('[data-test="phone"]').setValue('13812345678')
    await wrapper.get('[data-test="verify-code"]').setValue('123456')
    await wrapper.get('[data-test="password"]').setValue('Test123456')
    await wrapper.get('[data-test="confirm-password"]').setValue('Test654321')
    await wrapper.get('form').trigger('submit.prevent')
    expect(wrapper.text()).toContain('两次输入的密码不一致')
    expect(register).not.toHaveBeenCalled()
  })

  it('提交与后端 RegisterDTO 一致并跳回预填手机号的登录页', async () => {
    register.mockResolvedValue(undefined)
    const wrapper = mount(RegisterView, { global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } } })
    await wrapper.get('[data-test="phone"]').setValue('13812345678')
    await wrapper.get('[data-test="verify-code"]').setValue('123456')
    await wrapper.get('[data-test="password"]').setValue('Test123456')
    await wrapper.get('[data-test="confirm-password"]').setValue('Test123456')
    await wrapper.get('form').trigger('submit.prevent')
    expect(register).toHaveBeenCalledWith({ phone: '13812345678', verifyCode: '123456', password: 'Test123456', confirmPassword: 'Test123456' })
    expect(push).toHaveBeenCalledWith({ name: 'login', query: { phone: '13812345678', registered: '1' } })
  })

  it('注册验证码仅在发送成功后开始倒计时', async () => {
    sendRegisterCode.mockResolvedValue(undefined)
    const wrapper = mount(RegisterView, { global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } } })
    await wrapper.get('[data-test="phone"]').setValue('13812345678')
    await wrapper.get('[data-test="send-code"]').trigger('click')
    await Promise.resolve()
    expect(sendRegisterCode).toHaveBeenCalledWith('13812345678')
    expect(wrapper.get('[data-test="send-code"]').text()).toContain('60 秒后重试')
  })
})
