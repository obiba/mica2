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
export type NestedMessages = { [key: string]: string | NestedMessages };

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

/**
 * Replaces, in a schema or UI schema, every string that is exactly a `t(key)` token of a known key
 * by that key. Any other text is left as it is: a token of an unknown key (one of the Mica bundle,
 * from a legacy form) or a text mixing a token with other content are still resolved by the server
 * at render time.
 */
export function unwrapKeys<T>(value: T, isKnown: (key: string) => boolean, key?: string): T {
  if (typeof value === 'string') {
    if (key !== undefined && UNTRANSLATED_KEYS.has(key)) return value;
    const token = tokenKey(value);
    return (token !== undefined && isKnown(token) ? token : value) as T;
  }
  if (Array.isArray(value)) {
    return value.map((item) => unwrapKeys(item, isKnown, key)) as T;
  }
  if (isObject(value)) {
    const result: Record<string, unknown> = {};
    Object.keys(value).forEach((name) => {
      result[name] = unwrapKeys(value[name], isKnown, name);
    });
    return result as T;
  }
  return value;
}

/** the strings of a nested messages bundle, by dotted key */
export function flattenMessages(messages: unknown, prefix = '', result: Messages = {}): Messages {
  if (!isObject(messages)) return result;
  Object.entries(messages).forEach(([name, value]) => {
    const key = prefix ? `${prefix}.${name}` : name;
    if (typeof value === 'string') result[key] = value;
    else if (isObject(value)) flattenMessages(value, key, result);
  });
  return result;
}
