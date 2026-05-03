# Frontend Polish Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply five small frontend/UX changes — Ukrainian display font swap (Manrope), modal-only loading until both text+image ready, shorter generated titles, square loading card, book-style paragraph rendering.

**Architecture:** Pure CSS / React component refactor + 1 backend prompt edit. No API changes, no DB migrations, no new components. The five tasks are independent — each ends at a green check (`tsc --noEmit` + `npm run lint`) and a commit.

**Tech Stack:** React 19 · TypeScript 6 · Vite 8 · Spring Boot 4 · CSS Modules.

**Spec:** [`docs/superpowers/specs/2026-05-03-frontend-polish-pass-design.md`](../specs/2026-05-03-frontend-polish-pass-design.md)

---

## Pre-flight (run once before starting)

- [ ] **Verify clean baseline**

```bash
cd /Users/makar/dev/kazka/frontend && node_modules/.bin/tsc --noEmit && npm run lint
```

Expected: both pass. (There are uncommitted CSS/locale changes already in the tree — leave them as-is, they're pre-existing.)

---

## Task 1: Ukrainian display font (Manrope)

**Files:**
- Create: `frontend/public/fonts/manrope/manrope-400-latin.woff2`
- Create: `frontend/public/fonts/manrope/manrope-400-cyrillic.woff2`
- Create: `frontend/public/fonts/manrope/manrope-600-latin.woff2`
- Create: `frontend/public/fonts/manrope/manrope-600-cyrillic.woff2`
- Create: `frontend/public/fonts/manrope/manrope-700-latin.woff2`
- Create: `frontend/public/fonts/manrope/manrope-700-cyrillic.woff2`
- Modify: `frontend/src/design/global.css` (append `@font-face` for Manrope)
- Modify: `frontend/src/design/tokens.css` (append `html[data-lang="uk"]` rule)
- Modify: `frontend/src/lib/LocaleContext.tsx` (add `useEffect` that sets `data-lang`)

- [ ] **Step 1.1: Download Manrope woff2 files**

Use Google Fonts API to source self-hostable woff2 subsets. Run:

```bash
mkdir -p /Users/makar/dev/kazka/frontend/public/fonts/manrope
cd /Users/makar/dev/kazka/frontend/public/fonts/manrope

# Fetch the Google Fonts CSS for Manrope, then download the actual woff2 URLs.
# Pretend to be a recent Chrome to get woff2 (otherwise GF returns ttf).
UA="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

curl -sH "User-Agent: $UA" \
  "https://fonts.googleapis.com/css2?family=Manrope:wght@400;600;700&subset=latin,cyrillic&display=swap" \
  -o manrope.css

cat manrope.css
```

Expected: CSS with several `@font-face` blocks containing `unicode-range` and `https://fonts.gstatic.com/...woff2` URLs. Six total — three weights × two ranges (cyrillic + latin).

- [ ] **Step 1.2: Download each woff2 from the URLs in `manrope.css`**

Inspect `manrope.css` and download each `src: url(...)` to a friendly filename. There will be 6 URLs (the `latin-ext` ones can be skipped — we only need `latin` and `cyrillic`). The exact URLs change over time, so use the ones from your `manrope.css`. Example shape:

```bash
# Replace these with the URLs your manrope.css contains. Order: 400 cyrillic, 400 latin, 600 cyrillic, 600 latin, 700 cyrillic, 700 latin.
curl -L -o manrope-400-cyrillic.woff2 "https://fonts.gstatic.com/s/manrope/v…/<hash-cyrillic-400>.woff2"
curl -L -o manrope-400-latin.woff2    "https://fonts.gstatic.com/s/manrope/v…/<hash-latin-400>.woff2"
curl -L -o manrope-600-cyrillic.woff2 "https://fonts.gstatic.com/s/manrope/v…/<hash-cyrillic-600>.woff2"
curl -L -o manrope-600-latin.woff2    "https://fonts.gstatic.com/s/manrope/v…/<hash-latin-600>.woff2"
curl -L -o manrope-700-cyrillic.woff2 "https://fonts.gstatic.com/s/manrope/v…/<hash-cyrillic-700>.woff2"
curl -L -o manrope-700-latin.woff2    "https://fonts.gstatic.com/s/manrope/v…/<hash-latin-700>.woff2"

rm manrope.css
ls -la
```

Expected: 6 `.woff2` files, each 5–25 KB. Size check is the smoke test that downloads succeeded (a 1 KB file is an error page).

- [ ] **Step 1.3: Add `@font-face` declarations**

Append to `frontend/src/design/global.css` (just after the Nunito 700 block, line ~170):

```css
/* Manrope 400 */
@font-face {
  font-family: 'Manrope';
  font-style: normal;
  font-weight: 400;
  font-display: swap;
  src: url('/fonts/manrope/manrope-400-cyrillic.woff2') format('woff2');
  unicode-range: U+0301, U+0400-045F, U+0490-0491, U+04B0-04B1, U+2116;
}
@font-face {
  font-family: 'Manrope';
  font-style: normal;
  font-weight: 400;
  font-display: swap;
  src: url('/fonts/manrope/manrope-400-latin.woff2') format('woff2');
  unicode-range: U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+0304, U+0308, U+0329, U+2000-206F, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF, U+FFFD;
}

/* Manrope 600 */
@font-face {
  font-family: 'Manrope';
  font-style: normal;
  font-weight: 600;
  font-display: swap;
  src: url('/fonts/manrope/manrope-600-cyrillic.woff2') format('woff2');
  unicode-range: U+0301, U+0400-045F, U+0490-0491, U+04B0-04B1, U+2116;
}
@font-face {
  font-family: 'Manrope';
  font-style: normal;
  font-weight: 600;
  font-display: swap;
  src: url('/fonts/manrope/manrope-600-latin.woff2') format('woff2');
  unicode-range: U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+0304, U+0308, U+0329, U+2000-206F, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF, U+FFFD;
}

/* Manrope 700 */
@font-face {
  font-family: 'Manrope';
  font-style: normal;
  font-weight: 700;
  font-display: swap;
  src: url('/fonts/manrope/manrope-700-cyrillic.woff2') format('woff2');
  unicode-range: U+0301, U+0400-045F, U+0490-0491, U+04B0-04B1, U+2116;
}
@font-face {
  font-family: 'Manrope';
  font-style: normal;
  font-weight: 700;
  font-display: swap;
  src: url('/fonts/manrope/manrope-700-latin.woff2') format('woff2');
  unicode-range: U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+0304, U+0308, U+0329, U+2000-206F, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF, U+FFFD;
}
```

- [ ] **Step 1.4: Add the `data-lang` token override**

Append to the bottom of `frontend/src/design/tokens.css`:

```css
html[data-lang="uk"] {
  --font-display: 'Manrope', 'Nunito', 'Helvetica Neue', sans-serif;
}
```

(Body font deliberately stays Nunito for both languages — only display headers swap.)

- [ ] **Step 1.5: Set `data-lang` from `LocaleContext`**

Modify `frontend/src/lib/LocaleContext.tsx`. Replace the import line:

```ts
import { createContext, useContext, useState, useCallback } from 'react'
```

with:

```ts
import { createContext, useContext, useState, useCallback, useEffect } from 'react'
```

Then, inside `LocaleProvider`, immediately after the `const [lang, setLang] = useState<Lang>(stored)` line, insert:

```ts
useEffect(() => {
  document.documentElement.dataset.lang = lang
}, [lang])
```

- [ ] **Step 1.6: Verify**

```bash
cd /Users/makar/dev/kazka/frontend && node_modules/.bin/tsc --noEmit && npm run lint
```

Expected: both pass.

Visual check: `npm run dev`, open `http://localhost:5173`, confirm Ukrainian headings (e.g., the hero "Казка яка чекає...") render in Manrope (proportions slightly more open and consistent than Nunito Cyrillic). Toggle language to English — headings stay Nunito (no visual change).

- [ ] **Step 1.7: Commit**

```bash
cd /Users/makar/dev/kazka
git add frontend/public/fonts/manrope/ \
  frontend/src/design/global.css \
  frontend/src/design/tokens.css \
  frontend/src/lib/LocaleContext.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): swap Ukrainian display font to Manrope

Ukrainian headers (--font-display) now use Manrope, whose Cyrillic
and Latin glyphs are designed together by the same foundry — feels
visually consistent with how English headers render in Nunito.
Body font stays Nunito for both languages.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Modal-only loading until text + image both ready

**Files:**
- Modify: `frontend/src/components/modal/StoryModal.tsx`
- Modify: `frontend/src/components/modal/StoryModal.module.css`
- Modify: `frontend/src/pages/StoryDetailPage.tsx`

- [ ] **Step 2.1: Refactor `StoryModal.tsx` — drop StoryStream, add silent stream + image polling**

Replace the entire contents of `frontend/src/components/modal/StoryModal.tsx` with:

```tsx
import { useEffect, useCallback, useState, useRef } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import { StoryForm } from '../form/StoryForm'
import { useStoryModal } from '../../lib/StoryModalContext'
import { useLocale } from '../../lib/LocaleContext'
import { streamStory } from '../../lib/sseClient'
import { api } from '../../lib/apiClient'
import type { GenerationRequest } from '../../lib/types'
import styles from './StoryModal.module.css'

type Phase = 'form' | 'creating'

const POLL_INTERVAL_MS = 2000
const POLL_TIMEOUT_MS = 60000

export function StoryModal() {
  const { open, closeModal } = useStoryModal()
  const { t } = useLocale()
  const navigate = useNavigate()
  const [phase, setPhase] = useState<Phase>('form')
  const [request, setRequest] = useState<GenerationRequest | null>(null)
  const [error, setError] = useState<string | null>(null)
  const cancelRef = useRef<{ cancel: () => void } | null>(null)

  useEffect(() => {
    if (open) {
      setPhase('form')
      setRequest(null)
      setError(null)
    }
  }, [open])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && phase !== 'creating') closeModal()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, phase, closeModal])

  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : ''
    return () => { document.body.style.overflow = '' }
  }, [open])

  // Cancel any running creation when the modal closes/unmounts.
  useEffect(() => {
    return () => cancelRef.current?.cancel()
  }, [])

  useEffect(() => {
    if (phase !== 'creating' || !request) return

    const ctrl = new AbortController()
    let cancelled = false
    let pollTimer: number | undefined

    cancelRef.current = {
      cancel: () => {
        cancelled = true
        ctrl.abort()
        if (pollTimer) window.clearTimeout(pollTimer)
      },
    }

    const pollUntilReady = async (id: string) => {
      const startedAt = Date.now()
      const tick = async () => {
        if (cancelled) return
        try {
          const story = await api.getStory(id)
          if (story.illustrationStatus !== 'PENDING') {
            if (!cancelled) {
              closeModal()
              navigate(`/stories/${id}`)
            }
            return
          }
        } catch {
          // network blip — keep polling until timeout
        }
        if (Date.now() - startedAt > POLL_TIMEOUT_MS) {
          if (!cancelled) {
            closeModal()
            navigate(`/stories/${id}`)
          }
          return
        }
        pollTimer = window.setTimeout(tick, POLL_INTERVAL_MS)
      }
      tick()
    }

    streamStory(
      request,
      {
        onToken: () => {},
        onDone: ({ id }) => {
          if (cancelled) return
          api.illustrate(id).catch(() => null)
          pollUntilReady(id)
        },
        onError: ({ message }) => {
          if (cancelled) return
          setError(message)
          setPhase('form')
        },
      },
      ctrl.signal,
    ).catch(err => {
      if (cancelled || err?.name === 'AbortError') return
      setError(String(err))
      setPhase('form')
    })

    return () => {
      cancelled = true
      ctrl.abort()
      if (pollTimer) window.clearTimeout(pollTimer)
    }
  }, [phase, request, closeModal, navigate])

  const handleSubmit = useCallback((req: GenerationRequest) => {
    setRequest(req)
    setError(null)
    setPhase('creating')
  }, [])

  if (!open) return null

  const panelClass = phase === 'creating'
    ? `${styles.panel} ${styles.panelSquare}`
    : styles.panel

  return createPortal(
    <div
      className={styles.backdrop}
      onClick={phase !== 'creating' ? closeModal : undefined}
      role="dialog"
      aria-modal="true"
      aria-label="Створити казку"
    >
      <div className={panelClass} onClick={(e) => e.stopPropagation()}>
        {phase !== 'creating' && (
          <>
            <div className={styles.topBorder} />
            <div className={styles.header}>
              <div className={styles.ornament} aria-hidden="true">
                <svg viewBox="0 0 140 20" fill="none">
                  <path d="M10 10 Q35 3 70 10 Q105 17 130 10" stroke="currentColor" strokeWidth="1.5"/>
                  <circle cx="70" cy="10" r="4" fill="currentColor"/>
                  <path d="M67 10 L70 4 L73 10 L70 7Z" fill="currentColor" opacity="0.7"/>
                  <circle cx="35" cy="8" r="2" fill="currentColor" opacity="0.5"/>
                  <circle cx="105" cy="12" r="2" fill="currentColor" opacity="0.5"/>
                </svg>
              </div>
              <button className={styles.closeBtn} onClick={closeModal} aria-label="Закрити">✕</button>
            </div>
          </>
        )}
        <div className={styles.body}>
          {error && <div className={styles.error}>{error}</div>}
          {phase === 'form' && (
            <StoryForm onSubmit={handleSubmit} loading={false} inModal />
          )}
          {phase === 'creating' && (
            <div className={styles.creating}>
              <div className={styles.sun} aria-hidden="true">
                <svg viewBox="0 0 64 64" width="64" height="64">
                  <g className={styles.sunRays} stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
                    <line x1="32" y1="4"  x2="32" y2="12" />
                    <line x1="32" y1="52" x2="32" y2="60" />
                    <line x1="4"  y1="32" x2="12" y2="32" />
                    <line x1="52" y1="32" x2="60" y2="32" />
                    <line x1="12" y1="12" x2="17.5" y2="17.5" />
                    <line x1="46.5" y1="46.5" x2="52" y2="52" />
                    <line x1="52" y1="12" x2="46.5" y2="17.5" />
                    <line x1="17.5" y1="46.5" x2="12" y2="52" />
                  </g>
                  <circle cx="32" cy="32" r="11" fill="currentColor" />
                </svg>
              </div>
              <h2 className={styles.creatingTitle}>{t.form.generating}</h2>
            </div>
          )}
        </div>
      </div>
    </div>,
    document.body,
  )
}
```

Notes on this rewrite:
- Phase renamed `streaming` → `creating` to reflect that it covers text + illustration both.
- `StoryStream` import is gone. `streamStory` is now invoked directly inside an effect; tokens are discarded.
- After text completes (`onDone`), kicks off `api.illustrate(id)` and polls until the story's `illustrationStatus !== 'PENDING'` (READY or FAILED), then closes + navigates.
- 60s polling cap: if exceeded, navigate anyway — detail page handles all states.
- Backdrop click and Esc remain disabled during `creating`.
- `handleDone` and `handleError` callbacks are no longer needed — logic is inline.

- [ ] **Step 2.2: Rename `.streaming`/`.streamingTitle` CSS to `.creating`/`.creatingTitle`**

In `frontend/src/components/modal/StoryModal.module.css`, find:

```css
.streaming {
  width: 100%;
}

.streamingTitle {
  font-size: 1.125rem;
  color: var(--color-text-muted);
  margin-bottom: 16px;
  text-align: center;
}
```

Replace with:

```css
.creating {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.creatingTitle {
  font-size: 1.125rem;
  color: var(--color-text-muted);
  margin-top: 8px;
  text-align: center;
}
```

(Just the rename — no square shape yet. Task 4 adds the `.panelSquare` modifier.)

- [ ] **Step 2.3: Remove illustration polling from `StoryDetailPage.tsx`**

In `frontend/src/pages/StoryDetailPage.tsx`, delete the entire useEffect block:

```tsx
  useEffect(() => {
    if (!story || story.illustrationStatus !== 'PENDING') return
    const poll = setInterval(async () => {
      try {
        const updated = await api.getStory(story.id)
        setStory(updated)
      } catch {
        clearInterval(poll)
      }
    }, 3000)
    return () => clearInterval(poll)
  }, [story?.illustrationStatus, story?.id])
```

(That's the second `useEffect` in the file, lines ~37-48.)

The `useEffect` import on line 1 is still used elsewhere — leave it.

- [ ] **Step 2.4: Verify**

```bash
cd /Users/makar/dev/kazka/frontend && node_modules/.bin/tsc --noEmit && npm run lint
```

Expected: both pass.

Visual smoke test: `npm run dev`, click "Create story", fill out the form, submit. The modal should now show only the spinning sun + "Створюємо казку..." (no text streaming visible). After ~10–60s, modal closes and you land on `/stories/<id>` with both text and illustration already rendered. The panel is still the original wide rectangle — Task 4 makes it square.

- [ ] **Step 2.5: Commit**

```bash
cd /Users/makar/dev/kazka
git add frontend/src/components/modal/StoryModal.tsx \
  frontend/src/components/modal/StoryModal.module.css \
  frontend/src/pages/StoryDetailPage.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): wait for both text and illustration before navigation

StoryModal no longer renders the streaming text view. Instead it
shows a quiet loading card during creation, then polls until the
illustration finishes (or 60s timeout) before closing and routing
to the story page. StoryDetailPage drops its own polling block.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Shorter generated titles + UI clamp

**Files:**
- Modify: `backend/src/main/resources/prompts/story-system.txt`
- Modify: `frontend/src/components/story/StoryCard.module.css`
- Modify: `frontend/src/pages/StoryDetailPage.module.css`

- [ ] **Step 3.1: Tighten title format in the prompt**

In `backend/src/main/resources/prompts/story-system.txt`, change line 13:

From:
```
Line 1: a short book-style title (3–6 words, no punctuation at the end, no quotes, no "Title:" prefix)
```

To:
```
Line 1: a short book-style title (2–4 words maximum, no punctuation at the end, no quotes, no colons, no subtitles, no "Title:" prefix)
```

- [ ] **Step 3.2: Clamp StoryCard title to 2 lines**

In `frontend/src/components/story/StoryCard.module.css`, modify the `.title` rule (lines ~36-41) to:

```css
.title {
  font-size: 1.0625rem;
  margin-bottom: var(--spacing-2);
  color: var(--color-text);
  line-height: 1.4;
  overflow-wrap: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
```

- [ ] **Step 3.3: Allow detail-page title to wrap cleanly**

In `frontend/src/pages/StoryDetailPage.module.css`, modify the `.title` rule (lines ~45-51) to:

```css
.title {
  font-family: var(--font-display);
  font-size: clamp(1.5rem, 4vw, 2.5rem);
  margin-bottom: var(--spacing-4);
  line-height: 1.2;
  font-weight: 600;
  max-width: 100%;
  overflow-wrap: break-word;
}
```

- [ ] **Step 3.4: Verify**

```bash
cd /Users/makar/dev/kazka/frontend && node_modules/.bin/tsc --noEmit && npm run lint
```

Expected: both pass. (No backend test changes — the prompt is loaded at startup but no tests assert on its content.)

Smoke test the prompt change locally if Docker is running:

```bash
cd /Users/makar/dev/kazka/backend && ./gradlew bootRun
```

Generate a new story and confirm the title is 2–4 words. (If Docker / HF token isn't set up, the visual CSS clamps still cap legacy long titles to 2 lines — that's the safety net.)

- [ ] **Step 3.5: Commit**

```bash
cd /Users/makar/dev/kazka
git add backend/src/main/resources/prompts/story-system.txt \
  frontend/src/components/story/StoryCard.module.css \
  frontend/src/pages/StoryDetailPage.module.css
git commit -m "$(cat <<'EOF'
feat(stories): cap generated titles at 2-4 words + clamp UI

Tightens story-system prompt to "2-4 words maximum, no colons,
no subtitles". Adds CSS line-clamp on StoryCard and word-break
on StoryDetailPage title so legacy long titles still display
cleanly.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Square loading card

**Files:**
- Modify: `frontend/src/components/modal/StoryModal.module.css`

(`StoryModal.tsx` already applies `styles.panelSquare` from Task 2 — Task 4 only needs to add the matching CSS rules.)

- [ ] **Step 4.1: Add `.panelSquare` modifier**

Append to `frontend/src/components/modal/StoryModal.module.css` (just before the `@media` block at the bottom):

```css
.panel.panelSquare {
  max-width: 360px;
  width: 360px;
  min-height: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-xl);
}
.panel.panelSquare .body {
  padding: 24px;
  text-align: center;
  width: 100%;
}
```

(`.topBorder` and `.header` aren't rendered when `.panelSquare` is active — `StoryModal.tsx` conditionally omits them — so no CSS rule needed to hide them.)

- [ ] **Step 4.2: Add mobile fallback**

Find the existing mobile media query block at the bottom of `StoryModal.module.css`:

```css
@media (max-width: 720px) {
  .backdrop { align-items: flex-end; padding: 0; }
  .panel {
    max-width: 100%;
    border-radius: var(--radius-lg) var(--radius-lg) 0 0;
    max-height: 92vh;
  }
  .body { padding: 8px 16px 28px; }
}
```

Replace with:

```css
@media (max-width: 720px) {
  .backdrop { align-items: flex-end; padding: 0; }
  .panel {
    max-width: 100%;
    border-radius: var(--radius-lg) var(--radius-lg) 0 0;
    max-height: 92vh;
  }
  .body { padding: 8px 16px 28px; }

  .panel.panelSquare {
    align-self: center;
    width: 88vw;
    max-width: 360px;
    min-height: 88vw;
    border-radius: var(--radius-xl);
  }
  .backdrop:has(.panelSquare) { align-items: center; padding: 20px; }
}
```

- [ ] **Step 4.3: Verify**

```bash
cd /Users/makar/dev/kazka/frontend && node_modules/.bin/tsc --noEmit && npm run lint
```

Expected: both pass.

Visual smoke test: `npm run dev`, click "Create story" and submit. The loading panel should be a square ~360×360px, centered, with the spinning sun and the "Створюємо казку..." label centered. No top gradient strip, no ornament, no close button.

- [ ] **Step 4.4: Commit**

```bash
cd /Users/makar/dev/kazka
git add frontend/src/components/modal/StoryModal.module.css
git commit -m "$(cat <<'EOF'
style(frontend): square loading card for StoryModal creating phase

Renames .streaming to .creating to reflect the broader scope
(text + illustration), and adds a .panelSquare modifier so the
modal collapses to a 360x360 square during creation.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Book-style paragraph rendering

**Files:**
- Modify: `frontend/src/pages/StoryDetailPage.tsx`
- Modify: `frontend/src/pages/StoryDetailPage.module.css`

- [ ] **Step 5.1: Render content as parsed paragraphs**

In `frontend/src/pages/StoryDetailPage.tsx`, find the read-mode content render:

```tsx
            ) : (
              <p className={styles.content}>{story.content}</p>
            )}
```

Replace with:

```tsx
            ) : (
              <div className={styles.content}>
                {story.content
                  .split(/\n\s*\n+/)
                  .map(p => p.trim())
                  .filter(Boolean)
                  .map((para, i) => (
                    <p key={i}>{para}</p>
                  ))}
              </div>
            )}
```

(Edit mode — the `<textarea>` — stays untouched.)

- [ ] **Step 5.2: Update `.content` styles**

In `frontend/src/pages/StoryDetailPage.module.css`, find the existing `.content` rule (lines ~86-93):

```css
.content {
  font-family: var(--font-display);
  font-size: 1.0625rem;
  line-height: 1.85;
  color: var(--color-text);
  white-space: pre-wrap;
  margin-bottom: var(--spacing-8);
}
```

Replace with:

```css
.content {
  font-family: var(--font-display);
  font-size: 1.0625rem;
  line-height: 1.85;
  color: var(--color-text);
  margin-bottom: var(--spacing-8);
}

.content p {
  text-indent: 2em;
  margin: 0;
}

.content p:first-child {
  text-indent: 0;
}
```

(`white-space: pre-wrap` is removed — paragraphs are now real `<p>` elements.)

- [ ] **Step 5.3: Verify**

```bash
cd /Users/makar/dev/kazka/frontend && node_modules/.bin/tsc --noEmit && npm run lint
```

Expected: both pass.

Visual smoke test: `npm run dev`, open any existing story (`/stories/<id>`). Body text should render with each paragraph indented (~2em) on its first line, no blank lines between paragraphs, opening paragraph flush left.

If a real story has no blank lines between paragraphs (single `\n` only), the parser will treat the whole story as one paragraph. Inspect in DevTools — if you see this happen on real data, fall back to splitting on `/\n+/` instead of `/\n\s*\n+/` and re-verify.

- [ ] **Step 5.4: Commit**

```bash
cd /Users/makar/dev/kazka
git add frontend/src/pages/StoryDetailPage.tsx frontend/src/pages/StoryDetailPage.module.css
git commit -m "$(cat <<'EOF'
style(frontend): book-style paragraph rendering on StoryDetailPage

Story body now parses into real <p> elements, with first-line
indent (~2em) and no margin between paragraphs — reads like a
printed book. Opening paragraph stays flush-left per book
typography convention.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Final verification

- [ ] **Final type-check + lint**

```bash
cd /Users/makar/dev/kazka/frontend && node_modules/.bin/tsc --noEmit && npm run lint
```

Expected: both pass.

- [ ] **Final visual sweep**

`npm run dev`, then walk through:

1. Hero on `/` in Ukrainian — headings render in Manrope (consistent Cyrillic/Latin proportions).
2. Toggle to English — headings render in Nunito (no visible change vs before).
3. Click "Create story", fill the form, submit — loading shows a 360×360 square with spinning sun + "Створюємо казку..." (no streaming text). After ~10–60s, modal closes and detail page loads with story + illustration both ready.
4. On `/stories/<new-id>` — title is 2–4 words, body text reads as indented book-style paragraphs.
5. On `/stories` (archive) — any long legacy title is clamped to 2 lines.

- [ ] **Push (optional)**

If the user requests a PR or push, do so explicitly. Otherwise leave the 5 commits on `main` (or whichever branch you're on).
