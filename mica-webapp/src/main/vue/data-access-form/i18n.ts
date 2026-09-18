import { createI18n } from 'vue-i18n';
import { messages as libraryMessages } from '@obiba/quasar-ui-json-form';

type Messages = Record<string, any>;

function isObject(value: unknown): value is Record<string, any> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Deep merge, `override` wins, except that a scalar never replaces a message namespace: the
 * Mica bundle has top-level scalars (`error: "Error"`, `upload`...) named like the library
 * namespaces (`error.required`, `files.upload`...), which must survive so that the form
 * messages stay translated; a custom translation can still override an individual key.
 */
export function deepMerge(base: Messages, override: Messages): Messages {
  const result: Messages = { ...base };
  Object.entries(override).forEach(([key, value]) => {
    if (isObject(value) && isObject(result[key])) {
      result[key] = deepMerge(result[key], value);
    } else if (!isObject(result[key])) {
      result[key] = value;
    }
  });
  return result;
}

export function createFormI18n(lang: string) {
  const defaults = (libraryMessages as Messages)[lang] || (libraryMessages as Messages).en;
  return createI18n({
    legacy: false,
    locale: lang,
    fallbackLocale: 'en',
    missingWarn: false,
    fallbackWarn: false,
    messages: { [lang]: defaults, en: (libraryMessages as Messages).en },
  });
}

/**
 * Loads the Mica translations (built-in + custom, the bundle angular-translate used to load)
 * and merges them over the library defaults. Not awaited by the mount: the renderers re-render
 * when the messages arrive.
 */
export async function loadMicaTranslations(i18n: ReturnType<typeof createFormI18n>, contextPath: string, lang: string): Promise<void> {
  const response = await fetch(`${contextPath}/ws/config/i18n/${lang}.json`, { headers: { Accept: 'application/json' } });
  if (!response.ok) {
    throw new Error(`Cannot load translations: ${response.status}`);
  }
  const translations = (await response.json()) as Messages;
  const current = i18n.global.getLocaleMessage(lang) as Messages;
  i18n.global.setLocaleMessage(lang, deepMerge(current, translations));
}
