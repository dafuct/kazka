import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/AuthContext'
import { useBilling } from '../lib/BillingContext'
import { useLocale } from '../lib/LocaleContext'
import { auth as authApi, billing as billingApi } from '../lib/apiClient'
import { ApiError } from '../lib/types'
import type { Entitlement } from '../lib/types'
import styles from './SettingsPage.module.css'

type Status = 'idle' | 'saving' | 'saved' | 'error'

function formatDate(iso: string | null): string {
  if (!iso) return '—'
  try { return new Date(iso).toLocaleDateString() } catch { return iso }
}

export function SettingsPage() {
  const { t } = useLocale()
  const { user, refresh: refreshAuth } = useAuth()
  const { entitlements, isPro, refresh: refreshBilling } = useBilling()
  const navigate = useNavigate()

  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [profileStatus, setProfileStatus] = useState<Status>('idle')
  const [profileMsg, setProfileMsg] = useState<string | null>(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [cancelStatus, setCancelStatus] = useState<Status>('idle')
  const [cancelMsg, setCancelMsg] = useState<string | null>(null)

  useEffect(() => {
    if (user) setDisplayName(user.displayName)
  }, [user])

  if (!user) return null

  const activeEntitlement: Entitlement | undefined = entitlements.find(
    e => e.state === 'ACTIVE' || e.state === 'GRACE'
  )
  const isAppleManaged = activeEntitlement?.source === 'APPLE'
  const isMonobankRecurring = activeEntitlement?.source === 'MONOBANK'

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

  async function handleCancel() {
    setConfirmOpen(false)
    setCancelStatus('saving')
    setCancelMsg(null)
    try {
      await billingApi.cancelSubscription()
      await refreshBilling()
      setCancelStatus('saved')
      if (isMonobankRecurring) {
        setCancelMsg(
          (t.settings.cancelledNotice ?? t.settings.cancelled)
            .replace('{date}', formatDate(activeEntitlement?.expiresAt ?? null))
        )
      } else {
        setCancelMsg(t.settings.cancelled)
      }
    } catch (err) {
      setCancelStatus('error')
      if (err instanceof ApiError && err.body?.error === 'APPLE_MANAGED') {
        setCancelMsg(t.settings.cancelAppleNotice)
      } else {
        setCancelMsg(t.settings.cancelError)
      }
    }
  }

  return (
    <div className={styles.page}>
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

      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>{t.settings.subscriptionSection}</h2>
        {!isPro && (
          <div className={styles.subBlock}>
            <p className={styles.muted}>{t.settings.statusInactive}</p>
            <button className={styles.primaryBtn} onClick={() => navigate('/pricing')}>
              {t.settings.upgradeCta}
            </button>
          </div>
        )}
        {isPro && activeEntitlement && (
          <div className={styles.subBlock}>
            <dl className={styles.detailList}>
              <div className={styles.detailRow}>
                <dt>{t.settings.statusLabel}</dt>
                <dd>
                  <span className={styles.statusBadge}>
                    {activeEntitlement.state === 'GRACE' ? t.settings.statusGrace : t.settings.statusActive}
                  </span>
                </dd>
              </div>
              <div className={styles.detailRow}>
                <dt>{t.settings.sourceLabel}</dt>
                <dd>{t.settings.sources[activeEntitlement.source]}</dd>
              </div>
              <div className={styles.detailRow}>
                <dt>{t.settings.expiresAt}</dt>
                <dd>{formatDate(activeEntitlement.expiresAt)}</dd>
              </div>
            </dl>
            {cancelMsg && (
              <p className={cancelStatus === 'error' ? styles.errorMsg : styles.successMsg}>{cancelMsg}</p>
            )}
            {isAppleManaged ? (
              <p className={styles.muted}>{t.settings.cancelAppleNotice}</p>
            ) : (
              <button
                className={styles.dangerBtn}
                onClick={() => setConfirmOpen(true)}
                disabled={cancelStatus === 'saving'}
              >
                {t.settings.cancel}
              </button>
            )}
          </div>
        )}
      </section>

      <p className={styles.backLink}>
        <Link to="/">← {t.story.back}</Link>
      </p>

      {confirmOpen && (
        <div className={styles.modalOverlay} onClick={() => setConfirmOpen(false)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <h3 className={styles.modalTitle}>{t.settings.cancelConfirmTitle}</h3>
            <p>
              {isMonobankRecurring
                ? (t.settings.cancelMonobankConfirmBody ?? t.settings.cancelConfirmBody)
                    .replace('{date}', formatDate(activeEntitlement?.expiresAt ?? null))
                : t.settings.cancelConfirmBody}
            </p>
            <div className={styles.modalActions}>
              <button className={styles.ghostBtn} onClick={() => setConfirmOpen(false)}>
                {t.settings.cancelConfirmNo}
              </button>
              <button className={styles.dangerBtn} onClick={handleCancel}>
                {t.settings.cancelConfirmYes}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
