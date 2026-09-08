export function resolveLegacyLocation(pathname: string, search: string): string | null {
  const query = new URLSearchParams(search)

  switch (pathname) {
    case '/index.html':
      return '/'
    case '/login.html':
      return '/login'
    case '/login2.html':
      return '/login/password'
    case '/shop-list.html': {
      const serializedQuery = query.toString()
      return serializedQuery ? `/shops?${serializedQuery}` : '/shops'
    }
    case '/shop-detail.html':
      return toDetailRoute('/shops', query.get('id'))
    case '/blog-detail.html':
      return toDetailRoute('/blogs', query.get('id'))
    case '/blog-edit.html':
      return '/publish'
    case '/info.html':
      return '/me'
    case '/info-edit.html':
      return '/me/edit'
    case '/other-info.html':
      return toDetailRoute('/users', query.get('id'))
    default:
      return null
  }
}

function toDetailRoute(prefix: string, id: string | null): string | null {
  return id ? `${prefix}/${encodeURIComponent(id)}` : null
}
