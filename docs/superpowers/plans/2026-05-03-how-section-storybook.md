# How section as a turning storybook — implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current `#how` section (3-step text list + sidebar illustration) with an open two-page storybook that auto-turns pages with a realistic curl, with arrow/dot/keyboard controls, mobile single-page mode, and full a11y/reduced-motion support.

**Architecture:** A new `StoryBook` component owns the book and controls. It lazily loads `react-pageflip` (wraps StPageFlip ~50 KB). `HowItWorks` wraps `<StoryBook />` in `<StoryBookErrorBoundary><Suspense>` with a stacked-text fallback that handles both loading and chunk-load errors. Three small new hooks (`useReducedMotion`, `useBreakpoint`, `useAutoAdvance`) keep effect logic out of the component body.

**Tech Stack:** React 19, TypeScript 6, Vite 8, CSS Modules, `react-pageflip` 2.x.

**Reference spec:** `docs/superpowers/specs/2026-05-03-how-section-storybook-design.md`

**Important constraints (from `CLAUDE.md`):**
- No test framework. Verification = `cd frontend && node_modules/.bin/tsc --noEmit` + `cd frontend && npm run lint` + manual browser check.
- TypeScript command is `node_modules/.bin/tsc`, **not** `npx tsc`.
- All UI text via `useLocale()` `t` — no hardcoded Ukrainian/English strings.
- Design tokens in `src/design/tokens.css`. Use `var(--color-*)` rather than hex codes.

---

## File structure

**Created:**
- `frontend/src/lib/useReducedMotion.ts` — boolean hook for `prefers-reduced-motion: reduce`.
- `frontend/src/lib/useBreakpoint.ts` — boolean hook returning `true` when viewport ≤ a given px.
- `frontend/src/lib/useAutoAdvance.ts` — interval-based hook that fires `onTick` while enabled.
- `frontend/src/components/home/StoryBook.tsx` — book orchestrator (state, controls, a11y).
- `frontend/src/components/home/StoryBook.module.css` — book chrome, page surfaces, arrows, dots.
- `frontend/src/components/home/StoryPage.tsx` — left/text page (forwardRef).
- `frontend/src/components/home/IllustrationPage.tsx` — right/image page (forwardRef).
- `frontend/src/components/home/StoryBookFallback.tsx` — stacked text fallback shown while the lazy chunk loads or if it errors.
- `frontend/src/components/home/StoryBookErrorBoundary.tsx` — class-component error boundary scoped to the storybook.
- `frontend/public/illustrations/how-step{1,2,3}-{light,dark}.png` — six new images (asset task).

**Modified:**
- `frontend/package.json` — add `react-pageflip`.
- `frontend/src/components/home/HowItWorks.tsx` — replace 2-column layout with `<StoryBookErrorBoundary fallback={<StoryBookFallback />}><Suspense fallback={<StoryBookFallback />}><StoryBook /></Suspense></StoryBookErrorBoundary>`.
- `frontend/src/components/home/HowItWorks.module.css` — drop step/illustration CSS, keep section/inner/label/title/bgIllust.

**Unchanged but referenced:**
- `frontend/src/locales/{en,uk}.ts` — existing `t.howItWorks.steps` shape is fine.
- `frontend/src/lib/ThemeContext.tsx`, `LocaleContext.tsx` — used for theme/locale.

---

## Pre-flight

- [ ] **P.1: Confirm clean working tree**

Run: `git status`
Expected: only `frontend/src/components/home/Features.module.css` (the existing in-progress change unrelated to this work) is modified, or fully clean.
If other in-progress work exists, stash or commit it before starting this plan.

- [ ] **P.2: Confirm baseline build passes**

Run:
```bash
cd frontend && node_modules/.bin/tsc --noEmit && npm run lint
```
Expected: both succeed with no errors. If they fail before this plan starts, stop and fix the baseline first — otherwise we can't tell which task broke things.

---

## Task 1: Install `react-pageflip`

Add the page-curl library and verify it integrates with the React 19 / TS 6 / Vite 8 stack.

**Files:**
- Modify: `frontend/package.json`, `frontend/package-lock.json`

- [ ] **Step 1: Install the package**

Run:
```bash
cd frontend && npm install react-pageflip
```
Expected: package installs. If npm reports a peer-dep conflict on React (the library historically declared `react@^18`), retry with:
```bash
cd frontend && npm install react-pageflip --legacy-peer-deps
```
Note in the commit message that `--legacy-peer-deps` was needed if it was.

- [ ] **Step 2: Sanity-check the typings**

Run:
```bash
ls frontend/node_modules/react-pageflip/build/
cat frontend/node_modules/react-pageflip/build/HtmlFlipBook.d.ts | head -80
```
Expected: directory exists; `HtmlFlipBook.d.ts` is present and exports a default component with a props interface (e.g. `IProps`). Skim the prop names — you'll reference `width`, `height`, `size`, `usePortrait`, `flippingTime`, `drawShadow`, `useMouseEvents`, `mobileScrollSupport`, `clickEventForward`, `disableFlipByClick`, `showCover`, `startPage`, `maxShadowOpacity`. If a prop name in this plan turns out to differ from the actual `.d.ts`, trust the `.d.ts`.

- [ ] **Step 3: Verify the build still passes**

Run:
```bash
cd frontend && node_modules/.bin/tsc --noEmit && npm run lint
```
Expected: both succeed.

- [ ] **Step 4: Commit**

Run:
```bash
git add frontend/package.json frontend/package-lock.json
git commit -m "feat(frontend): add react-pageflip for storybook page-turn effect"
```

---

## Task 2: Generate the six step illustrations

Per the spec, the page-flip mechanic must not ship without the 3 step images (in light + dark variants). This task is asset generation, not code — but it's listed here so the implementation thread doesn't ship a half-finished section.

**Files:**
- Create:
  - `frontend/public/illustrations/how-step1-light.png`
  - `frontend/public/illustrations/how-step1-dark.png`
  - `frontend/public/illustrations/how-step2-light.png`
  - `frontend/public/illustrations/how-step2-dark.png`
  - `frontend/public/illustrations/how-step3-light.png`
  - `frontend/public/illustrations/how-step3-dark.png`

**Image briefs** (use the same illustration style as the existing `how-3-5-light.png` etc. — soft children's-storybook watercolor, warm palette in light mode, deep purples/golds in dark mode):

- **Step 1 — "Tell us about your child":** A parent and a child sitting together on a sofa or bed, the child whispering into the parent's ear or pointing at a sketchpad/tablet showing a doodle of a hero character. Cozy room, soft evening light. *Conveys: telling us about the child.*
- **Step 2 — "Choose the magic":** The child surrounded by floating fragments of imagined worlds — an enchanted tree, a starry archway, a small dragon — choosing among them. Whimsical, dreamy. *Conveys: picking the world/theme.*
- **Step 3 — "Enjoy the story":** Parent reading aloud to a child curled up under a blanket, an open book glowing softly between them, perhaps with little magical creatures peeking out. *Conveys: reading the finished story.*

Output: PNG, ~1024×1280 (4:5 portrait), under ~1.8 MB each (matches existing how-* file sizes).

- [ ] **Step 1: Generate the 6 PNGs and place them in `frontend/public/illustrations/`**

Use the project's usual image-generation workflow (the existing `hero-*`, `how-*`, `preview-*` PNGs were produced this way). File names must be exactly:
```
how-step1-light.png  how-step1-dark.png
how-step2-light.png  how-step2-dark.png
how-step3-light.png  how-step3-dark.png
```

- [ ] **Step 2: Verify file presence and rough sizes**

Run:
```bash
ls -la frontend/public/illustrations/how-step*.png
```
Expected: 6 files, each between ~500 KB and ~2 MB. If any file is missing, do not advance — the page-flip code below references these paths.

- [ ] **Step 3: Visual sanity check**

Open each file in Preview / a browser. Confirm:
- Aspect ratio is roughly 4:5 portrait.
- Light variants read clearly on a `#FAF0DC` (cream) background.
- Dark variants read clearly on a `#0F0A1E` (deep purple) background.
- No text overlays inside the image (text comes from the locale).

- [ ] **Step 4: Commit**

Run:
```bash
git add frontend/public/illustrations/how-step*.png
git commit -m "assets(frontend): add 6 step illustrations for storybook How section"
```

---

## Task 3: `useReducedMotion` hook

A small reusable hook that returns `true` if the user prefers reduced motion, kept up-to-date if the OS setting changes mid-session.

**Files:**
- Create: `frontend/src/lib/useReducedMotion.ts`

- [ ] **Step 1: Write the hook**

Create `frontend/src/lib/useReducedMotion.ts`:

```ts
import { useEffect, useState } from 'react'

const QUERY = '(prefers-reduced-motion: reduce)'

export function useReducedMotion(): boolean {
  const [reduced, setReduced] = useState<boolean>(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return false
    return window.matchMedia(QUERY).matches
  })

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return
    const mql = window.matchMedia(QUERY)
    const handler = (e: MediaQueryListEvent) => setReduced(e.matches)
    mql.addEventListener('change', handler)
    return () => mql.removeEventListener('change', handler)
  }, [])

  return reduced
}
```

- [ ] **Step 2: Verify build**

Run:
```bash
cd frontend && node_modules/.bin/tsc --noEmit && npm run lint
```
Expected: both succeed.

- [ ] **Step 3: Commit**

Run:
```bash
git add frontend/src/lib/useReducedMotion.ts
git commit -m "feat(frontend): add useReducedMotion hook"
```

---

## Task 4: `useBreakpoint` hook

Returns `true` while the viewport width is at or below a given threshold. Used to switch the book between landscape (desktop) and portrait (mobile) modes.

**Files:**
- Create: `frontend/src/lib/useBreakpoint.ts`

- [ ] **Step 1: Write the hook**

Create `frontend/src/lib/useBreakpoint.ts`:

```ts
import { useEffect, useState } from 'react'

/**
 * Returns true while the viewport width is at most `maxPx` (inclusive).
 * Updates on resize. SSR-safe (defaults to false).
 */
export function useBreakpoint(maxPx: number): boolean {
  const [match, setMatch] = useState<boolean>(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return false
    return window.matchMedia(`(max-width: ${maxPx}px)`).matches
  })

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return
    const mql = window.matchMedia(`(max-width: ${maxPx}px)`)
    const handler = (e: MediaQueryListEvent) => setMatch(e.matches)
    mql.addEventListener('change', handler)
    return () => mql.removeEventListener('change', handler)
  }, [maxPx])

  return match
}
```

- [ ] **Step 2: Verify build**

Run:
```bash
cd frontend && node_modules/.bin/tsc --noEmit && npm run lint
```
Expected: both succeed.

- [ ] **Step 3: Commit**

Run:
```bash
git add frontend/src/lib/useBreakpoint.ts
git commit -m "feat(frontend): add useBreakpoint hook"
```

---

## Task 5: `useAutoAdvance` hook

Calls `onTick` every `intervalMs` while `enabled` is true. Resets the timer if `enabled` flips false→true. The consumer controls the "should we advance?" logic (in-view, paused, reduced-motion) by setting `enabled`.

**Files:**
- Create: `frontend/src/lib/useAutoAdvance.ts`

- [ ] **Step 1: Write the hook**

Create `frontend/src/lib/useAutoAdvance.ts`:

```ts
import { useEffect, useRef } from 'react'

interface Options {
  enabled: boolean
  intervalMs: number
  onTick: () => void
}

export function useAutoAdvance({ enabled, intervalMs, onTick }: Options): void {
  const tickRef = useRef(onTick)
  tickRef.current = onTick

  useEffect(() => {
    if (!enabled) return
    const id = window.setInterval(() => tickRef.current(), intervalMs)
    return () => window.clearInterval(id)
  }, [enabled, intervalMs])
}
```

Note: keeping `onTick` in a ref is intentional — it lets the consumer pass a fresh closure each render without resetting the interval.

- [ ] **Step 2: Verify build**

Run:
```bash
cd frontend && node_modules/.bin/tsc --noEmit && npm run lint
```
Expected: both succeed.

- [ ] **Step 3: Commit**

Run:
```bash
git add frontend/src/lib/useAutoAdvance.ts
git commit -m "feat(frontend): add useAutoAdvance hook"
```

---

## Task 6: `StoryPage` component (text/left page)

The text page of each spread. Renders step number, label, title, and description.

**Files:**
- Create: `frontend/src/components/home/StoryPage.tsx`

- [ ] **Step 1: Write the component**

Create `frontend/src/components/home/StoryPage.tsx`:

```tsx
import { forwardRef } from 'react'
import styles from './StoryBook.module.css'

export interface StoryPageProps {
  num: string         // 'I' | 'II' | 'III' from locale
  stepLabel: string   // e.g. 'Step one'
  title: string
  desc: string
  pageId: string      // for aria-labelledby (e.g. 'storybook-step-1')
}

export const StoryPage = forwardRef<HTMLDivElement, StoryPageProps>(
  function StoryPage({ num, stepLabel, title, desc, pageId }, ref) {
    return (
      <div ref={ref} className={`${styles.page} ${styles.pageStory}`}>
        <div className={styles.pageInner}>
          <div className={styles.stepLabel}>{stepLabel}</div>
          <h3 id={pageId} className={styles.stepTitle}>{title}</h3>
          <p className={styles.stepDesc}>{desc}</p>
        </div>
        <div className={styles.pageNumber} aria-hidden="true">{num}</div>
      </div>
    )
  }
)
```

`react-pageflip` requires page children to either be plain DOM elements or `forwardRef` components — that's why this is a forwardRef.

- [ ] **Step 2: Verify build**

Note: `tsc` will fail at this step because `StoryBook.module.css` doesn't exist yet. That's fine — we'll create the CSS in Task 8 and re-verify then. Skip the build check until Task 8.

- [ ] **Step 3: Commit**

Run:
```bash
git add frontend/src/components/home/StoryPage.tsx
git commit -m "feat(frontend): add StoryPage component for storybook left page"
```

---

## Task 7: `IllustrationPage` component (image/right page)

The image page of each spread. Loads a step illustration (theme-aware), shows a parchment skeleton until the image loads.

**Files:**
- Create: `frontend/src/components/home/IllustrationPage.tsx`

- [ ] **Step 1: Write the component**

Create `frontend/src/components/home/IllustrationPage.tsx`:

```tsx
import { forwardRef, useEffect, useState } from 'react'
import { useTheme } from '../../lib/ThemeContext'
import styles from './StoryBook.module.css'

export interface IllustrationPageProps {
  step: 1 | 2 | 3
}

export const IllustrationPage = forwardRef<HTMLDivElement, IllustrationPageProps>(
  function IllustrationPage({ step }, ref) {
    const { theme } = useTheme()
    const src = `/illustrations/how-step${step}-${theme}.png`
    const [loaded, setLoaded] = useState(false)

    // Reset loading state when src changes (theme toggle).
    useEffect(() => {
      setLoaded(false)
    }, [src])

    return (
      <div ref={ref} className={`${styles.page} ${styles.pageIllust}`}>
        <img
          src={src}
          alt=""
          className={`${styles.illustImg} ${loaded ? styles.illustImgLoaded : ''}`}
          onLoad={() => setLoaded(true)}
          loading="eager"
          decoding="async"
        />
      </div>
    )
  }
)
```

`alt=""` is intentional — the story content on the paired text page is the screen-reader source of truth, so the image is decorative (per spec a11y section).

- [ ] **Step 2: Skip build check (waits for CSS in Task 8)**

- [ ] **Step 3: Commit**

Run:
```bash
git add frontend/src/components/home/IllustrationPage.tsx
git commit -m "feat(frontend): add IllustrationPage component for storybook right page"
```

---

## Task 8: `StoryBook.module.css`

All book chrome: page surfaces, parchment colors, controls. The page-curl animation itself comes from `react-pageflip`; this CSS only styles the page contents and the surrounding chrome.

**Files:**
- Create: `frontend/src/components/home/StoryBook.module.css`

- [ ] **Step 1: Write the CSS**

Create `frontend/src/components/home/StoryBook.module.css`:

```css
.wrap {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.bookFrame {
  position: relative;
  display: flex;
  justify-content: center;
}

/* Each react-pageflip "page" gets these styles via the className on our
   forwardRef components. The library wraps each in its own absolutely-positioned
   layer; we own the inside. */
.page {
  background: var(--color-surface);
  border: 1px solid var(--color-surface-deep);
  box-sizing: border-box;
  overflow: hidden;
}
[data-theme="dark"] .page {
  background: var(--color-surface-2);
  border-color: var(--color-surface-deep);
}

.pageStory {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 40px 36px;
  position: relative;
}

.pageInner {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stepLabel {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--color-text-faint);
}

.stepTitle {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 600;
  margin: 0;
  color: var(--color-text);
}

.stepDesc {
  font-size: 15px;
  color: var(--color-text-muted);
  line-height: 1.7;
  margin: 0;
}

.pageNumber {
  position: absolute;
  bottom: 18px;
  left: 36px;
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 28px;
  color: var(--color-text-faint);
  opacity: 0.6;
  letter-spacing: 0.08em;
}

.pageIllust {
  padding: 0;
  background: var(--color-surface-2);
}

.illustImg {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 250ms ease;
}
.illustImgLoaded {
  opacity: 1;
}

/* Controls below the book */
.controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.arrow {
  background: var(--color-surface);
  border: 1px solid var(--color-surface-deep);
  color: var(--color-text);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 200ms ease, transform 150ms ease;
  font-size: 18px;
  line-height: 1;
}
.arrow:hover { background: var(--color-surface-deep); transform: scale(1.05); }
.arrow:focus-visible { outline: 2px solid var(--color-magic); outline-offset: 2px; }
.arrow:disabled { opacity: 0.4; cursor: default; transform: none; }

.dots {
  display: flex;
  gap: 8px;
}

.dot {
  background: transparent;
  border: 1px solid var(--color-text-faint);
  width: 10px;
  height: 10px;
  border-radius: 50%;
  padding: 0;
  cursor: pointer;
  transition: background 200ms ease, transform 150ms ease;
}
.dot:hover { transform: scale(1.15); }
.dot:focus-visible { outline: 2px solid var(--color-magic); outline-offset: 2px; }
.dotActive {
  background: var(--color-gold);
  border-color: var(--color-gold);
}

/* Visually-hidden live region for SR announcements */
.srOnly {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0,0,0,0);
  white-space: nowrap;
  border: 0;
}

@media (prefers-reduced-motion: reduce) {
  .arrow, .dot, .illustImg { transition: none; }
}
```

- [ ] **Step 2: Verify build**

Run:
```bash
cd frontend && node_modules/.bin/tsc --noEmit && npm run lint
```
Expected: both succeed (Tasks 6 and 7's components now resolve their CSS module imports).

- [ ] **Step 3: Commit**

Run:
```bash
git add frontend/src/components/home/StoryBook.module.css
git commit -m "style(frontend): add StoryBook CSS module"
```

---

## Task 9: `StoryBookFallback` component + `StoryBookErrorBoundary`

The fallback has two roles: shown by `<Suspense>` while the lazy chunk is loading, and shown by an `ErrorBoundary` if the chunk fails to load entirely. Plain `<Suspense>` does not catch errors — React 19 still requires a class-component error boundary.

**Files:**
- Create: `frontend/src/components/home/StoryBookFallback.tsx`
- Create: `frontend/src/components/home/StoryBookErrorBoundary.tsx`

- [ ] **Step 1: Write `StoryBookFallback.tsx`**

Create `frontend/src/components/home/StoryBookFallback.tsx`:

```tsx
import { useLocale } from '../../lib/LocaleContext'
import styles from './StoryBook.module.css'

export function StoryBookFallback() {
  const { t } = useLocale()
  return (
    <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 24, maxWidth: 560 }}>
      {t.howItWorks.steps.map((step, i) => (
        <li key={i} style={{ display: 'flex', gap: 16 }}>
          <div className={styles.pageNumber} style={{ position: 'static', opacity: 0.7 }}>{step.num}</div>
          <div>
            <div className={styles.stepLabel}>{step.stepLabel}</div>
            <h3 className={styles.stepTitle}>{step.title}</h3>
            <p className={styles.stepDesc}>{step.desc}</p>
          </div>
        </li>
      ))}
    </ul>
  )
}
```

Inline styles are used here so the fallback doesn't need its own CSS module — it's a degradation path that shouldn't accumulate complexity.

- [ ] **Step 2: Write `StoryBookErrorBoundary.tsx`**

Create `frontend/src/components/home/StoryBookErrorBoundary.tsx`:

```tsx
import { Component, type ReactNode } from 'react'

interface Props {
  fallback: ReactNode
  children: ReactNode
}

interface State {
  hasError: boolean
}

export class StoryBookErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: unknown) {
    // eslint-disable-next-line no-console
    console.warn('[StoryBook] failed to render, falling back to text list:', error)
  }

  render() {
    if (this.state.hasError) return this.props.fallback
    return this.props.children
  }
}
```

Single-purpose error boundary scoped to the storybook only — keeps the rest of the page healthy if `react-pageflip` fails.

- [ ] **Step 3: Verify build**

Run:
```bash
cd frontend && node_modules/.bin/tsc --noEmit && npm run lint
```
Expected: both succeed.

- [ ] **Step 4: Commit**

Run:
```bash
git add frontend/src/components/home/StoryBookFallback.tsx frontend/src/components/home/StoryBookErrorBoundary.tsx
git commit -m "feat(frontend): add StoryBookFallback and ErrorBoundary"
```

---

## Task 10: `StoryBook` component

The orchestrator. Owns page state, auto-advance, in-view detection, reduced motion, prev/next/jump handlers, arrows, dots, keyboard, and the live region. Lazily imports `react-pageflip`.

**Files:**
- Create: `frontend/src/components/home/StoryBook.tsx`

- [ ] **Step 1: Write the component**

Create `frontend/src/components/home/StoryBook.tsx`:

```tsx
import { lazy, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocale } from '../../lib/LocaleContext'
import { useTheme } from '../../lib/ThemeContext'
import { useReducedMotion } from '../../lib/useReducedMotion'
import { useBreakpoint } from '../../lib/useBreakpoint'
import { useAutoAdvance } from '../../lib/useAutoAdvance'
import { StoryPage } from './StoryPage'
import { IllustrationPage } from './IllustrationPage'
import styles from './StoryBook.module.css'

// react-pageflip is browser-only and ~50 KB. Lazy-load it.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const HTMLFlipBook = lazy(async () => {
  const mod = await import('react-pageflip')
  return { default: mod.default as any }
})

// react-pageflip's ref handle exposes a pageFlip() accessor that returns
// the underlying StPageFlip instance. We only need a few methods — type
// them narrowly rather than depending on the library's internal types.
interface PageFlipApi {
  flipNext: (corner?: 'top' | 'bottom') => void
  flipPrev: (corner?: 'top' | 'bottom') => void
  flip: (pageNum: number, corner?: 'top' | 'bottom') => void
  getCurrentPageIndex: () => number
}
interface FlipBookHandle {
  pageFlip: () => PageFlipApi
}

// react-pageflip onFlip event shape.
interface FlipEvent {
  data: number
  // object: PageFlip — we don't use it
}

const AUTO_ADVANCE_MS = 6000
const PORTRAIT_BREAKPOINT_PX = 860

export function StoryBook() {
  const { t } = useLocale()
  const { theme } = useTheme()
  const reducedMotion = useReducedMotion()
  const isPortrait = useBreakpoint(PORTRAIT_BREAKPOINT_PX)

  // Preload all 3 step illustrations for the current theme on mount and on
  // theme change. Same trick as IllustrationCarousel — `new Image(); .src = …`
  // pulls the file into the browser cache so subsequent <img> mounts paint
  // instantly when the page curls.
  useEffect(() => {
    for (let step = 1; step <= 3; step++) {
      const img = new Image()
      img.src = `/illustrations/how-step${step}-${theme}.png`
    }
  }, [theme])

  const sectionRef = useRef<HTMLDivElement>(null)
  const bookRef = useRef<FlipBookHandle | null>(null)

  const [logicalStep, setLogicalStep] = useState(0)   // 0..2 — which step is showing
  const [inView, setInView] = useState(false)
  const [interacted, setInteracted] = useState(false)
  const [tabHidden, setTabHidden] = useState(false)

  const steps = t.howItWorks.steps

  // Build the page list. Desktop: 3 spreads = 6 page nodes (story + illust × 3).
  // Portrait: same 6 nodes but rendered as single pages by usePortrait=true.
  const pages = useMemo(() => {
    return steps.flatMap((step, i) => {
      const stepNum = (i + 1) as 1 | 2 | 3
      const pageId = `storybook-step-${stepNum}`
      return [
        <StoryPage
          key={`story-${stepNum}`}
          num={step.num}
          stepLabel={step.stepLabel}
          title={step.title}
          desc={step.desc}
          pageId={pageId}
        />,
        <IllustrationPage key={`illust-${stepNum}`} step={stepNum} />,
      ]
    })
  }, [steps])

  // IntersectionObserver — pause auto-advance when the section leaves the viewport.
  useEffect(() => {
    const el = sectionRef.current
    if (!el) return
    const obs = new IntersectionObserver(
      ([entry]) => setInView(entry.isIntersecting && entry.intersectionRatio >= 0.5),
      { threshold: [0, 0.5, 1] }
    )
    obs.observe(el)
    return () => obs.disconnect()
  }, [])

  // Pause auto-advance while the tab is hidden.
  useEffect(() => {
    const handler = () => setTabHidden(document.hidden)
    document.addEventListener('visibilitychange', handler)
    return () => document.removeEventListener('visibilitychange', handler)
  }, [])

  const autoEnabled = inView && !interacted && !reducedMotion && !tabHidden

  useAutoAdvance({
    enabled: autoEnabled,
    intervalMs: AUTO_ADVANCE_MS,
    onTick: () => {
      const api = bookRef.current?.pageFlip()
      if (!api) return
      const current = api.getCurrentPageIndex()
      // 6 page nodes total; loop back to 0 after the last page.
      const next = (current + (isPortrait ? 1 : 2)) % pages.length
      api.flip(next)
    },
  })

  const goPrev = useCallback(() => {
    setInteracted(true)
    bookRef.current?.pageFlip().flipPrev()
  }, [])

  const goNext = useCallback(() => {
    setInteracted(true)
    bookRef.current?.pageFlip().flipNext()
  }, [])

  const goToStep = useCallback((stepIdx: number) => {
    setInteracted(true)
    // 2 page nodes per step in landscape, 2 page nodes per step in portrait too,
    // but portrait shows one at a time — jump to the story page of that step.
    const target = stepIdx * 2
    bookRef.current?.pageFlip().flip(target)
  }, [])

  // Track logical step from page-flip events.
  const onFlip = useCallback((e: FlipEvent) => {
    const idx = e.data
    // Story pages live at indices 0, 2, 4. Map page→logical step.
    setLogicalStep(Math.floor(idx / 2))
  }, [])

  // Keyboard nav within the book region.
  const onKeyDown = useCallback((e: React.KeyboardEvent<HTMLDivElement>) => {
    if (e.key === 'ArrowLeft')  { e.preventDefault(); goPrev() }
    else if (e.key === 'ArrowRight') { e.preventDefault(); goNext() }
    else if (e.key === 'Home')       { e.preventDefault(); goToStep(0) }
    else if (e.key === 'End')        { e.preventDefault(); goToStep(steps.length - 1) }
  }, [goPrev, goNext, goToStep, steps.length])

  // Book sizing. Desktop: ~360x460 per page (so spread is ~720 wide).
  // Portrait: same per-page dimensions, library handles single-page layout.
  const pageWidth = 360
  const pageHeight = 460

  return (
    <div
      ref={sectionRef}
      className={styles.wrap}
      role="region"
      aria-roledescription="storybook"
      aria-label={t.howItWorks.title}
      onKeyDown={onKeyDown}
    >
      <div className={styles.bookFrame}>
        {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
        <HTMLFlipBook
          ref={bookRef as any}
          width={pageWidth}
          height={pageHeight}
          size="fixed"
          minWidth={280}
          maxWidth={420}
          minHeight={360}
          maxHeight={560}
          usePortrait={isPortrait}
          flippingTime={reducedMotion ? 0 : 800}
          drawShadow={!reducedMotion}
          useMouseEvents={true}
          mobileScrollSupport={true}
          showCover={false}
          startPage={0}
          maxShadowOpacity={0.5}
          clickEventForward={false}
          disableFlipByClick={false}
          onFlip={onFlip}
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          {...({} as any)}
        >
          {pages}
        </HTMLFlipBook>
      </div>

      <div className={styles.controls} aria-hidden={false}>
        <button
          type="button"
          className={styles.arrow}
          onClick={goPrev}
          aria-label={t.howItWorks.prevAria}
        >‹</button>

        <div className={styles.dots} role="tablist">
          {steps.map((_, i) => (
            <button
              key={i}
              type="button"
              className={`${styles.dot} ${logicalStep === i ? styles.dotActive : ''}`}
              onClick={() => goToStep(i)}
              aria-label={t.howItWorks.dotAria.replace('{n}', String(i + 1))}
              aria-pressed={logicalStep === i}
            />
          ))}
        </div>

        <button
          type="button"
          className={styles.arrow}
          onClick={goNext}
          aria-label={t.howItWorks.nextAria}
        >›</button>
      </div>

      <div className={styles.srOnly} role="status" aria-live="polite">
        {t.howItWorks.announce
          .replace('{n}', String(logicalStep + 1))
          .replace('{total}', String(steps.length))
          .replace('{title}', steps[logicalStep]?.title ?? '')}
      </div>
    </div>
  )
}
```

Notes for the engineer reading this:
- The `as any` casts on `HTMLFlipBook` and the ref are because `react-pageflip`'s exported types are loose (component is typed as `ComponentType<IProps>` with a generic `forwardRef` that doesn't expose its handle type cleanly). After install, check `node_modules/react-pageflip/build/HtmlFlipBook.d.ts`; if it now exports a `RefHandle` type, use it and drop the casts.
- New locale keys are referenced (`prevAria`, `nextAria`, `dotAria`, `announce`). Task 11 adds them.
- Mobile portrait mode: `usePortrait={true}` switches the library to single-page rendering. The 6 page nodes become 6 sequential pages. The `flip()` math (jump by `+2` desktop, `+1` portrait) keeps "next step" consistent in both modes.

- [ ] **Step 2: Skip build check (waits for locale keys in Task 11)**

`tsc` will fail at this step on the missing locale keys (`prevAria`, `nextAria`, etc.). That's fine — Task 11 adds them and we re-verify.

- [ ] **Step 3: Commit**

Run:
```bash
git add frontend/src/components/home/StoryBook.tsx
git commit -m "feat(frontend): add StoryBook component (text+illust pages, controls, a11y)"
```

---

## Task 11: Add new locale keys

Add the four new strings the StoryBook references: `prevAria`, `nextAria`, `dotAria`, `announce`. Both English and Ukrainian.

**Locale type note:** `frontend/src/locales/uk.ts` ends with `export type Locale = typeof uk`. The Locale type is **inferred from `uk.ts`** — so update `uk.ts` first; `en.ts` must structurally match. There is no separate type file to edit (the locale shape is not in `lib/types.ts`).

**Files:**
- Modify: `frontend/src/locales/uk.ts`
- Modify: `frontend/src/locales/en.ts`

- [ ] **Step 1: Add keys to `uk.ts` (the type source)**

Open `frontend/src/locales/uk.ts`. Inside the `howItWorks` block, after the `steps` array, add:

```ts
    prevAria: 'Попередній крок',
    nextAria: 'Наступний крок',
    dotAria: 'Перейти до кроку {n}',
    announce: 'Крок {n} з {total}: {title}',
```

- [ ] **Step 2: Add matching keys to `en.ts`**

Open `frontend/src/locales/en.ts`. Inside the `howItWorks` block, after the `steps` array, add:

```ts
    prevAria: 'Previous step',
    nextAria: 'Next step',
    dotAria: 'Go to step {n}',
    announce: 'Step {n} of {total}: {title}',
```

- [ ] **Step 3: Verify build**

Run:
```bash
cd frontend && node_modules/.bin/tsc --noEmit && npm run lint
```
Expected: both succeed. The `StoryBook.tsx` references resolve, and `en.ts` structurally matches the `Locale` type derived from `uk.ts`.

- [ ] **Step 4: Commit**

Run:
```bash
git add frontend/src/locales/uk.ts frontend/src/locales/en.ts
git commit -m "i18n(frontend): add storybook control labels (prev/next/dot/announce)"
```

---

## Task 12: Wire StoryBook into HowItWorks

Replace the existing 2-column layout with the new component, drop the now-unused CSS, drop the `IllustrationCarousel` import.

**Files:**
- Modify: `frontend/src/components/home/HowItWorks.tsx` (full rewrite of the component body)
- Modify: `frontend/src/components/home/HowItWorks.module.css` (delete unused rules)

- [ ] **Step 1: Rewrite `HowItWorks.tsx`**

Open `frontend/src/components/home/HowItWorks.tsx` and replace its entire contents with:

```tsx
import { Suspense } from 'react'
import { useReveal } from '../../lib/useReveal'
import { useLocale } from '../../lib/LocaleContext'
import { SectionParticles } from './SectionParticles'
import { StoryBook } from './StoryBook'
import { StoryBookFallback } from './StoryBookFallback'
import { StoryBookErrorBoundary } from './StoryBookErrorBoundary'
import styles from './HowItWorks.module.css'

export function HowItWorks() {
  const { t } = useLocale()
  const { ref: headRef, visible: headVisible } = useReveal()

  return (
    <section className={styles.section} id="how">
      <SectionParticles />
      <div className={styles.bgIllust} aria-hidden="true">
        <svg viewBox="0 0 300 400" fill="none" xmlns="http://www.w3.org/2000/svg" width="100%" height="100%">
          <circle cx="200" cy="80" r="60" fill="url(#howMG)" opacity="0.4"/>
          <path d="M215 55 A35 35 0 1 0 215 105 A25 25 0 1 1 215 55Z" fill="#EDD9A3" opacity="0.5"/>
          <path d="M0 120 C40 110 60 140 100 125 C120 118 130 130 150 120" stroke="#6B4C3B" strokeWidth="1.2" fill="none" opacity="0.25"/>
          <path d="M100 125 C105 105 115 95 125 80" stroke="#6B4C3B" strokeWidth="0.8" fill="none" opacity="0.2"/>
          <ellipse cx="125" cy="78" rx="5" ry="10" transform="rotate(-20 125 78)" fill="#166534" opacity="0.15"/>
          <path d="M20 400 C25 350 35 300 30 250 C28 220 20 180 0 150" stroke="#6B4C3B" strokeWidth="2" fill="none" opacity="0.15"/>
          <circle cx="80" cy="200" r="3" fill="#F59E0B" opacity="0.5">
            <animate attributeName="opacity" values="0.5;0.15;0.5" dur="3s" repeatCount="indefinite"/>
          </circle>
          <circle cx="180" cy="260" r="2.5" fill="#EDD9A3" opacity="0.45">
            <animate attributeName="opacity" values="0.45;0.1;0.45" dur="4s" repeatCount="indefinite"/>
          </circle>
          <path d="M250 40L251 36L255 38L251 35L250 31L249 35L245 33L249 36Z" fill="#C4B5FD" opacity="0.5">
            <animate attributeName="opacity" values="0.5;0.2;0.5" dur="4s" repeatCount="indefinite"/>
          </path>
          <defs>
            <radialGradient id="howMG" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0%" stopColor="#EDD9A3" stopOpacity="0.35"/>
              <stop offset="100%" stopColor="#EDD9A3" stopOpacity="0"/>
            </radialGradient>
          </defs>
        </svg>
      </div>
      <div className={styles.inner}>
        <div ref={headRef} className={`reveal ${headVisible ? 'visible' : ''}`}>
          <div className={styles.label}>{t.howItWorks.label}</div>
          <div className={styles.title}>{t.howItWorks.title}</div>
        </div>

        <div className={styles.bookSlot}>
          <StoryBookErrorBoundary fallback={<StoryBookFallback />}>
            <Suspense fallback={<StoryBookFallback />}>
              <StoryBook />
            </Suspense>
          </StoryBookErrorBoundary>
        </div>
      </div>
    </section>
  )
}
```

What was deleted: the `StepItem` sub-component, the `STEP_REVEAL_CLASSES` constant, the `IllustrationCarousel` import and usage, the `useRef`/`useState`/`useEffect`/`IntersectionObserver` plumbing for per-step reveal, the second `useReveal` call for the illustration, and the entire 2-column `.layout` grid.

What was kept: the section wrapper, particles, decorative `bgIllust` SVG, the header (label + title), the first `useReveal` for the heading.

- [ ] **Step 2: Trim `HowItWorks.module.css`**

Open `frontend/src/components/home/HowItWorks.module.css` and replace its entire contents with:

```css
.section {
  padding: 100px 48px;
  background: var(--color-surface);
  transition: background 0.5s;
  position: relative;
  overflow: hidden;
}

.inner {
  max-width: 1100px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.label {
  font-weight: 600;
  font-size: 12px;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--color-magic);
  margin-bottom: 12px;
}

.title {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: clamp(28px, 3.5vw, 42px);
  line-height: 1.25;
  margin-bottom: 48px;
}

.bookSlot {
  display: flex;
  justify-content: center;
}

.bgIllust {
  position: absolute;
  right: -60px;
  top: 40px;
  width: 400px;
  height: 500px;
  pointer-events: none;
  z-index: 0;
  opacity: 0.07;
}
[data-theme="dark"] .bgIllust { opacity: 0.05; }

@media (max-width: 860px) {
  .section { padding: 80px 20px; }
}
```

What was deleted: `.layout`, `.steps`, `.step`, `.stepLine`, `.stepNumWrap`, `.stepNum`, `.stepContent`, `.stepLabel`, `.stepTitle`, `.stepDesc`, `.illustWrap`, `.illustQuill`, the `.illustWrap { display: none }` rule in the mobile media query.

What was kept: `.section`, `.inner`, `.label`, `.title`, `.bgIllust` (and its dark-mode opacity tweak), the mobile section padding override.

What was added: `.bookSlot` to center the book.

- [ ] **Step 3: Verify build and lint**

Run:
```bash
cd frontend && node_modules/.bin/tsc --noEmit && npm run lint
```
Expected: both succeed. If `eslint` complains about unused imports, remove them.

- [ ] **Step 4: Commit**

Run:
```bash
git add frontend/src/components/home/HowItWorks.tsx frontend/src/components/home/HowItWorks.module.css
git commit -m "refactor(frontend): replace HowItWorks step list with StoryBook"
```

---

## Task 13: Manual browser verification

The frontend has no test framework, so this is the canonical verification step. Run through each item; if any fails, fix before declaring the plan done.

**Setup:**
- [ ] Start the dev server: `cd frontend && npm run dev`. Open `http://localhost:5173/`. (If the backend isn't running, the home page should still render — only the API-bound flows fail.)
- [ ] Open DevTools, switch to a desktop-width viewport (≥ 1024px).

**Auto-advance:**
- [ ] **Scroll the `#how` section into view.** The book opens to step 1 (left page = "Tell us about your child" text, right page = step-1 illustration).
- [ ] **Wait ~6 s.** The page curls and turns to step 2.
- [ ] **Wait another ~6 s.** Curl turns to step 3.
- [ ] **Wait another ~6 s.** Loops back to step 1.
- [ ] **Scroll the section out of view, wait 10 s, scroll back.** Auto-advance resumes from where it was; doesn't try to "catch up".
- [ ] **Switch to another browser tab for 15 s, switch back.** Auto-advance pauses while hidden, resumes on return.

**Manual controls:**
- [ ] Click `›` arrow. Page advances. Auto-advance is now permanently paused — wait 10 s and confirm the page does not turn on its own.
- [ ] Reload the page, scroll back to `#how`. Click a dot. Auto-advance pauses. Wait 10 s — no auto-turn.
- [ ] Click each of the 3 dots in turn. Active dot is gold (`var(--color-gold)`); the other two are outlined.
- [ ] Reload, focus the book region (Tab to it), press `→` then `←` then `Home` then `End`. All four navigations work and pause auto-advance.

**Animation quality:**
- [ ] The curl looks like a real page lifting from the spine — not a flat hinge. Shadow visible during the lift.
- [ ] Toggle dark mode (theme switcher). Curl plays smoothly; pages switch to dark surface color; image swaps to `-dark` variant cleanly.
- [ ] No flicker on the image when it first paints (skeleton parchment shows, then the image fades in over ~250 ms).

**Mobile / portrait:**
- [ ] Resize the viewport to 400 px wide (or use DevTools device-emulation iPhone). Book switches to single-page mode automatically: image page is full width, then story page is full width on next turn.
- [ ] Auto-advance, arrows, dots, keyboard all still work in portrait.
- [ ] Resize back to desktop width without reloading. The book reinitializes into spread mode; no console errors.

**Reduced motion:**
- [ ] In DevTools → Rendering → Emulate CSS media feature `prefers-reduced-motion: reduce`.
- [ ] Reload. Section opens to step 1. Wait 10 s — no auto-advance.
- [ ] Click `›`. Page changes instantly with no curl, no shadow.
- [ ] Click a dot. Same — instant.
- [ ] Disable the emulation; reload; auto-advance and animation return.

**Locale:**
- [ ] Switch language UA ↔ EN. The current page stays put; text re-renders in the new locale immediately.

**Lazy-load fallback:**
- [ ] DevTools → Network → set throttling to "Offline".
- [ ] In DevTools Application tab, clear site data and reload. The lazy chunk fails to fetch; `StoryBookFallback` should render — three stacked text steps.
- [ ] Re-enable network and reload to restore normal behavior.

**Accessibility:**
- [ ] Run Lighthouse → Accessibility audit on `/`. Score should be ≥ the pre-change score. Note both numbers in the commit message of Task 14.
- [ ] (If you have a screen reader available) — VoiceOver / NVDA: navigate to the book, advance one page, confirm a "Step 2 of 3: Choose the magic" announcement is read aloud.
- [ ] Tab through the section. Focus visibly outlines the arrows and dots (purple ring per `:focus-visible`). The book region is reachable but doesn't trap focus.

**Console hygiene:**
- [ ] DevTools console is clean across all of the above (no warnings, no errors).

If everything passes, you're done. If something fails, file targeted follow-up tasks rather than patching all at once.

- [ ] **Step 1: Run the manual checklist above**

- [ ] **Step 2: Capture before/after Lighthouse a11y score**

Note both scores in the next commit message.

- [ ] **Step 3: Final verification commit (no code change, but confirms QA pass)**

Run:
```bash
cd frontend && node_modules/.bin/tsc --noEmit && npm run lint
```
Expected: both succeed.

If you made any small fixups during QA, group them in:
```bash
git add frontend/...
git commit -m "fix(frontend): storybook QA fixes — <one-line summary>

Lighthouse a11y before: <N>, after: <M>."
```
If no fixups were needed, skip this commit.

---

## Out of scope (do not implement here)

- Removing the now-unused `frontend/public/illustrations/how-{age}-{theme}.png` files. File a follow-up cleanup commit after this plan ships.
- Sound effects on page turn.
- Drag-velocity-based curl physics (StPageFlip defaults are good enough).
- A/B testing the auto-advance interval.

## Risks (recap from spec)

- **Bundle size.** ~50 KB lazy chunk for `react-pageflip`. Verify with `npm run build` output.
- **React 19 peer-dep conflict.** Handled in Task 1.
- **Library quality.** `react-pageflip` is the established option. If blocking bugs surface during Task 13, the fallback is to hand-roll the curl with CSS 3D transforms — significantly more work, deferred unless needed.
