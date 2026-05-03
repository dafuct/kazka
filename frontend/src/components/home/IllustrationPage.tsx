import { forwardRef, useEffect, useState } from 'react'
import styles from './StoryBook.module.css'

export interface IllustrationPageProps {
  src: string
  /** Optional page number rendered in the bottom-right corner overlay. */
  pageNum?: number
}

export const IllustrationPage = forwardRef<HTMLDivElement, IllustrationPageProps>(
  function IllustrationPage({ src, pageNum }, ref) {
    const [loaded, setLoaded] = useState(false)

    // Reset loading state when src changes (theme toggle, etc.)
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
          onError={() => setLoaded(true)}
          loading="eager"
          decoding="async"
        />
        {pageNum !== undefined && (
          <div className={`${styles.pageNumber} ${styles.pageNumberRight}`} aria-hidden="true">
            {pageNum}
          </div>
        )}
      </div>
    )
  }
)
