import { useEffect, useRef } from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { ThemeProvider } from './lib/ThemeContext'
import { LocaleProvider } from './lib/LocaleContext'
import { StoryModalProvider } from './lib/StoryModalContext'
import { StoryModal } from './components/modal/StoryModal'
import { Nav } from './components/chrome/Nav'
import { Footer } from './components/chrome/Footer'
import { HomePage } from './pages/HomePage'
import { ArchivePage } from './pages/ArchivePage'
import { StoryDetailPage } from './pages/StoryDetailPage'

function ScrollProgress() {
  const barRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const bar = barRef.current
    if (!bar) return
    const update = () => {
      const scrollable = document.body.scrollHeight - window.innerHeight
      const pct = scrollable > 0 ? (window.scrollY / scrollable) * 100 : 0
      bar.style.width = pct + '%'
    }
    window.addEventListener('scroll', update, { passive: true })
    return () => window.removeEventListener('scroll', update)
  }, [])
  return <div ref={barRef} className="scrollProgress" />
}

function CursorTrail() {
  useEffect(() => {
    const colors = ['#C4B5FD', '#EDD9A3', '#D97706', '#7C3AED', '#F59E0B']
    let lastX = 0, lastY = 0, lastTime = 0
    const onMove = (e: MouseEvent) => {
      const now = Date.now()
      const dx = e.clientX - lastX
      const dy = e.clientY - lastY
      if (now - lastTime < 40 || Math.sqrt(dx * dx + dy * dy) < 20) return
      lastX = e.clientX
      lastY = e.clientY
      lastTime = now
      const size = 8 + Math.random() * 10
      const color = colors[Math.floor(Math.random() * colors.length)]
      const star = document.createElement('div')
      star.className = 'cursorStar'
      star.innerHTML = `<svg width="${size}" height="${size}" viewBox="0 0 16 16" fill="none"><path d="M8 0L9.5 6.5L16 8L9.5 9.5L8 16L6.5 9.5L0 8L6.5 6.5Z" fill="${color}"/></svg>`
      star.style.left = (e.clientX - size / 2 + (Math.random() - 0.5) * 10) + 'px'
      star.style.top = (e.clientY - size / 2 + (Math.random() - 0.5) * 10) + 'px'
      document.body.appendChild(star)
      setTimeout(() => star.remove(), 800)
    }
    document.addEventListener('mousemove', onMove)
    return () => document.removeEventListener('mousemove', onMove)
  }, [])
  return null
}

export default function App() {
  return (
    <ThemeProvider>
      <LocaleProvider>
        <BrowserRouter>
          <StoryModalProvider>
            <ScrollProgress />
            <CursorTrail />
            <Nav />
            <main>
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/stories" element={<ArchivePage />} />
                <Route path="/stories/:id" element={<StoryDetailPage />} />
              </Routes>
            </main>
            <Footer />
            <StoryModal />
          </StoryModalProvider>
        </BrowserRouter>
      </LocaleProvider>
    </ThemeProvider>
  )
}
