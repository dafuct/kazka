import { useEffect } from 'react'
import { useAuth } from '../../lib/AuthContext'
import { useLocale } from '../../lib/LocaleContext'
import { ApiError } from '../../lib/types'
import styles from './AuthModal.module.css'

declare global {
  interface Window {
    AppleID?: {
      auth: {
        init(config: AppleIDInitConfig): void
        signIn(): Promise<AppleIDSignInResponse>
      }
    }
  }
}

interface AppleIDInitConfig {
  clientId: string
  scope: string
  redirectURI: string
  usePopup: boolean
}

interface AppleIDSignInResponse {
  authorization: { id_token: string; code: string; state?: string }
  user?: { name?: { firstName?: string; lastName?: string }; email?: string }
}

const APPLE_SCRIPT_SRC =
  'https://appleid.cdn-apple.com/appleauth/static/jsapi/appleid/1/en_US/appleid.auth.js'

export function AppleButton() {
  const clientId = import.meta.env.VITE_APPLE_WEB_CLIENT_ID as string | undefined
  const redirectURI = import.meta.env.VITE_APPLE_WEB_REDIRECT_URI as string | undefined
  const { refresh } = useAuth()
  const { t } = useLocale()

  useEffect(() => {
    if (!clientId || !redirectURI) return
    const initSDK = () => {
      window.AppleID?.auth.init({
        clientId, scope: 'name email', redirectURI, usePopup: true,
      })
    }
    if (document.querySelector(`script[src="${APPLE_SCRIPT_SRC}"]`)) {
      initSDK()
      return
    }
    const script = document.createElement('script')
    script.src = APPLE_SCRIPT_SRC
    script.async = true
    // Apple rotates the appleauth bundle; SRI pinning isn't viable. Defense in depth
    // is provided by CSP `script-src` (nginx.conf) restricting loads to appleid.cdn-apple.com.
    script.crossOrigin = 'anonymous'
    script.referrerPolicy = 'no-referrer'
    script.onload = initSDK
    document.head.appendChild(script)
  }, [clientId, redirectURI])

  if (!clientId || !redirectURI) return null

  async function onClick() {
    try {
      if (!window.AppleID) throw new Error('AppleID SDK not loaded')
      const r = await window.AppleID.auth.signIn()
      const fullName = r.user?.name
        ? [r.user.name.firstName, r.user.name.lastName].filter(Boolean).join(' ')
        : undefined

      const res = await fetch('/api/auth/oauth/apple', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          identityToken: r.authorization.id_token,
          authorizationCode: r.authorization.code,
          fullName,
          email: r.user?.email,
        }),
      })
      if (!res.ok) {
        const body = await res.json().catch(() => ({ error: 'ERROR' }))
        throw new ApiError(res.status, body)
      }
      await refresh()
    } catch (err) {
      if ((err as { error?: string })?.error === 'popup_closed_by_user') return
      const code = err instanceof ApiError ? (err.body.error as string) : 'ERROR'
      const label = t.auth.errors[code as keyof typeof t.auth.errors] ?? t.auth.errors.ERROR
      // eslint-disable-next-line no-alert
      alert(label)
    }
  }

  return (
    <button type="button" className={styles.appleBtn} onClick={onClick}>
      <svg className={styles.appleLogo} viewBox="0 0 384 512" aria-hidden="true">
        <path d="M318.7 268.7c-.2-36.7 16.4-64.4 50-84.8-18.8-26.9-47.2-41.7-84.7-44.6-35.5-2.8-74.3 20.7-88.5 20.7-15 0-49.4-19.7-76.4-19.7C63.3 141.2 4 184.8 4 273.5q0 39.3 14.4 81.2c12.8 36.7 59 126.7 107.2 125.2 25.2-.6 43-17.9 75.8-17.9 31.8 0 48.3 17.9 76.4 17.9 48.6-.7 90.4-82.5 102.6-119.3-65.2-30.7-61.7-90-61.7-91.9zm-56.6-164.2c27.3-32.4 24.8-61.9 24-72.5-24.1 1.4-52 16.4-67.9 34.9-17.5 19.8-27.8 44.3-25.6 71.9 26.1 2 49.9-11.4 69.5-34.3z" />
      </svg>
      {t.auth.actions.apple}
    </button>
  )
}
