/**
 * The bridge between the `t(key)` tokens of the Mica form configurations and the translation keys
 * of the form builder: the texts of a form are stored with it, by language, and the server resolves
 * its tokens from them before the Mica translations.
 */

/** a string made of a single `t(key)` token */
const KEY_TOKEN = /^t\(\s*([^()]+?)\s*\)$/;

/** the keys of a schema or UI schema whose string values are never translation tokens */
const UNTRANSLATED_KEYS = new Set([
  'type',
  'format',
  'pattern',
  'enum',
  'const',
  'default',
  'required',
  '$ref',
  'key',
  'scope',
  'condition',
  'expr',
  'wordLimit',
  'htmlClass',
  'dateFormat',
]);

export type Messages = Record<string, string>;

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/** the key of a string that is exactly a `t(key)` token, else undefined */
export function tokenKey(value: unknown): string | undefined {
  if (typeof value !== 'string') return undefined;
  const match = KEY_TOKEN.exec(value);
  return match ? match[1] : undefined;
}

export function toToken(key: string): string {
  return `t(${key})`;
}

/** the strings of a schema or UI schema, replaced but for the ones under the untranslated keys */
function mapStrings<T>(value: T, replace: (text: string) => string, key?: string): T {
  if (typeof value === 'string') {
    return (key !== undefined && UNTRANSLATED_KEYS.has(key) ? value : replace(value)) as T;
  }
  if (Array.isArray(value)) {
    return value.map((item) => mapStrings(item, replace, key)) as T;
  }
  if (isObject(value)) {
    const result: Record<string, unknown> = {};
    Object.keys(value).forEach((name) => {
      result[name] = mapStrings(value[name], replace, name);
    });
    return result as T;
  }
  return value;
}

/**
 * Replaces, in a schema or UI schema, every string that is exactly a `t(key)` token of a known key
 * by that key. Any other text is left as it is: a token of an unknown key (one of the Mica bundle,
 * from a legacy form) or a text mixing a token with other content are still resolved by the server
 * at render time.
 */
export function unwrapKeys<T>(value: T, isKnown: (key: string) => boolean): T {
  return mapStrings(value, (text) => {
    const token = tokenKey(text);
    return token !== undefined && isKnown(token) ? token : text;
  });
}

/** Replaces, in a schema or UI schema, every string that is exactly a known key by its `t(key)` token. */
export function wrapKeys<T>(value: T, isKnown: (key: string) => boolean): T {
  return mapStrings(value, (text) => (isKnown(text) ? toToken(text) : text));
}
