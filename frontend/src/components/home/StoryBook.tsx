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
