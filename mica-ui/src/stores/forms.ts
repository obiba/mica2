import { defineStore } from 'pinia';
import { api } from 'src/boot/api';
import type { EntityFormDto } from 'src/models/Mica';

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

  function clear() {
    forms.value = {};
  }

  return { forms, getForm, clear };
});
