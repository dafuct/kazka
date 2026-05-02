# PNG Illustrations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace inline-SVG illustrations on home/how/preview marketing sections with PNG carousels (3 ages × 2 themes), and revert the AI illustration backend from text-LLM SVG output to image-diffusion PNG output with age-specific child-drawing style.

**Architecture:** Two-phase backend pipeline kept (LLM scene-extraction → image-diffusion), with three age-style and two theme prompt files. New reusable `<IllustrationCarousel />` React component renders age-tabbed PNGs with auto-cycle and theme switching. 18 sample PNGs generated once via a Spring profile-gated CLI runner and committed to `frontend/public/illustrations/`.

**Tech Stack:** Spring Boot 4 (Java 25, Gradle, WebFlux, JPA, Testcontainers, JUnit 5, Mockito, AssertJ); React 19 + TypeScript 6 + Vite 8 + CSS Modules; HuggingFace Inference Router (`black-forest-labs/FLUX.1-schnell` for images, `Qwen/Qwen2.5-72B-Instruct` for scene extraction).

**Source spec:** `docs/superpowers/specs/2026-05-02-png-illustrations-design.md`

---

## Pre-flight

- [ ] **Step P1: Read the spec**

Read `docs/superpowers/specs/2026-05-02-png-illustrations-design.md` end-to-end. Every task below traces back to a spec section.

- [ ] **Step P2: Confirm current branch is clean except for the previous in-progress UI work**

Run: `git status`
Expected: design doc commit `da2425b` is the latest; the working tree shows the in-progress edits to chrome/Footer.tsx, chrome/Nav.tsx, ArchiveShelf.tsx, Features.tsx, HowItWorks.tsx, NightCta.tsx, StoryPreview.{tsx,module.css}, locales/{en,uk}.ts, ArchivePage.tsx, HomePage.tsx — leave these untouched; this plan does not depend on them.

---

## Task 1: Add `Theme` enum and 6 image-style prompt files

**Files:**
- Create: `backend/src/main/java/com/kazka/story/Theme.java`
- Create: `backend/src/main/resources/prompts/image-style-3-5-light.txt`
- Create: `backend/src/main/resources/prompts/image-style-3-5-dark.txt`
- Create: `backend/src/main/resources/prompts/image-style-6-8-light.txt`
- Create: `backend/src/main/resources/prompts/image-style-6-8-dark.txt`
- Create: `backend/src/main/resources/prompts/image-style-9-12-light.txt`
- Create: `backend/src/main/resources/prompts/image-style-9-12-dark.txt`

These are pure additions; no test required at this stage — `PromptBuilder` tests in Task 2 will exercise them via `ClassPathResource`.

- [ ] **Step 1.1: Create `Theme.java`**

Create `backend/src/main/java/com/kazka/story/Theme.java`:

```java
package com.kazka.story;

public enum Theme {
    LIGHT, DARK;

    public String slug() {
        return name().toLowerCase();
    }
}
```

- [ ] **Step 1.2: Create `image-style-3-5-light.txt`**

Create `backend/src/main/resources/prompts/image-style-3-5-light.txt` (single line, no leading/trailing blank lines):

```
A child's drawing made with thick wax crayons by a 4-year-old. Scribbled lines, simple round shapes, big circular heads, stick limbs, bright primary colors on warm cream paper, naive perspective, no shading, drawn in a children's notebook.
```

- [ ] **Step 1.3: Create `image-style-3-5-dark.txt`**

Create `backend/src/main/resources/prompts/image-style-3-5-dark.txt`:

```
A child's drawing made with thick wax crayons by a 4-year-old. Scribbled lines, simple round shapes, big circular heads, stick limbs, glowing neon crayon strokes on dark navy paper, naive perspective, no shading, drawn in a bedtime sketchbook.
```

- [ ] **Step 1.4: Create `image-style-6-8-light.txt`**

Create `backend/src/main/resources/prompts/image-style-6-8-light.txt`:

```
A child's drawing with markers and colored pencils by a 7-year-old. Recognizable forms with eyes-nose-mouth, slightly better proportions, varied bright colors on light cream paper, simple foreground and background, no shading, school art style.
```

- [ ] **Step 1.5: Create `image-style-6-8-dark.txt`**

Create `backend/src/main/resources/prompts/image-style-6-8-dark.txt`:

```
A child's drawing with markers and colored pencils by a 7-year-old. Recognizable forms with eyes-nose-mouth, slightly better proportions, glowing rich colors on dark indigo paper, simple foreground and background, gentle moon-lit highlights, school art style.
```

- [ ] **Step 1.6: Create `image-style-9-12-light.txt`**

Create `backend/src/main/resources/prompts/image-style-9-12-light.txt`:

```
A pencil and colored-pencil sketch by a 10-year-old, on warm cream textured paper. Recognizable detailed forms, attempt at shading and texture, foreground and background distinction, soft daylight, slightly sophisticated school-age child art.
```

- [ ] **Step 1.7: Create `image-style-9-12-dark.txt`**

Create `backend/src/main/resources/prompts/image-style-9-12-dark.txt`:

```
A pencil and colored-pencil sketch by a 10-year-old, on dark navy textured paper. Recognizable detailed forms, attempt at shading and texture, foreground and background distinction, glowing highlights from moonlight or candlelight, slightly sophisticated school-age child art.
```

- [ ] **Step 1.8: Compile to confirm placement**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 1.9: Commit**

```bash
git add backend/src/main/java/com/kazka/story/Theme.java backend/src/main/resources/prompts/image-style-*.txt
git commit -m "feat(backend): add Theme enum and 6 image-style prompt files

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Add `PromptBuilder` image-prompt methods (TDD)

**Files:**
- Modify: `backend/src/main/java/com/kazka/story/PromptBuilder.java`
- Test: `backend/src/test/java/com/kazka/story/PromptBuilderTest.java`

The `buildSvg*` methods stay in this task; we delete them in Task 4 once everything that depended on them has been replaced.

- [ ] **Step 2.1: Write failing tests in `PromptBuilderTest.java`**

Append to `backend/src/test/java/com/kazka/story/PromptBuilderTest.java` before the closing brace:

```java
    @Test
    void buildImageStylePreamble_3_5_light_containsCrayonAndCream() {
        String style = builder.buildImageStylePreamble("3-5", Theme.LIGHT);
        assertThat(style).contains("4-year-old");
        assertThat(style).contains("crayons");
        assertThat(style).contains("cream paper");
    }

    @Test
    void buildImageStylePreamble_9_12_dark_containsPencilAndNavy() {
        String style = builder.buildImageStylePreamble("9-12", Theme.DARK);
        assertThat(style).contains("10-year-old");
        assertThat(style).contains("pencil");
        assertThat(style).contains("navy");
    }

    @Test
    void buildImageStylePreamble_unknownAge_fallsBackTo6to8() {
        // Story.ageGroup is unconstrained text; non-mapped values must not crash
        String style = builder.buildImageStylePreamble("100-200", Theme.LIGHT);
        assertThat(style).contains("7-year-old");
    }

    @Test
    void buildImagePrompt_combinesStylePreambleAndScene() {
        Story story = new Story();
        story.setAgeGroup("6-8");
        story.setCharacters(List.of("Mia"));

        String prompt = builder.buildImagePrompt(story, "a fox under a tree", Theme.LIGHT);

        assertThat(prompt).contains("7-year-old");          // from style
        assertThat(prompt).contains("a fox under a tree");  // from scene
    }

    @Test
    void buildImagePrompt_withNullScene_usesEmpty() {
        Story story = new Story();
        story.setAgeGroup("3-5");

        String prompt = builder.buildImagePrompt(story, null, Theme.DARK);

        assertThat(prompt).contains("4-year-old");
        // scene part absent, prompt still well-formed
        assertThat(prompt).doesNotContain("null");
    }
```

- [ ] **Step 2.2: Run tests to verify they fail**

Run: `cd backend && ./gradlew test --tests "com.kazka.story.PromptBuilderTest" -i`
Expected: FAILURE — `cannot find symbol: method buildImageStylePreamble(...)` and `buildImagePrompt(...)`.

- [ ] **Step 2.3: Add the methods to `PromptBuilder.java`**

In `backend/src/main/java/com/kazka/story/PromptBuilder.java`:

Replace the constructor body (lines 28–34) with:

```java
    private final String storySystem;
    private final String editorUk;
    private final String editorEn;
    private final String sceneExtractionSystem;
    private final String svgSystem;
    private final Map<String, Map<Theme, String>> imageStyleByAge;

    public PromptBuilder() {
        this.storySystem = readResource("prompts/story-system.txt");
        this.editorUk = readResource("prompts/editor-uk.txt");
        this.editorEn = readResource("prompts/editor-en.txt");
        this.sceneExtractionSystem = readResource("prompts/scene-extraction-system.txt");
        this.svgSystem = readResource("prompts/svg-system.txt");
        this.imageStyleByAge = Map.of(
                "3-5",  Map.of(
                        Theme.LIGHT, readResource("prompts/image-style-3-5-light.txt"),
                        Theme.DARK,  readResource("prompts/image-style-3-5-dark.txt")),
                "6-8",  Map.of(
                        Theme.LIGHT, readResource("prompts/image-style-6-8-light.txt"),
                        Theme.DARK,  readResource("prompts/image-style-6-8-dark.txt")),
                "9-12", Map.of(
                        Theme.LIGHT, readResource("prompts/image-style-9-12-light.txt"),
                        Theme.DARK,  readResource("prompts/image-style-9-12-dark.txt"))
        );
    }
```

Add these two methods at the end of the class, before the static helpers:

```java
    public String buildImageStylePreamble(String ageGroup, Theme theme) {
        Map<Theme, String> byTheme = imageStyleByAge.getOrDefault(ageGroup, imageStyleByAge.get("6-8"));
        return byTheme.get(theme).strip();
    }

    public String buildImagePrompt(Story story, String scene, Theme theme) {
        String style = buildImageStylePreamble(story.getAgeGroup(), theme);
        String safeScene = (scene == null || scene.isBlank()) ? "" : " " + scene.strip();
        return style + safeScene;
    }
```

- [ ] **Step 2.4: Run tests to verify they pass**

Run: `cd backend && ./gradlew test --tests "com.kazka.story.PromptBuilderTest" -i`
Expected: All `PromptBuilderTest` tests pass (including the older ones).

- [ ] **Step 2.5: Commit**

```bash
git add backend/src/main/java/com/kazka/story/PromptBuilder.java backend/src/test/java/com/kazka/story/PromptBuilderTest.java
git commit -m "feat(backend): add age- and theme-aware image prompt builder

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Add `HuggingFaceClient.generateImage(...)` and rename `svgModel` → `sceneModel`

**Files:**
- Modify: `backend/src/main/java/com/kazka/hf/HuggingFaceClient.java`
- Modify: `backend/src/main/java/com/kazka/config/HuggingFaceProperties.java`
- Modify: `backend/src/main/resources/application.yml`

The `imageModel` property already exists (`HuggingFaceProperties.java:11`, `application.yml:25`). We only need to wire up the call. We rename `svgModel` to `sceneModel` because it's now used solely for the scene-extraction LLM call.

No new unit tests for the HTTP method — exercised via integration through `IllustrationServiceTest` added in Task 5.

- [ ] **Step 3.1: Update `HuggingFaceProperties.java`**

In `backend/src/main/java/com/kazka/config/HuggingFaceProperties.java`, rename `svgModel` to `sceneModel`:

Replace lines 11–12:
```java
    private String imageModel = "black-forest-labs/FLUX.1-schnell";
    private String svgModel = "Qwen/Qwen2.5-72B-Instruct";
```

with:
```java
    private String imageModel = "black-forest-labs/FLUX.1-schnell";
    private String sceneModel = "Qwen/Qwen2.5-72B-Instruct";
```

Replace the `getSvgModel`/`setSvgModel` accessors (lines 28–29) with:
```java
    public String getSceneModel() { return sceneModel; }
    public void setSceneModel(String sceneModel) { this.sceneModel = sceneModel; }
```

- [ ] **Step 3.2: Update `application.yml`**

In `backend/src/main/resources/application.yml`, replace the line:
```yaml
    svg-model: ${HF_SVG_MODEL:Qwen/Qwen2.5-72B-Instruct}
```
with:
```yaml
    scene-model: ${HF_SCENE_MODEL:Qwen/Qwen2.5-72B-Instruct}
```

- [ ] **Step 3.3: Update `HuggingFaceClient.java` — add image client + rename caller**

In `backend/src/main/java/com/kazka/hf/HuggingFaceClient.java`:

Replace lines 25–39 (the field + constructor) with:

```java
    private final WebClient textClient;
    private final WebClient imageClient;
    private final HuggingFaceProperties props;

    public HuggingFaceClient(WebClient.Builder builder, HuggingFaceProperties props) {
        this.props = props;
        if (props.getApiToken() == null || props.getApiToken().isBlank()) {
            log.warn("kazka.huggingface.api-token is not set — HF API calls will fail with 401");
        }
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
```

In `generateText()` (lines 49–68), change the model lookup from `props.getSvgModel()` (two occurrences: the body field on line 54 and the doOnError log on line 65) to `props.getSceneModel()`.

After the `streamRequest` method (line 99), add:

```java
    public Mono<byte[]> generateImage(String prompt, int width, int height) {
        return generateImage(prompt, width, height, null);
    }

    public Mono<byte[]> generateImage(String prompt, int width, int height, Long seed) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("width", width);
        params.put("height", height);
        if (seed != null) params.put("seed", seed);

        return imageClient.post()
                .uri("/hf-inference/models/" + props.getImageModel())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.IMAGE_PNG)
                .bodyValue(Map.of(
                        "inputs", prompt,
                        "parameters", params
                ))
                .retrieve()
                .bodyToMono(byte[].class)
                .doOnError(e -> log.warn("generateImage failed (model={}): {}", props.getImageModel(), e.getMessage()));
    }
```

- [ ] **Step 3.4: Verify the project still compiles**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3.5: Run the existing `PromptBuilderTest` to confirm nothing regressed**

Run: `cd backend && ./gradlew test --tests "com.kazka.story.PromptBuilderTest" -i`
Expected: all green.

- [ ] **Step 3.6: Commit**

```bash
git add backend/src/main/java/com/kazka/hf/HuggingFaceClient.java backend/src/main/java/com/kazka/config/HuggingFaceProperties.java backend/src/main/resources/application.yml
git commit -m "feat(backend): add HF generateImage() and rename svgModel to sceneModel

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Remove SVG prompt path from `PromptBuilder` and delete `svg-system.txt`

**Files:**
- Modify: `backend/src/main/java/com/kazka/story/PromptBuilder.java`
- Modify: `backend/src/test/java/com/kazka/story/PromptBuilderTest.java`
- Delete: `backend/src/main/resources/prompts/svg-system.txt`

This task is a pure removal once the upstream callers (`IllustrationService`) have been switched. We do this BEFORE Task 5/6 so the compile fails loudly if anything still calls `buildSvgSystem`/`buildSvgUser`. Task 5 fixes the failure.

- [ ] **Step 4.1: Delete the SVG-related test methods**

In `backend/src/test/java/com/kazka/story/PromptBuilderTest.java`, delete the two test methods `buildSvgSystem_containsSvgOutputRules` (lines 80–87) and `buildSvgUser_fillsSceneCharacterAndAgeGroup` (lines 89–105).

- [ ] **Step 4.2: Delete the SVG-related production methods**

In `backend/src/main/java/com/kazka/story/PromptBuilder.java`:

- Remove `private final String svgSystem;` (line 26).
- Remove `this.svgSystem = readResource("prompts/svg-system.txt");` from the constructor.
- Remove `buildSvgSystem()` method (lines 68–70).
- Remove `buildSvgUser()` method (lines 72–104).
- Remove `firstTwoSentences` static helper (lines 106–110) — only `buildSvgUser` referenced it.

- [ ] **Step 4.3: Delete `svg-system.txt`**

```bash
rm backend/src/main/resources/prompts/svg-system.txt
```

- [ ] **Step 4.4: Compile — expect failure pointing to `IllustrationService.java`**

Run: `cd backend && ./gradlew compileJava`
Expected: FAILURE with errors at `IllustrationService.java` referring to `buildSvgSystem` / `buildSvgUser`. This is desired — Task 5 fixes it.

- [ ] **Step 4.5: Defer commit until Task 5**

Do not commit yet. The build is broken; we fix it in the next task and commit them together.

---

## Task 5: Rewrite `IllustrationService` for two-image flow + add unit test

**Files:**
- Modify: `backend/src/main/java/com/kazka/illustration/IllustrationService.java`
- Modify: `backend/src/main/java/com/kazka/illustration/ImageStorageService.java`
- Modify: `backend/src/test/java/com/kazka/illustration/ImageStorageServiceTest.java`
- Create: `backend/src/test/java/com/kazka/illustration/IllustrationServiceTest.java`
- Modify: `backend/src/main/java/com/kazka/story/Story.java`
- Modify: `backend/src/main/resources/schema.sql`
- Modify: `backend/src/main/java/com/kazka/story/dto/StoryDto.java`

This is the largest task; it bundles everything that has to change together for compile + tests to be green.

- [ ] **Step 5.1: Update the `Story` entity**

In `backend/src/main/java/com/kazka/story/Story.java`:

Replace lines 40–41 (the `illustrationPath` field):
```java
    @Column(name = "illustration_path", length = 500)
    private String illustrationPath;
```

with:
```java
    @Column(name = "illustration_path_light", length = 500)
    private String illustrationPathLight;

    @Column(name = "illustration_path_dark", length = 500)
    private String illustrationPathDark;
```

Replace the accessors at lines 79–80:
```java
    public String getIllustrationPath() { return illustrationPath; }
    public void setIllustrationPath(String illustrationPath) { this.illustrationPath = illustrationPath; }
```

with:
```java
    public String getIllustrationPathLight() { return illustrationPathLight; }
    public void setIllustrationPathLight(String illustrationPathLight) { this.illustrationPathLight = illustrationPathLight; }

    public String getIllustrationPathDark() { return illustrationPathDark; }
    public void setIllustrationPathDark(String illustrationPathDark) { this.illustrationPathDark = illustrationPathDark; }
```

- [ ] **Step 5.2: Update `schema.sql`**

In `backend/src/main/resources/schema.sql`, replace line 11:
```sql
    illustration_path   VARCHAR(500)  NULL,
```
with:
```sql
    illustration_path_light VARCHAR(500) NULL,
    illustration_path_dark  VARCHAR(500) NULL,
```

- [ ] **Step 5.3: Update `StoryDto`**

In `backend/src/main/java/com/kazka/story/dto/StoryDto.java`, replace lines 9–22 (the record + `from` method) with:

```java
public record StoryDto(
        String id,
        String title,
        String theme,
        List<String> characters,
        String ageGroup,
        String length,
        String language,
        String content,
        String illustrationPathLight,
        String illustrationPathDark,
        IllustrationStatus illustrationStatus,
        Instant createdAt,
        Instant updatedAt
) {
    public static StoryDto from(Story s) {
        return new StoryDto(
                s.getId(), s.getTitle(), s.getTheme(), s.getCharacters(),
                s.getAgeGroup(), s.getLength(), s.getLanguage(), s.getContent(),
                s.getIllustrationPathLight(), s.getIllustrationPathDark(),
                s.getIllustrationStatus(),
                s.getCreatedAt(), s.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 5.4: Update `ImageStorageService` — add `savePng(theme)` and drop `saveSvg`**

In `backend/src/main/java/com/kazka/illustration/ImageStorageService.java`:

Replace the entire `save` and `saveSvg` methods (lines 36–54) with:

```java
    public String savePng(String storyId, com.kazka.story.Theme theme, byte[] imageBytes) {
        String filename = storyId + "-" + theme.slug() + ".png";
        Path file = uploadsDir.resolve(filename);
        try {
            Files.write(file, imageBytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot save PNG for story " + storyId + " theme " + theme, e);
        }
        return "/uploads/" + filename;
    }
```

Replace the `delete` method (lines 56–59) with:

```java
    public void delete(String storyId) {
        tryDelete(uploadsDir.resolve(storyId + ".png"));            // legacy
        tryDelete(uploadsDir.resolve(storyId + ".svg"));            // legacy SVG era
        tryDelete(uploadsDir.resolve(storyId + "-light.png"));
        tryDelete(uploadsDir.resolve(storyId + "-dark.png"));
    }
```

Remove the now-unused `import java.nio.charset.StandardCharsets;` if present.

- [ ] **Step 5.5: Replace `ImageStorageServiceTest`**

Replace the entire contents of `backend/src/test/java/com/kazka/illustration/ImageStorageServiceTest.java` with:

```java
package com.kazka.illustration;

import com.kazka.story.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void savePng_light_writesFileWithLightSuffix() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        byte[] bytes = "fake-png-light".getBytes();

        String path = service.savePng("story-123", Theme.LIGHT, bytes);

        assertThat(path).isEqualTo("/uploads/story-123-light.png");
        Path file = tempDir.resolve("story-123-light.png");
        assertThat(file).exists();
        assertThat(Files.readAllBytes(file)).isEqualTo(bytes);
    }

    @Test
    void savePng_dark_writesFileWithDarkSuffix() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        byte[] bytes = "fake-png-dark".getBytes();

        String path = service.savePng("story-456", Theme.DARK, bytes);

        assertThat(path).isEqualTo("/uploads/story-456-dark.png");
        assertThat(Files.readAllBytes(tempDir.resolve("story-456-dark.png"))).isEqualTo(bytes);
    }

    @Test
    void savePng_throwsUncheckedIOException_whenWriteFails() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        // create a directory where the PNG file should go to make the write fail
        Files.createDirectory(tempDir.resolve("blocker-light.png"));

        assertThatThrownBy(() -> service.savePng("blocker", Theme.LIGHT, new byte[]{1, 2}))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("blocker");
    }

    @Test
    void delete_removesAllVariants() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        Files.writeString(tempDir.resolve("story-xyz.png"), "legacy");
        Files.writeString(tempDir.resolve("story-xyz.svg"), "<svg/>");
        Files.writeString(tempDir.resolve("story-xyz-light.png"), "light");
        Files.writeString(tempDir.resolve("story-xyz-dark.png"), "dark");

        service.delete("story-xyz");

        assertThat(tempDir.resolve("story-xyz.png")).doesNotExist();
        assertThat(tempDir.resolve("story-xyz.svg")).doesNotExist();
        assertThat(tempDir.resolve("story-xyz-light.png")).doesNotExist();
        assertThat(tempDir.resolve("story-xyz-dark.png")).doesNotExist();
    }

    @Test
    void delete_doesNothingIfFilesAbsent() {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        service.delete("nonexistent");  // no exception
    }
}
```

- [ ] **Step 5.6: Rewrite `IllustrationService.java`**

Replace the entire contents of `backend/src/main/java/com/kazka/illustration/IllustrationService.java` with:

```java
package com.kazka.illustration;

import com.kazka.hf.HuggingFaceClient;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.PromptBuilder;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
public class IllustrationService {

    private static final Logger log = LoggerFactory.getLogger(IllustrationService.class);
    private static final int IMAGE_W = 1024;
    private static final int IMAGE_H = 768;

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
                    List<String> chars = story.getCharacters();
                    String firstChar = (chars != null && !chars.isEmpty()) ? chars.get(0) : "a character";
                    String fallback = firstChar + " in a magical scene from " + story.getTitle();

                    return hfClient.generateText(
                                    promptBuilder.buildSceneExtractionSystem(),
                                    promptBuilder.buildSceneExtractionUser(story.getContent()))
                            .onErrorReturn(fallback)
                            .map(scene -> scene.isBlank() ? fallback : scene)
                            .flatMap(scene -> Mono.zip(
                                    hfClient.generateImage(
                                            promptBuilder.buildImagePrompt(story, scene, Theme.LIGHT),
                                            IMAGE_W, IMAGE_H),
                                    hfClient.generateImage(
                                            promptBuilder.buildImagePrompt(story, scene, Theme.DARK),
                                            IMAGE_W, IMAGE_H)
                            ))
                            .flatMap(tuple -> savePair(story, tuple.getT1(), tuple.getT2()))
                            .onErrorResume(e -> {
                                log.warn("PNG illustration failed for {}: {}", storyId, e.getMessage());
                                return markFailed(story);
                            });
                });
    }

    private Mono<Void> savePair(Story story, byte[] light, byte[] dark) {
        return Mono.fromRunnable(() -> {
            String lightPath = imageStorageService.savePng(story.getId(), Theme.LIGHT, light);
            String darkPath = imageStorageService.savePng(story.getId(), Theme.DARK, dark);
            story.setIllustrationPathLight(lightPath);
            story.setIllustrationPathDark(darkPath);
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

- [ ] **Step 5.7: Add `IllustrationServiceTest.java`**

Create `backend/src/test/java/com/kazka/illustration/IllustrationServiceTest.java`:

```java
package com.kazka.illustration;

import com.kazka.hf.HuggingFaceClient;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.PromptBuilder;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.story.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IllustrationServiceTest {

    @Mock HuggingFaceClient hfClient;
    @Mock ImageStorageService imageStorage;
    @Mock StoryRepository storyRepo;
    @Mock PromptBuilder promptBuilder;

    @InjectMocks IllustrationService service;

    private Story sampleStory() {
        Story s = new Story();
        s.setId("s1");
        s.setTitle("The Fox");
        s.setAgeGroup("6-8");
        s.setCharacters(List.of("Mia"));
        s.setContent("Once upon a time...");
        return s;
    }

    @Test
    void generateAndStore_savesLightAndDarkPng_onSuccess() {
        Story story = sampleStory();
        when(storyRepo.findById("s1")).thenReturn(Optional.of(story));
        when(hfClient.generateText(any(), any())).thenReturn(Mono.just("a fox under a tree"));
        when(promptBuilder.buildSceneExtractionSystem()).thenReturn("scene-sys");
        when(promptBuilder.buildSceneExtractionUser(anyString())).thenReturn("scene-user");
        when(promptBuilder.buildImagePrompt(eq(story), anyString(), eq(Theme.LIGHT))).thenReturn("light-prompt");
        when(promptBuilder.buildImagePrompt(eq(story), anyString(), eq(Theme.DARK))).thenReturn("dark-prompt");
        when(hfClient.generateImage(eq("light-prompt"), eq(1024), eq(768))).thenReturn(Mono.just(new byte[]{1}));
        when(hfClient.generateImage(eq("dark-prompt"), eq(1024), eq(768))).thenReturn(Mono.just(new byte[]{2}));
        when(imageStorage.savePng("s1", Theme.LIGHT, new byte[]{1})).thenReturn("/uploads/s1-light.png");
        when(imageStorage.savePng("s1", Theme.DARK, new byte[]{2})).thenReturn("/uploads/s1-dark.png");

        StepVerifier.create(service.generateAndStore("s1")).verifyComplete();

        ArgumentCaptor<Story> saved = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).save(saved.capture());
        assertThat(saved.getValue().getIllustrationPathLight()).isEqualTo("/uploads/s1-light.png");
        assertThat(saved.getValue().getIllustrationPathDark()).isEqualTo("/uploads/s1-dark.png");
        assertThat(saved.getValue().getIllustrationStatus()).isEqualTo(IllustrationStatus.READY);
    }

    @Test
    void generateAndStore_marksFailed_whenImageCallFails() {
        Story story = sampleStory();
        when(storyRepo.findById("s1")).thenReturn(Optional.of(story));
        when(hfClient.generateText(any(), any())).thenReturn(Mono.just("a fox"));
        lenient().when(promptBuilder.buildSceneExtractionSystem()).thenReturn("scene-sys");
        lenient().when(promptBuilder.buildSceneExtractionUser(anyString())).thenReturn("scene-user");
        when(promptBuilder.buildImagePrompt(any(), anyString(), eq(Theme.LIGHT))).thenReturn("light-prompt");
        when(promptBuilder.buildImagePrompt(any(), anyString(), eq(Theme.DARK))).thenReturn("dark-prompt");
        when(hfClient.generateImage(eq("light-prompt"), eq(1024), eq(768)))
                .thenReturn(Mono.error(new RuntimeException("HF down")));
        // dark call may not happen due to zip short-circuit, but allow lenient stub
        lenient().when(hfClient.generateImage(eq("dark-prompt"), eq(1024), eq(768)))
                .thenReturn(Mono.just(new byte[]{2}));

        StepVerifier.create(service.generateAndStore("s1")).verifyComplete();

        ArgumentCaptor<Story> saved = ArgumentCaptor.forClass(Story.class);
        verify(storyRepo).save(saved.capture());
        assertThat(saved.getValue().getIllustrationStatus()).isEqualTo(IllustrationStatus.FAILED);
        assertThat(saved.getValue().getIllustrationPathLight()).isNull();
        assertThat(saved.getValue().getIllustrationPathDark()).isNull();
    }

    @Test
    void generateAndStore_noOp_whenStoryMissing() {
        when(storyRepo.findById("missing")).thenReturn(Optional.empty());

        StepVerifier.create(service.generateAndStore("missing")).verifyComplete();

        verify(storyRepo, org.mockito.Mockito.never()).save(any());
    }
}
```

- [ ] **Step 5.8: Run all backend tests**

Run: `cd backend && ./gradlew test -i`
Expected: BUILD SUCCESSFUL — all tests pass. (Testcontainers integration tests require Docker; if Docker isn't running, run only the focused tests:
`./gradlew test --tests "com.kazka.story.PromptBuilderTest" --tests "com.kazka.illustration.*Test"`.)

- [ ] **Step 5.9: Commit Tasks 4 + 5 together**

```bash
git add backend/src/main/java/com/kazka/illustration backend/src/main/java/com/kazka/story/Story.java backend/src/main/java/com/kazka/story/PromptBuilder.java backend/src/main/java/com/kazka/story/dto/StoryDto.java backend/src/main/resources/schema.sql backend/src/test/java/com/kazka/illustration backend/src/test/java/com/kazka/story/PromptBuilderTest.java
git rm backend/src/main/resources/prompts/svg-system.txt
git commit -m "feat(backend): replace SVG generation with two-PNG diffusion pipeline

Drops the text-LLM SVG path. IllustrationService now extracts a scene with
the existing LLM call, then runs two FLUX.1-schnell image-diffusion calls
(light + dark theme) and stores both PNGs. Story entity, schema, and DTO
gain illustrationPathLight / illustrationPathDark.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: `IllustrationSampleGenerator` CLI runner

**Files:**
- Create: `backend/src/main/java/com/kazka/tools/IllustrationSampleGenerator.java`
- Modify: `backend/build.gradle`

A Spring `@Profile("sample-gen")` CommandLineRunner that calls the live HF pipeline 18 times and writes PNGs into `frontend/public/illustrations/`. Run manually after each prompt iteration.

- [ ] **Step 6.1: Create the runner**

Create `backend/src/main/java/com/kazka/tools/IllustrationSampleGenerator.java`:

```java
package com.kazka.tools;

import com.kazka.hf.HuggingFaceClient;
import com.kazka.story.PromptBuilder;
import com.kazka.story.Story;
import com.kazka.story.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
@Profile("sample-gen")
public class IllustrationSampleGenerator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IllustrationSampleGenerator.class);

    private static final Map<String, String> SCENES = Map.of(
            "hero",    "a friendly fox sitting under a starry forest at twilight, with glowing mushrooms",
            "how",     "a magical castle on a hill at night with shooting stars and glowing windows",
            "preview", "a tiny glowing star named Mia walking on a silvery moss path beside a great oak tree in an enchanted forest"
    );

    private static final Map<String, int[]> DIMS = Map.of(
            "hero",    new int[]{1024, 768},
            "how",     new int[]{768, 1024},
            "preview", new int[]{1024, 768}
    );

    private static final List<String> AGES = List.of("3-5", "6-8", "9-12");
    private static final Path OUT_DIR = Path.of("../frontend/public/illustrations");

    private final HuggingFaceClient hfClient;
    private final PromptBuilder promptBuilder;
    private final ConfigurableApplicationContext ctx;

    public IllustrationSampleGenerator(HuggingFaceClient hfClient,
                                       PromptBuilder promptBuilder,
                                       ConfigurableApplicationContext ctx) {
        this.hfClient = hfClient;
        this.promptBuilder = promptBuilder;
        this.ctx = ctx;
    }

    @Override
    public void run(String... args) throws Exception {
        Files.createDirectories(OUT_DIR);
        int total = 0;
        int failed = 0;

        for (Map.Entry<String, String> sectionEntry : SCENES.entrySet()) {
            String section = sectionEntry.getKey();
            String scene = sectionEntry.getValue();
            int[] dims = DIMS.get(section);

            for (String age : AGES) {
                for (Theme theme : Theme.values()) {
                    total++;
                    String name = section + "-" + age + "-" + theme.slug() + ".png";
                    try {
                        Story dummy = new Story();
                        dummy.setAgeGroup(age);
                        String prompt = promptBuilder.buildImagePrompt(dummy, scene, theme);
                        // Deterministic per-tuple seed so re-runs produce near-identical PNGs.
                        long seed = (long) name.hashCode() & 0xFFFFFFFFL;
                        log.info("[{}] generating {} ({}x{} seed={})...", total, name, dims[0], dims[1], seed);
                        byte[] bytes = hfClient.generateImage(prompt, dims[0], dims[1], seed).block();
                        if (bytes == null || bytes.length == 0) {
                            throw new IllegalStateException("empty image bytes");
                        }
                        Files.write(OUT_DIR.resolve(name), bytes);
                        log.info("    wrote {} ({} bytes)", name, bytes.length);
                    } catch (Exception e) {
                        failed++;
                        log.error("    FAILED {}: {}", name, e.getMessage());
                    }
                }
            }
        }

        log.info("Done. {} total, {} failed.", total, failed);
        ctx.close();
    }
}
```

- [ ] **Step 6.2: Add the Gradle task**

Append to `backend/build.gradle`:

```groovy
tasks.register('generateSamples', JavaExec) {
    group = 'application'
    description = 'Generate the 18 marketing PNG samples by calling the live HF pipeline.'
    mainClass = 'com.kazka.KazkaApplication'
    classpath = sourceSets.main.runtimeClasspath
    systemProperty 'spring.profiles.active', 'sample-gen'
    workingDir = projectDir
}
```

- [ ] **Step 6.3: Verify the project still compiles**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6.4: Run unit tests to make sure nothing regressed**

Run: `cd backend && ./gradlew test --tests "com.kazka.illustration.*Test" --tests "com.kazka.story.PromptBuilderTest"`
Expected: all green.

- [ ] **Step 6.5: Commit**

```bash
git add backend/src/main/java/com/kazka/tools/IllustrationSampleGenerator.java backend/build.gradle
git commit -m "feat(backend): add IllustrationSampleGenerator CLI runner

Run via './gradlew generateSamples' (requires HUGGINGFACE_API_TOKEN). Writes
18 PNGs to frontend/public/illustrations/ — three sections × three ages ×
two themes. Used to refresh the marketing samples after prompt iteration.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: Generate the 18 sample PNGs and commit them

**Files (after run):**
- Create: `frontend/public/illustrations/{hero,how,preview}-{3-5,6-8,9-12}-{light,dark}.png` (18 files)

This task does require a working `HUGGINGFACE_API_TOKEN` and network access. If HF is unreachable, defer this task and continue with frontend wiring (Tasks 8–13) — the carousel will render broken images until samples land.

- [ ] **Step 7.1: Ensure `HUGGINGFACE_API_TOKEN` is set in the environment**

Run: `echo "${HUGGINGFACE_API_TOKEN:0:6}…"`
Expected: prints the first 6 chars of the token (e.g. `hf_xyz…`). If empty, set `export HUGGINGFACE_API_TOKEN=hf_...` from `.env` first.

- [ ] **Step 7.2: Make sure MySQL is running (the app requires DB connection on startup, even for the sample-gen profile)**

Run: `docker-compose ps mysql | grep -q "Up" || docker-compose up -d mysql`
Expected: MySQL is `Up (healthy)` within ~30s.

- [ ] **Step 7.3: Run `generateSamples`**

Run: `cd backend && ./gradlew generateSamples`
Expected: log lines `[1..18] generating ... wrote ...`. May take 5–10 min depending on FLUX queue. End log: `Done. 18 total, 0 failed.`

- [ ] **Step 7.4: Verify the 18 files exist and are non-empty**

Run: `ls -la frontend/public/illustrations/*.png | wc -l`
Expected: `18`.

Run: `find frontend/public/illustrations -name "*.png" -size -1k`
Expected: empty output (no zero-byte files).

- [ ] **Step 7.5: Eyeball-review the PNGs**

Open `frontend/public/illustrations/` in Finder (`open frontend/public/illustrations`). Spot-check that:
- Light variants have warm/cream/pastel backgrounds.
- Dark variants have dark/navy/glowing backgrounds.
- 3-5 files look more scribbly than 9-12 files.

If any look off, iterate on the matching prompt file in `backend/src/main/resources/prompts/image-style-*.txt` and re-run Step 7.3 — the runner overwrites existing files in place.

- [ ] **Step 7.6: Commit the generated PNGs**

```bash
git add frontend/public/illustrations
git commit -m "feat(frontend): add 18 sample illustration PNGs (3 sections × 3 ages × 2 themes)

Generated via 'gradlew generateSamples' against FLUX.1-schnell. Used by the
IllustrationCarousel on the home page hero / how-it-works / story-preview
sections.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: Update frontend types for two illustration paths

**Files:**
- Modify: `frontend/src/lib/types.ts`

- [ ] **Step 8.1: Update the `Story` interface**

In `frontend/src/lib/types.ts`, replace line 12:

```ts
  illustrationPath: string | null
```

with:

```ts
  illustrationPathLight: string | null
  illustrationPathDark: string | null
```

- [ ] **Step 8.2: Run the type checker — expect failures in StoryCard / StoryDetailPage / IllustrationFrame consumers**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: errors at `StoryCard.tsx:18`, `StoryDetailPage.tsx:38`, `:54`, `:140`. These are fixed in Task 9.

- [ ] **Step 8.3: Defer commit until Task 9**

---

## Task 9: Update `IllustrationFrame` and consumers for theme-aware src

**Files:**
- Modify: `frontend/src/components/story/IllustrationFrame.tsx`
- Modify: `frontend/src/components/story/StoryCard.tsx`
- Modify: `frontend/src/pages/StoryDetailPage.tsx`

- [ ] **Step 9.1: Update `IllustrationFrame.tsx` to take both paths and pick by theme**

Replace the entire contents of `frontend/src/components/story/IllustrationFrame.tsx`:

```tsx
import { useState } from 'react'
import { PlaceholderSvg } from './PlaceholderSvg'
import { useTheme } from '../../lib/ThemeContext'
import type { IllustrationStatus } from '../../lib/types'
import styles from './IllustrationFrame.module.css'

interface IllustrationFrameProps {
  pathLight: string | null
  pathDark: string | null
  status: IllustrationStatus
}

export function IllustrationFrame({ pathLight, pathDark, status }: IllustrationFrameProps) {
  const [imgError, setImgError] = useState(false)
  const { theme } = useTheme()
  const path = theme === 'dark' ? pathDark ?? pathLight : pathLight ?? pathDark

  if (status === 'READY' && path && !imgError) {
    return (
      <div className={styles.frame}>
        <img
          src={path}
          alt="Ілюстрація казки"
          className={styles.img}
          onError={() => setImgError(true)}
        />
      </div>
    )
  }

  if (status === 'PENDING') {
    return (
      <div className={styles.frame}>
        <div className={styles.skeleton}>
          <PlaceholderSvg />
          <div className={styles.shimmer} />
        </div>
      </div>
    )
  }

  return (
    <div className={styles.frame}>
      <PlaceholderSvg />
    </div>
  )
}
```

- [ ] **Step 9.2: Update `StoryCard.tsx`**

In `frontend/src/components/story/StoryCard.tsx`, replace lines 17–20:

```tsx
        <IllustrationFrame
          path={story.illustrationPath}
          status={story.illustrationStatus}
        />
```

with:

```tsx
        <IllustrationFrame
          pathLight={story.illustrationPathLight}
          pathDark={story.illustrationPathDark}
          status={story.illustrationStatus}
        />
```

- [ ] **Step 9.3: Update `StoryDetailPage.tsx`**

In `frontend/src/pages/StoryDetailPage.tsx`, around line 140, replace the `<IllustrationFrame ... />` props the same way:

```tsx
            <IllustrationFrame
              pathLight={story.illustrationPathLight}
              pathDark={story.illustrationPathDark}
              status={story.illustrationStatus}
            />
```

- [ ] **Step 9.4: Type-check**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: clean (no errors).

- [ ] **Step 9.5: Lint**

Run: `cd frontend && npm run lint`
Expected: clean (no errors).

- [ ] **Step 9.6: Commit Tasks 8 + 9 together**

```bash
git add frontend/src/lib/types.ts frontend/src/components/story/IllustrationFrame.tsx frontend/src/components/story/StoryCard.tsx frontend/src/pages/StoryDetailPage.tsx
git commit -m "feat(frontend): theme-aware illustration src for story pages

Story type gains illustrationPathLight + illustrationPathDark. The
IllustrationFrame picks the right URL based on the active theme, falling
back to the other if one is missing (legacy single-path stories).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 10: Add `carouselTickStore` and `<IllustrationCarousel />`

**Files:**
- Create: `frontend/src/components/illustrations/carouselTickStore.ts`
- Create: `frontend/src/components/illustrations/IllustrationCarousel.tsx`
- Create: `frontend/src/components/illustrations/IllustrationCarousel.module.css`

- [ ] **Step 10.1: Create `carouselTickStore.ts`**

Create `frontend/src/components/illustrations/carouselTickStore.ts`:

```ts
type Listener = () => void

const listeners = new Set<Listener>()
let timer: ReturnType<typeof setInterval> | null = null
let activeCount = 0

const DEFAULT_INTERVAL_MS = 4000

function ensureTimer(intervalMs: number) {
  if (timer) return
  timer = setInterval(() => {
    listeners.forEach((l) => l())
  }, intervalMs)
}

function maybeStopTimer() {
  if (activeCount === 0 && timer) {
    clearInterval(timer)
    timer = null
  }
}

export function subscribeCarouselTick(listener: Listener, intervalMs = DEFAULT_INTERVAL_MS): () => void {
  listeners.add(listener)
  activeCount++
  ensureTimer(intervalMs)
  return () => {
    listeners.delete(listener)
    activeCount = Math.max(0, activeCount - 1)
    maybeStopTimer()
  }
}
```

- [ ] **Step 10.2: Create `IllustrationCarousel.module.css`**

Create `frontend/src/components/illustrations/IllustrationCarousel.module.css`:

```css
.wrap {
  position: relative;
  display: block;
}

.frame {
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  background: var(--color-surface-2);
}

.img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity 300ms ease-in-out;
}

.imgFading {
  opacity: 0;
}

.ageBadge {
  position: absolute;
  top: 12px;
  left: 12px;
  font-size: 11px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  padding: 4px 10px;
  border-radius: 999px;
  pointer-events: none;
  font-weight: 600;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  justify-content: center;
}

.tab {
  font: inherit;
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid var(--color-text-faint);
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: background 200ms, color 200ms, border-color 200ms;
}

.tab:hover { color: var(--color-text); border-color: var(--color-text-muted); }
.tabActive { background: var(--color-magic); color: #fff; border-color: var(--color-magic); }

@media (prefers-reduced-motion: reduce) {
  .img { transition: none; }
}
```

- [ ] **Step 10.3: Create `IllustrationCarousel.tsx`**

Create `frontend/src/components/illustrations/IllustrationCarousel.tsx`:

```tsx
import { useEffect, useRef, useState } from 'react'
import { useTheme } from '../../lib/ThemeContext'
import { useLocale } from '../../lib/LocaleContext'
import { subscribeCarouselTick } from './carouselTickStore'
import styles from './IllustrationCarousel.module.css'

const AGE_KEYS = ['3-5', '6-8', '9-12'] as const
type AgeKey = (typeof AGE_KEYS)[number]

interface Props {
  section: 'hero' | 'how' | 'preview'
  width: number
  height: number
  className?: string
  intervalMs?: number
}

export function IllustrationCarousel({ section, width, height, className, intervalMs }: Props) {
  const { theme } = useTheme()
  const { t } = useLocale()
  const [ageIndex, setAgeIndex] = useState(0)
  const [manual, setManual] = useState(false)
  const [fading, setFading] = useState(false)
  const fadeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const age = AGE_KEYS[ageIndex]
  const src = `/illustrations/${section}-${age}-${theme}.png`

  useEffect(() => {
    if (manual) return
    if (typeof window !== 'undefined' &&
        window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
      return
    }
    return subscribeCarouselTick(() => {
      setFading(true)
      if (fadeTimerRef.current) clearTimeout(fadeTimerRef.current)
      fadeTimerRef.current = setTimeout(() => {
        setAgeIndex((i) => (i + 1) % AGE_KEYS.length)
        setFading(false)
      }, 250)
    }, intervalMs)
  }, [manual, intervalMs])

  // Preload the other ages for the current theme
  useEffect(() => {
    AGE_KEYS.forEach((a, i) => {
      if (i === ageIndex) return
      const img = new Image()
      img.src = `/illustrations/${section}-${a}-${theme}.png`
    })
  }, [section, theme, ageIndex])

  const onTabClick = (i: number) => {
    setManual(true)
    setFading(true)
    if (fadeTimerRef.current) clearTimeout(fadeTimerRef.current)
    fadeTimerRef.current = setTimeout(() => {
      setAgeIndex(i)
      setFading(false)
    }, 200)
  }

  const ageLabel = t.form.ageGroups[age]

  return (
    <div className={`${styles.wrap} ${className ?? ''}`}>
      <div className={styles.frame} style={{ width, height }} role="img" aria-label={`${ageLabel} drawing`}>
        <img
          src={src}
          alt={`${ageLabel} child drawing`}
          className={`${styles.img} ${fading ? styles.imgFading : ''}`}
          loading="eager"
          decoding="async"
        />
        <span className={styles.ageBadge} aria-hidden="true">{ageLabel}</span>
      </div>
      <div className={styles.tabs} role="tablist">
        {AGE_KEYS.map((a, i) => (
          <button
            key={a}
            role="tab"
            aria-pressed={i === ageIndex}
            className={`${styles.tab} ${i === ageIndex ? styles.tabActive : ''}`}
            onClick={() => onTabClick(i)}
          >
            {t.form.ageGroups[a]}
          </button>
        ))}
      </div>
    </div>
  )
}
```

- [ ] **Step 10.4: Type-check**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: clean.

- [ ] **Step 10.5: Lint**

Run: `cd frontend && npm run lint`
Expected: clean.

- [ ] **Step 10.6: Commit**

```bash
git add frontend/src/components/illustrations
git commit -m "feat(frontend): add IllustrationCarousel component and tick store

Reusable component used by the home / how / preview marketing sections.
Auto-cycles through 3-5 / 6-8 / 9-12 child-drawing samples (synchronized
across instances via a module-level event emitter), pauses on manual age-
tab click, swaps between light- and dark-theme PNGs based on useTheme().

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 11: Replace `HeroIllustration` in `HomePage.tsx`

**Files:**
- Modify: `frontend/src/pages/HomePage.tsx`

- [ ] **Step 11.1: Delete the inline SVG component and replace usage**

In `frontend/src/pages/HomePage.tsx`:

Delete lines 54–225-ish (the entire `function HeroIllustration() { ... }` body — it ends at the `</svg>` matching its opening on line ~95).

Verify by running: `grep -n "^function HeroIllustration\|^}" /Users/makar/dev/kazka/frontend/src/pages/HomePage.tsx | head -5` to find the closing brace.

Add at the top of the imports block (after existing imports):

```tsx
import { IllustrationCarousel } from '../components/illustrations/IllustrationCarousel'
```

If the deleted function was the only consumer of `useTheme` in this file, delete the import:

```tsx
import { useTheme } from '../lib/ThemeContext'
```

Then run: `grep -n "useTheme" frontend/src/pages/HomePage.tsx`. If empty, the import is gone (ok). If non-empty, leave the import.

Replace `<HeroIllustration />` (around line 448) with:

```tsx
              <IllustrationCarousel section="hero" width={520} height={390} />
```

- [ ] **Step 11.2: Type-check**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: clean.

- [ ] **Step 11.3: Lint**

Run: `cd frontend && npm run lint`
Expected: clean.

- [ ] **Step 11.4: Commit**

```bash
git add frontend/src/pages/HomePage.tsx
git commit -m "feat(frontend): swap HomePage hero SVG for IllustrationCarousel

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 12: Replace `CastleIllustration` in `HowItWorks.tsx`

**Files:**
- Modify: `frontend/src/components/home/HowItWorks.tsx`

- [ ] **Step 12.1: Delete `CastleIllustration` and update usage**

In `frontend/src/components/home/HowItWorks.tsx`:

Delete the `CastleIllustration` function (lines 8–238 — confirm the closing `}` with `grep -n "^function\|^}" /Users/makar/dev/kazka/frontend/src/components/home/HowItWorks.tsx | head -10`).

If `CastleIllustration` was the only consumer of `useTheme` in this file (`grep -n "useTheme" frontend/src/components/home/HowItWorks.tsx`), remove the `import { useTheme } from '../../lib/ThemeContext'` line.

Add this import (after the other component imports):

```tsx
import { IllustrationCarousel } from '../illustrations/IllustrationCarousel'
```

Replace `<CastleIllustration />` (around line 337) with:

```tsx
            <IllustrationCarousel section="how" width={300} height={400} />
```

- [ ] **Step 12.2: Type-check**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: clean.

- [ ] **Step 12.3: Lint**

Run: `cd frontend && npm run lint`
Expected: clean.

- [ ] **Step 12.4: Commit**

```bash
git add frontend/src/components/home/HowItWorks.tsx
git commit -m "feat(frontend): swap HowItWorks castle SVG for IllustrationCarousel

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 13: Replace `StoryIllustration` in `StoryPreview.tsx`

**Files:**
- Modify: `frontend/src/components/home/StoryPreview.tsx`

- [ ] **Step 13.1: Delete `StoryIllustration` and update usage**

In `frontend/src/components/home/StoryPreview.tsx`:

Delete the `StoryIllustration` function (lines 7–297 — find the closing `}` with `grep -n "^function StoryIllustration\|^export function StoryPreview" /Users/makar/dev/kazka/frontend/src/components/home/StoryPreview.tsx`).

Remove `import { useTheme } from '../../lib/ThemeContext'` if `useTheme` is no longer referenced in the remaining code (`grep -n "useTheme" frontend/src/components/home/StoryPreview.tsx`).

Add:

```tsx
import { IllustrationCarousel } from '../illustrations/IllustrationCarousel'
```

Replace `<StoryIllustration />` (in the JSX) with:

```tsx
            <IllustrationCarousel section="preview" width={520} height={390} />
```

- [ ] **Step 13.2: Type-check**

Run: `cd frontend && node_modules/.bin/tsc --noEmit`
Expected: clean.

- [ ] **Step 13.3: Lint**

Run: `cd frontend && npm run lint`
Expected: clean.

- [ ] **Step 13.4: Commit**

```bash
git add frontend/src/components/home/StoryPreview.tsx
git commit -m "feat(frontend): swap StoryPreview SVG for IllustrationCarousel

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 14: Browser verification + final cleanup

**Files:** none modified — verification only.

- [ ] **Step 14.1: Boot the stack**

Run: `cd /Users/makar/dev/kazka && docker-compose up --build -d`
Expected: backend and frontend containers `Up`.

- [ ] **Step 14.2: Open the app and verify the home page**

Open `http://localhost` in a browser. Verify:
- Hero image is one of the 9 hero PNGs (matches current theme).
- Below it, 3 age tabs (3–5, 6–8, 9–12). Active tab matches the displayed image.
- After ~4s, all three sections (hero / how-it-works / story-preview) advance ages together.
- Click a tab: the corresponding image swaps in; auto-cycle for that section stops; other sections keep cycling.
- Toggle theme (light/dark): all three carousels swap to the matching theme variant.

- [ ] **Step 14.3: Verify the story flow end-to-end**

Click "Create a story", fill in form fields (any age), submit. Wait for generation.
- Story page loads.
- Once illustration_status becomes READY, two PNGs exist on disk: `uploads/{id}-light.png` and `uploads/{id}-dark.png`.
- Story page shows the right one for the current theme.
- Toggle theme on the story page → image swaps.

- [ ] **Step 14.4: Check the JS console**

DevTools → Console. Expect no errors. Network tab: 18 PNG requests for the 3 carousels' preloads — all 200.

- [ ] **Step 14.5: Run the full backend test suite**

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 14.6: Final tsc + lint**

Run: `cd frontend && node_modules/.bin/tsc --noEmit && npm run lint`
Expected: clean.

- [ ] **Step 14.7: If everything is green, no commit needed — the plan is complete.**

If step 14.2 / 14.3 reveals visual issues with a particular age/theme PNG, revisit the relevant prompt file (`backend/src/main/resources/prompts/image-style-*.txt`), tweak it, re-run `./gradlew generateSamples`, then `git add frontend/public/illustrations && git commit -m "fix(frontend): refresh sample PNGs after prompt tuning"`.

---

## Notes for the implementer

- **Existing in-progress UI edits:** the working tree at plan-start has unstaged edits in `chrome/Footer.tsx`, `chrome/Nav.tsx`, `ArchiveShelf.tsx`, etc. These are unrelated and must not be touched by this plan. Use `git status` after each commit to confirm only your intended files moved.

- **Database state:** `schema.sql` uses `DROP TABLE IF EXISTS stories; CREATE TABLE ...` so re-init wipes existing rows. To regenerate cleanly: `docker-compose down -v && docker-compose up --build -d`. To preserve existing rows on a dev DB, run before redeploy:
  ```sql
  ALTER TABLE stories CHANGE COLUMN illustration_path illustration_path_light VARCHAR(500) NULL;
  ALTER TABLE stories ADD COLUMN illustration_path_dark VARCHAR(500) NULL AFTER illustration_path_light;
  ```

- **HuggingFace endpoint shape:** the FLUX router endpoint may need adjustment (`/hf-inference/models/{model}` vs. another path) once you run Step 7.3 against the live API. If Step 7.3 returns 404 or HTML, inspect the response and update the URI in `HuggingFaceClient.generateImage`. Other backend tests are insulated from this via Mockito and remain green.

- **Token budget for FLUX:** prompts above ~75 tokens get truncated. Each style preamble is ~40 tokens; scenes are ~20 tokens. Stay under 60 to be safe. If a prompt seems to ignore part of the scene, shorten the style file.

- **No frontend tests exist** (per CLAUDE.md). Verification is `tsc --noEmit` + `npm run lint` + manual browser check.
