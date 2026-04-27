# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Backend (Spring Boot 4, Java 25, Gradle 9)
```bash
cd backend
./gradlew bootRun          # start on port 8080
./gradlew test             # run all tests (requires Docker for Testcontainers)
./gradlew test --tests "com.kazka.story.StoryControllerTest#saveAndGetStory_roundtrip"  # single test
./gradlew build            # compile + test
```

### Frontend (React 19, Vite 8, TypeScript 6)
```bash
cd frontend
npm run dev                # start on port 5173
npm run build              # tsc -b && vite build
npm run lint
node_modules/.bin/tsc --noEmit  # type-check only (npx tsc installs wrong package)
```

### Infrastructure
```bash
docker-compose up -d       # start MySQL 8.4
# First-time schema init (also run after schema.sql changes):
docker exec -i kazkar-mysql mysql -ukazkar -pkazkar kazkar < backend/src/main/resources/schema.sql
```

## Architecture

### Backend
**Spring WebFlux reactive API** — all handlers return `Mono`/`Flux`. JPA is blocking, so every repository call is wrapped in `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`. Never call JPA from a reactor thread directly.

**Story generation flow:**
1. `StoryController.generate()` returns `Flux<ServerSentEvent<Object>>`
2. `StoryService` saves an empty `Story` entity (ID assigned upfront), then calls `OllamaClient.streamGenerate()`
3. Tokens stream in real-time via `.doOnNext(contentBuffer::append).map(SseEvent::token)`
4. After the stream completes, `.concatWith(Mono.fromCallable(...))` saves full content + extracted title (first line) and emits a `done` event

**SSE event types:** `meta` (id), `token` (text), `done` (id + title), `error` (message)

**Illustration generation** is fire-and-forget: `StoryController.illustrate()` calls `storyService.illustrate(id).subscribe()` and returns 202 immediately. The client polls `GET /api/stories/{id}` to check `illustrationStatus` (PENDING → READY or FAILED).

**OllamaClient** uses `bodyToFlux(String.class)` + manual Jackson 2.x parsing (`MAPPER.readTree(line)`), **not** `bodyToFlux(JsonNode.class)`. Spring Boot 4 ships Jackson 3.x (`tools.jackson`) which is incompatible with Jackson 2.x `JsonNode`. The project includes `com.fasterxml.jackson.core:jackson-databind` (2.x) explicitly for Ollama response parsing and `CharactersConverter`.

**CORS** is only enabled in the `dev` Spring profile (`CorsConfig` is `@Profile("dev")`). The dev profile is not activated by default — set `SPRING_PROFILES_ACTIVE=dev` or add `-Dspring.profiles.active=dev` to `bootRun`.

### Database
Single `stories` table. Schema lives in `schema.sql` (idempotent: DROP TABLE IF EXISTS + CREATE). Hibernate is set to `validate` — it checks that entities match the schema on startup but never modifies the DB. **Update `schema.sql` and re-run the init command whenever entity fields change.**

`characters` (List<String>) is stored as JSON via `CharactersConverter` (custom `AttributeConverter`).

### Frontend
**Three pages:** `HomePage` (form + SSE streaming), `ArchivePage` (paginated grid), `StoryDetailPage` (full story + inline edit + illustration).

**SSE client** (`lib/sseClient.ts`) uses `fetch()` + `ReadableStream` with manual line buffering, **not** `EventSource`. EventSource doesn't support POST or request bodies.

**Contexts:** `ThemeContext` (light/dark, `data-theme` attribute on `<html>`) and `LocaleContext` (uk/en), both persisted to `localStorage`.

**Design tokens** are CSS custom properties in `src/design/tokens.css`, applied via `[data-theme="dark"]` selector. Global styles and `@font-face` rules are in `src/design/global.css`.

Vite proxies `/api` and `/uploads` to `http://localhost:8080` in development.

## Key Constraints

- **Spring Boot 4** dropped Flyway and Liquibase autoconfiguration entirely — schema init uses `spring.sql.init` (`DataSourceInitializationAutoConfiguration` from `spring-boot-jdbc`). Do not add Flyway or Liquibase starters expecting autoconfiguration.
- **`@DataJpaTest` does not exist** in Spring Boot 4. Use `@SpringBootTest @ActiveProfiles("test")` instead.
- **`WebTestClient` is not auto-configured** in `@SpringBootTest`. Create manually: `WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build()`.
- Test config (`application-test.yml`) sets `spring.sql.init.mode: always` so schema.sql runs on each test container start. The schema uses `DROP TABLE IF EXISTS` to be idempotent across shared Testcontainers instances.
- The `x/flux2-klein` illustration model only works on macOS (Apple Silicon). On other platforms it fails silently; the UI shows a decorative SVG placeholder.
- Prompt templates are read from classpath at startup: `prompts/system-uk.txt`, `prompts/system-en.txt`, `prompts/illustration-style.txt`. Missing files cause a hard startup failure.
