import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import HomeView from './HomeView.vue'

const { getHotBlogs, getShopTypes, push, observerCallback } = vi.hoisted(() => ({ getHotBlogs: vi.fn(), getShopTypes: vi.fn(), push: vi.fn(), observerCallback: { value: undefined as undefined | ((entries: IntersectionObserverEntry[]) => void) } }))

vi.mock('@/api/blog', () => ({ getHotBlogs, getBlog: vi.fn(), likeBlog: vi.fn() }))
vi.mock('@/api/shop', () => ({ getShopTypes }))
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn() } }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))

class TestIntersectionObserver {
  constructor(callback: (entries: IntersectionObserverEntry[]) => void) { observerCallback.value = callback }
  observe() {}
  disconnect() {}
  root = null; rootMargin = ''; thresholds = []
  takeRecords() { return [] }
  unobserve() {}
}

describe('HomeView', () => {
  beforeEach(() => {
    vi.stubGlobal('IntersectionObserver', TestIntersectionObserver)
    getShopTypes.mockReset().mockResolvedValue([{ id: 1, name: '咖啡' }])
    getHotBlogs.mockReset().mockResolvedValueOnce([{ id: 1, title: '第一篇', name: '小林' }]).mockRejectedValueOnce(new Error('网络异常'))
    push.mockReset(); observerCallback.value = undefined
  })

  it('下一页加载失败时保留博客并显示可重试错误', async () => {
    const wrapper = mount(HomeView)
    await flushPromises()
    observerCallback.value?.([{ isIntersecting: true } as IntersectionObserverEntry])
    await flushPromises()
    expect(wrapper.text()).toContain('热门博客加载失败，请稍后重试。')
    const retry = wrapper.get('.page-state .state-retry')
    await retry.trigger('click')
    expect(getHotBlogs).toHaveBeenCalledTimes(3)
  })

  it('将搜索保留为可见且禁用的暂未开放入口', async () => {
    const wrapper = mount(HomeView)
    await flushPromises()
    const entry = wrapper.get('.search-entry')
    expect(entry.attributes('disabled')).toBeDefined()
    expect(entry.text()).toContain('暂未开放')
  })
})
