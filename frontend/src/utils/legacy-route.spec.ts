import { describe, expect, it } from 'vitest'
import { resolveLegacyLocation } from './legacy-route'

describe('resolveLegacyLocation', () => {
  it.each([
    ['/index.html', '', '/'],
    ['/login.html', '', '/login'],
    ['/login2.html', '', '/login/password'],
    ['/shop-list.html', '?type=1&name=美食', '/shops?type=1&name=%E7%BE%8E%E9%A3%9F'],
    ['/shop-detail.html', '?id=7', '/shops/7'],
    ['/blog-detail.html', '?id=8', '/blogs/8'],
    ['/blog-edit.html', '', '/publish'],
    ['/info.html', '', '/me'],
    ['/info-edit.html', '', '/me/edit'],
    ['/other-info.html', '?id=9', '/users/9'],
    ['/shop-detail.html', '', null],
    ['/unknown.html', '', null],
    ['/shops', '', null],
  ] as const)('maps %s%s to %s', (pathname, search, expected) => {
    expect(resolveLegacyLocation(pathname, search)).toBe(expected)
  })
})
