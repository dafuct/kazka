import type { Story, PageResponse, UpdateStoryRequest } from './types'

const BASE = '/api/stories'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...init?.headers },
    ...init,
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`HTTP ${res.status}: ${text}`)
  }
  return res.json() as Promise<T>
}

export const api = {
  listStories(page = 0, size = 20): Promise<PageResponse<Story>> {
    return request(`${BASE}?page=${page}&size=${size}`)
  },

  getStory(id: string): Promise<Story> {
    return request(`${BASE}/${id}`)
  },

  updateStory(id: string, body: UpdateStoryRequest): Promise<Story> {
    return request(`${BASE}/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    })
  },

  deleteStory(id: string): Promise<void> {
    return fetch(`${BASE}/${id}`, { method: 'DELETE' }).then(() => undefined)
  },

  illustrate(id: string): Promise<void> {
    return fetch(`${BASE}/${id}/illustrate`, { method: 'POST' }).then(() => undefined)
  },
}
