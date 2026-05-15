import { boot } from 'quasar/wrappers';
import { createI18n } from 'vue-i18n';
import messages from 'src/i18n';
import { Quasar, Cookies } from 'quasar';

export type MessageLanguages = keyof typeof messages;
// Type-define 'en-US' as the master schema for the resource
export type MessageSchema = (typeof messages)['en'];

// See https://vue-i18n.intlify.dev/guide/advanced/typescript.html#global-resource-schema-type-definition
/* eslint-disable @typescript-eslint/no-empty-object-type */
declare module 'vue-i18n' {
  // define the locale messages schema
  export interface DefineLocaleMessage extends MessageSchema {}

  // define the datetime format schema
  export interface DefineDateTimeFormat {}

  // define the number format schema
  export interface DefineNumberFormat {}
}
/* eslint-enable @typescript-eslint/no-empty-object-type */

const defaultLocales = ['en', 'fr'];

const locales = defaultLocales;

function getCurrentLocale(): string {
  let detectedLocale = Cookies.get('locale')
    ? Cookies.get('locale') // previously selected
    : Quasar.lang.getLocale(); // browser
  if (!detectedLocale) {
    detectedLocale = locales[0];
  } else if (!locales.includes(detectedLocale)) {
    detectedLocale = detectedLocale.split('-')[0];
    if (!detectedLocale || !locales.includes(detectedLocale)) {
      detectedLocale = locales[0];
    }
  }
  return detectedLocale || locales[0] || 'en';
}

// function mergeWithCustomMessages() {
//   const serverTranslations = translationAsMap(systemStore.configuration.translations || []);
//
//   Object.keys(serverTranslations).forEach((lang) => {
//     const existingMessages = i18n.global.getLocaleMessage(lang) || {};
//     const newMessages = (serverTranslations[lang] || ([] as AttributeDto[])).reduce(
//       (acc, tr) => {
//         if (tr.name && tr.values && tr.values.length > 0) {
//           const localeValue = tr.values.find((v) => v.lang === lang)?.value || tr.values?.[0]?.value;
//           acc[tr.name] = localeValue || '';
//         }
//         return acc;
//       },
//       {} as Record<string, string>,
//     );
//
//     i18n.global.setLocaleMessage(lang, {
//       ...existingMessages,
//       ...newMessages,
//     });
//   });
// }

const i18n = createI18n<{ message: MessageSchema }, MessageLanguages>({
  locale: getCurrentLocale(),
  fallbackLocale: locales[0] || 'en',
  globalInjection: true,
  legacy: false,
  messages,
});

export default boot(({ app }) => {
  // Set i18n instance on app
  app.use(i18n);
});

// const systemStore = useSystemStore();

// watch(
//   () => systemStore.configuration.translations,
//   (newValue) => {
//     if (newValue) {
//       mergeWithCustomMessages();
//     }
//   },
// );

const t = i18n.global.t;

export { i18n, t, locales, getCurrentLocale };
