import { useEffect, useRef } from 'react'

interface Props {
  light?: boolean
}

export function SectionParticles({ light = false }: Props) {
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const field = ref.current
    if (!field) return
    // Carpathian thread tones (toned down from the old purple sparkle)
    const colors = light
      ? ['#D6A23A', '#C0402C', '#9C2F4A']
      : ['#D6A23A', '#C0402C', '#2F6B43', '#2E6E82']

    for (let i = 0; i < 16; i++) {
      const el = document.createElement('div')
      el.className = 'sectionParticle'
      const x = Math.random() * 100
      const y = Math.random() * 100
      const size = 6 + Math.random() * 8
      const dur = 4 + Math.random() * 6
      const delay = Math.random() * 5
      const opacity = 0.2 + Math.random() * 0.4
      const color = colors[Math.floor(Math.random() * colors.length)]

      if (Math.random() > 0.5) {
        el.innerHTML = `<svg width="${size}" height="${size}" viewBox="0 0 16 16" fill="none"><path d="M8 0L9.5 6.5L16 8L9.5 9.5L8 16L6.5 9.5L0 8L6.5 6.5Z" fill="${color}"/></svg>`
        el.style.cssText = `left:${x}%;top:${y}%;--sp-opacity:${opacity};animation:sectionFloat ${dur}s ease-in-out ${delay}s infinite;`
      } else {
        const ds = 1.5 + Math.random() * 2
        el.style.cssText = `left:${x}%;top:${y}%;width:${ds}px;height:${ds}px;border-radius:50%;background:${color};animation:sectionTwinkle ${dur}s ease-in-out ${delay}s infinite;`
      }
      field.appendChild(el)
    }

    return () => { field.innerHTML = '' }
  }, [])

  return <div ref={ref} className="sectionParticles" aria-hidden="true" />
}
