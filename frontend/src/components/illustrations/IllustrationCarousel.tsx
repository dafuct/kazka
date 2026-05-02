import { useEffect, useRef, useState } from 'react'
import { useTheme } from '../../lib/ThemeContext'
import { useLocale } from '../../lib/LocaleContext'
import { subscribeCarouselTick } from './carouselTickStore'
import styles from './IllustrationCarousel.module.css'

const AGE_KEYS = ['3-5', '6-8', '9-12'] as const
type AgeKey = (typeof AGE_KEYS)[number]

interface Props {
  section: 'hero' | 'how' | 'preview'
  width: number
  height: number
  className?: string
  intervalMs?: number
}

export function IllustrationCarousel({ section, width, height, className, intervalMs }: Props) {
  const { theme } = useTheme()
  const { t } = useLocale()
  const [ageIndex, setAgeIndex] = useState(0)
  const [manual, setManual] = useState(false)
  const [fading, setFading] = useState(false)
  const fadeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const age: AgeKey = AGE_KEYS[ageIndex]
  const src = `/illustrations/${section}-${age}-${theme}.png`

  useEffect(() => {
    if (manual) return
    if (typeof window !== 'undefined' &&
        window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
      return
    }
    return subscribeCarouselTick(() => {
      setFading(true)
      if (fadeTimerRef.current) clearTimeout(fadeTimerRef.current)
      fadeTimerRef.current = setTimeout(() => {
        setAgeIndex((i) => (i + 1) % AGE_KEYS.length)
        setFading(false)
      }, 250)
    }, intervalMs)
  }, [manual, intervalMs])

  // Preload the other ages for the current theme
  useEffect(() => {
    AGE_KEYS.forEach((a, i) => {
      if (i === ageIndex) return
      const img = new Image()
      img.src = `/illustrations/${section}-${a}-${theme}.png`
    })
  }, [section, theme, ageIndex])

  const onTabClick = (i: number) => {
    setManual(true)
    setFading(true)
    if (fadeTimerRef.current) clearTimeout(fadeTimerRef.current)
    fadeTimerRef.current = setTimeout(() => {
      setAgeIndex(i)
      setFading(false)
    }, 200)
  }

  const ageLabel = t.form.ageGroups[age]

  return (
    <div className={`${styles.wrap} ${className ?? ''}`}>
      <div className={styles.frame} style={{ width, height }} role="img" aria-label={`${ageLabel} drawing`}>
        <img
          src={src}
          alt={`${ageLabel} child drawing`}
          className={`${styles.img} ${fading ? styles.imgFading : ''}`}
          loading="eager"
          decoding="async"
        />
        <span className={styles.ageBadge} aria-hidden="true">{ageLabel}</span>
      </div>
      <div className={styles.tabs} role="tablist">
        {AGE_KEYS.map((a, i) => (
          <button
            key={a}
            role="tab"
            aria-pressed={i === ageIndex}
            className={`${styles.tab} ${i === ageIndex ? styles.tabActive : ''}`}
            onClick={() => onTabClick(i)}
          >
            {t.form.ageGroups[a]}
          </button>
        ))}
      </div>
    </div>
  )
}
