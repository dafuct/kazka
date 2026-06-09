import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { narration } from './apiClient'

describe('narration api', () => {
  beforeEach(() => {
    // jsdom has no XSRF cookie; withCsrf reads document.cookie (empty) — fine for these tests.
    vi.stubGlobal('fetch', vi.fn(async () =>
      new Response(JSON.stringify({ status: 'GENERATING', url: null }), {
        status: 202,
        headers: { 'Content-Type': 'application/json' },
      })))
  })
  afterEach(() => vi.unstubAllGlobals())

  it('POSTs to the narration endpoint and parses the body', async () => {
    const res = await narration.request('story-1')
    expect(fetch).toHaveBeenCalledWith('/api/stories/story-1/narration',
      expect.objectContaining({ method: 'POST' }))
    expect(res.status).toBe('GENERATING')
    expect(res.url).toBeNull()
  })

  it('GETs the narration endpoint', async () => {
    await narration.get('story-1')
    expect(fetch).toHaveBeenCalledWith('/api/stories/story-1/narration', expect.anything())
  })
})
