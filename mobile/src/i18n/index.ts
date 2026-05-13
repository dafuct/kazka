import i18n, { type LanguageDetectorModule } from 'i18next';
import { initReactI18next } from 'react-i18next';
import * as Localization from 'expo-localization';
import { createMMKV } from 'react-native-mmkv';
import { en } from './en';
import { uk } from './uk';

const localeStorage = createMMKV({ id: 'kazka-locale' });
const STORAGE_KEY = 'language';

const mmkvDetector: LanguageDetectorModule = {
  type: 'languageDetector',
  init: () => {},
  detect: () => {
    const saved = localeStorage.getString(STORAGE_KEY);
    if (saved === 'uk' || saved === 'en') return saved;
    const deviceLocale = Localization.getLocales()[0]?.languageCode;
    return deviceLocale === 'uk' ? 'uk' : 'en';
  },
  cacheUserLanguage: (lng: string) => {
    if (lng === 'uk' || lng === 'en') localeStorage.set(STORAGE_KEY, lng);
  },
};

void i18n
  .use(mmkvDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: en },
      uk: { translation: uk },
    },
    fallbackLng: 'en',
    interpolation: { escapeValue: false },
    compatibilityJSON: 'v4',
  });

export { i18n };
