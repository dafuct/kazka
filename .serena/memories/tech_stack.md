# Tech Stack

## Backend (`backend/`)
- **Language:** Java 25 (toolchain via Gradle)
- **Framework:** Spring Boot 4.0.0
- **Build:** Gradle 9 with Kotlin DSL (`build.gradle.kts`)
- **Web:** Spring WebFlux (reactive, SSE for streaming)
- **Persistence:** Spring Data JPA + Hibernate; sync calls wrapped in `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`
- **Array column mapping:** `io.hypersistence:hypersistence-utils-hibernate-65` for `text[]`
- **Database:** PostgreSQL 16
- **Migrations:** Flyway (`flyway-core` + `flyway-database-postgresql`)
- **HTTP client:** `WebClient` (for Ollama)
- **Tests:** JUnit 5, `WebTestClient`, Testcontainers (Postgres), WireMock, Mockito

## Frontend (`frontend/`)
- **Language:** TypeScript 5
- **Framework:** React 19, no UI framework (plain CSS modules + design tokens)
- **Build/dev:** Vite 6, `@vitejs/plugin-react`
- **Routing:** React Router v7
- **i18n:** in-house `LocaleContext` + `uk.json`/`en.json` dictionaries (no library)
- **Tests:** Vitest + jsdom + React Testing Library + jest-dom

## Infrastructure
- **Postgres:** docker-compose (`postgres:16` image)
- **Ollama:** runs on the host (NOT containerized — `x/flux2-klein` needs macOS GPU)
- **Models:** `gemma3:4b` (text), `x/flux2-klein` (image). Auto-pulled on backend startup via `OllamaModelInitializer`.
