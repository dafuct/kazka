import { useState } from 'react'
import { PlaceholderSvg } from './PlaceholderSvg'
import type { IllustrationStatus } from '../../lib/types'
import styles from './IllustrationFrame.module.css'

interface IllustrationFrameProps {
  path: string | null
  status: IllustrationStatus
}

export function IllustrationFrame({ path, status }: IllustrationFrameProps) {
  const [imgError, setImgError] = useState(false)

  if (status === 'READY' && path && !imgError) {
    return (
      <div className={styles.frame}>
        <img
          src={path}
          alt="Ілюстрація казки"
          className={styles.img}
          onError={() => setImgError(true)}
        />
      </div>
    )
  }

  if (status === 'PENDING') {
    return (
      <div className={styles.frame}>
        <div className={styles.skeleton}>
          <PlaceholderSvg />
          <div className={styles.shimmer} />
        </div>
      </div>
    )
  }

  return (
    <div className={styles.frame}>
      <PlaceholderSvg />
    </div>
  )
}
