import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BlogCard from './BlogCard.vue'
import type { Blog } from '@/types/api'

const blog: Blog = { id: 8, title: '雨天里的巷口咖啡', name: '小林', liked: 12, isLike: true }

describe('BlogCard', () => {
  it('点击卡片主体时发出 open', async () => {
    const wrapper = mount(BlogCard, { props: { blog } })
    await wrapper.get('[data-test="blog-card-open"]').trigger('click')
    expect(wrapper.emitted('open')).toEqual([[blog]])
  })

  it('点击点赞仅发出 like，不打开详情', async () => {
    const wrapper = mount(BlogCard, { props: { blog } })
    await wrapper.get('[data-test="blog-like"]').trigger('click')
    expect(wrapper.emitted('like')).toEqual([[blog]])
    expect(wrapper.emitted('open')).toBeUndefined()
  })

  it('以 isLike 提供点赞状态', () => {
    const wrapper = mount(BlogCard, { props: { blog } })
    expect(wrapper.get('[data-test="blog-like"]').attributes('aria-pressed')).toBe('true')
  })
})
