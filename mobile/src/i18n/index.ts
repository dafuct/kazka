import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import * as Localization from 'expo-localization';
import { en } from './en';
import { uk } from './uk';

const deviceLocale = Localization.getLocales()[0]?.languageCode;
const defaultLng = deviceLocale === 'uk' ? 'uk' : 'en';

void i18n
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: en },
      uk: { translation: uk },
    },
    lng: defaultLng,
    fallbackLng: 'en',
    interpolation: { escapeValue: false },
    compatibilityJSON: 'v4',
  });

export { i18n };
