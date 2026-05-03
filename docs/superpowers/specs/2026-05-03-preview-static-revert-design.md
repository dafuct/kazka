# Preview-section static revert + storybook cleanup — design spec

**Date:** 2026-05-03
**Scope:** `frontend/src/components/home/StoryPreview.{tsx,module.css}` + deletion of the now-unused storybook component trio, lib hooks, and `react-pageflip` dependency.
**Status:** Approved by user (brainstorming session)

## Problem

Earlier today, the `#preview` section was replaced with a 2-spread `react-pageflip` storybook (the same mechanic that briefly lived in `#how`). After seeing it in the browser, the user determined the flip-book mechanic isn't a fit here either — the runtime-loaded library, the page-curl interaction, and the page numbers all add motion and chrome that get in the way of the "here's a sample story" purpose. The page also currently shows a broken-image right page because the 4 illustrations the storybook expects (`preview-page1/2-{light,dark}.png`) were never generated.

## Goal

Revert `#preview` to its pre-storybook static design (text on left with dropcap + typewriter, `IllustrationCarousel` on right), then delete every piece of code that was added to support the flip book and is now orphaned. Three intentional deltas vs. the historical version: no page numbers, `border-radius: 16px` (matching other content blocks), and the orphaned locale keys gone.

## Files

### Modified

- `frontend/src/components/home/StoryPreview.tsx` — full rewrite. Restores the pre-storybook structure (text + typewriter on left, `IllustrationCarousel section="preview"` on right) but without the page-number `<div>`s.
- `frontend/src/components/home/StoryPreview.module.css` — full rewrite. Restores the pre-storybook structure but with `border-radius: 16px` on `.bookSpread` (was `4px` historically) and without the `.pageNum`, `.pageNumLeft`, `.pageNumRight` rules.
- `frontend/src/locales/uk.ts` — remove `text2`, `prevAria`, `nextAria`, `dotAria`, `announce` from the `storyPreview` block. (Locale type source is `typeof uk`, so update `uk.ts` first.)
- `frontend/src/locales/en.ts` — same removals; structurally must match `uk.ts`.
- `frontend/package.json` + `frontend/package-lock.json` — remove `react-pageflip` dependency.

### Deleted

- `frontend/src/components/home/StoryBook.tsx`
- `frontend/src/components/home/IllustrationPage.tsx`
- `frontend/src/components/home/StoryBookErrorBoundary.tsx`
- `frontend/src/components/home/StoryBook.module.css`
- `frontend/src/lib/useReducedMotion.ts`
- `frontend/src/lib/useBreakpoint.ts`
- `frontend/src/lib/useAutoAdvance.ts`

### Unchanged

- `HowItWorks.tsx` and `HowItWorks.module.css` — already reverted earlier today.
- `IllustrationCarousel.tsx` and `carouselTickStore.ts` — used by the restored preview, already in place. Its `section` prop union still includes `'preview'`.
- The 6 existing `frontend/public/illustrations/preview-{3-5,6-8,9-12}-{light,dark}.png` files — used by the restored carousel.

## User experience

Same as the original pre-storybook design:

- Header: label ("Story example") + 2-line title.
- Centered open-book layout below: full-width 1fr 1fr grid with a soft drop shadow, `border-radius: 16px`.
  - **Left page:** dropcap + body text. The body types out character-by-character (10 ms per character) when the book first scrolls into view. After the typewriter completes, a blinking cursor disappears.
  - **Right page:** `IllustrationCarousel section="preview"` rotating through the 6 age-themed illustrations, synced with hero/how carousels via the existing `carouselTickStore`.
- Tagline below.
- A subtle bottom-right corner gradient (`bookSpread::after`) gives the visual hint of a paper edge.

**Differences vs. the historical pre-storybook design:**
1. No page numbers (`3` and `4` removed from both pages).
2. `.bookSpread` border-radius is `16px` (was `4px`).
3. The five orphaned locale keys (`text2`, `prevAria`, `nextAria`, `dotAria`, `announce`) are removed. None of them were referenced after the revert; keeping them would be dead data.

## File contents

### `StoryPreview.tsx` (full file)

```tsx
import { useEffect, useRef, useState } from 'react'
import { useReveal } from '../../lib/useReveal'
import { useLocale } from '../../lib/LocaleContext'
import { SectionParticles } from './SectionParticles'
import styles from './StoryPreview.module.css'
import { IllustrationCarousel } from '../illustrations/IllustrationCarousel'

export function StoryPreview() {
  const { t, lang } = useLocale()
  const { ref: headRef, visible: headVisible } = useReveal()
  const { ref: bookRef, visible: bookVisible } = useReveal({ threshold: 0 })
  const [typed, setTyped] = useState('')
  const [done, setDone] = useState(false)
  const typeStarted = useRef(false)

  useEffect(() => {
    setTyped('')
    typeStarted.current = false
    setDone(false)
  }, [lang])

  useEffect(() => {
    if (!bookVisible || typeStarted.current) return
    typeStarted.current = true
    const storyText = t.storyPreview.text
    if (window.innerWidth < 640) {
      setTyped(storyText)
      setDone(true)
      return
    }
    let idx = 0
    const interval = setInterval(() => {
      if (idx < storyText.length) {
        setTyped(storyText.slice(0, idx + 1))
        idx++
      } else {
        clearInterval(interval)
        setDone(true)
      }
    }, 10)
    return () => clearInterval(interval)
  }, [bookVisible, t.storyPreview.text])

  const titleLines = t.storyPreview.title.split('\n')

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

        <div
          ref={bookRef}
          className={`${styles.bookSpread} reveal ${bookVisible ? 'visible' : ''} ${bookVisible ? styles.bookVisible : ''}`}
        >
          <div className={styles.bookLeft}>
            <div className={styles.storyText}>
              <span className={styles.dropCap}>{t.storyPreview.dropCap}</span>
              {typed}
              {!done && <span className={styles.cursor} aria-hidden="true" />}
            </div>
          </div>

          <div className={styles.bookRight}>
            <IllustrationCarousel section="preview" />
          </div>
        </div>

        <div className={`${styles.tagline} reveal ${headVisible ? 'visible' : ''}`}>
          {t.storyPreview.tagline}
        </div>
      </div>
    </section>
  )
}
```

Vs. the historical (`acc9716`) version: the only change is removal of the two `<div className={styles.pageNum...}>3</div>` / `4</div>` elements (one from each side).

### `StoryPreview.module.css` (full file)

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

.bookSpread {
  display: grid;
  grid-template-columns: 1fr 1fr;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 40px rgba(0,0,0,0.4), 0 2px 8px rgba(0,0,0,0.2);
  position: relative;
}

.bookSpread::after {
  content: '';
  position: absolute;
  bottom: 0;
  right: 0;
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, transparent 50%, rgba(0,0,0,0.06) 50%);
  pointer-events: none;
  z-index: 2;
}

.bookLeft {
  background: var(--color-surface);
  color: var(--color-text);
  padding: 48px;
  position: relative;
  border-right: 1px solid rgba(160,120,96,0.15);
}
[data-theme="dark"] .bookLeft { background: #1A1035; color: #E8DCC8; }

.bookRight {
  overflow: hidden;
  position: relative;
  min-height: 300px;
  transform-origin: left center;
  background: #0a0d18;
}
[data-theme="light"] .bookRight {
  background: #4A90D9;
}

.storyIllustration {
  width: 100%;
  height: 100%;
  min-height: 300px;
  display: block;
}

.bookVisible .bookRight {
  animation: pageOpen 0.8s cubic-bezier(0.34,1.56,0.64,1) forwards;
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

.illustHero {
  width: 100%;
  height: 100%;
  min-height: 300px;
  background:
    radial-gradient(ellipse at 30% 60%, rgba(124,58,237,0.25) 0%, transparent 50%),
    radial-gradient(ellipse at 70% 40%, rgba(217,119,6,0.3) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 80%, rgba(22,101,52,0.15) 0%, transparent 40%),
    radial-gradient(ellipse at 50% 50%, rgba(237,217,163,0.4) 0%, transparent 60%),
    var(--color-surface-2);
}

.tagline {
  text-align: center;
  margin-top: 32px;
  font-size: 15px;
  color: var(--color-magic-glow);
  font-weight: 500;
}

@media (max-width: 860px) {
  .section { padding: 80px 20px; }
  .bookSpread { grid-template-columns: 1fr; }
  .bookRight { min-height: 200px; }
}
```

Vs. the historical (`acc9716`) version: two changes — `.bookSpread { border-radius: 16px }` (was `4px`) and the `.pageNum`, `.pageNumLeft`, `.pageNumRight` rules removed. Both `@keyframes blink` and `@keyframes pageOpen` are defined globally in `src/design/global.css` (lines 346 and 367 respectively), so `.cursor` and `.bookVisible .bookRight` will animate correctly.

### Locale `storyPreview` block (final shape)

UA (`uk.ts`):
```ts
storyPreview: {
  label: 'Приклад казки',
  title: 'Ось яка казка може чекати\nсьогодні ввечері',
  tagline: 'Кожна казка — унікальна. Жодного повторення.',
  dropCap: 'Д',
  text: "авним-давно, у самому серці Зачарованого лісу, жила маленька зірочка на ім'я Мія. Вона не світила на небі, як інші зірки — натомість мешкала у дуплі старезного дуба і щоночі вирушала у подорож стежками, вкритими сріблястим мохом.",
},
```

EN (`en.ts`):
```ts
storyPreview: {
  label: 'Story example',
  title: "Here's a story that could be waiting\nfor you tonight",
  tagline: 'Every story is unique. No two alike.',
  dropCap: 'O',
  text: "nce upon a time, deep in the heart of the Enchanted Forest, there lived a little star named Mia. She didn't shine in the sky like other stars — instead she lived in the hollow of an ancient oak and every night set out on a journey along paths covered in silvery moss.",
},
```

Five keys each. Both languages structurally identical.

## Verification

The frontend has no test framework. Per `CLAUDE.md`:

- `cd frontend && node_modules/.bin/tsc --noEmit` is the documented command but is a no-op against the project's `tsconfig.json` stub. Use `cd frontend && npm run build` for the real type-check (`tsc -b && vite build`).
- `cd frontend && npm run lint` — at-or-below the pre-existing 8 errors / 8 warnings baseline.

After the implementation:

1. `npm run build` exits 0.
2. The `react-pageflip` lazy chunk is **gone** from the build output (not just tree-shaken — the dep is removed). Other chunk sizes should stay roughly the same.
3. `npm run lint` ≤ baseline.
4. Manual browser check (after rebuild + serve):
   - `#preview` shows: text + dropcap + typewriter on left, rotating carousel illustration on right.
   - Corners are rounded 16px.
   - No page numbers visible anywhere.
   - Carousel rotates through the 3 age groups, in sync with the hero/how sections.
   - `#how` continues to work (already reverted earlier).
   - Console clean.

## Out of scope

- Removing the `'preview'` section name from `IllustrationCarousel`'s `section` prop union — still in active use.
- Removing the `preview-{age}-{theme}.png` files — still in active use by the carousel.
- Removing the spec/plan markdown files from earlier today — they're useful historical context; cheap to leave under `docs/superpowers/`.
- Restoring or improving the storybook implementation in any other section. If revisited later, the old commits are recoverable from git history.
- Renaming the `IllustrationCarousel` `'preview'` section to something more descriptive.

## Risks

- **Locale key removal cascading.** The 5 removed keys (`text2`, `prevAria`, `nextAria`, `dotAria`, `announce`) were only referenced by `StoryPreview.tsx` (via `t.storyPreview.*`). The new `StoryPreview.tsx` doesn't reference them. The deletions of the storybook trio remove the only other potential consumers. So the locale removals are safe — but a sanity grep before removing is part of the implementation plan.
- **`react-pageflip` removal cascading.** The package was only imported (lazily) by the now-deleted `StoryBook.tsx`. After the deletion of that file, no source references the package. `npm uninstall` should not trigger any peer-dep alarms.
