# SVG Illustration Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace raster PNG generation with a two-phase LLM pipeline that generates SVG code and stores it as a `.svg` file in the uploads directory.

**Architecture:** `IllustrationService.generateAndStore()` first calls the LLM to extract a one-sentence scene description from the story content, then calls the LLM again with a detailed SVG system prompt to generate valid SVG code. The extracted `<svg>` tag is written to disk by `ImageStorageService`.

**Tech Stack:** Spring Boot 4 (WebFlux reactive), `HuggingFaceClient` (WebClient-based), JUnit 5 + AssertJ (unit tests), `application.yml` config binding via `@ConfigurationProperties`.

---

### Task 1: Add `svgModel` to config

**Files:**
- Modify: `backend/src/main/java/com/kazka/config/HuggingFaceProperties.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `.env.example`

No tests for config classes — verified by startup.

- [ ] **Step 1: Add `svgModel` field to `HuggingFaceProperties`**

Open `backend/src/main/java/com/kazka/config/HuggingFaceProperties.java`.
Add field after `imageModel`:

```java
private String svgModel = "Qwen/Qwen2.5-72B-Instruct";
```

Add getter and setter after the `imageModel` ones:

```java
public String getSvgModel() { return svgModel; }
public void setSvgModel(String svgModel) { this.svgModel = svgModel; }
```

- [ ] **Step 2: Bind `svgModel` in `application.yml`**

Open `backend/src/main/resources/application.yml`.
Add this line directly under `image-model`:

```yaml
    svg-model: ${HF_SVG_MODEL:Qwen/Qwen2.5-72B-Instruct}
```

The full `kazka.huggingface` block should now end with:
```yaml
    image-model: ${HF_IMAGE_MODEL:black-forest-labs/FLUX.1-schnell}
    svg-model: ${HF_SVG_MODEL:Qwen/Qwen2.5-72B-Instruct}
    text-base-url: ${HF_TEXT_BASE_URL:https://router.huggingface.co}
    image-base-url: ${HF_IMAGE_BASE_URL:https://router.huggingface.co}
```

- [ ] **Step 3: Document in `.env.example`**

Open `.env.example`. Add after `HF_IMAGE_MODEL`:

```
HF_SVG_MODEL=Qwen/Qwen2.5-72B-Instruct
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kazka/config/HuggingFaceProperties.java \
        backend/src/main/resources/application.yml \
        .env.example
git commit -m "feat: add HF_SVG_MODEL config property"
```

---

### Task 2: Add prompt files

**Files:**
- Create: `backend/src/main/resources/prompts/scene-extraction-system.txt`
- Create: `backend/src/main/resources/prompts/svg-system.txt`

These are classpath resources loaded at startup by `PromptBuilder`. A missing file causes a hard startup failure (existing behaviour for all prompt files).

- [ ] **Step 1: Create `scene-extraction-system.txt`**

Create `backend/src/main/resources/prompts/scene-extraction-system.txt` with this exact content:

```
You are a visual scene extractor for children's fairy tales.
Given a story, output exactly one sentence describing the main visual scene: the setting, the main character, and what they are doing.
Return only that sentence — no preamble, no explanation, no punctuation beyond the sentence itself.
```

- [ ] **Step 2: Create `svg-system.txt`**

Create `backend/src/main/resources/prompts/svg-system.txt` with this exact content:

```
You are an expert SVG illustrator specializing in children's fairy tale artwork.
You generate clean, valid, readable SVG code for storybook illustrations.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
VISUAL STYLE — MANDATORY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Style: flat vector, children's storybook, bold and cheerful
Outline: stroke-width 2.5–4px on ALL main shapes, color #2d1a0e or dark variant of fill
Palette: maximum 6 warm soft colors per illustration — peach, sage green, sky blue, warm yellow, coral, cream
Corners: rx/ry minimum 8px on all rectangles and body shapes
Composition: one clear focal character, centered or center-left, occupying 25–35% of canvas height

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
BACKGROUND — MANDATORY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

- ALWAYS daytime or golden hour — NEVER dark night scene
- Sky: solid warm color or simple 2-stop linear gradient (light blue to peach)
  Example: linearGradient from #fde8c8 to #a8d8f0, top to bottom
- Ground: single solid color stripe at bottom 20% of canvas (#7bc67e or #a8d86e)
- Minimum 50% of total canvas must be bright background (sky + ground)
- NO dark backgrounds, NO night scenes, NO navy/dark-blue fills

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
MAIN CHARACTER — MANDATORY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Build characters from these geometric primitives only:
- Head: circle or ellipse, filled with warm skin/fur color
- Eyes: two small filled circles (dark), with optional white dot highlight
- Ears: two small circles or rounded triangles on top of head
- Body: rounded rectangle (rx 12+)
- Arms: two small ellipses or rounded rectangles, rotated
- Legs: two rectangles with rounded bottom
- Tail (if animal): curved thick path with rounded linecap

Character color: choose an appropriate warm, soft hex color for the character based on their name and type.
For example: warm peach (#f4c09e) for a human child, golden (#f7c059) for a fox, soft grey-blue (#a0b8d0) for a wolf.
Apply the chosen color as the head fill. Use the same color family for the body.

FORBIDDEN character shapes:
- NO star shapes as characters
- NO abstract symbols representing living beings
- NO silhouettes without internal detail
- NO characters that occupy less than 20% of canvas height

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SCENE ELEMENTS — RULES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Trees:
- Lollipop style: circle crown (#7bc67e) on rectangle trunk (#8B5E3C)
- OR triangle pine: 3 stacked triangles decreasing in size, flat fill
- Each tree is an INDIVIDUAL separate shape — NEVER merge into silhouette mass
- Maximum 3 trees per scene, clearly separated with gap between them

Clouds:
- 3–4 overlapping circles, flat white fill, no stroke
- Only in upper 30% of canvas

Flowers:
- Circle center + 5–6 petal ellipses radiating outward
- Small, decorative only, in ground strip

Sun:
- Simple circle, warm yellow #FFD166, optional 6–8 short line rays
- Place in upper corner, not center

FORBIDDEN scene elements:
- NO dashed lines, dotted paths, or trajectory arcs unless it is clearly a rainbow
- NO ambiguous shapes in corners that have no narrative purpose
- NO objects that float without context
- NO dark merged silhouettes on horizon
- NO realistic perspective or vanishing point

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SVG TECHNICAL RULES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

- viewBox: always "0 0 800 600"
- width="800" height="600"
- Total elements: maximum 55 SVG elements — keep it clean
- NO <filter> tags (no feGaussianBlur, feTurbulence, feBlend — they cause artifacts)
- NO <image> tags — no external references
- NO <text> or <tspan> — no letters inside SVG
- NO <clipPath> with complex shapes
- NO <use> referencing complex symbols
- NO <mask> tags
- Gradients: only simple 2-stop linearGradient for sky background
- All paths: use simple cubic bezier curves only, no complex d= strings
- Group related elements: <g id="background">, <g id="character">, <g id="decorations">

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SELF-CHECK BEFORE OUTPUT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Before writing the final SVG, verify internally:
- Background is bright — no dark fills covering more than 10% of canvas
- Main character is a recognizable animal or person made of geometric shapes
- Every shape in the scene has a clear narrative purpose — no mystery objects
- No dashed lines, no trajectory paths, no abstract arcs
- Trees are individual separated shapes, not merged silhouettes
- Total element count is under 55
- No forbidden tags: filter, image, text, clipPath, mask
- Character has eyes (two circles) and is clearly readable at small size

OUTPUT FORMAT:
Return only valid SVG code.
Start with: <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="800" height="600">
End with: </svg>
No markdown code blocks, no explanation, no XML declaration.
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/prompts/scene-extraction-system.txt \
        backend/src/main/resources/prompts/svg-system.txt
git commit -m "feat: add scene extraction and SVG illustration prompt files"
```

---

### Task 3: Add `generateText()` to `HuggingFaceClient`

**Files:**
- Modify: `backend/src/main/java/com/kazka/hf/HuggingFaceClient.java`

The new method is non-streaming: it posts to `/v1/chat/completions` with `"stream": false` and reads the full JSON response to extract `choices[0].message.content`.

- [ ] **Step 1: Add `generateText()` method**

Open `backend/src/main/java/com/kazka/hf/HuggingFaceClient.java`.
Add this method after `streamEdit()` (before `streamRequest()`):

```java
public Mono<String> generateText(String system, String user) {
    return textClient.post()
            .uri("/v1/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                    "model", props.getSvgModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", user)
                    ),
                    "stream", false,
                    "max_tokens", 4096
            ))
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(node -> node.path("choices").path(0)
                    .path("message").path("content").asText(""));
}
```

No import changes needed — `JsonNode`, `Map`, `List`, `Mono` are already imported.

- [ ] **Step 2: Compile-check**

```bash
cd backend && ./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kazka/hf/HuggingFaceClient.java
git commit -m "feat: add generateText() non-streaming method to HuggingFaceClient"
```

---

### Task 4: Add SVG prompt methods to `PromptBuilder`, remove dead code

**Files:**
- Modify: `backend/src/main/java/com/kazka/story/PromptBuilder.java`
- Modify: `backend/src/test/java/com/kazka/story/PromptBuilderTest.java`

`buildIllustrationPrompt()` and `illustrationStyle` become dead code once `IllustrationService` is rewritten (Task 6). Remove them now so they don't linger.

- [ ] **Step 1: Write the failing tests first**

Open `backend/src/test/java/com/kazka/story/PromptBuilderTest.java`.

Remove the existing `buildIllustrationPrompt_includesTitleAndCharacters` test (it tests a method we're deleting).

Add these four new tests at the end of the class, before the closing `}`:

```java
@Test
void buildSceneExtractionSystem_returnsNonBlank() {
    assertThat(builder.buildSceneExtractionSystem()).isNotBlank();
}

@Test
void buildSceneExtractionUser_wrapsStoryContent() {
    String user = builder.buildSceneExtractionUser("Once upon a time a fox ran.");

    assertThat(user).contains("Once upon a time a fox ran.");
}

@Test
void buildSvgSystem_containsSvgOutputRules() {
    String system = builder.buildSvgSystem();

    assertThat(system).contains("viewBox");
    assertThat(system).contains("800");
    assertThat(system).contains("filter");
}

@Test
void buildSvgUser_fillsSceneCharacterAndAgeGroup() {
    Story story = new Story();
    story.setCharacters(List.of("Mia", "the Fox"));
    story.setAgeGroup("6-8");
    story.setContent("Once there was a girl. She walked into the forest. More text here.");

    String user = builder.buildSvgUser(story, "a girl standing near a tall oak tree");

    assertThat(user).contains("a girl standing near a tall oak tree");
    assertThat(user).contains("Mia");
    assertThat(user).contains("the Fox");
    assertThat(user).contains("6-8");
    assertThat(user).contains("Once there was a girl");
    assertThat(user).contains("She walked into the forest");
}
```

Add the required import at the top of the file (if not already present):

```java
import com.kazka.story.Story;
import java.util.List;
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd backend && ./gradlew test --tests "com.kazka.story.PromptBuilderTest" --info 2>&1 | tail -30
```

Expected: compilation failure — `buildSceneExtractionSystem`, `buildSvgSystem`, `buildSvgUser` do not exist yet.

- [ ] **Step 3: Update `PromptBuilder`**

Open `backend/src/main/java/com/kazka/story/PromptBuilder.java`.

**Remove** `illustrationStyle` field and its load from the constructor.
**Remove** `buildIllustrationPrompt()` method entirely.

**Add** two new fields after the existing ones:

```java
private final String sceneExtractionSystem;
private final String svgSystem;
```

**Update** the constructor to load the new files (add after existing `illustrationStyle` load is removed):

```java
public PromptBuilder() {
    this.storySystem = readResource("prompts/story-system.txt");
    this.editorUk = readResource("prompts/editor-uk.txt");
    this.editorEn = readResource("prompts/editor-en.txt");
    this.sceneExtractionSystem = readResource("prompts/scene-extraction-system.txt");
    this.svgSystem = readResource("prompts/svg-system.txt");
}
```

**Add** the three new public methods at the end of the class (before the `readResource` helper):

```java
public String buildSceneExtractionSystem() {
    return sceneExtractionSystem.strip();
}

public String buildSceneExtractionUser(String storyContent) {
    return "Story:\n\n" + (storyContent == null ? "" : storyContent);
}

public String buildSvgSystem() {
    return svgSystem.strip();
}

public String buildSvgUser(Story story, String sceneDescription) {
    String mainChar = (story.getCharacters() == null || story.getCharacters().isEmpty())
            ? "a child" : story.getCharacters().get(0);
    List<String> supporting = (story.getCharacters() != null && story.getCharacters().size() > 1)
            ? story.getCharacters().subList(1, story.getCharacters().size())
            : List.of();
    String supportingLine = supporting.isEmpty()
            ? ""
            : "Supporting characters: " + String.join(", ", supporting) + "\n";

    return "Generate a flat vector SVG illustration for a children's fairy tale.\n\n" +
           "Scene: " + sceneDescription + "\n" +
           "Main character: " + mainChar + "\n" +
           supportingLine +
           "Character position: center-left of canvas\n" +
           "Mood: warm, cheerful, safe\n" +
           "Time of day: sunny afternoon or golden hour\n" +
           "Sky color: soft blue (#a8d8f0) fading to peach (#fde8c8)\n" +
           "Ground color: soft green (#7bc67e)\n\n" +
           "Decorative elements (choose 2 maximum):\n" +
           "- A lollipop-style tree to the right of character\n" +
           "- 2-3 white clouds in upper sky\n" +
           "- Small colorful flowers in ground strip\n" +
           "- Sun in upper-left corner with short rays\n\n" +
           "Character details:\n" +
           "- Head shape: circle, choose an appropriate warm color for this character based on their name and type\n" +
           "- Eyes: two small dark circles with white highlight dot\n" +
           "- Expression: happy, slightly smiling (use a simple arc path for mouth)\n" +
           "- Body: rounded rectangle, same color family as head\n" +
           "- Scale: character height = 35-40% of canvas height (210-240px)\n\n" +
           "Age group: " + story.getAgeGroup() + "\n" +
           "Story context: " + firstTwoSentences(story.getContent());
}

private static String firstTwoSentences(String content) {
    if (content == null || content.isBlank()) return "";
    String[] parts = content.split("(?<=\\.)\\s+", 3);
    return parts.length >= 2 ? parts[0] + ". " + parts[1] : parts[0];
}
```

Add the required import for `Story` at the top of `PromptBuilder.java` (remove the `GenerationRequest` import if it's no longer used after removing `buildIllustrationPrompt` — check first):

```java
import com.kazka.story.Story;
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd backend && ./gradlew test --tests "com.kazka.story.PromptBuilderTest" --info 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` — all tests green.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/kazka/story/PromptBuilder.java \
        backend/src/test/java/com/kazka/story/PromptBuilderTest.java
git commit -m "feat: add SVG prompt methods to PromptBuilder, remove raster illustration prompt"
```

---

### Task 5: Add `saveSvg()` to `ImageStorageService`, update `delete()`

**Files:**
- Modify: `backend/src/main/java/com/kazka/illustration/ImageStorageService.java`
- Modify: `backend/src/test/java/com/kazka/illustration/ImageStorageServiceTest.java`

`delete()` currently only removes `.png`. It needs to also remove `.svg` so that deleting a story cleans up whichever file exists.

- [ ] **Step 1: Write the failing tests**

Open `backend/src/test/java/com/kazka/illustration/ImageStorageServiceTest.java`.
Add these tests at the end of the class before the closing `}`:

```java
@Test
void saveSvg_writesFileToDisk() throws IOException {
    ImageStorageService service = new ImageStorageService(tempDir.toString());
    String svgText = "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>";

    String path = service.saveSvg("story-789", svgText);

    assertThat(path).isEqualTo("/uploads/story-789.svg");
    Path file = tempDir.resolve("story-789.svg");
    assertThat(file).exists();
    assertThat(Files.readString(file)).isEqualTo(svgText);
}

@Test
void delete_removesSvgFile() throws IOException {
    ImageStorageService service = new ImageStorageService(tempDir.toString());
    Path file = tempDir.resolve("story-abc.svg");
    Files.writeString(file, "<svg/>");

    service.delete("story-abc");

    assertThat(file).doesNotExist();
}

@Test
void delete_removesBothPngAndSvg() throws IOException {
    ImageStorageService service = new ImageStorageService(tempDir.toString());
    Path png = tempDir.resolve("story-xyz.png");
    Path svg = tempDir.resolve("story-xyz.svg");
    Files.writeString(png, "png-data");
    Files.writeString(svg, "<svg/>");

    service.delete("story-xyz");

    assertThat(png).doesNotExist();
    assertThat(svg).doesNotExist();
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd backend && ./gradlew test --tests "com.kazka.illustration.ImageStorageServiceTest" --info 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` for existing tests, new tests fail with `saveSvg method not found`.
Actually compilation will fail first — that's the expected failure.

- [ ] **Step 3: Update `ImageStorageService`**

Open `backend/src/main/java/com/kazka/illustration/ImageStorageService.java`.

Add `StandardCharsets` import at the top:

```java
import java.nio.charset.StandardCharsets;
```

Add `saveSvg()` method after the existing `save()` method:

```java
public String saveSvg(String storyId, String svgText) {
    Path file = uploadsDir.resolve(storyId + ".svg");
    try {
        Files.writeString(file, svgText, StandardCharsets.UTF_8);
    } catch (IOException e) {
        throw new UncheckedIOException("Cannot save SVG for story " + storyId, e);
    }
    return "/uploads/" + storyId + ".svg";
}
```

Replace the existing `delete()` method with:

```java
public void delete(String storyId) {
    tryDelete(uploadsDir.resolve(storyId + ".png"));
    tryDelete(uploadsDir.resolve(storyId + ".svg"));
}

private void tryDelete(Path file) {
    try {
        Files.deleteIfExists(file);
    } catch (IOException e) {
        log.warn("Could not delete file {}: {}", file, e.getMessage());
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd backend && ./gradlew test --tests "com.kazka.illustration.ImageStorageServiceTest" --info 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` — all 6 tests green.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/kazka/illustration/ImageStorageService.java \
        backend/src/test/java/com/kazka/illustration/ImageStorageServiceTest.java
git commit -m "feat: add saveSvg() to ImageStorageService, update delete() to remove both png and svg"
```

---

### Task 6: Rewrite `IllustrationService` with two-phase SVG pipeline

**Files:**
- Modify: `backend/src/main/java/com/kazka/illustration/IllustrationService.java`

Replace the single-call raster pipeline with the two-phase reactive chain. Remove the old `saveImage()` private method. Keep `deleteImage()` unchanged — it delegates to `imageStorageService.delete()` which already handles both file types.

- [ ] **Step 1: Replace `IllustrationService` body**

Open `backend/src/main/java/com/kazka/illustration/IllustrationService.java`.

Replace the entire file content with:

```java
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

    private static final String PLACEHOLDER_SVG =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 800 600\" width=\"800\" height=\"600\">" +
            "<rect width=\"800\" height=\"600\" fill=\"#fde8c8\"/>" +
            "</svg>";

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
                    String fallback = story.getCharacters().get(0)
                            + " in a magical scene from " + story.getTitle();

                    return hfClient.generateText(
                                    promptBuilder.buildSceneExtractionSystem(),
                                    promptBuilder.buildSceneExtractionUser(story.getContent()))
                            .onErrorReturn(fallback)
                            .map(scene -> scene.isBlank() ? fallback : scene)
                            .flatMap(scene ->
                                    hfClient.generateText(
                                            promptBuilder.buildSvgSystem(),
                                            promptBuilder.buildSvgUser(story, scene)))
                            .map(this::extractSvgTag)
                            .flatMap(svg -> saveSvg(story, svg))
                            .onErrorResume(e -> {
                                log.warn("SVG illustration failed for {}: {}", storyId, e.getMessage());
                                return markFailed(story);
                            });
                });
    }

    private String extractSvgTag(String raw) {
        int start = raw.indexOf("<svg");
        int end = raw.lastIndexOf("</svg>") + 6;
        if (start == -1 || end < 6) {
            log.warn("LLM response contained no <svg> tag; using placeholder");
            return PLACEHOLDER_SVG;
        }
        return raw.substring(start, end);
    }

    private Mono<Void> saveSvg(Story story, String svgText) {
        return Mono.fromRunnable(() -> {
            String path = imageStorageService.saveSvg(story.getId(), svgText);
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

- [ ] **Step 2: Compile-check**

```bash
cd backend && ./gradlew compileJava 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run all unit tests**

```bash
cd backend && ./gradlew test --tests "com.kazka.illustration.*" --tests "com.kazka.story.PromptBuilderTest" --info 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` — all tests green.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kazka/illustration/IllustrationService.java
git commit -m "feat: replace raster image pipeline with two-phase SVG generation in IllustrationService"
```

---

## Self-Review Checklist

- [x] **Spec coverage**: Config (Task 1), prompt files (Task 2), `generateText()` (Task 3), `PromptBuilder` methods (Task 4), `saveSvg()` (Task 5), reactive chain (Task 6). All spec sections covered.
- [x] **No placeholders**: All steps contain actual code. No "TBD" or "handle edge cases".
- [x] **Type consistency**: `generateText(String, String): Mono<String>` defined in Task 3, used in Task 6. `saveSvg(String, String): String` defined in Task 5, used in Task 6. `buildSceneExtractionSystem/User/SvgSystem/SvgUser` defined in Task 4, used in Task 6. All consistent.
- [x] **`PromptBuilder` constructor**: Old `illustrationStyle` load removed, two new loads added. `readResource` helper unchanged and reused.
- [x] **Dead code**: `buildIllustrationPrompt()` removed in Task 4. `saveImage()` removed in Task 6. Corresponding test removed in Task 4.
