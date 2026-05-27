import { useEffect, useMemo, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import styles from './PricingPage.module.css'
import { PlanToggle, type Period } from '../components/billing/PlanToggle'
import { PlanCard } from '../components/billing/PlanCard'
import { ProviderSelector } from '../components/billing/ProviderSelector'
import { useLocale } from '../lib/LocaleContext'
import { useAuth } from '../lib/AuthContext'
import { useAuthModal } from '../lib/AuthModalContext'
import { useBilling } from '../lib/BillingContext'
import { billing } from '../lib/apiClient'
import { ApiError } from '../lib/types'
import type { Product, ProviderName, GeoResponse, Entitlement } from '../lib/types'

function formatPrice(p: Product): string {
  const dollars = p.priceMicro / 1_000_000
  if (p.currency === 'USD') return `$${dollars.toFixed(2)}`
  return `${dollars.toFixed(2)} ${p.currency}`
}

function formatDate(iso: string | null): string {
  if (!iso) return '—'
  try { return new Date(iso).toLocaleDateString() } catch { return iso }
}

export function PricingPage() {
  const { t } = useLocale()
  const { user } = useAuth()
  const { openAuth } = useAuthModal()
  const { entitlements, isPro, refresh: refreshBilling } = useBilling()
  const [params] = useSearchParams()
  const [period, setPeriod] = useState<Period>('yearly')
  const [products, setProducts] = useState<Product[]>([])
  const [geo, setGeo] = useState<GeoResponse | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const [cancelMsg, setCancelMsg] = useState<string | null>(null)
  const [cancelErr, setCancelErr] = useState(false)
  const cancelled = params.get('cancelled') === '1'

  useEffect(() => {
    billing.listProducts().then(setProducts).catch(() => setProducts([]))
    billing.geo().then(setGeo).catch(() => setGeo({ country: 'US', isUkraine: false }))
  }, [])

  const activeEntitlement: Entitlement | undefined = entitlements.find(
    e => e.state === 'ACTIVE' || e.state === 'GRACE'
  )
  const isAppleManaged = activeEntitlement?.source === 'APPLE'
  const currentProduct = useMemo(() => {
    if (!activeEntitlement) return undefined
    return products.find(p => p.appleProductId === activeEntitlement.productAppleId)
  }, [products, activeEntitlement])

  const proProduct = useMemo(() => {
    const code = period === 'monthly' ? 'P1M' : 'P1Y'
    return products.find(p => p.period === code && p.tier === 'pro')
  }, [products, period])

  const proIsCurrent = !!(isPro && proProduct && currentProduct && proProduct.id === currentProduct.id)

  async function handleSubscribe(provider: ProviderName) {
    if (!user) {
      openAuth('signIn')
      return
    }
    if (!proProduct) return
    setSubmitting(true)
    setError(null)
    try {
      const session = await billing.createCheckoutSession(
        proProduct.id, provider, geo?.country
      )
      if (session.checkoutUrl) {
        window.location.href = session.checkoutUrl
      } else {
        setError('No checkout URL returned')
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Checkout failed')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCountryOverride(country: string | null) {
    const next = await billing.geo(country ?? undefined)
    setGeo(next)
  }

  async function handleCancel() {
    setConfirmOpen(false)
    setCancelling(true)
    setCancelMsg(null)
    setCancelErr(false)
    try {
      await billing.cancelSubscription()
      await refreshBilling()
      setCancelMsg(t.settings.cancelled)
    } catch (err) {
      setCancelErr(true)
      if (err instanceof ApiError && err.body?.error === 'APPLE_MANAGED') {
        setCancelMsg(t.settings.cancelAppleNotice)
      } else {
        setCancelMsg(t.settings.cancelError)
      }
    } finally {
      setCancelling(false)
    }
  }

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>{isPro ? t.pricing.manageTitle : t.pricing.title}</h1>
      {!isPro && <p className={styles.subtitle}>{t.pricing.subtitle}</p>}

      {isPro && activeEntitlement && (
        <section className={styles.managePanel}>
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
            <p className={cancelErr ? styles.error : styles.notice}>{cancelMsg}</p>
          )}
          {isAppleManaged ? (
            <p className={styles.appleNotice}>{t.settings.cancelAppleNotice}</p>
          ) : (
            <button
              className={styles.dangerBtn}
              onClick={() => setConfirmOpen(true)}
              disabled={cancelling}
            >
              {t.settings.cancel}
            </button>
          )}
        </section>
      )}

      {cancelled && <div className={styles.notice}>{t.pricing.cancelled}</div>}
      {error && <div className={styles.error}>{error}</div>}

      <div className={styles.toggleRow}>
        <PlanToggle value={period} onChange={setPeriod} />
      </div>

      <div className={styles.plans}>
        <PlanCard
          name={t.pricing.free.name}
          price={t.pricing.free.price}
          bullets={t.pricing.free.bullets}
          badge={!isPro ? t.pricing.currentPlanBadge : undefined}
        />
        <PlanCard
          name={t.pricing.pro.name}
          price={proProduct ? formatPrice(proProduct) : '—'}
          pricePeriod={period === 'monthly' ? t.pricing.perMonth : t.pricing.perYear}
          bullets={t.pricing.pro.bullets}
          highlighted
          badge={proIsCurrent ? t.pricing.currentPlanBadge : 'PRO'}
        />
      </div>

      {!isPro && (
        <div className={styles.providerRow}>
          <ProviderSelector
            country={geo?.country ?? 'US'}
            isUkraine={geo?.isUkraine ?? false}
            loading={submitting}
            onSubscribe={handleSubscribe}
            onCountryChange={handleCountryOverride}
          />
        </div>
      )}

      {isPro && !proIsCurrent && (
        <p className={styles.switchHint}>{t.pricing.switchToCancel}</p>
      )}

      {!isPro && (
        <p className={styles.haveCode}>
          <Link to="/redeem">{(t as any).redeem?.haveCode ?? 'Have a code?'}</Link>
        </p>
      )}

      {confirmOpen && (
        <div className={styles.modalOverlay} onClick={() => setConfirmOpen(false)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <h3 className={styles.modalTitle}>{t.settings.cancelConfirmTitle}</h3>
            <p>{t.settings.cancelConfirmBody}</p>
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
