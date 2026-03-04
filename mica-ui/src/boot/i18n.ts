import { defineBoot } from '#q-app/wrappers';
import { createI18n } from 'vue-i18n';
import messages from 'src/i18n';
import { Cookies } from 'quasar';

const SUPPORTED_LOCALES = ['en', 'fr'];
const DEFAULT_LOCALE = 'en';

function detectLocale(): string {
  // 1. Check cookie
  const cookieLocale = Cookies.get('locale');
  if (cookieLocale && SUPPORTED_LOCALES.includes(cookieLocale)) {
    return cookieLocale;
  }

  // 2. Browser language
  const browserLang = navigator.language?.split('-')[0];
  if (browserLang && SUPPORTED_LOCALES.includes(browserLang)) {
    return browserLang;
  }

  return DEFAULT_LOCALE;
}

export const i18n = createI18n({
  locale: detectLocale(),
  fallbackLocale: DEFAULT_LOCALE,
  legacy: false,
  globalInjection: true,
  messages,
});

export default defineBoot(({ app }) => {
  app.use(i18n);
});
