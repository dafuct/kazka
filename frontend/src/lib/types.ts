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
  ModerationCategory,
  ApiErrorBody,
  ChildProfileDto,
  CharacterDto,
  CreateChildProfileRequest,
  UpdateChildProfileRequest,
  ExtractedCandidateDto,
  ConfirmCharactersRequest,
  UpdateCharacterRequest,
  BedtimeScheduleDto,
  BedtimeUpdateRequest,
  HolidayDto,
  BranchingStartRequest,
  BranchingChoiceRequest,
  BranchingResponse,
  BranchingChoice,
  TranslateRequest,
  Dashboard,
  DashboardAggregates,
  ChildSummary,
  RedeemGiftRequest,
  RedemptionResult,
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
  source: 'APPLE' | 'MONOBANK' | 'GIFT'
}

export interface GeoResponse {
  country: string
  isUkraine: boolean
}

export type ProviderName = 'monobank'

export interface CheckoutSessionResponse {
  provider: ProviderName
  checkoutUrl: string | null
  providerReference: string | null
}
