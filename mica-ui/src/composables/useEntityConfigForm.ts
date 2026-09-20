import { toJsonForms } from '@obiba/quasar-ui-json-form';
import type { AsfDiagnostic } from '@obiba/quasar-ui-json-form';
import {
  fromDefinition,
  toDefinition,
  locations,
  textSlots,
  rawText,
  isKnownKey,
  propertySchema,
} from '@obiba/quasar-ui-json-form/builder';
import type { FormDefinition, FormModel, FormNode, TextSlot } from '@obiba/quasar-ui-json-form/builder';
import { api } from 'src/boot/api';
import type { EntityFormDto, EntityFormDto_Type } from 'src/models/Mica';
import { useFormsStore } from 'src/stores/forms';
import { notifyError } from 'src/utils/notify';
import { flattenMessages, toToken, unwrapKeys, type Messages } from 'src/utils/formTranslations';

export interface EntityConfigTarget {
  /** the configuration resource: `network` for `/config/network/form-custom` */
  name: string;
  type: EntityFormDto_Type;
}

/** the translations of a form, by language then by dotted key */
export type FormTranslations = Record<string, Messages>;

type Path = (string | number)[];

function getPath(target: Record<string, unknown> | undefined, path: Path): unknown {
  let value: unknown = target;
  for (const segment of path) {
    if (typeof value !== 'object' || value === null) return undefined;
    value = (value as Record<string | number, unknown>)[segment];
  }
  return value;
}

function setPath(target: Record<string, unknown>, path: Path, value: unknown): void {
  let node: Record<string | number, unknown> = target;
  path.slice(0, -1).forEach((segment, index) => {
    const child = node[segment];
    if (typeof child !== 'object' || child === null) {
      node[segment] = typeof path[index + 1] === 'number' ? [] : {};
    }
    node = node[segment] as Record<string | number, unknown>;
  });
  node[path[path.length - 1] as string | number] = value;
}

/** the object holding a text slot: the property schema of a control, or the UI schema element */
function slotTarget(model: FormModel, node: FormNode, slot: TextSlot): Record<string, unknown> | undefined {
  return slot.target === 'schema' ? propertySchema(model, node.id) : node.element;
}

/** the translations stored with a form (`{ "en": { "<key>": "<text>" } }`, nested or flat), flat by language */
export function parseTranslations(json: string | undefined): FormTranslations {
  if (!json) return {};
  try {
    const parsed: unknown = JSON.parse(json);
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) return {};
    const translations: FormTranslations = {};
    Object.entries(parsed).forEach(([language, messages]) => {
      translations[language] = flattenMessages(messages);
    });
    return translations;
  } catch {
    return {};
  }
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
  const used = new Set<string>();
  for (const { node } of locations(copy)) {
    for (const slot of textSlots(copy, node)) {
      const raw = rawText(copy, node, slot);
      if (raw === undefined || !isKnownKey(copy, raw)) continue;
      used.add(raw);
      const holder = slotTarget(copy, node, slot);
      if (holder && getPath(holder, slot.path) === raw) setPath(holder, slot.path, toToken(raw));
    }
  }
  const translations: FormTranslations = {};
  Object.entries(copy.translations).forEach(([language, messages]) => {
    const texts: Messages = {};
    used.forEach((key) => {
      const text = messages[key];
      if (text !== undefined && text !== '') texts[key] = text;
    });
    if (Object.keys(texts).length > 0) translations[language] = texts;
  });
  const definition = toDefinition(copy);
  return { schema: definition.schema, uischema: definition.uischema, translations };
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
  /** what was loaded, for the dirty check */
  const snapshot = ref('');

  const dirty = computed(() => form.value !== undefined && JSON.stringify(form.value) !== snapshot.value);

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
      const translations = parseTranslations(response.data.translations);
      const isKnown = (key: string) => Object.values(translations).some((messages) => key in messages);
      form.value = {
        schema: unwrapKeys(converted.schema, isKnown),
        uischema: unwrapKeys(converted.uischema, isKnown),
        translations,
      };
      snapshot.value = JSON.stringify(form.value);
    } catch (error) {
      form.value = undefined;
      notifyError(error);
    } finally {
      loading.value = false;
    }
  }

  /** saves the form with its texts, and reloads it */
  async function save(model: FormModel): Promise<boolean> {
    saving.value = true;
    try {
      const prepared = prepareForm(model);
      const dto: EntityFormDto = {
        type: target.type,
        schema: JSON.stringify(prepared.schema),
        definition: JSON.stringify(prepared.uischema),
      };
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

  return { loading, saving, form, diagnostics, dirty, load, save };
}
