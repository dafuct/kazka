import { useEffect, useRef, useState } from 'react'
import { useReveal } from '../../lib/useReveal'
import { useLocale } from '../../lib/LocaleContext'
import { SectionParticles } from './SectionParticles'
import styles from './HowItWorks.module.css'
import { IllustrationCarousel } from '../illustrations/IllustrationCarousel'

const STEP_REVEAL_CLASSES = ['revealLeft', 'revealRight', 'revealLeft']

interface StepItemProps {
  step: { num: string; stepLabel: string; title: string; desc: string }
  revealClass: string
  index: number
}

function StepItem({ step, revealClass, index }: StepItemProps) {
  const ref = useRef<HTMLDivElement>(null)
  const [visible, setVisible] = useState(false)
  const [numFlipped, setNumFlipped] = useState(false)
  const [lineDrawn, setLineDrawn] = useState(false)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    const obs = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true)
          setTimeout(() => setNumFlipped(true), 100)
          setTimeout(() => setLineDrawn(true), 400)
          obs.unobserve(el)
        }
      },
      { threshold: 0.15, rootMargin: '0px 0px -40px 0px' }
    )
    obs.observe(el)
    return () => obs.disconnect()
  }, [])

  return (
    <div
      ref={ref}
      className={`${styles.step} ${revealClass} ${visible ? 'visible' : ''}`}
      style={{ transitionDelay: `${index * 0.1}s` }}
    >
      <div className={styles.stepNumWrap}>
        <div className={`${styles.stepNum} ${numFlipped ? styles.flipped : ''}`}>
          {step.num}
        </div>
      </div>
      <div className={styles.stepContent}>
        <div className={styles.stepLabel}>{step.stepLabel}</div>
        <h3 className={styles.stepTitle}>{step.title}</h3>
        <p className={styles.stepDesc}>{step.desc}</p>
      </div>
      {index < STEP_REVEAL_CLASSES.length - 1 && (
        <div className={`${styles.stepLine} ${lineDrawn ? styles.drawn : ''}`} />
      )}
    </div>
  )
}

export function HowItWorks() {
  const { t } = useLocale()
  const { ref: headRef, visible: headVisible } = useReveal()
  const { ref: illustRef, visible: illustVisible } = useReveal()

  return (
    <section className={styles.section} id="how">
      <SectionParticles />
      <div className={styles.bgIllust} aria-hidden="true">
        <svg viewBox="0 0 300 400" fill="none" xmlns="http://www.w3.org/2000/svg" width="100%" height="100%">
          <circle cx="200" cy="80" r="60" fill="url(#howMG)" opacity="0.4"/>
          <path d="M215 55 A35 35 0 1 0 215 105 A25 25 0 1 1 215 55Z" fill="#EDD9A3" opacity="0.5"/>
          <path d="M0 120 C40 110 60 140 100 125 C120 118 130 130 150 120" stroke="#6B4C3B" strokeWidth="1.2" fill="none" opacity="0.25"/>
          <path d="M100 125 C105 105 115 95 125 80" stroke="#6B4C3B" strokeWidth="0.8" fill="none" opacity="0.2"/>
          <ellipse cx="125" cy="78" rx="5" ry="10" transform="rotate(-20 125 78)" fill="#166534" opacity="0.15"/>
          <path d="M20 400 C25 350 35 300 30 250 C28 220 20 180 0 150" stroke="#6B4C3B" strokeWidth="2" fill="none" opacity="0.15"/>
          <circle cx="80" cy="200" r="3" fill="#F59E0B" opacity="0.5">
            <animate attributeName="opacity" values="0.5;0.15;0.5" dur="3s" repeatCount="indefinite"/>
          </circle>
          <circle cx="180" cy="260" r="2.5" fill="#EDD9A3" opacity="0.45">
            <animate attributeName="opacity" values="0.45;0.1;0.45" dur="4s" repeatCount="indefinite"/>
          </circle>
          <path d="M250 40L251 36L255 38L251 35L250 31L249 35L245 33L249 36Z" fill="#E6C77A" opacity="0.5">
            <animate attributeName="opacity" values="0.5;0.2;0.5" dur="4s" repeatCount="indefinite"/>
          </path>
          <defs>
            <radialGradient id="howMG" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0%" stopColor="#EDD9A3" stopOpacity="0.35"/>
              <stop offset="100%" stopColor="#EDD9A3" stopOpacity="0"/>
            </radialGradient>
          </defs>
        </svg>
      </div>
      <div className={styles.inner}>
        <div ref={headRef} className={`reveal ${headVisible ? 'visible' : ''}`}>
          <div className={styles.label}>{t.howItWorks.label}</div>
          <div className={styles.title}>{t.howItWorks.title}</div>
        </div>

        <div className={styles.layout}>
          <div className={styles.steps}>
            {t.howItWorks.steps.map((step, i) => (
              <StepItem key={i} step={step} revealClass={STEP_REVEAL_CLASSES[i]} index={i} />
            ))}
          </div>

          <div
            ref={illustRef}
            className={`${styles.illustWrap} reveal ${illustVisible ? 'visible' : ''}`}
          >
            <IllustrationCarousel section="how" />
          </div>
        </div>
      </div>
    </section>
  )
}
