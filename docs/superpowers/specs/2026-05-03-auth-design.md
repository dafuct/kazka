# Auth, Per-User Archives, and Admin — Design

**Date:** 2026-05-03
**Status:** Approved by user, pending plan
**Scope:** Add sign-up / sign-in (email + password and Google), make every story belong to a user, gate story creation behind an auth + email-verified check, and introduce an admin role with a "see all users" page.

## 1. Goals

- Users sign up and sign in via email + password, with bcrypt-hashed passwords stored in MySQL.
- Users can also sign up / sign in via Google OAuth2.
- Every story has exactly one owner. Each user sees only their own stories on `/stories` and `/stories/:id`. Sharing those URLs with non-owners returns 404.
- Anonymous users can browse the home page (hero, how it works, features, story preview, night CTA) but cannot view the archive, view individual stories, or open the create-story flow. Clicking any of those triggers the auth modal.
- Email verification is required before a user can create stories. Unverified users may sign in and browse but get a friendly "verify your email" panel when they try to create.
- Password reset flow exists: request reset → email link → set new password → all existing sessions invalidated.
- An ADMIN role exists. The first admin is seeded from env vars at startup. Admin can view a list of all users (without their password hash). Admin bypasses ownership on all story endpoints.
- All sign-in / sign-up UX happens in a modal, mirroring the existing `StoryModal` pattern.

## 2. Non-goals

- Social sharing of stories (PDF export is a future feature).
- Public stories / "showcase" mode.
- Two-factor authentication.
- Promoting users to admin via UI (manual DB / future admin tools).
- Admin moderation actions on users or stories (read-only admin in v1).
- Edit / delete / disable users from the admin page.
- Pagination, search, or filters on the admin users page.
- Rate limiting on auth endpoints (out of scope for v1; consider a follow-up if abuse appears).

## 3. Architecture

Spring Security on the backend with session cookie auth. Sessions are stored in Redis via `spring-session-data-redis`. Passwords are bcrypt-hashed. Google OAuth uses Spring Security's standard OAuth2 client (server-side redirect flow). The frontend talks to the existing `/api/*` routes and includes credentials and a CSRF header on all mutating requests.

### 3.1 New backend modules

Under `com.kazka.auth.*`:
- `SecurityConfig` — `SecurityWebFilterChain`, `PasswordEncoder` (bcrypt), authentication manager, login/logout endpoints, CSRF (`CookieServerCsrfTokenRepository.withHttpOnlyFalse()`), CORS preserved, OAuth2 success handler.
- `AuthController` — `/api/auth/signup`, `/api/auth/login`, `/api/auth/logout`, `/api/auth/me`, `/api/auth/verify-email`, `/api/auth/verify-email/resend`, `/api/auth/password-reset/request`, `/api/auth/password-reset/confirm`.
- `AuthService` — orchestrates signup, login, password reset, verification.
- `OAuth2SuccessHandler` — looks up or provisions the user from a successful Google OAuth2 authentication.
- `MailService` — sends verification and password reset emails. Plain text only for v1 (one link per email); no template engine. Subjects and bodies live as classpath resources under `src/main/resources/mail/` with `{token}` and `{baseUrl}` placeholders, mirroring how `prompts/` is loaded today.
- `CurrentUserResolver` — small helper that extracts the authenticated user id and role from the `ServerWebExchange` (via `ReactiveSecurityContextHolder`).
- `EmailVerificationException`, `EmailAlreadyExistsException`, `EmailNotVerifiedException`, `InvalidTokenException`, `MailDeliveryException` — domain exceptions handled by `GlobalExceptionHandler`.

Under `com.kazka.user.*`:
- `User` JPA entity, `UserRepository`, `UserDto`.
- `EmailVerificationToken`, `EmailVerificationTokenRepository`.
- `PasswordResetToken`, `PasswordResetTokenRepository`.
- `AdminUserDto`, `AdminController`, `AdminService` for the "see all users" feature.
- `UserSeedRunner` (an `ApplicationRunner`) — at startup, if `ADMIN_EMAIL` is set and no user exists with that email, creates one with role `ADMIN`, hashed password from `ADMIN_PASSWORD`, `email_verified = true`.

### 3.2 New backend dependencies (`backend/build.gradle`)

```
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
implementation 'org.springframework.boot:spring-boot-starter-mail'
implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'
implementation 'org.springframework.session:spring-session-data-redis'

testImplementation 'com.icegreen:greenmail-junit5:2.1.0'
testImplementation 'com.redis:testcontainers-redis:2.2.2'
```

### 3.3 New frontend modules

Under `src/lib/`:
- `AuthContext.tsx` — `AuthProvider`, `useAuth()`, holds `user`, `loading`, `signIn`, `signUp`, `signOut`, `requestPasswordReset`, `confirmPasswordReset`, `resendVerification`, `refresh`. On mount calls `GET /api/auth/me` once.
- `AuthModalContext.tsx` — `{ open, tab, openAuth(tab?), closeAuth }`. Mirrors `StoryModalContext`.
- `csrf.ts` — small helper that reads `XSRF-TOKEN` cookie. `apiClient` uses it to attach `X-XSRF-TOKEN` header on all non-GET requests, and switches `fetch` to `credentials: 'include'`.

Under `src/components/auth/`:
- `AuthModal.tsx` + `AuthModal.module.css` — backdrop, panel, tabs (Sign in / Sign up / Forgot), submit handlers.
- `SignInForm.tsx`, `SignUpForm.tsx`, `ForgotPasswordForm.tsx` — small focused forms.
- `GoogleButton.tsx` — anchor to `/oauth2/authorization/google`.
- `RequireAuth.tsx` — route wrapper.
- `RequireAdmin.tsx` — route wrapper that 404s for non-admins.

Under `src/pages/`:
- `EmailVerifiedPage.tsx` — renders confirmation/error from query string, button to open sign-in modal.
- `PasswordResetPage.tsx` — new-password form using `?token=…`.
- `AdminUsersPage.tsx` — read-only table of all users.

Updates:
- `App.tsx` — wraps `<AuthProvider>` and `<AuthModalProvider>` around the rest; new routes; `<AuthModal />` rendered alongside `<StoryModal />`; `RequireAuth` around `/stories` and `/stories/:id`; `RequireAdmin` around `/admin/users`.
- `Nav.tsx` — auth-aware: when `user` is null, show **Sign in** + **Sign up**. When authenticated, show display name with a small dropdown (My archive, Sign out, plus "Admin → Users" if admin). The "Try" CTA stays visible and continues to call `openModal()` (the create-story modal); the modal itself routes through `requireAuth` first.
- `HomePage.tsx`, `Footer.tsx` and any other "Try / Create story" call sites — wrap their handler with `requireAuth(() => openModal())`.
- `StoryModal.tsx` — when `phase === 'form'`, if `!user.emailVerified`, replace the form with a "verify your email — resend link" panel.
- `apiClient.ts` — switch all calls to `credentials: 'include'`, add `X-XSRF-TOKEN` header on non-GET; add `auth.signup/login/logout/me/...` and `admin.listUsers()` methods.
- `sseClient.ts` — same `credentials: 'include'` and CSRF header on the POST.
- `locales/uk.ts`, `locales/en.ts` — new `auth` block (form labels, error messages, email subjects/bodies are kept in the backend).

### 3.4 Infrastructure

- `docker-compose.yml` gains a `redis:7-alpine` service; the backend service depends on it; new env var `SPRING_DATA_REDIS_HOST=redis`.
- New env vars (documented in `.env.example`):
  - `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
  - `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`
  - `ADMIN_EMAIL`, `ADMIN_PASSWORD`
  - `APP_BASE_URL` (used in email links — e.g. `http://localhost` for compose, the prod URL otherwise)
- `application.yml`:
  - `spring.session.store-type=redis`, `spring.session.redis.namespace=kazka:session`, `spring.session.timeout=14d`
  - `spring.security.oauth2.client.registration.google.{client-id,client-secret}` from env
  - `spring.security.oauth2.client.registration.google.scope=openid,email,profile`
  - `spring.mail.*` from env
  - `kazka.auth.token-ttl.email-verification=24h`, `kazka.auth.token-ttl.password-reset=1h`
  - `kazka.auth.app-base-url=${APP_BASE_URL:http://localhost}`
  - `kazka.auth.admin.email=${ADMIN_EMAIL:}`, `kazka.auth.admin.password=${ADMIN_PASSWORD:}`

## 4. Data model

Sessions live in Redis (not MySQL). All persistent state below goes in MySQL via `schema.sql`. The CLAUDE.md notes that Spring Boot 4 uses `spring.sql.init` + `schema.sql` (no Flyway). Following the existing convention in `schema.sql`, every table is preceded by `DROP TABLE IF EXISTS`. Drops run in reverse-FK order (children before parents) so re-init is idempotent.

```sql
DROP TABLE IF EXISTS stories;
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS email_verification_tokens;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(72)  NULL,
    google_subject  VARCHAR(255) NULL,
    display_name    VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_google_subject (google_subject)
);

CREATE TABLE email_verification_tokens (
    token        VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id      VARCHAR(36)  NOT NULL,
    expires_at   DATETIME(3)  NOT NULL,
    consumed_at  DATETIME(3)  NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_evt_user (user_id)
);

CREATE TABLE password_reset_tokens (
    token        VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id      VARCHAR(36)  NOT NULL,
    expires_at   DATETIME(3)  NOT NULL,
    consumed_at  DATETIME(3)  NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_prt_user (user_id)
);

CREATE TABLE stories (
    id                       VARCHAR(36)   NOT NULL PRIMARY KEY,
    user_id                  VARCHAR(36)   NOT NULL,
    title                    TEXT          NOT NULL,
    theme                    TEXT          NOT NULL,
    characters               JSON          NOT NULL,
    age_group                VARCHAR(10)   NOT NULL,
    length                   VARCHAR(10)   NOT NULL,
    language                 VARCHAR(5)    NOT NULL DEFAULT 'uk',
    content                  LONGTEXT      NOT NULL,
    illustration_path_light  VARCHAR(500)  NULL,
    illustration_path_dark   VARCHAR(500)  NULL,
    illustration_status      VARCHAR(20)   NOT NULL DEFAULT 'pending',
    created_at               DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at               DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_stories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_stories_user_created (user_id, created_at DESC)
);
```

Notes:
- Existing rows in `stories` are **wiped** on the migration (decided with the user — no data is worth preserving).
- `password_hash` is `VARCHAR(72)` (bcrypt produces 60 chars, leaves headroom). Nullable to support Google-only users.
- `google_subject` is the OpenID `sub` claim. Indexed via the unique key.
- Tokens are 32-byte random strings encoded URL-safe base64 (43 chars). `VARCHAR(64)` leaves headroom. They are stored unhashed; they're short-lived single-use and only valid for one specific user — the security model is "the email inbox is the secret".
- `email_verified` is `false` for fresh email-signup users, `true` for Google users (Google verifies the email) and the seeded admin.
- Admin sees a `UserDto` containing `id, email, displayName, role, emailVerified, googleLinked (boolean derived from google_subject != null), createdAt, storyCount` — `password_hash` and `google_subject` are never serialized.

`Story.java` entity gains a `userId` field (string FK column) — not a `@ManyToOne User` association, to avoid loading the User on every story query. Repository methods change:

```java
Page<Story> findAllByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
Optional<Story> findByIdAndUserId(String id, String userId);
```

Admin code path uses `findById` and `findAllByOrderByCreatedAtDesc` (the existing methods).

## 5. Auth flows

### 5.1 Sign up (email + password)

1. Frontend modal POSTs `/api/auth/signup` with `{ email, password, displayName }`.
2. Backend validates: email format, password ≥ 8 characters, displayName 1–100 characters.
3. `users` row created (id = UUID, role = `USER`, `email_verified = false`, `password_hash` = bcrypt of the password). On unique-key collision → 409 `EMAIL_TAKEN`.
4. `EmailVerificationToken` generated (`SecureRandom`, 32 bytes URL-safe base64), persisted with `expires_at = now + 24h`.
5. Email sent: `MAIL_FROM` → user's email, subject "Confirm your Kazka email", body contains `{APP_BASE_URL}/verify-email?token={token}`. If `MailException` is thrown, log a warning but **still return 201** — the user can use the resend endpoint after they sign in.
6. Session is created and `SESSION` cookie set (httpOnly, SameSite=Lax, Secure in prod). Response body: `{ user: <UserDto> }`.
7. Frontend `AuthContext` updates `user` from the response; modal closes.

### 5.2 Verify email

- `GET /api/auth/verify-email?token={token}` (intentionally GET so a plain link in an email works without a frontend round-trip).
- Backend: load token; if missing / expired / consumed → redirect to `{APP_BASE_URL}/verify-email?error=TOKEN_INVALID`.
- Otherwise: set `email_verified = true` on the user, set `consumed_at = now` on the token, redirect to `{APP_BASE_URL}/verify-email?ok=1`.
- `EmailVerifiedPage` reads the query and renders a success or error message + a "Sign in" button (which calls `openAuth('signIn')` if signed out, or just navigates home if already signed in).

Resend:
- `POST /api/auth/verify-email/resend` — auth required. Marks any unconsumed prior token for this user as consumed (so the old email link stops working), generates a new one, sends a new email. Returns 204.
- Used by the "Resend verification" link inside the StoryModal "please verify" panel and from the dropdown user menu.

### 5.3 Sign in (email + password)

- `POST /api/auth/login` with `{ email, password }`. Spring Security's `ReactiveAuthenticationManager` runs `UserDetailsService` → loads `User` by email → bcrypt-checks the password → on success creates a session and sets the cookie. Body: `{ user: <UserDto> }`. On any failure → 401 `INVALID_CREDENTIALS` with no information about whether the email exists.
- Unverified users **can** log in; the verification gate only applies at story creation (per Q7 decision).

### 5.4 Sign in / sign up with Google

- The "Continue with Google" button is an anchor: `<a href="/oauth2/authorization/google">`. Clicking it leaves the SPA and starts the Spring Security OAuth2 flow.
- Google redirects back to `/login/oauth2/code/google` (Spring Security's default handler endpoint). Spring exchanges the code for a token and resolves an `OAuth2User`.
- Custom `OAuth2SuccessHandler`:
  1. Read `subject` (`sub`) and `email` and `name` from the `OAuth2User`.
  2. Lookup by `google_subject`. If found → log in.
  3. Else lookup by `email`. If found and `google_subject` is null → set `google_subject`, set `email_verified = true` (Google verified them — in case they hadn't yet), persist; log in. (Account linking.) If found and `google_subject` is non-null and different → 409 (shouldn't happen — different sub for same email is a conflict).
  4. Else create a new user: random UUID, `email`, `display_name = name` (fall back to local-part of email if blank), `password_hash = NULL`, `google_subject`, `role = USER`, `email_verified = true`. Persist.
  5. Establish a session; redirect to `{APP_BASE_URL}/?auth=ok`.
- Frontend, on mount of `App`, checks `?auth=ok`; if present, strips the param via `replaceState` and calls `auth.refresh()` to hydrate the user.

### 5.5 Sign out

- `POST /api/auth/logout` — Spring Security `ServerLogoutHandler` invalidates the session in Redis and clears the cookie. Frontend wipes `user` and navigates to `/`.

### 5.6 Password reset

- `POST /api/auth/password-reset/request` `{ email }` — **always returns 204**, regardless of whether the email exists. If the user exists and `password_hash` is non-null (i.e. they actually have a password to reset — Google-only users get the same 204 with no email), generate a `PasswordResetToken` (1h TTL) and send the reset email with link `{APP_BASE_URL}/reset-password?token={token}`.
- `POST /api/auth/password-reset/confirm` `{ token, newPassword }` — validate the token, validate the new password (≥ 8 chars), set the new bcrypt hash on the user, mark the token consumed, **invalidate all of that user's existing sessions** in Redis (so any other browser they were signed into is signed out). Returns 204.
- `PasswordResetPage` reads `?token=…`, shows new-password and confirm-password fields, calls confirm, on success shows "Password updated — please sign in" + open-modal button.

### 5.7 Get current user

- `GET /api/auth/me` — returns `{ id, email, displayName, role, emailVerified, googleLinked }` for an authenticated session, else 401 (frontend treats 401 as `user = null`).
- Called once on `App` mount; any time the auth state is suspect (e.g. after Google redirect, after manual `refresh()` call), called again.

### 5.8 Story creation gate

- `POST /api/stories/generate` and `POST /api/stories/{id}/illustrate`: require auth; check `user.emailVerified === true`. If false → 403 `EMAIL_NOT_VERIFIED`. Otherwise proceed; persist the new `Story` with `user_id = currentUserId`.
- `GET /api/stories`, `GET /api/stories/{id}`, `PUT /api/stories/{id}`, `DELETE /api/stories/{id}`: require auth; for non-admins, scope to `currentUserId` (via `findByIdAndUserId` etc.). Non-existent or other user's story → 404 (never 403, to avoid leaking existence).
- Admin: bypasses ownership scoping on all story endpoints.

## 6. Authorization config

`SecurityConfig` `SecurityWebFilterChain`:

```
permitAll:
  POST /api/auth/signup
  POST /api/auth/login
  POST /api/auth/logout
  GET  /api/auth/me
  POST /api/auth/password-reset/request
  POST /api/auth/password-reset/confirm
  GET  /api/auth/verify-email
  /oauth2/**, /login/oauth2/**
  GET  /uploads/**

authenticated:
  POST /api/auth/verify-email/resend
  /api/stories/**

hasRole('ADMIN'):
  /api/admin/**
```

CSRF: enabled with `CookieServerCsrfTokenRepository.withHttpOnlyFalse()`. Ignored on `/oauth2/**` and `/login/oauth2/**` (Spring's flow handles its own protection via state param). The frontend reads `XSRF-TOKEN` cookie and sends `X-XSRF-TOKEN` on every non-GET request.

CORS: existing dev profile config preserved. `allowCredentials = true` is required so cookies cross origins. `allowedOrigins` must be explicit (not `*`) when credentials are allowed — for dev: `http://localhost:5173`.

## 7. Error handling

`GlobalExceptionHandler` gains:

| Exception | Status | Body |
|---|---|---|
| `BadCredentialsException` | 401 | `{ error: "INVALID_CREDENTIALS" }` |
| `EmailAlreadyExistsException` | 409 | `{ error: "EMAIL_TAKEN" }` |
| `EmailNotVerifiedException` | 403 | `{ error: "EMAIL_NOT_VERIFIED" }` |
| `InvalidTokenException` | 400 | `{ error: "TOKEN_INVALID" }` |
| `MailDeliveryException` | 503 | `{ error: "MAIL_SEND_FAILED" }` |
| `AccessDeniedException` (admin only) | 403 | `{ error: "FORBIDDEN" }` |
| `MethodArgumentNotValidException` | 400 | `{ error: "VALIDATION", fields: {...} }` |

Body shape across the whole app: `{ error: <CODE>, message?: <localizable string>, fields?: {...} }`. Frontend has a switch over `error` → `t.auth.errors[code]` (mapped per-locale).

## 8. Frontend modal & UX

`AuthModal` follows `StoryModal`'s structure: `createPortal` to body, backdrop click-to-close, escape-key close, body scroll lock, header with ornament + close button.

Tabs:
- **Sign in** — email, password, primary submit, secondary "Continue with Google" (full-width button), text link "Forgot password?" → switches to forgot tab.
- **Sign up** — email, displayName, password, confirm password, primary submit, secondary "Continue with Google".
- **Forgot password** — email, primary submit "Send reset link", success state ("If that email exists, we've sent a reset link.")

Loading states: during submission, the primary button shows the same `sun` spinner used in `StoryModal`'s creating phase, and inputs are disabled.

Inline validation:
- Field validation errors (empty, password too short, password mismatch) render below each field.
- Submit-time errors (`INVALID_CREDENTIALS`, `EMAIL_TAKEN`, `MAIL_SEND_FAILED`) render in a banner above the submit button.

All copy via `useLocale()` `t.auth.*`. Both `uk.ts` and `en.ts` get parallel `auth` blocks: `tabs.signIn/signUp/forgot`, `fields.email/displayName/password/confirmPassword`, `actions.signIn/signUp/google/sendResetLink/resend`, `errors.{CODE}`, `messages.checkEmail/passwordUpdated/...`.

`Nav` updates:
- Unauthenticated: replace "Try" CTA with two buttons — **[Sign in]** opens `AuthModal` on `signIn` tab, **[Sign up]** opens it on `signUp` tab.
- Authenticated: dropdown trigger shows display name; menu has "My archive" (link to `/stories`), "Sign out", and "Admin → Users" (admin only).
- The "Try" CTA stays for authenticated users and continues to call `openModal()`. For unauthenticated users it routes through `requireAuth()` and opens the auth modal first.

`StoryModal`:
- When `phase === 'form'`, branch on `user.emailVerified`. If false, render a "Verify your email" panel instead of `<StoryForm>` — copy: "We've sent a confirmation link to {email}. Click it to start creating stories." + a "Resend link" button calling `auth.resendVerification()`.

Routes (in `App.tsx`):
- `/` → `HomePage` (unchanged).
- `/stories` → `<RequireAuth><ArchivePage /></RequireAuth>`.
- `/stories/:id` → `<RequireAuth><StoryDetailPage /></RequireAuth>`.
- `/verify-email` → `<EmailVerifiedPage />` (always public).
- `/reset-password` → `<PasswordResetPage />` (always public).
- `/admin/users` → `<RequireAdmin><AdminUsersPage /></RequireAdmin>`.

`RequireAuth`: while `auth.loading`, render the same skeleton placeholder used by `ArchivePage` while loading (`p.msg = "..."`). If no user, navigate to `/` and call `openAuth('signIn')`. Otherwise render children.

`RequireAdmin`: same loading behaviour; if user is missing or not admin, render a 404 page.

`AdminUsersPage`: a single `GET /api/admin/users` call on mount. Renders a table: email, displayName, role, emailVerified, googleLinked, createdAt, storyCount. No edit, no pagination — sized for "small enough to render at once" for the foreseeable future.

`apiClient.ts`:
- All `fetch` calls switch to `credentials: 'include'`.
- Helper `withCsrf(init)` reads `document.cookie` for `XSRF-TOKEN` and merges `X-XSRF-TOKEN` header on non-GET. Used by `request<T>` and `streamStory` (`sseClient.ts`).
- New methods: `auth.signup/login/logout/me/verifyResend/passwordResetRequest/passwordResetConfirm`, `admin.listUsers`.

## 9. Testing

Per the project's Spring Boot 4 / CLAUDE.md rules: no `@DataJpaTest`. All persistence-touching tests use `@SpringBootTest @ActiveProfiles("test")` + Testcontainers MySQL + Testcontainers Redis. Email is captured with GreenMail.

### 9.1 Unit tests

- `AuthServiceTest` — signup hashes the password, generates a token, calls `MailService`. Login throws on mismatch. Password reset confirm flips the hash and consumes the token.
- `OAuth2SuccessHandlerTest` — three branches: existing google_subject, link by email, brand-new user. Verifies user is persisted with the right fields.
- `TokenServiceTest` — generates URL-safe random tokens of expected length; expired/consumed tokens fail validation.
- `CurrentUserResolverTest` — extracts user id and role from a stubbed reactive security context.

### 9.2 Integration tests

- `AuthControllerIT` — full HTTP round-trip with `WebTestClient`. Signup → SESSION cookie returned → `/me` works → logout invalidates → `/me` is 401. CSRF is enforced on POST without `X-XSRF-TOKEN`.
- `EmailVerificationIT` — signup → email captured by GreenMail → extract token → GET verify-email → user `email_verified` becomes true. Same token can't verify twice. Expired token (manually aged) returns redirect with `error`. Resend invalidates prior unconsumed tokens.
- `PasswordResetIT` — request with non-existent email = 204, no email sent. Request with real email = 204, email sent. Confirm with valid token = 204, password updated, prior session invalidated. Confirm with consumed/expired token = 400.
- `StoryAccessIT` — user A creates a story; user B GET/PUT/DELETE on it = 404. Admin GET/PUT/DELETE = 200. Unverified user POST `/generate` = 403 `EMAIL_NOT_VERIFIED`. Verified user = 200.
- `AdminControllerIT` — non-admin GET `/api/admin/users` = 403. Admin = 200, body shape verified, `password_hash` and `google_subject` absent.
- `SeedAdminIT` — boot context with `ADMIN_EMAIL` env, verify a user is created with role ADMIN; reboot with same env, verify no duplicate.

### 9.3 Frontend verification

Per CLAUDE.md, no test framework. Verification is `tsc --noEmit` + `npm run lint` + manual click-through:

- Sign up flow (manual SMTP — easiest is a Gmail account with an app password set in `MAIL_*` env) → email arrives → click link → verified.
- Sign in / sign out cycle.
- Forgot password full loop.
- Google OAuth round-trip against a real Google client (dev credentials only — production `GOOGLE_CLIENT_*` set at deploy time).
- Anonymous user on `/stories` → redirected, modal opens.
- Anonymous user clicks "Try" → modal opens.
- Verified user can create story; unverified user sees verify panel inside StoryModal.
- Admin user: nav shows "Admin → Users", page renders, no `password_hash` in network response.
- Two browser sessions → request password reset → confirm → other session is signed out (next API call returns 401).

## 10. Risks and open questions

- **Google client setup is a one-time environmental step** (Google Cloud Console → OAuth client → authorized redirect URIs include `{APP_BASE_URL}/login/oauth2/code/google`). Documented in `.env.example`. Not testable without real Google credentials.
- **SMTP configuration in dev**: GreenMail covers tests; for manual dev a Gmail SMTP with an "app password" is the simplest path. `.env.example` documents both.
- **No rate limiting in v1**. Anonymous abuse of `/api/auth/signup` could spam the user table and emails. Mitigations deferred to a follow-up; sign-up volume is expected to be low.
- **Account-linking edge case**: if user A signs up with email + password, then user B uses Google with the same email (same Google `sub`) — they hijack A's account on link. Mitigation: the link path requires that the user is actually the email owner, which Google attests to. Real risk is only "another person at the same email" which is a recovery scenario for that email's owner anyway. Accepted.
- **Session timeout**: 14 days sliding. Acceptable for a low-stakes app. Configurable via `spring.session.timeout`.
- **Cookie `Secure` flag** must be on in prod and off in HTTP dev. Spring's session cookie config is profile-driven (`server.reactive.session.cookie.secure: ${COOKIE_SECURE:false}`); set `true` in the prod env.

## 11. Out-of-scope follow-ups (next specs)

- Rate limiting on auth endpoints.
- "Public story" flag + share-link feature (after PDF export).
- Admin moderation (disable user, force-verify, reset another user's password).
- 2FA / TOTP.
- Account deletion (right-to-erasure) — straightforward via `DELETE FROM users` cascade, but needs a UI and a confirmation flow.
- "Remember me" toggle to extend session.
