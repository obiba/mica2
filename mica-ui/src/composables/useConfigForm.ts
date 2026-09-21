import { toJsonForms } from '@obiba/quasar-ui-json-form';
import type { AsfDiagnostic } from '@obiba/quasar-ui-json-form';
import {
  descendants,
  fromDefinition,
  pruneTranslations,
  rawText,
  setText,
  textSlots,
  toDefinition,
} from '@obiba/quasar-ui-json-form/builder';
import type { FormDefinition, FormModel, FormNode } from '@obiba/quasar-ui-json-form/builder';
import type { FormTranslations as StoredTranslations } from '@obiba/quasar-ui-json-form';
import { api } from 'src/boot/api';
import { useFormsStore } from 'src/stores/forms';
import { useSystemStore } from 'src/stores/system';
import { notifyError } from 'src/utils/notify';
import {
  resolveTokens,
  toToken,
  tokenKey,
  tokenKeys,
  translatableStrings,
  unwrapKeys,
  wrapKeys,
  type Messages,
} from 'src/utils/formTranslations';

/** what every form configuration DTO holds */
export interface ConfigFormDto {
  schema: string;
  definition: string;
  translations?: string | undefined;
  /** the forms with a draft / published lifecycle */
  revision?: number | undefined;
  lastUpdateDate?: string | undefined;
}

/** a form configuration edited with the builder: where it is and how it is saved */
export interface ConfigFormSource<D extends ConfigFormDto = ConfigFormDto> {
  /** the resource: `/config/network/form-custom`, `/config/data-access-form` */
  path: string;
  /** the query parameters of the load (`{ revision: 'draft' }`) */
  params?: Record<string, string>;
  /** the fields of the loaded DTO sent back with the form (the form pair and the texts are set by the composable) */
  payload: (dto: D) => Record<string, unknown>;
  /** the form has a published revision: `GET ?revision=latest`, `PUT {path}/_publish` */
  revisions?: boolean;
  /** a form loaded in the legacy dialect (converted by the builder) has to be saved: it is reported as changed */
  legacyIsDirty?: boolean;
}

/** the texts of a form, by language then by dotted key */
export type FormTranslations = Record<string, Messages>;

/** the published revision of a form */
export interface PublishedRevision {
  revision: number;
  lastUpdateDate?: string | undefined;
}

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

/** the node, its children and the item nodes of its lists, recursively */
function allNodes(node: FormNode): FormNode[] {
  const nodes = [node, ...descendants(node)];
  const details = nodes.flatMap((n) => (n.detail ? allNodes(n.detail) : []));
  return [...nodes, ...details];
}

/**
 * The keys of the `t()` tokens of the form that its own texts do not translate: the keys of the Mica
 * bundle a legacy form relies on.
 */
export function untranslatedTokens(definition: FormDefinition): string[] {
  const isKnown = knownKeys(definition.translations ?? {});
  const keys = new Set<string>();
  [...translatableStrings(definition.schema), ...translatableStrings(definition.uischema ?? {})].forEach((text) =>
    tokenKeys(text).forEach((key) => {
      if (!isKnown(key)) keys.add(key);
    }),
  );
  return Array.from(keys);
}

/**
 * Seeds the texts of the builder with the Mica messages of the `t()` tokens the form does not translate
 * itself (a legacy form), for the given languages: a string that is exactly `t(key)` keeps `key` as its
 * translation key, with the message of each language as its text; a token embedded in a text (an HTML
 * help block) is replaced by its message in every language, the text then owned by the builder under a
 * key of its own. A token whose key no bundle knows is left as it is, resolved by the server at render
 * time. Returns the number of keys seeded.
 */
export function seedTranslations(
  model: FormModel,
  bundles: Record<string, Messages>,
  /** the model as a definition, when the caller has it already */
  definition: Required<FormDefinition> = toDefinition(model),
): number {
  const languages = Object.keys(bundles);
  const isKnown = knownKeys(model.translations);
  const known = (key: string) => languages.some((language) => key in (bundles[language] as Messages));
  let seeded = 0;

  // the strings made of a single token: the key stays, the texts come from the bundles
  const wholeKeys = new Set<string>();
  [...translatableStrings(definition.schema), ...translatableStrings(definition.uischema)].forEach((text) => {
    const key = tokenKey(text);
    if (key !== undefined && !isKnown(key) && known(key)) wholeKeys.add(key);
  });
  wholeKeys.forEach((key) => {
    languages.forEach((language) => {
      const message = (bundles[language] as Messages)[key];
      if (message === undefined) return;
      const texts = (model.translations[language] ??= {});
      texts[key] = message;
    });
    seeded++;
  });

  // the tokens embedded in the texts of the nodes: the builder owns the resolved text
  allNodes(model.root).forEach((node) => {
    textSlots(model, node).forEach((slot) => {
      const raw = rawText(model, node, slot);
      if (raw === undefined || tokenKey(raw) !== undefined || !tokenKeys(raw).some(known)) return;
      languages.forEach((language) => {
        const resolved = resolveTokens(raw, bundles[language] as Messages);
        if (resolved !== raw) setText(model, node, slot, language, resolved);
      });
      seeded++;
    });
  });
  return seeded;
}

/**
 * A form configuration as a form definition for `QJsonFormBuilder`: the `t(key)` tokens of the keys
 * translated by the form become translation keys, the texts being stored with the form; the tokens
 * of a legacy form are seeded from the Mica bundles.
 */
export function useConfigForm<D extends ConfigFormDto = ConfigFormDto>(source: ConfigFormSource<D>) {
  const formsStore = useFormsStore();
  const systemStore = useSystemStore();

  const loading = ref(false);
  const saving = ref(false);
  const publishing = ref(false);
  const form = ref<FormDefinition>();
  /** the DTO as loaded: its other fields are sent back on save */
  const dto = ref<D>();
  const diagnostics = ref<AsfDiagnostic[]>([]);
  /** true when the builder changed the form since it was loaded */
  const dirty = ref(false);
  /** true when the loaded form was in the legacy dialect (angular-schema-form), converted on load */
  const legacy = ref(false);
  /** the published revision, for a form with revisions */
  const published = ref<PublishedRevision>();
  /** true when the saved draft is newer than the published revision */
  const canPublish = computed(
    () =>
      source.revisions === true &&
      dto.value !== undefined &&
      published.value !== undefined &&
      dto.value.lastUpdateDate !== published.value.lastUpdateDate,
  );

  /** the Mica messages of the configured languages, by language */
  async function bundles(): Promise<Record<string, Messages>> {
    const languages = systemStore.languages;
    const messages = await Promise.all(languages.map((language) => formsStore.getBundle(language)));
    return Object.fromEntries(languages.map((language, index) => [language, messages[index] as Messages]));
  }

  async function loadPublished(): Promise<void> {
    if (!source.revisions) return;
    try {
      const response = await api.get<D>(source.path, { params: { revision: 'latest' } });
      published.value = { revision: response.data.revision ?? 0, lastUpdateDate: response.data.lastUpdateDate };
    } catch (error) {
      published.value = undefined;
      notifyError(error);
    }
  }

  async function load(): Promise<void> {
    loading.value = true;
    try {
      const response = source.params
        ? await api.get<D>(source.path, { params: source.params })
        : await api.get<D>(source.path);
      dto.value = response.data;
      const definition: unknown = JSON.parse(response.data.definition);
      legacy.value = Array.isArray(definition);
      const converted = toJsonForms(JSON.parse(response.data.schema), definition, {
        translate: toToken,
        logger: (diagnostic: AsfDiagnostic) => {
          const log = diagnostic.level === 'warn' ? console.warn : console.info;
          log(`[${source.path}] ${diagnostic.message}`, diagnostic.key ?? '', diagnostic.element ?? '');
        },
      });
      diagnostics.value = converted.diagnostics;
      const model = fromDefinition({
        schema: converted.schema,
        uischema: converted.uischema,
        translations: parseTranslations(response.data.translations),
      });
      // the form as the builder models it: its translations flat, by dotted key
      let modelled = toDefinition(model);
      // a legacy form relies on the Mica bundle: its texts are brought into the builder
      if (untranslatedTokens(modelled).length > 0) {
        seedTranslations(model, await bundles(), modelled);
        modelled = toDefinition(model);
      }
      const isKnown = knownKeys(modelled.translations);
      form.value = {
        schema: unwrapKeys(modelled.schema, isKnown),
        uischema: unwrapKeys(modelled.uischema, isKnown),
        translations: modelled.translations,
      };
      await loadPublished();
      // a mounted builder echoes the form it is given, once modelled: not a change
      await nextTick();
      dirty.value = source.legacyIsDirty === true && legacy.value;
    } catch (error) {
      form.value = undefined;
      dto.value = undefined;
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

  /** saves the form with its texts and the other fields of the DTO, and reloads it */
  async function save(model: FormModel): Promise<boolean> {
    if (!dto.value) return false;
    saving.value = true;
    try {
      const prepared = prepareForm(model);
      const payload: Record<string, unknown> = {
        ...source.payload(dto.value),
        schema: JSON.stringify(prepared.schema),
        definition: JSON.stringify(prepared.uischema),
      };
      if (Object.keys(prepared.translations).length > 0) payload.translations = JSON.stringify(prepared.translations);
      await api.put(source.path, payload);
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

  /** publishes the saved draft as the next revision */
  async function publish(): Promise<boolean> {
    if (!source.revisions) return false;
    publishing.value = true;
    try {
      await api.put(`${source.path}/_publish`);
      await loadPublished();
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    } finally {
      publishing.value = false;
    }
  }

  return {
    source,
    loading,
    saving,
    publishing,
    form,
    dto,
    diagnostics,
    dirty,
    legacy,
    published,
    canPublish,
    load,
    update,
    save,
    publish,
  };
}

/** the state of a form configuration, as returned by `useConfigForm` */
export type ConfigFormState<D extends ConfigFormDto = ConfigFormDto> = ReturnType<typeof useConfigForm<D>>;
