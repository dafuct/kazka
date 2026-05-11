// Friendly aliases over the generated api-types. Importers should prefer
// these names over reaching into `components['schemas']` directly.

export type { components, paths } from './api-types';

// openapi-typescript v7 with --root-types emits each schema as a top-level
// `Schema{Name}` alias (e.g. SchemaStoryDto = components['schemas']['StoryDto']).
// We re-export them under their bare DTO names so callers can write `Story`
// instead of `SchemaStoryDto`.
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
} from './api-types';

// The backend's `PageResponse<T>` is a Java generic that springdoc emits as
// concrete instantiations (PageResponseStoryDto, PageResponseFlaggedAttemptDto,
// …) rather than a parameterised type. Declare the generic shape here so
// frontend code can write `PageResponse<Story>` etc.
export interface PageResponse<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

// Frontend-defined enumerations and runtime helpers (not in the OpenAPI spec).
export type {
  AuthErrorCode,
  ModerationErrorCode,
  ApiErrorBody,
} from './errors';

export { ApiError } from './errors';

// IllustrationStatus and UserRole are Java enums; openapi-typescript inlines
// them as anonymous string-literal unions on each containing schema rather
// than emitting a named type. Re-declare the unions here for ergonomic use.
export type IllustrationStatus = 'PENDING' | 'READY' | 'FAILED';
export type UserRole = 'USER' | 'ADMIN';
