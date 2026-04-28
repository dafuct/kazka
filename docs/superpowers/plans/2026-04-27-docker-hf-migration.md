# Docker + Hugging Face Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Ollama with Hugging Face Inference API for both text and image generation, then Dockerize the full stack (backend + frontend + MySQL) behind an nginx reverse proxy accessible at `http://localhost`.

**Architecture:** `HuggingFaceClient` replaces `OllamaClient` — text via HF's OpenAI-compatible chat completions endpoint (SSE streaming), images via HF Inference API (binary bytes). Three Docker services: `mysql` (MySQL 8.4), `backend` (Spring Boot JAR), `frontend` (nginx serving built React + proxying `/api` and `/uploads` to backend).

**Tech Stack:** Java 25, Spring Boot 4, Spring WebFlux, React 19, nginx, Docker Compose, Hugging Face Inference API (free tier).

---

## File Map

**Create:**
- `backend/src/main/java/com/kazka/hf/HuggingFaceClient.java`
- `backend/src/main/java/com/kazka/config/HuggingFaceProperties.java`
- `backend/Dockerfile`
- `backend/.dockerignore`
- `frontend/Dockerfile`
- `frontend/.dockerignore`
- `frontend/nginx.conf`

**Modify:**
- `backend/src/main/resources/application.yml`
- `backend/src/test/resources/application-test.yml`
- `backend/src/main/java/com/kazka/illustration/ImageStorageService.java`
- `backend/src/main/java/com/kazka/illustration/IllustrationService.java`
- `backend/src/main/java/com/kazka/story/StoryService.java`
- `backend/src/test/java/com/kazka/story/StoryControllerTest.java`
- `docker-compose.yml`
- `.env.example`
- `CLAUDE.md`

**Delete:**
- `backend/src/main/java/com/kazka/ollama/OllamaClient.java`
- `backend/src/main/java/com/kazka/ollama/OllamaModelInitializer.java`
- `backend/src/main/java/com/kazka/config/OllamaProperties.java`
- `backend/src/main/java/com/kazka/config/WebClientConfig.java`

---

## Task 1: HuggingFaceProperties + config files

**Files:**
- Create: `backend/src/main/java/com/kazka/config/HuggingFaceProperties.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/resources/application-test.yml`
- Modify: `.env.example`

- [ ] **Step 1: Create HuggingFaceProperties**

`KazkaApplication` uses `@ConfigurationPropertiesScan("com.kazka")` so any `@ConfigurationProperties` class in the package is auto-registered — no annotation besides `@ConfigurationProperties` needed.

```java
// backend/src/main/java/com/kazka/config/HuggingFaceProperties.java
package com.kazka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kazka.huggingface")
public class HuggingFaceProperties {

    private String apiToken = "";
    private String textModel = "google/gemma-3-4b-it";
    private String imageModel = "black-forest-labs/FLUX.1-schnell";
    private String textBaseUrl = "https://router.huggingface.co";
    private String imageBaseUrl = "https://api-inference.huggingface.co";

    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }

    public String getTextModel() { return textModel; }
    public void setTextModel(String textModel) { this.textModel = textModel; }

    public String getImageModel() { return imageModel; }
    public void setImageModel(String imageModel) { this.imageModel = imageModel; }

    public String getTextBaseUrl() { return textBaseUrl; }
    public void setTextBaseUrl(String textBaseUrl) { this.textBaseUrl = textBaseUrl; }

    public String getImageBaseUrl() { return imageBaseUrl; }
    public void setImageBaseUrl(String imageBaseUrl) { this.imageBaseUrl = imageBaseUrl; }
}
```

- [ ] **Step 2: Replace application.yml kazka.ollama section**

Replace the entire file with:

```yaml
# backend/src/main/resources/application.yml
spring:
  application:
    name: kazkar
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/kazkar}
    username: ${DB_USER:kazkar}
    password: ${DB_PASS:kazkar}
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: never
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.dialect: org.hibernate.dialect.MySQLDialect
    defer-datasource-initialization: true
server:
  port: 8080
kazka:
  huggingface:
    api-token: ${HUGGINGFACE_API_TOKEN:}
    text-model: ${HF_TEXT_MODEL:google/gemma-3-4b-it}
    image-model: ${HF_IMAGE_MODEL:black-forest-labs/FLUX.1-schnell}
    text-base-url: ${HF_TEXT_BASE_URL:https://router.huggingface.co}
    image-base-url: ${HF_IMAGE_BASE_URL:https://api-inference.huggingface.co}
  uploads:
    dir: ${UPLOADS_DIR:./uploads}
```

- [ ] **Step 3: Update application-test.yml**

Replace the entire file with:

```yaml
# backend/src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:tc:mysql:8:///kazkar_test
    username: test
    password: test
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  sql:
    init:
      mode: always
  jpa:
    hibernate:
      ddl-auto: none
kazka:
  huggingface:
    api-token: test-token
```

- [ ] **Step 4: Update .env.example**

Replace the entire file with:

```bash
# Hugging Face — get a free token at https://huggingface.co/settings/tokens
HUGGINGFACE_API_TOKEN=hf_your_token_here
HF_TEXT_MODEL=google/gemma-3-4b-it
HF_IMAGE_MODEL=black-forest-labs/FLUX.1-schnell

# Database
DB_URL=jdbc:mysql://mysql:3306/kazkar
DB_USER=kazkar
DB_PASS=kazkar
```

- [ ] **Step 5: Compile to verify**

```bash
cd backend && ./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/kazka/config/HuggingFaceProperties.java \
        backend/src/main/resources/application.yml \
        backend/src/test/resources/application-test.yml \
        .env.example
git commit -m "feat: add HuggingFaceProperties, replace Ollama config"
```

---

## Task 2: HuggingFaceClient

**Files:**
- Create: `backend/src/main/java/com/kazka/hf/HuggingFaceClient.java`

`HuggingFaceClient` owns two `WebClient` instances (different base URLs). It uses `WebClient.Builder` (Spring Boot's auto-configured prototype bean) to build each.

**Text streaming** parses HF's OpenAI-compatible SSE format:
```
data: {"choices":[{"delta":{"content":"token"},"finish_reason":null}]}
data: [DONE]
```

**Image generation** POSTs to the HF Inference API and receives binary image bytes directly.

- [ ] **Step 1: Create HuggingFaceClient**

```java
// backend/src/main/java/com/kazka/hf/HuggingFaceClient.java
package com.kazka.hf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kazka.config.HuggingFaceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class HuggingFaceClient {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient textClient;
    private final WebClient imageClient;
    private final HuggingFaceProperties props;

    public HuggingFaceClient(WebClient.Builder builder, HuggingFaceProperties props) {
        this.props = props;
        String auth = "Bearer " + props.getApiToken();
        this.textClient = builder.clone()
                .baseUrl(props.getTextBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, auth)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.imageClient = builder.clone()
                .baseUrl(props.getImageBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, auth)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();
    }

    public Flux<String> streamText(String prompt) {
        return textClient.post()
                .uri("/hf-inference/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "model", props.getTextModel(),
                        "messages", List.of(Map.of("role", "user", "content", prompt)),
                        "stream", true,
                        "max_tokens", 2048
                ))
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(line -> {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) return Flux.empty();
                    String json = trimmed.startsWith("data:") ? trimmed.substring(5).trim() : trimmed;
                    if (json.equals("[DONE]")) return Flux.empty();
                    try {
                        JsonNode node = MAPPER.readTree(json);
                        String token = node.path("choices").path(0)
                                .path("delta").path("content").asText("");
                        return token.isEmpty() ? Flux.empty() : Flux.just(token);
                    } catch (Exception e) {
                        return Flux.empty();
                    }
                });
    }

    public Mono<byte[]> generateImage(String prompt) {
        return imageClient.post()
                .uri("/models/" + props.getImageModel())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("inputs", prompt))
                .retrieve()
                .bodyToMono(byte[].class)
                .onErrorResume(e -> {
                    log.warn("HF image generation failed: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
```

- [ ] **Step 2: Compile**

```bash
cd backend && ./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/hf/HuggingFaceClient.java
git commit -m "feat: add HuggingFaceClient (text streaming + image generation)"
```

---

## Task 3: ImageStorageService — byte[] instead of base64

**Files:**
- Modify: `backend/src/main/java/com/kazka/illustration/ImageStorageService.java`

The Ollama image API returned a base64 string. HF returns raw bytes. Change `save()` signature to accept `byte[]` directly — no Base64 decoding needed.

- [ ] **Step 1: Replace save() method**

Replace the entire file:

```java
// backend/src/main/java/com/kazka/illustration/ImageStorageService.java
package com.kazka.illustration;

import com.kazka.config.UploadsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

    private final Path uploadsDir;

    @Autowired
    public ImageStorageService(UploadsProperties props) {
        this(props.getDir());
    }

    ImageStorageService(String dir) {
        this.uploadsDir = Path.of(dir);
        try {
            Files.createDirectories(uploadsDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create uploads directory", e);
        }
    }

    public String save(String storyId, byte[] imageBytes) {
        Path file = uploadsDir.resolve(storyId + ".png");
        try {
            Files.write(file, imageBytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot save image for story " + storyId, e);
        }
        return "/uploads/" + storyId + ".png";
    }

    public void delete(String storyId) {
        Path file = uploadsDir.resolve(storyId + ".png");
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete image for story {}: {}", storyId, e.getMessage());
        }
    }
}
```

- [ ] **Step 2: Compile**

```bash
cd backend && ./gradlew compileJava
```

Expected: FAIL — `IllustrationService` still calls `save(storyId, base64String)`. Fix in Task 4.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/illustration/ImageStorageService.java
git commit -m "feat: ImageStorageService saves byte[] directly (no base64)"
```

---

## Task 4: IllustrationService — use HuggingFaceClient

**Files:**
- Modify: `backend/src/main/java/com/kazka/illustration/IllustrationService.java`

Replace `OllamaClient` + `OllamaProperties` dependencies with `HuggingFaceClient`. The `generateImage()` call now returns `Mono<byte[]>` instead of `Mono<String>`.

- [ ] **Step 1: Replace IllustrationService**

```java
// backend/src/main/java/com/kazka/illustration/IllustrationService.java
package com.kazka.illustration;

import com.kazka.hf.HuggingFaceClient;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.PromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class IllustrationService {

    private static final Logger log = LoggerFactory.getLogger(IllustrationService.class);

    private final HuggingFaceClient hfClient;
    private final ImageStorageService imageStorageService;
    private final StoryRepository storyRepository;
    private final PromptBuilder promptBuilder;

    public IllustrationService(HuggingFaceClient hfClient,
                               ImageStorageService imageStorageService,
                               StoryRepository storyRepository,
                               PromptBuilder promptBuilder) {
        this.hfClient = hfClient;
        this.imageStorageService = imageStorageService;
        this.storyRepository = storyRepository;
        this.promptBuilder = promptBuilder;
    }

    public Mono<Void> generateAndStore(String storyId) {
        return Mono.fromCallable(() -> storyRepository.findById(storyId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(opt -> opt.map(Mono::just).orElse(Mono.empty()))
                .flatMap(story -> {
                    String prompt = promptBuilder.buildIllustrationPrompt(
                            story.getTitle(), story.getCharacters());
                    return hfClient.generateImage(prompt)
                            .flatMap(bytes -> saveImage(story, bytes))
                            .switchIfEmpty(markFailed(story))
                            .onErrorResume(e -> {
                                log.warn("Illustration failed for {}: {}", storyId, e.getMessage());
                                return markFailed(story);
                            });
                });
    }

    private Mono<Void> saveImage(Story story, byte[] imageBytes) {
        return Mono.fromRunnable(() -> {
            String path = imageStorageService.save(story.getId(), imageBytes);
            story.setIllustrationPath(path);
            story.setIllustrationStatus(IllustrationStatus.READY);
            storyRepository.save(story);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private Mono<Void> markFailed(Story story) {
        return Mono.fromRunnable(() -> {
            story.setIllustrationStatus(IllustrationStatus.FAILED);
            storyRepository.save(story);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public void deleteImage(String storyId) {
        imageStorageService.delete(storyId);
    }
}
```

- [ ] **Step 2: Compile**

```bash
cd backend && ./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL` (IllustrationService compiles; OllamaClient + OllamaProperties still exist so no missing-class errors yet)

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/illustration/IllustrationService.java
git commit -m "feat: IllustrationService uses HuggingFaceClient"
```

---

## Task 5: StoryService — use HuggingFaceClient

**Files:**
- Modify: `backend/src/main/java/com/kazka/story/StoryService.java`

Replace `OllamaClient.streamGenerate(model, prompt)` with `HuggingFaceClient.streamText(prompt)` — model is now embedded in the client's properties.

- [ ] **Step 1: Replace StoryService**

```java
// backend/src/main/java/com/kazka/story/StoryService.java
package com.kazka.story;

import com.kazka.hf.HuggingFaceClient;
import com.kazka.illustration.IllustrationService;
import com.kazka.story.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Service
public class StoryService {

    private final StoryRepository repository;
    private final HuggingFaceClient hfClient;
    private final PromptBuilder promptBuilder;
    private final IllustrationService illustrationService;

    public StoryService(StoryRepository repository, HuggingFaceClient hfClient,
                        PromptBuilder promptBuilder, IllustrationService illustrationService) {
        this.repository = repository;
        this.hfClient = hfClient;
        this.promptBuilder = promptBuilder;
        this.illustrationService = illustrationService;
    }

    public Flux<SseEvent> generate(GenerationRequest req) {
        String id = UUID.randomUUID().toString();
        String prompt = promptBuilder.buildPrompt(req);

        Story story = new Story();
        story.setId(id);
        story.setTitle("");
        story.setTheme(req.theme());
        story.setCharacters(req.characters());
        story.setAgeGroup(req.ageGroup());
        story.setLength(req.length());
        story.setLanguage(req.language());
        story.setContent("");
        story.setIllustrationStatus(IllustrationStatus.PENDING);

        return Mono.fromCallable(() -> repository.save(story))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(saved -> {
                    Flux<SseEvent> meta = Flux.just(SseEvent.meta(id));

                    StringBuilder contentBuffer = new StringBuilder();
                    Flux<SseEvent> tokens = hfClient.streamText(prompt)
                            .doOnNext(contentBuffer::append)
                            .map(SseEvent::token)
                            .concatWith(Mono.fromCallable(() -> {
                                String fullContent = contentBuffer.toString();
                                String[] lines = fullContent.split("\n", 2);
                                String title = lines[0].strip();
                                saved.setTitle(title);
                                saved.setContent(fullContent);
                                repository.save(saved);
                                return SseEvent.done(id, title);
                            }).subscribeOn(Schedulers.boundedElastic()))
                            .onErrorResume(e -> Flux.just(SseEvent.error(e.getMessage())));

                    return meta.concatWith(tokens);
                });
    }

    public Mono<Void> illustrate(String id) {
        return illustrationService.generateAndStore(id)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<PageResponse<StoryDto>> list(int page, int size) {
        return Mono.fromCallable(() -> {
            Page<Story> p = repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
            return new PageResponse<>(
                    p.getContent().stream().map(StoryDto::from).toList(),
                    p.getNumber(), p.getSize(), p.getTotalElements());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<StoryDto> findById(String id) {
        return Mono.fromCallable(() -> repository.findById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .map(opt -> opt.map(StoryDto::from)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    public Mono<StoryDto> update(String id, UpdateStoryRequest req) {
        return Mono.fromCallable(() -> repository.findById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(opt -> {
                    Story story = opt.orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    story.setTitle(req.title());
                    story.setContent(req.content());
                    return Mono.fromCallable(() -> repository.save(story))
                            .subscribeOn(Schedulers.boundedElastic());
                })
                .map(StoryDto::from);
    }

    public Mono<Void> delete(String id) {
        return Mono.fromCallable(() -> repository.findById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(opt -> {
                    Story story = opt.orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    return Mono.fromRunnable(() -> {
                        if (story.getIllustrationPath() != null) {
                            illustrationService.deleteImage(id);
                        }
                        repository.deleteById(id);
                    }).subscribeOn(Schedulers.boundedElastic());
                }).then();
    }
}
```

- [ ] **Step 2: Compile**

```bash
cd backend && ./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/story/StoryService.java
git commit -m "feat: StoryService uses HuggingFaceClient for text streaming"
```

---

## Task 6: Delete Ollama classes + fix tests + verify

**Files:**
- Delete: `backend/src/main/java/com/kazka/ollama/OllamaClient.java`
- Delete: `backend/src/main/java/com/kazka/ollama/OllamaModelInitializer.java`
- Delete: `backend/src/main/java/com/kazka/config/OllamaProperties.java`
- Delete: `backend/src/main/java/com/kazka/config/WebClientConfig.java`
- Modify: `backend/src/test/java/com/kazka/story/StoryControllerTest.java`

- [ ] **Step 1: Delete the four Ollama files**

```bash
rm backend/src/main/java/com/kazka/ollama/OllamaClient.java
rm backend/src/main/java/com/kazka/ollama/OllamaModelInitializer.java
rm backend/src/main/java/com/kazka/config/OllamaProperties.java
rm backend/src/main/java/com/kazka/config/WebClientConfig.java
```

- [ ] **Step 2: Compile to verify no dangling references**

```bash
cd backend && ./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL` — all Ollama references were already removed in Tasks 4 and 5.

- [ ] **Step 3: Update StoryControllerTest**

The WireMock setup was only providing `kazka.ollama.base-url`. None of the tests call the AI API, so WireMock is no longer needed. Replace the entire file:

```java
// backend/src/test/java/com/kazka/story/StoryControllerTest.java
package com.kazka.story;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StoryControllerTest {

    @LocalServerPort
    int port;

    WebTestClient webTestClient() {
        return WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Autowired
    StoryRepository storyRepository;

    @Test
    void getStories_returnsEmptyPage() {
        webTestClient().get().uri("/api/stories")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items").isArray()
                .jsonPath("$.total").isEqualTo(0);
    }

    @Test
    void getStory_notFound_returns404() {
        webTestClient().get().uri("/api/stories/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateStory_notFound_returns404() {
        webTestClient().put().uri("/api/stories/" + UUID.randomUUID())
                .bodyValue("""
                        {"title":"Updated","content":"New content"}
                        """)
                .header("Content-Type", "application/json")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteStory_notFound_returns404() {
        webTestClient().delete().uri("/api/stories/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void generate_invalidRequest_returns400() {
        webTestClient().post().uri("/api/stories/generate")
                .bodyValue("""
                        {"theme":"","characters":[],"ageGroup":"invalid","length":"x","language":"fr"}
                        """)
                .header("Content-Type", "application/json")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void saveAndGetStory_roundtrip() {
        Story story = new Story();
        story.setId(UUID.randomUUID().toString());
        story.setTitle("Test Story");
        story.setTheme("test");
        story.setCharacters(List.of("hero"));
        story.setAgeGroup("6-8");
        story.setLength("short");
        story.setLanguage("uk");
        story.setContent("Once upon a time...");
        story.setIllustrationStatus(IllustrationStatus.PENDING);
        storyRepository.save(story);

        webTestClient().get().uri("/api/stories/" + story.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Test Story")
                .jsonPath("$.illustrationStatus").isEqualTo("PENDING");
    }
}
```

- [ ] **Step 4: Run all tests**

```bash
cd backend && ./gradlew test
```

Expected: `BUILD SUCCESSFUL`, all 6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: remove Ollama classes, simplify tests — HF migration complete"
```

---

## Task 7: backend/Dockerfile + .dockerignore

**Files:**
- Create: `backend/Dockerfile`
- Create: `backend/.dockerignore`

Multi-stage build: stage 1 compiles the JAR with JDK 25, stage 2 runs it with JRE 25.

- [ ] **Step 1: Create backend/.dockerignore**

```
# backend/.dockerignore
build/
.gradle/
.idea/
*.iml
```

- [ ] **Step 2: Create backend/Dockerfile**

```dockerfile
# backend/Dockerfile
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle/ gradle/
RUN chmod +x gradlew

COPY build.gradle settings.gradle ./
COPY src/ src/

RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/kazkar-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 3: Verify the image builds**

```bash
cd /path/to/kazka
docker build -t kazkar-backend ./backend
```

Expected: image builds successfully. The `bootJar` step will take 2–5 minutes on first run (downloads Gradle dependencies).

- [ ] **Step 4: Commit**

```bash
git add backend/Dockerfile backend/.dockerignore
git commit -m "feat: add backend multi-stage Dockerfile (Java 25)"
```

---

## Task 8: frontend/nginx.conf + Dockerfile + .dockerignore

**Files:**
- Create: `frontend/nginx.conf`
- Create: `frontend/Dockerfile`
- Create: `frontend/.dockerignore`

nginx serves the built React SPA and proxies `/api/` and `/uploads/` to `backend:8080`. SSE (story streaming) needs `proxy_buffering off` so tokens reach the browser immediately.

- [ ] **Step 1: Create frontend/.dockerignore**

```
# frontend/.dockerignore
node_modules/
dist/
*.md
```

- [ ] **Step 2: Create frontend/nginx.conf**

```nginx
# frontend/nginx.conf
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    gzip on;
    gzip_types text/plain text/css application/json application/javascript
               text/javascript application/xml;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        # Required for SSE streaming — disable buffering so tokens reach browser immediately
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
    }

    location /uploads/ {
        proxy_pass http://backend:8080/uploads/;
        proxy_set_header Host $host;
    }
}
```

- [ ] **Step 3: Create frontend/Dockerfile**

```dockerfile
# frontend/Dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

- [ ] **Step 4: Verify the image builds**

```bash
docker build -t kazkar-frontend ./frontend
```

Expected: image builds successfully.

- [ ] **Step 5: Commit**

```bash
git add frontend/nginx.conf frontend/Dockerfile frontend/.dockerignore
git commit -m "feat: add frontend Dockerfile (nginx + React build)"
```

---

## Task 9: docker-compose.yml

**Files:**
- Modify: `docker-compose.yml`

Three services: `mysql`, `backend`, `frontend`. The schema is auto-initialized via MySQL's `initdb` mechanism (only runs when the data volume is empty — safe for restarts). A named `uploads` volume is shared with `backend` so generated images persist.

- [ ] **Step 1: Replace docker-compose.yml entirely**

```yaml
# docker-compose.yml
services:
  mysql:
    image: mysql:8.4
    container_name: kazkar-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_USER: kazkar
      MYSQL_PASSWORD: kazkar
      MYSQL_DATABASE: kazkar
    ports:
      - "3306:3306"
    volumes:
      - mysqldata:/var/lib/mysql
      - ./backend/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/schema.sql:ro
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "kazkar", "-pkazkar"]
      interval: 5s
      timeout: 3s
      retries: 10

  backend:
    build: ./backend
    environment:
      DB_URL: jdbc:mysql://mysql:3306/kazkar
      DB_USER: kazkar
      DB_PASS: kazkar
      HUGGINGFACE_API_TOKEN: ${HUGGINGFACE_API_TOKEN}
      HF_TEXT_MODEL: ${HF_TEXT_MODEL:-google/gemma-3-4b-it}
      HF_IMAGE_MODEL: ${HF_IMAGE_MODEL:-black-forest-labs/FLUX.1-schnell}
      UPLOADS_DIR: /uploads
    volumes:
      - uploads:/uploads
    depends_on:
      mysql:
        condition: service_healthy

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysqldata: {}
  uploads: {}
```

- [ ] **Step 2: Commit**

```bash
git add docker-compose.yml
git commit -m "feat: full docker-compose with backend, frontend, MySQL, nginx"
```

---

## Task 10: Update CLAUDE.md + smoke test

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Update CLAUDE.md Commands section**

Replace the Commands section with:

```markdown
## Commands

### Backend (Spring Boot 4, Java 25, Gradle 9)
```bash
cd backend
./gradlew bootRun          # start on port 8080 (requires local MySQL)
./gradlew test             # run all tests (requires Docker for Testcontainers)
./gradlew test --tests "com.kazka.story.StoryControllerTest#saveAndGetStory_roundtrip"
./gradlew build
node_modules/.bin/tsc --noEmit  # type-check only (npx tsc installs wrong package)
```

### Frontend (React 19, Vite 8, TypeScript 6)
```bash
cd frontend
npm run dev                # start on port 5173
npm run build
npm run lint
```

### Full stack via Docker (recommended)
```bash
# First time: copy and fill in your HF token
cp .env.example .env
# edit .env and set HUGGINGFACE_API_TOKEN=hf_...

docker-compose up --build  # builds images, starts all services
# App available at http://localhost

docker-compose up          # subsequent runs (no rebuild)
docker-compose down        # stop
docker-compose down -v     # stop and delete all data (MySQL + uploads)
```

### Infrastructure (local dev only)
```bash
docker-compose up -d mysql                  # MySQL only
docker exec -i kazkar-mysql mysql -ukazkar -pkazkar kazkar < backend/src/main/resources/schema.sql
```
```

- [ ] **Step 2: Update CLAUDE.md Key Constraints section — add HF notes**

Add after the existing constraints:

```markdown
- **HuggingFaceClient** owns two WebClient instances built in its constructor: `textClient` (base: `kazka.huggingface.text-base-url`) for OpenAI-compatible chat completions streaming, and `imageClient` (base: `kazka.huggingface.image-base-url`) for binary image generation. Both URLs are configurable so WireMock can intercept them in future tests.
- **Image generation** returns `byte[]` from `HuggingFaceClient.generateImage()` and is saved directly by `ImageStorageService.save(storyId, byte[])` — no base64 decoding.
- **Docker:** `docker-compose up --build` is the one-command start. The `HUGGINGFACE_API_TOKEN` env var must be set in `.env`. Schema is auto-initialized via MySQL `initdb` on first run only (volume must be empty).
- **SSE proxy:** nginx has `proxy_buffering off` on `/api/` — required for story streaming tokens to reach the browser in real-time.
```

- [ ] **Step 3: Commit CLAUDE.md**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md for HF migration and Docker workflow"
```

- [ ] **Step 4: Create .env from .env.example and set token**

```bash
cp .env.example .env
# Open .env and set HUGGINGFACE_API_TOKEN to your real token from https://huggingface.co/settings/tokens
```

- [ ] **Step 5: Smoke test — docker-compose up**

```bash
docker-compose up --build
```

Expected output (in order):
1. MySQL starts, runs healthcheck (passes after ~10s)
2. Backend starts (Spring Boot banner, then `Started KazkaApplication`)
3. Frontend builds and nginx starts

- [ ] **Step 6: Verify the app at http://localhost**

Open `http://localhost` in a browser.

Expected:
- Nav bar with "Казкар" brand visible
- Story form loads on `/`
- `/stories` loads the archive (empty list)
- No console errors

- [ ] **Step 7: Final commit**

```bash
git add .env.example
git commit -m "chore: confirm docker-compose smoke test passes"
```
