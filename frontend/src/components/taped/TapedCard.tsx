import type { CSSProperties, ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { cardRotation } from './rotation'
import styles from './TapedCard.module.css'

interface TapedCardProps {
  /** Stable id the tilt is derived from (story id, slot name, …). */
  rotationKey: string | number
  cover?: string | null
  coverAlt?: string
  coverHeight?: number
  coverAspect?: string
  title?: ReactNode
  meta?: ReactNode
  to?: string
  onClick?: () => void
  /** Custom photo-frame content (e.g. ComicsReader) instead of the cover img. */
  children?: ReactNode
  className?: string
}

export function TapedCard({
  rotationKey, cover, coverAlt = '', coverHeight, coverAspect,
  title, meta, to, onClick, children, className,
}: TapedCardProps) {
  const { rot, trot } = cardRotation(rotationKey)
  const style = { '--rot': rot, '--trot': trot } as CSSProperties
  const photoStyle: CSSProperties = {}
  if (coverHeight != null) photoStyle.height = coverHeight
  if (coverAspect != null) photoStyle.aspectRatio = coverAspect

  const body = (
    <>
      <span className={styles.tape} aria-hidden="true" />
      <div className={styles.photo} style={photoStyle}>
        {children ??
          (cover ? (
            <img src={cover} alt={coverAlt} loading="lazy" />
          ) : (
            <div className={styles.fallback} aria-hidden="true">✦</div>
          ))}
      </div>
      {title != null && <h3 className={styles.title}>{title}</h3>}
      {meta != null && <div className={styles.meta}>{meta}</div>}
    </>
  )

  const cls = className ? `${styles.taped} ${className}` : styles.taped
  if (to) {
    return <Link to={to} className={cls} style={style}>{body}</Link>
  }
  return <div className={cls} style={style} onClick={onClick}>{body}</div>
}
