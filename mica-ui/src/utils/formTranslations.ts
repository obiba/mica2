/**
 * The bridge between the `t(key)` tokens of the Mica form configurations and the translation keys
 * of the form builder.
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
 * Replaces, in a schema or UI schema, every string that is exactly a `t(key)` token by its key
 * (a text mixing a token with other content is left as it is: the server still resolves it at
 * render time).
 */
export function unwrapKeys<T>(value: T, key?: string): T {
  if (typeof value === 'string') {
    if (key !== undefined && UNTRANSLATED_KEYS.has(key)) return value;
    return (tokenKey(value) ?? value) as T;
  }
  if (Array.isArray(value)) {
    return value.map((item) => unwrapKeys(item, key)) as T;
  }
  if (isObject(value)) {
    const result: Record<string, unknown> = {};
    Object.keys(value).forEach((name) => {
      result[name] = unwrapKeys(value[name], name);
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

/** dotted keys as a nested bundle (the format of the Mica custom translations) */
export function nestMessages(messages: Messages): NestedMessages {
  const result: NestedMessages = {};
  Object.entries(messages).forEach(([key, value]) => {
    const segments = key.split('.');
    const name = segments.pop() as string;
    let node = result;
    for (const segment of segments) {
      const child = node[segment];
      if (isObject(child)) {
        node = child as NestedMessages;
      } else {
        const created: NestedMessages = {};
        node[segment] = created;
        node = created;
      }
    }
    if (!isObject(node[name])) node[name] = value;
  });
  return result;
}
