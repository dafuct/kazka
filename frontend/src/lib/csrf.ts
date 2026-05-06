export function readCsrfCookie(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)
  return match ? decodeURIComponent(match[1]) : null
}

export function withCsrf(init: RequestInit = {}): RequestInit {
  const method = (init.method ?? 'GET').toUpperCase()
  const safe = method === 'GET' || method === 'HEAD' || method === 'OPTIONS'
  const headers = new Headers(init.headers)
  if (!safe) {
    const token = readCsrfCookie()
    if (token) headers.set('X-XSRF-TOKEN', token)
  }
  return { ...init, headers, credentials: 'include' }
}
