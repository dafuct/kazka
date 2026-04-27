import { createContext, useContext, useState, useCallback } from 'react'
import type { ReactNode } from 'react'
import { uk } from '../locales/uk'
import { en } from '../locales/en'
import type { Locale } from '../locales/uk'

type Lang = 'uk' | 'en'

const dictionaries: Record<Lang, Locale> = { uk, en }

interface LocaleCtx {
  lang: Lang
  t: Locale
  toggleLang: () => void
}

const LocaleContext = createContext<LocaleCtx | null>(null)

export function LocaleProvider({ children }: { children: ReactNode }) {
  const stored = (localStorage.getItem('lang') as Lang) ?? 'uk'
  const [lang, setLang] = useState<Lang>(stored)

  const toggleLang = useCallback(() => {
    setLang(prev => {
      const next: Lang = prev === 'uk' ? 'en' : 'uk'
      localStorage.setItem('lang', next)
      return next
    })
  }, [])

  return (
    <LocaleContext.Provider value={{ lang, t: dictionaries[lang], toggleLang }}>
      {children}
    </LocaleContext.Provider>
  )
}

export function useLocale(): LocaleCtx {
  const ctx = useContext(LocaleContext)
  if (!ctx) throw new Error('useLocale must be used within LocaleProvider')
  return ctx
}
