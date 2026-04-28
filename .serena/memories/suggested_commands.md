# Suggested Commands (macOS / Darwin)

## First-time prerequisites
```bash
brew install --cask temurin@25      # Java 25
brew install node                   # Node 20+
brew install --cask docker          # Docker Desktop
brew install ollama                 # Ollama (or download from ollama.com)
ollama serve                        # In a background terminal/launchctl
```

## Bring up Postgres
```bash
docker-compose up -d postgres       # Start
docker-compose ps                   # Health check
docker-compose down                 # Stop (volume preserved)
docker-compose down -v              # Stop and wipe data
```

## Backend (run from `backend/`)
```bash
./gradlew bootRun                   # Start dev server on :8080
./gradlew test                      # Run all tests (JUnit + Testcontainers + WireMock)
./gradlew test --tests StoryServiceTest          # Single class
./gradlew test --tests OllamaClientTest -i       # Verbose
./gradlew clean test                # Force fresh build
./gradlew compileJava               # Just check it compiles
./gradlew dependencies --configuration runtimeClasspath -q | head -50
```

## Frontend (run from `frontend/`)
```bash
npm install                         # First time
npm run dev                         # Vite dev server on :5173 with /api proxy to :8080
npm run build                       # Type-check + production build
npm test                            # Vitest run-once
npm run test:watch                  # Vitest watch mode
npm test -- StoryForm               # Single test by name
```

## Full local dev (4 terminals)
```bash
# t1
docker-compose up postgres
# t2
ollama serve
# t3
cd backend && ./gradlew bootRun
# t4
cd frontend && npm run dev
```
Then open http://localhost:5173

## Ollama model management
```bash
ollama list                                          # See pulled models
ollama pull gemma3:4b                                # Manual text-model pull
ollama pull x/flux2-klein                            # Manual image-model pull (mac-only)
curl http://localhost:11434/api/tags                 # API: list models
```

## Git
```bash
git status
git log --oneline -10
git add <specific files>            # NEVER use `git add .` or `-A` blindly
git commit -m "feat: …"             # Conventional Commits
```

## Darwin-specific notes
- **Find files:** prefer `Glob` (Claude tool) over BSD `find`; `find . -name "*.java"` works but is BSD `find`, NOT GNU.
- **Search content:** prefer `Grep` (Claude tool) over `grep`/`rg`.
- **Read files:** prefer `Read` over `cat`/`head`/`tail`.
- **Heredoc:** `cat <<'EOF' > file.txt` works the same as Linux.
- `ls -la` for hidden files. `du -sh` for directory size. `lsof -i :8080` to see what's on a port.
