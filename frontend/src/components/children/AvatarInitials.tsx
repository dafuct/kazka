import styles from './AvatarInitials.module.css'

const PALETTE = ['#C4B5FD', '#FCD34D', '#86EFAC', '#FCA5A5', '#93C5FD', '#FBBF24', '#A78BFA']

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
