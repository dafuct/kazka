import { ScBand, BAND_PALETTE } from './StitchCanvas'
import styles from './OrnamentBand.module.css'

interface OrnamentBandProps {
  /** cell size in px (controls overall band height) */
  stitch?: number
  /** wrap in the maroon + gold cloth-band rails (true) or render bare (false) */
  framed?: boolean
  /** how many columns to draw before clipping; tiles seamlessly */
  cols?: number
  className?: string
}

/**
 * A traditional Hutsul border ornament — a seamless ribbon of multicolour
 * concentric-diamond medallions with small diamond fillers, framed by maroon
 * bars + gold pinlines. Theme-reactive (threads brighten on dark).
 */
export function OrnamentBand({
  stitch = 7,
  framed = true,
  cols = 240,
  className,
}: OrnamentBandProps) {
  return (
    <div
      className={`${framed ? styles.band : styles.bare} ${className ?? ''}`}
      aria-hidden="true"
    >
      <ScBand
        cols={cols}
        H={13}
        P={20}
        D={6}
        stitch={stitch}
        palette={BAND_PALETTE}
        ground="var(--color-surface)"
      />
    </div>
  )
}
