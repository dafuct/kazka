// Friendly aliases over the generated api-types. Story, User, etc. are
// re-exported directly because the backend now emits the correct shape
// (required fields + nullable where appropriate) via @Schema annotations
// on the DTO records. No Required<> wrapping needed.

export type { components, paths } from './api-types';

export type {
  SchemaStoryDto as Story,
  SchemaUserDto as User,
  SchemaGenerationRequest as GenerationRequest,
  SchemaUpdateStoryRequest as UpdateStoryRequest,
  SchemaAuthResponse as AuthResponse,
  SchemaSignupRequest as SignupRequest,
  SchemaPasswordResetRequestRequest as PasswordResetRequestRequest,
  SchemaPasswordResetConfirmRequest as PasswordResetConfirmRequest,
  SchemaAdminUserDto as AdminUserDto,
  SchemaFlaggedAttemptDto as FlaggedAttemptDto,
  SchemaSuspendedUserDto as SuspendedUserDto,
  SchemaCursorPageResponseStoryDto as StoryCursorPage,
  SchemaChildProfileDto as ChildProfileDto,
  SchemaCharacterDto as CharacterDto,
  SchemaCreateChildProfileRequest as CreateChildProfileRequest,
  SchemaUpdateChildProfileRequest as UpdateChildProfileRequest,
  SchemaExtractedCandidateDto as ExtractedCandidateDto,
  SchemaConfirmCharactersRequest as ConfirmCharactersRequest,
  SchemaUpdateCharacterRequest as UpdateCharacterRequest,
  SchemaBedtimeScheduleDto as BedtimeScheduleDto,
  SchemaBedtimeUpdateRequest as BedtimeUpdateRequest,
  SchemaHolidayDto as HolidayDto,
  SchemaBranchingStartRequest as BranchingStartRequest,
  SchemaBranchingChoiceRequest as BranchingChoiceRequest,
  SchemaBranchingResponse as BranchingResponse,
  SchemaBranchingChoice as BranchingChoice,
  SchemaTranslateRequest as TranslateRequest,
} from './api-types';

// PageResponse<T> is generic on the Java side. openapi-typescript emits
// non-generic concrete instantiations (e.g. SchemaPageResponseStoryDto);
// keep a hand-written generic alias for ergonomic use in frontend code.
export interface PageResponse<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

// String enum unions emitted inline by openapi-typescript (not as named types).
// Hand-declare here for explicit re-export.
export type IllustrationStatus = 'PENDING' | 'READY' | 'FAILED';
export type UserRole = 'USER' | 'ADMIN';

// Frontend-defined enumerations and runtime helpers (not in the OpenAPI spec).
export type {
  AuthErrorCode,
  ModerationErrorCode,
  ApiErrorBody,
} from './errors';

export { ApiError } from './errors';
