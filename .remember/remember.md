# Handoff

## State
Docker + Hugging Face migration complete and merged to main. All 10 tasks done: HuggingFaceClient/Properties, ImageStorageService (byte[]), IllustrationService, StoryService, backend Dockerfile (Java 25), frontend Dockerfile + nginx.conf, docker-compose.yml (3 services). All 17 tests pass. Smoke test confirmed http://localhost returns 200.

## Next
- Set real HUGGINGFACE_API_TOKEN in .env and do a live generation test
- Optional: add backend HEALTHCHECK to Dockerfile (requires actuator dep) for frontend depends_on reliability
- Optional: remove MySQL port 3306 host exposure from docker-compose.yml for production hardening

## Context
Project has no git remote. All commits on main directly. WebClientConfig.java was kept (not deleted) — Spring Boot 4 needs explicit WebClient.Builder bean registration. Gradle files are Groovy DSL (build.gradle / settings.gradle), not Kotlin DSL — JAR is kazka-backend-0.1.0.jar.
