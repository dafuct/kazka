# Preview-section static revert + storybook cleanup — implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Revert `#preview` to its pre-storybook static design (text + IllustrationCarousel) with three deltas (no page numbers, `border-radius: 16px`, orphaned locale keys removed), and delete every piece of code that was added today to support the now-removed flip book.

**Architecture:** Six small steps: rewrite `StoryPreview.{tsx,module.css}` to a known-good historical shape with three deltas, then garbage-collect the orphaned storybook trio + 3 lib hooks + `react-pageflip` dependency + 5 locale keys. Each task verifies with `npm run build` (the real type-check) before committing.

**Tech Stack:** React 19, TypeScript 6, Vite 8, CSS Modules.

**Reference spec:** `docs/superpowers/specs/2026-05-03-preview-static-revert-design.md`

**Important constraints (from `CLAUDE.md` + lessons learned):**
- TypeScript verification: `cd frontend && node_modules/.bin/tsc --noEmit` is the documented command, **but it's a no-op against the project's `tsconfig.json` stub**. The REAL type-check is `cd frontend && npm run build` (which runs `tsc -b && vite build`). **Every code task in this plan verifies with `npm run build`.**
- React 19, TS 6, Vite 8.
- All UI text via `useLocale()` `t` — no hardcoded strings.
- No test framework — verification = `npm run build` + `npm run lint` + manual browser check.
- Pre-existing baseline lint count: capture in pre-flight; tasks must not increase it.

---

## File structure

**Modified:**
- `frontend/src/components/home/StoryPreview.tsx` — full rewrite (restored static structure, no page-number `<div>`s).
- `frontend/src/components/home/StoryPreview.module.css` — full rewrite (restored static structure, `border-radius: 16px`, no `.pageNum*` rules).
- `frontend/src/locales/uk.ts` — remove 5 keys from `storyPreview` block.
- `frontend/src/locales/en.ts` — remove 5 keys from `storyPreview` block.
- `frontend/package.json` + `frontend/package-lock.json` — `npm uninstall react-pageflip`.

**Deleted:**
- `frontend/src/components/home/StoryBook.tsx`
- `frontend/src/components/home/IllustrationPage.tsx`
- `frontend/src/components/home/StoryBookErrorBoundary.tsx`
- `frontend/src/components/home/StoryBook.module.css`
- `frontend/src/lib/useReducedMotion.ts`
- `frontend/src/lib/useBreakpoint.ts`
- `frontend/src/lib/useAutoAdvance.ts`

**Unchanged but referenced:**
- `frontend/src/components/illustrations/IllustrationCarousel.tsx` — used by the restored `StoryPreview`.
- `frontend/src/design/global.css` — defines `@keyframes blink` (line 346) and `@keyframes pageOpen` (line 367) that the restored CSS references.
- `frontend/public/illustrations/preview-{3-5,6-8,9-12}-{light,dark}.png` — used by the restored carousel.

---

## Pre-flight

- [ ] **P.1: Confirm clean working tree on `main`**

Run from `/Users/makar/dev/kazka`:
```bash
git status
git log --oneline -3
```
Expected: clean or only the pre-existing `frontend/src/components/home/Features.module.css` modification (unrelated to this work). HEAD should include commit `2190da0` (the spec doc).

- [ ] **P.2: Capture baseline build state on main**

Run from `/Users/makar/dev/kazka/frontend`:
```bash
node_modules/.bin/tsc --noEmit ; echo "tsc-noemit exit=$?"
npm run lint 2>&1 | tail -3
npm run build 2>&1 | tail -8
```
Note these baseline numbers — every code task must keep them at-or-below this baseline:
- `npm run lint` problem count (expected: **8 errors / 8 warnings** carried over).
- `npm run build`: must exit 0. Capture the lazy chunk size for `react-pageflip` (~44.84 kB raw / ~10.86 kB gzipped). After Task 6 (uninstall), this chunk should be GONE.

If baseline differs, record actual numbers in the plan-execution log — tasks compare to actual baseline.

---

## Task 1: Set up isolated worktree

Standard isolation per project convention.

- [ ] **Step 1: Create worktree on a new feature branch**

Run from `/Users/makar/dev/kazka`:
```bash
git worktree add .worktrees/preview-static-revert -b feature/preview-static-revert
```
Expected: worktree created at `.worktrees/preview-static-revert` on branch `feature/preview-static-revert`.

- [ ] **Step 2: Install deps in the worktree**

Run:
```bash
cd /Users/makar/dev/kazka/.worktrees/preview-static-revert/frontend && npm install 2>&1 | tail -3
```
Expected: clean install.

- [ ] **Step 3: Verify baseline holds in the worktree**

Run:
```bash
cd /Users/makar/dev/kazka/.worktrees/preview-static-revert/frontend
node_modules/.bin/tsc --noEmit ; echo "tsc-noemit exit=$?"
npm run lint 2>&1 | tail -3
npm run build 2>&1 | tail -8
```
Expected: same baseline numbers as P.2.

**No commit for this task** — worktree setup, not a code change.

---

## Task 2: Rewrite `StoryPreview.tsx` to the static design

Restore the pre-storybook React tree, with the page-number `<div>`s removed.

**Files:**
- Modify (full replace): `frontend/src/components/home/StoryPreview.tsx`

- [ ] **Step 1: Replace the file's entire contents**

Replace `frontend/src/components/home/StoryPreview.tsx` with this exact code:

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

What changed vs. the historical (`acc9716`) version: removed two `<div className={styles.pageNum...}>3</div>` / `4</div>` elements (one inside `.bookLeft` and one inside `.bookRight`). Everything else is identical.

What changed vs. the current version (storybook flip book): everything — the React tree shrinks dramatically.

- [ ] **Step 2: Verify build**

Note: `npm run build` will likely FAIL here because:
- The current `t.storyPreview.text2` etc. keys are still in the locale (still valid).
- The newly rewritten `StoryPreview.tsx` doesn't import `StoryBook`, `IllustrationPage`, `StoryBookErrorBoundary`, or any of the lib hooks. But those files all still exist and still compile on their own (no broken imports), so the build should be GREEN.

Run:
```bash
cd /Users/makar/dev/kazka/.worktrees/preview-static-revert/frontend && npm run build 2>&1 | tail -8 ; echo "BUILD-EXIT=$?"
```

Expected: exit 0. Note that the lazy `react-pageflip` chunk should already be tree-shaken from the bundle (no consumer), but the file is still in `node_modules` and the dependency entry is still in `package.json` — Tasks 5 and 6 finish that cleanup.

If the build fails on a missing CSS class reference (e.g. `styles.bookSpread` undefined), that means the CSS file hasn't been rewritten yet — Task 3 fixes that. But CSS Modules are structurally typed in this project, so undefined class references resolve to `undefined` at runtime without TS error. The build SHOULD pass.

If it fails on something else, stop and report.

- [ ] **Step 3: Commit**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-static-revert
git add frontend/src/components/home/StoryPreview.tsx
git commit -m "refactor(frontend): rewrite StoryPreview to static layout without page numbers"
```

---

## Task 3: Rewrite `StoryPreview.module.css` to the static design

Restore the pre-storybook CSS, with `border-radius: 16px` and the page-number rules removed.

**Files:**
- Modify (full replace): `frontend/src/components/home/StoryPreview.module.css`

- [ ] **Step 1: Replace the file's entire contents**

Replace `frontend/src/components/home/StoryPreview.module.css` with this exact CSS:

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

What changed vs. historical (`acc9716`):
- `.bookSpread { border-radius: 16px }` (was `4px`).
- Removed `.pageNum`, `.pageNumLeft`, `.pageNumRight` rules.

What changed vs. current (storybook): full rewrite. Removed `.bookSlot`, `.fallback`, `.fallbackLeft`, `.fallbackRight`. Restored `.bookSpread`, `.bookSpread::after`, `.bookLeft`, `.bookRight`, `.bookVisible .bookRight`, `.illustHero`, `.storyIllustration`.

`@keyframes blink` and `@keyframes pageOpen` are NOT in this file — they live in `frontend/src/design/global.css` (lines 346 and 367) and are referenced by `.cursor` and `.bookVisible .bookRight` respectively. Don't add them here.

- [ ] **Step 2: Verify build**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-static-revert/frontend && npm run build 2>&1 | tail -8 ; echo "BUILD-EXIT=$?"
npm run lint 2>&1 | tail -3
```
Expected: exit 0. Lint at-or-below 8 errors / 8 warnings.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/home/StoryPreview.module.css
git commit -m "style(frontend): rewrite StoryPreview CSS to static layout (16px radius, no page numbers)"
```

---

## Task 4: Delete the orphaned storybook + lib files

After Tasks 2 and 3, the storybook trio + 3 lib hooks are unused. Delete them.

**Files to delete:**
- `frontend/src/components/home/StoryBook.tsx`
- `frontend/src/components/home/IllustrationPage.tsx`
- `frontend/src/components/home/StoryBookErrorBoundary.tsx`
- `frontend/src/components/home/StoryBook.module.css`
- `frontend/src/lib/useReducedMotion.ts`
- `frontend/src/lib/useBreakpoint.ts`
- `frontend/src/lib/useAutoAdvance.ts`

- [ ] **Step 1: Verify nothing in the codebase still references these**

Run:
```bash
cd /Users/makar/dev/kazka/.worktrees/preview-static-revert
grep -rn "StoryBook\|IllustrationPage\|StoryBookErrorBoundary\|useReducedMotion\|useBreakpoint\|useAutoAdvance" frontend/src/
```
Expected output: only the files themselves (the symbol declarations and self-references). NO external imports.

If any external file references any of these symbols, STOP and report — that's an unexpected consumer that needs to be addressed before deletion.

- [ ] **Step 2: Delete all 7 files**

```bash
rm frontend/src/components/home/StoryBook.tsx \
   frontend/src/components/home/IllustrationPage.tsx \
   frontend/src/components/home/StoryBookErrorBoundary.tsx \
   frontend/src/components/home/StoryBook.module.css \
   frontend/src/lib/useReducedMotion.ts \
   frontend/src/lib/useBreakpoint.ts \
   frontend/src/lib/useAutoAdvance.ts
```

- [ ] **Step 3: Verify build**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-static-revert/frontend && npm run build 2>&1 | tail -8 ; echo "BUILD-EXIT=$?"
npm run lint 2>&1 | tail -3
```
Expected: exit 0. Lint at-or-below 8 errors / 8 warnings.

If the build fails with `Cannot find module './StoryBook'` or similar, search for the consumer (`grep -rn "from.*StoryBook" frontend/src/`) and stop — report what you found.

- [ ] **Step 4: Commit**

```bash
git add -A frontend/src/components/home/StoryBook.tsx \
       frontend/src/components/home/IllustrationPage.tsx \
       frontend/src/components/home/StoryBookErrorBoundary.tsx \
       frontend/src/components/home/StoryBook.module.css \
       frontend/src/lib/useReducedMotion.ts \
       frontend/src/lib/useBreakpoint.ts \
       frontend/src/lib/useAutoAdvance.ts
git commit -m "refactor(frontend): delete orphaned storybook trio and 3 lib hooks"
```

(`git add -A <path>` stages a deletion when the file is gone.)

---

## Task 5: Remove orphaned locale keys

The 5 keys (`text2`, `prevAria`, `nextAria`, `dotAria`, `announce`) added to `storyPreview` are no longer referenced. Remove them. Locale type source is `uk.ts`.

**Files:**
- Modify: `frontend/src/locales/uk.ts`
- Modify: `frontend/src/locales/en.ts`

- [ ] **Step 1: Verify the 5 keys aren't referenced anywhere**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-static-revert
grep -rn "storyPreview\.\(text2\|prevAria\|nextAria\|dotAria\|announce\)" frontend/src/
```
Expected: no results. If any consumer remains, stop and report.

- [ ] **Step 2: Remove the 5 keys from `uk.ts`**

In `frontend/src/locales/uk.ts`, find the `storyPreview` block and delete these 5 lines:
```ts
    text2: 'Вона перейшла струмок місячного світла, де сріблясті рибки стрибали з каменю на камінь, і нарешті дісталась галявини, де сім світлячків сиділи колом, чекаючи на найменшу зірку у світі.',
    prevAria: 'Попередня сторінка',
    nextAria: 'Наступна сторінка',
    dotAria: 'Перейти до сторінки {n}',
    announce: 'Сторінка {n} з {total}',
```

After deletion, the `storyPreview` block should look exactly like:
```ts
  storyPreview: {
    label: 'Приклад казки',
    title: 'Ось яка казка може чекати\nсьогодні ввечері',
    tagline: 'Кожна казка — унікальна. Жодного повторення.',
    dropCap: 'Д',
    text: "авним-давно, у самому серці Зачарованого лісу, жила маленька зірочка на ім'я Мія. Вона не світила на небі, як інші зірки — натомість мешкала у дуплі старезного дуба і щоночі вирушала у подорож стежками, вкритими сріблястим мохом.",
  },
```

- [ ] **Step 3: Remove the 5 matching keys from `en.ts`**

In `frontend/src/locales/en.ts`, find the `storyPreview` block and delete these 5 lines:
```ts
    text2: 'She crossed a brook of moonlight, where silver fish leapt from rock to rock, and at last reached a clearing where seven fireflies sat in a circle, waiting for the smallest star in the world.',
    prevAria: 'Previous page',
    nextAria: 'Next page',
    dotAria: 'Go to page {n}',
    announce: 'Page {n} of {total}',
```

After deletion, the `storyPreview` block should look exactly like:
```ts
  storyPreview: {
    label: 'Story example',
    title: "Here's a story that could be waiting\nfor you tonight",
    tagline: 'Every story is unique. No two alike.',
    dropCap: 'O',
    text: "nce upon a time, deep in the heart of the Enchanted Forest, there lived a little star named Mia. She didn't shine in the sky like other stars — instead she lived in the hollow of an ancient oak and every night set out on a journey along paths covered in silvery moss.",
  },
```

- [ ] **Step 4: Verify build**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-static-revert/frontend && npm run build 2>&1 | tail -8 ; echo "BUILD-EXIT=$?"
npm run lint 2>&1 | tail -3
```
Expected: exit 0, lint at-or-below baseline.

If a structural mismatch error appears (e.g. "Property 'text2' is missing in type"), the two locale files have diverged — re-check that both have exactly 5 `storyPreview` keys.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/locales/uk.ts frontend/src/locales/en.ts
git commit -m "i18n(frontend): remove orphaned storyPreview keys (text2, prevAria, nextAria, dotAria, announce)"
```

---

## Task 6: Uninstall `react-pageflip`

The dependency has no remaining consumers (StoryBook was the only importer, deleted in Task 4). Remove from `package.json` + lockfile.

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`

- [ ] **Step 1: Verify nothing still imports the package**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-static-revert
grep -rn "react-pageflip" frontend/src/
```
Expected: no results.

- [ ] **Step 2: Uninstall**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-static-revert/frontend
npm uninstall react-pageflip 2>&1 | tail -5
```
Expected: clean uninstall. The output should mention the package being removed and possibly a few transitive deps (e.g. `page-flip`).

- [ ] **Step 3: Verify build**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-static-revert/frontend && npm run build 2>&1 | tail -8 ; echo "BUILD-EXIT=$?"
npm run lint 2>&1 | tail -3
```
Expected: exit 0. **Confirm the lazy `react-pageflip` chunk is gone from the build output** — only the main JS chunk and CSS chunk should appear (no `index.es-*.js` chunk for the lazy import).

- [ ] **Step 4: Commit**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-static-revert
git add frontend/package.json frontend/package-lock.json
git commit -m "chore(frontend): uninstall react-pageflip (no remaining consumers)"
```

---

## Task 7: Manual browser verification

The frontend has no test framework. Walk this checklist; fix anything that fails before declaring done.

**Setup:**
- [ ] Start dev server: `cd /Users/makar/dev/kazka/.worktrees/preview-static-revert/frontend && npm run dev`. Open `http://localhost:5173/`.
- [ ] DevTools open. Desktop viewport (≥ 1024 px).

**`#preview` section:**
- [ ] Section displays as 2 columns: text + dropcap on left, illustration on right.
- [ ] Typewriter runs character-by-character on first scroll into view.
- [ ] Cursor blinks while typing; disappears when text completes.
- [ ] Right-side illustration rotates through age groups (3-5, 6-8, 9-12), in sync with the hero/how carousels.
- [ ] No page numbers (3, 4) visible anywhere.
- [ ] Block has visibly rounded corners (`16px`).
- [ ] No flip-book controls (arrows, dots) visible.
- [ ] Subtle bottom-right corner gradient (the `bookSpread::after` paper-edge hint) is visible.
- [ ] Drop shadow under the block.

**`#how` section:**
- [ ] Already-reverted design continues to work: 3 stacked numbered text steps, dashed connector, sidebar `IllustrationCarousel` rotating through age groups.

**Theme:**
- [ ] Toggle theme. The preview block's left page swaps surface color, right page's `IllustrationCarousel` swaps to dark/light variant. Section background swaps.

**Locale:**
- [ ] Toggle UA ↔ EN. Text resets and re-types in the new language. Title and tagline update.

**Mobile:**
- [ ] Resize viewport to 400 px wide. The 2-column grid collapses to single column. Right page is below left page. Typewriter is skipped on mobile (text appears whole — matches existing `window.innerWidth < 640` logic).

**Console hygiene:**
- [ ] DevTools console is clean across all of the above.

**Build:**
- [ ] Final `npm run build`: exits 0, NO `react-pageflip` lazy chunk in the output.

- [ ] **Step 1: Run the manual checklist above**

If anything fails, fix and add a small follow-up commit: `fix(frontend): preview revert QA — <one-line summary>`.

- [ ] **Step 2: Final verification**

```bash
cd /Users/makar/dev/kazka/.worktrees/preview-static-revert/frontend
node_modules/.bin/tsc --noEmit ; echo "tsc-noemit exit=$?"
npm run lint 2>&1 | tail -3
npm run build 2>&1 | tail -10
```
All three must succeed at-or-below baseline. Build output should NOT contain a chunk for `react-pageflip` or `page-flip`.

---

## Out of scope

- Removing the spec/plan markdown files from earlier today (`docs/superpowers/`). They're useful historical context; cheap to leave in place.
- Renaming the `'preview'` section name in `IllustrationCarousel`'s prop union to something more descriptive.
- Removing the 6 `preview-{age}-{theme}.png` files — still actively used by the carousel.

## Risks (recap)

- **`@keyframes pageOpen` reference.** The restored CSS rule `.bookVisible .bookRight { animation: pageOpen ...; }` references a global keyframe. Verified at `src/design/global.css:367`. Should animate correctly.
- **`@keyframes blink` reference.** Same — verified at `src/design/global.css:346`.
- **`react-pageflip` uninstall side effects.** The package's transitive dependency `page-flip` will also be removed. No other consumer depends on `page-flip`, so this is safe.
- **Locale removal cascading.** The 5 removed keys are only referenced by the (now-deleted) StoryBook trio plus the (now-replaced) old StoryPreview. After Tasks 2 and 4, no consumers remain — safe to remove.
