// Friendly aliases over the generated api-types. Importers should prefer
// these names over reaching into `components['schemas']` directly.

import type { components as Components } from './api-types';

export type { components, paths } from './api-types';

// openapi-typescript v7 with --root-types emits each schema as a top-level
// `Schema{Name}` alias (e.g. SchemaStoryDto = components['schemas']['StoryDto']).
// We re-export them under their bare DTO names so callers can write `Story`
// instead of `SchemaStoryDto`.
//
// NB: `Story` and `User` are NOT re-exported from api-types here — they are
// narrowed below to match the runtime contract (see comments).
export type {
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

// Backend Java records emit every field as optional in the generated OpenAPI
// schema (springdoc treats record components as non-required by default).
// At runtime however the controllers always populate these fields, and the
// frontend was authored against the required shape. Narrow the generated
// types here so call sites don't have to deal with spurious `undefined`s.
//
// The two illustration paths are genuinely nullable on the wire (StoryDto
// preserves them as `null` until the image generation completes), so we
// widen them to `string | null` rather than `string`.
type StoryBase = Components['schemas']['StoryDto'];
export type Story = Required<Omit<StoryBase, 'illustrationPathLight' | 'illustrationPathDark'>> & {
  illustrationPathLight: string | null;
  illustrationPathDark: string | null;
};

type UserBase = Components['schemas']['UserDto'];
export type User = Required<UserBase>;

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
