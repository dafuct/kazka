import styles from './AvatarInitials.module.css'

// Carpathian cross-stitch thread tones
const PALETTE = ['#C0402C', '#D6A23A', '#2F6B43', '#2E6E82', '#9C2F4A', '#B5763A', '#7E2A33']

function pickColor(seed: string): string {
  let h = 0
  for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) | 0
  return PALETTE[Math.abs(h) % PALETTE.length]
}

export function AvatarInitials({ name, seed, size = 32 }: { name: string; seed: string; size?: number }) {
  const initials = (name || '?').trim().split(/\s+/).slice(0, 2).map(w => w[0]?.toUpperCase() ?? '').join('')
  const bg = pickColor(seed || name || '?')
  return (
    <span className={styles.avatar} style={{ background: bg, width: size, height: size, fontSize: size * 0.42 }}>
      {initials || '?'}
    </span>
  )
}
