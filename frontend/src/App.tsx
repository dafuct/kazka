import { useEffect, useRef } from 'react'
import { BrowserRouter, Routes, Route, useLocation, useNavigate } from 'react-router-dom'
import { ThemeProvider } from './lib/ThemeContext'
import { LocaleProvider } from './lib/LocaleContext'
import { StoryModalProvider } from './lib/StoryModalContext'
import { AuthProvider, useAuth } from './lib/AuthContext'
import { ChildrenProvider } from './lib/ChildrenContext'
import { AuthModalProvider } from './lib/AuthModalContext'
import { ActiveStoryProvider, useActiveStory } from './lib/ActiveStoryContext'
import { StarField } from './components/atmosphere/StarField'
import { StoryModal } from './components/modal/StoryModal'
import { ProgressWidget } from './components/comics/ProgressWidget'
import { AuthModal } from './components/auth/AuthModal'
import { Nav } from './components/chrome/Nav'
import { SuspensionBanner } from './components/chrome/SuspensionBanner'
import { Footer } from './components/chrome/Footer'
import { RequireAuth } from './components/auth/RequireAuth'
import { RequireAdmin } from './components/auth/RequireAdmin'
import { RequireChild } from './components/children/RequireChild'
import { HomePage } from './pages/HomePage'
import { ShowcasePage } from './pages/ShowcasePage'
import { ShowcaseDetailPage } from './pages/ShowcaseDetailPage'
import { ArchivePage } from './pages/ArchivePage'
import { StoryDetailPage } from './pages/StoryDetailPage'
import { EmailVerifiedPage } from './pages/EmailVerifiedPage'
import { PasswordResetPage } from './pages/PasswordResetPage'
import { AdminUsersPage } from './pages/AdminUsersPage'
import { AdminModerationPage } from './pages/AdminModerationPage'
import { SettingsPage } from './pages/SettingsPage'
import { ChildProfileEditPage } from './pages/ChildProfileEditPage'
import { ChildrenListPage } from './pages/ChildrenListPage'
import { CharacterLibraryPage } from './pages/CharacterLibraryPage'
import { DashboardPage } from './pages/DashboardPage'
import { LegalPage } from './pages/legal/LegalPage'

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
    let lastX = 0, lastY = 0, lastTime = 0
    const onMove = (e: MouseEvent) => {
      const now = Date.now()
      const dx = e.clientX - lastX
      const dy = e.clientY - lastY
      if (now - lastTime < 40 || Math.sqrt(dx * dx + dy * dy) < 24) return
      lastX = e.clientX
      lastY = e.clientY
      lastTime = now
      const star = document.createElement('span')
      star.className = 'star-fx'
      star.textContent = '✦'
      star.style.position = 'fixed'
      star.style.zIndex = '9998'
      star.style.left = e.clientX + 'px'
      star.style.top = e.clientY + 'px'
      star.style.fontSize = 9 + Math.random() * 11 + 'px'
      document.body.appendChild(star)
      star.addEventListener('animationend', () => star.remove())
    }
    document.addEventListener('mousemove', onMove)
    return () => document.removeEventListener('mousemove', onMove)
  }, [])
  return null
}

function GoogleAuthLanding() {
  const location = useLocation()
  const navigate = useNavigate()
  const { refresh } = useAuth()
  useEffect(() => {
    const params = new URLSearchParams(location.search)
    if (params.get('auth') === 'ok') {
      params.delete('auth')
      const search = params.toString()
      navigate({ pathname: location.pathname, search: search ? '?' + search : '' }, { replace: true })
      refresh()
    }
  }, [location, navigate, refresh])
  return null
}

function ActiveStoryProgressWidget() {
  const { activeStoryId, setActiveStoryId } = useActiveStory()
  return <ProgressWidget storyId={activeStoryId} onClear={() => setActiveStoryId(null)} />
}

// Logged-out visitors at "/" see the marketing HomePage (its hero CTA opens the
// auth modal). Logged-in users keep the original RequireChild><HomePage behavior
// unchanged. The public sample-tale showcase lives at /showcase.
function HomeOrShowcase() {
  const { user, loading } = useAuth()
  if (loading) return <p style={{ padding: 32, textAlign: 'center' }}>...</p>
  if (!user) return <HomePage />
  return (
    <RequireChild>
      <HomePage />
    </RequireChild>
  )
}

function AppShell() {
  return (
    <>
      <StarField />
      <div className="appContent">
        <ScrollProgress />
        <CursorTrail />
        <GoogleAuthLanding />
        <Nav />
        <SuspensionBanner />
        <main>
          <Routes>
            <Route path="/" element={<HomeOrShowcase />} />
            <Route path="/showcase" element={<ShowcasePage />} />
            <Route path="/showcase/:id" element={<ShowcaseDetailPage />} />
            <Route path="/stories" element={<RequireAuth><RequireChild><ArchivePage /></RequireChild></RequireAuth>} />
            <Route path="/stories/:id" element={<RequireAuth><RequireChild><StoryDetailPage /></RequireChild></RequireAuth>} />
            <Route path="/verify-email" element={<EmailVerifiedPage />} />
            <Route path="/reset-password" element={<PasswordResetPage />} />
            <Route path="/admin/users" element={<RequireAdmin><AdminUsersPage /></RequireAdmin>} />
            <Route path="/admin/moderation" element={<RequireAdmin><AdminModerationPage /></RequireAdmin>} />
            <Route path="/settings" element={<RequireAuth><SettingsPage /></RequireAuth>} />
            <Route path="/settings/children" element={<RequireAuth><ChildrenListPage /></RequireAuth>} />
            <Route path="/settings/children/new" element={<RequireAuth><ChildProfileEditPage /></RequireAuth>} />
            <Route path="/settings/children/:id" element={<RequireAuth><ChildProfileEditPage /></RequireAuth>} />
            <Route path="/settings/children/:id/characters" element={<RequireAuth><CharacterLibraryPage /></RequireAuth>} />
            <Route path="/dashboard" element={<RequireAuth><DashboardPage /></RequireAuth>} />
            <Route path="/legal/terms"   element={<LegalPage slug="terms" />} />
            <Route path="/legal/privacy" element={<LegalPage slug="privacy" />} />
            <Route path="/legal/support" element={<LegalPage slug="support" />} />
          </Routes>
        </main>
        <Footer />
        <StoryModal />
        <AuthModal />
        <ActiveStoryProgressWidget />
      </div>
    </>
  )
}

export default function App() {
  return (
    <ThemeProvider>
      <LocaleProvider>
        <BrowserRouter>
          <AuthProvider>
            <ChildrenProvider>
              <AuthModalProvider>
                <StoryModalProvider>
                  <ActiveStoryProvider>
                    <AppShell />
                  </ActiveStoryProvider>
                </StoryModalProvider>
              </AuthModalProvider>
            </ChildrenProvider>
          </AuthProvider>
        </BrowserRouter>
      </LocaleProvider>
    </ThemeProvider>
  )
}
