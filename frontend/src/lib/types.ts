export type IllustrationStatus = 'PENDING' | 'READY' | 'FAILED'

export interface Story {
  id: string
  title: string
  theme: string
  characters: string[]
  ageGroup: string
  length: string
  language: string
  content: string
  illustrationPathLight: string | null
  illustrationPathDark: string | null
  illustrationStatus: IllustrationStatus
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface GenerationRequest {
  theme: string
  characters: string[]
  ageGroup: '3-5' | '6-8' | '9-12'
  length: 'short' | 'medium' | 'long'
  language: 'uk' | 'en'
}

export interface UpdateStoryRequest {
  title: string
  content: string
}

export type UserRole = 'USER' | 'ADMIN'

export interface User {
  id: string
  email: string
  displayName: string
  role: UserRole
  emailVerified: boolean
  googleLinked: boolean
  suspended: boolean
}

export type AuthErrorCode =
  | 'INVALID_CREDENTIALS'
  | 'EMAIL_TAKEN'
  | 'EMAIL_NOT_VERIFIED'
  | 'TOKEN_INVALID'
  | 'MAIL_SEND_FAILED'
  | 'VALIDATION'
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'ACCOUNT_SUSPENDED'
  | 'ERROR'

export interface ApiErrorBody {
  error: AuthErrorCode | string
  message?: string
  fields?: Record<string, string>
}

export class ApiError extends Error {
  status: number
  body: ApiErrorBody
  constructor(status: number, body: ApiErrorBody) {
    super(body.error)
    this.status = status
    this.body = body
  }
}

export type ModerationErrorCode = 'BLOCKED_INPUT' | 'JUDGE_UNAVAILABLE'
