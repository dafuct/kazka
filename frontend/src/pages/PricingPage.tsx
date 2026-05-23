import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import styles from './PricingPage.module.css'
import { PlanToggle, type Period } from '../components/billing/PlanToggle'
import { PlanCard } from '../components/billing/PlanCard'
import { ProviderSelector } from '../components/billing/ProviderSelector'
import { useLocale } from '../lib/LocaleContext'
import { useAuth } from '../lib/AuthContext'
import { useAuthModal } from '../lib/AuthModalContext'
import { useBilling } from '../lib/BillingContext'
import { billing } from '../lib/apiClient'
import type { Product, ProviderName, GeoResponse } from '../lib/types'

function formatPrice(p: Product): string {
  const dollars = p.priceMicro / 1_000_000
  if (p.currency === 'USD') return `$${dollars.toFixed(2)}`
  return `${dollars.toFixed(2)} ${p.currency}`
}

export function PricingPage() {
  const { t } = useLocale()
  const { user } = useAuth()
  const { openAuth } = useAuthModal()
  const { isPro } = useBilling()
  const [params] = useSearchParams()
  const [period, setPeriod] = useState<Period>('yearly')
  const [products, setProducts] = useState<Product[]>([])
  const [geo, setGeo] = useState<GeoResponse | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const cancelled = params.get('cancelled') === '1'

  useEffect(() => {
    billing.listProducts().then(setProducts).catch(() => setProducts([]))
    billing.geo().then(setGeo).catch(() => setGeo({ country: 'US', isUkraine: false }))
  }, [])

  const proProduct = useMemo(() => {
    const code = period === 'monthly' ? 'P1M' : 'P1Y'
    return products.find(p => p.period === code && p.tier === 'pro')
  }, [products, period])

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

  if (isPro) {
    return (
      <div className={styles.page}>
        <h1 className={styles.title}>{t.pricing.alreadyPro}</h1>
        <p className={styles.subtitle}>{t.pricing.manageOnApple}</p>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>{t.pricing.title}</h1>
      <p className={styles.subtitle}>{t.pricing.subtitle}</p>

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
        />
        <PlanCard
          name={t.pricing.pro.name}
          price={proProduct ? formatPrice(proProduct) : '—'}
          pricePeriod={period === 'monthly' ? t.pricing.perMonth : t.pricing.perYear}
          bullets={t.pricing.pro.bullets}
          highlighted
          badge="PRO"
        />
      </div>

      <div className={styles.providerRow}>
        <ProviderSelector
          country={geo?.country ?? 'US'}
          isUkraine={geo?.isUkraine ?? false}
          loading={submitting}
          onSubscribe={handleSubscribe}
          onCountryChange={handleCountryOverride}
        />
      </div>
    </div>
  )
}
