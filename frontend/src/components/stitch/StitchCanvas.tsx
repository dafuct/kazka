// StitchCanvas — realistic counted cross-stitch renderer (canvas).
// Ported from the «Гуцульська» design handoff (src/stitch-canvas.jsx) to a
// typed React module: no window globals, proper imports/exports.
//
// Draws an aida-cloth grid and renders each filled cell as an X made of two
// crossing threads (the ↘ thread darker = "under", ↗ lighter = "over"), like
// real counted cross-stitch. Pattern builders are rule-based so every motif is
// perfectly symmetric.
//
// Theme-aware: `ground`, `gridColor`, and any `palette` entry may be a CSS
// custom-property string like "var(--kz-thread-red)". They are resolved against
// :root at draw time, and the canvas redraws when the `data-theme` attribute
// flips — so threads brighten on the dark Carpathian palette automatically.
import { useEffect, useMemo, useRef } from 'react'

export type StitchGrid = (number | null)[][]
export type StitchRule = (dx: number, dy: number, R: number) => number

/** Convenience thread ring palette (theme-reactive via CSS vars). */
export const THREAD = [
  'var(--kz-thread-red)',
  'var(--kz-thread-gold)',
  'var(--kz-thread-green)',
  'var(--kz-thread-teal)',
  'var(--kz-thread-maroon)',
  'var(--kz-thread-black)',
  'var(--kz-thread-berry)',
]

/** Border-band palette: gold centre → … → black outer ring (matches the
    traditional concentric-diamond border ornament). Theme-reactive. */
export const BAND_PALETTE = [
  'var(--kz-thread-gold)',
  'var(--kz-thread-red)',
  'var(--kz-thread-green)',
  'var(--kz-thread-teal)',
  'var(--kz-thread-maroon)',
  'var(--kz-thread-berry)',
  'var(--kz-thread-black)',
]

function resolveColor(c: string): string {
  const v = c.trim()
  if (v.startsWith('var(')) {
    const name = v.slice(4, -1).split(',')[0].trim()
    const resolved = getComputedStyle(document.documentElement)
      .getPropertyValue(name)
      .trim()
    return resolved || '#F5F2E9'
  }
  return v
}

export function scShade(hex: string, f: number): string {
  if (!hex.startsWith('#')) return hex // already rgb()/named — leave unshaded
  const h = hex.replace('#', '')
  const r = parseInt(h.slice(0, 2), 16),
    g = parseInt(h.slice(2, 4), 16),
    b = parseInt(h.slice(4, 6), 16)
  const cl = (v: number) => Math.max(0, Math.min(255, Math.round(v)))
  if (f <= 1) return `rgb(${cl(r * f)},${cl(g * f)},${cl(b * f)})`
  const t = f - 1
  return `rgb(${cl(r + (255 - r) * t)},${cl(g + (255 - g) * t)},${cl(b + (255 - b) * t)})`
}

interface StitchCanvasProps {
  grid: StitchGrid
  palette: string[]
  stitch?: number
  /** hex, "var(--x)", or null for a transparent (grid-only) motif on the page */
  ground?: string | null
  gridLines?: boolean
  /** optional grid-line colour override; defaults to the themed aida grid */
  gridColor?: string
}

export function StitchCanvas({
  grid,
  palette,
  stitch = 12,
  ground = 'var(--color-surface)',
  gridLines = true,
  gridColor,
}: StitchCanvasProps) {
  const ref = useRef<HTMLCanvasElement>(null)
  const rows = grid.length
  const cols = grid[0] ? grid[0].length : 0

  useEffect(() => {
    const cv = ref.current
    if (!cv) return

    const draw = () => {
      const dpr = Math.min(2, window.devicePixelRatio || 1)
      const W = cols * stitch,
        H = rows * stitch
      cv.width = W * dpr
      cv.height = H * dpr
      cv.style.width = W + 'px'
      cv.style.height = H + 'px'
      const ctx = cv.getContext('2d')
      if (!ctx) return
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      ctx.clearRect(0, 0, W, H)

      const groundColor = ground != null ? resolveColor(ground) : null
      if (groundColor) {
        ctx.fillStyle = groundColor
        ctx.fillRect(0, 0, W, H)
      }

      if (gridLines) {
        ctx.strokeStyle = gridColor
          ? resolveColor(gridColor)
          : resolveColor('var(--kz-aida-grid)') ||
            (ground ? 'rgba(120,110,90,0.18)' : 'rgba(120,110,90,0.13)')
        ctx.lineWidth = 1
        for (let x = 0; x <= cols; x++) {
          const px = Math.round(x * stitch) + 0.5
          ctx.beginPath()
          ctx.moveTo(px, 0)
          ctx.lineTo(px, H)
          ctx.stroke()
        }
        for (let y = 0; y <= rows; y++) {
          const py = Math.round(y * stitch) + 0.5
          ctx.beginPath()
          ctx.moveTo(0, py)
          ctx.lineTo(W, py)
          ctx.stroke()
        }
      }

      ctx.lineCap = 'round'
      const lw = Math.max(1.4, stitch * 0.32),
        m = stitch * 0.15
      for (let r = 0; r < rows; r++) {
        const row = grid[r]
        for (let c = 0; c < cols; c++) {
          const idx = row[c]
          if (idx == null || idx < 0) continue
          const col = resolveColor(palette[idx % palette.length])
          const x = c * stitch,
            y = r * stitch
          ctx.strokeStyle = scShade(col, 0.68)
          ctx.lineWidth = lw
          ctx.beginPath()
          ctx.moveTo(x + m, y + m)
          ctx.lineTo(x + stitch - m, y + stitch - m)
          ctx.stroke()
          ctx.strokeStyle = scShade(col, 1.12)
          ctx.lineWidth = lw
          ctx.beginPath()
          ctx.moveTo(x + stitch - m, y + m)
          ctx.lineTo(x + m, y + stitch - m)
          ctx.stroke()
        }
      }
    }

    draw()
    // Redraw when the theme flips — grounds/grid/threads use CSS vars.
    const obs = new MutationObserver(draw)
    obs.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['data-theme'],
    })
    return () => obs.disconnect()
  }, [grid, palette, stitch, ground, gridLines, gridColor, rows, cols])

  return <canvas ref={ref} style={{ display: 'block' }} />
}

// ── Rule-based motifs (return palette ring index or -1) ──
export const SCM: Record<'medallion' | 'star8' | 'rose', StitchRule> = {
  // Rich central medallion: rose centre + 8-ray star + nested diamond frames
  medallion(dx, dy, R) {
    const ax = Math.abs(dx),
      ay = Math.abs(dy),
      ad = ax + ay,
      am = Math.max(ax, ay)
    if (ad === 0) return 0
    if (ad === 2) return 1
    if (ax === ay && am <= R) return am // diagonal arms (X)
    if ((dx === 0 || dy === 0) && am <= R) return am // axis arms (+)
    if (ad === R) return R // outer diamond frame
    if (ad === R - 3) return R - 3 // inner diamond ring
    const p = Math.round(R * 0.5)
    for (const [cx, cy] of [
      [p, 0],
      [-p, 0],
      [0, p],
      [0, -p],
    ])
      if (Math.abs(dx - cx) + Math.abs(dy - cy) <= 1) return 1
    return -1
  },
  // 8-pointed star «ружа»
  star8(dx, dy, R) {
    const ax = Math.abs(dx),
      ay = Math.abs(dy),
      ad = ax + ay,
      am = Math.max(ax, ay)
    if (ad <= 2) return ad
    if (ad === R) return R
    if ((dx === 0 || dy === 0 || ax === ay) && am <= R) return am
    return -1
  },
  // Flower rosette: centre + axis stems + 4 fat petals + diagonal buds
  rose(dx, dy, R) {
    const ax = Math.abs(dx),
      ay = Math.abs(dy),
      ad = ax + ay,
      am = Math.max(ax, ay)
    if (ad <= 1) return 0 // centre plus
    if (ad === 2) return 5 // contrast ring
    const p = R - 1
    for (const [cx, cy] of [
      [p, 0],
      [-p, 0],
      [0, p],
      [0, -p],
    ]) {
      const dd = Math.abs(dx - cx) + Math.abs(dy - cy)
      if (dd <= 2) return 2 + dd
    } // fat petals
    const q = Math.max(2, Math.round(R * 0.55))
    for (const [cx, cy] of [
      [q, q],
      [q, -q],
      [-q, q],
      [-q, -q],
    ])
      if (Math.abs(dx - cx) + Math.abs(dy - cy) <= 1) return 3 // diagonal buds
    if ((dx === 0 || dy === 0) && am <= p) return 4 // stems joining centre to petals
    return -1
  },
}

export function scRect(n: number, rule: StitchRule): StitchGrid {
  const cen = (n - 1) / 2,
    g: StitchGrid = []
  for (let r = 0; r < n; r++) {
    const row: (number | null)[] = []
    for (let c = 0; c < n; c++) row.push(rule(c - cen, r - cen, cen))
    g.push(row)
  }
  return g
}

// Continuous border band: a chain of concentric-diamond lozenges (period P,
// radius D) with small diamonds filling the gaps. Tiles seamlessly.
export function scBand(cols: number, H: number, P: number, D: number): StitchGrid {
  const cy0 = (H - 1) / 2,
    g: StitchGrid = []
  for (let y = 0; y < H; y++) {
    const row: (number | null)[] = [],
      cy = y - cy0
    for (let x = 0; x < cols; x++) {
      const lx = ((x % P) + P) % P,
        dxc = lx <= P / 2 ? lx : lx - P,
        ad = Math.abs(dxc) + Math.abs(cy)
      const lx2 = (((x - P / 2) % P) + P) % P,
        dxc2 = lx2 <= P / 2 ? lx2 : lx2 - P,
        ad2 = Math.abs(dxc2) + Math.abs(cy)
      let v = -1
      if (ad <= D) v = ad
      else if (ad2 <= 2) v = ad2 + 1
      row.push(v)
    }
    g.push(row)
  }
  return g
}

// ── Convenience wrappers (memoise the grid) ──
interface ScMotifProps {
  rule: StitchRule
  n?: number
  stitch?: number
  palette: string[]
  ground?: string | null
  gridLines?: boolean
}
export function ScMotif({
  rule,
  n = 13,
  stitch = 8,
  palette,
  ground = 'var(--color-surface)',
  gridLines = true,
}: ScMotifProps) {
  const grid = useMemo(() => scRect(n, rule), [rule, n])
  return (
    <StitchCanvas
      grid={grid}
      palette={palette}
      stitch={stitch}
      ground={ground}
      gridLines={gridLines}
    />
  )
}

interface ScBandProps {
  cols?: number
  H?: number
  P?: number
  D?: number
  stitch?: number
  palette: string[]
  ground?: string | null
}
export function ScBand({
  cols = 120,
  H = 13,
  P = 14,
  D = 5,
  stitch = 9,
  palette,
  ground = 'var(--color-surface)',
}: ScBandProps) {
  const grid = useMemo(() => scBand(cols, H, P, D), [cols, H, P, D])
  return <StitchCanvas grid={grid} palette={palette} stitch={stitch} ground={ground} />
}
