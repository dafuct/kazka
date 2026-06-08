import { useEffect, useState } from 'react'
import { OrnamentBand } from '../components/stitch/OrnamentBand'
import { Link } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'
import { useLocale } from '../lib/LocaleContext'
import { auth as authApi } from '../lib/apiClient'
import styles from './SettingsPage.module.css'

type Status = 'idle' | 'saving' | 'saved' | 'error'

export function SettingsPage() {
  const { t } = useLocale()
  const { user, refresh: refreshAuth } = useAuth()

  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [profileStatus, setProfileStatus] = useState<Status>('idle')
  const [profileMsg, setProfileMsg] = useState<string | null>(null)

  useEffect(() => {
    if (user) setDisplayName(user.displayName)
  }, [user])

  if (!user) return null

  async function handleSaveProfile(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = displayName.trim()
    if (!trimmed || trimmed === user!.displayName) return
    setProfileStatus('saving')
    setProfileMsg(null)
    try {
      await authApi.updateProfile(trimmed)
      await refreshAuth()
      setProfileStatus('saved')
      setProfileMsg(t.settings.profileSaved)
    } catch {
      setProfileStatus('error')
      setProfileMsg(t.settings.profileError)
    }
  }

  return (
    <div className={`${styles.page} kz-page`}><OrnamentBand framed={false} stitch={5} cols={120} className="kz-orn-top" />
      <h1 className={styles.title}>{t.settings.title}</h1>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>{t.settings.profileSection}</h2>
        <form onSubmit={handleSaveProfile} className={styles.form}>
          <label className={styles.field}>
            <span className={styles.label}>{t.settings.email}</span>
            <input className={styles.input} value={user.email} disabled />
          </label>
          <label className={styles.field}>
            <span className={styles.label}>{t.settings.displayName}</span>
            <input
              className={styles.input}
              value={displayName}
              onChange={e => setDisplayName(e.target.value)}
              maxLength={100}
              required
            />
          </label>
          {profileMsg && (
            <p className={profileStatus === 'error' ? styles.errorMsg : styles.successMsg}>{profileMsg}</p>
          )}
          <button
            type="submit"
            className={styles.primaryBtn}
            disabled={profileStatus === 'saving' || displayName.trim() === user.displayName || !displayName.trim()}
          >
            {t.settings.saveProfile}
          </button>
        </form>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>{(t as any).settings?.childrenSection ?? (t as any).children?.listTitle ?? 'Children'}</h2>
        <p className={styles.muted}>{(t as any).settings?.childrenDescription ?? 'Manage child profiles and their character libraries.'}</p>
        <Link to="/settings/children" className={styles.primaryBtn}>
          {(t as any).settings?.manageChildren ?? (t as any).children?.manageLink ?? 'Manage children'}
        </Link>
      </section>

      <p className={styles.backLink}>
        <Link to="/">← {t.story.back}</Link>
      </p>
    </div>
  )
}
