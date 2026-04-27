import styles from './PlaceholderSvg.module.css'

export function PlaceholderSvg() {
  return (
    <div className={styles.wrapper}>
      <svg
        viewBox="0 0 400 300"
        xmlns="http://www.w3.org/2000/svg"
        className={styles.svg}
        aria-hidden="true"
      >
        <rect width="400" height="300" fill="var(--color-surface-2)" rx="12" />
        <circle cx="200" cy="110" r="50" fill="var(--color-surface-deep)" opacity="0.6" />
        <ellipse cx="200" cy="220" rx="80" ry="30" fill="var(--color-surface-deep)" opacity="0.4" />
        <path
          d="M150 110 Q200 60 250 110 Q280 140 250 170 Q200 200 150 170 Q120 140 150 110Z"
          fill="var(--color-magic-soft)"
          opacity="0.5"
        />
        <circle cx="185" cy="105" r="6" fill="var(--color-magic)" opacity="0.4" />
        <circle cx="215" cy="105" r="6" fill="var(--color-magic)" opacity="0.4" />
        <path
          d="M185 130 Q200 145 215 130"
          stroke="var(--color-magic)"
          strokeWidth="2.5"
          fill="none"
          strokeLinecap="round"
          opacity="0.5"
        />
        <text
          x="200"
          y="270"
          textAnchor="middle"
          fill="var(--color-text-faint)"
          fontSize="13"
          fontFamily="var(--font-body)"
        >
          Ілюстрація готується...
        </text>
      </svg>
    </div>
  )
}
