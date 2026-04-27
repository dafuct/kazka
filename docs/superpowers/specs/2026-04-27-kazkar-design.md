# Казкар — Design Spec

**Date:** 2026-04-27
**Status:** Approved (ready for implementation plan)

## 1. Goal

Build "Казкар" — a full-stack Ukrainian/English fairy tale generator that runs entirely on local AI (Ollama). Parents enter a theme, characters, age group, and length; the app streams a personalized bedtime story and generates an accompanying illustration. Stories are persisted, editable, deletable, and browseable in an archive.

The visual aesthetic is taken from `docs/_ Standalone.html` (the "Казкар v2" landing-page mockup): warm parchment palette, Lora + Nunito typography, paper-grain texture, magic-purple/ember-orange accents, particle field background, optional dark theme.

## 2. Tech Stack

- **Backend:** Java 25, Spring Boot 4, Spring WebFlux, Spring Data JPA, Gradle 9 (Kotlin DSL)
- **Database:** PostgreSQL 16 (synchronous JPA wrapped in `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` for WebFlux compatibility)
- **AI runtime:** Ollama, models `gemma3:4b` (text) and `x/flux2-klein` (image), pulled automatically on app start
- **Frontend:** React 19 + TypeScript + Vite, no UI framework (plain CSS modules + design tokens)
- **i18n:** Lightweight in-house `LocaleContext` (UK + EN), no external library
- **Container:** docker-compose for Postgres (Ollama runs on host, not in container, because `x/flux2-klein` needs macOS host GPU)

## 3. Repository Layout

```
kazka/
├── backend/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── src/main/
│       ├── java/com/kazka/
│       │   ├── KazkaApplication.java
│       │   ├── config/         # WebClientConfig, CorsConfig, OllamaProperties
│       │   ├── ollama/         # OllamaClient, OllamaModelInitializer
│       │   ├── story/          # Story (entity), StoryRepository, StoryService, StoryController, dto/
│       │   └── illustration/   # IllustrationService, ImageStorageService, PlaceholderSvgService
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           └── prompts/        # system-uk.txt, system-en.txt, illustration-style.txt
├── frontend/
│   ├── package.json
│   ├── vite.config.ts          # /api proxy → http://localhost:8080
│   ├── index.html
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── design/             # tokens.css (palette/typography lifted from mockup), global.css
│       ├── lib/                # apiClient.ts, sseClient.ts, themeContext.tsx, localeContext.tsx
│       ├── locales/            # uk.json, en.json
│       ├── components/
│       │   ├── chrome/         # Nav, ThemeToggle, LocaleToggle, ScrollProgress, PaperGrain, ParticleField, Footer
│       │   ├── home/           # Hero, HowItWorks, Features, StoryPreview, ArchiveTeaser, NightCta
│       │   ├── form/           # StoryForm (theme/characters/age/length/language), TagInput, AgeSelect, LengthSelect
│       │   ├── story/          # StoryStream (SSE renderer), StoryCard, IllustrationFrame, StoryActions (edit/delete)
│       │   └── modal/          # ConfirmModal
│       ├── pages/              # HomePage, ArchivePage, StoryDetailPage
│       └── public/fonts/       # 19 woff2 files copied from docs/extracted/
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

## 4. Data Model

```sql
CREATE TABLE stories (
  id                   UUID PRIMARY KEY,
  title                TEXT NOT NULL,
  theme                TEXT NOT NULL,
  characters           TEXT[] NOT NULL,            -- mapped to List<String> via Hibernate's `ListArrayType` (hypersistence-utils)
  age_group            TEXT NOT NULL,              -- '3-5' | '6-8' | '9-12'
  length               TEXT NOT NULL,              -- 'short' | 'medium' | 'long' (stored canonical)
  language             TEXT NOT NULL DEFAULT 'uk', -- 'uk' | 'en'
  content              TEXT NOT NULL,
  illustration_path    TEXT,                       -- '/uploads/<id>.png' or NULL
  illustration_status  TEXT NOT NULL DEFAULT 'pending', -- 'pending' | 'ready' | 'failed'
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX stories_created_at_idx ON stories (created_at DESC);
```

`length` is stored canonically (`short`/`medium`/`long`) and translated to UI labels client-side.

## 5. Backend API

All under `/api`. CORS allows `http://localhost:5173` in `dev` profile.

### `POST /api/stories/generate`
**Body:**
```json
{
  "theme": "пригоди в чарівному лісі",
  "characters": ["Мія", "лисичка"],
  "ageGroup": "6-8",
  "length": "medium",
  "language": "uk"
}
```
**Response:** `text/event-stream`. Server creates DB row immediately with status `pending`, then emits:

```
event: meta
data: {"id":"<uuid>"}

event: token
data: {"text":"Жила-була "}

event: token
data: {"text":"маленька дівчинка..."}

event: done
data: {"id":"<uuid>","title":"Мія та чарівна лисичка"}
```

On Ollama failure: `event: error` with `{"message": "..."}`. Client shows toast.

### `POST /api/stories/{id}/illustrate`
Fire-and-forget. Returns `202 Accepted` immediately; runs illustration generation on `boundedElastic` scheduler. Updates `illustration_status` and `illustration_path`. Failure → status `failed`, no path stored.

### `GET /api/stories?page=0&size=20`
Paginated archive. Returns `{ items: Story[], page, size, total }`. Stories are returned in `created_at DESC` order.

### `GET /api/stories/{id}`
Single story (used for detail page + polling for illustration readiness).

### `PUT /api/stories/{id}`
**Body:** `{ "title": "...", "content": "..." }`. Returns updated story. Only title and content are editable.

### `DELETE /api/stories/{id}`
Deletes row and `uploads/<id>.png` if present. Returns `204 No Content`.

### `GET /uploads/<id>.png`
Static resource handler maps `/uploads/**` to the configured `uploads/` directory on disk.

## 6. Ollama Integration

`OllamaClient` is a thin `WebClient` wrapper around the Ollama REST API.

- **Text streaming:** `POST {OLLAMA_BASE_URL}/api/generate` with `{model, prompt, stream: true}`. The response is newline-delimited JSON; each line is `{response: "...", done: bool}`. `OllamaClient.streamGenerate(...)` returns `Flux<String>` of token chunks.
- **Image generation:** `POST {OLLAMA_BASE_URL}/api/generate` with `{model: x/flux2-klein, prompt, stream: false}`. Response includes a base64 PNG in the `images` array. Wrapped in `try/catch`; any exception → caller treats as failure.
- **Model pull:** `POST {OLLAMA_BASE_URL}/api/pull` with `{name, stream: true}`. Streams progress lines; we log them.

`OllamaModelInitializer` implements `ApplicationRunner`. On boot, fires async pulls for both models in parallel. Does **not** block startup. If a pull fails, log a warning — first user request will retry.

`OllamaProperties` (`@ConfigurationProperties("kazka.ollama")`):
```yaml
kazka:
  ollama:
    base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
    text-model: ${OLLAMA_TEXT_MODEL:gemma3:4b}
    image-model: ${OLLAMA_IMAGE_MODEL:x/flux2-klein}
```

## 7. Prompts

`prompts/system-uk.txt`:
> Ти — досвідчений казкар, який створює добрі вечірні казки для дітей. Пиши теплою, образною мовою. Не використовуй страшних чи жорстоких сцен. Кожна казка має початок, розвиток і світлий кінець. Перший рядок — назва казки (без слова "Назва:"). Далі — текст казки.

`prompts/system-en.txt`: equivalent in English.

User prompt is templated with `theme`, `characters` (joined), `ageGroup`, `length` (mapped to ~150/~400/~800 word targets).

`prompts/illustration-style.txt`:
> Watercolor children's book illustration, soft warm parchment palette, golden afternoon light, gentle brush strokes, dreamy atmosphere, no text, no letters, centered composition.

Illustration prompt = `{title} featuring {characters joined}. {style suffix}`. Always built in English (Flux works better in English). For UK stories, the title is passed verbatim — Flux handles it acceptably; we accept slight quality loss.

## 8. Frontend

### Routing

- `/` — `HomePage` (hybrid: form + landing sections)
- `/stories` — `ArchivePage`
- `/stories/:id` — `StoryDetailPage`

Single-page React Router v7. All pages share `<AppShell>` (Nav + ParticleField + PaperGrain + ScrollProgress + Footer).

### `HomePage` (hybrid)

Above the fold:
- Left: Hero text — Lora headline (animated character-by-character reveal), subhead, "★ 4.9 · 2 400+ казок створено" social proof.
- Right: `<StoryForm>` card on parchment surface with form fields; primary CTA "Створити казку" (ember orange button with ripple). Below button, smaller secondary link to `/stories` ("Архів казок →").

When the form is submitted:
- Form scrolls smoothly to a `<StoryStream>` block that appears beneath the hero.
- SSE tokens render word-by-word into a parchment card with serif body.
- On `done`, the `IllustrationFrame` slides in below with a skeleton; polling begins.
- Once illustration is ready (or after 60s timeout), final state shows.

Below the fold (always visible):
- How it works (3 steps)
- Features (4-card grid)
- Story preview — shows the most-recently-generated story if archive non-empty, else a hardcoded sample
- Archive teaser — 3 latest cards, "Дивитися всі →" link to `/stories`
- Night CTA section
- Footer

### `ArchivePage`

Grid of `StoryCard` (thumbnail or placeholder SVG, title, age tag, date). Hover: trash icon → `ConfirmModal`. Pagination at bottom.

### `StoryDetailPage`

- Top: back link to archive
- Hero: large `IllustrationFrame` with the saved illustration (or polling if still pending; or SVG placeholder if failed)
- Below: serif body text
- Right rail (or top-right on mobile): `<StoryActions>` — "Редагувати" / "Видалити"
  - Edit mode: title becomes a contentEditable input; body becomes a textarea preserving paragraph breaks. "Зберегти" / "Скасувати". On save, `PUT` the changes. No auto-save.
  - Delete: `ConfirmModal` ("Видалити цю казку назавжди?"); on confirm, `DELETE` then redirect to `/stories`.

### Design tokens (`design/tokens.css`)

Lifted directly from `docs/extracted/template.html`:
```css
:root {
  --color-bg: #FDF6EC;
  --color-surface: #FAF0DC;
  --color-surface-2: #F5E6C8;
  --color-surface-deep: #EDD9A3;
  --color-text: #2C1810;
  --color-text-muted: #6B4C3B;
  --color-text-faint: #A07860;
  --color-magic: #7C3AED;
  --color-magic-soft: #EDE9FE;
  --color-magic-glow: #C4B5FD;
  --color-ember: #C2410C;
  --color-gold: #D97706;
  --color-forest: #166534;
  --color-night: #0F0A1E;
  --font-display: 'Lora', 'Georgia', serif;
  --font-body: 'Nunito', 'Helvetica Neue', sans-serif;
}
[data-theme="dark"] {
  --color-bg: #0F0A1E;
  --color-surface: #1A1035;
  --color-surface-2: #231548;
  --color-surface-deep: #2D2055;
  --color-text: #E8DCC8;
  --color-text-muted: #A090C0;
  --color-text-faint: #6B5A8C;
  --color-ember: #EA580C;
  --color-gold: #F59E0B;
}
```

The 19 `.woff2` files in `docs/extracted/` are copied to `frontend/public/fonts/` and referenced via `@font-face` in `design/global.css` (no Google Fonts at runtime).

### i18n

- `LocaleContext` provides `{ locale: 'uk' | 'en', setLocale, t(key) }`.
- `t(key)` does dot-path lookup in the active dictionary; missing keys fall back to UK then to the key itself (with a console warning in dev).
- Initial locale: `localStorage.kazka.locale` → `navigator.language.startsWith('en') ? 'en' : 'uk'`.
- Locale toggle pill in nav (UK / EN). Story bodies render in their saved language regardless of UI locale.

### Theme

`ThemeContext` toggles `[data-theme="dark"]` on `<html>`. Initial: `localStorage.kazka.theme` → `prefers-color-scheme`.

### SSE client

Native `fetch` + `ReadableStream`, not `EventSource` (so we can `POST` a JSON body). Implementation in `lib/sseClient.ts`: parses `event:` and `data:` lines, dispatches typed callbacks `onMeta`, `onToken`, `onDone`, `onError`.

## 9. Error Handling

- **Ollama unavailable on text generation:** SSE emits `error` event; frontend toast: `"Не вдалося зв'язатися з казкарем. Перевірте, чи запущено Ollama."` / `"Could not reach the storyteller. Make sure Ollama is running."` Persisted row stays with empty content; user can retry from form.
- **Ollama unavailable on image:** Caught in `IllustrationService`; status set to `failed`. Frontend never sees the error — `IllustrationFrame` falls back to the decorative SVG (the same parchment-tone tree silhouette from the mockup loader).
- **Image polling timeout:** After 60s of `pending`, frontend stops polling and shows the SVG placeholder. The row remains `pending` server-side (a future "regenerate" button could retry).
- **Edit/delete on missing story:** 404 → frontend toast and redirect to `/stories`.

## 10. Testing

- **Backend:** JUnit 5 + WebFlux `WebTestClient`. Unit tests for `StoryService` (request validation, prompt building, Ollama-client calls mocked with WireMock or `Mono.just(...)` stubs). Integration test for `POST /api/stories/generate` end-to-end with a mocked `OllamaClient`. Repository test with Testcontainers Postgres.
- **Frontend:** Vitest + React Testing Library. Unit tests for `sseClient`, `LocaleContext`, `StoryForm` validation. Component test for `StoryStream` rendering tokens incrementally. No e2e tests in v1.

## 11. Build & Run

`docker-compose.yml` brings up Postgres only:
```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_USER: kazkar
      POSTGRES_PASSWORD: kazkar
      POSTGRES_DB: kazkar
    ports: ["5432:5432"]
    volumes: [pgdata:/var/lib/postgresql/data]
volumes: { pgdata: {} }
```

`.env.example`:
```
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_TEXT_MODEL=gemma3:4b
OLLAMA_IMAGE_MODEL=x/flux2-klein
DB_URL=jdbc:postgresql://localhost:5432/kazkar
DB_USER=kazkar
DB_PASS=kazkar
```

`README.md` covers: prerequisites (Java 25, Node 20+, Docker, Ollama installed on host), `docker-compose up -d`, `cd backend && ./gradlew bootRun`, `cd frontend && npm install && npm run dev`, then visit `http://localhost:5173`.

## 12. Non-Goals (v1)

- No authentication or per-user accounts (archive is global)
- No "regenerate illustration" button (failed illustrations stay failed; users can edit text and re-create from form)
- No translation of existing stories (a story stays in its original language; UI chrome is the only thing that switches)
- No mobile-first redesign (responsive but desktop-primary, matching the mockup)
- No analytics, no rate limiting, no abuse prevention (local-only app)
- No story sharing (no public URLs beyond the local instance)

## 13. Constraints (from task brief)

- No cloud AI APIs — Ollama only
- All Ollama URLs from config, never hardcoded
- All Ollama calls via `WebClient`, never blocking the reactive thread
- Auto-pull both models on startup with logged progress
- CORS allows `http://localhost:5173` in dev profile
- Ukrainian-first UI (default locale `'uk'`)
