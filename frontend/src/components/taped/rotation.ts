/** Deterministic per-card tilt derived from the story id, so tilts are stable
    across renders and sessions (spec: never Math.random at render time). */
export function hashId(id: string | number): number {
  const s = String(id)
  let h = 0
  for (let i = 0; i < s.length; i++) h = ((h << 5) - h + s.charCodeAt(i)) | 0
  return Math.abs(h)
}

export function cardRotation(id: string | number): { rot: string; trot: string } {
  const h = hashId(id)
  const rot = (((h % 5) - 2) * 0.9).toFixed(2) + 'deg' // -1.8 … 1.8
  const trot = ((Math.floor(h / 7) % 7) - 3) + 'deg' // -3 … 3
  return { rot, trot }
}
