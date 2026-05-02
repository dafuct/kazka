# Project Guidelines

## Commands

### Backend (Spring Boot 4 · Java 25 · Gradle)
- `cd backend && ./gradlew bootRun` — port 8080, requires running MySQL
- `cd backend && ./gradlew test` — requires Docker (Testcontainers)
- `cd backend && ./gradlew test --tests "com.kazka.Foo#method"`

### Frontend (React 19 · TypeScript 6 · Vite 8)
- `cd frontend && npm run dev` — port 5173
- `cd frontend && node_modules/.bin/tsc --noEmit` — type-check only (NOT `npx tsc`)
- `cd frontend && npm run lint`

### Full stack
- Copy `.env.example` → `.env` and set `HUGGINGFACE_API_TOKEN=hf_...`
- `docker-compose up --build` → http://localhost

## Git
- Feature branches only — never commit to main
- Descriptive commits: what + why

<important if="language=java">
- All handlers return `Mono`/`Flux`; never call JPA from a reactor thread
- JPA calls: wrap in `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`
- Spring Boot 4 dropped Flyway/Liquibase autoconfiguration — schema via `spring.sql.init` + `schema.sql`
- `@DataJpaTest` does not exist in SB4 — use `@SpringBootTest @ActiveProfiles("test")` + Testcontainers MySQL
- `WebTestClient` is not auto-configured — create manually: `WebTestClient.bindToServer().baseUrl("http://localhost:"+port).build()`
- Jackson: SB4 ships Jackson 3.x (`tools.jackson`); project keeps Jackson 2.x explicit for Ollama — never mix the two
- CORS only active in `dev` profile — set `SPRING_PROFILES_ACTIVE=dev` for local non-Docker dev
- Hibernate is `validate` — update `schema.sql` and re-run init whenever entity fields change
- Prompt templates read from classpath at startup (`prompts/system-uk.txt`, `system-en.txt`, `scene-extraction-system.txt`, `svg-system.txt`) — missing = hard startup failure
</important>

<important if="language=typescript,tsx">
- SSE client (`lib/sseClient.ts`) uses `fetch()` + ReadableStream — never `EventSource` (doesn't support POST bodies)
- All UI text via `useLocale()` `t` — no hardcoded Ukrainian or English strings in components
- Vite dev proxy: `/api` and `/uploads` → `http://localhost:8080`
- Design tokens in `src/design/tokens.css`; global styles and `@font-face` in `src/design/global.css`
- No test framework — verify correctness with `tsc --noEmit` + `npm run lint`
</important>
