# Task Completion Checklist

When a plan task (or any meaningful change) is "done", run this checklist before committing or claiming success.

## After every backend task
1. **Tests pass:** `cd backend && ./gradlew test`
2. **Compile clean:** Gradle's test phase compiles too — if test passes, compile passes.
3. **No accidental blocking I/O on WebFlux thread** — JPA calls inside reactive pipelines must be wrapped in `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`.
4. **Commit:** `git add <specific files> && git commit -m "feat|test|chore|fix: …"`

## After every frontend task
1. **Type-check + build:** `cd frontend && npm run build`
2. **Tests pass:** `cd frontend && npm test`
3. **Both `uk.json` and `en.json` updated** if any new user-visible string was added.
4. **Co-located CSS imported** if a new component was added.
5. **Commit:** `git add <specific files> && git commit -m "feat: …"`

## After UI changes (manual smoke-test)
The plan's Task 31 documents the full manual flow. At minimum verify:
- Story streams word-by-word in the browser (don't trust unit tests for this).
- Theme toggle (☾/☀) and Locale toggle (UK/EN) both flip and persist across reload.
- `/stories/:id` edit + delete flows work end-to-end.

## Before claiming "done" for the whole feature
Run the full Task 31 checklist (manual end-to-end with Ollama running).

## NEVER
- Commit `.env` or anything from `backend/uploads/`.
- Push to remote unless the user explicitly asks.
- Use `git add .` or `git add -A` — always stage specific files.
- Skip hooks (`--no-verify`) or amend already-committed work without explicit user OK.
- Mark a task complete without seeing tests pass for that task.
