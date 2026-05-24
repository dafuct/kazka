import { withCsrf } from './csrf'
import { ApiError } from './types'
import type {
  Story, PageResponse, UpdateStoryRequest, User, ApiErrorBody,
  Product, Entitlement, GeoResponse, CheckoutSessionResponse, ProviderName,
  ChildProfileDto, CharacterDto, CreateChildProfileRequest, UpdateChildProfileRequest,
  ConfirmCharactersRequest, UpdateCharacterRequest, ExtractedCandidateDto,
} from './types'

const STORIES = '/api/stories'
const AUTH = '/api/auth'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, withCsrf({
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    ...init,
  }))
  if (res.status === 402) {
    const currentPath = window.location.pathname + window.location.search
    if (!currentPath.startsWith('/pricing') && !url.startsWith('/api/billing/')) {
      window.location.href = `/pricing?redirect=${encodeURIComponent(currentPath)}`
    }
    throw new ApiError(402, { error: 'PAYWALL_REQUIRED' })
  }
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
  updateProfile(displayName: string): Promise<AuthEnvelope> {
    return request(`${AUTH}/me`, { method: 'PATCH', body: JSON.stringify({ displayName }) })
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

export interface FlaggedAttemptDto {
  id: string
  userId: string
  userEmail: string
  pipeline: 'TEXT_INPUT' | 'IMAGE_SCENE'
  category: string
  language: string
  promptText: string
  confidence: number | null
  judgeModel: string | null
  createdAt: string
}

export interface SuspendedUserDto {
  id: string
  email: string
  displayName: string
  suspendedAt: string
  suspendedReason: string
  suspendedBy: string | null
}

export const adminModeration = {
  listFlagged(page = 0, size = 50): Promise<PageResponse<FlaggedAttemptDto>> {
    return request(`/api/admin/moderation/flagged?page=${page}&size=${size}`)
  },
  listSuspended(): Promise<SuspendedUserDto[]> {
    return request(`/api/admin/moderation/suspended`)
  },
  unsuspend(userId: string): Promise<void> {
    return request(`/api/admin/users/${userId}/unsuspend`, { method: 'POST' })
  },
}

const BILLING = '/api/billing'

export const billing = {
  listProducts(): Promise<Product[]> {
    return request(`${BILLING}/products`)
  },
  entitlements(): Promise<Entitlement[]> {
    return request(`${BILLING}/entitlements`)
  },
  geo(countryHint?: string): Promise<GeoResponse> {
    const q = countryHint ? `?country=${encodeURIComponent(countryHint)}` : ''
    return request(`${BILLING}/geo${q}`)
  },
  createCheckoutSession(planId: string, provider: ProviderName, countryHint?: string): Promise<CheckoutSessionResponse> {
    return request(`${BILLING}/checkout-session`, {
      method: 'POST',
      body: JSON.stringify({ planId, provider, countryHint }),
    })
  },
  cancelSubscription(): Promise<Entitlement[]> {
    return request(`${BILLING}/subscription/cancel`, { method: 'POST' })
  },
}

const CHILDREN = '/api/children'
const CHARACTERS = '/api/characters'

export const children = {
  list(): Promise<ChildProfileDto[]> {
    return request(`${CHILDREN}`)
  },
  get(id: string): Promise<ChildProfileDto> {
    return request(`${CHILDREN}/${id}`)
  },
  create(body: CreateChildProfileRequest): Promise<ChildProfileDto> {
    return request(`${CHILDREN}`, { method: 'POST', body: JSON.stringify(body) })
  },
  update(id: string, body: UpdateChildProfileRequest): Promise<ChildProfileDto> {
    return request(`${CHILDREN}/${id}`, { method: 'PATCH', body: JSON.stringify(body) })
  },
  archive(id: string): Promise<void> {
    return request(`${CHILDREN}/${id}`, { method: 'DELETE' })
  },
  listCharacters(id: string): Promise<CharacterDto[]> {
    return request(`${CHILDREN}/${id}/characters`)
  },
}

export const charactersApi = {
  update(id: string, body: UpdateCharacterRequest): Promise<CharacterDto> {
    return request(`${CHARACTERS}/${id}`, { method: 'PATCH', body: JSON.stringify(body) })
  },
  archive(id: string): Promise<void> {
    return request(`${CHARACTERS}/${id}`, { method: 'DELETE' })
  },
  confirm(childProfileId: string, body: ConfirmCharactersRequest): Promise<CharacterDto[]> {
    return request(`${CHARACTERS}/confirm?childProfileId=${encodeURIComponent(childProfileId)}`,
      { method: 'POST', body: JSON.stringify(body) })
  },
}

export const extraction = {
  candidates(storyId: string): Promise<ExtractedCandidateDto[]> {
    return request(`/api/stories/${storyId}/extraction-candidates`)
  },
  retrigger(storyId: string): Promise<void> {
    return request(`/api/stories/${storyId}/extract-characters`, { method: 'POST' })
  },
}
