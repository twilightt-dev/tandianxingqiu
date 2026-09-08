import { dataOf, http } from './http'
export const normalizeBlogImageUrl = (path: string) => {
  if (!path || /^https?:\/\//i.test(path) || path.startsWith('/imgs/')) return path
  return `/imgs/${path.replace(/^\/+/, '')}`
}
export const uploadBlogImage = (file: File) => { const form = new FormData(); form.append('file', file); return dataOf<string>(http.post<string>('/upload/blog', form)) }
export const deleteBlogImage = (path: string) => dataOf<void>(http.get('/upload/blog/delete', { params: { name: path.replace(/^\/imgs(?=\/)/, '') } }))
