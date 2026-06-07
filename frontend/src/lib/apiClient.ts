import { withCsrf } from './csrf'
import { ApiError } from './types'
import type { components } from '@kazka/shared'
import type {
  Story, PageResponse, UpdateStoryRequest, User, ApiErrorBody,
  ChildProfileDto, CharacterDto, CreateChildProfileRequest, UpdateChildProfileRequest,
  ConfirmCharactersRequest, UpdateCharacterRequest, ExtractedCandidateDto,
  BedtimeScheduleDto, BedtimeUpdateRequest,
  HolidayDto,
  BranchingStartRequest, BranchingChoiceRequest, BranchingResponse,
  TranslateRequest,
  Dashboard,
} from './types'

const STORIES = '/api/stories'
const AUTH = '/api/auth'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, withCsrf({
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    ...init,
  }))
  if (res.status === 402) {
    throw new ApiError(402, { error: 'MONTHLY_LIMIT' })
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
  listStories(page = 0, size = 20, childProfileId?: string): Promise<PageResponse<Story>> {
    const q = childProfileId ? `&childProfileId=${encodeURIComponent(childProfileId)}` : ''
    return request(`${STORIES}?page=${page}&size=${size}${q}`)
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
  retry(id: string): Promise<void> {
    return request(`${STORIES}/${id}/retry`, { method: 'POST' })
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
  setShowcase(storyId: string, on: boolean): Promise<void> {
    return request(`/api/admin/stories/${storyId}/showcase?on=${on}`, { method: 'POST' })
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
  createBatch(body: { children: CreateChildProfileRequest[] }): Promise<ChildProfileDto[]> {
    return request(`${CHILDREN}/batch`, { method: 'POST', body: JSON.stringify(body) })
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
  getBedtime(id: string): Promise<BedtimeScheduleDto> {
    return request(`${CHILDREN}/${id}/bedtime`)
  },
  updateBedtime(id: string, body: BedtimeUpdateRequest): Promise<BedtimeScheduleDto> {
    return request(`${CHILDREN}/${id}/bedtime`, { method: 'PUT', body: JSON.stringify(body) })
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
  candidates(storyId: string, lang?: string): Promise<ExtractedCandidateDto[]> {
    const qs = lang ? `?lang=${encodeURIComponent(lang)}` : ''
    return request(`/api/stories/${storyId}/extraction-candidates${qs}`)
  },
  retrigger(storyId: string): Promise<void> {
    return request(`/api/stories/${storyId}/extract-characters`, { method: 'POST' })
  },
}

const HOLIDAYS = '/api/holidays'

export const holidays = {
  async today(tz: string, lang?: string): Promise<HolidayDto | null> {
    const params = new URLSearchParams({ tz })
    if (lang) params.set('lang', lang)
    try {
      const result = await request<HolidayDto>(`${HOLIDAYS}/today?${params.toString()}`)
      // 204 No Content → request<T> returns undefined per the existing helper
      return result ?? null
    } catch (e) {
      // If 204 manifests as a different error type, surface as null
      if (e instanceof ApiError && (e.status === 204 || e.status === 404)) return null
      throw e
    }
  },
}

export const branching = {
  start(body: BranchingStartRequest): Promise<BranchingResponse> {
    return request<BranchingResponse>('/api/stories/branching', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },
  choose(storyId: string, choiceId: string): Promise<BranchingResponse> {
    return request<BranchingResponse>(`/api/stories/${storyId}/branching/choose`, {
      method: 'POST',
      body: JSON.stringify({ choiceId } satisfies BranchingChoiceRequest),
    })
  },
}

export const translation = {
  translate(storyId: string, targetLanguage: 'uk' | 'en'): Promise<Story> {
    return request<Story>(`/api/stories/${storyId}/translate`, {
      method: 'POST',
      body: JSON.stringify({ targetLanguage } satisfies TranslateRequest),
    })
  },
}

export const dashboard = {
  get(): Promise<Dashboard> {
    return request<Dashboard>('/api/dashboard', { method: 'GET' })
  },
}

// The OpenAPI-generated @kazka/shared types weren't regenerated for the public
// showcase endpoints, but the backend returns the same StoryDto shape. Alias it
// locally so the read-only reader can reuse ComicsReader (which expects StoryDto).
export type ShowcaseStoryDto = components['schemas']['StoryDto']

const PUBLIC_SHOWCASE = '/api/public/showcase'

// Public, unauthenticated read-only access to curated sample tales. These hit
// permitAll backend routes, so no Authorization/session is required. The shared
// request() helper never redirects on 401/402, so a logged-out visitor calling
// these will never be bounced into the auth/redirect flow.
export const showcase = {
  list(): Promise<ShowcaseStoryDto[]> {
    return request<ShowcaseStoryDto[]>(PUBLIC_SHOWCASE)
  },
  get(id: string): Promise<ShowcaseStoryDto> {
    return request<ShowcaseStoryDto>(`${PUBLIC_SHOWCASE}/${id}`)
  },
}
