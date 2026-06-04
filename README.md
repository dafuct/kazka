# Казкар (Kazka) — Ukrainian Fairy Tale Generator

A full-stack app that generates personalized, illustrated Ukrainian fairy tales
with AI. It runs on the web and on iOS/Android, supports accounts and
subscriptions, and streams stories token-by-token as they are written.

Production: **[kazkatales.com](https://kazkatales.com)**.

> Story generation, editing, scene extraction, and moderation use **Google
> Gemini 2.5 Flash** via the OpenAI-compatible endpoint. Illustrations use
> **Fal.ai FLUX.1-schnell**. Free tiers exist for both — a Google API key
> (Gemini) is required to run the backend; a Fal.ai key is required to
> generate illustrations.

## Features

- **Real-time streaming** — story text appears word by word over SSE as the model writes it.
- **Two-pass generation** — a storyteller pass plus an editor pass that fixes invented words and Ukrainian typography.
- **Illustrations** — AI-generated cover art (FLUX), with a decorative SVG fallback when generation is unavailable.
- **Child profiles & characters** — per-child preferences (age, language) and a reusable character library extracted from past stories.
- **Branching tales** — interactive "choose what happens next" stories.
- **Bedtime ritual** — schedule a nightly story per child.
- **Bilingual** — Ukrainian and English, switchable per user/child.
- **Seasonal / holiday packs** — themed prompts tied to the current date and locale.
- **Parent dashboard** — activity overview across children.
- **Accounts** — email/password (with verification + reset), Google, and Apple sign-in; JWT sessions.
- **Subscriptions** — free tier (3 stories/month) plus Pro via Paddle and Monobank (web) and Apple In-App Purchase (mobile); gift codes.
- **Content moderation** — an LLM judge screens prompts; repeated violations auto-suspend.

## Tech stack

| Layer      | Stack                                                                 |
|------------|-----------------------------------------------------------------------|
| Backend    | Spring Boot 4 (WebFlux), Java 25, Gradle                              |
| Data       | MySQL 8 (Liquibase migrations) · Redis (Spring Session)              |
| AI         | Google Gemini 2.5 Flash (text, editor, scene, moderation) · Fal.ai FLUX.1-schnell (images) |
| Web        | React 19 + TypeScript + Vite                                         |
| Mobile     | Expo (React Native) — iOS & Android                                  |
| Shared     | `@kazka/shared` — TS types generated from the backend OpenAPI spec   |
| Storage    | Local filesystem (dev) or private Cloudflare R2 (presigned URLs)     |

## Monorepo layout

The repo is an npm workspaces monorepo:

```
kazka/
├─ backend/                # Spring Boot 4 (WebFlux) — Gradle
├─ frontend/               # React 19 + Vite — npm workspace "frontend"
├─ mobile/                 # Expo / React Native — npm workspace "@kazka/mobile"
├─ packages/
│  └─ shared/              # @kazka/shared — TS types generated from backend OpenAPI
├─ scripts/
│  └─ gen-types.sh         # OpenAPI → TS codegen
├─ docker-compose.yml      # local full stack (mysql, redis, backend, frontend)
├─ docker-compose.prod.yml # production stack
└─ package.json            # root workspaces config
```

## Prerequisites

- **Java 25** (`sdk install java 25-open` via SDKMAN, or [jdk.java.net/25](https://jdk.java.net/25/))
- **Node.js 20+** and npm
- **Docker** (MySQL + Redis, and the full-stack compose)
- **A Google Gemini API key** — free at [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)
- **A Fal.ai API key** — free starter credit at [fal.ai/dashboard/keys](https://fal.ai/dashboard/keys) (only needed if you want generated illustrations; the app falls back to decorative SVG covers when missing)

## Quick start

### 1. Configure environment

```bash
cp .env.example .env
# At minimum, set GOOGLE_API_KEY (Gemini). FAL_KEY enables generated
# illustrations. KAZKA_JWT_SECRET has a dev default but should be rotated
# for anything real (openssl rand -base64 48).
```

`.env.example` documents every variable, grouped by area (AI, database, auth,
storage, billing). Most have sensible local defaults; only the Google Gemini
API key is strictly required to generate stories.

### 2a. Run the whole stack with Docker (simplest)

```bash
docker-compose up -d   # mysql + redis + backend + frontend
```

The web app is served at `http://localhost`. Liquibase migrations run
automatically on backend start.

### 2b. Run locally for development (hot reload)

```bash
# Infra only (MySQL + Redis):
docker-compose up -d mysql redis

# Backend (port 8080):
cd backend && ./gradlew bootRun

# Web frontend (port 5173, proxies /api to :8080):
npm install
npm run frontend:dev
```

Open `http://localhost:5173`.

## Configuration

All configuration is via environment variables (see `.env.example`). Highlights:

| Variable                | Purpose                                                          |
|-------------------------|-----------------------------------------------------------------|
| `GOOGLE_API_KEY`        | **Required.** Google Gemini API key (text, editor, scene, judge). |
| `FAL_KEY`               | Fal.ai key for illustration generation (FLUX.1-schnell).         |
| `AI_TEXT_MODEL`         | Text/storyteller model (default: `gemini-2.5-flash`).            |
| `AI_IMAGE_MODEL`        | Illustration model (default: `fal-ai/flux/schnell`).             |
| `DB_URL` / `DB_USER` / `DB_PASS` | MySQL connection.                                      |
| `SPRING_DATA_REDIS_HOST` / `..._PORT` | Redis (session store).                     |
| `KAZKA_JWT_SECRET`      | JWT signing secret (min 32 chars for HS256).                    |
| `APP_BASE_URL`          | Base URL used in email links and OAuth redirects.               |
| `STORAGE_PROVIDER`      | `filesystem` (default) or `r2`.                                 |
| `GOOGLE_CLIENT_ID` / `APPLE_WEB_CLIENT_ID` | Social sign-in (optional).            |
| `PADDLE_*` / `MONOBANK_*` | Subscription providers (optional).       |

Leaving an optional provider's keys blank disables that provider with a clear
"not configured" error rather than failing startup.

## Shared TypeScript types

The web and mobile apps consume API types from `@kazka/shared`, generated from
the backend's OpenAPI spec at `/v3/api-docs`.

```bash
# Terminal 1: start the backend
cd backend && ./gradlew bootRun

# Terminal 2: regenerate types
npm run gen:types
```

Commit the regenerated `packages/shared/src/api-types.ts`. CI runs
`npm run verify:types` to fail any PR that ships stale generated types.

## Web frontend

```bash
npm run frontend:dev        # http://localhost:5173
npm run frontend:build
npm run frontend:lint
```

## Mobile app

The `@kazka/mobile` workspace is an Expo (React Native) app sharing the same
backend and generated types. Bundle IDs: `app.kazka.ios` / `app.kazka.android`.

```bash
npm run mobile:start        # Expo dev server
npm run mobile:ios          # run on iOS simulator/device
npm run mobile:lint
npm run mobile:test
```

The `mobile/ios/` native project is committed (for the widget extension and
signing config); `mobile/android/` is generated.

## Database & migrations

Schema is managed with **Liquibase**. The root changelog is
`backend/src/main/resources/db/changelog/db.changelog-master.yaml`, which
includes the changesets under `db/changelog/changes/`. Migrations run
automatically on every backend start — no manual step required. Hibernate runs
with `ddl-auto: validate`, so the entity model must match the migrated schema.

Inspect migration state:

```bash
docker exec -i kazkar-mysql mysql -ukazkar -pkazkar kazkar \
  -e "SELECT id, author, filename, dateexecuted, exectype FROM DATABASECHANGELOG ORDER BY orderexecuted"
```

Reset the dev database completely:

```bash
docker-compose down -v && docker-compose up -d
```

## Image storage

- **`filesystem`** (default) — illustrations are written to `UPLOADS_DIR`
  (`./uploads`) and served as static resources at `/uploads/`.
- **`r2`** — illustrations are uploaded to a **private** Cloudflare R2 bucket;
  the backend mints short-lived presigned GET URLs at DTO-build time, so the
  bucket is never public and links are only issued for stories the user may see.
  Configure with `R2_ENDPOINT`, `R2_ACCESS_KEY`, `R2_SECRET_KEY`, `R2_BUCKET`,
  `R2_PRESIGN_TTL`.

## Subscriptions & billing

Free accounts can generate a limited number of stories per month
(`BILLING_FREE_LIMIT`, default 3). Pro unlocks unlimited generation and premium
features. Payment providers are pluggable and region-aware:

- **Paddle** — card checkout (web).
- **Monobank Acquiring** — Ukrainian tokenized recurring (web); first charge saves the card, a `@Scheduled` job renews monthly via pay-by-token.
- **Apple In-App Purchase** — mobile (StoreKit).
- **Gift codes** — redeemable for Pro entitlements.

Each provider exposes a webhook under `/api/billing/webhook/*`. See
`.env.example` for the keys each provider needs.

## API

The backend is documented via OpenAPI at `/v3/api-docs`. Main areas:

| Area              | Base path                         |
|-------------------|-----------------------------------|
| Auth & accounts   | `/api/auth`, `/api/auth/oauth`, `/api/auth/token`, `/api/devices` |
| Stories           | `/api/stories` (generate via SSE, CRUD, branching, translation)  |
| Children          | `/api/children`, `/api/children/{childId}/bedtime`               |
| Holidays          | `/api/holidays`                   |
| Billing           | `/api/billing`, `/api/billing/webhook/*` |
| Admin & moderation| `/api/admin`, `/api/admin/moderation` |

Core story endpoints:

| Method | Path                          | Description              |
|--------|-------------------------------|--------------------------|
| POST   | `/api/stories/generate`       | Stream a story via SSE   |
| GET    | `/api/stories`                | List stories (paginated) |
| GET    | `/api/stories/{id}`           | Get a single story       |
| PUT    | `/api/stories/{id}`           | Update title + content   |
| DELETE | `/api/stories/{id}`           | Delete a story           |

## Tests & CI

```bash
cd backend && ./gradlew test     # Testcontainers (MySQL 8) + WireMock — Docker required
```

GitHub Actions (`.github/workflows/ci.yml`) runs three jobs on push/PR to `main`:

- **Backend tests** — `./gradlew test`
- **Frontend build + lint** — `npm ci`, `npm audit --audit-level=high`, build, `eslint .`
- **Shared types up to date** — boots the backend against MySQL/Redis and verifies `api-types.ts` is regenerated

## Production

`docker-compose.prod.yml` defines the production stack (MySQL, Redis, backend,
frontend) with persistent volumes. The frontend image is built from the repo
root (npm workspace deps) and takes `VITE_*` build args for client-side config.
Production runs against `kazkatales.com` with R2 storage and `COOKIE_SECURE=true`.
