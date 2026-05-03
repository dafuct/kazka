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
