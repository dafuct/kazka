# Preview-section storybook + how-section revert — implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the `react-pageflip` storybook from `#how` (which goes back to its pre-storybook 3-step text design) to `#preview` (which becomes a 2-spread book with manual navigation, preserved typewriter, and 4 new theme-aware illustrations).

**Architecture:** Two coordinated changes. (1) Restore `HowItWorks.tsx`/`.module.css` from commit `0b32b96`. (2) Generalize the existing `StoryBook`/`IllustrationPage` primitives to be content-agnostic, delete the now-unused `StoryPage` + `StoryBookFallback`, then rewrite `StoryPreview.tsx` to use `<StoryBook>` with 2 spreads composed inline. Locale keys for the controls (`prevAria`/`nextAria`/`dotAria`/`announce`) move from `howItWorks` to `storyPreview`, and `announce` is rewritten from "Step N of M: title" to "Page N of M".

**Tech Stack:** React 19, TypeScript 6, Vite 8, CSS Modules, `react-pageflip` 2.x (already installed).

**Reference spec:** `docs/superpowers/specs/2026-05-03-preview-storybook-design.md`

**Important constraints (from `CLAUDE.md` + lessons from the prior storybook plan):**
- TypeScript verification: `cd frontend && node_modules/.bin/tsc --noEmit` is the documented command, **but it's a no-op against the project's `tsconfig.json` stub (`"files": []`)**. The real type-check is `cd frontend && npm run build` (which runs `tsc -b && vite build`). **Every code task in this plan verifies with `npm run build`.** A passing `tsc --noEmit` does NOT mean code compiles.
- React 19, TS 6, Vite 8.
- All UI text via `useLocale()` `t` — no hardcoded strings.
- No test framework — verification = `npm run build` + `npm run lint` + manual browser check.
- Pre-existing baseline lint count: capture in pre-flight; tasks must not increase it.
- `react-pageflip` is already installed (commit `9d4af61`). Don't reinstall.

---

## File structure

**Created:**
- `frontend/public/illustrations/preview-page1-light.png` (asset task — user provides)
- `frontend/public/illustrations/preview-page1-dark.png` (asset task — user provides)
- `frontend/public/illustrations/preview-page2-light.png` (asset task — user provides)
- `frontend/public/illustrations/preview-page2-dark.png` (asset task — user provides)

**Modified (refactor — content-agnostic):**
- `frontend/src/components/home/StoryBook.tsx` — drops `useLocale`/`useTheme`/`StoryPage`/`useMemo`-of-steps; takes `pages`, `ariaLabel`, `autoAdvance`, `intervalMs`, `prevAria`, `nextAria`, `dotAria`, `announce`, `onUserInteract` as props; image-preload moves to caller.
- `frontend/src/components/home/IllustrationPage.tsx` — takes `src: string` instead of `step: 1|2|3`; adds optional `pageNum?: number`; drops `useTheme`.
- `frontend/src/components/home/StoryBook.module.css` — drop `.stepLabel`, `.stepTitle`, `.stepDesc` (no consumer after StoryPage delete). Add `.pageNumberRight` for right-page page-number positioning. Keep `.page`, `.pageStory`, `.pageInner`, `.pageNumber`, `.pageIllust`, `.illustImg`, `.illustImgLoaded`, `.controls`, `.arrow`, `.dot`, `.dotActive`, `.dots`, `.srOnly`, `.wrap`, `.bookFrame`, `.pageIllust::before`, the keyframes, the reduced-motion block, `.wrap:focus-visible`.

**Modified (revert):**
- `frontend/src/components/home/HowItWorks.tsx` — restore byte-for-byte from commit `0b32b96`.
- `frontend/src/components/home/HowItWorks.module.css` — restore byte-for-byte from commit `0b32b96`.

**Modified (full rewrite):**
- `frontend/src/components/home/StoryPreview.tsx` — use `<StoryBook>` with 2 spreads composed of inline `PreviewStoryPage` (text + dropcap + page number) and the refactored `IllustrationPage`. Owns the typewriter state and image preload. Inline `PreviewBookFallback` for Suspense + ErrorBoundary.
- `frontend/src/components/home/StoryPreview.module.css` — drop `.bookSpread`, `.bookLeft`, `.bookRight`, `.pageOpen`, `.bookVisible .bookRight`, `.illustHero`, `.bookSpread::after`. Keep section/inner/label/title/dropCap/storyText/cursor/pageNum/pageNumLeft/pageNumRight/tagline. Add `.bookSlot { display: flex; justify-content: center; }`.

**Modified (locale):**
- `frontend/src/locales/uk.ts` — move `prevAria`/`nextAria`/`dotAria`/`announce` from `howItWorks` to `storyPreview`; rewrite `announce`; add `text2`.
- `frontend/src/locales/en.ts` — same.

**Deleted:**
- `frontend/src/components/home/StoryPage.tsx` — was specific to step layout; preview's left page is a different shape (inline in `StoryPreview.tsx`).
- `frontend/src/components/home/StoryBookFallback.tsx` — was hardcoded to render the 3 how-it-works steps; preview gets its own inline fallback.

**Unchanged but referenced:**
- `frontend/src/components/home/StoryBookErrorBoundary.tsx` — generic error boundary, no changes.
- `frontend/src/lib/useReducedMotion.ts`, `useBreakpoint.ts`, `useAutoAdvance.ts` — unchanged.
- `frontend/src/components/illustrations/IllustrationCarousel.tsx` — unchanged; the reverted `HowItWorks` will reference it again.

---

## Pre-flight

- [ ] **P.1: Confirm clean working tree on `main`**

Run: `git status`
Expected: clean or only the pre-existing `frontend/src/components/home/Features.module.css` modification (which is unrelated to this work). Confirm we're on branch `main` at HEAD `c3bbed5` (the spec commit) or later.

- [ ] **P.2: Capture baseline build state**

Run from `/Users/makar/dev/kazka/frontend`:
```bash
node_modules/.bin/tsc --noEmit ; echo "tsc-noemit exit=$?"
npm run lint 2>&1 | tail -3
npm run build 2>&1 | tail -8
```
Note these baseline numbers — every code task must keep them at-or-below this baseline:
- `npm run lint` problem count: should be **8 errors / 8 warnings** (carried over from the storybook batch).
- `npm run build`: must exit 0. Lazy chunk size for `react-pageflip` should be ~44.84 kB raw / ~10.86 kB gzipped.

If baseline differs from these numbers, record the actual baseline in the plan-execution log — tasks compare to it, not to the doc.

---

## Task 1: Set up isolated worktree

Standard isolation per project convention. The previous storybook work used `.worktrees/how-storybook`; this one gets its own.

- [ ] **Step 1: Create worktree on a new feature branch**

Run from `/Users/makar/dev/kazka`:
```bash
git worktree add .worktrees/preview-storybook -b feature/preview-storybook
```
Expected: worktree created at `.worktrees/preview-storybook` on branch `feature/preview-storybook`.

- [ ] **Step 2: Install deps in the worktree**

Run:
```bash
cd .worktrees/preview-storybook/frontend && npm install 2>&1 | tail -3
```
Expected: clean install, no audit warnings (or whatever the baseline is).

- [ ] **Step 3: Verify baseline holds in the worktree**

Run:
```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend
node_modules/.bin/tsc --noEmit ; echo "tsc-noemit exit=$?"
npm run lint 2>&1 | tail -3
npm run build 2>&1 | tail -8
```
Expected: same numbers as P.2. If they differ, stop and investigate.

**No commit for this task.** It's worktree setup, not a code change.

---

## Task 2: Revert `HowItWorks.tsx` to its pre-storybook contents

Restore the original 3-step + sidebar carousel layout. The exact source is in git at `0b32b96:frontend/src/components/home/HowItWorks.tsx`.

**Files:**
- Modify: `frontend/src/components/home/HowItWorks.tsx`

- [ ] **Step 1: Replace the file with the pre-storybook contents**

In the worktree at `/Users/makar/dev/kazka/.worktrees/preview-storybook/frontend/src/components/home/HowItWorks.tsx`, replace the entire file contents with:

```tsx
import { useEffect, useRef, useState } from 'react'
import { useReveal } from '../../lib/useReveal'
import { useLocale } from '../../lib/LocaleContext'
import { SectionParticles } from './SectionParticles'
import styles from './HowItWorks.module.css'
import { IllustrationCarousel } from '../illustrations/IllustrationCarousel'

const STEP_REVEAL_CLASSES = ['revealLeft', 'revealRight', 'revealLeft']

interface StepItemProps {
  step: { num: string; stepLabel: string; title: string; desc: string }
  revealClass: string
  index: number
}

function StepItem({ step, revealClass, index }: StepItemProps) {
  const ref = useRef<HTMLDivElement>(null)
  const [visible, setVisible] = useState(false)
  const [numFlipped, setNumFlipped] = useState(false)
  const [lineDrawn, setLineDrawn] = useState(false)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    const obs = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true)
          setTimeout(() => setNumFlipped(true), 100)
          setTimeout(() => setLineDrawn(true), 400)
          obs.unobserve(el)
        }
      },
      { threshold: 0.15, rootMargin: '0px 0px -40px 0px' }
    )
    obs.observe(el)
    return () => obs.disconnect()
  }, [])

  return (
    <div
      ref={ref}
      className={`${styles.step} ${revealClass} ${visible ? 'visible' : ''}`}
      style={{ transitionDelay: `${index * 0.1}s` }}
    >
      <div className={styles.stepNumWrap}>
        <div className={`${styles.stepNum} ${numFlipped ? styles.flipped : ''}`}>
          {step.num}
        </div>
      </div>
      <div className={styles.stepContent}>
        <div className={styles.stepLabel}>{step.stepLabel}</div>
        <h3 className={styles.stepTitle}>{step.title}</h3>
        <p className={styles.stepDesc}>{step.desc}</p>
      </div>
      {index < STEP_REVEAL_CLASSES.length - 1 && (
        <div className={`${styles.stepLine} ${lineDrawn ? styles.drawn : ''}`} />
      )}
    </div>
  )
}

export function HowItWorks() {
  const { t } = useLocale()
  const { ref: headRef, visible: headVisible } = useReveal()
  const { ref: illustRef, visible: illustVisible } = useReveal()

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

        <div className={styles.layout}>
          <div className={styles.steps}>
            {t.howItWorks.steps.map((step, i) => (
              <StepItem key={i} step={step} revealClass={STEP_REVEAL_CLASSES[i]} index={i} />
            ))}
          </div>

          <div
            ref={illustRef}
            className={`${styles.illustWrap} reveal ${illustVisible ? 'visible' : ''}`}
          >
            <IllustrationCarousel section="how" />
          </div>
        </div>
      </div>
    </section>
  )
}
```

This is the verbatim contents of `0b32b96:frontend/src/components/home/HowItWorks.tsx`. No changes from the original.

- [ ] **Step 2: Verify the build is still green**

Note: `npm run build` will TEMPORARILY break here because the current locale shape has `t.howItWorks.prevAria/nextAria/dotAria/announce` fields that StoryBook still references in `StoryBook.tsx` (which still exists and is still being imported by — wait, no, it's NOT imported by anything yet in this branch since HowItWorks no longer imports it; let me re-check). Actually after this revert, no one references `StoryBook.tsx` — so the build should succeed.

Run:
```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend && npm run build 2>&1 | tail -8
```
Expected: exit 0. If it fails on something other than the StoryBook-related references, stop and investigate.

- [ ] **Step 3: Commit**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook
git add frontend/src/components/home/HowItWorks.tsx
git commit -m "refactor(frontend): revert HowItWorks to pre-storybook 3-step layout"
```

---

## Task 3: Revert `HowItWorks.module.css` to its pre-storybook contents

Companion to Task 2. The CSS rules for `.layout`, `.steps`, `.step`, `.stepLine`, `.stepNumWrap`, `.stepNum`, `.stepContent`, `.stepLabel`, `.stepTitle`, `.stepDesc`, `.illustWrap`, `.illustQuill` need to come back; the new `.bookSlot` rule goes away.

**Files:**
- Modify: `frontend/src/components/home/HowItWorks.module.css`

- [ ] **Step 1: Replace the file with the pre-storybook contents**

Replace the entire contents of `frontend/src/components/home/HowItWorks.module.css` with:

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

.layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 60px;
  align-items: center;
}

.steps {
  position: relative;
  display: flex;
  flex-direction: column;
}

.step {
  display: flex;
  gap: 24px;
  margin-bottom: 48px;
  position: relative;
}
.step:last-child { margin-bottom: 0; }

.stepLine {
  position: absolute;
  left: 28px;
  top: 72px;
  bottom: -48px;
  width: 0;
  border-left: 2px dashed var(--color-surface-deep);
  transition: border-color 0.6s;
}
.stepLine.drawn { border-left-color: var(--color-gold); }

.stepNumWrap {
  flex-shrink: 0;
  width: 56px;
  text-align: center;
  perspective: 600px;
}

.stepNum {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 42px;
  color: var(--color-surface-deep);
  line-height: 1;
  transform: rotateX(-90deg);
  opacity: 0;
  transition: all 0.5s cubic-bezier(0.34,1.56,0.64,1);
}
[data-theme="dark"] .stepNum { color: var(--color-surface-2); }
.stepNum.flipped { transform: rotateX(0); opacity: 1; }

.stepContent h3 {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 6px;
}

.stepLabel {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--color-text-faint);
  margin-bottom: 6px;
}

.stepTitle {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 6px;
}

.stepDesc {
  font-size: 15px;
  color: var(--color-text-muted);
  line-height: 1.7;
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

.illustWrap {
  aspect-ratio: 3/4;
  align-self: start;
  margin-top: -60px;
  border-radius: 16px;
  overflow: hidden;
}

.illustQuill {
  width: 100%;
  height: 100%;
  background:
    radial-gradient(ellipse at 60% 50%, rgba(217,119,6,0.3) 0%, transparent 50%),
    radial-gradient(ellipse at 30% 60%, rgba(124,58,237,0.2) 0%, transparent 45%),
    radial-gradient(ellipse at 70% 30%, rgba(237,217,163,0.3) 0%, transparent 40%),
    var(--color-surface-2);
  min-height: 300px;
}

@media (max-width: 860px) {
  .section { padding: 80px 20px; }
  .layout { grid-template-columns: 1fr; }
  .illustWrap { display: none; }
}
```

This is the verbatim contents of `0b32b96:frontend/src/components/home/HowItWorks.module.css`.

- [ ] **Step 2: Verify build**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend && npm run build 2>&1 | tail -8
```
Expected: exit 0. The reverted `HowItWorks.tsx` references `styles.layout`, `styles.steps`, `styles.illustWrap`, etc. — all now present. Lazy chunk for `react-pageflip` still exists because StoryBook is still in the tree.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/home/HowItWorks.module.css
git commit -m "refactor(frontend): revert HowItWorks CSS to pre-storybook layout"
```

---

## Task 4: Refactor `IllustrationPage` to take `src` instead of `step`

Make the component reusable across different illustration paths.

**Files:**
- Modify: `frontend/src/components/home/IllustrationPage.tsx`

- [ ] **Step 1: Replace the file**

Replace the entire contents of `frontend/src/components/home/IllustrationPage.tsx` with:

```tsx
import { forwardRef, useEffect, useState } from 'react'
import styles from './StoryBook.module.css'

export interface IllustrationPageProps {
  src: string
  /** Optional page number rendered in the bottom-right corner overlay. */
  pageNum?: number
}

export const IllustrationPage = forwardRef<HTMLDivElement, IllustrationPageProps>(
  function IllustrationPage({ src, pageNum }, ref) {
    const [loaded, setLoaded] = useState(false)

    // Reset loading state when src changes (theme toggle, etc.)
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
          onError={() => setLoaded(true)}
          loading="eager"
          decoding="async"
        />
        {pageNum !== undefined && (
          <div className={`${styles.pageNumber} ${styles.pageNumberRight}`} aria-hidden="true">
            {pageNum}
          </div>
        )}
      </div>
    )
  }
)
```

Changes vs. the old version:
- Removed `import { useTheme } from '../../lib/ThemeContext'` (caller computes the themed src now).
- Prop: `step: 1 | 2 | 3` → `src: string`.
- Removed the internal `src = '/illustrations/how-step${step}-${theme}.png'` construction.
- Added optional `pageNum?: number` prop with a corner-overlay rendering. The new `.pageNumberRight` CSS class is added in Task 8.

- [ ] **Step 2: Verify build**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend && npm run build 2>&1 | tail -10
```
Expected: build will FAIL with errors in `StoryBook.tsx` (which still passes `step={stepNum}` to `IllustrationPage` — that prop no longer exists). That is expected; Task 5 fixes StoryBook. Do NOT try to fix here.

If the failure is anything OTHER than `IllustrationPage` prop type errors in `StoryBook.tsx`, stop and investigate.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/home/IllustrationPage.tsx
git commit -m "refactor(frontend): IllustrationPage takes src prop instead of step"
```

---

## Task 5: Refactor `StoryBook` to be content-agnostic

Drop the hardcoded "3 steps from `t.howItWorks.steps`" composition. Caller now passes `pages`, `ariaLabel`, `autoAdvance`, control strings, and an optional `onUserInteract` callback.

**Files:**
- Modify: `frontend/src/components/home/StoryBook.tsx`

- [ ] **Step 1: Replace the file**

Replace the entire contents of `frontend/src/components/home/StoryBook.tsx` with:

```tsx
import {
  lazy,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ComponentType,
  type ReactNode,
  type KeyboardEvent as ReactKeyboardEvent,
} from 'react'
import { useReducedMotion } from '../../lib/useReducedMotion'
import { useBreakpoint } from '../../lib/useBreakpoint'
import { useAutoAdvance } from '../../lib/useAutoAdvance'
import styles from './StoryBook.module.css'

// react-pageflip is browser-only and ~50 KB. Lazy-load it.
const HTMLFlipBook = lazy(async () => {
  const mod = await import('react-pageflip')
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return { default: mod.default as ComponentType<any> }
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

interface FlipEvent {
  data: number
}

const DEFAULT_AUTO_ADVANCE_MS = 6000
const PORTRAIT_BREAKPOINT_PX = 860
const PAGE_WIDTH_PX = 360
const PAGE_HEIGHT_PX = 460

export interface StoryBookProps {
  /** Page nodes — must be an EVEN number; 2 nodes per spread (story + illust). */
  pages: ReactNode[]
  /** Region aria-label for screen readers. */
  ariaLabel: string
  /** When true, pages turn automatically every `intervalMs`. Default true. */
  autoAdvance?: boolean
  /** Auto-advance interval in milliseconds. Default 6000. Ignored when autoAdvance=false. */
  intervalMs?: number
  /** Aria-label for the previous-page arrow button. */
  prevAria: string
  /** Aria-label for the next-page arrow button. */
  nextAria: string
  /** Aria-label template for dot buttons; should contain the literal `{n}`. */
  dotAria: string
  /** Live-region announce template; should contain `{n}` and `{total}` literals. */
  announce: string
  /** Optional callback fired on the FIRST manual interaction (arrow/dot/keyboard). */
  onUserInteract?: () => void
}

export function StoryBook({
  pages,
  ariaLabel,
  autoAdvance = true,
  intervalMs = DEFAULT_AUTO_ADVANCE_MS,
  prevAria,
  nextAria,
  dotAria,
  announce,
  onUserInteract,
}: StoryBookProps) {
  const reducedMotion = useReducedMotion()
  const isPortrait = useBreakpoint(PORTRAIT_BREAKPOINT_PX)

  const sectionRef = useRef<HTMLDivElement>(null)
  const bookRef = useRef<FlipBookHandle | null>(null)

  // Spread count: 2 page nodes per spread.
  const spreadCount = Math.floor(pages.length / 2)

  const [logicalSpread, setLogicalSpread] = useState(0)  // 0..spreadCount-1
  const [inView, setInView] = useState(false)
  const [interacted, setInteracted] = useState(false)
  const [tabHidden, setTabHidden] = useState(false)

  // Keep a stable callback for the user-interaction notification (caller may
  // pass a fresh closure each render; we only fire on the FIRST interaction).
  const onUserInteractRef = useRef(onUserInteract)
  useEffect(() => { onUserInteractRef.current = onUserInteract })

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

  const autoEnabled = autoAdvance && inView && !interacted && !reducedMotion && !tabHidden

  useAutoAdvance({
    enabled: autoEnabled,
    intervalMs,
    onTick: () => {
      const api = bookRef.current?.pageFlip()
      if (!api) return
      const current = api.getCurrentPageIndex()
      // Step by 2 nodes in landscape (one spread), 1 node in portrait.
      const next = (current + (isPortrait ? 1 : 2)) % pages.length
      api.flip(next)
    },
  })

  const markInteracted = useCallback(() => {
    setInteracted((prev) => {
      if (!prev) onUserInteractRef.current?.()
      return true
    })
  }, [])

  const goPrev = useCallback(() => {
    markInteracted()
    bookRef.current?.pageFlip().flipPrev()
  }, [markInteracted])

  const goNext = useCallback(() => {
    markInteracted()
    bookRef.current?.pageFlip().flipNext()
  }, [markInteracted])

  const goToSpread = useCallback((spreadIdx: number) => {
    markInteracted()
    // 2 page nodes per spread; jump to the FIRST node of the target spread.
    const target = spreadIdx * 2
    bookRef.current?.pageFlip().flip(target)
  }, [markInteracted])

  // Track logical spread from page-flip events.
  const onFlip = useCallback((e: FlipEvent) => {
    const idx = e.data
    setLogicalSpread(Math.floor(idx / 2))
  }, [])

  const onKeyDown = useCallback((e: ReactKeyboardEvent<HTMLDivElement>) => {
    if (e.key === 'ArrowLeft')       { e.preventDefault(); goPrev() }
    else if (e.key === 'ArrowRight') { e.preventDefault(); goNext() }
    else if (e.key === 'Home')       { e.preventDefault(); goToSpread(0) }
    else if (e.key === 'End')        { e.preventDefault(); goToSpread(spreadCount - 1) }
  }, [goPrev, goNext, goToSpread, spreadCount])

  // Materialize dot indices once per pages-length change to avoid array-allocation noise.
  const dotIndices = useMemo(() => Array.from({ length: spreadCount }, (_, i) => i), [spreadCount])

  return (
    <div
      ref={sectionRef}
      className={styles.wrap}
      role="region"
      aria-roledescription="storybook"
      aria-label={ariaLabel}
      tabIndex={0}
      onKeyDown={onKeyDown}
    >
      <div className={styles.bookFrame}>
        <HTMLFlipBook
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          ref={bookRef as any}
          width={PAGE_WIDTH_PX}
          height={PAGE_HEIGHT_PX}
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
        >
          {pages}
        </HTMLFlipBook>
      </div>

      <div className={styles.controls}>
        <button
          type="button"
          className={styles.arrow}
          onClick={goPrev}
          aria-label={prevAria}
        >‹</button>

        <div className={styles.dots}>
          {dotIndices.map((i) => (
            <button
              key={i}
              type="button"
              className={`${styles.dot} ${logicalSpread === i ? styles.dotActive : ''}`}
              onClick={() => goToSpread(i)}
              aria-label={dotAria.replace('{n}', String(i + 1))}
              aria-pressed={logicalSpread === i}
            />
          ))}
        </div>

        <button
          type="button"
          className={styles.arrow}
          onClick={goNext}
          aria-label={nextAria}
        >›</button>
      </div>

      <div className={styles.srOnly} role="status" aria-live="polite">
        {announce
          .replace('{n}', String(logicalSpread + 1))
          .replace('{total}', String(spreadCount))}
      </div>
    </div>
  )
}
```

Key changes vs. the old version:
- Removed `useLocale`, `useTheme`, `StoryPage`, `IllustrationPage` (the last is now used by callers, not internally).
- Removed the image-preload effect (caller's responsibility now — different sections preload different files).
- `pages` is a `ReactNode[]` prop, not internally composed from locale.
- Renamed `logicalStep` → `logicalSpread` and `goToStep` → `goToSpread` (a "spread" is a more accurate term now that the book may not be steps).
- `autoAdvance` boolean prop (defaults `true`); `useAutoAdvance` is gated through `autoEnabled` which now folds in this prop.
- `intervalMs` prop (default `DEFAULT_AUTO_ADVANCE_MS = 6000`).
- `prevAria`, `nextAria`, `dotAria`, `announce` are individual string props passed by caller.
- `announce` template no longer includes `{title}` (callers can include it themselves if they want).
- `onUserInteract` optional callback fires once on first manual interaction (arrow/dot/keyboard). Stored in a ref to prevent the consumer's fresh closure from re-firing it.
- Page constants stay (`PAGE_WIDTH_PX`, `PAGE_HEIGHT_PX`, `PORTRAIT_BREAKPOINT_PX`).

- [ ] **Step 2: Verify build**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend && npm run build 2>&1 | tail -10
```
Expected: build will likely still FAIL because `StoryPage.tsx` still exists and now has a broken `IllustrationPage` import (Task 4 changed the prop). The Task 6 deletion fixes this. If the failure is anything OTHER than `StoryPage.tsx`/`StoryBookFallback.tsx` (which still references the old props), stop and investigate.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/home/StoryBook.tsx
git commit -m "refactor(frontend): generalize StoryBook to take pages + control strings as props"
```

---

## Task 6: Delete `StoryPage` and `StoryBookFallback`

Both were specific to the how-section. Preview composes its pages inline; preview's fallback is also inline.

**Files:**
- Delete: `frontend/src/components/home/StoryPage.tsx`
- Delete: `frontend/src/components/home/StoryBookFallback.tsx`

- [ ] **Step 1: Delete both files**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook
rm frontend/src/components/home/StoryPage.tsx frontend/src/components/home/StoryBookFallback.tsx
```

- [ ] **Step 2: Verify build**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend && npm run build 2>&1 | tail -10
```
Expected: build now SUCCEEDS, exit 0. No file imports either deleted module (HowItWorks no longer imports them after Task 2; StoryBook no longer imports StoryPage after Task 5; nothing imports StoryBookFallback). Lazy chunk for `react-pageflip` should appear.

If the build fails with `Cannot find module './StoryPage'` or `'./StoryBookFallback'`, that means the previous task didn't fully strip references. Trace the failure to its caller and fix.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/home/StoryPage.tsx frontend/src/components/home/StoryBookFallback.tsx
git commit -m "refactor(frontend): delete unused StoryPage and StoryBookFallback"
```

---

## Task 7: Move locale keys from `howItWorks` to `storyPreview` + add `text2`

Locale type source is `uk.ts` (`export type Locale = typeof uk`). Update `uk.ts` first; `en.ts` must structurally match.

**Files:**
- Modify: `frontend/src/locales/uk.ts`
- Modify: `frontend/src/locales/en.ts`

- [ ] **Step 1: Update `uk.ts`**

In `frontend/src/locales/uk.ts`:

(a) In the `howItWorks` block, remove these 4 lines:
```ts
    prevAria: 'Попередній крок',
    nextAria: 'Наступний крок',
    dotAria: 'Перейти до кроку {n}',
    announce: 'Крок {n} з {total}: {title}',
```

(b) In the `storyPreview` block, after the existing `text:` line, add 5 new lines:
```ts
    text2: 'Вона перейшла струмок місячного світла, де сріблясті рибки стрибали з каменю на камінь, і нарешті дісталась галявини, де сім світлячків сиділи колом, чекаючи на найменшу зірку у світі.',
    prevAria: 'Попередня сторінка',
    nextAria: 'Наступна сторінка',
    dotAria: 'Перейти до сторінки {n}',
    announce: 'Сторінка {n} з {total}',
```

(Note: `prevAria`/`nextAria`/`dotAria` say "сторінка" — page — instead of "крок" — step. The `announce` template no longer has `{title}`.)

The final `storyPreview` block in `uk.ts` should look like:

```ts
  storyPreview: {
    label: 'Приклад казки',
    title: 'Ось яка казка може чекати\nсьогодні ввечері',
    tagline: 'Кожна казка — унікальна. Жодного повторення.',
    dropCap: 'Д',
    text: "авним-давно, у самому серці Зачарованого лісу, жила маленька зірочка на ім'я Мія. Вона не світила на небі, як інші зірки — натомість мешкала у дуплі старезного дуба і щоночі вирушала у подорож стежками, вкритими сріблястим мохом.",
    text2: 'Вона перейшла струмок місячного світла, де сріблясті рибки стрибали з каменю на камінь, і нарешті дісталась галявини, де сім світлячків сиділи колом, чекаючи на найменшу зірку у світі.',
    prevAria: 'Попередня сторінка',
    nextAria: 'Наступна сторінка',
    dotAria: 'Перейти до сторінки {n}',
    announce: 'Сторінка {n} з {total}',
  },
```

- [ ] **Step 2: Update `en.ts`**

Same surgery in `frontend/src/locales/en.ts`:

(a) Remove from `howItWorks`:
```ts
    prevAria: 'Previous step',
    nextAria: 'Next step',
    dotAria: 'Go to step {n}',
    announce: 'Step {n} of {total}: {title}',
```

(b) Add to `storyPreview` (after `text:` line):
```ts
    text2: 'She crossed a brook of moonlight, where silver fish leapt from rock to rock, and at last reached a clearing where seven fireflies sat in a circle, waiting for the smallest star in the world.',
    prevAria: 'Previous page',
    nextAria: 'Next page',
    dotAria: 'Go to page {n}',
    announce: 'Page {n} of {total}',
```

The final `storyPreview` block in `en.ts` should look like:

```ts
  storyPreview: {
    label: 'Story example',
    title: "Here's a story that could be waiting\nfor you tonight",
    tagline: 'Every story is unique. No two alike.',
    dropCap: 'O',
    text: "nce upon a time, deep in the heart of the Enchanted Forest, there lived a little star named Mia. She didn't shine in the sky like other stars — instead she lived in the hollow of an ancient oak and every night set out on a journey along paths covered in silvery moss.",
    text2: 'She crossed a brook of moonlight, where silver fish leapt from rock to rock, and at last reached a clearing where seven fireflies sat in a circle, waiting for the smallest star in the world.',
    prevAria: 'Previous page',
    nextAria: 'Next page',
    dotAria: 'Go to page {n}',
    announce: 'Page {n} of {total}',
  },
```

- [ ] **Step 3: Verify build**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend && npm run build 2>&1 | tail -10
```
Expected: exit 0. The locale shape change is structurally consistent (both languages match), and no current file references the removed `howItWorks` keys (StoryBook no longer reads from locale; HowItWorks reverted version doesn't reference them).

If the build fails on a missing `t.howItWorks.prevAria` or similar, search for the consumer (`grep -rn "howItWorks.prevAria"` in `frontend/src`) — it shouldn't exist after Task 5.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/locales/uk.ts frontend/src/locales/en.ts
git commit -m "i18n(frontend): move book control labels from howItWorks to storyPreview, add text2"
```

---

## Task 8: Trim unused rules from `StoryBook.module.css` + add `.pageNumberRight`

After the previous tasks, `.stepLabel`, `.stepTitle`, `.stepDesc` are no longer referenced. Clean them out. Also add a new `.pageNumberRight` modifier class for the right-page page-number positioning (referenced by `IllustrationPage` per Task 4).

**Files:**
- Modify: `frontend/src/components/home/StoryBook.module.css`

- [ ] **Step 1: Remove three rules**

In `frontend/src/components/home/StoryBook.module.css`, delete these 3 rule blocks:

```css
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
```

Keep everything else, especially: `.wrap`, `.wrap:focus-visible`, `.bookFrame`, `.page`, `[data-theme="dark"] .page`, `.pageStory`, `.pageInner`, `.pageNumber`, `.pageIllust`, `.pageIllust::before`, `.pageIllust:has(.illustImgLoaded)::before`, `@keyframes storybookSkeleton`, `.illustImg`, `.illustImgLoaded`, `.controls`, `.arrow` (+ pseudos), `.dots`, `.dot` (+ pseudos), `.dotActive`, `.srOnly`, the `prefers-reduced-motion` block.

- [ ] **Step 2: Add `.pageNumberRight` modifier**

Find the existing `.pageNumber` rule (it has `position: absolute; bottom: 18px; left: 36px; ...`). Immediately after that rule block, add:

```css
.pageNumberRight {
  left: auto;
  right: 36px;
  color: rgba(255, 255, 255, 0.65);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.4);
}
```

This class is added by `IllustrationPage` alongside `.pageNumber` (`className={`${styles.pageNumber} ${styles.pageNumberRight}`}`). It overrides `left` to position the number on the right side of the page and uses a light color with a subtle shadow so it reads against the illustration regardless of light/dark theme.

- [ ] **Step 3: Verify build**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend && npm run build 2>&1 | tail -10
```
Expected: exit 0. CSS Modules don't produce TS errors for unused class definitions; the produced CSS bundle gets a few bytes smaller and gains the new `.pageNumberRight` rule (referenced by Task 4's IllustrationPage when `pageNum` is provided).

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/home/StoryBook.module.css
git commit -m "style(frontend): trim unused step classes and add pageNumberRight to StoryBook module"
```

---

## Task 9: Rewrite `StoryPreview.module.css`

Drop the static-book CSS rules; add the `bookSlot` centering rule. Keep everything else.

**Files:**
- Modify: `frontend/src/components/home/StoryPreview.module.css`

- [ ] **Step 1: Replace the file**

Replace the entire contents of `frontend/src/components/home/StoryPreview.module.css` with:

```css
.section {
  padding: 100px 48px;
  background: var(--color-night);
  color: #E8DCC8;
  position: relative;
  overflow: hidden;
}
[data-theme="light"] .section {
  background: var(--color-surface-2);
  color: var(--color-text);
}

.inner {
  max-width: 1000px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.label {
  font-weight: 600;
  font-size: 12px;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--color-magic-glow);
  margin-bottom: 12px;
}

.title {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: clamp(28px, 3.5vw, 42px);
  line-height: 1.25;
  margin-bottom: 48px;
  color: #E8DCC8;
}
[data-theme="light"] .title {
  color: var(--color-text);
}

.bookSlot {
  display: flex;
  justify-content: center;
}

.dropCap {
  float: left;
  font-family: var(--font-display);
  font-size: 72px;
  line-height: 0.8;
  font-weight: 700;
  color: var(--color-magic);
  margin-right: 8px;
  margin-top: 6px;
}

.storyText {
  font-family: var(--font-display);
  font-size: 16px;
  line-height: 1.85;
  font-weight: 400;
  margin: 0;
}
[data-theme="dark"] .storyText { color: #d4c8b4; }

.cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: var(--color-magic);
  vertical-align: text-bottom;
  margin-left: 2px;
  animation: blink 0.7s step-end infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.tagline {
  text-align: center;
  margin-top: 32px;
  font-size: 15px;
  color: var(--color-magic-glow);
  font-weight: 500;
}

.fallback {
  display: grid;
  grid-template-columns: 1fr 1fr;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 40px rgba(0,0,0,0.4), 0 2px 8px rgba(0,0,0,0.2);
  position: relative;
  background: var(--color-surface);
}
[data-theme="dark"] .fallback { background: #1A1035; }

.fallbackLeft {
  padding: 48px;
  border-right: 1px solid rgba(160,120,96,0.15);
  color: var(--color-text);
}
[data-theme="dark"] .fallbackLeft { color: #E8DCC8; }

.fallbackRight {
  min-height: 300px;
  background: var(--color-surface-2);
}

@media (max-width: 860px) {
  .section { padding: 80px 20px; }
  .fallback { grid-template-columns: 1fr; }
  .fallbackRight { min-height: 200px; }
}
```

What was deleted: `.bookSpread`, `.bookSpread::after`, `.bookLeft`, `[data-theme="dark"] .bookLeft`, `.bookRight`, `[data-theme="light"] .bookRight`, `.storyIllustration`, `.bookVisible .bookRight`, `.illustHero`, `.pageOpen` keyframes, `.pageNum`, `.pageNumLeft`, `.pageNumRight`, `[data-theme="light"] .pageNumRight`. The page numbers move to `StoryBook.module.css`'s `.pageNumber` (already styled there).

What was kept: section/inner/label/title/dropCap/storyText/cursor/tagline.

What was added: `.bookSlot`, `@keyframes blink` (was implied before but actually wasn't defined in this file — verify), and a `.fallback*` block for the inline fallback.

- [ ] **Step 2: Verify keyframes**

`@keyframes blink` was used by `.cursor` previously but never defined in this file — it must have been in `global.css`. Verify by searching:
```bash
grep -rn "@keyframes blink" /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend/src/
```
If `blink` is already defined elsewhere (e.g., `src/design/global.css`), DELETE the local `@keyframes blink` block from `StoryPreview.module.css` (avoid duplicate definitions). If it's not defined elsewhere, KEEP the local definition.

- [ ] **Step 3: Verify build**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend && npm run build 2>&1 | tail -10
```
Expected: exit 0. CSS module typing is structural, so unused or missing classes won't error here — but the next task wires up consumers.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/home/StoryPreview.module.css
git commit -m "style(frontend): rewrite StoryPreview CSS for bookSlot + fallback layout"
```

---

## Task 10: Rewrite `StoryPreview.tsx` to use `<StoryBook>`

The biggest task. Composes 2 spreads (4 page nodes), wires the typewriter, image preload, and fallback.

**Files:**
- Modify: `frontend/src/components/home/StoryPreview.tsx`

- [ ] **Step 1: Replace the file**

Replace the entire contents of `frontend/src/components/home/StoryPreview.tsx` with:

```tsx
import {
  forwardRef,
  Suspense,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import { useReveal } from '../../lib/useReveal'
import { useLocale } from '../../lib/LocaleContext'
import { useTheme } from '../../lib/ThemeContext'
import { SectionParticles } from './SectionParticles'
import { StoryBook } from './StoryBook'
import { StoryBookErrorBoundary } from './StoryBookErrorBoundary'
import { IllustrationPage } from './IllustrationPage'
import storyBookStyles from './StoryBook.module.css'
import styles from './StoryPreview.module.css'

const TYPE_INTERVAL_MS = 10
const MOBILE_TYPEWRITER_MAX_WIDTH = 640

interface PreviewStoryPageProps {
  pageNum: number
  dropCap?: string
  body: string
  showCursor?: boolean
}

const PreviewStoryPage = forwardRef<HTMLDivElement, PreviewStoryPageProps>(
  function PreviewStoryPage({ pageNum, dropCap, body, showCursor }, ref) {
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
  }
)

interface PreviewBookFallbackProps {
  dropCap: string
  text: string
  imageSrc: string
}

function PreviewBookFallback({ dropCap, text, imageSrc }: PreviewBookFallbackProps) {
  return (
    <div className={styles.fallback}>
      <div className={styles.fallbackLeft}>
        <p className={styles.storyText}>
          <span className={styles.dropCap}>{dropCap}</span>
          {text}
        </p>
      </div>
      <div className={styles.fallbackRight}>
        <img
          src={imageSrc}
          alt=""
          style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
        />
      </div>
    </div>
  )
}

export function StoryPreview() {
  const { t, lang } = useLocale()
  const { theme } = useTheme()
  const { ref: headRef, visible: headVisible } = useReveal()
  const { ref: bookRef, visible: bookVisible } = useReveal({ threshold: 0 })

  const [typed, setTyped] = useState('')
  const [done, setDone] = useState(false)
  const [interacted, setInteracted] = useState(false)

  const intervalIdRef = useRef<number | null>(null)
  const interactedRef = useRef(false)
  useEffect(() => { interactedRef.current = interacted })

  // Reset typewriter state when language changes (text content changes).
  useEffect(() => {
    setTyped('')
    setDone(false)
    interactedRef.current = false
    setInteracted(false)
  }, [lang])

  // Run typewriter when the book first becomes visible.
  useEffect(() => {
    if (!bookVisible) return
    if (done) return
    if (interactedRef.current) return

    const fullText = t.storyPreview.text

    // Skip the typewriter on small screens — it's slow on mobile.
    if (typeof window !== 'undefined' && window.innerWidth < MOBILE_TYPEWRITER_MAX_WIDTH) {
      setTyped(fullText)
      setDone(true)
      return
    }

    let idx = 0
    intervalIdRef.current = window.setInterval(() => {
      // If the user interacted, snap to full text and stop.
      if (interactedRef.current) {
        setTyped(fullText)
        setDone(true)
        if (intervalIdRef.current !== null) {
          window.clearInterval(intervalIdRef.current)
          intervalIdRef.current = null
        }
        return
      }
      if (idx < fullText.length) {
        setTyped(fullText.slice(0, idx + 1))
        idx++
      } else {
        setDone(true)
        if (intervalIdRef.current !== null) {
          window.clearInterval(intervalIdRef.current)
          intervalIdRef.current = null
        }
      }
    }, TYPE_INTERVAL_MS)

    return () => {
      if (intervalIdRef.current !== null) {
        window.clearInterval(intervalIdRef.current)
        intervalIdRef.current = null
      }
    }
  }, [bookVisible, done, t.storyPreview.text])

  // Preload both spread illustrations for the current theme on mount + on theme change.
  useEffect(() => {
    for (let page = 1; page <= 2; page++) {
      const img = new Image()
      img.src = `/illustrations/preview-page${page}-${theme}.png`
    }
  }, [theme])

  const handleUserInteract = useCallback(() => {
    setInteracted(true)
    // The typewriter effect listens to interactedRef, but to fast-cancel the
    // current ongoing interval, also nudge the loop directly:
    if (intervalIdRef.current !== null) {
      setTyped(t.storyPreview.text)
      setDone(true)
      window.clearInterval(intervalIdRef.current)
      intervalIdRef.current = null
    }
  }, [t.storyPreview.text])

  const titleLines = t.storyPreview.title.split('\n')

  // Compose the 4 page nodes. Page numbers are 3, 4, 5, 6 (matching the
  // existing convention from the static design).
  const pages = useMemo(() => [
    <PreviewStoryPage
      key="text-1"
      pageNum={3}
      dropCap={t.storyPreview.dropCap}
      body={typed}
      showCursor={!done}
    />,
    <IllustrationPage
      key="img-1"
      src={`/illustrations/preview-page1-${theme}.png`}
      pageNum={4}
    />,
    <PreviewStoryPage
      key="text-2"
      pageNum={5}
      body={t.storyPreview.text2}
    />,
    <IllustrationPage
      key="img-2"
      src={`/illustrations/preview-page2-${theme}.png`}
      pageNum={6}
    />,
  ], [t.storyPreview.dropCap, t.storyPreview.text2, typed, done, theme])

  const fallback = (
    <PreviewBookFallback
      dropCap={t.storyPreview.dropCap}
      text={t.storyPreview.text}
      imageSrc={`/illustrations/preview-page1-${theme}.png`}
    />
  )

  return (
    <section className={styles.section} id="preview">
      <SectionParticles light />
      <div className={styles.inner}>
        <div ref={headRef} className={`reveal ${headVisible ? 'visible' : ''}`}>
          <div className={styles.label}>{t.storyPreview.label}</div>
          <div className={styles.title}>
            {titleLines[0]}<br />{titleLines[1]}
          </div>
        </div>

        <div ref={bookRef} className={styles.bookSlot}>
          <StoryBookErrorBoundary fallback={fallback}>
            <Suspense fallback={fallback}>
              <StoryBook
                pages={pages}
                ariaLabel={t.storyPreview.title.replace('\n', ' ')}
                autoAdvance={false}
                prevAria={t.storyPreview.prevAria}
                nextAria={t.storyPreview.nextAria}
                dotAria={t.storyPreview.dotAria}
                announce={t.storyPreview.announce}
                onUserInteract={handleUserInteract}
              />
            </Suspense>
          </StoryBookErrorBoundary>
        </div>

        <div className={`${styles.tagline} reveal ${headVisible ? 'visible' : ''}`}>
          {t.storyPreview.tagline}
        </div>
      </div>
    </section>
  )
}
```

Key design points:
- `PreviewStoryPage` and `PreviewBookFallback` are inline components in this file (per the spec).
- Typewriter logic largely mirrors the existing implementation, but with a stop-on-interact path. The `interactedRef` is read inside the interval to avoid stale-closure issues; `handleUserInteract` also clears the interval directly for immediate snap.
- `pages` is `useMemo`-ed with deps `[t.storyPreview.dropCap, t.storyPreview.text2, typed, done, theme]`. `typed` changes every 10 ms during typewriter; this drives a re-render that updates spread 1's text in place. The library's children diff should handle this without remount because the keys are stable.
- `aria-label` for the book region uses the title with the newline replaced (so screen readers don't read a literal `\n`).
- `useReveal({ threshold: 0 })` for the book — same as the existing implementation; fires as soon as ANY part of the book is in view, which is when we want the typewriter to start.
- Image preload covers both spreads' active-theme variants.

- [ ] **Step 2: Verify build**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend && npm run build 2>&1 | tail -15
```
Expected: exit 0. The lazy chunk for `react-pageflip` should still appear at ~44.84 kB raw / ~10.86 kB gzipped.

If you see a TS error like `Cannot find name 'XYZ'` or a missing locale property, check:
- `t.storyPreview.text2` exists in both `uk.ts` and `en.ts` (Task 7).
- `t.storyPreview.prevAria` etc. exist in both (Task 7).
- `IllustrationPage` accepts `src` (Task 4).
- `StoryBook` accepts the new prop set (Task 5).

- [ ] **Step 3: Verify lint**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend && npm run lint 2>&1 | tail -3
```
Expected: count at-or-below the baseline captured in P.2 (8 errors / 8 warnings).

If new lint errors appear in `StoryPreview.tsx`:
- React hook deps warnings on the typewriter effect — verify deps are correct; if a value is ref-managed, the rule may legitimately flag it (acceptable, but document).
- Anything else, fix before committing.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/home/StoryPreview.tsx
git commit -m "feat(frontend): rewrite StoryPreview as 2-spread storybook with typewriter"
```

---

## Task 11: Generate the 4 preview illustrations

Per the spec, the section visibly degrades without the images (the storybook is a story preview — empty illustration pages defeat the purpose). This task is asset prep, not code.

**Files:**
- Create: `frontend/public/illustrations/preview-page1-light.png`
- Create: `frontend/public/illustrations/preview-page1-dark.png`
- Create: `frontend/public/illustrations/preview-page2-light.png`
- Create: `frontend/public/illustrations/preview-page2-dark.png`

**Image briefs** (match the existing illustration style — soft children's-storybook watercolor; warm dawn palette in light mode, deep purples / star-glow in dark mode):

- **Page 1 — Mia in the oak tree at night.** A tiny luminous star-being curled in the hollow of an ancient gnarled oak. Soft moonlight spilling in through the bark, fireflies hovering nearby. Dark variant: deep purples and golds, the tree silhouetted against a starry sky. Light variant: warm dawn-amber tones, the inside of the hollow lit like a lantern.
- **Page 2 — Mia on the silver-moss path / brook of moonlight.** A small radiant figure walking a pathway covered in silvery moss, crossing a small brook where silver fish leap. Forest archway in the background. Dark variant: indigo-night with luminous moss and water. Light variant: pre-dawn warm light filtering through trees, the moss faintly glowing.

Output: PNG, ~1024×1280 (4:5 portrait), under ~2 MB each (matches existing `preview-{age}-{theme}.png` file sizes).

- [ ] **Step 1: Generate the 4 PNGs and place them in `frontend/public/illustrations/`**

Use the project's image-generation workflow. Filenames must be exactly:
```
preview-page1-light.png  preview-page1-dark.png
preview-page2-light.png  preview-page2-dark.png
```

- [ ] **Step 2: Verify file presence and rough sizes**

```bash
ls -la /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend/public/illustrations/preview-page*.png
```
Expected: 4 files, each between ~500 KB and ~2 MB.

- [ ] **Step 3: Visual sanity check**

Open each in Preview / browser. Confirm:
- Aspect ratio roughly 4:5 portrait.
- Light variant reads on the section's `var(--color-surface-2)` (a warm cream) parchment background.
- Dark variant reads on `var(--color-night)` (deep purple) background.
- No text overlays (text is on the paired left page).

- [ ] **Step 4: Commit**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook
git add frontend/public/illustrations/preview-page1-light.png \
       frontend/public/illustrations/preview-page1-dark.png \
       frontend/public/illustrations/preview-page2-light.png \
       frontend/public/illustrations/preview-page2-dark.png
git commit -m "assets(frontend): add 4 preview-section storybook illustrations"
```

---

## Task 12: Manual browser verification

The frontend has no test framework. Run through this checklist; fix anything that fails before declaring the plan done.

**Setup:**
- [ ] Start the dev server: `cd /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend && npm run dev`. Open `http://localhost:5173/`.
- [ ] DevTools open. Desktop-width viewport (≥ 1024 px).

**`#how` section (revert verification):**
- [ ] Section displays as 2 columns: 3 numbered text steps on the left, sidebar `IllustrationCarousel` on the right.
- [ ] Step numbers (`I` / `II` / `III`) flip in on scroll-reveal.
- [ ] Dashed connector line draws between steps as they enter view.
- [ ] Sidebar carousel rotates through 3 age-group illustrations (`how-3-5/6-8/9-12-{theme}.png`).
- [ ] No console warnings or errors.

**`#preview` section (storybook):**

*Initial reveal:*
- [ ] Scroll into `#preview`. Book opens to spread 1 (left = text + dropcap, right = `preview-page1-{theme}.png`).
- [ ] Typewriter runs character-by-character on spread 1's text.
- [ ] Cursor blinks while typing.
- [ ] Right page shows the parchment skeleton (pulse) until the image loads, then fades in.

*Manual navigation (no auto-advance):*
- [ ] Wait 10 s — page does NOT auto-flip.
- [ ] Click `›` arrow mid-typewriter → text snaps to full, page curls to spread 2.
- [ ] Click `‹` to return → spread 1 text is still complete, no replay of typewriter.
- [ ] Click dot 2, then dot 1 — same.
- [ ] Drag the bottom-right corner of a page → book turns smoothly.
- [ ] Keyboard: focus the book region (Tab), press `→`, `←`, `Home`, `End` — all work.
- [ ] First interaction permanently disables the typewriter (can't restart it by waiting).

*Mobile:*
- [ ] Resize viewport to 400 px wide. Book switches to single-page portrait (4 sequential pages).
- [ ] Typewriter is skipped on initial render (text appears whole) — per existing logic.
- [ ] Arrows, dots, keyboard, drag all still work in portrait.
- [ ] Resize back to desktop without reload — book reinitializes into spread mode without losing the current spread.

*Theme:*
- [ ] Toggle theme. Both spread illustrations swap to the other variant cleanly.
- [ ] Section background switches (`var(--color-night)` ↔ `var(--color-surface-2)`).

*Locale:*
- [ ] Toggle UA ↔ EN. Spread 1 text resets and the typewriter re-runs (matches existing logic).
- [ ] Spread 2's text updates immediately.

*Reduced motion:*
- [ ] DevTools → Rendering → emulate `prefers-reduced-motion: reduce`.
- [ ] Reload. Typewriter is bypassed (text appears whole). Page curl is instant (no animation).
- [ ] Manual controls still work.

*Lazy-load fallback:*
- [ ] DevTools → Network → Offline.
- [ ] Clear site data + reload. The lazy `react-pageflip` chunk fails to fetch.
- [ ] `PreviewBookFallback` renders: a 2-column static layout with spread-1 text on left and the spread-1 image on right.

*A11y:*
- [ ] Run Lighthouse → Accessibility audit on `/`. Score ≥ pre-change baseline.
- [ ] (If screen reader available) VoiceOver / NVDA on each flip should announce "Page 2 of 2" (or "of 2" — the book has 2 spreads).
- [ ] Tab through the section: arrows + dots have visible focus rings.

*Console hygiene:*
- [ ] DevTools console clean across all of the above (no warnings, no errors).

*Build size:*
- [ ] `npm run build` lazy chunk size ≤ ~50 kB raw / ~12 kB gzipped (matches the previous storybook batch).

- [ ] **Step 1: Run the manual checklist above**

If anything fails, fix and add a small follow-up commit. If a fix touches multiple files, commit with: `fix(frontend): preview storybook QA — <one-line summary>`.

- [ ] **Step 2: Final verification**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-storybook/frontend
node_modules/.bin/tsc --noEmit ; echo "tsc-noemit exit=$?"
npm run lint 2>&1 | tail -3
npm run build 2>&1 | tail -10
```
All three must succeed at-or-below the baseline.

---

## Out of scope

- Removal of the now-orphaned `preview-{age}-{theme}.png` files (3-5/6-8/9-12 × light/dark). Follow-up cleanup commit.
- Renaming `StoryBook`/`IllustrationPage`/`StoryBookErrorBoundary` to neutral names (e.g., `FlipBook`, `LazyErrorBoundary`). They're now genuinely generic, but renaming creates extra diff for no functional benefit.
- Building a third book somewhere else.
- Adding a "story title" field to the announce template.

## Risks (recap)

- **Generalizing `StoryBook` is the highest-risk part.** The component has a dense set of interacting effects (IO observer, visibilitychange, auto-advance, keyboard, dots) plus the `react-pageflip` library quirks. Any regression in this refactor breaks the user flow. Tasks 5 and 12 cover this: Task 5 is the careful refactor, Task 12 verifies the behaviors end-to-end.
- **Typewriter race with page-flip.** The typewriter mutates state on a `setInterval`. The implementation in Task 10 uses an `interactedRef` checked inside the interval AND clears the interval immediately on user interact — both paths converge on "snap to full text + stop". If only one of the paths is reliable, half-typed text could persist. Verify in Task 12 by clicking `›` mid-type repeatedly.
- **`useMemo` for `pages` includes `typed`.** Spread 1's content updates on every typewriter tick. The library's child diff handles stable `key`s without remount. If we observe spread 1's image flickering during typewriter (the right-page child gets re-rendered too), the fix is to memoize spread-1 page node and spread-1 illust node separately, only updating the text page on `typed` change. Verify in Task 12.
- **Locale key relocation.** Moving `prevAria`/etc. from `howItWorks` to `storyPreview` in Task 7 will break the build if any consumer still references the old paths — but the only consumer was `StoryBook.tsx`, which the refactor in Task 5 strips. If this breaks unexpectedly, search for stragglers.
