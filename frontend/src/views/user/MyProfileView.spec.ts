import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MyProfileView from './MyProfileView.vue'

const { fetchCurrentUser, signOut, getUserInfo, getMyBlogs, getFollowFeed, push, confirm } = vi.hoisted(() => ({
  fetchCurrentUser: vi.fn(), signOut: vi.fn(), getUserInfo: vi.fn(), getMyBlogs: vi.fn(), getFollowFeed: vi.fn(), push: vi.fn(), confirm: vi.fn(),
}))
vi.stubGlobal('confirm', confirm)
vi.mock('@/stores/auth', () => ({ useAuthStore: () => ({ fetchCurrentUser, signOut }) }))
vi.mock('@/api/user', () => ({ getUserInfo }))
vi.mock('@/api/blog', () => ({ getMyBlogs, getFollowFeed }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))

describe('MyProfileView', () => {
  beforeEach(() => {
    fetchCurrentUser.mockReset().mockResolvedValue({ id: 1, nickName: '我自己' })
    getUserInfo.mockReset().mockResolvedValue({ city: '杭州', introduce: '城市漫游' })
    getMyBlogs.mockReset().mockResolvedValue([{ id: 8, title: '我的第一篇', name: '我自己' }])
    getFollowFeed.mockReset().mockResolvedValue({ list: [], minTime: 0, offset: 0 })
    signOut.mockReset(); push.mockReset(); confirm.mockReset().mockReturnValue(true)
  })

  it('呈现当前用户的真实博客列表', async () => {
    const wrapper = mount(MyProfileView)
    await flushPromises()

    expect(getMyBlogs).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('我的第一篇')
  })

  it('退出接口失败后仍由 auth 清理会话并回到首页', async () => {
    signOut.mockRejectedValue(new Error('network'))
    const wrapper = mount(MyProfileView)
    await flushPromises()
    await wrapper.get('[data-test="logout"]').trigger('click')
    await flushPromises()

    expect(signOut).toHaveBeenCalledTimes(1)
    expect(push).toHaveBeenCalledWith({ name: 'home' })
  })
})
