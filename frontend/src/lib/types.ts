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
} from '@kazka/shared';

export { ApiError } from '@kazka/shared';

export interface NarrationResponse {
  status: 'NONE' | 'GENERATING' | 'READY' | 'FAILED'
  url: string | null
}
