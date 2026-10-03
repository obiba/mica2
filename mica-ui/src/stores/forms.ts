import { defineStore } from 'pinia';
import { api } from 'src/boot/api';
import type { EntityFormDto } from 'src/models/Mica';
import { flattenMessages, type Messages } from 'src/utils/formTranslations';

/** a form pair as stored by Mica, parsed: the definition is an angular-schema-form array or a JSON Forms UI schema */
export interface AsfForm {
  schema: Record<string, unknown>;
  definition: unknown;
}

/**
 * The entity form configurations (`/config/{type}/form`), with their `t()` tokens resolved by the
 * server for a locale, cached per path and locale.
 */
export const useFormsStore = defineStore('forms', () => {
  const forms = ref<Record<string, AsfForm>>({});
  /** the Mica translations resolved for a language (`/config/i18n/{lang}.json`), flat by dotted key */
  const bundles = ref<Record<string, Messages>>({});

  function key(formPath: string, locale: string) {
    return `${formPath}?locale=${locale}`;
  }

  /** the requests in flight, shared by concurrent callers; removed once settled, so a failure is retried */
  const pendingForms = new Map<string, Promise<AsfForm>>();
  const pendingBundles = new Map<string, Promise<Messages>>();

  function getForm(formPath: string, locale: string): Promise<AsfForm> {
    const k = key(formPath, locale);
    const cached = forms.value[k];
    if (cached) return Promise.resolve(cached);
    let request = pendingForms.get(k);
    if (!request) {
      request = api
        .get<EntityFormDto>(formPath, { params: { locale } })
        .then((response) => {
          const form: AsfForm = {
            schema: JSON.parse(response.data.schema),
            definition: JSON.parse(response.data.definition),
          };
          forms.value[k] = form;
          return form;
        })
        .finally(() => pendingForms.delete(k));
      pendingForms.set(k, request);
    }
    return request;
  }

  /** the Mica messages of a language, by dotted key, cached; empty (and not cached) when the bundle cannot be read */
  function getBundle(language: string): Promise<Messages> {
    const cached = bundles.value[language];
    if (cached) return Promise.resolve(cached);
    let request = pendingBundles.get(language);
    if (!request) {
      request = api
        .get<unknown>(`/config/i18n/${language}.json`)
        .then((response) => {
          const messages = flattenMessages(response.data);
          bundles.value[language] = messages;
          return messages;
        })
        .catch((error) => {
          console.warn(`[forms] cannot read the ${language} translations`, error);
          return {};
        })
        .finally(() => pendingBundles.delete(language));
      pendingBundles.set(language, request);
    }
    return request;
  }

  function clear() {
    forms.value = {};
    bundles.value = {};
    pendingForms.clear();
    pendingBundles.clear();
  }

  return { forms, bundles, getForm, getBundle, clear };
});
