import { useEffect, useRef } from 'react'
import { BrowserRouter, Routes, Route, useLocation, useNavigate } from 'react-router-dom'
import { ThemeProvider } from './lib/ThemeContext'
import { LocaleProvider } from './lib/LocaleContext'
import { StoryModalProvider } from './lib/StoryModalContext'
import { AuthProvider, useAuth } from './lib/AuthContext'
import { BillingProvider } from './lib/BillingContext'
import { ChildrenProvider } from './lib/ChildrenContext'
import { AuthModalProvider } from './lib/AuthModalContext'
import { StoryModal } from './components/modal/StoryModal'
import { AuthModal } from './components/auth/AuthModal'
import { Nav } from './components/chrome/Nav'
import { SuspensionBanner } from './components/chrome/SuspensionBanner'
import { Footer } from './components/chrome/Footer'
import { RequireAuth } from './components/auth/RequireAuth'
import { RequireAdmin } from './components/auth/RequireAdmin'
import { RequireChild } from './components/children/RequireChild'
import { HomePage } from './pages/HomePage'
import { ArchivePage } from './pages/ArchivePage'
import { StoryDetailPage } from './pages/StoryDetailPage'
import { EmailVerifiedPage } from './pages/EmailVerifiedPage'
import { PasswordResetPage } from './pages/PasswordResetPage'
import { AdminUsersPage } from './pages/AdminUsersPage'
import { AdminModerationPage } from './pages/AdminModerationPage'
import { PricingPage } from './pages/PricingPage'
import { SubscriptionSuccessPage } from './pages/SubscriptionSuccessPage'
import { CheckoutPage } from './pages/CheckoutPage'
import { SettingsPage } from './pages/SettingsPage'
import { ChildProfileEditPage } from './pages/ChildProfileEditPage'
import { ChildrenListPage } from './pages/ChildrenListPage'
import { CharacterLibraryPage } from './pages/CharacterLibraryPage'
import { DashboardPage } from './pages/DashboardPage'
import { RedeemPage } from './pages/RedeemPage'
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

function AppShell() {
  return (
    <>
      <ScrollProgress />
      <CursorTrail />
      <GoogleAuthLanding />
      <Nav />
      <SuspensionBanner />
      <main>
        <Routes>
          <Route path="/" element={<RequireChild><HomePage /></RequireChild>} />
          <Route path="/stories" element={<RequireAuth><RequireChild><ArchivePage /></RequireChild></RequireAuth>} />
          <Route path="/stories/:id" element={<RequireAuth><RequireChild><StoryDetailPage /></RequireChild></RequireAuth>} />
          <Route path="/verify-email" element={<EmailVerifiedPage />} />
          <Route path="/reset-password" element={<PasswordResetPage />} />
          <Route path="/admin/users" element={<RequireAdmin><AdminUsersPage /></RequireAdmin>} />
          <Route path="/admin/moderation" element={<RequireAdmin><AdminModerationPage /></RequireAdmin>} />
          <Route path="/pricing" element={<PricingPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/subscription/success" element={<RequireAuth><SubscriptionSuccessPage /></RequireAuth>} />
          <Route path="/settings" element={<RequireAuth><SettingsPage /></RequireAuth>} />
          <Route path="/settings/children" element={<RequireAuth><ChildrenListPage /></RequireAuth>} />
          <Route path="/settings/children/new" element={<RequireAuth><ChildProfileEditPage /></RequireAuth>} />
          <Route path="/settings/children/:id" element={<RequireAuth><ChildProfileEditPage /></RequireAuth>} />
          <Route path="/settings/children/:id/characters" element={<RequireAuth><CharacterLibraryPage /></RequireAuth>} />
          <Route path="/dashboard" element={<RequireAuth><DashboardPage /></RequireAuth>} />
          <Route path="/redeem" element={<RequireAuth><RedeemPage /></RequireAuth>} />
          <Route path="/legal/terms"   element={<LegalPage slug="terms" />} />
          <Route path="/legal/privacy" element={<LegalPage slug="privacy" />} />
          <Route path="/legal/support" element={<LegalPage slug="support" />} />
        </Routes>
      </main>
      <Footer />
      <StoryModal />
      <AuthModal />
    </>
  )
}

export default function App() {
  return (
    <ThemeProvider>
      <LocaleProvider>
        <BrowserRouter>
          <AuthProvider>
            <BillingProvider>
              <ChildrenProvider>
                <AuthModalProvider>
                  <StoryModalProvider>
                    <AppShell />
                  </StoryModalProvider>
                </AuthModalProvider>
              </ChildrenProvider>
            </BillingProvider>
          </AuthProvider>
        </BrowserRouter>
      </LocaleProvider>
    </ThemeProvider>
  )
}
