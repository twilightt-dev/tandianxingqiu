import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ImageUploader from './ImageUploader.vue'

const { uploadBlogImage, deleteBlogImage } = vi.hoisted(() => ({ uploadBlogImage: vi.fn(), deleteBlogImage: vi.fn() }))
vi.mock('@/api/upload', () => ({ uploadBlogImage, deleteBlogImage, normalizeBlogImageUrl: (path: string) => path.startsWith('/imgs/') ? path : `/imgs/${path.replace(/^\/+/, '')}` }))

describe('ImageUploader', () => {
  beforeEach(() => { uploadBlogImage.mockReset(); deleteBlogImage.mockReset() })

  async function chooseFile(wrapper: ReturnType<typeof mount>, file: File) {
    const input = wrapper.get('input[type="file"]')
    Object.defineProperty(input.element, 'files', { configurable: true, value: [file] })
    await input.trigger('change')
  }

  it('上传成功后才更新模型，并将后端相对路径规范为图片 URL', async () => {
    uploadBlogImage.mockResolvedValue('/blogs/2/3/coffee.jpg')
    const wrapper = mount(ImageUploader, { props: { modelValue: [] } })
    const file = new File(['image'], 'coffee.jpg', { type: 'image/jpeg' })

    await chooseFile(wrapper, file)
    await flushPromises()

    expect(uploadBlogImage).toHaveBeenCalledWith(file)
    expect(wrapper.emitted('update:modelValue')).toEqual([[['/imgs/blogs/2/3/coffee.jpg']]])
  })

  it('上传失败时不提前改变模型', async () => {
    uploadBlogImage.mockRejectedValue(new Error('network'))
    const wrapper = mount(ImageUploader, { props: { modelValue: [] } })
    const file = new File(['image'], 'coffee.jpg', { type: 'image/jpeg' })

    await chooseFile(wrapper, file)
    await flushPromises()

    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('服务端删除失败时保留已上传图片', async () => {
    deleteBlogImage.mockRejectedValue(new Error('network'))
    const wrapper = mount(ImageUploader, { props: { modelValue: ['/imgs/blogs/2/3/coffee.jpg'] } })

    await wrapper.get('[data-test="remove-image-0"]').trigger('click')
    await flushPromises()

    expect(deleteBlogImage).toHaveBeenCalledWith('/imgs/blogs/2/3/coffee.jpg')
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('删除控件保留 44px 触控尺寸', () => {
    const wrapper = mount(ImageUploader, { props: { modelValue: ['/imgs/blogs/2/3/coffee.jpg'] } })
    const button = wrapper.get('[data-test="remove-image-0"]')

    expect(getComputedStyle(button.element).width).toBe('44px')
    expect(getComputedStyle(button.element).height).toBe('44px')
  })
})
