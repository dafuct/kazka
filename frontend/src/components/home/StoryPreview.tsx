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
