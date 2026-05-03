import { forwardRef, useEffect, useState } from 'react'
import { useTheme } from '../../lib/ThemeContext'
import styles from './StoryBook.module.css'

export interface IllustrationPageProps {
  step: 1 | 2 | 3
}

export const IllustrationPage = forwardRef<HTMLDivElement, IllustrationPageProps>(
  function IllustrationPage({ step }, ref) {
    const { theme } = useTheme()
    const src = `/illustrations/how-step${step}-${theme}.png`
    const [loaded, setLoaded] = useState(false)

    // Reset loading state when src changes (theme toggle).
    useEffect(() => {
      setLoaded(false)
    }, [src])

    return (
      <div ref={ref} className={`${styles.page} ${styles.pageIllust}`}>
        <img
          src={src}
          alt=""
          className={`${styles.illustImg} ${loaded ? styles.illustImgLoaded : ''}`}
          onLoad={() => setLoaded(true)}
          loading="eager"
          decoding="async"
        />
      </div>
    )
  }
)
