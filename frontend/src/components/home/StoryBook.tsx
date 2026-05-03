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
const HTMLFlipBook = lazy(async () => {
  const mod = await import('react-pageflip')
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
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
}

const AUTO_ADVANCE_MS = 6000
const PORTRAIT_BREAKPOINT_PX = 860
const PAGE_WIDTH_PX = 360
const PAGE_HEIGHT_PX = 460

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

  return (
    <div
      ref={sectionRef}
      className={styles.wrap}
      role="region"
      aria-roledescription="storybook"
      aria-label={t.howItWorks.title}
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
          aria-label={t.howItWorks.prevAria}
        >‹</button>

        <div className={styles.dots}>
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
