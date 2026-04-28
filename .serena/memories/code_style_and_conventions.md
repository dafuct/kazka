# Code Style and Conventions

## Cross-cutting
- **TDD always.** Write the failing test first, then minimal implementation, then verify, then commit. Each commit is one task from the plan.
- **Conventional Commits** prefixes: `feat:`, `chore:`, `test:`, `fix:`, `docs:`.
- **Frequent commits.** Final step of every plan task is a commit.
- **YAGNI / DRY.** No speculative abstractions, no dead code, no commented-out code.
- **No emojis** unless the user asks.
- **No hardcoded URLs or model names** — always read from `OllamaProperties` / config.

## Backend (Java)
- **Package root:** `com.kazka` (subpackages: `config`, `ollama`, `story`, `story.dto`, `illustration`).
- **Records** for DTOs and `@ConfigurationProperties`.
- **Constructor injection** only. No `@Autowired` on fields.
- **No blocking the WebFlux thread.** Wrap any JPA/IO in `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`.
- **Validation** via `jakarta.validation` annotations on records (`@NotBlank`, `@Size`, `@Pattern`).
- **Exception handling** centralized in `GlobalExceptionHandler` (`@RestControllerAdvice`).
- **Tests** live under `src/test/java/com/kazka/<same-package>/<ClassName>Test.java`.
- **AssertJ** for assertions; **WireMock** for HTTP stubs; **Testcontainers JDBC URL** (`jdbc:tc:postgresql:16:///kazkar_test`) for repository tests.
- **Profiles:** `dev` (CORS for `localhost:5173`), `test` (Testcontainers).

## Frontend (TypeScript / React)
- **Strict TypeScript** (`"strict": true`, `noUnusedLocals`, `noUnusedParameters`).
- **Function components** + hooks. No class components.
- **Co-located CSS:** each component has a sibling `.css` file imported by the component (e.g., `Nav.tsx` + `Nav.css`).
- **Design tokens** via CSS custom properties on `:root` (and `[data-theme="dark"]` override). Lifted from `docs/extracted/template.html` so colors match the mockup exactly.
- **Self-hosted fonts** in `frontend/public/fonts/` (copied from `docs/extracted/asset_*.woff2`). NEVER reach out to Google Fonts.
- **i18n:** every user-visible string goes through `t('namespace.key')`. Both `uk.json` and `en.json` must have the same key set.
- **API:** all calls through `lib/apiClient.ts`. SSE streaming through `lib/sseClient.ts` (uses `fetch` + `ReadableStream`, NOT `EventSource`, because we POST a body).
- **Theme + locale:** stored in `localStorage` under keys `kazka.theme` and `kazka.locale`.
- **Tests** co-located: `Component.tsx` + `Component.test.tsx`.

## Plan execution
- The implementation plan in `docs/superpowers/plans/2026-04-27-kazkar.md` is the source of truth for sequencing. Each numbered task is bite-sized (~2–5 min per step) and has full code in the steps. Don't deviate from it without re-running the brainstorming/writing-plans flow.
