import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { ThemeProvider } from './lib/ThemeContext'
import { LocaleProvider } from './lib/LocaleContext'
import { Nav } from './components/chrome/Nav'
import { Footer } from './components/chrome/Footer'
import { HomePage } from './pages/HomePage'
import { ArchivePage } from './pages/ArchivePage'
import { StoryDetailPage } from './pages/StoryDetailPage'

export default function App() {
  return (
    <ThemeProvider>
      <LocaleProvider>
        <BrowserRouter>
          <Nav />
          <main>
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/stories" element={<ArchivePage />} />
              <Route path="/stories/:id" element={<StoryDetailPage />} />
            </Routes>
          </main>
          <Footer />
        </BrowserRouter>
      </LocaleProvider>
    </ThemeProvider>
  )
}
