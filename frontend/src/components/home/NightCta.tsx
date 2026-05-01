import { useEffect, useRef } from 'react'
import { useReveal } from '../../lib/useReveal'
import { handleRipple } from '../../lib/ripple'
import { useStoryModal } from '../../lib/StoryModalContext'
import styles from './NightCta.module.css'

function StarLayer() {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const layer = ref.current
    if (!layer) return
    for (let i = 0; i < 80; i++) {
      const s = document.createElement('div')
      s.className = styles.star
      const size = [1, 1.5, 2, 3][Math.floor(Math.random() * 4)]
      const dur = size > 2 ? 1 + Math.random() : 2 + Math.random() * 3
      s.style.cssText = `left:${Math.random() * 100}%;top:${Math.random() * 100}%;width:${size}px;height:${size}px;--dur:${dur}s;--delay:${Math.random() * 5}s;`
      layer.appendChild(s)
    }
  }, [])
  return <div ref={ref} className={styles.starLayer} aria-hidden="true" />
}

export function NightCta() {
  const { openModal } = useStoryModal()
  const { ref: r1, visible: v1 } = useReveal()
  const { ref: r2, visible: v2 } = useReveal()
  const { ref: r3, visible: v3 } = useReveal()
  const { ref: r4, visible: v4 } = useReveal()

  return (
    <section className={styles.section}>
      <div className={styles.bg}>
        <div className={styles.illustSky} />
      </div>
      <StarLayer />
      <div className={styles.moonGlow} aria-hidden="true" />
      <div className={styles.shootingStar} aria-hidden="true" />

      <div className={styles.content}>
        <h2 ref={r1} className={`reveal ${v1 ? 'visible' : ''} ${styles.heading}`}>
          Починайте казку<br />цього вечора
        </h2>
        <p ref={r2} className={`reveal ${v2 ? 'visible' : ''} ${styles.sub}`}>
          Одна казка — і ваша дитина попросить ще
        </p>
        <a
          ref={r3}
          href="#"
          className={`reveal ${v3 ? 'visible' : ''} ${styles.btn}`}
          onClick={(e) => { e.preventDefault(); openModal(); handleRipple(e) }}
        >
          Створити першу казку — безкоштовно
        </a>
        <p ref={r4} className={`reveal ${v4 ? 'visible' : ''} ${styles.fine}`}>
          Без реєстрації · Перша казка миттєво
        </p>
      </div>
    </section>
  )
}
