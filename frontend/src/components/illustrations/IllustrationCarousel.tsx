import { useEffect, useRef, useState } from 'react'
import { useTheme } from '../../lib/ThemeContext'
import { useLocale } from '../../lib/LocaleContext'
import { subscribeCarouselTick } from './carouselTickStore'
import styles from './IllustrationCarousel.module.css'

const AGE_KEYS = ['3-5', '6-8', '9-12'] as const

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
  const [fading, setFading] = useState(false)
  const fadeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const age = AGE_KEYS[ageIndex]
  const src = `/illustrations/${section}-${age}-${theme}.png`

  useEffect(() => {
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
  }, [intervalMs])

  useEffect(() => {
    AGE_KEYS.forEach((a, i) => {
      if (i === ageIndex) return
      const img = new Image()
      img.src = `/illustrations/${section}-${a}-${theme}.png`
    })
  }, [section, theme, ageIndex])

  useEffect(() => () => {
    if (fadeTimerRef.current) clearTimeout(fadeTimerRef.current)
  }, [])

  return (
    <div className={`${styles.wrap} ${className ?? ''}`}>
      <div className={styles.frame} style={{ width, height }} role="img" aria-label={t.form.ageGroups[age]}>
        <img
          src={src}
          alt=""
          className={`${styles.img} ${fading ? styles.imgFading : ''}`}
          loading="eager"
          decoding="async"
        />
      </div>
    </div>
  )
}
