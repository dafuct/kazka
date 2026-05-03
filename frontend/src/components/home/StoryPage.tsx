import { forwardRef } from 'react'
import styles from './StoryBook.module.css'

export interface StoryPageProps {
  num: string         // 'I' | 'II' | 'III' from locale
  stepLabel: string   // e.g. 'Step one'
  title: string
  desc: string
  pageId: string      // for aria-labelledby (e.g. 'storybook-step-1')
}

export const StoryPage = forwardRef<HTMLDivElement, StoryPageProps>(
  function StoryPage({ num, stepLabel, title, desc, pageId }, ref) {
    return (
      <div ref={ref} className={`${styles.page} ${styles.pageStory}`}>
        <div className={styles.pageInner}>
          <div className={styles.stepLabel}>{stepLabel}</div>
          <h3 id={pageId} className={styles.stepTitle}>{title}</h3>
          <p className={styles.stepDesc}>{desc}</p>
        </div>
        <div className={styles.pageNumber} aria-hidden="true">{num}</div>
      </div>
    )
  }
)
