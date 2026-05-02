# PNG Illustrations — Child-Drawn Style by Age

**Date:** 2026-05-02
**Status:** Design (awaiting implementation plan)

## 1. Goals & Scope

### Goals

1. Replace the three inline-SVG illustrations on the marketing pages (`HomePage` hero / `HowItWorks` / `StoryPreview`) with PNG images that visibly demonstrate the child-drawing-by-age personalization the product offers.
2. Revert the AI illustration backend from text-LLM SVG output back to image-diffusion PNG output, with three age-specific child-drawing prompts.
3. Each generated story produces a light-theme PNG and a dark-theme PNG, matching the frontend theme toggle.

### In Scope

- **Backend:** revert SVG path in `IllustrationService` + `ImageStorageService` + `PromptBuilder`. Add 6 prompt files (3 ages × 2 themes). Use a HuggingFace image-diffusion model. Keep two-phase flow: scene-extraction LLM stays, only the second phase becomes diffusion. Two API calls per story (light + dark).
- **Static assets:** 18 sample PNGs in `frontend/public/illustrations/` (3 sections × 3 ages × 2 themes). Generated once via a one-shot Spring Boot CLI runner that calls the new pipeline; results committed to git.
- **Frontend:** new reusable `<IllustrationCarousel />` component — auto-cycles through the three age groups every ~4s, manual age tabs override, theme-aware image source. Used in all three sections; the existing `HeroIllustration` / `CastleIllustration` / `StoryIllustration` inline SVGs are deleted.

### Out of Scope

- Decorative `bgIllust` SVG behind `HowItWorks` (kept untouched).
- `SectionParticles` component.
- Story-form fields and story-generation text pipeline.
- `nginx` mime-type configuration for `.svg` (kept; harmless to other usages).

---

## 2. Backend — Revert SVG, Add Diffusion + Age-Style

### Prompt files

Six new style preamble files in `backend/src/main/resources/prompts/`:

- `image-style-3-5-light.txt`
- `image-style-3-5-dark.txt`
- `image-style-6-8-light.txt`
- `image-style-6-8-dark.txt`
- `image-style-9-12-light.txt`
- `image-style-9-12-dark.txt`

Each contains only the style preamble (no scene). The runtime prompt is `[style preamble] + [extracted scene]`.

Sample for `image-style-3-5-light.txt`:

> A child's drawing made with thick wax crayons by a 4-year-old. Scribbled lines, simple round shapes, big circular heads, stick limbs, bright primary colors on warm cream paper, naive perspective, no shading, drawn in a children's notebook.

Sample for `image-style-9-12-dark.txt`:

> A pencil and colored-pencil sketch by a 10-year-old, on dark navy textured paper. Recognizable detailed forms, attempt at shading and texture, foreground and background distinction, glowing highlights from moonlight or candlelight, slightly sophisticated school-age child art.

### Files removed

- `backend/src/main/resources/prompts/svg-system.txt`
- `IllustrationService.PLACEHOLDER_SVG`
- `IllustrationService.extractSvgTag()`
- `IllustrationService.saveSvg()`
- `ImageStorageService.saveSvg()`
- `PromptBuilder.buildSvgSystem()` / `buildSvgUser()`
- `HuggingFaceClient` SVG-text invocation path is generalized away (the `generateText` method itself stays — scene extraction still uses it)
- `HF_SVG_MODEL` config property

### Files kept

- `scene-extraction-system.txt` and the related `PromptBuilder.buildSceneExtractionSystem()` / `buildSceneExtractionUser()` methods.

### `PromptBuilder` additions

- `buildImageStylePreamble(AgeGroup age, Theme theme)` returns the right of 6 strings.
- `buildImagePrompt(Story story, String scene, Theme theme)` returns `[style preamble] + ". " + [scene]`, capped to ~75 tokens (FLUX prompt limit).
- `enum Theme { LIGHT, DARK }` lives next to `AgeGroup`.

### `HuggingFaceClient` additions

- Restore `Mono<byte[]> generateImage(String model, String prompt, int width, int height)` calling the HF inference image endpoint. Default model from `HF_IMAGE_MODEL` env var, recommended `black-forest-labs/FLUX.1-schnell`.
- `ImageStorageService.savePng(String storyId, Theme theme, byte[] bytes) → String path` reinstated. Storage layout: `uploads/{storyId}-light.png` and `uploads/{storyId}-dark.png`.

### `IllustrationService.generateAndStore(storyId)` reactive flow

```
findById
  → scene-extract (1 LLM call, theme-neutral)
  → Mono.zip(
        generateImage(buildImagePrompt(story, scene, LIGHT), 1024, 768),
        generateImage(buildImagePrompt(story, scene, DARK),  1024, 768)
    )
  → saveBoth(story, lightBytes, darkBytes)  // sets two paths
  → set status=READY, save story
```

### `Story` entity changes

- Field rename: `illustrationPath` → `illustrationPathLight`.
- New field: `illustrationPathDark` (nullable for legacy rows).
- `schema.sql` updated to match (Hibernate is `validate`-only per `CLAUDE.md`).
- Story REST DTO exposes both URLs; frontend chooses by current theme.

### Failure mode

If either of the two image-generation calls fails, mark `IllustrationStatus.FAILED`. No partial success — a story showing in only one theme feels broken.

### Tests

- `ImageStorageServiceTest` reverted/expanded for `savePng` light + dark pair.
- `IllustrationServiceTest` (new or reinstated) covers the two-image flow and partial-failure path.
- `saveSvg` failure-path test removed.

---

## 3. Static Asset Generation (18 Marketing PNGs)

### Goal

Produce 18 PNGs at `frontend/public/illustrations/` matching the live pipeline's output exactly, so users see consistent style.

### Approach

A one-shot Spring Boot CLI runner — `IllustrationSampleGenerator` — invoked by a Gradle task. Not part of the running app.

### Files

- `backend/src/main/java/com/kazka/tools/IllustrationSampleGenerator.java` — `@Component` + `CommandLineRunner` activated only by Spring profile `sample-gen`.
- `backend/build.gradle` — new task `generateSamples` runs `bootRun --args='--spring.profiles.active=sample-gen'`.

### Inputs (hard-coded)

Three section scenes (the same scene is reused across the three ages — only drawing style varies, not subject):

- `hero` scene: "a friendly fox sitting under a starry forest at twilight, with glowing mushrooms"
- `how` scene: "a magical castle on a hill at night with shooting stars and glowing windows"
- `preview` scene: "a tiny glowing star named Mia walking on a silvery moss path beside a great oak tree in an enchanted forest"

### Behavior

The runner iterates `sections × ages × themes` (3 × 3 × 2 = 18) and:

1. Calls `PromptBuilder.buildImageStylePreamble(age, theme)` + scene.
2. Calls `HuggingFaceClient.generateImage(...)` with `1024×768` for `hero` / `preview`, `768×1024` for `how` (matches existing viewBox aspect ratios).
3. Writes bytes to `frontend/public/illustrations/{section}-{age}-{theme}.png`. Filenames: `hero-3-5-light.png`, `hero-3-5-dark.png`, …, `preview-9-12-dark.png` (18 total).

### Reproducibility

Seed every call from a fixed RNG so re-running produces near-identical outputs. Helps when iterating prompts.

### When run

Locally, manually, after each prompt edit. PNGs are committed to git (one-time cost, not regenerated on every push). They're treated like designed assets — version-controlled and reviewed in PR.

### Fallback

If HF is unreachable, runner exits non-zero; existing PNGs in repo remain valid until a successful regeneration. No CI dependency.

---

## 4. Frontend — `<IllustrationCarousel />`

### Location

`frontend/src/components/illustrations/IllustrationCarousel.tsx`. Used by all three sections.

### Props

```ts
type AgeGroup = '3-5' | '6-8' | '9-12'
interface Props {
  section: 'hero' | 'how' | 'preview'
  width: number
  height: number
  className?: string
  intervalMs?: number  // default 4000
}
```

### State

- `ageIndex: number` — `0 | 1 | 2`, index into `['3-5', '6-8', '9-12']`.
- `manual: boolean` — true once a user clicks an age tab; auto-cycle stops permanently for that mount.

### Theme awareness

```ts
const { theme } = useTheme()
const src = `/illustrations/${section}-${AGE_KEYS[ageIndex]}-${theme}.png`
```

### Rendering

- Container: `position: relative`, fixed `width`/`height`, rounded corners matching design.
- Image: `<img src={src} alt={ageLabel + ' drawing'} loading="eager" decoding="async" />` absolutely positioned full-bleed. Crossfade via opacity transition (300ms) when `src` changes.
- Optional age-label badge in a corner: small chip showing `t.form.ageGroups[currentAge]` ("3–5 years"). Subtle.
- Age tabs row below image: 3 small pill buttons ("3–5", "6–8", "9–12"). Clicking sets `ageIndex` + `manual = true`. Active tab highlighted.

### Auto-cycle

`useEffect` sets `setInterval` to advance `ageIndex` if `!manual`. Cleared on unmount or when `manual` becomes true.

### Synchronization across sections

A tiny module-level event emitter (`carouselTickStore.ts`) emits each "advance" tick. Each `<IllustrationCarousel />` listens and advances together — gives the coordinated demo feel without any of them owning the timer. When any one carousel goes manual, only that instance stops; others keep cycling. Simpler than React Context for this single concern.

### Preloading

On first mount, prefetch the other 2 age PNGs for the current theme (`new Image().src = ...`). When `theme` changes, prefetch the matching age in the new theme.

### Accessibility

- `role="img"` on container with `aria-label={alt}`.
- Tabs are `<button>` elements with `aria-pressed`.
- `prefers-reduced-motion` disables auto-cycle and crossfade.

### No fallback rendering

If a PNG fails to load, the broken image stays — these are committed assets, missing one is a bug, not a runtime case.

---

## 5. Section Integrations

### `HomePage.tsx` (hero)

- Delete the entire `HeroIllustration()` function.
- Replace `<HeroIllustration />` with:
  ```tsx
  <IllustrationCarousel section="hero" width={520} height={390} className={styles.heroSvg} />
  ```
- `styles.heroSvg` keeps the layout slot. May be renamed to `.heroIllust` (cosmetic; can defer).

### `HowItWorks.tsx`

- Delete `CastleIllustration()`.
- Replace `<CastleIllustration />` inside `.illustWrap` with:
  ```tsx
  <IllustrationCarousel section="how" width={300} height={400} />
  ```
- The decorative inline `bgIllust` SVG (~lines 296–318) is kept untouched — it's a soft background flourish, not the main illustration.

### `StoryPreview.tsx`

- Delete `StoryIllustration()`.
- Replace `<StoryIllustration />` inside `.bookRight` with:
  ```tsx
  <IllustrationCarousel section="preview" width={520} height={390} />
  ```
- The recently-added `[data-theme="light"]` overrides for `.section`, `.title`, `.bookRight`, `.pageNumRight` are kept — they handle the surrounding chrome regardless of carousel content.

### Story detail page

- Locate the existing illustration render in the story detail page (`grep -r "illustrationPath" frontend/src`). It currently renders the SVG path returned by the backend.
- Replace with theme-aware src selection:
  ```tsx
  const { theme } = useTheme()
  const src = theme === 'dark' ? story.illustrationPathDark : story.illustrationPathLight
  ```
- If existing render uses inline SVG injection (from the recent SVG migration), replace with plain `<img>`.

### Locale

No new strings needed; reuse `t.form.ageGroups['3-5']` etc. for age-tab labels.

---

## 6. Migration & Risks

### Database migration

- Field rename `illustrationPath` → `illustrationPathLight`.
- New field `illustrationPathDark` (nullable for legacy rows).
- `schema.sql` updated; existing rows have their old `illustrationPath` value moved to `illustrationPathLight`; `illustrationPathDark` defaults `NULL`.
- Stories with only `light` set will appear broken in dark mode until regenerated. Acceptable for the current scale; a future "regenerate illustration" UI button is a non-blocking follow-up.

### Stale `.svg` files in `uploads/`

Harmless. `deleteImage()` on story-delete cleans them up over time.

### HuggingFace API cost

Doubled per story (2 image calls). Stays within HF free tier during development; rate-limit handling deferred.

### Prompt iteration risk

Child-drawing style is hard to nail with diffusion models. Plan ~3 prompt-tweak rounds after the first 18-PNG generation. Handled organically; not part of the implementation plan.

### Tests

Per `CLAUDE.md`, the frontend has no test framework — verify the carousel component manually in browser across both themes plus `tsc --noEmit` and `npm run lint`.
