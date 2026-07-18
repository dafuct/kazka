import { describe, it, expect } from 'vitest'
import { cardRotation } from './rotation'

describe('cardRotation', () => {
  it('is deterministic — same id gives the same rotation every call', () => {
    const a = cardRotation('story-abc-123')
    const b = cardRotation('story-abc-123')
    expect(a).toEqual(b)
  })

  it('varies across ids (sample set produces at least 2 distinct tilts)', () => {
    const ids = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h']
    const rots = new Set(ids.map(id => cardRotation(id).rot))
    expect(rots.size).toBeGreaterThan(1)
  })

  it('stays within the handoff tilt range (±1.8deg card, ±3deg tape)', () => {
    for (const id of ['x1', 'x2', 'x3', 'uuid-4-like-0000', '42']) {
      const { rot, trot } = cardRotation(id)
      expect(Math.abs(parseFloat(rot))).toBeLessThanOrEqual(1.8)
      expect(Math.abs(parseFloat(trot))).toBeLessThanOrEqual(3)
    }
  })
})
