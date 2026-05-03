# Preview section as a 2-spread flip book — design spec

**Date:** 2026-05-03
**Scope:** `frontend/src/components/home/StoryPreview.tsx`, plus a revert of `HowItWorks.tsx` to its pre-storybook state and a small refactor of the existing storybook primitives.
**Status:** Approved by user (brainstorming session)

## Problem

Earlier today the page-curl storybook mechanic was placed in the `#how` section. After seeing it live, the user determined that the storybook metaphor is a poor fit for an informational "how it works" explainer (which reads better as a numbered text list with a sidebar illustration), but is a strong fit for the `#preview` section, which already pretends to be an open book showing a story example. The `#preview` section currently has a static 2-column "left = text, right = illustration" layout with a one-shot typewriter and no real page-flip — the metaphor is implied but not delivered.

## Goal

Two coordinated changes:
1. Revert `#how` to its pre-storybook design (3 stacked numbered text steps + sidebar `IllustrationCarousel` rotating through age groups).
2. Replace `#preview` with a real 2-spread flip book using the `react-pageflip` machinery built earlier today. Manual navigation only (no auto-advance). Preserve the existing typewriter on the first spread's left page; subsequent pages render instantly.

## Part 1 — `#how` revert

### Files

- Modify: `frontend/src/components/home/HowItWorks.tsx` — restore the contents from commit `0b32b96` (the version that existed immediately before the storybook batch).
- Modify: `frontend/src/components/home/HowItWorks.module.css` — restore the contents from commit `0b32b96`.

### Behavior

The section returns to its earlier behavior:
- Header (label + title).
- 3 stacked numbered steps (`I` / `II` / `III`) with per-step animated reveals (number flips in, dashed connector line draws between steps).
- Sidebar `IllustrationCarousel section="how"` rotating through age-group illustrations.
- Decorative `bgIllust` SVG remains (it was kept across both versions).

### Verification

After the revert:
- The `IllustrationCarousel` import resolves.
- The 3 step entries from `t.howItWorks.steps` continue to render correctly (locale shape unchanged).
- All locale strings the reverted file references are still present.

## Part 2 — `#preview` becomes a 2-spread book

### User experience

- The section initially renders with the book occupying the central area. The header (label + title) stays above; the existing `tagline` stays below.
- When the section enters the viewport, spread 1 reveals: left page = the existing dropcap-led "Once upon a time…" opening, **typing out** character-by-character (10 ms per character, same logic as today). Right page = `preview-page1-{theme}.png` illustration with the loading-skeleton parchment background and fade-in.
- The user can flip the page at any time via:
  - `›` arrow button under the book
  - dot 2 under the book
  - keyboard `→`, `End`
  - drag the bottom-right page corner
- Spread 2: left page = a continuation paragraph (`text2` from the locale) — renders instantly, no typewriter. Right page = `preview-page2-{theme}.png` illustration.
- Page numbers in the corner of each page: `3 / 4 / 5 / 6`.
- **No auto-advance.** The book stays where the user puts it.
- If the user flips the page before the typewriter finishes, the typewriter is stopped and the rest of the spread-1 text appears in full immediately. (No half-typed pages floating in the background.)
- Below 860 px, the book switches to single-page portrait mode (4 sequential pages). Same controls.
- `prefers-reduced-motion`: typewriter disabled (text appears whole on first reveal); page curl disabled (snap); auto-advance was already off, so nothing changes.

### Architecture

The existing storybook primitives built earlier today are refactored to be content-agnostic, then reused by the preview. No new `PreviewBook` component — `StoryBook` becomes the generic container.

#### Files: refactored

- `frontend/src/components/home/StoryBook.tsx` — drop the hardcoded `t.howItWorks.steps` composition. New props:

  ```ts
  interface StoryBookProps {
    pages: ReactNode[]                    // caller assembles
    ariaLabel: string                     // for the region label
    autoAdvance?: boolean                 // default true; preview passes false
    intervalMs?: number                   // default 6000; ignored when autoAdvance=false
    prevAria: string                      // arrow-button labels
    nextAria: string
    dotAria: string                       // template containing `{n}`
    announce: string                      // template containing `{n}` and `{total}`
    onUserInteract?: () => void           // fires on first manual interaction (used by preview to stop the typewriter)
  }
  ```

  The `useAutoAdvance` hook is only invoked when `autoAdvance === true`. The IO observer, visibilitychange handler, keyboard nav, dots, arrows, live region, image-preload, mobile-portrait switch all stay. The `logicalStep` math becomes `Math.floor(currentPage / 2)` of `pages.length / 2` total spreads. The image preload effect (currently hardcoded to `/illustrations/how-step{n}-{theme}.png`) is removed — preload is now the caller's concern (StoryPreview will preload its 4 images).

- `frontend/src/components/home/IllustrationPage.tsx` — drop the `step: 1 | 2 | 3` constructor. New prop:

  ```ts
  interface IllustrationPageProps {
    src: string
  }
  ```

  `useTheme` is no longer needed inside the component (the caller computes the themed src). Loading state, skeleton fade-out, `onError`, `onLoad`, `alt=""` all stay.

#### Files: deleted

- `frontend/src/components/home/StoryPage.tsx` — was specific to the how-section "step number / label / title / desc" shape. Preview's left page is a different shape (dropcap + flowing prose). Replaced by inline `PreviewStoryPage` JSX in `StoryPreview.tsx`.
- `frontend/src/components/home/StoryBookFallback.tsx` — was hardcoded to render the 3 how-it-works steps. Each caller now provides its own fallback. `PreviewBookFallback` lives **inline in `StoryPreview.tsx`** (small enough that a separate file is over-engineering), same as `PreviewStoryPage`.

#### Files: kept as-is

- `frontend/src/components/home/StoryBookErrorBoundary.tsx` — generic error boundary, no changes.
- `frontend/src/components/home/StoryBook.module.css` — class names already content-agnostic. The `.pageStory` styles (padding, position) are still useful for the preview's left pages; the new `PreviewStoryPage` will reuse `.page` + `.pageStory`. The `.stepLabel`, `.stepTitle`, `.stepDesc`, `.pageNumber` rules become unused — drop them in this batch.
- `frontend/src/lib/useReducedMotion.ts`, `useBreakpoint.ts`, `useAutoAdvance.ts` — unchanged.

#### Files: modified for preview

- `frontend/src/components/home/StoryPreview.tsx` — full rewrite. Owns the typewriter state, theme lookup, image preload, and page composition. Uses `<StoryBook>` as the container.
- `frontend/src/components/home/StoryPreview.module.css` — drop the static `.bookSpread`, `.bookLeft`, `.bookRight`, `.pageOpen` animation, `.illustHero`. Keep `.section`, `.inner`, `.label`, `.title`, `.tagline`, `.dropCap`, `.storyText`, `.cursor`, `.pageNum`, `.pageNumLeft`, `.pageNumRight`. Add `.bookSlot` for centering the book.

#### Files: locale

- `frontend/src/locales/uk.ts` and `frontend/src/locales/en.ts`:
  - **Move** `prevAria`, `nextAria`, `dotAria`, `announce` from `howItWorks` to `storyPreview`.
  - **Rewrite** `announce` template: was `"Step {n} of {total}: {title}"`; becomes `"Page {n} of {total}"` (UA: `"Сторінка {n} з {total}"`). The preview pages don't have titles per se.
  - **Add** to `storyPreview`:
    - `text2` — the second paragraph of Mia's story (continuation). User to provide both languages.

  Page numbers (`3 / 4 / 5 / 6`) are hardcoded in the React component, not in the locale (they're presentational, not translatable).

  Provisional content:
  ```
  EN: text2: "She crossed a brook of moonlight, where silver fish leapt from rock to rock, and at last reached a clearing where seven fireflies sat in a circle, waiting for the smallest star in the world."
  UA: text2: "Вона перейшла струмок місячного світла, де сріблясті рибки стрибали з каменю на камінь, і нарешті дісталась галявини, де сім світлячків сиділи колом, чекаючи на найменшу зірку у світі."
  ```

  These can be tweaked by the user during the implementation review.

#### Files: assets (user provides)

- `frontend/public/illustrations/preview-page1-light.png` — Mia in the hollow of the ancient oak at night, warm dawn palette.
- `frontend/public/illustrations/preview-page1-dark.png` — same scene, deep-purple / star-glow palette.
- `frontend/public/illustrations/preview-page2-light.png` — Mia walking the silvery moss path (or the brook + fireflies scene from `text2`).
- `frontend/public/illustrations/preview-page2-dark.png` — same scene, dark variant.

The existing `preview-{age}-{theme}.png` files (3-5 / 6-8 / 9-12 × light/dark) become orphaned in this section — they are no longer referenced anywhere if the homepage hero doesn't use them. (The hero uses `IllustrationCarousel section="hero"`, which uses `hero-{age}-{theme}.png`. Confirm `IllustrationCarousel section="preview"` is no longer called from anywhere; if so, the preview-age files are safely orphaned.) Their cleanup is **out of scope** for this spec — file a follow-up.

### Component shape

Sketch (not final code):

```tsx
// StoryPreview.tsx (sketch)

function StoryPreview() {
  const { t, lang } = useLocale()
  const { theme } = useTheme()
  const sectionRef = useRef<HTMLDivElement>(null)
  const [inView, setInView] = useState(false)
  const [typed, setTyped] = useState('')
  const [done, setDone] = useState(false)
  const [interacted, setInteracted] = useState(false)

  // IO observer to start the typewriter on first reveal (matches existing logic).
  useEffect(() => { /* IO that flips inView true once */ }, [])

  // Typewriter — runs once when inView becomes true. Stops if user interacts.
  useEffect(() => { /* typewriter loop, completes early if interacted */ }, [inView, t.storyPreview.text, interacted])

  // Image preload for both spreads' images, current theme.
  useEffect(() => { /* new Image() × 2 */ }, [theme])

  const pages = useMemo(() => [
    <PreviewStoryPage key="text-1" pageNum={3} dropCap={t.storyPreview.dropCap} body={typed} showCursor={!done} />,
    <IllustrationPage key="img-1" src={`/illustrations/preview-page1-${theme}.png`} />,
    <PreviewStoryPage key="text-2" pageNum={5} body={t.storyPreview.text2} />,
    <IllustrationPage key="img-2" src={`/illustrations/preview-page2-${theme}.png`} />,
  ], [t, theme, typed, done])

  return (
    <section ref={sectionRef} className={styles.section} id="preview">
      <SectionParticles light />
      <div className={styles.inner}>
        {/* header (kept) */}
        <div className={styles.bookSlot}>
          <StoryBookErrorBoundary fallback={<PreviewBookFallback typed={typed} done={done} />}>
            <Suspense fallback={<PreviewBookFallback typed={typed} done={done} />}>
              <StoryBook
                pages={pages}
                ariaLabel={t.storyPreview.title}
                autoAdvance={false}
                prevAria={t.storyPreview.prevAria}
                nextAria={t.storyPreview.nextAria}
                dotAria={t.storyPreview.dotAria}
                announce={t.storyPreview.announce}
                onUserInteract={() => setInteracted(true)}
              />
            </Suspense>
          </StoryBookErrorBoundary>
        </div>
        <div className={styles.tagline}>{t.storyPreview.tagline}</div>
      </div>
    </section>
  )
}

// PreviewStoryPage — small forwardRef component declared in same file or in a separate one.
const PreviewStoryPage = forwardRef<HTMLDivElement, {
  pageNum: number
  dropCap?: string
  body: string
  showCursor?: boolean
}>(function PreviewStoryPage({ pageNum, dropCap, body, showCursor }, ref) {
  return (
    <div ref={ref} className={`${storyBookStyles.page} ${storyBookStyles.pageStory}`}>
      <div className={storyBookStyles.pageInner}>
        <p className={styles.storyText}>
          {dropCap && <span className={styles.dropCap}>{dropCap}</span>}
          {body}
          {showCursor && <span className={styles.cursor} aria-hidden="true" />}
        </p>
      </div>
      <div className={storyBookStyles.pageNumber} aria-hidden="true">{pageNum}</div>
    </div>
  )
})
```

To support the `interacted` flag for stopping the typewriter early, `StoryBook` exposes an `onUserInteract` callback prop that fires on the first manual interaction (arrow click, dot click, drag, keyboard).

### Behavior details

- **Typewriter timing.** The existing 10 ms / char loop is preserved verbatim. It begins when the section first scrolls into view (IO observer with the existing threshold). `lang` change resets `typed` to empty and re-types — also preserved. If `interacted` flips true mid-loop, the loop sets `typed = t.storyPreview.text` (full string) and `done = true` immediately.
- **First reveal vs. flip-back.** If the user flips to spread 2 and then back to spread 1, the text on spread 1 is already typed — no replay. Same for `lang`-driven re-type: it only happens on the initial reveal of spread 1, not on subsequent flips.
- **Image loading.** Same skeleton-pulse-then-fade as the previous storybook implementation. Preloading both spread-1 and spread-2 images on mount means the page-curl always reveals an already-loaded image.
- **Mobile.** `usePortrait={true}` below 860 px. The 4 page nodes become 4 sequential pages. The flip math in `StoryBook` (`isPortrait ? +1 : +2` for "next spread") is preserved.
- **Reduced motion.** Typewriter is bypassed (text appears whole on first reveal); curl flippingTime is 0; drawShadow is false. Manual controls all still work.

### Edge cases

- **`react-pageflip` chunk fails to load.** `StoryBookErrorBoundary` catches it and renders `PreviewBookFallback`, which displays the spread-1 left page (dropcap + typewritten text) plus the spread-1 illustration — essentially today's design, statically. The user can still read the example; just no flip. (Spread 2's content is not shown in the fallback to avoid layout complexity.)
- **Locale toggle mid-flip.** Text re-renders; current page index preserved. Typewriter does NOT replay on locale change once the section has been scrolled past once (because `typed` and `done` are seeded from the previous render — see `useEffect`). Acceptable behavior — same as today.
- **Theme toggle.** Image swaps on next render. Skeleton may briefly appear if cache hasn't warmed. Both images are preloaded for the active theme on mount, and the theme-change effect re-preloads.
- **User keeps clicking → past the last spread.** Library should clamp at the last page; verify in QA.

## Testing & verification

The frontend has no test framework. Verification per `CLAUDE.md`:
- `cd frontend && node_modules/.bin/tsc --noEmit` clean. **Note:** `tsc --noEmit` against the project's `tsconfig.json` stub silently checks zero files — so this is a weak check. The strong check is `cd frontend && npm run build` (which runs `tsc -b && vite build`). Both must pass.
- `cd frontend && npm run lint` — at most the pre-existing baseline (8 errors / 8 warnings).
- `cd frontend && npm run build` — must exit 0. The `react-pageflip` lazy chunk should remain ~45 KB raw / ~11 KB gzipped.

Manual checklist (run after the 4 illustrations land):
1. `#how` looks identical to its pre-storybook design — 3 numbered steps stack vertically, dashed line draws on reveal, sidebar carousel rotates through ages.
2. `#preview` opens to spread 1 on first scroll. Typewriter runs, dropcap renders correctly, cursor blinks during typing.
3. Click `›` mid-typewriter — text snaps to full, page curls to spread 2. Spread 2's text is fully visible.
4. Click `‹` to return — spread 1 text is still complete, no replay.
5. Click dot 2, then dot 1 — same.
6. Keyboard `→`, `←`, `Home`, `End` all work; first interaction also stops the typewriter early.
7. Drag the bottom-right corner — book turns smoothly.
8. Resize across 860 px — book switches to portrait mode without losing the current spread.
9. Toggle theme — both images swap to the dark/light variant cleanly; no flash if the variant is preloaded.
10. Toggle locale (UA ↔ EN) — text on the current spread updates immediately; current page preserved.
11. `prefers-reduced-motion: reduce` — typewriter disabled, page curl disabled, manual controls still work.
12. DevTools → offline + clear site data → reload — `PreviewBookFallback` renders (text + image, no flip).
13. VoiceOver / NVDA reads "Page 2 of 2" on flip.
14. Lighthouse a11y score on `/` does not regress.

## Out of scope

- Removal of the now-unused `preview-{age}-{theme}.png` files (3-5 / 6-8 / 9-12 × light / dark). Follow-up cleanup commit.
- Removal of the now-unused `how-step{1,2,3}-{light,dark}.png` files (these were never created, so nothing to remove).
- Re-purposing the existing `IllustrationCarousel` for some other section. (`section="hero"` continues to be used by `HomePage.tsx`.)
- Tests (no test framework in the project).

## Risks

- **Typewriter race with page-flip.** The typewriter mutates `typed` state on a `setInterval`. If the user flips during typing, we must guarantee the interval is cleared and the final state is `typed = full text` — otherwise the spread shows half-typed text the next time it's flipped to. The implementation must clear the interval AND set the full string in the same effect cleanup path. Acceptable; just be careful in the implementation.
- **Generalizing `StoryBook` is a real refactor.** The existing component currently couples state to `t.howItWorks.steps`. Removing that coupling cleanly while keeping the IO/visibility/keyboard/dots/arrows/live-region intact is the highest-risk part of the implementation. Should be reviewed carefully.
- **Locale key relocation.** Moving `prevAria` etc. from `howItWorks` to `storyPreview` is a structural change. The locale type is `typeof uk`, so `uk.ts` must be edited first; `en.ts` must structurally match. Any consumer that still references `t.howItWorks.prevAria` will break the build — but the only consumer is `StoryBook.tsx`, which is being refactored in the same change.
