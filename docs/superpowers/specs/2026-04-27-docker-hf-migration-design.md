# Docker + Hugging Face Migration Design

## Goal

Run the full Казкар stack (frontend + backend + MySQL) via a single `docker-compose up` command, accessible at `http://localhost`. Replace Ollama with Hugging Face Inference API for both text and image generation, removing all local AI dependencies.

## Architecture

```
Browser → http://localhost
            │
            ▼
        nginx:80
        ├── /          → serves built React SPA (static files)
        ├── /api/*     → proxy → backend:8080
        └── /uploads/* → proxy → backend:8080
            │
            ▼
        backend:8080 (Spring Boot)
        ├── text generation  → HF API (gemma-3-4b-it)
        └── image generation → HF API (FLUX.1-schnell)
            │
            ▼
        mysql:3306
```

No Ollama service. No local GPU required. Works on any Linux server (Hetzner, AWS, etc.).

## Tech Stack

- **Backend:** Java 25, Spring Boot 4, Spring WebFlux, MySQL 8.4
- **Frontend:** React 19, Vite 8, nginx alpine
- **AI:** Hugging Face Inference API (free tier)
  - Text model: `google/gemma-3-4b-it` via `/v1/chat/completions` (OpenAI-compatible, streaming)
  - Image model: `black-forest-labs/FLUX.1-schnell` via HF Inference API (binary PNG response)
- **Infrastructure:** Docker Compose

---

## Components

### 1. `backend/Dockerfile` (multi-stage)

**Stage 1 — build:**
- Base: `eclipse-temurin:25-jdk`
- Copy Gradle wrapper + source, run `./gradlew bootJar -x test`
- Output: `build/libs/kazkar-0.1.0.jar`

**Stage 2 — run:**
- Base: `eclipse-temurin:25-jre`
- Copy JAR from stage 1
- Expose 8080
- Entrypoint: `java -jar kazkar-0.1.0.jar`

### 2. `frontend/Dockerfile` (multi-stage)

**Stage 1 — build:**
- Base: `node:20-alpine`
- `npm ci && npm run build`
- Output: `dist/`

**Stage 2 — serve:**
- Base: `nginx:alpine`
- Copy `dist/` to `/usr/share/nginx/html`
- Copy custom `nginx.conf`
- Expose 80

### 3. `frontend/nginx.conf`

- Serve static files from `/usr/share/nginx/html`
- `try_files $uri $uri/ /index.html` — SPA routing (React Router)
- `location /api/` → `proxy_pass http://backend:8080/api/`
- `location /uploads/` → `proxy_pass http://backend:8080/uploads/`
- Gzip enabled for JS/CSS/JSON

### 4. `docker-compose.yml` (updated)

Three services:
- `mysql` — unchanged (image: mysql:8.4, healthcheck, named volume)
- `backend` — built from `./backend`, depends_on mysql healthy, env vars from `.env`
- `frontend` — built from `./frontend`, depends_on backend, ports `80:80`

Uploads directory mounted as a named volume shared between host and backend so generated images persist across restarts.

### 5. `HuggingFaceClient.java` (replaces `OllamaClient`)

**Text streaming** — `Flux<String> streamText(String prompt)`:
```
POST https://router.huggingface.co/hf-inference/v1/chat/completions
Authorization: Bearer {token}
Body: { model, messages: [{role: user, content: prompt}], stream: true, max_tokens: 2048 }
```
Response: SSE stream, each chunk is `data: {"choices":[{"delta":{"content":"token"}}]}`.
Parse `choices[0].delta.content` for each chunk. Stop on `data: [DONE]`.

**Image generation** — `Mono<byte[]> generateImage(String prompt)`:
```
POST https://api-inference.huggingface.co/models/black-forest-labs/FLUX.1-schnell
Authorization: Bearer {token}
Body: { inputs: prompt }
```
Response: binary image bytes (PNG/JPEG). Save directly to uploads directory.

### 6. `HuggingFaceProperties.java` (replaces `OllamaProperties`)

```yaml
kazka:
  huggingface:
    api-token: ${HUGGINGFACE_API_TOKEN}
    text-model: ${HF_TEXT_MODEL:google/gemma-3-4b-it}
    image-model: ${HF_IMAGE_MODEL:black-forest-labs/FLUX.1-schnell}
```

### 7. `ImageStorageService` (minor update)

Current code decodes base64 (from Ollama). HF returns binary bytes directly.
Add overload: `save(String storyId, byte[] imageBytes)` alongside existing base64 method,
or replace entirely since Ollama is gone.

### 8. Deletions

- `OllamaClient.java` — deleted
- `OllamaProperties.java` — deleted
- `OllamaModelInitializer.java` — deleted (no model pulling needed)
- `WebClientConfig.java` — update or replace with HF-specific WebClient bean

---

## Environment Variables

`.env.example` updated:

```bash
# Hugging Face
HUGGINGFACE_API_TOKEN=hf_your_token_here
HF_TEXT_MODEL=google/gemma-3-4b-it
HF_IMAGE_MODEL=black-forest-labs/FLUX.1-schnell

# Database
DB_URL=jdbc:mysql://mysql:3306/kazkar   # note: service name, not localhost
DB_USER=kazkar
DB_PASS=kazkar
```

Note: `DB_URL` changes from `localhost:3306` to `mysql:3306` (Docker service name).

---

## Schema Initialization

MySQL `initdb` mechanism: mount `schema.sql` into `/docker-entrypoint-initdb.d/`. MySQL runs this automatically on first container creation (when data volume is empty). On subsequent starts the file is ignored — data persists. No manual `docker exec` needed.

---

## Access URL

`http://localhost` — served by nginx on port 80.

For Hetzner/AWS deployment: same setup, just point your domain or use the server's public IP. For HTTPS, add certbot + Let's Encrypt to nginx (out of scope for this plan — can be added separately).

---

## HF Free Tier Limits

- Text (Inference API): ~300–500 requests/day on free tier
- Image (Inference API): similar rate limits
- Adequate for personal/family use of a fairy tale generator

## Privacy Note

With this approach, story prompts and content pass through Hugging Face's API servers. This replaces the original fully-local design. Acceptable trade-off for cloud deployment; use the local Ollama setup if full privacy is required.
