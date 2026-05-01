import { useEffect, useState } from 'react'
import { useReveal } from '../../lib/useReveal'
import { SectionParticles } from './SectionParticles'
import styles from './StoryPreview.module.css'

const STORY_FULL = "авним-давно, у самому серці Зачарованого лісу, жила маленька зірочка на ім'я Мія. Вона не світила на небі, як інші зірки — натомість мешкала у дуплі старезного дуба і щоночі вирушала у подорож стежками, вкритими сріблястим мохом."

export function StoryPreview() {
  const { ref: headRef, visible: headVisible } = useReveal()
  const { ref: bookRef, visible: bookVisible } = useReveal({ threshold: 0.3 })
  const [typed, setTyped] = useState('')
  const [typeStarted, setTypeStarted] = useState(false)
  const [done, setDone] = useState(false)

  useEffect(() => {
    if (!bookVisible || typeStarted) return
    setTypeStarted(true)
    if (window.innerWidth < 640) {
      setTyped(STORY_FULL)
      setDone(true)
      return
    }
    let idx = 0
    const interval = setInterval(() => {
      if (idx < STORY_FULL.length) {
        setTyped(STORY_FULL.slice(0, idx + 1))
        idx++
      } else {
        clearInterval(interval)
        setDone(true)
      }
    }, 22)
    return () => clearInterval(interval)
  }, [bookVisible, typeStarted])

  return (
    <section className={styles.section} id="preview">
      <SectionParticles light />
      <div className={styles.inner}>
        <div ref={headRef} className={`reveal ${headVisible ? 'visible' : ''}`}>
          <div className={styles.label}>Приклад казки</div>
          <div className={styles.title}>
            Ось яка казка може чекати<br />сьогодні ввечері
          </div>
        </div>

        <div
          ref={bookRef}
          className={`${styles.bookSpread} reveal ${bookVisible ? 'visible' : ''} ${bookVisible ? styles.bookVisible : ''}`}
        >
          <div className={styles.bookLeft}>
            <div className={styles.storyText}>
              <span className={styles.dropCap}>Д</span>
              {typed}
              {!done && <span className={styles.cursor} aria-hidden="true" />}
            </div>
            <div className={`${styles.pageNum} ${styles.pageNumLeft}`}>3</div>
          </div>

          <div className={styles.bookRight}>
            <div className={styles.illustHero} />
            <span className={styles.illustCaption}>✦ Ілюстрація згенерована AI</span>
            <div className={styles.pageNumRight}>4</div>
          </div>
        </div>

        <div className={`${styles.tagline} reveal ${headVisible ? 'visible' : ''}`}>
          Кожна казка — унікальна. Жодного повторення.
        </div>
      </div>
    </section>
  )
}
