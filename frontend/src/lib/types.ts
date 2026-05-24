// Re-export façade: frontend code imports types and ApiError from this file
// (16 call sites as of M1). The actual definitions live in @kazka/shared,
// which is generated from the backend OpenAPI spec.

export type {
  IllustrationStatus,
  Story,
  PageResponse,
  GenerationRequest,
  UpdateStoryRequest,
  UserRole,
  User,
  AuthErrorCode,
  ModerationErrorCode,
  ApiErrorBody,
  ChildProfileDto,
  CharacterDto,
  CreateChildProfileRequest,
  UpdateChildProfileRequest,
  ExtractedCandidateDto,
  ConfirmCharactersRequest,
  UpdateCharacterRequest,
} from '@kazka/shared';

export { ApiError } from '@kazka/shared';

export interface Product {
  id: string
  appleProductId: string
  name: string
  priceMicro: number
  currency: string
  period: 'P1M' | 'P1Y'
  tier: string
}

export interface Entitlement {
  productAppleId: string
  state: 'ACTIVE' | 'GRACE' | 'EXPIRED' | 'REFUNDED'
  expiresAt: string | null
  source: 'APPLE' | 'PADDLE' | 'LIQPAY' | 'MONOBANK'
}

export interface GeoResponse {
  country: string
  isUkraine: boolean
}

export type ProviderName = 'paddle' | 'liqpay' | 'monobank'

export interface CheckoutSessionResponse {
  provider: ProviderName
  checkoutUrl: string | null
  paddleTransactionId: string | null
}
