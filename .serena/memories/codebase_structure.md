# Codebase Structure

> NOTE: This is the *planned* structure from the implementation plan. Today only `docs/` exists. Update this memory as the code is built out.

## Top level
```
kazka/
├── backend/                # Spring Boot 4 service
├── frontend/               # React 19 + Vite SPA
├── docs/                   # Spec, plan, mockup assets (already exists)
│   ├── _ Standalone.html
│   ├── extracted/          # 19 woff2 fonts + template.html (visual reference)
│   └── superpowers/
│       ├── specs/2026-04-27-kazkar-design.md
│       └── plans/2026-04-27-kazkar.md
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

## Backend (`backend/src/main/java/com/kazka/`)
- `KazkaApplication.java` — main + `@SpringBootApplication` + `@ConfigurationPropertiesScan`
- `config/` — `CorsConfig` (dev profile only), `StaticUploadsConfig`, `WebClientConfig`, `UploadsProperties`, `GlobalExceptionHandler`
- `ollama/` — `OllamaProperties` (record), `OllamaClient` (WebClient wrapper for `streamGenerate` / `generateImage` / `pullModel`), `OllamaModelInitializer` (`ApplicationRunner`, async pull on boot)
- `story/` — `Story` (entity), `IllustrationStatus` (enum), `StoryRepository`, `StoryService`, `StoryController`, `PromptBuilder`
- `story/dto/` — `GenerationRequest`, `StoryDto`, `UpdateStoryRequest`, `PageResponse`, `SseEvent`
- `illustration/` — `IllustrationService`, `ImageStorageService`

## Backend resources (`backend/src/main/resources/`)
- `application.yml` — main config (env-driven datasource and `kazka.*` properties)
- `application-dev.yml`, `application-test.yml` — profile overrides
- `db/migration/V1__create_stories.sql` — Flyway
- `prompts/` — `system-uk.txt`, `system-en.txt`, `illustration-style.txt`

## Frontend (`frontend/src/`)
- `main.tsx`, `App.tsx`
- `design/` — `tokens.css` (palette + typography from mockup), `global.css`, `fonts.css` (generated, references `/fonts/asset_*.woff2`)
- `lib/` — `apiClient.ts`, `sseClient.ts`, `themeContext.tsx`, `localeContext.tsx`, `types.ts`
- `locales/` — `uk.json` (default), `en.json`
- `components/`
  - `chrome/` — `Nav`, `ThemeToggle`, `LocaleToggle`, `Footer`, `PaperGrain`, `ScrollProgress`, `ParticleField`, `AppShell`
  - `home/` — `Hero`, `HowItWorks`, `Features`, `StoryPreview`, `ArchiveTeaser`, `NightCta`
  - `form/` — `StoryForm`, `TagInput`
  - `story/` — `StoryStream`, `StoryCard`, `IllustrationFrame`, `PlaceholderSvg`, `StoryActions`
  - `modal/` — `ConfirmModal`
- `pages/` — `HomePage` (hybrid: form-as-hero + landing sections), `ArchivePage`, `StoryDetailPage`

## Frontend public (`frontend/public/`)
- `fonts/` — 19 self-hosted Lora + Nunito woff2 files (copied from `docs/extracted/`)

## Database schema (`stories` table)
Columns: `id UUID PK`, `title`, `theme`, `characters TEXT[]`, `age_group`, `length`, `language`, `content`, `illustration_path`, `illustration_status` (`pending`/`ready`/`failed`), `created_at`, `updated_at`.

## API surface (`/api`)
- `POST /api/stories/generate` → `text/event-stream` with events `meta`, `token`, `done`, `error`
- `POST /api/stories/{id}/illustrate` → 202 (fire-and-forget)
- `GET /api/stories?page=&size=` → paginated archive
- `GET /api/stories/{id}` → single story (used for polling illustration status)
- `PUT /api/stories/{id}` → edit title + content
- `DELETE /api/stories/{id}` → 204
- `GET /uploads/<id>.png` → static file served from `UPLOADS_DIR`
