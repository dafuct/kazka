import { useMemo } from 'react'
import styles from './StarField.module.css'

const rnd = (a: number, b: number) => a + Math.random() * (b - a)

/** Fixed night-sky backdrop: 42 twinkling ✦ glyphs behind all content. */
export function StarField() {
  const stars = useMemo(
    () =>
      Array.from({ length: 42 }, () => ({
        left: rnd(0, 100),
        top: rnd(0, 100),
        size: rnd(8, 20),
        dur: rnd(3, 7),
        delay: rnd(0, 5),
      })),
    [],
  )
  return (
    <div className={styles.atmos} aria-hidden="true">
      {stars.map((s, i) => (
        <span
          key={i}
          className={styles.star}
          style={{
            left: `${s.left}%`,
            top: `${s.top}%`,
            fontSize: s.size,
            animationDuration: `${s.dur}s`,
            animationDelay: `${s.delay}s`,
          }}
        >
          ✦
        </span>
      ))}
    </div>
  )
}
