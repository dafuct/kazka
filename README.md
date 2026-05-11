# Казкар — Ukrainian Fairy Tale Generator

A full-stack app that generates personalized Ukrainian fairy tales using local AI (Ollama). Everything runs on your machine — no cloud, no subscriptions, full privacy.

## Prerequisites

- **Java 25** (`sdk install java 25-open` via SDKMAN, or download from [jdk.java.net](https://jdk.java.net/25/))
- **Node.js 20+** and npm
- **Docker** (for MySQL via docker-compose)
- **Ollama** installed and running ([ollama.ai](https://ollama.ai))

## Quick Start

### 1. Start MySQL

```bash
docker-compose up -d
```

### 2. Initialize the database schema

```bash
docker exec -i kazkar-mysql mysql -ukazkar -pkazkar kazkar < backend/src/main/resources/schema.sql
```

### 3. Start the backend

```bash
cd backend
./gradlew bootRun
```

The backend starts on `http://localhost:8080`. On first run it pulls the Ollama models automatically (requires Ollama running).

### 4. Start the frontend

See the [Monorepo layout](#monorepo-layout) section for workspace commands.
The short version:

```bash
npm install
npm run frontend:dev
```

Open `http://localhost:5173`.

## Monorepo layout

The repo is an npm workspaces monorepo:

```
kazka/
├─ backend/                # Spring Boot 4 (WebFlux) — Gradle
├─ frontend/               # React 19 + Vite — npm workspace "frontend"
├─ packages/
│  └─ shared/              # @kazka/shared — TS types generated from backend OpenAPI
├─ scripts/
│  └─ gen-types.sh         # codegen script
└─ package.json            # root workspaces config
```

### Setup

```bash
npm install                 # installs all workspaces
```

### Generating shared TypeScript types

The frontend (and future mobile app) consume API types from `@kazka/shared`,
which is generated from the backend's OpenAPI spec at `/v3/api-docs`.

```bash
# Terminal 1: start the backend
cd backend && ./gradlew bootRun

# Terminal 2: regenerate types
npm run gen:types
```

Commit the regenerated `packages/shared/src/api-types.ts`. CI runs
`npm run verify:types` to fail any PR that ships stale generated types.

### Running the frontend

```bash
npm run frontend:dev        # http://localhost:5173
npm run frontend:build
npm run frontend:lint
```

## Environment Variables

Copy `.env.example` and customize as needed:

```bash
cp .env.example .env
```

| Variable            | Default                             | Description                |
|---------------------|-------------------------------------|----------------------------|
| `OLLAMA_BASE_URL`   | `http://localhost:11434`            | Ollama API base URL        |
| `OLLAMA_TEXT_MODEL` | `gemma3:4b`                         | Model for story generation |
| `OLLAMA_IMAGE_MODEL`| `x/flux2-klein`                     | Model for illustrations    |
| `DB_URL`            | `jdbc:mysql://localhost:3306/kazkar` | MySQL connection URL       |
| `DB_USER`           | `kazkar`                            | MySQL username             |
| `DB_PASS`           | `kazkar`                            | MySQL password             |
| `UPLOADS_DIR`       | `./uploads`                         | Directory for illustrations|

Pass env vars to the backend:

```bash
cd backend
OLLAMA_TEXT_MODEL=llama3.2 ./gradlew bootRun
```

## Features

- **Real-time streaming** — story text appears word by word as the AI generates it
- **Illustration generation** — experimental image generation via `x/flux2-klein` (macOS)
- **Story archive** — browse, edit, and delete past stories
- **Dark/light theme** — persisted to localStorage
- **Ukrainian & English** — switch languages in the nav bar
- **Offline-capable** — Ollama runs locally; no internet required after setup

## API

| Method | Path                          | Description                  |
|--------|-------------------------------|------------------------------|
| POST   | `/api/stories/generate`       | Stream story via SSE         |
| POST   | `/api/stories/{id}/illustrate`| Trigger image generation     |
| GET    | `/api/stories`                | List stories (paginated)     |
| GET    | `/api/stories/{id}`           | Get single story             |
| PUT    | `/api/stories/{id}`           | Update title + content       |
| DELETE | `/api/stories/{id}`           | Delete story                 |

Static uploads are served from `/uploads/`.

## Running Tests

```bash
cd backend
./gradlew test
```

Tests use Testcontainers (MySQL 8) + WireMock — Docker must be running.

## Project Structure

```
kazka/
├── backend/           # Spring Boot 4 + WebFlux (port 8080)
│   └── src/main/java/com/kazka/
│       ├── story/         # StoryController, StoryService, StoryRepository
│       ├── ollama/        # OllamaClient, OllamaModelInitializer
│       ├── illustration/  # IllustrationService, ImageStorageService
│       └── config/        # WebClient, CORS, uploads config
├── frontend/          # React 19 + TypeScript + Vite (port 5173)
│   └── src/
│       ├── components/    # form, story, home, chrome, modal
│       ├── lib/           # apiClient, sseClient, LocaleContext, ThemeContext
│       ├── pages/         # HomePage, ArchivePage, StoryDetailPage
│       └── locales/       # uk.ts, en.ts
├── docker-compose.yml
└── .env.example
```

## Notes on Image Generation

- `x/flux2-klein` is an experimental model that currently works on macOS with Apple Silicon.
- If generation fails, the UI shows a decorative SVG placeholder — no error is shown to the user.
- Generated images are saved as PNG in the `uploads/` directory and served as static resources.
