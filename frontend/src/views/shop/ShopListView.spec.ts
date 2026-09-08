import { flushPromises, mount } from '@vue/test-utils'
import { nextTick, reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ShopListView from './ShopListView.vue'

const { getShopTypes, getShopsByType, searchShops, push } = vi.hoisted(() => ({
  getShopTypes: vi.fn(), getShopsByType: vi.fn(), searchShops: vi.fn(), push: vi.fn(),
}))

const route = reactive({ query: { type: '1' as string | undefined, name: '美食' as string | undefined, sort: '' } })
vi.mock('@/api/shop', () => ({ getShopTypes, getShopsByType, searchShops }))
vi.mock('vue-router', () => ({ useRoute: () => route, useRouter: () => ({ push }) }))

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => { resolve = done })
  return { promise, resolve }
}

describe('ShopListView', () => {
  beforeEach(() => {
    route.query.type = '1'; route.query.name = '美食'; route.query.sort = ''
    getShopTypes.mockReset().mockResolvedValue([{ id: 1, name: '美食' }, { id: 2, name: 'KTV' }])
    getShopsByType.mockReset().mockResolvedValue([]); searchShops.mockReset(); push.mockReset()
  })

  it('在条件变化时立即发起新请求，且旧响应不能覆盖新列表', async () => {
    const oldRequest = deferred<Array<{ id: number; name: string }>>()
    const newRequest = deferred<Array<{ id: number; name: string }>>()
    getShopsByType.mockImplementationOnce(() => oldRequest.promise).mockImplementationOnce(() => newRequest.promise)
    const wrapper = mount(ShopListView)
    await nextTick()

    route.query.type = '2'; route.query.name = 'KTV'; route.query.sort = 'score'
    await nextTick()
    expect(getShopsByType).toHaveBeenCalledTimes(2)
    expect(getShopsByType).toHaveBeenLastCalledWith(expect.objectContaining({ typeId: 2, current: 1, sortBy: 'score' }))

    newRequest.resolve([{ id: 2, name: '新条件门店' }])
    await flushPromises()
    oldRequest.resolve([{ id: 1, name: '旧条件门店' }])
    await flushPromises()

    expect(wrapper.text()).toContain('新条件门店')
    expect(wrapper.text()).not.toContain('旧条件门店')
  })

  it('切换到无筛选条件时使旧请求失效并保持空状态', async () => {
    const oldRequest = deferred<Array<{ id: number; name: string }>>()
    getShopsByType.mockImplementationOnce(() => oldRequest.promise)
    const wrapper = mount(ShopListView)
    await nextTick()

    route.query.type = undefined
    route.query.name = undefined
    await nextTick()
    oldRequest.resolve([{ id: 1, name: '旧条件门店' }])
    await flushPromises()

    expect(wrapper.text()).toContain('还没有内容')
    expect(wrapper.text()).not.toContain('旧条件门店')
  })

  it('保留分类选择与距离、人气、评分排序入口', async () => {
    const wrapper = mount(ShopListView)
    await flushPromises()

    expect(wrapper.text()).toContain('距离')
    expect(wrapper.text()).toContain('人气')
    expect(wrapper.text()).toContain('评分')
    expect(wrapper.get('[aria-label="门店排序"]').attributes('role')).toBe('group')
    expect(wrapper.get('.sort-list button').attributes('aria-pressed')).toBe('true')
    await wrapper.get('[data-test="shop-type-2"]').trigger('click')
    expect(push).toHaveBeenCalledWith({ name: 'shops', query: { type: 2, name: 'KTV', sort: '' } })
  })
})
