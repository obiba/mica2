import { toJsonForms } from '@obiba/quasar-ui-json-form';
import type { AsfDiagnostic } from '@obiba/quasar-ui-json-form';
import { fromDefinition, toDefinition, pruneTranslations } from '@obiba/quasar-ui-json-form/builder';
import type { FormDefinition, FormModel } from '@obiba/quasar-ui-json-form/builder';
import type { FormTranslations as StoredTranslations } from '@obiba/quasar-ui-json-form';
import { api } from 'src/boot/api';
import type { EntityFormDto, EntityFormDto_Type } from 'src/models/Mica';
import { useFormsStore } from 'src/stores/forms';
import { notifyError } from 'src/utils/notify';
import { toToken, unwrapKeys, wrapKeys, type Messages } from 'src/utils/formTranslations';

/** the configuration resource (`network` for `/config/network/form-custom`) and the type of its form DTO */
export type EntityConfigTarget =
  | { name: string; type: EntityFormDto_Type }
  /** the project form has its own DTO, without type */
  | { name: 'project'; type?: undefined };

/** the form as sent to `/config/{type}/form-custom`: an EntityFormDto, or a ProjectFormDto without type */
export type EntityFormPayload = Omit<EntityFormDto, 'type'> & Partial<Pick<EntityFormDto, 'type'>>;

/** the texts of a form, by language then by dotted key */
export type FormTranslations = Record<string, Messages>;

/** the translations stored with a form (`{ "en": { "<key>": "<text>" } }`, nested or flat), as they are */
export function parseTranslations(json: string | undefined): StoredTranslations {
  if (!json) return {};
  try {
    const parsed: unknown = JSON.parse(json);
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) return {};
    return parsed as StoredTranslations;
  } catch {
    return {};
  }
}

/** true when a language of the form defines the key */
function knownKeys(translations: StoredTranslations): (key: string) => boolean {
  const messages = Object.values(translations);
  return (key) => messages.some((texts) => key in texts);
}

/** the texts of a form, without the empty ones and the languages left without text */
function nonEmptyTexts(translations: StoredTranslations): FormTranslations {
  const result: FormTranslations = {};
  Object.entries(translations).forEach(([language, messages]) => {
    const texts: Messages = {};
    Object.entries(messages).forEach(([key, text]) => {
      if (typeof text === 'string' && text !== '') texts[key] = text;
    });
    if (Object.keys(texts).length > 0) result[language] = texts;
  });
  return result;
}

/** the result of the preparation of a form for the server */
export interface PreparedForm {
  schema: Record<string, unknown>;
  uischema: Record<string, unknown>;
  /** the texts of the keys the form uses, by language, without the empty ones */
  translations: FormTranslations;
}

/**
 * Writes the keys of the form as `t(key)` tokens for the server, with the texts of the keys it uses.
 */
export function prepareForm(model: FormModel): PreparedForm {
  // a detached copy: the builder keeps its model
  const copy = fromDefinition(toDefinition(model));
  pruneTranslations(copy);
  const definition = toDefinition(copy);
  const isKnown = knownKeys(definition.translations);
  return {
    schema: wrapKeys(definition.schema, isKnown),
    uischema: wrapKeys(definition.uischema, isKnown),
    translations: nonEmptyTexts(definition.translations),
  };
}

/**
 * The custom part of the form configuration of a document type (`/config/{type}/form-custom`),
 * as a form definition for `QJsonFormBuilder`: the `t(key)` tokens of the keys translated by the
 * form become translation keys, the texts being stored with the form.
 */
export function useEntityConfigForm(target: EntityConfigTarget) {
  const formsStore = useFormsStore();

  const loading = ref(false);
  const saving = ref(false);
  const form = ref<FormDefinition>();
  const diagnostics = ref<AsfDiagnostic[]>([]);
  /** true when the builder changed the form since it was loaded */
  const dirty = ref(false);

  async function load(): Promise<void> {
    loading.value = true;
    try {
      const response = await api.get<EntityFormDto>(`/config/${target.name}/form-custom`);
      const converted = toJsonForms(JSON.parse(response.data.schema), JSON.parse(response.data.definition), {
        translate: toToken,
        logger: (diagnostic: AsfDiagnostic) => {
          const log = diagnostic.level === 'warn' ? console.warn : console.info;
          log(`[${target.name}] ${diagnostic.message}`, diagnostic.key ?? '', diagnostic.element ?? '');
        },
      });
      diagnostics.value = converted.diagnostics;
      // the form as the builder models it: its translations flat, by dotted key
      const definition = toDefinition(
        fromDefinition({
          schema: converted.schema,
          uischema: converted.uischema,
          translations: parseTranslations(response.data.translations),
        }),
      );
      const isKnown = knownKeys(definition.translations);
      form.value = {
        schema: unwrapKeys(definition.schema, isKnown),
        uischema: unwrapKeys(definition.uischema, isKnown),
        translations: definition.translations,
      };
      // a mounted builder echoes the form it is given, once modelled: not a change
      await nextTick();
      dirty.value = false;
    } catch (error) {
      form.value = undefined;
      notifyError(error);
    } finally {
      loading.value = false;
    }
  }

  /** the form as changed by the builder */
  function update(definition: FormDefinition): void {
    form.value = definition;
    dirty.value = true;
  }

  /** saves the form with its texts, and reloads it */
  async function save(model: FormModel): Promise<boolean> {
    saving.value = true;
    try {
      const prepared = prepareForm(model);
      const dto: EntityFormPayload = {
        schema: JSON.stringify(prepared.schema),
        definition: JSON.stringify(prepared.uischema),
      };
      if (target.type) dto.type = target.type;
      if (Object.keys(prepared.translations).length > 0) dto.translations = JSON.stringify(prepared.translations);
      await api.put(`/config/${target.name}/form-custom`, dto);
      formsStore.clear();
      await load();
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    } finally {
      saving.value = false;
    }
  }

  return { loading, saving, form, diagnostics, dirty, load, update, save };
}
