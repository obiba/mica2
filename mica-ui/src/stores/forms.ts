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

  async function getForm(formPath: string, locale: string): Promise<AsfForm> {
    const cached = forms.value[key(formPath, locale)];
    if (cached) return cached;
    const response = await api.get<EntityFormDto>(formPath, { params: { locale } });
    const form: AsfForm = {
      schema: JSON.parse(response.data.schema),
      definition: JSON.parse(response.data.definition),
    };
    forms.value[key(formPath, locale)] = form;
    return form;
  }

  /** the Mica messages of a language, by dotted key, cached; empty when the bundle cannot be read */
  async function getBundle(language: string): Promise<Messages> {
    const cached = bundles.value[language];
    if (cached) return cached;
    let messages: Messages = {};
    try {
      const response = await api.get<unknown>(`/config/i18n/${language}.json`);
      messages = flattenMessages(response.data);
    } catch (error) {
      console.warn(`[forms] cannot read the ${language} translations`, error);
    }
    bundles.value[language] = messages;
    return messages;
  }

  function clear() {
    forms.value = {};
    bundles.value = {};
  }

  return { forms, bundles, getForm, getBundle, clear };
});
