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
