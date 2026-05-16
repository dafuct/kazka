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
    <button type="button" className={styles.appleBtn ?? styles.googleBtn} onClick={onClick}>
      {t.auth.actions.apple}
    </button>
  )
}
