import type { MaybeRefOrGetter } from 'vue';
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
import {
  flattenMessages,
  nestMessages,
  toToken,
  unwrapKeys,
  type Messages,
  type NestedMessages,
} from 'src/utils/formTranslations';

export interface EntityConfigTarget {
  /** the configuration resource: `network` for `/config/network/form-custom` */
  name: string;
  type: EntityFormDto_Type;
}

/** the translations of a form, by language then by dotted key */
type FormTranslations = Record<string, Messages>;

/** the prefix of the keys created by the builder, a namespace of its own in the Mica bundle */
export function formKeyPrefix(target: EntityConfigTarget): string {
  return `${target.name}-form.`;
}

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

/** every string held by a text slot of the form: the translation keys and the literals */
function slotTexts(model: FormModel): string[] {
  const texts = new Set<string>();
  for (const { node } of locations(model)) {
    for (const slot of textSlots(model, node)) {
      const raw = rawText(model, node, slot);
      if (raw !== undefined && raw !== '') texts.add(raw);
    }
  }
  return [...texts];
}

/** the result of the preparation of a form for the server */
export interface PreparedForm {
  schema: Record<string, unknown>;
  uischema: Record<string, unknown>;
  /** the translations to add to the Mica bundle, by language then by (prefixed) dotted key */
  translations: FormTranslations;
}

/**
 * Writes the keys of the form as `t(key)` tokens for the server: a key loaded from the Mica
 * bundle is kept as it is, a key created by the builder gets the form prefix. Returns the
 * translations of the keys the form uses that differ from the loaded ones.
 */
export function prepareForm(
  model: FormModel,
  target: EntityConfigTarget,
  loadedKeys: Set<string>,
  loaded: FormTranslations,
): PreparedForm {
  // a detached copy: the builder keeps its model
  const copy = fromDefinition(toDefinition(model));
  const prefix = formKeyPrefix(target);
  /** the key stored, by the key held in the builder */
  const renamed = new Map<string, string>();
  for (const { node } of locations(copy)) {
    for (const slot of textSlots(copy, node)) {
      const raw = rawText(copy, node, slot);
      if (raw === undefined || !isKnownKey(copy, raw)) continue;
      const key = loadedKeys.has(raw) || raw.startsWith(prefix) ? raw : `${prefix}${raw}`;
      renamed.set(raw, key);
      const holder = slotTarget(copy, node, slot);
      if (holder && getPath(holder, slot.path) === raw) setPath(holder, slot.path, toToken(key));
    }
  }
  const translations: FormTranslations = {};
  Object.entries(copy.translations).forEach(([language, messages]) => {
    const changed: Messages = {};
    renamed.forEach((key, raw) => {
      const text = messages[raw];
      if (text === undefined || text === '' || loaded[language]?.[raw] === text) return;
      changed[key] = text;
    });
    if (Object.keys(changed).length > 0) translations[language] = changed;
  });
  const definition = toDefinition(copy);
  return { schema: definition.schema, uischema: definition.uischema, translations };
}

/**
 * The custom part of the form configuration of a document type (`/config/{type}/form-custom`),
 * as a form definition for `QJsonFormBuilder`: the `t(key)` tokens become translation keys, with
 * the texts of the Mica bundle of every language of the configuration.
 */
export function useEntityConfigForm(target: EntityConfigTarget, languages: MaybeRefOrGetter<string[]>) {
  const formsStore = useFormsStore();

  const loading = ref(false);
  const saving = ref(false);
  const form = ref<FormDefinition>();
  const diagnostics = ref<AsfDiagnostic[]>([]);
  /** what was loaded, for the dirty check */
  const snapshot = ref('');
  /** the keys found in the Mica bundle, kept as they are on save */
  let loadedKeys = new Set<string>();
  let loadedTranslations: FormTranslations = {};

  const dirty = computed(() => form.value !== undefined && JSON.stringify(form.value) !== snapshot.value);

  async function loadBundle(language: string): Promise<Messages> {
    const response = await api.get<NestedMessages>(`/config/i18n/${language}.json`);
    return flattenMessages(response.data);
  }

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
      const definition: FormDefinition = {
        schema: unwrapKeys(converted.schema),
        uischema: unwrapKeys(converted.uischema),
      };
      const texts = slotTexts(fromDefinition(definition));
      const codes = toValue(languages);
      const bundles = await Promise.all(codes.map(loadBundle));
      const translations: FormTranslations = {};
      const keys = new Set<string>();
      codes.forEach((language, index) => {
        const bundle = bundles[index] ?? {};
        const messages: Messages = {};
        texts.forEach((text) => {
          const value = bundle[text];
          if (value !== undefined) {
            messages[text] = value;
            keys.add(text);
          }
        });
        translations[language] = messages;
      });
      loadedKeys = keys;
      loadedTranslations = JSON.parse(JSON.stringify(translations));
      form.value = { ...definition, translations };
      snapshot.value = JSON.stringify(form.value);
    } catch (error) {
      form.value = undefined;
      notifyError(error);
    } finally {
      loading.value = false;
    }
  }

  /** saves the translations then the form, and reloads it */
  async function save(model: FormModel): Promise<boolean> {
    saving.value = true;
    try {
      const prepared = prepareForm(model, target, loadedKeys, loadedTranslations);
      if (Object.keys(prepared.translations).length > 0) {
        const nested: Record<string, NestedMessages> = {};
        Object.entries(prepared.translations).forEach(([language, messages]) => {
          nested[language] = nestMessages(messages);
        });
        await api.put('/config/i18n/custom/import', nested, { params: { merge: true } });
      }
      const dto: EntityFormDto = {
        type: target.type,
        schema: JSON.stringify(prepared.schema),
        definition: JSON.stringify(prepared.uischema),
      };
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
