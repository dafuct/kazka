# Frontend Polish Pass — Design

**Date:** 2026-05-03
**Scope:** Five small, independent UI/UX changes packaged as one polish pass.
**Surface area:** Frontend (React + CSS) + two backend prompt files.

---

## Goals

1. Make Ukrainian display headers visually consistent with the English Nunito look.
2. Replace the in-modal text streaming with a single quiet loading state; only navigate to the story page once both text and illustration are fully ready.
3. Generate shorter, punchier story titles (and stop existing long titles from breaking the UI).
4. Shrink the loading-state modal to a square card.
5. Render story body text in a book-style layout: paragraphs separated by first-line indent, no blank line between them.

---

## 1. Ukrainian display font swap

### Problem

Both languages declare `--font-display: 'Nunito'`, but the Cyrillic and Latin glyph sets in the bundled Nunito files don't feel visually consistent. The user prefers the English (Latin) Nunito look.

### Approach

Self-host **Manrope** (weights 400, 600, 700) under `frontend/public/fonts/manrope/`. Manrope's Cyrillic and Latin glyphs are designed together by the same foundry, so they share the same proportions, weight, and spacing.

Use Manrope **only** for `--font-display` when Ukrainian is the active language. Body font stays Nunito for both languages (only display headers change).

### Implementation

1. **Download Manrope woff2 files** for weights 400, 600, 700 (latin + cyrillic subsets, total ~6 files) into `frontend/public/fonts/manrope/`.
2. **`src/design/global.css`** — add `@font-face` declarations for each weight, latin and cyrillic unicode-ranges (mirroring the Nunito declaration style already in the file).
3. **`src/design/tokens.css`** — add a sibling rule:
   ```css
   html[data-lang="uk"] {
     --font-display: 'Manrope', 'Nunito', 'Helvetica Neue', sans-serif;
   }
   ```
4. **`src/lib/LocaleContext.tsx`** — inside `LocaleProvider`, add a `useEffect` that runs on mount and whenever `lang` changes:
   ```ts
   useEffect(() => {
     document.documentElement.dataset.lang = lang
   }, [lang])
   ```
   (Add `useEffect` to the existing react import.)
5. **No component changes.** Every place that uses `var(--font-display)` picks up the swap automatically.

### Risks / notes

- File size: 6 woff2 files × ~15-25KB each = ~120KB extra payload, only loaded when Ukrainian is active (per unicode-range subsetting).
- If Manrope's Cyrillic feel is still off when the user previews it, plan B is Rubik (similar warmth, slightly more geometric).

---

## 2. Modal-only loading until both text + image ready

### Problem

Today: form → click Create → modal shows streaming text + spinner → on text done, fire-and-forget `api.illustrate(id)` and navigate immediately → detail page polls every 3s for the illustration to appear (so user sees a loading placeholder for ~10–30s on the detail page).

User wants: a single quiet loading state in the modal, no streaming text, navigate **only** when both text and illustration are fully ready.

### Approach

Refactor the streaming phase of `StoryModal` into a "creating" phase that:
- runs the same `streamStory` call as today, but discards the token stream (keeps the SSE infra exactly as is — no API change)
- on stream `onDone({id, title})`, calls `api.illustrate(id)` and starts polling `api.getStory(id)` every ~2s
- exits the loop when `illustrationStatus !== 'PENDING'` (i.e., `READY` or `FAILED`)
- closes the modal and navigates to `/stories/${id}`
- has a 60s safety cap for the polling — if exceeded, navigate anyway (detail page already handles `FAILED` and `null`)

`StoryStream` component is no longer used by the modal. Leave it in the codebase for now (it's small, no other consumers); a follow-up cleanup can delete it if it stays orphaned.

`StoryDetailPage` removes its own illustration polling block (no longer needed since the story is loaded fresh after navigation, with the illustration already complete or failed). The manual "Generate illustration" button stays for re-illustration.

### Implementation

**`src/components/modal/StoryModal.tsx`:**
- Remove `StoryStream` import and the `<StoryStream ...>` JSX.
- Add a small inline subscriber that calls `streamStory` with `onToken` as a no-op, `onDone` as the handler that triggers illustration + polling.
- Polling helper: `await pollUntilReady(id, { intervalMs: 2000, timeoutMs: 60000 })`. Helper can live in `StoryModal.tsx` or `lib/apiClient.ts` — prefer keeping it in the modal (single consumer).
- Cancel polling on unmount / modal close.

**`src/pages/StoryDetailPage.tsx`:**
- Remove the `useEffect` that polls when `story.illustrationStatus === 'PENDING'` (lines ~37-48).

### Risks / notes

- Total wait may be 20–60s. The new design accepts this — the loading card is calm and a brief message ("Створюємо казку...") signals progress. No mid-flight progress bar requested.
- If the user closes the browser mid-creation, the story still exists in the DB (text was saved when stream completed). Same behavior as today.

---

## 3. Shorter generated titles

### Problem

The LLM sometimes returns long, multi-clause titles ("Маленька зірочка Мія та її подорож зачарованим лісом"). They look bad in the StoryCard grid and the detail page header.

### Approach

Constrain via the system prompt — the right place to fix it. Add a belt-and-suspenders CSS truncation for any pre-existing long titles already in the DB.

### Implementation

**`backend/src/main/resources/prompts/story-system.txt`** — already constrains title to "3–6 words". Tighten to "2–4 words":
- Change `Line 1: a short book-style title (3–6 words, no punctuation at the end, no quotes, no "Title:" prefix)`
- To: `Line 1: a short book-style title (2–4 words maximum, no punctuation at the end, no quotes, no colons, no subtitles, no "Title:" prefix)`

(`system-uk.txt` and `system-en.txt` contain language quality rules only — no title format — leave them alone.)

**`frontend/src/components/story/StoryCard.module.css`** — add to the title rule:
```css
overflow-wrap: break-word;
display: -webkit-box;
-webkit-line-clamp: 2;
-webkit-box-orient: vertical;
overflow: hidden;
```

**`frontend/src/pages/StoryDetailPage.module.css`** — add to `.title`:
```css
max-width: 100%;
overflow-wrap: break-word;
```

### Risks / notes

- Prompt change won't retroactively fix existing stories — that's why CSS truncation matters.
- If the model occasionally still over-runs, it's bounded to 2 lines visually.

---

## 4. Square loading card

### Problem

The modal panel is `max-width: 680px`. During text streaming it feels too wide for what's now just an icon + a title.

### Approach

Add a `.streaming` modifier class to the panel; override max-width and force a square aspect.

### Implementation

**`src/components/modal/StoryModal.tsx`:**
- Conditionally apply `styles.panelSquare` to the `.panel` element when `phase === 'streaming'`.
- The existing `styles.streaming` (on the inner wrapper div) stays as-is — distinct class name avoids collision with the new panel modifier.

**`src/components/modal/StoryModal.module.css`:**
```css
.panel.panelSquare {
  max-width: 360px;
  width: 360px;
  min-height: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.panel.panelSquare .topBorder,
.panel.panelSquare .header { display: none; }
.panel.panelSquare .body { padding: 24px; text-align: center; }

@media (max-width: 720px) {
  .panel.panelSquare { width: 90vw; max-width: 360px; min-height: 90vw; }
}
```

---

## 5. Book-style paragraph formatting

### Problem

Story content is rendered as `<p style="white-space: pre-wrap">{story.content}</p>` — newlines from the LLM render as visible blank lines, no first-line indent. Doesn't read like a printed book.

### Approach

Parse the content into paragraphs (split on blank lines), render each as `<p>` with first-line indent and zero margin between them. The opening paragraph is flush-left (book convention).

### Implementation

**`src/pages/StoryDetailPage.tsx`** — replace:
```tsx
<p className={styles.content}>{story.content}</p>
```
with:
```tsx
<div className={styles.content}>
  {story.content.split(/\n\s*\n+/).filter(Boolean).map((para, i) => (
    <p key={i}>{para.trim()}</p>
  ))}
</div>
```

**`src/pages/StoryDetailPage.module.css`** — replace `.content`:
```css
.content {
  font-family: var(--font-display);
  font-size: 1.0625rem;
  line-height: 1.85;
  color: var(--color-text);
  margin-bottom: var(--spacing-8);
}
.content p { text-indent: 2em; margin: 0; }
.content p:first-child { text-indent: 0; }
```

### Risks / notes

- If the LLM uses single newlines (not blank lines) between paragraphs, the splitter will treat the whole story as one paragraph. Inspect a few real outputs during implementation; if needed, fall back to splitting on `/\n+/`.
- Edit mode (`<textarea>`) is unchanged — still uses raw text with newlines.

---

## File summary

**Frontend:**
- `frontend/public/fonts/manrope/` — new directory with woff2 files (6 files)
- `frontend/src/design/global.css` — `@font-face` for Manrope (modify)
- `frontend/src/design/tokens.css` — `html[data-lang="uk"]` rule (modify)
- `frontend/src/lib/LocaleContext.tsx` — set `data-lang` on `<html>` (modify)
- `frontend/src/components/modal/StoryModal.tsx` — refactor streaming phase (modify)
- `frontend/src/components/modal/StoryModal.module.css` — square panel + clean-up (modify)
- `frontend/src/pages/StoryDetailPage.tsx` — paragraph parsing + remove polling (modify)
- `frontend/src/pages/StoryDetailPage.module.css` — paragraph styles + title clamp (modify)
- `frontend/src/components/story/StoryCard.module.css` — title clamp (modify)

**Backend:**
- `backend/src/main/resources/prompts/story-system.txt` — tighten title length cap (modify)

**No new components, no new tests, no API changes, no DB migrations.**

---

## Out of scope

- Refactoring `StoryStream` away (leave in place; will become orphan after this change — separate cleanup PR).
- Editor view changes (textarea unchanged).
- Mid-creation progress indicators (calm sun + label only).
- Style changes to the homepage hero, archive, or features sections.
