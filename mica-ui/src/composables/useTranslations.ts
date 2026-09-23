import { api } from 'src/boot/api';
import { notifyError } from 'src/utils/notify';

/** the translations of a language, by dotted key */
export type Messages = Record<string, string>;

/** the subtree overwritten by the Agate translations on the server: customizing it has no effect */
const IGNORED_PREFIX = 'userProfile.';

/** flattens a JSON tree of translations into dotted keys, the non-string leaves are ignored */
export function flatten(tree: unknown, prefix = '', acc: Messages = {}): Messages {
  if (tree && typeof tree === 'object') {
    Object.entries(tree as Record<string, unknown>).forEach(([name, value]) => {
      const key = prefix ? `${prefix}.${name}` : name;
      if (typeof value === 'string') acc[key] = value;
      else flatten(value, key, acc);
    });
  }
  return acc;
}

/** rebuilds the JSON tree of translations from dotted keys */
export function unflatten(messages: Messages): Record<string, unknown> {
  const tree: Record<string, unknown> = {};
  Object.entries(messages).forEach(([key, value]) => {
    const path = key.split('.');
    const last = path.pop() as string;
    let node = tree;
    path.forEach((name) => {
      if (typeof node[name] !== 'object' || node[name] === null) node[name] = {};
      node = node[name] as Record<string, unknown>;
    });
    node[last] = value;
  });
  return tree;
}

/**
 * The custom translations of the public site (`/config/i18n/custom`), edited over the default ones
 * (`/config/i18n/{locale}.json?default=true`): a value is customized when it is stored in the custom
 * translations, a key is custom when it has no default in any language.
 */
export function useTranslations() {
  const languages = ref<string[]>([]);
  const defaults = ref<Record<string, Messages>>({});
  const custom = ref<Record<string, Messages>>({});
  /** the custom translations as last loaded or saved, to tell the unsaved changes */
  const saved = ref('{}');
  const loading = ref(false);
  const saving = ref(false);

  const dirty = computed(() => JSON.stringify(custom.value) !== saved.value);

  const keys = computed(() => {
    const all = new Set<string>();
    [...Object.values(defaults.value), ...Object.values(custom.value)].forEach((messages) =>
      Object.keys(messages).forEach((key) => all.add(key)),
    );
    return [...all].sort();
  });

  const hasDefault = (key: string) => languages.value.some((locale) => defaults.value[locale]?.[key] !== undefined);
  const isCustomized = (key: string, locale: string) => custom.value[locale]?.[key] !== undefined;
  const value = (key: string, locale: string) => custom.value[locale]?.[key] ?? defaults.value[locale]?.[key] ?? '';

  /** the number of customized values of each language */
  const customizedCounts = computed(() =>
    Object.fromEntries(languages.value.map((locale) => [locale, Object.keys(custom.value[locale] ?? {}).length])),
  );

  /** the keys whose key or value in any language contains the text (case insensitive) */
  function filterKeys(text: string, customizedOnly = false): string[] {
    const needle = text.trim().toLowerCase();
    return keys.value.filter(
      (key) =>
        (!customizedOnly || languages.value.some((locale) => isCustomized(key, locale))) &&
        (needle === '' ||
          key.toLowerCase().includes(needle) ||
          languages.value.some((locale) => value(key, locale).toLowerCase().includes(needle))),
    );
  }

  /** sets a value, which is no longer customized when it is back to the default one */
  function setValue(key: string, locale: string, text: string) {
    const messages = (custom.value[locale] ??= {});
    if (text === defaults.value[locale]?.[key]) delete messages[key];
    else messages[key] = text;
  }

  function reset(key: string, locale: string) {
    delete custom.value[locale]?.[key];
  }

  /** adds a custom key, empty in all the languages; tells whether it was added */
  function addKey(key: string): boolean {
    const name = key.trim();
    if (!/^[^.\s]+(\.[^.\s]+)*$/.test(name) || keys.value.includes(name) || name.startsWith(IGNORED_PREFIX))
      return false;
    languages.value.forEach((locale) => ((custom.value[locale] ??= {})[name] = ''));
    return true;
  }

  function removeKey(key: string) {
    languages.value.forEach((locale) => reset(key, locale));
  }

  /** loads the default and the custom translations of the languages, and tells whether they could be loaded */
  async function load(locales: string[]): Promise<boolean> {
    loading.value = true;
    try {
      const responses = await Promise.all(
        locales.map((locale) =>
          Promise.all([
            api.get(`/config/i18n/${locale}.json`, { params: { default: true } }),
            api.get(`/config/i18n/custom/${locale}.json`),
          ]),
        ),
      );
      const withoutIgnored = (messages: Messages) =>
        Object.fromEntries(Object.entries(messages).filter(([key]) => !key.startsWith(IGNORED_PREFIX)));
      languages.value = locales;
      defaults.value = Object.fromEntries(
        locales.map((locale, i) => [locale, withoutIgnored(flatten(responses[i]?.[0].data))]),
      );
      custom.value = Object.fromEntries(
        locales.map((locale, i) => [locale, withoutIgnored(flatten(responses[i]?.[1].data))]),
      );
      saved.value = JSON.stringify(custom.value);
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    } finally {
      loading.value = false;
    }
  }

  /** saves the custom translations of each language, and tells whether they were all saved */
  async function save(): Promise<boolean> {
    saving.value = true;
    try {
      for (const locale of languages.value) {
        await api.put(`/config/i18n/custom/${locale}.json`, unflatten(custom.value[locale] ?? {}), {
          params: { merge: false },
        });
      }
      saved.value = JSON.stringify(custom.value);
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    } finally {
      saving.value = false;
    }
  }

  /** the custom translations of all the languages, as stored on the server */
  async function exportAll(): Promise<Record<string, unknown> | undefined> {
    try {
      return (await api.get('/config/i18n/custom/export')).data;
    } catch (error) {
      notifyError(error);
      return undefined;
    }
  }

  /** the complete translations (default and custom) of a language, in the Gettext format */
  async function exportGettext(locale: string): Promise<string | undefined> {
    try {
      return (await api.get<string>(`/config/i18n/${locale}.po`, { responseType: 'text' })).data;
    } catch (error) {
      notifyError(error);
      return undefined;
    }
  }

  /**
   * Imports the custom translations of an export, merged into the current ones or replacing them (a language
   * missing from the export is then emptied), then reloads; tells whether it was imported.
   */
  async function importAll(text: string, merge: boolean): Promise<boolean> {
    try {
      const data: unknown = JSON.parse(text);
      if (!data || typeof data !== 'object' || Array.isArray(data))
        throw new Error('Translations must be a JSON object');
      const tree = data as Record<string, unknown>;
      // the server expects each language in a replace
      const payload = merge ? tree : Object.fromEntries(languages.value.map((locale) => [locale, tree[locale] ?? {}]));
      await api.put('/config/i18n/custom/import', payload, { params: { merge } });
    } catch (error) {
      notifyError(error);
      return false;
    }
    return load(languages.value);
  }

  return {
    languages,
    keys,
    loading,
    saving,
    dirty,
    customizedCounts,
    hasDefault,
    isCustomized,
    value,
    filterKeys,
    setValue,
    reset,
    addKey,
    removeKey,
    load,
    save,
    exportAll,
    exportGettext,
    importAll,
  };
}
