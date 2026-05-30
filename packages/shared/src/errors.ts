// Runtime helpers shared between web and mobile clients. These are not
// derivable from the OpenAPI spec — error codes are part of our application
// contract (returned in `ApiErrorBody.error`), and `ApiError` is the
// throwable representation our fetch wrappers use.

export type AuthErrorCode =
  | 'INVALID_CREDENTIALS'
  | 'EMAIL_TAKEN'
  | 'EMAIL_NOT_VERIFIED'
  | 'TOKEN_INVALID'
  | 'INVALID_REFRESH_TOKEN'
  | 'INVALID_APPLE_TOKEN'
  | 'INVALID_GOOGLE_TOKEN'
  | 'MAIL_SEND_FAILED'
  | 'VALIDATION'
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'ACCOUNT_SUSPENDED'
  | 'ERROR';

export type ModerationErrorCode = 'BLOCKED_INPUT' | 'JUDGE_UNAVAILABLE';

// Mirrors com.kazka.moderation.ModerationCategory. Sent on BLOCKED_INPUT SSE error
// events so the UI can show a category-specific message explaining what was blocked.
// JUDGE_UNAVAILABLE never carries a category — it's a system error, not a content issue.
export type ModerationCategory =
  | 'SEXUAL'
  | 'VIOLENCE'
  | 'HATE'
  | 'SELF_HARM'
  | 'DANGEROUS'
  | 'SUBSTANCE'
  | 'PROFANITY'
  | 'DEATH'
  | 'WAR';

export interface ApiErrorBody {
  error: AuthErrorCode | string;
  message?: string;
  fields?: Record<string, string>;
}

export class ApiError extends Error {
  public readonly status: number;
  public readonly body: ApiErrorBody;

  constructor(status: number, body: ApiErrorBody) {
    super(body.error);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}
