import { withCsrf } from './csrf'
import { ApiError } from './types'
import type {
  Story, PageResponse, UpdateStoryRequest, User, ApiErrorBody,
} from './types'

const STORIES = '/api/stories'
const AUTH = '/api/auth'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, withCsrf({
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    ...init,
  }))
  if (!res.ok) {
    let body: ApiErrorBody
    try { body = await res.json() } catch { body = { error: 'ERROR' } }
    throw new ApiError(res.status, body)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export const api = {
  listStories(page = 0, size = 20): Promise<PageResponse<Story>> {
    return request(`${STORIES}?page=${page}&size=${size}`)
  },
  getStory(id: string): Promise<Story> {
    return request(`${STORIES}/${id}`)
  },
  updateStory(id: string, body: UpdateStoryRequest): Promise<Story> {
    return request(`${STORIES}/${id}`, { method: 'PUT', body: JSON.stringify(body) })
  },
  deleteStory(id: string): Promise<void> {
    return request(`${STORIES}/${id}`, { method: 'DELETE' })
  },
  illustrate(id: string): Promise<void> {
    return request(`${STORIES}/${id}/illustrate`, { method: 'POST' })
  },
}

interface AuthEnvelope { user: User }

export const auth = {
  signup(email: string, password: string, displayName: string): Promise<AuthEnvelope> {
    return request(`${AUTH}/signup`, { method: 'POST', body: JSON.stringify({ email, password, displayName }) })
  },
  login(email: string, password: string): Promise<AuthEnvelope> {
    return request(`${AUTH}/login`, { method: 'POST', body: JSON.stringify({ email, password }) })
  },
  logout(): Promise<void> {
    return request(`${AUTH}/logout`, { method: 'POST' })
  },
  me(): Promise<AuthEnvelope | null> {
    return request<AuthEnvelope>(`${AUTH}/me`).catch(err => {
      if (err instanceof ApiError && err.status === 401) return null
      throw err
    })
  },
  resendVerification(): Promise<void> {
    return request(`${AUTH}/verify-email/resend`, { method: 'POST' })
  },
  passwordResetRequest(email: string): Promise<void> {
    return request(`${AUTH}/password-reset/request`, { method: 'POST', body: JSON.stringify({ email }) })
  },
  passwordResetConfirm(token: string, newPassword: string): Promise<void> {
    return request(`${AUTH}/password-reset/confirm`, { method: 'POST', body: JSON.stringify({ token, newPassword }) })
  },
}

export interface AdminUser {
  id: string
  email: string
  displayName: string
  role: 'USER' | 'ADMIN'
  emailVerified: boolean
  googleLinked: boolean
  createdAt: string
  storyCount: number
}

export const admin = {
  listUsers(): Promise<AdminUser[]> {
    return request(`/api/admin/users`)
  },
}
