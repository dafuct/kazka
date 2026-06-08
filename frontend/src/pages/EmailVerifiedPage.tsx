import { useSearchParams, useNavigate } from 'react-router-dom'
import { OrnamentBand } from '../components/stitch/OrnamentBand'
import { useLocale } from '../lib/LocaleContext'
import { useAuth } from '../lib/AuthContext'
import { useAuthModal } from '../lib/AuthModalContext'
import { useEffect } from 'react'
import styles from './EmailVerifiedPage.module.css'

export function EmailVerifiedPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const { user, refresh } = useAuth()
  const { openAuth } = useAuthModal()
  const { t } = useLocale()
  const ok = params.get('ok') === '1'

  useEffect(() => { if (ok) refresh() }, [ok, refresh])

  return (
    <div className={`${styles.page} kz-page`}><OrnamentBand framed={false} stitch={5} cols={120} className="kz-orn-top" />
      <h1 className={styles.heading}>
        {ok ? t.auth.messages.verifySuccess : t.auth.messages.verifyError}
      </h1>
      <button
        className={styles.btn}
        onClick={() => user ? navigate('/stories') : openAuth('signIn')}
      >
        {user ? t.auth.actions.myArchive : t.auth.tabs.signIn}
      </button>
    </div>
  )
}
