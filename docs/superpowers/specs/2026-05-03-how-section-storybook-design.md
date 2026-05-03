# How section as a turning storybook — design spec

**Date:** 2026-05-03
**Scope:** `frontend/src/components/home/HowItWorks.tsx` and friends
**Status:** Approved by user (brainstorming session)

## Problem

The `#how` section currently uses a two-column layout: a stacked list of three text-only steps on the left and a single auto-rotating illustration on the right (the same image cycling through age groups). The page-turn metaphor is invisible. For a children's storybook product (`kazka` = "fairy tale"), the section is missing the brand's central visual cue.

## Goal

Replace the section with an open storybook. Each "page" shows one step's story (text) and its illustration. Pages turn with a realistic paper curl, auto-advancing while the section is in view, with manual controls for users who want to drive.

## User experience

- The header (`How it works` label + section title) stays above the book.
- An open two-page book sits centered below the header, replacing the current 2-column layout.
  - **Left page:** step number (`I` / `II` / `III`), step label, title, and description — text only.
  - **Right page:** full-bleed illustration for that step.
- The book opens to step 1 when the section first scrolls into view.
- Auto-advance turns the page every 6 seconds with a realistic curl animation. After step 3 it loops to step 1.
- `‹` `›` arrow buttons sit at the outer edges of the book; pagination dots sit underneath. Both jump to a page and permanently pause auto-advance.
- Keyboard `←` / `→` turn pages when focus is in the book; `Home` / `End` jump to first / last.
- `prefers-reduced-motion: reduce` disables auto-advance and the curl animation; pages snap instantly.
- Below 860px the book switches to a single portrait page (image on top, story below) using the same curl, same auto-advance, same controls.

## Architecture

### New files

- `frontend/src/components/home/StoryBook.tsx` — owns the book: page state, auto-advance timer, in-view detection, reduced-motion gate, prev/next handlers, dots, arrows, keyboard.
- `frontend/src/components/home/StoryBook.module.css` — book chrome (arrows, dots, page surfaces, parchment colors). Page-curl animation comes from the library; this CSS only styles the page contents.
- `frontend/src/lib/useReducedMotion.ts` — small hook returning `boolean` from `matchMedia('(prefers-reduced-motion: reduce)')` with a change listener.

### Modified files

- `frontend/src/components/home/HowItWorks.tsx` — shrinks to: header (label + title) + `<Suspense fallback={<FallbackSteps />}><StoryBook /></Suspense>`. Removes `StepItem`, the dashed connector line, the 2-column grid, the `IllustrationCarousel` slot. Keeps the decorative `bgIllust` SVG.
- `frontend/src/components/home/HowItWorks.module.css` — drop `.layout`, `.steps`, `.step`, `.stepLine`, `.stepNumWrap`, `.stepNum`, `.stepContent`, `.stepLabel`, `.stepTitle`, `.stepDesc`, `.illustWrap`, `.illustQuill`. Keep `.section`, `.inner`, `.label`, `.title`, `.bgIllust`.
- `frontend/package.json` — add `react-pageflip` (latest, ~50 KB).

### New assets

Six PNG illustrations in `frontend/public/illustrations/`:

- `how-step1-light.png`, `how-step1-dark.png`
- `how-step2-light.png`, `how-step2-dark.png`
- `how-step3-light.png`, `how-step3-dark.png`

The existing `how-{age}-{theme}.png` files become orphaned in this section (still used for `IllustrationCarousel` if referenced from elsewhere — they are not). They can be removed in a follow-up cleanup commit.

The per-age rotation is dropped for the `how` section: each step now has its own art, so rotating the same image through age groups adds no value. Hero and preview sections keep their age rotation unchanged.

### Library: `react-pageflip`

`react-pageflip` (v2.x) wraps the StPageFlip vanilla library and provides a React `<HTMLFlipBook>` component with the realistic page-curl effect, drag-corner support, programmatic flip API (`flipNext`, `flipPrev`, `flip(n)`), and a built-in `usePortrait` mode for narrow screens. Bundle cost ~50 KB minified, lazy-loaded with `React.lazy` since `#how` is below the fold.

### Component shape

```tsx
// StoryBook.tsx (sketch — not final code)
const HTMLFlipBook = lazy(() => import('react-pageflip'))

function StoryBook() {
  const { t } = useLocale()
  const { theme } = useTheme()
  const reducedMotion = useReducedMotion()
  const sectionRef = useRef<HTMLDivElement>(null)
  const bookRef = useRef<HTMLFlipBookHandle>(null)
  const [paused, setPaused] = useState(false)
  const [page, setPage] = useState(0)
  const isPortrait = useMediaQuery('(max-width: 860px)')

  // auto-advance: 6s timer, only when section in view, not paused, not reduced motion
  useAutoAdvance({ enabled: !paused && !reducedMotion, intervalMs: 6000, onTick: () => bookRef.current?.flipNext() })

  return (
    <div ref={sectionRef} role="region" aria-roledescription="storybook" aria-label={t.howItWorks.title}>
      <HTMLFlipBook ref={bookRef} usePortrait={isPortrait} flippingTime={reducedMotion ? 0 : 800} ...>
        {pages.map(p => <Page key={p.id}>{p.content}</Page>)}
      </HTMLFlipBook>
      <Arrows onPrev={...} onNext={...} />
      <Dots count={steps.length} active={page} onJump={...} />
      <LiveAnnouncer page={page} />
    </div>
  )
}
```

Page composition differs by viewport:

- **Desktop (`!isPortrait`)** — 3 spreads, each spread = `<StoryPage>` + `<IllustrationPage>`. Total: 6 page nodes the library treats as 3 spreads.
- **Mobile (`isPortrait`)** — 6 single pages alternating image / story / image / story... The library handles the mode swap on `usePortrait` change; current page index is preserved by snapping the new index to the equivalent step.

### Data

No locale or schema changes. Reads `t.howItWorks.steps` (already an array of `{num, stepLabel, title, desc}`).

## Behavior details

### Auto-advance

- Starts when `IntersectionObserver` reports the section ≥ 50% visible for the first time.
- Pauses when the section leaves the viewport; resumes when it returns (until first user interaction).
- Pauses when `document.visibilitychange` reports `hidden`.
- Pauses **permanently** on first user interaction (arrow click, dot click, corner drag/click, keyboard). The book stays where the user left it; manual controls remain active.

### Accessibility

- `role="region"`, `aria-roledescription="storybook"`, `aria-label={section title}` on the book wrapper.
- Each story page is `aria-labelledby` its step heading.
- Image pages use `alt=""` (decorative); the story content is the screen-reader-accessible source of truth so users don't get duplicates.
- `<div role="status" aria-live="polite" class="sr-only">` announces page changes: *"Step 2 of 3: Choose the magic"*.
- Arrows and dots are real `<button>` elements with descriptive `aria-label`s.
- Keyboard: `←` / `→` turn pages, `Home` / `End` jump to first / last. Focus stays inside the book region while navigating.

### Reduced motion

- Auto-advance disabled.
- `flippingTime: 0`, `drawShadow: false` on the library — pages snap.
- Arrows, dots, keyboard still work.

### Edge cases

- **Image still loading on flip** — page background shows the parchment color + a subtle pulse skeleton until `<img>` `onLoad` fires. Adjacent step images are preloaded at mount (same trick `IllustrationCarousel.tsx` uses).
- **`react-pageflip` chunk fails to load** — `<Suspense>` + an `ErrorBoundary` render `FallbackSteps`, a stacked text-only list of the three steps (no images, no animation). Section remains usable.
- **Theme toggle mid-flip** — image swaps to the dark / light variant on next render; no attempt to swap during the curl animation.
- **Locale toggle** — text re-renders on the current page; current page index is preserved.
- **SSR / Vite** — `react-pageflip` is browser-only; lazy load + `Suspense` already gates it.
- **Resize across the 860 px breakpoint** — book reinitializes with the new `usePortrait` value; current logical step (0..2) is preserved by mapping desktop spread index ↔ mobile page index.

## Testing & verification

The frontend has no test framework — verification is `tsc --noEmit` + `npm run lint` + manual browser check, per project `CLAUDE.md`.

**Automated:**
- `cd frontend && node_modules/.bin/tsc --noEmit` passes.
- `cd frontend && npm run lint` passes.

**Manual checklist:**
1. Section auto-advances when scrolled into view; stops when scrolled away; resumes when scrolled back (until first interaction).
2. Auto-advance pauses while another tab is focused.
3. `‹` / `›` arrows, dots, and `←` / `→` all work; first interaction permanently pauses auto-advance.
4. Keyboard `Home` / `End` jump to first / last page.
5. Page curl plays smoothly in light and dark themes; image and text both legible during the curl.
6. Resizing across 860 px swaps between two-page spread and single portrait page without losing the current step.
7. `prefers-reduced-motion: reduce` (toggle in browser devtools) — no curl, no auto-advance, controls still work.
8. Locale toggle preserves current page; text updates immediately.
9. With DevTools → offline (after initial load), force a fresh chunk load and confirm `FallbackSteps` renders.
10. VoiceOver / NVDA reads the page label on each turn ("Step 2 of 3…").
11. Lighthouse a11y score on `/` does not regress.

## Out of scope

- Per-age image rotation in the `#how` section (replaced by per-step images).
- Cleanup / removal of the now-orphaned `how-{age}-{theme}.png` files (follow-up commit).
- Sound effects on page turn.
- Drag-velocity-based curl physics (StPageFlip's defaults are good enough).
- A/B testing the auto-advance interval.

## Risks

- **Bundle size.** `react-pageflip` adds ~50 KB to the lazy chunk. Acceptable for a hero marketing section that loads after first paint, but worth verifying with `vite build` size output.
- **Library quality.** `react-pageflip` is the established option but maintenance cadence varies. If we hit blocking bugs, the fallback is to hand-roll the curl with CSS 3D transforms — significantly more work, postponed unless needed.
- **Image generation.** Six new illustrations are required for this section to look finished and they must land in the same release as the mechanic (decided during brainstorming). If image generation slips, the mechanic should not ship — a book with three identical-looking pages is worse than the current static layout.
