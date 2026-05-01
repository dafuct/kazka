import { useReveal } from '../../lib/useReveal'
import { SectionParticles } from './SectionParticles'
import styles from './Features.module.css'

export function Features() {
  const { ref: headRef, visible: headVisible } = useReveal()
  const { ref: c1, visible: v1 } = useReveal({ threshold: 0.1 })
  const { ref: c2, visible: v2 } = useReveal({ threshold: 0.1 })
  const { ref: c3, visible: v3 } = useReveal({ threshold: 0.1 })
  const { ref: c4, visible: v4 } = useReveal({ threshold: 0.1 })

  return (
    <section className={styles.section} id="features">
      <SectionParticles />
      <div className={styles.bgIllust} aria-hidden="true">
        <svg viewBox="0 0 300 400" fill="none" xmlns="http://www.w3.org/2000/svg" width="100%" height="100%">
          <circle cx="200" cy="80" r="60" fill="url(#featMG)" opacity="0.4"/>
          <path d="M215 55 A35 35 0 1 0 215 105 A25 25 0 1 1 215 55Z" fill="#EDD9A3" opacity="0.5"/>
          <path d="M0 120 C40 110 60 140 100 125" stroke="#6B4C3B" strokeWidth="1.2" fill="none" opacity="0.25"/>
          <ellipse cx="125" cy="78" rx="5" ry="10" transform="rotate(-20 125 78)" fill="#166534" opacity="0.15"/>
          <path d="M20 400 C25 350 35 300 30 250 C28 220 20 180 0 150" stroke="#6B4C3B" strokeWidth="2" fill="none" opacity="0.15"/>
          <circle cx="80" cy="200" r="3" fill="#F59E0B" opacity="0.5">
            <animate attributeName="opacity" values="0.5;0.15;0.5" dur="3s" repeatCount="indefinite"/>
          </circle>
          <path d="M250 40L251 36L255 38L251 35L250 31L249 35L245 33L249 36Z" fill="#C4B5FD" opacity="0.5">
            <animate attributeName="opacity" values="0.5;0.2;0.5" dur="4s" repeatCount="indefinite"/>
          </path>
          <defs>
            <radialGradient id="featMG" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0%" stopColor="#EDD9A3" stopOpacity="0.35"/>
              <stop offset="100%" stopColor="#EDD9A3" stopOpacity="0"/>
            </radialGradient>
          </defs>
        </svg>
      </div>
      <div className={styles.inner}>
        <div ref={headRef} className={`reveal ${headVisible ? 'visible' : ''}`}>
          <div className={styles.label}>Можливості</div>
          <div className={styles.title}>Що робить Казкар особливим</div>
        </div>

        <div className={styles.bento}>
          {/* Large card */}
          <div ref={c1} className={`${styles.card} ${styles.cardLarge} reveal ${v1 ? 'visible' : ''}`}>
            <div className={styles.bgImage}>
              <div className={styles.illustCozy} />
            </div>
            <div className={styles.largeText}>
              <h3>Живий світ вашої дитини</h3>
              <p>Штучний інтелект пам'ятає всіх персонажів, улюблені теми та події між казками. Кожна нова історія — продовження магічного всесвіту, який належить лише вашій дитині.</p>
            </div>
          </div>

          {/* Medium card */}
          <div ref={c2} className={`${styles.card} ${styles.cardMedium} reveal ${v2 ? 'visible' : ''}`}>
            <div className={styles.mediumText}>
              <h3>Казка знає ваш день</h3>
              <p>Погода, пора року, настрій дитини та події дня м'яко вплітаються в сюжет. Казка стає дзеркалом реального життя малюка.</p>
            </div>
            <div className={styles.mediumImg}>
              <div className={styles.illustForest} />
            </div>
          </div>

          {/* Text-only card */}
          <div ref={c3} className={`${styles.card} ${styles.cardText} reveal ${v3 ? 'visible' : ''}`}>
            <div className={styles.dropInitial}>Р</div>
            <h3>Різні оповідачі</h3>
            <p>Щовечора — нова оповідальна особистість. Тихий лісовий мудрець, весела зірочка-мандрівниця або добрий вітер із далеких країв. Кожен голос — неповторний.</p>
          </div>

          {/* Image card */}
          <div ref={c4} className={`${styles.card} ${styles.cardImg} reveal ${v4 ? 'visible' : ''}`}>
            <div className={styles.bgImage}>
              <div className={styles.illustBook} />
            </div>
            <div className={styles.imgText}>
              <h3>Архів на роки</h3>
              <p>Магічний щоденник дитинства</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
