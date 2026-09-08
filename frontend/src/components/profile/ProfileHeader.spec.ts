import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ProfileHeader from './ProfileHeader.vue'

describe('ProfileHeader', () => {
  const user = { id: 7, nickName: '小林', icon: 'https://img.example/avatar.jpg' }
  const info = { city: '杭州', introduce: '慢慢逛城市', fans: 12, followee: 5 }

  it('自己的主页只提供编辑和退出操作', async () => {
    const wrapper = mount(ProfileHeader, { props: { user, info, ownProfile: true, followed: false, busy: false } })

    expect(wrapper.find('[data-test="toggle-follow"]').exists()).toBe(false)
    await wrapper.get('[data-test="edit-profile"]').trigger('click')
    await wrapper.get('[data-test="logout"]').trigger('click')
    expect(wrapper.emitted('edit')).toEqual([[]])
    expect(wrapper.emitted('logout')).toEqual([[]])
  })

  it('他人主页提供带 pressed 状态的关注按钮且只发出关注事件', async () => {
    const wrapper = mount(ProfileHeader, { props: { user, info, ownProfile: false, followed: true, busy: false } })
    const follow = wrapper.get('[data-test="toggle-follow"]')

    expect(wrapper.find('[data-test="edit-profile"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="logout"]').exists()).toBe(false)
    expect(follow.attributes('aria-pressed')).toBe('true')
    await follow.trigger('click')
    expect(wrapper.emitted('toggle-follow')).toEqual([[]])
  })
})
